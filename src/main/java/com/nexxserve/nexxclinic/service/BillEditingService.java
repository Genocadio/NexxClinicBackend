package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import com.nexxserve.nexxclinic.entity.Worker;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the BILL_EDITING transitional status on visits.
 * <p>
 * Flow:
 * <pre>
 *   COMPLETED → startBillEditing → BILL_EDITING → exit → COMPLETED
 *   CREATED/IN_PROGRESS → startBillEditing → BILL_EDITING → exit → same status
 *   BILL_EDITING → completeBillEditing → pre-edit status
 *   BILL_EDITING → cancelBillEditing → pre-edit status
 * </pre>
 * The pre-edit status is remembered on the visit
 * ({@code billing_edit_source_status}) so exiting the mode restores it.
 * <p>
 * While in BILL_EDITING, the following mutations are permitted:
 * <ul>
 *   <li>{@code editBillVisit} — correct billing lines</li>
 *   <li>{@code addVisitDepartmentProduct} / {@code removeVisitDepartmentProduct}</li>
 *   <li>{@code linkVisitInsurances} / {@code unlinkVisitInsurances}</li>
 * </ul>
 */
@Service
public class BillEditingService {

    private static final Logger log = LoggerFactory.getLogger(BillEditingService.class);
    private static final long EDIT_SESSION_TIMEOUT_MINUTES = 30;

    private final VisitRepository visitRepository;
    private final VisitBillingRepository visitBillingRepository;
    private final WorkerRepository workerRepository;

    public BillEditingService(
        VisitRepository visitRepository,
        VisitBillingRepository visitBillingRepository,
        WorkerRepository workerRepository
    ) {
        this.visitRepository = visitRepository;
        this.visitBillingRepository = visitBillingRepository;
        this.workerRepository = workerRepository;
    }

    /**
     * Transition a COMPLETED or PENDING (CREATED/IN_PROGRESS) visit into
     * BILL_EDITING mode so billing corrections and product/insurance changes
     * can be made. The pre-edit status is remembered on the visit so exiting
     * the mode restores it instead of always forcing COMPLETED.
     */
    @Transactional
    public ApiResponse<?> startBillEditing(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Worker actor = resolveWorker(authUser);
        if (actor == null) {
            return ApiResponse.error("Authentication is required.");
        }

        Visit visit = visitRepository.findByIdForUpdate(visitId)
            .orElse(null);
        if (visit == null) {
            return ApiResponse.error("Visit not found.");
        }

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot edit billing on a cancelled visit.");
        }

        if (visit.getStatus() == VisitStatus.FINALISED) {
            return ApiResponse.error("Cannot edit billing on a finalised visit.");
        }

        if (visit.getStatus() == VisitStatus.BILL_EDITING) {
            // A browser crash or lost connection must not leave financial work
            // locked indefinitely. The visit's updatedAt is refreshed when the
            // mode was entered; a later authorized user can safely reclaim an
            // abandoned session after the timeout.
            LocalDateTime timeout = LocalDateTime.now().minusMinutes(EDIT_SESSION_TIMEOUT_MINUTES);
            if (visit.getUpdatedAt() != null && visit.getUpdatedAt().isBefore(timeout)) {
                log.warn("Recovering abandoned billing edit session for visit {}", visitId);
                visit.setStatus(
                    visit.getBillingEditSourceStatus() != null
                        ? visit.getBillingEditSourceStatus()
                        : VisitStatus.COMPLETED
                );
                visit.setBillingEditSourceStatus(null);
            } else {
                return ApiResponse.error("Visit is already in billing edit mode.");
            }
        }

        if (visit.getStatus() != VisitStatus.CREATED
                && visit.getStatus() != VisitStatus.IN_PROGRESS
                && visit.getStatus() != VisitStatus.COMPLETED) {
            return ApiResponse.error(
                "Visit must be COMPLETED or PENDING (CREATED/IN_PROGRESS) to enter billing edit mode. Current status: " + visit.getStatus()
            );
        }

        // Must have existing billing
        if (visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId).isEmpty()) {
            return ApiResponse.error("Visit has not been billed yet. Use billVisit first.");
        }

        visit.setBillingEditSourceStatus(visit.getStatus());
        visit.setStatus(VisitStatus.BILL_EDITING);
        Visit saved = visitRepository.save(visit);

        log.info(
            "Visit {} entered BILL_EDITING mode by user {} (source status {})",
            visitId, actor.getId(), visit.getBillingEditSourceStatus()
        );

        return ApiResponse.success(
            "Billing edit mode activated.",
            Map.of(
                "visitId", saved.getId(),
                "status", saved.getStatus().name()
            )
        );
    }

    /**
     * Exit BILL_EDITING mode and lock billing back to the visit's pre-edit
     * status. Called after a successful editBillVisit.
     */
    @Transactional
    public ApiResponse<?> completeBillEditing(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Worker actor = resolveWorker(authUser);
        if (actor == null) {
            return ApiResponse.error("Authentication is required.");
        }

        Visit visit = visitRepository.findByIdForUpdate(visitId)
            .orElse(null);
        if (visit == null) {
            return ApiResponse.error("Visit not found.");
        }

        if (visit.getStatus() != VisitStatus.BILL_EDITING) {
            return ApiResponse.error(
                "Visit is not in billing edit mode. Current status: " + visit.getStatus()
            );
        }

        VisitStatus restored = restoreFromBillingEditing(visit);

        log.info(
            "Visit {} exited BILL_EDITING → {} by user {}",
            visitId, restored, actor.getId()
        );

        return ApiResponse.success(
            "Billing edit mode deactivated. Visit is now " + restored + ".",
            Map.of(
                "visitId", visit.getId(),
                "status", restored.name()
            )
        );
    }

    /**
     * Exit BILL_EDITING mode without saving changes (user cancelled the edit).
     * Transitions back to the visit's pre-edit status.
     */
    @Transactional
    public ApiResponse<?> cancelBillEditing(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Worker actor = resolveWorker(authUser);
        if (actor == null) {
            return ApiResponse.error("Authentication is required.");
        }

        Visit visit = visitRepository.findByIdForUpdate(visitId)
            .orElse(null);
        if (visit == null) {
            return ApiResponse.error("Visit not found.");
        }

        if (visit.getStatus() != VisitStatus.BILL_EDITING) {
            return ApiResponse.error(
                "Visit is not in billing edit mode. Current status: " + visit.getStatus()
            );
        }

        VisitStatus restored = restoreFromBillingEditing(visit);

        log.info(
            "Visit {} exited BILL_EDITING → {} (cancelled) by user {}",
            visitId, restored, actor.getId()
        );

        return ApiResponse.success(
            "Billing edit mode cancelled. Visit is now " + restored + ".",
            Map.of(
                "visitId", visit.getId(),
                "status", restored.name()
            )
        );
    }

    /**
     * Restore a visit from BILL_EDITING to its remembered pre-edit status,
     * falling back to COMPLETED (legacy behaviour) when no source was recorded.
     * Returns the restored status.
     */
    private VisitStatus restoreFromBillingEditing(Visit visit) {
        VisitStatus restored = visit.getBillingEditSourceStatus() != null
            ? visit.getBillingEditSourceStatus()
            : VisitStatus.COMPLETED;
        visit.setBillingEditSourceStatus(null);
        visit.setStatus(restored);
        visitRepository.save(visit);
        return restored;
    }

    private Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }
}

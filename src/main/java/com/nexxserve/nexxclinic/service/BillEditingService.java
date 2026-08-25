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
 *   COMPLETED → startBillEditing → BILL_EDITING
 *   BILL_EDITING → completeBillEditing → COMPLETED
 *   BILL_EDITING → cancelBillEditing → COMPLETED
 * </pre>
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
     * Transition a COMPLETED visit into BILL_EDITING mode so billing corrections
     * and product/insurance changes can be made.
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

        if (visit.getStatus() == VisitStatus.BILL_EDITING) {
            return ApiResponse.error("Visit is already in billing edit mode.");
        }

        if (visit.getStatus() == VisitStatus.CREATED) {
            return ApiResponse.error("Cannot edit billing on a visit that has not been completed yet.");
        }

        if (visit.getStatus() == VisitStatus.IN_PROGRESS) {
            return ApiResponse.error("Cannot edit billing on a visit that is still in progress. Complete the visit first.");
        }

        // Must be COMPLETED to enter BILL_EDITING
        if (visit.getStatus() != VisitStatus.COMPLETED) {
            return ApiResponse.error(
                "Visit must be COMPLETED to enter billing edit mode. Current status: " + visit.getStatus()
            );
        }

        // Must have existing billing
        if (visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId).isEmpty()) {
            return ApiResponse.error("Visit has not been billed yet. Use billVisit first.");
        }

        visit.setStatus(VisitStatus.BILL_EDITING);
        Visit saved = visitRepository.save(visit);

        log.info(
            "Visit {} entered BILL_EDITING mode by user {}",
            visitId, actor.getId()
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
     * Exit BILL_EDITING mode and lock billing back to COMPLETED.
     * Called after a successful editBillVisit.
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

        visit.setStatus(VisitStatus.COMPLETED);
        Visit saved = visitRepository.save(visit);

        log.info(
            "Visit {} exited BILL_EDITING → COMPLETED by user {}",
            visitId, actor.getId()
        );

        return ApiResponse.success(
            "Billing edit mode deactivated. Visit is now COMPLETED.",
            Map.of(
                "visitId", saved.getId(),
                "status", saved.getStatus().name()
            )
        );
    }

    /**
     * Exit BILL_EDITING mode without saving changes (user cancelled the edit).
     * Transitions back to COMPLETED.
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

        visit.setStatus(VisitStatus.COMPLETED);
        Visit saved = visitRepository.save(visit);

        log.info(
            "Visit {} exited BILL_EDITING → COMPLETED (cancelled) by user {}",
            visitId, actor.getId()
        );

        return ApiResponse.success(
            "Billing edit mode cancelled. Visit is now COMPLETED.",
            Map.of(
                "visitId", saved.getId(),
                "status", saved.getStatus().name()
            )
        );
    }

    private Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }
}

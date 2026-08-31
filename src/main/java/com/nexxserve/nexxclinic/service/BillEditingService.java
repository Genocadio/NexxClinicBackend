package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import com.nexxserve.nexxclinic.entity.Worker;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the DEPARTMENT_EDITING transitional status on visit departments.
 * <p>
 * Edit mode is per-department — the visit status is left untouched.
 * Flow:
 * <pre>
 *   COMPLETED/FINALISED → startBillEditing → DEPARTMENT_EDITING → exit → back to pre-edit status
 *   DEPARTMENT_EDITING → completeBillEditing → pre-edit status
 *   DEPARTMENT_EDITING → cancelBillEditing → pre-edit status
 * </pre>
 * The pre-edit department status is remembered on the department
 * ({@code billing_edit_source_status}) so exiting the mode restores it.
 * <p>
 * While a department is in DEPARTMENT_EDITING, the following mutations are permitted
 * on that department:
 * <ul>
 *   <li>{@code editBillVisit} — correct billing lines for this department</li>
 *   <li>{@code addVisitDepartmentProduct} / {@code removeVisitDepartmentProduct}</li>
 *   <li>{@code updateVisitDepartmentProductQuantity}</li>
 * </ul>
 */
@Service
public class BillEditingService {

    private static final Logger log = LoggerFactory.getLogger(BillEditingService.class);
    private static final long EDIT_SESSION_TIMEOUT_MINUTES = 30;

    private final VisitRepository visitRepository;
    private final VisitBillingRepository visitBillingRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentBillingRepository visitDepartmentBillingRepository;
    private final WorkerRepository workerRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;

    public BillEditingService(
        VisitRepository visitRepository,
        VisitBillingRepository visitBillingRepository,
        VisitDepartmentRepository visitDepartmentRepository,
        VisitDepartmentBillingRepository visitDepartmentBillingRepository,
        WorkerRepository workerRepository,
        VisitDepartmentProductRepository visitDepartmentProductRepository
    ) {
        this.visitRepository = visitRepository;
        this.visitBillingRepository = visitBillingRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentBillingRepository = visitDepartmentBillingRepository;
        this.workerRepository = workerRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
    }

    /**
     * Transition a COMPLETED or FINALISED visit department into
     * DEPARTMENT_EDITING mode so billing corrections and product changes
     * can be made. The visit status is NOT changed.
     * <p>
     * CANCELLED departments cannot be edited.
     */
    @Transactional
    public ApiResponse<?> startBillEditing(UUID visitDepartmentId, AuthenticatedUser authUser) {
        if (visitDepartmentId == null) {
            return ApiResponse.error("visitDepartmentId is required.");
        }

        Worker actor = resolveWorker(authUser);
        if (actor == null) {
            return ApiResponse.error("Authentication is required.");
        }

        VisitDepartment dept = visitDepartmentRepository.findByIdForUpdate(visitDepartmentId)
            .orElse(null);
        if (dept == null) {
            return ApiResponse.error("Visit department not found.");
        }

        if (dept.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot edit billing on a cancelled department.");
        }

        if (dept.getStatus() == VisitDepartmentStatus.DEPARTMENT_EDITING) {
            // Abandoned session recovery: if the department has been in DEPARTMENT_EDITING
            // for longer than the timeout, allow a new user to reclaim it.
            LocalDateTime timeout = LocalDateTime.now().minusMinutes(EDIT_SESSION_TIMEOUT_MINUTES);
            if (dept.getUpdatedAt() != null && dept.getUpdatedAt().isBefore(timeout)) {
                log.warn("Recovering abandoned billing edit session for visit department {}", visitDepartmentId);
                dept.setStatus(
                    dept.getBillingEditSourceStatus() != null
                        ? dept.getBillingEditSourceStatus()
                        : VisitDepartmentStatus.COMPLETED
                );
                dept.setBillingEditSourceStatus(null);
            } else {
                return ApiResponse.error("Department is already in billing edit mode.");
            }
        }

        if (dept.getStatus() != VisitDepartmentStatus.COMPLETED
                && dept.getStatus() != VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error(
                "Department must be COMPLETED or FINALISED to enter billing edit mode. Current status: " + dept.getStatus()
            );
        }

        // Must have existing billing for this department
        if (!visitDepartmentBillingRepository.existsByVisitDepartmentId(visitDepartmentId)) {
            return ApiResponse.error("Department has not been billed yet. Use billVisit first.");
        }

        dept.setBillingEditSourceStatus(dept.getStatus());
        dept.setStatus(VisitDepartmentStatus.DEPARTMENT_EDITING);
        VisitDepartment saved = visitDepartmentRepository.save(dept);

        // Transition billed/exempted products in THIS department to CORRECTION_PENDING
        int productsUpdated = transitionProductsToCorrectionPending(visitDepartmentId);

        log.info(
            "Visit department {} entered DEPARTMENT_EDITING mode by user {} (source status {}, products transitioned: {})",
            visitDepartmentId, actor.getId(), saved.getBillingEditSourceStatus(), productsUpdated
        );

        return ApiResponse.success(
            "Billing edit mode activated.",
            Map.of(
                "visitDepartmentId", saved.getId(),
                "status", saved.getStatus().name(),
                "productsTransitioned", productsUpdated
            )
        );
    }

    /**
     * Exit DEPARTMENT_EDITING mode and restore the department's pre-edit status.
     * Called after a successful editBillVisit on this department.
     */
    @Transactional
    public ApiResponse<?> completeBillEditing(UUID visitDepartmentId, AuthenticatedUser authUser) {
        if (visitDepartmentId == null) {
            return ApiResponse.error("visitDepartmentId is required.");
        }

        Worker actor = resolveWorker(authUser);
        if (actor == null) {
            return ApiResponse.error("Authentication is required.");
        }

        VisitDepartment dept = visitDepartmentRepository.findByIdForUpdate(visitDepartmentId)
            .orElse(null);
        if (dept == null) {
            return ApiResponse.error("Visit department not found.");
        }

        if (dept.getStatus() != VisitDepartmentStatus.DEPARTMENT_EDITING) {
            return ApiResponse.error(
                "Department is not in billing edit mode. Current status: " + dept.getStatus()
            );
        }

        VisitDepartmentStatus restored = restoreFromBillingEditing(dept);

        log.info(
            "Visit department {} exited DEPARTMENT_EDITING → {} by user {}",
            visitDepartmentId, restored, actor.getId()
        );

        return ApiResponse.success(
            "Billing edit mode deactivated. Department is now " + restored + ".",
            Map.of(
                "visitDepartmentId", dept.getId(),
                "status", restored.name()
            )
        );
    }

    /**
     * Exit DEPARTMENT_EDITING mode without saving changes (user cancelled the edit).
     * Transitions back to the department's pre-edit status, restores products,
     * and removes any PENDING products that were added during the edit session.
     *
     * @param addedProductIds product IDs that were added during the edit session (may be null)
     */
    @Transactional
    public ApiResponse<?> cancelBillEditing(UUID visitDepartmentId, java.util.List<UUID> addedProductIds, AuthenticatedUser authUser) {
        if (visitDepartmentId == null) {
            return ApiResponse.error("visitDepartmentId is required.");
        }

        Worker actor = resolveWorker(authUser);
        if (actor == null) {
            return ApiResponse.error("Authentication is required.");
        }

        VisitDepartment dept = visitDepartmentRepository.findByIdForUpdate(visitDepartmentId)
            .orElse(null);
        if (dept == null) {
            return ApiResponse.error("Visit department not found.");
        }

        if (dept.getStatus() != VisitDepartmentStatus.DEPARTMENT_EDITING) {
            return ApiResponse.error(
                "Department is not in billing edit mode. Current status: " + dept.getStatus()
            );
        }

        VisitDepartmentStatus restored = restoreFromBillingEditing(dept);

        // Remove PENDING products that were added during the edit session.
        // These are identified by the frontend's list of added product IDs.
        if (addedProductIds != null && !addedProductIds.isEmpty()) {
            int removed = removeAddedProducts(visitDepartmentId, addedProductIds);
            log.info("Removed {} products added during cancelled edit for visit department {}", removed, visitDepartmentId);
        }

        log.info(
            "Visit department {} exited DEPARTMENT_EDITING → {} (cancelled) by user {}",
            visitDepartmentId, restored, actor.getId()
        );

        return ApiResponse.success(
            "Billing edit mode cancelled. Department is now " + restored + ".",
            Map.of(
                "visitDepartmentId", dept.getId(),
                "status", restored.name()
            )
        );
    }

    /**
     * Restore a department from DEPARTMENT_EDITING to its remembered pre-edit status,
     * falling back to COMPLETED when no source was recorded. Also restores any
     * products that were transitioned to CORRECTION_PENDING back to BILLED.
     * Returns the restored status.
     */
    private VisitDepartmentStatus restoreFromBillingEditing(VisitDepartment dept) {
        VisitDepartmentStatus restored = dept.getBillingEditSourceStatus() != null
            ? dept.getBillingEditSourceStatus()
            : VisitDepartmentStatus.COMPLETED;
        dept.setBillingEditSourceStatus(null);
        dept.setStatus(restored);
        visitDepartmentRepository.save(dept);

        // Restore CORRECTION_PENDING products back to BILLED so the department
        // doesn't stay in an intermediate editing state after cancellation.
        int restored2 = restoreCorrectionPendingProducts(dept.getId());
        log.info("Restored {} CORRECTION_PENDING products to BILLED for visit department {}", restored2, dept.getId());

        return restored;
    }

    /**
     * Transition all billed/exempted/patient-share-exempted products in a
     * visit department to {@link VisitProductStatus#CORRECTION_PENDING}.
     *
     * @return number of products transitioned
     */
    private int transitionProductsToCorrectionPending(UUID visitDepartmentId) {
        List<VisitDepartmentProduct> products =
            visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartmentId);
        int count = 0;
        for (VisitDepartmentProduct p : products) {
            if (p.isDeleted()) continue;
            if (p.getStatus() == VisitProductStatus.BILLED
                    || p.getStatus() == VisitProductStatus.EXEMPTED
                    || p.getStatus() == VisitProductStatus.PATIENT_SHARE_EXEMPTED) {
                p.setStatus(VisitProductStatus.CORRECTION_PENDING);
                visitDepartmentProductRepository.save(p);
                count++;
            }
        }
        return count;
    }

    /**
     * Restore any products stuck in {@link VisitProductStatus#CORRECTION_PENDING}
     * back to BILLED. This happens when a billing edit session is cancelled
     * before {@code editBillVisit} could re-bill them.
     *
     * @return number of products restored
     */
    private int restoreCorrectionPendingProducts(UUID visitDepartmentId) {
        List<VisitDepartmentProduct> products =
            visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartmentId);
        int count = 0;
        for (VisitDepartmentProduct p : products) {
            if (p.isDeleted()) continue;
            if (p.getStatus() == VisitProductStatus.CORRECTION_PENDING) {
                p.setStatus(VisitProductStatus.BILLED);
                visitDepartmentProductRepository.save(p);
                count++;
            }
        }
        return count;
    }

    /**
     * Soft-delete products that were added during the edit session.
     * Called when the user cancels the edit — these products should not persist.
     *
     * @return number of products removed
     */
    private int removeAddedProducts(UUID visitDepartmentId, java.util.List<UUID> addedProductIds) {
        int count = 0;
        for (UUID productId : addedProductIds) {
            if (productId == null) continue;
            List<VisitDepartmentProduct> allRows = visitDepartmentProductRepository
                .findAllByVisitDepartmentIdAndProductIdIncludingDeleted(visitDepartmentId, productId);
            VisitDepartmentProduct activeRow = allRows.stream()
                .filter(r -> !r.isDeleted())
                .findFirst().orElse(null);
            if (activeRow != null && !activeRow.isDeleted()) {
                activeRow.setDeleted(true);
                visitDepartmentProductRepository.save(activeRow);
                count++;
            }
        }
        return count;
    }

    private Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }
}

package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.graphql.input.EditBillVisitInput;
import com.nexxserve.nexxclinic.model.VisitDepartmentProductSource;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import static com.nexxserve.nexxclinic.service.billing.MoneyUtils.toQuantity;

/**
 * Applies per-department product corrections (add/remove/update) for the
 * edit-billing flow, and converts {@link EditBillVisitInput} to
 * {@link BillVisitInput} so the shared billing pipeline can re-bill the
 * corrected visit state.
 *
 * <p>Extracted from {@code VisitBillingService} to isolate the Phase-1
 * correction logic from the billing orchestration.
 */
@Component
public class BillingCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(BillingCorrectionService.class);

    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final ProductRepository productRepository;
    private final WorkerRepository workerRepository;
    private final BillingValidation billingValidation;

    public BillingCorrectionService(
        VisitRepository visitRepository,
        VisitDepartmentRepository visitDepartmentRepository,
        VisitDepartmentProductRepository visitDepartmentProductRepository,
        ProductRepository productRepository,
        WorkerRepository workerRepository,
        BillingValidation billingValidation
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
        this.productRepository = productRepository;
        this.workerRepository = workerRepository;
        this.billingValidation = billingValidation;
    }

    /**
     * Phase 1 of the edit-billing flow: applies product corrections
     * (add/remove/update) to the visit department products.
     *
     * <p>Runs INSIDE the billing transaction. Mutations are flushed
     * immediately ({@code saveAndFlush}) so Phase 2 (billing) sees the
     * corrected state. The caller's {@code rollback-only} guard ensures
     * a rejected correction never leaves half-applied changes.
     *
     * @return {@code null} on success, or an error {@link ApiResponse}
     */
    public ApiResponse applyVisitProductCorrections(
        EditBillVisitInput input,
        AuthenticatedUser authUser
    ) {
        if (input.visitId() == null) {
            return ApiResponse.error("visitId is required.");
        }

        Worker actingUser = resolveWorker(authUser);
        if (actingUser == null) {
            // F3/F4 fix: fail closed (see billOrEditVisitInternal).
            return ApiResponse.error("Authentication is required to edit billing.");
        }
        long unreadNotes = billingValidation.countUnreadNotesForVisit(input.visitId(), actingUser);
        if (unreadNotes > 0) {
            return ApiResponse.error("You have unread notes. Please read them before editing billing.");
        }

        Visit visit = visitRepository.findById(input.visitId()).orElse(null);
        if (visit == null) {
            return ApiResponse.error("Visit not found.");
        }
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cancelled visits cannot be edited.");
        }

        if (input.departments() == null || input.departments().isEmpty()) {
            return ApiResponse.error("At least one department is required.");
        }

        // Apply per-department corrections
        for (EditBillVisitInput.EditBillVisitDepartmentInput dept : input.departments()) {
            if (dept == null || dept.visitDepartmentId() == null) {
                return ApiResponse.error("Each department entry requires visitDepartmentId.");
            }
            VisitDepartment vd = visitDepartmentRepository.findById(dept.visitDepartmentId()).orElse(null);
            if (vd == null) {
                return ApiResponse.error("Visit department not found.");
            }
            if (!vd.getVisit().getId().equals(visit.getId())) {
                return ApiResponse.error("Visit department does not belong to the visit.");
            }

            // B7 fix: validate updatedProducts vs billProducts quantity conflicts BEFORE
            // any mutation so a rejected correction never leaves half-applied changes.
            Map<UUID, BigDecimal> updatedQtyByProductId = new HashMap<>();
            if (dept.updatedProducts() != null) {
                for (EditBillVisitInput.EditBillVisitUpdateProductInput upd : dept.updatedProducts()) {
                    if (upd != null && upd.productId() != null && upd.quantity() != null) {
                        updatedQtyByProductId.put(upd.productId(), upd.quantity());
                    }
                }
            }
            if (dept.billProducts() != null) {
                for (EditBillVisitInput.EditBillVisitBillProductInput bp : dept.billProducts()) {
                    if (bp == null || bp.productId() == null) {
                        continue;
                    }
                    BigDecimal updatedQty = updatedQtyByProductId.get(bp.productId());
                    if (
                        updatedQty != null &&
                        bp.quantity() != null &&
                        updatedQty.compareTo(bp.quantity()) != 0
                    ) {
                        VisitDepartmentProduct vdp = visitDepartmentProductRepository
                            .findByVisitDepartmentIdAndProductId(dept.visitDepartmentId(), bp.productId())
                            .orElse(null);
                        return ApiResponse.error(
                            "Quantity mismatch for product '" +
                            (vdp != null ? productName(vdp) : bp.productId()) +
                            "': updatedProducts.quantity (" +
                            updatedQty +
                            ") differs from billProducts.quantity (" +
                            bp.quantity() +
                            "). Provide the same quantity in both."
                        );
                    }
                }
            }

            // removals (by productId) — soft delete so historical billing items remain valid
            if (dept.removedProductIds() != null) {
                for (UUID productId : dept.removedProductIds()) {
                    if (productId == null) continue;
                    // Query all rows (active + deleted): Optional is unsafe here because a
                    // product removed multiple times produces multiple soft-deleted rows.
                    // We want the currently active row to remove it.
                    VisitDepartmentProduct vdp = visitDepartmentProductRepository
                        .findAllByVisitDepartmentIdAndProductIdIncludingDeleted(vd.getId(), productId)
                        .stream()
                        .filter(r -> !r.isDeleted())
                        .findFirst()
                        .orElse(null);
                    if (vdp == null) {
                        return ApiResponse.error("Product to remove not found in the visit department.");
                    }
                    // Profile-sourced products are managed by the visit department's
                    // profile: they cannot be removed from billing individually —
                    // changeVisitDepartmentProfile is the only way to replace them.
                    if (vdp.getSource() == VisitDepartmentProductSource.PROFILE) {
                        return ApiResponse.error(
                            "Product '" + productName(vdp) + "' is a profile product and cannot be removed from billing. " +
                            "Change the visit department's profile instead."
                        );
                    }
                    if (!vdp.isDeleted()) {
                        vdp.setDeleted(true);
                        try {
                            visitDepartmentProductRepository.saveAndFlush(vdp);
                        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                            return ApiResponse.error("Unable to remove the product due to a data conflict. Refresh and try again.");
                        }
                    }
                }
            }

            // updates (by productId)
            if (dept.updatedProducts() != null) {
                for (EditBillVisitInput.EditBillVisitUpdateProductInput upd : dept.updatedProducts()) {
                    if (upd == null || upd.productId() == null) {
                        return ApiResponse.error("Each updatedProducts entry requires productId.");
                    }
                    VisitDepartmentProduct vdp = visitDepartmentProductRepository
                        .findByVisitDepartmentIdAndProductId(vd.getId(), upd.productId())
                        .orElse(null);
                    if (vdp == null) {
                        return ApiResponse.error("Product to update not found in the visit department.");
                    }
                    if (upd.quantity() != null && upd.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                        return ApiResponse.error("quantity must be greater than 0.");
                    }
                    if (upd.quantity() != null) {
                        vdp.setQuantity(toQuantity(upd.quantity()));
                    }
                    // On correction, mark as CORRECTION_PENDING (was BILLED/EXEMPTED/
                    // PATIENT_SHARE_EXEMPTED) so billing can re-evaluate. Transient:
                    // Phase 2 re-bills it in the same transaction, moving it back to the
                    // appropriate billed/exempted status.
                    vdp.setStatus(VisitProductStatus.CORRECTION_PENDING);
                    vdp.setBilledBy(null);
                    try {
                        visitDepartmentProductRepository.saveAndFlush(vdp);
                    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                        return ApiResponse.error("Unable to update the product due to a data conflict. Refresh and try again.");
                    }
                }
            }

            // additions
            if (dept.addedProducts() != null) {
                for (EditBillVisitInput.EditBillVisitAddProductInput add : dept.addedProducts()) {
                    if (add == null || add.productId() == null || add.quantity() == null) {
                        return ApiResponse.error("Each addedProducts entry requires productId and quantity.");
                    }
                    if (add.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                        return ApiResponse.error("quantity must be greater than 0.");
                    }
                    Product product = productRepository.findById(add.productId()).orElse(null);
                    if (product == null) {
                        return ApiResponse.error("Product not found.");
                    }

                    // If already exists in department (including soft-deleted), treat as quantity update.
                    // Query all rows: Optional is unsafe when multiple soft-deleted rows exist.
                    // Prefer an active row; fall back to the most-recent soft-deleted one.
                    java.util.List<VisitDepartmentProduct> existingRows = visitDepartmentProductRepository
                        .findAllByVisitDepartmentIdAndProductIdIncludingDeleted(vd.getId(), product.getId());
                    VisitDepartmentProduct existing = existingRows.stream()
                        .filter(r -> !r.isDeleted())
                        .findFirst()
                        .orElseGet(() -> existingRows.stream()
                            .filter(VisitDepartmentProduct::isDeleted)
                            .findFirst()
                            .orElse(null));
                    if (existing != null) {
                        // Un-delete + update: this product was billed in a previous
                        // version, so mark it CORRECTION_PENDING.
                        existing.setQuantity(toQuantity(add.quantity()));
                        existing.setStatus(VisitProductStatus.CORRECTION_PENDING);
                        existing.setBilledBy(null);
                        existing.setDeleted(false);
                        if (add.processorId() != null) {
                            Worker processor = workerRepository.findById(add.processorId()).orElse(null);
                            existing.setProcessor(processor);
                        }
                        try {
                            visitDepartmentProductRepository.saveAndFlush(existing);
                        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                            return ApiResponse.error("Unable to add the product due to a data conflict. Refresh and try again.");
                        }
                        continue;
                    }

                    VisitDepartmentProduct vdp = new VisitDepartmentProduct();
                    vdp.setVisitDepartment(vd);
                    vdp.setProduct(product);
                    vdp.setQuantity(toQuantity(add.quantity()));
                    vdp.setAddedBy(actingUser);
                    // Freshly added, never billed -> PENDING.
                    vdp.setStatus(VisitProductStatus.PENDING);
                    if (add.processorId() != null) {
                        Worker processor = workerRepository.findById(add.processorId()).orElse(null);
                        vdp.setProcessor(processor);
                    }

                    try {
                        visitDepartmentProductRepository.saveAndFlush(vdp);
                    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                        // Partial unique index: a concurrent edit added the same product.
                        return ApiResponse.error(
                            "Product already exists in this visit department."
                        );
                    }
                }
            }
        }

        // BILL_EDITING guard: billing corrections require the visit to be in
        // BILL_EDITING mode. COMPLETED visits must call startBillEditing first.
        // IN_PROGRESS visits are allowed (pre-completion corrections).
        if (visit.getStatus() != VisitStatus.BILL_EDITING
                && visit.getStatus() != VisitStatus.IN_PROGRESS) {
            return ApiResponse.error(
                "Billing corrections require the visit to be in BILL_EDITING mode. " +
                "Use startBillEditing before editing a completed visit."
            );
        }

        return null; // success
    }

    /**
     * Converts an {@link EditBillVisitInput} to a {@link BillVisitInput} by
     * mapping each {@code billProducts.productId} to its
     * {@code visitDepartmentProductId} using the corrected current visit
     * department state.
     *
     * <p>Also enforces the B7 fix: if an {@code updatedProducts} entry
     * declares a quantity that differs from the corresponding
     * {@code billProducts} entry, an {@link IllegalArgumentException} is
     * thrown so the caller can mark the transaction rollback-only.
     */
    public BillVisitInput convertEditInputToBillVisitInput(EditBillVisitInput input) {
        List<BillVisitInput.BillVisitDepartmentInput> departments =
            input.departments() == null
                ? List.of()
                : input.departments().stream().map(d -> {
                    // Collect the quantities declared in updatedProducts so Phase 2 can
                    // detect (and reject) conflicting quantities instead of silently
                    // overwriting the correction (B7 fix).
                    Map<UUID, BigDecimal> updatedQtyByProductId = new HashMap<>();
                    if (d.updatedProducts() != null) {
                        for (EditBillVisitInput.EditBillVisitUpdateProductInput upd : d.updatedProducts()) {
                            if (upd != null && upd.productId() != null && upd.quantity() != null) {
                                updatedQtyByProductId.put(upd.productId(), upd.quantity());
                            }
                        }
                    }

                    // Map productId -> visitDepartmentProductId using the (corrected) current visit department state
                    List<BillVisitInput.BillVisitDepartmentProductInput> billProducts =
                        d.billProducts() == null
                            ? List.of()
                            : d.billProducts().stream().map(bp -> {
                                VisitDepartmentProduct vdp = visitDepartmentProductRepository
                                    .findByVisitDepartmentIdAndProductId(d.visitDepartmentId(), bp.productId())
                                    .orElse(null);
                                if (vdp == null) {
                                    throw new IllegalArgumentException("Product not found in visit department for billing: " + bp.productId());
                                }
                                BigDecimal updatedQty = updatedQtyByProductId.get(
                                    bp.productId()
                                );
                                if (
                                    updatedQty != null &&
                                    bp.quantity() != null &&
                                    updatedQty.compareTo(bp.quantity()) != 0
                                ) {
                                    throw new IllegalArgumentException(
                                        "Quantity mismatch for product '" +
                                        productName(vdp) +
                                        "' (id: " +
                                        bp.productId() +
                                        "): updatedProducts.quantity (" +
                                        updatedQty +
                                        ") differs from billProducts.quantity (" +
                                        bp.quantity() +
                                        "). Provide the same quantity in both."
                                    );
                                }
                                return new BillVisitInput.BillVisitDepartmentProductInput(
                                    vdp.getId(),
                                    d.visitDepartmentId(),
                                    bp.quantity(),
                                    bp.coverageType(),
                                    bp.patientInsuranceId(),
                                    bp.exemptionType(),
                                    bp.patientSharePercentageOverride()
                                );
                            }).toList();

                    return new BillVisitInput.BillVisitDepartmentInput(
                        d.visitDepartmentId(),
                        billProducts,
                        d.payments(),
                        d.note(),
                        d.outstandingType(),
                        d.outstandingReason()
                    );
                }).toList();
        return new BillVisitInput(input.visitId(), departments);
    }

    private Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }

    static String productName(VisitDepartmentProduct item) {
        if (item == null || item.getProduct() == null) {
            return "Unknown product";
        }
        String name = item.getProduct().getName();
        return name == null || name.isBlank() ? "Unknown product" : name;
    }
}

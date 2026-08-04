package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitBillingItem;
import com.nexxserve.nexxclinic.entity.VisitBillingPayment;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartmentNote;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.graphql.input.EditBillVisitInput;
import com.nexxserve.nexxclinic.graphql.input.RecordVisitBillingPaymentInput;
import com.nexxserve.nexxclinic.model.CoverageType;
import com.nexxserve.nexxclinic.model.NoteType;
import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingItemRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingVersionRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentNoteRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductSnapshotRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitInsuranceRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import com.nexxserve.nexxclinic.service.billing.BillingDataMapper;
import com.nexxserve.nexxclinic.service.billing.BillingPaymentDistributor;
import com.nexxserve.nexxclinic.service.billing.BillingPricingCalculator;
import com.nexxserve.nexxclinic.service.billing.BillingValidation;
import com.nexxserve.nexxclinic.service.billing.BillingVersionBuilder;
import static com.nexxserve.nexxclinic.service.billing.MoneyUtils.ZERO;
import static com.nexxserve.nexxclinic.service.billing.MoneyUtils.toMoney;
import static com.nexxserve.nexxclinic.service.billing.MoneyUtils.toQuantity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitBillingService {

    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final ProductRepository productRepository;
    private final VisitBillingRepository visitBillingRepository;
    private final VisitDepartmentBillingRepository visitDepartmentBillingRepository;
    private final DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;
    private final VisitBillingItemRepository visitBillingItemRepository;
    private final VisitBillingVersionRepository visitBillingVersionRepository;
    private final VisitDepartmentProductSnapshotRepository visitDepartmentProductSnapshotRepository;
    private final WorkerRepository workerRepository;
    private final VisitDepartmentNoteRepository visitDepartmentNoteRepository;
    private final BillingVersionBuilder billingVersionBuilder;
    private final BillingPaymentDistributor paymentDistributor;
    private final BillingPricingCalculator pricingCalculator;
    private final BillingValidation billingValidation;
    private final BillingDataMapper billingDataMapper;

    private static final Logger log = LoggerFactory.getLogger(VisitBillingService.class);

    public VisitBillingService(
        VisitRepository visitRepository,
        VisitDepartmentRepository visitDepartmentRepository,
        VisitDepartmentProductRepository visitDepartmentProductRepository,
        VisitInsuranceRepository visitInsuranceRepository,
        PatientInsuranceRepository patientInsuranceRepository,
        ProductRepository productRepository,
        VisitBillingRepository visitBillingRepository,
        VisitDepartmentBillingRepository visitDepartmentBillingRepository,
        DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository,
        VisitBillingItemRepository visitBillingItemRepository,
        VisitBillingVersionRepository visitBillingVersionRepository,
        VisitDepartmentProductSnapshotRepository visitDepartmentProductSnapshotRepository,
        WorkerRepository workerRepository,
        VisitDepartmentNoteRepository visitDepartmentNoteRepository,
        BillingVersionBuilder billingVersionBuilder,
        BillingPaymentDistributor paymentDistributor,
        BillingPricingCalculator pricingCalculator,
        BillingValidation billingValidation,
        BillingDataMapper billingDataMapper
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentProductRepository =
            visitDepartmentProductRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.productRepository = productRepository;
        this.visitBillingRepository = visitBillingRepository;
        this.visitDepartmentBillingRepository =
            visitDepartmentBillingRepository;
        this.departmentInsuranceBillingRepository =
            departmentInsuranceBillingRepository;
        this.visitBillingItemRepository = visitBillingItemRepository;
        this.visitBillingVersionRepository = visitBillingVersionRepository;
        this.visitDepartmentProductSnapshotRepository = visitDepartmentProductSnapshotRepository;
        this.workerRepository = workerRepository;
        this.visitDepartmentNoteRepository = visitDepartmentNoteRepository;
        this.billingVersionBuilder = billingVersionBuilder;
        this.paymentDistributor = paymentDistributor;
        this.pricingCalculator = pricingCalculator;
        this.billingValidation = billingValidation;
        this.billingDataMapper = billingDataMapper;
    }

    @Transactional
    public ApiResponse billVisit(
        BillVisitInput input,
        AuthenticatedUser authUser
    ) {
        try {
            ApiResponse result = billOrEditVisitInternal(input, authUser, false);
            // A bill must be all-or-nothing. billOrEditVisitInternal may have mutated and
            // stamped managed visit-department products (status BILLED/EXEMPTED, billedBy)
            // BEFORE hitting a later validation error (overpayment, missing note, etc.). A
            // returned error ApiResponse alone would COMMIT those dirty entities (Spring
            // only rolls back on exceptions), leaving products permanently BILLED with NO
            // billing container — a state from which neither billVisit nor editBillVisit
            // can recover. Mark the transaction rollback-only so a failed bill never
            // leaves partial product mutations behind.
            if (result.status() != com.nexxserve.nexxclinic.model.ResponseStatus.SUCCESS) {
                org.springframework.transaction.interceptor.TransactionAspectSupport
                    .currentTransactionStatus()
                    .setRollbackOnly();
            }
            return result;
        } catch (IllegalArgumentException e) {
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return ApiResponse.error(e.getMessage());
        }
    }

    @Transactional
    public ApiResponse editBillVisit(
        EditBillVisitInput input,
        AuthenticatedUser authUser
    ) {
        if (input == null) {
            return ApiResponse.error("input is required.");
        }
        if (input.visitId() == null) {
            return ApiResponse.error("visitId is required.");
        }
        // A1/A2/A4 fix: serialize all billing operations per visit. editBillVisit mutates
        // products in Phase 1 (applyVisitProductCorrections) BEFORE billOrEditVisitInternal
        // runs, so the pessimistic lock must be acquired here, before any mutation.
        visitRepository.findByIdForUpdate(input.visitId());

        // Error-correction workflow:
        // 1) Synchronize visit department products (add/remove/update)
        // 2) Create a new immutable billing version from the corrected visit state
        ApiResponse sync = applyVisitProductCorrections(input, authUser);
        if (sync != null) {
            // Phase 1 (applyVisitProductCorrections) may already have mutated and saved
            // products for EARLIER departments before hitting this error (e.g. a product
            // not found in a later department). Return an error ApiResponse alone would
            // COMMIT those mutations (Spring only rolls back on exceptions), so mark the
            // transaction rollback-only — a correction is all-or-nothing.
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return sync;
        }

        try {
            BillVisitInput asBill = convertEditInputToBillVisitInput(input);
            ApiResponse result = billOrEditVisitInternal(asBill, authUser, true);
            // Phase 1 (applyVisitProductCorrections) may already have mutated and saved
            // products. If Phase 2 returns ANY error — not just a thrown exception — the
            // whole correction must roll back so a failed edit never leaves half-applied
            // product changes. A correction is all-or-nothing.
            if (result.status() != com.nexxserve.nexxclinic.model.ResponseStatus.SUCCESS) {
                org.springframework.transaction.interceptor.TransactionAspectSupport
                    .currentTransactionStatus()
                    .setRollbackOnly();
            }
            return result;
        } catch (IllegalArgumentException e) {
            // Phase 1 (applyVisitProductCorrections) may already have mutated and saved
            // products. Mark the transaction rollback-only so those changes are NOT
            // committed when Phase 2 fails — a correction must be all-or-nothing.
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return ApiResponse.error(e.getMessage());
        }
    }

    private ApiResponse billOrEditVisitInternal(
        BillVisitInput input,
        AuthenticatedUser authUser,
        boolean isEdit
    ) {
        // Orchestrator: run the read-only validation/preparation pass, then — only if
        // every validation passed — the persistence pass. All status marks are applied
        // strictly after the billing container is saved in the persistence pass.
        PreparedBill prepared = prepareBill(input, authUser, isEdit);
        if (prepared.error() != null) {
            return prepared.error();
        }
        return persistBill(prepared);
    }

    /**
     * Validation/preparation pass: makes NO explicit DB writes — it validates the
     * request, resolves insurance and pricing, and builds the in-memory billing
     * container (departments, insurance buckets, items, snapshots, payments) inside a
     * {@link PreparedBill}. Only in-memory status changes on managed entities (the
     * orphaned-product reset) occur here; their flush is deferred to the persistence
     * pass. Returns a clean error as a {@link PreparedBill} with a non-null
     * {@code error()} when anything fails, so the persistence pass never runs on
     * invalid input. Must be invoked inside the billing transaction (the deferred
     * orphan flush relies on the transaction's rollback-only guard on error).
     */
    private PreparedBill prepareBill(
        BillVisitInput input,
        AuthenticatedUser authUser,
        boolean isEdit
    ) {
        if (input == null || input.visitId() == null) {
            return PreparedBill.error("visitId is required.");
        }

        Worker actingUser = resolveWorker(authUser);
        // F3/F4 fix: fail closed. A null acting user would silently bypass the unread-notes
        // gate (countUnreadNotesForVisit returns 0 for a null viewer) and stamp products
        // with billedBy = null.
        if (actingUser == null) {
            return PreparedBill.error("Authentication is required to bill a visit.");
        }

        // Block billing when the acting user has unread notes on any department in this visit.
        // (Business rule: cannot bill or edit if there are unread notes.)
        long unreadNotes = billingValidation.countUnreadNotesForVisit(input.visitId(), actingUser);
        if (unreadNotes > 0) {
            log.warn("Unread notes block billing for visit {} and user {}: {}", input.visitId(), actingUser.getId(), unreadNotes);
            return PreparedBill.error(
                isEdit
                    ? "You have unread notes. Please read them before editing billing."
                    : "You have unread notes. Please read them before billing."
            );
        }

        if (input.departments() == null || input.departments().isEmpty()) {
            return PreparedBill.error("At least one department is required.");
        }

        // A1/A2 fix: pessimistic lock serializes concurrent bill/edit per visit so the
        // version counter and product billing cannot race.
        Optional<Visit> visitOptional = visitRepository.findByIdForUpdate(
            input.visitId()
        );
        if (visitOptional.isEmpty()) {
            return PreparedBill.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return PreparedBill.error("Cancelled visits cannot be billed.");
        }

        // B2 fix: allow incremental billing. If a container exists, automatically
        // pivot to an 'edit' flow that preserves existing data and appends the new
        // departments. This allows a Receptionist to bill Dept A and later Dept B
        // without hitting a 'already billed' wall.
        boolean alreadyBilled = billingVersionBuilder.hasAnyExistingBilling(input.visitId());
        boolean effectiveIsEdit = isEdit || alreadyBilled;

        // Reset orphaned status products ONLY if we are truly doing a fresh first bill.
        // The status change is applied in-memory here; the actual flush to the DB is
        // deferred to the persistence pass so the validation pass stays write-free.
        List<VisitDepartmentProduct> orphanedStatusProducts = List.of();
        if (!effectiveIsEdit) {
            orphanedStatusProducts = loadVisitDepartmentProducts(
                visit.getId()
            )
                .stream()
                .filter(p -> !p.isDeleted())
                .filter(p -> !requiresBilling(p))
                .toList();
            if (!orphanedStatusProducts.isEmpty()) {
                log.warn(
                    "Reset {} orphaned BILLED/EXEMPTED product(s) to PENDING for visit {} (no billing container exists).",
                    orphanedStatusProducts.size(),
                    visit.getId()
                );
                for (VisitDepartmentProduct p : orphanedStatusProducts) {
                    p.setStatus(VisitProductStatus.PENDING);
                    p.setBilledBy(null);
                }
            }
        }

        // N2 fix: on edit, carry the previous billing version's payments forward so a
        // correction never resets already-recorded payments to zero.
        Map<UUID, List<BillVisitInput.BillingPaymentInput>> carriedPaymentsByDepartment = new HashMap<>();
        Map<UUID, BigDecimal> carriedPaidByDepartment = new HashMap<>();
        boolean previousVersionFullyPaid = false;
        VisitBilling previousBilling = null;
        if (effectiveIsEdit) {
            // F2 fix: the "latest" billing is the one with the highest version number.
            List<VisitBilling> existingBillings = billingVersionBuilder.orderByVersionDesc(
                visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visit.getId())
            );
            if (!existingBillings.isEmpty()) {
                previousBilling = existingBillings.get(0);
                boolean fullyPaid = true;
                boolean anyDepartment = false;
                for (VisitDepartmentBilling deptBilling : previousBilling.getDepartments()) {
                    if (deptBilling == null || deptBilling.getVisitDepartment() == null) {
                        continue;
                    }
                    anyDepartment = true;
                    UUID rootDeptId = deptBilling.getVisitDepartment().getId();
                    carriedPaidByDepartment.put(
                        rootDeptId,
                        deptBilling.getPaidAmount() == null ? ZERO : deptBilling.getPaidAmount()
                    );
                    if (
                        deptBilling.getOutstandingAmount() != null &&
                        deptBilling.getOutstandingAmount().compareTo(ZERO) > 0
                    ) {
                        fullyPaid = false;
                    }
                    List<BillVisitInput.BillingPaymentInput> payments = deptBilling.getPayments() == null
                        ? List.of()
                        : deptBilling.getPayments().stream()
                            .map(p -> new BillVisitInput.BillingPaymentInput(
                                p.getAmount(),
                                p.getPaymentMethod(),
                                p.getReference()
                            ))
                            .toList();
                    if (!payments.isEmpty()) {
                        carriedPaymentsByDepartment.put(rootDeptId, payments);
                    }
                }
                previousVersionFullyPaid = anyDepartment && fullyPaid;
            }
        }

        List<VisitDepartment> allVisitDepartments =
            visitDepartmentRepository.findByVisitId(visit.getId());
        Map<UUID, VisitDepartment> visitDepartmentsById = allVisitDepartments
            .stream()
            .collect(Collectors.toMap(VisitDepartment::getId, d -> d, (a, b) -> a));

        Map<UUID, VisitDepartment> rootDepartments = new LinkedHashMap<>();
        Map<
            UUID,
            List<BillVisitInput.BillingPaymentInput>
        > rootPaymentsByDepartment = new HashMap<>();
        Map<UUID, BigDecimal> remainingPaidByDepartment = new HashMap<>();

        for (BillVisitInput.BillVisitDepartmentInput departmentInput : input.departments()) {
            if (
                departmentInput == null ||
                departmentInput.visitDepartmentId() == null
            ) {
                return PreparedBill.error(
                    "Each department entry requires a visitDepartmentId."
                );
            }

            VisitDepartment rootVisitDepartment = visitDepartmentsById.get(
                departmentInput.visitDepartmentId()
            );
            if (rootVisitDepartment == null) {
                return PreparedBill.error("Visit department not found.");
            }

            if (!rootVisitDepartment.getVisit().getId().equals(visit.getId())) {
                return PreparedBill.error(
                    "Visit department does not belong to the visit."
                );
            }

            if (!isTopLevelDepartment(rootVisitDepartment)) {
                return PreparedBill.error(
                    "visitDepartmentId must reference a top-level department."
                );
            }

            if (rootDepartments.containsKey(rootVisitDepartment.getId())) {
                return PreparedBill.error(
                    "Duplicate visitDepartmentId provided."
                );
            }

            rootDepartments.put(
                rootVisitDepartment.getId(),
                rootVisitDepartment
            );
            // N2: carry the previous version's payments for this department unless the
            // client explicitly supplies new ones (applies to both edit and incremental
            // billVisit, so payments are never silently reset to zero).
            List<BillVisitInput.BillingPaymentInput> paymentsForDepartment = departmentInput.payments();
            if (effectiveIsEdit && (paymentsForDepartment == null || paymentsForDepartment.isEmpty())) {
                paymentsForDepartment = carriedPaymentsByDepartment.get(rootVisitDepartment.getId());
            }
            rootPaymentsByDepartment.put(
                rootVisitDepartment.getId(),
                paymentsForDepartment
            );

            String paymentError = billingValidation.validatePayments(paymentsForDepartment);
            if (paymentError != null) {
                return PreparedBill.error(paymentError);
            }
            BigDecimal totalPaid = paymentDistributor.totalPayments(paymentsForDepartment);

            if (totalPaid.compareTo(ZERO) > 0) {
                remainingPaidByDepartment.put(
                    rootVisitDepartment.getId(),
                    totalPaid
                );
            } else if (
                effectiveIsEdit &&
                carriedPaymentsByDepartment.get(rootVisitDepartment.getId()) == null &&
                carriedPaidByDepartment
                    .getOrDefault(rootVisitDepartment.getId(), ZERO)
                    .compareTo(ZERO) > 0
            ) {
                // Legacy data: the previous version recorded a paid amount without
                // payment rows. Honor the paid amount so the corrected bill doesn't
                // silently reset it to zero.
                remainingPaidByDepartment.put(
                    rootVisitDepartment.getId(),
                    carriedPaidByDepartment.get(rootVisitDepartment.getId())
                );
            }
        }

        // B1 fix: the new billing version is a projection of the request's departments. If
        // an edit omits a department that has products, that department silently vanishes
        // from the authoritative latest billing view and its payments are never carried
        // forward. Require edits to cover every ROOT department with non-deleted products
        // (products may live on child departments, which bill under their root).
        //
        // Note: a department that appears in the previous billing container but has NO
        // active products today (e.g. all products soft-deleted by an earlier edit, or a
        // legacy hard-delete) is intentionally NOT required — it has nothing billable, so
        // demanding it in the request would make the edit impossible. B5 already prevents
        // hard-deleting billed products, so billed departments always still have rows.
        if (isEdit) {
            Set<UUID> requiredDeptIds = new LinkedHashSet<>();
            for (VisitDepartmentProduct p :
                    visitDepartmentProductRepository.findByVisitDepartmentVisitId(visit.getId())) {
                if (!p.isDeleted()) {
                    requiredDeptIds.add(resolveRootVisitDepartmentId(p.getVisitDepartment()));
                }
            }
            List<UUID> missing = requiredDeptIds.stream()
                .filter(id -> !rootDepartments.containsKey(id))
                .toList();
            if (!missing.isEmpty()) {
                return PreparedBill.error(
                    "editBillVisit must include every department that has products. Missing: " +
                    missing + ". Submit the complete corrected bill for the whole visit."
                );
            }
        }

        List<VisitDepartmentProduct> allProducts = loadVisitDepartmentProducts(
            visit.getId()
        );
        Map<UUID, VisitDepartmentProduct> allProductsById = allProducts
            .stream()
            .collect(Collectors.toMap(VisitDepartmentProduct::getId, p -> p, (a, b) -> a));

        // Collect notes keyed by visitDepartmentId for later validation and persistence
        Map<UUID, String> noteByDepartmentId = new HashMap<>();

        List<BillVisitInput.BillVisitDepartmentInput> departmentsToProcess = input.departments();

        // Merge previous version's departments into the current billing request.
        // E2 fix: billVisit/editBillVisit now explicitly carries forward every department
        // from the previous version that is NOT in the current request and still has
        // billable products, so incremental billing never drops previously billed
        // departments from the authoritative latest view. Carried departments are
        // re-billed at the current catalog/coverage price (no client price override
        // exists anymore); the identity guard below rejects an incremental re-bill if
        // the derived price drifted from the previous snapshot. They are excluded from
        // the fresh-balance note rule below (their notes were already recorded). Their
        // payments are also carried forward with their original method/reference.
        Set<UUID> carriedDepartmentIds = new HashSet<>();
        if (effectiveIsEdit && previousBilling != null) {
            Set<UUID> requestedDeptIds = input.departments().stream()
                .map(BillVisitInput.BillVisitDepartmentInput::visitDepartmentId)
                .collect(Collectors.toSet());

            List<BillVisitInput.BillVisitDepartmentInput> carriedDepartments = new ArrayList<>();
            for (VisitDepartmentBilling prevDeptBilling : previousBilling.getDepartments()) {
                if (prevDeptBilling == null || prevDeptBilling.getVisitDepartment() == null) {
                    continue;
                }
                UUID deptId = prevDeptBilling.getVisitDepartment().getId();
                if (requestedDeptIds.contains(deptId)) {
                    continue;
                }

                // Skip departments whose billed products were all removed by a later
                // edit — they have nothing to carry forward.
                List<BillVisitInput.BillVisitDepartmentProductInput> products =
                    prevDeptBilling.getInsuranceBillings().stream()
                        .filter(ib -> ib != null)
                        .flatMap(ib -> ib.getItems().stream())
                        .filter(item -> item != null && item.getVisitDepartmentProduct() != null)
                        .filter(item -> allProductsById.containsKey(item.getVisitDepartmentProduct().getId()))
                        .map(item -> {
                            UUID carriedInsuranceId =
                                item.getAppliedPatientInsurance() != null
                                    ? item.getAppliedPatientInsurance().getId()
                                    : null;
                            return new BillVisitInput.BillVisitDepartmentProductInput(
                                item.getVisitDepartmentProduct().getId(),
                                null,
                                item.getQuantitySnapshot(),
                                carriedInsuranceId != null ? CoverageType.INSURANCE : CoverageType.PRIVATE,
                                carriedInsuranceId,
                                item.getVisitDepartmentProduct().getStatus() == VisitProductStatus.EXEMPTED
                            );
                        })
                        .toList();
                if (products.isEmpty()) {
                    continue;
                }
                log.info("Carrying forward previously billed department {} to the new billing version.", deptId);

                List<BillVisitInput.BillingPaymentInput> payments =
                    prevDeptBilling.getPayments() == null
                        ? List.of()
                        : prevDeptBilling.getPayments().stream()
                            // Keep only rows that are actually recordable, so the total
                            // we distribute never exceeds what the queue can record.
                            .filter(p -> p != null && p.getAmount() != null
                                && p.getAmount().compareTo(ZERO) > 0
                                && p.getPaymentMethod() != null)
                            .map(p -> new BillVisitInput.BillingPaymentInput(
                                p.getAmount(),
                                p.getPaymentMethod(),
                                p.getReference()
                            ))
                            .toList();

                carriedDepartments.add(new BillVisitInput.BillVisitDepartmentInput(
                    deptId,
                    products,
                    payments,
                    null
                ));
                carriedDepartmentIds.add(deptId);

                // Feed the carried payments into the same distribution path as explicit
                // payments so the re-billed department keeps its original method and
                // reference instead of being re-created as CASH.
                rootPaymentsByDepartment.put(deptId, payments);
                BigDecimal paymentsTotal = ZERO;
                for (BillVisitInput.BillingPaymentInput payment : payments) {
                    paymentsTotal = toMoney(paymentsTotal.add(payment.amount()));
                }
                BigDecimal carriedPaid = prevDeptBilling.getPaidAmount() == null
                    ? ZERO
                    : prevDeptBilling.getPaidAmount();
                remainingPaidByDepartment.put(
                    deptId,
                    paymentsTotal.compareTo(ZERO) > 0 ? paymentsTotal : carriedPaid
                );
                rootDepartments.put(deptId, prevDeptBilling.getVisitDepartment());
            }
            if (!carriedDepartments.isEmpty()) {
                departmentsToProcess = new ArrayList<>(input.departments());
                departmentsToProcess.addAll(carriedDepartments);
            }
        }

        for (BillVisitInput.BillVisitDepartmentInput deptInput : departmentsToProcess) {
            if (hasText(deptInput.note())) {
                noteByDepartmentId.put(
                    deptInput.visitDepartmentId(),
                    deptInput.note().trim()
                );
            }
        }

        // Payment queues per root department, consumed in order when distributing a
        // department's total payment across its insurance buckets. A single submitted
        // payment may be split across buckets, but every bucket-level record keeps that
        // payment's own method and reference.
        Map<UUID, ArrayDeque<BillVisitInput.BillingPaymentInput>> paymentQueuesByRoot =
            paymentDistributor.buildPaymentQueues(rootPaymentsByDepartment);

        Map<UUID, UUID> requestedInsuranceByItem = new LinkedHashMap<>();
        Map<UUID, java.math.BigDecimal> requestedQuantityByItem =
            new LinkedHashMap<>();
        Map<UUID, Boolean> requestedExemptedByItem = new LinkedHashMap<>();
        Set<UUID> requestedProductIds = new LinkedHashSet<>();

        List<VisitInsurance> visitInsurances =
            visitInsuranceRepository.findByVisitId(visit.getId());
        Set<UUID> visitInsurancePatientInsuranceIds = visitInsurances
            .stream()
            .map(v -> v.getPatientInsurance().getId())
            .collect(Collectors.toSet());

        // Snapshot of every already-billed line from the previous version, keyed by the
        // visit department product id. Used to keep the incremental billVisit path
        // strictly idempotent (no silent price/quantity/insurance/exemption drift).
        Map<UUID, PreviousItemSnapshot> previousItemSnapshots = new HashMap<>();
        if (previousBilling != null) {
            for (VisitDepartmentBilling pdb : previousBilling.getDepartments()) {
                if (pdb == null) {
                    continue;
                }
                for (DepartmentInsuranceBilling ib : pdb.getInsuranceBillings()) {
                    if (ib == null) {
                        continue;
                    }
                    for (VisitBillingItem item : ib.getItems()) {
                        if (item == null || item.getVisitDepartmentProduct() == null) {
                            continue;
                        }
                        previousItemSnapshots.put(
                            item.getVisitDepartmentProduct().getId(),
                            new PreviousItemSnapshot(
                                item.getUnitPriceSnapshot(),
                                item.getQuantitySnapshot(),
                                item.getAppliedPatientInsurance() == null
                                    ? null
                                    : item.getAppliedPatientInsurance().getId(),
                                item.getVisitDepartmentProduct().getStatus()
                                    == VisitProductStatus.EXEMPTED
                            )
                        );
                    }
                }
            }
        }

        // actingUser resolved earlier
        Map<UUID, PatientInsurance> appliedInsuranceByItem = new HashMap<>();
        Map<BillingGroup, List<VisitDepartmentProduct>> grouping =
            new LinkedHashMap<>();

        // Iterate departmentsToProcess (requested + carried-forward) so previously
        // billed departments are re-billed into the new version instead of being
        // silently dropped from the authoritative latest billing view.
        for (BillVisitInput.BillVisitDepartmentInput departmentInput : departmentsToProcess) {
            if (
                departmentInput.products() == null ||
                departmentInput.products().isEmpty()
            ) {
                return PreparedBill.error(
                    "Each department must contain at least one product to bill."
                );
            }

            UUID rootVisitDepartmentId = departmentInput.visitDepartmentId();
            for (BillVisitInput.BillVisitDepartmentProductInput productInput : departmentInput.products()) {
                if (
                    productInput == null ||
                    productInput.visitDepartmentProductId() == null
                ) {
                    return PreparedBill.error(
                        "Each product entry requires visitDepartmentProductId."
                    );
                }

                if (
                    requestedProductIds.contains(
                        productInput.visitDepartmentProductId()
                    )
                ) {
                    return PreparedBill.error(
                        "Duplicate visitDepartmentProductId provided in request."
                    );
                }
                requestedProductIds.add(
                    productInput.visitDepartmentProductId()
                );

                VisitDepartmentProduct item = allProductsById.get(
                    productInput.visitDepartmentProductId()
                );
                if (item == null) {
                    // S7 fix: name the offending product id instead of a generic message.
                    return PreparedBill.error(
                        "Invalid billing selection: product id " +
                        productInput.visitDepartmentProductId() +
                        " was not found or is not billable."
                    );
                }
                if (!effectiveIsEdit && !requiresBilling(item)) {
                    // S7 fix: name the offending product so the frontend can debug.
                    return PreparedBill.error(
                        "Invalid billing selection: product '" +
                        productName(item) +
                        "' (id: " +
                        item.getId() +
                        ") is already billed or exempted and cannot be billed again. Use editBillVisit to correct billing."
                    );
                }

                if (
                    productInput.parentVisitDepartmentId() != null &&
                    !item
                        .getVisitDepartment()
                        .getId()
                        .equals(productInput.parentVisitDepartmentId())
                ) {
                    return PreparedBill.error(
                        "Selected product does not belong to the provided parent visit department."
                    );
                }

                if (
                    !isProductUnderRootDepartment(item, rootVisitDepartmentId)
                ) {
                    return PreparedBill.error(
                        "Selected product is not under the requested visit department."
                    );
                }

                if (
                    productInput.quantity() != null &&
                    productInput.quantity().compareTo(ZERO) <= 0
                ) {
                    return PreparedBill.error(
                        "quantity must be greater than 0."
                    );
                }
                if (productInput.patientInsuranceId() != null) {
                    requestedInsuranceByItem.put(
                        item.getId(),
                        productInput.patientInsuranceId()
                    );
                }
                if (productInput.quantity() != null) {
                    requestedQuantityByItem.put(
                        item.getId(),
                        productInput.quantity()
                    );
                }
                if (productInput.isExempted() != null) {
                    requestedExemptedByItem.put(
                        item.getId(),
                        productInput.isExempted()
                    );
                }

                UUID requestedPatientInsuranceId = requestedInsuranceByItem.get(
                    item.getId()
                );
                // Coverage is explicit per line — there is no auto-assignment.
                // PRIVATE: no insurance may be provided. INSURANCE: a patient
                // insurance id is required (and validated below against the visit,
                // the patient and the product coverage).
                CoverageType coverageType = productInput.coverageType();
                if (coverageType == null) {
                    return PreparedBill.error(
                        "coverageType is required for each product. Use PRIVATE or INSURANCE."
                    );
                }
                if (coverageType == CoverageType.PRIVATE && requestedPatientInsuranceId != null) {
                    return PreparedBill.error(
                        "patientInsuranceId cannot be provided when coverageType is PRIVATE."
                    );
                }
                if (coverageType == CoverageType.INSURANCE && requestedPatientInsuranceId == null) {
                    return PreparedBill.error(
                        "patientInsuranceId is required when coverageType is INSURANCE."
                    );
                }
                PatientInsurance appliedInsurance = pricingCalculator.resolveAppliedInsurance(
                    item,
                    coverageType,
                    requestedPatientInsuranceId,
                    visitInsurancePatientInsuranceIds,
                    visitInsurances
                );
                if (
                    coverageType == CoverageType.INSURANCE &&
                    appliedInsurance == null
                ) {
                    return PreparedBill.error(
                        "Selected patientInsuranceId is invalid: it is not linked to this visit, does not cover the product, or the insurance policy is not active."
                    );
                }

                UUID appliedPatientInsuranceId =
                    appliedInsurance == null ? null : appliedInsurance.getId();

                // Incremental billVisit (visit already billed, caller re-bills an
                // already-billed line): the request is only accepted when it is an
                // IDENTICAL re-bill. Changing price, quantity, exemption or the
                // applied insurance is a correction — those must go through
                // editBillVisit, which carries the same role gate (FINANCE/ADMIN).
                if (effectiveIsEdit && !isEdit && !requiresBilling(item)) {
                    PreviousItemSnapshot prev = previousItemSnapshots.get(item.getId());
                    // Price is always derived from the product catalog / applied
                    // insurance (clients can no longer pass a price), so the
                    // "same as before?" comparison uses the live-derived price
                    // against the previous snapshot instead of a stored row price.
                    BigDecimal reqPrice = pricingCalculator.resolveDefaultUnitPrice(
                        item,
                        appliedInsurance
                    );
                    BigDecimal reqQty = requestedQuantityByItem.containsKey(item.getId())
                        ? toQuantity(requestedQuantityByItem.get(item.getId()))
                        : toQuantity(item.getQuantity());
                    boolean requestedExempted = Boolean.TRUE.equals(
                        requestedExemptedByItem.get(item.getId())
                    );
                    boolean exemptedMatches =
                        prev != null && prev.exempted() == requestedExempted;
                    boolean priceMatches =
                        prev != null && reqPrice.compareTo(prev.unitPrice()) == 0;
                    boolean qtyMatches =
                        prev != null && reqQty.compareTo(prev.quantity()) == 0;
                    boolean insuranceMatches =
                        prev != null && Objects.equals(
                            appliedPatientInsuranceId,
                            prev.patientInsuranceId()
                        );
                    if (
                        prev == null ||
                        !exemptedMatches ||
                        !priceMatches ||
                        !qtyMatches ||
                        !insuranceMatches
                    ) {
                        return PreparedBill.error(
                            "Product '" +
                            productName(item) +
                            "' is already billed and its price (as configured in the product catalog), quantity, exemption or insurance differs from the previously billed line. Use editBillVisit to correct the billing."
                        );
                    }
                }

                BillingGroup group = new BillingGroup(
                    rootVisitDepartmentId,
                    appliedPatientInsuranceId
                );
                grouping
                    .computeIfAbsent(group, key -> new ArrayList<>())
                    .add(item);
                appliedInsuranceByItem.put(item.getId(), appliedInsurance);
            }
        }

        if (grouping.isEmpty()) {
            return PreparedBill.error("No products eligible for billing.");
        }

        Set<UUID> exemptedRootDepartmentIds = new LinkedHashSet<>();
        Map<UUID, VisitDepartmentBilling> departmentBillingByRoot =
            new HashMap<>();
        List<VisitDepartmentProduct> productsToSave = new ArrayList<>();
        List<PendingSnapshot> pendingSnapshots = new ArrayList<>();

        // Always create a NEW billing container (immutable versions). The billing
        // version row is minted only AFTER every validation below has passed, so a
        // failed bill never burns a version number or leaves partial writes behind.
        VisitBilling visitBilling = new VisitBilling();
        visitBilling.setVisit(visit);

        for (Map.Entry<
            BillingGroup,
            List<VisitDepartmentProduct>
        > entry : grouping.entrySet()) {
            BillingGroup group = entry.getKey();
            // Resolve from the already-loaded map (includes carried-forward departments)
            // instead of issuing one findById query per billing group.
            VisitDepartment rootVisitDepartment = rootDepartments.get(
                group.rootVisitDepartmentId()
            );
            if (rootVisitDepartment == null) {
                return PreparedBill.error(
                    "Root visit department could not be resolved."
                );
            }

            VisitDepartmentBilling departmentBilling =
                departmentBillingByRoot.computeIfAbsent(
                    rootVisitDepartment.getId(),
                    key -> {
                        VisitDepartmentBilling billing =
                            new VisitDepartmentBilling();
                        billing.setVisitBilling(visitBilling);
                        billing.setVisitDepartment(rootVisitDepartment);
                        billing.setStatus(VisitBillingStatus.UNPAID);
                        billing.setTotalAmount(ZERO);
                        billing.setInsuranceCoveredAmount(ZERO);
                        billing.setPatientPayableAmount(ZERO);
                        billing.setPaidAmount(ZERO);
                        billing.setOutstandingAmount(ZERO);
                        visitBilling.getDepartments().add(billing);
                        return billing;
                    }
                );

            DepartmentInsuranceBilling insuranceBilling =
                new DepartmentInsuranceBilling();
            insuranceBilling.setVisitDepartmentBilling(departmentBilling);
            insuranceBilling.setPatientInsurance(
                group.patientInsuranceId() == null
                    ? null
                    : patientInsuranceRepository
                          .findById(group.patientInsuranceId())
                          .orElse(null)
            );
            insuranceBilling.setStatus(VisitBillingStatus.UNPAID);
            insuranceBilling.setTotalAmount(ZERO);
            insuranceBilling.setInsuranceCoveredAmount(ZERO);
            insuranceBilling.setPatientPayableAmount(ZERO);
            insuranceBilling.setPaidAmount(ZERO);
            insuranceBilling.setOutstandingAmount(ZERO);
            departmentBilling.getInsuranceBillings().add(insuranceBilling);

            BigDecimal total = ZERO;
            BigDecimal insuranceCovered = ZERO;
            BigDecimal patientPayable = ZERO;

            // When editing, we may need to clear any previous invoice for the affected insurance billing
            // so a fresh invoice can be generated for the updated items.
            if (isEdit && hasText(insuranceBilling.getInvoiceUrl())) {
                insuranceBilling.setInvoiceUrl(null);
            }

            for (VisitDepartmentProduct item : entry.getValue()) {
                PatientInsurance appliedInsurance = appliedInsuranceByItem.get(
                    item.getId()
                );
                boolean isExempted = Boolean.TRUE.equals(
                    requestedExemptedByItem.get(item.getId())
                );
                if (isExempted) {
                    exemptedRootDepartmentIds.add(
                        group.rootVisitDepartmentId()
                    );
                }
                BigDecimal unitPrice = pricingCalculator.resolveDefaultUnitPrice(
                    item,
                    appliedInsurance
                );
                BigDecimal quantity = requestedQuantityByItem.containsKey(
                    item.getId()
                )
                    ? toQuantity(requestedQuantityByItem.get(item.getId()))
                    : toQuantity(item.getQuantity());

                BigDecimal lineTotal;
                BigDecimal coveredAmount;
                BigDecimal patientAmount;

                if (isExempted) {
                    unitPrice = ZERO;
                    quantity = BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
                    lineTotal = ZERO;
                    coveredAmount = ZERO;
                    patientAmount = ZERO;
                } else {
                    lineTotal = toMoney(unitPrice.multiply(quantity));
                    coveredAmount = pricingCalculator.calculateCoveredAmount(
                        item,
                        appliedInsurance,
                        quantity,
                        lineTotal
                    );
                    patientAmount = toMoney(lineTotal.subtract(coveredAmount));
                }

                // Snapshot the visit product for this billing version (immutable history).
                // The snapshot is NOT persisted here — it is saved only after every
                // validation has passed so a failed bill leaves no partial writes behind.
                com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot snap =
                    new com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot();
                snap.setVisitDepartmentProductId(item.getId());
                snap.setVisitDepartment(item.getVisitDepartment());
                snap.setProduct(item.getProduct());
                snap.setQuantity(quantity);
                snap.setUnitPrice(unitPrice);
                snap.setStatus(isExempted ? VisitProductStatus.EXEMPTED : VisitProductStatus.BILLED);
                snap.setAppliedPatientInsurance(appliedInsurance);
                snap.setAddedBy(item.getAddedBy());
                snap.setBilledBy(actingUser);

                VisitBillingItem billingItem = new VisitBillingItem();
                billingItem.setDepartmentInsuranceBilling(insuranceBilling);
                billingItem.setVisitDepartmentProduct(item);
                billingItem.setAppliedPatientInsurance(appliedInsurance);
                billingItem.setUnitPriceSnapshot(unitPrice);
                billingItem.setQuantitySnapshot(quantity);
                billingItem.setLineTotal(lineTotal);
                billingItem.setInsuranceCoveredAmount(coveredAmount);
                billingItem.setPatientPayableAmount(patientAmount);
                insuranceBilling.getItems().add(billingItem);
                pendingSnapshots.add(new PendingSnapshot(snap, billingItem));

                total = toMoney(total.add(lineTotal));
                insuranceCovered = toMoney(insuranceCovered.add(coveredAmount));
                patientPayable = toMoney(patientPayable.add(patientAmount));

                // Product status (BILLED/EXEMPTED) is intentionally NOT stamped here.
                // It is applied only after every validation has passed (see the
                // "Status marks are applied only after success" block below), so a
                // rejected bill can never leave products permanently marked BILLED.
                productsToSave.add(item);
            }

            BigDecimal remainingPaidAmount =
                remainingPaidByDepartment.getOrDefault(
                    group.rootVisitDepartmentId(),
                    ZERO
                );
            BigDecimal paidAmount = ZERO;
            if (remainingPaidAmount.compareTo(ZERO) > 0) {
                paidAmount =
                    remainingPaidAmount.compareTo(patientPayable) >= 0
                        ? patientPayable
                        : remainingPaidAmount;
                remainingPaidByDepartment.put(
                    group.rootVisitDepartmentId(),
                    toMoney(remainingPaidAmount.subtract(paidAmount))
                );

                // Tie payment to the insurance billing bucket by consuming the
                // department's submitted payments in order and splitting them across
                // buckets, so every bucket-level record keeps the ORIGINAL payment
                // method and reference (the old code used only the first payment's
                // method/reference and re-created carried-forward payments as CASH).
                if (paidAmount.compareTo(ZERO) > 0) {
                    paymentDistributor.allocatePaymentsToBucket(
                        paymentQueuesByRoot.get(group.rootVisitDepartmentId()),
                        departmentBilling,
                        insuranceBilling,
                        paidAmount
                    );
                }
            }

            BigDecimal outstanding = toMoney(
                patientPayable.subtract(paidAmount)
            );
            insuranceBilling.setTotalAmount(total);
            insuranceBilling.setInsuranceCoveredAmount(insuranceCovered);
            insuranceBilling.setPatientPayableAmount(patientPayable);
            insuranceBilling.setPaidAmount(paidAmount);
            insuranceBilling.setOutstandingAmount(outstanding);
            insuranceBilling.setStatus(
                paymentDistributor.resolveBillingStatus(paidAmount, patientPayable)
            );

            departmentBilling.setTotalAmount(
                toMoney(departmentBilling.getTotalAmount().add(total))
            );
            departmentBilling.setInsuranceCoveredAmount(
                toMoney(
                    departmentBilling
                        .getInsuranceCoveredAmount()
                        .add(insuranceCovered)
                )
            );
            departmentBilling.setPatientPayableAmount(
                toMoney(
                    departmentBilling
                        .getPatientPayableAmount()
                        .add(patientPayable)
                )
            );
            departmentBilling.setPaidAmount(
                toMoney(departmentBilling.getPaidAmount().add(paidAmount))
            );
            departmentBilling.setOutstandingAmount(
                toMoney(
                    departmentBilling.getOutstandingAmount().add(outstanding)
                )
            );
            departmentBilling.setStatus(
                paymentDistributor.resolveBillingStatus(
                    departmentBilling.getPaidAmount(),
                    departmentBilling.getPatientPayableAmount()
                )
            );
        }

        // Reject overpayment: if any department has remaining unapplied payments
        // after distribution across insurance groups, the total payment exceeded patient payable.
        for (Map.Entry<UUID, BigDecimal> entry : remainingPaidByDepartment.entrySet()) {
            if (entry.getValue().compareTo(ZERO) > 0) {
                VisitDepartment dept = rootDepartments.get(entry.getKey());
                String deptName = dept != null && dept.getDepartment() != null
                    ? dept.getDepartment().getName()
                    : "department";
                if (effectiveIsEdit && carriedPaidByDepartment.containsKey(entry.getKey())) {
                    // N2: the corrected bill is smaller than the amount already paid
                    // (e.g. a paid product was erased). Surface this clearly instead of
                    // silently dropping the patient's money.
                    return PreparedBill.error(
                        "The corrected bill for " + deptName + " (" +
                        entry.getValue().toPlainString() +
                        ") is smaller than the amount already paid. Keep the paid product or " +
                        "adjust the payments before correcting the billing."
                    );
                }
                return PreparedBill.error(
                    "Payment amount would exceed the patient payable amount for " + deptName + "."
                );
            }
        }

        // Validate billing notes before persisting: required when any product is exempted
        // or when the patient payment is less than the full payable amount.
        for (Map.Entry<
            UUID,
            VisitDepartmentBilling
        > noteEntry : departmentBillingByRoot.entrySet()) {
            UUID rootDeptId = noteEntry.getKey();
            if (carriedDepartmentIds.contains(rootDeptId)) {
                // A carried-forward department was already billed (with its note) in a
                // previous version and is being re-billed with identical amounts. The
                // fresh-balance note rule does not apply to it.
                continue;
            }
            VisitDepartmentBilling deptBilling = noteEntry.getValue();
            boolean requiresNote =
                exemptedRootDepartmentIds.contains(rootDeptId) ||
                deptBilling.getOutstandingAmount().compareTo(ZERO) > 0;
            if (requiresNote && !hasText(noteByDepartmentId.get(rootDeptId))) {
                return PreparedBill.error(
                    "A billing note is required when items are exempted or the patient payment is less than the payable amount."
                );
            }
        }

        return PreparedBill.ready(
            visit,
            actingUser,
            isEdit,
            previousVersionFullyPaid,
            visitBilling,
            productsToSave,
            pendingSnapshots,
            noteByDepartmentId,
            requestedExemptedByItem,
            requestedQuantityByItem,
            orphanedStatusProducts
        );
    }

    /**
     * Persistence pass: applies every write ONLY after all validations passed in
     * {@link #prepareBill}. Status marks (product BILLED/EXEMPTED, department/visit
     * COMPLETED) are applied strictly AFTER the billing container has been saved.
     */
    private ApiResponse persistBill(PreparedBill prepared) {
        Visit visit = prepared.visit();
        Worker actingUser = prepared.actingUser();
        boolean isEdit = prepared.isEdit();
        boolean previousVersionFullyPaid = prepared.previousVersionFullyPaid();
        VisitBilling visitBilling = prepared.visitBilling();
        List<VisitDepartmentProduct> productsToSave = prepared.productsToSave();
        List<PendingSnapshot> pendingSnapshots = prepared.pendingSnapshots();
        Map<UUID, String> noteByDepartmentId = prepared.noteByDepartmentId();
        Map<UUID, Boolean> requestedExemptedByItem = prepared.requestedExemptedByItem();
        Map<UUID, BigDecimal> requestedQuantityByItem = prepared.requestedQuantityByItem();

        // Deferred orphaned-status flush: products reset to PENDING by the validation
        // pass when no billing container exists.
        visitDepartmentProductRepository.saveAll(prepared.orphanedStatusProducts());

        // Mint the new immutable version (all-or-nothing) and link every child row.
        com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion billingVersion =
            billingVersionBuilder.createNextBillingVersion(visit);
        visitBilling.setBillingVersion(billingVersion);
        for (VisitDepartmentBilling db : visitBilling.getDepartments()) {
            for (DepartmentInsuranceBilling ib : db.getInsuranceBillings()) {
                ib.setBillingVersion(billingVersion);
                for (VisitBillingItem it : ib.getItems()) {
                    it.setBillingVersion(billingVersion);
                }
            }
            for (VisitBillingPayment p : db.getPayments()) {
                p.setBillingVersion(billingVersion);
            }
        }

        // Persist snapshots first so items can reference their immutable history rows.
        for (PendingSnapshot pending : pendingSnapshots) {
            pending.snapshot().setBillingVersion(billingVersion);
            com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot saved =
                visitDepartmentProductSnapshotRepository.save(pending.snapshot());
            pending.item().setVisitDepartmentProductSnapshotId(saved.getId());
        }

        // Status marks applied only after success: stamp products BILLED/EXEMPTED and
        // apply quantity corrections (edit) now that every validation has passed.
        for (VisitDepartmentProduct item : productsToSave) {
            boolean isExempted = Boolean.TRUE.equals(
                requestedExemptedByItem.get(item.getId())
            );
            if (isExempted) {
                item.setStatus(VisitProductStatus.EXEMPTED);
            } else {
                item.setStatus(VisitProductStatus.BILLED);
            }
            item.setBilledBy(actingUser);
            if (isEdit && requestedQuantityByItem.containsKey(item.getId())) {
                item.setQuantity(toQuantity(requestedQuantityByItem.get(item.getId())));
            }
        }
        visitDepartmentProductRepository.saveAll(productsToSave);

        VisitBilling savedVisitBilling = visitBillingRepository.save(
            visitBilling
        );

        // B2 fix: invalidate invoices of ALL previous billing versions.
        // The old code checked insuranceBilling.getInvoiceUrl() on a freshly created
        // object (always null), so stale PDFs from earlier versions were never cleared.
        if (isEdit) {
            List<DepartmentInsuranceBilling> oldBillings =
                departmentInsuranceBillingRepository.findAllByVisitIdExcludingVersion(
                    visit.getId(),
                    billingVersion.getId()
                );
            for (DepartmentInsuranceBilling oldBilling : oldBillings) {
                if (oldBilling.getInvoiceUrl() != null) {
                    oldBilling.setInvoiceUrl(null);
                    departmentInsuranceBillingRepository.save(oldBilling);
                }
            }
        }

        // Persist billing notes attached to the relevant visit department
        for (VisitDepartmentBilling deptBilling : visitBilling.getDepartments()) {
            UUID rootDeptId = deptBilling.getVisitDepartment().getId();
            String note = noteByDepartmentId.get(rootDeptId);
            if (hasText(note)) {
                VisitDepartmentNote billingNote = new VisitDepartmentNote();
                billingNote.setVisitDepartment(deptBilling.getVisitDepartment());
                billingNote.setContent(note);
                billingNote.setCreatedBy(actingUser);
                billingNote.setNoteType(NoteType.BILLING);
                visitDepartmentNoteRepository.save(billingNote);
            }
        }

        boolean fullyBilled = billingVersionBuilder.isVisitFullyBilled(visit.getId());
        if (fullyBilled) {
            // Requirement: Only mark the visit COMPLETED when every non-CANCELLED department is finished (terminal).
            List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visit.getId());
            boolean allFinished = departments.stream()
                .allMatch(dept -> dept.getStatus() == VisitDepartmentStatus.CANCELLED
                    || dept.getStatus() == VisitDepartmentStatus.COMPLETED
                    || dept.getStatus() == VisitDepartmentStatus.BILLING); // BILLING means it's with finance

            if (allFinished) {
                for (VisitDepartment dept : departments) {
                    if (dept.getStatus() == VisitDepartmentStatus.BILLING) {
                        dept.setStatus(VisitDepartmentStatus.COMPLETED);
                        visitDepartmentRepository.save(dept);
                    }
                }
                visit.setStatus(VisitStatus.COMPLETED);
                visitRepository.save(visit);
            }
        }

        // D2 fix: surface which products are still pending billing instead of a
        // generic success message that hides a partially-billed visit.
        String successMessage = "Visit billed successfully.";
        if (isEdit && previousVersionFullyPaid) {
            successMessage =
                "Visit re-billed successfully. Note: the previous billing version was fully paid — " +
                "verify the carried-forward payments.";
        }
        if (!fullyBilled) {
            List<String> pendingProductNames = loadVisitDepartmentProducts(visit.getId())
                .stream()
                .filter(p -> !p.isDeleted() && requiresBilling(p))
                .map(p -> productName(p))
                .toList();
            if (!pendingProductNames.isEmpty()) {
                successMessage =
                    "Visit billed successfully. " +
                    pendingProductNames.size() +
                    " product(s) still pending billing: " +
                    String.join(", ", pendingProductNames) +
                    ".";
            }
        }

        return ApiResponse.success(
            successMessage,
            billingDataMapper.visitBillingToMap(savedVisitBilling)
        );
    }

    @Transactional
    public ApiResponse recordVisitBillingPayment(
        RecordVisitBillingPaymentInput input,
        AuthenticatedUser authUser
    ) {
        if (input == null || input.departmentInsuranceBillingId() == null) {
            return ApiResponse.error("departmentInsuranceBillingId is required.");
        }

        Worker actingUser = resolveWorker(authUser);
        if (actingUser == null) {
            // F3/F4 fix: fail closed (see billOrEditVisitInternal).
            return ApiResponse.error("Authentication is required to record a payment.");
        }
        // For consistency with billing/editing rules: block payments when user has unread notes.
        // (If you want payments allowed even with unread notes, we can relax this.)
        // A3 fix: use the LIGHTWEIGHT visitId lookup here (it does not hydrate the billing
        // entity), so the billing row is not cached in the persistence context with a
        // pre-lock snapshot before we acquire the per-visit lock below.
        UUID visitId = departmentInsuranceBillingRepository
            .findVisitIdById(input.departmentInsuranceBillingId())
            .orElse(null);
        if (visitId == null) {
            return ApiResponse.error("Associated visit not found for this billing.");
        }
        long unreadNotes = billingValidation.countUnreadNotesForVisit(visitId, actingUser);
        if (unreadNotes > 0) {
            return ApiResponse.error("You have unread notes. Please read them before recording payments.");
        }
        String paymentError = billingValidation.validatePayments(List.of(
            new BillVisitInput.BillingPaymentInput(
                input.amount(),
                input.paymentMethod(),
                input.reference()
            )
        ));
        if (paymentError != null) {
            return ApiResponse.error(paymentError);
        }

        // A3/A4 fix: acquire the per-visit lock BEFORE reading paidAmount so the
        // read-modify-write is serialized against concurrent payments/edits (otherwise two
        // payments could both pass the <= patientPayable check and overpay or lose one).
        visitRepository.findByIdForUpdate(visitId);

        // A3 fix: load the billing row with a PESSIMISTIC_WRITE lock AFTER the per-visit
        // lock. The FOR UPDATE re-reads committed state into the persistence context, so
        // paidAmount below reflects any payment a concurrent transaction already committed
        // (a plain findById here would return the stale pre-lock snapshot).
        Optional<DepartmentInsuranceBilling> billingOptional =
            departmentInsuranceBillingRepository.findByIdWithDepartmentBillingAndVisitForUpdate(
                input.departmentInsuranceBillingId()
            );
        if (billingOptional.isEmpty()) {
            return ApiResponse.error("Department insurance billing not found.");
        }

        DepartmentInsuranceBilling insuranceBilling = billingOptional.get();
        Visit visit = insuranceBilling
            .getVisitDepartmentBilling()
            .getVisitBilling()
            .getVisit();
        if (visit != null && visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error(
                "Cancelled visits cannot accept billing payments."
            );
        }

        // H1/H2 fix: a payment must always target the LATEST billing version. Paying
        // against an old version would silently mutate stale data the finance team no
        // longer sees as authoritative. Legacy rows with no version are accepted only
        // when the visit has no version rows at all (pre-version-system data).
        Optional<com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion> latestVersionOpt =
            visitBillingVersionRepository.findFirstByVisitIdOrderByVersionDesc(visit.getId());
        UUID latestVersionId = latestVersionOpt
            .map(com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion::getId)
            .orElse(null);
        UUID paymentVersionId = insuranceBilling.getBillingVersion() == null
            ? null
            : insuranceBilling.getBillingVersion().getId();
        boolean isLatestVersion =
            latestVersionId == null
                ? paymentVersionId == null
                : latestVersionId.equals(paymentVersionId);
        if (!isLatestVersion) {
            return ApiResponse.error(
                "Payment must be recorded against the latest billing version. Refresh the billing data and try again."
            );
        }

        // Validate note requirement: mandatory when payment leaves an outstanding balance
        // (null-safe: legacy rows may have a NULL paid_amount -> treat as zero).
        BigDecimal currentPaid = insuranceBilling.getPaidAmount() == null
            ? ZERO
            : insuranceBilling.getPaidAmount();
        BigDecimal candidatePaid = toMoney(
            currentPaid.add(input.amount())
        );
        if (
            candidatePaid.compareTo(
                insuranceBilling.getPatientPayableAmount()
            ) > 0
        ) {
            return ApiResponse.error(
                "Payment amount would exceed the patient payable amount."
            );
        }
        if (
            candidatePaid.compareTo(
                insuranceBilling.getPatientPayableAmount()
            ) < 0 &&
            !hasText(input.note())
        ) {
            return ApiResponse.error(
                "A billing note is required when the payment does not cover the full outstanding amount."
            );
        }

        BigDecimal nextPaid = toMoney(
            currentPaid.add(input.amount())
        );

        insuranceBilling.setPaidAmount(nextPaid);
        insuranceBilling.setOutstandingAmount(
            toMoney(
                insuranceBilling.getPatientPayableAmount().subtract(nextPaid)
            )
        );
        insuranceBilling.setStatus(
            paymentDistributor.resolveBillingStatus(
                nextPaid,
                insuranceBilling.getPatientPayableAmount()
            )
        );

        VisitDepartmentBilling departmentBilling =
            insuranceBilling.getVisitDepartmentBilling();
        VisitBillingPayment billingPayment = new VisitBillingPayment();
        billingPayment.setVisitDepartmentBilling(departmentBilling);
        billingPayment.setDepartmentInsuranceBilling(insuranceBilling);
        // S6 fix: populate the billing version on payments recorded outside the
        // initial billing flow so the payment audit trail is complete.
        billingPayment.setBillingVersion(insuranceBilling.getBillingVersion());
        billingPayment.setAmount(toMoney(input.amount()));
        billingPayment.setPaymentMethod(input.paymentMethod());
        billingPayment.setReference(input.reference());
        departmentBilling.getPayments().add(billingPayment);

        departmentInsuranceBillingRepository.save(insuranceBilling);
        BigDecimal totalAmount = ZERO;
        BigDecimal insuranceCoveredAmount = ZERO;
        BigDecimal patientPayableAmount = ZERO;
        BigDecimal paidAmount = ZERO;
        BigDecimal outstandingAmount = ZERO;

        for (DepartmentInsuranceBilling childBilling : departmentBilling.getInsuranceBillings()) {
            totalAmount = toMoney(
                totalAmount.add(childBilling.getTotalAmount())
            );
            insuranceCoveredAmount = toMoney(
                insuranceCoveredAmount.add(
                    childBilling.getInsuranceCoveredAmount()
                )
            );
            patientPayableAmount = toMoney(
                patientPayableAmount.add(childBilling.getPatientPayableAmount())
            );
            paidAmount = toMoney(paidAmount.add(childBilling.getPaidAmount()));
            outstandingAmount = toMoney(
                outstandingAmount.add(childBilling.getOutstandingAmount())
            );
        }

        departmentBilling.setTotalAmount(totalAmount);
        departmentBilling.setInsuranceCoveredAmount(insuranceCoveredAmount);
        departmentBilling.setPatientPayableAmount(patientPayableAmount);
        departmentBilling.setPaidAmount(paidAmount);
        departmentBilling.setOutstandingAmount(outstandingAmount);
        departmentBilling.setStatus(
            paymentDistributor.resolveBillingStatus(paidAmount, patientPayableAmount)
        );
        visitDepartmentBillingRepository.save(departmentBilling);

        // Persist billing note if provided
        if (hasText(input.note())) {
            VisitDepartment noteDept = departmentBilling.getVisitDepartment();
            VisitDepartmentNote billingNote = new VisitDepartmentNote();
            billingNote.setVisitDepartment(noteDept);
            billingNote.setContent(input.note().trim());
            billingNote.setCreatedBy(actingUser);
            billingNote.setNoteType(NoteType.BILLING);
            visitDepartmentNoteRepository.save(billingNote);
        }

        return ApiResponse.success(
            "Payment recorded.",
            billingDataMapper.visitBillingToMap(departmentBilling.getVisitBilling())
        );
    }

    @Transactional(readOnly = true)
    public ApiResponse visitBilling(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        List<VisitBilling> billings = billingVersionBuilder.orderByVersionDesc(
            visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId)
        );
        if (billings.isEmpty()) {
            return ApiResponse.error("Visit billing not found.");
        }

        return ApiResponse.success(
            "Visit billing fetched.",
            billingDataMapper.visitBillingToMap(billings.get(0))
        );
    }

    @Transactional(readOnly = true)
    public ApiResponse visitBillings(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        if (!visitRepository.existsById(visitId)) {
            return ApiResponse.error("Visit not found.");
        }

        List<Map<String, Object>> billings = billingVersionBuilder.orderByVersionDesc(
            visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId)
        ).stream()
            .map(billingDataMapper::visitBillingToMap)
            .toList();

        return ApiResponse.success("Visit billings fetched.", billings);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ApiResponse applyVisitProductCorrections(EditBillVisitInput input, AuthenticatedUser authUser) {
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
                    VisitDepartmentProduct vdp = visitDepartmentProductRepository
                        .findByVisitDepartmentIdAndProductIdIncludingDeleted(vd.getId(), productId)
                        .orElse(null);
                    if (vdp == null) {
                        return ApiResponse.error("Product to remove not found in the visit department.");
                    }
                    // Profile-sourced products are managed by the visit department's
                    // profile: they cannot be removed from billing individually —
                    // changeVisitDepartmentProfile is the only way to replace them.
                    if (vdp.getSource() == com.nexxserve.nexxclinic.model.VisitDepartmentProductSource.PROFILE) {
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
                    // On correction, mark as CORRECTION_PENDING (was BILLED/EXEMPTED)
                    // so billing can re-evaluate. Transient: Phase 2 re-bills it in the
                    // same transaction, moving it back to BILLED/EXEMPTED.
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

                    // If already exists in department (including soft-deleted), treat as quantity update
                    VisitDepartmentProduct existing = visitDepartmentProductRepository
                        .findByVisitDepartmentIdAndProductIdIncludingDeleted(vd.getId(), product.getId())
                        .orElse(null);
                    if (existing != null) {
                        // Un-delete + update: this product was billed in a previous
                        // version, so mark it CORRECTION_PENDING.
                        existing.setQuantity(toQuantity(add.quantity()));
                        existing.setStatus(VisitProductStatus.CORRECTION_PENDING);
                        existing.setBilledBy(null);
                        existing.setDeleted(false);
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

        // B3 fix: an edit-billing correction mutates the visit's products, so a
        // COMPLETED visit must be reopened (IN_PROGRESS) for the correction window.
        // Previously applyVisitProductCorrections never called reopenVisitIfCompleted,
        // leaving the visit COMPLETED while products were being corrected.
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            visit.setStatus(VisitStatus.IN_PROGRESS);
            visitRepository.save(visit);
        }

        return null; // success
    }

    private BillVisitInput convertEditInputToBillVisitInput(EditBillVisitInput input) {
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
                                    bp.isExempted()
                                );
                            }).toList();

                    return new BillVisitInput.BillVisitDepartmentInput(
                        d.visitDepartmentId(),
                        billProducts,
                        d.payments(),
                        d.note()
                    );
                }).toList();
        return new BillVisitInput(input.visitId(), departments);
    }

    private List<VisitDepartmentProduct> loadVisitDepartmentProducts(
        UUID visitId
    ) {
        // Single query instead of one per department (N+1). Same semantics as the old
        // per-department query: soft-deleted products are excluded.
        return visitDepartmentProductRepository.findByVisitDepartmentVisitId(visitId);
    }

    private boolean requiresBilling(VisitDepartmentProduct item) {
        // PENDING, UNPAID and CORRECTION_PENDING all require billing.
        return (
            item.getStatus() != VisitProductStatus.BILLED &&
            item.getStatus() != VisitProductStatus.EXEMPTED
        );
    }

    private String productName(VisitDepartmentProduct item) {
        if (item == null || item.getProduct() == null) {
            return "Unknown product";
        }
        String name = item.getProduct().getName();
        return name == null || name.isBlank() ? "Unknown product" : name;
    }

    private UUID resolveRootVisitDepartmentId(VisitDepartment visitDepartment) {
        VisitDepartment current = visitDepartment;
        while (current.getParentVisitDepartment() != null) {
            current = current.getParentVisitDepartment();
        }
        return current.getId();
    }

    private boolean isTopLevelDepartment(VisitDepartment department) {
        return department.getParentVisitDepartment() == null;
    }

    private boolean isProductUnderRootDepartment(
        VisitDepartmentProduct item,
        UUID rootVisitDepartmentId
    ) {
        return resolveRootVisitDepartmentId(item.getVisitDepartment()).equals(
            rootVisitDepartmentId
        );
    }

    private record BillingGroup(
        UUID rootVisitDepartmentId,
        UUID patientInsuranceId
    ) {}

    /**
     * Immutable snapshot of a billed line taken from the previous billing version.
     * Used to enforce that the incremental {@code billVisit} path only ever re-bills
     * an already-billed product IDENTICALLY — any price/quantity/exemption/insurance
     * change is a correction and must go through {@code editBillVisit}.
     */
    private record PreviousItemSnapshot(
        BigDecimal unitPrice,
        BigDecimal quantity,
        UUID patientInsuranceId,
        boolean exempted
    ) {}

    /**
     * A billing snapshot awaiting persistence, paired with the billing item whose
     * {@code visitDepartmentProductSnapshotId} must be backfilled after the snapshot
     * is saved. Snapshots are only persisted after every billing validation passes.
     */
    private record PendingSnapshot(
        com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot snapshot,
        VisitBillingItem item
    ) {}

    /**
     * All state produced by the validation/preparation pass that the persistence
     * pass needs. No DB writes happen while it is built (apart from in-memory status
     * changes on managed entities); {@link #error()} is non-null when validation
     * failed and the flow must stop before persisting anything.
     */
    private record PreparedBill(
        ApiResponse<?> error,
        Visit visit,
        Worker actingUser,
        boolean isEdit,
        boolean previousVersionFullyPaid,
        VisitBilling visitBilling,
        List<VisitDepartmentProduct> productsToSave,
        List<PendingSnapshot> pendingSnapshots,
        Map<UUID, String> noteByDepartmentId,
        Map<UUID, Boolean> requestedExemptedByItem,
        Map<UUID, BigDecimal> requestedQuantityByItem,
        List<VisitDepartmentProduct> orphanedStatusProducts
    ) {
        static PreparedBill error(String message) {
            return new PreparedBill(
                ApiResponse.error(message),
                null,
                null,
                false,
                false,
                null,
                List.of(),
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of()
            );
        }

        static PreparedBill ready(
            Visit visit,
            Worker actingUser,
            boolean isEdit,
            boolean previousVersionFullyPaid,
            VisitBilling visitBilling,
            List<VisitDepartmentProduct> productsToSave,
            List<PendingSnapshot> pendingSnapshots,
            Map<UUID, String> noteByDepartmentId,
            Map<UUID, Boolean> requestedExemptedByItem,
            Map<UUID, BigDecimal> requestedQuantityByItem,
            List<VisitDepartmentProduct> orphanedStatusProducts
        ) {
            return new PreparedBill(
                null,
                visit,
                actingUser,
                isEdit,
                previousVersionFullyPaid,
                visitBilling,
                productsToSave,
                pendingSnapshots,
                noteByDepartmentId,
                requestedExemptedByItem,
                requestedQuantityByItem,
                orphanedStatusProducts
            );
        }
    }

    private Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }

    @Transactional
    public ApiResponse flushSoftDeletedVisitProducts(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        // H2 fix: Acquire pessimistic lock to prevent racing with concurrent bill/edit.
        visitRepository.findByIdForUpdate(visitId);

        if (!visitRepository.existsById(visitId)) {
            return ApiResponse.error("Visit not found.");
        }

        List<VisitDepartmentProduct> softDeleted = visitDepartmentProductRepository.findSoftDeletedByVisitId(visitId);
        if (softDeleted.isEmpty()) {
            return ApiResponse.success("No soft-deleted products to flush.", Map.of("deletedCount", 0));
        }

        int deletedCount = 0;
        int skippedCount = 0;
        for (VisitDepartmentProduct vdp : softDeleted) {
            // B4 fix: NEVER delete VisitBillingItem rows. A soft-deleted product with
            // billing history must be kept (deleted = true) so historical billing
            // versions keep a valid FK. Only hard-delete products without any
            // billing history.
            List<VisitBillingItem> billingItems = visitBillingItemRepository
                .findByVisitDepartmentProductId(vdp.getId());
            if (!billingItems.isEmpty()) {
                skippedCount++;
                continue;
            }
            visitDepartmentProductRepository.delete(vdp);
            deletedCount++;
        }

        return ApiResponse.success(
            "Soft-deleted products flushed successfully.",
            Map.of("deletedCount", deletedCount, "skippedCount", skippedCount)
        );
    }
}

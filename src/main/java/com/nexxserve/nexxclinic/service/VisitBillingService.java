package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
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
import com.nexxserve.nexxclinic.model.EncounterType;
import com.nexxserve.nexxclinic.model.PatientShareSource;
import com.nexxserve.nexxclinic.model.ExemptionType;
import com.nexxserve.nexxclinic.model.NoteType;
import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
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
import com.nexxserve.nexxclinic.service.billing.BillingCarryForwardService;
import com.nexxserve.nexxclinic.service.billing.BillingCorrectionService;
import com.nexxserve.nexxclinic.service.billing.BillingDataMapper;
import com.nexxserve.nexxclinic.service.billing.InvoiceGenerator;
import com.nexxserve.nexxclinic.service.billing.BillingPaymentDistributor;
import com.nexxserve.nexxclinic.service.billing.BillingPaymentService;
import com.nexxserve.nexxclinic.service.billing.BillingPricingCalculator;
import com.nexxserve.nexxclinic.service.billing.BillingQueryService;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitBillingService {

    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
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
    private final BillingCorrectionService billingCorrectionService;
    private final BillingCarryForwardService billingCarryForwardService;
    private final BillingPaymentService billingPaymentService;
    private final BillingQueryService billingQueryService;
    private final InvoiceGenerator invoiceGenerator;
    private final VisitPriceEstimateService visitPriceEstimateService;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final com.nexxserve.nexxclinic.repository.VisitPriceEstimateRepository visitPriceEstimateRepository;

    private static final Logger log = LoggerFactory.getLogger(VisitBillingService.class);

    public VisitBillingService(
        VisitRepository visitRepository,
        VisitDepartmentRepository visitDepartmentRepository,
        VisitDepartmentProductRepository visitDepartmentProductRepository,
        PatientInsuranceRepository patientInsuranceRepository,
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
        BillingDataMapper billingDataMapper,        BillingCorrectionService billingCorrectionService,
        BillingCarryForwardService billingCarryForwardService,
        BillingPaymentService billingPaymentService,
        BillingQueryService billingQueryService,
        InvoiceGenerator invoiceGenerator,
        @Lazy VisitPriceEstimateService visitPriceEstimateService,
        VisitInsuranceRepository visitInsuranceRepository,
        com.nexxserve.nexxclinic.repository.VisitPriceEstimateRepository visitPriceEstimateRepository
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentProductRepository =
            visitDepartmentProductRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.visitBillingRepository = visitBillingRepository;
        this.visitDepartmentBillingRepository =
            visitDepartmentBillingRepository;
        this.departmentInsuranceBillingRepository = departmentInsuranceBillingRepository;
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
        this.billingCorrectionService = billingCorrectionService;
        this.billingCarryForwardService = billingCarryForwardService;
        this.billingPaymentService = billingPaymentService;
        this.billingQueryService = billingQueryService;
        this.invoiceGenerator = invoiceGenerator;
        this.visitPriceEstimateService = visitPriceEstimateService;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.visitPriceEstimateRepository = visitPriceEstimateRepository;
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

        // BILL_EDITING guard: editing an already-billed visit requires BILL_EDITING mode.
        // Use startBillEditing mutation to enter this mode on a COMPLETED visit.
        java.util.Optional<com.nexxserve.nexxclinic.entity.Visit> visitCheck =
            visitRepository.findById(input.visitId());
        if (visitCheck.isPresent()) {
            com.nexxserve.nexxclinic.entity.Visit v = visitCheck.get();
            if (v.getStatus() == com.nexxserve.nexxclinic.model.VisitStatus.COMPLETED) {
                return ApiResponse.error(
                    "Visit is COMPLETED. Use startBillEditing to enter billing edit mode first."
                );
            }
            if (v.getStatus() == com.nexxserve.nexxclinic.model.VisitStatus.CANCELLED) {
                return ApiResponse.error("Cannot edit billing on a cancelled visit.");
            }
        }

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
        // A true billing edit (editBillVisit) is a fully independent new snapshot: it
        // recomputes totals from the current products/coverage and does NOT carry
        // forward previously-collected payments, apply credits, or compare against the
        // prior paid amount. Incremental billVisit re-bills (alreadyBilled, no explicit
        // edit) still carry previous departments/payments so the authoritative latest
        // version is never silently reset. This boolean gates every carry-forward step.
        boolean carryForward = effectiveIsEdit && !isEdit;

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
        if (carryForward) {
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
            // N2: on incremental re-bills, carry the previous version's payments for
            // this department unless the client explicitly supplies new ones. True
            // edits (carryForward == false) do NOT carry — they start fresh.
            List<BillVisitInput.BillingPaymentInput> paymentsForDepartment = departmentInput.payments();
            if (carryForward && (paymentsForDepartment == null || paymentsForDepartment.isEmpty())) {
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
                carryForward &&
                carriedPaymentsByDepartment.get(rootVisitDepartment.getId()) == null &&
                carriedPaidByDepartment
                    .getOrDefault(rootVisitDepartment.getId(), ZERO)
                    .compareTo(ZERO) > 0
            ) {
                // Legacy data (incremental re-bill only): the previous version recorded
                // a paid amount without payment rows. Honor the paid amount so the
                // corrected bill doesn't
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

        // DIAG: submitted payments and the running remaining-paid per department.
        if (log.isDebugEnabled()) {
            log.debug(
                "[BILL-DIAG] visit={} isEdit={} effectiveIsEdit={} departmentsInRequest={}",
                visit.getId(), isEdit, effectiveIsEdit,
                input.departments() == null ? 0 : input.departments().size()
            );
            for (UUID deptId : rootDepartments.keySet()) {
                VisitDepartment dept = rootDepartments.get(deptId);
                String deptName = dept != null && dept.getDepartment() != null
                    ? dept.getDepartment().getName()
                    : "department";
                List<BillVisitInput.BillingPaymentInput> payments =
                    rootPaymentsByDepartment.get(deptId);
                String paymentsSummary = payments == null
                    ? "[]"
                    : payments.stream()
                        .map(p -> p == null ? "null"
                            : p.paymentMethod() + "=" + p.amount())
                        .collect(Collectors.joining(", ", "[", "]"));
                log.debug(
                    "[BILL-DIAG] visit={} dept={} ({}): submittedPayments={} totalPaid={} remainingPaid={}",
                    visit.getId(), deptId, deptName, paymentsSummary,
                    paymentDistributor.totalPayments(payments),
                    remainingPaidByDepartment.getOrDefault(deptId, ZERO)
                );
            }
        }
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

        Map<UUID, VisitDepartmentProduct> allProductsById = billingQueryService
            .loadVisitDepartmentProductsById(visit.getId());

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
        if (carryForward && previousBilling != null) {
            BillingCarryForwardService.CarryForwardResult carryResult =
                billingCarryForwardService.mergePreviousDepartments(
                    input,
                    previousBilling,
                    allProductsById,
                    rootDepartments,
                    rootPaymentsByDepartment,
                    remainingPaidByDepartment
                );
            departmentsToProcess = carryResult.departmentsToProcess();
            carriedDepartmentIds = carryResult.carriedDepartmentIds();
            // Note: rootDepartments, rootPaymentsByDepartment, and remainingPaidByDepartment
            // are mutated in place by the carry-forward service (new entries are put into
            // them). No reassignment needed — the existing map references remain valid.
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
        Map<UUID, ExemptionType> requestedExemptionTypeByItem = new LinkedHashMap<>();
        Map<UUID, UUID> requestedPatientShareOverrideByItem = new LinkedHashMap<>();
        Set<UUID> requestedProductIds = new LinkedHashSet<>();

        List<VisitInsurance> visitInsurances =
            billingQueryService.loadVisitInsurances(visit.getId());
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
                                resolveExemptionTypeFromStatus(
                                    item.getVisitDepartmentProduct().getStatus()
                                )
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
                ExemptionType resolvedExemption = productInput.exemptionType() != null
                    ? productInput.exemptionType()
                    : ExemptionType.NONE;
                if (resolvedExemption != ExemptionType.NONE) {
                    requestedExemptionTypeByItem.put(
                        item.getId(),
                        resolvedExemption
                    );
                }
                if (productInput.patientSharePercentageOverride() != null) {
                    requestedPatientShareOverrideByItem.put(
                        item.getId(),
                        productInput.patientSharePercentageOverride()
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
                    ExemptionType requestedExemption = requestedExemptionTypeByItem.getOrDefault(
                        item.getId(), ExemptionType.NONE
                    );
                    boolean exemptionMatches =
                        prev != null && prev.exemptionType() == requestedExemption;
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
                        !exemptionMatches ||
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

        // Pre-fetch all ProductInsuranceCoverage rows for insurance-billed products
        // in a single query, eliminating the N+1 pattern where each billing line
        // triggered its own coverage lookup (resolveDefaultUnitPrice + calculateCoveredAmount).
        Set<UUID> insuranceProductIds = new HashSet<>();
        Set<UUID> insuranceProviderIds = new HashSet<>();
        for (Map.Entry<UUID, PatientInsurance> e : appliedInsuranceByItem.entrySet()) {
            PatientInsurance pi = e.getValue();
            if (pi != null && pi.getInsuranceProvider() != null) {
                VisitDepartmentProduct vdp = allProductsById.get(e.getKey());
                if (vdp != null && vdp.getProduct() != null) {
                    insuranceProductIds.add(vdp.getProduct().getId());
                    insuranceProviderIds.add(pi.getInsuranceProvider().getId());
                }
            }
        }
        Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetchedProductCoverages =
            pricingCalculator.prefetchProductCoverages(insuranceProductIds, insuranceProviderIds);
        Map<UUID, Map<UUID, List<com.nexxserve.nexxclinic.entity.InsuranceCoverage>>> prefetchedShareCoverages =
            pricingCalculator.prefetchPatientShareCoverages(insuranceProviderIds);

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

        // Lookup map: visitDepartmentId -> department input (for outstandingType/reason)
        Map<UUID, BillVisitInput.BillVisitDepartmentInput> deptInputById = new HashMap<>();
        for (BillVisitInput.BillVisitDepartmentInput d : departmentsToProcess) {
            deptInputById.put(d.visitDepartmentId(), d);
        }

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
            // If the current billing time would fall before the visit date (e.g. the
            // visit date was edited forward for an unbilled visit), do not backdate
            // billing before the visit. Clamp the billing date to at least 5 minutes
            // after the visit date, matching the admin billing-date validation rule.
            // Otherwise leave it null so the @PrePersist hook stamps LocalDateTime.now().
            java.time.LocalDateTime visitDateNow = visit.getVisitDate();
            if (visitDateNow != null) {
                java.time.LocalDateTime minBillingDate = visitDateNow.plusMinutes(5);
                java.time.LocalDateTime billingNow = java.time.LocalDateTime.now();
                if (billingNow.isBefore(minBillingDate)) {
                    insuranceBilling.setBillingDate(minBillingDate);
                }
            }
            departmentBilling.getInsuranceBillings().add(insuranceBilling);

            BigDecimal total = ZERO;
            BigDecimal insuranceCovered = ZERO;
            BigDecimal patientPayable = ZERO;

            for (VisitDepartmentProduct item : entry.getValue()) {
                PatientInsurance appliedInsurance = appliedInsuranceByItem.get(
                    item.getId()
                );
                ExemptionType exemptionType = requestedExemptionTypeByItem.getOrDefault(
                    item.getId(), ExemptionType.NONE
                );
                if (exemptionType != ExemptionType.NONE) {
                    exemptedRootDepartmentIds.add(
                        group.rootVisitDepartmentId()
                    );
                }
                BigDecimal unitPrice = pricingCalculator.resolveDefaultUnitPrice(
                    item,
                    appliedInsurance,
                    prefetchedProductCoverages
                );
                BigDecimal quantity = requestedQuantityByItem.containsKey(
                    item.getId()
                )
                    ? toQuantity(requestedQuantityByItem.get(item.getId()))
                    : toQuantity(item.getQuantity());

                BigDecimal lineTotal;
                BigDecimal coveredAmount;
                BigDecimal patientAmount;

                ExemptionType lineExemptionType = requestedExemptionTypeByItem.getOrDefault(
                    item.getId(), ExemptionType.NONE
                );

                // Resolve patient share percentage via the multi-layer chain.
                // Guard: item.getVisitDepartment() or getDepartment() may be null on
                // deserialized/detached entities, so extract identifiers defensively.
                UUID departmentId = null;
                EncounterType encounterType = null;
                try {
                    if (item.getVisitDepartment() != null) {
                        encounterType = item.getVisitDepartment().getEncounterType();
                        if (item.getVisitDepartment().getDepartment() != null) {
                            departmentId = item.getVisitDepartment().getDepartment().getId();
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract department/encounterType from item {}: {}", item.getId(), e.getMessage());
                }
                UUID override = requestedPatientShareOverrideByItem != null
                    ? requestedPatientShareOverrideByItem.get(item.getId()) : null;
                com.nexxserve.nexxclinic.service.billing.BillingPricingCalculator.ResolvedPatientShare resolved =
                    pricingCalculator.resolvePatientSharePercentage(
                        appliedInsurance, departmentId, encounterType, override, prefetchedShareCoverages
                    );

                if (lineExemptionType == ExemptionType.FULL) {
                    // FULL waiver: entire line zeroed (insurance covers nothing)
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
                        lineTotal,
                        resolved.percentage(),
                        prefetchedProductCoverages
                    );
                    patientAmount = toMoney(lineTotal.subtract(coveredAmount));
                    if (lineExemptionType == ExemptionType.PATIENT_SHARE) {
                        // Patient share waiver: insurance keeps its covered amount,
                        // patient pays nothing for this line.
                        patientAmount = ZERO;
                    }
                }

                // DIAG: per-line money evaluation.
                if (log.isDebugEnabled()) {
                    String insuranceInfo = "null";
                    if (appliedInsurance != null) {
                        try {
                            var provider = appliedInsurance.getInsuranceProvider();
                            insuranceInfo = appliedInsurance.getId()
                                + "(" + (provider != null ? provider.getInsuranceName() : "?")
                                + ", providerPct=" + (provider != null ? provider.getBasePatientSharePercentage() : "?")
                                + ")";
                        } catch (Exception e) {
                            insuranceInfo = appliedInsurance.getId() + "(lazy-load-error)";
                        }
                    }                    log.debug(
                        "[BILL-DIAG] visit={} rootDept={} product='{}' (id={}, qty={}, exemption={}, coverage={}, insurance={}): unitPrice={} lineTotal={} covered={} patientAmount={} patientSharePct={} source={}",
                        visit.getId(), group.rootVisitDepartmentId(),
                        productName(item),
                        item.getId(),
                        quantity,
                        lineExemptionType,
                        entry.getValue().isEmpty() ? "n/a" : appliedInsurance == null
                            ? "PRIVATE" : "INSURANCE",
                        insuranceInfo,
                        unitPrice,
                        lineTotal,
                        coveredAmount,
                        patientAmount,
                        resolved != null ? resolved.percentage() : "n/a",
                        resolved != null ? resolved.source() : "n/a"
                    );
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
                snap.setStatus(switch (lineExemptionType) {
                    case FULL -> VisitProductStatus.EXEMPTED;
                    case PATIENT_SHARE -> VisitProductStatus.PATIENT_SHARE_EXEMPTED;
                    case NONE -> VisitProductStatus.BILLED;
                });
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
                // Snapshot patient share audit fields
                if (lineExemptionType == ExemptionType.FULL) {
                    billingItem.setAppliedPatientSharePct(0);
                    billingItem.setPatientShareSource(PatientShareSource.EXEMPTED);
                } else if (lineExemptionType == ExemptionType.PATIENT_SHARE) {
                    billingItem.setAppliedPatientSharePct(resolved != null ? resolved.percentage() : 0);
                    billingItem.setPatientShareSource(PatientShareSource.EXEMPTED);
                } else if (appliedInsurance != null) {
                    billingItem.setAppliedPatientSharePct(resolved != null ? resolved.percentage() : 0);
                    billingItem.setPatientShareSource(resolved != null ? resolved.source() : PatientShareSource.PROVIDER_DEFAULT);
                }
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
            // Set outstanding classification from the department input
            BillVisitInput.BillVisitDepartmentInput deptInput = deptInputById.get(group.rootVisitDepartmentId());
            if (deptInput != null) {
                insuranceBilling.setOutstandingType(deptInput.outstandingType());
                insuranceBilling.setOutstandingReason(deptInput.outstandingReason());
            }

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

            // DIAG: bucket + department summary after money evaluation and payment
            // allocation for this billing group.
            if (log.isDebugEnabled()) {
                String insuranceLabel = group.patientInsuranceId() == null
                    ? "PRIVATE"
                    : group.patientInsuranceId().toString();
                log.debug(
                    "[BILL-DIAG] visit={} rootDept={} BUCKET insurance={}: total={} insuranceCovered={} patientPayable={} paidAmount={} outstanding={} status={} | remainingBefore={} allocatedNow={} remainingAfter={}",
                    visit.getId(),
                    group.rootVisitDepartmentId(),
                    insuranceLabel,
                    total,
                    insuranceCovered,
                    patientPayable,
                    paidAmount,
                    outstanding,
                    insuranceBilling.getStatus(),
                    remainingPaidAmount,
                    paidAmount,
                    remainingPaidByDepartment.getOrDefault(
                        group.rootVisitDepartmentId(), ZERO
                    )
                );
                log.debug(
                    "[BILL-DIAG] visit={} rootDept={} DEPT total={} insuranceCovered={} patientPayable={} paidAmount={} outstanding={} status={}",
                    visit.getId(),
                    group.rootVisitDepartmentId(),
                    departmentBilling.getTotalAmount(),
                    departmentBilling.getInsuranceCoveredAmount(),
                    departmentBilling.getPatientPayableAmount(),
                    departmentBilling.getPaidAmount(),
                    departmentBilling.getOutstandingAmount(),
                    departmentBilling.getStatus()
                );
            }
        }        // Reject overpayment: if any department has remaining unapplied payments
        // after distribution across insurance groups, the total payment exceeded patient payable.
        String overpaymentError = billingValidation.validateOverpayment(
            remainingPaidByDepartment,
            rootDepartments,
            rootPaymentsByDepartment,
            departmentBillingByRoot,
            effectiveIsEdit,
            carriedPaidByDepartment,
            paymentDistributor,
            visit.getId()
        );
        if (overpaymentError != null) {
            return PreparedBill.error(overpaymentError);
        }

        // Validate billing notes before persisting: required when any product is exempted
        // or when the patient payment is less than the full payable amount.
        String noteError = billingValidation.validateBillingNotes(
            departmentBillingByRoot,
            exemptedRootDepartmentIds,
            carriedDepartmentIds,
            noteByDepartmentId
        );
        if (noteError != null) {
            return PreparedBill.error(noteError);
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
            requestedExemptionTypeByItem,
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
        Map<UUID, ExemptionType> requestedExemptionTypeByItem = prepared.requestedExemptionTypeByItem();
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
        // Batch save: set the billing version on every snapshot, then persist all at
        // once and link generated IDs back to the billing items.
        for (PendingSnapshot pending : pendingSnapshots) {
            pending.snapshot().setBillingVersion(billingVersion);
        }
        List<com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot> savedSnapshots =
            visitDepartmentProductSnapshotRepository.saveAll(
                pendingSnapshots.stream().map(PendingSnapshot::snapshot).toList()
            );
        for (int i = 0; i < pendingSnapshots.size(); i++) {
            pendingSnapshots.get(i).item().setVisitDepartmentProductSnapshotId(
                savedSnapshots.get(i).getId()
            );
        }

        // Status marks applied only after success: stamp products BILLED/EXEMPTED/
        // PATIENT_SHARE_EXEMPTED and apply quantity corrections (edit) now that every
        // validation has passed.
        for (VisitDepartmentProduct item : productsToSave) {
            ExemptionType ext = requestedExemptionTypeByItem.getOrDefault(
                item.getId(), ExemptionType.NONE
            );
            item.setStatus(switch (ext) {
                case FULL -> VisitProductStatus.EXEMPTED;
                case PATIENT_SHARE -> VisitProductStatus.PATIENT_SHARE_EXEMPTED;
                case NONE -> VisitProductStatus.BILLED;
            });
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

        // Delete pre-billing price estimates now that real billing has been persisted
        visitPriceEstimateService.deleteEstimates(visit.getId());

        return ApiResponse.success(
            successMessage,
            billingDataMapper.visitBillingToMap(savedVisitBilling)
        );
    }    @Transactional
    public ApiResponse recordVisitBillingPayment(
        RecordVisitBillingPaymentInput input,
        AuthenticatedUser authUser
    ) {
        return billingPaymentService.recordVisitBillingPayment(input, authUser);
    }

    @Transactional(readOnly = true)
    public ApiResponse visitBilling(UUID visitId) {
        return billingQueryService.visitBilling(visitId);
    }

    @Transactional
    public ApiResponse quickBill(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Worker actingUser = resolveWorker(authUser);
        if (actingUser == null) {
            return ApiResponse.error("Authentication is required to quick-bill a visit.");
        }

        Optional<Visit> visitOpt = visitRepository.findById(visitId);
        if (visitOpt.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }
        Visit visit = visitOpt.get();

        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Visit is already completed.");
        }
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot bill a cancelled visit.");
        }

        // Eligibility: ≤1 linked insurance
        List<VisitInsurance> visitInsurances = visitInsuranceRepository.findByVisitId(visitId);
        if (visitInsurances.size() > 1) {
            return ApiResponse.error("Visit has more than 1 linked insurance. Use the full billing flow instead.");
        }

        // Eligibility: 0 unread notes
        long unreadNotes = billingValidation.countUnreadNotesForVisit(visitId, actingUser);
        if (unreadNotes > 0) {
            return ApiResponse.error("You have unread notes. Please read them before billing.");
        }

        // Load pre-calculated estimates
        List<com.nexxserve.nexxclinic.entity.VisitPriceEstimate> estimates =
            visitPriceEstimateRepository.findByVisitId(visitId);
        if (estimates.isEmpty()) {
            return ApiResponse.error("No products to bill. Add products to the visit first.");
        }

        // Build BillVisitInput from estimates
        BillVisitInput input = buildQuickBillInput(visit, visitInsurances, estimates);
        if (input == null) {
            return ApiResponse.error("Could not build billing input from pre-calculations.");
        }

        return billVisit(input, authUser);
    }

    /**
     * Builds a BillVisitInput from the pre-calculated price estimates.
     * Groups products by their root visit department, auto-determines
     * coverage type based on whether the estimate has an applied insurance.
     */
    private BillVisitInput buildQuickBillInput(
        Visit visit,
        List<VisitInsurance> visitInsurances,
        List<com.nexxserve.nexxclinic.entity.VisitPriceEstimate> estimates
    ) {
        // Build a map of patientInsuranceId -> VisitInsurance for quick lookup
        Map<UUID, VisitInsurance> visitInsuranceByPiId = new HashMap<>();
        for (VisitInsurance vi : visitInsurances) {
            visitInsuranceByPiId.put(vi.getPatientInsurance().getId(), vi);
        }

        // Group estimates by root visit department
        Map<UUID, List<com.nexxserve.nexxclinic.entity.VisitPriceEstimate>> estimatesByRootDept = new LinkedHashMap<>();
        for (com.nexxserve.nexxclinic.entity.VisitPriceEstimate est : estimates) {
            UUID rootDeptId = resolveRootVisitDepartmentId(est.getVisitDepartmentProduct().getVisitDepartment());
            estimatesByRootDept.computeIfAbsent(rootDeptId, k -> new ArrayList<>()).add(est);
        }

        List<BillVisitInput.BillVisitDepartmentInput> departments = new ArrayList<>();
        for (Map.Entry<UUID, List<com.nexxserve.nexxclinic.entity.VisitPriceEstimate>> entry : estimatesByRootDept.entrySet()) {
            UUID rootDeptId = entry.getKey();
            List<com.nexxserve.nexxclinic.entity.VisitPriceEstimate> deptEstimates = entry.getValue();

            List<BillVisitInput.BillVisitDepartmentProductInput> products = new ArrayList<>();
            for (com.nexxserve.nexxclinic.entity.VisitPriceEstimate est : deptEstimates) {
                UUID vdpId = est.getVisitDepartmentProduct().getId();
                com.nexxserve.nexxclinic.model.CoverageType coverageType;
                UUID patientInsuranceId = null;

                if (est.getAppliedPatientInsurance() != null && est.getResolvedPatientSharePct() < 100) {
                    coverageType = com.nexxserve.nexxclinic.model.CoverageType.INSURANCE;
                    patientInsuranceId = est.getAppliedPatientInsurance().getId();
                } else {
                    coverageType = com.nexxserve.nexxclinic.model.CoverageType.PRIVATE;
                }

                products.add(new BillVisitInput.BillVisitDepartmentProductInput(
                    vdpId,
                    null, // parentVisitDepartmentId — root resolves automatically
                    est.getQuantity(),
                    coverageType,
                    patientInsuranceId,
                    null, // exemptionType
                    null  // patientSharePercentageOverride
                ));
            }

            departments.add(new BillVisitInput.BillVisitDepartmentInput(
                rootDeptId,
                products,
                null,  // payments — no payment at quick-bill time
                null,  // note
                null,  // outstandingType
                null   // outstandingReason
            ));
        }

        return new BillVisitInput(visit.getId(), departments);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ApiResponse applyVisitProductCorrections(EditBillVisitInput input, AuthenticatedUser authUser) {
        return billingCorrectionService.applyVisitProductCorrections(input, authUser);
    }

    private BillVisitInput convertEditInputToBillVisitInput(EditBillVisitInput input) {
        return billingCorrectionService.convertEditInputToBillVisitInput(input);
    }

    private List<VisitDepartmentProduct> loadVisitDepartmentProducts(
        UUID visitId
    ) {
        return billingQueryService.loadVisitDepartmentProducts(visitId);
    }

    private boolean requiresBilling(VisitDepartmentProduct item) {
        // PENDING, UNPAID and CORRECTION_PENDING all require billing.
        return (
            item.getStatus() != VisitProductStatus.BILLED &&
            item.getStatus() != VisitProductStatus.EXEMPTED &&
            item.getStatus() != VisitProductStatus.PATIENT_SHARE_EXEMPTED
        );
    }

    private ExemptionType resolveExemptionTypeFromStatus(VisitProductStatus status) {
        if (status == VisitProductStatus.EXEMPTED) {
            return ExemptionType.FULL;
        }
        if (status == VisitProductStatus.PATIENT_SHARE_EXEMPTED) {
            return ExemptionType.PATIENT_SHARE;
        }
        return ExemptionType.NONE;
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
        ExemptionType exemptionType
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
        Map<UUID, ExemptionType> requestedExemptionTypeByItem,
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
            Map<UUID, ExemptionType> requestedExemptionTypeByItem,
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
                requestedExemptionTypeByItem,
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

    /**
     * Null-safe navigation from an insurance billing row to its owning visit.
     */
    private Visit resolveVisitFromBilling(DepartmentInsuranceBilling billing) {
        if (billing == null || billing.getVisitDepartmentBilling() == null
                || billing.getVisitDepartmentBilling().getVisitBilling() == null) {
            return null;
        }
        return billing.getVisitDepartmentBilling().getVisitBilling().getVisit();
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

    /**
     * Update the billing date shown on an invoice. Only ADMIN and MANAGER roles
     * are permitted. Changing the date invalidates the current invoice PDF so a
     * fresh one is generated on next access.
     */
    @Transactional
    public ApiResponse updateBillingDate(
        com.nexxserve.nexxclinic.graphql.input.UpdateBillingDateInput input,
        AuthenticatedUser authUser
    ) {
        if (input == null || input.departmentInsuranceBillingId() == null) {
            return ApiResponse.error("departmentInsuranceBillingId is required.");
        }
        if (input.billingDate() == null) {
            return ApiResponse.error("billingDate is required.");
        }

        Worker actingUser = resolveWorker(authUser);
        if (actingUser == null) {
            return ApiResponse.error("Authentication is required.");
        }

        // Only ADMIN and MANAGER may change the billing date.
        boolean authorised = actingUser.getRoles().contains(com.nexxserve.nexxclinic.model.RoleName.ADMIN)
            || actingUser.getRoles().contains(com.nexxserve.nexxclinic.model.RoleName.MANAGER);
        if (!authorised) {
            return ApiResponse.error("Only admin or manager can update the billing date.");
        }

        Optional<DepartmentInsuranceBilling> billingOpt =
            departmentInsuranceBillingRepository.findByIdWithDepartmentBillingAndVisit(
                input.departmentInsuranceBillingId()
            );
        if (billingOpt.isEmpty()) {
            return ApiResponse.error("Department insurance billing not found.");
        }

        DepartmentInsuranceBilling billing = billingOpt.get();

        // Validate: visit must not be in BILL_EDITING or CANCELLED state.
        Visit visit = billing.getVisitDepartmentBilling().getVisitBilling().getVisit();
        if (visit == null) {
            return ApiResponse.error("Visit not found for this billing.");
        }
        if (visit.getStatus() == com.nexxserve.nexxclinic.model.VisitStatus.BILL_EDITING) {
            return ApiResponse.error("Cannot update billing date while the visit is in billing edit mode.");
        }
        if (visit.getStatus() == com.nexxserve.nexxclinic.model.VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot update billing date for a cancelled visit.");
        }

        // Validate: billing date must be at least 5 minutes after visit date
        if (visit.getVisitDate() != null && input.billingDate() != null) {
            long visitTimeMs = visit.getVisitDate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long billingTimeMs = input.billingDate().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long fiveMinutesMs = 5 * 60 * 1000;
            if (billingTimeMs < visitTimeMs + fiveMinutesMs) {
                return ApiResponse.error("Billing date must be at least 5 minutes after the visit date.");
            }
        }

        billing.setBillingDate(input.billingDate());
        // Invalidate existing invoice so a fresh PDF is generated.
        billing.setInvoiceUrl(null);
        departmentInsuranceBillingRepository.save(billing);

        // Regenerate the invoice immediately after the transaction commits.
        // InvoiceGenerator uses its own TransactionTemplate internally, so it must
        // NOT run inside this @Transactional method (the expensive PDF render and
        // upload should not hold the DB connection open).
        UUID billingId = billing.getId();
        org.springframework.transaction.support.TransactionSynchronizationManager
            .registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            ApiResponse<?> result = invoiceGenerator.generateInvoice(billingId, authUser);
                            if (result.status() != com.nexxserve.nexxclinic.model.ResponseStatus.SUCCESS) {
                                log.warn(
                                    "Invoice regeneration after billing date update for {} returned: {}",
                                    billingId, result.message()
                                );
                            }
                        } catch (Exception e) {
                            log.error(
                                "Failed to regenerate invoice after billing date update for {}: {}",
                                billingId, e.getMessage(), e
                            );
                        }
                    }
                }
            );

        return ApiResponse.success(
            "Billing date updated successfully.",
            Map.of(
                "id", billing.getId(),
                "billingDate", billing.getBillingDate()
            )
        );
    }
}

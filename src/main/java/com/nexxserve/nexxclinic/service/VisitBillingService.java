package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
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
import com.nexxserve.nexxclinic.model.NoteType;
import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.ClinicProfileRepository;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class VisitBillingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(
        2,
        RoundingMode.HALF_UP
    );

    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final ProductInsuranceCoverageRepository productInsuranceCoverageRepository;
    private final ProductRepository productRepository;
    private final VisitBillingRepository visitBillingRepository;
    private final VisitDepartmentBillingRepository visitDepartmentBillingRepository;
    private final DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;
    private final VisitBillingItemRepository visitBillingItemRepository;
    private final VisitBillingVersionRepository visitBillingVersionRepository;
    private final VisitDepartmentProductSnapshotRepository visitDepartmentProductSnapshotRepository;
    private final WorkerRepository workerRepository;
    private final ClinicProfileRepository clinicProfileRepository;
    private final VisitDepartmentNoteRepository visitDepartmentNoteRepository;
    private final SupabaseStorageService supabaseStorageService;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate readOnlyTransactionTemplate;

    private static final Logger log = LoggerFactory.getLogger(VisitBillingService.class);

    public VisitBillingService(
        VisitRepository visitRepository,
        VisitDepartmentRepository visitDepartmentRepository,
        VisitDepartmentProductRepository visitDepartmentProductRepository,
        VisitInsuranceRepository visitInsuranceRepository,
        PatientInsuranceRepository patientInsuranceRepository,
        ProductInsuranceCoverageRepository productInsuranceCoverageRepository,
        ProductRepository productRepository,
        VisitBillingRepository visitBillingRepository,
        VisitDepartmentBillingRepository visitDepartmentBillingRepository,
        DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository,
        VisitBillingItemRepository visitBillingItemRepository,
        VisitBillingVersionRepository visitBillingVersionRepository,
        VisitDepartmentProductSnapshotRepository visitDepartmentProductSnapshotRepository,
        WorkerRepository workerRepository,
        ClinicProfileRepository clinicProfileRepository,
        VisitDepartmentNoteRepository visitDepartmentNoteRepository,
        SupabaseStorageService supabaseStorageService,
        PlatformTransactionManager transactionManager
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentProductRepository =
            visitDepartmentProductRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.productInsuranceCoverageRepository =
            productInsuranceCoverageRepository;
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
        this.clinicProfileRepository = clinicProfileRepository;
        this.visitDepartmentNoteRepository = visitDepartmentNoteRepository;
        this.supabaseStorageService = supabaseStorageService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTransactionTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTransactionTemplate.setReadOnly(true);
    }

    @Transactional
    public ApiResponse billVisit(
        BillVisitInput input,
        AuthenticatedUser authUser
    ) {
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
        if (input == null || input.visitId() == null) {
            return ApiResponse.error("visitId is required.");
        }

        Worker actingUser = resolveWorker(authUser);
        // F3/F4 fix: fail closed. A null acting user would silently bypass the unread-notes
        // gate (countUnreadNotesForVisit returns 0 for a null viewer) and stamp products
        // with billedBy = null.
        if (actingUser == null) {
            return ApiResponse.error("Authentication is required to bill a visit.");
        }

        // Block billing when the acting user has unread notes on any department in this visit.
        // (Business rule: cannot bill or edit if there are unread notes.)
        long unreadNotes = countUnreadNotesForVisit(input.visitId(), actingUser);
        if (unreadNotes > 0) {
            return ApiResponse.error(
                isEdit
                    ? "You have unread notes. Please read them before editing billing."
                    : "You have unread notes. Please read them before billing."
            );
        }

        if (input.departments() == null || input.departments().isEmpty()) {
            return ApiResponse.error("At least one department is required.");
        }

        // A1/A2 fix: pessimistic lock serializes concurrent bill/edit per visit so the
        // version counter and product billing cannot race.
        Optional<Visit> visitOptional = visitRepository.findByIdForUpdate(
            input.visitId()
        );
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cancelled visits cannot be billed.");
        }

        if (isEdit && !hasAnyExistingBilling(input.visitId())) {
            return ApiResponse.error("No existing billing found for this visit.");
        }

        // B2 fix: billVisit is strictly first-time-only. A second plain billVisit on an
        // already-billed visit would create a new version containing ONLY the departments
        // in the request — an incomplete container that drops previously billed
        // departments (and their payments) from the authoritative latest view. Corrections
        // must go through editBillVisit, which carries payments and invalidates invoices.
        if (!isEdit && hasAnyExistingBilling(input.visitId())) {
            return ApiResponse.error(
                "This visit has already been billed. Use editBillVisit to correct the billing."
            );
        }

        // Recovery (orphaned BILLED/EXEMPTED): an earlier billVisit could have returned
        // an error AFTER stamping product statuses but BEFORE creating the billing
        // container, and that error committed the product mutations (the rollback guard
        // in billVisit now prevents this, but visits corrupted before the fix still
        // exist). Because the check above verified this visit has NO billing container,
        // any BILLED/EXEMPTED product here is an orphaned status — a real bill always
        // pairs the status with a container. Reset them to PENDING so the visit can
        // actually be billed. (editBillVisit is untouched: it requires an existing
        // container and handles genuine corrections.)
        if (!isEdit) {
            List<VisitDepartmentProduct> orphanedStatusProducts = loadVisitDepartmentProducts(
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
                visitDepartmentProductRepository.saveAll(orphanedStatusProducts);
            }
        }

        // N2 fix: on edit, carry the previous billing version's payments forward so a
        // correction never resets already-recorded payments to zero. The client may
        // still supply payments explicitly — carried amounts are only used when the
        // input has none for a department.
        Map<UUID, List<BillVisitInput.BillingPaymentInput>> carriedPaymentsByDepartment = new HashMap<>();
        Map<UUID, BigDecimal> carriedPaidByDepartment = new HashMap<>();
        boolean previousVersionFullyPaid = false;
        VisitBilling previousBilling = null;
        if (isEdit) {
            // F2 fix: the "latest" billing is the one with the highest version number, not
            // necessarily the most recent createdAt (clock skew / backfill). Order by the
            // version so carry-forward always reads the true latest container.
            List<VisitBilling> existingBillings = orderByVersionDesc(
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
                return ApiResponse.error(
                    "Each department entry requires a visitDepartmentId."
                );
            }

            VisitDepartment rootVisitDepartment = visitDepartmentsById.get(
                departmentInput.visitDepartmentId()
            );
            if (rootVisitDepartment == null) {
                return ApiResponse.error("Visit department not found.");
            }

            if (!rootVisitDepartment.getVisit().getId().equals(visit.getId())) {
                return ApiResponse.error(
                    "Visit department does not belong to the visit."
                );
            }

            if (!isTopLevelDepartment(rootVisitDepartment)) {
                return ApiResponse.error(
                    "visitDepartmentId must reference a top-level department."
                );
            }

            if (rootDepartments.containsKey(rootVisitDepartment.getId())) {
                return ApiResponse.error(
                    "Duplicate visitDepartmentId provided."
                );
            }

            rootDepartments.put(
                rootVisitDepartment.getId(),
                rootVisitDepartment
            );
            // N2: on edit, carry the previous version's payments for this department
            // unless the client explicitly supplies new ones.
            List<BillVisitInput.BillingPaymentInput> paymentsForDepartment = departmentInput.payments();
            if (isEdit && (paymentsForDepartment == null || paymentsForDepartment.isEmpty())) {
                paymentsForDepartment = carriedPaymentsByDepartment.get(rootVisitDepartment.getId());
            }
            rootPaymentsByDepartment.put(
                rootVisitDepartment.getId(),
                paymentsForDepartment
            );

            BigDecimal totalPaid = ZERO;
            if (paymentsForDepartment != null) {
                for (BillVisitInput.BillingPaymentInput payment : paymentsForDepartment) {
                    if (
                        payment == null ||
                        payment.amount() == null ||
                        payment.paymentMethod() == null
                    ) {
                        return ApiResponse.error(
                            "Each payment requires amount and paymentMethod."
                        );
                    }
                    if (payment.amount().compareTo(ZERO) <= 0) {
                        return ApiResponse.error(
                            "Payment amount must be greater than 0."
                        );
                    }
                    totalPaid = toMoney(totalPaid.add(payment.amount()));
                }
            }

            if (totalPaid.compareTo(ZERO) > 0) {
                remainingPaidByDepartment.put(
                    rootVisitDepartment.getId(),
                    totalPaid
                );
            } else if (
                isEdit &&
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
                return ApiResponse.error(
                    "editBillVisit must include every department that has products. Missing: " +
                    missing + ". Submit the complete corrected bill for the whole visit."
                );
            }
        }

        // Collect notes keyed by visitDepartmentId for later validation and persistence
        Map<UUID, String> noteByDepartmentId = new HashMap<>();
        for (BillVisitInput.BillVisitDepartmentInput deptInput : input.departments()) {
            if (hasText(deptInput.note())) {
                noteByDepartmentId.put(
                    deptInput.visitDepartmentId(),
                    deptInput.note().trim()
                );
            }
        }

        List<VisitDepartmentProduct> allProducts = loadVisitDepartmentProducts(
            visit.getId()
        );
        Map<UUID, VisitDepartmentProduct> allProductsById = allProducts
            .stream()
            .collect(Collectors.toMap(VisitDepartmentProduct::getId, p -> p, (a, b) -> a));

        Map<UUID, UUID> requestedInsuranceByItem = new LinkedHashMap<>();
        Map<UUID, java.math.BigDecimal> requestedUnitPriceByItem =
            new LinkedHashMap<>();
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

        // actingUser resolved earlier
        Map<UUID, PatientInsurance> appliedInsuranceByItem = new HashMap<>();
        Map<BillingGroup, List<VisitDepartmentProduct>> grouping =
            new LinkedHashMap<>();

        for (BillVisitInput.BillVisitDepartmentInput departmentInput : input.departments()) {
            if (
                departmentInput.products() == null ||
                departmentInput.products().isEmpty()
            ) {
                return ApiResponse.error(
                    "Each department must contain at least one product to bill."
                );
            }

            UUID rootVisitDepartmentId = departmentInput.visitDepartmentId();
            for (BillVisitInput.BillVisitDepartmentProductInput productInput : departmentInput.products()) {
                if (
                    productInput == null ||
                    productInput.visitDepartmentProductId() == null
                ) {
                    return ApiResponse.error(
                        "Each product entry requires visitDepartmentProductId."
                    );
                }

                if (
                    requestedProductIds.contains(
                        productInput.visitDepartmentProductId()
                    )
                ) {
                    return ApiResponse.error(
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
                    return ApiResponse.error(
                        "Invalid billing selection: product id " +
                        productInput.visitDepartmentProductId() +
                        " was not found or is not billable."
                    );
                }
                if (!isEdit && !requiresBilling(item)) {
                    // S7 fix: name the offending product so the frontend can debug.
                    return ApiResponse.error(
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
                    return ApiResponse.error(
                        "Selected product does not belong to the provided parent visit department."
                    );
                }

                if (
                    !isProductUnderRootDepartment(item, rootVisitDepartmentId)
                ) {
                    return ApiResponse.error(
                        "Selected product is not under the requested visit department."
                    );
                }

                if (
                    productInput.quantity() != null &&
                    productInput.quantity().compareTo(ZERO) <= 0
                ) {
                    return ApiResponse.error(
                        "quantity must be greater than 0."
                    );
                }
                if (
                    productInput.unitPrice() != null &&
                    productInput.unitPrice().compareTo(ZERO) < 0
                ) {
                    return ApiResponse.error(
                        "unitPrice must be zero or positive."
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
                if (productInput.unitPrice() != null) {
                    requestedUnitPriceByItem.put(
                        item.getId(),
                        productInput.unitPrice()
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
                PatientInsurance appliedInsurance = resolveAppliedInsurance(
                    item,
                    requestedPatientInsuranceId,
                    visitInsurancePatientInsuranceIds,
                    visitInsurances
                );
                if (
                    requestedPatientInsuranceId != null &&
                    appliedInsurance == null
                ) {
                    return ApiResponse.error(
                        "Selected patientInsuranceId is invalid for the visit or does not cover the product."
                    );
                }

                UUID appliedPatientInsuranceId =
                    appliedInsurance == null ? null : appliedInsurance.getId();
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
            return ApiResponse.error("No products eligible for billing.");
        }

        Set<UUID> exemptedRootDepartmentIds = new LinkedHashSet<>();
        Map<UUID, VisitDepartmentBilling> departmentBillingByRoot =
            new HashMap<>();
        List<VisitDepartmentProduct> productsToSave = new ArrayList<>();

        // Always create a NEW billing container (immutable versions).
        VisitBilling visitBilling = new VisitBilling();
        visitBilling.setVisit(visit);

        // Create a billing version row and attach it to this billing container.
        com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion billingVersion =
            createNextBillingVersion(visit);
        visitBilling.setBillingVersion(billingVersion);

        final VisitBilling visitBillingFinal = visitBilling;
        final com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion billingVersionFinal = billingVersion;

        // B1 fix: payments must be recorded ONCE per root department, not once per
        // insurance billing group. Without this guard, a department with two insurance
        // groups (one product insured, one not) would write its payments twice.
        Set<UUID> paymentRecordedFor = new HashSet<>();

        for (Map.Entry<
            BillingGroup,
            List<VisitDepartmentProduct>
        > entry : grouping.entrySet()) {
            BillingGroup group = entry.getKey();
            VisitDepartment rootVisitDepartment = visitDepartmentRepository
                .findById(group.rootVisitDepartmentId())
                .orElse(null);
            if (rootVisitDepartment == null) {
                return ApiResponse.error(
                    "Root visit department could not be resolved."
                );
            }

            VisitDepartmentBilling departmentBilling =
                departmentBillingByRoot.computeIfAbsent(
                    rootVisitDepartment.getId(),
                    key -> {
                        VisitDepartmentBilling billing =
                            new VisitDepartmentBilling();
                        billing.setVisitBilling(visitBillingFinal);
                        billing.setVisitDepartment(rootVisitDepartment);
                        billing.setStatus(VisitBillingStatus.UNPAID);
                        billing.setTotalAmount(ZERO);
                        billing.setInsuranceCoveredAmount(ZERO);
                        billing.setPatientPayableAmount(ZERO);
                        billing.setPaidAmount(ZERO);
                        billing.setOutstandingAmount(ZERO);
                        visitBillingFinal.getDepartments().add(billing);
                        return billing;
                    }
                );

            DepartmentInsuranceBilling insuranceBilling =
                new DepartmentInsuranceBilling();
            insuranceBilling.setVisitDepartmentBilling(departmentBilling);
            insuranceBilling.setBillingVersion(billingVersionFinal);
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

            if (!paymentRecordedFor.contains(rootVisitDepartment.getId())) {
                List<BillVisitInput.BillingPaymentInput> payments =
                    rootPaymentsByDepartment.get(rootVisitDepartment.getId());
                if (payments != null) {
                    for (BillVisitInput.BillingPaymentInput payment : payments) {
                        VisitBillingPayment billingPayment =
                            new VisitBillingPayment();
                        billingPayment.setVisitDepartmentBilling(departmentBilling);
                        billingPayment.setBillingVersion(billingVersionFinal);
                        billingPayment.setAmount(toMoney(payment.amount()));
                        billingPayment.setPaymentMethod(payment.paymentMethod());
                        billingPayment.setReference(payment.reference());
                        departmentBilling.getPayments().add(billingPayment);
                    }
                }
                paymentRecordedFor.add(rootVisitDepartment.getId());
            }

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
                BigDecimal unitPrice = requestedUnitPriceByItem.containsKey(
                    item.getId()
                )
                    ? toMoney(requestedUnitPriceByItem.get(item.getId()))
                    : toMoney(item.getPrice());
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
                    coveredAmount = calculateCoveredAmount(
                        item,
                        appliedInsurance,
                        quantity,
                        lineTotal
                    );
                    patientAmount = toMoney(lineTotal.subtract(coveredAmount));
                }

                // Snapshot the visit product for this billing version (immutable history)
                com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot snap =
                    new com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot();
                snap.setBillingVersion(billingVersionFinal);
                snap.setVisitDepartmentProductId(item.getId());
                snap.setVisitDepartment(item.getVisitDepartment());
                snap.setProduct(item.getProduct());
                snap.setQuantity(quantity);
                snap.setUnitPrice(unitPrice);
                snap.setStatus(isExempted ? VisitProductStatus.EXEMPTED : VisitProductStatus.BILLED);
                snap.setAppliedPatientInsurance(appliedInsurance);
                snap.setAddedBy(item.getAddedBy());
                snap.setBilledBy(actingUser);
                snap = visitDepartmentProductSnapshotRepository.save(snap);

                VisitBillingItem billingItem = new VisitBillingItem();
                billingItem.setDepartmentInsuranceBilling(insuranceBilling);
                billingItem.setVisitDepartmentProduct(item);
                billingItem.setBillingVersion(billingVersionFinal);
                billingItem.setVisitDepartmentProductSnapshotId(snap.getId());
                billingItem.setAppliedPatientInsurance(appliedInsurance);
                billingItem.setUnitPriceSnapshot(unitPrice);
                billingItem.setQuantitySnapshot(quantity);
                billingItem.setLineTotal(lineTotal);
                billingItem.setInsuranceCoveredAmount(coveredAmount);
                billingItem.setPatientPayableAmount(patientAmount);
                insuranceBilling.getItems().add(billingItem);

                total = toMoney(total.add(lineTotal));
                insuranceCovered = toMoney(insuranceCovered.add(coveredAmount));
                patientPayable = toMoney(patientPayable.add(patientAmount));

                if (isExempted) {
                    item.setStatus(VisitProductStatus.EXEMPTED);
                } else {
                    item.setStatus(VisitProductStatus.BILLED);
                }
                item.setBilledBy(actingUser);
                productsToSave.add(item);

                // On edit, allow changing quantity/price snapshots regardless of current billed status.
                // We do not mutate the underlying VisitDepartmentProduct.price here (billing uses snapshots).
                // Quantity of the underlying product should reflect the edited quantity.
                if (isEdit) {
                    if (requestedQuantityByItem.containsKey(item.getId())) {
                        item.setQuantity(toQuantity(requestedQuantityByItem.get(item.getId())));
                    }
                }
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
                resolveBillingStatus(paidAmount, patientPayable)
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
                resolveBillingStatus(
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
                if (isEdit && carriedPaidByDepartment.containsKey(entry.getKey())) {
                    // N2: the corrected bill is smaller than the amount already paid
                    // (e.g. a paid product was erased). Surface this clearly instead of
                    // silently dropping the patient's money. The correction must be
                    // adjusted (e.g. keep the paid product, or submit a smaller payment
                    // set that matches the corrected bill) because the refund/adjustment
                    // mechanism does not exist in editBillVisit.
                    return ApiResponse.error(
                        "The corrected bill for " + deptName + " (" +
                        entry.getValue().toPlainString() +
                        ") is smaller than the amount already paid. Keep the paid product or " +
                        "adjust the payments before correcting the billing."
                    );
                }
                return ApiResponse.error(
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
            VisitDepartmentBilling deptBilling = noteEntry.getValue();
            boolean requiresNote =
                exemptedRootDepartmentIds.contains(rootDeptId) ||
                deptBilling.getOutstandingAmount().compareTo(ZERO) > 0;
            if (requiresNote && !hasText(noteByDepartmentId.get(rootDeptId))) {
                return ApiResponse.error(
                    "A billing note is required when items are exempted or the patient payment is less than the payable amount."
                );
            }
        }

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
                    billingVersionFinal.getId()
                );
            for (DepartmentInsuranceBilling oldBilling : oldBillings) {
                if (oldBilling.getInvoiceUrl() != null) {
                    oldBilling.setInvoiceUrl(null);
                    departmentInsuranceBillingRepository.save(oldBilling);
                }
            }
        }

        visitDepartmentProductRepository.saveAll(productsToSave);

        // Persist billing notes attached to the relevant visit department
        for (Map.Entry<
            UUID,
            VisitDepartmentBilling
        > noteEntry : departmentBillingByRoot.entrySet()) {
            UUID rootDeptId = noteEntry.getKey();
            String note = noteByDepartmentId.get(rootDeptId);
            if (hasText(note)) {
                VisitDepartment dept = visitDepartmentRepository
                    .findById(rootDeptId)
                    .orElse(null);
                if (dept != null) {
                    VisitDepartmentNote billingNote = new VisitDepartmentNote();
                    billingNote.setVisitDepartment(dept);
                    billingNote.setContent(note);
                    billingNote.setCreatedBy(actingUser);
                    billingNote.setNoteType(NoteType.BILLING);
                    visitDepartmentNoteRepository.save(billingNote);
                }
            }
        }

        boolean fullyBilled = isVisitFullyBilled(visit.getId());
        if (fullyBilled) {
            visit.setStatus(VisitStatus.COMPLETED);
            visitRepository.save(visit);
            // Do not auto-generate invoices for all department insurance billings here.
            // Invoice generation should be explicit (generateInvoice) and on edit we only invalidate/regenerate
            // invoices for changed insurance billings.
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
            visitBillingToMap(savedVisitBilling)
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
        if (visitId != null) {
            long unreadNotes = countUnreadNotesForVisit(visitId, actingUser);
            if (unreadNotes > 0) {
                return ApiResponse.error("You have unread notes. Please read them before recording payments.");
            }
        }
        if (input.amount() == null || input.paymentMethod() == null) {
            return ApiResponse.error(
                "amount and paymentMethod are required."
            );
        }

        if (input.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("amount must be greater than 0.");
        }

        // A3/A4 fix: acquire the per-visit lock BEFORE reading paidAmount so the
        // read-modify-write is serialized against concurrent payments/edits (otherwise two
        // payments could both pass the <= patientPayable check and overpay or lose one).
        if (visitId != null) {
            visitRepository.findByIdForUpdate(visitId);
        }

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
        if (
            nextPaid.compareTo(insuranceBilling.getPatientPayableAmount()) > 0
        ) {
            return ApiResponse.error(
                "Payment amount would exceed the patient payable amount."
            );
        }

        insuranceBilling.setPaidAmount(nextPaid);
        insuranceBilling.setOutstandingAmount(
            toMoney(
                insuranceBilling.getPatientPayableAmount().subtract(nextPaid)
            )
        );
        insuranceBilling.setStatus(
            resolveBillingStatus(
                nextPaid,
                insuranceBilling.getPatientPayableAmount()
            )
        );

        VisitDepartmentBilling departmentBilling =
            insuranceBilling.getVisitDepartmentBilling();
        VisitBillingPayment billingPayment = new VisitBillingPayment();
        billingPayment.setVisitDepartmentBilling(departmentBilling);
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
            resolveBillingStatus(paidAmount, patientPayableAmount)
        );
        visitDepartmentBillingRepository.save(departmentBilling);

        // Persist billing note if provided
        if (hasText(input.note())) {
            VisitDepartment noteDept = departmentBilling.getVisitDepartment();
            Worker paymentWorker = resolveWorker(authUser);
            VisitDepartmentNote billingNote = new VisitDepartmentNote();
            billingNote.setVisitDepartment(noteDept);
            billingNote.setContent(input.note().trim());
            billingNote.setCreatedBy(paymentWorker);
            billingNote.setNoteType(NoteType.BILLING);
            visitDepartmentNoteRepository.save(billingNote);
        }

        return ApiResponse.success(
            "Payment recorded.",
            visitBillingToMap(departmentBilling.getVisitBilling())
        );
    }

    @Transactional(readOnly = true)
    public ApiResponse visitBilling(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        List<VisitBilling> billings = orderByVersionDesc(
            visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId)
        );
        if (billings.isEmpty()) {
            return ApiResponse.error("Visit billing not found.");
        }

        return ApiResponse.success(
            "Visit billing fetched.",
            visitBillingToMap(billings.get(0))
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

        List<Map<String, Object>> billings = orderByVersionDesc(
            visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId)
        ).stream()
            .map(this::visitBillingToMap)
            .toList();

        return ApiResponse.success("Visit billings fetched.", billings);
    }

    /**
     * Generates an invoice PDF for a department insurance billing row.
     *
     * <p>The work is split into three short phases so the slow PDF rendering and the
     * Supabase HTTP upload NEVER hold a DB transaction/connection open:
     * <ol>
     *   <li><b>Snapshot</b> — a short read-only transaction authenticates, validates
     *       and loads every association the renderer touches (via
     *       {@code findByIdWithInvoiceData}) into an {@link InvoiceSnapshot}.</li>
     *   <li><b>Render + upload</b> — runs entirely OUTSIDE any transaction.</li>
     *   <li><b>Persist</b> — a short write transaction stores the object path.</li>
     * </ol>
     * Only {@code IOException} was caught before; a runtime failure inside the renderer
     * or the HTTP client now degrades to a clean error instead of an uncaught 500.
     */
    public ApiResponse generateInvoice(
        UUID departmentInsuranceBillingId,
        AuthenticatedUser authUser
    ) {
        if (departmentInsuranceBillingId == null) {
            return ApiResponse.error(
                "departmentInsuranceBillingId is required."
            );
        }

        // Phase 1 — short read-only tx: authenticate, validate, snapshot the data the
        // PDF needs. All lazy associations are eagerly fetched inside the tx.
        InvoiceSnapshot snapshot = readOnlyTransactionTemplate.execute(
            status -> loadInvoiceSnapshot(departmentInsuranceBillingId, authUser)
        );
        if (snapshot == null) {
            return ApiResponse.error("Unable to load invoice data. Please try again.");
        }
        if (snapshot.error() != null) {
            return snapshot.error();
        }

        DepartmentInsuranceBilling billing = snapshot.billing();

        // Invoice already stored — return a fresh signed URL (pure IO, no DB tx held).
        if (hasText(billing.getInvoiceUrl())) {
            try {
                String signed = supabaseStorageService.signedUrl(
                    billing.getInvoiceUrl(),
                    300
                );
                return ApiResponse.success(
                    "Invoice already exists.",
                    Map.of("signedUrl", signed)
                );
            } catch (IOException e) {
                return ApiResponse.error(
                    "Invoice exists but could not generate download URL."
                );
            }
        }

        // Phase 2 — PDF render + upload, entirely OUTSIDE any DB transaction.
        String objectPath;
        try {
            objectPath = generateInvoicePdfFile(snapshot);
        } catch (Exception e) {
            log.error(
                "Invoice generation failed for billing {}: {}",
                departmentInsuranceBillingId,
                e.getMessage(),
                e
            );
            return ApiResponse.error("Failed to generate or upload invoice.");
        }

        // Phase 3 — short write tx: persist the object path so getInvoice can sign it.
        // If the persist fails (or the billing row vanished mid-flight), the uploaded
        // file would be an orphan with no DB reference — clean it up best-effort.
        boolean persisted;
        try {
            persisted = persistInvoiceUrl(departmentInsuranceBillingId, objectPath);
        } catch (Exception e) {
            log.error(
                "Invoice uploaded for billing {} but object path could not be persisted: {}",
                departmentInsuranceBillingId,
                e.getMessage(),
                e
            );
            cleanupOrphanedInvoice(departmentInsuranceBillingId, objectPath);
            return ApiResponse.error(
                "Invoice was generated but could not be saved. Please try again."
            );
        }
        if (!persisted) {
            // The billing row disappeared between snapshot and persist (concurrent
            // edit/delete). Do not report success for a file nothing references.
            log.warn(
                "Invoice {} for billing {} could not be persisted — billing row not found; cleaning up upload.",
                objectPath,
                departmentInsuranceBillingId
            );
            cleanupOrphanedInvoice(departmentInsuranceBillingId, objectPath);
            return ApiResponse.error(
                "Invoice could not be saved because the billing record is no longer available. Please refresh and try again."
            );
        }

        try {
            String signed = supabaseStorageService.signedUrl(objectPath, 300);
            return ApiResponse.success(
                "Invoice generated successfully.",
                Map.of("signedUrl", signed)
            );
        } catch (IOException e) {
            return ApiResponse.error(
                "Invoice generated but could not create download URL."
            );
        }
    }

    /**
     * Phase-1 helper (runs inside a short read-only transaction): resolves the acting
     * user, runs the unread-notes gate and the latest-version / fully-billed guards,
     * and snapshots everything {@link InvoicePdfGenerator} needs. Returns an
     * {@link InvoiceSnapshot} carrying either a clean error or the loaded data.
     */
    private InvoiceSnapshot loadInvoiceSnapshot(
        UUID departmentInsuranceBillingId,
        AuthenticatedUser authUser
    ) {
        Worker actingUser = resolveWorker(authUser);
        if (actingUser == null) {
            // F3/F4 fix: fail closed (see billOrEditVisitInternal).
            return InvoiceSnapshot.error(
                "Authentication is required to generate an invoice."
            );
        }
        UUID visitId = resolveVisitIdForDepartmentInsuranceBilling(
            departmentInsuranceBillingId
        );
        if (visitId != null) {
            long unreadNotes = countUnreadNotesForVisit(visitId, actingUser);
            if (unreadNotes > 0) {
                return InvoiceSnapshot.error(
                    "You have unread notes. Please read them before generating an invoice."
                );
            }
        }
        Optional<DepartmentInsuranceBilling> billingOptional =
            departmentInsuranceBillingRepository.findByIdWithInvoiceData(
                departmentInsuranceBillingId
            );
        if (billingOptional.isEmpty()) {
            return InvoiceSnapshot.error("Department insurance billing not found.");
        }

        DepartmentInsuranceBilling billing = billingOptional.get();
        Visit visit = billing
            .getVisitDepartmentBilling()
            .getVisitBilling()
            .getVisit();
        if (visit == null) {
            return InvoiceSnapshot.error("Visit not found for billing.");
        }

        // Flow I: invoices are only ever generated for the LATEST billing version.
        // Old versions have their invoiceUrl cleared by editBillVisit (B2 fix); this
        // guard prevents regenerating a fresh PDF from stale data via an old row id.
        Optional<com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion> latestVersionOpt =
            visitBillingVersionRepository.findFirstByVisitIdOrderByVersionDesc(visit.getId());
        UUID latestVersionId = latestVersionOpt
            .map(com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion::getId)
            .orElse(null);
        UUID invoiceVersionId = billing.getBillingVersion() == null
            ? null
            : billing.getBillingVersion().getId();
        boolean isLatestVersion =
            latestVersionId == null
                ? invoiceVersionId == null
                : latestVersionId.equals(invoiceVersionId);
        if (!isLatestVersion) {
            return InvoiceSnapshot.error(
                "Invoices can only be generated for the latest billing version. Use the current billing data."
            );
        }

        if (!isVisitFullyBilled(visit.getId())) {
            return InvoiceSnapshot.error(
                "Invoice can only be generated after all visit products are billed."
            );
        }

        ClinicProfile clinicProfile = clinicProfileRepository
            .findFirstByOrderByCreatedAtAsc()
            .orElse(null);

        List<Map<String, Object>> items = visitBillingItemRepository
            .findByDepartmentInsuranceBillingIdWithProduct(billing.getId())
            .stream()
            .map(this::visitBillingItemToMap)
            .toList();

        return InvoiceSnapshot.ready(billing, clinicProfile, items);
    }

    /**
     * Phase-2 helper (runs OUTSIDE any DB transaction): renders the invoice PDF to a
     * temp file and uploads it to Supabase Storage. Returns the uploaded object path.
     * Persisting the path is the caller's job ({@link #persistInvoiceUrl}).
     */
    private String generateInvoicePdfFile(
        InvoiceSnapshot snapshot
    ) throws IOException {
        DepartmentInsuranceBilling billing = snapshot.billing();
        ClinicProfile clinicProfile = snapshot.clinicProfile();

        // Render PDF to a temp file
        Path tempFile = Files.createTempFile("invoice-", ".pdf");
        try {
            InvoicePdfGenerator.createInvoicePdf(
                tempFile,
                billing,
                snapshot.items(),
                clinicProfile
            );

            // Upload to Supabase Storage  data/{invoices}/{clinicName?}/invoice-{id}.pdf
            byte[] pdfBytes = Files.readAllBytes(tempFile);
            String clinicName = (clinicProfile != null)
                ? clinicProfile.getName()
                : null;
            String objectPath = supabaseStorageService.buildObjectPath(
                clinicName,
                billing.getId().toString()
            );
            supabaseStorageService.upload(pdfBytes, objectPath);
            return objectPath;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Phase-3 helper (runs in its own short write transaction): persists the uploaded
     * object path on the billing row so {@code getInvoice} can sign it later. Kept
     * separate from the render/upload IO so the write never holds a DB connection
     * during the slow PDF/HTTP work. Returns {@code false} when the billing row no
     * longer exists (the caller must not report success).
     */
    private boolean persistInvoiceUrl(
        UUID departmentInsuranceBillingId,
        String objectPath
    ) {
        return transactionTemplate.execute(status -> {
            Optional<DepartmentInsuranceBilling> billingOptional =
                departmentInsuranceBillingRepository.findById(departmentInsuranceBillingId);
            if (billingOptional.isEmpty()) {
                return false;
            }
            DepartmentInsuranceBilling billing = billingOptional.get();
            billing.setInvoiceUrl(objectPath);
            departmentInsuranceBillingRepository.save(billing);
            return true;
        });
    }

    /**
     * Best-effort removal of an uploaded invoice file that no DB row references
     * (persist failed or the billing row vanished mid-flight). Never throws — the
     * orphan is logged and left for manual cleanup if Supabase is unreachable.
     */
    private void cleanupOrphanedInvoice(
        UUID departmentInsuranceBillingId,
        String objectPath
    ) {
        if (objectPath == null || objectPath.isBlank()) {
            return;
        }
        try {
            supabaseStorageService.delete("data", objectPath);
            log.info(
                "Cleaned up orphaned invoice upload {} for billing {}.",
                objectPath,
                departmentInsuranceBillingId
            );
        } catch (Exception e) {
            log.warn(
                "Could not clean up orphaned invoice upload {} for billing {}: {}",
                objectPath,
                departmentInsuranceBillingId,
                e.getMessage()
            );
        }
    }

    /**
     * Immutable snapshot of everything the invoice PDF renderer needs, captured inside
     * a short read-only transaction. All lazy associations on {@code billing} are
     * eagerly fetched, so the entity can be safely rendered outside any transaction.
     */
    private record InvoiceSnapshot(
        ApiResponse<?> error,
        DepartmentInsuranceBilling billing,
        ClinicProfile clinicProfile,
        List<Map<String, Object>> items
    ) {
        static InvoiceSnapshot error(String message) {
            return new InvoiceSnapshot(ApiResponse.error(message), null, null, List.of());
        }

        static InvoiceSnapshot ready(
            DepartmentInsuranceBilling billing,
            ClinicProfile clinicProfile,
            List<Map<String, Object>> items
        ) {
            return new InvoiceSnapshot(null, billing, clinicProfile, items);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private long countUnreadNotesForVisit(UUID visitId, Worker viewer) {
        if (visitId == null || viewer == null || viewer.getId() == null) {
            return 0;
        }
        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);
        long total = 0;
        for (VisitDepartment vd : departments) {
            // F1 fix: notes on CANCELLED departments must not block billing of the
            // departments that are actually being billed.
            if (vd.getStatus() == com.nexxserve.nexxclinic.model.VisitDepartmentStatus.CANCELLED) {
                continue;
            }
            total += visitDepartmentNoteRepository.countNewNotesForViewer(vd.getId(), viewer.getId());
        }
        return total;
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
        long unreadNotes = countUnreadNotesForVisit(input.visitId(), actingUser);
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
                    // E1 fix: default the live-row price to the catalog price instead of 0.
                    // billing falls back to item.getPrice() when billProducts.unitPrice is
                    // omitted, so a 0 here silently billed the new product for free.
                    vdp.setPrice(
                        product.getClinicPrice() == null
                            ? BigDecimal.ZERO
                            : product.getClinicPrice()
                    );
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
                                    bp.unitPrice(),
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

    private UUID resolveVisitIdForDepartmentInsuranceBilling(UUID departmentInsuranceBillingId) {
        if (departmentInsuranceBillingId == null) {
            return null;
        }
        return departmentInsuranceBillingRepository
            .findByIdWithDepartmentBillingAndVisit(departmentInsuranceBillingId)
            .map(b -> {
                Visit v = b.getVisitDepartmentBilling().getVisitBilling().getVisit();
                return v != null ? v.getId() : null;
            })
            .orElse(null);
    }

    private List<VisitBilling> orderByVersionDesc(List<VisitBilling> billings) {
        if (billings == null || billings.isEmpty()) {
            return billings == null ? List.of() : billings;
        }
        return billings.stream()
            .sorted(
                java.util.Comparator.comparingInt(
                    (VisitBilling b) ->
                        b.getBillingVersion() == null
                            ? -1
                            : b.getBillingVersion().getVersion()
                ).reversed()
            )
            .toList();
    }

    private boolean hasAnyExistingBilling(UUID visitId) {
        if (visitId == null) {
            return false;
        }
        return !visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId).isEmpty();
    }

    private com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion createNextBillingVersion(Visit visit) {
        if (visit == null || visit.getId() == null) {
            throw new IllegalArgumentException("visit is required");
        }

        // Retry once on a unique-index collision ((visit_id, version) unique): a
        // concurrent un-locked path could have inserted a version between our read and
        // save. Re-read the max version and retry; if it still collides, rethrow so the
        // transaction fails loudly instead of writing a duplicate.
        for (int attempt = 0; attempt < 2; attempt++) {
            com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion latest = visitBillingVersionRepository
                .findFirstByVisitIdOrderByVersionDesc(visit.getId())
                .orElse(null);

            com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion v =
                new com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion();
            v.setVisit(visit);
            v.setVersion(latest == null ? 1 : (latest.getVersion() + 1));
            v.setSupersedesVersionId(latest == null ? null : latest.getId());
            try {
                // saveAndFlush: the unique index on (visit_id, version) is only checked at
                // flush time — a plain save() defers the INSERT and the violation would
                // surface at commit, outside this catch, making the retry dead code.
                return visitBillingVersionRepository.saveAndFlush(v);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                if (attempt == 1) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Unable to allocate a billing version. Please retry.");
    }

    private List<VisitDepartmentProduct> loadVisitDepartmentProducts(
        UUID visitId
    ) {
        return visitDepartmentRepository
            .findByVisitId(visitId)
            .stream()
            .flatMap(vd ->
                visitDepartmentProductRepository
                    .findByVisitDepartmentId(vd.getId())
                    .stream()
            )
            .toList();
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

    private PatientInsurance resolveAppliedInsurance(
        VisitDepartmentProduct item,
        UUID requestedPatientInsuranceId,
        Set<UUID> visitInsurancePatientInsuranceIds,
        List<VisitInsurance> visitInsurances
    ) {
        if (requestedPatientInsuranceId != null) {
            if (
                !visitInsurancePatientInsuranceIds.contains(
                    requestedPatientInsuranceId
                )
            ) {
                return null;
            }
            Optional<PatientInsurance> insuranceOptional =
                patientInsuranceRepository.findById(
                    requestedPatientInsuranceId
                );
            if (insuranceOptional.isEmpty()) {
                return null;
            }
            PatientInsurance insurance = insuranceOptional.get();
            ProductInsuranceCoverage coverage =
                productInsuranceCoverageRepository
                    .findByProductIdAndInsuranceProviderId(
                        item.getProduct().getId(),
                        insurance.getInsuranceProvider().getId()
                    )
                    .orElse(null);
            if (coverage == null || !coverage.isCovered()) {
                return null;
            }
            return insurance;
        }

        for (VisitInsurance visitInsurance : visitInsurances) {
            PatientInsurance insurance = visitInsurance.getPatientInsurance();
            ProductInsuranceCoverage coverage =
                productInsuranceCoverageRepository
                    .findByProductIdAndInsuranceProviderId(
                        item.getProduct().getId(),
                        insurance.getInsuranceProvider().getId()
                    )
                    .orElse(null);
            if (coverage != null && coverage.isCovered()) {
                return insurance;
            }
        }
        return null;
    }

    private BigDecimal calculateCoveredAmount(
        VisitDepartmentProduct item,
        PatientInsurance appliedInsurance,
        BigDecimal quantity,
        BigDecimal lineTotal
    ) {
        if (appliedInsurance == null) {
            return ZERO;
        }

        Optional<ProductInsuranceCoverage> coverageOptional =
            productInsuranceCoverageRepository.findByProductIdAndInsuranceProviderId(
                item.getProduct().getId(),
                appliedInsurance.getInsuranceProvider().getId()
            );
        if (coverageOptional.isEmpty() || !coverageOptional.get().isCovered()) {
            return ZERO;
        }

        BigDecimal coverageAmount =
            coverageOptional.get().getCost() == null
                ? ZERO
                : toMoney(coverageOptional.get().getCost().multiply(quantity));

        if (coverageAmount.compareTo(lineTotal) > 0) {
            coverageAmount = lineTotal;
        }

        return toMoney(coverageAmount);
    }

    private VisitBillingStatus resolveBillingStatus(
        BigDecimal paidAmount,
        BigDecimal patientPayableAmount
    ) {
        if (patientPayableAmount.compareTo(ZERO) == 0) {
            return VisitBillingStatus.PAID;
        }
        if (paidAmount.compareTo(ZERO) == 0) {
            return VisitBillingStatus.UNPAID;
        }
        if (paidAmount.compareTo(patientPayableAmount) >= 0) {
            return VisitBillingStatus.PAID;
        }
        return VisitBillingStatus.PARTIALLY_PAID;
    }

    private boolean isVisitFullyBilled(UUID visitId) {
        // B6 fix: evaluate ALL rows, including soft-deleted ones, explicitly.
        // A soft-deleted product was removed from the current bill (it either has
        // billing history from a previous version or was removed before ever being
        // billed), so it never blocks completion. Only non-deleted products must be
        // BILLED/EXEMPTED for the visit to be considered fully billed.
        List<VisitDepartmentProduct> items =
            visitDepartmentProductRepository.findByVisitDepartmentVisitIdIncludingDeleted(
                visitId
            );
        if (items.isEmpty()) {
            return false;
        }

        return items
            .stream()
            .allMatch(
                item ->
                    item.isDeleted() ||
                    item.getStatus() == VisitProductStatus.BILLED ||
                    item.getStatus() == VisitProductStatus.EXEMPTED
            );
    }

    private Map<String, Object> visitBillingToMap(VisitBilling billing) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", billing.getId());
        data.put("visitId", billing.getVisit().getId());
        data.put("createdAt", billing.getCreatedAt());
        data.put("updatedAt", billing.getUpdatedAt());
        data.put(
            "departments",
            billing
                .getDepartments()
                .stream()
                .map(this::visitDepartmentBillingToMap)
                .toList()
        );
        return data;
    }

    private Map<String, Object> visitDepartmentBillingToMap(
        VisitDepartmentBilling billing
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", billing.getId());
        data.put(
            "visitDepartment",
            visitDepartmentToMap(billing.getVisitDepartment())
        );
        data.put("status", billing.getStatus());
        data.put("totalAmount", billing.getTotalAmount());
        data.put("insuranceCoveredAmount", billing.getInsuranceCoveredAmount());
        data.put("patientPayableAmount", billing.getPatientPayableAmount());
        data.put("paidAmount", billing.getPaidAmount());
        data.put("outstandingAmount", billing.getOutstandingAmount());
        data.put(
            "payments",
            billing
                .getPayments()
                .stream()
                .map(this::visitBillingPaymentToMap)
                .toList()
        );
        data.put(
            "insuranceBillings",
            billing
                .getInsuranceBillings()
                .stream()
                .map(this::departmentInsuranceBillingToMap)
                .toList()
        );
        data.put("createdAt", billing.getCreatedAt());
        data.put("updatedAt", billing.getUpdatedAt());
        return data;
    }

    private Map<String, Object> departmentInsuranceBillingToMap(
        DepartmentInsuranceBilling billing
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", billing.getId());
        data.put(
            "patientInsurance",
            billing.getPatientInsurance() == null
                ? null
                : patientInsuranceToMap(billing.getPatientInsurance())
        );
        data.put("status", billing.getStatus());
        data.put("totalAmount", billing.getTotalAmount());
        data.put("insuranceCoveredAmount", billing.getInsuranceCoveredAmount());
        data.put("patientPayableAmount", billing.getPatientPayableAmount());
        data.put("paidAmount", billing.getPaidAmount());
        data.put("outstandingAmount", billing.getOutstandingAmount());
        data.put(
            "items",
            billing
                .getItems()
                .stream()
                .map(this::visitBillingItemToMap)
                .toList()
        );
        data.put("createdAt", billing.getCreatedAt());
        data.put("updatedAt", billing.getUpdatedAt());
        return data;
    }

    private Map<String, Object> visitDepartmentToMap(
        VisitDepartment visitDepartment
    ) {
        if (visitDepartment == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", visitDepartment.getId());
        data.put(
            "department",
            departmentToMap(visitDepartment.getDepartment())
        );
        data.put("status", visitDepartment.getStatus());
        data.put("createdAt", visitDepartment.getCreatedAt());
        data.put("updatedAt", visitDepartment.getUpdatedAt());
        return data;
    }

    private Map<String, Object> departmentToMap(Department department) {
        if (department == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", department.getId());
        data.put("name", department.getName());
        return data;
    }

    private Map<String, Object> patientInsuranceToMap(
        PatientInsurance insurance
    ) {
        if (insurance == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", insurance.getId());
        data.put(
            "insuranceProviderId",
            insurance.getInsuranceProvider() == null
                ? null
                : insurance.getInsuranceProvider().getId()
        );
        data.put("insuranceCardNumber", insurance.getInsuranceCardNumber());
        data.put("principalMemberName", insurance.getPrincipalMemberName());
        return data;
    }

    private Map<String, Object> visitBillingItemToMap(VisitBillingItem item) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put(
            "visitDepartmentProductId",
            item.getVisitDepartmentProduct().getId()
        );
        data.put(
            "productId",
            item.getVisitDepartmentProduct().getProduct().getId()
        );
        data.put(
            "productName",
            item.getVisitDepartmentProduct().getProduct().getName()
        );
        data.put("unitPriceSnapshot", item.getUnitPriceSnapshot());
        data.put("quantitySnapshot", item.getQuantitySnapshot());
        data.put("lineTotal", item.getLineTotal());
        data.put("insuranceCoveredAmount", item.getInsuranceCoveredAmount());
        data.put("patientPayableAmount", item.getPatientPayableAmount());
        data.put("createdAt", item.getCreatedAt());
        data.put("updatedAt", item.getUpdatedAt());
        return data;
    }

    private Map<String, Object> visitBillingPaymentToMap(
        VisitBillingPayment payment
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", payment.getId());
        data.put("amount", payment.getAmount());
        data.put("paymentMethod", payment.getPaymentMethod());
        data.put("reference", payment.getReference());
        data.put("createdAt", payment.getCreatedAt());
        data.put("updatedAt", payment.getUpdatedAt());
        return data;
    }

    private Map<String, Object> workerToMap(Worker worker) {
        if (worker == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", worker.getId());
        data.put("firstName", worker.getFirstName());
        data.put("lastName", worker.getLastName());
        data.put("username", worker.getUsername());
        return data;
    }

    private Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }

    private BigDecimal toMoney(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal toQuantity(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    @Transactional
    public ApiResponse flushSoftDeletedVisitProducts(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

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

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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        SupabaseStorageService supabaseStorageService
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
    }

    @Transactional
    public ApiResponse billVisit(
        BillVisitInput input,
        AuthenticatedUser authUser
    ) {
        return billOrEditVisitInternal(input, authUser, false);
    }

    @Transactional
    public ApiResponse editBillVisit(
        EditBillVisitInput input,
        AuthenticatedUser authUser
    ) {
        if (input == null) {
            return ApiResponse.error("input is required.");
        }

        // Error-correction workflow:
        // 1) Synchronize visit department products (add/remove/update)
        // 2) Create a new immutable billing version from the corrected visit state
        ApiResponse sync = applyVisitProductCorrections(input, authUser);
        if (sync != null) {
            return sync;
        }

        try {
            BillVisitInput asBill = convertEditInputToBillVisitInput(input);
            return billOrEditVisitInternal(asBill, authUser, true);
        } catch (IllegalArgumentException e) {
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

        Optional<Visit> visitOptional = visitRepository.findById(
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

        List<VisitDepartment> allVisitDepartments =
            visitDepartmentRepository.findByVisitId(visit.getId());
        Map<UUID, VisitDepartment> visitDepartmentsById = allVisitDepartments
            .stream()
            .collect(Collectors.toMap(VisitDepartment::getId, d -> d));

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
            rootPaymentsByDepartment.put(
                rootVisitDepartment.getId(),
                departmentInput.payments()
            );

            BigDecimal totalPaid = ZERO;
            if (departmentInput.payments() != null) {
                for (BillVisitInput.BillingPaymentInput payment : departmentInput.payments()) {
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
            .collect(Collectors.toMap(VisitDepartmentProduct::getId, p -> p));

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
                    return ApiResponse.error(
                        "Invalid billing selection. Ensure product ids exist and are billable."
                    );
                }
                if (!isEdit && !requiresBilling(item)) {
                    return ApiResponse.error(
                        "Invalid billing selection. Ensure product ids exist and are billable."
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

        return ApiResponse.success(
            "Visit billed successfully.",
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
        // For consistency with billing/editing rules: block payments when user has unread notes.
        // (If you want payments allowed even with unread notes, we can relax this.)
        UUID visitId = resolveVisitIdForDepartmentInsuranceBilling(input.departmentInsuranceBillingId());
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

        Optional<DepartmentInsuranceBilling> billingOptional =
            departmentInsuranceBillingRepository.findByIdWithDepartmentBillingAndVisit(
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

        // Validate note requirement: mandatory when payment leaves an outstanding balance
        BigDecimal candidatePaid = toMoney(
            insuranceBilling.getPaidAmount().add(input.amount())
        );
        if (
            candidatePaid.compareTo(
                insuranceBilling.getPatientPayableAmount()
            ) > 0
        ) {
            candidatePaid = insuranceBilling.getPatientPayableAmount();
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
            insuranceBilling.getPaidAmount().add(input.amount())
        );
        if (
            nextPaid.compareTo(insuranceBilling.getPatientPayableAmount()) > 0
        ) {
            nextPaid = insuranceBilling.getPatientPayableAmount();
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

        List<VisitBilling> billings =
            visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId);
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

        List<Map<String, Object>> billings = visitBillingRepository
            .findByVisitIdOrderByCreatedAtDesc(visitId)
            .stream()
            .map(this::visitBillingToMap)
            .toList();

        return ApiResponse.success("Visit billings fetched.", billings);
    }

    @Transactional
    public ApiResponse generateInvoice(
        UUID departmentInsuranceBillingId,
        AuthenticatedUser authUser
    ) {
        if (departmentInsuranceBillingId == null) {
            return ApiResponse.error(
                "departmentInsuranceBillingId is required."
            );
        }

        Worker actingUser = resolveWorker(authUser);
        UUID visitId = resolveVisitIdForDepartmentInsuranceBilling(departmentInsuranceBillingId);
        if (visitId != null) {
            long unreadNotes = countUnreadNotesForVisit(visitId, actingUser);
            if (unreadNotes > 0) {
                return ApiResponse.error("You have unread notes. Please read them before generating an invoice.");
            }
        }
        Optional<DepartmentInsuranceBilling> billingOptional =
            departmentInsuranceBillingRepository.findByIdWithDepartmentBillingAndVisit(
                departmentInsuranceBillingId
            );
        if (billingOptional.isEmpty()) {
            return ApiResponse.error("Department insurance billing not found.");
        }

        DepartmentInsuranceBilling billing = billingOptional.get();
        Visit visit = billing
            .getVisitDepartmentBilling()
            .getVisitBilling()
            .getVisit();
        if (visit == null) {
            return ApiResponse.error("Visit not found for billing.");
        }

        if (!isVisitFullyBilled(visit.getId())) {
            return ApiResponse.error(
                "Invoice can only be generated after all visit products are billed."
            );
        }

        if (hasText(billing.getInvoiceUrl())) {
            // Invoice already stored — return a fresh signed URL
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

        try {
            ClinicProfile clinicProfile = clinicProfileRepository
                .findFirstByOrderByCreatedAtAsc()
                .orElse(null);
            String objectPath = generateInvoicePdfFile(billing, clinicProfile);
            String signed = supabaseStorageService.signedUrl(objectPath, 300);
            return ApiResponse.success(
                "Invoice generated successfully.",
                Map.of("signedUrl", signed)
            );
        } catch (IOException e) {
            return ApiResponse.error("Failed to generate or upload invoice.");
        }
    }

    private void generateInvoicesWhenVisitFullyBilled(UUID visitId) {
        ClinicProfile clinicProfile = clinicProfileRepository
            .findFirstByOrderByCreatedAtAsc()
            .orElse(null);
        for (DepartmentInsuranceBilling billing : departmentInsuranceBillingRepository.findAllByVisitIdWithDetails(
            visitId
        )) {
            if (hasText(billing.getInvoiceUrl())) {
                continue;
            }
            try {
                generateInvoicePdfFile(billing, clinicProfile);
            } catch (IOException ignored) {
                // Billing should remain successful even if PDF generation fails.
            }
        }
    }

    /**
     * Generates the invoice PDF, uploads it to Supabase Storage, persists the
     * object path in {@code billing.invoiceUrl}, and returns that path.
     * The caller is responsible for turning the path into a signed URL.
     */
    private String generateInvoicePdfFile(
        DepartmentInsuranceBilling billing,
        ClinicProfile clinicProfile
    ) throws IOException {
        // Render PDF to a temp file
        Path tempFile = Files.createTempFile("invoice-", ".pdf");
        try {
            List<Map<String, Object>> items = visitBillingItemRepository
                .findByDepartmentInsuranceBillingIdWithProduct(billing.getId())
                .stream()
                .map(this::visitBillingItemToMap)
                .toList();

            InvoicePdfGenerator.createInvoicePdf(
                tempFile,
                billing,
                items,
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

            // Persist the object path (not a URL) so getInvoice can sign it later
            billing.setInvoiceUrl(objectPath);
            departmentInsuranceBillingRepository.save(billing);
            return objectPath;
        } finally {
            Files.deleteIfExists(tempFile);
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
            total += visitDepartmentNoteRepository.countNewNotesForViewer(vd.getId(), viewer.getId());
        }
        return total;
    }

    private ApiResponse applyVisitProductCorrections(EditBillVisitInput input, AuthenticatedUser authUser) {
        if (input.visitId() == null) {
            return ApiResponse.error("visitId is required.");
        }

        Worker actingUser = resolveWorker(authUser);
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
                    if (!vdp.isDeleted()) {
                        vdp.setDeleted(true);
                        visitDepartmentProductRepository.save(vdp);
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
                    // On correction, mark as UNPAID so billing can re-evaluate.
                    vdp.setStatus(VisitProductStatus.UNPAID);
                    vdp.setBilledBy(null);
                    visitDepartmentProductRepository.save(vdp);
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
                        existing.setQuantity(toQuantity(add.quantity()));
                        existing.setStatus(VisitProductStatus.UNPAID);
                        existing.setBilledBy(null);
                        existing.setDeleted(false);
                        visitDepartmentProductRepository.save(existing);
                        continue;
                    }

                    VisitDepartmentProduct vdp = new VisitDepartmentProduct();
                    vdp.setVisitDepartment(vd);
                    vdp.setProduct(product);
                    vdp.setQuantity(toQuantity(add.quantity()));
                    // keep price as 0; billing uses unitPriceSnapshot from billProducts
                    vdp.setPrice(BigDecimal.ZERO);
                    vdp.setAddedBy(actingUser);
                    vdp.setStatus(VisitProductStatus.UNPAID);

                    visitDepartmentProductRepository.save(vdp);
                }
            }
        }

        return null; // success
    }

    private BillVisitInput convertEditInputToBillVisitInput(EditBillVisitInput input) {
        List<BillVisitInput.BillVisitDepartmentInput> departments =
            input.departments() == null
                ? List.of()
                : input.departments().stream().map(d -> {
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

        com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion latest = visitBillingVersionRepository
            .findFirstByVisitIdOrderByVersionDesc(visit.getId())
            .orElse(null);

        com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion v =
            new com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion();
        v.setVisit(visit);
        v.setVersion(latest == null ? 1 : (latest.getVersion() + 1));
        v.setSupersedesVersionId(latest == null ? null : latest.getId());
        return visitBillingVersionRepository.save(v);
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
        return (
            item.getStatus() != VisitProductStatus.BILLED &&
            item.getStatus() != VisitProductStatus.EXEMPTED
        );
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
        List<VisitDepartmentProduct> items = loadVisitDepartmentProducts(
            visitId
        );
        if (items.isEmpty()) {
            return false;
        }

        return items
            .stream()
            .allMatch(
                item ->
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
        for (VisitDepartmentProduct vdp : softDeleted) {
            List<VisitBillingItem> billingItems = visitBillingItemRepository
                .findByVisitDepartmentProductId(vdp.getId());
            if (!billingItems.isEmpty()) {
                visitBillingItemRepository.deleteAll(billingItems);
            }
            visitDepartmentProductRepository.delete(vdp);
            deletedCount++;
        }

        return ApiResponse.success(
            "Soft-deleted products flushed successfully.",
            Map.of("deletedCount", deletedCount)
        );
    }
}

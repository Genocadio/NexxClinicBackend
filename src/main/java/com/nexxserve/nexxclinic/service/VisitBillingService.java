package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitBillingItem;
import com.nexxserve.nexxclinic.entity.VisitBillingPayment;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.graphql.input.RecordVisitBillingPaymentInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingItemRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitInsuranceRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
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

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String INVOICE_DIR = "invoices";
    private static final String INVOICE_URL_PATH = "/invoices/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final ProductInsuranceCoverageRepository productInsuranceCoverageRepository;
    private final VisitBillingRepository visitBillingRepository;
    private final VisitDepartmentBillingRepository visitDepartmentBillingRepository;
    private final DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;
    private final VisitBillingItemRepository visitBillingItemRepository;
    private final WorkerRepository workerRepository;

    public VisitBillingService(
            VisitRepository visitRepository,
            VisitDepartmentRepository visitDepartmentRepository,
            VisitDepartmentProductRepository visitDepartmentProductRepository,
            VisitInsuranceRepository visitInsuranceRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            ProductInsuranceCoverageRepository productInsuranceCoverageRepository,
            VisitBillingRepository visitBillingRepository,
            VisitDepartmentBillingRepository visitDepartmentBillingRepository,
            DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository,
            VisitBillingItemRepository visitBillingItemRepository,
            WorkerRepository workerRepository
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.productInsuranceCoverageRepository = productInsuranceCoverageRepository;
        this.visitBillingRepository = visitBillingRepository;
        this.visitDepartmentBillingRepository = visitDepartmentBillingRepository;
        this.departmentInsuranceBillingRepository = departmentInsuranceBillingRepository;
        this.visitBillingItemRepository = visitBillingItemRepository;
        this.workerRepository = workerRepository;
    }

    @Transactional
    public ApiResponse billVisit(BillVisitInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitId() == null) {
            return ApiResponse.error("visitId is required.", "VALIDATION_ERROR");
        }

        if (input.departments() == null || input.departments().isEmpty()) {
            return ApiResponse.error("At least one department is required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(input.visitId());
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cancelled visits cannot be billed.", "INVALID_VISIT_STATUS_FOR_BILLING");
        }

        List<VisitDepartment> allVisitDepartments = visitDepartmentRepository.findByVisitId(visit.getId());
        Map<UUID, VisitDepartment> visitDepartmentsById = allVisitDepartments.stream()
                .collect(Collectors.toMap(VisitDepartment::getId, d -> d));

        Map<UUID, VisitDepartment> rootDepartments = new LinkedHashMap<>();
        Map<UUID, List<BillVisitInput.BillingPaymentInput>> rootPaymentsByDepartment = new HashMap<>();
        Map<UUID, BigDecimal> remainingPaidByDepartment = new HashMap<>();

        for (BillVisitInput.BillVisitDepartmentInput departmentInput : input.departments()) {
            if (departmentInput == null || departmentInput.visitDepartmentId() == null) {
                return ApiResponse.error("Each department entry requires a visitDepartmentId.", "VALIDATION_ERROR");
            }

            VisitDepartment rootVisitDepartment = visitDepartmentsById.get(departmentInput.visitDepartmentId());
            if (rootVisitDepartment == null) {
                return ApiResponse.error("Visit department not found.", "INVALID_BILLING_SELECTION");
            }

            if (!rootVisitDepartment.getVisit().getId().equals(visit.getId())) {
                return ApiResponse.error("Visit department does not belong to the visit.", "INVALID_BILLING_SELECTION");
            }

            if (!isTopLevelDepartment(rootVisitDepartment)) {
                return ApiResponse.error("visitDepartmentId must reference a top-level department.", "INVALID_BILLING_SELECTION");
            }

            if (rootDepartments.containsKey(rootVisitDepartment.getId())) {
                return ApiResponse.error("Duplicate visitDepartmentId provided.", "VALIDATION_ERROR");
            }

            rootDepartments.put(rootVisitDepartment.getId(), rootVisitDepartment);
            rootPaymentsByDepartment.put(rootVisitDepartment.getId(), departmentInput.payments());

            BigDecimal totalPaid = ZERO;
            if (departmentInput.payments() != null) {
                for (BillVisitInput.BillingPaymentInput payment : departmentInput.payments()) {
                    if (payment == null || payment.amount() == null || payment.paymentMethod() == null) {
                        return ApiResponse.error("Each payment requires amount and paymentMethod.", "VALIDATION_ERROR");
                    }
                    if (payment.amount().compareTo(ZERO) <= 0) {
                        return ApiResponse.error("Payment amount must be greater than 0.", "VALIDATION_ERROR");
                    }
                    totalPaid = toMoney(totalPaid.add(payment.amount()));
                }
            }

            if (totalPaid.compareTo(ZERO) > 0) {
                remainingPaidByDepartment.put(rootVisitDepartment.getId(), totalPaid);
            }
        }

        List<VisitDepartmentProduct> allProducts = loadVisitDepartmentProducts(visit.getId());
        Map<UUID, VisitDepartmentProduct> allProductsById = allProducts.stream()
                .collect(Collectors.toMap(VisitDepartmentProduct::getId, p -> p));

        Map<UUID, UUID> requestedInsuranceByItem = new LinkedHashMap<>();
        Map<UUID, java.math.BigDecimal> requestedUnitPriceByItem = new LinkedHashMap<>();
        Map<UUID, java.math.BigDecimal> requestedQuantityByItem = new LinkedHashMap<>();
        Map<UUID, Boolean> requestedExemptedByItem = new LinkedHashMap<>();
        Set<UUID> requestedProductIds = new LinkedHashSet<>();

        List<VisitInsurance> visitInsurances = visitInsuranceRepository.findByVisitId(visit.getId());
        Set<UUID> visitInsurancePatientInsuranceIds = visitInsurances.stream()
                .map(v -> v.getPatientInsurance().getId())
                .collect(Collectors.toSet());

        Worker actingUser = resolveWorker(authUser);
        Map<UUID, PatientInsurance> appliedInsuranceByItem = new HashMap<>();
        Map<BillingGroup, List<VisitDepartmentProduct>> grouping = new LinkedHashMap<>();

        for (BillVisitInput.BillVisitDepartmentInput departmentInput : input.departments()) {
            if (departmentInput.products() == null || departmentInput.products().isEmpty()) {
                return ApiResponse.error("Each department must contain at least one product to bill.", "VALIDATION_ERROR");
            }

            UUID rootVisitDepartmentId = departmentInput.visitDepartmentId();
            for (BillVisitInput.BillVisitDepartmentProductInput productInput : departmentInput.products()) {
                if (productInput == null || productInput.visitDepartmentProductId() == null) {
                    return ApiResponse.error("Each product entry requires visitDepartmentProductId.", "VALIDATION_ERROR");
                }

                if (requestedProductIds.contains(productInput.visitDepartmentProductId())) {
                    return ApiResponse.error("Duplicate visitDepartmentProductId provided in request.", "VALIDATION_ERROR");
                }
                requestedProductIds.add(productInput.visitDepartmentProductId());

                VisitDepartmentProduct item = allProductsById.get(productInput.visitDepartmentProductId());
                if (item == null || !requiresBilling(item)) {
                    return ApiResponse.error("Invalid billing selection. Ensure product ids exist and are billable.", "INVALID_BILLING_SELECTION");
                }

                if (productInput.parentVisitDepartmentId() != null
                        && !item.getVisitDepartment().getId().equals(productInput.parentVisitDepartmentId())) {
                    return ApiResponse.error("Selected product does not belong to the provided parent visit department.", "INVALID_BILLING_SELECTION");
                }

                if (!isProductUnderRootDepartment(item, rootVisitDepartmentId)) {
                    return ApiResponse.error("Selected product is not under the requested visit department.", "INVALID_BILLING_SELECTION");
                }

                if (productInput.quantity() != null && productInput.quantity().compareTo(ZERO) <= 0) {
                    return ApiResponse.error("quantity must be greater than 0.", "VALIDATION_ERROR");
                }
                if (productInput.unitPrice() != null && productInput.unitPrice().compareTo(ZERO) < 0) {
                    return ApiResponse.error("unitPrice must be zero or positive.", "VALIDATION_ERROR");
                }

                if (productInput.patientInsuranceId() != null) {
                    requestedInsuranceByItem.put(item.getId(), productInput.patientInsuranceId());
                }
                if (productInput.quantity() != null) {
                    requestedQuantityByItem.put(item.getId(), productInput.quantity());
                }
                if (productInput.unitPrice() != null) {
                    requestedUnitPriceByItem.put(item.getId(), productInput.unitPrice());
                }
                if (productInput.isExempted() != null) {
                    requestedExemptedByItem.put(item.getId(), productInput.isExempted());
                }

                UUID requestedPatientInsuranceId = requestedInsuranceByItem.get(item.getId());
                PatientInsurance appliedInsurance = resolveAppliedInsurance(item, requestedPatientInsuranceId, visitInsurancePatientInsuranceIds, visitInsurances);
                if (requestedPatientInsuranceId != null && appliedInsurance == null) {
                    return ApiResponse.error("Selected patientInsuranceId is invalid for the visit or does not cover the product.", "INVALID_VISIT_INSURANCE_SELECTION");
                }

                UUID appliedPatientInsuranceId = appliedInsurance == null ? null : appliedInsurance.getId();
                BillingGroup group = new BillingGroup(rootVisitDepartmentId, appliedPatientInsuranceId);
                grouping.computeIfAbsent(group, key -> new ArrayList<>()).add(item);
                appliedInsuranceByItem.put(item.getId(), appliedInsurance);
            }
        }

        if (grouping.isEmpty()) {
            return ApiResponse.error("No products eligible for billing.", "NOTHING_TO_BILL");
        }

        Map<UUID, VisitDepartmentBilling> departmentBillingByRoot = new HashMap<>();
        List<VisitDepartmentProduct> productsToSave = new ArrayList<>();

        VisitBilling visitBilling = new VisitBilling();
        visitBilling.setVisit(visit);

        for (Map.Entry<BillingGroup, List<VisitDepartmentProduct>> entry : grouping.entrySet()) {
            BillingGroup group = entry.getKey();
            VisitDepartment rootVisitDepartment = visitDepartmentRepository.findById(group.rootVisitDepartmentId()).orElse(null);
            if (rootVisitDepartment == null) {
                return ApiResponse.error("Root visit department could not be resolved.", "INVALID_BILLING_SELECTION");
            }

            VisitDepartmentBilling departmentBilling = departmentBillingByRoot.computeIfAbsent(rootVisitDepartment.getId(), key -> {
                VisitDepartmentBilling billing = new VisitDepartmentBilling();
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
            });

            DepartmentInsuranceBilling insuranceBilling = new DepartmentInsuranceBilling();
            insuranceBilling.setVisitDepartmentBilling(departmentBilling);
            insuranceBilling.setPatientInsurance(group.patientInsuranceId() == null ? null : patientInsuranceRepository.findById(group.patientInsuranceId()).orElse(null));
            insuranceBilling.setStatus(VisitBillingStatus.UNPAID);
            insuranceBilling.setTotalAmount(ZERO);
            insuranceBilling.setInsuranceCoveredAmount(ZERO);
            insuranceBilling.setPatientPayableAmount(ZERO);
            insuranceBilling.setPaidAmount(ZERO);
            insuranceBilling.setOutstandingAmount(ZERO);
            departmentBilling.getInsuranceBillings().add(insuranceBilling);

            List<BillVisitInput.BillingPaymentInput> payments = rootPaymentsByDepartment.get(rootVisitDepartment.getId());
            if (payments != null) {
                for (BillVisitInput.BillingPaymentInput payment : payments) {
                    VisitBillingPayment billingPayment = new VisitBillingPayment();
                    billingPayment.setVisitDepartmentBilling(departmentBilling);
                    billingPayment.setAmount(toMoney(payment.amount()));
                    billingPayment.setPaymentMethod(payment.paymentMethod());
                    billingPayment.setReference(payment.reference());
                    departmentBilling.getPayments().add(billingPayment);
                }
            }

            BigDecimal total = ZERO;
            BigDecimal insuranceCovered = ZERO;
            BigDecimal patientPayable = ZERO;

            for (VisitDepartmentProduct item : entry.getValue()) {
                PatientInsurance appliedInsurance = appliedInsuranceByItem.get(item.getId());
                boolean isExempted = Boolean.TRUE.equals(requestedExemptedByItem.get(item.getId()));
                BigDecimal unitPrice = requestedUnitPriceByItem.containsKey(item.getId())
                        ? toMoney(requestedUnitPriceByItem.get(item.getId()))
                        : toMoney(item.getPrice());
                BigDecimal quantity = requestedQuantityByItem.containsKey(item.getId())
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
                    coveredAmount = calculateCoveredAmount(item, appliedInsurance, quantity, lineTotal);
                    patientAmount = toMoney(lineTotal.subtract(coveredAmount));
                }

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
            }

            BigDecimal remainingPaidAmount = remainingPaidByDepartment.getOrDefault(group.rootVisitDepartmentId(), ZERO);
            BigDecimal paidAmount = ZERO;
            if (remainingPaidAmount.compareTo(ZERO) > 0) {
                paidAmount = remainingPaidAmount.compareTo(patientPayable) >= 0
                        ? patientPayable
                        : remainingPaidAmount;
                remainingPaidByDepartment.put(group.rootVisitDepartmentId(), toMoney(remainingPaidAmount.subtract(paidAmount)));
            }

            BigDecimal outstanding = toMoney(patientPayable.subtract(paidAmount));
            insuranceBilling.setTotalAmount(total);
            insuranceBilling.setInsuranceCoveredAmount(insuranceCovered);
            insuranceBilling.setPatientPayableAmount(patientPayable);
            insuranceBilling.setPaidAmount(paidAmount);
            insuranceBilling.setOutstandingAmount(outstanding);
            insuranceBilling.setStatus(resolveBillingStatus(paidAmount, patientPayable));

            departmentBilling.setTotalAmount(toMoney(departmentBilling.getTotalAmount().add(total)));
            departmentBilling.setInsuranceCoveredAmount(toMoney(departmentBilling.getInsuranceCoveredAmount().add(insuranceCovered)));
            departmentBilling.setPatientPayableAmount(toMoney(departmentBilling.getPatientPayableAmount().add(patientPayable)));
            departmentBilling.setPaidAmount(toMoney(departmentBilling.getPaidAmount().add(paidAmount)));
            departmentBilling.setOutstandingAmount(toMoney(departmentBilling.getOutstandingAmount().add(outstanding)));
            departmentBilling.setStatus(resolveBillingStatus(departmentBilling.getPaidAmount(), departmentBilling.getPatientPayableAmount()));
        }

        VisitBilling savedVisitBilling = visitBillingRepository.save(visitBilling);
        visitDepartmentProductRepository.saveAll(productsToSave);

        boolean fullyBilled = isVisitFullyBilled(visit.getId());
        if (fullyBilled) {
            visit.setStatus(VisitStatus.COMPLETED);
            visitRepository.save(visit);
        }

        return ApiResponse.success("Visit billed successfully.", visitBillingToMap(savedVisitBilling));
    }

    @Transactional
    public ApiResponse recordVisitBillingPayment(RecordVisitBillingPaymentInput input, AuthenticatedUser authUser) {
        if (input == null || input.departmentInsuranceBillingId() == null || input.amount() == null || input.paymentMethod() == null) {
            return ApiResponse.error("departmentInsuranceBillingId, amount and paymentMethod are required.", "VALIDATION_ERROR");
        }

        if (input.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("amount must be greater than 0.", "VALIDATION_ERROR");
        }

        Optional<DepartmentInsuranceBilling> billingOptional = departmentInsuranceBillingRepository
                .findByIdWithDepartmentBillingAndVisit(input.departmentInsuranceBillingId());
        if (billingOptional.isEmpty()) {
            return ApiResponse.error("Department insurance billing not found.", "NOT_FOUND");
        }

        DepartmentInsuranceBilling insuranceBilling = billingOptional.get();
        Visit visit = insuranceBilling.getVisitDepartmentBilling().getVisitBilling().getVisit();
        if (visit != null && visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cancelled visits cannot accept billing payments.", "INVALID_VISIT_STATUS_FOR_BILLING");
        }

        BigDecimal nextPaid = toMoney(insuranceBilling.getPaidAmount().add(input.amount()));
        if (nextPaid.compareTo(insuranceBilling.getPatientPayableAmount()) > 0) {
            nextPaid = insuranceBilling.getPatientPayableAmount();
        }

        insuranceBilling.setPaidAmount(nextPaid);
        insuranceBilling.setOutstandingAmount(toMoney(insuranceBilling.getPatientPayableAmount().subtract(nextPaid)));
        insuranceBilling.setStatus(resolveBillingStatus(nextPaid, insuranceBilling.getPatientPayableAmount()));

        VisitDepartmentBilling departmentBilling = insuranceBilling.getVisitDepartmentBilling();
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
            totalAmount = toMoney(totalAmount.add(childBilling.getTotalAmount()));
            insuranceCoveredAmount = toMoney(insuranceCoveredAmount.add(childBilling.getInsuranceCoveredAmount()));
            patientPayableAmount = toMoney(patientPayableAmount.add(childBilling.getPatientPayableAmount()));
            paidAmount = toMoney(paidAmount.add(childBilling.getPaidAmount()));
            outstandingAmount = toMoney(outstandingAmount.add(childBilling.getOutstandingAmount()));
        }

        departmentBilling.setTotalAmount(totalAmount);
        departmentBilling.setInsuranceCoveredAmount(insuranceCoveredAmount);
        departmentBilling.setPatientPayableAmount(patientPayableAmount);
        departmentBilling.setPaidAmount(paidAmount);
        departmentBilling.setOutstandingAmount(outstandingAmount);
        departmentBilling.setStatus(resolveBillingStatus(paidAmount, patientPayableAmount));
        visitDepartmentBillingRepository.save(departmentBilling);

        return ApiResponse.success("Payment recorded.", visitBillingToMap(departmentBilling.getVisitBilling()));
    }

    @Transactional(readOnly = true)
    public ApiResponse visitBilling(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.", "VALIDATION_ERROR");
        }

        List<VisitBilling> billings = visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId);
        if (billings.isEmpty()) {
            return ApiResponse.error("Visit billing not found.", "NOT_FOUND");
        }

        return ApiResponse.success("Visit billing fetched.", visitBillingToMap(billings.get(0)));
    }

    @Transactional(readOnly = true)
    public ApiResponse visitBillings(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.", "VALIDATION_ERROR");
        }

        if (!visitRepository.existsById(visitId)) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        List<Map<String, Object>> billings = visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId)
                .stream()
                .map(this::visitBillingToMap)
                .toList();

        return ApiResponse.success("Visit billings fetched.", billings);
    }

    @Transactional
    public ApiResponse generateInvoice(UUID departmentInsuranceBillingId, AuthenticatedUser authUser) {
        if (departmentInsuranceBillingId == null) {
            return ApiResponse.error("departmentInsuranceBillingId is required.", "VALIDATION_ERROR");
        }

        Optional<DepartmentInsuranceBilling> billingOptional = departmentInsuranceBillingRepository
                .findByIdWithDepartmentBillingAndVisit(departmentInsuranceBillingId);
        if (billingOptional.isEmpty()) {
            return ApiResponse.error("Department insurance billing not found.", "NOT_FOUND");
        }

        DepartmentInsuranceBilling billing = billingOptional.get();
        Visit visit = billing.getVisitDepartmentBilling().getVisitBilling().getVisit();

        try {
            Path invoiceDirectory = Path.of(INVOICE_DIR).toAbsolutePath();
            Files.createDirectories(invoiceDirectory);
            String filename = "invoice-" + departmentInsuranceBillingId + ".pdf";
            Path invoiceFile = invoiceDirectory.resolve(filename);

            if (Files.exists(invoiceFile)) {
                Map<String, Object> data = Map.of("invoiceUrl", INVOICE_URL_PATH + filename);
                return ApiResponse.success("Invoice already exists.", data);
            }

            if (visit != null && visit.getStatus() != VisitStatus.COMPLETED) {
                visit.setStatus(VisitStatus.COMPLETED);
                visitRepository.save(visit);
            }

            List<Map<String, Object>> items = visitBillingItemRepository.findByDepartmentInsuranceBillingIdWithProduct(billing.getId())
                    .stream()
                    .map(this::visitBillingItemToMap)
                    .toList();

            InvoicePdfGenerator.createInvoicePdf(invoiceFile, billing, items);
            billing.setInvoiceUrl(INVOICE_URL_PATH + filename);
            departmentInsuranceBillingRepository.save(billing);
            Map<String, Object> data = Map.of("invoiceUrl", INVOICE_URL_PATH + filename);
            return ApiResponse.success("Invoice generated successfully.", data);
        } catch (IOException e) {
            return ApiResponse.error("Failed to generate invoice PDF.", "INVOICE_GENERATION_FAILED");
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse getInvoice(UUID departmentInsuranceBillingId) {
        if (departmentInsuranceBillingId == null) {
            return ApiResponse.error("departmentInsuranceBillingId is required.", "VALIDATION_ERROR");
        }

        Optional<DepartmentInsuranceBilling> billingOptional = departmentInsuranceBillingRepository.findById(departmentInsuranceBillingId);
        if (billingOptional.isEmpty()) {
            return ApiResponse.error("Department insurance billing not found.", "NOT_FOUND");
        }

        String filename = "invoice-" + departmentInsuranceBillingId + ".pdf";
        Path invoiceFile = Path.of(INVOICE_DIR).toAbsolutePath().resolve(filename);
        if (Files.exists(invoiceFile)) {
            Map<String, Object> data = Map.of("invoiceUrl", INVOICE_URL_PATH + filename);
            return ApiResponse.success("Invoice fetched.", data);
        }

        return ApiResponse.error("Invoice not found. Generate it first.", "INVOICE_NOT_FOUND");
    }

    private List<VisitDepartmentProduct> loadVisitDepartmentProducts(UUID visitId) {
        return visitDepartmentRepository.findByVisitId(visitId)
                .stream()
                .flatMap(vd -> visitDepartmentProductRepository.findByVisitDepartmentId(vd.getId()).stream())
                .toList();
    }

        private boolean requiresBilling(VisitDepartmentProduct item) {
        return item.getStatus() != VisitProductStatus.BILLED
                && item.getStatus() != VisitProductStatus.EXEMPTED;
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

    private boolean isProductUnderRootDepartment(VisitDepartmentProduct item, UUID rootVisitDepartmentId) {
        return resolveRootVisitDepartmentId(item.getVisitDepartment()).equals(rootVisitDepartmentId);
    }

    private record BillingGroup(UUID rootVisitDepartmentId, UUID patientInsuranceId) {
    }

    private PatientInsurance resolveAppliedInsurance(
            VisitDepartmentProduct item,
            UUID requestedPatientInsuranceId,
            Set<UUID> visitInsurancePatientInsuranceIds,
            List<VisitInsurance> visitInsurances
    ) {
        if (requestedPatientInsuranceId != null) {
            if (!visitInsurancePatientInsuranceIds.contains(requestedPatientInsuranceId)) {
                return null;
            }
            Optional<PatientInsurance> insuranceOptional = patientInsuranceRepository.findById(requestedPatientInsuranceId);
            if (insuranceOptional.isEmpty()) {
                return null;
            }
            PatientInsurance insurance = insuranceOptional.get();
            ProductInsuranceCoverage coverage = productInsuranceCoverageRepository
                    .findByProductIdAndInsuranceProviderId(item.getProduct().getId(), insurance.getInsuranceProvider().getId())
                    .orElse(null);
            if (coverage == null || !coverage.isCovered()) {
                return null;
            }
            return insurance;
        }

        for (VisitInsurance visitInsurance : visitInsurances) {
            PatientInsurance insurance = visitInsurance.getPatientInsurance();
            ProductInsuranceCoverage coverage = productInsuranceCoverageRepository
                    .findByProductIdAndInsuranceProviderId(item.getProduct().getId(), insurance.getInsuranceProvider().getId())
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

        Optional<ProductInsuranceCoverage> coverageOptional = productInsuranceCoverageRepository
                .findByProductIdAndInsuranceProviderId(item.getProduct().getId(), appliedInsurance.getInsuranceProvider().getId());
        if (coverageOptional.isEmpty() || !coverageOptional.get().isCovered()) {
            return ZERO;
        }

        BigDecimal coverageAmount = coverageOptional.get().getCost() == null
                ? ZERO
                : toMoney(coverageOptional.get().getCost().multiply(quantity));

        if (coverageAmount.compareTo(lineTotal) > 0) {
            coverageAmount = lineTotal;
        }

        return toMoney(coverageAmount);
    }

    private VisitBillingStatus resolveBillingStatus(BigDecimal paidAmount, BigDecimal patientPayableAmount) {
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
        List<VisitDepartmentProduct> items = loadVisitDepartmentProducts(visitId);
        if (items.isEmpty()) {
            return false;
        }

        return items.stream().allMatch(item ->
                item.getStatus() == VisitProductStatus.BILLED
                        || item.getStatus() == VisitProductStatus.EXEMPTED
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
                billing.getDepartments().stream()
                        .map(this::visitDepartmentBillingToMap)
                        .toList()
        );
        return data;
    }

    private Map<String, Object> visitDepartmentBillingToMap(VisitDepartmentBilling billing) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", billing.getId());
        data.put("visitDepartment", visitDepartmentToMap(billing.getVisitDepartment()));
        data.put("status", billing.getStatus());
        data.put("totalAmount", billing.getTotalAmount());
        data.put("insuranceCoveredAmount", billing.getInsuranceCoveredAmount());
        data.put("patientPayableAmount", billing.getPatientPayableAmount());
        data.put("paidAmount", billing.getPaidAmount());
        data.put("outstandingAmount", billing.getOutstandingAmount());
        data.put(
                "payments",
                billing.getPayments().stream()
                        .map(this::visitBillingPaymentToMap)
                        .toList()
        );
        data.put(
                "insuranceBillings",
                billing.getInsuranceBillings().stream()
                        .map(this::departmentInsuranceBillingToMap)
                        .toList()
        );
        data.put("createdAt", billing.getCreatedAt());
        data.put("updatedAt", billing.getUpdatedAt());
        return data;
    }

    private Map<String, Object> departmentInsuranceBillingToMap(DepartmentInsuranceBilling billing) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", billing.getId());
        data.put("patientInsurance", billing.getPatientInsurance() == null ? null : patientInsuranceToMap(billing.getPatientInsurance()));
        data.put("status", billing.getStatus());
        data.put("totalAmount", billing.getTotalAmount());
        data.put("insuranceCoveredAmount", billing.getInsuranceCoveredAmount());
        data.put("patientPayableAmount", billing.getPatientPayableAmount());
        data.put("paidAmount", billing.getPaidAmount());
        data.put("outstandingAmount", billing.getOutstandingAmount());
        data.put("invoiceUrl", billing.getInvoiceUrl());
        data.put(
                "items",
                billing.getItems().stream()
                        .map(this::visitBillingItemToMap)
                        .toList()
        );
        data.put("createdAt", billing.getCreatedAt());
        data.put("updatedAt", billing.getUpdatedAt());
        return data;
    }

    private Map<String, Object> visitDepartmentToMap(VisitDepartment visitDepartment) {
        if (visitDepartment == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", visitDepartment.getId());
        data.put("department", departmentToMap(visitDepartment.getDepartment()));
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

    private Map<String, Object> patientInsuranceToMap(PatientInsurance insurance) {
        if (insurance == null) {
            return null;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", insurance.getId());
        data.put("insuranceProviderId", insurance.getInsuranceProvider() == null ? null : insurance.getInsuranceProvider().getId());
        data.put("insuranceCardNumber", insurance.getInsuranceCardNumber());
        data.put("principalMemberName", insurance.getPrincipalMemberName());
        return data;
    }

    private Map<String, Object> visitBillingItemToMap(VisitBillingItem item) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("visitDepartmentProductId", item.getVisitDepartmentProduct().getId());
        data.put("productId", item.getVisitDepartmentProduct().getProduct().getId());
        data.put("productName", item.getVisitDepartmentProduct().getProduct().getName());
        data.put("unitPriceSnapshot", item.getUnitPriceSnapshot());
        data.put("quantitySnapshot", item.getQuantitySnapshot());
        data.put("lineTotal", item.getLineTotal());
        data.put("insuranceCoveredAmount", item.getInsuranceCoveredAmount());
        data.put("patientPayableAmount", item.getPatientPayableAmount());
        data.put("createdAt", item.getCreatedAt());
        data.put("updatedAt", item.getUpdatedAt());
        return data;
    }

    private Map<String, Object> visitBillingPaymentToMap(VisitBillingPayment payment) {
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
}

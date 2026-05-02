package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitBillingItem;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.graphql.input.RecordVisitBillingPaymentInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingItemRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitInsuranceRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final ProductInsuranceCoverageRepository productInsuranceCoverageRepository;
    private final VisitBillingRepository visitBillingRepository;
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
        this.visitBillingItemRepository = visitBillingItemRepository;
        this.workerRepository = workerRepository;
    }

    @Transactional
    public ApiResponse billVisit(BillVisitInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitId() == null) {
            return ApiResponse.error("visitId is required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(input.visitId());
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cancelled visits cannot be billed.", "INVALID_VISIT_STATUS_FOR_BILLING");
        }
        List<VisitDepartmentProduct> allProducts = loadVisitDepartmentProducts(visit.getId());
        Map<UUID, VisitDepartmentProduct> allProductsById = allProducts.stream()
                .collect(Collectors.toMap(VisitDepartmentProduct::getId, p -> p));

        Map<UUID, UUID> requestedInsuranceByItem = new LinkedHashMap<>();
        List<VisitDepartmentProduct> targetItems = resolveTargetBillingItems(input, allProductsById, requestedInsuranceByItem);
        if (targetItems == null) {
            return ApiResponse.error("Invalid billing selection. Ensure item ids exist and are billable.", "INVALID_BILLING_SELECTION");
        }

        if (targetItems.isEmpty()) {
            return ApiResponse.error("No products eligible for billing.", "NOTHING_TO_BILL");
        }

        List<VisitInsurance> visitInsurances = visitInsuranceRepository.findByVisitId(visit.getId());
        Set<UUID> visitInsurancePatientInsuranceIds = visitInsurances.stream()
                .map(v -> v.getPatientInsurance().getId())
                .collect(Collectors.toSet());

        Worker actingUser = resolveWorker(authUser);
        VisitBilling billing = new VisitBilling();
        billing.setVisit(visit);
        billing.setBilledBy(actingUser);
        billing.setBillingDate(visit.getVisitDate());
        billing.setStatus(VisitBillingStatus.UNPAID);
        billing.setTotalAmount(ZERO);
        billing.setInsuranceCoveredAmount(ZERO);
        billing.setPatientPayableAmount(ZERO);
        billing.setPaidAmount(ZERO);
        billing.setOutstandingAmount(ZERO);
        billing.setFullyBilledVisit(false);

        VisitBilling savedBilling = visitBillingRepository.save(billing);

        BigDecimal total = ZERO;
        BigDecimal insuranceCovered = ZERO;
        BigDecimal patientPayable = ZERO;

        List<VisitBillingItem> billingItems = new ArrayList<>();
        for (VisitDepartmentProduct item : targetItems) {
            UUID requestedPatientInsuranceId = requestedInsuranceByItem.get(item.getId());
            PatientInsurance appliedInsurance = resolveAppliedInsurance(item, requestedPatientInsuranceId, visitInsurancePatientInsuranceIds, visitInsurances);
            if (requestedPatientInsuranceId != null && appliedInsurance == null) {
                return ApiResponse.error("Selected patientInsuranceId is invalid for the visit or does not cover the product.", "INVALID_VISIT_INSURANCE_SELECTION");
            }

            BigDecimal unitPrice = toMoney(item.getPrice());
            BigDecimal quantity = toQuantity(item.getQuantity());
            BigDecimal lineTotal = toMoney(unitPrice.multiply(quantity));
            BigDecimal coveredAmount = calculateCoveredAmount(item, appliedInsurance, quantity, lineTotal);
            BigDecimal patientAmount = toMoney(lineTotal.subtract(coveredAmount));

            VisitBillingItem billingItem = new VisitBillingItem();
            billingItem.setVisitBilling(savedBilling);
            billingItem.setVisitDepartmentProduct(item);
            billingItem.setAppliedPatientInsurance(appliedInsurance);
            billingItem.setUnitPriceSnapshot(unitPrice);
            billingItem.setQuantitySnapshot(quantity);
            billingItem.setLineTotal(lineTotal);
            billingItem.setInsuranceCoveredAmount(coveredAmount);
            billingItem.setPatientPayableAmount(patientAmount);
            billingItems.add(billingItem);

            total = toMoney(total.add(lineTotal));
            insuranceCovered = toMoney(insuranceCovered.add(coveredAmount));
            patientPayable = toMoney(patientPayable.add(patientAmount));

            item.setStatus(VisitProductStatus.BILLED);
            item.setBilledBy(actingUser);
            visitDepartmentProductRepository.save(item);
        }

        visitBillingItemRepository.saveAll(billingItems);

        BigDecimal paidAmount = toMoney(input.paidAmount() == null ? ZERO : input.paidAmount());
        if (paidAmount.compareTo(patientPayable) > 0) {
            paidAmount = patientPayable;
        }

        BigDecimal outstanding = toMoney(patientPayable.subtract(paidAmount));

        savedBilling.setTotalAmount(total);
        savedBilling.setInsuranceCoveredAmount(insuranceCovered);
        savedBilling.setPatientPayableAmount(patientPayable);
        savedBilling.setPaidAmount(paidAmount);
        savedBilling.setOutstandingAmount(outstanding);
        savedBilling.setStatus(resolveBillingStatus(paidAmount, patientPayable));

        boolean fullyBilled = isVisitFullyBilled(visit.getId());
        savedBilling.setFullyBilledVisit(fullyBilled);
        if (fullyBilled) {
            visit.setStatus(VisitStatus.COMPLETED);
            visitRepository.save(visit);
        }

        VisitBilling latest = visitBillingRepository.save(savedBilling);
        return ApiResponse.success("Visit billed successfully.", visitBillingToMap(latest));
    }

    @Transactional
    public ApiResponse recordVisitBillingPayment(RecordVisitBillingPaymentInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitBillingId() == null || input.amount() == null) {
            return ApiResponse.error("visitBillingId and amount are required.", "VALIDATION_ERROR");
        }

        if (input.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ApiResponse.error("amount must be greater than 0.", "VALIDATION_ERROR");
        }

        Optional<VisitBilling> billingOptional = visitBillingRepository.findById(input.visitBillingId());
        if (billingOptional.isEmpty()) {
            return ApiResponse.error("Visit billing not found.", "NOT_FOUND");
        }

        VisitBilling billing = billingOptional.get();
        if (billing.getVisit() != null && billing.getVisit().getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cancelled visits cannot accept billing payments.", "INVALID_VISIT_STATUS_FOR_BILLING");
        }
        BigDecimal nextPaid = toMoney(billing.getPaidAmount().add(input.amount()));
        if (nextPaid.compareTo(billing.getPatientPayableAmount()) > 0) {
            nextPaid = billing.getPatientPayableAmount();
        }

        billing.setPaidAmount(nextPaid);
        billing.setOutstandingAmount(toMoney(billing.getPatientPayableAmount().subtract(nextPaid)));
        billing.setStatus(resolveBillingStatus(nextPaid, billing.getPatientPayableAmount()));

        VisitBilling saved = visitBillingRepository.save(billing);
        return ApiResponse.success("Payment recorded.", visitBillingToMap(saved));
    }

    @Transactional(readOnly = true)
    public ApiResponse visitBilling(UUID visitBillingId) {
        if (visitBillingId == null) {
            return ApiResponse.error("visitBillingId is required.", "VALIDATION_ERROR");
        }

        Optional<VisitBilling> billingOptional = visitBillingRepository.findById(visitBillingId);
        if (billingOptional.isEmpty()) {
            return ApiResponse.error("Visit billing not found.", "NOT_FOUND");
        }

        return ApiResponse.success("Visit billing fetched.", visitBillingToMap(billingOptional.get()));
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

    private List<VisitDepartmentProduct> loadVisitDepartmentProducts(UUID visitId) {
        return visitDepartmentRepository.findByVisitIdOrderByCreatedAtAsc(visitId)
                .stream()
                .flatMap(vd -> visitDepartmentProductRepository.findByVisitDepartmentId(vd.getId()).stream())
                .toList();
    }

    private List<VisitDepartmentProduct> resolveTargetBillingItems(
            BillVisitInput input,
            Map<UUID, VisitDepartmentProduct> allProductsById,
            Map<UUID, UUID> requestedInsuranceByItem
    ) {
        boolean billAll = Boolean.TRUE.equals(input.billAllProducts());

        if (billAll) {
            return allProductsById.values().stream()
                    .filter(this::requiresBilling)
                    .toList();
        }

        if (input.items() == null || input.items().isEmpty()) {
            return null;
        }

        List<VisitDepartmentProduct> selected = new ArrayList<>();
        for (BillVisitInput.BillVisitItemInput itemInput : input.items()) {
            if (itemInput == null || itemInput.visitDepartmentProductId() == null) {
                return null;
            }

            VisitDepartmentProduct product = allProductsById.get(itemInput.visitDepartmentProductId());
            if (product == null || !requiresBilling(product)) {
                return null;
            }

            selected.add(product);
            requestedInsuranceByItem.put(product.getId(), itemInput.patientInsuranceId());
        }

        return selected;
    }

    private boolean requiresBilling(VisitDepartmentProduct item) {
        return item.getStatus() != VisitProductStatus.BILLED
                && item.getStatus() != VisitProductStatus.EXEMPTED;
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
        data.put("status", billing.getStatus());
        data.put("totalAmount", billing.getTotalAmount());
        data.put("insuranceCoveredAmount", billing.getInsuranceCoveredAmount());
        data.put("patientPayableAmount", billing.getPatientPayableAmount());
        data.put("paidAmount", billing.getPaidAmount());
        data.put("outstandingAmount", billing.getOutstandingAmount());
        data.put("fullyBilledVisit", billing.isFullyBilledVisit());
        data.put("billingDate", billing.getBillingDate() == null ? billing.getVisit().getVisitDate() : billing.getBillingDate());
        data.put("billedBy", workerToMap(billing.getBilledBy()));
        data.put(
                "items",
                visitBillingItemRepository.findByVisitBillingId(billing.getId())
                        .stream()
                        .map(this::visitBillingItemToMap)
                        .toList()
        );
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
        data.put("appliedPatientInsuranceId", item.getAppliedPatientInsurance() == null ? null : item.getAppliedPatientInsurance().getId());
        data.put("createdAt", item.getCreatedAt());
        data.put("updatedAt", item.getUpdatedAt());
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

package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Read-only billing queries: fetch the latest billing version, load visit
 * department products, resolve visit insurances, and prefetch coverage data.
 *
 * <p>Extracted from {@code VisitBillingService} to isolate the read paths
 * from the billing orchestration so the service only contains write/validate
 * logic.
 */
@Component
public class BillingQueryService {

    private final com.nexxserve.nexxclinic.repository.VisitBillingRepository visitBillingRepository;
    private final com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final com.nexxserve.nexxclinic.repository.VisitInsuranceRepository visitInsuranceRepository;
    private final com.nexxserve.nexxclinic.repository.VisitDepartmentRepository visitDepartmentRepository;
    private final BillingVersionBuilder billingVersionBuilder;
    private final BillingDataMapper billingDataMapper;

    public BillingQueryService(
        com.nexxserve.nexxclinic.repository.VisitBillingRepository visitBillingRepository,
        com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository visitDepartmentProductRepository,
        com.nexxserve.nexxclinic.repository.VisitInsuranceRepository visitInsuranceRepository,
        com.nexxserve.nexxclinic.repository.VisitDepartmentRepository visitDepartmentRepository,
        BillingVersionBuilder billingVersionBuilder,
        BillingDataMapper billingDataMapper
    ) {
        this.visitBillingRepository = visitBillingRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.billingVersionBuilder = billingVersionBuilder;
        this.billingDataMapper = billingDataMapper;
    }

    /**
     * Fetches the latest billing version for a visit and returns it as a map
     * suitable for the GraphQL response.
     */
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

    /**
     * Loads all visit department products for a visit. Soft-deleted products are
     * excluded (same as the old per-department query).
     */
    public List<VisitDepartmentProduct> loadVisitDepartmentProducts(UUID visitId) {
        return visitDepartmentProductRepository.findByVisitDepartmentVisitId(visitId);
    }

    /**
     * Loads all visit insurances for a visit and returns the set of
     * patientInsurance IDs.
     */
    public Set<UUID> loadVisitInsurancePatientIds(UUID visitId) {
        List<VisitInsurance> visitInsurances = visitInsuranceRepository.findByVisitId(visitId);
        return visitInsurances.stream()
            .map(v -> v.getPatientInsurance().getId())
            .collect(Collectors.toSet());
    }

    /**
     * Loads all visit insurances for a visit.
     */
    public List<VisitInsurance> loadVisitInsurances(UUID visitId) {
        return visitInsuranceRepository.findByVisitId(visitId);
    }

    /**
     * Loads all visit department products for a visit as a map keyed by product ID.
     */
    public Map<UUID, VisitDepartmentProduct> loadVisitDepartmentProductsById(UUID visitId) {
        return loadVisitDepartmentProducts(visitId).stream()
            .collect(Collectors.toMap(VisitDepartmentProduct::getId, p -> p, (a, b) -> a));
    }
}

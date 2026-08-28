package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.InsuranceCoverage;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.entity.VisitPriceEstimate;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.model.EncounterType;
import com.nexxserve.nexxclinic.model.PatientShareSource;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.repository.InsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitInsuranceRepository;
import com.nexxserve.nexxclinic.repository.VisitPriceEstimateRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auto-computes pre-billing price estimates for a visit.
 * <p>
 * Whenever products or insurances change on a visit, this service recomputes
 * per-line estimates and updates the visit's summary price fields. The estimates
 * give a live price preview before a biller touches the visit — they are NOT
 * financial records and are deleted when {@code billVisit} is called.
 * <p>
 * Insurance auto-selection logic:
 * <ol>
 *   <li>For each product, check all linked visit insurances that cover it
 *       (via {@link ProductInsuranceCoverage}).</li>
 *   <li>For each covering insurance, resolve the patient share % using the
 *       standard coverage resolution chain (exact match → dept → encounter
 *       type → base → patient default).</li>
 *   <li>Prefer the insurance that gives the patient the best deal (lowest
 *       patient share %). Encounter-type/dept rule matches take priority
 *       over base-only matches when resolving which insurance is "best".</li>
 * </ol>
 */
@Service
public class VisitPriceEstimateService {

    private static final Logger log = LoggerFactory.getLogger(VisitPriceEstimateService.class);

    private final VisitRepository visitRepository;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final ProductInsuranceCoverageRepository productInsuranceCoverageRepository;
    private final InsuranceCoverageRepository insuranceCoverageRepository;
    private final VisitPriceEstimateRepository visitPriceEstimateRepository;

    public VisitPriceEstimateService(
        VisitRepository visitRepository,
        VisitInsuranceRepository visitInsuranceRepository,
        VisitDepartmentProductRepository visitDepartmentProductRepository,
        ProductInsuranceCoverageRepository productInsuranceCoverageRepository,
        InsuranceCoverageRepository insuranceCoverageRepository,
        VisitPriceEstimateRepository visitPriceEstimateRepository
    ) {
        this.visitRepository = visitRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
        this.productInsuranceCoverageRepository = productInsuranceCoverageRepository;
        this.insuranceCoverageRepository = insuranceCoverageRepository;
        this.visitPriceEstimateRepository = visitPriceEstimateRepository;
    }

    /**
     * Recompute all pre-billing price estimates for a visit.
     * Called after any product or insurance mutation on the visit.
     * Safe to call in a read-only transaction (just recalculates from DB).
     */
    @Transactional
    public void recomputeEstimates(UUID visitId) {
        Optional<Visit> visitOpt = visitRepository.findById(visitId);
        if (visitOpt.isEmpty()) {
            log.warn("recomputeEstimates called for non-existent visit {}", visitId);
            return;
        }
        Visit visit = visitOpt.get();

        // Fetch all active (non-deleted) visit department products across all departments
        List<VisitDepartment> departments = visit.getPatient() != null
            ? new ArrayList<>() // placeholder — we query via repository
            : new ArrayList<>();

        // Load all products for this visit
        List<VisitDepartmentProduct> allProducts = visitDepartmentProductRepository
            .findByVisitDepartmentVisitId(visitId);

        // Filter to active (non-deleted) products in non-exempted status
        List<VisitDepartmentProduct> activeProducts = allProducts.stream()
            .filter(p -> !p.isDeleted())
            .filter(p -> p.getStatus() == VisitProductStatus.PENDING
                      || p.getStatus() == VisitProductStatus.UNPAID
                      || p.getStatus() == VisitProductStatus.CORRECTION_PENDING)
            .toList();

        if (activeProducts.isEmpty()) {
            // No products — clear all estimates and null out visit totals
            visitPriceEstimateRepository.deleteByVisitId(visitId);
            visit.setEstimatedTotal(null);
            visit.setEstimatedInsurancePay(null);
            visit.setEstimatedPatientPay(null);
            visitRepository.save(visit);
            return;
        }

        // Fetch visit insurances and their provider IDs
        List<VisitInsurance> visitInsurances = visitInsuranceRepository.findByVisitId(visitId);

        // Build sets for batch lookups
        Set<UUID> productIds = activeProducts.stream()
            .map(p -> p.getProduct().getId())
            .collect(java.util.stream.Collectors.toSet());

        Set<UUID> providerIds = visitInsurances.stream()
            .map(vi -> vi.getPatientInsurance().getInsuranceProvider().getId())
            .collect(java.util.stream.Collectors.toSet());

        // Batch prefetch product insurance coverages
        Map<UUID, Map<UUID, ProductInsuranceCoverage>> productCoverages = prefetchProductCoverages(productIds, providerIds);

        // Batch prefetch patient share coverages
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> patientShareCoverages = prefetchPatientShareCoverages(providerIds);

        // Delete existing estimates for this visit
        visitPriceEstimateRepository.deleteByVisitId(visitId);

        // Compute estimates for each active product
        List<VisitPriceEstimate> newEstimates = new ArrayList<>();
        BigDecimal totalLineTotal = BigDecimal.ZERO;
        BigDecimal totalInsurancePay = BigDecimal.ZERO;
        BigDecimal totalPatientPay = BigDecimal.ZERO;

        for (VisitDepartmentProduct vdp : activeProducts) {
            try {
                VisitPriceEstimate estimate = computeEstimateForProduct(
                    visit, vdp, visitInsurances, productCoverages, patientShareCoverages
                );
                newEstimates.add(estimate);
                totalLineTotal = totalLineTotal.add(estimate.getLineTotal());
                totalInsurancePay = totalInsurancePay.add(estimate.getInsuranceCoveredAmount());
                totalPatientPay = totalPatientPay.add(estimate.getPatientPayableAmount());
            } catch (Exception e) {
                log.warn("Failed to compute estimate for product {} on visit {}: {}",
                    vdp.getProduct().getName(), visitId, e.getMessage());
                // Still add a zero estimate so the product is represented
                VisitPriceEstimate zeroEstimate = createZeroEstimate(visit, vdp);
                newEstimates.add(zeroEstimate);
                // totalLineTotal etc. remain unaffected (add zero)
            }
        }

        // Bulk save all new estimates
        visitPriceEstimateRepository.saveAll(newEstimates);

        // Update the visit's summary price fields
        visit.setEstimatedTotal(totalLineTotal.compareTo(BigDecimal.ZERO) == 0 ? null : totalLineTotal);
        visit.setEstimatedInsurancePay(totalInsurancePay.compareTo(BigDecimal.ZERO) == 0 ? null : totalInsurancePay);
        visit.setEstimatedPatientPay(totalPatientPay.compareTo(BigDecimal.ZERO) == 0 ? null : totalPatientPay);
        visitRepository.save(visit);
    }

    /**
     * Delete all estimates for a visit. Called when billing is created.
     */
    @Transactional
    public void deleteEstimates(UUID visitId) {
        visitPriceEstimateRepository.deleteByVisitId(visitId);
        Optional<Visit> visitOpt = visitRepository.findById(visitId);
        if (visitOpt.isPresent()) {
            Visit visit = visitOpt.get();
            visit.setEstimatedTotal(null);
            visit.setEstimatedInsurancePay(null);
            visit.setEstimatedPatientPay(null);
            visitRepository.save(visit);
        }
    }

    @Transactional
    public void deleteEstimatesByVisitDepartmentId(UUID visitDepartmentId) {
        visitPriceEstimateRepository.deleteByVisitDepartmentId(visitDepartmentId);
    }

    /**
     * Fetch all pre-billing price estimates for a visit.
     */
    @Transactional(readOnly = true)
    public com.nexxserve.nexxclinic.dto.out.ApiResponse getEstimatesForVisit(UUID visitId) {
        if (visitId == null) {
            return com.nexxserve.nexxclinic.dto.out.ApiResponse.error("visitId is required.");
        }
        List<VisitPriceEstimate> estimates = visitPriceEstimateRepository.findByVisitId(visitId);
        return com.nexxserve.nexxclinic.dto.out.ApiResponse.success("Price estimates fetched.", estimates);
    }

    // ─── Internal computation ──────────────────────────────────────

    /**
     * Compute a single estimate for a visit department product.
     * Resolves the best insurance and calculates line-level pricing.
     */
    private VisitPriceEstimate computeEstimateForProduct(
        Visit visit,
        VisitDepartmentProduct vdp,
        List<VisitInsurance> visitInsurances,
        Map<UUID, Map<UUID, ProductInsuranceCoverage>> productCoverages,
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> patientShareCoverages
    ) {
        VisitPriceEstimate estimate = new VisitPriceEstimate();
        estimate.setVisit(visit);
        estimate.setVisitDepartmentProduct(vdp);
        estimate.setQuantity(vdp.getQuantity());

        UUID productId = vdp.getProduct().getId();

        // Resolve which department and encounter type this product belongs to
        VisitDepartment vd = vdp.getVisitDepartment();
        UUID departmentId = vd != null && vd.getDepartment() != null ? vd.getDepartment().getId() : null;
        EncounterType encounterType = vd != null ? vd.getEncounterType() : null;

        // Try to find the best insurance for this product
        PatientInsurance bestInsurance = null;
        int bestPatientSharePct = 101; // higher than any valid percentage
        PatientShareSource bestSource = PatientShareSource.PROVIDER_DEFAULT;

        for (VisitInsurance vi : visitInsurances) {
            PatientInsurance pi = vi.getPatientInsurance();
            if (pi == null || pi.isDeactivated()) continue;

            // Check validity period
            LocalDate today = LocalDate.now();
            if (pi.getValidFrom() != null && pi.getValidFrom().isAfter(today)) continue;
            if (pi.getValidUntil() != null && pi.getValidUntil().isBefore(today)) continue;

            UUID providerId = pi.getInsuranceProvider().getId();

            // Check if this insurance covers this product
            ProductInsuranceCoverage coverage = lookupProductCoverage(
                productId, providerId, productCoverages
            );
            if (coverage == null || !coverage.isCovered() || coverage.isNotPaid()) continue;

            // Resolve the patient share percentage
            ResolvedShare resolved = resolvePatientShare(
                pi, departmentId, encounterType, patientShareCoverages
            );

            // If a rule match exists (has encounterType/dept condition), it takes priority.
            // Among rule matches, prefer the one with lowest patient share %.
            // Among non-rule matches (base/patient-default), prefer the one with lowest %.
            // Rule match always beats non-rule match.
            boolean isRuleMatch = resolved.source == PatientShareSource.RULE;

            if (bestInsurance == null) {
                bestInsurance = pi;
                bestPatientSharePct = resolved.percentage;
                bestSource = resolved.source;
            } else {
                boolean currentIsRule = bestSource == PatientShareSource.RULE;
                if (isRuleMatch && !currentIsRule) {
                    // New is rule, current is not — new wins
                    bestInsurance = pi;
                    bestPatientSharePct = resolved.percentage;
                    bestSource = resolved.source;
                } else if (isRuleMatch == currentIsRule) {
                    // Same category — prefer lower patient share %
                    if (resolved.percentage < bestPatientSharePct) {
                        bestInsurance = pi;
                        bestPatientSharePct = resolved.percentage;
                        bestSource = resolved.source;
                    }
                }
                // else: current is rule, new is not — keep current
            }
        }

        // Set the resolved insurance
        estimate.setAppliedPatientInsurance(bestInsurance);
        estimate.setResolvedPatientSharePct(bestInsurance != null ? bestPatientSharePct : 0);
        estimate.setPatientShareSource(bestSource);

        // Resolve unit price
        BigDecimal unitPrice;
        if (bestInsurance != null) {
            ProductInsuranceCoverage bestCoverage = lookupProductCoverage(
                productId,
                bestInsurance.getInsuranceProvider().getId(),
                productCoverages
            );
            unitPrice = resolveInsuranceUnitPrice(vdp, bestCoverage);
        } else {
            unitPrice = resolvePrivateUnitPrice(vdp);
        }

        estimate.setUnitPrice(unitPrice);

        // Calculate line total
        BigDecimal quantity = vdp.getQuantity() != null ? vdp.getQuantity() : BigDecimal.ONE;
        BigDecimal lineTotal = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        estimate.setLineTotal(lineTotal);

        // Calculate insurance covered and patient payable
        if (bestInsurance != null && bestPatientSharePct < 100) {
            BigDecimal insuranceSharePct = BigDecimal.valueOf(100 - bestPatientSharePct);
            BigDecimal insuranceCovered = lineTotal.multiply(insuranceSharePct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            // Cap at line total
            if (insuranceCovered.compareTo(lineTotal) > 0) {
                insuranceCovered = lineTotal;
            }
            BigDecimal patientPayable = lineTotal.subtract(insuranceCovered);
            estimate.setInsuranceCoveredAmount(insuranceCovered);
            estimate.setPatientPayableAmount(patientPayable);
        } else {
            // No insurance or 100% patient share — patient pays everything
            estimate.setInsuranceCoveredAmount(BigDecimal.ZERO);
            estimate.setPatientPayableAmount(lineTotal);
        }

        return estimate;
    }

    private VisitPriceEstimate createZeroEstimate(Visit visit, VisitDepartmentProduct vdp) {
        VisitPriceEstimate estimate = new VisitPriceEstimate();
        estimate.setVisit(visit);
        estimate.setVisitDepartmentProduct(vdp);
        estimate.setQuantity(vdp.getQuantity() != null ? vdp.getQuantity() : BigDecimal.ONE);
        estimate.setUnitPrice(BigDecimal.ZERO);
        estimate.setLineTotal(BigDecimal.ZERO);
        estimate.setInsuranceCoveredAmount(BigDecimal.ZERO);
        estimate.setPatientPayableAmount(BigDecimal.ZERO);
        estimate.setResolvedPatientSharePct(0);
        estimate.setPatientShareSource(PatientShareSource.PROVIDER_DEFAULT);
        return estimate;
    }

    // ─── Price resolution ──────────────────────────────────────────

    private BigDecimal resolveInsuranceUnitPrice(
        VisitDepartmentProduct vdp,
        ProductInsuranceCoverage coverage
    ) {
        if (coverage == null) {
            return resolvePrivateUnitPrice(vdp);
        }
        if (coverage.isNotPaid()) {
            return BigDecimal.ZERO;
        }
        if (coverage.getCost() != null && coverage.getCost().compareTo(BigDecimal.ZERO) > 0) {
            return coverage.getCost();
        }
        return resolvePrivateUnitPrice(vdp);
    }

    private BigDecimal resolvePrivateUnitPrice(VisitDepartmentProduct vdp) {
        if (vdp.getProduct().isNotPaid()) {
            return BigDecimal.ZERO;
        }
        if (vdp.getProduct().getClinicPrice() != null) {
            return vdp.getProduct().getClinicPrice();
        }
        if (vdp.getProduct().getPrivateRhicPrice() != null) {
            return vdp.getProduct().getPrivateRhicPrice();
        }
        return BigDecimal.ZERO;
    }

    // ─── Patient share resolution ──────────────────────────────────

    private record ResolvedShare(int percentage, PatientShareSource source) {}

    /**
     * Resolves the patient share percentage for a product, following the same
     * resolution chain as BillingPricingCalculator:
     * 1. Coverage rule: dept + encounterType (most specific)
     * 2. Coverage rule: dept only
     * 3. Coverage rule: encounterType only
     * 4. Base coverage (no conditions)
     * 5. Patient-specific default (PatientInsurance.patientSharePercentage)
     * 6. 0 (insurance covers everything)
     */
    private ResolvedShare resolvePatientShare(
        PatientInsurance pi,
        UUID departmentId,
        EncounterType encounterType,
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetchedCoverages
    ) {
        UUID providerId = pi.getInsuranceProvider().getId();

        Map<UUID, List<InsuranceCoverage>> byDept = prefetchedCoverages != null
            ? prefetchedCoverages.get(providerId)
            : null;

        if (byDept != null) {
            // 1. Exact: dept + encounterType
            if (departmentId != null && encounterType != null) {
                List<InsuranceCoverage> deptCoverages = byDept.get(departmentId);
                if (deptCoverages != null) {
                    for (InsuranceCoverage cov : deptCoverages) {
                        if (encounterType.equals(cov.getEncounterType())) {
                            return new ResolvedShare(
                                cov.getPatientSharePercentage(),
                                PatientShareSource.RULE
                            );
                        }
                    }
                }
            }

            // 2. Dept only
            if (departmentId != null) {
                List<InsuranceCoverage> deptCoverages = byDept.get(departmentId);
                if (deptCoverages != null) {
                    for (InsuranceCoverage cov : deptCoverages) {
                        if (cov.getEncounterType() == null) {
                            return new ResolvedShare(
                                cov.getPatientSharePercentage(),
                                PatientShareSource.RULE
                            );
                        }
                    }
                }
            }

            // 3. EncounterType only
            if (encounterType != null) {
                List<InsuranceCoverage> nullDeptCoverages = byDept.get(null);
                if (nullDeptCoverages != null) {
                    for (InsuranceCoverage cov : nullDeptCoverages) {
                        if (encounterType.equals(cov.getEncounterType())) {
                            return new ResolvedShare(
                                cov.getPatientSharePercentage(),
                                PatientShareSource.RULE
                            );
                        }
                    }
                }
            }

            // 4. Base (no conditions)
            List<InsuranceCoverage> providerCoverages = byDept.get(null);
            if (providerCoverages != null) {
                for (InsuranceCoverage cov : providerCoverages) {
                    if (cov.getEncounterType() == null) {
                        return new ResolvedShare(
                            cov.getPatientSharePercentage(),
                            PatientShareSource.PROVIDER_DEFAULT
                        );
                    }
                }
            }
        }

        // 5. Patient-specific default
        Integer patientDefault = null;
        if (pi.getPatientShareCoverage() != null) {
            patientDefault = pi.getPatientShareCoverage().getPatientSharePercentage();
        }
        if (patientDefault == null) {
            patientDefault = pi.getPatientSharePercentage();
        }
        if (patientDefault != null) {
            return new ResolvedShare(patientDefault, PatientShareSource.PATIENT_DEFAULT);
        }

        // 6. Provider base (fallback lazy-load)
        Integer providerBase = pi.getInsuranceProvider().getBasePatientSharePercentage();
        if (providerBase != null) {
            return new ResolvedShare(providerBase, PatientShareSource.PROVIDER_DEFAULT);
        }

        // Default: 0% patient share (insurance covers everything)
        return new ResolvedShare(0, PatientShareSource.PROVIDER_DEFAULT);
    }

    // ─── Batch prefetch ────────────────────────────────────────────

    private Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetchProductCoverages(
        Set<UUID> productIds, Set<UUID> providerIds
    ) {
        if (productIds.isEmpty() || providerIds.isEmpty()) {
            return Map.of();
        }
        List<ProductInsuranceCoverage> coverages = productInsuranceCoverageRepository
            .findByProductIdInAndInsuranceProviderIdIn(productIds, providerIds);
        Map<UUID, Map<UUID, ProductInsuranceCoverage>> result = new HashMap<>();
        for (ProductInsuranceCoverage c : coverages) {
            result.computeIfAbsent(c.getProduct().getId(), k -> new HashMap<>())
                  .put(c.getInsuranceProvider().getId(), c);
        }
        return result;
    }

    private Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetchPatientShareCoverages(
        Set<UUID> providerIds
    ) {
        if (providerIds.isEmpty()) {
            return Map.of();
        }
        List<InsuranceCoverage> coverages = insuranceCoverageRepository
            .findByInsuranceProviderIdIn(providerIds);
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> result = new HashMap<>();
        for (InsuranceCoverage c : coverages) {
            UUID provId = c.getInsuranceProvider().getId();
            UUID deptId = c.getDepartment() != null ? c.getDepartment().getId() : null;
            result.computeIfAbsent(provId, k -> new HashMap<>())
                  .computeIfAbsent(deptId, k -> new ArrayList<>())
                  .add(c);
        }
        return result;
    }

    private ProductInsuranceCoverage lookupProductCoverage(
        UUID productId, UUID providerId,
        Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetched
    ) {
        if (prefetched != null) {
            Map<UUID, ProductInsuranceCoverage> byProvider = prefetched.get(productId);
            if (byProvider != null) {
                ProductInsuranceCoverage cached = byProvider.get(providerId);
                if (cached != null) return cached;
            }
        }
        return productInsuranceCoverageRepository
            .findByProductIdAndInsuranceProviderId(productId, providerId)
            .orElse(null);
    }
}

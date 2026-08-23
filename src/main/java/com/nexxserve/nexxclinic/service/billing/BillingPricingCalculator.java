package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.InsuranceCoverage;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.model.CoverageType;
import com.nexxserve.nexxclinic.model.EncounterType;
import com.nexxserve.nexxclinic.model.PatientShareSource;
import com.nexxserve.nexxclinic.repository.InsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Resolves which patient insurance applies to a product and computes the unit
 * price and the insurance-covered amount for a billing line. Kept separate from
 * the billing orchestration so pricing rules live in one testable place.
 *
 * <p>Patient share resolution uses a multi-layer chain (most specific wins):
 * <ol>
 *   <li>Per-line override (from billing input)</li>
 *   <li>{@link InsuranceCoverage} matching (provider + department + encounter type)</li>
 *   <li>Patient-specific default ({@code PatientInsurance.patientSharePercentage})</li>
 *   <li>Base coverage (provider-wide, no conditions)</li>
 * </ol>
 */
@Component
public class BillingPricingCalculator {

    private final PatientInsuranceRepository patientInsuranceRepository;
    private final ProductInsuranceCoverageRepository productInsuranceCoverageRepository;
    private final InsuranceCoverageRepository coverageRepository;

    public BillingPricingCalculator(
        PatientInsuranceRepository patientInsuranceRepository,
        ProductInsuranceCoverageRepository productInsuranceCoverageRepository,
        InsuranceCoverageRepository coverageRepository
    ) {
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.productInsuranceCoverageRepository = productInsuranceCoverageRepository;
        this.coverageRepository = coverageRepository;
    }

    /**
     * Pre-fetches all {@link ProductInsuranceCoverage} rows for the given
     * product/provider pairs in a single query.
     */
    public Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetchProductCoverages(
        Set<UUID> productIds,
        Set<UUID> insuranceProviderIds
    ) {
        if (productIds == null || productIds.isEmpty() ||
            insuranceProviderIds == null || insuranceProviderIds.isEmpty()) {
            return Map.of();
        }
        List<ProductInsuranceCoverage> coverages = productInsuranceCoverageRepository
            .findByProductIdInAndInsuranceProviderIdIn(productIds, insuranceProviderIds);
        return coverages.stream().collect(
            Collectors.groupingBy(
                c -> c.getProduct().getId(),
                Collectors.toMap(
                    c -> c.getInsuranceProvider().getId(),
                    c -> c,
                    (a, b) -> a
                )
            )
        );
    }

    /**
     * Pre-fetches all {@link InsuranceCoverage} rows for the given
     * insurance providers, keyed by {@code (insuranceProviderId, departmentId)}.
     * Coverages with null department are stored under key {@code null}.
     */
    public Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetchPatientShareCoverages(
        Set<UUID> insuranceProviderIds
    ) {
        if (insuranceProviderIds == null || insuranceProviderIds.isEmpty()) {
            return Map.of();
        }
        List<InsuranceCoverage> coverages = coverageRepository
            .findByInsuranceProviderIdIn(insuranceProviderIds);
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> result = new HashMap<>();
        for (InsuranceCoverage c : coverages) {
            UUID providerId = c.getInsuranceProvider().getId();
            UUID deptId = c.getDepartment() != null ? c.getDepartment().getId() : null;
            result.computeIfAbsent(providerId, k -> new HashMap<>())
                  .computeIfAbsent(deptId, k -> new ArrayList<>())
                  .add(c);
        }
        return result;
    }

    /**
     * Resolves the patient share percentage for a billing line.
     *
     * <p>Resolution chain (most specific wins):
     * <ol>
     *   <li><b>Per-line override</b> — only accepted when no coverage blocks it.</li>
     *   <li><b>Coverage: dept + encounterType</b> — exact match.</li>
     *   <li><b>Coverage: dept only</b> — department-level.</li>
     *   <li><b>Coverage: encounterType only</b> — encounter-type-level.</li>
     *   <li><b>Base coverage</b> — provider-wide (no conditions).</li>
     *   <li><b>Patient default</b> — {@code PatientInsurance.patientSharePercentage}.</li>
     *   <li><b>0</b> — insurance covers everything.</li>
     * </ol>
     */
    public ResolvedPatientShare resolvePatientSharePercentage(
        PatientInsurance appliedInsurance,
        UUID departmentId,
        EncounterType encounterType,
        Integer perLineOverride,
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetchedCoverages
    ) {
        if (appliedInsurance == null) {
            return new ResolvedPatientShare(0, PatientShareSource.PROVIDER_DEFAULT);
        }

        UUID providerId = appliedInsurance.getInsuranceProvider().getId();

        // Layer 1: per-line override — only accepted when no coverage blocks it
        if (perLineOverride != null) {
            if (isOverrideAllowed(providerId, departmentId, encounterType, prefetchedCoverages)) {
                int clamped = Math.max(0, Math.min(100, perLineOverride));
                return new ResolvedPatientShare(clamped, PatientShareSource.OVERRIDE);
            }
            // Override blocked — fall through to coverage resolution
        }

        // Layer 2: coverage-based resolution (most specific wins)
        Integer coveragePct = lookupCoveragePercentage(providerId, departmentId, encounterType, prefetchedCoverages);
        if (coveragePct != null) {
            return new ResolvedPatientShare(coveragePct, PatientShareSource.RULE);
        }

        // Layer 3: patient-specific default
        Integer patientDefault = appliedInsurance.getPatientSharePercentage();
        if (patientDefault != null) {
            return new ResolvedPatientShare(patientDefault, PatientShareSource.PATIENT_DEFAULT);
        }

        // Layer 4: provider base coverage (from InsuranceProvider.getCoverages())
        Integer providerDefault = appliedInsurance.getInsuranceProvider().getBasePatientSharePercentage();
        if (providerDefault != null) {
            return new ResolvedPatientShare(providerDefault, PatientShareSource.PROVIDER_DEFAULT);
        }

        // Layer 5: zero = insurance covers everything
        return new ResolvedPatientShare(0, PatientShareSource.PROVIDER_DEFAULT);
    }

    /**
     * Determines whether a per-line override is allowed.
     */
    private boolean isOverrideAllowed(
        UUID providerId,
        UUID departmentId,
        EncounterType encounterType,
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetchedCoverages
    ) {
        Map<UUID, List<InsuranceCoverage>> byDept = prefetchedCoverages != null
            ? prefetchedCoverages.get(providerId)
            : null;

        if (byDept == null || byDept.isEmpty()) {
            return true;
        }

        boolean hasDeptMatch = false;
        boolean hasEncounterMatch = false;
        boolean hasExactMatch = false;

        for (Map.Entry<UUID, List<InsuranceCoverage>> entry : byDept.entrySet()) {
            UUID covDeptId = entry.getKey();
            for (InsuranceCoverage cov : entry.getValue()) {
                EncounterType covEnc = cov.getEncounterType();

                if (deptMatches(departmentId, covDeptId) && encounterType != null && encounterType.equals(covEnc)) {
                    hasExactMatch = true;
                }
                if (deptMatches(departmentId, covDeptId)) {
                    hasDeptMatch = true;
                }
                if (covEnc != null && encounterType != null && encounterType.equals(covEnc)) {
                    hasEncounterMatch = true;
                }
            }
        }

        if (hasExactMatch) return false;
        if (hasDeptMatch || hasEncounterMatch) return true;
        return false;
    }

    private boolean deptMatches(UUID departmentId, UUID covDeptId) {
        if (departmentId == null && covDeptId == null) return true;
        if (departmentId == null || covDeptId == null) return false;
        return departmentId.equals(covDeptId);
    }

    /**
     * Looks up the most specific coverage percentage for the given provider/department/encounter type.
     */
    private Integer lookupCoveragePercentage(
        UUID providerId,
        UUID departmentId,
        EncounterType encounterType,
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetchedCoverages
    ) {
        Map<UUID, List<InsuranceCoverage>> byDept = prefetchedCoverages != null
            ? prefetchedCoverages.get(providerId)
            : null;

        if (byDept == null) {
            return null;
        }

        // 1. Exact match: provider + dept + encounterType
        if (departmentId != null && encounterType != null) {
            List<InsuranceCoverage> deptCoverages = byDept.get(departmentId);
            if (deptCoverages != null) {
                for (InsuranceCoverage cov : deptCoverages) {
                    if (encounterType.equals(cov.getEncounterType())) {
                        return cov.getPatientSharePercentage();
                    }
                }
            }
        }

        // 2. Department-level: provider + dept, encounterType = null
        if (departmentId != null) {
            List<InsuranceCoverage> deptCoverages = byDept.get(departmentId);
            if (deptCoverages != null) {
                for (InsuranceCoverage cov : deptCoverages) {
                    if (cov.getEncounterType() == null) {
                        return cov.getPatientSharePercentage();
                    }
                }
            }
        }

        // 3. Encounter-type-level: provider + encounterType, dept = null
        if (encounterType != null) {
            List<InsuranceCoverage> nullDeptCoverages = byDept.get(null);
            if (nullDeptCoverages != null) {
                for (InsuranceCoverage cov : nullDeptCoverages) {
                    if (encounterType.equals(cov.getEncounterType())) {
                        return cov.getPatientSharePercentage();
                    }
                }
            }
        }

        // 4. Base: provider, dept = null, encounterType = null
        List<InsuranceCoverage> providerCoverages = byDept.get(null);
        if (providerCoverages != null) {
            for (InsuranceCoverage cov : providerCoverages) {
                if (cov.getEncounterType() == null) {
                    return cov.getPatientSharePercentage();
                }
            }
        }

        return null;
    }

    /** Resolves the insurance that applies to this product line. */
    public PatientInsurance resolveAppliedInsurance(
        VisitDepartmentProduct item,
        CoverageType coverageType,
        UUID requestedPatientInsuranceId,
        Set<UUID> visitInsurancePatientInsuranceIds,
        List<VisitInsurance> visitInsurances
    ) {
        return resolveAppliedInsurance(item, coverageType, requestedPatientInsuranceId,
            visitInsurancePatientInsuranceIds, visitInsurances, null);
    }

    /** Resolves the insurance that applies to this product line, using pre-fetched coverage data. */
    public PatientInsurance resolveAppliedInsurance(
        VisitDepartmentProduct item,
        CoverageType coverageType,
        UUID requestedPatientInsuranceId,
        Set<UUID> visitInsurancePatientInsuranceIds,
        List<VisitInsurance> visitInsurances,
        Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetchedCoverages
    ) {
        if (coverageType == CoverageType.PRIVATE) {
            return null;
        }
        if (coverageType != CoverageType.INSURANCE) {
            return null;
        }
        if (requestedPatientInsuranceId == null) {
            return null;
        }
        if (!visitInsurancePatientInsuranceIds.contains(requestedPatientInsuranceId)) {
            return null;
        }
        PatientInsurance insurance = resolvePatientInsuranceFromVisitInsurances(
            requestedPatientInsuranceId, visitInsurances
        );
        if (insurance == null) {
            return null;
        }
        if (insurance.isDeactivated()) {
            return null;
        }
        LocalDate today = LocalDate.now();
        if (insurance.getValidFrom() != null && insurance.getValidFrom().isAfter(today)) {
            return null;
        }
        if (insurance.getValidUntil() != null && insurance.getValidUntil().isBefore(today)) {
            return null;
        }
        ProductInsuranceCoverage coverage = lookupProductCoverage(
            item.getProduct().getId(),
            insurance.getInsuranceProvider().getId(),
            prefetchedCoverages
        );
        if (coverage == null || !coverage.isCovered()) {
            return null;
        }
        return insurance;
    }

    private PatientInsurance resolvePatientInsuranceFromVisitInsurances(
        UUID patientInsuranceId,
        List<VisitInsurance> visitInsurances
    ) {
        if (visitInsurances != null) {
            for (VisitInsurance vi : visitInsurances) {
                if (vi.getPatientInsurance() != null &&
                    vi.getPatientInsurance().getId().equals(patientInsuranceId)) {
                    return vi.getPatientInsurance();
                }
            }
        }
        return patientInsuranceRepository.findById(patientInsuranceId).orElse(null);
    }

    public BigDecimal resolveDefaultUnitPrice(
        VisitDepartmentProduct item,
        PatientInsurance appliedInsurance
    ) {
        return resolveDefaultUnitPrice(item, appliedInsurance, null);
    }

    public BigDecimal resolveDefaultUnitPrice(
        VisitDepartmentProduct item,
        PatientInsurance appliedInsurance,
        Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetchedCoverages
    ) {
        if (appliedInsurance != null) {
            ProductInsuranceCoverage coverage = lookupProductCoverage(
                item.getProduct().getId(),
                appliedInsurance.getInsuranceProvider().getId(),
                prefetchedCoverages
            );

            if (coverage == null) {
                throw new IllegalArgumentException(
                    "Insurance coverage cost not found for product: " +
                    item.getProduct().getName() +
                    " (" + item.getProduct().getCode() + ")"
                );
            }
            if (coverage.isNotPaid()) {
                return MoneyUtils.ZERO;
            }
            if (coverage.getCost() == null) {
                throw new IllegalArgumentException(
                    "Insurance coverage cost not found for product: " +
                    item.getProduct().getName() +
                    " (" + item.getProduct().getCode() + ")"
                );
            }
            return MoneyUtils.toMoney(coverage.getCost());
        }

        Product product = item.getProduct();
        if (product.isNotPaid()) {
            return MoneyUtils.ZERO;
        }
        if (product.getClinicPrice() != null) {
            return MoneyUtils.toMoney(product.getClinicPrice());
        }
        if (product.getPrivateRhicPrice() != null) {
            return MoneyUtils.toMoney(product.getPrivateRhicPrice());
        }
        return MoneyUtils.ZERO;
    }

    /**
     * Calculates the insurance-covered amount using the resolved patient share percentage.
     */
    public BigDecimal calculateCoveredAmount(
        VisitDepartmentProduct item,
        PatientInsurance appliedInsurance,
        BigDecimal quantity,
        BigDecimal lineTotal,
        int resolvedPatientSharePct
    ) {
        return calculateCoveredAmount(item, appliedInsurance, quantity, lineTotal,
            resolvedPatientSharePct, null);
    }

    /** Backward-compatible overload: resolves percentage from base coverage. */
    public BigDecimal calculateCoveredAmount(
        VisitDepartmentProduct item,
        PatientInsurance appliedInsurance,
        BigDecimal quantity,
        BigDecimal lineTotal
    ) {
        if (appliedInsurance == null) {
            return MoneyUtils.ZERO;
        }
        Integer pct = appliedInsurance.getInsuranceProvider().getBasePatientSharePercentage();
        int resolvedPct = (pct != null) ? pct : 0;
        return calculateCoveredAmount(item, appliedInsurance, quantity, lineTotal,
            resolvedPct, null);
    }

    /** Backward-compatible overload with prefetched coverages. */
    public BigDecimal calculateCoveredAmount(
        VisitDepartmentProduct item,
        PatientInsurance appliedInsurance,
        BigDecimal quantity,
        BigDecimal lineTotal,
        Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetchedCoverages
    ) {
        if (appliedInsurance == null) {
            return MoneyUtils.ZERO;
        }
        Integer pct = appliedInsurance.getInsuranceProvider().getBasePatientSharePercentage();
        int resolvedPct = (pct != null) ? pct : 0;
        return calculateCoveredAmount(item, appliedInsurance, quantity, lineTotal,
            resolvedPct, prefetchedCoverages);
    }

    /**
     * Core coverage calculation using an explicit resolved patient share percentage.
     *
     * @param resolvedPatientSharePct the patient's share (0-100); insurance covers (100 - pct)%
     */
    public BigDecimal calculateCoveredAmount(
        VisitDepartmentProduct item,
        PatientInsurance appliedInsurance,
        BigDecimal quantity,
        BigDecimal lineTotal,
        int resolvedPatientSharePct,
        Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetchedCoverages
    ) {
        if (appliedInsurance == null) {
            return MoneyUtils.ZERO;
        }

        ProductInsuranceCoverage coverage = lookupProductCoverage(
            item.getProduct().getId(),
            appliedInsurance.getInsuranceProvider().getId(),
            prefetchedCoverages
        );
        if (coverage == null || !coverage.isCovered()) {
            return MoneyUtils.ZERO;
        }

        if (coverage.isNotPaid()) {
            return MoneyUtils.ZERO;
        }

        BigDecimal coverageAmount = MoneyUtils.ZERO;
        if (coverage.getCost() != null) {
            BigDecimal coverageCostTotal = MoneyUtils.toMoney(coverage.getCost().multiply(quantity));
            if (resolvedPatientSharePct > 0 && resolvedPatientSharePct < 100) {
                BigDecimal insuranceSharePct = BigDecimal.valueOf(100 - resolvedPatientSharePct);
                coverageAmount = MoneyUtils.toMoney(
                    coverageCostTotal.multiply(insuranceSharePct).divide(
                        BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP
                    )
                );
            } else if (resolvedPatientSharePct == 0) {
                coverageAmount = coverageCostTotal;
            }
        } else {
            if (resolvedPatientSharePct > 0 && resolvedPatientSharePct < 100) {
                BigDecimal insuranceSharePct = BigDecimal.valueOf(100 - resolvedPatientSharePct);
                coverageAmount = MoneyUtils.toMoney(
                    lineTotal.multiply(insuranceSharePct).divide(
                        BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP
                    )
                );
            } else if (resolvedPatientSharePct == 0) {
                coverageAmount = lineTotal;
            }
        }

        if (coverageAmount.compareTo(lineTotal) > 0) {
            coverageAmount = lineTotal;
        }

        return MoneyUtils.toMoney(coverageAmount);
    }

    private ProductInsuranceCoverage lookupProductCoverage(
        UUID productId,
        UUID insuranceProviderId,
        Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetchedCoverages
    ) {
        if (prefetchedCoverages != null) {
            Map<UUID, ProductInsuranceCoverage> byProvider = prefetchedCoverages.get(productId);
            if (byProvider != null) {
                ProductInsuranceCoverage cached = byProvider.get(insuranceProviderId);
                if (cached != null) {
                    return cached;
                }
            }
        }
        return productInsuranceCoverageRepository
            .findByProductIdAndInsuranceProviderId(productId, insuranceProviderId)
            .orElse(null);
    }

    /**
     * Immutable result of patient share resolution: the percentage and where it came from.
     */
    public record ResolvedPatientShare(int percentage, PatientShareSource source) {}
}

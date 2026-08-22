package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.InsuranceCoverageRule;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.model.CoverageType;
import com.nexxserve.nexxclinic.model.EncounterType;
import com.nexxserve.nexxclinic.model.PatientShareSource;
import com.nexxserve.nexxclinic.repository.InsuranceCoverageRuleRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
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
 * <p>Patient share resolution uses a 3-layer chain (most specific wins):
 * <ol>
 *   <li>Per-line override (from billing input)</li>
 *   <li>{@link InsuranceCoverageRule} matching (provider + department + encounter type)</li>
 *   <li>{@code InsuranceProvider.defaultPatientSharePercentage}</li>
 *   <li>0 (insurance covers 100%)</li>
 * </ol>
 */
@Component
public class BillingPricingCalculator {

    private final PatientInsuranceRepository patientInsuranceRepository;
    private final ProductInsuranceCoverageRepository productInsuranceCoverageRepository;
    private final InsuranceCoverageRuleRepository coverageRuleRepository;

    public BillingPricingCalculator(
        PatientInsuranceRepository patientInsuranceRepository,
        ProductInsuranceCoverageRepository productInsuranceCoverageRepository,
        InsuranceCoverageRuleRepository coverageRuleRepository
    ) {
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.productInsuranceCoverageRepository = productInsuranceCoverageRepository;
        this.coverageRuleRepository = coverageRuleRepository;
    }

    /**
     * Pre-fetches all {@link ProductInsuranceCoverage} rows for the given
     * product/provider pairs in a single query.
     */
    public Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetchCoverages(
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
     * Pre-fetches all {@link InsuranceCoverageRule} rows for the given
     * insurance providers, keyed by {@code (insuranceProviderId, departmentId)}.
     * Rules with null department are stored under key {@code null}.
     */
    public Map<UUID, Map<UUID, List<InsuranceCoverageRule>>> prefetchCoverageRules(
        Set<UUID> insuranceProviderIds
    ) {
        if (insuranceProviderIds == null || insuranceProviderIds.isEmpty()) {
            return Map.of();
        }
        List<InsuranceCoverageRule> rules = coverageRuleRepository
            .findByInsuranceProviderIdIn(insuranceProviderIds);
        return rules.stream().collect(
            Collectors.groupingBy(
                r -> r.getInsuranceProvider().getId(),
                Collectors.groupingBy(
                    r -> r.getDepartment() != null ? r.getDepartment().getId() : null,
                    Collectors.toList()
                )
            )
        );
    }

    /**
     * Resolves the patient share percentage for a billing line.
     *
     * <p>Resolution chain (most specific wins):
     * <ol>
     *   <li><b>Per-line override</b> — only accepted when no rule blocks it, i.e.
     *       there is no rule at all, OR a rule matches the department or encounter type
     *       (the override aligns with an existing rule's scope).</li>
     *   <li><b>Rule: dept + encounterType</b> — exact match.</li>
     *   <li><b>Rule: dept only</b> — department-level fallback.</li>
     *   <li><b>Rule: encounterType only</b> — encounter-type-level fallback.</li>
     *   <li><b>Rule: provider-wide</b> — global rule for the provider.</li>
     *   <li><b>Patient default</b> — {@code PatientInsurance.patientSharePercentage}.</li>
     *   <li><b>Provider default</b> — {@code InsuranceProvider.defaultPatientSharePercentage}.</li>
     * </ol>
     *
     * <p>Preference: rules matching specific context (dept or encounter type) are
     * always preferred over global (provider-wide) rules.
     */
    public ResolvedPatientShare resolvePatientSharePercentage(
        PatientInsurance appliedInsurance,
        UUID departmentId,
        EncounterType encounterType,
        Integer perLineOverride,
        Map<UUID, Map<UUID, List<InsuranceCoverageRule>>> prefetchedRules
    ) {
        if (appliedInsurance == null) {
            return new ResolvedPatientShare(0, PatientShareSource.PROVIDER_DEFAULT);
        }

        UUID providerId = appliedInsurance.getInsuranceProvider().getId();

        // Layer 1: per-line override — only accepted when no rule blocks it
        if (perLineOverride != null) {
            if (isOverrideAllowed(providerId, departmentId, encounterType, prefetchedRules)) {
                int clamped = Math.max(0, Math.min(100, perLineOverride));
                return new ResolvedPatientShare(clamped, PatientShareSource.OVERRIDE);
            }
            // Override blocked by a rule — fall through to rule resolution
        }

        // Layer 2: rule-based resolution (most specific wins)
        Integer rulePct = lookupRulePercentage(providerId, departmentId, encounterType, prefetchedRules);
        if (rulePct != null) {
            return new ResolvedPatientShare(rulePct, PatientShareSource.RULE);
        }

        // Layer 3: patient-specific default
        Integer patientDefault = appliedInsurance.getPatientSharePercentage();
        if (patientDefault != null) {
            return new ResolvedPatientShare(patientDefault, PatientShareSource.PATIENT_DEFAULT);
        }

        // Layer 4: provider global default
        Integer providerDefault = appliedInsurance.getInsuranceProvider().getDefaultPatientSharePercentage();
        if (providerDefault != null) {
            return new ResolvedPatientShare(providerDefault, PatientShareSource.PROVIDER_DEFAULT);
        }

        // Layer 5: zero = insurance covers everything
        return new ResolvedPatientShare(0, PatientShareSource.PROVIDER_DEFAULT);
    }

    /**
     * Determines whether a per-line override is allowed. The override is accepted when:
     * <ul>
     *   <li>No rules exist at all for this provider, OR</li>
     *   <li>A rule exists that matches either the department OR the encounter type
     *       (the override aligns with an existing rule's scope).</li>
     * </ul>
     * The override is rejected when there is a rule for a different dept AND a
     * different encounter type — meaning the override would bypass a specific rule.
     */
    private boolean isOverrideAllowed(
        UUID providerId,
        UUID departmentId,
        EncounterType encounterType,
        Map<UUID, Map<UUID, List<InsuranceCoverageRule>>> prefetchedRules
    ) {
        Map<UUID, List<InsuranceCoverageRule>> byDept = prefetchedRules != null
            ? prefetchedRules.get(providerId)
            : null;

        if (byDept == null || byDept.isEmpty()) {
            return true; // no rules at all — override allowed
        }

        boolean hasDeptMatch = false;
        boolean hasEncounterMatch = false;
        boolean hasExactMatch = false;

        for (Map.Entry<UUID, List<InsuranceCoverageRule>> entry : byDept.entrySet()) {
            UUID ruleDeptId = entry.getKey();
            for (InsuranceCoverageRule rule : entry.getValue()) {
                EncounterType ruleEnc = rule.getEncounterType();

                // Check exact match (dept + encounter type)
                if (deptMatches(departmentId, ruleDeptId) && encounterType != null && encounterType.equals(ruleEnc)) {
                    hasExactMatch = true;
                }
                // Check dept match (rule covers this dept, any encounter type)
                if (deptMatches(departmentId, ruleDeptId)) {
                    hasDeptMatch = true;
                }
                // Check encounter type match (rule covers this encounter type, any dept)
                if (ruleEnc != null && encounterType != null && encounterType.equals(ruleEnc)) {
                    hasEncounterMatch = true;
                }
            }
        }

        // If there's an exact match, the override is blocked (rule wins)
        if (hasExactMatch) {
            return false;
        }

        // If there's a partial match (dept OR encounter type), override is allowed
        // (the override aligns with an existing rule's scope)
        if (hasDeptMatch || hasEncounterMatch) {
            return true;
        }

        // No matching rule at all — override is blocked
        // (there are rules, but none for this dept or encounter type)
        return false;
    }

    /** Checks if a rule's department matches the given department. */
    private boolean deptMatches(UUID departmentId, UUID ruleDeptId) {
        if (departmentId == null && ruleDeptId == null) return true;
        if (departmentId == null || ruleDeptId == null) return false;
        return departmentId.equals(ruleDeptId);
    }

    /**
     * Looks up the most specific rule for the given provider/department/encounter type.
     * Resolution order (most specific wins):
     * <ol>
     *   <li>Exact: provider + dept + encounterType</li>
     *   <li>Department: provider + dept, encounterType = null</li>
     *   <li>Encounter type: provider + encounterType, dept = null</li>
     *   <li>Global: provider, dept = null, encounterType = null</li>
     * </ol>
     */
    private Integer lookupRulePercentage(
        UUID providerId,
        UUID departmentId,
        EncounterType encounterType,
        Map<UUID, Map<UUID, List<InsuranceCoverageRule>>> prefetchedRules
    ) {
        Map<UUID, List<InsuranceCoverageRule>> byDept = prefetchedRules != null
            ? prefetchedRules.get(providerId)
            : null;

        if (byDept == null) {
            return null;
        }

        // 1. Exact match: provider + dept + encounterType
        if (departmentId != null && encounterType != null) {
            List<InsuranceCoverageRule> deptRules = byDept.get(departmentId);
            if (deptRules != null) {
                for (InsuranceCoverageRule rule : deptRules) {
                    if (encounterType.equals(rule.getEncounterType())) {
                        return rule.getPatientSharePercentage();
                    }
                }
            }
        }

        // 2. Department-level: provider + dept, encounterType = null
        if (departmentId != null) {
            List<InsuranceCoverageRule> deptRules = byDept.get(departmentId);
            if (deptRules != null) {
                for (InsuranceCoverageRule rule : deptRules) {
                    if (rule.getEncounterType() == null) {
                        return rule.getPatientSharePercentage();
                    }
                }
            }
        }

        // 3. Encounter-type-level: provider + encounterType, dept = null
        if (encounterType != null) {
            List<InsuranceCoverageRule> nullDeptRules = byDept.get(null);
            if (nullDeptRules != null) {
                for (InsuranceCoverageRule rule : nullDeptRules) {
                    if (encounterType.equals(rule.getEncounterType())) {
                        return rule.getPatientSharePercentage();
                    }
                }
            }
        }

        // 4. Global: provider, dept = null, encounterType = null
        List<InsuranceCoverageRule> providerRules = byDept.get(null);
        if (providerRules != null) {
            for (InsuranceCoverageRule rule : providerRules) {
                if (rule.getEncounterType() == null) {
                    return rule.getPatientSharePercentage();
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
        ProductInsuranceCoverage coverage = lookupCoverage(
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
            ProductInsuranceCoverage coverage = lookupCoverage(
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

    /** Backward-compatible overload: resolves percentage from provider default. */
    public BigDecimal calculateCoveredAmount(
        VisitDepartmentProduct item,
        PatientInsurance appliedInsurance,
        BigDecimal quantity,
        BigDecimal lineTotal
    ) {
        if (appliedInsurance == null) {
            return MoneyUtils.ZERO;
        }
        Integer pct = appliedInsurance.getInsuranceProvider().getDefaultPatientSharePercentage();
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
        Integer pct = appliedInsurance.getInsuranceProvider().getDefaultPatientSharePercentage();
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

        ProductInsuranceCoverage coverage = lookupCoverage(
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
                // Patient pays nothing: insurance covers 100%
                coverageAmount = coverageCostTotal;
            }
            // resolvedPatientSharePct == 100: patient pays everything, insurance covers 0
        } else {
            // No explicit cost: fall back to resolved percentage of lineTotal
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

    private ProductInsuranceCoverage lookupCoverage(
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

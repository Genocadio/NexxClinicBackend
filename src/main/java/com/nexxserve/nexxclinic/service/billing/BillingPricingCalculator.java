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
     *   <li><b>Per-line override</b> — only accepted when no exact coverage rule blocks it.</li>
     *   <li><b>Patient-specific tier</b> — {@code PatientInsurance.patientShareCoverage} FK
     *       (or legacy integer column). Per-patient negotiated rates take priority over
     *       generic provider rules. Previously this was Layer 3, causing dept-level rules
     *       to silently override a patient's personal rate.</li>
     *   <li><b>Coverage: dept + encounterType</b> — exact match.</li>
     *   <li><b>Coverage: dept only</b> — department-level.</li>
     *   <li><b>Coverage: encounterType only</b> — encounter-type-level.</li>
     *   <li><b>Base coverage</b> — provider-wide (no conditions).</li>
     *   <li><b>0</b> — insurance covers everything.</li>
     * </ol>
     */
    public ResolvedPatientShare resolvePatientSharePercentage(
        PatientInsurance appliedInsurance,
        UUID departmentId,
        EncounterType encounterType,
        UUID perLineOverrideCoverageId,
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetchedCoverages
    ) {
        if (appliedInsurance == null) {
            return new ResolvedPatientShare(0, PatientShareSource.PROVIDER_DEFAULT);
        }

        UUID providerId = appliedInsurance.getInsuranceProvider().getId();

        // Layer 1: per-line override via InsuranceCoverage ID reference
        // The frontend sends the ID of a specific InsuranceCoverage record;
        // we look it up, validate it belongs to the same provider, and use
        // its percentage — never trusting a raw client-supplied number.
        if (perLineOverrideCoverageId != null) {
            InsuranceCoverage overrideCoverage = resolveOverrideCoverage(
                perLineOverrideCoverageId, providerId, prefetchedCoverages
            );
            // Honor the override only when the tier's own conditions are satisfied
            // by the current billing context. A dept/encounterType-specific tier
            // must not be applied on a visit that doesn't match those conditions.
            if (overrideCoverage != null && isOverrideAllowed(overrideCoverage, departmentId, encounterType)) {
                int clamped = Math.max(0, Math.min(100, overrideCoverage.getPatientSharePercentage()));
                return new ResolvedPatientShare(clamped, PatientShareSource.OVERRIDE);
            }
            // Override rejected (tier conditions not met or coverage not found) —
            // fall through to patient-specific and coverage-rule resolution.
        }

        // Layer 2: patient-specific assigned tier — the FK to an InsuranceCoverage
        // record is a per-patient negotiated rate and takes priority over generic
        // provider coverage rules (Layer 3). Without this ordering, a dept-level
        // rule (e.g. Dental/OUTPATIENT → 20%) would silently override a patient's
        // personal rate (e.g. 10%), causing the billed % to differ from what the
        // frontend displayed. The legacy integer column is a lower-priority fallback
        // for patients who were set up before the FK was introduced.
        if (appliedInsurance.getPatientShareCoverage() != null) {
            int patientCoveragePct = appliedInsurance.getPatientShareCoverage().getPatientSharePercentage();
            return new ResolvedPatientShare(patientCoveragePct, PatientShareSource.PATIENT_DEFAULT);
        }
        if (appliedInsurance.getPatientSharePercentage() != null) {
            return new ResolvedPatientShare(
                appliedInsurance.getPatientSharePercentage(), PatientShareSource.PATIENT_DEFAULT
            );
        }

        // Layer 3: provider coverage rules (most specific wins: dept+type → dept → type → base).
        // These are provider-wide rules that apply to all patients without a personal tier.
        Integer coveragePct = lookupCoveragePercentage(providerId, departmentId, encounterType, prefetchedCoverages);
        if (coveragePct != null) {
            return new ResolvedPatientShare(coveragePct, PatientShareSource.RULE);
        }

        // Layer 4: provider base coverage — try prefetched map first, then lazy-load
        Integer providerDefault = lookupBaseCoveragePercentage(providerId, prefetchedCoverages);
        if (providerDefault == null) {
            // Fallback: lazy-load from the entity (works within @Transactional)
            providerDefault = appliedInsurance.getInsuranceProvider().getBasePatientSharePercentage();
        }
        if (providerDefault != null) {
            return new ResolvedPatientShare(providerDefault, PatientShareSource.PROVIDER_DEFAULT);
        }

        // Layer 5: zero = insurance covers everything
        return new ResolvedPatientShare(0, PatientShareSource.PROVIDER_DEFAULT);
    }

    /**
     * Determines whether a per-line override is allowed for the current billing context.
     *
     * <p>A tier override is valid when the tier's own conditions are satisfied by
     * the billing line's department and encounter type:
     * <ul>
     *   <li>Base tier (no dept, no encounterType) → always valid</li>
     *   <li>Dept-only tier → valid when departmentId matches</li>
     *   <li>EncounterType-only tier → valid when encounterType matches</li>
     *   <li>Dept + encounterType tier → valid only when BOTH match</li>
     * </ul>
     *
     * <p>An override whose tier conditions don’t fit the current context is
     * silently dropped so resolution falls through to the auto-selection chain.
     */
    private boolean isOverrideAllowed(
        InsuranceCoverage overrideTier,
        UUID departmentId,
        EncounterType encounterType
    ) {
        if (overrideTier == null) return false;

        UUID tierDept = overrideTier.getDepartment() != null ? overrideTier.getDepartment().getId() : null;
        EncounterType tierEt = overrideTier.getEncounterType();

        // Base tier — no conditions, always applicable
        if (tierDept == null && tierEt == null) return true;

        // Has a dept condition — must match the billing department
        if (tierDept != null && !tierDept.equals(departmentId)) return false;

        // Has an encounterType condition — must match the visit encounter type
        if (tierEt != null && !tierEt.equals(encounterType)) return false;

        return true;
    }

    private boolean deptMatches(UUID departmentId, UUID covDeptId) {
        if (departmentId == null && covDeptId == null) return true;
        if (departmentId == null || covDeptId == null) return false;
        return departmentId.equals(covDeptId);
    }

    /**
     * Resolves an InsuranceCoverage by its UUID, validating that it belongs
     * to the same insurance provider as the applied insurance. Returns null
     * if the ID is invalid, belongs to a different provider, or the record
     * doesn't exist.
     */
    private InsuranceCoverage resolveOverrideCoverage(
        UUID coverageId,
        UUID providerId,
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetchedCoverages
    ) {
        // Search through prefetched coverages for this ID
        if (prefetchedCoverages != null) {
            Map<UUID, List<InsuranceCoverage>> byDept = prefetchedCoverages.get(providerId);
            if (byDept != null) {
                for (List<InsuranceCoverage> deptCoverages : byDept.values()) {
                    for (InsuranceCoverage cov : deptCoverages) {
                        if (cov.getId().equals(coverageId)) {
                            // Validate: must belong to the same provider
                            if (cov.getInsuranceProvider() != null
                                    && cov.getInsuranceProvider().getId().equals(providerId)) {
                                return cov;
                            }
                            return null; // Different provider — reject
                        }
                    }
                }
            }
        }
        // Not in prefetched map — lazy-load from DB (works within @Transactional)
        return coverageRepository.findById(coverageId)
            .filter(cov -> cov.getInsuranceProvider() != null
                && cov.getInsuranceProvider().getId().equals(providerId))
            .orElse(null);
    }

    /**
     * Looks up the base (unconditional) coverage percentage from the prefetched map.
     */
    private Integer lookupBaseCoveragePercentage(
        UUID providerId,
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetchedCoverages
    ) {
        if (prefetchedCoverages == null) return null;
        Map<UUID, List<InsuranceCoverage>> byDept = prefetchedCoverages.get(providerId);
        if (byDept == null) return null;
        List<InsuranceCoverage> baseCoverages = byDept.get(null);
        if (baseCoverages == null) return null;
        for (InsuranceCoverage cov : baseCoverages) {
            if (cov.getEncounterType() == null) {
                return cov.getPatientSharePercentage();
            }
        }
        return null;
    }

    /**
     * Finds the lowest patient-share % among coverage tiers that are applicable
     * to the current billing context (provider + department + encounterType).
     *
     * <p>A tier is applicable when its own conditions are satisfied:
     * <ul>
     *   <li>Base (no dept, no encounterType) → always applicable; lower priority
     *       than any contextual rule that also applies.</li>
     *   <li>Dept-only → applicable when departmentId matches.</li>
     *   <li>EncounterType-only → applicable when encounterType matches.</li>
     *   <li>Dept + encounterType → applicable only when BOTH match.</li>
     * </ul>
     *
     * <p>Among applicable contextual rules (with at least one condition), the
     * one with the lowest patient-share % wins. If no contextual rule applies,
     * the base tier with the lowest % is returned. This gives the patient the
     * best deal while respecting the insurer’s negotiated structure.
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

        Integer lowestRule = null;  // best (lowest) among contextual rules
        Integer lowestBase = null;  // best (lowest) among base tiers

        for (java.util.Map.Entry<UUID, List<InsuranceCoverage>> entry : byDept.entrySet()) {
            UUID covDeptId = entry.getKey(); // null → no-dept tiers
            for (InsuranceCoverage cov : entry.getValue()) {
                int pct = cov.getPatientSharePercentage();
                boolean isBase = covDeptId == null && cov.getEncounterType() == null;

                if (isBase) {
                    // Base tier (no conditions) — lowest priority
                    if (lowestBase == null || pct < lowestBase) lowestBase = pct;
                    continue;
                }

                // Check whether this tier’s conditions match the current context
                boolean deptOk = covDeptId == null || covDeptId.equals(departmentId);
                boolean etOk   = cov.getEncounterType() == null || cov.getEncounterType().equals(encounterType);

                if (deptOk && etOk) {
                    // Contextual rule that fits — track the lowest %
                    if (lowestRule == null || pct < lowestRule) lowestRule = pct;
                }
            }
        }

        // Contextual rules beat the base tier; within each group the lowest % wins.
        if (lowestRule != null) return lowestRule;
        return lowestBase;
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

package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.InsuranceCoverage;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.model.EncounterType;
import com.nexxserve.nexxclinic.model.PatientShareSource;
import com.nexxserve.nexxclinic.repository.InsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Comprehensive tests for {@link BillingPricingCalculator#resolvePatientSharePercentage}.
 *
 * <p>Covers every layer of the resolution chain:
 * <ol>
 *   <li>Per-line override (when no exact-match coverage blocks it)</li>
 *   <li>Coverage: dept + encounterType (exact match)</li>
 *   <li>Coverage: dept only</li>
 *   <li>Coverage: encounterType only</li>
 *   <li>Base coverage (no conditions)</li>
 *   <li>Patient-specific default ({@code PatientInsurance.patientSharePercentage})</li>
 *   <li>Fallback to 0 (insurance covers everything)</li>
 * </ol>
 */
class BillingPricingCalculatorResolveTest {

    private final PatientInsuranceRepository patientInsuranceRepository =
        mock(PatientInsuranceRepository.class);
    private final ProductInsuranceCoverageRepository productCoverageRepository =
        mock(ProductInsuranceCoverageRepository.class);
    private final InsuranceCoverageRepository coverageRepository =
        mock(InsuranceCoverageRepository.class);
    private final BillingPricingCalculator calculator =
        new BillingPricingCalculator(patientInsuranceRepository, productCoverageRepository, coverageRepository);

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private UUID deptA;
    private UUID deptB;
    private InsuranceProvider provider;
    private PatientInsurance patientInsurance;

    @BeforeEach
    void setUp() {
        deptA = UUID.randomUUID();
        deptB = UUID.randomUUID();

        provider = new InsuranceProvider();
        provider.setId(UUID.randomUUID());
        provider.setCoverages(new ArrayList<>());

        patientInsurance = new PatientInsurance();
        patientInsurance.setId(UUID.randomUUID());
        patientInsurance.setInsuranceProvider(provider);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Department dept(UUID id) {
        Department d = new Department();
        d.setId(id);
        d.setName("Dept-" + id.toString().substring(0, 8));
        return d;
    }

    private InsuranceCoverage baseCoverage(InsuranceProvider p, int pct) {
        InsuranceCoverage cov = new InsuranceCoverage();
        cov.setInsuranceProvider(p);
        cov.setDepartment(null);
        cov.setEncounterType(null);
        cov.setPatientSharePercentage(pct);
        return cov;
    }

    private InsuranceCoverage deptCoverage(InsuranceProvider p, UUID deptId, int pct) {
        InsuranceCoverage cov = new InsuranceCoverage();
        cov.setInsuranceProvider(p);
        cov.setDepartment(dept(deptId));
        cov.setEncounterType(null);
        cov.setPatientSharePercentage(pct);
        return cov;
    }

    private InsuranceCoverage encounterCoverage(InsuranceProvider p, EncounterType et, int pct) {
        InsuranceCoverage cov = new InsuranceCoverage();
        cov.setInsuranceProvider(p);
        cov.setDepartment(null);
        cov.setEncounterType(et);
        cov.setPatientSharePercentage(pct);
        return cov;
    }

    private InsuranceCoverage exactCoverage(InsuranceProvider p, UUID deptId, EncounterType et, int pct) {
        InsuranceCoverage cov = new InsuranceCoverage();
        cov.setInsuranceProvider(p);
        cov.setDepartment(dept(deptId));
        cov.setEncounterType(et);
        cov.setPatientSharePercentage(pct);
        return cov;
    }

    /**
     * Builds a prefetched coverages map keyed by (providerId -> departmentId -> [coverages]).
     * deptId=null covers are stored under the null key.
     */
    private Map<UUID, Map<UUID, List<InsuranceCoverage>>> buildPrefetchedMap(InsuranceCoverage... coverages) {
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> map = new HashMap<>();
        for (InsuranceCoverage c : coverages) {
            UUID pid = c.getInsuranceProvider().getId();
            UUID did = c.getDepartment() != null ? c.getDepartment().getId() : null;
            map.computeIfAbsent(pid, k -> new HashMap<>())
               .computeIfAbsent(did, k -> new ArrayList<>())
               .add(c);
        }
        return map;
    }

    private void addCoveragesToProvider(InsuranceCoverage... coverages) {
        for (InsuranceCoverage c : coverages) {
            provider.addCoverage(c);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Layer 0: null insurance → 0
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void nullInsurance_returnsZero() {
        BillingPricingCalculator.ResolvedPatientShare result =
            calculator.resolvePatientSharePercentage(null, deptA, EncounterType.OUTPATIENT, null, null);

        assertEquals(0, result.percentage());
        assertEquals(PatientShareSource.PROVIDER_DEFAULT, result.source());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Layer 1: Per-line override
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class PerLineOverride {

        @Test
        void overrideAccepted_whenNoCoveragesExist() {
            // No coverages at all → override is always allowed
            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(patientInsurance, deptA, null, 25, null);

            assertEquals(25, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void overrideAccepted_whenDeptOnlyMatchButNoExactMatch() {
            // Provider has dept A coverage but no encounter type → dept-only match
            // means override IS allowed (coverage doesn't fully apply)
            InsuranceCoverage cov = deptCoverage(provider, deptA, 10);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched = buildPrefetchedMap(cov);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(patientInsurance, deptA, EncounterType.OUTPATIENT, 30, prefetched);

            assertEquals(30, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void overrideAccepted_whenEncounterOnlyMatchButNoExactMatch() {
            // Provider has OUTPATIENT coverage but no dept → encounter-only match
            // means override IS allowed (coverage doesn't fully apply)
            InsuranceCoverage cov = encounterCoverage(provider, EncounterType.OUTPATIENT, 10);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched = buildPrefetchedMap(cov);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(patientInsurance, deptA, EncounterType.OUTPATIENT, 30, prefetched);

            assertEquals(30, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void overrideBlocked_whenExactDeptAndEncounterMatch() {
            // Provider has exact match for dept A + OUTPATIENT → override blocked
            InsuranceCoverage cov = exactCoverage(provider, deptA, EncounterType.OUTPATIENT, 10);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched = buildPrefetchedMap(cov);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, 50, prefetched);

            // Override blocked → falls through to coverage resolution → 10%
            assertEquals(10, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void overrideClampedToMax100() {
            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(patientInsurance, deptA, null, 150, null);

            assertEquals(100, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void overrideClampedToMin0() {
            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(patientInsurance, deptA, null, -10, null);

            assertEquals(0, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void overrideAllowed_whenDeptOnlyMatchButNotExact() {
            // Provider has dept A coverage (no encounter type) → dept-only match
            // means override IS allowed (coverage doesn't fully apply)
            InsuranceCoverage cov = deptCoverage(provider, deptA, 20);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched = buildPrefetchedMap(cov);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(patientInsurance, deptA, EncounterType.OUTPATIENT, 50, prefetched);

            assertEquals(50, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void overrideAllowed_whenEncounterOnlyMatchButNotExact() {
            // Provider has OUTPATIENT coverage (no dept) → encounter-only match
            // means override IS allowed (coverage doesn't fully apply)
            InsuranceCoverage cov = encounterCoverage(provider, EncounterType.OUTPATIENT, 15);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched = buildPrefetchedMap(cov);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(patientInsurance, deptA, EncounterType.OUTPATIENT, 40, prefetched);

            assertEquals(40, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void overrideAccepted_whenOnlyBaseCoverageExists_inPrefetchedMap() {
            // Regression: production always passes a populated prefetched map. A provider
            // backfilled with only a base coverage (V16) used to block every override
            // because isOverrideAllowed fell through to false whenever ANY rows existed.
            InsuranceCoverage base = baseCoverage(provider, 20);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched = buildPrefetchedMap(base);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, 30, prefetched);

            assertEquals(30, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void overrideAccepted_whenRulesExistOnlyForAnotherDepartment() {
            // Rules for dept B are unrelated to a dept A line — they must not block.
            InsuranceCoverage otherDept = deptCoverage(provider, deptB, 10);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched = buildPrefetchedMap(otherDept);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, 30, prefetched);

            assertEquals(30, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void overrideAccepted_whenBasePlusExactRuleForOtherDepartment() {
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage otherDept = exactCoverage(provider, deptB, EncounterType.OUTPATIENT, 5);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, otherDept);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, 30, prefetched);

            assertEquals(30, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void overrideStillBlocked_byExactMatch_withPopulatedPrefetchedMap() {
            // Guard: the relaxation must NOT weaken the exact-rule block.
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage exact = exactCoverage(provider, deptA, EncounterType.OUTPATIENT, 10);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, exact);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, 50, prefetched);

            assertEquals(10, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Layer 2: Coverage-based resolution (most specific wins)
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class CoverageResolution {

        @Test
        void exactMatch_deptAndEncounterType_wins() {
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage deptOnly = deptCoverage(provider, deptA, 15);
            InsuranceCoverage encOnly = encounterCoverage(provider, EncounterType.OUTPATIENT, 12);
            InsuranceCoverage exact = exactCoverage(provider, deptA, EncounterType.OUTPATIENT, 8);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, deptOnly, encOnly, exact);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, prefetched);

            assertEquals(8, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void deptOnlyMatch_whenNoExactMatch() {
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage deptOnly = deptCoverage(provider, deptA, 15);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, deptOnly);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.INPATIENT_ADMISSION, null, prefetched);

            assertEquals(15, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void encounterTypeOnlyMatch_whenNoDeptMatch() {
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage encOnly = encounterCoverage(provider, EncounterType.INPATIENT_OBSERVATION, 10);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, encOnly);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptB, EncounterType.INPATIENT_OBSERVATION, null, prefetched);

            assertEquals(10, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void baseCoverage_usedAsLastResortInCoverageResolution() {
            InsuranceCoverage base = baseCoverage(provider, 25);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, prefetched);

            assertEquals(25, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void differentDepartments_getDifferentPercentages() {
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage deptAOnly = deptCoverage(provider, deptA, 10);
            InsuranceCoverage deptBOnly = deptCoverage(provider, deptB, 30);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, deptAOnly, deptBOnly);

            BillingPricingCalculator.ResolvedPatientShare resultA =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, null, null, prefetched);
            assertEquals(10, resultA.percentage());

            BillingPricingCalculator.ResolvedPatientShare resultB =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptB, null, null, prefetched);
            assertEquals(30, resultB.percentage());
        }

        @Test
        void differentEncounterTypes_getDifferentPercentages() {
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage outp = encounterCoverage(provider, EncounterType.OUTPATIENT, 5);
            InsuranceCoverage inpat = encounterCoverage(provider, EncounterType.INPATIENT_ADMISSION, 35);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, outp, inpat);

            BillingPricingCalculator.ResolvedPatientShare resultOutp =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, null, EncounterType.OUTPATIENT, null, prefetched);
            assertEquals(5, resultOutp.percentage());

            BillingPricingCalculator.ResolvedPatientShare resultInpat =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, null, EncounterType.INPATIENT_ADMISSION, null, prefetched);
            assertEquals(35, resultInpat.percentage());
        }

        @Test
        void deptPlusEncounter_overridesDeptOnlyAndEncounterOnly() {
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage deptOnly = deptCoverage(provider, deptA, 15);
            InsuranceCoverage encOnly = encounterCoverage(provider, EncounterType.OUTPATIENT, 12);
            InsuranceCoverage exact = exactCoverage(provider, deptA, EncounterType.OUTPATIENT, 7);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, deptOnly, encOnly, exact);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, prefetched);

            assertEquals(7, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void noDepartmentInBilling_fallsToEncounterTypeOrBase() {
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage encOnly = encounterCoverage(provider, EncounterType.OUTPATIENT, 10);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, encOnly);

            // billing with no department → encounter type match
            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, null, EncounterType.OUTPATIENT, null, prefetched);
            assertEquals(10, result.percentage());

            // billing with no department, no encounter type → base
            BillingPricingCalculator.ResolvedPatientShare resultBase =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, null, null, null, prefetched);
            assertEquals(20, resultBase.percentage());
        }

        @Test
        void emptyPrefetchedMap_returnsNull_forLookup() {
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched = Map.of();

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, prefetched);

            // No coverage found → falls through to patient default or provider base
            // In this case: provider has no base coverage set → 0
            assertEquals(0, result.percentage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Layer 3: Patient-specific default
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class PatientDefault {

        @Test
        void patientDefaultUsed_whenNoCoverageMatches() {
            patientInsurance.setPatientSharePercentage(35);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched = Map.of();

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, prefetched);

            assertEquals(35, result.percentage());
            assertEquals(PatientShareSource.PATIENT_DEFAULT, result.source());
        }

        @Test
        void patientDefaultSkipped_whenCoverageMatches() {
            patientInsurance.setPatientSharePercentage(35);
            InsuranceCoverage base = baseCoverage(provider, 15);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, prefetched);

            // Coverage base wins over patient default
            assertEquals(15, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void patientDefault_nullFallsThrough() {
            patientInsurance.setPatientSharePercentage(null);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched = Map.of();

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, null, null, prefetched);

            // No patient default, no coverage → falls to provider base or 0
            assertEquals(0, result.percentage());
        }

        @Test
        void patientDefault_beatsProviderBase() {
            patientInsurance.setPatientSharePercentage(40);
            InsuranceCoverage base = baseCoverage(provider, 15);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base);

            // Coverage matches (base) → patient default never reached
            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, null, null, prefetched);
            assertEquals(15, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());

            // No coverage in prefetched → patient default wins over provider entity base
            BillingPricingCalculator.ResolvedPatientShare resultNoCov =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, null, null, Map.of());
            assertEquals(40, resultNoCov.percentage());
            assertEquals(PatientShareSource.PATIENT_DEFAULT, resultNoCov.source());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Layer 4: Provider base coverage
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class ProviderBaseCoverage {

        @Test
        void providerBaseFromPrefetchedMap() {
            InsuranceCoverage base = baseCoverage(provider, 20);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, prefetched);

            assertEquals(20, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void providerBaseFromLazyLoadedEntity_whenPrefetchedEmpty() {
            // No coverage in prefetched map → falls to lazy-load from entity
            InsuranceCoverage base = baseCoverage(provider, 20);
            addCoveragesToProvider(base);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, Map.of());

            assertEquals(20, result.percentage());
            assertEquals(PatientShareSource.PROVIDER_DEFAULT, result.source());
        }

        @Test
        void providerBaseFromLazyLoadedEntity_whenPrefetchedNull() {
            InsuranceCoverage base = baseCoverage(provider, 18);
            addCoveragesToProvider(base);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, null);

            assertEquals(18, result.percentage());
            assertEquals(PatientShareSource.PROVIDER_DEFAULT, result.source());
        }

        @Test
        void providerBaseUsed_whenNoPatientDefaultAndNoCoverageMatch() {
            patientInsurance.setPatientSharePercentage(null);
            InsuranceCoverage base = baseCoverage(provider, 22);
            addCoveragesToProvider(base);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, null);

            assertEquals(22, result.percentage());
            assertEquals(PatientShareSource.PROVIDER_DEFAULT, result.source());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Layer 5: Fallback to 0
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class FallbackToZero {

        @Test
        void zeroReturned_whenNoCoverageAndNoPatientDefaultAndNoProviderBase() {
            patientInsurance.setPatientSharePercentage(null);
            // provider has no coverages → getBasePatientSharePercentage() returns null

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, null);

            assertEquals(0, result.percentage());
            assertEquals(PatientShareSource.PROVIDER_DEFAULT, result.source());
        }

        @Test
        void zeroReturned_whenPrefetchedEmptyAndEntityEmpty() {
            patientInsurance.setPatientSharePercentage(null);
            // provider coverages list is empty

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, null, null, Map.of());

            assertEquals(0, result.percentage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Full chain integration: verify precedence order
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class FullChainPrecedence {

        @Test
        void overrideBeatsAll_whenAllowed() {
            patientInsurance.setPatientSharePercentage(50);
            InsuranceCoverage base = baseCoverage(provider, 20);
            addCoveragesToProvider(base);
            // No prefetched coverages → override always allowed

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, 33, null);

            assertEquals(33, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void exactCoverageBeatsDeptOnlyAndBaseAndPatientDefault() {
            patientInsurance.setPatientSharePercentage(50);
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage deptOnly = deptCoverage(provider, deptA, 15);
            InsuranceCoverage encOnly = encounterCoverage(provider, EncounterType.OUTPATIENT, 12);
            InsuranceCoverage exact = exactCoverage(provider, deptA, EncounterType.OUTPATIENT, 7);
            addCoveragesToProvider(base);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, deptOnly, encOnly, exact);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, prefetched);

            assertEquals(7, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void deptOnlyBeatsBaseAndPatientDefault() {
            patientInsurance.setPatientSharePercentage(50);
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage deptOnly = deptCoverage(provider, deptA, 15);
            addCoveragesToProvider(base);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, deptOnly);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, prefetched);

            assertEquals(15, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void encounterOnlyBeatsBaseAndPatientDefault() {
            patientInsurance.setPatientSharePercentage(50);
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage encOnly = encounterCoverage(provider, EncounterType.OUTPATIENT, 12);
            addCoveragesToProvider(base);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, encOnly);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, null, EncounterType.OUTPATIENT, null, prefetched);

            assertEquals(12, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void patientDefaultBeatsProviderBase() {
            patientInsurance.setPatientSharePercentage(30);
            InsuranceCoverage base = baseCoverage(provider, 20);
            addCoveragesToProvider(base);
            // No prefetched coverages → lookupCoveragePercentage returns null
            // Patient default (30) beats provider base from lazy-load (20)

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, Map.of());

            assertEquals(30, result.percentage());
            assertEquals(PatientShareSource.PATIENT_DEFAULT, result.source());
        }

        @Test
        void providerBaseBeatsZero() {
            patientInsurance.setPatientSharePercentage(null);
            InsuranceCoverage base = baseCoverage(provider, 18);
            addCoveragesToProvider(base);
            // No prefetched coverages → no coverage match, no patient default
            // Provider base from lazy-load → 18

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, Map.of());

            assertEquals(18, result.percentage());
            assertEquals(PatientShareSource.PROVIDER_DEFAULT, result.source());
        }

        @Test
        void completeChain_exactMatch_winsOverEverything() {
            // Setup: provider has base 20%, dept-only 15%, encounter-only 12%, exact 5%
            // Patient has default 40%
            // Override of 99% is attempted
            patientInsurance.setPatientSharePercentage(40);
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage deptOnly = deptCoverage(provider, deptA, 15);
            InsuranceCoverage encOnly = encounterCoverage(provider, EncounterType.OUTPATIENT, 12);
            InsuranceCoverage exact = exactCoverage(provider, deptA, EncounterType.OUTPATIENT, 5);
            addCoveragesToProvider(base);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, deptOnly, encOnly, exact);

            // Override blocked by exact match → exact match wins
            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, 99, prefetched);

            assertEquals(5, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void completeChain_noCoverage_patientDefaultWins() {
            patientInsurance.setPatientSharePercentage(45);
            InsuranceCoverage base = baseCoverage(provider, 20);
            addCoveragesToProvider(base);
            // Empty prefetched → no coverage match
            // Patient default (45) beats provider lazy-load (20)

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, Map.of());

            assertEquals(45, result.percentage());
            assertEquals(PatientShareSource.PATIENT_DEFAULT, result.source());
        }

        @Test
        void completeChain_nothingSet_returnsZero() {
            patientInsurance.setPatientSharePercentage(null);
            // provider has no coverages

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, null);

            assertEquals(0, result.percentage());
            assertEquals(PatientShareSource.PROVIDER_DEFAULT, result.source());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Edge cases
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class EdgeCases {

        @Test
        void zeroPatientSharePercentage_isValidAndReturnsZero() {
            InsuranceCoverage base = baseCoverage(provider, 0);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, null, null, prefetched);

            assertEquals(0, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void hundredPatientSharePercentage_returns100() {
            InsuranceCoverage base = baseCoverage(provider, 100);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, null, null, prefetched);

            assertEquals(100, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void providerHasNoCoveragesAtAll_returnsZero() {
            // Provider coverages list is empty → getBasePatientSharePercentage() returns null
            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, EncounterType.OUTPATIENT, null, null);

            assertEquals(0, result.percentage());
        }

        @Test
        void multipleBaseCoverages_returnsFirst() {
            // Edge case: if somehow there are multiple base coverages, first one wins
            InsuranceCoverage base1 = baseCoverage(provider, 15);
            InsuranceCoverage base2 = baseCoverage(provider, 25);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base1, base2);

            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, null, null, prefetched);

            // First base coverage wins (15)
            assertEquals(15, result.percentage());
        }

        @Test
        void coverageWithDifferentDepartmentDoesNotMatch() {
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage deptBOnly = deptCoverage(provider, deptB, 10);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, deptBOnly);

            // Billing for deptA → deptB coverage doesn't match, base wins
            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, null, null, prefetched);

            assertEquals(20, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void coverageWithDifferentEncounterTypeDoesNotMatch() {
            InsuranceCoverage base = baseCoverage(provider, 20);
            InsuranceCoverage inpatObs = encounterCoverage(provider, EncounterType.INPATIENT_OBSERVATION, 10);
            Map<UUID, Map<UUID, List<InsuranceCoverage>>> prefetched =
                buildPrefetchedMap(base, inpatObs);

            // Billing for OUTPATIENT → INPATIENT_OBSERVATION doesn't match, base wins
            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, null, EncounterType.OUTPATIENT, null, prefetched);

            assertEquals(20, result.percentage());
            assertEquals(PatientShareSource.RULE, result.source());
        }

        @Test
        void overrideExactlyAtZero_isAccepted() {
            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, null, 0, null);

            assertEquals(0, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }

        @Test
        void overrideExactlyAt100_isAccepted() {
            BillingPricingCalculator.ResolvedPatientShare result =
                calculator.resolvePatientSharePercentage(
                    patientInsurance, deptA, null, 100, null);

            assertEquals(100, result.percentage());
            assertEquals(PatientShareSource.OVERRIDE, result.source());
        }
    }
}

package com.nexxserve.nexxclinic.service.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.InsuranceCoverage;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.model.EncounterType;
import com.nexxserve.nexxclinic.model.PatientShareSource;
import com.nexxserve.nexxclinic.repository.InsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BillingPricingCalculatorResolveTest {

    private BillingPricingCalculator calculator;
    private InsuranceProvider provider;
    private InsuranceProvider otherProvider;
    private PatientInsurance patientInsurance;
    private Department deptADepartment;
    private Department deptBDepartment;
    private UUID deptA;
    private UUID deptB;

    @BeforeEach
    void setUp() {
        PatientInsuranceRepository pir = mock(PatientInsuranceRepository.class);
        ProductInsuranceCoverageRepository picr = mock(ProductInsuranceCoverageRepository.class);
        InsuranceCoverageRepository icr = mock(InsuranceCoverageRepository.class);
        calculator = new BillingPricingCalculator(pir, picr, icr);

        provider = new InsuranceProvider();
        provider.setId(UUID.randomUUID());

        otherProvider = new InsuranceProvider();
        otherProvider.setId(UUID.randomUUID());

        patientInsurance = new PatientInsurance();
        patientInsurance.setInsuranceProvider(provider);
        patientInsurance.setPatientSharePercentage(null);

        deptA = UUID.randomUUID();
        deptB = UUID.randomUUID();

        deptADepartment = new Department();
        deptADepartment.setId(deptA);
        deptBDepartment = new Department();
        deptBDepartment.setId(deptB);
    }

    // ─── helpers ──────────────────────────────────────────────────────

    private InsuranceCoverage cov(InsuranceProvider prov, Department dept, EncounterType enc, int pct) {
        InsuranceCoverage c = new InsuranceCoverage();
        c.setId(UUID.randomUUID());
        c.setInsuranceProvider(prov);
        c.setDepartment(dept);
        c.setEncounterType(enc);
        c.setPatientSharePercentage(pct);
        return c;
    }

    private InsuranceCoverage baseCov(InsuranceProvider prov, int pct) {
        return cov(prov, null, null, pct);
    }

    private InsuranceCoverage deptCov(InsuranceProvider prov, Department dept, int pct) {
        return cov(prov, dept, null, pct);
    }

    private InsuranceCoverage encCov(InsuranceProvider prov, EncounterType enc, int pct) {
        return cov(prov, null, enc, pct);
    }

    private InsuranceCoverage exactCov(InsuranceProvider prov, Department dept, EncounterType enc, int pct) {
        return cov(prov, dept, enc, pct);
    }

    /** Build the prefetched map from varargs coverages. */
    private Map<UUID, Map<UUID, List<InsuranceCoverage>>> buildPrefetched(InsuranceCoverage... coverages) {
        Map<UUID, Map<UUID, List<InsuranceCoverage>>> map = new HashMap<>();
        for (InsuranceCoverage c : coverages) {
            UUID provId = c.getInsuranceProvider().getId();
            UUID deptId = c.getDepartment() != null ? c.getDepartment().getId() : null;
            map.computeIfAbsent(provId, k -> new HashMap<>())
               .computeIfAbsent(deptId, k -> new ArrayList<>())
               .add(c);
        }
        return map;
    }

    // ═══════════════════════════════════════════════════════════════════
    // 0. No insurance
    // ═══════════════════════════════════════════════════════════════════

    @Test
    void noInsurance_returnsZero() {
        var r = calculator.resolvePatientSharePercentage(null, deptA, EncounterType.OUTPATIENT, null, null);
        assertEquals(0, r.percentage());
        assertEquals(PatientShareSource.PROVIDER_DEFAULT, r.source());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 1. Per-line override via InsuranceCoverage UUID
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class PerLineOverride {

        @Test
        void accepted_whenNoCoveragesExist() {
            InsuranceCoverage oc = cov(provider, null, null, 25);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, oc.getId(), buildPrefetched(oc));
            assertEquals(25, r.percentage());
            assertEquals(PatientShareSource.OVERRIDE, r.source());
        }

        @Test
        void accepted_whenDeptOnlyMatchButNoExactMatch() {
            InsuranceCoverage dc = deptCov(provider, deptADepartment, 10);
            InsuranceCoverage oc = cov(provider, null, null, 30);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, oc.getId(), buildPrefetched(dc, oc));
            assertEquals(30, r.percentage());
            assertEquals(PatientShareSource.OVERRIDE, r.source());
        }

        @Test
        void blocked_whenExactDeptAndEncounterMatch() {
            InsuranceCoverage ec = exactCov(provider, deptADepartment, EncounterType.OUTPATIENT, 10);
            InsuranceCoverage oc = cov(provider, null, null, 50);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, oc.getId(), buildPrefetched(ec, oc));
            assertEquals(10, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }

        @Test
        void clampedToMax100() {
            InsuranceCoverage oc = cov(provider, null, null, 150);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, null, oc.getId(), buildPrefetched(oc));
            assertEquals(100, r.percentage());
            assertEquals(PatientShareSource.OVERRIDE, r.source());
        }

        @Test
        void clampedToMin0() {
            InsuranceCoverage oc = cov(provider, null, null, -10);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, null, oc.getId(), buildPrefetched(oc));
            assertEquals(0, r.percentage());
            assertEquals(PatientShareSource.OVERRIDE, r.source());
        }

        @Test
        void rejected_whenCoverageBelongsToDifferentProvider() {
            InsuranceCoverage oc = cov(otherProvider, null, null, 50);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, oc.getId(), buildPrefetched(oc));
            assertEquals(0, r.percentage());
            assertEquals(PatientShareSource.PROVIDER_DEFAULT, r.source());
        }

        @Test
        void rejected_whenCoverageIdNotFound() {
            UUID fakeId = UUID.randomUUID();
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, fakeId, Map.of());
            assertEquals(0, r.percentage());
            assertEquals(PatientShareSource.PROVIDER_DEFAULT, r.source());
        }

        @Test
        void zeroTakesEffect() {
            InsuranceCoverage oc = cov(provider, null, null, 0);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, oc.getId(), buildPrefetched(oc));
            assertEquals(0, r.percentage());
            assertEquals(PatientShareSource.OVERRIDE, r.source());
        }

        @Test
        void exact100TakesEffect() {
            InsuranceCoverage oc = cov(provider, null, null, 100);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, oc.getId(), buildPrefetched(oc));
            assertEquals(100, r.percentage());
            assertEquals(PatientShareSource.OVERRIDE, r.source());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. Coverage-based resolution
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class CoverageResolution {

        @Test
        void exactDeptAndEncounter() {
            InsuranceCoverage c = exactCov(provider, deptADepartment, EncounterType.OUTPATIENT, 8);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, null, buildPrefetched(c));
            assertEquals(8, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }

        @Test
        void deptOnly_matchesDept() {
            InsuranceCoverage c = deptCov(provider, deptADepartment, 10);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, null, buildPrefetched(c));
            assertEquals(10, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }

        @Test
        void noMatchingDept_usesBase() {
            InsuranceCoverage c = baseCov(provider, 25);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptB, EncounterType.OUTPATIENT, null, buildPrefetched(c));
            assertEquals(25, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }

        @Test
        void multipleDepts_eachGetsOwnRate() {
            InsuranceCoverage cA = deptCov(provider, deptADepartment, 10);
            InsuranceCoverage cB = deptCov(provider, deptBDepartment, 30);
            var map = buildPrefetched(cA, cB);
            assertEquals(10, calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, null, null, map).percentage());
            assertEquals(30, calculator.resolvePatientSharePercentage(
                patientInsurance, deptB, null, null, map).percentage());
        }

        @Test
        void encounterOnly_matchesEncounter() {
            InsuranceCoverage c = encCov(provider, EncounterType.OUTPATIENT, 5);
            var map = buildPrefetched(c);
            assertEquals(5, calculator.resolvePatientSharePercentage(
                patientInsurance, null, EncounterType.OUTPATIENT, null, map).percentage());
            assertEquals(0, calculator.resolvePatientSharePercentage(
                patientInsurance, null, EncounterType.INPATIENT_ADMISSION, null, map).percentage());
        }

        @Test
        void noMatchingCoverage_fallsThrough() {
            InsuranceCoverage c = exactCov(provider, deptADepartment, EncounterType.OUTPATIENT, 7);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptB, EncounterType.INPATIENT_ADMISSION, null, buildPrefetched(c));
            assertEquals(0, r.percentage());
        }

        @Test
        void exactWinsOverBase() {
            InsuranceCoverage base = baseCov(provider, 20);
            InsuranceCoverage exact = exactCov(provider, deptADepartment, EncounterType.OUTPATIENT, 5);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, null, buildPrefetched(base, exact));
            assertEquals(5, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. Patient default fallback
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class PatientDefault {

        @Test
        void usedWhenNoCoverageMatches() {
            patientInsurance.setPatientSharePercentage(35);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, null, Map.of());
            assertEquals(35, r.percentage());
            assertEquals(PatientShareSource.PATIENT_DEFAULT, r.source());
        }

        @Test
        void coverageWinsOverPatientDefault() {
            patientInsurance.setPatientSharePercentage(35);
            InsuranceCoverage c = exactCov(provider, deptADepartment, EncounterType.OUTPATIENT, 15);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, null, buildPrefetched(c));
            assertEquals(15, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. Provider default fallback
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class ProviderDefault {

        @Test
        void usedWhenNothingElseMatches() {
            provider.addCoverage(baseCov(provider, 20));
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, null, Map.of());
            assertEquals(20, r.percentage());
            assertEquals(PatientShareSource.PROVIDER_DEFAULT, r.source());
        }

        @Test
        void zeroWhenNothingSet() {
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, null, Map.of());
            assertEquals(0, r.percentage());
            assertEquals(PatientShareSource.PROVIDER_DEFAULT, r.source());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. Override + coverage interaction
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class OverrideAndCoverageInteraction {

        @Test
        void overrideWinsOverCoverage_whenAllowed() {
            InsuranceCoverage dc = deptCov(provider, deptADepartment, 10);
            InsuranceCoverage oc = cov(provider, null, null, 40);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, oc.getId(), buildPrefetched(dc, oc));
            assertEquals(40, r.percentage());
            assertEquals(PatientShareSource.OVERRIDE, r.source());
        }

        @Test
        void overrideBlockedByExactMatch_fallsToCoverage() {
            InsuranceCoverage ec = exactCov(provider, deptADepartment, EncounterType.OUTPATIENT, 7);
            InsuranceCoverage oc = cov(provider, null, null, 99);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, oc.getId(), buildPrefetched(ec, oc));
            assertEquals(7, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }

        @Test
        void wrongProviderOverride_fallsToCoverage() {
            InsuranceCoverage ec = exactCov(provider, deptADepartment, EncounterType.OUTPATIENT, 12);
            InsuranceCoverage oc = cov(otherProvider, null, null, 50);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, oc.getId(), buildPrefetched(ec, oc));
            assertEquals(12, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }

        @Test
        void patientDefaultWinsOverProviderDefault() {
            patientInsurance.setPatientSharePercentage(30);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, null, Map.of());
            assertEquals(30, r.percentage());
            assertEquals(PatientShareSource.PATIENT_DEFAULT, r.source());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 6. Priority chain
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class PriorityChain {

        @Test
        void coverageWinsOverProviderDefault() {
            InsuranceCoverage base = baseCov(provider, 15);
            provider.addCoverage(baseCov(provider, 20));
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, null, buildPrefetched(base));
            assertEquals(15, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }

        @Test
        void zeroPercentCoverage_treatedAsValidRule() {
            InsuranceCoverage c = exactCov(provider, deptADepartment, EncounterType.OUTPATIENT, 0);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, null, buildPrefetched(c));
            assertEquals(0, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }

        @Test
        void hundredPercentCoverage() {
            InsuranceCoverage c = exactCov(provider, deptADepartment, EncounterType.OUTPATIENT, 100);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, EncounterType.OUTPATIENT, null, buildPrefetched(c));
            assertEquals(100, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }

        @Test
        void nullEncounterType_matchesBaseCoverage() {
            InsuranceCoverage base = baseCov(provider, 20);
            var r = calculator.resolvePatientSharePercentage(
                patientInsurance, deptA, null, null, buildPrefetched(base));
            assertEquals(20, r.percentage());
            assertEquals(PatientShareSource.RULE, r.source());
        }
    }
}

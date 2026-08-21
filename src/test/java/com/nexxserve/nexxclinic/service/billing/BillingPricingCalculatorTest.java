package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import com.nexxserve.nexxclinic.model.CoverageType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Locks in the PRIVATE pricing resolution and the "add as not paid" (notPaid)
 * shortcuts of {@link BillingPricingCalculator}:
 * <ul>
 *   <li>PRIVATE: an explicit clinicPrice wins (even an explicit 0 = free); a
 *       null clinicPrice falls back to privateRhicPrice; neither set -> 0.</li>
 *   <li>notPaid on a product (PRIVATE) or on a coverage (INSURANCE) bills at 0
 *       without any price lookup.</li>
 * </ul>
 */
class BillingPricingCalculatorTest {

    private final PatientInsuranceRepository patientInsuranceRepository =
        mock(PatientInsuranceRepository.class);
    private final ProductInsuranceCoverageRepository coverageRepository =
        mock(ProductInsuranceCoverageRepository.class);
    private final BillingPricingCalculator calculator =
        new BillingPricingCalculator(patientInsuranceRepository, coverageRepository);

    private Product product(BigDecimal clinicPrice, BigDecimal privateRhicPrice, boolean notPaid) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setCode("TEST-001");
        product.setClinicPrice(clinicPrice);
        product.setPrivateRhicPrice(privateRhicPrice);
        product.setNotPaid(notPaid);
        return product;
    }

    private VisitDepartmentProduct item(Product product) {
        VisitDepartmentProduct item = new VisitDepartmentProduct();
        item.setProduct(product);
        return item;
    }

    private InsuranceProvider provider() {
        InsuranceProvider provider = new InsuranceProvider();
        provider.setId(UUID.randomUUID());
        provider.setDefaultCoveragePercentage(15);
        return provider;
    }

    private PatientInsurance insurance(InsuranceProvider provider) {
        PatientInsurance insurance = new PatientInsurance();
        insurance.setId(UUID.randomUUID());
        insurance.setInsuranceProvider(provider);
        return insurance;
    }

    private ProductInsuranceCoverage coverage(InsuranceProvider provider, BigDecimal cost, boolean notPaid) {
        ProductInsuranceCoverage coverage = new ProductInsuranceCoverage();
        coverage.setCost(cost);
        coverage.setCovered(cost != null && cost.compareTo(BigDecimal.ZERO) > 0);
        coverage.setNotPaid(notPaid);
        coverage.setInsuranceProvider(provider);
        return coverage;
    }

    private void stubCoverage(Product product, ProductInsuranceCoverage coverage) {
        when(coverageRepository.findByProductIdAndInsuranceProviderId(
            product.getId(), coverage.getInsuranceProvider().getId()
        )).thenReturn(Optional.of(coverage));
    }

    // ── PRIVATE pricing resolution ──────────────────────────────────────────

    @Test
    void privateUsesExplicitClinicPrice() {
        Product product = product(new BigDecimal("100.00"), new BigDecimal("90.00"), false);
        assertEquals(
            new BigDecimal("100.00"),
            calculator.resolveDefaultUnitPrice(item(product), null)
        );
    }

    @Test
    void privateFallsBackToPrivateRhicPriceWhenClinicPriceUnset() {
        Product product = product(null, new BigDecimal("90.00"), false);
        assertEquals(
            new BigDecimal("90.00"),
            calculator.resolveDefaultUnitPrice(item(product), null)
        );
    }

    @Test
    void privateExplicitZeroWinsOverRhicFallback() {
        // clinicPrice explicitly 0 means "free" — it must NOT fall back to the
        // private RHIC price.
        Product product = product(BigDecimal.ZERO, new BigDecimal("90.00"), false);
        assertEquals(
            new BigDecimal("0.00"),
            calculator.resolveDefaultUnitPrice(item(product), null)
        );
    }

    @Test
    void privateBillsZeroWhenNeitherPriceIsSet() {
        Product product = product(null, null, false);
        assertEquals(
            new BigDecimal("0.00"),
            calculator.resolveDefaultUnitPrice(item(product), null)
        );
    }

    @Test
    void privateNotPaidBillsZeroWithoutPriceLookup() {
        Product product = product(new BigDecimal("100.00"), new BigDecimal("90.00"), true);
        assertEquals(
            new BigDecimal("0.00"),
            calculator.resolveDefaultUnitPrice(item(product), null)
        );
    }

    // ── INSURANCE pricing resolution ────────────────────────────────────────

    @Test
    void insuranceUsesCoverageCost() {
        InsuranceProvider provider = provider();
        Product product = product(null, null, false);
        ProductInsuranceCoverage coverage = coverage(provider, new BigDecimal("500.00"), false);
        stubCoverage(product, coverage);

        assertEquals(
            new BigDecimal("500.00"),
            calculator.resolveDefaultUnitPrice(item(product), insurance(provider))
        );
    }

    @Test
    void insuranceNotPaidCoverageBillsZero() {
        InsuranceProvider provider = provider();
        Product product = product(null, null, false);
        ProductInsuranceCoverage coverage = coverage(provider, new BigDecimal("500.00"), true);
        stubCoverage(product, coverage);

        assertEquals(
            new BigDecimal("0.00"),
            calculator.resolveDefaultUnitPrice(item(product), insurance(provider))
        );
    }

    @Test
    void calculateCoveredAmountIsZeroForNotPaidCoverage() {
        InsuranceProvider provider = provider();
        Product product = product(null, null, false);
        ProductInsuranceCoverage coverage = coverage(provider, new BigDecimal("500.00"), true);
        stubCoverage(product, coverage);

        assertEquals(
            new BigDecimal("0.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                new BigDecimal("2"),
                new BigDecimal("0.00")
            )
        );
    }

    @Test
    void calculateCoveredAmountUsesProviderPercentage() {
        InsuranceProvider provider = provider();
        provider.setDefaultCoveragePercentage(15); // patient pays 15% -> insurer covers 85%
        Product product = product(null, null, false);
        ProductInsuranceCoverage coverage = coverage(provider, new BigDecimal("100.00"), false);
        stubCoverage(product, coverage);

        // line total 200.00 (2 x 100.00); insurer covers 85% = 170.00
        assertEquals(
            new BigDecimal("170.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                new BigDecimal("2"),
                new BigDecimal("200.00")
            )
        );
    }

    // ── I1 fallback: coverage cost is null, uses provider % of lineTotal ──

    @Test
    void calculateCoveredAmountFallbackToProviderPercentageWhenCoverageCostIsNull() {
        InsuranceProvider provider = provider();
        provider.setDefaultCoveragePercentage(20); // patient pays 20% -> insurer covers 80%
        Product product = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, null, false);
        cov.setCovered(true);
        stubCoverage(product, cov);

        // lineTotal 150.00; insurer covers 80% = 120.00
        assertEquals(
            new BigDecimal("120.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                BigDecimal.ONE,
                new BigDecimal("150.00")
            )
        );
    }

    @Test
    void calculateCoveredAmountCappedAtLineTotal() {
        InsuranceProvider provider = provider();
        provider.setDefaultCoveragePercentage(0); // insurer covers 100%
        Product product = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, new BigDecimal("999.00"), false);
        stubCoverage(product, cov);

        // Coverage amount would be 999.00 but lineTotal is only 50.00 — must be capped.
        assertEquals(
            new BigDecimal("50.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                BigDecimal.ONE,
                new BigDecimal("50.00")
            )
        );
    }

    @Test
    void calculateCoveredAmountReturnsZeroForNullInsurance() {
        Product product = product(new BigDecimal("100.00"), null, false);
        assertEquals(
            new BigDecimal("0.00"),
            calculator.calculateCoveredAmount(
                item(product),
                null,
                BigDecimal.ONE,
                new BigDecimal("100.00")
            )
        );
    }

    // ── resolveAppliedInsurance ──────────────────────────────────────────────

    @Test
    void resolveAppliedInsuranceReturnsNullForPrivate() {
        assertNull(calculator.resolveAppliedInsurance(
            item(product(new BigDecimal("100"), null, false)),
            CoverageType.PRIVATE,
            null,
            Set.of(),
            List.of()
        ));
    }

    @Test
    void resolveAppliedInsuranceReturnsNullWhenNotLinkedToVisit() {
        InsuranceProvider provider = provider();
        PatientInsurance pi = insurance(provider);
        // pi is NOT in the visitInsurancePatientInsuranceIds set
        assertNull(calculator.resolveAppliedInsurance(
            item(product(null, null, false)),
            CoverageType.INSURANCE,
            pi.getId(),
            Set.of(), // empty — insurance not linked
            List.of()
        ));
    }

    @Test
    void resolveAppliedInsuranceReturnsNullWhenExpired() {
        InsuranceProvider provider = provider();
        PatientInsurance pi = insurance(provider);
        pi.setValidFrom(LocalDate.now().minusDays(10));
        pi.setValidUntil(LocalDate.now().minusDays(1)); // expired yesterday

        assertNull(calculator.resolveAppliedInsurance(
            item(product(null, null, false)),
            CoverageType.INSURANCE,
            pi.getId(),
            Set.of(pi.getId()),
            List.of()
        ));
    }

    @Test
    void resolveAppliedInsuranceReturnsNullWhenNotYetActive() {
        InsuranceProvider provider = provider();
        PatientInsurance pi = insurance(provider);
        pi.setValidFrom(LocalDate.now().plusDays(5)); // starts in 5 days
        pi.setValidUntil(LocalDate.now().plusDays(30));

        assertNull(calculator.resolveAppliedInsurance(
            item(product(null, null, false)),
            CoverageType.INSURANCE,
            pi.getId(),
            Set.of(pi.getId()),
            List.of()
        ));
    }

    @Test
    void resolveAppliedInsuranceReturnsNullWhenDeactivated() {
        InsuranceProvider provider = provider();
        PatientInsurance pi = insurance(provider);
        pi.setDeactivated(true);
        pi.setValidFrom(LocalDate.now().minusDays(5));
        pi.setValidUntil(LocalDate.now().plusDays(5));

        assertNull(calculator.resolveAppliedInsurance(
            item(product(null, null, false)),
            CoverageType.INSURANCE,
            pi.getId(),
            Set.of(pi.getId()),
            List.of()
        ));
    }

    @Test
    void resolveAppliedInsuranceReturnsNullWhenProductNotCovered() {
        InsuranceProvider provider = provider();
        PatientInsurance pi = insurance(provider);
        pi.setValidFrom(LocalDate.now().minusDays(5));
        pi.setValidUntil(LocalDate.now().plusDays(5));

        Product p = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, new BigDecimal("100"), false);
        cov.setCovered(false); // not covered!
        stubCoverage(p, cov);

        assertNull(calculator.resolveAppliedInsurance(
            item(p),
            CoverageType.INSURANCE,
            pi.getId(),
            Set.of(pi.getId()),
            List.of()
        ));
    }

    @Test
    void resolveAppliedInsuranceResolvesFromVisitInsurancesList() {
        InsuranceProvider provider = provider();
        PatientInsurance pi = insurance(provider);
        pi.setValidFrom(LocalDate.now().minusDays(5));
        pi.setValidUntil(LocalDate.now().plusDays(5));

        com.nexxserve.nexxclinic.entity.VisitInsurance vi =
            new com.nexxserve.nexxclinic.entity.VisitInsurance();
        vi.setPatientInsurance(pi);

        Product p = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, new BigDecimal("100"), false);
        stubCoverage(p, cov);

        PatientInsurance result = calculator.resolveAppliedInsurance(
            item(p),
            CoverageType.INSURANCE,
            pi.getId(),
            Set.of(pi.getId()),
            List.of(vi)
        );
        assertEquals(pi.getId(), result.getId());
    }

    // ── Multi-tier insurance coverage tests ────────────────────────────────────

    @Test
    void calculateCoveredAmountZeroPatientShareCoversAll() {
        // Provider defaultCoveragePercentage = 0 means patient pays 0% -> insurance covers 100%
        InsuranceProvider provider = provider();
        provider.setDefaultCoveragePercentage(0);
        Product product = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, new BigDecimal("200.00"), false);
        stubCoverage(product, cov);

        // lineTotal = 3 x 200.00 = 600.00; insurer covers 100% = 600.00
        assertEquals(
            new BigDecimal("600.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                new BigDecimal("3"),
                new BigDecimal("600.00")
            )
        );
    }

    @Test
    void calculateCoveredAmountFiftyPatientShareCoversHalf() {
        // Provider defaultCoveragePercentage = 50 -> patient pays 50%, insurance covers 50%
        InsuranceProvider provider = provider();
        provider.setDefaultCoveragePercentage(50);
        Product product = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, new BigDecimal("100.00"), false);
        stubCoverage(product, cov);

        // lineTotal = 4 x 100.00 = 400.00; insurer covers 50% = 200.00
        assertEquals(
            new BigDecimal("200.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                new BigDecimal("4"),
                new BigDecimal("400.00")
            )
        );
    }

    @Test
    void calculateCoveredAmountHundredPatientShareCoversNothing() {
        // Provider defaultCoveragePercentage = 100 -> patient pays 100%, insurance covers 0%
        InsuranceProvider provider = provider();
        provider.setDefaultCoveragePercentage(100);
        Product product = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, new BigDecimal("50.00"), false);
        stubCoverage(product, cov);

        // lineTotal = 2 x 50.00 = 100.00; insurer covers 0% = 0.00
        assertEquals(
            new BigDecimal("0.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                new BigDecimal("2"),
                new BigDecimal("100.00")
            )
        );
    }

    @Test
    void calculateCoveredAmountCostMultipliedByQuantity() {
        // Coverage cost is per-unit; insurance covers (100 - pct)% of (cost x qty)
        InsuranceProvider provider = provider();
        provider.setDefaultCoveragePercentage(20); // patient pays 20% -> insurance covers 80%
        Product product = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, new BigDecimal("150.00"), false);
        stubCoverage(product, cov);

        // lineTotal = 5 x 150.00 = 750.00; insurer covers 80% of (150.00 x 5) = 600.00
        assertEquals(
            new BigDecimal("600.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                new BigDecimal("5"),
                new BigDecimal("750.00")
            )
        );
    }

    @Test
    void calculateCoveredAmountPrefetchedCoveragesPath() {
        // Same as basic test but using the pre-fetched coverages map instead of DB fallback
        InsuranceProvider provider = provider();
        provider.setDefaultCoveragePercentage(15);
        Product product = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, new BigDecimal("100.00"), false);

        Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetched = Map.of(
            product.getId(), Map.of(provider.getId(), cov)
        );

        // lineTotal = 2 x 100.00 = 200.00; insurer covers 85% = 170.00
        assertEquals(
            new BigDecimal("170.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                new BigDecimal("2"),
                new BigDecimal("200.00"),
                prefetched
            )
        );
    }

    @Test
    void calculateCoveredAmountCappedAtLineTotalForCostBased() {
        // Coverage cost x quantity > lineTotal -> must be capped at lineTotal
        InsuranceProvider provider = provider();
        provider.setDefaultCoveragePercentage(0); // insurer covers 100%
        Product product = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, new BigDecimal("500.00"), false);
        stubCoverage(product, cov);

        // cost x qty = 500.00 x 3 = 1500.00, but lineTotal is only 200.00
        assertEquals(
            new BigDecimal("200.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                new BigDecimal("3"),
                new BigDecimal("200.00")
            )
        );
    }

    @Test
    void calculateCoveredAmountFractionalRounding() {
        // Test rounding: 100.00 cost x 1 qty, 33% patient share -> 67% covered
        // 100.00 * 67 / 100 = 67.00 (HALF_UP)
        InsuranceProvider provider = provider();
        provider.setDefaultCoveragePercentage(33);
        Product product = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, new BigDecimal("100.00"), false);
        stubCoverage(product, cov);

        assertEquals(
            new BigDecimal("67.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                BigDecimal.ONE,
                new BigDecimal("100.00")
            )
        );
    }

    @Test
    void calculateCoveredAmountNotInPrefetchedFallsBackToDb() {
        // Prefetched map is empty -> falls back to DB lookup via stubCoverage
        InsuranceProvider provider = provider();
        provider.setDefaultCoveragePercentage(10); // patient pays 10% -> insurance covers 90%
        Product product = product(null, null, false);
        ProductInsuranceCoverage cov = coverage(provider, new BigDecimal("200.00"), false);
        stubCoverage(product, cov);

        Map<UUID, Map<UUID, ProductInsuranceCoverage>> prefetched = Map.of(); // empty

        // lineTotal = 2 x 200.00 = 400.00; insurer covers 90% = 360.00
        assertEquals(
            new BigDecimal("360.00"),
            calculator.calculateCoveredAmount(
                item(product),
                insurance(provider),
                new BigDecimal("2"),
                new BigDecimal("400.00"),
                prefetched
            )
        );
    }
}

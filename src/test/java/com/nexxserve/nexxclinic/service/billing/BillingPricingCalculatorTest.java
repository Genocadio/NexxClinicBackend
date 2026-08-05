package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}

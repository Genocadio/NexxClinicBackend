package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolves which patient insurance applies to a product and computes the unit
 * price and the insurance-covered amount for a billing line. Kept separate from
 * the billing orchestration so pricing rules live in one testable place.
 */
@Component
public class BillingPricingCalculator {

    private final PatientInsuranceRepository patientInsuranceRepository;
    private final ProductInsuranceCoverageRepository productInsuranceCoverageRepository;

    public BillingPricingCalculator(
        PatientInsuranceRepository patientInsuranceRepository,
        ProductInsuranceCoverageRepository productInsuranceCoverageRepository
    ) {
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.productInsuranceCoverageRepository = productInsuranceCoverageRepository;
    }

    /**
     * Resolves the insurance that applies to this product:
     * <ul>
     *   <li>If a {@code requestedPatientInsuranceId} is supplied it must be linked
     *       to the visit AND cover the product, otherwise {@code null}.</li>
     *   <li>Otherwise the first visit insurance whose provider covers the product
     *       wins.</li>
     * </ul>
     */
    public PatientInsurance resolveAppliedInsurance(
        VisitDepartmentProduct item,
        UUID requestedPatientInsuranceId,
        Set<UUID> visitInsurancePatientInsuranceIds,
        List<VisitInsurance> visitInsurances
    ) {
        if (requestedPatientInsuranceId != null) {
            if (!visitInsurancePatientInsuranceIds.contains(requestedPatientInsuranceId)) {
                return null;
            }
            Optional<PatientInsurance> insuranceOptional = patientInsuranceRepository.findById(
                requestedPatientInsuranceId
            );
            if (insuranceOptional.isEmpty()) {
                return null;
            }
            PatientInsurance insurance = insuranceOptional.get();
            ProductInsuranceCoverage coverage = productInsuranceCoverageRepository
                .findByProductIdAndInsuranceProviderId(
                    item.getProduct().getId(),
                    insurance.getInsuranceProvider().getId()
                )
                .orElse(null);
            if (coverage == null || !coverage.isCovered()) {
                return null;
            }
            return insurance;
        }

        for (VisitInsurance visitInsurance : visitInsurances) {
            PatientInsurance insurance = visitInsurance.getPatientInsurance();
            ProductInsuranceCoverage coverage = productInsuranceCoverageRepository
                .findByProductIdAndInsuranceProviderId(
                    item.getProduct().getId(),
                    insurance.getInsuranceProvider().getId()
                )
                .orElse(null);
            if (coverage != null && coverage.isCovered()) {
                return insurance;
            }
        }
        return null;
    }

    /**
     * Default unit price for a line when the request does not override it:
     * insurance coverage cost for covered products (throws if missing), otherwise
     * the clinic price, otherwise the private RHIC price, otherwise zero.
     */
    public BigDecimal resolveDefaultUnitPrice(
        VisitDepartmentProduct item,
        PatientInsurance appliedInsurance
    ) {
        if (appliedInsurance != null) {
            ProductInsuranceCoverage coverage = productInsuranceCoverageRepository
                .findByProductIdAndInsuranceProviderId(
                    item.getProduct().getId(),
                    appliedInsurance.getInsuranceProvider().getId()
                )
                .orElse(null);

            if (coverage == null || coverage.getCost() == null) {
                throw new IllegalArgumentException(
                    "Insurance coverage cost not found for product: " +
                    item.getProduct().getName() +
                    " (" + item.getProduct().getCode() + ")"
                );
            }
            return MoneyUtils.toMoney(coverage.getCost());
        }

        Product product = item.getProduct();
        if (product.getClinicPrice() != null) {
            return MoneyUtils.toMoney(product.getClinicPrice());
        }
        if (product.getPrivateRhicPrice() != null) {
            return MoneyUtils.toMoney(product.getPrivateRhicPrice());
        }
        return MoneyUtils.ZERO;
    }

    /**
     * How much of this line the insurance covers. The provider's
     * {@code defaultCoveragePercentage} defines what the PATIENT pays; the
     * insurance covers the remainder of the coverage cost. Falls back to the
     * coverage percentage of the line total when no explicit cost exists, and
     * never exceeds the line total.
     */
    public BigDecimal calculateCoveredAmount(
        VisitDepartmentProduct item,
        PatientInsurance appliedInsurance,
        BigDecimal quantity,
        BigDecimal lineTotal
    ) {
        if (appliedInsurance == null) {
            return MoneyUtils.ZERO;
        }

        Optional<ProductInsuranceCoverage> coverageOptional =
            productInsuranceCoverageRepository.findByProductIdAndInsuranceProviderId(
                item.getProduct().getId(),
                appliedInsurance.getInsuranceProvider().getId()
            );
        if (coverageOptional.isEmpty() || !coverageOptional.get().isCovered()) {
            return MoneyUtils.ZERO;
        }

        ProductInsuranceCoverage coverage = coverageOptional.get();
        BigDecimal coverageAmount = MoneyUtils.ZERO;
        if (coverage.getCost() != null) {
            Integer pct = appliedInsurance.getInsuranceProvider().getDefaultCoveragePercentage();
            BigDecimal coverageCostTotal = MoneyUtils.toMoney(coverage.getCost().multiply(quantity));
            if (pct != null && pct > 0) {
                // pct = the share the PATIENT pays; insurance covers (100 - pct)%.
                BigDecimal patientSharePct = BigDecimal.valueOf(pct);
                BigDecimal insuranceSharePct = BigDecimal.valueOf(100).subtract(patientSharePct);

                coverageAmount = MoneyUtils.toMoney(
                    coverageCostTotal.multiply(insuranceSharePct).divide(
                        BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP
                    )
                );
            } else {
                coverageAmount = coverageCostTotal;
            }
        } else {
            // I1 fix: if explicit coverage cost is missing, fall back to the provider's
            // defaultCoveragePercentage of the lineTotal.
            Integer pct = appliedInsurance.getInsuranceProvider().getDefaultCoveragePercentage();
            if (pct != null && pct > 0) {
                coverageAmount = MoneyUtils.toMoney(
                    lineTotal.multiply(BigDecimal.valueOf(pct)).divide(
                        BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP
                    )
                );
            }
        }

        if (coverageAmount.compareTo(lineTotal) > 0) {
            coverageAmount = lineTotal;
        }

        return MoneyUtils.toMoney(coverageAmount);
    }
}

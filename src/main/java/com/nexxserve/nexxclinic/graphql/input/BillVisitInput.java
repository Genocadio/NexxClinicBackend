package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.CoverageType;
import com.nexxserve.nexxclinic.model.ExemptionType;
import com.nexxserve.nexxclinic.model.OutstandingType;
import com.nexxserve.nexxclinic.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BillVisitInput(
        @NotNull(message = "visitId is required")
        UUID visitId,

        @NotNull(message = "departments is required")
        @Size(min = 1, max = 20, message = "departments must have 1-20 entries")
        @Valid
        List<BillVisitDepartmentInput> departments
) {
    public record BillVisitDepartmentInput(
            @NotNull(message = "visitDepartmentId is required")
            UUID visitDepartmentId,

            @Size(max = 50, message = "Too many products (max 50)")
            @Valid
            List<BillVisitDepartmentProductInput> products,

            @Size(max = 20, message = "Too many payments (max 20)")
            @Valid
            List<BillingPaymentInput> payments,

            String note,

            OutstandingType outstandingType,
            String outstandingReason
    ) {
    }

    public record BillVisitDepartmentProductInput(
            @NotNull(message = "visitDepartmentProductId is required")
            UUID visitDepartmentProductId,
            UUID parentVisitDepartmentId,
            java.math.BigDecimal quantity,
            @NotNull(message = "coverageType is required")
            CoverageType coverageType,
            UUID patientInsuranceId,
            ExemptionType exemptionType,
            Integer patientSharePercentageOverride
    ) {
    }

    public record BillingPaymentInput(
            @NotNull(message = "amount is required")
            @Positive(message = "amount must be positive")
            java.math.BigDecimal amount,
            @NotNull(message = "paymentMethod is required")
            PaymentMethod paymentMethod,
            String reference
    ) {
    }
}

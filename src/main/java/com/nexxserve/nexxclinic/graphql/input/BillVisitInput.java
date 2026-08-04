package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.CoverageType;
import com.nexxserve.nexxclinic.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BillVisitInput(
        @NotNull(message = "visitId is required")
        UUID visitId,

        @Valid
        List<BillVisitDepartmentInput> departments
) {
    public record BillVisitDepartmentInput(
            @NotNull(message = "visitDepartmentId is required")
            UUID visitDepartmentId,

            @Valid
            List<BillVisitDepartmentProductInput> products,

            @Valid
            List<BillingPaymentInput> payments,

            String note
    ) {
    }

    public record BillVisitDepartmentProductInput(
            @NotNull(message = "visitDepartmentProductId is required")
            UUID visitDepartmentProductId,
            UUID parentVisitDepartmentId,
            java.math.BigDecimal quantity,
            CoverageType coverageType,
            UUID patientInsuranceId,
            Boolean isExempted
    ) {
    }

    public record BillingPaymentInput(
            @NotNull(message = "amount is required")
            java.math.BigDecimal amount,
            @NotNull(message = "paymentMethod is required")
            PaymentMethod paymentMethod,
            String reference
    ) {
    }
}

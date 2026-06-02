package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record RecordVisitBillingPaymentInput(
        @NotNull(message = "departmentInsuranceBillingId is required")
        UUID departmentInsuranceBillingId,

        @NotNull(message = "amount is required")
        BigDecimal amount
) {
}

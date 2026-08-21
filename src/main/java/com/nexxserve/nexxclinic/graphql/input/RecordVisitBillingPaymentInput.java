package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record RecordVisitBillingPaymentInput(
        @NotNull(message = "departmentInsuranceBillingId is required")
        UUID departmentInsuranceBillingId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        @NotNull(message = "paymentMethod is required")
        PaymentMethod paymentMethod,
        String reference,
        String note
) {
}

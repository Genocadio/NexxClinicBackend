package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateBillingDateInput(
        @NotNull(message = "departmentInsuranceBillingId is required")
        UUID departmentInsuranceBillingId,

        @NotNull(message = "billingDate is required")
        LocalDateTime billingDate
) {
}

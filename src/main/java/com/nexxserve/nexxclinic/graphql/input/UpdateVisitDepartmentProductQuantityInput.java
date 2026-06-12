package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateVisitDepartmentProductQuantityInput(
        @NotNull(message = "visitDepartmentProductId is required")
        UUID visitDepartmentProductId,

        @NotNull(message = "quantity is required")
        BigDecimal quantity
) {
}

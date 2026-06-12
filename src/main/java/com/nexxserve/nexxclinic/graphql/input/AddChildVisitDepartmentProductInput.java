package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddChildVisitDepartmentProductInput(
        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "quantity is required")
        Double quantity
) {
}

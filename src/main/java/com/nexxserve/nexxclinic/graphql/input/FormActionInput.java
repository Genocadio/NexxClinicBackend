package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FormActionInput(
        @NotBlank(message = "id is required")
        String id,

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "type is required")
        String type,

        @NotNull(message = "quantity is required")
        Integer quantity,

        @NotNull(message = "price is required")
        BigDecimal price,

        @NotNull(message = "isQuantifiable is required")
        Boolean isQuantifiable,

        String backendId
) {
}

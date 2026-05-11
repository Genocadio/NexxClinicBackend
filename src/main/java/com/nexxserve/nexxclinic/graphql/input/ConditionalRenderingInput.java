package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.ConditionalCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConditionalRenderingInput(
        @NotBlank(message = "dependsOn is required")
        String dependsOn,

        @NotNull(message = "condition is required")
        ConditionalCondition condition,

        String value,

        String itemType
) {
}

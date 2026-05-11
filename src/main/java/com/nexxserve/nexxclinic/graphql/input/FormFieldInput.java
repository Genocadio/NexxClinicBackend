package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.FieldType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FormFieldInput(
        @NotBlank(message = "id is required")
        String id,

        @NotBlank(message = "label is required")
        String label,

        @NotNull(message = "type is required")
        FieldType type,

        String placeholder,

        @NotNull(message = "required is required")
        Boolean required,

        @NotNull(message = "order is required")
        Integer order,

        Boolean hideLabel,

        Boolean boldLabel,

        Boolean italicLabel,

        Boolean underlineLabel,

        Boolean centerLabel,

        List<String> options,

        @Valid TableConfigInput tableConfig,

        @Valid ConditionalRenderingInput conditionalRendering
) {
}

package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.ClinicContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClinicContactInput(
        @NotNull(message = "contactType is required")
        ClinicContactType contactType,

        @NotBlank(message = "value is required")
        @Size(max = 200, message = "value must not exceed 200 characters")
        String value,

        @Size(max = 200, message = "description must not exceed 200 characters")
        String description
) {
}
package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;

public record AddVisitVitalSignItemInput(
        @NotBlank(message = "measurementName is required")
        String measurementName,

        @NotBlank(message = "value is required")
        String value,

        @NotBlank(message = "unit is required")
        String unit
) {
}

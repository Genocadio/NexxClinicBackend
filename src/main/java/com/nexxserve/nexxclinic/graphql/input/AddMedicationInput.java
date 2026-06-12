package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;

public record AddMedicationInput(
        @NotBlank(message = "visitDepartmentId is required")
        String visitDepartmentId,

        @NotBlank(message = "medicationName is required")
        String medicationName,

        @NotBlank(message = "instructions is required")
        String instructions
) {
}

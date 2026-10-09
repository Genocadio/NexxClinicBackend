package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;

public record AddSymptomInput(
        @NotBlank(message = "visitDepartmentId is required") String visitDepartmentId,
        @NotBlank(message = "symptomName is required") String symptomName,
        String sonomedId,
        String notes
) {}

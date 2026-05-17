package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;

public record AddDiagnosisInput(
        @NotBlank(message = "visitDepartmentId is required")
        String visitDepartmentId,

        @NotBlank(message = "diagnosisName is required")
        String diagnosisName,

        String icd11Code
) {
}

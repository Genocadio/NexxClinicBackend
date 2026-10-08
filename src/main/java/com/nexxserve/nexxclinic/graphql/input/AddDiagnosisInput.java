package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.VisitDepartmentDiagnosisType;
import jakarta.validation.constraints.NotBlank;

public record AddDiagnosisInput(
        @NotBlank(message = "visitDepartmentId is required")
        String visitDepartmentId,

        @NotBlank(message = "diagnosisName is required")
        String diagnosisName,

        String icd11Code,

        VisitDepartmentDiagnosisType type,

        String notes
) {
    public AddDiagnosisInput(String visitDepartmentId, String diagnosisName, String icd11Code) {
        this(visitDepartmentId, diagnosisName, icd11Code, null, null);
    }
}

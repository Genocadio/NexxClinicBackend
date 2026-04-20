package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;

public record CreateVisitInput(
        @NotNull(message = "patientId is required")
        UUID patientId,

        LocalDate visitDate,

        List<UUID> linkedPatientInsuranceIds,

        @Valid
        List<CreateVisitDepartmentInput> departments
) {
}

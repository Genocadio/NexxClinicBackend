package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;

public record CreateVisitInput(
        @NotNull(message = "patientId is required")
        UUID patientId,

        LocalDateTime visitDate,

        List<UUID> linkedPatientInsuranceIds,

        @Valid
        List<CreateVisitDepartmentInput> departments
) {
}

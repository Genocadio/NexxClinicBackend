package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AddChildVisitDepartmentInput(
        @NotNull(message = "parentVisitDepartmentId is required")
        UUID parentVisitDepartmentId,

        @NotNull(message = "departmentId is required")
        UUID departmentId,

        @NotNull(message = "products are required")
        @Valid
        List<AddChildVisitDepartmentProductInput> products,

        UUID processorId,

        com.nexxserve.nexxclinic.model.EncounterType encounterType
) {
}

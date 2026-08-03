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

        @Valid
        List<AddChildVisitDepartmentProductInput> products,

        /**
         * Optional department profile whose products are auto-added as source=PROFILE.
         * When omitted, the department's default profile (if any) is used. At least one
         * product must exist on the child after creation (explicit + profile), because
         * child departments can never exist with zero products.
         */
        UUID profileId,

        UUID processorId,

        com.nexxserve.nexxclinic.model.EncounterType encounterType
) {
}

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
         * Deprecated/reserved: child departments always support requests, and
         * profiles can only be set on departments that do not support requests — so
         * a profile can never be used here. At least one product is required, since
         * child departments can never exist with zero products.
         */
        UUID profileId,

        UUID processorId,

        com.nexxserve.nexxclinic.model.EncounterType encounterType
) {
}

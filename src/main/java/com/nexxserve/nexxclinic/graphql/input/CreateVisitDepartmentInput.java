package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.EncounterType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateVisitDepartmentInput(
        @NotNull(message = "departmentId is required")
        UUID departmentId,

        EncounterType encounterType,

        /**
         * Optional department profile whose products are auto-added to the visit
         * department as source=PROFILE. When omitted, the department's default
         * profile (if any) is used.
         */
        UUID profileId,

        UUID processorId,

        @Valid
        List<CreateVisitDepartmentProductItemInput> products
) {
    public record CreateVisitDepartmentProductItemInput(
            @NotNull(message = "productId is required")
            UUID productId,

            UUID processorId,

            BigDecimal quantity,

            BigDecimal price,

            VisitProductStatus status
    ) {
    }
}

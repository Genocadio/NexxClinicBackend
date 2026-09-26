package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateVisitDepartmentEncounterDateInput(
        @NotNull(message = "visitDepartmentId is required")
        UUID visitDepartmentId,

        @NotNull(message = "encounterDate is required")
        LocalDateTime encounterDate
) {
}

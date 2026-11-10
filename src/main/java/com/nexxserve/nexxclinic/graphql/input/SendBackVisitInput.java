package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SendBackVisitInput(
        @NotNull(message = "visitId is required")
        UUID visitId,

        @NotNull(message = "fromDepartmentId is required")
        UUID fromDepartmentId,

        UUID targetDepartmentId,

        String note
) {}

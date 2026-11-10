package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SendVisitToNextInput(
        @NotNull(message = "visitId is required")
        UUID visitId,

        @NotNull(message = "fromDepartmentId is required")
        UUID fromDepartmentId,

        @NotNull(message = "targetDepartmentId is required")
        UUID targetDepartmentId,

        String note
) {}

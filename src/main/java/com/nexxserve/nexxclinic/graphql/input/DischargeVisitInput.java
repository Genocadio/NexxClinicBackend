package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DischargeVisitInput(
        @NotNull(message = "visitId is required")
        UUID visitId,

        UUID departmentId,

        String note
) {}

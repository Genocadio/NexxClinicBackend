package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdminSetUserSessionLimitInput(
        @NotNull(message = "userId is required")
        UUID userId,

        @NotNull(message = "maxActiveSessions is required")
        @Min(value = 1, message = "maxActiveSessions must be at least 1")
        Integer maxActiveSessions
) {
}

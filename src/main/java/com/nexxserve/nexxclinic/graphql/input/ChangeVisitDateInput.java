package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChangeVisitDateInput(
        @NotNull(message = "visitId is required")
        UUID visitId,

        @NotNull(message = "visitDate is required")
        LocalDateTime visitDate
) {
}
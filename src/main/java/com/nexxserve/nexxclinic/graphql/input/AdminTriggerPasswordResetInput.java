package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AdminTriggerPasswordResetInput(
	@NotNull(message = "userId is required")
	UUID userId,
	Boolean revokeSessions
) {
}

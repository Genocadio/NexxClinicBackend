package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.RoleName;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record ActivateUserInput(
	@NotNull(message = "userId is required")
	UUID userId,

	@NotEmpty(message = "roles is required")
	Set<RoleName> roles
) {
}

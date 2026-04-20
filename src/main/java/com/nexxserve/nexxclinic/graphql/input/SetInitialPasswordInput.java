package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetInitialPasswordInput(
	@NotBlank(message = "identifier is required")
	String identifier,

	@NotBlank(message = "newPassword is required")
	@Size(min = 8, message = "newPassword must be at least 8 characters")
	String newPassword
) {
}

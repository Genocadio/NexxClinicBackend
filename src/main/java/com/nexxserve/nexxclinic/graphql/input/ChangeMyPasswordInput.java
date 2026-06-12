package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeMyPasswordInput(
	@NotBlank(message = "currentPassword is required")
	String currentPassword,

	@NotBlank(message = "newPassword is required")
	@Size(min = 8, message = "newPassword must be at least 8 characters")
	String newPassword
) {
}

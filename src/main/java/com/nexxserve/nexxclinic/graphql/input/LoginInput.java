package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;

public record LoginInput(
	@NotBlank(message = "identifier is required")
	String identifier,

	@NotBlank(message = "password is required")
	String password
) {
}

package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;

public record RefreshSessionInput(
	@NotBlank(message = "refreshToken is required")
	String refreshToken
) {
}

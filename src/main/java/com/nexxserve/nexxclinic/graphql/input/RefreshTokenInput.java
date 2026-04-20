package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenInput(
        @NotBlank(message = "refreshToken is required")
        String refreshToken
) {
}

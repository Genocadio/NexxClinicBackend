package com.nexxserve.nexxclinic.graphql.input;

public record LogoutInput(String refreshToken, Boolean revokeAllSessions) {
}

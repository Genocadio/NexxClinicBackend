package com.nexxserve.nexxclinic.auth;

import com.nexxserve.nexxclinic.model.RoleName;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AccessTokenInfo(
        UUID userId,
        String principal,
        Set<RoleName> roles,
        String tokenId,
        Instant expiresAt,
        String tokenType
) {
}

package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.RoleName;
import java.util.Set;
import java.util.UUID;

public record LoginResponseDto(
        String accessToken,
        String refreshToken,
        WorkerDto user,
        UUID userId,
        Set<RoleName> roles,
        boolean mustChangeOnNextLogin,
        Integer maxActiveSessions,
        Integer activeSessions
) {}
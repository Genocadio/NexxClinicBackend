package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RoleDto(
        UUID id,
        String name,
        String description,
        boolean isSystem,
        List<PermissionDto> permissions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

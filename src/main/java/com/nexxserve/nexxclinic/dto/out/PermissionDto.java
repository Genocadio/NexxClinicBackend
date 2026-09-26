package com.nexxserve.nexxclinic.dto.out;

import java.util.UUID;

public record PermissionDto(
        UUID id,
        String name,
        String description,
        String module
) {}

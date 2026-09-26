package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record UpdateRoleInput(
        @NotNull(message = "Role ID is required")
        UUID id,
        String name,
        String description,
        List<String> permissionNames
) {}

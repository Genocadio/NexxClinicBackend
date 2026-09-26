package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record AssignPermissionsToRoleInput(
        @NotNull(message = "Role ID is required")
        UUID roleId,
        @NotEmpty(message = "Permission names cannot be empty")
        List<String> permissionNames
) {}

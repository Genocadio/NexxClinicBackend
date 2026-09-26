package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateRoleInput(
        @NotBlank(message = "Role name is required")
        String name,
        String description,
        List<String> permissionNames
) {}

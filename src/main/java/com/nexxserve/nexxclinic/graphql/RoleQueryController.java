package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.RolePermissionService;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class RoleQueryController {

    private final RolePermissionService rolePermissionService;

    public RoleQueryController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse getRoles(
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return ApiResponse.success("Roles fetched successfully.", rolePermissionService.getAllRoles());
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse getRole(
            @Argument UUID id,
            @Argument String name,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        if (id != null) {
            return rolePermissionService.getRoleById(id)
                    .map(r -> ApiResponse.success("Role fetched.", r))
                    .orElseGet(() -> ApiResponse.error("Role not found."));
        }
        if (name != null) {
            return rolePermissionService.getRoleByName(name)
                    .map(r -> ApiResponse.success("Role fetched.", r))
                    .orElseGet(() -> ApiResponse.error("Role not found."));
        }
        return ApiResponse.error("Role ID or name is required.");
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse getPermissions(
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return ApiResponse.success("Permissions fetched successfully.", rolePermissionService.getAllPermissions());
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse roles(
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return getRoles(authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse role(
            @Argument UUID id,
            @Argument String name,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return getRole(id, name, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.MANAGER})
    @QueryMapping
    public ApiResponse permissions(
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return getPermissions(authUser);
    }
}

package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.graphql.input.AssignPermissionsToRoleInput;
import com.nexxserve.nexxclinic.graphql.input.CreateRoleInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateRoleInput;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.RolePermissionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class RoleMutationController {

    private final RolePermissionService rolePermissionService;

    public RoleMutationController(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse createRole(
            @Argument @Valid CreateRoleInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return rolePermissionService.createRole(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse updateRole(
            @Argument @Valid UpdateRoleInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return rolePermissionService.updateRole(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse deleteRole(
            @Argument UUID id,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return rolePermissionService.deleteRole(id, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse assignPermissionsToRole(
            @Argument @Valid AssignPermissionsToRoleInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return rolePermissionService.assignPermissionsToRole(input, authUser);
    }
}

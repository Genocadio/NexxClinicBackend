package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.CreateDepartmentInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateDepartmentInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.DepartmentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class DepartmentMutationController {

    private final DepartmentService departmentService;

    public DepartmentMutationController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse createDepartment(
            @Argument @Valid CreateDepartmentInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentService.createDepartment(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse updateDepartment(
            @Argument UUID departmentId,
            @Argument @Valid UpdateDepartmentInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentService.updateDepartment(departmentId, input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse removeDepartmentProfile(
            @Argument UUID profileId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentService.removeDepartmentProfile(profileId);
    }
}

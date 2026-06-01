package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.SearchDepartmentsInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.DepartmentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class DepartmentQueryController {

    private final DepartmentService departmentService;

    public DepartmentQueryController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE, RoleName.CLINICIAN, RoleName.NURSE})
    @QueryMapping
    public ApiResponse department(
            @Argument UUID departmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentService.department(departmentId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE, RoleName.NURSE, RoleName.CLINICIAN, RoleName.NURSE })
    @QueryMapping
    public ApiResponse departments(
            @Argument @Valid SearchDepartmentsInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentService.departments(input);
    }
}

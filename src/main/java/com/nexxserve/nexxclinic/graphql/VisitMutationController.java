package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.ChangeVisitDateInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentProductInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentProductStatusInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.VisitService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class VisitMutationController {

    private final VisitService visitService;

    public VisitMutationController(VisitService visitService) {
        this.visitService = visitService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse createVisit(
            @Argument @Valid CreateVisitInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.createVisit(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse changeVisitDate(
            @Argument @Valid ChangeVisitDateInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.changeVisitDate(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addVisitDepartment(
            @Argument UUID visitId,
            @Argument UUID departmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.addVisitDepartment(visitId, departmentId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse addVisitDepartmentProduct(
            @Argument @Valid CreateVisitDepartmentProductInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.addVisitDepartmentProduct(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse updateVisitDepartmentProductStatus(
            @Argument @Valid UpdateVisitDepartmentProductStatusInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.updateVisitDepartmentProductStatus(input, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse completeVisit(
            @Argument UUID visitId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.completeVisit(visitId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse cancelVisit(
            @Argument UUID visitId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return visitService.cancelVisit(visitId);
    }
}

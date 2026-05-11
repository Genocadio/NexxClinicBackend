package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.DepartmentFormService;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class DepartmentFormQueryController {

    private final DepartmentFormService departmentFormService;

    public DepartmentFormQueryController(DepartmentFormService departmentFormService) {
        this.departmentFormService = departmentFormService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public ApiResponse getForms(
            @Argument UUID departmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentFormService.getForms(departmentId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public ApiResponse getForm(
            @Argument UUID departmentId,
            @Argument UUID formId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentFormService.getForm(departmentId, formId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public ApiResponse getLatestForm(
            @Argument UUID departmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentFormService.getLatestForm(departmentId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public ApiResponse getFormVersionHistory(
            @Argument UUID departmentId,
            @Argument UUID formId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentFormService.getFormVersionHistory(departmentId, formId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public ApiResponse getFormVersion(
            @Argument UUID departmentId,
            @Argument UUID formId,
            @Argument("version") String versionNumber,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentFormService.getFormVersion(departmentId, formId, versionNumber);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public ApiResponse getConsultationAnswers(
            @Argument UUID consultationId,
            @Argument UUID departmentId,
            @Argument UUID formId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentFormService.getConsultationAnswers(consultationId, departmentId, formId);
    }
}

package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.ConsultationAnswersInput;
import com.nexxserve.nexxclinic.graphql.input.FormInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.DepartmentFormService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class DepartmentFormMutationController {

    private final DepartmentFormService departmentFormService;

    public DepartmentFormMutationController(DepartmentFormService departmentFormService) {
        this.departmentFormService = departmentFormService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse createForm(
            @Argument UUID departmentId,
            @Argument @Valid FormInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentFormService.createForm(departmentId, input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse updateForm(
            @Argument UUID departmentId,
            @Argument UUID formId,
            @Argument @Valid FormInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentFormService.updateForm(departmentId, formId, input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse finalizeForm(
            @Argument UUID departmentId,
            @Argument UUID formId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentFormService.finalizeForm(departmentId, formId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse upsertConsultationAnswers(
            @Argument @Valid ConsultationAnswersInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return departmentFormService.upsertConsultationAnswers(input, authUser);
    }
}

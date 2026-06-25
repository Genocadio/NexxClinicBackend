package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.*;
import com.nexxserve.nexxclinic.entity.StandaloneFormVersion;
import com.nexxserve.nexxclinic.mappers.out.StandaloneFormMapper;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.StandaloneFormService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class StandaloneFormQueryController {

    private final StandaloneFormService formService;
    private final StandaloneFormMapper mapper;

    public StandaloneFormQueryController(StandaloneFormService formService, StandaloneFormMapper mapper) {
        this.formService = formService;
        this.mapper = mapper;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public ApiResponse<List<StandaloneFormDto>> getStandaloneForms(
            @Argument Boolean isTemplate,
            @Argument String category,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return formService.getForms(isTemplate, category);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public ApiResponse<List<DepartmentFormDto>> getDepartmentForms(
            @Argument UUID departmentId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return formService.getDepartmentFormsWithDefault(departmentId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public ApiResponse<StandaloneFormDto> getStandaloneForm(
            @Argument UUID id,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return formService.getForm(id);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public StandaloneFormVersionDto getStandaloneFormVersion(
            @Argument UUID versionId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        StandaloneFormVersion version = formService.getVersion(versionId);
        return version != null ? mapper.toDto(version) : null;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public List<StandaloneFormAnswerDto> getStandaloneAnswers(
            @Argument UUID formId,
            @Argument UUID patientId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return formService.getAnswers(formId, patientId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @QueryMapping
    public ApiResponse<StandaloneFormAnswerDto> getStandaloneAnswer(
            @Argument UUID id,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return formService.getAnswer(id);
    }
}

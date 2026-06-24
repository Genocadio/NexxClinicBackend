package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.StandaloneFormAnswerDto;
import com.nexxserve.nexxclinic.dto.out.StandaloneFormDto;
import com.nexxserve.nexxclinic.dto.out.VisitStandaloneAnswerDto;
import com.nexxserve.nexxclinic.graphql.input.StandaloneFormInput;
import com.nexxserve.nexxclinic.model.AnswerStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.StandaloneFormService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class StandaloneFormMutationController {

    private final StandaloneFormService formService;

    public StandaloneFormMutationController(StandaloneFormService formService) {
        this.formService = formService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse<StandaloneFormDto> createStandaloneForm(
            @Argument @Valid StandaloneFormInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        UUID workerId = authUser != null ? authUser.userId() : null;
        return formService.createForm(input, workerId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse<StandaloneFormDto> updateStandaloneForm(
            @Argument UUID id,
            @Argument @Valid StandaloneFormInput input,
            @Argument Boolean markFinal,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return formService.updateForm(id, input, markFinal);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse<StandaloneFormDto> duplicateStandaloneForm(
            @Argument UUID sourceFormId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        UUID workerId = authUser != null ? authUser.userId() : null;
        return formService.duplicateForm(sourceFormId, workerId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse<Boolean> deleteStandaloneForm(
            @Argument UUID id,
            @Argument Boolean confirmDeleteAnswers,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return formService.deleteForm(id, confirmDeleteAnswers);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse<StandaloneFormAnswerDto> saveStandaloneAnswer(
            @Argument UUID formVersionId,
            @Argument Object answers,
            @Argument AnswerStatus status,
            @Argument Double score,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        UUID workerId = authUser != null ? authUser.userId() : null;
        return formService.saveAnswer(formVersionId, answers, status, score, workerId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse<StandaloneFormAnswerDto> updateStandaloneAnswer(
            @Argument UUID answerId,
            @Argument Object answers,
            @Argument AnswerStatus status,
            @Argument Double score,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return formService.updateAnswer(answerId, answers, status, score);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse<Boolean> deleteStandaloneAnswer(
            @Argument UUID answerId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return formService.deleteAnswer(answerId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse<String> generateStandaloneFormPdf(
            @Argument UUID answerId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        // Placeholder as per requirements
        return ApiResponse.success("PDF generation triggered", "https://example.com/pdf/" + answerId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN})
    @MutationMapping
    public ApiResponse<VisitStandaloneAnswerDto> saveVisitStandaloneAnswer(
            @Argument UUID visitId,
            @Argument UUID departmentId,
            @Argument UUID formVersionId,
            @Argument Object answers,
            @Argument AnswerStatus status,
            @Argument Double score,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        UUID workerId = authUser != null ? authUser.userId() : null;
        return formService.saveVisitStandaloneAnswer(visitId, departmentId, formVersionId, answers, status, score, workerId, authUser);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse<StandaloneFormDto> linkStandaloneFormToDepartment(
            @Argument UUID departmentId,
            @Argument UUID formId
    ) {
        return formService.linkFormToDepartment(departmentId, formId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse<Boolean> unlinkStandaloneFormFromDepartment(
            @Argument UUID departmentId,
            @Argument UUID formId
    ) {
        return formService.unlinkFormFromDepartment(departmentId, formId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse<StandaloneFormDto> setDefaultStandaloneFormForDepartment(
            @Argument UUID departmentId,
            @Argument UUID formId
    ) {
        return formService.setDefaultFormForDepartment(departmentId, formId);
    }
}

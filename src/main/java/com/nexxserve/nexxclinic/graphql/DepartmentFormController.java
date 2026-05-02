package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.entity.AnswerStatus;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.service.DepartmentFormService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class DepartmentFormController {

    @Autowired
    private DepartmentFormService formService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Queries

    @QueryMapping
    public ApiResponse getDepartmentForms(@Argument UUID departmentId) {
        return formService.getFormsByDepartment(departmentId);
    }

    @QueryMapping
    public ApiResponse getDepartmentForm(
        @Argument UUID formId,
        @Argument UUID departmentId
    ) {
        return formService.getForm(formId, departmentId);
    }

    @QueryMapping
    public ApiResponse getDepartmentFormVersions(@Argument UUID formId) {
        return formService.getFormVersions(formId);
    }

    @QueryMapping
    public ApiResponse getDepartmentFormVersion(@Argument UUID versionId) {
        return formService.getFormVersion(versionId);
    }

    @QueryMapping
    public ApiResponse getLatestDepartmentFormVersion(@Argument UUID formId) {
        return formService.getLatestFormVersion(formId);
    }

    @QueryMapping
    public ApiResponse getConsultationAnswers(
        @Argument String consultationId,
        @Argument UUID departmentId,
        @Argument UUID formId
    ) {
        return formService.getConsultationAnswers(consultationId, departmentId, formId);
    }

    // Mutations

    @MutationMapping
    public ApiResponse createDepartmentForm(
        @Argument UUID departmentId,
        @Argument Map<String, Object> input
    ) {
        try {
            String title = (String) input.get("title");
            String description = (String) input.get("description");
            Integer schemaVersion = ((Number) input.getOrDefault("schemaVersion", 1)).intValue();

            // Serialize the input to JSON for formData
            String formData = objectMapper.writeValueAsString(input);

            return formService.createForm(departmentId, title, description, schemaVersion, formData);
        } catch (Exception e) {
            return ApiResponse.error("Failed to create form: " + e.getMessage(), "FORM_CREATE_ERROR");
        }
    }

    @MutationMapping
    public ApiResponse updateDepartmentForm(
        @Argument UUID formId,
        @Argument Map<String, Object> input
    ) {
        try {
            String title = (String) input.get("title");
            String description = (String) input.get("description");

            // Serialize the input to JSON for formData
            String formData = objectMapper.writeValueAsString(input);

            return formService.updateForm(formId, title, description, formData);
        } catch (Exception e) {
            return ApiResponse.error("Failed to update form: " + e.getMessage(), "FORM_UPDATE_ERROR");
        }
    }

    @MutationMapping
    public ApiResponse finalizeDepartmentForm(@Argument UUID formId) {
        return formService.finalizeForm(formId);
    }

    @MutationMapping
    public ApiResponse upsertDepartmentFormAnswers(
        @Argument Map<String, Object> input
    ) {
        try {
            String consultationId = (String) input.get("consultationId");
            String visitId = (String) input.get("visitId");
            String patientId = (String) input.get("patientId");
            UUID departmentId = UUID.fromString((String) input.get("departmentId"));
            UUID formId = UUID.fromString((String) input.get("formId"));
            String answers = (String) input.get("answers");

            // Parse status enum
            AnswerStatus status = null;
            Object statusObj = input.get("status");
            if (statusObj != null) {
                status = AnswerStatus.valueOf(statusObj.toString().toUpperCase());
            }

            return formService.upsertFormAnswer(
                consultationId,
                visitId,
                patientId,
                departmentId,
                formId,
                answers,
                status
            );
        } catch (Exception e) {
            return ApiResponse.error("Failed to upsert answer: " + e.getMessage(), "ANSWER_UPSERT_ERROR");
        }
    }
}

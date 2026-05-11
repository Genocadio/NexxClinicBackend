package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.AnswerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConsultationAnswersInput(
        @NotNull(message = "consultationId is required")
        UUID consultationId,

        @NotNull(message = "visitId is required")
        UUID visitId,

        @NotNull(message = "patientId is required")
        UUID patientId,

        @NotNull(message = "departmentId is required")
        UUID departmentId,

        @NotNull(message = "formId is required")
        UUID formId,

        String formVersion,

        @NotNull(message = "status is required")
        AnswerStatus status,

        @NotBlank(message = "answers is required")
        String answers
) {
}

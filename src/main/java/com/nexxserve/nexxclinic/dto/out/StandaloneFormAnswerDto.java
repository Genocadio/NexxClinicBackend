package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.AnswerStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StandaloneFormAnswerDto(
        UUID id,
        StandaloneFormDto form,
        StandaloneFormVersionDto formVersion,
        Object answers,
        BigDecimal score,
        AnswerStatus status,
        UUID patientId,
        UUID visitId,
        UUID submittedBy,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

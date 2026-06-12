package com.nexxserve.nexxclinic.dto.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VisitPreInstructionMedicationRequestDto(
        UUID id,
        String medName,
        String dosage,
        String route,
        String frequency,
        String duration,
        String quantity,
        String otherInstructions,
        LocalDateTime createdAt
) {}

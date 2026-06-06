package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitDepartmentMedicationDto(
        UUID id,
        String medicationName,
        String instructions,
        LocalDateTime createdAt
) {}

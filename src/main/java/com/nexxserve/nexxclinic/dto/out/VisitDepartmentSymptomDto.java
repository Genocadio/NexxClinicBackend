package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitDepartmentSymptomDto(
        UUID id,
        String symptomName,
        String sonomedId,
        String notes,
        WorkerDto addedBy,
        LocalDateTime createdAt
) {}

package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VisitVitalSignsGroupDto(
        UUID id,
        LocalDateTime createdAt,
        WorkerDto addedBy,
        List<VitalMeasurementDto> measurements
) {}

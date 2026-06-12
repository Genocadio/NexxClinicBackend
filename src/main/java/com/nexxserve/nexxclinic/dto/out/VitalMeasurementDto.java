package com.nexxserve.nexxclinic.dto.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VitalMeasurementDto(
        UUID id,
        String measurementName,
        String value,
        String unit,
        LocalDateTime createdAt
) {}

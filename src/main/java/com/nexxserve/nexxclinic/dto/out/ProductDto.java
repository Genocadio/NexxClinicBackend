package com.nexxserve.nexxclinic.dto.out;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String genericName,
        String code,
        String description,
        Object metadata,
        BigDecimal privateRhicPrice,
        BigDecimal clinicPrice,
        List<ProductInsuranceCoverageDto> insuranceCoverages,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
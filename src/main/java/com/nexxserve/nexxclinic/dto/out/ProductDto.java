package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.ProductUnit;

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
        ProductType type,
        ProductUnit unit,
        Object metadata,
        BigDecimal privateRhicPrice,
        BigDecimal clinicPrice,
        boolean notPaid,
        List<ProductInsuranceCoverageDto> insuranceCoverages,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
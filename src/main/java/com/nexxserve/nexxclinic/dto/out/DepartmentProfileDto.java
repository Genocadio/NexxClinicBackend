package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DepartmentProfileDto(
        UUID id,
        String name,
        boolean isDefault,
        List<ProductDto> products,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

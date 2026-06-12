package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.VisitProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VisitDepartmentProductDto(
        UUID id,
        ProductDto product,
        BigDecimal quantity,
        BigDecimal price,
        VisitProductStatus status,
        WorkerDto addedBy,
        WorkerDto billedBy,
        WorkerDto processor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

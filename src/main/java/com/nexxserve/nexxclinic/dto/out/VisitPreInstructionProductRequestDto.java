package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.VisitPreInstructionProductStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record VisitPreInstructionProductRequestDto(
        UUID id,
        ProductDto product,
        Double quantity,
        WorkerDto requestedBy,
        VisitPreInstructionProductStatus status,
        WorkerDto processedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

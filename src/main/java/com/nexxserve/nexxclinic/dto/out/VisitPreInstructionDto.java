package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VisitPreInstructionDto(
        UUID id,
        String type,
        String note,
        WorkerDto addedBy,
        LocalDateTime createdAt,
        List<VisitPreInstructionMedicationRequestDto> medications,
        List<VisitPreInstructionProductRequestDto> products
) {}

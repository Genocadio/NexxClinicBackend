package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.EncounterType;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VisitDepartmentDto(
        UUID id,
        DepartmentDto department,
        VisitDepartmentStatus status,
        EncounterType encounterType,
        LocalDateTime completedAt,
        List<WorkerDto> processors,
        List<VisitDepartmentProductDto> products,
        List<VisitDepartmentDiagnosisDto> diagnostics,
        List<VisitDepartmentMedicationDto> medications,
        List<VisitPreInstructionDto> preInstructions,
        List<VisitDepartmentDto> childVisitDepartments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

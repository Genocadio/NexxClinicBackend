package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;
import com.nexxserve.nexxclinic.model.VisitDepartmentDiagnosisType;

public record VisitDepartmentDiagnosisDto(
        UUID id,
        String diagnosisName,
        String icd11Code,
        VisitDepartmentDiagnosisType type,
        String notes,
        WorkerDto addedBy,
        LocalDateTime createdAt
) {}

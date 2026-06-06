package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitDepartmentDiagnosisDto(
        UUID id,
        String diagnosisName,
        String icd11Code,
        LocalDateTime createdAt
) {}

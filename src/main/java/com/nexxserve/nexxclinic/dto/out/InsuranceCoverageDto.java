package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.EncounterType;
import java.time.LocalDateTime;
import java.util.UUID;

public record InsuranceCoverageDto(
    UUID id,
    UUID insuranceProviderId,
    String insuranceProviderName,
    UUID departmentId,
    String departmentName,
    EncounterType encounterType,
    Integer patientSharePercentage,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

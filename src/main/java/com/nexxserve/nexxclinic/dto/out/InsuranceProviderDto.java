package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.entity.InsuranceCoverage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InsuranceProviderDto(
        UUID id,
        String insuranceName,
        String acronym,
        List<InsuranceCoverageDto> coverages,
        boolean supportedByClinic,
        String iconUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

public record InsuranceProviderDto(
        UUID id,
        String insuranceName,
        String acronym,
        Integer defaultCoveragePercentage,
        boolean supportedByClinic,
        String iconUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DepartmentDto(
        UUID id,
        String name,
        Object insurancePolicyMode, // Replace with proper Enum if available
        List<InsuranceProviderDto> insurancePolicies,
        List<ProductDto> defaultProducts,
        boolean nursing,
        boolean supportRequests,
        boolean requestsProducts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
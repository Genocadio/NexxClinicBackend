package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.nexxserve.nexxclinic.model.DepartmentInsurancePolicyMode;

public record DepartmentDto(
        UUID id,
        String name,
        DepartmentInsurancePolicyMode insurancePolicyMode,
        List<InsuranceProviderDto> insurancePolicies,
        List<DepartmentProfileDto> profiles,
        boolean nursing,
        boolean supportRequests,
        boolean requestsProducts,
        List<StandaloneFormDto> standaloneForms,
        StandaloneFormDto defaultStandaloneForm,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.DepartmentInsurancePolicyMode;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateDepartmentInput(
        @Size(max = 150, message = "name must not exceed 150 characters")
        String name,

        DepartmentInsurancePolicyMode insurancePolicyMode,

        List<UUID> insuranceProviderIds,

        List<UUID> defaultProductIds
) {
}

package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.DrugAdministrationFrequency;
import com.nexxserve.nexxclinic.model.MustPrescribedBy;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductInsuranceCoverageInput(
        @NotNull(message = "insuranceProviderId is required")
        UUID insuranceProviderId,

        @DecimalMin(value = "0.0", message = "cost must be greater than or equal to 0")
        BigDecimal cost,

        Boolean covered,

        Boolean requireMedicalAdvisor,

        MustPrescribedBy mustPrescribedBy,

        DrugAdministrationFrequency drugAdministrationFrequency,

        @Size(max = 20, message = "authorizationRequestReasons must not exceed 20 entries")
        List<@Size(max = 120, message = "authorization reason must not exceed 120 characters") String> authorizationRequestReasons
) {
}

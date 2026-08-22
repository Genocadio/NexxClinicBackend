package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.EncounterType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateInsuranceCoverageRuleInput(
    @NotNull(message = "insuranceProviderId is required")
    UUID insuranceProviderId,

    UUID departmentId,

    EncounterType encounterType,

    @NotNull(message = "patientSharePercentage is required")
    @Min(value = 0, message = "patientSharePercentage must be between 0 and 100")
    @Max(value = 100, message = "patientSharePercentage must be between 0 and 100")
    Integer patientSharePercentage
) {}

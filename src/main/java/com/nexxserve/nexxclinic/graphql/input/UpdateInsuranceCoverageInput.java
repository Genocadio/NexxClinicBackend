package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.EncounterType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record UpdateInsuranceCoverageInput(
    UUID insuranceProviderId,

    UUID departmentId,

    EncounterType encounterType,

    @Min(value = 0, message = "patientSharePercentage must be between 0 and 100")
    @Max(value = 100, message = "patientSharePercentage must be between 0 and 100")
    Integer patientSharePercentage
) {}

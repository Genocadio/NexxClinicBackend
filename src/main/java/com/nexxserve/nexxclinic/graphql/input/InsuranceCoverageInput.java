package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.EncounterType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Input for creating a patient share coverage inline when creating/updating
 * an InsuranceProvider. The insuranceProviderId is inferred from the parent.
 */
public record InsuranceCoverageInput(
    UUID departmentId,

    EncounterType encounterType,

    @NotNull(message = "patientSharePercentage is required")
    @Min(value = 0, message = "patientSharePercentage must be between 0 and 100")
    @Max(value = 100, message = "patientSharePercentage must be between 0 and 100")
    Integer patientSharePercentage
) {}

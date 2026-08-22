package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateInsuranceProviderInput(
        @Size(max = 150, message = "insuranceName must not exceed 150 characters")
        String insuranceName,

        @Size(max = 30, message = "acronym must not exceed 30 characters")
        String acronym,

        @Min(value = 0, message = "defaultPatientSharePercentage must be between 0 and 100")
        @Max(value = 100, message = "defaultPatientSharePercentage must be between 0 and 100")
        Integer defaultPatientSharePercentage,

        Boolean supportedByClinic,

        @Size(max = 500, message = "iconUrl must not exceed 500 characters")
        String iconUrl
) {
}

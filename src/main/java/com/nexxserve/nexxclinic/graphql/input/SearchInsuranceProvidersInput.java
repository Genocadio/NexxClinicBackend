package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SearchInsuranceProvidersInput(
        @Size(max = 150, message = "query must not exceed 150 characters")
        String query,

        Boolean supportedByClinic,

        @Min(value = 0, message = "page must be 0 or greater")
        Integer page,

        @Min(value = 1, message = "size must be at least 1")
        Integer size
) {
}

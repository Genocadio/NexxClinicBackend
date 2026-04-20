package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SearchPatientsInput(
        @Size(max = 150, message = "name must not exceed 150 characters")
        String name,

        @Size(max = 30, message = "phoneNumber must not exceed 30 characters")
        String phoneNumber,
        UUID insuranceProviderId,

        @Min(value = 0, message = "age must be 0 or greater")
        Integer age,

        @Min(value = 0, message = "minAge must be 0 or greater")
        Integer minAge,

        @Min(value = 0, message = "maxAge must be 0 or greater")
        Integer maxAge,

        @Min(value = 0, message = "page must be 0 or greater")
        Integer page,

        @Min(value = 1, message = "size must be at least 1")
        Integer size
) {
}

package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateInsuranceProviderInput(
        @NotBlank(message = "insuranceName is required")
        @Size(max = 150, message = "insuranceName must not exceed 150 characters")
        String insuranceName,

        @Size(max = 30, message = "acronym must not exceed 30 characters")
        String acronym,

        /**
         * Patient share coverages. At least one base coverage (department=null,
         * encounterType=null) is required. Additional conditional coverages can
         * target specific departments or encounter types.
         */
        @NotEmpty(message = "At least one coverage is required")
        @Valid
        List<InsuranceCoverageInput> coverages,

        Boolean supportedByClinic,

        @Size(max = 500, message = "iconUrl must not exceed 500 characters")
        String iconUrl
) {}

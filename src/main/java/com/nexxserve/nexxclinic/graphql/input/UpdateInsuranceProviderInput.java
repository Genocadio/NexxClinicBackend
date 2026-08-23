package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateInsuranceProviderInput(
        @Size(max = 150, message = "insuranceName must not exceed 150 characters")
        String insuranceName,

        @Size(max = 30, message = "acronym must not exceed 30 characters")
        String acronym,

        /**
         * When provided, replaces ALL existing coverages. Must contain at least
         * one base coverage (department=null, encounterType=null).
         */
        @Valid
        List<InsuranceCoverageInput> coverages,

        Boolean supportedByClinic,

        @Size(max = 500, message = "iconUrl must not exceed 500 characters")
        String iconUrl
) {}

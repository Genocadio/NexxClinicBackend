package com.nexxserve.nexxclinic.graphql.input;

import java.util.UUID;

public record SearchInsuranceCoverageRulesInput(
    UUID insuranceProviderId,
    UUID departmentId
) {}

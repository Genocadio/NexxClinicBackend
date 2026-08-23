package com.nexxserve.nexxclinic.graphql.input;

import java.util.UUID;

public record SearchInsuranceCoveragesInput(
    UUID insuranceProviderId,
    UUID departmentId
) {}

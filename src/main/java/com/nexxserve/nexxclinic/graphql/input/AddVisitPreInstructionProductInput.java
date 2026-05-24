package com.nexxserve.nexxclinic.graphql.input;

import java.util.UUID;

public record AddVisitPreInstructionProductInput(
        UUID productId,
        Double quantity
) {
}

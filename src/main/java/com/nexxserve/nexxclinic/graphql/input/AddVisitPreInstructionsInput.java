package com.nexxserve.nexxclinic.graphql.input;

import java.util.List;
import java.util.UUID;

public record AddVisitPreInstructionsInput(
        UUID visitDepartmentId,
        List<AddVisitPreInstructionItemInput> items
) {
}

package com.nexxserve.nexxclinic.graphql.input;

import java.util.List;

public record AddVisitPreInstructionItemInput(
        String type,
        String note,
        List<AddVisitPreInstructionMedicationInput> medications,
        List<AddVisitPreInstructionProductInput> products
) {
}

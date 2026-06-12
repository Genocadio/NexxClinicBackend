package com.nexxserve.nexxclinic.graphql.input;

public record AddVisitPreInstructionMedicationInput(
        String medName,
        String dosage,
        String route,
        String frequency,
        String duration,
        String quantity,
        String otherInstructions
) {
}

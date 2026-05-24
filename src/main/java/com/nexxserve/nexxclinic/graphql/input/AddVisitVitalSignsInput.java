package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AddVisitVitalSignsInput(
        @NotNull(message = "visitId is required")
        UUID visitId,

        @NotNull(message = "vitalSigns are required")
        @Size(min = 1, message = "At least one vital sign item is required")
        @Valid
        List<AddVisitVitalSignItemInput> vitalSigns
) {
}

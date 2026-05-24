package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record LabRecordConfigInput(
        @NotBlank(message = "layout is required")
        String layout,

        List<@Valid LabRecordRowInput> rows
) {
}
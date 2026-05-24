package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record LabRecordRowInput(
        String id,

        @NotBlank(message = "name is required")
        String name,

        String unitMode,

        List<String> unitOptions,

        String defaultUnit,

        List<String> resultOptions
) {
}
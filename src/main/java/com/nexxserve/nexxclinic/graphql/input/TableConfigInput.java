package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.TableMode;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TableConfigInput(
        @NotNull(message = "mode is required")
        TableMode mode,

        Integer rows,

        Integer columns,

        String headerPlacement,

        List<String> columnHeaders,

        List<String> rowHeaders
) {
}

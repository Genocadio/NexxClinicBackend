package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FormSectionInput(
        @NotBlank(message = "id is required")
        String id,

        @NotBlank(message = "title is required")
        String title,

        Boolean boldTitle,

        Boolean italicTitle,

        Boolean underlineTitle,

        Boolean centerTitle,

        @NotNull(message = "columns is required")
        Integer columns,

        @NotNull(message = "order is required")
        Integer order,

        List<@Valid FormFieldInput> fields
) {
}

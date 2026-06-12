package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record FormInput(
        @NotBlank(message = "title is required")
        String title,

        String description,

        List<@Valid FormFieldInput> fields,

        List<@Valid FormSectionInput> sections,

        List<@Valid FormActionInput> actions
) {
}

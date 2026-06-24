package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StandaloneFormInput(
        @NotBlank String name,
        String description,
        @NotBlank String type,
        String category,
        Boolean isTemplate,
        @NotNull Object blocks,
        Object theme
) {}

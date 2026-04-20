package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.ProductType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SearchProductsInput(
        @Size(max = 200, message = "name must not exceed 200 characters")
        String name,

        ProductType type,

        @Min(value = 0, message = "page must be 0 or greater")
        Integer page,

        @Min(value = 1, message = "size must be at least 1")
        Integer size
) {
}

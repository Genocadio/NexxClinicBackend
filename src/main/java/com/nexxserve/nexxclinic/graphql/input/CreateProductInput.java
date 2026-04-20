package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.ProductUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductInput(
        UUID id,

        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must not exceed 200 characters")
        String name,

        @Size(max = 200, message = "genericName must not exceed 200 characters")
        String genericName,

        @NotBlank(message = "code is required")
        @Size(max = 64, message = "code must not exceed 64 characters")
        String code,

        @NotBlank(message = "description is required")
        @Size(max = 500, message = "description must not exceed 500 characters")
        String description,

        @NotNull(message = "type is required")
        ProductType type,

        @NotNull(message = "unit is required")
        ProductUnit unit,

        Object metadata,

        @DecimalMin(value = "0.0", message = "privateRhicPrice must be greater than or equal to 0")
        BigDecimal privateRhicPrice,

        @DecimalMin(value = "0.0", message = "clinicPrice must be greater than or equal to 0")
        BigDecimal clinicPrice,

        List<@Valid CreateProductInsuranceCoverageInput> insuranceCoverages
) {
}

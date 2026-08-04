package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.VisitProductStatus;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateVisitDepartmentProductInput(
        @NotNull(message = "visitId is required")
        UUID visitId,

        @NotNull(message = "departmentId is required")
        UUID departmentId,

        @NotNull(message = "productId is required")
        UUID productId,

        UUID processorId,

        BigDecimal quantity,

        VisitProductStatus status
) {
}

package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.VisitProductStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateVisitDepartmentProductStatusInput(
        @NotNull(message = "visitDepartmentProductId is required")
        UUID visitDepartmentProductId,

        @NotNull(message = "status is required")
        VisitProductStatus status
) {
}

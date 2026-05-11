package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateVisitDepartmentStatusInput(
        @NotNull(message = "visitDepartmentId is required")
        UUID visitDepartmentId,

        @NotNull(message = "status is required")
        VisitDepartmentStatus status
) {
}

package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BillVisitInput(
        @NotNull(message = "visitId is required")
        UUID visitId,

        Boolean billAllProducts,

        @Valid
        List<BillVisitItemInput> items,

        BigDecimal paidAmount
) {
    public record BillVisitItemInput(
            @NotNull(message = "visitDepartmentProductId is required")
            UUID visitDepartmentProductId,

            UUID patientInsuranceId
    ) {
    }
}

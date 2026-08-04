package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.CoverageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EditBillVisitInput(
    @NotNull(message = "visitId is required")
    UUID visitId,

    @Valid
    List<EditBillVisitDepartmentInput> departments
) {

    public record EditBillVisitDepartmentInput(
        @NotNull(message = "visitDepartmentId is required")
        UUID visitDepartmentId,

        @Valid
        List<EditBillVisitAddProductInput> addedProducts,

        List<UUID> removedProductIds,

        @Valid
        List<EditBillVisitUpdateProductInput> updatedProducts,

        @Valid
        List<EditBillVisitBillProductInput> billProducts,

        @Valid
        List<BillVisitInput.BillingPaymentInput> payments,

        String note
    ) {}

    public record EditBillVisitBillProductInput(
        @NotNull(message = "productId is required")
        UUID productId,

        UUID patientInsuranceId,

        CoverageType coverageType,

        BigDecimal quantity,

        Boolean isExempted
    ) {}

    public record EditBillVisitAddProductInput(
        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "quantity is required")
        BigDecimal quantity
    ) {}

    public record EditBillVisitUpdateProductInput(
        @NotNull(message = "productId is required")
        UUID productId,

        BigDecimal quantity
    ) {}
}

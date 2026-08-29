package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.CoverageType;
import com.nexxserve.nexxclinic.model.ExemptionType;
import com.nexxserve.nexxclinic.model.OutstandingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EditBillVisitInput(
    @NotNull(message = "visitId is required")
    UUID visitId,

    @NotNull(message = "expectedBillingVersionId is required")
    UUID expectedBillingVersionId,

    @NotNull(message = "departments is required")
    @Size(min = 1, max = 20, message = "departments must have 1-20 entries")
    @Valid
    List<EditBillVisitDepartmentInput> departments
) {

    public record EditBillVisitDepartmentInput(
        @NotNull(message = "visitDepartmentId is required")
        UUID visitDepartmentId,

        @Size(max = 20, message = "Too many added products (max 20)")
        @Valid
        List<EditBillVisitAddProductInput> addedProducts,

        @Size(max = 50, message = "Too many removed products (max 50)")
        List<UUID> removedProductIds,

        @Size(max = 50, message = "Too many updated products (max 50)")
        @Valid
        List<EditBillVisitUpdateProductInput> updatedProducts,

        @Size(max = 50, message = "Too many bill products (max 50)")
        @Valid
        List<EditBillVisitBillProductInput> billProducts,

        @Size(max = 20, message = "Too many payments (max 20)")
        @Valid
        List<BillVisitInput.BillingPaymentInput> payments,

        String note,

        OutstandingType outstandingType,
        String outstandingReason
    ) {}

    public record EditBillVisitBillProductInput(
        @NotNull(message = "productId is required")
        UUID productId,

        UUID patientInsuranceId,

        @NotNull(message = "coverageType is required")
        CoverageType coverageType,

        BigDecimal quantity,

        ExemptionType exemptionType,

        UUID patientSharePercentageOverride
    ) {}

    public record EditBillVisitAddProductInput(
        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "quantity is required")
        BigDecimal quantity,

        UUID processorId
    ) {}

    public record EditBillVisitUpdateProductInput(
        @NotNull(message = "productId is required")
        UUID productId,

        BigDecimal quantity
    ) {}
}

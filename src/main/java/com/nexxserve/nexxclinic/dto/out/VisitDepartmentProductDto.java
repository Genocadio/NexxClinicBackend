package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.BillingConfirmationStatus;
import com.nexxserve.nexxclinic.model.BillingState;
import com.nexxserve.nexxclinic.model.ExemptionType;
import com.nexxserve.nexxclinic.model.VisitDepartmentProductSource;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VisitDepartmentProductDto(
        UUID id,
        ProductDto product,
        BigDecimal quantity,
        VisitProductStatus status,
        BillingState billingState,
        ExemptionType exemptionMode,
        VisitDepartmentProductSource source,
        WorkerDto addedBy,
        WorkerDto billedBy,
        WorkerDto confirmedBy,
        BillingConfirmationStatus billingConfirmationStatus,
        WorkerDto processor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

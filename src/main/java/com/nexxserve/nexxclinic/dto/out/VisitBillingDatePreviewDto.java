package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only preview of how an individual billing row's billing date will change
 * when a visit's date is edited. Used by the manager-facing "change visit date"
 * flow to show proposed billing dates BEFORE the change is applied.
 */
public record VisitBillingDatePreviewDto(
        UUID id,
        String departmentName,
        String insuranceLabel,
        LocalDateTime currentBillingDate,
        LocalDateTime proposedBillingDate,
        String invoiceUrl
) {
}

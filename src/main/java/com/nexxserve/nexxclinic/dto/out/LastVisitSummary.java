package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.VisitStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight visit summary attached to each patient in search results.
 * Lets the registration desk see at a glance whether a patient already has
 * an active visit before attempting to create a new one.
 */
public record LastVisitSummary(
        UUID id,
        VisitStatus status,
        LocalDateTime visitDate
) {}

package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Input for {@code updateVisitDepartmentProductProcessor}.
 *
 * <p>Rules:
 * <ul>
 *   <li>Only the worker who originally added the product ({@code addedBy}) may change
 *       the processor, unless the caller has FINANCE or ADMIN role.</li>
 *   <li>FINANCE/ADMIN may assign any worker as processor.</li>
 *   <li>Non-FINANCE/ADMIN callers may only pick from workers already linked to the
 *       visit department (its {@code processors} list).</li>
 *   <li>Pass {@code processorId = null} to clear the processor assignment.</li>
 * </ul>
 */
public record UpdateVisitDepartmentProductProcessorInput(
    @NotNull(message = "visitDepartmentProductId is required")
    UUID visitDepartmentProductId,

    /** Target processor worker id. Null clears the processor assignment. */
    UUID processorId
) {}

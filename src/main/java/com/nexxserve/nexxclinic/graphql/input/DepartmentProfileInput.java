package com.nexxserve.nexxclinic.graphql.input;

import java.util.List;
import java.util.UUID;

/**
 * A department profile to create or update.
 * <ul>
 *   <li>{@code id} — optional; present when updating an existing profile.</li>
 *   <li>{@code name} — profile name.</li>
 *   <li>{@code isDefault} — at most ONE profile per department can be default.</li>
 *   <li>{@code productIds} — products in this profile (may be empty).</li>
 * </ul>
 */
public record DepartmentProfileInput(
        UUID id,
        String name,
        Boolean isDefault,
        List<UUID> productIds
) {
}

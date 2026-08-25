package com.nexxserve.nexxclinic.model;

/**
 * Classifies the unpaid (outstanding) portion of a billing bucket.
 * <ul>
 *   <li>{@code LOAN} — patient still owes this amount (preselected when partially paid)</li>
 *   <li>{@code GIVEAWAY} — clinic absorbs this amount (preselected when fully exempted)</li>
 * </ul>
 */
public enum OutstandingType {
    LOAN,
    GIVEAWAY
}

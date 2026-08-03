package com.nexxserve.nexxclinic.model;

public enum VisitProductStatus {
    BILLED,
    EXEMPTED,
    /**
     * Was BILLED/EXEMPTED and reset by the edit-billing correction flow
     * ({@code editBillVisit.updatedProducts}). It is transient within the
     * correction transaction: the same request re-bills these products, moving
     * them back to BILLED/EXEMPTED in the new billing version.
     */
    CORRECTION_PENDING,
    /**
     * Legacy value — used by rows created before {@link #CORRECTION_PENDING}
     * existed. Treated as "needs billing", same as {@link #PENDING} and
     * {@link #CORRECTION_PENDING}.
     */
    UNPAID,
    PENDING
}

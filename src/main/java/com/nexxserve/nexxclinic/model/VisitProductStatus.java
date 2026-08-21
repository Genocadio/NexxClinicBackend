package com.nexxserve.nexxclinic.model;

public enum VisitProductStatus {
    BILLED,
    EXEMPTED,
    /**
     * The patient's share was waived ({@link ExemptionType#PATIENT_SHARE}) but the
     * insurance still covers its normal amount. Distinct from {@link #EXEMPTED} where
     * the entire line is zeroed.
     */
    PATIENT_SHARE_EXEMPTED,
    /**
     * Was BILLED/EXEMPTED/PATIENT_SHARE_EXEMPTED and reset by the edit-billing
     * correction flow ({@code editBillVisit.updatedProducts}). It is transient within
     * the correction transaction: the same request re-bills these products, moving
     * them back to BILLED/EXEMPTED/PATIENT_SHARE_EXEMPTED in the new billing version.
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


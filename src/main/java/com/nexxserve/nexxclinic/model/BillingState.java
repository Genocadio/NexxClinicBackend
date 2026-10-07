package com.nexxserve.nexxclinic.model;

/**
 * Billing lifecycle state of a visit department product, separated from the
 * exemption mode ({@link ExemptionType}) that is only meaningful once billed.
 *
 * <p>This replaces the conflation that previously lived in
 * {@link VisitProductStatus}, where "was it billed?" and "how is the patient
 * exempted?" were flattened into one enum (BILLED vs EXEMPTED vs
 * PATIENT_SHARE_EXEMPTED) alongside two unbilled variants (PENDING/UNPAID and
 * CORRECTION_PENDING).
 *
 * <ul>
 *   <li>{@link #UNBILLED} — needs billing (legacy PENDING/UNPAID rows).</li>
 *   <li>{@link #BILLED} — billed; the line's {@link ExemptionType} records how
 *       it was billed (NONE = normal charge, FULL/PATIENT_SHARE = waived).</li>
 *   <li>{@link #CORRECTING} — was billed but is under an active billing
 *       correction (legacy CORRECTION_PENDING). The recovery-sensitive middle
 *       state quick-bill rejects until the edit completes.</li>
 * </ul>
 *
 * <p>The legacy {@link VisitProductStatus} value remains available via
 * {@code VisitDepartmentProduct.getStatus()} (derived/mirrored) so existing
 * consumers and the GraphQL {@code status} field keep working unchanged.
 */
public enum BillingState {
    UNBILLED,
    BILLED,
    CORRECTING
}

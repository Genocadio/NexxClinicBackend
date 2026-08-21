package com.nexxserve.nexxclinic.model;

/**
 * Defines how a billing product line is exempted from patient payment.
 *
 * <ul>
 *   <li>{@code NONE} — no exemption; the patient pays their share (or the full amount
 *       for PRIVATE coverage).</li>
 *   <li>{@code PATIENT_SHARE} — the patient's share is waived; the insurance still
 *       covers its normal percentage. The product is stamped
 *       {@link VisitProductStatus#PATIENT_SHARE_EXEMPTED}.</li>
 *   <li>{@code FULL} — the entire line is waived (unit price zeroed, quantity forced to 1,
 *       insurance covers nothing). The product is stamped
 *       {@link VisitProductStatus#EXEMPTED}.</li>
 * </ul>
 */
public enum ExemptionType {
    NONE,
    PATIENT_SHARE,
    FULL
}

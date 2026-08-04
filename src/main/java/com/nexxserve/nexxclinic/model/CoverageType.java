package com.nexxserve.nexxclinic.model;

/**
 * How a billed product line is covered.
 *
 * <p>There is no automatic insurance assignment per product. Every product in a
 * billing request must be explicitly marked:
 * <ul>
 *   <li>{@link #PRIVATE} — billed without insurance. {@code patientInsuranceId}
 *       must NOT be provided.</li>
 *   <li>{@link #INSURANCE} — billed against an explicit insurance.
 *       {@code patientInsuranceId} is required and must be linked to the visit,
 *       belong to the visit's patient, be active (policy period covers today) and
 *       cover the product.</li>
 * </ul>
 */
public enum CoverageType {
    PRIVATE,
    INSURANCE
}

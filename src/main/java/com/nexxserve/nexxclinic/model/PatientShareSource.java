package com.nexxserve.nexxclinic.model;

/**
 * Indicates the source of the patient share percentage applied to a billing line.
 * Used for audit trail on {@code VisitBillingItem} so reports can show why each
 * line got its percentage.
 */
public enum PatientShareSource {
    /** Per-line override provided during billing (highest priority). */
    OVERRIDE,
    /** Matched an {@code InsuranceCoverage} (dept + encounter type). */
    RULE,
    /** Fell back to {@code PatientInsurance.patientSharePercentage} (patient-specific default). */
    PATIENT_DEFAULT,
    /** Fell back to {@code InsuranceProvider.defaultPatientSharePercentage}. */
    PROVIDER_DEFAULT,
    /** Line was exempted (FULL or PATIENT_SHARE exemption). */
    EXEMPTED
}

-- Rename the ambiguous "coverage" column to explicit "patientShare" naming
ALTER TABLE insurance_providers
    RENAME COLUMN default_coverage_percentage TO default_patient_share_percentage;

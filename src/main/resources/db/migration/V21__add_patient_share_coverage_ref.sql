-- Add a foreign key reference from patient_insurances to insurance_coverage,
-- replacing the free-form patient_share_percentage integer with a reference
-- to an existing coverage record. The old column is kept for backward
-- compatibility (existing data); new code should use the FK reference.
ALTER TABLE patient_insurances
    ADD COLUMN patient_share_coverage_id UUID;

ALTER TABLE patient_insurances
    ADD CONSTRAINT fk_patient_insurance_share_coverage
    FOREIGN KEY (patient_share_coverage_id)
    REFERENCES insurance_coverage (id)
    ON DELETE SET NULL;

CREATE INDEX idx_patient_insurance_share_coverage
    ON patient_insurances (patient_share_coverage_id);

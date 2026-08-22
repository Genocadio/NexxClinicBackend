ALTER TABLE patient_insurances
    ADD COLUMN patient_share_percentage INTEGER;

-- Validate range on insert/update
ALTER TABLE patient_insurances
    ADD CONSTRAINT chk_patient_insurance_share
    CHECK (patient_share_percentage IS NULL OR (patient_share_percentage >= 0 AND patient_share_percentage <= 100));

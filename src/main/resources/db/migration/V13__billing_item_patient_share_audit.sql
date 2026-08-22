ALTER TABLE visit_billing_items
    ADD COLUMN applied_patient_share_pct INTEGER,
    ADD COLUMN patient_share_source VARCHAR(20);

-- V27: Add billing_note column to visit_department_billings.
-- The billing note (justification for outstanding balance or exemption) is a
-- financial annotation on the department bill. It does NOT belong in
-- visit_department_notes (which are inter-department communication notes).
-- Existing rows get NULL (no note was stored before this column existed).
ALTER TABLE visit_department_billings
    ADD COLUMN billing_note varchar(1000);

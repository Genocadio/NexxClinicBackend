-- V23: Add nullable estimated price columns to visits.
-- These are auto-computed whenever products or insurances change on a visit,
-- giving a live price preview before a biller touches the visit.
-- They are nullable because they are only meaningful when at least one product
-- and one insurance (or no insurance = full private) exist on the visit.

ALTER TABLE visits
    ADD COLUMN estimated_total        numeric(19,2),
    ADD COLUMN estimated_insurance_pay numeric(19,2),
    ADD COLUMN estimated_patient_pay   numeric(19,2);

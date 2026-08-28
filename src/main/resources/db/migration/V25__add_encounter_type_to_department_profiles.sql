-- Profiles now carry the encounter type they apply to.
-- Existing profiles default to OUTPATIENT (the most common type).
ALTER TABLE department_profiles
    ADD COLUMN encounter_type VARCHAR(30) NOT NULL DEFAULT 'OUTPATIENT';

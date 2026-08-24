-- V17: Add FOLLOWUP encounter type to the visit_departments CHECK constraint.
--
-- The old constraint only allowed OUTPATIENT, INPATIENT_OBSERVATION, INPATIENT_ADMISSION.
-- PostgreSQL requires dropping and re-adding the constraint to add a new enum value.

ALTER TABLE visit_departments
    DROP CONSTRAINT visit_departments_encounter_type_check;

ALTER TABLE visit_departments
    ADD CONSTRAINT visit_departments_encounter_type_check
    CHECK (encounter_type IN ('OUTPATIENT','INPATIENT_OBSERVATION','INPATIENT_ADMISSION','FOLLOWUP'));

-- Per-department billing edit mode.
-- Billing edit mode moves from the whole visit (BILL_EDITING) down to the
-- individual visit department (DEPARTMENT_EDITING). The visit status is left
-- untouched; only the target department and its products are edited.
--
-- DEPARTMENT_EDITING is a transitional status:
--   COMPLETED/FINALISED -> DEPARTMENT_EDITING -> back to the remembered status.
-- billing_edit_source_status remembers that pre-edit status on the department.

ALTER TABLE visit_departments
    DROP CONSTRAINT IF EXISTS visit_departments_status_check;

ALTER TABLE visit_departments
    ADD CONSTRAINT visit_departments_status_check
    CHECK (status IN ('ACTIVE', 'PENDING', 'ON_HOLD', 'BILLING', 'COMPLETED', 'FINALISED', 'CANCELLED', 'DEPARTMENT_EDITING'));

ALTER TABLE visit_departments
    ADD COLUMN IF NOT EXISTS billing_edit_source_status VARCHAR(32);

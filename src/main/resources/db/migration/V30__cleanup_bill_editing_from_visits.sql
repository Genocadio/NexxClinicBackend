-- V30: Clean up visit-level BILL_EDITING status.
--
-- BILL_EDITING was the old visit-level editing status (V19). It has been
-- superseded by DEPARTMENT_EDITING on visit_departments (V29). Any visits
-- still stuck in BILL_EDITING are reset to COMPLETED.

-- 1. Reset any visits stuck in BILL_EDITING back to COMPLETED.
UPDATE visits
SET status = 'COMPLETED'
WHERE status = 'BILL_EDITING';

-- 2. Remove BILL_EDITING from the visit status check constraint.
ALTER TABLE visits
    DROP CONSTRAINT IF EXISTS visits_status_check;

ALTER TABLE visits
    ADD CONSTRAINT visits_status_check
    CHECK (status IN ('CREATED', 'IN_PROGRESS', 'CANCELLED', 'COMPLETED', 'FINALISED'));

-- 3. Drop the visit-level billing_edit_source_status column.
-- This was only used by BILL_EDITING (V28). The department-level equivalent
-- on visit_departments.billing_edit_source_status (V29) is still in use.
ALTER TABLE visits
    DROP COLUMN IF EXISTS billing_edit_source_status;

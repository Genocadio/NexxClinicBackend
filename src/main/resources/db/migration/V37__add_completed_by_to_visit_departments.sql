-- Migration V37: Add completed_by_id to visit_departments
-- Tracks which worker/clinician marked or finalised the department completion

ALTER TABLE visit_departments
ADD COLUMN IF NOT EXISTS completed_by_id UUID REFERENCES workers(id);

CREATE INDEX IF NOT EXISTS idx_visit_departments_completed_by
ON visit_departments(completed_by_id);

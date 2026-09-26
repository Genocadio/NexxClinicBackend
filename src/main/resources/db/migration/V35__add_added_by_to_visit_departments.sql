-- ============================================================================
-- V35: Add Added By (Worker) relationship to Visit Departments
-- ============================================================================

ALTER TABLE visit_departments
    ADD COLUMN IF NOT EXISTS added_by_id UUID REFERENCES workers(id);

CREATE INDEX IF NOT EXISTS idx_visit_departments_added_by
    ON visit_departments (added_by_id);

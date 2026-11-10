-- ============================================================================
-- V52: Visit Flow, Department Kinds, Routing Transitions & Dashboard State
-- ============================================================================

-- 1. Add Department Kind categorization
ALTER TABLE departments
    ADD COLUMN IF NOT EXISTS kind VARCHAR(32) NOT NULL DEFAULT 'CONSULTATION';

-- Heuristic backfill for existing departments based on name
UPDATE departments SET kind = 'LABORATORY' WHERE kind = 'CONSULTATION' AND name ILIKE '%lab%';
UPDATE departments SET kind = 'IMAGING' WHERE kind = 'CONSULTATION' AND (name ILIKE '%imag%' OR name ILIKE '%radio%' OR name ILIKE '%x-ray%' OR name ILIKE '%scan%');
UPDATE departments SET kind = 'WARD' WHERE kind = 'CONSULTATION' AND (name ILIKE '%ward%' OR name ILIKE '%inpatient%' OR name ILIKE '%hospit%');
UPDATE departments SET kind = 'TRIAGE' WHERE kind = 'CONSULTATION' AND (name ILIKE '%triag%' OR name ILIKE '%recept%');
UPDATE departments SET kind = 'PHARMACY' WHERE kind = 'CONSULTATION' AND (name ILIKE '%pharm%' OR name ILIKE '%dispens%');
UPDATE departments SET kind = 'PROCEDURE_ROOM' WHERE kind = 'CONSULTATION' AND (name ILIKE '%theatre%' OR name ILIKE '%surg%' OR name ILIKE '%proced%');

-- 2. Add routing and awaiting state columns to visit_departments
ALTER TABLE visit_departments
    ADD COLUMN IF NOT EXISTS awaiting_role VARCHAR(32),
    ADD COLUMN IF NOT EXISTS awaiting_since TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_routing_action VARCHAR(40),
    ADD COLUMN IF NOT EXISTS previous_department_id UUID REFERENCES departments(id);

CREATE INDEX IF NOT EXISTS idx_visit_departments_awaiting
    ON visit_departments (department_id, status, awaiting_role);

CREATE INDEX IF NOT EXISTS idx_visit_departments_prev_dept
    ON visit_departments (previous_department_id);

-- 3. Add flow summary tracking columns to visits
ALTER TABLE visits
    ADD COLUMN IF NOT EXISTS current_locations TEXT,
    ADD COLUMN IF NOT EXISTS active_episode_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_discharged BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS has_returned_from_billing BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS has_pending_results BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_flow_activity_at TIMESTAMP DEFAULT NOW();

-- Backfill is_discharged based on current visit status
UPDATE visits
SET is_discharged = TRUE
WHERE status IN ('COMPLETED', 'FINALISED');

CREATE INDEX IF NOT EXISTS idx_visits_flow_discharged
    ON visits (is_discharged, last_flow_activity_at DESC);

-- 4. Create visit_department_transitions table for audit log and timeline tracking
CREATE TABLE IF NOT EXISTS visit_department_transitions (
    id UUID PRIMARY KEY,
    visit_id UUID NOT NULL REFERENCES visits(id) ON DELETE CASCADE,
    visit_department_id UUID NOT NULL REFERENCES visit_departments(id) ON DELETE CASCADE,
    from_status VARCHAR(32) NOT NULL,
    to_status VARCHAR(32) NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    target_department_id UUID REFERENCES departments(id),
    previous_department_id UUID REFERENCES departments(id),
    actor_id UUID REFERENCES workers(id),
    occurred_at TIMESTAMP NOT NULL DEFAULT NOW(),
    note TEXT
);

CREATE INDEX IF NOT EXISTS idx_vdt_visit ON visit_department_transitions(visit_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_vdt_dept ON visit_department_transitions(visit_department_id, occurred_at);

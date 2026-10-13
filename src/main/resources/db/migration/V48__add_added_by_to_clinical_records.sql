-- ============================================================================
-- V48: Add added_by_id (Worker) to VisitDepartmentDiagnosis,
--      VisitDepartmentSymptom, and VisitDepartmentMedication
--      Backfill existing records with the department's clinician / operator
-- ============================================================================

-- 1. Add added_by_id column to visit_department_diagnostics
ALTER TABLE visit_department_diagnostics
    ADD COLUMN IF NOT EXISTS added_by_id UUID REFERENCES workers(id);

CREATE INDEX IF NOT EXISTS idx_visit_department_diagnostics_added_by
    ON visit_department_diagnostics (added_by_id);

-- 2. Add added_by_id column to visit_department_symptoms
ALTER TABLE visit_department_symptoms
    ADD COLUMN IF NOT EXISTS added_by_id UUID REFERENCES workers(id);

CREATE INDEX IF NOT EXISTS idx_visit_department_symptoms_added_by
    ON visit_department_symptoms (added_by_id);

-- 3. Add added_by_id column to visit_department_medications
ALTER TABLE visit_department_medications
    ADD COLUMN IF NOT EXISTS added_by_id UUID REFERENCES workers(id);

CREATE INDEX IF NOT EXISTS idx_visit_department_medications_added_by
    ON visit_department_medications (added_by_id);

-- 4. Backfill existing diagnoses added_by_id:
--    Prefer department completed_by_id, otherwise first processor, otherwise department added_by_id
UPDATE visit_department_diagnostics d
SET added_by_id = COALESCE(
    vd.completed_by_id,
    (SELECT vdp.worker_id FROM visit_department_processors vdp WHERE vdp.visit_department_id = vd.id LIMIT 1),
    vd.added_by_id
)
FROM visit_departments vd
WHERE d.visit_department_id = vd.id
  AND d.added_by_id IS NULL;

-- 5. Backfill existing symptoms added_by_id:
UPDATE visit_department_symptoms s
SET added_by_id = COALESCE(
    vd.completed_by_id,
    (SELECT vdp.worker_id FROM visit_department_processors vdp WHERE vdp.visit_department_id = vd.id LIMIT 1),
    vd.added_by_id
)
FROM visit_departments vd
WHERE s.visit_department_id = vd.id
  AND s.added_by_id IS NULL;

-- 6. Backfill existing medications added_by_id:
UPDATE visit_department_medications m
SET added_by_id = COALESCE(
    vd.completed_by_id,
    (SELECT vdp.worker_id FROM visit_department_processors vdp WHERE vdp.visit_department_id = vd.id LIMIT 1),
    vd.added_by_id
)
FROM visit_departments vd
WHERE m.visit_department_id = vd.id
  AND m.added_by_id IS NULL;

-- Migration V39: Indexes for the visit read path
--
-- The visit list/detail fetchers (VisitService.visits / visitsToDtos) were
-- running as sequential scans because `visits` only ever had its primary key.
-- Every predicate and sort in the default operating-window query was unindexed:
--
--   WHERE (status IN ('CREATED','IN_PROGRESS')
--          OR visit_date >= ?
--          OR updated_at >= ?)
--   ORDER BY visit_date DESC, created_at DESC
--
-- `OR` across three different columns cannot use a single index, so these are
-- deliberately separate indexes rather than one composite: Postgres can BitmapOr
-- the small "active" branch off uk_visits_active_patient and fall back to an
-- index scan on the date branch.
--
-- All statements are idempotent so this is safe to re-run on a baselined DB.

-- ---------------------------------------------------------------------------
-- visits
-- ---------------------------------------------------------------------------

-- Default sort of VisitService.visits():
--   Sort.by(DESC, "visitDate").and(Sort.by(DESC, "createdAt"))
-- Without this, Postgres sorts the whole matched set in memory per page.
CREATE INDEX IF NOT EXISTS idx_visits_visit_date_created_at
    ON visits (visit_date DESC, created_at DESC);

-- The default 7-day rolling window OR-branches on updated_at, so it needs its
-- own index to avoid a second sequential scan.
CREATE INDEX IF NOT EXISTS idx_visits_updated_at
    ON visits (updated_at);

-- findByPatientId / patient history / V7's partial unique index only covers
-- ACTIVE rows; this covers the historical lookups too.
CREATE INDEX IF NOT EXISTS idx_visits_patient_id
    ON visits (patient_id);

-- The explicit `status = ?` filter (VisitService.java:503) and the
-- status IN (...) branches are otherwise unindexed.
CREATE INDEX IF NOT EXISTS idx_visits_status
    ON visits (status);

-- ---------------------------------------------------------------------------
-- visit_departments
-- ---------------------------------------------------------------------------

-- uk_visit_department (visit_id, department_id) already serves findByVisitIdIn.
-- These two columns are not covered by it and were seq-scanned.
CREATE INDEX IF NOT EXISTS idx_visit_departments_profile_id
    ON visit_departments (profile_id);

-- findByParentVisitDepartmentId(collect) drives the child-department tree.
CREATE INDEX IF NOT EXISTS idx_visit_departments_parent_id
    ON visit_departments (parent_visit_department_id);

-- Clinician role-scoping loads by department, which the (visit_id, department_id)
-- index cannot serve because visit_id is the leading column.
CREATE INDEX IF NOT EXISTS idx_visit_departments_department_id
    ON visit_departments (department_id);

-- ---------------------------------------------------------------------------
-- visit_department_processors
-- ---------------------------------------------------------------------------

-- The table has no indexes at all (V1:49) and is joined to resolve processor
-- workers for every department in a page.
CREATE INDEX IF NOT EXISTS idx_vdp_processors_visit_department_id
    ON visit_department_processors (visit_department_id);

CREATE INDEX IF NOT EXISTS idx_vdp_processors_worker_id
    ON visit_department_processors (worker_id);

-- ---------------------------------------------------------------------------
-- patient_insurances
-- ---------------------------------------------------------------------------

-- findByPatientIdIn runs on every visit page load (VisitService.java:1501) and
-- had no index on patient_id.
CREATE INDEX IF NOT EXISTS idx_patient_insurances_patient_id
    ON patient_insurances (patient_id);

-- ---------------------------------------------------------------------------
-- visit_insurances
-- ---------------------------------------------------------------------------

-- uk_visit_patient_insurance (visit_id, patient_insurance_id) already serves
-- findByVisitIdIn, so nothing to add here.

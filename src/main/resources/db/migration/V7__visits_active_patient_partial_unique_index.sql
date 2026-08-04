-- ============================================================================
-- V7: One active visit per patient — partial unique index on visits
--
-- Backend : createVisit already serializes concurrent creations with a
--           PESSIMISTIC_WRITE lock on the patient row (PatientRepository
--           .findByIdForUpdate). This index is the database-level guarantee:
--           even if that lock is bypassed or a non-default isolation level is
--           in use, a patient can never hold two CREATED/IN_PROGRESS visits.
--
-- Status is stored as enum names (CREATED / IN_PROGRESS / CANCELLED /
-- COMPLETED, see VisitStatus and the check constraint on visits.status). The
-- partial predicate keeps only the active states, mirroring the V2 approach
-- for visit_department_products. JPA cannot express partial indexes, so this
-- migration is the single source of truth.
--
-- Fresh databases : V1 creates the table WITHOUT this index; this file adds it.
-- Existing DBs    : baselined at V1, so this file runs normally there too.
-- ============================================================================

-- 1) Guard: fail loudly if legacy data already violates the rule. Creating the
--    index would otherwise abort with Postgres' cryptic unique-index error and
--    give no hint about which patients need attention. No data is changed here;
--    ops must resolve the duplicates, then re-run the migration.
DO $$
DECLARE
    dup RECORD;
BEGIN
    FOR dup IN
        SELECT patient_id, COUNT(*) AS active_visit_count
        FROM visits
        WHERE status IN ('CREATED', 'IN_PROGRESS')
        GROUP BY patient_id
        HAVING COUNT(*) > 1
    LOOP
        RAISE EXCEPTION 'Patient % has % active visits (CREATED/IN_PROGRESS). Resolve the duplicates before applying V7.',
            dup.patient_id, dup.active_visit_count;
    END LOOP;
END $$;

-- 2) Partial unique index over active visits only. Postgres has no
--    CREATE UNIQUE INDEX IF NOT EXISTS, so drop-then-create (idempotent),
--    matching V2.
DROP INDEX IF EXISTS uk_visits_active_patient;
CREATE UNIQUE INDEX uk_visits_active_patient
    ON visits (patient_id)
    WHERE status IN ('CREATED', 'IN_PROGRESS');

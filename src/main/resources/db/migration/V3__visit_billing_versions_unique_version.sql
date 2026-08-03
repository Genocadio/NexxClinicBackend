-- ============================================================================
-- V3: Unique (visit_id, version) on visit_billing_versions
-- Reference : docs/billing-flow-review-round2.md — Finding A1 (version counter race)
--
-- Problem
-- -------
-- createNextBillingVersion does findFirstByVisitIdOrderByVersionDesc then inserts
-- version = latest + 1. Two concurrent billVisit/editBillVisit calls can both
-- insert version N+1. A unique (visit_id, version) index prevents that race.
--
-- Fresh databases : V1 already declares `unique (visit_id, version)` on the
--                   table, so this migration is a NO-OP there (guarded below).
-- Existing DBs    : baselined at V1, the constraint does NOT exist; this file
--                   creates it.
--
-- IMPORTANT: if duplicate (visit_id, version) rows already exist in an
-- environment, this migration will FAIL until they are deduplicated. Flyway runs
-- at application startup, so the app will NOT boot until the rows are fixed.
-- Pre-flight check (must return 0 rows):
--     SELECT visit_id, version, count(*)
--     FROM visit_billing_versions
--     GROUP BY visit_id, version
--     HAVING count(*) > 1;
-- Resolve duplicates by renumbering or removing the stale row before starting
-- the app.
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_index i
        WHERE i.indrelid = 'visit_billing_versions'::regclass
          AND i.indisunique
          AND NOT i.indisprimary
          -- the unique index must be EXACTLY (visit_id, version): two key
          -- columns, both of which are the target columns
          AND (SELECT count(*)
               FROM pg_attribute a
               WHERE a.attrelid = i.indrelid AND a.attnum = ANY (i.indkey)) = 2
          AND 'visit_id' = ANY (ARRAY(
              SELECT a.attname FROM pg_attribute a
              WHERE a.attrelid = i.indrelid AND a.attnum = ANY (i.indkey)
          ))
          AND 'version' = ANY (ARRAY(
              SELECT a.attname FROM pg_attribute a
              WHERE a.attrelid = i.indrelid AND a.attnum = ANY (i.indkey)
          ))
    ) THEN
        CREATE UNIQUE INDEX uk_visit_billing_version
            ON visit_billing_versions (visit_id, version);
    END IF;
END $$;

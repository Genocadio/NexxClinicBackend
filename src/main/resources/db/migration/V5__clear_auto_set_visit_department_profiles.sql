-- ============================================================================
-- V5: Clear auto-set visit department profiles
--
-- Background
-- ----------
-- Before the manual-only profile change, adding a department to a visit
-- AUTO-APPLIED the department's default profile (is_default = true) whenever no
-- explicit profileId was supplied, and auto-added its products as
-- source=PROFILE. After the change, profiles are only ever set EXPLICITLY
-- (addVisitDepartment / createVisit with a profileId, or
-- changeVisitDepartmentProfile), and only on departments that do NOT support
-- requests (support_requests = false).
--
-- This migration cleans up the legacy auto-set state:
--   1. visit_departments.profile_id is cleared where the profile was auto-set:
--        a) the profile is the department's default profile, OR
--        b) the department supports requests (support_requests = true) — such
--           departments can never have a profile under the new rule (this also
--           covers child departments, which always support requests and were
--           the main auto-set path).
--   2. The PROFILE-sourced products those visit departments received from the
--      auto-applied profile are removed:
--        a) HARD-deleted when they have NO billing history (nothing references
--           them), or
--        b) SOFT-deleted (deleted = true) when they DO have billing history —
--           visit_billing_items keeps a NOT NULL FK to the product row, so the
--           row must stay (same rule the app enforces: never hard-delete a
--           product with billing history, see B5 in VisitDepartmentService /
--           flushSoftDeletedVisitProducts in VisitBillingService).
--
-- Note: a child visit department that ends up with zero products is NOT
-- deleted here — the app's deleteChildVisitDepartmentIfEmpty handles empty
-- children lazily and its FK guards (notes/diagnostics/billing) make a blind
-- SQL delete unsafe.
--
-- Idempotent: safe to re-run (temp table is dropped first; the profile clear
-- only touches rows that still have a profile_id).
-- ============================================================================

-- 0) Temporary index so the billing-history lookups below do not seq-scan
--    visit_billing_items per product row on large databases.
CREATE INDEX IF NOT EXISTS idx_v5_billing_items_vdp
    ON visit_billing_items (visit_department_product_id);

-- 1) Snapshot the visit departments whose profile was auto-set so the product
--    cleanup and the profile clear operate on the exact same set.
DROP TABLE IF EXISTS v5_auto_set_visit_departments;
CREATE TEMP TABLE v5_auto_set_visit_departments AS
SELECT vd.id AS visit_department_id
FROM visit_departments vd
WHERE vd.profile_id IS NOT NULL
  AND (
        EXISTS (
            SELECT 1 FROM department_profiles dp
            WHERE dp.id = vd.profile_id AND dp.is_default = true
        )
        OR EXISTS (
            SELECT 1 FROM departments d
            WHERE d.id = vd.department_id AND d.support_requests = true
        )
  );

-- 2a) Hard-delete PROFILE products with NO billing history. A product is only
--     hard-deleted when nothing references it: no visit_billing_items rows and
--     no billing-version snapshots (snapshots have no FK, so a dangling
--     reference would silently corrupt invoice/history rendering).
DELETE FROM visit_department_products vdp
USING v5_auto_set_visit_departments t
WHERE vdp.visit_department_id = t.visit_department_id
  AND vdp.source = 'PROFILE'
  AND NOT EXISTS (
        SELECT 1 FROM visit_billing_items vbi
        WHERE vbi.visit_department_product_id = vdp.id
  )
  AND NOT EXISTS (
        SELECT 1 FROM visit_department_product_snapshots s
        WHERE s.visit_department_product_id = vdp.id
  );

-- 2b) Soft-delete PROFILE products WITH billing history (row must stay for the
--     visit_billing_items FK and to preserve the billing snapshot trail).
UPDATE visit_department_products vdp
SET deleted = true,
    updated_at = NOW()
FROM v5_auto_set_visit_departments t
WHERE vdp.visit_department_id = t.visit_department_id
  AND vdp.source = 'PROFILE'
  AND vdp.deleted = false
  AND (
        EXISTS (
            SELECT 1 FROM visit_billing_items vbi
            WHERE vbi.visit_department_product_id = vdp.id
        )
        OR EXISTS (
            SELECT 1 FROM visit_department_product_snapshots s
            WHERE s.visit_department_product_id = vdp.id
        )
  );

-- 3) Clear the auto-set profile links.
UPDATE visit_departments vd
SET profile_id = NULL,
    updated_at = NOW()
FROM v5_auto_set_visit_departments t
WHERE vd.id = t.visit_department_id;

-- 4) Drop the temporary snapshot table and the helper index.
DROP TABLE v5_auto_set_visit_departments;
DROP INDEX IF EXISTS idx_v5_billing_items_vdp;

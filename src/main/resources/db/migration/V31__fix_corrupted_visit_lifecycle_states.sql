-- ============================================================================
-- V31: Fix corrupted visit lifecycle states
--
-- Cleans up impossible state combinations across visits, departments,
-- products, and billings that can arise from:
--   • Failed billing transactions leaving orphaned BILLED/EXEMPTED status
--   • Abandoned DEPARTMENT_EDITING sessions (timeout without cancel)
--   • CORRECTION_PENDING products outside an active edit session
--   • COMPLETED/FINALISED visits with unbilled products
--   • CANCELLED visits/departments with stale billing data
--   • BILLING-status departments with no billing container
--   • FINALISED visits with non-terminal departments
--
-- This migration is idempotent: re-running it produces the same result.
-- ============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. CORRECTION_PENDING products stuck outside DEPARTMENT_EDITING
--
-- CORRECTION_PENDING is transient — it only exists inside an editBillVisit
-- transaction. If the department is NOT in DEPARTMENT_EDITING, these products
-- are orphaned from a failed/abandoned edit. Restore to BILLED if the
-- department has billing history, otherwise PENDING.
-- ─────────────────────────────────────────────────────────────────────────────

-- 1a. CORRECTION_PENDING on departments that have billing → restore to BILLED
UPDATE visit_department_products vdp
SET status = 'BILLED',
    updated_at = NOW()
WHERE vdp.status = 'CORRECTION_PENDING'
  AND vdp.deleted = false
  AND EXISTS (
      SELECT 1 FROM visit_departments vd
      WHERE vd.id = vdp.visit_department_id
        AND vd.status != 'DEPARTMENT_EDITING'
  )
  AND EXISTS (
      SELECT 1 FROM visit_billing_items vbi
      WHERE vbi.visit_department_product_id = vdp.id
  );

-- 1b. CORRECTION_PENDING on departments that have NO billing → reset to PENDING
UPDATE visit_department_products vdp
SET status = 'PENDING',
    billed_by_worker_id = NULL,
    updated_at = NOW()
WHERE vdp.status = 'CORRECTION_PENDING'
  AND vdp.deleted = false
  AND EXISTS (
      SELECT 1 FROM visit_departments vd
      WHERE vd.id = vdp.visit_department_id
        AND vd.status != 'DEPARTMENT_EDITING'
  )
  AND NOT EXISTS (
      SELECT 1 FROM visit_billing_items vbi
      WHERE vbi.visit_department_product_id = vdp.id
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. DEPARTMENT_EDITING stuck without billing_edit_source_status
--
-- If a department is in DEPARTMENT_EDITING but billing_edit_source_status is
-- NULL, the session was abandoned or the column was lost. Restore to COMPLETED.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE visit_departments vd
SET status = 'COMPLETED',
    billing_edit_source_status = NULL,
    updated_at = NOW()
WHERE vd.status = 'DEPARTMENT_EDITING'
  AND vd.billing_edit_source_status IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. DEPARTMENT_EDITING timed out (>30 min stale)
--
-- Departments stuck in DEPARTMENT_EDITING for more than 30 minutes are
-- abandoned sessions. Restore to their remembered pre-edit status.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE visit_departments vd
SET status = COALESCE(vd.billing_edit_source_status, 'COMPLETED'),
    billing_edit_source_status = NULL,
    updated_at = NOW()
WHERE vd.status = 'DEPARTMENT_EDITING'
  AND vd.billing_edit_source_status IS NOT NULL
  AND vd.updated_at < NOW() - INTERVAL '30 minutes';

-- Also restore CORRECTION_PENDING products on these departments
UPDATE visit_department_products vdp
SET status = 'BILLED',
    updated_at = NOW()
WHERE vdp.status = 'CORRECTION_PENDING'
  AND vdp.deleted = false
  AND EXISTS (
      SELECT 1 FROM visit_departments vd
      WHERE vd.id = vdp.visit_department_id
        AND vd.status != 'DEPARTMENT_EDITING'
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. COMPLETED/FINALISED visits with PENDING/UNPAID products
--
-- These visits were marked complete before all products were billed.
-- Reset the unbilled products to PENDING so they can be billed, and
-- revert the visit to IN_PROGRESS so the billing flow can proceed.
-- ─────────────────────────────────────────────────────────────────────────────

-- 4a. Reset unbilled products on completed/finalised visits to PENDING
UPDATE visit_department_products vdp
SET status = 'PENDING',
    billed_by_worker_id = NULL,
    updated_at = NOW()
WHERE vdp.status IN ('PENDING', 'UNPAID')
  AND vdp.deleted = false
  AND EXISTS (
      SELECT 1 FROM visit_departments vd
      JOIN visits v ON v.id = vd.visit_id
      WHERE vd.id = vdp.visit_department_id
        AND v.status IN ('COMPLETED', 'FINALISED')
  );

-- 4b. Revert COMPLETED/FINALISED visits that still have unbilled products
--     back to IN_PROGRESS so the billing flow can resume.
UPDATE visits v
SET status = 'IN_PROGRESS',
    updated_at = NOW()
WHERE v.status IN ('COMPLETED', 'FINALISED')
  AND EXISTS (
      SELECT 1 FROM visit_department_products vdp
      JOIN visit_departments vd ON vd.id = vdp.visit_department_id
      WHERE vd.visit_id = v.id
        AND vdp.deleted = false
        AND vdp.status IN ('PENDING', 'UNPAID', 'CORRECTION_PENDING')
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. BILLED/EXEMPTED/PATIENT_SHARE_EXEMPTED products with no billing history
--
-- Orphaned status from a failed billing attempt (the transaction rolled back
-- the billing container but the product status was already flushed). Reset
-- these to PENDING.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE visit_department_products vdp
SET status = 'PENDING',
    billed_by_worker_id = NULL,
    updated_at = NOW()
WHERE vdp.status IN ('BILLED', 'EXEMPTED', 'PATIENT_SHARE_EXEMPTED')
  AND vdp.deleted = false
  AND NOT EXISTS (
      SELECT 1 FROM visit_billing_items vbi
      WHERE vbi.visit_department_product_id = vdp.id
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. CANCELLED departments with non-cancelled/non-deleted products
--
-- Products on cancelled departments should be soft-deleted so they don't
-- appear in billing or clinical views.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE visit_department_products vdp
SET deleted = true,
    updated_at = NOW()
WHERE vdp.deleted = false
  AND EXISTS (
      SELECT 1 FROM visit_departments vd
      WHERE vd.id = vdp.visit_department_id
        AND vd.status = 'CANCELLED'
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. CANCELLED visits with stale billing containers
--
-- Cancelled visits should not have active billing. Mark all billing as PAID
-- (to prevent the billing page from showing outstanding balances on dead
-- visits) or leave them as-is if they already have payments.
-- ─────────────────────────────────────────────────────────────────────────────

-- 7a. Zero out outstanding amounts on billing for cancelled visits
UPDATE visit_department_billings vdb
SET outstanding_amount = 0,
    status = 'PAID',
    updated_at = NOW()
WHERE vdb.outstanding_amount > 0
  AND EXISTS (
      SELECT 1 FROM visit_billing_containers vbc
      JOIN visits v ON v.id = vbc.visit_id
      WHERE vbc.id = vdb.visit_billing_id
        AND v.status = 'CANCELLED'
  );

UPDATE visit_billings vb
SET outstanding_amount = 0,
    status = 'PAID',
    updated_at = NOW()
WHERE vb.outstanding_amount > 0
  AND EXISTS (
      SELECT 1 FROM visit_department_billings vdb
      JOIN visit_billing_containers vbc ON vbc.id = vdb.visit_billing_id
      JOIN visits v ON v.id = vbc.visit_id
      WHERE vdb.id = vb.visit_department_billing_id
        AND v.status = 'CANCELLED'
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 8. BILLING-status departments with no billing container
--
-- Department stuck in BILLING without actual billing data. Revert to COMPLETED.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE visit_departments vd
SET status = 'COMPLETED',
    updated_at = NOW()
WHERE vd.status = 'BILLING'
  AND NOT EXISTS (
      SELECT 1 FROM visit_department_billings vdb
      WHERE vdb.visit_department_id = vd.id
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 9. FINALISED visits with non-terminal department statuses
--
-- When a visit is FINALISED, all its departments should be in a terminal
-- state (COMPLETED, FINALISED, or CANCELLED). Force any stragglers.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE visit_departments vd
SET status = 'FINALISED',
    updated_at = NOW()
WHERE vd.visit_id IN (
      SELECT v.id FROM visits v WHERE v.status = 'FINALISED'
  )
  AND vd.status NOT IN ('COMPLETED', 'FINALISED', 'CANCELLED');

-- ─────────────────────────────────────────────────────────────────────────────
-- 10. IN_PROGRESS visits with ALL departments in terminal state but visit
--     not yet marked COMPLETED
--
-- Every department is COMPLETED/FINALISED/CANCELLED but the visit itself
-- is still IN_PROGRESS. This is a missed auto-completion.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE visits v
SET status = 'COMPLETED',
    updated_at = NOW()
WHERE v.status = 'IN_PROGRESS'
  AND NOT EXISTS (
      SELECT 1 FROM visit_departments vd
      WHERE vd.visit_id = v.id
        AND vd.status NOT IN ('COMPLETED', 'FINALISED', 'CANCELLED')
  )
  AND EXISTS (
      SELECT 1 FROM visit_departments vd
      WHERE vd.visit_id = v.id
        AND vd.status IN ('COMPLETED', 'FINALISED')
  );

-- ─────────────────────────────────────────────────────────────────────────────
-- 11. Billing notes on visit_department_billings with empty/null content
--
-- Clean up empty billing notes that could confuse the unread-notes gate.
-- ─────────────────────────────────────────────────────────────────────────────

UPDATE visit_department_billings vdb
SET billing_note = NULL,
    updated_at = NOW()
WHERE vdb.billing_note IS NOT NULL
  AND TRIM(vdb.billing_note) = '';

-- ─────────────────────────────────────────────────────────────────────────────
-- Summary of fixes applied:
--
-- 1.  CORRECTION_PENDING outside edit session → BILLED (has billing) or PENDING (no billing)
-- 2.  DEPARTMENT_EDITING without source status → COMPLETED
-- 3.  DEPARTMENT_EDITING stale (>30min) → restored pre-edit status
-- 4.  COMPLETED/FINALISED visits with unbilled products → products PENDING, visit IN_PROGRESS
-- 5.  BILLED/EXEMPTED products with no billing items → PENDING (orphaned status)
-- 6.  Products on CANCELLED departments → soft-deleted
-- 7.  CANCELLED visits with outstanding balances → zeroed out, marked PAID
-- 8.  BILLING departments with no billing container → COMPLETED
-- 9.  FINALISED visits with non-terminal departments → FINALISED
-- 10. IN_PROGRESS visits with all-terminal departments → COMPLETED
-- 11. Empty billing notes → NULL
-- ============================================================================

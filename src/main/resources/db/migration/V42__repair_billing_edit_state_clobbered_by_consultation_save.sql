-- ============================================================================
-- V42: Repair departments whose billing edit mode was silently clobbered.
--
-- Bug (now guarded in code):
--   Consultation saves (VisitService.completeVisit) and standalone-form saves
--   (StandaloneFormService) mark any non-CANCELLED/non-COMPLETED department
--   with unbilled products as BILLING — including departments in
--   DEPARTMENT_EDITING. That overwrite exits edit mode in every UI (edit mode
--   is derived from the status), while its products stay CORRECTION_PENDING.
--   Every restore path (completeBillEditing/cancelBillEditing) then refuses
--   with "Department is not in billing edit mode", and invoice generation /
--   quick-bill stay blocked forever by the fully-billed guards.
--
-- Signature of the clobber: billing_edit_source_status is set but the
-- department is no longer DEPARTMENT_EDITING. All legitimate exits
-- (complete, cancel, editBillVisit success, abandoned-session recovery)
-- clear billing_edit_source_status in the same transaction they restore
-- the status, so this combination can only come from the clobber.
--
-- Repair:
--   1) Restore CORRECTION_PENDING products to BILLED whenever no active edit
--      session exists to finish them (mirrors
--      BillEditingService.restoreCorrectionPendingProducts, which also
--      restores unconditionally to BILLED).
--   2) Restore clobbered departments to their remembered pre-edit status
--      and clear the leftover source marker.
--
-- Departments actually in DEPARTMENT_EDITING (live edit sessions) are left
-- untouched. Idempotent: safe to re-run.
-- ============================================================================

-- 1) Products stuck mid-correction with no active edit session to finish them.
UPDATE visit_department_products
SET status = 'BILLED'
WHERE status = 'CORRECTION_PENDING'
  AND deleted = false
  AND NOT EXISTS (
      SELECT 1
      FROM visit_departments d
      WHERE d.id = visit_department_products.visit_department_id
        AND d.status = 'DEPARTMENT_EDITING'
  );

-- 2) Departments taken out of edit mode by the consultation/form save mixup.
UPDATE visit_departments
SET status = billing_edit_source_status,
    billing_edit_source_status = NULL
WHERE billing_edit_source_status IS NOT NULL
  AND status <> 'DEPARTMENT_EDITING'
  AND status <> billing_edit_source_status;

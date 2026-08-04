-- ============================================================================
-- V9: Soft-deactivation flag for patient insurances
--
-- A patient insurance that has already been used (linked to a visit or applied
-- to a billing line) can no longer be hard-deleted — the billing/audit trail
-- references it. From now on `deletePatientInsurance` deactivates such records
-- instead of deleting them, and deactivated policies are excluded from billing
-- and new visit links.
--
-- `deactivated` defaults to FALSE so existing rows remain active.
-- Idempotent: safe to re-run.
-- ============================================================================

ALTER TABLE patient_insurances ADD COLUMN IF NOT EXISTS deactivated BOOLEAN NOT NULL DEFAULT FALSE;

-- ============================================================================
-- V53: Add last_return_reason column to visits table
-- ============================================================================

ALTER TABLE visits
    ADD COLUMN IF NOT EXISTS last_return_reason TEXT;

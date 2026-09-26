-- ============================================================================
-- V36: Add started_at timestamp to visit_departments
-- ============================================================================

ALTER TABLE visit_departments
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP;

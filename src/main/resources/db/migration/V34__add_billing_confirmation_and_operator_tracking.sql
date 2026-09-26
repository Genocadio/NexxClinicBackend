-- ============================================================================
-- V34: Add Billing Confirmation Status and Operator Tracking to Products
-- ============================================================================

ALTER TABLE visit_department_products
    ADD COLUMN IF NOT EXISTS confirmed_by_worker_id UUID REFERENCES workers(id),
    ADD COLUMN IF NOT EXISTS billing_confirmation_status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED';

CREATE INDEX IF NOT EXISTS idx_vdp_dept_confirmation
    ON visit_department_products (visit_department_id, billing_confirmation_status);

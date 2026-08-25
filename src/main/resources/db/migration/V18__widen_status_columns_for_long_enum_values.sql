-- Widen status columns that store VisitProductStatus enum values.
-- PATIENT_SHARE_EXEMPTED (23 chars) and CORRECTION_PENDING (18 chars)
-- exceed the original VARCHAR(16) limit.

ALTER TABLE visit_department_products
    ALTER COLUMN status TYPE VARCHAR(32);

ALTER TABLE visit_department_product_snapshots
    ALTER COLUMN status TYPE VARCHAR(32);

-- Drop the old CHECK constraints that are missing PATIENT_SHARE_EXEMPTED
ALTER TABLE visit_department_products
    DROP CONSTRAINT IF EXISTS visit_department_products_status_check;

ALTER TABLE visit_department_product_snapshots
    DROP CONSTRAINT IF EXISTS visit_department_product_snapshots_status_check;

-- Re-create with all VisitProductStatus enum values including PATIENT_SHARE_EXEMPTED
ALTER TABLE visit_department_products
    ADD CONSTRAINT visit_department_products_status_check
    CHECK (status IN ('BILLED', 'EXEMPTED', 'PATIENT_SHARE_EXEMPTED', 'CORRECTION_PENDING', 'UNPAID', 'PENDING'));

ALTER TABLE visit_department_product_snapshots
    ADD CONSTRAINT visit_department_product_snapshots_status_check
    CHECK (status IN ('BILLED', 'EXEMPTED', 'PATIENT_SHARE_EXEMPTED', 'CORRECTION_PENDING', 'UNPAID', 'PENDING'));

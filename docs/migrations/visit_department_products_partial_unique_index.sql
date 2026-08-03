-- ============================================================================
-- Migration: Partial unique index on visit_department_products
--
-- Problem
-- -------
-- The JPA entity declares a plain unique constraint on
-- (visit_department_id, product_id). Because soft-deleted rows (deleted = true)
-- are kept in the table to preserve billing history, the constraint prevents a
-- re-added product from coexisting with its own soft-deleted row. The current
-- code works around this by un-deleting the soft-deleted row, but a concurrent
-- insert can still hit the constraint (race condition).
--
-- Fix
-- ---
-- Replace the full unique constraint with a PARTIAL unique index that only
-- applies to active rows (deleted = false). Soft-deleted rows may then exist
-- alongside a re-added active row with the same (visit_department_id, product_id).
--
-- SUPERSEDED BY FLYWAY
-- ---------------------
-- This migration is now managed automatically by Flyway as
-- src/main/resources/db/migration/V2__visit_department_products_partial_unique_index.sql
-- (executed on fresh AND existing databases at application startup).
-- Keep this file as documentation/reference only — do NOT run it manually
-- alongside Flyway.
-- ============================================================================

-- 1) Drop the old full unique constraint (name may differ across environments).
ALTER TABLE visit_department_products
    DROP CONSTRAINT IF EXISTS uk_visit_department_product;

DROP INDEX IF EXISTS uk_visit_department_product;

-- 2) Create the partial unique index over active rows only.
CREATE UNIQUE INDEX uk_visit_department_product_active
    ON visit_department_products (visit_department_id, product_id)
    WHERE deleted = false;

-- 3) (Optional, once verified) Update the JPA entity:
--      @Table(name = "visit_department_products")
--    (remove the uniqueConstraints attribute) so ddl-auto=update never
--    recreates the full constraint on fresh databases.

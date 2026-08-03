-- ============================================================================
-- V2: Partial unique index on visit_department_products
-- Reference : docs/visit-billing-flows-analysis.md — Structural Issue S3 / Rule 7
--
-- Problem
-- -------
-- Soft-deleted rows (deleted = true) are kept to preserve billing history, so a
-- full unique constraint on (visit_department_id, product_id) prevents a
-- re-added product from coexisting with its own soft-deleted row. JPA cannot
-- express partial indexes, so this migration is the single source of truth.
--
-- Fresh databases : V1 creates the table WITHOUT the full constraint (the JPA
--                   @UniqueConstraint was removed); this file adds the partial
--                   index.
-- Existing DBs    : baselined at V1, so this file must also DROP the legacy
--                   full constraint that ddl-auto=update may have created.
--
-- Safe to run repeatedly (idempotent).
-- ============================================================================

-- 1) Drop any legacy full unique constraint/index on (visit_department_id, product_id).
--    JPA/ddl-auto may have auto-named it differently per environment, so look it
--    up by columns instead of guessing a name (a wrong guess silently leaves the
--    old full constraint in place, defeating this migration).
DO $$
DECLARE
    con record;
BEGIN
    FOR con IN
        SELECT c.conname
        FROM pg_constraint c
        WHERE c.conrelid = 'visit_department_products'::regclass
          AND c.contype = 'u'
          AND c.conkey = ARRAY[
              (SELECT attnum FROM pg_attribute
               WHERE attrelid = 'visit_department_products'::regclass
                 AND attname = 'visit_department_id'),
              (SELECT attnum FROM pg_attribute
               WHERE attrelid = 'visit_department_products'::regclass
                 AND attname = 'product_id')
          ]
    LOOP
        EXECUTE 'ALTER TABLE visit_department_products DROP CONSTRAINT ' || quote_ident(con.conname);
    END LOOP;
END $$;

DROP INDEX IF EXISTS uk_visit_department_product;

-- 2) Partial unique index over active rows only.
DROP INDEX IF EXISTS uk_visit_department_product_active;
CREATE UNIQUE INDEX uk_visit_department_product_active
    ON visit_department_products (visit_department_id, product_id)
    WHERE deleted = false;

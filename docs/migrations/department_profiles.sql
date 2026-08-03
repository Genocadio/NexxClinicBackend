-- ============================================================================
-- Department Profiles (replaces department_default_products)
--
-- SUPERSEDED BY FLYWAY
-- --------------------
-- This migration is now managed automatically by Flyway as
-- src/main/resources/db/migration/V4__department_profiles.sql
-- (executed on fresh AND existing databases at application startup).
-- Keep this file as documentation/reference only — do NOT run it manually
-- alongside Flyway.
--
-- What it did (kept for reference): JPA ddl-auto=update creates the new
-- tables/columns on fresh databases, but existing databases need this file, and
-- the partial unique index / backfill / drop of the legacy table are NOT
-- expressible in JPA annotations — they must be applied manually here.
--
-- What this migration does:
--   1. Creates department_profiles (a named set of products a department offers)
--   2. Creates department_profile_products (link between a profile and a product)
--   3. Adds visit_departments.profile_id (which profile a visit department used)
--   4. Adds visit_department_products.source (USER | PROFILE origin of the row)
--   5. Backfills: for every department that had legacy default products, creates a
--      "Default" profile (is_default = true) and moves those products into it
--   6. Drops the legacy department_default_products table
-- ============================================================================

-- 1) department_profiles ------------------------------------------------------
CREATE TABLE IF NOT EXISTS department_profiles (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES departments(id),
    name VARCHAR(150) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_department_profile_name UNIQUE (department_id, name)
);

-- At most ONE default profile per department (the single-default rule the
-- backend enforces in DepartmentService.syncProfiles).
CREATE UNIQUE INDEX IF NOT EXISTS uk_department_profile_single_default
    ON department_profiles (department_id)
    WHERE is_default = true;

CREATE INDEX IF NOT EXISTS idx_department_profile_department
    ON department_profiles (department_id);

-- 2) department_profile_products ----------------------------------------------
CREATE TABLE IF NOT EXISTS department_profile_products (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES department_profiles(id),
    product_id UUID NOT NULL REFERENCES products(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_department_profile_product UNIQUE (profile_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_department_profile_product_profile
    ON department_profile_products (profile_id);
CREATE INDEX IF NOT EXISTS idx_department_profile_product_product
    ON department_profile_products (product_id);

-- 3) visit_departments.profile_id ----------------------------------------------
ALTER TABLE visit_departments ADD COLUMN IF NOT EXISTS profile_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_visit_departments_profile'
    ) THEN
        ALTER TABLE visit_departments
            ADD CONSTRAINT fk_visit_departments_profile
            FOREIGN KEY (profile_id) REFERENCES department_profiles(id);
    END IF;
END $$;

-- 4) visit_department_products.source -------------------------------------------
-- Existing rows were all added manually → USER. New rows default to USER in JPA.
ALTER TABLE visit_department_products ADD COLUMN IF NOT EXISTS source VARCHAR(16) NOT NULL DEFAULT 'USER';

-- 5) Backfill legacy default products into a "Default" profile -------------------
-- Only for departments that actually had default products. Departments with none
-- are left without profiles (a profile is optional; a profile may have 0 products).
INSERT INTO department_profiles (id, department_id, name, is_default, created_at, updated_at)
SELECT gen_random_uuid(), ddp.department_id, 'Default', true, NOW(), NOW()
FROM department_default_products ddp
WHERE NOT EXISTS (
    SELECT 1 FROM department_profiles p
    WHERE p.department_id = ddp.department_id
);

INSERT INTO department_profile_products (id, profile_id, product_id, created_at, updated_at)
SELECT gen_random_uuid(), p.id, ddp.product_id, NOW(), NOW()
FROM department_default_products ddp
JOIN department_profiles p ON p.department_id = ddp.department_id AND p.is_default = true
ON CONFLICT (profile_id, product_id) DO NOTHING;

-- 6) Drop the legacy table ------------------------------------------------------
DROP TABLE IF EXISTS department_default_products;

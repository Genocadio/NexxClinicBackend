-- ============================================================================
-- V4: Department Profiles (replaces department_default_products)
--
-- Fresh databases : V1 already created department_profiles,
--                   department_profile_products, visit_departments.profile_id
--                   and visit_department_products.source. This file only adds
--                   what JPA cannot express (single-default partial index) and
--                   the legacy backfill is skipped (no legacy table).
-- Existing DBs    : baselined at V1; this file creates the tables/columns and
--                   backfills legacy default products into a "Default" profile,
--                   then drops the legacy table. All statements are idempotent
--                   so re-running (e.g. after a manual run) is safe.
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

-- At most ONE default profile per department (backend rule in
-- DepartmentService.syncProfiles). Not expressible in JPA → always needed.
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

-- 3) visit_departments.profile_id ---------------------------------------------
-- Guarded so a fresh DB (FK already created by V1 with an auto name) does not
-- end up with a second, differently-named FK on the same column.
ALTER TABLE visit_departments ADD COLUMN IF NOT EXISTS profile_id UUID;
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'visit_departments'::regclass
          AND contype = 'f'
          AND confrelid = 'department_profiles'::regclass
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
-- Only runs when the legacy table still exists (prod may already have run the
-- manual migration which drops it). Departments with no default products are
-- left without profiles (a profile is optional; a profile may have 0 products).
DO $$
BEGIN
    IF to_regclass('department_default_products') IS NOT NULL THEN
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
    END IF;
END $$;

-- 6) Drop the legacy table -----------------------------------------------------
DROP TABLE IF EXISTS department_default_products;

-- Rename table from insurance_coverage_rules to insurance_coverage
ALTER TABLE insurance_coverage_rules RENAME TO insurance_coverage;

-- Rename constraints and indexes to match new table name
ALTER INDEX IF EXISTS idx_coverage_rule_provider RENAME TO idx_coverage_provider;
ALTER INDEX IF EXISTS idx_coverage_rule_dept RENAME TO idx_coverage_dept;
ALTER INDEX IF EXISTS uk_coverage_rule RENAME TO uk_coverage;

-- Backfill: For every provider that has a default_patient_share_percentage
-- but does NOT already have a base coverage row (department=NULL, encounter_type=NULL)
-- in insurance_coverage, insert one. This migrates the old simple percentage
-- into the new multi-tier coverage model.
INSERT INTO insurance_coverage (id, insurance_provider_id, department_id, encounter_type, patient_share_percentage, created_at, updated_at)
SELECT gen_random_uuid(),
       p.id,
       NULL,
       NULL,
       p.default_patient_share_percentage,
       NOW(),
       NOW()
FROM insurance_providers p
WHERE p.default_patient_share_percentage IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM insurance_coverage c
    WHERE c.insurance_provider_id = p.id
      AND c.department_id IS NULL
      AND c.encounter_type IS NULL
  );

-- Drop the old default_patient_share_percentage column from insurance_providers.
-- The base coverage is now defined as a row in insurance_coverage with
-- department=NULL and encounter_type=NULL.
ALTER TABLE insurance_providers DROP COLUMN IF EXISTS default_patient_share_percentage;

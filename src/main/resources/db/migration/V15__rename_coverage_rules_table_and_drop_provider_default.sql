-- Rename table from insurance_coverage_rules to insurance_coverage
ALTER TABLE insurance_coverage_rules RENAME TO insurance_coverage;

-- Rename constraints and indexes to match new table name
ALTER INDEX IF EXISTS idx_coverage_rule_provider RENAME TO idx_coverage_provider;
ALTER INDEX IF EXISTS idx_coverage_rule_dept RENAME TO idx_coverage_dept;
ALTER INDEX IF EXISTS uk_coverage_rule RENAME TO uk_coverage;

-- Drop the old default_patient_share_percentage column from insurance_providers.
-- The base coverage is now defined as a row in insurance_coverage with
-- department=NULL and encounter_type=NULL.
ALTER TABLE insurance_providers DROP COLUMN IF EXISTS default_patient_share_percentage;

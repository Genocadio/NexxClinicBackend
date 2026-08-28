-- Allow multiple coverage rules per condition set so the same
-- insurance provider + department + encounter type can have
-- different patient share percentages (e.g. 10% and 15%).
ALTER TABLE insurance_coverage DROP CONSTRAINT IF EXISTS uk_coverage;

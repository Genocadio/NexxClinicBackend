-- Safety-net migration: backfill base coverage rows for providers that
-- have NO coverage in insurance_coverage yet.
-- This handles the case where V15 already ran without backfilling
-- the old default_patient_share_percentage values.
-- Since the column is already dropped, we use a sensible default (0%)
-- and the admin can update providers with the correct percentages.
INSERT INTO insurance_coverage (id, insurance_provider_id, department_id, encounter_type, patient_share_percentage, created_at, updated_at)
SELECT gen_random_uuid(),
       p.id,
       NULL,
       NULL,
       0,
       NOW(),
       NOW()
FROM insurance_providers p
WHERE NOT EXISTS (
    SELECT 1 FROM insurance_coverage c
    WHERE c.insurance_provider_id = p.id
      AND c.department_id IS NULL
      AND c.encounter_type IS NULL
);

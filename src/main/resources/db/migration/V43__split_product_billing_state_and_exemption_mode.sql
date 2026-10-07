-- V43: separate the billing lifecycle state from the exemption mode on visit products.
--
-- Until now VisitProductStatus conflated both facts:
--   * "was it billed?"  → BILLED vs EXEMPTED vs PATIENT_SHARE_EXEMPTED (billed)
--                         PENDING / UNPAID (needs billing) / CORRECTION_PENDING (mid-edit)
--   * "how exempted?"   → encoded into the terminal status itself
--
-- This adds two authoritative columns and backfills them from the legacy status.
-- The legacy `status` column is kept and remains mirrored by the application on
-- every write, so existing reports/queries and the GraphQL `status` field are
-- unaffected.

ALTER TABLE visit_department_products ADD COLUMN billing_state VARCHAR(16);
ALTER TABLE visit_department_products ADD COLUMN exemption_mode VARCHAR(16);

UPDATE visit_department_products
SET billing_state = CASE status
        WHEN 'BILLED' THEN 'BILLED'
        WHEN 'EXEMPTED' THEN 'BILLED'
        WHEN 'PATIENT_SHARE_EXEMPTED' THEN 'BILLED'
        WHEN 'CORRECTION_PENDING' THEN 'CORRECTING'
        ELSE 'UNBILLED'
    END,
    exemption_mode = CASE status
        WHEN 'EXEMPTED' THEN 'FULL'
        WHEN 'PATIENT_SHARE_EXEMPTED' THEN 'PATIENT_SHARE'
        ELSE 'NONE'
    END;

ALTER TABLE visit_department_products ALTER COLUMN billing_state SET NOT NULL;
ALTER TABLE visit_department_products ALTER COLUMN exemption_mode SET NOT NULL;

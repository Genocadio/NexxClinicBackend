-- Add BILL_EDITING status to visits for the billing-edit workflow.
-- BILL_EDITING is a transitional status: COMPLETED → BILL_EDITING → COMPLETED.
-- It gates billing, product, and insurance mutations.

ALTER TABLE visits
    DROP CONSTRAINT IF EXISTS visits_status_check;

ALTER TABLE visits
    ADD CONSTRAINT visits_status_check
    CHECK (status IN ('CREATED', 'IN_PROGRESS', 'CANCELLED', 'COMPLETED', 'BILL_EDITING'));

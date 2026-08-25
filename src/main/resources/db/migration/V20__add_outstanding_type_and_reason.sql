-- Add outstanding classification (LOAN/GIVEAWAY) and reason to billing buckets.
-- Allows tracking whether unpaid amounts are patient debt or clinic write-offs.

ALTER TABLE visit_billings
    ADD COLUMN outstanding_type VARCHAR(16);

ALTER TABLE visit_billings
    ADD COLUMN outstanding_reason VARCHAR(500);

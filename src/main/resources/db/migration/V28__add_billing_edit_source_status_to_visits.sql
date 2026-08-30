-- V28: Track the visit status that existed before entering BILL_EDITING.
--
-- BILL_EDITING was previously only reachable from COMPLETED, so exiting the
-- mode always restored COMPLETED. Now a PENDING (CREATED/IN_PROGRESS) visit
-- that has been billed can also enter billing edit mode; that visit must be
-- returned to its own pre-edit status (CREATED/IN_PROGRESS) when the session
-- is completed or cancelled instead of being force-completed.
--
-- Nullable: rows created before this migration have no remembered source.
-- When null, exiting BILL_EDITING falls back to COMPLETED (legacy behaviour).

ALTER TABLE visits
    ADD COLUMN billing_edit_source_status VARCHAR(20);
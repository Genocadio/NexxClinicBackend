-- Add billing_date column: the date/time shown on the invoice.
-- Defaults to created_at for existing rows; can be overridden by admin/manager.
ALTER TABLE visit_billings ADD COLUMN billing_date TIMESTAMP;

-- Backfill existing rows so the default matches the current created_at value.
UPDATE visit_billings SET billing_date = created_at WHERE billing_date IS NULL;

-- For new rows the JPA @PrePersist sets billingDate = createdAt, but the column
-- itself is nullable so the entity can be loaded without migration issues.

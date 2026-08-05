-- ============================================================================
-- V10: "Add as not paid" flags for products and insurance coverages
--
-- A product (or a product-insurance coverage) can be marked as NOT PAID. When
-- the flag is TRUE the line is billed at 0 RWF without any price lookup:
--   - products.not_paid = TRUE   -> PRIVATE lines bill at 0
--   - product_insurance_coverages.not_paid = TRUE -> INSURANCE lines for that
--     coverage bill at 0
-- This lets the clinic decide per-case whether an item is added to the bill as
-- paid or not paid, independently for private billing and for each insurer.
--
-- The flag defaults to FALSE so existing rows remain paid as before.
-- Idempotent: safe to re-run.
-- ============================================================================

ALTER TABLE products ADD COLUMN IF NOT EXISTS not_paid BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE product_insurance_coverages ADD COLUMN IF NOT EXISTS not_paid BOOLEAN NOT NULL DEFAULT FALSE;

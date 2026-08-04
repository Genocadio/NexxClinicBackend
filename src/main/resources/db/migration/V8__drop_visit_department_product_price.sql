-- ============================================================================
-- V8: Drop the price snapshot column from visit_department_products
--
-- Prices are no longer stored on the live visit-department-product row. Both
-- add-time and bill-time resolve the price from the same source (product
-- catalog clinicPrice/privateRhicPrice, or the insurance coverage cost when an
-- insurance applies — see BillingPricingCalculator.resolveDefaultUnitPrice), so
-- the "DTO shows X, bill shows Y" bug is structurally impossible.
--
-- The immutable billed price continues to live in the billing snapshot rows
-- (visit_department_product_snapshots.unit_price / visit_billing_items
-- .unit_price_snapshot), so invoices and billing history are unaffected.
--
-- Safe to run repeatedly (idempotent).
-- ============================================================================

ALTER TABLE visit_department_products DROP COLUMN IF EXISTS price;

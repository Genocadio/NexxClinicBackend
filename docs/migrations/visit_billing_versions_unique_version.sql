-- ============================================================================
-- Migration: Unique (visit_id, version) on visit_billing_versions
--
-- Problem
-- -------
-- createNextBillingVersion does findFirstByVisitIdOrderByVersionDesc then inserts
-- version = latest + 1. Under two concurrent billVisit/editBillVisit calls for the
-- same visit, both read version N and both insert version N+1 — duplicate version
-- numbers. The "latest version" guards in recordVisitBillingPayment / generateInvoice
-- (findFirstByVisitIdOrderByVersionDesc) then become non-deterministic, and billing
-- containers can collide.
--
-- Fix
-- ---
-- A unique constraint on (visit_id, version). The JPA entity now declares it too
-- (@UniqueConstraint on VisitBillingVersion), so fresh databases get it via
-- ddl-auto=update. This SQL is required for EXISTING databases where ddl-auto=update
-- does not add constraints to tables that already exist.
--
-- SUPERSEDED BY FLYWAY
-- ---------------------
-- This migration is now managed automatically by Flyway as
-- src/main/resources/db/migration/V3__visit_billing_versions_unique_version.sql
-- (executed on fresh AND existing databases at application startup).
-- Keep this file as documentation/reference only — do NOT run it manually
-- alongside Flyway.
--
-- NOTE: if duplicate (visit_id, version) rows already exist in an environment,
-- the Flyway migration will FAIL until they are deduplicated (verify with:
--     SELECT visit_id, version, count(*)
--     FROM visit_billing_versions
--     GROUP BY visit_id, version
--     HAVING count(*) > 1;)
-- ============================================================================

DROP INDEX IF EXISTS uk_visit_billing_version;

CREATE UNIQUE INDEX uk_visit_billing_version
    ON visit_billing_versions (visit_id, version);

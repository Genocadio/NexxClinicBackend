-- ============================================================================
-- V47: Indexes for foreign-key columns
--
-- Problem
-- -------
-- 56 foreign-key columns across ~30 tables have no index in which the FK column
-- is the LEADING column. Postgres cannot use an index to drive a join or a
-- cascade check when the FK column is not leading, so every such join degrades
-- to a sequential scan once the table outgrows a few pages. At the current row
-- counts (14 visits, 10 workers) this is invisible; it becomes a table scan per
-- request at scale. Adding them now is effectively free.
--
-- Notable high-value targets:
--   * refresh_tokens.worker_id -- pruned daily by TokenCleanupScheduler and
--     queried on every token refresh; was seq-scanned on each auth request.
--   * visit_department_products.* -- the four worker-attribute FKs plus
--     product_id, all joined when reconstructing billing history.
--   * visit_department_product_snapshots.* -- six FKs, the frozen billing
--     snapshots read during invoice regeneration.
--   * visit_billings / visit_billing_items / visit_billing_payments /
--     visit_billing_containers / visit_department_billings -- the whole billing
--     chain was joined on unindexed columns.
--
-- Naming: idx_<table>_<col>, abbreviated where the full name would exceed
-- Postgres's 63-char identifier limit (vdps = visit_department_product_snapshots,
-- vipr = visit_pre_instruction_product_requests, vipm =
-- visit_pre_instruction_medications, vdnv = visit_department_note_viewers).
--
-- All statements are idempotent so this is safe to re-run on a baselined DB.
-- Plain (non-CONCURRENT) CREATE INDEX: Flyway runs migrations inside a
-- transaction, and CONCURRENTLY cannot run in one. Locks are irrelevant at
-- these row counts.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- consultation_answers
-- ---------------------------------------------------------------------------

-- Submitted-form history is read per visit; these three FKs were unindexed.
CREATE INDEX IF NOT EXISTS idx_consultation_answers_department_form_id
    ON consultation_answers (department_form_id);

CREATE INDEX IF NOT EXISTS idx_consultation_answers_department_id
    ON consultation_answers (department_id);

CREATE INDEX IF NOT EXISTS idx_consultation_answers_submitted_by_worker_id
    ON consultation_answers (submitted_by_worker_id);

-- ---------------------------------------------------------------------------
-- department_standalone_forms
-- ---------------------------------------------------------------------------

-- The form<->department link table is scanned whenever a department's forms
-- are resolved; standalone_form_id is not leading on its composite PK.
CREATE INDEX IF NOT EXISTS idx_department_standalone_forms_standalone_form_id
    ON department_standalone_forms (standalone_form_id);

-- ---------------------------------------------------------------------------
-- note_target_users
-- ---------------------------------------------------------------------------

-- Join table for per-user note visibility; both FKs are unindexed.
CREATE INDEX IF NOT EXISTS idx_note_target_users_note_id
    ON note_target_users (note_id);

CREATE INDEX IF NOT EXISTS idx_note_target_users_worker_id
    ON note_target_users (worker_id);

-- ---------------------------------------------------------------------------
-- password_history
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_password_history_worker_id
    ON password_history (worker_id);

-- ---------------------------------------------------------------------------
-- patient_insurances
-- ---------------------------------------------------------------------------

-- patient_id is already indexed (V39); the provider FK was not, so resolving
-- coverage rules per provider seq-scanned the table.
CREATE INDEX IF NOT EXISTS idx_patient_insurances_insurance_provider_id
    ON patient_insurances (insurance_provider_id);

-- ---------------------------------------------------------------------------
-- product_coverage_authorization_reasons
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_product_coverage_authorization_reasons_coverage_id
    ON product_coverage_authorization_reasons (coverage_id);

-- ---------------------------------------------------------------------------
-- refresh_tokens
-- ---------------------------------------------------------------------------

-- Highest-value index in this migration: TokenCleanupScheduler purges by
-- expires_at daily and every token refresh looks up by worker. worker_id had
-- no index at all.
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_worker_id
    ON refresh_tokens (worker_id);

-- ---------------------------------------------------------------------------
-- visit_billing_containers
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_visit_billing_containers_billing_version_id
    ON visit_billing_containers (billing_version_id);

CREATE INDEX IF NOT EXISTS idx_visit_billing_containers_visit_id
    ON visit_billing_containers (visit_id);

-- ---------------------------------------------------------------------------
-- visit_billing_items
-- ---------------------------------------------------------------------------

-- Four unindexed FKs on the invoice line-item table; all are joined when
-- rebuilding or correcting a bill.
CREATE INDEX IF NOT EXISTS idx_visit_billing_items_applied_patient_insurance_id
    ON visit_billing_items (applied_patient_insurance_id);

CREATE INDEX IF NOT EXISTS idx_visit_billing_items_billing_version_id
    ON visit_billing_items (billing_version_id);

CREATE INDEX IF NOT EXISTS idx_visit_billing_items_department_insurance_billing_id
    ON visit_billing_items (department_insurance_billing_id);

CREATE INDEX IF NOT EXISTS idx_visit_billing_items_visit_department_product_id
    ON visit_billing_items (visit_department_product_id);

-- ---------------------------------------------------------------------------
-- visit_billing_payments
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_visit_billing_payments_billing_version_id
    ON visit_billing_payments (billing_version_id);

CREATE INDEX IF NOT EXISTS idx_visit_billing_payments_department_insurance_billing_id
    ON visit_billing_payments (department_insurance_billing_id);

CREATE INDEX IF NOT EXISTS idx_visit_billing_payments_visit_department_billing_id
    ON visit_billing_payments (visit_department_billing_id);

-- ---------------------------------------------------------------------------
-- visit_billings
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_visit_billings_billing_version_id
    ON visit_billings (billing_version_id);

CREATE INDEX IF NOT EXISTS idx_visit_billings_patient_insurance_id
    ON visit_billings (patient_insurance_id);

CREATE INDEX IF NOT EXISTS idx_visit_billings_visit_department_billing_id
    ON visit_billings (visit_department_billing_id);

-- ---------------------------------------------------------------------------
-- visit_department_billings
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_visit_department_billings_visit_billing_id
    ON visit_department_billings (visit_billing_id);

CREATE INDEX IF NOT EXISTS idx_visit_department_billings_visit_department_id
    ON visit_department_billings (visit_department_id);

-- ---------------------------------------------------------------------------
-- visit_department_diagnostics / visit_department_medications
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_visit_department_diagnostics_visit_department_id
    ON visit_department_diagnostics (visit_department_id);

CREATE INDEX IF NOT EXISTS idx_visit_department_medications_visit_department_id
    ON visit_department_medications (visit_department_id);

-- ---------------------------------------------------------------------------
-- visit_department_note_viewers / visit_department_notes
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_vdnv_viewer_id
    ON visit_department_note_viewers (viewer_id);

CREATE INDEX IF NOT EXISTS idx_visit_department_notes_created_by_id
    ON visit_department_notes (created_by_id);

CREATE INDEX IF NOT EXISTS idx_visit_department_notes_visit_department_id
    ON visit_department_notes (visit_department_id);

-- ---------------------------------------------------------------------------
-- visit_department_product_snapshots
-- ---------------------------------------------------------------------------

-- Six unindexed FKs. These are the frozen billing snapshots read during
-- invoice regeneration (BillingVersionBuilder), so every regeneration joined
-- on unindexed columns.
CREATE INDEX IF NOT EXISTS idx_vdps_added_by_worker_id
    ON visit_department_product_snapshots (added_by_worker_id);

CREATE INDEX IF NOT EXISTS idx_vdps_applied_patient_insurance_id
    ON visit_department_product_snapshots (applied_patient_insurance_id);

CREATE INDEX IF NOT EXISTS idx_vdps_billed_by_worker_id
    ON visit_department_product_snapshots (billed_by_worker_id);

CREATE INDEX IF NOT EXISTS idx_vdps_billing_version_id
    ON visit_department_product_snapshots (billing_version_id);

CREATE INDEX IF NOT EXISTS idx_vdps_product_id
    ON visit_department_product_snapshots (product_id);

CREATE INDEX IF NOT EXISTS idx_vdps_visit_department_id
    ON visit_department_product_snapshots (visit_department_id);

-- ---------------------------------------------------------------------------
-- visit_department_products
-- ---------------------------------------------------------------------------

-- product_id plus the four worker-attribute FKs (added_by / confirmed_by /
-- processor / billed_by). All are joined when reconstructing a visit's product
-- history. Note: the partial unique index uk_visit_department_product_active
-- (V2) leads with visit_department_id and does not cover these.
CREATE INDEX IF NOT EXISTS idx_visit_department_products_added_by_worker_id
    ON visit_department_products (added_by_worker_id);

CREATE INDEX IF NOT EXISTS idx_visit_department_products_billed_by_worker_id
    ON visit_department_products (billed_by_worker_id);

CREATE INDEX IF NOT EXISTS idx_visit_department_products_confirmed_by_worker_id
    ON visit_department_products (confirmed_by_worker_id);

CREATE INDEX IF NOT EXISTS idx_visit_department_products_processor_worker_id
    ON visit_department_products (processor_worker_id);

CREATE INDEX IF NOT EXISTS idx_visit_department_products_product_id
    ON visit_department_products (product_id);

-- ---------------------------------------------------------------------------
-- visit_insurances
-- ---------------------------------------------------------------------------

-- visit_id is covered by uk_visit_patient_insurance; patient_insurance_id is
-- not leading there.
CREATE INDEX IF NOT EXISTS idx_visit_insurances_patient_insurance_id
    ON visit_insurances (patient_insurance_id);

-- ---------------------------------------------------------------------------
-- visit_pre_instructions / _medications / _product_requests
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_visit_pre_instructions_added_by_id
    ON visit_pre_instructions (added_by_id);

CREATE INDEX IF NOT EXISTS idx_visit_pre_instructions_visit_department_id
    ON visit_pre_instructions (visit_department_id);

CREATE INDEX IF NOT EXISTS idx_visit_pre_instructions_visit_id
    ON visit_pre_instructions (visit_id);

CREATE INDEX IF NOT EXISTS idx_vipm_pre_instruction_id
    ON visit_pre_instruction_medications (pre_instruction_id);

CREATE INDEX IF NOT EXISTS idx_vipr_pre_instruction_id
    ON visit_pre_instruction_product_requests (pre_instruction_id);

CREATE INDEX IF NOT EXISTS idx_vipr_processed_by_id
    ON visit_pre_instruction_product_requests (processed_by_id);

CREATE INDEX IF NOT EXISTS idx_vipr_product_id
    ON visit_pre_instruction_product_requests (product_id);

CREATE INDEX IF NOT EXISTS idx_vipr_requested_by_id
    ON visit_pre_instruction_product_requests (requested_by_id);

-- ---------------------------------------------------------------------------
-- visit_price_estimates
-- ---------------------------------------------------------------------------

-- visit_id is already indexed (idx_visit_price_estimates_visit); the
-- insurance FK was not, so resolving applied coverage seq-scanned.
CREATE INDEX IF NOT EXISTS idx_visit_price_estimates_applied_patient_insurance_id
    ON visit_price_estimates (applied_patient_insurance_id);

-- ---------------------------------------------------------------------------
-- visit_vital_signs_groups / vital_measurements
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_visit_vital_signs_groups_added_by_id
    ON visit_vital_signs_groups (added_by_id);

CREATE INDEX IF NOT EXISTS idx_visit_vital_signs_groups_visit_id
    ON visit_vital_signs_groups (visit_id);

-- group_id is not leading on vital_measurements' composite PK.
CREATE INDEX IF NOT EXISTS idx_vital_measurements_group_id
    ON vital_measurements (group_id);

-- ---------------------------------------------------------------------------
-- worker_departments / worker_roles / workers
-- ---------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_worker_departments_department_id
    ON worker_departments (department_id);

-- worker_roles' composite PK leads with role_id, so lookups by worker (the
-- common direction for auth) had no usable index.
CREATE INDEX IF NOT EXISTS idx_worker_roles_worker_id
    ON worker_roles (worker_id);

-- workers.department_id supports listing staff by department.
CREATE INDEX IF NOT EXISTS idx_workers_department_id
    ON workers (department_id);

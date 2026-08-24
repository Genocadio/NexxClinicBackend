#!/usr/bin/env bash
# ============================================================================
# NexxClinic Local Seed Script
# Drops the local DB, lets Spring Boot apply Flyway migrations, then inserts
# demo data. Invoke manually: ./seed-local.sh
# ============================================================================
set -euo pipefail

DB_NAME="${DB_NAME:-nexxclinic}"
DB_USER="${DB_USER:-nexxclinic_user}"
PG="docker exec -i nexxclinic-postgres psql -U $DB_USER -d $DB_NAME"

echo "🗑  Dropping and recreating database..."
docker exec nexxclinic-postgres psql -U "$DB_USER" -d postgres -c \
  "DROP DATABASE IF EXISTS $DB_NAME;" 2>/dev/null
docker exec nexxclinic-postgres psql -U "$DB_USER" -d postgres -c \
  "CREATE DATABASE $DB_NAME;" 2>/dev/null

echo "🔄  Starting backend to apply Flyway migrations..."
cd "$(dirname "$0")"
nohup ./gradlew bootRun > /tmp/nexxclinic-migrate.log 2>&1 &
MIGRATE_PID=$!

# Wait for Spring Boot to finish (up to 90s)
for i in $(seq 1 90); do
  if grep -q "Started\|Application run failed" /tmp/nexxclinic-migrate.log 2>/dev/null; then
    break
  fi
  sleep 1
done

if grep -q "Started" /tmp/nexxclinic-migrate.log 2>/dev/null; then
  echo "  ✅ Migrations applied successfully"
elif grep -q "Application run failed" /tmp/nexxclinic-migrate.log 2>/dev/null; then
  echo "  ⚠  Backend startup failed — check /tmp/nexxclinic-migrate.log"
  kill $MIGRATE_PID 2>/dev/null; wait $MIGRATE_PID 2>/dev/null; exit 1
else
  echo "  ⚠  Timeout waiting for backend — continuing anyway"
fi

# Stop the backend
kill $MIGRATE_PID 2>/dev/null || true
wait $MIGRATE_PID 2>/dev/null || true
sleep 2

echo "🌱  Seeding demo data..."

# ── Fixed UUIDs ───────────────────────────────────────────────────────────────
DEPT_DENTAL='a0000001-0000-0000-0000-000000000001'
DEPT_NURSING='a0000002-0000-0000-0000-000000000002'
DEPT_INTERNAL='a0000003-0000-0000-0000-000000000003'
INS_RSSB='b0000001-0000-0000-0000-000000000001'
INS_MMI='b0000002-0000-0000-0000-000000000002'
INS_OLDMUTUAL='b0000003-0000-0000-0000-000000000003'
INS_RADIANT='b0000004-0000-0000-0000-000000000004'

$PG <<'SQL'

-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  DEPARTMENTS                                                            ║
-- ╚══════════════════════════════════════════════════════════════════════════╝
INSERT INTO departments (id, name, nursing, support_requests, requests_products, insurance_policy_mode, created_at, updated_at) VALUES
('a0000001-0000-0000-0000-000000000001', 'Dental',           false, true, true, 'ALL', now(), now()),
('a0000002-0000-0000-0000-000000000002', 'Nursing',          true,  true, true, 'ALL', now(), now()),
('a0000003-0000-0000-0000-000000000003', 'Internal Medicine',false, true, true, 'ALL', now(), now());

-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  USERS / WORKERS  (password: Password123 → BCrypt hash)                 ║
-- ╚══════════════════════════════════════════════════════════════════════════╝
INSERT INTO workers (id, first_name, last_name, username, email, phone_number, password_hash, gender, account_status, active, auto_reset, must_change_on_next_login, max_active_sessions, required, created_at, updated_at, department_id) VALUES
('c0000001-0000-0000-0000-000000000001', 'Admin', 'User', 'admin', 'admin@nexxclinic.local', '+250788000001',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 'MALE', 'ACTIVE', true, false, false, 5, false, now(), now(), NULL),
('c0000002-0000-0000-0000-000000000002', 'Multi', 'Role', 'doctor', 'doctor@nexxclinic.local', '+250788000002',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
 'MALE', 'ACTIVE', true, false, false, 5, false, now(), now(), 'a0000003-0000-0000-0000-000000000003');

INSERT INTO worker_roles (worker_id, role_name) VALUES
('c0000001-0000-0000-0000-000000000001', 'ADMIN'),
('c0000002-0000-0000-0000-000000000002', 'CLINICIAN'),
('c0000002-0000-0000-0000-000000000002', 'FINANCE'),
('c0000002-0000-0000-0000-000000000002', 'RECEPTION');

INSERT INTO worker_departments (worker_id, department_id) VALUES
('c0000002-0000-0000-0000-000000000002', 'a0000001-0000-0000-0000-000000000001'),
('c0000002-0000-0000-0000-000000000002', 'a0000002-0000-0000-0000-000000000002'),
('c0000002-0000-0000-0000-000000000002', 'a0000003-0000-0000-0000-000000000003');

-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  INSURANCE PROVIDERS                                                     ║
-- ╚══════════════════════════════════════════════════════════════════════════╝
INSERT INTO insurance_providers (id, insurance_name, acronym, supported_by_clinic, created_at, updated_at) VALUES
('b0000001-0000-0000-0000-000000000001', 'Rwanda Social Security Board', 'RSSB',        true, now(), now()),
('b0000002-0000-0000-0000-000000000002', 'Madison Insurance Company',    'MMI',         true, now(), now()),
('b0000003-0000-0000-0000-000000000003', 'Old Mutual Rwanda',            'OLDMUTUAL',   true, now(), now()),
('b0000004-0000-0000-0000-000000000004', 'Radiant Health Insurance',     'Radiant',     true, now(), now());

-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  INSURANCE COVERAGE TIERS                                                ║
-- ║  RSSB  → single tier (base 20%)                                         ║
-- ║  MMI   → single tier (base 15%)                                         ║
-- ║  OLDMUTUAL → multi-tier: base 20%, Dental 10%, Nursing 15%, Internal 12% ║
-- ║  Radiant → multi-tier: base 25%, Dental 5%, Nursing 10%, Inpatient 8%    ║
-- ╚══════════════════════════════════════════════════════════════════════════╝
INSERT INTO insurance_coverage (id, insurance_provider_id, department_id, encounter_type, patient_share_percentage, created_at, updated_at) VALUES
-- RSSB: base only
('e0000001-0000-0000-0000-000000000001', 'b0000001-0000-0000-0000-000000000001', NULL, NULL, 20, now(), now()),
-- MMI: base only
('e0000003-0000-0000-0000-000000000003', 'b0000002-0000-0000-0000-000000000002', NULL, NULL, 15, now(), now()),
-- OLDMUTUAL: multi-tier
('e0000004-0000-0000-0000-000000000004', 'b0000003-0000-0000-0000-000000000003', NULL, NULL, 20, now(), now()),
('e0000005-0000-0000-0000-000000000005', 'b0000003-0000-0000-0000-000000000003', 'a0000001-0000-0000-0000-000000000001', NULL, 10, now(), now()),
('e0000006-0000-0000-0000-000000000006', 'b0000003-0000-0000-0000-000000000003', 'a0000002-0000-0000-0000-000000000002', NULL, 15, now(), now()),
('e0000007-0000-0000-0000-000000000007', 'b0000003-0000-0000-0000-000000000003', 'a0000003-0000-0000-0000-000000000003', NULL, 12, now(), now()),
-- Radiant: multi-tier
('e0000008-0000-0000-0000-000000000008', 'b0000004-0000-0000-0000-000000000004', NULL, NULL, 25, now(), now()),
('e0000009-0000-0000-0000-000000000009', 'b0000004-0000-0000-0000-000000000004', 'a0000001-0000-0000-0000-000000000001', NULL, 5,  now(), now()),
('e000000a-0000-0000-0000-00000000000a', 'b0000004-0000-0000-0000-000000000004', 'a0000002-0000-0000-0000-000000000002', NULL, 10, now(), now()),
('e000000b-0000-0000-0000-00000000000b', 'b0000004-0000-0000-0000-000000000004', NULL, 'INPATIENT_ADMISSION', 8, now(), now());

-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  PRODUCTS  (20: 8 drugs, 8 medical acts, 4 consumables)                  ║
-- ╚══════════════════════════════════════════════════════════════════════════╝
INSERT INTO products (id, type, code, unit, name, generic_name, description, clinic_price, private_rhic_price, created_at, updated_at, metadata) VALUES
('10000001-0000-0000-0000-000000000001', 'DRUG', 'DRG-001', 'TABLET',   'Amoxicillin 500mg',    'Amoxicillin',    'Antibiotic capsule',            2500.00,  3000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000002-0000-0000-0000-000000000002', 'DRUG', 'DRG-002', 'TABLET',   'Paracetamol 500mg',    'Paracetamol',    'Pain relief tablet',             800.00,  1000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000003-0000-0000-0000-000000000003', 'DRUG', 'DRG-003', 'CAPSULE',  'Omeprazole 20mg',      'Omeprazole',     'Proton pump inhibitor',         3500.00,  4000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000004-0000-0000-0000-000000000004', 'DRUG', 'DRG-004', 'TABLET',   'Metformin 500mg',      'Metformin',      'Antidiabetic tablet',           1200.00,  1500.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000005-0000-0000-0000-000000000005', 'DRUG', 'DRG-005', 'VIAL',     'Insulin Regular',      'Insulin',        'Injectable insulin',            15000.00, 18000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000006-0000-0000-0000-000000000006', 'DRUG', 'DRG-006', 'AMPOULE',  'Diclofenac 75mg',      'Diclofenac',     'NSAID injection',               1500.00,  2000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000007-0000-0000-0000-000000000007', 'DRUG', 'DRG-007', 'TABLET',   'Ciprofloxacin 500mg',  'Ciprofloxacin',  'Fluoroquinolone antibiotic',    2000.00,  2500.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000008-0000-0000-0000-000000000008', 'DRUG', 'DRG-008', 'TUBE',     'Hydrocortisone Cream', 'Hydrocortisone', 'Topical steroid cream',         1800.00,  2200.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000009-0000-0000-0000-000000000009', 'MEDICAL_ACT', 'ACT-001', 'DOSE', 'Dental Consultation',  NULL, 'General dental checkup',       10000.00, 12000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('1000000a-0000-0000-0000-00000000000a', 'MEDICAL_ACT', 'ACT-002', 'DOSE', 'Tooth Extraction',     NULL, 'Simple tooth extraction',      15000.00, 18000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('1000000b-0000-0000-0000-00000000000b', 'MEDICAL_ACT', 'ACT-003', 'DOSE', 'Dental X-Ray',         NULL, 'Periapical radiograph',        8000.00,  10000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('1000000c-0000-0000-0000-00000000000c', 'MEDICAL_ACT', 'ACT-004', 'DOSE', 'Blood Test - CBC',     NULL, 'Complete blood count',         5000.00,  6000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('1000000d-0000-0000-0000-00000000000d', 'MEDICAL_ACT', 'ACT-005', 'DOSE', 'Blood Test - Glucose', NULL, 'Fasting blood glucose',        3000.00,  4000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('1000000e-0000-0000-0000-00000000000e', 'MEDICAL_ACT', 'ACT-006', 'DOSE', 'ECG',                  NULL, 'Electrocardiogram',           12000.00, 15000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('1000000f-0000-0000-0000-00000000000f', 'MEDICAL_ACT', 'ACT-007', 'DOSE', 'General Consultation', NULL, 'Doctor consultation',          10000.00, 12000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000010-0000-0000-0000-000000000010', 'MEDICAL_ACT', 'ACT-008', 'DOSE', 'Wound Dressing',       NULL, 'Wound care and dressing',      3000.00,  4000.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000011-0000-0000-0000-000000000011', 'CONSUMABLE_DEVICE', 'CSM-001', 'PIECE', 'Syringe 5ml',    NULL, 'Disposable syringe',           500.00,   700.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000012-0000-0000-0000-000000000012', 'CONSUMABLE_DEVICE', 'CSM-002', 'PIECE', 'Gloves (pair)',  NULL, 'Disposable latex gloves',      300.00,   400.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000013-0000-0000-0000-000000000013', 'CONSUMABLE_DEVICE', 'CSM-003', 'PIECE', 'IV Cannula 20G', NULL, 'Intravenous cannula',          1200.00,  1500.00, now(), now(), lo_from_bytea(0, E'\\x7b7d')),
('10000014-0000-0000-0000-000000000014', 'CONSUMABLE_DEVICE', 'CSM-004', 'PIECE', 'Surgical Mask',  NULL, 'Disposable face mask',         200.00,   300.00, now(), now(), lo_from_bytea(0, E'\\x7b7d'));

-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  PRODUCT INSURANCE COVERAGE                                               ║
-- ╚══════════════════════════════════════════════════════════════════════════╝
-- RSSB covers drugs + some acts + consumables
INSERT INTO product_insurance_coverages (id, product_id, insurance_provider_id, cost, covered, not_paid, require_medical_advisor, drug_administration_frequency, must_prescribed_by, created_at, updated_at) VALUES
(gen_random_uuid(), '10000001-0000-0000-0000-000000000001', 'b0000001-0000-0000-0000-000000000001', 2000.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000002-0000-0000-0000-000000000002', 'b0000001-0000-0000-0000-000000000001', 600.00,   true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000003-0000-0000-0000-000000000003', 'b0000001-0000-0000-0000-000000000001', 2800.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000004-0000-0000-0000-000000000004', 'b0000001-0000-0000-0000-000000000001', 900.00,   true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000009-0000-0000-0000-000000000009', 'b0000001-0000-0000-0000-000000000001', 8000.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000c-0000-0000-0000-00000000000c', 'b0000001-0000-0000-0000-000000000001', 4000.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000d-0000-0000-0000-00000000000d', 'b0000001-0000-0000-0000-000000000001', 2500.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000f-0000-0000-0000-00000000000f', 'b0000001-0000-0000-0000-000000000001', 8000.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000011-0000-0000-0000-000000000011', 'b0000001-0000-0000-0000-000000000001', 400.00,   true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000012-0000-0000-0000-000000000012', 'b0000001-0000-0000-0000-000000000001', 250.00,   true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
-- Insulin NOT covered by RSSB
(gen_random_uuid(), '10000005-0000-0000-0000-000000000005', 'b0000001-0000-0000-0000-000000000001', 0.00,     false, true, false, 'CUSTOM_HOURS', 'ALL', now(), now());

-- MMI covers select products
INSERT INTO product_insurance_coverages (id, product_id, insurance_provider_id, cost, covered, not_paid, require_medical_advisor, drug_administration_frequency, must_prescribed_by, created_at, updated_at) VALUES
(gen_random_uuid(), '10000001-0000-0000-0000-000000000001', 'b0000002-0000-0000-0000-000000000002', 1800.00, true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000002-0000-0000-0000-000000000002', 'b0000002-0000-0000-0000-000000000002', 500.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000c-0000-0000-0000-00000000000c', 'b0000002-0000-0000-0000-000000000002', 3500.00, true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000f-0000-0000-0000-00000000000f', 'b0000002-0000-0000-0000-000000000002', 7000.00, true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now());

-- OLDMUTUAL covers most products
INSERT INTO product_insurance_coverages (id, product_id, insurance_provider_id, cost, covered, not_paid, require_medical_advisor, drug_administration_frequency, must_prescribed_by, created_at, updated_at) VALUES
(gen_random_uuid(), '10000001-0000-0000-0000-000000000001', 'b0000003-0000-0000-0000-000000000003', 1500.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000002-0000-0000-0000-000000000002', 'b0000003-0000-0000-0000-000000000003', 450.00,   true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000003-0000-0000-0000-000000000003', 'b0000003-0000-0000-0000-000000000003', 2500.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000009-0000-0000-0000-000000000009', 'b0000003-0000-0000-0000-000000000003', 7500.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000a-0000-0000-0000-00000000000a', 'b0000003-0000-0000-0000-000000000003', 12000.00, true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000c-0000-0000-0000-00000000000c', 'b0000003-0000-0000-0000-000000000003', 3800.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000d-0000-0000-0000-00000000000d', 'b0000003-0000-0000-0000-000000000003', 2200.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000f-0000-0000-0000-00000000000f', 'b0000003-0000-0000-0000-000000000003', 7000.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000011-0000-0000-0000-000000000011', 'b0000003-0000-0000-0000-000000000003', 350.00,   true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000013-0000-0000-0000-000000000013', 'b0000003-0000-0000-0000-000000000003', 900.00,   true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now());

-- Radiant covers most products
INSERT INTO product_insurance_coverages (id, product_id, insurance_provider_id, cost, covered, not_paid, require_medical_advisor, drug_administration_frequency, must_prescribed_by, created_at, updated_at) VALUES
(gen_random_uuid(), '10000001-0000-0000-0000-000000000001', 'b0000004-0000-0000-0000-000000000004', 1600.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000002-0000-0000-0000-000000000002', 'b0000004-0000-0000-0000-000000000004', 400.00,   true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000005-0000-0000-0000-000000000005', 'b0000004-0000-0000-0000-000000000004', 12000.00, true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000009-0000-0000-0000-000000000009', 'b0000004-0000-0000-0000-000000000004', 8000.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000a-0000-0000-0000-00000000000a', 'b0000004-0000-0000-0000-000000000004', 13000.00, true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000c-0000-0000-0000-00000000000c', 'b0000004-0000-0000-0000-000000000004', 3500.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000e-0000-0000-0000-00000000000e', 'b0000004-0000-0000-0000-000000000004', 9000.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '1000000f-0000-0000-0000-00000000000f', 'b0000004-0000-0000-0000-000000000004', 8000.00,  true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now()),
(gen_random_uuid(), '10000013-0000-0000-0000-000000000013', 'b0000004-0000-0000-0000-000000000004', 800.00,   true, false, false, 'CUSTOM_HOURS', 'ALL', now(), now());

-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  PATIENTS                                                               ║
-- ╚══════════════════════════════════════════════════════════════════════════╝
INSERT INTO patients (id, first_name, last_name, full_name, gender, date_of_birth, patient_identifier, primary_phone_number, village, cell, city, district, created_at, updated_at) VALUES
('d0000001-0000-0000-0000-000000000001', 'Jean',   'Hakizimana', 'Jean Hakizimana',  'MALE',   '1985-03-15', 'PAT-00001', '+250788111001', 'Kimironko',  'Gasabo',      'Kigali', 'Gasabo', now(), now()),
('d0000002-0000-0000-0000-000000000002', 'Marie',  'Uwimana',    'Marie Uwimana',    'FEMALE', '1992-07-22', 'PAT-00002', '+250788111002', 'Nyamirambo',  'Nyarugenge',  'Kigali', 'Nyarugenge', now(), now()),
('d0000003-0000-0000-0000-000000000003', 'Patrick','Mugenzi',    'Patrick Mugenzi',  'MALE',   '1978-11-08', 'PAT-00003', '+250788111003', 'Rubavu',      'Rubavu',      'Rubavu', 'Rubavu', now(), now());

-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  PATIENT INSURANCES                                                      ║
-- ╚══════════════════════════════════════════════════════════════════════════╝
INSERT INTO patient_insurances (id, patient_id, insurance_provider_id, insurance_card_number, principal_member, principal_member_name, providing_company_or_employer, valid_from, valid_until, deactivated, created_at, updated_at) VALUES
(gen_random_uuid(), 'd0000001-0000-0000-0000-000000000001', 'b0000001-0000-0000-0000-000000000001', 'RSSB-001-JEAN',  true,  'Jean Hakizimana',  'Government of Rwanda', '2025-01-01', '2026-12-31', false, now(), now()),
(gen_random_uuid(), 'd0000001-0000-0000-0000-000000000001', 'b0000003-0000-0000-0000-000000000003', 'OM-JEAN-002',    false, 'Jean Hakizimana',  'Self-employed',       '2025-06-01', '2026-06-01', false, now(), now()),
(gen_random_uuid(), 'd0000002-0000-0000-0000-000000000002', 'b0000004-0000-0000-0000-000000000004', 'RAD-MARIE-001',  true,  'Marie Uwimana',   'Radiant Health Co',   '2025-03-01', '2026-03-01', false, now(), now()),
(gen_random_uuid(), 'd0000003-0000-0000-0000-000000000003', 'b0000002-0000-0000-0000-000000000002', 'MMI-PAT-001',    true,  'Patrick Mugenzi', 'Rwanda Mines Board',  '2025-01-01', '2026-12-31', false, now(), now()),
(gen_random_uuid(), 'd0000003-0000-0000-0000-000000000003', 'b0000003-0000-0000-0000-000000000003', 'OM-PAT-002',     false, 'Patrick Mugenzi', 'Self-employed',       '2025-04-01', '2026-04-01', false, now(), now());

-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  DEPARTMENT INSURANCE POLICIES                                           ║
-- ╚══════════════════════════════════════════════════════════════════════════╝
INSERT INTO department_insurance_policies (id, department_id, insurance_provider_id, created_at, updated_at) VALUES
(gen_random_uuid(), 'a0000001-0000-0000-0000-000000000001', 'b0000001-0000-0000-0000-000000000001', now(), now()),
(gen_random_uuid(), 'a0000001-0000-0000-0000-000000000001', 'b0000002-0000-0000-0000-000000000002', now(), now()),
(gen_random_uuid(), 'a0000001-0000-0000-0000-000000000001', 'b0000003-0000-0000-0000-000000000003', now(), now()),
(gen_random_uuid(), 'a0000001-0000-0000-0000-000000000001', 'b0000004-0000-0000-0000-000000000004', now(), now()),
(gen_random_uuid(), 'a0000002-0000-0000-0000-000000000002', 'b0000001-0000-0000-0000-000000000001', now(), now()),
(gen_random_uuid(), 'a0000002-0000-0000-0000-000000000002', 'b0000002-0000-0000-0000-000000000002', now(), now()),
(gen_random_uuid(), 'a0000002-0000-0000-0000-000000000002', 'b0000003-0000-0000-0000-000000000003', now(), now()),
(gen_random_uuid(), 'a0000002-0000-0000-0000-000000000002', 'b0000004-0000-0000-0000-000000000004', now(), now()),
(gen_random_uuid(), 'a0000003-0000-0000-0000-000000000003', 'b0000001-0000-0000-0000-000000000001', now(), now()),
(gen_random_uuid(), 'a0000003-0000-0000-0000-000000000003', 'b0000002-0000-0000-0000-000000000002', now(), now()),
(gen_random_uuid(), 'a0000003-0000-0000-0000-000000000003', 'b0000003-0000-0000-0000-000000000003', now(), now()),
(gen_random_uuid(), 'a0000003-0000-0000-0000-000000000003', 'b0000004-0000-0000-0000-000000000004', now(), now());

SQL

echo ""
echo "✅  Seed complete!"
echo ""
echo "┌─────────────────────────────────────────────────────────┐"
echo "│  DEMO ACCOUNTS (password: Password123)                 │"
echo "├─────────────┬───────────────────────────────────────────┤"
echo "│  admin      │  ADMIN role (full access)                │"
echo "│  doctor     │  CLINICIAN + FINANCE + RECEPTION roles   │"
echo "├─────────────┼───────────────────────────────────────────┤"
echo "│  Patients:  │  Jean Hakizimana (RSSB + OLDMUTUAL)     │"
echo "│             │  Marie Uwimana (Radiant)                 │"
echo "│             │  Patrick Mugenzi (MMI + OLDMUTUAL)       │"
echo "├─────────────┼───────────────────────────────────────────┤"
echo "│  Insurance: │  RSSB (single tier: 20%)                │"
echo "│             │  MMI (single tier: 15%)                 │"
echo "│             │  OLDMUTUAL (multi: 20/10/15/12%)        │"
echo "│             │  Radiant (multi: 25/5/10/8%)            │"
echo "├─────────────┼───────────────────────────────────────────┤"
echo "│  Products:  │  20 (8 drugs, 8 acts, 4 consumables)    │"
echo "│  Depts:     │  Dental, Nursing, Internal Medicine      │"
echo "└─────────────┴───────────────────────────────────────────┘"

-- ============================================================================
-- seed-visits.sql — Add sample visits with products for billing demo
-- Run AFTER seed-local.sh: docker exec -i nexxclinic-postgres psql -U nexxclinic_user -d nexxclinic < seed-visits.sql
-- ============================================================================

-- Clean up any existing sample visits (safe to re-run)
DELETE FROM visit_department_products WHERE visit_department_id IN (SELECT id FROM visit_departments WHERE visit_id IN (
  'a1000001-0000-0000-0000-000000000001','a2000001-0000-0000-0000-000000000002','a3000001-0000-0000-0000-000000000003',
  'a4000001-0000-0000-0000-000000000004','a5000001-0000-0000-0000-000000000005','a6000001-0000-0000-0000-000000000006',
  'a7000001-0000-0000-0000-000000000007'));
DELETE FROM visit_insurances WHERE visit_id IN (
  'a1000001-0000-0000-0000-000000000001','a2000001-0000-0000-0000-000000000002','a3000001-0000-0000-0000-000000000003',
  'a4000001-0000-0000-0000-000000000004','a5000001-0000-0000-0000-000000000005','a6000001-0000-0000-0000-000000000006',
  'a7000001-0000-0000-0000-000000000007');
DELETE FROM visit_departments WHERE visit_id IN (
  'a1000001-0000-0000-0000-000000000001','a2000001-0000-0000-0000-000000000002','a3000001-0000-0000-0000-000000000003',
  'a4000001-0000-0000-0000-000000000004','a5000001-0000-0000-0000-000000000005','a6000001-0000-0000-0000-000000000006',
  'a7000001-0000-0000-0000-000000000007');
DELETE FROM visits WHERE id IN (
  'a1000001-0000-0000-0000-000000000001','a2000001-0000-0000-0000-000000000002','a3000001-0000-0000-0000-000000000003',
  'a4000001-0000-0000-0000-000000000004','a5000001-0000-0000-0000-000000000005','a6000001-0000-0000-0000-000000000006',
  'a7000001-0000-0000-0000-000000000007');

-- ── Visit 1: Jean (PAT-00001) — Dental, RSSB 20% ────────────────────────────
INSERT INTO visits (id, patient_id, status, visit_date, created_at, updated_at) VALUES
('a1000001-0000-0000-0000-000000000001', 'd0000001-0000-0000-0000-000000000001', 'IN_PROGRESS', '2026-08-20 09:00:00', now(), now());
INSERT INTO visit_departments (id, visit_id, department_id, status, encounter_type, created_at, updated_at) VALUES
('ad000001-0000-0000-0000-000000000001', 'a1000001-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000001', 'BILLING', 'OUTPATIENT', now(), now());
INSERT INTO visit_insurances (id, visit_id, patient_insurance_id, created_at, updated_at) VALUES
('ae000001-0000-0000-0000-000000000001', 'a1000001-0000-0000-0000-000000000001',
 (SELECT id FROM patient_insurances WHERE insurance_card_number = 'RSSB-001-JEAN'), now(), now());
INSERT INTO visit_department_products (id, visit_department_id, product_id, quantity, source, status, deleted, added_by_worker_id, created_at, updated_at) VALUES
('ab000001-0000-0000-0000-000000000001', 'ad000001-0000-0000-0000-000000000001', '10000009-0000-0000-0000-000000000009', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000002-0000-0000-0000-000000000002', 'ad000001-0000-0000-0000-000000000001', '1000000a-0000-0000-0000-00000000000a', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000003-0000-0000-0000-000000000003', 'ad000001-0000-0000-0000-000000000001', '10000001-0000-0000-0000-000000000001', 10, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now());

-- ── Visit 2: Marie (PAT-00002) — Internal Medicine, Radiant 25% ─────────────
INSERT INTO visits (id, patient_id, status, visit_date, created_at, updated_at) VALUES
('a2000001-0000-0000-0000-000000000002', 'd0000002-0000-0000-0000-000000000002', 'IN_PROGRESS', '2026-08-21 10:30:00', now(), now());
INSERT INTO visit_departments (id, visit_id, department_id, status, encounter_type, created_at, updated_at) VALUES
('ad000002-0000-0000-0000-000000000002', 'a2000001-0000-0000-0000-000000000002', 'a0000003-0000-0000-0000-000000000003', 'BILLING', 'OUTPATIENT', now(), now());
INSERT INTO visit_insurances (id, visit_id, patient_insurance_id, created_at, updated_at) VALUES
('ae000002-0000-0000-0000-000000000002', 'a2000001-0000-0000-0000-000000000002',
 (SELECT id FROM patient_insurances WHERE insurance_card_number = 'RAD-MARIE-001'), now(), now());
INSERT INTO visit_department_products (id, visit_department_id, product_id, quantity, source, status, deleted, added_by_worker_id, created_at, updated_at) VALUES
('ab000004-0000-0000-0000-000000000004', 'ad000002-0000-0000-0000-000000000002', '1000000f-0000-0000-0000-00000000000f', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000005-0000-0000-0000-000000000005', 'ad000002-0000-0000-0000-000000000002', '1000000c-0000-0000-0000-00000000000c', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000006-0000-0000-0000-000000000006', 'ad000002-0000-0000-0000-000000000002', '1000000d-0000-0000-0000-00000000000d', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000007-0000-0000-0000-000000000007', 'ad000002-0000-0000-0000-000000000002', '10000002-0000-0000-0000-000000000002', 20, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now());

-- ── Visit 3: Claudine (PAT-00004) — Dental, Radiant 5% (dept-specific) ──────
INSERT INTO visits (id, patient_id, status, visit_date, created_at, updated_at) VALUES
('a3000001-0000-0000-0000-000000000003', 'd0000004-0000-0000-0000-000000000004', 'IN_PROGRESS', '2026-08-22 14:00:00', now(), now());
INSERT INTO visit_departments (id, visit_id, department_id, status, encounter_type, created_at, updated_at) VALUES
('ad000003-0000-0000-0000-000000000003', 'a3000001-0000-0000-0000-000000000003', 'a0000001-0000-0000-0000-000000000001', 'BILLING', 'OUTPATIENT', now(), now());
INSERT INTO visit_insurances (id, visit_id, patient_insurance_id, created_at, updated_at) VALUES
('ae000003-0000-0000-0000-000000000003', 'a3000001-0000-0000-0000-000000000003',
 (SELECT id FROM patient_insurances WHERE insurance_card_number = 'RAD-CLAUDINE-001'), now(), now());
INSERT INTO visit_department_products (id, visit_department_id, product_id, quantity, source, status, deleted, added_by_worker_id, created_at, updated_at) VALUES
('ab000008-0000-0000-0000-000000000008', 'ad000003-0000-0000-0000-000000000003', '10000009-0000-0000-0000-000000000009', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000009-0000-0000-0000-000000000009', 'ad000003-0000-0000-0000-000000000003', '1000000b-0000-0000-0000-00000000000b', 2, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab00000a-0000-0000-0000-00000000000a', 'ad000003-0000-0000-0000-000000000003', '10000011-0000-0000-0000-000000000011', 3, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab00000b-0000-0000-0000-00000000000b', 'ad000003-0000-0000-0000-000000000003', '10000012-0000-0000-0000-000000000012', 5, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now());

-- ── Visit 4: Emmanuel (PAT-00005) — Nursing, RSSB + Radiant combo ────────────
INSERT INTO visits (id, patient_id, status, visit_date, created_at, updated_at) VALUES
('a4000001-0000-0000-0000-000000000004', 'd0000005-0000-0000-0000-000000000005', 'IN_PROGRESS', '2026-08-23 08:30:00', now(), now());
INSERT INTO visit_departments (id, visit_id, department_id, status, encounter_type, created_at, updated_at) VALUES
('ad000004-0000-0000-0000-000000000004', 'a4000001-0000-0000-0000-000000000004', 'a0000002-0000-0000-0000-000000000002', 'BILLING', 'OUTPATIENT', now(), now());
INSERT INTO visit_insurances (id, visit_id, patient_insurance_id, created_at, updated_at) VALUES
('ae000004-0000-0000-0000-000000000004', 'a4000001-0000-0000-0000-000000000004',
 (SELECT id FROM patient_insurances WHERE insurance_card_number = 'RSSB-EMMANUEL-001'), now(), now()),
('ae000005-0000-0000-0000-000000000005', 'a4000001-0000-0000-0000-000000000004',
 (SELECT id FROM patient_insurances WHERE insurance_card_number = 'RAD-EMMANUEL-002'), now(), now());
INSERT INTO visit_department_products (id, visit_department_id, product_id, quantity, source, status, deleted, added_by_worker_id, created_at, updated_at) VALUES
('ab00000c-0000-0000-0000-00000000000c', 'ad000004-0000-0000-0000-000000000004', '10000010-0000-0000-0000-000000000010', 2, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab00000d-0000-0000-0000-00000000000d', 'ad000004-0000-0000-0000-000000000004', '10000013-0000-0000-0000-000000000013', 2, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab00000e-0000-0000-0000-00000000000e', 'ad000004-0000-0000-0000-000000000004', '10000011-0000-0000-0000-000000000011', 3, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab00000f-0000-0000-0000-00000000000f', 'ad000004-0000-0000-0000-000000000004', '10000002-0000-0000-0000-000000000002', 30, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000010-0000-0000-0000-000000000010', 'ad000004-0000-0000-0000-000000000004', '10000007-0000-0000-0000-000000000007', 14, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now());

-- ── Visit 5: Thomas (PAT-00007) — Internal Medicine, OLDMUTUAL 12% ──────────
INSERT INTO visits (id, patient_id, status, visit_date, created_at, updated_at) VALUES
('a5000001-0000-0000-0000-000000000005', 'd0000007-0000-0000-0000-000000000007', 'IN_PROGRESS', '2026-08-23 11:00:00', now(), now());
INSERT INTO visit_departments (id, visit_id, department_id, status, encounter_type, created_at, updated_at) VALUES
('ad000005-0000-0000-0000-000000000005', 'a5000001-0000-0000-0000-000000000005', 'a0000003-0000-0000-0000-000000000003', 'BILLING', 'OUTPATIENT', now(), now());
INSERT INTO visit_insurances (id, visit_id, patient_insurance_id, created_at, updated_at) VALUES
('ae000006-0000-0000-0000-000000000006', 'a5000001-0000-0000-0000-000000000005',
 (SELECT id FROM patient_insurances WHERE insurance_card_number = 'OM-THOMAS-001'), now(), now());
INSERT INTO visit_department_products (id, visit_department_id, product_id, quantity, source, status, deleted, added_by_worker_id, created_at, updated_at) VALUES
('ab000011-0000-0000-0000-000000000011', 'ad000005-0000-0000-0000-000000000005', '1000000f-0000-0000-0000-00000000000f', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000012-0000-0000-0000-000000000012', 'ad000005-0000-0000-0000-000000000005', '1000000e-0000-0000-0000-00000000000e', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000013-0000-0000-0000-000000000013', 'ad000005-0000-0000-0000-000000000005', '10000003-0000-0000-0000-000000000003', 30, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000014-0000-0000-0000-000000000014', 'ad000005-0000-0000-0000-000000000005', '10000004-0000-0000-0000-000000000004', 60, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000015-0000-0000-0000-000000000015', 'ad000005-0000-0000-0000-000000000005', '10000005-0000-0000-0000-000000000005', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now());

-- ── Visit 6: Alice (PAT-00006) — Dental, self-pay (no insurance) ────────────
INSERT INTO visits (id, patient_id, status, visit_date, created_at, updated_at) VALUES
('a6000001-0000-0000-0000-000000000006', 'd0000006-0000-0000-0000-000000000006', 'IN_PROGRESS', '2026-08-24 09:00:00', now(), now());
INSERT INTO visit_departments (id, visit_id, department_id, status, encounter_type, created_at, updated_at) VALUES
('ad000006-0000-0000-0000-000000000006', 'a6000001-0000-0000-0000-000000000006', 'a0000001-0000-0000-0000-000000000001', 'BILLING', 'OUTPATIENT', now(), now());
INSERT INTO visit_department_products (id, visit_department_id, product_id, quantity, source, status, deleted, added_by_worker_id, created_at, updated_at) VALUES
('ab000016-0000-0000-0000-000000000016', 'ad000006-0000-0000-0000-000000000006', '10000009-0000-0000-0000-000000000009', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000017-0000-0000-0000-000000000017', 'ad000006-0000-0000-0000-000000000006', '1000000a-0000-0000-0000-00000000000a', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab000018-0000-0000-0000-000000000018', 'ad000006-0000-0000-0000-000000000006', '10000008-0000-0000-0000-000000000008', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now());

-- ── Visit 7: Patrick (PAT-00003) — Nursing, INPATIENT_ADMISSION, MMI 15% ────
INSERT INTO visits (id, patient_id, status, visit_date, created_at, updated_at) VALUES
('a7000001-0000-0000-0000-000000000007', 'd0000003-0000-0000-0000-000000000003', 'IN_PROGRESS', '2026-08-19 15:00:00', now(), now());
INSERT INTO visit_departments (id, visit_id, department_id, status, encounter_type, created_at, updated_at) VALUES
('ad000007-0000-0000-0000-000000000007', 'a7000001-0000-0000-0000-000000000007', 'a0000002-0000-0000-0000-000000000002', 'BILLING', 'INPATIENT_ADMISSION', now(), now());
INSERT INTO visit_insurances (id, visit_id, patient_insurance_id, created_at, updated_at) VALUES
('ae000007-0000-0000-0000-000000000007', 'a7000001-0000-0000-0000-000000000007',
 (SELECT id FROM patient_insurances WHERE insurance_card_number = 'MMI-PAT-001'), now(), now());
INSERT INTO visit_department_products (id, visit_department_id, product_id, quantity, source, status, deleted, added_by_worker_id, created_at, updated_at) VALUES
('ab000019-0000-0000-0000-000000000019', 'ad000007-0000-0000-0000-000000000007', '1000000c-0000-0000-0000-00000000000c', 1, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab00001a-0000-0000-0000-00000000001a', 'ad000007-0000-0000-0000-000000000007', '10000010-0000-0000-0000-000000000010', 3, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now()),
('ab00001b-0000-0000-0000-00000000001b', 'ad000007-0000-0000-0000-000000000007', '10000014-0000-0000-0000-000000000014', 10, 'USER', 'UNPAID', false, 'c0000002-0000-0000-0000-000000000002', now(), now());

-- ╔══════════════════════════════════════════════════════════════════════════╗
-- ║  SUMMARY                                                                ║
-- ╚══════════════════════════════════════════════════════════════════════════╝
SELECT p.patient_identifier, p.first_name || ' ' || p.last_name AS patient,
       d.name AS department, vd.encounter_type,
       (SELECT count(*) FROM visit_department_products vdp WHERE vdp.visit_department_id=vd.id AND vdp.deleted=false) AS products,
       (SELECT string_agg(pi.insurance_card_number, ', ') FROM visit_insurances vi JOIN patient_insurances pi ON vi.patient_insurance_id=pi.id WHERE vi.visit_id=v.id) AS insurances
FROM visits v JOIN patients p ON v.patient_id=p.id JOIN visit_departments vd ON vd.visit_id=v.id JOIN departments d ON vd.department_id=d.id ORDER BY v.visit_date;

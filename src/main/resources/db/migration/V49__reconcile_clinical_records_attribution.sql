-- ============================================================================
-- V49: Reconcile clinical records added_by_id to genuine clinical processors
--      Corrects records inadvertently attributed to cashier / billing users
-- ============================================================================

-- 1. Re-attribute diagnoses where added_by_id belongs to a worker who lacks CLINICIAN role
--    or where the department had a dedicated clinical processor.
UPDATE visit_department_diagnostics d
SET added_by_id = COALESCE(
    (
        SELECT vdp.worker_id
        FROM visit_department_processors vdp
        JOIN workers w ON vdp.worker_id = w.id
        JOIN worker_roles r ON w.id = r.worker_id
        WHERE vdp.visit_department_id = d.visit_department_id
          AND r.role_name = 'CLINICIAN'
          AND vdp.worker_id NOT IN (
              SELECT wr.worker_id FROM worker_roles wr WHERE wr.role_name = 'FINANCE'
          )
        LIMIT 1
    ),
    (
        SELECT vdp.worker_id
        FROM visit_department_processors vdp
        JOIN workers w ON vdp.worker_id = w.id
        JOIN worker_roles r ON w.id = r.worker_id
        WHERE vdp.visit_department_id = d.visit_department_id
          AND r.role_name = 'CLINICIAN'
        LIMIT 1
    ),
    d.added_by_id
)
WHERE d.added_by_id IS NULL
   OR d.added_by_id IN (
       SELECT w.id
       FROM workers w
       JOIN worker_roles r ON w.id = r.worker_id
       WHERE r.role_name = 'FINANCE'
   );

-- 2. Re-attribute symptoms where added_by_id belongs to a worker who lacks CLINICIAN role
--    or was attributed to a finance/cashier user.
UPDATE visit_department_symptoms s
SET added_by_id = COALESCE(
    (
        SELECT vdp.worker_id
        FROM visit_department_processors vdp
        JOIN workers w ON vdp.worker_id = w.id
        JOIN worker_roles r ON w.id = r.worker_id
        WHERE vdp.visit_department_id = s.visit_department_id
          AND r.role_name = 'CLINICIAN'
          AND vdp.worker_id NOT IN (
              SELECT wr.worker_id FROM worker_roles wr WHERE wr.role_name = 'FINANCE'
          )
        LIMIT 1
    ),
    (
        SELECT vdp.worker_id
        FROM visit_department_processors vdp
        JOIN workers w ON vdp.worker_id = w.id
        JOIN worker_roles r ON w.id = r.worker_id
        WHERE vdp.visit_department_id = s.visit_department_id
          AND r.role_name = 'CLINICIAN'
        LIMIT 1
    ),
    s.added_by_id
)
WHERE s.added_by_id IS NULL
   OR s.added_by_id IN (
       SELECT w.id
       FROM workers w
       JOIN worker_roles r ON w.id = r.worker_id
       WHERE r.role_name = 'FINANCE'
   );

-- 3. Re-attribute medications where added_by_id belongs to a worker who lacks CLINICIAN role
--    or was attributed to a finance/cashier user.
UPDATE visit_department_medications m
SET added_by_id = COALESCE(
    (
        SELECT vdp.worker_id
        FROM visit_department_processors vdp
        JOIN workers w ON vdp.worker_id = w.id
        JOIN worker_roles r ON w.id = r.worker_id
        WHERE vdp.visit_department_id = m.visit_department_id
          AND r.role_name = 'CLINICIAN'
          AND vdp.worker_id NOT IN (
              SELECT wr.worker_id FROM worker_roles wr WHERE wr.role_name = 'FINANCE'
          )
        LIMIT 1
    ),
    (
        SELECT vdp.worker_id
        FROM visit_department_processors vdp
        JOIN workers w ON vdp.worker_id = w.id
        JOIN worker_roles r ON w.id = r.worker_id
        WHERE vdp.visit_department_id = m.visit_department_id
          AND r.role_name = 'CLINICIAN'
        LIMIT 1
    ),
    m.added_by_id
)
WHERE m.added_by_id IS NULL
   OR m.added_by_id IN (
       SELECT w.id
       FROM workers w
       JOIN worker_roles r ON w.id = r.worker_id
       WHERE r.role_name = 'FINANCE'
   );

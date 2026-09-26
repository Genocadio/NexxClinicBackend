-- ============================================================================
-- V33: Add Granular Visit Viewing & Patient History Permissions
-- ============================================================================

-- 1. Insert new permissions into permissions table
INSERT INTO permissions (name, description, module) VALUES
    ('VISIT_VIEW_ALL', 'View all visits across the clinic without department scoping', 'VISIT'),
    ('VISIT_VIEW_ASSIGNED', 'View visits assigned to user or routed to their departments', 'VISIT'),
    ('VISIT_STATUS_VIEW', 'View visit and department lifecycle/routing statuses without clinical diagnostics', 'VISIT'),
    ('PATIENT_HISTORY_VIEW', 'View comprehensive all-time historical encounter timeline for a patient', 'PATIENT')
ON CONFLICT (name) DO NOTHING;

-- 2. Helper macro/block to assign new permissions to roles
DO $$
DECLARE
    v_perm_view_all UUID;
    v_perm_view_assigned UUID;
    v_perm_status_view UUID;
    v_perm_patient_history UUID;
    v_role_id UUID;
BEGIN
    SELECT id INTO v_perm_view_all FROM permissions WHERE name = 'VISIT_VIEW_ALL';
    SELECT id INTO v_perm_view_assigned FROM permissions WHERE name = 'VISIT_VIEW_ASSIGNED';
    SELECT id INTO v_perm_status_view FROM permissions WHERE name = 'VISIT_STATUS_VIEW';
    SELECT id INTO v_perm_patient_history FROM permissions WHERE name = 'PATIENT_HISTORY_VIEW';

    -- ADMIN: gets all
    SELECT id INTO v_role_id FROM roles WHERE name = 'ADMIN';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_view_all),
            (v_role_id, v_perm_view_assigned),
            (v_role_id, v_perm_status_view),
            (v_role_id, v_perm_patient_history)
        ON CONFLICT DO NOTHING;
    END IF;

    -- CLINIC_ADMIN: gets all
    SELECT id INTO v_role_id FROM roles WHERE name = 'CLINIC_ADMIN';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_view_all),
            (v_role_id, v_perm_view_assigned),
            (v_role_id, v_perm_status_view),
            (v_role_id, v_perm_patient_history)
        ON CONFLICT DO NOTHING;
    END IF;

    -- MANAGER: gets all
    SELECT id INTO v_role_id FROM roles WHERE name = 'MANAGER';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_view_all),
            (v_role_id, v_perm_view_assigned),
            (v_role_id, v_perm_status_view),
            (v_role_id, v_perm_patient_history)
        ON CONFLICT DO NOTHING;
    END IF;

    -- RECEPTION: gets VISIT_VIEW_ALL, VISIT_STATUS_VIEW
    SELECT id INTO v_role_id FROM roles WHERE name = 'RECEPTION';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_view_all),
            (v_role_id, v_perm_status_view)
        ON CONFLICT DO NOTHING;
    END IF;

    -- CLINICIAN: gets VISIT_VIEW_ASSIGNED, VISIT_VIEW_ALL, VISIT_STATUS_VIEW, PATIENT_HISTORY_VIEW
    SELECT id INTO v_role_id FROM roles WHERE name = 'CLINICIAN';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_view_all),
            (v_role_id, v_perm_view_assigned),
            (v_role_id, v_perm_status_view),
            (v_role_id, v_perm_patient_history)
        ON CONFLICT DO NOTHING;
    END IF;

    -- NURSE: gets VISIT_VIEW_ASSIGNED, VISIT_STATUS_VIEW
    SELECT id INTO v_role_id FROM roles WHERE name = 'NURSE';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_view_assigned),
            (v_role_id, v_perm_status_view)
        ON CONFLICT DO NOTHING;
    END IF;

    -- FINANCE: gets VISIT_STATUS_VIEW
    SELECT id INTO v_role_id FROM roles WHERE name = 'FINANCE';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_status_view)
        ON CONFLICT DO NOTHING;
    END IF;

    -- STAFF: gets VISIT_STATUS_VIEW
    SELECT id INTO v_role_id FROM roles WHERE name = 'STAFF';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_status_view)
        ON CONFLICT DO NOTHING;
    END IF;

END $$;

-- ============================================================================
-- V50: Add Report and Analytics Permissions
-- ============================================================================

-- 1. Insert report permissions into permissions table
INSERT INTO permissions (name, description, module) VALUES
    ('REPORTS_VIEW', 'View clinic operational, user performance, and activity reports', 'REPORTS'),
    ('FINANCE_REPORTS_VIEW', 'View clinic financial reports, earnings, waivers, adjustments, and loans', 'REPORTS'),
    ('MEDICAL_REPORTS_VIEW', 'View clinical epidemiology, diagnostic, and symptom reports', 'REPORTS')
ON CONFLICT (name) DO NOTHING;

-- 2. Map permissions to default system roles
DO $$
DECLARE
    v_perm_reports UUID;
    v_perm_finance_reports UUID;
    v_perm_medical_reports UUID;
    v_role_id UUID;
BEGIN
    SELECT id INTO v_perm_reports FROM permissions WHERE name = 'REPORTS_VIEW';
    SELECT id INTO v_perm_finance_reports FROM permissions WHERE name = 'FINANCE_REPORTS_VIEW';
    SELECT id INTO v_perm_medical_reports FROM permissions WHERE name = 'MEDICAL_REPORTS_VIEW';

    -- ADMIN: gets all 3
    SELECT id INTO v_role_id FROM roles WHERE name = 'ADMIN';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_reports),
            (v_role_id, v_perm_finance_reports),
            (v_role_id, v_perm_medical_reports)
        ON CONFLICT DO NOTHING;
    END IF;

    -- CLINIC_ADMIN: gets all 3
    SELECT id INTO v_role_id FROM roles WHERE name = 'CLINIC_ADMIN';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_reports),
            (v_role_id, v_perm_finance_reports),
            (v_role_id, v_perm_medical_reports)
        ON CONFLICT DO NOTHING;
    END IF;

    -- MANAGER: gets all 3
    SELECT id INTO v_role_id FROM roles WHERE name = 'MANAGER';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_reports),
            (v_role_id, v_perm_finance_reports),
            (v_role_id, v_perm_medical_reports)
        ON CONFLICT DO NOTHING;
    END IF;

    -- FINANCE: gets REPORTS_VIEW and FINANCE_REPORTS_VIEW
    SELECT id INTO v_role_id FROM roles WHERE name = 'FINANCE';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_reports),
            (v_role_id, v_perm_finance_reports)
        ON CONFLICT DO NOTHING;
    END IF;

    -- CLINICIAN: gets REPORTS_VIEW and MEDICAL_REPORTS_VIEW
    SELECT id INTO v_role_id FROM roles WHERE name = 'CLINICIAN';
    IF v_role_id IS NOT NULL THEN
        INSERT INTO role_permissions (role_id, permission_id) VALUES
            (v_role_id, v_perm_reports),
            (v_role_id, v_perm_medical_reports)
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

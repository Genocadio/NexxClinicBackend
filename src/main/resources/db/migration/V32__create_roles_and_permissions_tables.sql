-- ============================================================================
-- V32: Create Roles & Permissions Tables and Seed Initial Data
--
-- Introduces a dynamic database-backed Role-Based Access Control (RBAC)
-- architecture with granular permissions, allowing creation of custom roles,
-- permission assignment/revocation, and dynamic authorization checks.
-- ============================================================================

-- 1. Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    module VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 2. Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. Create role_permissions join table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);

-- 4. Seed Standard Permissions
INSERT INTO permissions (name, description, module) VALUES
    -- User & Worker Management
    ('USER_VIEW', 'View system users and workers', 'USER'),
    ('USER_CREATE', 'Create new staff and users', 'USER'),
    ('USER_UPDATE', 'Update existing user profiles and roles', 'USER'),
    ('USER_ACTIVATE', 'Activate user accounts', 'USER'),
    ('USER_DEACTIVATE', 'Deactivate user accounts', 'USER'),
    ('USER_SESSION_LIMIT', 'Manage concurrent session limits', 'USER'),
    ('USER_PASSWORD_RESET', 'Trigger administrative password resets', 'USER'),

    -- Role & Permission Management
    ('ROLE_VIEW', 'View roles and their permissions', 'USER'),
    ('ROLE_MANAGE', 'Create, update, and manage custom roles and permission assignments', 'USER'),

    -- Clinic Profile & Configuration
    ('CLINIC_PROFILE_VIEW', 'View clinic profile and details', 'CLINIC'),
    ('CLINIC_PROFILE_UPDATE', 'Update clinic profile, logo, and contacts', 'CLINIC'),
    ('CLINIC_SETTINGS_UPDATE', 'Configure clinic operational settings', 'CLINIC'),

    -- Department & Profile Management
    ('DEPARTMENT_VIEW', 'View clinic departments and profiles', 'DEPARTMENT'),
    ('DEPARTMENT_CREATE', 'Create clinic departments', 'DEPARTMENT'),
    ('DEPARTMENT_UPDATE', 'Update clinic departments and profiles', 'DEPARTMENT'),
    ('DEPARTMENT_DELETE', 'Delete clinic departments', 'DEPARTMENT'),
    ('DEPARTMENT_PROFILE_MANAGE', 'Manage and assign department profiles and encounter types', 'DEPARTMENT'),

    -- Clinical Form Templates
    ('FORM_TEMPLATE_VIEW', 'View clinical standalone form templates', 'FORM_TEMPLATE'),
    ('FORM_TEMPLATE_CREATE', 'Design new clinical form templates', 'FORM_TEMPLATE'),
    ('FORM_TEMPLATE_UPDATE', 'Edit clinical form templates and fields', 'FORM_TEMPLATE'),
    ('FORM_TEMPLATE_PUBLISH', 'Publish new versions of clinical form templates', 'FORM_TEMPLATE'),
    ('FORM_TEMPLATE_DELETE', 'Archive or delete clinical form templates', 'FORM_TEMPLATE'),

    -- Patient Management
    ('PATIENT_VIEW', 'Search and view patient demographics and history', 'PATIENT'),
    ('PATIENT_CREATE', 'Register new patients', 'PATIENT'),
    ('PATIENT_UPDATE', 'Update patient demographic information', 'PATIENT'),
    ('PATIENT_DELETE', 'Archive or remove patient records', 'PATIENT'),
    ('PATIENT_INSURANCE_VIEW', 'View patient insurance policies and coverages', 'PATIENT'),
    ('PATIENT_INSURANCE_MANAGE', 'Add, update, or remove patient insurance policies', 'PATIENT'),

    -- Visit Lifecycle & Routing
    ('VISIT_VIEW', 'View patient visits and queue lists', 'VISIT'),
    ('VISIT_CREATE', 'Create new patient visits and check-in', 'VISIT'),
    ('VISIT_UPDATE', 'Update visit metadata and notes', 'VISIT'),
    ('VISIT_CANCEL', 'Cancel active patient visits', 'VISIT'),
    ('VISIT_ADD_DEPARTMENT', 'Route visit to additional clinic departments', 'VISIT'),
    ('VISIT_DATE_CHANGE', 'Modify visit scheduling and consultation dates', 'VISIT'),
    ('VISIT_FINALISE', 'Finalise and close patient visits and departments', 'VISIT'),
    ('VISIT_SETTINGS_MANAGE', 'Manage visit overrides and administrative settings', 'VISIT'),

    -- Triage & Vital Signs
    ('TRIAGE_VIEW', 'View vital signs and triage assessments', 'TRIAGE'),
    ('TRIAGE_RECORD', 'Conduct triage and record vital measurements', 'TRIAGE'),
    ('VITAL_SIGNS_RECORD', 'Record and update patient vital signs', 'TRIAGE'),

    -- Clinical Consultation & Practice
    ('CONSULTATION_VIEW', 'View clinical consultation records and notes', 'CONSULTATION'),
    ('CONSULTATION_CONDUCT', 'Conduct consultations and fill clinical encounter forms', 'CONSULTATION'),
    ('CONSULTATION_ANSWERS_SAVE', 'Save and edit consultation form answers', 'CONSULTATION'),
    ('CONSULTATION_FINALISE', 'Finalise clinical consultation forms', 'CONSULTATION'),
    ('DIAGNOSIS_RECORD', 'Add and manage clinical diagnoses (ICD-10/custom)', 'CONSULTATION'),
    ('MEDICATION_PRESCRIBE', 'Prescribe medications and dosage instructions', 'CONSULTATION'),
    ('CLINICAL_NOTE_CREATE', 'Create clinical and departmental notes', 'CONSULTATION'),
    ('CLINICAL_NOTE_VIEW', 'Read clinical and departmental notes', 'CONSULTATION'),

    -- Billing, Payments & Invoicing
    ('BILLING_VIEW', 'View visit billings, items, and payment status', 'BILLING'),
    ('BILLING_PROCESS', 'Bill visit products and apply coverage splits', 'BILLING'),
    ('BILLING_EDIT', 'Modify existing billing lines, prices, and splits', 'BILLING'),
    ('BILLING_PAYMENT_COLLECT', 'Collect payments and record receipts', 'BILLING'),
    ('INVOICE_GENERATE', 'Generate, download, and preview printable invoices and receipts', 'BILLING'),
    ('BILLING_EXEMPTION_APPLY', 'Apply patient waivers and full billing exemptions', 'BILLING'),

    -- Products, Pricing & Insurance Coverage Rules
    ('PRODUCT_VIEW', 'View billable products, acts, and medications catalog', 'PRODUCT'),
    ('PRODUCT_CREATE', 'Add new billable products to catalog', 'PRODUCT'),
    ('PRODUCT_UPDATE', 'Update product details and base prices', 'PRODUCT'),
    ('PRODUCT_DELETE', 'Remove products from catalog', 'PRODUCT'),
    ('PRODUCT_PRICE_MANAGE', 'Configure insurance tier prices and coverage percentages', 'PRODUCT'),
    ('INSURANCE_COVERAGE_MANAGE', 'Manage insurance providers, plans, and coverage rules', 'PRODUCT'),

    -- Audit & Administration
    ('AUDIT_LOG_VIEW', 'View administrative and clinical audit logs', 'ADMIN'),
    ('SEARCH_INDEX_MANAGE', 'Trigger search index re-indexing and maintenance', 'ADMIN')
ON CONFLICT (name) DO NOTHING;

-- 5. Seed System Default Roles
INSERT INTO roles (name, description, is_system) VALUES
    ('ADMIN', 'Super Administrator with unrestricted access to all system functions', TRUE),
    ('CLINIC_ADMIN', 'Clinic Administrator managing staff, clinic configuration, departments, and templates', TRUE),
    ('MANAGER', 'Operations Manager overseeing department workflows, visit finalisation, and supervision', TRUE),
    ('CLINICIAN', 'Medical Doctor / Specialist conducting consultations, diagnoses, and prescriptions', TRUE),
    ('NURSE', 'Nursing Officer responsible for patient triage, vital signs, and intake notes', TRUE),
    ('RECEPTION', 'Receptionist managing patient registration, queue check-in, and department routing', TRUE),
    ('FINANCE', 'Billing and Finance Officer managing invoices, payments, and insurance claims', TRUE),
    ('STAFF', 'General clinic staff with basic informational access', TRUE)
ON CONFLICT (name) DO NOTHING;

-- 6. Map Standard Permissions to Roles

-- Helper function to map permissions by role name
DO $$
DECLARE
    r_admin UUID;
    r_clinic_admin UUID;
    r_manager UUID;
    r_clinician UUID;
    r_nurse UUID;
    r_reception UUID;
    r_finance UUID;
    r_staff UUID;
BEGIN
    SELECT id INTO r_admin FROM roles WHERE name = 'ADMIN';
    SELECT id INTO r_clinic_admin FROM roles WHERE name = 'CLINIC_ADMIN';
    SELECT id INTO r_manager FROM roles WHERE name = 'MANAGER';
    SELECT id INTO r_clinician FROM roles WHERE name = 'CLINICIAN';
    SELECT id INTO r_nurse FROM roles WHERE name = 'NURSE';
    SELECT id INTO r_reception FROM roles WHERE name = 'RECEPTION';
    SELECT id INTO r_finance FROM roles WHERE name = 'FINANCE';
    SELECT id INTO r_staff FROM roles WHERE name = 'STAFF';

    -- ADMIN gets ALL permissions
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT r_admin, id FROM permissions
    ON CONFLICT DO NOTHING;

    -- CLINIC_ADMIN gets all clinic management permissions
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT r_clinic_admin, id FROM permissions
    WHERE module IN ('USER', 'CLINIC', 'DEPARTMENT', 'FORM_TEMPLATE', 'PATIENT', 'VISIT', 'TRIAGE', 'CONSULTATION', 'BILLING', 'PRODUCT', 'ADMIN')
      AND name NOT IN ('AUDIT_LOG_VIEW')
    ON CONFLICT DO NOTHING;

    -- MANAGER permissions
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT r_manager, id FROM permissions
    WHERE name IN (
        'USER_VIEW', 'ROLE_VIEW', 'CLINIC_PROFILE_VIEW',
        'DEPARTMENT_VIEW', 'DEPARTMENT_PROFILE_MANAGE', 'FORM_TEMPLATE_VIEW',
        'PATIENT_VIEW', 'PATIENT_INSURANCE_VIEW',
        'VISIT_VIEW', 'VISIT_UPDATE', 'VISIT_DATE_CHANGE', 'VISIT_FINALISE', 'VISIT_SETTINGS_MANAGE',
        'TRIAGE_VIEW', 'CONSULTATION_VIEW', 'CLINICAL_NOTE_VIEW',
        'BILLING_VIEW', 'INVOICE_GENERATE',
        'PRODUCT_VIEW', 'PRODUCT_PRICE_MANAGE', 'INSURANCE_COVERAGE_MANAGE'
    )
    ON CONFLICT DO NOTHING;

    -- CLINICIAN permissions
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT r_clinician, id FROM permissions
    WHERE name IN (
        'DEPARTMENT_VIEW', 'FORM_TEMPLATE_VIEW',
        'PATIENT_VIEW', 'PATIENT_INSURANCE_VIEW',
        'VISIT_VIEW', 'VISIT_CREATE', 'VISIT_ADD_DEPARTMENT',
        'TRIAGE_VIEW', 'TRIAGE_RECORD', 'VITAL_SIGNS_RECORD',
        'CONSULTATION_VIEW', 'CONSULTATION_CONDUCT', 'CONSULTATION_ANSWERS_SAVE', 'CONSULTATION_FINALISE',
        'DIAGNOSIS_RECORD', 'MEDICATION_PRESCRIBE', 'CLINICAL_NOTE_CREATE', 'CLINICAL_NOTE_VIEW',
        'BILLING_VIEW', 'PRODUCT_VIEW'
    )
    ON CONFLICT DO NOTHING;

    -- NURSE permissions
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT r_nurse, id FROM permissions
    WHERE name IN (
        'DEPARTMENT_VIEW', 'FORM_TEMPLATE_VIEW',
        'PATIENT_VIEW', 'PATIENT_INSURANCE_VIEW',
        'VISIT_VIEW', 'VISIT_CREATE', 'VISIT_ADD_DEPARTMENT',
        'TRIAGE_VIEW', 'TRIAGE_RECORD', 'VITAL_SIGNS_RECORD',
        'CONSULTATION_VIEW', 'CLINICAL_NOTE_CREATE', 'CLINICAL_NOTE_VIEW',
        'PRODUCT_VIEW'
    )
    ON CONFLICT DO NOTHING;

    -- RECEPTION permissions
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT r_reception, id FROM permissions
    WHERE name IN (
        'USER_VIEW', 'DEPARTMENT_VIEW',
        'PATIENT_VIEW', 'PATIENT_CREATE', 'PATIENT_UPDATE',
        'PATIENT_INSURANCE_VIEW', 'PATIENT_INSURANCE_MANAGE',
        'VISIT_VIEW', 'VISIT_CREATE', 'VISIT_UPDATE', 'VISIT_ADD_DEPARTMENT', 'VISIT_CANCEL',
        'BILLING_VIEW', 'PRODUCT_VIEW', 'INSURANCE_COVERAGE_MANAGE'
    )
    ON CONFLICT DO NOTHING;

    -- FINANCE permissions
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT r_finance, id FROM permissions
    WHERE name IN (
        'USER_VIEW', 'DEPARTMENT_VIEW',
        'PATIENT_VIEW', 'PATIENT_INSURANCE_VIEW', 'PATIENT_INSURANCE_MANAGE',
        'VISIT_VIEW',
        'BILLING_VIEW', 'BILLING_PROCESS', 'BILLING_EDIT', 'BILLING_PAYMENT_COLLECT', 'INVOICE_GENERATE', 'BILLING_EXEMPTION_APPLY',
        'PRODUCT_VIEW', 'PRODUCT_PRICE_MANAGE', 'INSURANCE_COVERAGE_MANAGE'
    )
    ON CONFLICT DO NOTHING;

    -- STAFF permissions
    INSERT INTO role_permissions (role_id, permission_id)
    SELECT r_staff, id FROM permissions
    WHERE name IN (
        'DEPARTMENT_VIEW', 'PATIENT_VIEW', 'VISIT_VIEW', 'PRODUCT_VIEW'
    )
    ON CONFLICT DO NOTHING;
END $$;

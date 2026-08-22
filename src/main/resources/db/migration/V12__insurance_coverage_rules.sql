CREATE TABLE insurance_coverage_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insurance_provider_id UUID NOT NULL REFERENCES insurance_providers(id) ON DELETE CASCADE,
    department_id UUID REFERENCES departments(id) ON DELETE CASCADE,
    encounter_type VARCHAR(32),
    patient_share_percentage INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_coverage_rule UNIQUE (insurance_provider_id, department_id, encounter_type),
    CONSTRAINT chk_patient_share CHECK (patient_share_percentage >= 0 AND patient_share_percentage <= 100)
);

CREATE INDEX idx_coverage_rule_provider ON insurance_coverage_rules(insurance_provider_id);
CREATE INDEX idx_coverage_rule_dept ON insurance_coverage_rules(department_id);

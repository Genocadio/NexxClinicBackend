ALTER TABLE visit_department_diagnostics
    ADD COLUMN diagnosis_type varchar(16) NOT NULL DEFAULT 'FINAL'
        CHECK (diagnosis_type IN ('FINAL', 'HYPOTHESIS')),
    ADD COLUMN notes text NOT NULL DEFAULT '';

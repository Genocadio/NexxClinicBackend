CREATE TABLE visit_department_symptoms (
    id uuid NOT NULL,
    visit_department_id uuid NOT NULL,
    symptom_name varchar(255) NOT NULL,
    sonomed_id varchar(255),
    notes text NOT NULL DEFAULT '',
    created_at timestamp(6) NOT NULL,
    updated_at timestamp(6) NOT NULL,
    CONSTRAINT pk_visit_department_symptoms PRIMARY KEY (id),
    CONSTRAINT fk_visit_department_symptoms_department
        FOREIGN KEY (visit_department_id) REFERENCES visit_departments(id)
);

CREATE INDEX idx_visit_department_symptoms_department
    ON visit_department_symptoms (visit_department_id);

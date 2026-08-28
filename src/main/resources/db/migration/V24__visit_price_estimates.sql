-- V24: Create visit_price_estimates table.
-- Per-product line pre-billing estimates that track which insurance would be
-- applied and at what percentage, even before a biller creates a real bill.
-- One row per active (non-deleted) visit_department_product.
-- Deleted when billing is created (billVisit).

CREATE TABLE visit_price_estimates (
    id                          uuid not null primary key,
    visit_id                    uuid not null references visits(id),
    visit_department_product_id uuid not null references visit_department_products(id),

    -- Which insurance would be auto-applied (null = private / no insurance covers it)
    applied_patient_insurance_id uuid references patient_insurances(id),
    resolved_patient_share_pct   integer not null default 0,
    patient_share_source         varchar(20),

    -- Pricing
    unit_price                   numeric(19,2) not null default 0,
    quantity                     numeric(19,4) not null default 1,
    line_total                   numeric(19,2) not null default 0,
    insurance_covered_amount     numeric(19,2) not null default 0,
    patient_payable_amount       numeric(19,2) not null default 0,

    created_at                   timestamp(6) not null default now(),
    updated_at                   timestamp(6) not null default now(),

    constraint uk_visit_price_estimate_product unique (visit_department_product_id)
);

CREATE INDEX idx_visit_price_estimates_visit ON visit_price_estimates(visit_id);

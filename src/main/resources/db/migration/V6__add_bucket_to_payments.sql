-- Add department_insurance_billing_id to visit_billing_payments to allow bucket-level payment attribution
ALTER TABLE visit_billing_payments ADD COLUMN IF NOT EXISTS department_insurance_billing_id UUID;

-- Optional: Add a foreign key constraint
-- NOTE: the "bucket" entity (DepartmentInsuranceBilling) is mapped to the
-- table visit_billings (see @Table(name = "visit_billings")), NOT
-- department_insurance_billings.
ALTER TABLE visit_billing_payments 
ADD CONSTRAINT fk_visit_billing_payments_dept_ins_billing 
FOREIGN KEY (department_insurance_billing_id) 
REFERENCES visit_billings(id);

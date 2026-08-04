-- Add department_insurance_billing_id to visit_billing_payments to allow bucket-level payment attribution
ALTER TABLE visit_billing_payments ADD COLUMN department_insurance_billing_id UUID;

-- Optional: Add a foreign key constraint
ALTER TABLE visit_billing_payments 
ADD CONSTRAINT fk_visit_billing_payments_dept_ins_billing 
FOREIGN KEY (department_insurance_billing_id) 
REFERENCES department_insurance_billings(id);

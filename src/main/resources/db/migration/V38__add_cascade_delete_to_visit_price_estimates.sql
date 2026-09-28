-- V38: Add ON DELETE CASCADE to visit_price_estimates foreign keys
-- so deleting a visit_department_product or a visit automatically removes
-- its pre-billing price estimates without FK constraint violations.

ALTER TABLE visit_price_estimates
    DROP CONSTRAINT IF EXISTS visit_price_estimates_visit_department_product_id_fkey,
    ADD CONSTRAINT visit_price_estimates_visit_department_product_id_fkey
        FOREIGN KEY (visit_department_product_id)
        REFERENCES visit_department_products(id)
        ON DELETE CASCADE;

ALTER TABLE visit_price_estimates
    DROP CONSTRAINT IF EXISTS visit_price_estimates_visit_id_fkey,
    ADD CONSTRAINT visit_price_estimates_visit_id_fkey
        FOREIGN KEY (visit_id)
        REFERENCES visits(id)
        ON DELETE CASCADE;

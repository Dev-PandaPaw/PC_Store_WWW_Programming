-- Structured facts for basic socket guardrails. Unknown values stay NULL.
CREATE TABLE product_component_specs (
    product_id UUID PRIMARY KEY REFERENCES products(id) ON DELETE CASCADE,
    socket VARCHAR(20)
);
-- Only explicit socket declarations in existing catalog descriptions are imported.
INSERT INTO product_component_specs (product_id,socket)
SELECT p.id, replace(upper(substring(p.description from '(?i)(LGA[ -]*[0-9]{3,4}|AM[345])')), ' ', '')
FROM products p JOIN categories c ON c.id=p.category_id
WHERE c.slug IN ('cpu','mainboard')
ON CONFLICT (product_id) DO NOTHING;

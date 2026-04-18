ALTER TABLE medicines
    ADD COLUMN IF NOT EXISTS base_unit VARCHAR(50);
ALTER TABLE medicines
    ADD COLUMN IF NOT EXISTS base_quantity INTEGER;
UPDATE medicines
SET base_unit = LOWER(TRIM(COALESCE(unit_type, 'tablet')))
WHERE base_unit IS NULL OR TRIM(base_unit) = '';
UPDATE medicines
SET base_quantity = quantity
WHERE base_quantity IS NULL;
ALTER TABLE medicines
    ALTER COLUMN base_unit SET NOT NULL;
ALTER TABLE medicines
    ALTER COLUMN base_quantity SET NOT NULL;
CREATE TABLE IF NOT EXISTS medicine_unit_definitions (
    medicine_id BIGINT NOT NULL,
    unit_type VARCHAR(50) NOT NULL,
    parent_unit VARCHAR(50),
    units_per_parent INTEGER,
    conversion_to_base INTEGER NOT NULL,
    purchase_price DECIMAL(12, 2) NOT NULL,
    selling_price DECIMAL(12, 2) NOT NULL,
    CONSTRAINT pk_medicine_unit_definitions PRIMARY KEY (medicine_id, unit_type),
    CONSTRAINT fk_medicine_unit_definitions_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE
);
INSERT INTO medicine_unit_definitions (medicine_id, unit_type, parent_unit, units_per_parent, conversion_to_base, purchase_price, selling_price)
SELECT m.id,
       LOWER(TRIM(m.unit_type)),
       NULL,
       NULL,
       1,
       m.purchase_price,
       m.selling_price
FROM medicines m
WHERE NOT EXISTS (
    SELECT 1
    FROM medicine_unit_definitions mud
    WHERE mud.medicine_id = m.id
      AND mud.unit_type = LOWER(TRIM(m.unit_type))
);
INSERT INTO medicine_unit_definitions (medicine_id, unit_type, parent_unit, units_per_parent, conversion_to_base, purchase_price, selling_price)
SELECT mau.medicine_id,
       LOWER(TRIM(mau.unit_type)),
       NULL,
       NULL,
       1,
       m.purchase_price,
       m.selling_price
FROM medicine_allowed_units mau
JOIN medicines m ON m.id = mau.medicine_id
WHERE NOT EXISTS (
    SELECT 1
    FROM medicine_unit_definitions mud
    WHERE mud.medicine_id = mau.medicine_id
      AND mud.unit_type = LOWER(TRIM(mau.unit_type))
);

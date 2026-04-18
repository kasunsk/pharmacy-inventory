CREATE TABLE IF NOT EXISTS medicine_allowed_units (
    medicine_id BIGINT NOT NULL,
    unit_type VARCHAR(50) NOT NULL,
    CONSTRAINT pk_medicine_allowed_units PRIMARY KEY (medicine_id, unit_type),
    CONSTRAINT fk_medicine_allowed_units_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE
);

INSERT INTO medicine_allowed_units (medicine_id, unit_type)
SELECT m.id, LOWER(TRIM(m.unit_type))
FROM medicines m
WHERE TRIM(COALESCE(m.unit_type, '')) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM medicine_allowed_units mau
      WHERE mau.medicine_id = m.id
        AND mau.unit_type = LOWER(TRIM(m.unit_type))
  );



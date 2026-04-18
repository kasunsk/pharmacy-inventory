-- Normalize legacy DEFAULT tenant code to DEMO when possible.
UPDATE tenants
SET code = 'DEMO', name = 'DEMO'
WHERE UPPER(code) = 'DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM tenants WHERE UPPER(code) = 'DEMO');
INSERT INTO tenants (code, name, enabled, billing_enabled, transactions_enabled, inventory_enabled, analytics_enabled, ai_assistant_enabled)
SELECT 'DEMO', 'DEMO', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM tenants WHERE UPPER(code) = 'DEMO'
);
INSERT INTO pharmacies (tenant_id, code, name, enabled)
SELECT t.id, 'MAIN', 'Demo Pharmacy P/L Badulla', TRUE
FROM tenants t
WHERE UPPER(t.code) = 'DEMO'
  AND NOT EXISTS (
      SELECT 1 FROM pharmacies p WHERE p.tenant_id = t.id AND UPPER(p.code) = 'MAIN'
  );
INSERT INTO pharmacies (tenant_id, code, name, enabled)
SELECT t.id, 'HALIELA', 'Demo HaliEla Medicine', TRUE
FROM tenants t
WHERE UPPER(t.code) = 'DEMO'
  AND NOT EXISTS (
      SELECT 1 FROM pharmacies p WHERE p.tenant_id = t.id AND UPPER(p.code) = 'HALIELA'
  );
UPDATE pharmacies p
SET name = 'Demo Pharmacy P/L Badulla', enabled = TRUE
WHERE UPPER(p.code) = 'MAIN'
  AND p.tenant_id = (SELECT id FROM tenants WHERE UPPER(code) = 'DEMO');
UPDATE pharmacies p
SET name = 'Demo HaliEla Medicine', enabled = TRUE
WHERE UPPER(p.code) = 'HALIELA'
  AND p.tenant_id = (SELECT id FROM tenants WHERE UPPER(code) = 'DEMO');
UPDATE tenants t
SET default_pharmacy_id = (
    SELECT p.id
    FROM pharmacies p
    WHERE p.tenant_id = t.id AND UPPER(p.code) = 'MAIN'
)
WHERE UPPER(t.code) = 'DEMO';
INSERT INTO users (username, tenant_id, password_hash, enabled, default_pharmacy_id)
SELECT
    'admin',
    t.id,
    '$2b$12$lrQ.002.m/1lwQ8mmUWzyu7GaCveS8WV6TpfTcOCrZw7DvC/UECUi',
    TRUE,
    (SELECT p.id FROM pharmacies p WHERE p.tenant_id = t.id AND UPPER(p.code) = 'MAIN')
FROM tenants t
WHERE UPPER(t.code) = 'DEMO'
  AND NOT EXISTS (
      SELECT 1 FROM users u WHERE u.tenant_id = t.id AND u.username = 'admin'
  );
UPDATE users u
SET password_hash = '$2b$12$lrQ.002.m/1lwQ8mmUWzyu7GaCveS8WV6TpfTcOCrZw7DvC/UECUi',
    enabled = TRUE,
    default_pharmacy_id = (
        SELECT p.id
        FROM pharmacies p
        WHERE p.tenant_id = u.tenant_id
          AND UPPER(p.code) = 'MAIN'
    )
WHERE u.username = 'admin'
  AND u.tenant_id = (SELECT id FROM tenants WHERE UPPER(code) = 'DEMO');
INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ADMIN'
FROM users u
JOIN tenants t ON t.id = u.tenant_id
WHERE UPPER(t.code) = 'DEMO'
  AND u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1 FROM user_roles r WHERE r.user_id = u.id AND r.role = 'ADMIN'
  );
INSERT INTO user_pharmacies (user_id, pharmacy_id)
SELECT u.id, p.id
FROM users u
JOIN tenants t ON t.id = u.tenant_id
JOIN pharmacies p ON p.tenant_id = t.id
WHERE UPPER(t.code) = 'DEMO'
  AND u.username = 'admin'
  AND UPPER(p.code) IN ('MAIN', 'HALIELA')
  AND NOT EXISTS (
      SELECT 1
      FROM user_pharmacies up
      WHERE up.user_id = u.id
        AND up.pharmacy_id = p.id
  );
INSERT INTO medicines (name, batch_number, tenant_id, pharmacy_id, expiry_date, supplier, unit_type, purchase_price, selling_price, quantity)
SELECT 'Paracetamol 500mg', 'BAD-PARA-001', t.id, p.id, DATE '2028-12-31', 'Hemas Pharma', 'tablet', 18.00, 25.00, 180
FROM tenants t
JOIN pharmacies p ON p.tenant_id = t.id
WHERE UPPER(t.code) = 'DEMO' AND UPPER(p.code) = 'MAIN'
  AND NOT EXISTS (
      SELECT 1 FROM medicines m WHERE m.tenant_id = t.id AND m.pharmacy_id = p.id AND m.batch_number = 'BAD-PARA-001'
  );
INSERT INTO medicines (name, batch_number, tenant_id, pharmacy_id, expiry_date, supplier, unit_type, purchase_price, selling_price, quantity)
SELECT 'Amoxicillin 500mg', 'BAD-AMOX-002', t.id, p.id, DATE '2028-10-31', 'Cipla Lanka', 'capsule', 42.00, 60.00, 55
FROM tenants t
JOIN pharmacies p ON p.tenant_id = t.id
WHERE UPPER(t.code) = 'DEMO' AND UPPER(p.code) = 'MAIN'
  AND NOT EXISTS (
      SELECT 1 FROM medicines m WHERE m.tenant_id = t.id AND m.pharmacy_id = p.id AND m.batch_number = 'BAD-AMOX-002'
  );
INSERT INTO medicines (name, batch_number, tenant_id, pharmacy_id, expiry_date, supplier, unit_type, purchase_price, selling_price, quantity)
SELECT 'Cetirizine 10mg', 'BAD-CETI-003', t.id, p.id, DATE '2029-02-28', 'State Pharma', 'tablet', 12.00, 20.00, 140
FROM tenants t
JOIN pharmacies p ON p.tenant_id = t.id
WHERE UPPER(t.code) = 'DEMO' AND UPPER(p.code) = 'MAIN'
  AND NOT EXISTS (
      SELECT 1 FROM medicines m WHERE m.tenant_id = t.id AND m.pharmacy_id = p.id AND m.batch_number = 'BAD-CETI-003'
  );
INSERT INTO medicines (name, batch_number, tenant_id, pharmacy_id, expiry_date, supplier, unit_type, purchase_price, selling_price, quantity)
SELECT 'Metformin 500mg', 'BAD-METF-004', t.id, p.id, DATE '2028-11-30', 'Sun Pharma', 'tablet', 24.00, 35.00, 78
FROM tenants t
JOIN pharmacies p ON p.tenant_id = t.id
WHERE UPPER(t.code) = 'DEMO' AND UPPER(p.code) = 'MAIN'
  AND NOT EXISTS (
      SELECT 1 FROM medicines m WHERE m.tenant_id = t.id AND m.pharmacy_id = p.id AND m.batch_number = 'BAD-METF-004'
  );
INSERT INTO medicines (name, batch_number, tenant_id, pharmacy_id, expiry_date, supplier, unit_type, purchase_price, selling_price, quantity)
SELECT 'ORS Sachet', 'BAD-ORS-005', t.id, p.id, DATE '2028-06-30', 'GSK Sri Lanka', 'sachet', 28.00, 40.00, 210
FROM tenants t
JOIN pharmacies p ON p.tenant_id = t.id
WHERE UPPER(t.code) = 'DEMO' AND UPPER(p.code) = 'MAIN'
  AND NOT EXISTS (
      SELECT 1 FROM medicines m WHERE m.tenant_id = t.id AND m.pharmacy_id = p.id AND m.batch_number = 'BAD-ORS-005'
  );
INSERT INTO medicines (name, batch_number, tenant_id, pharmacy_id, expiry_date, supplier, unit_type, purchase_price, selling_price, quantity)
SELECT 'Paracetamol 500mg', 'HAL-PARA-001', t.id, p.id, DATE '2028-12-31', 'Hemas Pharma', 'tablet', 18.00, 25.00, 42
FROM tenants t
JOIN pharmacies p ON p.tenant_id = t.id
WHERE UPPER(t.code) = 'DEMO' AND UPPER(p.code) = 'HALIELA'
  AND NOT EXISTS (
      SELECT 1 FROM medicines m WHERE m.tenant_id = t.id AND m.pharmacy_id = p.id AND m.batch_number = 'HAL-PARA-001'
  );
INSERT INTO medicines (name, batch_number, tenant_id, pharmacy_id, expiry_date, supplier, unit_type, purchase_price, selling_price, quantity)
SELECT 'Amoxicillin 500mg', 'HAL-AMOX-002', t.id, p.id, DATE '2028-10-31', 'Cipla Lanka', 'capsule', 42.00, 60.00, 125
FROM tenants t
JOIN pharmacies p ON p.tenant_id = t.id
WHERE UPPER(t.code) = 'DEMO' AND UPPER(p.code) = 'HALIELA'
  AND NOT EXISTS (
      SELECT 1 FROM medicines m WHERE m.tenant_id = t.id AND m.pharmacy_id = p.id AND m.batch_number = 'HAL-AMOX-002'
  );
INSERT INTO medicines (name, batch_number, tenant_id, pharmacy_id, expiry_date, supplier, unit_type, purchase_price, selling_price, quantity)
SELECT 'Cetirizine 10mg', 'HAL-CETI-003', t.id, p.id, DATE '2029-02-28', 'State Pharma', 'tablet', 12.00, 20.00, 66
FROM tenants t
JOIN pharmacies p ON p.tenant_id = t.id
WHERE UPPER(t.code) = 'DEMO' AND UPPER(p.code) = 'HALIELA'
  AND NOT EXISTS (
      SELECT 1 FROM medicines m WHERE m.tenant_id = t.id AND m.pharmacy_id = p.id AND m.batch_number = 'HAL-CETI-003'
  );
INSERT INTO medicines (name, batch_number, tenant_id, pharmacy_id, expiry_date, supplier, unit_type, purchase_price, selling_price, quantity)
SELECT 'Metformin 500mg', 'HAL-METF-004', t.id, p.id, DATE '2028-11-30', 'Sun Pharma', 'tablet', 24.00, 35.00, 190
FROM tenants t
JOIN pharmacies p ON p.tenant_id = t.id
WHERE UPPER(t.code) = 'DEMO' AND UPPER(p.code) = 'HALIELA'
  AND NOT EXISTS (
      SELECT 1 FROM medicines m WHERE m.tenant_id = t.id AND m.pharmacy_id = p.id AND m.batch_number = 'HAL-METF-004'
  );
INSERT INTO medicines (name, batch_number, tenant_id, pharmacy_id, expiry_date, supplier, unit_type, purchase_price, selling_price, quantity)
SELECT 'ORS Sachet', 'HAL-ORS-005', t.id, p.id, DATE '2028-06-30', 'GSK Sri Lanka', 'sachet', 28.00, 40.00, 88
FROM tenants t
JOIN pharmacies p ON p.tenant_id = t.id
WHERE UPPER(t.code) = 'DEMO' AND UPPER(p.code) = 'HALIELA'
  AND NOT EXISTS (
      SELECT 1 FROM medicines m WHERE m.tenant_id = t.id AND m.pharmacy_id = p.id AND m.batch_number = 'HAL-ORS-005'
  );

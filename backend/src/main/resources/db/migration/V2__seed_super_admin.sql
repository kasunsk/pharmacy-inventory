INSERT INTO users (username, tenant_id, password_hash, enabled)
SELECT
    'super_admin',
    NULL,
    '$2b$12$lrQ.002.m/1lwQ8mmUWzyu7GaCveS8WV6TpfTcOCrZw7DvC/UECUi',
    TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE username = 'super_admin' AND tenant_id IS NULL
);
INSERT INTO user_roles (user_id, role)
SELECT u.id, 'SUPER_ADMIN'
FROM users u
WHERE u.username = 'super_admin'
  AND u.tenant_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles r
      WHERE r.user_id = u.id
        AND r.role = 'SUPER_ADMIN'
  );

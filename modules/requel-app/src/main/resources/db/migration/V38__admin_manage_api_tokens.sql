-- #392: the built-in admin gets ProjectUserRole.manageApiTokens, so a personal access token can be
-- minted without first editing admin's own permissions. New installs get it from
-- AdminProjectRoleInitializer; this covers databases where admin already has ProjectUserRole.
-- Only the "admin" user; anyone else still has it granted explicitly. Re-running is a no-op.

-- The permission row is normally created at startup (after Flyway); make sure it exists here.
INSERT IGNORE INTO user_role_permissions (name, role_type)
VALUES ('manageApiTokens', 'com.rreganjr.requel.project.ProjectUserRole');

INSERT INTO user_roles_permissions (user_role_id, user_role_permission_id)
SELECT r.id, p.id
FROM users u
JOIN user_roles r
  ON r.user_id = u.id AND r.role_type = 'com.rreganjr.requel.project.ProjectUserRole'
JOIN user_role_permissions p
  ON p.name = 'manageApiTokens' AND p.role_type = r.role_type
WHERE u.username = 'admin'
  AND NOT EXISTS (SELECT 1 FROM user_roles_permissions x
                  WHERE x.user_role_id = r.id AND x.user_role_permission_id = p.id);

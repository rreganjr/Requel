-- #381: a user with must_change_password set can sign in but can only change their password.
-- Every existing user starts unflagged. The built-in admin is flagged at startup by
-- DefaultAdminPasswordInitializer while its password is still "admin", so an install whose admin
-- password was already changed is unaffected.

ALTER TABLE `users` ADD COLUMN `must_change_password` BOOLEAN NOT NULL DEFAULT FALSE;

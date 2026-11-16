-- #446: when the user's password last changed (their own change or an admin reset). Sign-ins from
-- before it end: a login token or OAuth access token issued earlier is refused. Existing users
-- start with NULL, which refuses nothing. The rehash a sign-in does when the stored algorithm is
-- out of date is not a change and leaves it alone.

ALTER TABLE `users` ADD COLUMN `password_changed_at` TIMESTAMP NULL;

-- #380: Requel 1.0.1 stored unsalted MD5 password hashes and has none of the columns 1.0.2 added
-- to record the hashing; add them, empty, so upgrade-1.0.sql sees the 1.0.2 schema. With no
-- algorithm recorded, UserImpl checks the old MD5 hash and re-hashes the password with the
-- current algorithm at the user's next sign-in.
ALTER TABLE `users`
  ADD COLUMN `password_encrypting_algorithm_name` varchar(255) DEFAULT NULL,
  ADD COLUMN `password_encrypting_iterations` int DEFAULT NULL,
  ADD COLUMN `password_salt` varchar(255) DEFAULT NULL;

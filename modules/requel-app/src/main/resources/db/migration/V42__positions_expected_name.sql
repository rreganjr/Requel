-- #459: a RewriteNamePosition records the name its proposal was built from, so choosing it is
-- refused once the name has changed. Null for every other kind of position.
ALTER TABLE `positions` ADD COLUMN `expected_name` varchar(255) DEFAULT NULL;

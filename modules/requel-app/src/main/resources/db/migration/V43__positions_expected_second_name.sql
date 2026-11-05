-- #472: a MergeStepsPosition records the names of both steps it merges, so choosing it is refused
-- once either has changed. Null for every other kind of position.
ALTER TABLE `positions` ADD COLUMN `expected_second_name` varchar(255) DEFAULT NULL;

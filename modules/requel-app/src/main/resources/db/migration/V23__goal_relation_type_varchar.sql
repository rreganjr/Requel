-- Issue #257: goal relation types are defined by GoalRelationType alone.
--
-- V1 declared relation_type as enum('Conflicts','Supports'), which rejects every value added
-- since (Refines, Duplicates, DependsOn, Obstructs, Measures). A varchar keeps the Java enum the
-- only list, so adding a value later is not a column change. Existing values are unchanged.
ALTER TABLE `goal_relations` MODIFY `relation_type` varchar(32) DEFAULT NULL;

-- Issue #261: a goal can be held by more than one stakeholder. V1 created stakeholders_goals for
-- a one-to-many mapping, so goals_id carried a unique key and a second stakeholder adding the
-- same goal failed. The mapping is now many-to-many (as actor_goals and story_goals are); the
-- primary key (abstract_stakeholder_id, goals_id) still prevents duplicates.
--
-- The goals_id foreign key needs an index of its own before the unique one can go.

CREATE INDEX `idx_stakeholders_goals_goal` ON `stakeholders_goals` (`goals_id`);
ALTER TABLE `stakeholders_goals` DROP INDEX `UKk2eufeougg9qi1ipm2nvesemk`;

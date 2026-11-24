-- #468: a step's (or scenario's) actor as a role. actor_role is PRIMARY_ACTOR, SYSTEM or NAMED, or
-- NULL when the step's wording names its actor (every step before this migration). actor_id is the
-- actor a NAMED step names; deleting that actor turns its steps back into plain wording first, so
-- the foreign key needs no ON DELETE action.
ALTER TABLE `scenarios` ADD COLUMN `actor_role` varchar(32) DEFAULT NULL;
ALTER TABLE `scenarios` ADD COLUMN `actor_id` bigint DEFAULT NULL;
ALTER TABLE `scenarios` ADD CONSTRAINT `fk_scenarios_actor` FOREIGN KEY (`actor_id`) REFERENCES `actors` (`id`);

-- #468: the two new positions name what they change, so choosing one is refused once it is gone:
-- a SetStepActorPosition the role and, for NAMED, the actor; an AddActorToUseCasePosition the actor
-- and the use case. The trimmed name a SetStepActorPosition proposes uses proposed_word, and the
-- name it was built from expected_name (#459).
ALTER TABLE `positions` ADD COLUMN `proposed_actor_role` varchar(32) DEFAULT NULL;
ALTER TABLE `positions` ADD COLUMN `proposed_actor_name` varchar(255) DEFAULT NULL;
ALTER TABLE `positions` ADD COLUMN `proposed_use_case_name` varchar(255) DEFAULT NULL;

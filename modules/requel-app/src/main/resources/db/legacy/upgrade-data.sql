-- #380: data an old release left out, for every release (it runs after the schema scripts, so
-- the schema is V1's here). Each statement only adds what is missing.

-- A project's scenarios list (pods_scenarios): neither 1.0.x's nor 1.1.0's project import filled
-- it, so an upgraded project showed no scenarios although its scenarios and steps were there.
-- Every Scenario row (not a plain Step) belongs on its project's list, as ScenarioImpl's
-- constructor does in 2.x.
INSERT INTO `pods_scenarios` (`abstract_project_or_domain_id`, `scenarios_id`)
SELECT s.`projectordomain_id`, s.`id` FROM `scenarios` s
WHERE s.`type` LIKE '%Scenario'
  AND NOT EXISTS (SELECT 1 FROM `pods_scenarios` ps WHERE ps.`scenarios_id` = s.`id`);

-- An argument's support level: the old releases left it NULL when the argument had none (as the
-- sample's does); 2.x never does, and shows a NULL level as nothing at all. NULL becomes
-- Neutral, as 2.x's project import does for an argument without one. The column holds the
-- enum's ordinal (StronglyFor 0, For 1, Neutral 2, Against 3, StronglyAgainst 4), in the same
-- order since 1.0.
UPDATE `arguments` SET `support_level` = 2 WHERE `support_level` IS NULL;

-- Issue #268: which assistants run in a project, and the lexical findings become advisory.
--
-- One row per project and assistant that has been switched; no row means the assistant runs.
-- Only assistants that declare themselves project-switchable (the four lexical checks) can be
-- set. No foreign keys, like ignored_findings: the project delete path removes the rows.
CREATE TABLE IF NOT EXISTS `project_assistant_settings` (
  `project_id` bigint NOT NULL,
  `assistant_id` varchar(200) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `updated_by_id` bigint DEFAULT NULL,
  `date_updated` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`project_id`, `assistant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- The lexical assistants now raise advisory issues (must_be_resolved = 0); the issues already
-- raised, by them or by the old LexicalAssistant, become advisory too, as #271 backfilled
-- severity. Other issues keep theirs.
UPDATE `annotations` SET `must_be_resolved` = 0
 WHERE `annotation_type` = 'com.rreganjr.requel.annotation.impl.LexicalIssue';

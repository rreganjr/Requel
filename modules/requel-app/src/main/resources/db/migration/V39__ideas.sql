-- Ideas: a rough requirement idea (text, optionally from an uploaded .txt file) submitted to a
-- project with the SubmitIdea command. See doc/work/backlog/jama-requirement-assistant-on-requel.md
-- (section 4.2). No foreign keys, like ignored_findings (V21): the project and the user are
-- referred to by id, and DeleteProject removes a project's ideas explicitly.

CREATE TABLE IF NOT EXISTS `ideas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `idea_text` longtext NOT NULL,
  `source_file_name` varchar(255) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ideas_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- A product manager's free-text idea and the requirement drafts generated from it. Soft references
-- (no foreign keys) to the project and user, as assistant_runs does. A failed or skipped generation
-- keeps the idea and its error so the user can retry.
CREATE TABLE `ideas` (
  `id` VARCHAR(36) NOT NULL,
  `project_id` BIGINT NOT NULL,
  `created_by_user_id` BIGINT NULL,
  `created_by_username` VARCHAR(255) NULL,
  `idea_text` LONGTEXT NOT NULL,
  `existing_names_json` LONGTEXT NULL,
  `status` VARCHAR(20) NOT NULL,
  `attempts` INT NOT NULL DEFAULT 0,
  `last_run_id` VARCHAR(36) NULL,
  `error_summary` VARCHAR(1000) NULL,
  `result_summary` VARCHAR(2000) NULL,
  `messages_json` LONGTEXT NULL,
  `problems_json` LONGTEXT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE INDEX `idx_ideas_project` ON `ideas` (`project_id`, `created_at`);

CREATE TABLE `idea_drafts` (
  `id` VARCHAR(36) NOT NULL,
  `idea_id` VARCHAR(36) NOT NULL,
  `sort_order` INT NOT NULL,
  `kind` VARCHAR(20) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `draft_text` LONGTEXT NOT NULL,
  `note` LONGTEXT NULL,
  `assumption` BOOLEAN NOT NULL DEFAULT FALSE,
  `evidence_json` LONGTEXT NULL,
  `evidence_unverified` BOOLEAN NOT NULL DEFAULT FALSE,
  `confidence` DOUBLE NULL,
  `references_json` LONGTEXT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE INDEX `idx_idea_drafts_idea` ON `idea_drafts` (`idea_id`, `sort_order`);

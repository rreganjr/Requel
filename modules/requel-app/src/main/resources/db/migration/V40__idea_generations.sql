-- The state of generating requirement drafts from a submitted idea (see V39 `ideas`), and the
-- drafts themselves. Soft references (no foreign keys) to the project and user, as assistant_runs
-- does. A failed or skipped generation keeps the idea text and its error so the user can retry.
CREATE TABLE `idea_generations` (
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

CREATE INDEX `idx_idea_generations_project` ON `idea_generations` (`project_id`, `created_at`);

CREATE TABLE `idea_generation_drafts` (
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

CREATE INDEX `idx_idea_generation_drafts_idea` ON `idea_generation_drafts` (`idea_id`, `sort_order`);

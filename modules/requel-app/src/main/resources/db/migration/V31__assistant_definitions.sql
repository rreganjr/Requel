-- Issue #260: assistant definitions - an assistant described as data (scope, context providers,
-- instructions, finding vocabulary, output schema) and run by one generic executor. Bundled
-- definitions (project_id NULL) are seeded from classpath ai/definitions/*.json at startup by
-- version; project definitions override a bundled one with the same key for that project.
-- Scope, context providers and vocabulary are JSON text, validated in code.

CREATE TABLE `assistant_definitions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `definition_key` VARCHAR(120) NOT NULL,
    `display_name` VARCHAR(200) NOT NULL,
    `kind` VARCHAR(20) NOT NULL,
    `task_type` VARCHAR(80) NOT NULL,
    `scope_json` TEXT NOT NULL,
    `context_providers_json` TEXT NOT NULL,
    `instructions` TEXT NOT NULL,
    `vocabulary_json` TEXT NOT NULL,
    `output_schema_name` VARCHAR(120) NOT NULL,
    `output_schema_version` VARCHAR(40) NOT NULL,
    `enabled` BOOLEAN NOT NULL,
    `definition_version` INT NOT NULL,
    `source` VARCHAR(20) NOT NULL,
    `project_id` BIGINT NULL,
    `forked_from_version` INT NULL,
    `executor_bean` VARCHAR(200) NULL,
    `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `created_by` VARCHAR(255) NULL,
    `updated_by` VARCHAR(255) NULL,
    CONSTRAINT `uq_assistant_definitions_key_project` UNIQUE (`definition_key`, `project_id`),
    INDEX `idx_assistant_definitions_project` (`project_id`)
);

-- Issue #260: findings in the run whose cited evidence is not in the entity's text (kept, and
-- counted as a provider-quality signal).
ALTER TABLE `assistant_runs` ADD COLUMN `evidence_unverified` INT NOT NULL DEFAULT 0;

-- Issue #355: the assistants' own summary of a run (for AI review, the model's one-paragraph
-- summary), so the review read can return it and the evaluation script can score it. Written after
-- the analyze phase; null for runs that produced no summary and for runs before this migration.

ALTER TABLE `assistant_runs` ADD COLUMN `result_summary` varchar(2000) DEFAULT NULL;

-- The review read returns a target's newest run. created_at was whole seconds, so two runs queued
-- in the same second tied and the "newest" was picked by the random UUID id. Microseconds make the
-- order real.
ALTER TABLE `assistant_runs` MODIFY `created_at` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- The review read lists the findings one run reported.
CREATE INDEX `idx_assistant_findings_last_seen_run` ON `assistant_findings` (`last_seen_run_id`);

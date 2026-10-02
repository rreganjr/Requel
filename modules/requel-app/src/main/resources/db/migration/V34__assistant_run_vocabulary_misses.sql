-- Issue #263: findings in the run whose type is not in the definition's vocabulary (kept, and
-- counted as a provider-quality signal, like evidence_unverified).

ALTER TABLE `assistant_runs` ADD COLUMN `vocabulary_misses` INT NOT NULL DEFAULT 0;

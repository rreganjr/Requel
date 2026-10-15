-- Issue #270: the fingerprint of the entity text (name + text) a finding was derived from, taken
-- when the assistant loaded the entity for analysis and refreshed each time a run reports the
-- finding again. A finding whose entity no longer matches it reads as stale ("may no longer
-- apply"). NULL means the finding was recorded before #270; it reads as not stale until the next
-- run of its assistant on that entity fills it in, so there is no backfill.
ALTER TABLE `assistant_findings` ADD COLUMN `target_fingerprint` char(64) DEFAULT NULL;

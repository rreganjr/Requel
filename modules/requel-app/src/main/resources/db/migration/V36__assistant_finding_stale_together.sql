-- Issue #266: a corpus finding is about a relationship, so it has one row per participant and
-- one shared issue. When any participant changes, the issue is stale on all of them.
ALTER TABLE `assistant_findings` ADD COLUMN `stale_together` BOOLEAN NOT NULL DEFAULT FALSE;

-- Issue #265: a definition that must never be sent to a remote AI provider (for example a
-- PII-detection policy, which would disclose what it looks for). Left out of a run when the
-- active provider is remote.

ALTER TABLE `assistant_definitions` ADD COLUMN `local_only` BOOLEAN NOT NULL DEFAULT FALSE;

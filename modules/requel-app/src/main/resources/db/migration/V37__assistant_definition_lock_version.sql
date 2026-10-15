-- Issue #264: project members edit their project's assistant definitions, so a write carries the
-- version it read and a stale one is refused. definition_version stays the content version that
-- bundled seeding compares and every run records.
ALTER TABLE `assistant_definitions` ADD COLUMN `lock_version` INT NOT NULL DEFAULT 0;

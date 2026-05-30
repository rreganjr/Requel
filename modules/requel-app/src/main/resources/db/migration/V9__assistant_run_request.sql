-- Persist enough of the dispatched AnalysisRequest to faithfully reconstruct it
-- when a run row is read back (e.g. by the worker before invoking assistants, or
-- by run-history / MCP tooling). Before this, only user ids and target/project
-- refs were stored, so the rebuilt request dropped usernames, locale, and the
-- attributes map. See doc/43-phase-4.5-plan.md (Step 1) and
-- doc/assistant-spi-plan.md (Dispatcher transactions and async boundaries).
ALTER TABLE assistant_runs
    ADD COLUMN triggered_by_username VARCHAR(255) NULL,
    ADD COLUMN assistant_username VARCHAR(255) NULL,
    ADD COLUMN locale VARCHAR(40) NULL,
    ADD COLUMN attributes_json TEXT NULL;

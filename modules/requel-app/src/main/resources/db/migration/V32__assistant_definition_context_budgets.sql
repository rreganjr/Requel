-- Issue #261: a definition can override the character share of each context provider it names,
-- as a JSON object ({"goal-siblings": 20000}). NULL means every provider gets the default
-- (requel.assistant.context-pack.provider-budget).

ALTER TABLE `assistant_definitions` ADD COLUMN `context_budgets_json` TEXT NULL;

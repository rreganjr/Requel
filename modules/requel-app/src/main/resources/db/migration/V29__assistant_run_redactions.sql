-- Issue #262: what the redaction policy masked in a run's context, so an operator can see what was
-- stripped before text went to an AI provider. Written before the provider call, so a run refused
-- by the egress check still shows what would have been masked. Categories are a comma-separated
-- list of DataHandlingSettings.RedactionCategory names (CREDENTIALS,EMAIL,PHONE,SSN,CARD).

ALTER TABLE `assistant_runs` ADD COLUMN `redaction_count` int NOT NULL DEFAULT 0;
ALTER TABLE `assistant_runs` ADD COLUMN `redaction_categories` varchar(200) DEFAULT NULL;

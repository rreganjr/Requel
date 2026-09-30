-- Issue #275: bundled report generators render from the classpath.
--
-- A reports row with a builtin_key renders the current bundled template of that key
-- ("project-html", "ticket-markdown"), not its stored text, so an updated bundle reaches projects
-- created before it. Editing the text of such a row detaches it (clears the key). Existing rows
-- are adopted at startup by BuiltinReportGeneratorUpgrader, which keys only an unedited copy: one
-- whose text matches a bundled version listed in xslt/builtin-history.txt.

ALTER TABLE `reports` ADD COLUMN `builtin_key` varchar(64) DEFAULT NULL;

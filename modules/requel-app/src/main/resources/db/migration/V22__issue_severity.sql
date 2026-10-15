-- Issue #271: severity on issues (LOW | MEDIUM | HIGH, stored by name).
--
-- annotations is single-table and notes share it with issues (positions and arguments have
-- their own tables), so the column is nullable and only issue rows are backfilled. Lexical issues are the legacy spell-check,
-- vague-word and glossary-phrase output and default LOW; every other issue defaults MEDIUM. The
-- entity reads a missing value as its kind's default, but the backfill means no issue row relies
-- on that.
ALTER TABLE `annotations` ADD COLUMN `severity` varchar(20) DEFAULT NULL;

UPDATE `annotations` SET `severity` = 'LOW'
 WHERE `annotation_type` = 'com.rreganjr.requel.annotation.impl.LexicalIssue';

UPDATE `annotations` SET `severity` = 'MEDIUM'
 WHERE `annotation_type` = 'com.rreganjr.requel.annotation.Issue';

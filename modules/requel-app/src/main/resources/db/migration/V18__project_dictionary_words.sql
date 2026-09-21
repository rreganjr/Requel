-- Issue #313: project-scoped dictionary layer.
--
-- Words a user adds through "Add to Dictionary" stop going into the WordNet `word` table (which is
-- an installation-wide, read-only corpus loaded from the SQL dumps) and land here instead, scoped
-- to the project they were added in.
--
-- The uniqueness rule is (project_id, lemma). MySQL's default collation makes that
-- case-insensitive; H2 under create-drop (the ITs) would not, so the repository compares
-- lower-cased in its own queries and this constraint is the backstop rather than the rule.
--
-- project_id references `pods` — the single-table inheritance table for AbstractProjectOrDomain.
-- There is no `projects` table. Only ProjectImpl rows are ever referenced.

DROP TABLE IF EXISTS `project_dictionary_words`;

CREATE TABLE `project_dictionary_words` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `lemma` varchar(80) NOT NULL,
  `phonetic_code` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pdw_project_lemma` (`project_id`,`lemma`),
  KEY `idx_pdw_project_phonetic` (`project_id`,`phonetic_code`),
  CONSTRAINT `fk_pdw_project` FOREIGN KEY (`project_id`) REFERENCES `pods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

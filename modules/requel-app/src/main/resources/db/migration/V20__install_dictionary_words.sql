-- Issue #319: installation-wide dictionary layer.
--
-- Before #319, "Add to Dictionary" with no project (and every add before #313) wrote the word into
-- the WordNet `word` table as a row with no senses. That mixed user additions into the corpus:
-- they could not be listed apart from it, and a re-import of the WordNet dumps could collide with
-- them. Installation-wide additions now live in their own table, and `word` is read-only again.
--
-- created_by_id is a soft reference with no FK: dictionary-jpa cannot name `users` (the reason
-- project_dictionary_words gives in V18). Null for the rows this migration moves, whose adder was
-- never recorded.
--
-- The default collation makes uk_idw_lemma case-insensitive; the repository also compares
-- lower-cased, so H2 under create-drop agrees (#313 decision 7).
CREATE TABLE IF NOT EXISTS `install_dictionary_words` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lemma` varchar(80) NOT NULL,
  `phonetic_code` varchar(80) DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idw_lemma` (`lemma`),
  KEY `idx_idw_phonetic` (`phonetic_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Every spelling lookup queries `word` by phonetic code (DatabaseSpellDictionary.getWords), and
-- since #319 project checkers do too. The corpus shipped without an index on that column. The
-- WordNet dumps only INSERT rows, so an index added here survives a re-import. Guarded so a
-- re-run is harmless.
SET @has_idx = (SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'word' AND index_name = 'idx_word_phonetic');
SET @sql = IF(@has_idx = 0, 'CREATE INDEX `idx_word_phonetic` ON `word` (`phonetic_code`)', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- The move. In the WordNet dumps every corpus word has at least one sense, so a `word` row that
-- no sense, lexical link, morph or definition word points at is a user addition. (semcor, vnframe
-- and the definition-word rows reference sense, not word, so the sense check covers them;
-- synset_definition_word is checked as well because its word_id has no FK of its own.) On a
-- database with no corpus loaded, `word` is empty and this does nothing.
--
-- Done in steps rather than as one WHERE NOT EXISTS ... AND NOT EXISTS ... over `word`. The
-- single statement's lexlinkref test had to match word1id OR word2id, which no index serves, and
-- morphref has no index on wordid at all, so MySQL scanned both tables once per `word` row
-- (~150k rows on a full load) and did not finish. Here the sense anti-join, which the sense.wordid
-- index serves, cuts the candidates to the handful of user additions first, and each remaining
-- reference is removed from that small set by a join driven from it.
--
-- An ordinary table rather than a TEMPORARY one, for the reason V17 gives (MySQL cannot open a
-- temporary table twice in one statement). Dropped at the end.
DROP TABLE IF EXISTS v20_user_words;

CREATE TABLE v20_user_words (
    wordid BIGINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO v20_user_words (wordid)
SELECT w.wordid
FROM `word` w
LEFT JOIN `sense` s ON s.wordid = w.wordid
WHERE s.wordid IS NULL;

DELETE u FROM v20_user_words u JOIN `lexlinkref` l ON l.word1id = u.wordid;

DELETE u FROM v20_user_words u JOIN `lexlinkref` l ON l.word2id = u.wordid;

DELETE u FROM v20_user_words u JOIN `morphref` m ON m.wordid = u.wordid;

DELETE u FROM v20_user_words u JOIN `synset_definition_word` d ON d.word_id = u.wordid;

-- Skip a lemma the table already holds, so a re-run after a partial one cannot hit uk_idw_lemma.
INSERT INTO `install_dictionary_words` (lemma, phonetic_code, created_by_id, date_created)
SELECT w.lemma, w.phonetic_code, NULL, NULL
FROM `word` w
JOIN v20_user_words u ON u.wordid = w.wordid
WHERE NOT EXISTS (SELECT 1 FROM `install_dictionary_words` i WHERE i.lemma = w.lemma);

DELETE w FROM `word` w JOIN v20_user_words u ON u.wordid = w.wordid;

DROP TABLE IF EXISTS v20_user_words;

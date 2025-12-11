-- Cleanup legacy *_seq tables left from old TableGenerator usage
-- and ensure primary key columns are AUTO_INCREMENT for MySQL.

-- Temporarily disable FK checks so referenced PK columns can be altered safely.
SET @orig_fk_checks = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- Drop obsolete sequence tables (safe if they never existed)
DROP TABLE IF EXISTS `actors_seq`;
DROP TABLE IF EXISTS `goal_relations_seq`;
DROP TABLE IF EXISTS `goals_seq`;
DROP TABLE IF EXISTS `organizations_seq`;
DROP TABLE IF EXISTS `pods_seq`;
DROP TABLE IF EXISTS `reports_seq`;
DROP TABLE IF EXISTS `scenarios_seq`;
DROP TABLE IF EXISTS `semcor_file_seq`;
DROP TABLE IF EXISTS `semcor_sentence_seq`;
DROP TABLE IF EXISTS `semcor_sentence_word_seq`;
DROP TABLE IF EXISTS `stakeholders_seq`;
DROP TABLE IF EXISTS `stories_seq`;
DROP TABLE IF EXISTS `synset_definition_word_seq`;
DROP TABLE IF EXISTS `teams_seq`;
DROP TABLE IF EXISTS `terms_seq`;
DROP TABLE IF EXISTS `usecases_seq`;
DROP TABLE IF EXISTS `user_role_permissions_seq`;
DROP TABLE IF EXISTS `user_roles_seq`;
DROP TABLE IF EXISTS `users_seq`;
DROP TABLE IF EXISTS `vnframeref_seq`;
DROP TABLE IF EXISTS `vnroleref_seq`;
DROP TABLE IF EXISTS `vnroleselres_seq`;
DROP TABLE IF EXISTS `vnroletype_seq`;
DROP TABLE IF EXISTS `vnselres_seq`;
DROP TABLE IF EXISTS `word_seq`;

-- Helper: ensure a table's id column is AUTO_INCREMENT if it exists and isn't already.
-- MySQL doesn't allow IF outside routines, so use conditional SQL string building.
SET @ensure_ai = '
  SET @needs_ai = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = ?tbl
      AND column_name = ''id''
      AND extra NOT LIKE ''%auto_increment%''
  );
  SET @sql = IF(@needs_ai > 0,
     CONCAT(''ALTER TABLE `'', ?tbl, ''` MODIFY id BIGINT NOT NULL AUTO_INCREMENT''),
     ''SELECT 1'');
  PREPARE s FROM @sql;
  EXECUTE s;
  DEALLOCATE PREPARE s;
';

-- Because MySQL prepared statements can't substitute table names directly in CONCAT
-- with variables inside a stored snippet above, we inline the pattern per table:

-- actors
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'actors'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `actors` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- goals
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'goals'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `goals` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- goal_relations
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'goal_relations'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `goal_relations` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- organizations
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'organizations'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `organizations` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- pods
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'pods'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `pods` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- reports
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'reports'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `reports` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- scenarios
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'scenarios'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `scenarios` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- semcor_file
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'semcor_file'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `semcor_file` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- semcor_sentence
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'semcor_sentence'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `semcor_sentence` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- semcor_sentence_word
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'semcor_sentence_word'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `semcor_sentence_word` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- stakeholders
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'stakeholders'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `stakeholders` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- stories
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'stories'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `stories` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- synset_definition_word
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'synset_definition_word'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `synset_definition_word` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- teams
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'teams'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `teams` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- terms
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'terms'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `terms` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- usecases
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'usecases'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `usecases` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- user_role_permissions
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'user_role_permissions'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `user_role_permissions` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- user_roles
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'user_roles'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `user_roles` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- users
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'users'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `users` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnframeref
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'vnframeref'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `vnframeref` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnroleref
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'vnroleref'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `vnroleref` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnroleselres
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'vnroleselres'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `vnroleselres` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnroletype
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'vnroletype'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `vnroletype` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnselres
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'vnselres'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `vnselres` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- word
SET @needs_ai = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'word'
    AND column_name = 'id'
    AND extra NOT LIKE '%auto_increment%'
);
SET @sql = IF(@needs_ai > 0,
  'ALTER TABLE `word` MODIFY id BIGINT NOT NULL AUTO_INCREMENT',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Restore FK checks
SET FOREIGN_KEY_CHECKS = @orig_fk_checks;

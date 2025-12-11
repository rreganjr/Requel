-- Consolidated cleanup for legacy sequence-based IDs.
-- Drops obsolete *_seq tables and ensures PK columns are AUTO_INCREMENT.
-- Safe to run on fresh schemas (no-op); guarded by information_schema checks.

SET @orig_fk_checks = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- Drop legacy sequence tables (safe if absent)
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

-- Helper: if a column isn't AUTO_INCREMENT, alter it.

-- actors.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'actors' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `actors` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- annotations.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'annotations' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `annotations` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- arguments.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'arguments' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `arguments` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- categorydef.categoryid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'categorydef' AND column_name = 'categoryid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `categorydef` MODIFY `categoryid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- goal_relations.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'goal_relations' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `goal_relations` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- goals.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'goals' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `goals` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- lexlinkref.linkid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'lexlinkref' AND column_name = 'linkid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `lexlinkref` MODIFY `linkid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- linkdef.linkid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'linkdef' AND column_name = 'linkid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `linkdef` MODIFY `linkid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- organizations.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'organizations' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `organizations` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- pods.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'pods' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `pods` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- positions.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'positions' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `positions` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- reports.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'reports' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `reports` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- scenarios.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'scenarios' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `scenarios` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- semcor_file.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'semcor_file' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `semcor_file` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- semcor_sentence.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'semcor_sentence' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `semcor_sentence` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- semcor_sentence_word.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'semcor_sentence_word' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `semcor_sentence_word` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- stakeholder_permissions.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stakeholder_permissions' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `stakeholder_permissions` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- stakeholders.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stakeholders' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `stakeholders` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- stories.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'stories' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `stories` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- synset.synsetid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'synset' AND column_name = 'synsetid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `synset` MODIFY `synsetid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- synset_definition_word.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'synset_definition_word' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `synset_definition_word` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- teams.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'teams' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `teams` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- terms.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'terms' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `terms` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- usecases.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usecases' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `usecases` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- user_role_permissions.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_role_permissions' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `user_role_permissions` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- user_roles.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_roles' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `user_roles` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- users.id
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'id' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `users` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnclass.classid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vnclass' AND column_name = 'classid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `vnclass` MODIFY `classid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnframedef.frameid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vnframedef' AND column_name = 'frameid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `vnframedef` MODIFY `frameid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnframeref.framerefid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vnframeref' AND column_name = 'framerefid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `vnframeref` MODIFY `framerefid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnroleref.rolerefid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vnroleref' AND column_name = 'rolerefid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `vnroleref` MODIFY `rolerefid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnroleselres.roleselresid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vnroleselres' AND column_name = 'roleselresid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `vnroleselres` MODIFY `roleselresid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnroletype.roletypeid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vnroletype' AND column_name = 'roletypeid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `vnroletype` MODIFY `roletypeid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- vnselres.vnselresid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vnselres' AND column_name = 'vnselresid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `vnselres` MODIFY `vnselresid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- word.wordid
SET @needs_ai = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'word' AND column_name = 'wordid' AND extra NOT LIKE '%auto_increment%');
SET @sql = IF(@needs_ai > 0, 'ALTER TABLE `word` MODIFY `wordid` BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------------------------------------------------------------------------
-- Annotation discriminator cleanup (from previous migration), guarded by existence

SET @anno_exists = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'annotation_annotatable'
);

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Project''            WHERE annotatable_type = ''com.rreganjr.requel.project.Project''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''ProjectTeam''        WHERE annotatable_type = ''com.rreganjr.requel.project.ProjectTeam''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Goal''               WHERE annotatable_type = ''com.rreganjr.requel.project.Goal''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''GoalRelation''       WHERE annotatable_type = ''com.rreganjr.requel.project.GoalRelation''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''UseCase''            WHERE annotatable_type = ''com.rreganjr.requel.project.UseCase''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Scenario''           WHERE annotatable_type = ''com.rreganjr.requel.project.Scenario''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Step''               WHERE annotatable_type = ''com.rreganjr.requel.project.Step''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Story''              WHERE annotatable_type = ''com.rreganjr.requel.project.Story''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Actor''              WHERE annotatable_type = ''com.rreganjr.requel.project.Actor''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''GlossaryTerm''       WHERE annotatable_type = ''com.rreganjr.requel.project.GlossaryTerm''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''NonUserStakeholder'' WHERE annotatable_type = ''com.rreganjr.requel.project.NonUserStakeholder''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''UserStakeholder''    WHERE annotatable_type = ''com.rreganjr.requel.project.UserStakeholder''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @ann_exists = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'annotations'
);

SET @sql = IF(@ann_exists > 0, 'UPDATE annotations SET grouping_object_type = ''Project'' WHERE grouping_object_type = ''com.rreganjr.requel.project.Project''', 'SELECT 0');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET FOREIGN_KEY_CHECKS = @orig_fk_checks;

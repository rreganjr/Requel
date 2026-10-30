-- #380: turn a Requel 1.1.0 schema (Hibernate 6 ddl-auto) into exactly V1's. 1.1.0 already has
-- V1's columns, types, indexes and keys; it allocated ids from Hibernate *_seq tables instead of
-- AUTO_INCREMENT. (V2 makes the same change, guarded, which is why V1 looks like 1.1; doing it
-- here lets LegacySchemaUpgrader check the result is V1 before baselining Flyway at 1.) Written
-- from the difference between the recorded shapes (requel-1.1.0.shape vs v1.shape).

SET FOREIGN_KEY_CHECKS = 0;
-- WordNet's categorydef has a row with id 0; without this, adding AUTO_INCREMENT renumbers it.
SET SESSION sql_mode = CONCAT(@@SESSION.sql_mode, ',NO_AUTO_VALUE_ON_ZERO');

-- Ids: AUTO_INCREMENT, as V1 declares them. MySQL starts each at the table's highest id + 1.
ALTER TABLE `actors` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `categorydef` MODIFY `categoryid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `goal_relations` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `goals` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `lexlinkref` MODIFY `linkid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `linkdef` MODIFY `linkid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `organizations` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `pods` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `reports` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `scenarios` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `semcor_file` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `semcor_sentence_word` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `semcor_sentence` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `stakeholder_permissions` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `stakeholders` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `stories` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `synset_definition_word` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `synset` MODIFY `synsetid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `teams` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `terms` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `usecases` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `user_role_permissions` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `user_roles` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `users` MODIFY `id` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `vnclass` MODIFY `classid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `vnframedef` MODIFY `frameid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `vnframeref` MODIFY `framerefid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `vnroleref` MODIFY `rolerefid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `vnroleselres` MODIFY `roleselresid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `vnroletype` MODIFY `roletypeid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `vnselres` MODIFY `vnselresid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `word` MODIFY `wordid` bigint NOT NULL AUTO_INCREMENT;

-- Hibernate's id sequences, no longer used.
DROP TABLE `actors_seq`;
DROP TABLE `goal_relations_seq`;
DROP TABLE `goals_seq`;
DROP TABLE `organizations_seq`;
DROP TABLE `pods_seq`;
DROP TABLE `reports_seq`;
DROP TABLE `scenarios_seq`;
DROP TABLE `semcor_file_seq`;
DROP TABLE `semcor_sentence_seq`;
DROP TABLE `semcor_sentence_word_seq`;
DROP TABLE `stakeholder_permissions_seq`;
DROP TABLE `stakeholders_seq`;
DROP TABLE `stories_seq`;
DROP TABLE `synset_definition_word_seq`;
DROP TABLE `teams_seq`;
DROP TABLE `terms_seq`;
DROP TABLE `usecases_seq`;
DROP TABLE `user_role_permissions_seq`;
DROP TABLE `user_roles_seq`;
DROP TABLE `users_seq`;
DROP TABLE `vnframeref_seq`;
DROP TABLE `vnroleref_seq`;
DROP TABLE `vnroleselres_seq`;
DROP TABLE `vnroletype_seq`;
DROP TABLE `vnselres_seq`;
DROP TABLE `word_seq`;

SET FOREIGN_KEY_CHECKS = 1;

-- #380: turn a Requel 1.0.x schema (Hibernate 5 ddl-auto, then a mysqldump into MySQL 8.4) into
-- exactly V1's (data fix-ups are in upgrade-data.sql). LegacySchemaUpgrader runs this on one connection
-- after checking the schema is an unmodified 1.0.1/1.0.2 one, then checks the result against V1
-- and baselines Flyway at 1. Written from the difference between the recorded shapes
-- (requel-1.0.2.shape vs v1.shape). The tables' character set (1.0.x's are latin1) was converted
-- to V1's utf8mb4 just before this runs.

SET FOREIGN_KEY_CHECKS = 0;
-- WordNet's categorydef has a row with id 0; without this, adding AUTO_INCREMENT renumbers it.
SET SESSION sql_mode = CONCAT(@@SESSION.sql_mode, ',NO_AUTO_VALUE_ON_ZERO');

-- Unique keys: Hibernate 5 named them UK_<hash>, Hibernate 6 (and V1) UK<hash>.
ALTER TABLE `morphdef` RENAME INDEX `UK_5t08hpbkoi02uo9mr567irq6l` TO `UK5t08hpbkoi02uo9mr567irq6l`;
ALTER TABLE `organizations` RENAME INDEX `UK_p9pbw3flq9hkay8hdx3ypsldy` TO `UKp9pbw3flq9hkay8hdx3ypsldy`;
ALTER TABLE `pods_glossary_terms` RENAME INDEX `UK_j54xkakrtq7tq60uquyds04gi` TO `UKj54xkakrtq7tq60uquyds04gi`;
ALTER TABLE `pods_scenarios` RENAME INDEX `UK_lh55nupebwco0pqkqjjvx7786` TO `UKlh55nupebwco0pqkqjjvx7786`;
ALTER TABLE `stakeholders_goals` RENAME INDEX `UK_k2eufeougg9qi1ipm2nvesemk` TO `UKk2eufeougg9qi1ipm2nvesemk`;
ALTER TABLE `users_user_roles` RENAME INDEX `UK_iol7ycdodk96wfv6xsc2m1hmu` TO `UKiol7ycdodk96wfv6xsc2m1hmu`;
ALTER TABLE `users` RENAME INDEX `UK_r43af9ap4edm43mmtq01oddj6` TO `UKr43af9ap4edm43mmtq01oddj6`;
ALTER TABLE `vnroletype` RENAME INDEX `UK_578wkfgqijt8r6xis08c3029x` TO `UK578wkfgqijt8r6xis08c3029x`;
ALTER TABLE `word` RENAME INDEX `UK_1ri15gaw0ns42jjs3bphg377f` TO `UK1ri15gaw0ns42jjs3bphg377f`;

-- Column types as V1 declares them: datetime(6), enums, tinyint, mediumtext, and AUTO_INCREMENT on
-- the dictionary ids.
ALTER TABLE `actors` MODIFY `date_created` datetime(6) DEFAULT NULL;
ALTER TABLE `annotations` MODIFY `date_created` datetime(6) DEFAULT NULL, MODIFY `resolved_date` datetime(6) DEFAULT NULL;
ALTER TABLE `arguments` MODIFY `date_created` datetime(6) DEFAULT NULL, MODIFY `support_level` tinyint DEFAULT NULL;
ALTER TABLE `categorydef` MODIFY `categoryid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `goal_relations` MODIFY `date_created` datetime(6) DEFAULT NULL, MODIFY `relation_type` enum('Conflicts','Supports') DEFAULT NULL;
ALTER TABLE `goals` MODIFY `date_created` datetime(6) DEFAULT NULL;
ALTER TABLE `lexlinkref` MODIFY `linkid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `linkdef` MODIFY `linkid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `pods` MODIFY `date_created` datetime(6) DEFAULT NULL;
ALTER TABLE `positions` MODIFY `date_created` datetime(6) DEFAULT NULL;
ALTER TABLE `reports` MODIFY `date_created` datetime(6) DEFAULT NULL;
ALTER TABLE `scenarios` MODIFY `date_created` datetime(6) DEFAULT NULL, MODIFY `scenario_type` enum('Alternative','Exception','Optional','PreCondition','Primary') DEFAULT NULL;
ALTER TABLE `semcor_sentence_word` MODIFY `parse_tag` enum('ADJP','ADVP','CC','CD','CONJP','DT','EX','FRAG','FW','IN','INTJ','JJ','JJR','JJS','LS','LST','MD','NAC','NN','NNP','NNPS','NNS','NP','NX','PDT','POS','PP','PRN','PRP','PRP$','PRT','PUNC_DOLLAR','PUNC_DQUOTE','PUNC_NON_TERMINATOR','PUNC_SQUOTE','PUNC_TERMINATOR','QP','RB','RBR','RBS','ROOT','RP','RRC','S','SBAR','SBARQ','SINV','SQ','SYM','TO','UCP','UH','VB','VBD','VBG','VBN','VBP','VBZ','VP','WDT','WHADJP','WHAVP','WHNP','WHPP','WP','WP$','WRB','X') DEFAULT NULL;
ALTER TABLE `stakeholder_permissions` MODIFY `permission_type` enum('Delete','Edit','Grant') NOT NULL;
ALTER TABLE `stakeholders` MODIFY `date_created` datetime(6) DEFAULT NULL;
ALTER TABLE `stories` MODIFY `date_created` datetime(6) DEFAULT NULL, MODIFY `story_type` enum('Exception','Success') NOT NULL;
ALTER TABLE `synset` MODIFY `synsetid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `synset_definition_word` MODIFY `parse_tag` enum('ADJP','ADVP','CC','CD','CONJP','DT','EX','FRAG','FW','IN','INTJ','JJ','JJR','JJS','LS','LST','MD','NAC','NN','NNP','NNPS','NNS','NP','NX','PDT','POS','PP','PRN','PRP','PRP$','PRT','PUNC_DOLLAR','PUNC_DQUOTE','PUNC_NON_TERMINATOR','PUNC_SQUOTE','PUNC_TERMINATOR','QP','RB','RBR','RBS','ROOT','RP','RRC','S','SBAR','SBARQ','SINV','SQ','SYM','TO','UCP','UH','VB','VBD','VBG','VBN','VBP','VBZ','VP','WDT','WHADJP','WHAVP','WHNP','WHPP','WP','WP$','WRB','X') DEFAULT NULL;
ALTER TABLE `teams` MODIFY `date_created` datetime(6) DEFAULT NULL;
ALTER TABLE `terms` MODIFY `date_created` datetime(6) DEFAULT NULL;
ALTER TABLE `usecases` MODIFY `date_created` datetime(6) DEFAULT NULL;
ALTER TABLE `vnclass` MODIFY `classid` bigint NOT NULL AUTO_INCREMENT;
ALTER TABLE `vnframedef` MODIFY `frameid` bigint NOT NULL AUTO_INCREMENT, MODIFY `semantics` mediumtext NOT NULL, MODIFY `syntax` mediumtext NOT NULL;
ALTER TABLE `vnroletype` MODIFY `semrole` enum('ACTOR','ACTOR1','ACTOR2','AGENT','ASSET','ATTRIBUTE','BENEFICIARY','CAUSE','DESTINATION','EXPERIENCER','EXTENT','INSTRUMENT','LOCATION','MATERIAL','OBLIQUE','PARTICIPANT','PATIENT','PATIENT1','PATIENT2','PREDICATE','PRODUCT','PROPOSITION','RECIPIENT','SOURCE','STIMULUS','THEME','THEME1','THEME2','TIME','TOPIC','VALUE','VERB') NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

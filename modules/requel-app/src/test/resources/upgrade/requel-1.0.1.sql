-- #380 fixture: a Requel 1.0.1 database (schema, the sample project, no dictionary rows).
-- Made by scripts/legacy-fixtures.sh; regenerate rather than edit.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `actor_actorcontainers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `actor_actorcontainers` (
  `actor_id` bigint NOT NULL,
  `actorcontainer_type` varchar(255) NOT NULL,
  `actorcontainer_id` bigint NOT NULL,
  PRIMARY KEY (`actor_id`,`actorcontainer_type`,`actorcontainer_id`),
  CONSTRAINT `FKnedqyrnyr6ulf3pleqig3gsl6` FOREIGN KEY (`actor_id`) REFERENCES `actors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `actor_goals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `actor_goals` (
  `actor_id` bigint NOT NULL,
  `goal_id` bigint NOT NULL,
  PRIMARY KEY (`actor_id`,`goal_id`),
  KEY `FK8uc1eqjjss2e45w4mt9n1wv52` (`goal_id`),
  CONSTRAINT `FK8uc1eqjjss2e45w4mt9n1wv52` FOREIGN KEY (`goal_id`) REFERENCES `goals` (`id`),
  CONSTRAINT `FKowiyk0rkouxbni54rwqmmu53h` FOREIGN KEY (`actor_id`) REFERENCES `actors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `actors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `actors` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `version` int NOT NULL,
  `text` longtext,
  `name` varchar(255) NOT NULL,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3kjol8m7om5c4y4athe6mugsf` (`projectordomain_id`,`name`),
  KEY `FK68vxilcm6mw9hvpt6lxksvkdk` (`created_by_id`),
  CONSTRAINT `FK68vxilcm6mw9hvpt6lxksvkdk` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKoh54b4cs7m6pyrk3vc42sr26a` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `actors_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `actors_annotations` (
  `actor_impl_id` bigint NOT NULL,
  `annotations_id` bigint NOT NULL,
  PRIMARY KEY (`actor_impl_id`,`annotations_id`),
  KEY `FK2b7mx0crj79lr9ghddettpghq` (`annotations_id`),
  CONSTRAINT `FK2b7mx0crj79lr9ghddettpghq` FOREIGN KEY (`annotations_id`) REFERENCES `annotations` (`id`),
  CONSTRAINT `FKof7381yahebqmmg8b11pamu1a` FOREIGN KEY (`actor_impl_id`) REFERENCES `actors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `actors_glossary_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `actors_glossary_terms` (
  `actor_impl_id` bigint NOT NULL,
  `glossary_terms_id` bigint NOT NULL,
  PRIMARY KEY (`actor_impl_id`,`glossary_terms_id`),
  KEY `FKnmm524y8ydi8qix4ra44tu6yq` (`glossary_terms_id`),
  CONSTRAINT `FKnmm524y8ydi8qix4ra44tu6yq` FOREIGN KEY (`glossary_terms_id`) REFERENCES `terms` (`id`),
  CONSTRAINT `FKtdl6b6jttrtaycgjbjdokvdil` FOREIGN KEY (`actor_impl_id`) REFERENCES `actors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `annotation_annotatable`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `annotation_annotatable` (
  `annotation_id` bigint NOT NULL,
  `annotatable_type` varchar(255) NOT NULL,
  `annotatable_id` bigint NOT NULL,
  PRIMARY KEY (`annotation_id`,`annotatable_type`,`annotatable_id`),
  CONSTRAINT `FK2fqsqyp0urekxwc9a9gfrifct` FOREIGN KEY (`annotation_id`) REFERENCES `annotations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `annotations` (
  `annotation_type` varchar(255) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `grouping_object_type` varchar(255) DEFAULT NULL,
  `grouping_object_id` bigint NOT NULL,
  `text` longtext,
  `version` int NOT NULL,
  `must_be_resolved` bit(1) DEFAULT NULL,
  `resolved_date` datetime DEFAULT NULL,
  `annotatable_entity_property_name` varchar(255) DEFAULT NULL,
  `word` varchar(255) DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  `resolved_by_position_id` bigint DEFAULT NULL,
  `resolved_by_user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8lyno4fc9cutq19wg0rlxub2d` (`created_by_id`),
  KEY `FK47c1dnqn7ncvd20ilqotgiw9w` (`resolved_by_position_id`),
  KEY `FKbk8lcpsg64sbwosen4dgbhunc` (`resolved_by_user_id`),
  CONSTRAINT `FK47c1dnqn7ncvd20ilqotgiw9w` FOREIGN KEY (`resolved_by_position_id`) REFERENCES `positions` (`id`),
  CONSTRAINT `FK8lyno4fc9cutq19wg0rlxub2d` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKbk8lcpsg64sbwosen4dgbhunc` FOREIGN KEY (`resolved_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `arguments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `arguments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `support_level` int DEFAULT NULL,
  `text` varchar(255) DEFAULT NULL,
  `version` int NOT NULL,
  `created_by_id` bigint NOT NULL,
  `position_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKns6tce30pmr9ied2ug1nd4u00` (`created_by_id`),
  KEY `FKi1tev5vfub48gv4cpdg48xucy` (`position_id`),
  CONSTRAINT `FKi1tev5vfub48gv4cpdg48xucy` FOREIGN KEY (`position_id`) REFERENCES `positions` (`id`),
  CONSTRAINT `FKns6tce30pmr9ied2ug1nd4u00` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `categorydef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categorydef` (
  `categoryid` bigint NOT NULL,
  `name` varchar(32) DEFAULT NULL,
  `pos` varchar(2) DEFAULT NULL,
  PRIMARY KEY (`categoryid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `goal_relations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_relations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `relation_type` varchar(255) DEFAULT NULL,
  `version` int NOT NULL,
  `created_by_id` bigint NOT NULL,
  `from_goal_internal_id` bigint NOT NULL,
  `to_goal_internal_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjgrexy23trfif16eb36uck7ka` (`to_goal_internal_id`,`from_goal_internal_id`),
  KEY `FKjg5vjma06wlo3yceaffa8vd00` (`created_by_id`),
  KEY `FK7oruv04l011xwmyr1rhxmriq0` (`from_goal_internal_id`),
  CONSTRAINT `FK7oruv04l011xwmyr1rhxmriq0` FOREIGN KEY (`from_goal_internal_id`) REFERENCES `goals` (`id`),
  CONSTRAINT `FKfp6djjl2adxia4i9yjxstq2ad` FOREIGN KEY (`to_goal_internal_id`) REFERENCES `goals` (`id`),
  CONSTRAINT `FKjg5vjma06wlo3yceaffa8vd00` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `goal_relations_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_relations_annotations` (
  `goal_relation_impl_id` bigint NOT NULL,
  `annotations_id` bigint NOT NULL,
  PRIMARY KEY (`goal_relation_impl_id`,`annotations_id`),
  KEY `FKicld2fw5kg0ycm4lce7vx57m9` (`annotations_id`),
  CONSTRAINT `FKhov82o4sgmpcfa698892jvjqr` FOREIGN KEY (`goal_relation_impl_id`) REFERENCES `goal_relations` (`id`),
  CONSTRAINT `FKicld2fw5kg0ycm4lce7vx57m9` FOREIGN KEY (`annotations_id`) REFERENCES `annotations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `goals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goals` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `version` int NOT NULL,
  `text` longtext,
  `name` varchar(255) NOT NULL,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5ox2qiahr0igckg3nwxs2t7ic` (`projectordomain_id`,`name`),
  KEY `FKcduce4wkup21oro6nstw808uk` (`created_by_id`),
  CONSTRAINT `FKcduce4wkup21oro6nstw808uk` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKdyf9rqcegn3wd2ix24juogryp` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `goals_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goals_annotations` (
  `goal_impl_id` bigint NOT NULL,
  `annotations_id` bigint NOT NULL,
  PRIMARY KEY (`goal_impl_id`,`annotations_id`),
  KEY `FK42vfwb3vovl1gc1vven0op61h` (`annotations_id`),
  CONSTRAINT `FK42vfwb3vovl1gc1vven0op61h` FOREIGN KEY (`annotations_id`) REFERENCES `annotations` (`id`),
  CONSTRAINT `FK6h4g9443aedwj7wcgw6pjc9q1` FOREIGN KEY (`goal_impl_id`) REFERENCES `goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `goals_glossary_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goals_glossary_terms` (
  `goal_impl_id` bigint NOT NULL,
  `glossary_terms_id` bigint NOT NULL,
  PRIMARY KEY (`goal_impl_id`,`glossary_terms_id`),
  KEY `FKr17q6jrhjyrwcs275jfofaxco` (`glossary_terms_id`),
  CONSTRAINT `FKhtupq1yd6qlqku7dh42ua5aad` FOREIGN KEY (`goal_impl_id`) REFERENCES `goals` (`id`),
  CONSTRAINT `FKr17q6jrhjyrwcs275jfofaxco` FOREIGN KEY (`glossary_terms_id`) REFERENCES `terms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `goals_goalcontainers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goals_goalcontainers` (
  `goal_id` bigint NOT NULL,
  `goalcontainer_type` varchar(255) NOT NULL,
  `goalcontainer_id` bigint NOT NULL,
  PRIMARY KEY (`goal_id`,`goalcontainer_type`,`goalcontainer_id`),
  CONSTRAINT `FKmtm3ovfmp8cr54yw46ejf6ktq` FOREIGN KEY (`goal_id`) REFERENCES `goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `lexlinkref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lexlinkref` (
  `linkid` bigint NOT NULL,
  `synset1id` bigint NOT NULL,
  `synset2id` bigint NOT NULL,
  `word1id` bigint NOT NULL,
  `word2id` bigint NOT NULL,
  PRIMARY KEY (`linkid`,`synset1id`,`synset2id`,`word1id`,`word2id`),
  KEY `FKl4hno4ef5x3bxgh10dvxxn7jp` (`synset1id`),
  KEY `FK2srb4u7vegn4fehnnio6ca6wo` (`word1id`),
  KEY `FKj2629bn1jb9c7ijnfkm8imfsl` (`synset2id`),
  KEY `FK1rwu4bo35cxqf0iij5x7nm627` (`word2id`),
  CONSTRAINT `FK1rwu4bo35cxqf0iij5x7nm627` FOREIGN KEY (`word2id`) REFERENCES `word` (`wordid`),
  CONSTRAINT `FK2srb4u7vegn4fehnnio6ca6wo` FOREIGN KEY (`word1id`) REFERENCES `word` (`wordid`),
  CONSTRAINT `FKj2629bn1jb9c7ijnfkm8imfsl` FOREIGN KEY (`synset2id`) REFERENCES `synset` (`synsetid`),
  CONSTRAINT `FKl4hno4ef5x3bxgh10dvxxn7jp` FOREIGN KEY (`synset1id`) REFERENCES `synset` (`synsetid`),
  CONSTRAINT `FKndn6qepnwyq0svhrvbmfnr93s` FOREIGN KEY (`linkid`) REFERENCES `linkdef` (`linkid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `linkdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `linkdef` (
  `linkid` bigint NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `recurses` char(1) NOT NULL,
  PRIMARY KEY (`linkid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `morphdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `morphdef` (
  `morphid` int NOT NULL,
  `lemma` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`morphid`),
  UNIQUE KEY `UK_5t08hpbkoi02uo9mr567irq6l` (`lemma`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `morphref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `morphref` (
  `morphid` int NOT NULL,
  `pos` varchar(2) NOT NULL,
  `wordid` int NOT NULL,
  PRIMARY KEY (`morphid`,`pos`,`wordid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `organizations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organizations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `version` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_p9pbw3flq9hkay8hdx3ypsldy` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pods` (
  `type` varchar(255) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `text` longtext,
  `version` int NOT NULL,
  `status` varchar(255) DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `organization_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1ceepqgv9l6vddl7i0rwmbt1u` (`type`,`name`),
  KEY `FKbagfkkop1ydex4sat7p6l1xcd` (`created_by_id`),
  KEY `FK4cm66vkux1m8tow542o6rmihy` (`organization_id`),
  CONSTRAINT `FK4cm66vkux1m8tow542o6rmihy` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`),
  CONSTRAINT `FKbagfkkop1ydex4sat7p6l1xcd` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pods_glossary_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pods_glossary_terms` (
  `abstract_project_or_domain_id` bigint NOT NULL,
  `glossary_terms_id` bigint NOT NULL,
  PRIMARY KEY (`abstract_project_or_domain_id`,`glossary_terms_id`),
  UNIQUE KEY `UK_j54xkakrtq7tq60uquyds04gi` (`glossary_terms_id`),
  CONSTRAINT `FKkkp7ng3katt9x7woa21jpy0yw` FOREIGN KEY (`glossary_terms_id`) REFERENCES `terms` (`id`),
  CONSTRAINT `FKlye5funcs8w3gr6g3ad7p2bur` FOREIGN KEY (`abstract_project_or_domain_id`) REFERENCES `pods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pods_scenarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pods_scenarios` (
  `abstract_project_or_domain_id` bigint NOT NULL,
  `scenarios_id` bigint NOT NULL,
  PRIMARY KEY (`abstract_project_or_domain_id`,`scenarios_id`),
  UNIQUE KEY `UK_lh55nupebwco0pqkqjjvx7786` (`scenarios_id`),
  CONSTRAINT `FKa0syiqlptvhp8736banstk9ts` FOREIGN KEY (`abstract_project_or_domain_id`) REFERENCES `pods` (`id`),
  CONSTRAINT `FKn8xfht833jryogwufjgge3lib` FOREIGN KEY (`scenarios_id`) REFERENCES `scenarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `position_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `position_issue` (
  `position_id` bigint NOT NULL,
  `issue_id` bigint NOT NULL,
  PRIMARY KEY (`position_id`,`issue_id`),
  KEY `FK474e0xvjcvrcxaa020jp58koa` (`issue_id`),
  CONSTRAINT `FK474e0xvjcvrcxaa020jp58koa` FOREIGN KEY (`issue_id`) REFERENCES `annotations` (`id`),
  CONSTRAINT `FKaswkbehkpg0qm4voni48q5820` FOREIGN KEY (`position_id`) REFERENCES `positions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `positions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `positions` (
  `position_type` varchar(255) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `text` varchar(255) DEFAULT NULL,
  `version` int NOT NULL,
  `proposed_word` varchar(255) DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKns7yikt55e472ljvqsf8apn1l` (`created_by_id`),
  CONSTRAINT `FKns7yikt55e472ljvqsf8apn1l` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `project_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_annotations` (
  `project_impl_id` bigint NOT NULL,
  `annotations_id` bigint NOT NULL,
  PRIMARY KEY (`project_impl_id`,`annotations_id`),
  KEY `FK9tvt5t2jl8y89lff8x7v3i23d` (`annotations_id`),
  CONSTRAINT `FK9tvt5t2jl8y89lff8x7v3i23d` FOREIGN KEY (`annotations_id`) REFERENCES `annotations` (`id`),
  CONSTRAINT `FKgt8bbqd1j2krwq8sddhax72r8` FOREIGN KEY (`project_impl_id`) REFERENCES `pods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `version` int NOT NULL,
  `text` longtext,
  `name` varchar(255) NOT NULL,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKbm3uqq47ka5ikafkutlw32qxr` (`projectordomain_id`,`name`),
  KEY `FKcmj205aq8aa4oe7frdhftqi4v` (`created_by_id`),
  CONSTRAINT `FKbeksn8eljqiijkbjkdisqgmej` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`),
  CONSTRAINT `FKcmj205aq8aa4oe7frdhftqi4v` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `reports_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports_annotations` (
  `report_generator_impl_id` bigint NOT NULL,
  `annotations_id` bigint NOT NULL,
  PRIMARY KEY (`report_generator_impl_id`,`annotations_id`),
  KEY `FKh3pvew2h47usdr56ae12g7fir` (`annotations_id`),
  CONSTRAINT `FK3njp56q34wg2fp4lvs5gk9mh5` FOREIGN KEY (`report_generator_impl_id`) REFERENCES `reports` (`id`),
  CONSTRAINT `FKh3pvew2h47usdr56ae12g7fir` FOREIGN KEY (`annotations_id`) REFERENCES `annotations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `reports_glossary_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports_glossary_terms` (
  `report_generator_impl_id` bigint NOT NULL,
  `glossary_terms_id` bigint NOT NULL,
  PRIMARY KEY (`report_generator_impl_id`,`glossary_terms_id`),
  KEY `FKj6xatps05nnex3193oqleogtb` (`glossary_terms_id`),
  CONSTRAINT `FKj6xatps05nnex3193oqleogtb` FOREIGN KEY (`glossary_terms_id`) REFERENCES `terms` (`id`),
  CONSTRAINT `FKkky467f8jyhy63jdaphmouc9h` FOREIGN KEY (`report_generator_impl_id`) REFERENCES `reports` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `scenario_steps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenario_steps` (
  `scenario_id` bigint NOT NULL,
  `step_id` bigint NOT NULL,
  `step_index` int NOT NULL,
  PRIMARY KEY (`scenario_id`,`step_index`),
  KEY `FK27d69h3qech859hw09yl8gjuj` (`step_id`),
  CONSTRAINT `FK27d69h3qech859hw09yl8gjuj` FOREIGN KEY (`step_id`) REFERENCES `scenarios` (`id`),
  CONSTRAINT `FKridaeul7o6x2dvq4hepd4idlx` FOREIGN KEY (`scenario_id`) REFERENCES `scenarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `scenarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenarios` (
  `type` varchar(255) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `version` int NOT NULL,
  `text` longtext,
  `name` varchar(255) NOT NULL,
  `scenario_type` varchar(255) DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKphgdfddctt1c221pi5ix9w9xn` (`projectordomain_id`,`name`),
  KEY `FKtd16saic0mqbcs86jkatolnpj` (`created_by_id`),
  CONSTRAINT `FKoplmulkyjqc4foqfjtcwjxm2x` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`),
  CONSTRAINT `FKtd16saic0mqbcs86jkatolnpj` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `scenarios_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenarios_annotations` (
  `step_impl_id` bigint NOT NULL,
  `annotations_id` bigint NOT NULL,
  PRIMARY KEY (`step_impl_id`,`annotations_id`),
  KEY `FKlow7ur8ge37xkrj4kosiwjf8e` (`annotations_id`),
  CONSTRAINT `FKeur4hfbppbcnouo6418wbfulp` FOREIGN KEY (`step_impl_id`) REFERENCES `scenarios` (`id`),
  CONSTRAINT `FKlow7ur8ge37xkrj4kosiwjf8e` FOREIGN KEY (`annotations_id`) REFERENCES `annotations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `scenarios_glossary_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenarios_glossary_terms` (
  `step_impl_id` bigint NOT NULL,
  `glossary_terms_id` bigint NOT NULL,
  PRIMARY KEY (`step_impl_id`,`glossary_terms_id`),
  KEY `FKmnfjfhy6542e0ybeskv5xvunb` (`glossary_terms_id`),
  CONSTRAINT `FKi460xfw8a69hs7ocypjuns5ew` FOREIGN KEY (`step_impl_id`) REFERENCES `scenarios` (`id`),
  CONSTRAINT `FKmnfjfhy6542e0ybeskv5xvunb` FOREIGN KEY (`glossary_terms_id`) REFERENCES `terms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `semcor_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semcor_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file` varchar(255) DEFAULT NULL,
  `section` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `semcor_sentence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semcor_sentence` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `snum` bigint DEFAULT NULL,
  `file_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKb77sj8vsvh7y6t5ynmysay3sx` (`file_id`),
  CONSTRAINT `FKb77sj8vsvh7y6t5ynmysay3sx` FOREIGN KEY (`file_id`) REFERENCES `semcor_file` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `semcor_sentence_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semcor_sentence_word` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word_index` int DEFAULT NULL,
  `parse_tag` varchar(255) DEFAULT NULL,
  `text` varchar(255) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `sense_id` bigint DEFAULT NULL,
  `word_id` bigint DEFAULT NULL,
  `sentence_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_ssw_word` (`word_id`),
  KEY `index_ssw_synset` (`sense_id`),
  KEY `FKaic7k57by1uw7ll9u8ofsgts8` (`category_id`),
  KEY `FKkhi5rkqquwk62xg5tbbovn4fi` (`sense_id`,`word_id`),
  KEY `FK5x1sw8k3s9p066kh332c153kf` (`sentence_id`),
  CONSTRAINT `FK5x1sw8k3s9p066kh332c153kf` FOREIGN KEY (`sentence_id`) REFERENCES `semcor_sentence` (`id`),
  CONSTRAINT `FKaic7k57by1uw7ll9u8ofsgts8` FOREIGN KEY (`category_id`) REFERENCES `categorydef` (`categoryid`),
  CONSTRAINT `FKkhi5rkqquwk62xg5tbbovn4fi` FOREIGN KEY (`sense_id`, `word_id`) REFERENCES `sense` (`synsetid`, `wordid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `semlinkref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semlinkref` (
  `distance` int NOT NULL,
  `linkid` bigint NOT NULL,
  `synset1id` bigint NOT NULL,
  `synset2id` bigint NOT NULL,
  PRIMARY KEY (`distance`,`linkid`,`synset1id`,`synset2id`),
  KEY `FK5uwn6vfgdv3jxe5mtiv5ilmdd` (`synset1id`),
  KEY `FKlirs5de5dv6lhv49mktlamyqo` (`linkid`),
  KEY `FK6ujnlddugs2sqwt1njkbn7nop` (`synset2id`),
  CONSTRAINT `FK5uwn6vfgdv3jxe5mtiv5ilmdd` FOREIGN KEY (`synset1id`) REFERENCES `synset` (`synsetid`),
  CONSTRAINT `FK6ujnlddugs2sqwt1njkbn7nop` FOREIGN KEY (`synset2id`) REFERENCES `synset` (`synsetid`),
  CONSTRAINT `FKlirs5de5dv6lhv49mktlamyqo` FOREIGN KEY (`linkid`) REFERENCES `linkdef` (`linkid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `sense`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sense` (
  `synsetid` bigint NOT NULL,
  `wordid` bigint NOT NULL,
  `rank` int NOT NULL,
  `freq` int DEFAULT NULL,
  `sense_key` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`synsetid`,`wordid`),
  KEY `FK7wa5exip9bb80w4ek9iawapp1` (`wordid`),
  CONSTRAINT `FK7wa5exip9bb80w4ek9iawapp1` FOREIGN KEY (`wordid`) REFERENCES `word` (`wordid`),
  CONSTRAINT `FKi5dgfgetsmr11vx0r7ok5t212` FOREIGN KEY (`synsetid`) REFERENCES `synset` (`synsetid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stakeholder_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stakeholder_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `entity_type` varchar(255) NOT NULL,
  `permission_type` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKn2011ixwswp04atw4etb0fspb` (`entity_type`,`permission_type`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stakeholders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stakeholders` (
  `stakeholder_type` varchar(255) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `version` int NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `text` longtext,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `team_internal_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjchdokpxj5rvi95jvre7otj7i` (`projectordomain_id`,`name`,`user_id`),
  KEY `FKtviqp2hx1fny86reatjkw0h3` (`created_by_id`),
  KEY `FKrejfnrbqvyoaay923k2omp7lp` (`user_id`),
  KEY `FK3mwdtlbwqrj9x2vo3e0gjy8rk` (`team_internal_id`),
  CONSTRAINT `FK3mwdtlbwqrj9x2vo3e0gjy8rk` FOREIGN KEY (`team_internal_id`) REFERENCES `teams` (`id`),
  CONSTRAINT `FKoia047k3m9q31h0f7byd7c07l` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`),
  CONSTRAINT `FKrejfnrbqvyoaay923k2omp7lp` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKtviqp2hx1fny86reatjkw0h3` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stakeholders_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stakeholders_annotations` (
  `abstract_stakeholder_id` bigint NOT NULL,
  `annotations_id` bigint NOT NULL,
  PRIMARY KEY (`abstract_stakeholder_id`,`annotations_id`),
  KEY `FKbxstx3wcu7o3ka1nss5i4bm3f` (`annotations_id`),
  CONSTRAINT `FKa9blmqpquyskfh08ekmy2q3uq` FOREIGN KEY (`abstract_stakeholder_id`) REFERENCES `stakeholders` (`id`),
  CONSTRAINT `FKbxstx3wcu7o3ka1nss5i4bm3f` FOREIGN KEY (`annotations_id`) REFERENCES `annotations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stakeholders_glossary_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stakeholders_glossary_terms` (
  `abstract_stakeholder_id` bigint NOT NULL,
  `glossary_terms_id` bigint NOT NULL,
  PRIMARY KEY (`abstract_stakeholder_id`,`glossary_terms_id`),
  KEY `FKmt31ivvuv75tcumaky22vh5e` (`glossary_terms_id`),
  CONSTRAINT `FK1oqvjljt105im9podllxeo6j` FOREIGN KEY (`abstract_stakeholder_id`) REFERENCES `stakeholders` (`id`),
  CONSTRAINT `FKmt31ivvuv75tcumaky22vh5e` FOREIGN KEY (`glossary_terms_id`) REFERENCES `terms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stakeholders_goals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stakeholders_goals` (
  `abstract_stakeholder_id` bigint NOT NULL,
  `goals_id` bigint NOT NULL,
  PRIMARY KEY (`abstract_stakeholder_id`,`goals_id`),
  UNIQUE KEY `UK_k2eufeougg9qi1ipm2nvesemk` (`goals_id`),
  CONSTRAINT `FKkgv2u3ibvxjc61d4xam2ss9ww` FOREIGN KEY (`abstract_stakeholder_id`) REFERENCES `stakeholders` (`id`),
  CONSTRAINT `FKod1o4d27iru8jqjwivkw86mda` FOREIGN KEY (`goals_id`) REFERENCES `goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stakeholders_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stakeholders_permissions` (
  `stakeholder_id` bigint NOT NULL,
  `stakeholder_permission_id` bigint NOT NULL,
  PRIMARY KEY (`stakeholder_id`,`stakeholder_permission_id`),
  KEY `FKlekedvh4pqm5yott8y07p9avc` (`stakeholder_permission_id`),
  CONSTRAINT `FKhuac7i93xw0fkq1q4shllujpa` FOREIGN KEY (`stakeholder_id`) REFERENCES `stakeholders` (`id`),
  CONSTRAINT `FKlekedvh4pqm5yott8y07p9avc` FOREIGN KEY (`stakeholder_permission_id`) REFERENCES `stakeholder_permissions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `version` int NOT NULL,
  `text` longtext,
  `name` varchar(255) NOT NULL,
  `story_type` varchar(255) NOT NULL,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKa3xtdh1wq2y6w3t1ntdgyn79u` (`projectordomain_id`,`name`),
  KEY `FKcmqxed8auy56vw8wab7dkxgyu` (`created_by_id`),
  CONSTRAINT `FK7w91ak547kj9rkhamf28tyh0u` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`),
  CONSTRAINT `FKcmqxed8auy56vw8wab7dkxgyu` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stories_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stories_annotations` (
  `story_impl_id` bigint NOT NULL,
  `annotations_id` bigint NOT NULL,
  PRIMARY KEY (`story_impl_id`,`annotations_id`),
  KEY `FKm2msfyywek0extu1lgojmcjnh` (`annotations_id`),
  CONSTRAINT `FKm2msfyywek0extu1lgojmcjnh` FOREIGN KEY (`annotations_id`) REFERENCES `annotations` (`id`),
  CONSTRAINT `FKn1v73x8g1yp874tt1o4sx0vsk` FOREIGN KEY (`story_impl_id`) REFERENCES `stories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stories_glossary_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stories_glossary_terms` (
  `story_impl_id` bigint NOT NULL,
  `glossary_terms_id` bigint NOT NULL,
  PRIMARY KEY (`story_impl_id`,`glossary_terms_id`),
  KEY `FK9dwqxqk01r6cd7r6dsj57eu1l` (`glossary_terms_id`),
  CONSTRAINT `FK9dwqxqk01r6cd7r6dsj57eu1l` FOREIGN KEY (`glossary_terms_id`) REFERENCES `terms` (`id`),
  CONSTRAINT `FKq1wbnqibg4v1hsk72hnaa0241` FOREIGN KEY (`story_impl_id`) REFERENCES `stories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `story_actors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `story_actors` (
  `story_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  PRIMARY KEY (`story_id`,`actor_id`),
  KEY `FK7r37s4ti02wp64x2u43cl7ah5` (`actor_id`),
  CONSTRAINT `FK7r37s4ti02wp64x2u43cl7ah5` FOREIGN KEY (`actor_id`) REFERENCES `actors` (`id`),
  CONSTRAINT `FK8lv4n23yd277rtugf6e20rjgt` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `story_goals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `story_goals` (
  `story_id` bigint NOT NULL,
  `goal_id` bigint NOT NULL,
  PRIMARY KEY (`story_id`,`goal_id`),
  KEY `FKls0efayihd5kig0s7wdbv5mul` (`goal_id`),
  CONSTRAINT `FK4g5xpb1i0jkjkll4dqa09n9li` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`),
  CONSTRAINT `FKls0efayihd5kig0s7wdbv5mul` FOREIGN KEY (`goal_id`) REFERENCES `goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `story_storycontainers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `story_storycontainers` (
  `story_id` bigint NOT NULL,
  `storycontainer_type` varchar(255) NOT NULL,
  `storycontainer_id` bigint NOT NULL,
  PRIMARY KEY (`story_id`,`storycontainer_type`,`storycontainer_id`),
  CONSTRAINT `FK5tigs42abrfjnq043skxy25dv` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `synset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `synset` (
  `synsetid` bigint NOT NULL,
  `definition` varchar(1000) DEFAULT NULL,
  `pos` varchar(2) DEFAULT NULL,
  `categoryid` bigint NOT NULL,
  PRIMARY KEY (`synsetid`),
  KEY `FKi7mj1cj8sa2tlu73k2c8i91xq` (`categoryid`),
  CONSTRAINT `FKi7mj1cj8sa2tlu73k2c8i91xq` FOREIGN KEY (`categoryid`) REFERENCES `categorydef` (`categoryid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `synset_definition_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `synset_definition_word` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word_index` int DEFAULT NULL,
  `parse_tag` varchar(255) DEFAULT NULL,
  `text` varchar(255) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `sense_id` bigint DEFAULT NULL,
  `word_id` bigint DEFAULT NULL,
  `synset_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_sdw_word` (`word_id`),
  KEY `index_sdw_synset` (`sense_id`),
  KEY `FK245sqtu4t9qur7jdws2qov58b` (`category_id`),
  KEY `FK25v13hxaxnhglxmp4maoe9w0h` (`sense_id`,`word_id`),
  KEY `FKl5yphwf7x3jym14fjdlndcpys` (`synset_id`),
  CONSTRAINT `FK245sqtu4t9qur7jdws2qov58b` FOREIGN KEY (`category_id`) REFERENCES `categorydef` (`categoryid`),
  CONSTRAINT `FK25v13hxaxnhglxmp4maoe9w0h` FOREIGN KEY (`sense_id`, `word_id`) REFERENCES `sense` (`synsetid`, `wordid`),
  CONSTRAINT `FKl5yphwf7x3jym14fjdlndcpys` FOREIGN KEY (`synset_id`) REFERENCES `synset` (`synsetid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `synset_subsumer_counts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `synset_subsumer_counts` (
  `synsetid` bigint NOT NULL,
  `element` int DEFAULT NULL,
  `mapkey_linkid` bigint NOT NULL,
  PRIMARY KEY (`synsetid`,`mapkey_linkid`),
  KEY `FKpcrqhbl3k6ikci24qh3kvwsw2` (`mapkey_linkid`),
  CONSTRAINT `FK4d24nkp14rjxtsfdpb1yqrelp` FOREIGN KEY (`synsetid`) REFERENCES `synset` (`synsetid`),
  CONSTRAINT `FKpcrqhbl3k6ikci24qh3kvwsw2` FOREIGN KEY (`mapkey_linkid`) REFERENCES `linkdef` (`linkid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `team_stakeholders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team_stakeholders` (
  `team_id` bigint NOT NULL,
  `stakeholder_id` bigint NOT NULL,
  PRIMARY KEY (`team_id`,`stakeholder_id`),
  KEY `FKjr8hk4th1rch173xc7j8e9swa` (`stakeholder_id`),
  CONSTRAINT `FKjr8hk4th1rch173xc7j8e9swa` FOREIGN KEY (`stakeholder_id`) REFERENCES `stakeholders` (`id`),
  CONSTRAINT `FKkuie74f976707nbknppq1yijd` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `teams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teams` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `version` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjm062ymiehxltopyrl8g7wfiu` (`projectordomain_id`,`name`),
  KEY `FKcq9jk9qh4ox827y0d161rabce` (`created_by_id`),
  CONSTRAINT `FKcq9jk9qh4ox827y0d161rabce` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKnsujlcjs3hmo4q541g3d3495g` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `teams_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teams_annotations` (
  `project_team_impl_id` bigint NOT NULL,
  `annotations_id` bigint NOT NULL,
  PRIMARY KEY (`project_team_impl_id`,`annotations_id`),
  KEY `FKmvwg6x0mioxytqe0pbw1gsn1l` (`annotations_id`),
  CONSTRAINT `FK10nidn3su1h9w7oe0mafjqy7u` FOREIGN KEY (`project_team_impl_id`) REFERENCES `teams` (`id`),
  CONSTRAINT `FKmvwg6x0mioxytqe0pbw1gsn1l` FOREIGN KEY (`annotations_id`) REFERENCES `annotations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `teams_glossary_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teams_glossary_terms` (
  `project_team_impl_id` bigint NOT NULL,
  `glossary_terms_id` bigint NOT NULL,
  PRIMARY KEY (`project_team_impl_id`,`glossary_terms_id`),
  KEY `FK5hy7rid9uwagir5f8vd5p5avy` (`glossary_terms_id`),
  CONSTRAINT `FK5hy7rid9uwagir5f8vd5p5avy` FOREIGN KEY (`glossary_terms_id`) REFERENCES `terms` (`id`),
  CONSTRAINT `FKn3l04m6q2cb7s21sn0n5nijrq` FOREIGN KEY (`project_team_impl_id`) REFERENCES `teams` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `version` int NOT NULL,
  `text` longtext,
  `name` varchar(255) NOT NULL,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  `canonical_term_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKo41q4l4tjeo7o8sg8kygymtdl` (`projectordomain_id`,`name`),
  KEY `FKrbjypsxicjrpx3xs45jtdyh09` (`created_by_id`),
  KEY `FK57t8pl1m1a7769qmxeor9ixeu` (`canonical_term_id`),
  CONSTRAINT `FK57t8pl1m1a7769qmxeor9ixeu` FOREIGN KEY (`canonical_term_id`) REFERENCES `terms` (`id`),
  CONSTRAINT `FKackr6r6gmpplm6eh1vhwo899r` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`),
  CONSTRAINT `FKrbjypsxicjrpx3xs45jtdyh09` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `terms_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terms_annotations` (
  `glossary_term_impl_id` bigint NOT NULL,
  `annotations_id` bigint NOT NULL,
  PRIMARY KEY (`glossary_term_impl_id`,`annotations_id`),
  KEY `FKbv87q2iqv9bnqt146swqd6vlp` (`annotations_id`),
  CONSTRAINT `FK3f0n979l31w8kudk3vo7t1kkk` FOREIGN KEY (`glossary_term_impl_id`) REFERENCES `terms` (`id`),
  CONSTRAINT `FKbv87q2iqv9bnqt146swqd6vlp` FOREIGN KEY (`annotations_id`) REFERENCES `annotations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `terms_glossary_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terms_glossary_terms` (
  `glossary_term_impl_id` bigint NOT NULL,
  `glossary_terms_id` bigint NOT NULL,
  PRIMARY KEY (`glossary_term_impl_id`,`glossary_terms_id`),
  KEY `FKnm1o291ycgnardd21gd67wwqn` (`glossary_terms_id`),
  CONSTRAINT `FK9v4i6754m57ot4nr16fdik7gi` FOREIGN KEY (`glossary_term_impl_id`) REFERENCES `terms` (`id`),
  CONSTRAINT `FKnm1o291ycgnardd21gd67wwqn` FOREIGN KEY (`glossary_terms_id`) REFERENCES `terms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `terms_referers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terms_referers` (
  `term_id` bigint NOT NULL,
  `referer_type` varchar(255) NOT NULL,
  `referer_id` bigint NOT NULL,
  PRIMARY KEY (`term_id`,`referer_type`,`referer_id`),
  CONSTRAINT `FKfbuq9lgyptoec9elhkd84e8nm` FOREIGN KEY (`term_id`) REFERENCES `terms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `usecase_actors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usecase_actors` (
  `usecase_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  PRIMARY KEY (`usecase_id`,`actor_id`),
  KEY `FKp9w0fdyw8p3dmao2xdjbyy6e3` (`actor_id`),
  CONSTRAINT `FK3fynqhk1h79avjxl6uxmb7ttg` FOREIGN KEY (`usecase_id`) REFERENCES `usecases` (`id`),
  CONSTRAINT `FKp9w0fdyw8p3dmao2xdjbyy6e3` FOREIGN KEY (`actor_id`) REFERENCES `actors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `usecase_goals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usecase_goals` (
  `usecase_id` bigint NOT NULL,
  `goal_id` bigint NOT NULL,
  PRIMARY KEY (`usecase_id`,`goal_id`),
  KEY `FKs2lw29ytsdmw6jh334l271cvi` (`goal_id`),
  CONSTRAINT `FKfoko38k590b75ldxlw9fjalsu` FOREIGN KEY (`usecase_id`) REFERENCES `usecases` (`id`),
  CONSTRAINT `FKs2lw29ytsdmw6jh334l271cvi` FOREIGN KEY (`goal_id`) REFERENCES `goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `usecase_stories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usecase_stories` (
  `usecase_id` bigint NOT NULL,
  `story_id` bigint NOT NULL,
  PRIMARY KEY (`usecase_id`,`story_id`),
  KEY `FK2bdxgmfgysmr20b7om7pq72il` (`story_id`),
  CONSTRAINT `FK2bdxgmfgysmr20b7om7pq72il` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`),
  CONSTRAINT `FKc7p9040nnr4pr53n3sb08pn2a` FOREIGN KEY (`usecase_id`) REFERENCES `usecases` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `usecases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usecases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime DEFAULT NULL,
  `version` int NOT NULL,
  `text` longtext,
  `name` varchar(255) NOT NULL,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  `primary_actor_id` bigint NOT NULL,
  `scenario_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm5xr5atxwv1otu7vy79bbtggb` (`projectordomain_id`,`name`),
  KEY `FK2efsoen834amxmw6wvxgavebd` (`created_by_id`),
  KEY `FK73oxshn310amapwlknjnp928l` (`primary_actor_id`),
  KEY `FKdc6iew2mp2om7hjnd03fn10ct` (`scenario_id`),
  CONSTRAINT `FK2efsoen834amxmw6wvxgavebd` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK73oxshn310amapwlknjnp928l` FOREIGN KEY (`primary_actor_id`) REFERENCES `actors` (`id`),
  CONSTRAINT `FKdc6iew2mp2om7hjnd03fn10ct` FOREIGN KEY (`scenario_id`) REFERENCES `scenarios` (`id`),
  CONSTRAINT `FKe8kepbvh53w1dpsavcnkorwky` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `usecases_annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usecases_annotations` (
  `use_case_impl_id` bigint NOT NULL,
  `annotations_id` bigint NOT NULL,
  PRIMARY KEY (`use_case_impl_id`,`annotations_id`),
  KEY `FK16b0nnivq9o8lcnhve44eojmx` (`annotations_id`),
  CONSTRAINT `FK16b0nnivq9o8lcnhve44eojmx` FOREIGN KEY (`annotations_id`) REFERENCES `annotations` (`id`),
  CONSTRAINT `FK3b8qd067awuf02et4t5ruyb44` FOREIGN KEY (`use_case_impl_id`) REFERENCES `usecases` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `usecases_glossary_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usecases_glossary_terms` (
  `use_case_impl_id` bigint NOT NULL,
  `glossary_terms_id` bigint NOT NULL,
  PRIMARY KEY (`use_case_impl_id`,`glossary_terms_id`),
  KEY `FKcxe9jqb8pgm8n9u8ervqmb26c` (`glossary_terms_id`),
  CONSTRAINT `FK2oykog7nrybg6i4yslp8sdtnr` FOREIGN KEY (`use_case_impl_id`) REFERENCES `usecases` (`id`),
  CONSTRAINT `FKcxe9jqb8pgm8n9u8ervqmb26c` FOREIGN KEY (`glossary_terms_id`) REFERENCES `terms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `role_type` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmnjpg0c4hhycrot1lemlcf4x3` (`name`,`role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `role_type` varchar(255) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `version` int NOT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhfh9dx7w3ubf1co1vdev94g3f` (`user_id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_roles_active_projects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles_active_projects` (
  `project_user_role_id` bigint NOT NULL,
  `active_projects_id` bigint NOT NULL,
  PRIMARY KEY (`project_user_role_id`,`active_projects_id`),
  KEY `FKe1wg3c97w6ifbqyva0xga8cx6` (`active_projects_id`),
  CONSTRAINT `FKe0padewe66bwllx388hhnmb2h` FOREIGN KEY (`project_user_role_id`) REFERENCES `user_roles` (`id`),
  CONSTRAINT `FKe1wg3c97w6ifbqyva0xga8cx6` FOREIGN KEY (`active_projects_id`) REFERENCES `pods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_roles_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles_permissions` (
  `user_role_id` bigint NOT NULL,
  `user_role_permission_id` bigint NOT NULL,
  PRIMARY KEY (`user_role_id`,`user_role_permission_id`),
  KEY `FKkcmdnphr7j0ovbt9f9kj5q2sr` (`user_role_permission_id`),
  CONSTRAINT `FKbl9quyy34ov1cjoo0rvjer71i` FOREIGN KEY (`user_role_id`) REFERENCES `user_roles` (`id`),
  CONSTRAINT `FKkcmdnphr7j0ovbt9f9kj5q2sr` FOREIGN KEY (`user_role_permission_id`) REFERENCES `user_role_permissions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `editable` bit(1) NOT NULL,
  `email_address` varchar(255) NOT NULL,
  `hashed_password` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  `version` int NOT NULL,
  `organization_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_r43af9ap4edm43mmtq01oddj6` (`username`),
  KEY `FKqpugllwvyv37klq7ft9m8aqxk` (`organization_id`),
  CONSTRAINT `FKqpugllwvyv37klq7ft9m8aqxk` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `users_user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  UNIQUE KEY `UK_iol7ycdodk96wfv6xsc2m1hmu` (`role_id`),
  CONSTRAINT `FKkeoi5kuvb2tl5j7wa87jvlnyq` FOREIGN KEY (`role_id`) REFERENCES `user_roles` (`id`),
  CONSTRAINT `FKkfth240mxf8yd3ukhjmscs62w` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnclass`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnclass` (
  `classid` bigint NOT NULL,
  `class` varchar(32) DEFAULT NULL,
  `parentid` bigint DEFAULT NULL,
  PRIMARY KEY (`classid`),
  KEY `FKdrp95tc7nnq68jiula52fdc87` (`parentid`),
  CONSTRAINT `FKdrp95tc7nnq68jiula52fdc87` FOREIGN KEY (`parentid`) REFERENCES `vnclass` (`classid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnframedef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnframedef` (
  `frameid` bigint NOT NULL,
  `description1` varchar(64) DEFAULT NULL,
  `description2` varchar(64) DEFAULT NULL,
  `number` varchar(16) DEFAULT NULL,
  `semantics` longtext NOT NULL,
  `syntax` longtext NOT NULL,
  `xtag` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`frameid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnframeref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnframeref` (
  `framerefid` bigint NOT NULL AUTO_INCREMENT,
  `frameid` bigint NOT NULL,
  `synsetid` bigint NOT NULL,
  `wordid` bigint NOT NULL,
  `classid` bigint NOT NULL,
  PRIMARY KEY (`framerefid`),
  KEY `FKg2c8i8aka9bfnl08q8u9ko7tw` (`frameid`),
  KEY `FKkhxdot0eh2l3f0nq9emj0j5vw` (`synsetid`,`wordid`),
  KEY `FKed5aakio4fafmve4srguuccfl` (`classid`),
  CONSTRAINT `FKed5aakio4fafmve4srguuccfl` FOREIGN KEY (`classid`) REFERENCES `vnclass` (`classid`),
  CONSTRAINT `FKg2c8i8aka9bfnl08q8u9ko7tw` FOREIGN KEY (`frameid`) REFERENCES `vnframedef` (`frameid`),
  CONSTRAINT `FKkhxdot0eh2l3f0nq9emj0j5vw` FOREIGN KEY (`synsetid`, `wordid`) REFERENCES `sense` (`synsetid`, `wordid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnroleref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnroleref` (
  `rolerefid` bigint NOT NULL AUTO_INCREMENT,
  `selrestrs` varchar(255) NOT NULL,
  `classid` bigint NOT NULL,
  `roletypeid` bigint NOT NULL,
  PRIMARY KEY (`rolerefid`),
  KEY `FK4orv3ls5fnlv2aacg1ocjauv8` (`classid`),
  KEY `FKmyyhld9vhyhlb06a5v0g3qil1` (`roletypeid`),
  CONSTRAINT `FK4orv3ls5fnlv2aacg1ocjauv8` FOREIGN KEY (`classid`) REFERENCES `vnclass` (`classid`),
  CONSTRAINT `FKmyyhld9vhyhlb06a5v0g3qil1` FOREIGN KEY (`roletypeid`) REFERENCES `vnroletype` (`roletypeid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnroleselres`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnroleselres` (
  `roleselresid` bigint NOT NULL AUTO_INCREMENT,
  `include` varchar(255) DEFAULT NULL,
  `rolerefid` bigint NOT NULL,
  `roletypeid` bigint NOT NULL,
  PRIMARY KEY (`roleselresid`),
  KEY `FKa5lmytnqffufrlows0c9cotkh` (`rolerefid`),
  KEY `FKaeshx7ywhc9f4hc8aslxoob04` (`roletypeid`),
  CONSTRAINT `FKa5lmytnqffufrlows0c9cotkh` FOREIGN KEY (`rolerefid`) REFERENCES `vnroleref` (`rolerefid`),
  CONSTRAINT `FKaeshx7ywhc9f4hc8aslxoob04` FOREIGN KEY (`roletypeid`) REFERENCES `vnselres` (`vnselresid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnroletype`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnroletype` (
  `roletypeid` bigint NOT NULL AUTO_INCREMENT,
  `semrole` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  PRIMARY KEY (`roletypeid`),
  UNIQUE KEY `UK_578wkfgqijt8r6xis08c3029x` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnselres`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnselres` (
  `vnselresid` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `linkid` bigint DEFAULT NULL,
  `synsetid` bigint NOT NULL,
  PRIMARY KEY (`vnselresid`),
  KEY `FKhwp3q3biwna2i1np24r2glk6m` (`linkid`),
  KEY `FK5ripniqudusosvvfbx9p8f8nk` (`synsetid`),
  CONSTRAINT `FK5ripniqudusosvvfbx9p8f8nk` FOREIGN KEY (`synsetid`) REFERENCES `synset` (`synsetid`),
  CONSTRAINT `FKhwp3q3biwna2i1np24r2glk6m` FOREIGN KEY (`linkid`) REFERENCES `linkdef` (`linkid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `word` (
  `wordid` bigint NOT NULL AUTO_INCREMENT,
  `lemma` varchar(80) NOT NULL,
  `phonetic_code` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`wordid`),
  UNIQUE KEY `UK_1ri15gaw0ns42jjs3bphg377f` (`lemma`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

LOCK TABLES `actor_actorcontainers` WRITE;
/*!40000 ALTER TABLE `actor_actorcontainers` DISABLE KEYS */;
INSERT INTO `actor_actorcontainers` VALUES (2,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `actor_actorcontainers` VALUES (4,'com.rreganjr.requel.project.UseCase',1);
/*!40000 ALTER TABLE `actor_actorcontainers` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `actor_goals` WRITE;
/*!40000 ALTER TABLE `actor_goals` DISABLE KEYS */;
/*!40000 ALTER TABLE `actor_goals` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `actors` WRITE;
/*!40000 ALTER TABLE `actors` DISABLE KEYS */;
INSERT INTO `actors` VALUES (1,'2009-01-05 00:00:00',24,'The automated assistant is part of the system, but interacts with the requirements part of the system like a project user adding issues and notes during analysis. The automated assistant will be a stakeholder on all projects.','Automated Assistant',1,1);
INSERT INTO `actors` VALUES (2,'2009-01-20 00:00:00',8,'This is a placeholder for an interactive user','Interactive User',1,1);
INSERT INTO `actors` VALUES (3,'2009-01-05 00:00:00',17,'Project users use the system to create Requirements in projects.','Project User',1,1);
INSERT INTO `actors` VALUES (4,'2009-01-05 00:00:00',17,'System administrator users create other users and manage the system level user permissions.','System Admin',1,1);
/*!40000 ALTER TABLE `actors` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `actors_annotations` WRITE;
/*!40000 ALTER TABLE `actors_annotations` DISABLE KEYS */;
INSERT INTO `actors_annotations` VALUES (1,1);
INSERT INTO `actors_annotations` VALUES (3,1);
INSERT INTO `actors_annotations` VALUES (3,15);
INSERT INTO `actors_annotations` VALUES (3,17);
INSERT INTO `actors_annotations` VALUES (4,21);
INSERT INTO `actors_annotations` VALUES (1,42);
INSERT INTO `actors_annotations` VALUES (1,47);
INSERT INTO `actors_annotations` VALUES (1,62);
INSERT INTO `actors_annotations` VALUES (4,66);
INSERT INTO `actors_annotations` VALUES (1,97);
INSERT INTO `actors_annotations` VALUES (4,103);
INSERT INTO `actors_annotations` VALUES (1,107);
INSERT INTO `actors_annotations` VALUES (1,108);
INSERT INTO `actors_annotations` VALUES (1,109);
INSERT INTO `actors_annotations` VALUES (1,110);
INSERT INTO `actors_annotations` VALUES (1,111);
INSERT INTO `actors_annotations` VALUES (1,112);
INSERT INTO `actors_annotations` VALUES (2,112);
INSERT INTO `actors_annotations` VALUES (3,112);
INSERT INTO `actors_annotations` VALUES (4,112);
INSERT INTO `actors_annotations` VALUES (1,113);
INSERT INTO `actors_annotations` VALUES (1,114);
INSERT INTO `actors_annotations` VALUES (1,115);
INSERT INTO `actors_annotations` VALUES (2,116);
INSERT INTO `actors_annotations` VALUES (2,117);
INSERT INTO `actors_annotations` VALUES (2,118);
INSERT INTO `actors_annotations` VALUES (2,119);
INSERT INTO `actors_annotations` VALUES (2,120);
INSERT INTO `actors_annotations` VALUES (3,121);
INSERT INTO `actors_annotations` VALUES (3,122);
INSERT INTO `actors_annotations` VALUES (3,123);
INSERT INTO `actors_annotations` VALUES (3,124);
INSERT INTO `actors_annotations` VALUES (3,125);
INSERT INTO `actors_annotations` VALUES (4,126);
INSERT INTO `actors_annotations` VALUES (4,127);
INSERT INTO `actors_annotations` VALUES (4,128);
INSERT INTO `actors_annotations` VALUES (4,129);
INSERT INTO `actors_annotations` VALUES (4,130);
/*!40000 ALTER TABLE `actors_annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `actors_glossary_terms` WRITE;
/*!40000 ALTER TABLE `actors_glossary_terms` DISABLE KEYS */;
/*!40000 ALTER TABLE `actors_glossary_terms` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `annotation_annotatable` WRITE;
/*!40000 ALTER TABLE `annotation_annotatable` DISABLE KEYS */;
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Actor',3);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Goal',6);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (1,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `annotation_annotatable` VALUES (2,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (3,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (4,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (5,'com.rreganjr.requel.project.UseCase',1);
INSERT INTO `annotation_annotatable` VALUES (5,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `annotation_annotatable` VALUES (6,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (7,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (8,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (9,'com.rreganjr.requel.project.Goal',6);
INSERT INTO `annotation_annotatable` VALUES (10,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (11,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (12,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (13,'com.rreganjr.requel.project.Story',1);
INSERT INTO `annotation_annotatable` VALUES (14,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (15,'com.rreganjr.requel.project.Actor',3);
INSERT INTO `annotation_annotatable` VALUES (16,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (17,'com.rreganjr.requel.project.Actor',3);
INSERT INTO `annotation_annotatable` VALUES (17,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (17,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (17,'com.rreganjr.requel.project.Story',1);
INSERT INTO `annotation_annotatable` VALUES (18,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (18,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (19,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (20,'com.rreganjr.requel.project.Goal',9);
INSERT INTO `annotation_annotatable` VALUES (21,'com.rreganjr.requel.project.Actor',4);
INSERT INTO `annotation_annotatable` VALUES (22,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (22,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (23,'com.rreganjr.requel.project.Goal',10);
INSERT INTO `annotation_annotatable` VALUES (24,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (24,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (25,'com.rreganjr.requel.project.Goal',10);
INSERT INTO `annotation_annotatable` VALUES (26,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (27,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (28,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (29,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (30,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (30,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (31,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (32,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (33,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (33,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (33,'com.rreganjr.requel.project.UseCase',1);
INSERT INTO `annotation_annotatable` VALUES (34,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (35,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (36,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (37,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (38,'com.rreganjr.requel.project.Goal',9);
INSERT INTO `annotation_annotatable` VALUES (39,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (40,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (41,'com.rreganjr.requel.project.Goal',6);
INSERT INTO `annotation_annotatable` VALUES (42,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (42,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (42,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (43,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (44,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (45,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (45,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (45,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (46,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (46,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (47,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (48,'com.rreganjr.requel.project.Goal',6);
INSERT INTO `annotation_annotatable` VALUES (49,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (50,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (51,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (52,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (52,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (53,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (54,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (55,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (56,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (57,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (58,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (59,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (60,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (61,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (62,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (63,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (64,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (65,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (65,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (66,'com.rreganjr.requel.project.Actor',4);
INSERT INTO `annotation_annotatable` VALUES (66,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (66,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (67,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (68,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (69,'com.rreganjr.requel.project.Goal',9);
INSERT INTO `annotation_annotatable` VALUES (70,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (71,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (72,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (73,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (74,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (75,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (76,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (76,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (77,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (78,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (79,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (80,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (81,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (82,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (83,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (84,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (84,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (84,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (85,'com.rreganjr.requel.project.Step',2);
INSERT INTO `annotation_annotatable` VALUES (86,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (87,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (87,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (88,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (89,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (90,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (91,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (92,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (93,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (93,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (94,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (94,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (95,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (96,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (97,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (98,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (98,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (98,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (98,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (98,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (99,'com.rreganjr.requel.project.Goal',6);
INSERT INTO `annotation_annotatable` VALUES (99,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (100,'com.rreganjr.requel.project.Goal',10);
INSERT INTO `annotation_annotatable` VALUES (101,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (102,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (103,'com.rreganjr.requel.project.Actor',4);
INSERT INTO `annotation_annotatable` VALUES (104,'com.rreganjr.requel.project.Goal',10);
INSERT INTO `annotation_annotatable` VALUES (105,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (106,'com.rreganjr.requel.project.Goal',9);
INSERT INTO `annotation_annotatable` VALUES (107,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (108,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (109,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (110,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (111,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Actor',2);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Actor',3);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Actor',4);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Goal',6);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Goal',9);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Goal',10);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Scenario',1);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Step',2);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.Story',1);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.UseCase',1);
INSERT INTO `annotation_annotatable` VALUES (112,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `annotation_annotatable` VALUES (113,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (114,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (115,'com.rreganjr.requel.project.Actor',1);
INSERT INTO `annotation_annotatable` VALUES (116,'com.rreganjr.requel.project.Actor',2);
INSERT INTO `annotation_annotatable` VALUES (117,'com.rreganjr.requel.project.Actor',2);
INSERT INTO `annotation_annotatable` VALUES (118,'com.rreganjr.requel.project.Actor',2);
INSERT INTO `annotation_annotatable` VALUES (119,'com.rreganjr.requel.project.Actor',2);
INSERT INTO `annotation_annotatable` VALUES (120,'com.rreganjr.requel.project.Actor',2);
INSERT INTO `annotation_annotatable` VALUES (121,'com.rreganjr.requel.project.Actor',3);
INSERT INTO `annotation_annotatable` VALUES (122,'com.rreganjr.requel.project.Actor',3);
INSERT INTO `annotation_annotatable` VALUES (123,'com.rreganjr.requel.project.Actor',3);
INSERT INTO `annotation_annotatable` VALUES (124,'com.rreganjr.requel.project.Actor',3);
INSERT INTO `annotation_annotatable` VALUES (125,'com.rreganjr.requel.project.Actor',3);
INSERT INTO `annotation_annotatable` VALUES (126,'com.rreganjr.requel.project.Actor',4);
INSERT INTO `annotation_annotatable` VALUES (127,'com.rreganjr.requel.project.Actor',4);
INSERT INTO `annotation_annotatable` VALUES (128,'com.rreganjr.requel.project.Actor',4);
INSERT INTO `annotation_annotatable` VALUES (129,'com.rreganjr.requel.project.Actor',4);
INSERT INTO `annotation_annotatable` VALUES (130,'com.rreganjr.requel.project.Actor',4);
INSERT INTO `annotation_annotatable` VALUES (131,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (132,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (133,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (134,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (135,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (136,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (137,'com.rreganjr.requel.project.Goal',1);
INSERT INTO `annotation_annotatable` VALUES (138,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (139,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (140,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (141,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (142,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (143,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (144,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (145,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (146,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (147,'com.rreganjr.requel.project.Goal',2);
INSERT INTO `annotation_annotatable` VALUES (148,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (149,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (150,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (151,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (152,'com.rreganjr.requel.project.Goal',3);
INSERT INTO `annotation_annotatable` VALUES (153,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (154,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (155,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (156,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (157,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (158,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (159,'com.rreganjr.requel.project.Goal',4);
INSERT INTO `annotation_annotatable` VALUES (160,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (161,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (162,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (163,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (164,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (165,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (166,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (167,'com.rreganjr.requel.project.Goal',5);
INSERT INTO `annotation_annotatable` VALUES (168,'com.rreganjr.requel.project.Goal',6);
INSERT INTO `annotation_annotatable` VALUES (169,'com.rreganjr.requel.project.Goal',6);
INSERT INTO `annotation_annotatable` VALUES (170,'com.rreganjr.requel.project.Goal',6);
INSERT INTO `annotation_annotatable` VALUES (171,'com.rreganjr.requel.project.Goal',6);
INSERT INTO `annotation_annotatable` VALUES (172,'com.rreganjr.requel.project.Goal',6);
INSERT INTO `annotation_annotatable` VALUES (173,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (174,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (175,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (176,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (177,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (178,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (179,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (180,'com.rreganjr.requel.project.Goal',7);
INSERT INTO `annotation_annotatable` VALUES (181,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (182,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (183,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (184,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (185,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (186,'com.rreganjr.requel.project.Goal',8);
INSERT INTO `annotation_annotatable` VALUES (187,'com.rreganjr.requel.project.Goal',9);
INSERT INTO `annotation_annotatable` VALUES (188,'com.rreganjr.requel.project.Goal',9);
INSERT INTO `annotation_annotatable` VALUES (189,'com.rreganjr.requel.project.Goal',9);
INSERT INTO `annotation_annotatable` VALUES (190,'com.rreganjr.requel.project.Goal',9);
INSERT INTO `annotation_annotatable` VALUES (191,'com.rreganjr.requel.project.Goal',9);
INSERT INTO `annotation_annotatable` VALUES (192,'com.rreganjr.requel.project.Goal',9);
INSERT INTO `annotation_annotatable` VALUES (193,'com.rreganjr.requel.project.Goal',10);
INSERT INTO `annotation_annotatable` VALUES (194,'com.rreganjr.requel.project.Goal',10);
INSERT INTO `annotation_annotatable` VALUES (195,'com.rreganjr.requel.project.Goal',10);
INSERT INTO `annotation_annotatable` VALUES (196,'com.rreganjr.requel.project.Goal',10);
INSERT INTO `annotation_annotatable` VALUES (197,'com.rreganjr.requel.project.Goal',10);
INSERT INTO `annotation_annotatable` VALUES (198,'com.rreganjr.requel.project.Goal',10);
INSERT INTO `annotation_annotatable` VALUES (199,'com.rreganjr.requel.project.Story',1);
INSERT INTO `annotation_annotatable` VALUES (200,'com.rreganjr.requel.project.Story',1);
INSERT INTO `annotation_annotatable` VALUES (201,'com.rreganjr.requel.project.Step',2);
INSERT INTO `annotation_annotatable` VALUES (201,'com.rreganjr.requel.project.Story',1);
INSERT INTO `annotation_annotatable` VALUES (201,'com.rreganjr.requel.project.UseCase',1);
INSERT INTO `annotation_annotatable` VALUES (201,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `annotation_annotatable` VALUES (202,'com.rreganjr.requel.project.Step',2);
INSERT INTO `annotation_annotatable` VALUES (202,'com.rreganjr.requel.project.Story',1);
INSERT INTO `annotation_annotatable` VALUES (202,'com.rreganjr.requel.project.UseCase',1);
INSERT INTO `annotation_annotatable` VALUES (202,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `annotation_annotatable` VALUES (203,'com.rreganjr.requel.project.Story',1);
INSERT INTO `annotation_annotatable` VALUES (204,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (205,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (206,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (207,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (208,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (209,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (210,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (211,'com.rreganjr.requel.project.Story',2);
INSERT INTO `annotation_annotatable` VALUES (212,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (213,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (214,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (215,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (216,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (217,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (218,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (219,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (220,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (221,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (222,'com.rreganjr.requel.project.Story',3);
INSERT INTO `annotation_annotatable` VALUES (223,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (224,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (225,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (226,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (227,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (228,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (229,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (230,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (231,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (232,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (233,'com.rreganjr.requel.project.Story',4);
INSERT INTO `annotation_annotatable` VALUES (234,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (235,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (236,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (237,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (238,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (239,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (240,'com.rreganjr.requel.project.Story',5);
INSERT INTO `annotation_annotatable` VALUES (241,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (242,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (243,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (244,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (245,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (246,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (247,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (248,'com.rreganjr.requel.project.Story',6);
INSERT INTO `annotation_annotatable` VALUES (249,'com.rreganjr.requel.project.Scenario',1);
INSERT INTO `annotation_annotatable` VALUES (249,'com.rreganjr.requel.project.Step',2);
INSERT INTO `annotation_annotatable` VALUES (250,'com.rreganjr.requel.project.Step',2);
INSERT INTO `annotation_annotatable` VALUES (251,'com.rreganjr.requel.project.Step',2);
INSERT INTO `annotation_annotatable` VALUES (252,'com.rreganjr.requel.project.UseCase',1);
INSERT INTO `annotation_annotatable` VALUES (253,'com.rreganjr.requel.project.UseCase',1);
INSERT INTO `annotation_annotatable` VALUES (254,'com.rreganjr.requel.project.UseCase',1);
INSERT INTO `annotation_annotatable` VALUES (255,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `annotation_annotatable` VALUES (256,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `annotation_annotatable` VALUES (257,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `annotation_annotatable` VALUES (258,'com.rreganjr.requel.project.UseCase',2);
/*!40000 ALTER TABLE `annotation_annotatable` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `annotations` WRITE;
/*!40000 ALTER TABLE `annotations` DISABLE KEYS */;
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',1,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"The system\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'The system',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',2,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"\" Bailey Pet Supply \"\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'\" Bailey Pet Supply \"',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',3,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"\" Virile Mobile \"\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'\" Virile Mobile \"',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',4,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"A team\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'A team',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',5,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"A user\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'A user',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',6,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Automatted Assistance\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Automatted Assistance',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',7,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Collaborative Elicitation\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Collaborative Elicitation',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',8,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Dave the system administrator\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Dave the system administrator',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',9,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Flexible Process and Deliverables\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Flexible Process and Deliverables',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',10,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Improved Understanding\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Improved Understanding',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',11,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Mobile Media Portal\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Mobile Media Portal',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',12,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Notification\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Notification',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',13,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Poor Communication\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Poor Communication',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',14,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Project Changes\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Project Changes',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',15,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Project users\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Project users',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',16,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Purchase Order System\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Purchase Order System',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',17,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Requirements\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Requirements',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',18,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Rich\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Rich',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',19,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Ron\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Ron',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',20,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Secure data access\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Secure data access',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',21,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"System administrator users\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'System administrator users',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',22,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"The user\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'The user',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',23,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"The work\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'The work',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',24,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Theresa\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Theresa',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',25,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"Work\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'Work',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',26,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a change\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a change',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',27,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a distinct username and password\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a distinct username and password',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',28,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a help message\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a help message',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',29,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a link\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a link',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',30,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a list\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a list',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',31,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a message\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a message',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',32,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a new account\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a new account',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',33,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a new project\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a new project',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',34,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a novice user\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a novice user',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',35,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a password\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a password',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',36,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a periodic summary\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a periodic summary',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',37,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a problem connecting\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a problem connecting',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',38,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a project or non-users\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a project or non-users',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',39,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a project\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a project',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',40,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a short description\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a short description',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',41,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a specific requirements elicitation process\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a specific requirements elicitation process',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',42,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a stakeholder\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a stakeholder',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',43,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a username and password\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a username and password',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',44,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a username\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a username',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',45,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"a web browser\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'a web browser',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',46,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"all project entities\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'all project entities',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',47,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"all projects\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'all projects',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',48,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"all the features\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'all the features',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',49,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"all the stakeholders\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'all the stakeholders',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',50,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"an account\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'an account',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',51,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"an email message\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'an email message',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',52,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"an existing user session\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'an existing user session',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',53,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"an overview\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'an overview',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',54,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"any missing information\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'any missing information',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',55,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"each issue\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'each issue',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',56,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"each solution\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'each solution',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',57,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"email address\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'email address',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',58,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"emails and spreadsheets\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'emails and spreadsheets',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',59,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"every time\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'every time',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',60,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"existing customers\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'existing customers',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',61,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"integrated discussion\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'integrated discussion',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',62,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"issues and notes\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'issues and notes',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',63,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"natural language\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'natural language',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',64,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"non-integrated tools\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'non-integrated tools',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',65,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"non-technical users\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'non-technical users',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',66,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"other users\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'other users',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',67,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"password combination\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'password combination',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',68,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"phone number\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'phone number',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',69,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"project content\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'project content',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',70,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"pros and cons\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'pros and cons',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',71,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"recent project changes\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'recent project changes',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',72,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"specialized skills or training\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'specialized skills or training',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',73,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"specialized skills\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'specialized skills',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',74,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"state multiple solutions\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'state multiple solutions',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',75,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the \" Create Project \" option\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the \" Create Project \" option',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',76,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the Requel system address\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the Requel system address',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',77,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the Requel system\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the Requel system',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',78,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the customer\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the customer',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',79,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the description\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the description',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',80,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the elements\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the elements',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',81,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the help option\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the help option',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',82,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the information\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the information',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',83,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the list\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the list',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',84,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the login screen\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the login screen',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',85,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the members\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the members',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',86,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the need\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the need',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',87,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the new project interface\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the new project interface',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',88,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the new user setup interface\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the new user setup interface',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',89,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the notifications\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the notifications',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',90,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the novice user interface\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the novice user interface',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',91,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the personal information Dave\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the personal information Dave',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',92,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the project elements\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the project elements',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',93,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the project name\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the project name',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',94,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the project navigation interface\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the project navigation interface',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',95,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the project\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the project',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',96,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the project\'s entities\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the project\'s entities',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',97,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the requirements part\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the requirements part',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',98,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the requirements\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the requirements',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',99,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the stakeholders\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the stakeholders',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',100,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the student\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the student',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',101,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the system administrator\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the system administrator',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',102,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the system interface\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the system interface',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',103,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the system level user permissions\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the system level user permissions',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',104,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the thesis\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the thesis',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',105,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"the username\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'the username',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',106,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The phrase \"unauthorized access\" is a potential glossary term, actor, or domain object/property',1,_binary '','2026-10-30 14:10:55',NULL,'unauthorized access',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',107,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"be\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','be',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',108,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"interacts\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','interacts',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',109,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"part\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','part',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',110,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP (NNP Automated#1) (NNP Assistant#1)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',111,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (S\n                (NP (DT The) (VBN automated#1) (NN assistant#1))\n                (VP\n                (VP (VBZ is)\n                (NP\n                (NP (NN part#1))\n                (PP (IN of)\n                (NP (DT the) (NN system#1))))) (, ,) (CC but)\n                (VP (VBZ interacts#1)\n                (PP (IN with)\n                (NP\n                (NP (DT the) (NNS requirements#1) (NN part#1))\n                (PP (IN of)\n                (NP\n                (NP (DT the) (NN system#1))\n                (PP (IN like)\n                (NP (DT a) (NN project#2) (NN user#1)))))))\n                (S\n                (VP (VBG adding#1)\n                (NP (NNS issues#1) (CC and) (NNS notes#1))\n                (PP (IN during)\n                (NP (NN analysis#1)))))))) (. .)\n                (S\n                (NP (DT The) (VBN automated#1) (NN assistant#1))\n                (VP (MD will)\n                (VP (VB be#1)\n                (NP\n                (NP (DT a) (NN stakeholder#1))\n                (PP (IN on)\n                (NP (DT all) (NNS projects#2))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',112,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',113,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                interacts[AGENT]The automated assistant\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',114,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Automated-0 is the compound modifier of Assistant-1\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',115,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] The-0 is the determiner of assistant-2\n                [dep] automated-1 is the adjectival modifier of assistant-2\n                [dep] assistant-2 is the nominal subject of part-4\n                [dep] assistant-2 is the nominal subject of interacts-10\n                [dep] is-3 is the copula of part-4\n                [dep] of-5 is the case marker of system-7\n                [dep] the-6 is the determiner of system-7\n                [dep] system-7 is the nominal modifier of part-4\n                [dep] but-9 is the coordination of interacts-10\n                [dep] interacts-10 is the conjunct of part-4\n                [dep] with-11 is the case marker of part-14\n                [dep] the-12 is the determiner of part-14\n                [dep] requirements-13 is the compound modifier of part-14\n                [dep] of-15 is the case marker of system-17\n                [dep] the-16 is the determiner of system-17\n                [dep] system-17 is the nominal modifier of part-14\n                [dep] like-18 is the case marker of user-21\n                [dep] a-19 is the determiner of user-21\n                [dep] project-20 is the compound modifier of user-21\n                [dep] user-21 is the nominal modifier of system-17\n                [dep] adding-22 is the xclausal complement of interacts-10\n                [dep] issues-23 is the object of adding-22\n                [dep] and-24 is the coordination of notes-25\n                [dep] notes-25 is the object of adding-22\n                [dep] notes-25 is the conjunct of issues-23\n                [dep] during-26 is the case marker of analysis-27\n                [dep] The-29 is the determiner of assistant-31\n                [dep] automated-30 is the adjectival modifier of assistant-31\n                [dep] assistant-31 is the nominal subject of stakeholder-35\n                [dep] will-32 is the auxiliary of stakeholder-35\n                [dep] be-33 is the copula of stakeholder-35\n                [dep] a-34 is the determiner of stakeholder-35\n                [dep] stakeholder-35 is the parataxis of part-4\n                [dep] on-36 is the case marker of projects-38\n                [dep] all-37 is the determiner of projects-38\n                [dep] projects-38 is the nominal modifier of stakeholder-35\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',116,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP (JJ Interactive#1) (NN User#1)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',117,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (DT This))\n                (VP (VBZ is)\n                (NP\n                (VP (DT a) (VB placeholder))\n                (PP (IN for)\n                (NP (DT an) (JJ interactive#1) (NN user#1)))))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',118,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                placeholder[AGENT]This\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',119,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Interactive-0 is the adjectival modifier of User-1\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',120,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] This-0 is the nominal subject of placeholder-3\n                [dep] is-1 is the copula of placeholder-3\n                [dep] a-2 is the determiner of placeholder-3\n                [dep] for-4 is the case marker of user-7\n                [dep] an-5 is the determiner of user-7\n                [dep] interactive-6 is the adjectival modifier of user-7\n                [dep] user-7 is the nominal modifier of placeholder-3\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',121,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP (NNP Project#1) (NN User#1)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',122,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (NNP Project#1) (NNS users#1))\n                (VP (VBP use#1)\n                (NP (DT the) (NN system#1))\n                (S\n                (VP (TO to)\n                (VP (VB create#1)\n                (NP\n                (NP (NNP Requirements#1))\n                (PP (IN in)\n                (NP (NNS projects#1)))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',123,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                use[AGENT]Project users\n                use[VALUE]the system\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',124,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Project-0 is the compound modifier of User-1\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',125,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Project-0 is the compound modifier of users-1\n                [dep] users-1 is the nominal subject of use-2\n                [dep] the-3 is the determiner of system-4\n                [dep] system-4 is the object of use-2\n                [dep] to-5 is the marker of create-6\n                [dep] create-6 is the adverbial clause modifier of use-2\n                [dep] Requirements-7 is the object of create-6\n                [dep] in-8 is the case marker of projects-9\n                [dep] projects-9 is the nominal modifier of Requirements-7\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',126,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP (NNP System#1) (NNP Admin#1)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',127,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (NNP System#1) (NN administrator#1) (NNS users#1))\n                (VP\n                (VP (VBP create#1)\n                (NP (JJ other#1) (NNS users#1))) (CC and)\n                (VP (VBP manage#1)\n                (NP (DT the)\n                (X (NN system#1) (NN level#1)) (NN user#1) (NNS permissions#1)))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',128,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                create[AGENT]System administrator users\n                create[PRODUCT]other users\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',129,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] System-0 is the compound modifier of Admin-1\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',130,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] System-0 is the compound modifier of users-2\n                [dep] administrator-1 is the compound modifier of users-2\n                [dep] users-2 is the nominal subject of create-3\n                [dep] users-2 is the nominal subject of manage-7\n                [dep] other-4 is the adjectival modifier of users-5\n                [dep] users-5 is the object of create-3\n                [dep] and-6 is the coordination of manage-7\n                [dep] manage-7 is the conjunct of create-3\n                [dep] the-8 is the determiner of permissions-12\n                [dep] system-9 is the compound modifier of level-10\n                [dep] level-10 is the compound modifier of permissions-12\n                [dep] user-11 is the compound modifier of permissions-12\n                [dep] permissions-12 is the object of manage-7\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',131,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The text \"The system analyzes the requirements as they are added to a project and makes suggestions by adding issues to the elements.\" in the Text is complex and may be hard to understand.',1,_binary '','2026-10-30 14:10:55','Text',NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',132,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"Automatted\" in the Name is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Name','Automatted',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',133,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP (NNP Automatted#1) (NNP Assistance#1)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',134,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ analyzes#1)\n                (NP (DT the) (NNS requirements#1))\n                (SBAR (IN as)\n                (S\n                (NP (PRP they))\n                (VP\n                (VP (VBP are)\n                (VP (VBN added#1)\n                (PP (IN to)\n                (NP (DT a) (NN project#1))))) (CC and)\n                (VP (VBZ makes#1)\n                (NP (NNS suggestions#1))\n                (PP (IN by)\n                (S\n                (VP (VBG adding#1)\n                (NP (NNS issues#1))\n                (PP (IN to)\n                (NP (DT the) (NNS elements#1))))))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',135,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                makes[AGENT]The system\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',136,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Automatted-0 is the compound modifier of Assistance-1\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',137,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] The-0 is the determiner of system-1\n                [dep] system-1 is the nominal subject of analyzes-2\n                [dep] the-3 is the determiner of requirements-4\n                [dep] requirements-4 is the object of analyzes-2\n                [dep] as-5 is the marker of added-8\n                [dep] they-6 is the nominal subject of makes-13\n                [dep] added-8 is the adverbial clause modifier of analyzes-2\n                [dep] to-9 is the case marker of project-11\n                [dep] a-10 is the determiner of project-11\n                [dep] and-12 is the coordination of makes-13\n                [dep] makes-13 is the adverbial clause modifier of analyzes-2\n                [dep] makes-13 is the conjunct of added-8\n                [dep] suggestions-14 is the object of makes-13\n                [dep] by-15 is the marker of adding-16\n                [dep] adding-16 is the adverbial clause modifier of makes-13\n                [dep] issues-17 is the object of adding-16\n                [dep] to-18 is the case marker of elements-20\n                [dep] the-19 is the determiner of elements-20\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',138,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"create\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','create',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',139,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"created\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','created',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',140,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"creates\" in the Name is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Name','creates',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',141,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"creates\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','creates',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',142,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"stakeholders\" in the Text is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Text','stakeholders',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',143,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP\n                (NP (NNP Collaborative#1) (NNP Elicitation#1))\n                (PP (IN of)\n                (NP (NNP Requirements#1)))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',144,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP\n                (NP (DT A) (NN team#1))\n                (PP (IN of)\n                (NP\n                (UCP (JJ technical#1) (CC and) (NN business#1)) (NNS stakeholders#1))))\n                (VP (MD can)\n                (VP (VB use#1)\n                (NP (DT the) (NN system#1))\n                (S\n                (VP (TO to)\n                (VP (VB create#1)\n                (NP\n                (ADJP (JJ accurate#1) (, ,) (JJ precise#1) (CC and) (JJ unambiguous#1)) (NN system#1) (NNS requirements#1))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',145,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                use[AGENT]A team\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',146,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] A-0 is the determiner of team-1\n                [dep] team-1 is the nominal subject of use-8\n                [dep] of-2 is the case marker of stakeholders-6\n                [dep] technical-3 is the adjectival modifier of stakeholders-6\n                [dep] and-4 is the coordination of business-5\n                [dep] business-5 is the conjunct of technical-3\n                [dep] business-5 is the adjectival modifier of stakeholders-6\n                [dep] stakeholders-6 is the nominal modifier of team-1\n                [dep] can-7 is the auxiliary of use-8\n                [dep] the-9 is the determiner of system-10\n                [dep] system-10 is the object of use-8\n                [dep] to-11 is the marker of create-12\n                [dep] create-12 is the adverbial clause modifier of use-8\n                [dep] accurate-13 is the adjectival modifier of requirements-19\n                [dep] precise-15 is the conjunct of accurate-13\n                [dep] precise-15 is the adjectival modifier of requirements-19\n                [dep] and-16 is the coordination of unambiguous-17\n                [dep] unambiguous-17 is the conjunct of accurate-13\n                [dep] unambiguous-17 is the adjectival modifier of requirements-19\n                [dep] system-18 is the compound modifier of requirements-19\n                [dep] requirements-19 is the object of create-12\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',147,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Collaborative-0 is the compound modifier of Elicitation-1\n                [dep] of-2 is the case marker of Requirements-3\n                [dep] Requirements-3 is the nominal modifier of Elicitation-1\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',148,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP\n                (NP (NN Discussion#1))\n                (PP (IN of)\n                (NP (NNP Requirements#1)))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',149,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ supports#1)\n                (NP\n                (NP (JJ integrated#1) (NN discussion#1))\n                (PP (IN of)\n                (NP (DT the) (NNS requirements#1))))\n                (SBAR (JJ such#1) (IN that)\n                (S\n                (NP (NNS stakeholders#1))\n                (VP (MD can)\n                (VP (VB identify#1)\n                (NP\n                (NP\n                (NP (NNS issues#1))\n                (PP (IN with)\n                (NP (DT the) (NN project#1) (NNS elements#1)))) (, ,)\n                (NP\n                (NP (NN state#1) (JJ multiple#1) (NNS solutions#1))\n                (PP (IN to)\n                (NP (DT each) (NN issue#1)))) (, ,) (CC and)\n                (NP\n                (NP (NNS pros#1) (CC and) (NNS cons#1))\n                (PP (IN of)\n                (NP (DT each) (NN solution#1)))))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',150,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                identify[AGENT]The system\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',151,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] The-0 is the determiner of system-1\n                [dep] system-1 is the nominal subject of supports-2\n                [dep] integrated-3 is the adjectival modifier of discussion-4\n                [dep] discussion-4 is the object of supports-2\n                [dep] of-5 is the case marker of requirements-7\n                [dep] the-6 is the determiner of requirements-7\n                [dep] requirements-7 is the nominal modifier of discussion-4\n                [dep] such-8 is the dependent of identify-12\n                [dep] that-9 is the marker of identify-12\n                [dep] stakeholders-10 is the nominal subject of identify-12\n                [dep] can-11 is the auxiliary of identify-12\n                [dep] identify-12 is the dependent of supports-2\n                [dep] issues-13 is the object of identify-12\n                [dep] with-14 is the case marker of elements-17\n                [dep] the-15 is the determiner of elements-17\n                [dep] project-16 is the compound modifier of elements-17\n                [dep] elements-17 is the nominal modifier of issues-13\n                [dep] state-19 is the compound modifier of solutions-21\n                [dep] multiple-20 is the adjectival modifier of solutions-21\n                [dep] solutions-21 is the object of identify-12\n                [dep] solutions-21 is the conjunct of issues-13\n                [dep] to-22 is the case marker of issue-24\n                [dep] each-23 is the determiner of issue-24\n                [dep] issue-24 is the nominal modifier of solutions-21\n                [dep] and-26 is the coordination of pros-27\n                [dep] pros-27 is the object of identify-12\n                [dep] pros-27 is the conjunct of issues-13\n                [dep] and-28 is the coordination of cons-29\n                [dep] cons-29 is the conjunct of issues-13\n                [dep] cons-29 is the conjunct of pros-27\n                [dep] of-30 is the case marker of solution-32\n                [dep] each-31 is the determiner of solution-32\n                [dep] solution-32 is the nominal modifier of pros-27\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',152,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] of-1 is the case marker of Requirements-2\n                [dep] Requirements-2 is the nominal modifier of Discussion-0\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',153,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The text \"The system is easy to use so that non-technical users can contribute to the requirements without the need for specialized skills or training.\" in the Text is complex and may be hard to understand.',1,_binary '','2026-10-30 14:10:55','Text',NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',154,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"non-technical\" in the Text is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Text','non-technical',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',155,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (FRAG\n                (ADJP (JJ Easy#1)\n                (S\n                (VP (TO to)\n                (VP (VB Use#1)))))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',156,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ is)\n                (ADJP (JJ easy#1)\n                (S\n                (VP (TO to)\n                (VP (VB use#1)\n                (ADVP (RB so#1))\n                (SBAR (IN that)\n                (S\n                (NP (JJ non-technical) (NNS users#1))\n                (VP (MD can)\n                (VP (VB contribute#1)\n                (PP (IN to)\n                (NP (DT the) (NNS requirements#1)))\n                (PP (IN without)\n                (NP\n                (NP (DT the) (NN need#1))\n                (PP (IN for)\n                (NP (JJ specialized#1) (NNS skills#1) (CC or) (NN training#1)))))))))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',157,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                contribute[AGENT]The system\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',158,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] The-0 is the determiner of system-1\n                [dep] system-1 is the nominal subject of easy-3\n                [dep] system-1 is the controlling nominal subject of use-5\n                [dep] is-2 is the copula of easy-3\n                [dep] to-4 is the marker of use-5\n                [dep] use-5 is the xclausal complement of easy-3\n                [dep] so-6 is the adverbial modifier of use-5\n                [dep] that-7 is the marker of contribute-11\n                [dep] non-technical-8 is the adjectival modifier of users-9\n                [dep] users-9 is the nominal subject of contribute-11\n                [dep] can-10 is the auxiliary of contribute-11\n                [dep] contribute-11 is the clausal complement of use-5\n                [dep] to-12 is the case marker of requirements-14\n                [dep] the-13 is the determiner of requirements-14\n                [dep] without-15 is the case marker of need-17\n                [dep] the-16 is the determiner of need-17\n                [dep] for-18 is the case marker of skills-20\n                [dep] specialized-19 is the adjectival modifier of skills-20\n                [dep] skills-20 is the nominal modifier of need-17\n                [dep] or-21 is the coordination of training-22\n                [dep] training-22 is the nominal modifier of need-17\n                [dep] training-22 is the conjunct of skills-20\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',159,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] to-1 is the marker of Use-2\n                [dep] Use-2 is the xclausal complement of Easy-0\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',160,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The text \"The requirements are defined in natural language such that non-technical users do not need specialized skills to understand the requirements.\" in the Text is complex and may be hard to understand.',1,_binary '','2026-10-30 14:10:55','Text',NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',161,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"language\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','language',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',162,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (DT The) (NNS requirements#1))\n                (VP (VBP are)\n                (VP (VBN defined#1)\n                (PP (IN in)\n                (NP (JJ natural#1) (NN language#1)))\n                (SBAR (JJ such#1) (IN that)\n                (S\n                (NP (JJ non-technical) (NNS users#1))\n                (VP (VBP do#1) (RB not#1)\n                (VP (VB need#1)\n                (NP (JJ specialized#1) (NNS skills#1))\n                (S\n                (VP (TO to)\n                (VP (VB understand#1)\n                (NP (DT the) (NNS requirements#1))))))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',163,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (NNS Requirements#1))\n                (VP (VBN defined#1)\n                (PP (IN in)\n                (NP (NNP Natural#1) (NNP Language#1))))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',164,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                defined[AGENT]Requirements\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',165,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                need[AGENT]non-technical users\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',166,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Requirements-0 is the nominal subject of defined-1\n                [dep] in-2 is the case marker of Language-4\n                [dep] Natural-3 is the compound modifier of Language-4\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',167,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] The-0 is the determiner of requirements-1\n                [dep] in-4 is the case marker of language-6\n                [dep] natural-5 is the adjectival modifier of language-6\n                [dep] such-7 is the dependent of need-13\n                [dep] that-8 is the marker of need-13\n                [dep] non-technical-9 is the adjectival modifier of users-10\n                [dep] users-10 is the nominal subject of need-13\n                [dep] do-11 is the auxiliary of need-13\n                [dep] not-12 is the adverbial modifier of need-13\n                [dep] need-13 is the clausal complement of defined-3\n                [dep] specialized-14 is the adjectival modifier of skills-15\n                [dep] skills-15 is the object of need-13\n                [dep] to-16 is the marker of understand-17\n                [dep] understand-17 is the adverbial clause modifier of need-13\n                [dep] the-18 is the determiner of requirements-19\n                [dep] requirements-19 is the object of understand-17\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',168,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP (JJ Flexible#1) (NN Process#1) (CC and) (NN Deliverables#1)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',169,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ does#1) (RB not#1)\n                (VP (VB force#1)\n                (NP (DT a) (JJ specific#1) (NNS requirements#1) (NN elicitation#1) (NN process#1))\n                (PP (IN on)\n                (NP (DT the) (NNS stakeholders#1)))))) (. .)\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ does#1) (RB not#1)\n                (VP (VB require#1)\n                (NP (DT the) (NNS stakeholders#1))\n                (S\n                (VP (TO to)\n                (VP (VB use#1)\n                (NP\n                (NP (PDT all) (DT the) (NNS features#1))\n                (PP (IN of)\n                (NP (DT the) (NN system#1)))))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',170,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                require[AGENT]the stakeholders\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',171,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Flexible-0 is the adjectival modifier of Process-1\n                [dep] and-2 is the coordination of Deliverables-3\n                [dep] Deliverables-3 is the conjunct of Process-1\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',172,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] The-0 is the determiner of system-1\n                [dep] system-1 is the nominal subject of force-4\n                [dep] does-2 is the auxiliary of force-4\n                [dep] not-3 is the adverbial modifier of force-4\n                [dep] a-5 is the determiner of process-9\n                [dep] specific-6 is the adjectival modifier of process-9\n                [dep] requirements-7 is the compound modifier of process-9\n                [dep] elicitation-8 is the compound modifier of process-9\n                [dep] process-9 is the object of force-4\n                [dep] on-10 is the case marker of stakeholders-12\n                [dep] the-11 is the determiner of stakeholders-12\n                [dep] The-14 is the determiner of system-15\n                [dep] system-15 is the nominal subject of require-18\n                [dep] does-16 is the auxiliary of require-18\n                [dep] not-17 is the adverbial modifier of require-18\n                [dep] require-18 is the parataxis of force-4\n                [dep] the-19 is the determiner of stakeholders-20\n                [dep] stakeholders-20 is the object of require-18\n                [dep] stakeholders-20 is the controlling nominal subject of use-22\n                [dep] to-21 is the marker of use-22\n                [dep] use-22 is the xclausal complement of require-18\n                [dep] all-23 is the predeterminer of features-25\n                [dep] the-24 is the determiner of features-25\n                [dep] features-25 is the object of use-22\n                [dep] of-26 is the case marker of system-28\n                [dep] the-27 is the determiner of system-28\n                [dep] system-28 is the nominal modifier of features-25\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',173,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"emails\" in the Text is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Text','emails',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',174,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"improves\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','improves',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',175,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"non-integrated\" in the Text is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Text','non-integrated',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',176,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"tools\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','tools',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',177,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP (NNP Improved#1) (NNP Understanding#1)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',178,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (S\n                (VP (VBG Using#1)\n                (NP (DT the) (NN system#1))))\n                (VP (VBZ improves#1)\n                (NP\n                (NP (NN understanding#1))\n                (PP (IN of)\n                (NP (DT the) (NNS requirements#3))))\n                (PP (IN by)\n                (NP\n                (NP (PDT all) (DT the) (NNS stakeholders#1))\n                (PP (VBN compared#1)\n                (PP (IN to)\n                (NP\n                (NP (JJ non-integrated) (NNS tools#1))\n                (PP (JJ such#1) (IN as)\n                (NP (NNS emails#1) (CC and) (NNS spreadsheets#1))))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',179,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Improved-0 is the compound modifier of Understanding-1\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',180,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Using-0 is the clausal subject of improves-3\n                [dep] the-1 is the determiner of system-2\n                [dep] system-2 is the object of Using-0\n                [dep] understanding-4 is the object of improves-3\n                [dep] of-5 is the case marker of requirements-7\n                [dep] the-6 is the determiner of requirements-7\n                [dep] requirements-7 is the nominal modifier of understanding-4\n                [dep] by-8 is the case marker of stakeholders-11\n                [dep] all-9 is the predeterminer of stakeholders-11\n                [dep] the-10 is the determiner of stakeholders-11\n                [dep] compared-12 is the case marker of tools-15\n                [dep] non-integrated-14 is the adjectival modifier of tools-15\n                [dep] tools-15 is the nominal modifier of stakeholders-11\n                [dep] such-16 is the case marker of emails-18\n                [dep] emails-18 is the nominal modifier of tools-15\n                [dep] and-19 is the coordination of spreadsheets-20\n                [dep] spreadsheets-20 is the nominal modifier of tools-15\n                [dep] spreadsheets-20 is the conjunct of emails-18\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',181,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The text \"The system notifies the stakeholders of changes to the project. The user can customize how the notifications occur: every time a change is made, or a periodic summary.\" in the Text is complex and may be hard to understand.',1,_binary '','2026-10-30 14:10:55','Text',NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',182,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP\n                (NP (NNP Notification#1))\n                (PP (IN of)\n                (NP (NNP Project#1) (NNPS Changes#1)))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',183,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ notifies#1)\n                (NP\n                (NP (DT the) (NNS stakeholders#1))\n                (PP (IN of)\n                (NP (NNS changes#7))))\n                (PP (IN to)\n                (NP (DT the) (NN project#1))))) (. .)\n                (S\n                (NP (DT The) (NN user#1))\n                (VP (MD can)\n                (VP (VB customize#1)\n                (SBAR\n                (X (WRB how))\n                (S\n                (NP (DT the) (NNS notifications#1))\n                (VP (VBP occur#1)\n                (X)\n                (NP\n                (NP\n                (NP (DT every) (NN time#1))\n                (SBAR\n                (S\n                (NP (DT a) (NN change#7))\n                (VP (VBZ is)\n                (VP (VBN made)))))) (, ,) (CC or)\n                (NP (DT a) (JJ periodic#1) (NN summary#1))))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',184,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                occur[THEME]The system\n                occur[AGENT]The user\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',185,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] The-0 is the determiner of system-1\n                [dep] system-1 is the nominal subject of notifies-2\n                [dep] the-3 is the determiner of stakeholders-4\n                [dep] stakeholders-4 is the object of notifies-2\n                [dep] of-5 is the case marker of changes-6\n                [dep] changes-6 is the nominal modifier of stakeholders-4\n                [dep] to-7 is the case marker of project-9\n                [dep] the-8 is the determiner of project-9\n                [dep] The-11 is the determiner of user-12\n                [dep] user-12 is the nominal subject of customize-14\n                [dep] can-13 is the auxiliary of customize-14\n                [dep] customize-14 is the parataxis of notifies-2\n                [dep] how-15 is the adverbial modifier of occur-18\n                [dep] the-16 is the determiner of notifications-17\n                [dep] notifications-17 is the nominal subject of occur-18\n                [dep] occur-18 is the clausal complement of customize-14\n                [dep] every-20 is the determiner of time-21\n                [dep] time-21 is the object of occur-18\n                [dep] a-22 is the determiner of change-23\n                [dep] made-25 is the relative clause modifier of time-21\n                [dep] or-27 is the coordination of summary-30\n                [dep] a-28 is the determiner of summary-30\n                [dep] periodic-29 is the adjectival modifier of summary-30\n                [dep] summary-30 is the object of occur-18\n                [dep] summary-30 is the conjunct of time-21\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',186,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] of-1 is the case marker of Changes-3\n                [dep] Project-2 is the compound modifier of Changes-3\n                [dep] Changes-3 is the nominal modifier of Notification-0\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',187,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"non-users\" in the Text is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Text','non-users',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',188,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP (JJ Secure#1) (NN data#1) (NN access#1)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',189,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ secures#1)\n                (NP (NN project#1) (NN content#1))\n                (PP (IN from)\n                (NP (JJ unauthorized#1) (NN access#1)))\n                (PP (IN by)\n                (NP\n                (NP (NNS users#1))\n                (VP (RB not#1) (VBN assigned#1)\n                (PP (IN to)\n                (NP\n                (NP (DT a) (NN project#1) (CC or) (NNS non-users))\n                (PP (IN of)\n                (NP (DT the) (NN system#1))))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',190,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                secures[AGENT]The system\n                secures[THEME]project content\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',191,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Secure-0 is the adjectival modifier of access-2\n                [dep] data-1 is the compound modifier of access-2\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',192,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] The-0 is the determiner of system-1\n                [dep] system-1 is the nominal subject of secures-2\n                [dep] project-3 is the compound modifier of content-4\n                [dep] content-4 is the object of secures-2\n                [dep] from-5 is the case marker of access-7\n                [dep] unauthorized-6 is the adjectival modifier of access-7\n                [dep] by-8 is the case marker of users-9\n                [dep] not-10 is the adverbial modifier of assigned-11\n                [dep] assigned-11 is the clausal modifier of noun of users-9\n                [dep] to-12 is the case marker of project-14\n                [dep] a-13 is the determiner of project-14\n                [dep] or-15 is the coordination of non-users-16\n                [dep] non-users-16 is the conjunct of project-14\n                [dep] of-17 is the case marker of system-19\n                [dep] the-18 is the determiner of system-19\n                [dep] system-19 is the nominal modifier of project-14\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',193,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"work\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','work',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',194,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP\n                (NP (DT The) (NN work#1))\n                (VP (VBN done)\n                (PP (IN on)\n                (NP (DT the) (NN thesis#1)))))\n                (VP (VBZ is)\n                (ADVP (RB exclusively#1))\n                (VP (VBN done)\n                (PP (IN by)\n                (NP (DT the) (NN student#1))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',195,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (NNP Work#1))\n                (VP (VBZ is)\n                (VP (PRP$ your) (VB own#1)))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',196,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                own[AGENT]Work\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',197,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] The-0 is the determiner of work-1\n                [dep] done-2 is the clausal modifier of noun of work-1\n                [dep] on-3 is the case marker of thesis-5\n                [dep] the-4 is the determiner of thesis-5\n                [dep] exclusively-7 is the adverbial modifier of done-8\n                [dep] by-9 is the case marker of student-11\n                [dep] the-10 is the determiner of student-11\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',198,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Work-0 is the nominal subject of own-3\n                [dep] is-1 is the copula of own-3\n                [dep] your-2 is the possession modifier of own-3\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',199,'2009-01-20 00:00:00','com.rreganjr.requel.project.Project',1,'hello Cleveland',1,_binary '\0','2026-10-30 14:10:55',NULL,NULL,1,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',200,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (NP\n                (NP (NNP Poor#1) (NNP Communication#1))\n                (PP (IN in)\n                (NP (NNP Requirements#1)))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',201,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (X)',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',202,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',203,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Poor-0 is the compound modifier of Communication-1\n                [dep] in-2 is the case marker of Requirements-3\n                [dep] Requirements-3 is the nominal modifier of Communication-1\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',204,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The text \"Rich logs in to the system and it displays a list of his active projects and the option to create a new project. Rich chooses to create a new project and the system displays the new project interface. Rich enters \"Mobile Media Portal\" for the project name and enters a short description. Rich looks over the list of existing customers and doesn\'t see what he\'s looking for and enters \"Virile Mobile\".\n\n                The system creates the new project and adds Rich as a stakeholder with permission to edit all project entities and grant permission to other users to edit entities. The system closes the new project interface and adds the new project in the project navigation interface.\" in the Text is complex and may be hard to understand.',1,_binary '','2026-10-30 14:10:55','Text',NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',205,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"entities\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','entities',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',206,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (NNP Rich#1))\n                (VP (VBZ creates#1)\n                (NP (DT a) (JJ new#1) (NN project#1)))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',207,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (S\n                (NP (NNP Rich#1))\n                (VP (VBZ logs#1)\n                (PRT (RP in#1))\n                (PP (IN to)\n                (NP (DT the) (NN system#1))))) (CC and)\n                (S\n                (NP (PRP it))\n                (VP (VBZ displays#1)\n                (NP\n                (NP\n                (NP (DT a) (NN list#1))\n                (PP (IN of)\n                (NP (PRP$ his) (JJ active#1) (NNS projects#1)))) (CC and)\n                (NP (DT the) (NN option#1)\n                (S\n                (VP (TO to)\n                (VP (VB create#1)\n                (NP (DT a) (JJ new#1) (NN project#1))))))))) (. .)\n                (S\n                (S\n                (NP (NNP Rich#1))\n                (VP (VBZ chooses#1)\n                (S\n                (VP (TO to)\n                (VP (VB create#1)\n                (NP (DT a) (JJ new#1) (NN project#1))))))) (CC and)\n                (S\n                (NP (DT the) (NN system#1))\n                (VP (VBZ displays#1)\n                (NP (DT the) (JJ new#1) (NN project#1) (NN interface#1))))) (. .)\n                (S\n                (NP (NNP Rich#1))\n                (VP\n                (VP (VBZ enters#1)\n                (NP\n                (X)\n                (NP (NNP Mobile#1) (NNP Media#1) (NNP Portal#1))\n                (X)\n                (PP (IN for)\n                (NP (DT the) (NN project#1) (NN name#1))))) (CC and)\n                (VP (VBZ enters#1)\n                (NP (DT a) (JJ short#1) (NN description#1))))) (. .)\n                (S\n                (NP (NNP Rich#1))\n                (VP\n                (VP (VBZ looks#1)\n                (PP (IN over)\n                (NP\n                (NP (DT the) (NN list#1))\n                (PP (IN of)\n                (NP (VBG existing#1) (NNS customers#1)))))) (CC and)\n                (VP (VBZ does#1) (RB n\'t)\n                (VP (VB see#1)\n                (SBAR\n                (WHNP (WP what))\n                (S\n                (NP (PRP he))\n                (VP\n                (VP (VBZ \'s)\n                (VP (VBG looking#1)\n                (PP (IN for)))) (CC and)\n                (VP (VBZ enters#1)\n                (NP\n                (X) (NNP Virile#1) (NNP Mobile#1)\n                (X)))))))))) (. .)\n                (S\n                (NP (DT The) (NN system#1))\n                (VP\n                (VP (VBZ creates#1)\n                (NP (DT the) (JJ new#1) (NN project#1))) (CC and)\n                (VP (VBZ adds#1)\n                (NP (NNP Rich#1))\n                (PP (IN as)\n                (NP\n                (NP (DT a) (NN stakeholder#1))\n                (PP (IN with)\n                (NP (NN permission#1)\n                (S\n                (VP (TO to)\n                (VP\n                (VP (VB edit#1)\n                (NP (DT all) (NN project#1) (NNS entities#1))) (CC and)\n                (VP (VB grant#1)\n                (NP (NN permission#1))\n                (PP (IN to)\n                (NP (JJ other#1) (NNS users#1)))\n                (S\n                (VP (TO to)\n                (VP (VB edit#1)\n                (NP (NNS entities#1)))))))))))))))) (. .)\n                (S\n                (NP (DT The) (NN system#1))\n                (VP\n                (VP (VBZ closes#1)\n                (NP (DT the) (JJ new#1) (NN project#1) (NN interface#1))) (CC and)\n                (VP (VBZ adds#1)\n                (NP (DT the) (JJ new#1) (NN project#1))\n                (PP (IN in)\n                (NP (DT the) (NN project#1) (NN navigation#1) (NN interface#1)))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',208,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                creates[AGENT]Rich\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',209,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                enters[THEME]Rich\n                enters[AGENT]Rich\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',210,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Rich-0 is the nominal subject of creates-1\n                [dep] a-2 is the determiner of project-4\n                [dep] new-3 is the adjectival modifier of project-4\n                [dep] project-4 is the object of creates-1\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',211,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Rich-0 is the nominal subject of logs-1\n                [dep] in-2 is the phrasal verb particle of logs-1\n                [dep] to-3 is the case marker of system-5\n                [dep] the-4 is the determiner of system-5\n                [dep] and-6 is the coordination of displays-8\n                [dep] it-7 is the nominal subject of displays-8\n                [dep] displays-8 is the conjunct of logs-1\n                [dep] a-9 is the determiner of list-10\n                [dep] list-10 is the object of displays-8\n                [dep] of-11 is the case marker of projects-14\n                [dep] his-12 is the possession modifier of projects-14\n                [dep] active-13 is the adjectival modifier of projects-14\n                [dep] projects-14 is the nominal modifier of list-10\n                [dep] and-15 is the coordination of option-17\n                [dep] the-16 is the determiner of option-17\n                [dep] option-17 is the object of displays-8\n                [dep] option-17 is the conjunct of list-10\n                [dep] to-18 is the marker of create-19\n                [dep] create-19 is the clausal modifier of noun of option-17\n                [dep] a-20 is the determiner of project-22\n                [dep] new-21 is the adjectival modifier of project-22\n                [dep] project-22 is the object of create-19\n                [dep] Rich-24 is the nominal subject of chooses-25\n                [dep] Rich-24 is the controlling nominal subject of create-27\n                [dep] chooses-25 is the conjunct of logs-1\n                [dep] to-26 is the marker of create-27\n                [dep] create-27 is the xclausal complement of chooses-25\n                [dep] a-28 is the determiner of project-30\n                [dep] new-29 is the adjectival modifier of project-30\n                [dep] project-30 is the object of create-27\n                [dep] and-31 is the coordination of displays-34\n                [dep] the-32 is the determiner of system-33\n                [dep] system-33 is the nominal subject of displays-34\n                [dep] displays-34 is the conjunct of logs-1\n                [dep] displays-34 is the conjunct of chooses-25\n                [dep] the-35 is the determiner of interface-38\n                [dep] new-36 is the adjectival modifier of interface-38\n                [dep] project-37 is the compound modifier of interface-38\n                [dep] interface-38 is the object of displays-34\n                [dep] Rich-40 is the nominal subject of enters-41\n                [dep] Rich-40 is the nominal subject of enters-52\n                [dep] enters-41 is the conjunct of logs-1\n                [dep] Mobile-43 is the compound modifier of Portal-45\n                [dep] Media-44 is the compound modifier of Portal-45\n                [dep] Portal-45 is the object of enters-41\n                [dep] for-47 is the case marker of name-50\n                [dep] the-48 is the determiner of name-50\n                [dep] project-49 is the compound modifier of name-50\n                [dep] name-50 is the nominal modifier of Portal-45\n                [dep] and-51 is the coordination of enters-52\n                [dep] enters-52 is the conjunct of logs-1\n                [dep] enters-52 is the conjunct of enters-41\n                [dep] a-53 is the determiner of description-55\n                [dep] short-54 is the adjectival modifier of description-55\n                [dep] description-55 is the object of enters-52\n                [dep] Rich-57 is the nominal subject of looks-58\n                [dep] Rich-57 is the nominal subject of see-68\n                [dep] looks-58 is the conjunct of logs-1\n                [dep] over-59 is the case marker of list-61\n                [dep] the-60 is the determiner of list-61\n                [dep] of-62 is the case marker of customers-64\n                [dep] existing-63 is the adjectival modifier of customers-64\n                [dep] customers-64 is the nominal modifier of list-61\n                [dep] and-65 is the coordination of see-68\n                [dep] does-66 is the auxiliary of see-68\n                [dep] n\'t-67 is the adverbial modifier of see-68\n                [dep] see-68 is the conjunct of logs-1\n                [dep] see-68 is the conjunct of looks-58\n                [dep] he-70 is the nominal subject of looking-72\n                [dep] he-70 is the nominal subject of enters-75\n                [dep] \'s-71 is the auxiliary of looking-72\n                [dep] looking-72 is the clausal complement of see-68\n                [dep] for-73 is the case marker of what-69\n                [dep] and-74 is the coordination of enters-75\n                [dep] enters-75 is the clausal complement of see-68\n                [dep] enters-75 is the conjunct of looking-72\n                [dep] Virile-77 is the compound modifier of Mobile-78\n                [dep] Mobile-78 is the object of enters-75\n                [dep] The-81 is the determiner of system-82\n                [dep] system-82 is the nominal subject of creates-83\n                [dep] system-82 is the nominal subject of adds-88\n                [dep] creates-83 is the conjunct of logs-1\n                [dep] the-84 is the determiner of project-86\n                [dep] new-85 is the adjectival modifier of project-86\n                [dep] project-86 is the object of creates-83\n                [dep] and-87 is the coordination of adds-88\n                [dep] adds-88 is the conjunct of logs-1\n                [dep] adds-88 is the conjunct of creates-83\n                [dep] Rich-89 is the object of adds-88\n                [dep] as-90 is the case marker of stakeholder-92\n                [dep] a-91 is the determiner of stakeholder-92\n                [dep] with-93 is the case marker of permission-94\n                [dep] permission-94 is the nominal modifier of stakeholder-92\n                [dep] to-95 is the marker of edit-96\n                [dep] edit-96 is the clausal modifier of noun of permission-94\n                [dep] all-97 is the determiner of entities-99\n                [dep] project-98 is the compound modifier of entities-99\n                [dep] entities-99 is the object of edit-96\n                [dep] and-100 is the coordination of grant-101\n                [dep] grant-101 is the clausal modifier of noun of permission-94\n                [dep] grant-101 is the conjunct of edit-96\n                [dep] permission-102 is the object of grant-101\n                [dep] to-103 is the case marker of users-105\n                [dep] other-104 is the adjectival modifier of users-105\n                [dep] to-106 is the marker of edit-107\n                [dep] edit-107 is the adverbial clause modifier of grant-101\n                [dep] entities-108 is the object of edit-107\n                [dep] The-110 is the determiner of system-111\n                [dep] system-111 is the nominal subject of closes-112\n                [dep] system-111 is the nominal subject of adds-118\n                [dep] closes-112 is the conjunct of logs-1\n                [dep] the-113 is the determiner of interface-116\n                [dep] new-114 is the adjectival modifier of interface-116\n                [dep] project-115 is the compound modifier of interface-116\n                [dep] interface-116 is the object of closes-112\n                [dep] and-117 is the coordination of adds-118\n                [dep] adds-118 is the conjunct of logs-1\n                [dep] adds-118 is the conjunct of closes-112\n                [dep] the-119 is the determiner of project-121\n                [dep] new-120 is the adjectival modifier of project-121\n                [dep] project-121 is the object of adds-118\n                [dep] in-122 is the case marker of interface-126\n                [dep] the-123 is the determiner of interface-126\n                [dep] project-124 is the compound modifier of interface-126\n                [dep] navigation-125 is the compound modifier of interface-126\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',212,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The text \"Rich opens a web browser to the Requel system address. The system doesn’t detect an existing user session and presents the login screen. Rich enters his username and password. The system verifies the username and password combination is correct. The system logs that Rich has successfully logged into the system and shows him a list of his open projects and an overview of recent project changes.\" in the Text is complex and may be hard to understand.',1,_binary '','2026-10-30 14:10:55','Text',NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',213,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"Requel\" in the Text is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Text','Requel',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',214,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"login\" in the Text is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Text','login',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',215,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"n’t\" in the Text is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Text','n’t',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',216,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"username\" in the Text is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Text','username',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',217,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (NNP Rich#1))\n                (VP (VBZ logs#1)\n                (PRT (RP in#1))\n                (PP (IN to)\n                (NP (DT the) (NN system#1))))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',218,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (S\n                (NP (NNP Rich#1))\n                (VP (VBZ opens#1)\n                (NP (DT a) (NN web#1) (NN browser#1))\n                (PP (IN to)\n                (NP (DT the) (NNP Requel#1) (NN system#1) (NN address#1))) (. .)\n                (PRN\n                (S\n                (NP (DT The) (NN system#1))\n                (VP\n                (VP (VBZ does#1) (RB n’t)\n                (VP (VB detect#1)\n                (NP (DT an) (VBG existing#1) (NN user#1) (NN session#1)))) (CC and)\n                (VP (VBZ presents#1)\n                (NP (DT the) (JJ login) (NN screen#1)))))))) (. .)\n                (S\n                (NP (NNP Rich#1))\n                (VP (VBZ enters#1)\n                (NP (PRP$ his) (NN username) (CC and) (NN password#1)))) (, .)\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ verifies#1)\n                (NP (DT the) (NN username)))) (CC and)\n                (S\n                (NP (NN password#1) (NN combination#1))\n                (VP (VBZ is)\n                (ADJP (JJ correct#1)) (. .)\n                (PRN\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ logs#1)\n                (SBAR (IN that)\n                (S\n                (NP (NNP Rich#1))\n                (VP\n                (VP (VBZ has)\n                (ADVP (RB successfully#1))\n                (VP (VBN logged)\n                (PP (IN into)\n                (NP (DT the) (NN system#1))))) (CC and)\n                (VP (VBZ shows#1)\n                (NP (PRP him))\n                (NP\n                (NP\n                (NP (DT a) (NN list#1))\n                (PP (IN of)\n                (NP (PRP$ his) (JJ open#1) (NNS projects#1)))) (CC and)\n                (NP\n                (NP (DT an) (NN overview#1))\n                (PP (IN of)\n                (NP (JJ recent#1) (NN project#1) (NNS changes#1)))))))))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',219,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                logs[AGENT]Rich\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',220,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                logs[AGENT]The system\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',221,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Rich-0 is the nominal subject of logs-1\n                [dep] in-2 is the phrasal verb particle of logs-1\n                [dep] to-3 is the case marker of system-5\n                [dep] the-4 is the determiner of system-5\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',222,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Rich-0 is the nominal subject of opens-1\n                [dep] a-2 is the determiner of browser-4\n                [dep] web-3 is the compound modifier of browser-4\n                [dep] browser-4 is the object of opens-1\n                [dep] to-5 is the case marker of address-9\n                [dep] the-6 is the determiner of address-9\n                [dep] Requel-7 is the compound modifier of address-9\n                [dep] system-8 is the compound modifier of address-9\n                [dep] The-11 is the determiner of system-12\n                [dep] system-12 is the nominal subject of detect-15\n                [dep] system-12 is the nominal subject of presents-21\n                [dep] does-13 is the auxiliary of detect-15\n                [dep] n’t-14 is the adverbial modifier of detect-15\n                [dep] detect-15 is the parataxis of opens-1\n                [dep] an-16 is the determiner of session-19\n                [dep] existing-17 is the adjectival modifier of session-19\n                [dep] user-18 is the compound modifier of session-19\n                [dep] session-19 is the object of detect-15\n                [dep] and-20 is the coordination of presents-21\n                [dep] presents-21 is the parataxis of opens-1\n                [dep] presents-21 is the conjunct of detect-15\n                [dep] the-22 is the determiner of screen-24\n                [dep] login-23 is the adjectival modifier of screen-24\n                [dep] screen-24 is the object of presents-21\n                [dep] Rich-26 is the nominal subject of enters-27\n                [dep] enters-27 is the clausal complement of opens-1\n                [dep] his-28 is the possession modifier of username-29\n                [dep] username-29 is the object of enters-27\n                [dep] and-30 is the coordination of password-31\n                [dep] password-31 is the object of enters-27\n                [dep] password-31 is the conjunct of username-29\n                [dep] The-33 is the determiner of system-34\n                [dep] system-34 is the nominal subject of verifies-35\n                [dep] verifies-35 is the conjunct of opens-1\n                [dep] the-36 is the determiner of username-37\n                [dep] username-37 is the object of verifies-35\n                [dep] and-38 is the coordination of correct-42\n                [dep] password-39 is the compound modifier of combination-40\n                [dep] combination-40 is the nominal subject of correct-42\n                [dep] is-41 is the copula of correct-42\n                [dep] correct-42 is the conjunct of opens-1\n                [dep] The-44 is the determiner of system-45\n                [dep] system-45 is the nominal subject of logs-46\n                [dep] logs-46 is the parataxis of correct-42\n                [dep] that-47 is the marker of logged-51\n                [dep] Rich-48 is the nominal subject of logged-51\n                [dep] Rich-48 is the nominal subject of shows-56\n                [dep] has-49 is the auxiliary of logged-51\n                [dep] successfully-50 is the adverbial modifier of logged-51\n                [dep] logged-51 is the clausal complement of logs-46\n                [dep] into-52 is the case marker of system-54\n                [dep] the-53 is the determiner of system-54\n                [dep] and-55 is the coordination of shows-56\n                [dep] shows-56 is the clausal complement of logs-46\n                [dep] shows-56 is the conjunct of logged-51\n                [dep] him-57 is the indirect object of shows-56\n                [dep] a-58 is the determiner of list-59\n                [dep] list-59 is the object of shows-56\n                [dep] of-60 is the case marker of projects-63\n                [dep] his-61 is the possession modifier of projects-63\n                [dep] open-62 is the adjectival modifier of projects-63\n                [dep] projects-63 is the nominal modifier of list-59\n                [dep] and-64 is the coordination of overview-66\n                [dep] an-65 is the determiner of overview-66\n                [dep] overview-66 is the object of shows-56\n                [dep] overview-66 is the conjunct of list-59\n                [dep] of-67 is the case marker of changes-70\n                [dep] recent-68 is the adjectival modifier of changes-70\n                [dep] project-69 is the compound modifier of changes-70\n                [dep] changes-70 is the nominal modifier of overview-66\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',223,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The text \"Ron opens a web browser to the Requel system address. The system doesn’t detect an existing user session and presents the login screen. Ron doesn’t have a username and password yet so he chooses the help option. The system displays a message that the system requires a distinct username and password and that the user should contact Dave at extension 500 or via email to dave@requel if they need a new account or are having a problem connecting. The system also displays a help message describing what to do if the user forgot their account information. Ron sends Dave an email to create him a new account.\" in the Text is complex and may be hard to understand.',1,_binary '','2026-10-30 14:10:55','Text',NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',224,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"Dave\" in the Text is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Text','Dave',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',225,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"connecting\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','connecting',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',226,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"connects\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','connects',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',227,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"dave@requel\" in the Text is not recognized and may be spelled incorrectly.',1,_binary '','2026-10-30 14:10:55','Text','dave@requel',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',228,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (NNP Ron#1))\n                (VP (VBZ fails#1)\n                (S\n                (VP (TO to)\n                (VP (VB log#1)\n                (PRT (RP in#1))\n                (PP (IN to)\n                (NP (DT the) (NN system#1)))))))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',229,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (S\n                (NP (NNP Ron#1))\n                (VP (VBZ opens#1)\n                (NP (DT a) (NN web#1) (NN browser#1))\n                (PP (IN to)\n                (NP (DT the) (NNP Requel#1) (NN system#1) (NN address#1))))) (, .)\n                (S\n                (NP (DT The) (NN system#1))\n                (VP\n                (VP (VBZ does#1) (RB n’t)\n                (VP (VB detect#1)\n                (NP (DT an) (VBG existing#1) (NN user#1) (NN session#1)))) (CC and)\n                (VP (VBZ presents#1)\n                (NP (DT the) (JJ login) (NN screen#1))))) (. .)\n                (S\n                (NP (NNP Ron#1))\n                (VP (VBZ does#1) (RB n’t)\n                (VP (VB have#12)\n                (NP (DT a) (NN username) (CC and) (NN password#1))\n                (ADVP (RB yet#1))\n                (SBAR (IN so)\n                (S\n                (NP (PRP he))\n                (VP (VBZ chooses#1)\n                (NP (DT the) (NN help#1) (NN option#1)))))))) (. .)\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ displays#1)\n                (NP\n                (NP (DT a) (NN message#1))\n                (SBAR\n                (SBAR (IN that)\n                (S\n                (NP (DT the) (NN system#1))\n                (VP (VBZ requires#1)\n                (NP (DT a) (JJ distinct#1) (NN username) (CC and) (NN password#1))))) (CC and)\n                (SBAR (IN that)\n                (S\n                (NP (DT the) (NN user#1))\n                (VP (MD should)\n                (VP (VB contact#1)\n                (NP (NNP Dave#1))\n                (PP\n                (PP (IN at)\n                (NP (NN extension#1) (CD 500))) (CC or)\n                (PP (IN via)\n                (NP (NN email#1)\n                (S\n                (VP (TO to)\n                (VP (VB dave@requel)\n                (SBAR (IN if)\n                (S\n                (NP (PRP they))\n                (VP\n                (VP (VBP need#1)\n                (NP (DT a) (JJ new#1) (NN account#1))) (CC or)\n                (VP (VBP are)\n                (VP (VBG having#12)\n                (NP (DT a) (NN problem#1) (VBG connecting#1))))))))))))))))))))) (. .)\n                (S\n                (NP (DT The) (NN system#1))\n                (ADVP (RB also#1))\n                (VP (VBZ displays#1)\n                (NP (DT a) (NN help#1) (NN message#1))\n                (S\n                (VP (VBG describing#1)\n                (SBAR\n                (WHNP (WP what))\n                (S\n                (VP (TO to)\n                (VP (VB do#1)\n                (SBAR (IN if)\n                (S\n                (NP (DT the) (NN user#1))\n                (VP (VBD forgot)\n                (NP (PRP$ their) (NN account#1) (NN information#1))))))))))) (. .)\n                (PRN\n                (S\n                (NP (NNP Ron#1))\n                (VP (VBZ sends#1)\n                (NP (NNP Dave#1))\n                (NP (DT an) (NN email#1)\n                (S\n                (VP (TO to)\n                (VP (VB create#1)\n                (S\n                (NP (PRP him))\n                (NP (DT a) (JJ new#1) (NN account#1)))))))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',230,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                chooses[AGENT]Ron\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',231,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                fails[AGENT]Ron\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',232,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Ron-0 is the nominal subject of fails-1\n                [dep] Ron-0 is the controlling nominal subject of log-3\n                [dep] to-2 is the marker of log-3\n                [dep] log-3 is the xclausal complement of fails-1\n                [dep] in-4 is the phrasal verb particle of log-3\n                [dep] to-5 is the case marker of system-7\n                [dep] the-6 is the determiner of system-7\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',233,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Ron-0 is the nominal subject of opens-1\n                [dep] a-2 is the determiner of browser-4\n                [dep] web-3 is the compound modifier of browser-4\n                [dep] browser-4 is the object of opens-1\n                [dep] to-5 is the case marker of address-9\n                [dep] the-6 is the determiner of address-9\n                [dep] Requel-7 is the compound modifier of address-9\n                [dep] system-8 is the compound modifier of address-9\n                [dep] The-11 is the determiner of system-12\n                [dep] system-12 is the nominal subject of detect-15\n                [dep] system-12 is the nominal subject of presents-21\n                [dep] does-13 is the auxiliary of detect-15\n                [dep] n’t-14 is the adverbial modifier of detect-15\n                [dep] detect-15 is the parataxis of opens-1\n                [dep] an-16 is the determiner of session-19\n                [dep] existing-17 is the adjectival modifier of session-19\n                [dep] user-18 is the compound modifier of session-19\n                [dep] session-19 is the object of detect-15\n                [dep] and-20 is the coordination of presents-21\n                [dep] presents-21 is the parataxis of opens-1\n                [dep] presents-21 is the conjunct of detect-15\n                [dep] the-22 is the determiner of screen-24\n                [dep] login-23 is the adjectival modifier of screen-24\n                [dep] screen-24 is the object of presents-21\n                [dep] Ron-26 is the nominal subject of have-29\n                [dep] does-27 is the auxiliary of have-29\n                [dep] n’t-28 is the adverbial modifier of have-29\n                [dep] have-29 is the parataxis of opens-1\n                [dep] a-30 is the determiner of username-31\n                [dep] username-31 is the object of have-29\n                [dep] and-32 is the coordination of password-33\n                [dep] password-33 is the object of have-29\n                [dep] password-33 is the conjunct of username-31\n                [dep] yet-34 is the adverbial modifier of have-29\n                [dep] so-35 is the marker of chooses-37\n                [dep] he-36 is the nominal subject of chooses-37\n                [dep] chooses-37 is the adverbial clause modifier of have-29\n                [dep] the-38 is the determiner of option-40\n                [dep] help-39 is the compound modifier of option-40\n                [dep] option-40 is the object of chooses-37\n                [dep] The-42 is the determiner of system-43\n                [dep] system-43 is the nominal subject of displays-44\n                [dep] displays-44 is the parataxis of opens-1\n                [dep] a-45 is the determiner of message-46\n                [dep] message-46 is the object of displays-44\n                [dep] that-47 is the marker of requires-50\n                [dep] the-48 is the determiner of system-49\n                [dep] system-49 is the nominal subject of requires-50\n                [dep] requires-50 is the dependent of message-46\n                [dep] a-51 is the determiner of username-53\n                [dep] distinct-52 is the adjectival modifier of username-53\n                [dep] username-53 is the object of requires-50\n                [dep] and-54 is the coordination of password-55\n                [dep] password-55 is the object of requires-50\n                [dep] password-55 is the conjunct of username-53\n                [dep] and-56 is the coordination of contact-61\n                [dep] that-57 is the marker of contact-61\n                [dep] the-58 is the determiner of user-59\n                [dep] user-59 is the nominal subject of contact-61\n                [dep] should-60 is the auxiliary of contact-61\n                [dep] contact-61 is the dependent of message-46\n                [dep] contact-61 is the conjunct of requires-50\n                [dep] contact-61 is the conjunct of contact-61\n                [dep] Dave-62 is the object of contact-61\n                [dep] at-63 is the case marker of extension-64\n                [dep] 500-65 is the numeric modifier of extension-64\n                [dep] or-66 is the coordination of contact-61\n                [dep] via-67 is the case marker of email-68\n                [dep] to-69 is the marker of dave@requel-70\n                [dep] dave@requel-70 is the clausal modifier of noun of email-68\n                [dep] if-71 is the marker of need-73\n                [dep] they-72 is the nominal subject of need-73\n                [dep] they-72 is the nominal subject of having-79\n                [dep] need-73 is the adverbial clause modifier of dave@requel-70\n                [dep] a-74 is the determiner of account-76\n                [dep] new-75 is the adjectival modifier of account-76\n                [dep] account-76 is the object of need-73\n                [dep] or-77 is the coordination of having-79\n                [dep] are-78 is the auxiliary of having-79\n                [dep] having-79 is the adverbial clause modifier of dave@requel-70\n                [dep] having-79 is the conjunct of need-73\n                [dep] a-80 is the determiner of problem-81\n                [dep] problem-81 is the object of having-79\n                [dep] connecting-82 is the adjectival modifier of problem-81\n                [dep] The-84 is the determiner of system-85\n                [dep] system-85 is the nominal subject of displays-87\n                [dep] also-86 is the adverbial modifier of displays-87\n                [dep] displays-87 is the parataxis of opens-1\n                [dep] a-88 is the determiner of message-90\n                [dep] help-89 is the compound modifier of message-90\n                [dep] message-90 is the object of displays-87\n                [dep] describing-91 is the xclausal complement of displays-87\n                [dep] what-92 is the nominal subject of do-94\n                [dep] to-93 is the marker of do-94\n                [dep] do-94 is the clausal complement of describing-91\n                [dep] if-95 is the marker of forgot-98\n                [dep] the-96 is the determiner of user-97\n                [dep] user-97 is the nominal subject of forgot-98\n                [dep] forgot-98 is the adverbial clause modifier of do-94\n                [dep] their-99 is the possession modifier of information-101\n                [dep] account-100 is the compound modifier of information-101\n                [dep] information-101 is the object of forgot-98\n                [dep] Ron-103 is the nominal subject of sends-104\n                [dep] sends-104 is the parataxis of displays-87\n                [dep] Dave-105 is the indirect object of sends-104\n                [dep] an-106 is the determiner of email-107\n                [dep] email-107 is the object of sends-104\n                [dep] to-108 is the marker of create-109\n                [dep] create-109 is the clausal modifier of noun of email-107\n                [dep] him-110 is the nominal subject of account-113\n                [dep] a-111 is the determiner of account-113\n                [dep] new-112 is the adjectival modifier of account-113\n                [dep] account-113 is the xclausal complement of create-109\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',234,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The text \"When Theresa logs in, the system determines that she has permission to create projects and displays the \"Create Project\" option in the system interface. Theresa chooses to create a new project and the system displays the new project interface. Theresa enters \"Purchase Order System\" for the project name, leaves the description blank, and leaves \"Bailey Pet Supply\", her default automatically supplied, as the customer.\n\n                The system creates the new project and adds Theresa as a stakeholder with permission to edit all project entities and grant permission to other users to edit entities. The system closes the new project interface and displays the project navigation interface to add and edit the project\'s entities.\" in the Text is complex and may be hard to understand.',1,_binary '','2026-10-30 14:10:55','Text',NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',235,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (NNP Theresa#1))\n                (VP (VBZ creates#1)\n                (NP (DT a) (JJ new#1) (NN project#1)))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',236,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (S\n                (SBAR\n                (X (WRB When))\n                (S\n                (S\n                (NP (NNP Theresa#2))\n                (VP (VBZ logs#1)\n                (PRT (RP in#1)))) (, ,)\n                (S\n                (NP (DT the) (NN system#1))\n                (VP (VBZ determines#1)\n                (SBAR (IN that)\n                (S\n                (NP (PRP she))\n                (VP\n                (VP (VBZ has)\n                (NP (NN permission#1)\n                (S\n                (VP (TO to)\n                (VP (VB create#1)\n                (NP (NNS projects#1))))))) (CC and)\n                (VP (VBZ displays#1)\n                (NP (DT the)\n                (X)\n                (X (NNP Create#1) (NNP Project#1))\n                (X) (NN option#1))\n                (PP (IN in)\n                (NP (DT the) (NN system#1) (NN interface#1))))))))) (, .)\n                (S\n                (NP (NNP Theresa#2))\n                (VP (VBZ chooses#1)\n                (S\n                (VP (TO to)\n                (VP (VB create#1)\n                (NP (DT a) (JJ new#1) (NN project#1))))))) (CC and)\n                (S\n                (NP (DT the) (NN system#1))\n                (VP (VBZ displays#1)\n                (NP (DT the) (JJ new#1) (NN project#1) (NN interface#1)))) (. .)))\n                (S\n                (NP (NNP Theresa#2))\n                (VP\n                (VP (VBZ enters#1)\n                (NP\n                (X)\n                (NP (NNP Purchase#1) (NNP Order#1) (NNP System#1))\n                (X)\n                (PP (IN for)\n                (NP (DT the) (NN project#1) (NN name#1))))) (, ,)\n                (VP (VBZ leaves#1)\n                (NP (DT the) (NN description#1))\n                (S\n                (ADJP (JJ blank#1)))) (, ,) (CC and)\n                (VP (VBZ leaves#1)\n                (NP\n                (X) (NNP Bailey#1) (NNP Pet#1) (NNP Supply#1)\n                (X))))) (, ,)\n                (S\n                (NP (PRP$ her) (NN default#1))\n                (ADVP (RB automatically#1))\n                (VP (VBD supplied))) (, ,)\n                (S\n                (PP (IN as)\n                (NP (DT the) (NN customer#1))) (, .)\n                (NP (DT The) (NN system#1))\n                (VP\n                (VP (VBZ creates#1)\n                (NP (DT the) (JJ new#1) (NN project#1))) (CC and)\n                (VP (VBZ adds#1)\n                (NP (NNP Theresa#2))\n                (PP (IN as)\n                (NP\n                (NP (DT a) (NN stakeholder#1))\n                (PP (IN with)\n                (NP (NN permission#1))))))))\n                (S\n                (VP (TO to)\n                (VP\n                (VP (VB edit#1)\n                (NP (DT all) (NN project#1) (NNS entities#1))) (CC and)\n                (VP (VB grant#1)\n                (NP (NN permission#1))\n                (PP (IN to)\n                (NP (JJ other#1) (NNS users#1)))\n                (S\n                (VP (TO to)\n                (VP (VB edit#1)\n                (NP (NNS entities#1)))))))))) (. .)\n                (S\n                (NP (DT The) (NN system#1))\n                (VP\n                (VP (VBZ closes#1)\n                (NP (DT the) (JJ new#1) (NN project#1) (NN interface#1))) (CC and)\n                (VP (VBZ displays#1)\n                (NP (DT the) (NN project#1) (NN navigation#1) (NN interface#1))\n                (S\n                (VP (TO to)\n                (VP (VB add#1) (CC and) (VB edit#1)\n                (NP\n                (NP (DT the) (NN project#1) (POS \'s)) (NNS entities#1)))))))) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',237,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                creates[AGENT]Theresa\n                creates[PRODUCT]a new project\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',238,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                creates[AGENT]the system\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',239,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Theresa-0 is the nominal subject of creates-1\n                [dep] a-2 is the determiner of project-4\n                [dep] new-3 is the adjectival modifier of project-4\n                [dep] project-4 is the object of creates-1\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',240,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] When-0 is the adverbial modifier of logs-2\n                [dep] Theresa-1 is the nominal subject of logs-2\n                [dep] logs-2 is the adverbial clause modifier of enters-45\n                [dep] in-3 is the phrasal verb particle of logs-2\n                [dep] the-5 is the determiner of system-6\n                [dep] system-6 is the nominal subject of determines-7\n                [dep] determines-7 is the clausal complement of logs-2\n                [dep] that-8 is the marker of has-10\n                [dep] she-9 is the nominal subject of has-10\n                [dep] she-9 is the nominal subject of displays-16\n                [dep] has-10 is the clausal complement of determines-7\n                [dep] permission-11 is the object of has-10\n                [dep] to-12 is the marker of create-13\n                [dep] create-13 is the clausal modifier of noun of permission-11\n                [dep] projects-14 is the object of create-13\n                [dep] and-15 is the coordination of displays-16\n                [dep] displays-16 is the clausal complement of determines-7\n                [dep] displays-16 is the conjunct of has-10\n                [dep] the-17 is the determiner of option-22\n                [dep] Create-19 is the compound modifier of Project-20\n                [dep] Project-20 is the compound modifier of option-22\n                [dep] option-22 is the object of displays-16\n                [dep] in-23 is the case marker of interface-26\n                [dep] the-24 is the determiner of interface-26\n                [dep] system-25 is the compound modifier of interface-26\n                [dep] Theresa-28 is the nominal subject of chooses-29\n                [dep] Theresa-28 is the controlling nominal subject of create-31\n                [dep] chooses-29 is the conjunct of logs-2\n                [dep] chooses-29 is the adverbial clause modifier of enters-45\n                [dep] to-30 is the marker of create-31\n                [dep] create-31 is the xclausal complement of chooses-29\n                [dep] a-32 is the determiner of project-34\n                [dep] new-33 is the adjectival modifier of project-34\n                [dep] project-34 is the object of create-31\n                [dep] and-35 is the coordination of displays-38\n                [dep] the-36 is the determiner of system-37\n                [dep] system-37 is the nominal subject of displays-38\n                [dep] displays-38 is the conjunct of logs-2\n                [dep] displays-38 is the adverbial clause modifier of enters-45\n                [dep] the-39 is the determiner of interface-42\n                [dep] new-40 is the adjectival modifier of interface-42\n                [dep] project-41 is the compound modifier of interface-42\n                [dep] interface-42 is the object of displays-38\n                [dep] Theresa-44 is the nominal subject of enters-45\n                [dep] Theresa-44 is the nominal subject of leaves-56\n                [dep] Theresa-44 is the nominal subject of leaves-62\n                [dep] Purchase-47 is the compound modifier of System-49\n                [dep] Order-48 is the compound modifier of System-49\n                [dep] System-49 is the object of enters-45\n                [dep] for-51 is the case marker of name-54\n                [dep] the-52 is the determiner of name-54\n                [dep] project-53 is the compound modifier of name-54\n                [dep] name-54 is the nominal modifier of System-49\n                [dep] leaves-56 is the conjunct of enters-45\n                [dep] the-57 is the determiner of description-58\n                [dep] description-58 is the object of leaves-56\n                [dep] blank-59 is the xclausal complement of leaves-56\n                [dep] and-61 is the coordination of leaves-62\n                [dep] leaves-62 is the conjunct of enters-45\n                [dep] Bailey-64 is the compound modifier of Supply-66\n                [dep] Pet-65 is the compound modifier of Supply-66\n                [dep] Supply-66 is the object of leaves-62\n                [dep] her-69 is the possession modifier of default-70\n                [dep] default-70 is the nominal subject of supplied-72\n                [dep] automatically-71 is the adverbial modifier of supplied-72\n                [dep] supplied-72 is the parataxis of enters-45\n                [dep] as-74 is the case marker of customer-76\n                [dep] the-75 is the determiner of customer-76\n                [dep] The-78 is the determiner of system-79\n                [dep] system-79 is the nominal subject of creates-80\n                [dep] system-79 is the nominal subject of adds-85\n                [dep] creates-80 is the parataxis of enters-45\n                [dep] the-81 is the determiner of project-83\n                [dep] new-82 is the adjectival modifier of project-83\n                [dep] project-83 is the object of creates-80\n                [dep] and-84 is the coordination of adds-85\n                [dep] adds-85 is the parataxis of enters-45\n                [dep] adds-85 is the conjunct of creates-80\n                [dep] Theresa-86 is the object of adds-85\n                [dep] as-87 is the case marker of stakeholder-89\n                [dep] a-88 is the determiner of stakeholder-89\n                [dep] with-90 is the case marker of permission-91\n                [dep] permission-91 is the nominal modifier of stakeholder-89\n                [dep] to-92 is the marker of edit-93\n                [dep] edit-93 is the parataxis of enters-45\n                [dep] all-94 is the determiner of entities-96\n                [dep] project-95 is the compound modifier of entities-96\n                [dep] entities-96 is the object of edit-93\n                [dep] and-97 is the coordination of grant-98\n                [dep] grant-98 is the parataxis of enters-45\n                [dep] grant-98 is the conjunct of edit-93\n                [dep] permission-99 is the object of grant-98\n                [dep] to-100 is the case marker of users-102\n                [dep] other-101 is the adjectival modifier of users-102\n                [dep] to-103 is the marker of edit-104\n                [dep] edit-104 is the adverbial clause modifier of grant-98\n                [dep] entities-105 is the object of edit-104\n                [dep] The-107 is the determiner of system-108\n                [dep] system-108 is the nominal subject of closes-109\n                [dep] system-108 is the nominal subject of displays-115\n                [dep] closes-109 is the parataxis of enters-45\n                [dep] the-110 is the determiner of interface-113\n                [dep] new-111 is the adjectival modifier of interface-113\n                [dep] project-112 is the compound modifier of interface-113\n                [dep] interface-113 is the object of closes-109\n                [dep] and-114 is the coordination of displays-115\n                [dep] displays-115 is the parataxis of enters-45\n                [dep] displays-115 is the conjunct of closes-109\n                [dep] the-116 is the determiner of interface-119\n                [dep] project-117 is the compound modifier of interface-119\n                [dep] navigation-118 is the compound modifier of interface-119\n                [dep] interface-119 is the object of displays-115\n                [dep] to-120 is the marker of add-121\n                [dep] add-121 is the adverbial clause modifier of displays-115\n                [dep] and-122 is the coordination of edit-123\n                [dep] edit-123 is the adverbial clause modifier of displays-115\n                [dep] edit-123 is the conjunct of add-121\n                [dep] the-124 is the determiner of project-125\n                [dep] project-125 is the possession modifier of entities-127\n                [dep] \'s-126 is the case marker of project-125\n                [dep] entities-127 is the object of add-121\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',241,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The text \"Theresa connects to the Requel system via a web browser from a link in an email message sent by the Requel system when Dave the system administrator created an account for her. The system displays the new user setup interface, prompting Theresa to confirm the personal information Dave entered about her and filling in any missing information, such as name, phone number, email address, a username and a password. When Theresa completes entering the information the system forwards her to the login screen.\n\n                Theresa enters her username and password, and the system verifies it as correct. The system logs that Theresa has successfully logged into the system, and because Theresa is a novice user, the system displays the novice user interface to help her get started.\" in the Text is complex and may be hard to understand.',1,_binary '','2026-10-30 14:10:55','Text',NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.impl.LexicalIssue',242,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The word \"get\" in the Text is vague and may lead to ambiguity.',1,_binary '','2026-10-30 14:10:55','Text','get',3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',243,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP\n                (S\n                (S\n                (NP (NNP Theresa#1))\n                (VP (VBZ connects#1)\n                (PP (IN to)\n                (NP\n                (NP (DT the) (NNP Requel#1) (NN system#1))\n                (PP (IN via)\n                (NP (DT a) (NN web#1) (NN browser#1)))))\n                (PP (IN from)\n                (NP\n                (NP (DT a) (NN link#1))\n                (PP (IN in)\n                (NP\n                (NP (DT an) (NN email#1) (NN message#1))\n                (VP (VBN sent)\n                (PP (IN by)\n                (NP (DT the) (NNP Requel#1) (NN system#1)))\n                (SBAR\n                (X (WRB when))\n                (S\n                (NP\n                (NP (NNP Dave#1))\n                (NP (DT the) (NN system#1) (NN administrator#1)))\n                (VP (VBD created#1)\n                (NP (DT an) (NN account#1))\n                (PP (IN for)\n                (NP (PRP her))))))))))))) (. .)\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ displays#1)\n                (NP (DT the) (JJ new#1) (NN user#1) (NN setup#1) (NN interface#1)) (, ,)\n                (S\n                (VP\n                (VP (VBG prompting#1)\n                (NP (NNP Theresa#1))\n                (S\n                (VP (TO to)\n                (VP (VB confirm#1)\n                (NP\n                (NP (DT the)\n                (X (JJ personal#1) (NN information#1)) (NNP Dave#1))\n                (VP (VBN entered#1)\n                (PP (IN about)\n                (NP (PRP her))))))))) (CC and)\n                (VP (VBG filling#1)\n                (PP (IN in)\n                (NP (DT any) (VBG missing#1) (NN information#1))))))))) (, ,)\n                (S\n                (S\n                (PP (JJ such#1)\n                (PP (IN as)\n                (NP\n                (NP (NN name#1)) (, ,)\n                (NP (NN phone#1) (NN number#1)) (, ,)\n                (NP (NN email#1) (NN address#1)) (, ,)\n                (NP (DT a) (NN username)) (CC and)\n                (NP (DT a) (NN password#1))))) (, .)\n                (SBAR\n                (X (WRB When))\n                (S\n                (NP (NNP Theresa#1))\n                (VP (VBZ completes#1)\n                (S\n                (VP (VBG entering#1)\n                (NP (DT the) (NN information#1)))))))\n                (NP (DT the) (NN system#1))\n                (VP (VBD forwards#1)\n                (NP (PRP her))\n                (PP (IN to)\n                (NP (DT the) (NN login) (NN screen#1))))) (. .)\n                (S\n                (NP (NNP Theresa#1))\n                (VP (VBZ enters#1)\n                (NP (PRP$ her) (NN username) (CC and) (NN password#1))))) (, ,) (CC and)\n                (S\n                (S\n                (NP (DT the) (NN system#1))\n                (VP (VBZ verifies#1)\n                (S\n                (NP (PRP it))\n                (ADJP (RB as#1) (JJ correct#1))))) (. .)\n                (S\n                (S\n                (NP (DT The) (NN system#1))\n                (VP (VBZ logs#1)\n                (SBAR (IN that)\n                (S\n                (NP (NNP Theresa#1))\n                (VP (VBZ has)\n                (ADVP (RB successfully#1))\n                (VP (VBN logged)\n                (PP (IN into)\n                (NP (DT the) (NN system#1))))))))) (, ,) (CC and)\n                (S\n                (SBAR (IN because)\n                (S\n                (NP (NNP Theresa#1))\n                (VP (VBZ is)\n                (NP (DT a) (NN novice#1) (NN user#1))))) (, ,)\n                (NP (DT the) (NN system#1))\n                (VP (VBZ displays#1)\n                (NP (DT the) (NN novice#1) (NN user#1) (NN interface#1))\n                (S\n                (VP (TO to)\n                (VP (VB help#1)\n                (S\n                (NP (PRP her))\n                (VP (VB get#1)))))))))))\n                (VP (VBD started#1)) (. .)))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',244,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (NNP Theresa#1))\n                (VP (VBZ logs#1)\n                (PRT (RP in#1))\n                (PP (IN to)\n                (NP (DT the) (NN system#1))))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',245,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                created[AGENT]the system\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',246,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                logs[AGENT]Theresa\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',247,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Theresa-0 is the nominal subject of connects-1\n                [dep] connects-1 is the nominal subject of started-137\n                [dep] to-2 is the case marker of system-5\n                [dep] the-3 is the determiner of system-5\n                [dep] Requel-4 is the compound modifier of system-5\n                [dep] via-6 is the case marker of browser-9\n                [dep] a-7 is the determiner of browser-9\n                [dep] web-8 is the compound modifier of browser-9\n                [dep] browser-9 is the nominal modifier of system-5\n                [dep] from-10 is the case marker of link-12\n                [dep] a-11 is the determiner of link-12\n                [dep] in-13 is the case marker of message-16\n                [dep] an-14 is the determiner of message-16\n                [dep] email-15 is the compound modifier of message-16\n                [dep] message-16 is the nominal modifier of link-12\n                [dep] sent-17 is the clausal modifier of noun of message-16\n                [dep] by-18 is the case marker of system-21\n                [dep] the-19 is the determiner of system-21\n                [dep] Requel-20 is the compound modifier of system-21\n                [dep] when-22 is the adverbial modifier of created-27\n                [dep] Dave-23 is the nominal subject of created-27\n                [dep] the-24 is the determiner of administrator-26\n                [dep] system-25 is the compound modifier of administrator-26\n                [dep] administrator-26 is the dependent of Dave-23\n                [dep] created-27 is the adverbial clause modifier of sent-17\n                [dep] an-28 is the determiner of account-29\n                [dep] account-29 is the object of created-27\n                [dep] for-30 is the case marker of her-31\n                [dep] The-33 is the determiner of system-34\n                [dep] system-34 is the nominal subject of displays-35\n                [dep] displays-35 is the parataxis of connects-1\n                [dep] the-36 is the determiner of interface-40\n                [dep] new-37 is the adjectival modifier of interface-40\n                [dep] user-38 is the compound modifier of interface-40\n                [dep] setup-39 is the compound modifier of interface-40\n                [dep] interface-40 is the object of displays-35\n                [dep] prompting-42 is the dependent of displays-35\n                [dep] Theresa-43 is the object of prompting-42\n                [dep] Theresa-43 is the controlling nominal subject of confirm-45\n                [dep] to-44 is the marker of confirm-45\n                [dep] confirm-45 is the xclausal complement of prompting-42\n                [dep] the-46 is the determiner of Dave-49\n                [dep] personal-47 is the adjectival modifier of information-48\n                [dep] information-48 is the compound modifier of Dave-49\n                [dep] Dave-49 is the object of confirm-45\n                [dep] entered-50 is the clausal modifier of noun of Dave-49\n                [dep] about-51 is the case marker of her-52\n                [dep] and-53 is the coordination of filling-54\n                [dep] filling-54 is the dependent of displays-35\n                [dep] filling-54 is the conjunct of prompting-42\n                [dep] in-55 is the case marker of information-58\n                [dep] any-56 is the determiner of information-58\n                [dep] missing-57 is the adjectival modifier of information-58\n                [dep] such-60 is the case marker of name-62\n                [dep] as-61 is the case marker of name-62\n                [dep] name-62 is the adverbial clause modifier of forwards-84\n                [dep] phone-64 is the compound modifier of number-65\n                [dep] number-65 is the conjunct of name-62\n                [dep] number-65 is the adverbial clause modifier of forwards-84\n                [dep] email-67 is the compound modifier of address-68\n                [dep] address-68 is the conjunct of name-62\n                [dep] address-68 is the adverbial clause modifier of forwards-84\n                [dep] a-70 is the determiner of username-71\n                [dep] username-71 is the conjunct of name-62\n                [dep] username-71 is the adverbial clause modifier of forwards-84\n                [dep] and-72 is the coordination of password-74\n                [dep] a-73 is the determiner of password-74\n                [dep] password-74 is the conjunct of name-62\n                [dep] password-74 is the adverbial clause modifier of forwards-84\n                [dep] When-76 is the adverbial modifier of completes-78\n                [dep] Theresa-77 is the nominal subject of completes-78\n                [dep] completes-78 is the adverbial clause modifier of forwards-84\n                [dep] entering-79 is the xclausal complement of completes-78\n                [dep] the-80 is the determiner of information-81\n                [dep] information-81 is the object of entering-79\n                [dep] the-82 is the determiner of system-83\n                [dep] system-83 is the nominal subject of forwards-84\n                [dep] forwards-84 is the conjunct of connects-1\n                [dep] forwards-84 is the nominal subject of started-137\n                [dep] her-85 is the object of forwards-84\n                [dep] to-86 is the case marker of screen-89\n                [dep] the-87 is the determiner of screen-89\n                [dep] login-88 is the compound modifier of screen-89\n                [dep] Theresa-91 is the nominal subject of enters-92\n                [dep] enters-92 is the parataxis of forwards-84\n                [dep] her-93 is the possession modifier of username-94\n                [dep] username-94 is the object of enters-92\n                [dep] and-95 is the coordination of password-96\n                [dep] password-96 is the object of enters-92\n                [dep] password-96 is the conjunct of username-94\n                [dep] and-98 is the coordination of verifies-101\n                [dep] the-99 is the determiner of system-100\n                [dep] system-100 is the nominal subject of verifies-101\n                [dep] verifies-101 is the conjunct of connects-1\n                [dep] verifies-101 is the nominal subject of started-137\n                [dep] it-102 is the nominal subject of correct-104\n                [dep] as-103 is the adverbial modifier of correct-104\n                [dep] correct-104 is the xclausal complement of verifies-101\n                [dep] The-106 is the determiner of system-107\n                [dep] system-107 is the nominal subject of logs-108\n                [dep] logs-108 is the parataxis of verifies-101\n                [dep] that-109 is the marker of logged-113\n                [dep] Theresa-110 is the nominal subject of logged-113\n                [dep] has-111 is the auxiliary of logged-113\n                [dep] successfully-112 is the adverbial modifier of logged-113\n                [dep] logged-113 is the clausal complement of logs-108\n                [dep] into-114 is the case marker of system-116\n                [dep] the-115 is the determiner of system-116\n                [dep] and-118 is the coordination of displays-128\n                [dep] because-119 is the marker of user-124\n                [dep] Theresa-120 is the nominal subject of user-124\n                [dep] is-121 is the copula of user-124\n                [dep] a-122 is the determiner of user-124\n                [dep] novice-123 is the compound modifier of user-124\n                [dep] user-124 is the adverbial clause modifier of displays-128\n                [dep] the-126 is the determiner of system-127\n                [dep] system-127 is the nominal subject of displays-128\n                [dep] displays-128 is the parataxis of verifies-101\n                [dep] displays-128 is the conjunct of logs-108\n                [dep] the-129 is the determiner of interface-132\n                [dep] novice-130 is the compound modifier of interface-132\n                [dep] user-131 is the compound modifier of interface-132\n                [dep] interface-132 is the object of displays-128\n                [dep] to-133 is the marker of help-134\n                [dep] help-134 is the adverbial clause modifier of displays-128\n                [dep] her-135 is the nominal subject of get-136\n                [dep] get-136 is the clausal complement of help-134\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',248,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] Theresa-0 is the nominal subject of logs-1\n                [dep] in-2 is the phrasal verb particle of logs-1\n                [dep] to-3 is the case marker of system-5\n                [dep] the-4 is the determiner of system-5\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',249,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'The assistant could not analyze the structure of the step text. It may be too complex or didnt have an identifiable syntactic subject.',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',250,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (VP (VB collect#1)\n                (NP (DT the) (NNS members#1)))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',251,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] the-1 is the determiner of members-2\n                [dep] members-2 is the object of collect-0\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',252,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (DT A) (NN user#1))\n                (VP (VBZ creates#1)\n                (NP (DT a) (JJ new#1) (NN project#1)))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',253,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                creates[AGENT]A user\n                creates[PRODUCT]a new project\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',254,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] A-0 is the determiner of user-1\n                [dep] user-1 is the nominal subject of creates-2\n                [dep] a-3 is the determiner of project-5\n                [dep] new-4 is the adjectival modifier of project-5\n                [dep] project-5 is the object of creates-2\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',255,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Phrase Structure:\n\n                (ROOT\n                (S\n                (NP (DT A) (NN user#1))\n                (VP (VBZ logs#1)\n                (PRT (RP in#1))\n                (PP (IN to)\n                (NP (DT the) (NN system#1))))))',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',256,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Semantic Roles:\n                logs[AGENT]A user\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',257,'2025-10-14 00:00:00','com.rreganjr.requel.project.Project',1,'Syntax Dependencies:\n                [dep] A-0 is the determiner of user-1\n                [dep] user-1 is the nominal subject of logs-2\n                [dep] in-3 is the phrasal verb particle of logs-2\n                [dep] to-4 is the case marker of system-6\n                [dep] the-5 is the determiner of system-6\n            ',1,NULL,NULL,NULL,NULL,3,NULL,NULL);
INSERT INTO `annotations` VALUES ('com.rreganjr.requel.annotation.Note',258,'2009-01-20 00:00:00','com.rreganjr.requel.project.Project',1,'hi there',1,NULL,NULL,NULL,NULL,1,NULL,NULL);
/*!40000 ALTER TABLE `annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `arguments` WRITE;
/*!40000 ALTER TABLE `arguments` DISABLE KEYS */;
INSERT INTO `arguments` VALUES (1,'2009-01-20 00:00:00',NULL,'we should be more formal',1,1,284);
/*!40000 ALTER TABLE `arguments` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `categorydef` WRITE;
/*!40000 ALTER TABLE `categorydef` DISABLE KEYS */;
INSERT INTO `categorydef` VALUES (0,'adj.all','a');
INSERT INTO `categorydef` VALUES (1,'adj.pert','a');
INSERT INTO `categorydef` VALUES (2,'adv.all','r');
INSERT INTO `categorydef` VALUES (3,'noun.tops','n');
INSERT INTO `categorydef` VALUES (4,'noun.act','n');
INSERT INTO `categorydef` VALUES (5,'noun.animal','n');
INSERT INTO `categorydef` VALUES (6,'noun.artifact','n');
INSERT INTO `categorydef` VALUES (7,'noun.attribute','n');
INSERT INTO `categorydef` VALUES (8,'noun.body','n');
INSERT INTO `categorydef` VALUES (9,'noun.cognition','n');
INSERT INTO `categorydef` VALUES (10,'noun.communication','n');
INSERT INTO `categorydef` VALUES (11,'noun.event','n');
INSERT INTO `categorydef` VALUES (12,'noun.feeling','n');
INSERT INTO `categorydef` VALUES (13,'noun.food','n');
INSERT INTO `categorydef` VALUES (14,'noun.group','n');
INSERT INTO `categorydef` VALUES (15,'noun.location','n');
INSERT INTO `categorydef` VALUES (16,'noun.motive','n');
INSERT INTO `categorydef` VALUES (17,'noun.object','n');
INSERT INTO `categorydef` VALUES (18,'noun.person','n');
INSERT INTO `categorydef` VALUES (19,'noun.phenomenon','n');
INSERT INTO `categorydef` VALUES (20,'noun.plant','n');
INSERT INTO `categorydef` VALUES (21,'noun.possession','n');
INSERT INTO `categorydef` VALUES (22,'noun.process','n');
INSERT INTO `categorydef` VALUES (23,'noun.quantity','n');
INSERT INTO `categorydef` VALUES (24,'noun.linkdef','n');
INSERT INTO `categorydef` VALUES (25,'noun.shape','n');
INSERT INTO `categorydef` VALUES (26,'noun.state','n');
INSERT INTO `categorydef` VALUES (27,'noun.substance','n');
INSERT INTO `categorydef` VALUES (28,'noun.time','n');
INSERT INTO `categorydef` VALUES (29,'verb.body','v');
INSERT INTO `categorydef` VALUES (30,'verb.change','v');
INSERT INTO `categorydef` VALUES (31,'verb.cognition','v');
INSERT INTO `categorydef` VALUES (32,'verb.communication','v');
INSERT INTO `categorydef` VALUES (33,'verb.competition','v');
INSERT INTO `categorydef` VALUES (34,'verb.consumption','v');
INSERT INTO `categorydef` VALUES (35,'verb.contact','v');
INSERT INTO `categorydef` VALUES (36,'verb.creation','v');
INSERT INTO `categorydef` VALUES (37,'verb.emotion','v');
INSERT INTO `categorydef` VALUES (38,'verb.motion','v');
INSERT INTO `categorydef` VALUES (39,'verb.perception','v');
INSERT INTO `categorydef` VALUES (40,'verb.possession','v');
INSERT INTO `categorydef` VALUES (41,'verb.social','v');
INSERT INTO `categorydef` VALUES (42,'verb.stative','v');
INSERT INTO `categorydef` VALUES (43,'verb.weather','v');
INSERT INTO `categorydef` VALUES (44,'adj.ppl','a');
/*!40000 ALTER TABLE `categorydef` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `goal_relations` WRITE;
/*!40000 ALTER TABLE `goal_relations` DISABLE KEYS */;
INSERT INTO `goal_relations` VALUES (1,'2009-01-04 00:00:00','Supports',1,1,5,4);
INSERT INTO `goal_relations` VALUES (2,'2009-01-04 00:00:00','Supports',1,1,6,4);
/*!40000 ALTER TABLE `goal_relations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `goal_relations_annotations` WRITE;
/*!40000 ALTER TABLE `goal_relations_annotations` DISABLE KEYS */;
/*!40000 ALTER TABLE `goal_relations_annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `goals` WRITE;
/*!40000 ALTER TABLE `goals` DISABLE KEYS */;
INSERT INTO `goals` VALUES (1,'2009-01-04 00:00:00',20,'The system analyzes the requirements as they are added to a project and makes suggestions by adding issues to the elements.','Automatted Assistance',1,1);
INSERT INTO `goals` VALUES (2,'2009-01-04 00:00:00',20,'A team of technical and business stakeholders can use the system to create accurate, precise and unambiguous system requirements.','Collaborative Elicitation of Requirements',1,1);
INSERT INTO `goals` VALUES (3,'2009-01-04 00:00:00',20,'The system supports integrated discussion of the requirements such that stakeholders can identify issues with the project elements, state multiple solutions to each issue, and pros and cons of each solution.','Discussion of Requirements',1,1);
INSERT INTO `goals` VALUES (4,'2009-01-04 00:00:00',20,'The system is easy to use so that non-technical users can contribute to the requirements without the need for specialized skills or training.','Easy to Use',1,1);
INSERT INTO `goals` VALUES (5,'2009-01-04 00:00:00',19,'The requirements are defined in natural language such that non-technical users do not need specialized skills to understand the requirements.','Requirements defined in Natural Language',1,1);
INSERT INTO `goals` VALUES (6,'2009-01-04 00:00:00',17,'The system does not force a specific requirements elicitation process on the stakeholders. The system does not require the stakeholders to use all the features of the system.','Flexible Process and Deliverables',1,1);
INSERT INTO `goals` VALUES (7,'2009-01-04 00:00:00',21,'Using the system improves understanding of the requirements by all the stakeholders compared to non-integrated tools such as emails and spreadsheets.','Improved Understanding',1,1);
INSERT INTO `goals` VALUES (8,'2009-01-04 00:00:00',21,'The system notifies the stakeholders of changes to the project. The user can customize how the notifications occur: every time a change is made, or a periodic summary.','Notification of Project Changes',1,1);
INSERT INTO `goals` VALUES (9,'2009-01-20 00:00:00',23,'The system secures project content from unauthorized access by users not assigned to a project or non-users of the system.','Secure data access',1,1);
INSERT INTO `goals` VALUES (10,'2009-01-05 00:00:00',24,'The work done on the thesis is exclusively done by the student.','Work is your own',1,1);
/*!40000 ALTER TABLE `goals` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `goals_annotations` WRITE;
/*!40000 ALTER TABLE `goals_annotations` DISABLE KEYS */;
INSERT INTO `goals_annotations` VALUES (1,1);
INSERT INTO `goals_annotations` VALUES (2,1);
INSERT INTO `goals_annotations` VALUES (3,1);
INSERT INTO `goals_annotations` VALUES (4,1);
INSERT INTO `goals_annotations` VALUES (6,1);
INSERT INTO `goals_annotations` VALUES (7,1);
INSERT INTO `goals_annotations` VALUES (8,1);
INSERT INTO `goals_annotations` VALUES (2,4);
INSERT INTO `goals_annotations` VALUES (1,6);
INSERT INTO `goals_annotations` VALUES (2,7);
INSERT INTO `goals_annotations` VALUES (6,9);
INSERT INTO `goals_annotations` VALUES (7,10);
INSERT INTO `goals_annotations` VALUES (8,12);
INSERT INTO `goals_annotations` VALUES (8,14);
INSERT INTO `goals_annotations` VALUES (2,17);
INSERT INTO `goals_annotations` VALUES (3,17);
INSERT INTO `goals_annotations` VALUES (9,20);
INSERT INTO `goals_annotations` VALUES (8,22);
INSERT INTO `goals_annotations` VALUES (10,23);
INSERT INTO `goals_annotations` VALUES (10,25);
INSERT INTO `goals_annotations` VALUES (8,26);
INSERT INTO `goals_annotations` VALUES (8,36);
INSERT INTO `goals_annotations` VALUES (9,38);
INSERT INTO `goals_annotations` VALUES (1,39);
INSERT INTO `goals_annotations` VALUES (6,41);
INSERT INTO `goals_annotations` VALUES (6,48);
INSERT INTO `goals_annotations` VALUES (7,49);
INSERT INTO `goals_annotations` VALUES (3,55);
INSERT INTO `goals_annotations` VALUES (3,56);
INSERT INTO `goals_annotations` VALUES (7,58);
INSERT INTO `goals_annotations` VALUES (8,59);
INSERT INTO `goals_annotations` VALUES (3,61);
INSERT INTO `goals_annotations` VALUES (5,63);
INSERT INTO `goals_annotations` VALUES (7,64);
INSERT INTO `goals_annotations` VALUES (4,65);
INSERT INTO `goals_annotations` VALUES (5,65);
INSERT INTO `goals_annotations` VALUES (9,69);
INSERT INTO `goals_annotations` VALUES (3,70);
INSERT INTO `goals_annotations` VALUES (4,72);
INSERT INTO `goals_annotations` VALUES (5,73);
INSERT INTO `goals_annotations` VALUES (3,74);
INSERT INTO `goals_annotations` VALUES (1,80);
INSERT INTO `goals_annotations` VALUES (4,86);
INSERT INTO `goals_annotations` VALUES (8,89);
INSERT INTO `goals_annotations` VALUES (3,92);
INSERT INTO `goals_annotations` VALUES (8,95);
INSERT INTO `goals_annotations` VALUES (1,98);
INSERT INTO `goals_annotations` VALUES (3,98);
INSERT INTO `goals_annotations` VALUES (4,98);
INSERT INTO `goals_annotations` VALUES (5,98);
INSERT INTO `goals_annotations` VALUES (7,98);
INSERT INTO `goals_annotations` VALUES (6,99);
INSERT INTO `goals_annotations` VALUES (8,99);
INSERT INTO `goals_annotations` VALUES (10,100);
INSERT INTO `goals_annotations` VALUES (10,104);
INSERT INTO `goals_annotations` VALUES (9,106);
INSERT INTO `goals_annotations` VALUES (1,112);
INSERT INTO `goals_annotations` VALUES (2,112);
INSERT INTO `goals_annotations` VALUES (3,112);
INSERT INTO `goals_annotations` VALUES (4,112);
INSERT INTO `goals_annotations` VALUES (6,112);
INSERT INTO `goals_annotations` VALUES (7,112);
INSERT INTO `goals_annotations` VALUES (8,112);
INSERT INTO `goals_annotations` VALUES (9,112);
INSERT INTO `goals_annotations` VALUES (10,112);
INSERT INTO `goals_annotations` VALUES (1,131);
INSERT INTO `goals_annotations` VALUES (1,132);
INSERT INTO `goals_annotations` VALUES (1,133);
INSERT INTO `goals_annotations` VALUES (1,134);
INSERT INTO `goals_annotations` VALUES (1,135);
INSERT INTO `goals_annotations` VALUES (1,136);
INSERT INTO `goals_annotations` VALUES (1,137);
INSERT INTO `goals_annotations` VALUES (2,138);
INSERT INTO `goals_annotations` VALUES (2,142);
INSERT INTO `goals_annotations` VALUES (2,143);
INSERT INTO `goals_annotations` VALUES (2,144);
INSERT INTO `goals_annotations` VALUES (2,145);
INSERT INTO `goals_annotations` VALUES (2,146);
INSERT INTO `goals_annotations` VALUES (2,147);
INSERT INTO `goals_annotations` VALUES (3,148);
INSERT INTO `goals_annotations` VALUES (3,149);
INSERT INTO `goals_annotations` VALUES (3,150);
INSERT INTO `goals_annotations` VALUES (3,151);
INSERT INTO `goals_annotations` VALUES (3,152);
INSERT INTO `goals_annotations` VALUES (4,153);
INSERT INTO `goals_annotations` VALUES (4,154);
INSERT INTO `goals_annotations` VALUES (4,155);
INSERT INTO `goals_annotations` VALUES (4,156);
INSERT INTO `goals_annotations` VALUES (4,157);
INSERT INTO `goals_annotations` VALUES (4,158);
INSERT INTO `goals_annotations` VALUES (4,159);
INSERT INTO `goals_annotations` VALUES (5,160);
INSERT INTO `goals_annotations` VALUES (5,161);
INSERT INTO `goals_annotations` VALUES (5,162);
INSERT INTO `goals_annotations` VALUES (5,163);
INSERT INTO `goals_annotations` VALUES (5,164);
INSERT INTO `goals_annotations` VALUES (5,165);
INSERT INTO `goals_annotations` VALUES (5,166);
INSERT INTO `goals_annotations` VALUES (5,167);
INSERT INTO `goals_annotations` VALUES (6,168);
INSERT INTO `goals_annotations` VALUES (6,169);
INSERT INTO `goals_annotations` VALUES (6,170);
INSERT INTO `goals_annotations` VALUES (6,171);
INSERT INTO `goals_annotations` VALUES (6,172);
INSERT INTO `goals_annotations` VALUES (7,173);
INSERT INTO `goals_annotations` VALUES (7,174);
INSERT INTO `goals_annotations` VALUES (7,175);
INSERT INTO `goals_annotations` VALUES (7,176);
INSERT INTO `goals_annotations` VALUES (7,177);
INSERT INTO `goals_annotations` VALUES (7,178);
INSERT INTO `goals_annotations` VALUES (7,179);
INSERT INTO `goals_annotations` VALUES (7,180);
INSERT INTO `goals_annotations` VALUES (8,181);
INSERT INTO `goals_annotations` VALUES (8,182);
INSERT INTO `goals_annotations` VALUES (8,183);
INSERT INTO `goals_annotations` VALUES (8,184);
INSERT INTO `goals_annotations` VALUES (8,185);
INSERT INTO `goals_annotations` VALUES (8,186);
INSERT INTO `goals_annotations` VALUES (9,187);
INSERT INTO `goals_annotations` VALUES (9,188);
INSERT INTO `goals_annotations` VALUES (9,189);
INSERT INTO `goals_annotations` VALUES (9,190);
INSERT INTO `goals_annotations` VALUES (9,191);
INSERT INTO `goals_annotations` VALUES (9,192);
INSERT INTO `goals_annotations` VALUES (10,193);
INSERT INTO `goals_annotations` VALUES (10,194);
INSERT INTO `goals_annotations` VALUES (10,195);
INSERT INTO `goals_annotations` VALUES (10,196);
INSERT INTO `goals_annotations` VALUES (10,197);
INSERT INTO `goals_annotations` VALUES (10,198);
/*!40000 ALTER TABLE `goals_annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `goals_glossary_terms` WRITE;
/*!40000 ALTER TABLE `goals_glossary_terms` DISABLE KEYS */;
/*!40000 ALTER TABLE `goals_glossary_terms` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `goals_goalcontainers` WRITE;
/*!40000 ALTER TABLE `goals_goalcontainers` DISABLE KEYS */;
INSERT INTO `goals_goalcontainers` VALUES (1,'com.rreganjr.requel.project.Project',1);
INSERT INTO `goals_goalcontainers` VALUES (2,'com.rreganjr.requel.project.Project',1);
INSERT INTO `goals_goalcontainers` VALUES (3,'com.rreganjr.requel.project.Project',1);
INSERT INTO `goals_goalcontainers` VALUES (4,'com.rreganjr.requel.project.Project',1);
INSERT INTO `goals_goalcontainers` VALUES (5,'com.rreganjr.requel.project.Project',1);
INSERT INTO `goals_goalcontainers` VALUES (6,'com.rreganjr.requel.project.Project',1);
INSERT INTO `goals_goalcontainers` VALUES (7,'com.rreganjr.requel.project.Project',1);
INSERT INTO `goals_goalcontainers` VALUES (8,'com.rreganjr.requel.project.Project',1);
INSERT INTO `goals_goalcontainers` VALUES (9,'com.rreganjr.requel.project.Project',1);
INSERT INTO `goals_goalcontainers` VALUES (9,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `goals_goalcontainers` VALUES (10,'com.rreganjr.requel.project.NonUserStakeholder',3);
INSERT INTO `goals_goalcontainers` VALUES (10,'com.rreganjr.requel.project.Project',1);
INSERT INTO `goals_goalcontainers` VALUES (10,'com.rreganjr.requel.project.UseCase',2);
/*!40000 ALTER TABLE `goals_goalcontainers` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `organizations` WRITE;
/*!40000 ALTER TABLE `organizations` DISABLE KEYS */;
INSERT INTO `organizations` VALUES (1,'Requel',1);
INSERT INTO `organizations` VALUES (2,'Harvard University',1);
/*!40000 ALTER TABLE `organizations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `pods` WRITE;
/*!40000 ALTER TABLE `pods` DISABLE KEYS */;
INSERT INTO `pods` VALUES ('com.rreganjr.requel.project.impl.ProjectImpl',1,'2009-01-04 00:00:00','Requel','A collaborative system for the elicitation and analysis of requirements  facilitated by automated assistants.',21,'New',1,2);
/*!40000 ALTER TABLE `pods` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `pods_glossary_terms` WRITE;
/*!40000 ALTER TABLE `pods_glossary_terms` DISABLE KEYS */;
/*!40000 ALTER TABLE `pods_glossary_terms` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `pods_scenarios` WRITE;
/*!40000 ALTER TABLE `pods_scenarios` DISABLE KEYS */;
/*!40000 ALTER TABLE `pods_scenarios` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `position_issue` WRITE;
/*!40000 ALTER TABLE `position_issue` DISABLE KEYS */;
INSERT INTO `position_issue` VALUES (1,1);
INSERT INTO `position_issue` VALUES (2,1);
INSERT INTO `position_issue` VALUES (3,1);
INSERT INTO `position_issue` VALUES (3,2);
INSERT INTO `position_issue` VALUES (4,2);
INSERT INTO `position_issue` VALUES (5,2);
INSERT INTO `position_issue` VALUES (3,3);
INSERT INTO `position_issue` VALUES (6,3);
INSERT INTO `position_issue` VALUES (7,3);
INSERT INTO `position_issue` VALUES (3,4);
INSERT INTO `position_issue` VALUES (8,4);
INSERT INTO `position_issue` VALUES (9,4);
INSERT INTO `position_issue` VALUES (3,5);
INSERT INTO `position_issue` VALUES (10,5);
INSERT INTO `position_issue` VALUES (11,5);
INSERT INTO `position_issue` VALUES (3,6);
INSERT INTO `position_issue` VALUES (12,6);
INSERT INTO `position_issue` VALUES (13,6);
INSERT INTO `position_issue` VALUES (3,7);
INSERT INTO `position_issue` VALUES (14,7);
INSERT INTO `position_issue` VALUES (15,7);
INSERT INTO `position_issue` VALUES (3,8);
INSERT INTO `position_issue` VALUES (16,8);
INSERT INTO `position_issue` VALUES (17,8);
INSERT INTO `position_issue` VALUES (3,9);
INSERT INTO `position_issue` VALUES (18,9);
INSERT INTO `position_issue` VALUES (19,9);
INSERT INTO `position_issue` VALUES (3,10);
INSERT INTO `position_issue` VALUES (20,10);
INSERT INTO `position_issue` VALUES (21,10);
INSERT INTO `position_issue` VALUES (3,11);
INSERT INTO `position_issue` VALUES (22,11);
INSERT INTO `position_issue` VALUES (23,11);
INSERT INTO `position_issue` VALUES (3,12);
INSERT INTO `position_issue` VALUES (24,12);
INSERT INTO `position_issue` VALUES (25,12);
INSERT INTO `position_issue` VALUES (3,13);
INSERT INTO `position_issue` VALUES (26,13);
INSERT INTO `position_issue` VALUES (27,13);
INSERT INTO `position_issue` VALUES (3,14);
INSERT INTO `position_issue` VALUES (28,14);
INSERT INTO `position_issue` VALUES (29,14);
INSERT INTO `position_issue` VALUES (3,15);
INSERT INTO `position_issue` VALUES (30,15);
INSERT INTO `position_issue` VALUES (31,15);
INSERT INTO `position_issue` VALUES (3,16);
INSERT INTO `position_issue` VALUES (32,16);
INSERT INTO `position_issue` VALUES (33,16);
INSERT INTO `position_issue` VALUES (3,17);
INSERT INTO `position_issue` VALUES (34,17);
INSERT INTO `position_issue` VALUES (35,17);
INSERT INTO `position_issue` VALUES (3,18);
INSERT INTO `position_issue` VALUES (36,18);
INSERT INTO `position_issue` VALUES (37,18);
INSERT INTO `position_issue` VALUES (3,19);
INSERT INTO `position_issue` VALUES (38,19);
INSERT INTO `position_issue` VALUES (39,19);
INSERT INTO `position_issue` VALUES (3,20);
INSERT INTO `position_issue` VALUES (40,20);
INSERT INTO `position_issue` VALUES (41,20);
INSERT INTO `position_issue` VALUES (3,21);
INSERT INTO `position_issue` VALUES (42,21);
INSERT INTO `position_issue` VALUES (43,21);
INSERT INTO `position_issue` VALUES (3,22);
INSERT INTO `position_issue` VALUES (44,22);
INSERT INTO `position_issue` VALUES (45,22);
INSERT INTO `position_issue` VALUES (3,23);
INSERT INTO `position_issue` VALUES (46,23);
INSERT INTO `position_issue` VALUES (47,23);
INSERT INTO `position_issue` VALUES (3,24);
INSERT INTO `position_issue` VALUES (48,24);
INSERT INTO `position_issue` VALUES (49,24);
INSERT INTO `position_issue` VALUES (3,25);
INSERT INTO `position_issue` VALUES (50,25);
INSERT INTO `position_issue` VALUES (51,25);
INSERT INTO `position_issue` VALUES (3,26);
INSERT INTO `position_issue` VALUES (52,26);
INSERT INTO `position_issue` VALUES (53,26);
INSERT INTO `position_issue` VALUES (3,27);
INSERT INTO `position_issue` VALUES (54,27);
INSERT INTO `position_issue` VALUES (55,27);
INSERT INTO `position_issue` VALUES (3,28);
INSERT INTO `position_issue` VALUES (56,28);
INSERT INTO `position_issue` VALUES (57,28);
INSERT INTO `position_issue` VALUES (3,29);
INSERT INTO `position_issue` VALUES (58,29);
INSERT INTO `position_issue` VALUES (59,29);
INSERT INTO `position_issue` VALUES (3,30);
INSERT INTO `position_issue` VALUES (60,30);
INSERT INTO `position_issue` VALUES (61,30);
INSERT INTO `position_issue` VALUES (3,31);
INSERT INTO `position_issue` VALUES (62,31);
INSERT INTO `position_issue` VALUES (63,31);
INSERT INTO `position_issue` VALUES (3,32);
INSERT INTO `position_issue` VALUES (64,32);
INSERT INTO `position_issue` VALUES (65,32);
INSERT INTO `position_issue` VALUES (3,33);
INSERT INTO `position_issue` VALUES (66,33);
INSERT INTO `position_issue` VALUES (67,33);
INSERT INTO `position_issue` VALUES (3,34);
INSERT INTO `position_issue` VALUES (68,34);
INSERT INTO `position_issue` VALUES (69,34);
INSERT INTO `position_issue` VALUES (3,35);
INSERT INTO `position_issue` VALUES (70,35);
INSERT INTO `position_issue` VALUES (71,35);
INSERT INTO `position_issue` VALUES (3,36);
INSERT INTO `position_issue` VALUES (72,36);
INSERT INTO `position_issue` VALUES (73,36);
INSERT INTO `position_issue` VALUES (3,37);
INSERT INTO `position_issue` VALUES (74,37);
INSERT INTO `position_issue` VALUES (75,37);
INSERT INTO `position_issue` VALUES (3,38);
INSERT INTO `position_issue` VALUES (76,38);
INSERT INTO `position_issue` VALUES (77,38);
INSERT INTO `position_issue` VALUES (3,39);
INSERT INTO `position_issue` VALUES (78,39);
INSERT INTO `position_issue` VALUES (79,39);
INSERT INTO `position_issue` VALUES (3,40);
INSERT INTO `position_issue` VALUES (80,40);
INSERT INTO `position_issue` VALUES (81,40);
INSERT INTO `position_issue` VALUES (3,41);
INSERT INTO `position_issue` VALUES (82,41);
INSERT INTO `position_issue` VALUES (83,41);
INSERT INTO `position_issue` VALUES (3,42);
INSERT INTO `position_issue` VALUES (84,42);
INSERT INTO `position_issue` VALUES (85,42);
INSERT INTO `position_issue` VALUES (3,43);
INSERT INTO `position_issue` VALUES (86,43);
INSERT INTO `position_issue` VALUES (87,43);
INSERT INTO `position_issue` VALUES (3,44);
INSERT INTO `position_issue` VALUES (88,44);
INSERT INTO `position_issue` VALUES (89,44);
INSERT INTO `position_issue` VALUES (3,45);
INSERT INTO `position_issue` VALUES (90,45);
INSERT INTO `position_issue` VALUES (91,45);
INSERT INTO `position_issue` VALUES (3,46);
INSERT INTO `position_issue` VALUES (92,46);
INSERT INTO `position_issue` VALUES (93,46);
INSERT INTO `position_issue` VALUES (3,47);
INSERT INTO `position_issue` VALUES (94,47);
INSERT INTO `position_issue` VALUES (95,47);
INSERT INTO `position_issue` VALUES (3,48);
INSERT INTO `position_issue` VALUES (96,48);
INSERT INTO `position_issue` VALUES (97,48);
INSERT INTO `position_issue` VALUES (3,49);
INSERT INTO `position_issue` VALUES (98,49);
INSERT INTO `position_issue` VALUES (99,49);
INSERT INTO `position_issue` VALUES (3,50);
INSERT INTO `position_issue` VALUES (100,50);
INSERT INTO `position_issue` VALUES (101,50);
INSERT INTO `position_issue` VALUES (3,51);
INSERT INTO `position_issue` VALUES (102,51);
INSERT INTO `position_issue` VALUES (103,51);
INSERT INTO `position_issue` VALUES (3,52);
INSERT INTO `position_issue` VALUES (104,52);
INSERT INTO `position_issue` VALUES (105,52);
INSERT INTO `position_issue` VALUES (3,53);
INSERT INTO `position_issue` VALUES (106,53);
INSERT INTO `position_issue` VALUES (107,53);
INSERT INTO `position_issue` VALUES (3,54);
INSERT INTO `position_issue` VALUES (108,54);
INSERT INTO `position_issue` VALUES (109,54);
INSERT INTO `position_issue` VALUES (3,55);
INSERT INTO `position_issue` VALUES (110,55);
INSERT INTO `position_issue` VALUES (111,55);
INSERT INTO `position_issue` VALUES (3,56);
INSERT INTO `position_issue` VALUES (112,56);
INSERT INTO `position_issue` VALUES (113,56);
INSERT INTO `position_issue` VALUES (3,57);
INSERT INTO `position_issue` VALUES (114,57);
INSERT INTO `position_issue` VALUES (115,57);
INSERT INTO `position_issue` VALUES (3,58);
INSERT INTO `position_issue` VALUES (116,58);
INSERT INTO `position_issue` VALUES (117,58);
INSERT INTO `position_issue` VALUES (3,59);
INSERT INTO `position_issue` VALUES (118,59);
INSERT INTO `position_issue` VALUES (119,59);
INSERT INTO `position_issue` VALUES (3,60);
INSERT INTO `position_issue` VALUES (120,60);
INSERT INTO `position_issue` VALUES (121,60);
INSERT INTO `position_issue` VALUES (3,61);
INSERT INTO `position_issue` VALUES (122,61);
INSERT INTO `position_issue` VALUES (123,61);
INSERT INTO `position_issue` VALUES (3,62);
INSERT INTO `position_issue` VALUES (124,62);
INSERT INTO `position_issue` VALUES (125,62);
INSERT INTO `position_issue` VALUES (3,63);
INSERT INTO `position_issue` VALUES (126,63);
INSERT INTO `position_issue` VALUES (127,63);
INSERT INTO `position_issue` VALUES (3,64);
INSERT INTO `position_issue` VALUES (128,64);
INSERT INTO `position_issue` VALUES (129,64);
INSERT INTO `position_issue` VALUES (3,65);
INSERT INTO `position_issue` VALUES (130,65);
INSERT INTO `position_issue` VALUES (131,65);
INSERT INTO `position_issue` VALUES (3,66);
INSERT INTO `position_issue` VALUES (132,66);
INSERT INTO `position_issue` VALUES (133,66);
INSERT INTO `position_issue` VALUES (3,67);
INSERT INTO `position_issue` VALUES (134,67);
INSERT INTO `position_issue` VALUES (135,67);
INSERT INTO `position_issue` VALUES (3,68);
INSERT INTO `position_issue` VALUES (136,68);
INSERT INTO `position_issue` VALUES (137,68);
INSERT INTO `position_issue` VALUES (3,69);
INSERT INTO `position_issue` VALUES (138,69);
INSERT INTO `position_issue` VALUES (139,69);
INSERT INTO `position_issue` VALUES (3,70);
INSERT INTO `position_issue` VALUES (140,70);
INSERT INTO `position_issue` VALUES (141,70);
INSERT INTO `position_issue` VALUES (3,71);
INSERT INTO `position_issue` VALUES (142,71);
INSERT INTO `position_issue` VALUES (143,71);
INSERT INTO `position_issue` VALUES (3,72);
INSERT INTO `position_issue` VALUES (144,72);
INSERT INTO `position_issue` VALUES (145,72);
INSERT INTO `position_issue` VALUES (3,73);
INSERT INTO `position_issue` VALUES (146,73);
INSERT INTO `position_issue` VALUES (147,73);
INSERT INTO `position_issue` VALUES (3,74);
INSERT INTO `position_issue` VALUES (148,74);
INSERT INTO `position_issue` VALUES (149,74);
INSERT INTO `position_issue` VALUES (3,75);
INSERT INTO `position_issue` VALUES (150,75);
INSERT INTO `position_issue` VALUES (151,75);
INSERT INTO `position_issue` VALUES (3,76);
INSERT INTO `position_issue` VALUES (152,76);
INSERT INTO `position_issue` VALUES (153,76);
INSERT INTO `position_issue` VALUES (3,77);
INSERT INTO `position_issue` VALUES (154,77);
INSERT INTO `position_issue` VALUES (155,77);
INSERT INTO `position_issue` VALUES (3,78);
INSERT INTO `position_issue` VALUES (156,78);
INSERT INTO `position_issue` VALUES (157,78);
INSERT INTO `position_issue` VALUES (3,79);
INSERT INTO `position_issue` VALUES (158,79);
INSERT INTO `position_issue` VALUES (159,79);
INSERT INTO `position_issue` VALUES (3,80);
INSERT INTO `position_issue` VALUES (160,80);
INSERT INTO `position_issue` VALUES (161,80);
INSERT INTO `position_issue` VALUES (3,81);
INSERT INTO `position_issue` VALUES (162,81);
INSERT INTO `position_issue` VALUES (163,81);
INSERT INTO `position_issue` VALUES (3,82);
INSERT INTO `position_issue` VALUES (164,82);
INSERT INTO `position_issue` VALUES (165,82);
INSERT INTO `position_issue` VALUES (3,83);
INSERT INTO `position_issue` VALUES (166,83);
INSERT INTO `position_issue` VALUES (167,83);
INSERT INTO `position_issue` VALUES (3,84);
INSERT INTO `position_issue` VALUES (168,84);
INSERT INTO `position_issue` VALUES (169,84);
INSERT INTO `position_issue` VALUES (3,85);
INSERT INTO `position_issue` VALUES (170,85);
INSERT INTO `position_issue` VALUES (171,85);
INSERT INTO `position_issue` VALUES (3,86);
INSERT INTO `position_issue` VALUES (172,86);
INSERT INTO `position_issue` VALUES (173,86);
INSERT INTO `position_issue` VALUES (3,87);
INSERT INTO `position_issue` VALUES (174,87);
INSERT INTO `position_issue` VALUES (175,87);
INSERT INTO `position_issue` VALUES (3,88);
INSERT INTO `position_issue` VALUES (176,88);
INSERT INTO `position_issue` VALUES (177,88);
INSERT INTO `position_issue` VALUES (3,89);
INSERT INTO `position_issue` VALUES (178,89);
INSERT INTO `position_issue` VALUES (179,89);
INSERT INTO `position_issue` VALUES (3,90);
INSERT INTO `position_issue` VALUES (180,90);
INSERT INTO `position_issue` VALUES (181,90);
INSERT INTO `position_issue` VALUES (3,91);
INSERT INTO `position_issue` VALUES (182,91);
INSERT INTO `position_issue` VALUES (183,91);
INSERT INTO `position_issue` VALUES (3,92);
INSERT INTO `position_issue` VALUES (184,92);
INSERT INTO `position_issue` VALUES (185,92);
INSERT INTO `position_issue` VALUES (3,93);
INSERT INTO `position_issue` VALUES (186,93);
INSERT INTO `position_issue` VALUES (187,93);
INSERT INTO `position_issue` VALUES (3,94);
INSERT INTO `position_issue` VALUES (188,94);
INSERT INTO `position_issue` VALUES (189,94);
INSERT INTO `position_issue` VALUES (3,95);
INSERT INTO `position_issue` VALUES (190,95);
INSERT INTO `position_issue` VALUES (191,95);
INSERT INTO `position_issue` VALUES (3,96);
INSERT INTO `position_issue` VALUES (192,96);
INSERT INTO `position_issue` VALUES (193,96);
INSERT INTO `position_issue` VALUES (3,97);
INSERT INTO `position_issue` VALUES (194,97);
INSERT INTO `position_issue` VALUES (195,97);
INSERT INTO `position_issue` VALUES (3,98);
INSERT INTO `position_issue` VALUES (196,98);
INSERT INTO `position_issue` VALUES (197,98);
INSERT INTO `position_issue` VALUES (3,99);
INSERT INTO `position_issue` VALUES (198,99);
INSERT INTO `position_issue` VALUES (199,99);
INSERT INTO `position_issue` VALUES (3,100);
INSERT INTO `position_issue` VALUES (200,100);
INSERT INTO `position_issue` VALUES (201,100);
INSERT INTO `position_issue` VALUES (3,101);
INSERT INTO `position_issue` VALUES (202,101);
INSERT INTO `position_issue` VALUES (203,101);
INSERT INTO `position_issue` VALUES (3,102);
INSERT INTO `position_issue` VALUES (204,102);
INSERT INTO `position_issue` VALUES (205,102);
INSERT INTO `position_issue` VALUES (3,103);
INSERT INTO `position_issue` VALUES (206,103);
INSERT INTO `position_issue` VALUES (207,103);
INSERT INTO `position_issue` VALUES (3,104);
INSERT INTO `position_issue` VALUES (208,104);
INSERT INTO `position_issue` VALUES (209,104);
INSERT INTO `position_issue` VALUES (3,105);
INSERT INTO `position_issue` VALUES (210,105);
INSERT INTO `position_issue` VALUES (211,105);
INSERT INTO `position_issue` VALUES (3,106);
INSERT INTO `position_issue` VALUES (212,106);
INSERT INTO `position_issue` VALUES (213,106);
INSERT INTO `position_issue` VALUES (214,107);
INSERT INTO `position_issue` VALUES (215,107);
INSERT INTO `position_issue` VALUES (216,107);
INSERT INTO `position_issue` VALUES (217,107);
INSERT INTO `position_issue` VALUES (218,107);
INSERT INTO `position_issue` VALUES (219,108);
INSERT INTO `position_issue` VALUES (220,108);
INSERT INTO `position_issue` VALUES (221,108);
INSERT INTO `position_issue` VALUES (222,108);
INSERT INTO `position_issue` VALUES (223,108);
INSERT INTO `position_issue` VALUES (224,109);
INSERT INTO `position_issue` VALUES (225,109);
INSERT INTO `position_issue` VALUES (226,109);
INSERT INTO `position_issue` VALUES (227,109);
INSERT INTO `position_issue` VALUES (228,109);
INSERT INTO `position_issue` VALUES (229,131);
INSERT INTO `position_issue` VALUES (230,132);
INSERT INTO `position_issue` VALUES (231,132);
INSERT INTO `position_issue` VALUES (232,132);
INSERT INTO `position_issue` VALUES (233,132);
INSERT INTO `position_issue` VALUES (234,138);
INSERT INTO `position_issue` VALUES (235,138);
INSERT INTO `position_issue` VALUES (236,138);
INSERT INTO `position_issue` VALUES (237,138);
INSERT INTO `position_issue` VALUES (238,138);
INSERT INTO `position_issue` VALUES (234,139);
INSERT INTO `position_issue` VALUES (235,139);
INSERT INTO `position_issue` VALUES (236,139);
INSERT INTO `position_issue` VALUES (237,139);
INSERT INTO `position_issue` VALUES (238,139);
INSERT INTO `position_issue` VALUES (234,140);
INSERT INTO `position_issue` VALUES (235,140);
INSERT INTO `position_issue` VALUES (236,140);
INSERT INTO `position_issue` VALUES (237,140);
INSERT INTO `position_issue` VALUES (238,140);
INSERT INTO `position_issue` VALUES (234,141);
INSERT INTO `position_issue` VALUES (235,141);
INSERT INTO `position_issue` VALUES (236,141);
INSERT INTO `position_issue` VALUES (237,141);
INSERT INTO `position_issue` VALUES (238,141);
INSERT INTO `position_issue` VALUES (239,142);
INSERT INTO `position_issue` VALUES (240,142);
INSERT INTO `position_issue` VALUES (241,142);
INSERT INTO `position_issue` VALUES (242,142);
INSERT INTO `position_issue` VALUES (243,153);
INSERT INTO `position_issue` VALUES (244,154);
INSERT INTO `position_issue` VALUES (245,154);
INSERT INTO `position_issue` VALUES (246,154);
INSERT INTO `position_issue` VALUES (247,154);
INSERT INTO `position_issue` VALUES (248,160);
INSERT INTO `position_issue` VALUES (249,161);
INSERT INTO `position_issue` VALUES (250,161);
INSERT INTO `position_issue` VALUES (251,161);
INSERT INTO `position_issue` VALUES (252,161);
INSERT INTO `position_issue` VALUES (253,161);
INSERT INTO `position_issue` VALUES (254,173);
INSERT INTO `position_issue` VALUES (255,173);
INSERT INTO `position_issue` VALUES (256,173);
INSERT INTO `position_issue` VALUES (257,173);
INSERT INTO `position_issue` VALUES (258,173);
INSERT INTO `position_issue` VALUES (259,173);
INSERT INTO `position_issue` VALUES (260,174);
INSERT INTO `position_issue` VALUES (261,174);
INSERT INTO `position_issue` VALUES (262,174);
INSERT INTO `position_issue` VALUES (263,174);
INSERT INTO `position_issue` VALUES (264,174);
INSERT INTO `position_issue` VALUES (265,175);
INSERT INTO `position_issue` VALUES (266,175);
INSERT INTO `position_issue` VALUES (267,175);
INSERT INTO `position_issue` VALUES (268,175);
INSERT INTO `position_issue` VALUES (269,176);
INSERT INTO `position_issue` VALUES (270,176);
INSERT INTO `position_issue` VALUES (271,176);
INSERT INTO `position_issue` VALUES (272,176);
INSERT INTO `position_issue` VALUES (273,176);
INSERT INTO `position_issue` VALUES (274,181);
INSERT INTO `position_issue` VALUES (275,187);
INSERT INTO `position_issue` VALUES (276,187);
INSERT INTO `position_issue` VALUES (277,187);
INSERT INTO `position_issue` VALUES (278,187);
INSERT INTO `position_issue` VALUES (279,193);
INSERT INTO `position_issue` VALUES (280,193);
INSERT INTO `position_issue` VALUES (281,193);
INSERT INTO `position_issue` VALUES (282,193);
INSERT INTO `position_issue` VALUES (283,193);
INSERT INTO `position_issue` VALUES (284,199);
INSERT INTO `position_issue` VALUES (285,204);
INSERT INTO `position_issue` VALUES (286,205);
INSERT INTO `position_issue` VALUES (287,205);
INSERT INTO `position_issue` VALUES (288,205);
INSERT INTO `position_issue` VALUES (289,205);
INSERT INTO `position_issue` VALUES (290,212);
INSERT INTO `position_issue` VALUES (291,213);
INSERT INTO `position_issue` VALUES (292,213);
INSERT INTO `position_issue` VALUES (293,213);
INSERT INTO `position_issue` VALUES (294,213);
INSERT INTO `position_issue` VALUES (295,213);
INSERT INTO `position_issue` VALUES (296,213);
INSERT INTO `position_issue` VALUES (297,214);
INSERT INTO `position_issue` VALUES (298,214);
INSERT INTO `position_issue` VALUES (299,214);
INSERT INTO `position_issue` VALUES (300,214);
INSERT INTO `position_issue` VALUES (301,214);
INSERT INTO `position_issue` VALUES (302,214);
INSERT INTO `position_issue` VALUES (303,214);
INSERT INTO `position_issue` VALUES (304,214);
INSERT INTO `position_issue` VALUES (305,214);
INSERT INTO `position_issue` VALUES (306,214);
INSERT INTO `position_issue` VALUES (307,214);
INSERT INTO `position_issue` VALUES (308,215);
INSERT INTO `position_issue` VALUES (309,215);
INSERT INTO `position_issue` VALUES (310,215);
INSERT INTO `position_issue` VALUES (311,215);
INSERT INTO `position_issue` VALUES (312,215);
INSERT INTO `position_issue` VALUES (313,215);
INSERT INTO `position_issue` VALUES (314,215);
INSERT INTO `position_issue` VALUES (315,215);
INSERT INTO `position_issue` VALUES (316,216);
INSERT INTO `position_issue` VALUES (317,216);
INSERT INTO `position_issue` VALUES (318,216);
INSERT INTO `position_issue` VALUES (319,223);
INSERT INTO `position_issue` VALUES (320,224);
INSERT INTO `position_issue` VALUES (321,224);
INSERT INTO `position_issue` VALUES (322,224);
INSERT INTO `position_issue` VALUES (323,224);
INSERT INTO `position_issue` VALUES (324,224);
INSERT INTO `position_issue` VALUES (325,224);
INSERT INTO `position_issue` VALUES (326,224);
INSERT INTO `position_issue` VALUES (327,224);
INSERT INTO `position_issue` VALUES (328,224);
INSERT INTO `position_issue` VALUES (329,224);
INSERT INTO `position_issue` VALUES (330,224);
INSERT INTO `position_issue` VALUES (331,224);
INSERT INTO `position_issue` VALUES (332,224);
INSERT INTO `position_issue` VALUES (333,224);
INSERT INTO `position_issue` VALUES (334,224);
INSERT INTO `position_issue` VALUES (335,224);
INSERT INTO `position_issue` VALUES (336,224);
INSERT INTO `position_issue` VALUES (337,224);
INSERT INTO `position_issue` VALUES (338,224);
INSERT INTO `position_issue` VALUES (339,224);
INSERT INTO `position_issue` VALUES (340,224);
INSERT INTO `position_issue` VALUES (341,224);
INSERT INTO `position_issue` VALUES (342,224);
INSERT INTO `position_issue` VALUES (343,224);
INSERT INTO `position_issue` VALUES (344,224);
INSERT INTO `position_issue` VALUES (345,225);
INSERT INTO `position_issue` VALUES (346,225);
INSERT INTO `position_issue` VALUES (347,225);
INSERT INTO `position_issue` VALUES (348,225);
INSERT INTO `position_issue` VALUES (349,225);
INSERT INTO `position_issue` VALUES (345,226);
INSERT INTO `position_issue` VALUES (346,226);
INSERT INTO `position_issue` VALUES (347,226);
INSERT INTO `position_issue` VALUES (348,226);
INSERT INTO `position_issue` VALUES (349,226);
INSERT INTO `position_issue` VALUES (350,227);
INSERT INTO `position_issue` VALUES (351,227);
INSERT INTO `position_issue` VALUES (352,227);
INSERT INTO `position_issue` VALUES (353,234);
INSERT INTO `position_issue` VALUES (354,241);
INSERT INTO `position_issue` VALUES (355,242);
INSERT INTO `position_issue` VALUES (356,242);
INSERT INTO `position_issue` VALUES (357,242);
INSERT INTO `position_issue` VALUES (358,242);
INSERT INTO `position_issue` VALUES (359,242);
/*!40000 ALTER TABLE `position_issue` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `positions` WRITE;
/*!40000 ALTER TABLE `positions` DISABLE KEYS */;
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',1,'2025-10-14 00:00:00','Add \"The system\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',2,'2025-10-14 00:00:00','Add \"The system\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.PositionImpl',3,'2025-10-14 00:00:00','Ignore this phrase.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',4,'2025-10-14 00:00:00','Add \"\" Bailey Pet Supply \"\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',5,'2025-10-14 00:00:00','Add \"\" Bailey Pet Supply \"\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',6,'2025-10-14 00:00:00','Add \"\" Virile Mobile \"\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',7,'2025-10-14 00:00:00','Add \"\" Virile Mobile \"\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',8,'2025-10-14 00:00:00','Add \"A team\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',9,'2025-10-14 00:00:00','Add \"A team\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',10,'2025-10-14 00:00:00','Add \"A user\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',11,'2025-10-14 00:00:00','Add \"A user\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',12,'2025-10-14 00:00:00','Add \"Automatted Assistance\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',13,'2025-10-14 00:00:00','Add \"Automatted Assistance\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',14,'2025-10-14 00:00:00','Add \"Collaborative Elicitation\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',15,'2025-10-14 00:00:00','Add \"Collaborative Elicitation\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',16,'2025-10-14 00:00:00','Add \"Dave the system administrator\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',17,'2025-10-14 00:00:00','Add \"Dave the system administrator\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',18,'2025-10-14 00:00:00','Add \"Flexible Process and Deliverables\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',19,'2025-10-14 00:00:00','Add \"Flexible Process and Deliverables\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',20,'2025-10-14 00:00:00','Add \"Improved Understanding\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',21,'2025-10-14 00:00:00','Add \"Improved Understanding\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',22,'2025-10-14 00:00:00','Add \"Mobile Media Portal\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',23,'2025-10-14 00:00:00','Add \"Mobile Media Portal\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',24,'2025-10-14 00:00:00','Add \"Notification\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',25,'2025-10-14 00:00:00','Add \"Notification\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',26,'2025-10-14 00:00:00','Add \"Poor Communication\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',27,'2025-10-14 00:00:00','Add \"Poor Communication\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',28,'2025-10-14 00:00:00','Add \"Project Changes\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',29,'2025-10-14 00:00:00','Add \"Project Changes\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',30,'2025-10-14 00:00:00','Add \"Project users\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',31,'2025-10-14 00:00:00','Add \"Project users\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',32,'2025-10-14 00:00:00','Add \"Purchase Order System\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',33,'2025-10-14 00:00:00','Add \"Purchase Order System\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',34,'2025-10-14 00:00:00','Add \"Requirements\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',35,'2025-10-14 00:00:00','Add \"Requirements\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',36,'2025-10-14 00:00:00','Add \"Rich\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',37,'2025-10-14 00:00:00','Add \"Rich\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',38,'2025-10-14 00:00:00','Add \"Ron\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',39,'2025-10-14 00:00:00','Add \"Ron\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',40,'2025-10-14 00:00:00','Add \"Secure data access\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',41,'2025-10-14 00:00:00','Add \"Secure data access\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',42,'2025-10-14 00:00:00','Add \"System administrator users\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',43,'2025-10-14 00:00:00','Add \"System administrator users\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',44,'2025-10-14 00:00:00','Add \"The user\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',45,'2025-10-14 00:00:00','Add \"The user\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',46,'2025-10-14 00:00:00','Add \"The work\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',47,'2025-10-14 00:00:00','Add \"The work\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',48,'2025-10-14 00:00:00','Add \"Theresa\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',49,'2025-10-14 00:00:00','Add \"Theresa\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',50,'2025-10-14 00:00:00','Add \"Work\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',51,'2025-10-14 00:00:00','Add \"Work\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',52,'2025-10-14 00:00:00','Add \"a change\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',53,'2025-10-14 00:00:00','Add \"a change\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',54,'2025-10-14 00:00:00','Add \"a distinct username and password\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',55,'2025-10-14 00:00:00','Add \"a distinct username and password\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',56,'2025-10-14 00:00:00','Add \"a help message\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',57,'2025-10-14 00:00:00','Add \"a help message\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',58,'2025-10-14 00:00:00','Add \"a link\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',59,'2025-10-14 00:00:00','Add \"a link\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',60,'2025-10-14 00:00:00','Add \"a list\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',61,'2025-10-14 00:00:00','Add \"a list\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',62,'2025-10-14 00:00:00','Add \"a message\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',63,'2025-10-14 00:00:00','Add \"a message\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',64,'2025-10-14 00:00:00','Add \"a new account\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',65,'2025-10-14 00:00:00','Add \"a new account\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',66,'2025-10-14 00:00:00','Add \"a new project\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',67,'2025-10-14 00:00:00','Add \"a new project\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',68,'2025-10-14 00:00:00','Add \"a novice user\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',69,'2025-10-14 00:00:00','Add \"a novice user\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',70,'2025-10-14 00:00:00','Add \"a password\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',71,'2025-10-14 00:00:00','Add \"a password\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',72,'2025-10-14 00:00:00','Add \"a periodic summary\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',73,'2025-10-14 00:00:00','Add \"a periodic summary\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',74,'2025-10-14 00:00:00','Add \"a problem connecting\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',75,'2025-10-14 00:00:00','Add \"a problem connecting\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',76,'2025-10-14 00:00:00','Add \"a project or non-users\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',77,'2025-10-14 00:00:00','Add \"a project or non-users\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',78,'2025-10-14 00:00:00','Add \"a project\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',79,'2025-10-14 00:00:00','Add \"a project\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',80,'2025-10-14 00:00:00','Add \"a short description\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',81,'2025-10-14 00:00:00','Add \"a short description\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',82,'2025-10-14 00:00:00','Add \"a specific requirements elicitation process\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',83,'2025-10-14 00:00:00','Add \"a specific requirements elicitation process\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',84,'2025-10-14 00:00:00','Add \"a stakeholder\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',85,'2025-10-14 00:00:00','Add \"a stakeholder\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',86,'2025-10-14 00:00:00','Add \"a username and password\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',87,'2025-10-14 00:00:00','Add \"a username and password\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',88,'2025-10-14 00:00:00','Add \"a username\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',89,'2025-10-14 00:00:00','Add \"a username\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',90,'2025-10-14 00:00:00','Add \"a web browser\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',91,'2025-10-14 00:00:00','Add \"a web browser\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',92,'2025-10-14 00:00:00','Add \"all project entities\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',93,'2025-10-14 00:00:00','Add \"all project entities\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',94,'2025-10-14 00:00:00','Add \"all projects\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',95,'2025-10-14 00:00:00','Add \"all projects\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',96,'2025-10-14 00:00:00','Add \"all the features\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',97,'2025-10-14 00:00:00','Add \"all the features\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',98,'2025-10-14 00:00:00','Add \"all the stakeholders\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',99,'2025-10-14 00:00:00','Add \"all the stakeholders\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',100,'2025-10-14 00:00:00','Add \"an account\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',101,'2025-10-14 00:00:00','Add \"an account\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',102,'2025-10-14 00:00:00','Add \"an email message\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',103,'2025-10-14 00:00:00','Add \"an email message\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',104,'2025-10-14 00:00:00','Add \"an existing user session\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',105,'2025-10-14 00:00:00','Add \"an existing user session\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',106,'2025-10-14 00:00:00','Add \"an overview\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',107,'2025-10-14 00:00:00','Add \"an overview\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',108,'2025-10-14 00:00:00','Add \"any missing information\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',109,'2025-10-14 00:00:00','Add \"any missing information\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',110,'2025-10-14 00:00:00','Add \"each issue\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',111,'2025-10-14 00:00:00','Add \"each issue\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',112,'2025-10-14 00:00:00','Add \"each solution\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',113,'2025-10-14 00:00:00','Add \"each solution\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',114,'2025-10-14 00:00:00','Add \"email address\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',115,'2025-10-14 00:00:00','Add \"email address\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',116,'2025-10-14 00:00:00','Add \"emails and spreadsheets\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',117,'2025-10-14 00:00:00','Add \"emails and spreadsheets\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',118,'2025-10-14 00:00:00','Add \"every time\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',119,'2025-10-14 00:00:00','Add \"every time\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',120,'2025-10-14 00:00:00','Add \"existing customers\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',121,'2025-10-14 00:00:00','Add \"existing customers\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',122,'2025-10-14 00:00:00','Add \"integrated discussion\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',123,'2025-10-14 00:00:00','Add \"integrated discussion\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',124,'2025-10-14 00:00:00','Add \"issues and notes\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',125,'2025-10-14 00:00:00','Add \"issues and notes\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',126,'2025-10-14 00:00:00','Add \"natural language\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',127,'2025-10-14 00:00:00','Add \"natural language\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',128,'2025-10-14 00:00:00','Add \"non-integrated tools\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',129,'2025-10-14 00:00:00','Add \"non-integrated tools\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',130,'2025-10-14 00:00:00','Add \"non-technical users\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',131,'2025-10-14 00:00:00','Add \"non-technical users\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',132,'2025-10-14 00:00:00','Add \"other users\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',133,'2025-10-14 00:00:00','Add \"other users\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',134,'2025-10-14 00:00:00','Add \"password combination\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',135,'2025-10-14 00:00:00','Add \"password combination\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',136,'2025-10-14 00:00:00','Add \"phone number\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',137,'2025-10-14 00:00:00','Add \"phone number\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',138,'2025-10-14 00:00:00','Add \"project content\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',139,'2025-10-14 00:00:00','Add \"project content\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',140,'2025-10-14 00:00:00','Add \"pros and cons\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',141,'2025-10-14 00:00:00','Add \"pros and cons\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',142,'2025-10-14 00:00:00','Add \"recent project changes\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',143,'2025-10-14 00:00:00','Add \"recent project changes\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',144,'2025-10-14 00:00:00','Add \"specialized skills or training\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',145,'2025-10-14 00:00:00','Add \"specialized skills or training\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',146,'2025-10-14 00:00:00','Add \"specialized skills\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',147,'2025-10-14 00:00:00','Add \"specialized skills\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',148,'2025-10-14 00:00:00','Add \"state multiple solutions\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',149,'2025-10-14 00:00:00','Add \"state multiple solutions\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',150,'2025-10-14 00:00:00','Add \"the \" Create Project \" option\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',151,'2025-10-14 00:00:00','Add \"the \" Create Project \" option\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',152,'2025-10-14 00:00:00','Add \"the Requel system address\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',153,'2025-10-14 00:00:00','Add \"the Requel system address\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',154,'2025-10-14 00:00:00','Add \"the Requel system\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',155,'2025-10-14 00:00:00','Add \"the Requel system\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',156,'2025-10-14 00:00:00','Add \"the customer\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',157,'2025-10-14 00:00:00','Add \"the customer\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',158,'2025-10-14 00:00:00','Add \"the description\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',159,'2025-10-14 00:00:00','Add \"the description\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',160,'2025-10-14 00:00:00','Add \"the elements\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',161,'2025-10-14 00:00:00','Add \"the elements\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',162,'2025-10-14 00:00:00','Add \"the help option\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',163,'2025-10-14 00:00:00','Add \"the help option\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',164,'2025-10-14 00:00:00','Add \"the information\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',165,'2025-10-14 00:00:00','Add \"the information\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',166,'2025-10-14 00:00:00','Add \"the list\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',167,'2025-10-14 00:00:00','Add \"the list\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',168,'2025-10-14 00:00:00','Add \"the login screen\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',169,'2025-10-14 00:00:00','Add \"the login screen\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',170,'2025-10-14 00:00:00','Add \"the members\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',171,'2025-10-14 00:00:00','Add \"the members\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',172,'2025-10-14 00:00:00','Add \"the need\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',173,'2025-10-14 00:00:00','Add \"the need\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',174,'2025-10-14 00:00:00','Add \"the new project interface\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',175,'2025-10-14 00:00:00','Add \"the new project interface\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',176,'2025-10-14 00:00:00','Add \"the new user setup interface\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',177,'2025-10-14 00:00:00','Add \"the new user setup interface\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',178,'2025-10-14 00:00:00','Add \"the notifications\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',179,'2025-10-14 00:00:00','Add \"the notifications\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',180,'2025-10-14 00:00:00','Add \"the novice user interface\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',181,'2025-10-14 00:00:00','Add \"the novice user interface\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',182,'2025-10-14 00:00:00','Add \"the personal information Dave\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',183,'2025-10-14 00:00:00','Add \"the personal information Dave\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',184,'2025-10-14 00:00:00','Add \"the project elements\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',185,'2025-10-14 00:00:00','Add \"the project elements\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',186,'2025-10-14 00:00:00','Add \"the project name\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',187,'2025-10-14 00:00:00','Add \"the project name\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',188,'2025-10-14 00:00:00','Add \"the project navigation interface\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',189,'2025-10-14 00:00:00','Add \"the project navigation interface\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',190,'2025-10-14 00:00:00','Add \"the project\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',191,'2025-10-14 00:00:00','Add \"the project\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',192,'2025-10-14 00:00:00','Add \"the project\'s entities\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',193,'2025-10-14 00:00:00','Add \"the project\'s entities\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',194,'2025-10-14 00:00:00','Add \"the requirements part\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',195,'2025-10-14 00:00:00','Add \"the requirements part\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',196,'2025-10-14 00:00:00','Add \"the requirements\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',197,'2025-10-14 00:00:00','Add \"the requirements\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',198,'2025-10-14 00:00:00','Add \"the stakeholders\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',199,'2025-10-14 00:00:00','Add \"the stakeholders\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',200,'2025-10-14 00:00:00','Add \"the student\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',201,'2025-10-14 00:00:00','Add \"the student\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',202,'2025-10-14 00:00:00','Add \"the system administrator\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',203,'2025-10-14 00:00:00','Add \"the system administrator\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',204,'2025-10-14 00:00:00','Add \"the system interface\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',205,'2025-10-14 00:00:00','Add \"the system interface\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',206,'2025-10-14 00:00:00','Add \"the system level user permissions\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',207,'2025-10-14 00:00:00','Add \"the system level user permissions\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',208,'2025-10-14 00:00:00','Add \"the thesis\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',209,'2025-10-14 00:00:00','Add \"the thesis\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',210,'2025-10-14 00:00:00','Add \"the username\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',211,'2025-10-14 00:00:00','Add \"the username\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',212,'2025-10-14 00:00:00','Add \"unauthorized access\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',213,'2025-10-14 00:00:00','Add \"unauthorized access\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',214,'2025-10-14 00:00:00','Change the word \"be\" to \"be well\".',1,'be well',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',215,'2025-10-14 00:00:00','Change the word \"be\" to \"hang around\".',1,'hang around',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',216,'2025-10-14 00:00:00','Change the word \"be\" to \"make sense\".',1,'make sense',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',217,'2025-10-14 00:00:00','Change the word \"be\" to \"moon about\".',1,'moon about',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',218,'2025-10-14 00:00:00','Change the word \"be\" to \"stick by\".',1,'stick by',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',219,'2025-10-14 00:00:00','Change the word \"interacts\" to \"collude\".',1,'collude',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',220,'2025-10-14 00:00:00','Change the word \"interacts\" to \"interact\".',1,'interact',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',221,'2025-10-14 00:00:00','Change the word \"interacts\" to \"keep in line\".',1,'keep in line',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',222,'2025-10-14 00:00:00','Change the word \"interacts\" to \"marginalise\".',1,'marginalise',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',223,'2025-10-14 00:00:00','Change the word \"interacts\" to \"transact\".',1,'transact',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',224,'2025-10-14 00:00:00','Change the word \"part\" to \"component part\".',1,'component part',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',225,'2025-10-14 00:00:00','Change the word \"part\" to \"language unit\".',1,'language unit',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',226,'2025-10-14 00:00:00','Change the word \"part\" to \"linguistic unit\".',1,'linguistic unit',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',227,'2025-10-14 00:00:00','Change the word \"part\" to \"residuum\".',1,'residuum',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',228,'2025-10-14 00:00:00','Change the word \"part\" to \"subpart\".',1,'subpart',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',229,'2025-10-14 00:00:00','Add \"null\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',230,'2025-10-14 00:00:00','Add \"Automatted\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',231,'2025-10-14 00:00:00','Add \"Automatted\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',232,'2025-10-14 00:00:00','Add \"Automatted\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',233,'2025-10-14 00:00:00','Change the word \"Automatted\" to \"automated\".',1,'automated',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',234,'2025-10-14 00:00:00','Change the word \"create\" to \"beget\".',1,'beget',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',235,'2025-10-14 00:00:00','Change the word \"create\" to \"father\".',1,'father',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',236,'2025-10-14 00:00:00','Change the word \"create\" to \"procreate\".',1,'procreate',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',237,'2025-10-14 00:00:00','Change the word \"create\" to \"sire\".',1,'sire',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',238,'2025-10-14 00:00:00','Change the word \"create\" to \"spume\".',1,'spume',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',239,'2025-10-14 00:00:00','Add \"stakeholders\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',240,'2025-10-14 00:00:00','Add \"stakeholders\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',241,'2025-10-14 00:00:00','Add \"stakeholders\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',242,'2025-10-14 00:00:00','Change the word \"stakeholders\" to \"stakeholder\".',1,'stakeholder',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',243,'2025-10-14 00:00:00','Add \"null\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',244,'2025-10-14 00:00:00','Add \"non-technical\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',245,'2025-10-14 00:00:00','Add \"non-technical\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',246,'2025-10-14 00:00:00','Add \"non-technical\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',247,'2025-10-14 00:00:00','Change the word \"non-technical\" to \"nontechnical\".',1,'nontechnical',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',248,'2025-10-14 00:00:00','Add \"null\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',249,'2025-10-14 00:00:00','Change the word \"language\" to \"dead language\".',1,'dead language',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',250,'2025-10-14 00:00:00','Change the word \"language\" to \"linguistic communication\".',1,'linguistic communication',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',251,'2025-10-14 00:00:00','Change the word \"language\" to \"sign language\".',1,'sign language',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',252,'2025-10-14 00:00:00','Change the word \"language\" to \"signing\".',1,'signing',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',253,'2025-10-14 00:00:00','Change the word \"language\" to \"source language\".',1,'source language',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',254,'2025-10-14 00:00:00','Add \"emails\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',255,'2025-10-14 00:00:00','Add \"emails\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',256,'2025-10-14 00:00:00','Add \"emails\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',257,'2025-10-14 00:00:00','Change the word \"emails\" to \"email\".',1,'email',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',258,'2025-10-14 00:00:00','Change the word \"emails\" to \"mails\".',1,'mails',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',259,'2025-10-14 00:00:00','Change the word \"emails\" to \"remails\".',1,'remails',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',260,'2025-10-14 00:00:00','Change the word \"improves\" to \"bushel\".',1,'bushel',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',261,'2025-10-14 00:00:00','Change the word \"improves\" to \"emend\".',1,'emend',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',262,'2025-10-14 00:00:00','Change the word \"improves\" to \"fancify\".',1,'fancify',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',263,'2025-10-14 00:00:00','Change the word \"improves\" to \"furbish up\".',1,'furbish up',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',264,'2025-10-14 00:00:00','Change the word \"improves\" to \"put right\".',1,'put right',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',265,'2025-10-14 00:00:00','Add \"non-integrated\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',266,'2025-10-14 00:00:00','Add \"non-integrated\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',267,'2025-10-14 00:00:00','Add \"non-integrated\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',268,'2025-10-14 00:00:00','Change the word \"non-integrated\" to \"nonintegrated\".',1,'nonintegrated',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',269,'2025-10-14 00:00:00','Change the word \"tools\" to \"abrader\".',1,'abrader',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',270,'2025-10-14 00:00:00','Change the word \"tools\" to \"cutting implement\".',1,'cutting implement',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',271,'2025-10-14 00:00:00','Change the word \"tools\" to \"eolith\".',1,'eolith',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',272,'2025-10-14 00:00:00','Change the word \"tools\" to \"garden tool\".',1,'garden tool',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',273,'2025-10-14 00:00:00','Change the word \"tools\" to \"lawn tool\".',1,'lawn tool',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',274,'2025-10-14 00:00:00','Add \"null\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',275,'2025-10-14 00:00:00','Add \"non-users\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',276,'2025-10-14 00:00:00','Add \"non-users\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',277,'2025-10-14 00:00:00','Add \"non-users\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',278,'2025-10-14 00:00:00','Change the word \"non-users\" to \"nonusers\".',1,'nonusers',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',279,'2025-10-14 00:00:00','Change the word \"work\" to \"heavy lifting\".',1,'heavy lifting',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',280,'2025-10-14 00:00:00','Change the word \"work\" to \"housewifery\".',1,'housewifery',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',281,'2025-10-14 00:00:00','Change the word \"work\" to \"lavation\".',1,'lavation',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',282,'2025-10-14 00:00:00','Change the word \"work\" to \"polishing\".',1,'polishing',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',283,'2025-10-14 00:00:00','Change the word \"work\" to \"shining\".',1,'shining',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',284,'2009-01-20 00:00:00',NULL,1,'hello there',1);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',285,'2025-10-14 00:00:00','Add \"null\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',286,'2025-10-14 00:00:00','Change the word \"entities\" to \"abstract entity\".',1,'abstract entity',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',287,'2025-10-14 00:00:00','Change the word \"entities\" to \"abstraction\".',1,'abstraction',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',288,'2025-10-14 00:00:00','Change the word \"entities\" to \"physical entity\".',1,'physical entity',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',289,'2025-10-14 00:00:00','Change the word \"entities\" to \"thing\".',1,'thing',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',290,'2025-10-14 00:00:00','Add \"null\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',291,'2025-10-14 00:00:00','Add \"Requel\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',292,'2025-10-14 00:00:00','Add \"Requel\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',293,'2025-10-14 00:00:00','Add \"Requel\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',294,'2025-10-14 00:00:00','Change the word \"Requel\" to \"prequel\".',1,'prequel',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',295,'2025-10-14 00:00:00','Change the word \"Requel\" to \"refuel\".',1,'refuel',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',296,'2025-10-14 00:00:00','Change the word \"Requel\" to \"sequel\".',1,'sequel',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',297,'2025-10-14 00:00:00','Add \"login\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',298,'2025-10-14 00:00:00','Add \"login\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',299,'2025-10-14 00:00:00','Add \"login\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',300,'2025-10-14 00:00:00','Change the word \"login\" to \"log in\".',1,'log in',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',301,'2025-10-14 00:00:00','Change the word \"login\" to \"log-in\".',1,'log-in',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',302,'2025-10-14 00:00:00','Change the word \"login\" to \"logan\".',1,'logan',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',303,'2025-10-14 00:00:00','Change the word \"login\" to \"logia\".',1,'logia',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',304,'2025-10-14 00:00:00','Change the word \"login\" to \"logic\".',1,'logic',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',305,'2025-10-14 00:00:00','Change the word \"login\" to \"logie\".',1,'logie',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',306,'2025-10-14 00:00:00','Change the word \"login\" to \"logion\".',1,'logion',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',307,'2025-10-14 00:00:00','Change the word \"login\" to \"loin\".',1,'loin',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',308,'2025-10-14 00:00:00','Add \"n’t\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',309,'2025-10-14 00:00:00','Add \"n’t\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',310,'2025-10-14 00:00:00','Add \"n’t\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',311,'2025-10-14 00:00:00','Change the word \"n’t\" to \"net\".',1,'net',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',312,'2025-10-14 00:00:00','Change the word \"n’t\" to \"nit\".',1,'nit',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',313,'2025-10-14 00:00:00','Change the word \"n’t\" to \"not\".',1,'not',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',314,'2025-10-14 00:00:00','Change the word \"n’t\" to \"nt\".',1,'nt',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',315,'2025-10-14 00:00:00','Change the word \"n’t\" to \"nut\".',1,'nut',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',316,'2025-10-14 00:00:00','Add \"username\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',317,'2025-10-14 00:00:00','Add \"username\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',318,'2025-10-14 00:00:00','Add \"username\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',319,'2025-10-14 00:00:00','Add \"null\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',320,'2025-10-14 00:00:00','Add \"Dave\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',321,'2025-10-14 00:00:00','Add \"Dave\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',322,'2025-10-14 00:00:00','Add \"Dave\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',323,'2025-10-14 00:00:00','Change the word \"Dave\" to \"aave\".',1,'aave',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',324,'2025-10-14 00:00:00','Change the word \"Dave\" to \"ave\".',1,'ave',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',325,'2025-10-14 00:00:00','Change the word \"Dave\" to \"cave\".',1,'cave',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',326,'2025-10-14 00:00:00','Change the word \"Dave\" to \"dace\".',1,'dace',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',327,'2025-10-14 00:00:00','Change the word \"Dave\" to \"dale\".',1,'dale',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',328,'2025-10-14 00:00:00','Change the word \"Dave\" to \"dame\".',1,'dame',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',329,'2025-10-14 00:00:00','Change the word \"Dave\" to \"dane\".',1,'dane',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',330,'2025-10-14 00:00:00','Change the word \"Dave\" to \"dare\".',1,'dare',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',331,'2025-10-14 00:00:00','Change the word \"Dave\" to \"date\".',1,'date',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',332,'2025-10-14 00:00:00','Change the word \"Dave\" to \"davy\".',1,'davy',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',333,'2025-10-14 00:00:00','Change the word \"Dave\" to \"daze\".',1,'daze',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',334,'2025-10-14 00:00:00','Change the word \"Dave\" to \"dive\".',1,'dive',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',335,'2025-10-14 00:00:00','Change the word \"Dave\" to \"dove\".',1,'dove',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',336,'2025-10-14 00:00:00','Change the word \"Dave\" to \"eave\".',1,'eave',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',337,'2025-10-14 00:00:00','Change the word \"Dave\" to \"gave\".',1,'gave',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',338,'2025-10-14 00:00:00','Change the word \"Dave\" to \"have\".',1,'have',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',339,'2025-10-14 00:00:00','Change the word \"Dave\" to \"lave\".',1,'lave',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',340,'2025-10-14 00:00:00','Change the word \"Dave\" to \"nave\".',1,'nave',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',341,'2025-10-14 00:00:00','Change the word \"Dave\" to \"pave\".',1,'pave',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',342,'2025-10-14 00:00:00','Change the word \"Dave\" to \"rave\".',1,'rave',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',343,'2025-10-14 00:00:00','Change the word \"Dave\" to \"save\".',1,'save',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',344,'2025-10-14 00:00:00','Change the word \"Dave\" to \"wave\".',1,'wave',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',345,'2025-10-14 00:00:00','Change the word \"connecting\" to \"bring together\".',1,'bring together',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',346,'2025-10-14 00:00:00','Change the word \"connecting\" to \"conjoin\".',1,'conjoin',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',347,'2025-10-14 00:00:00','Change the word \"connecting\" to \"daisy-chain\".',1,'daisy-chain',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',348,'2025-10-14 00:00:00','Change the word \"connecting\" to \"hang together\".',1,'hang together',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',349,'2025-10-14 00:00:00','Change the word \"connecting\" to \"interdepend\".',1,'interdepend',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddActorPosition',350,'2025-10-14 00:00:00','Add \"dave@requel\" as an actor to the project.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',351,'2025-10-14 00:00:00','Add \"dave@requel\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.project.impl.AddGlossaryTermPosition',352,'2025-10-14 00:00:00','Add \"dave@requel\" to the project glossary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',353,'2025-10-14 00:00:00','Add \"null\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.AddWordToDictionaryPosition',354,'2025-10-14 00:00:00','Add \"null\" to the dictionary.',1,NULL,3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',355,'2025-10-14 00:00:00','Change the word \"get\" to \"come into\".',1,'come into',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',356,'2025-10-14 00:00:00','Change the word \"get\" to \"enter upon\".',1,'enter upon',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',357,'2025-10-14 00:00:00','Change the word \"get\" to \"luck into\".',1,'luck into',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',358,'2025-10-14 00:00:00','Change the word \"get\" to \"purchase\".',1,'purchase',3);
INSERT INTO `positions` VALUES ('com.rreganjr.requel.annotation.impl.ChangeSpellingPosition',359,'2025-10-14 00:00:00','Change the word \"get\" to \"win back\".',1,'win back',3);
/*!40000 ALTER TABLE `positions` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `project_annotations` WRITE;
/*!40000 ALTER TABLE `project_annotations` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `reports` WRITE;
/*!40000 ALTER TABLE `reports` DISABLE KEYS */;
INSERT INTO `reports` VALUES (1,'2025-10-14 00:00:00',1,'<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n                <!-- $Id: project2html.xslt,v 1.9 2009/03/06 02:06:45 rregan Exp $\r\n                \r\n                An XSLT for rendering a Requel Project xml file as an html document.\r\n                \r\n                -->\r\n                <xsl:stylesheet version=\"1.0\" xmlns=\"http://www.w3.org/1999/xhtml\"\r\n                xmlns:rp=\"http://www.rreganjr.com/requel\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\r\n                xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\r\n                xmlns:xi=\"http://www.w3.org/2001/XInclude\" exclude-result-prefixes=\"xs xsi rp xsl xi\">\r\n                \r\n                <xsl:output method=\"xml\" encoding=\"UTF-8\" indent=\"yes\" />\r\n                \r\n                <xsl:template match=\"/rp:project\">\r\n                <html>\r\n                <head>\r\n                <title>\r\n                <xsl:value-of select=\"rp:name\" />\r\n                </title>\r\n                <xsl:call-template name=\"css\"/>\r\n                </head>\r\n                <body>\r\n                <div class=\"project\">\r\n                <h1>Requirements for &quot;<xsl:value-of select=\"rp:name\" />&quot;</h1>\r\n                <h5>Client: <xsl:value-of select=\"rp:organization/@name\" /></h5>\r\n                <h5>Revision: <xsl:value-of select=\"@revision\" /></h5>\r\n                <h4>Table of Contents</h4>\r\n                <div class=\"toc\">\r\n                <ul>\r\n                <xsl:if test=\"count(rp:stakeholders/rp:stakeholder) > 0\">\r\n                <li><a href=\"#stakeholders\">Stakeholders</a>\r\n                <ol>\r\n                <xsl:apply-templates select=\"rp:stakeholders/rp:stakeholder\" mode=\"summary\"/>\r\n                </ol>								\r\n                </li>\r\n                </xsl:if>\r\n                <xsl:if test=\"count(rp:teams/rp:team) > 0\">\r\n                <li><a href=\"#teams\">Teams</a>\r\n                <ol>\r\n                <xsl:apply-templates select=\"rp:teams/rp:team\" mode=\"summary\"/>\r\n                </ol>								\r\n                </li>\r\n                </xsl:if>\r\n                <xsl:if test=\"count(rp:actors/rp:actor) > 0\">\r\n                <li><a href=\"#actors\">Actors</a>\r\n                <ol>\r\n                <xsl:apply-templates select=\"rp:actors/rp:actor\" mode=\"summary\"/>\r\n                </ol>								\r\n                </li>\r\n                </xsl:if>\r\n                <xsl:if test=\"count(rp:goals/rp:goal) > 0\">\r\n                <li><a href=\"#goals\">Goals</a>\r\n                <ol>\r\n                <xsl:apply-templates select=\"rp:goals/rp:goal\" mode=\"summary\"/>\r\n                </ol>								\r\n                </li>\r\n                </xsl:if>\r\n                <xsl:if test=\"count(rp:stories/rp:story) > 0\">\r\n                <li><a href=\"#stories\">Stories</a>\r\n                <ol>\r\n                <xsl:apply-templates select=\"rp:stories/rp:story\" mode=\"summary\"/>\r\n                </ol>								\r\n                </li>\r\n                </xsl:if>\r\n                <xsl:if test=\"count(rp:usecases/rp:usecase) > 0\">\r\n                <li><a href=\"#usecases\">Use Cases</a>\r\n                <ol>\r\n                <xsl:apply-templates select=\"rp:usecases/rp:usecase\" mode=\"summary\"/>\r\n                </ol>								\r\n                </li>\r\n                </xsl:if>\r\n                <xsl:if test=\"count(rp:scenarios/rp:scenario) > 0\">\r\n                <li><a href=\"#scenarios\">Scenarios</a>\r\n                <ol>\r\n                <xsl:apply-templates select=\"rp:scenarios/rp:scenario\" mode=\"summary\"/>\r\n                </ol>								\r\n                </li>\r\n                </xsl:if>\r\n                <xsl:if test=\"count(rp:glossary/rp:term) > 0\">\r\n                <li><a href=\"#glossary\">Glossary</a>\r\n                <ol>\r\n                <xsl:apply-templates select=\"rp:glossary/rp:term\" mode=\"summary\"/>\r\n                </ol>								\r\n                </li>\r\n                </xsl:if>\r\n                </ul>\r\n                </div>\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"rp:stakeholders\" />\r\n                <xsl:apply-templates select=\"rp:teams\" />\r\n                <xsl:apply-templates select=\"rp:actors\" />\r\n                <xsl:apply-templates select=\"rp:goals\" />\r\n                <xsl:apply-templates select=\"rp:stories\" />\r\n                <xsl:apply-templates select=\"rp:usecases\" />\r\n                <xsl:apply-templates select=\"rp:scenarios\" />\r\n                <xsl:apply-templates select=\"rp:glossary\" />\r\n                </div>\r\n                </div>\r\n                </body>\r\n                </html>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:stakeholder[count(rp:user/rp:username) = 0]|rp:team|rp:actor|rp:goal|rp:story|rp:usecase|rp:scenario|rp:term\" mode=\"summary\">\r\n                <li>\r\n                <a href=\"#{generate-id(.)}\">\r\n                <xsl:value-of select=\"rp:name\" />\r\n                </a>\r\n                </li>\r\n                </xsl:template>\r\n                <xsl:template match=\"rp:stakeholder[count(rp:user/rp:username) > 0]\" mode=\"summary\">\r\n                <li>\r\n                <a href=\"#{generate-id(.)}\">\r\n                <xsl:value-of select=\"rp:user/rp:name\" /> [<xsl:value-of select=\"rp:user/rp:username\" />]\r\n                </a>\r\n                </li>\r\n                </xsl:template>\r\n                \r\n                <!-- Stakeholders -->\r\n                <xsl:template match=\"rp:stakeholders\">\r\n                <xsl:if test=\"count(rp:stakeholder) > 0\">\r\n                <div class=\"stakeholders\">\r\n                <a name=\"stakeholders\"></a>\r\n                <h2>Stakeholders</h2>\r\n                <table class=\"stakeholders\">\r\n                <thead>\r\n                <tr>\r\n                <th>User?</th>\r\n                <th>Name</th>\r\n                <th>Organization</th>\r\n                <th>Email</th>\r\n                <th>Phone</th>\r\n                </tr>\r\n                </thead>\r\n                <tbody>\r\n                <xsl:apply-templates select=\"rp:stakeholder\" />\r\n                </tbody>\r\n                </table>\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:stakeholder[count(rp:user/rp:username) > 0]\">\r\n                <tr>\r\n                <td>\r\n                Yes\r\n                </td>\r\n                <td>\r\n                <a name=\"{generate-id(.)}\">\r\n                <xsl:value-of select=\"rp:user/rp:name\" />\r\n                (<xsl:value-of select=\"rp:user/rp:username\" />)\r\n                </a>\r\n                </td>\r\n                <td>\r\n                <xsl:apply-templates select=\"rp:user/rp:organization/@name\" />\r\n                </td>\r\n                <td>\r\n                <xsl:apply-templates select=\"rp:user/rp:emailAddress\" />\r\n                </td>\r\n                <td>\r\n                <xsl:apply-templates select=\"rp:user/rp:phoneNumber\" />\r\n                </td>\r\n                </tr>\r\n                <xsl:if test=\"count(rp:goals/*) > 0\">\r\n                <tr>\r\n                <td></td>\r\n                <td colspan=\"4\">\r\n                <xsl:apply-templates select=\"rp:goals\" />\r\n                </td>\r\n                </tr>\r\n                </xsl:if>\r\n                <xsl:if test=\"count(rp:annotations/*) > 0\">\r\n                <tr>\r\n                <td></td>\r\n                <td colspan=\"4\">\r\n                <xsl:apply-templates select=\"rp:annotations\" />\r\n                </td>\r\n                </tr>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:stakeholder\">\r\n                <tr>\r\n                <td>\r\n                No\r\n                </td>\r\n                <td>\r\n                <a name=\"{generate-id(.)}\">\r\n                <xsl:value-of select=\"rp:name\" />\r\n                </a>\r\n                </td>\r\n                <td>\r\n                <xsl:apply-templates select=\"rp:organization/@name\" />\r\n                </td>\r\n                <td>\r\n                </td>\r\n                <td>\r\n                </td>\r\n                </tr>\r\n                <xsl:if test=\"count(rp:goals/*) > 0\">\r\n                <tr>\r\n                <td></td>\r\n                <td colspan=\"4\">\r\n                <xsl:apply-templates select=\"rp:goals\" />\r\n                </td>\r\n                </tr>\r\n                </xsl:if>\r\n                <xsl:if test=\"count(rp:annotations/*) > 0\">\r\n                <tr>\r\n                <td></td>\r\n                <td colspan=\"4\">\r\n                <xsl:apply-templates select=\"rp:annotations\" />\r\n                </td>\r\n                </tr>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:stakeholderRef\">\r\n                <xsl:variable name=\"ref-id\" select=\".\" />\r\n                <li class=\"stakeholder\">\r\n                <span class=\"body\">\r\n                <a href=\"#{generate-id(//rp:stakeholder[@id = $ref-id])}\">\r\n                <xsl:apply-templates select=\"//rp:stakeholder[@id = $ref-id]/rp:name\" />\r\n                </a>\r\n                </span>\r\n                </li>\r\n                </xsl:template>\r\n                \r\n                <!-- Teams -->\r\n                <xsl:template match=\"rp:teams\">\r\n                <xsl:if test=\"count(rp:team) > 0\">\r\n                <a name=\"teams\"></a>\r\n                <h2>Teams</h2>\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"*\"/>\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:team\">\r\n                <h2><xsl:value-of select=\"rp:name\"/></h2>\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"rp:members\"/>\r\n                <xsl:apply-templates select=\"rp:annotations\" />\r\n                </div>\r\n                </xsl:template>\r\n                \r\n                <!-- Actors -->\r\n                <xsl:template match=\"rp:project/rp:actors\">\r\n                <xsl:if test=\"count(rp:actor) > 0\">\r\n                <a name=\"actors\"></a>\r\n                <h2>Actors</h2>\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"*\" />\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:actors\">\r\n                <xsl:if test=\"count(*) > 0\">\r\n                <h4>Actors</h4>\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"*\" />\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:actor\">\r\n                <div class=\"actor\">\r\n                <h4>\r\n                <a name=\"{generate-id(.)}\">\r\n                <xsl:value-of select=\"rp:name\" />\r\n                </a>\r\n                </h4>\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"rp:text\" />\r\n                <xsl:apply-templates select=\"rp:goals\" />\r\n                <xsl:apply-templates select=\"rp:annotations\" />\r\n                </div>\r\n                </div>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:actorRef\">\r\n                <xsl:variable name=\"ref-id\" select=\".\" />\r\n                <li class=\"actor\">\r\n                <span class=\"body\">\r\n                <a href=\"#{generate-id(//rp:actor[@id = $ref-id])}\">\r\n                <xsl:apply-templates select=\"//rp:actor[@id = $ref-id]/rp:name\" />\r\n                </a>\r\n                </span>\r\n                </li>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:primaryActorRef\">\r\n                <xsl:variable name=\"ref-id\" select=\".\" />\r\n                <a href=\"#{generate-id(//rp:actor[@id = $ref-id])}\">\r\n                <xsl:apply-templates select=\"//rp:actor[@id = $ref-id]/rp:name\" />\r\n                </a>\r\n                </xsl:template>\r\n                \r\n                <!-- Goals -->\r\n                <xsl:template match=\"rp:project/rp:goals\">\r\n                <xsl:if test=\"count(rp:goal) > 0\">\r\n                <div class=\"goals\">\r\n                <a name=\"goals\"></a>\r\n                <h2>Goals</h2>\r\n                <ul class=\"goals\">\r\n                <xsl:apply-templates select=\"*\" />\r\n                </ul>\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:goals\">\r\n                <xsl:if test=\"count(*) > 0\">\r\n                <h4>Goals</h4>\r\n                <ul class=\"goals\">\r\n                <xsl:apply-templates select=\"*\" />\r\n                </ul>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:goal\">\r\n                <li class=\"goal\">\r\n                <div class=\"goal\">\r\n                <h4>\r\n                <a name=\"{generate-id(.)}\">\r\n                <xsl:value-of select=\"rp:name\" />\r\n                </a>\r\n                </h4>\r\n                <div class=\"body\">\r\n                <div>\r\n                <xsl:value-of select=\"rp:text\" />\r\n                </div>\r\n                <xsl:apply-templates select=\"rp:goalRelations\" />\r\n                <xsl:apply-templates select=\"rp:annotations\" />\r\n                </div>\r\n                </div>\r\n                </li>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:goalRef\">\r\n                <xsl:variable name=\"ref-id\" select=\".\" />\r\n                <li class=\"goal\">\r\n                <span class=\"body\">\r\n                <a href=\"#{generate-id(//rp:goal[@id = $ref-id])}\">\r\n                <xsl:apply-templates select=\"//rp:goal[@id = $ref-id]/rp:name\" />\r\n                </a>\r\n                </span>\r\n                </li>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:goalRelations\">\r\n                <xsl:if test=\"count(rp:goalRelation) > 0\">\r\n                <h4>Goal Relations</h4>\r\n                <ul>\r\n                <xsl:apply-templates select=\"rp:goalRelation\" />\r\n                </ul>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:goalRelation\">\r\n                <xsl:variable name=\"ref-id\" select=\"@toGoal\" />\r\n                <li class=\"goal\">\r\n                <div>\r\n                <xsl:value-of select=\"@relationType\" /> -\r\n                <a href=\"#{generate-id(//rp:goal[@id = $ref-id])}\">\r\n                <xsl:apply-templates select=\"//rp:goal[@id = $ref-id]/rp:name\" />\r\n                </a>\r\n                </div>\r\n                <xsl:apply-templates select=\"rp:annotations\" />\r\n                </li>\r\n                </xsl:template>\r\n                \r\n                <!-- Stories -->\r\n                <xsl:template match=\"rp:project/rp:stories\">\r\n                <xsl:if test=\"count(rp:story) > 0\">\r\n                <a name=\"stories\"></a>\r\n                <h2>Stories</h2>\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"*\" />\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:stories\">\r\n                <xsl:if test=\"count(*) > 0\">\r\n                <h4>Stories</h4>\r\n                <xsl:choose>\r\n                <xsl:when test=\"count(rp:story) > 0\">\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"rp:story\" />\r\n                </div>\r\n                </xsl:when>\r\n                <xsl:when test=\"count(rp:storyRef) > 0\">\r\n                <div class=\"body\">\r\n                <ul>\r\n                <xsl:for-each select=\"rp:storyRef\">\r\n                <li><xsl:apply-templates select=\".\" /></li>\r\n                </xsl:for-each>\r\n                </ul>\r\n                </div>\r\n                </xsl:when>\r\n                </xsl:choose>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:story\">\r\n                <div class=\"story\">\r\n                <h4>\r\n                <a name=\"{generate-id(.)}\">\r\n                <xsl:value-of select=\"@storyType\" />\r\n                Story -\r\n                <xsl:value-of select=\"rp:name\" />\r\n                </a>\r\n                </h4>\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"rp:text\" />\r\n                </div>\r\n                <xsl:apply-templates select=\"rp:actors\" />\r\n                <xsl:apply-templates select=\"rp:goals\" />\r\n                <xsl:apply-templates select=\"rp:annotations\" />\r\n                </div>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:storyRef\">\r\n                <xsl:variable name=\"ref-id\" select=\".\" />\r\n                <a href=\"#{generate-id(//rp:story[@id = $ref-id])}\">\r\n                <xsl:apply-templates select=\"//rp:story[@id = $ref-id]/rp:name\" />\r\n                </a>\r\n                </xsl:template>\r\n                \r\n                <!-- Use Cases -->\r\n                <xsl:template match=\"rp:project/rp:usecases\">\r\n                <xsl:if test=\"count(rp:usecase) > 0\">\r\n                <a name=\"usecases\"></a>\r\n                <h2>Use Cases</h2>\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"*\" />\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:usecases\">\r\n                <xsl:if test=\"count(*) > 0\">\r\n                <h4>Use Cases</h4>\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"rp:usecase\" />\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:usecaseRef\">\r\n                <xsl:variable name=\"ref-id\" select=\".\" />\r\n                <a href=\"#{generate-id(//rp:usecase[@id = $ref-id])}\">\r\n                <xsl:apply-templates select=\"//rp:usecase[@id = $ref-id]/rp:name\" />\r\n                </a>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:usecase\">\r\n                <div class=\"usecase\">\r\n                <h4>\r\n                <a name=\"{generate-id(.)}\">\r\n                <xsl:value-of select=\"rp:name\" />\r\n                </a>\r\n                </h4>\r\n                <div class=\"body\">\r\n                <xsl:if test=\"string-length(rp:text) > 0\">\r\n                <h4>Description</h4>\r\n                <xsl:apply-templates select=\"rp:text\" />\r\n                </xsl:if>\r\n                <xsl:apply-templates select=\"rp:goals\" />\r\n                <xsl:apply-templates select=\"rp:stories\" />\r\n                <h4>Primary Actor: <xsl:apply-templates select=\"rp:primaryActorRef\" /></h4>\r\n                <xsl:apply-templates select=\"rp:actors\" />\r\n                <h4>Scenario</h4>\r\n                <xsl:variable name=\"scenarioRef\" select=\"string(rp:scenarioRef)\" />\r\n                <xsl:apply-templates select=\"//rp:scenario[@id = $scenarioRef]\" mode=\"usecase\" />\r\n                <xsl:apply-templates select=\"rp:annotations\" />\r\n                </div>\r\n                </div>\r\n                </xsl:template>\r\n                \r\n                <!-- Scenarios - only include scenarios that aren\'t part of another scenario or use case -->\r\n                <xsl:template match=\"rp:project/rp:scenarios\">\r\n                <xsl:variable name=\"rootScenariosExist\">\r\n                <xsl:for-each select=\"rp:scenario\">\r\n                <xsl:variable name=\"scenarioId\" select=\"@id\"/>\r\n                <xsl:choose>\r\n                <xsl:when test=\"count(//rp:scenarioRef[string(text()) = $scenarioId]) = 0 and count(//rp:stepRef[string(text()) = $scenarioId]) = 0\"><xsl:value-of select=\"concat(./@id,\' \')\"/></xsl:when>\r\n                </xsl:choose>\r\n                </xsl:for-each>\r\n                </xsl:variable>\r\n                <xsl:comment> <xsl:value-of select=\"$rootScenariosExist\"/> </xsl:comment>\r\n                <xsl:if test=\"string-length($rootScenariosExist) > 0\">\r\n                <a name=\"scenarios\"></a>\r\n                <h2>Scenarios (not used else where)</h2>\r\n                <div class=\"body\">\r\n                <xsl:for-each select=\"rp:scenario\">\r\n                <xsl:variable name=\"scenarioId\" select=\"@id\"/>\r\n                <xsl:if test=\"count(//rp:scenarioRef[string(text()) = $scenarioId]) = 0 and count(//rp:stepRef[string(text()) = $scenarioId]) = 0\">\r\n                <xsl:apply-templates select=\".\" />\r\n                </xsl:if>\r\n                </xsl:for-each>\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:scenarios\">\r\n                <xsl:if test=\"count(*) > 0\">\r\n                <h4>Scenarios</h4>\r\n                <div class=\"body\">\r\n                <xsl:apply-templates select=\"rp:scenario\" />\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:scenario\" mode=\"usecase\">\r\n                <div class=\"scenario\">\r\n                <xsl:apply-templates select=\"rp:steps\" />\r\n                </div>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:scenario\">\r\n                <div class=\"scenario\">\r\n                <h4>\r\n                <a name=\"{generate-id(.)}\">\r\n                <xsl:value-of select=\"@scenarioType\" />\r\n                -\r\n                <xsl:value-of select=\"rp:name\" />\r\n                </a>\r\n                </h4>\r\n                <xsl:if test=\"string-length(rp:text) > 0\">\r\n                <h4>Description</h4>\r\n                <xsl:apply-templates select=\"rp:text\" />\r\n                </xsl:if>\r\n                <xsl:apply-templates select=\"rp:steps\" />\r\n                <xsl:apply-templates select=\"rp:annotations\" />			\r\n                </div>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:steps\">\r\n                <ol class=\"scenario\">\r\n                <xsl:for-each select=\"rp:stepRef\">\r\n                <li class=\"step\"><xsl:apply-templates select=\".\"/></li>\r\n                </xsl:for-each>\r\n                </ol>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:stepRef\">\r\n                <xsl:variable name=\"stepRefId\" select=\"text()\" />\r\n                <xsl:apply-templates select=\"//rp:scenario[@id = $stepRefId]|//rp:step[@id = $stepRefId]\" mode=\"steps\"/>\r\n                </xsl:template>\r\n                \r\n                <!-- Steps and embedded scenarios -->\r\n                <xsl:template match=\"rp:scenario\" mode=\"steps\">\r\n                <xsl:value-of select=\"@scenarioType\" /> - <xsl:value-of select=\"rp:name\" />\r\n                <xsl:apply-templates select=\"rp:steps\" />\r\n                </xsl:template>\r\n                <xsl:template match=\"rp:step\" mode=\"steps\">\r\n                <xsl:value-of select=\"@scenarioType\" /> - <xsl:value-of select=\"rp:name\" />\r\n                </xsl:template>\r\n                \r\n                <!-- Glossary -->\r\n                <xsl:template match=\"rp:project/rp:glossary\">\r\n                <xsl:if test=\"count(rp:term) > 0\">\r\n                <a name=\"glossary\"></a>\r\n                <h2>Glossary</h2>\r\n                <div class=\"body\">\r\n                <table class=\"stakeholders\">\r\n                <thead>\r\n                <tr>\r\n                <th>Term</th>\r\n                <th>Definition</th>\r\n                </tr>\r\n                </thead>\r\n                <tbody>\r\n                <xsl:apply-templates select=\"rp:term\" />\r\n                </tbody>\r\n                </table>\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:term\">\r\n                <tr>\r\n                <td><xsl:value-of select=\"rp:name\" /></td>\r\n                <td><xsl:value-of select=\"rp:text\" /></td>\r\n                </tr>\r\n                </xsl:template>	\r\n                \r\n                <!-- Annotations -->\r\n                <xsl:template match=\"rp:annotations\">\r\n                <!-- \r\n                <xsl:if test=\"count(*) > 0\">\r\n                <h4>Annotations</h4>\r\n                <ul>\r\n                <xsl:apply-templates select=\"rp:note|rp:issue|rp:lexicalIssue\" />\r\n                </ul>\r\n                </xsl:if>\r\n                -->		\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:note\">\r\n                <li class=\"note\">\r\n                <xsl:apply-templates select=\"rp:text\" />\r\n                </li>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:issue|rp:lexicalIssue\">\r\n                <li class=\"issue\">\r\n                <xsl:apply-templates select=\"rp:text\" />\r\n                <xsl:apply-templates select=\"rp:positions\" />\r\n                </li>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:positions\">\r\n                <xsl:if test=\"count(*) > 0\">\r\n                <h4>Solutions</h4>\r\n                <div class=\"body\">\r\n                <ul>\r\n                <xsl:apply-templates select=\"*\" />\r\n                </ul>\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:position|rp:addWordToDictionaryPosition|rp:changeSpellingPosition|rp:addGlossaryTermPosition|rp:addActorPosition\">\r\n                <li class=\"position\">\r\n                <xsl:apply-templates select=\"rp:text\" />\r\n                <xsl:apply-templates select=\"rp:arguments\" />\r\n                </li>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:arguments\">\r\n                <xsl:if test=\"count(*) > 0\">\r\n                <h4>Arguments</h4>\r\n                <div class=\"body\">\r\n                <h5>Strongly For</h5>\r\n                <xsl:apply-templates select=\"rp:argument[@supportLevel = \'StronglyFor\']\" />\r\n                <h5>For</h5>\r\n                <xsl:apply-templates select=\"rp:argument[@supportLevel = \'For\']\" />\r\n                <h5>Neutral</h5>\r\n                <xsl:apply-templates select=\"rp:argument[@supportLevel = \'Neutral\']\" />\r\n                <h5>Against</h5>\r\n                <xsl:apply-templates select=\"rp:argument[@supportLevel = \'Against\']\" />\r\n                <h5>Strongly Against</h5>\r\n                <xsl:apply-templates select=\"rp:argument[@supportLevel = \'StronglyAgainst\']\" />\r\n                </div>\r\n                </xsl:if>\r\n                </xsl:template>\r\n                \r\n                <xsl:template match=\"rp:argument\">\r\n                <div class=\"argument\">\r\n                <xsl:apply-templates select=\"rp:text\" />\r\n                </div>\r\n                </xsl:template>\r\n                \r\n                \r\n                <!-- CSS style -->\r\n                <xsl:template name=\"css\">\r\n                <style type=\"text/css\">\r\n                html {\r\n                height: 100%;\r\n                }\r\n                \r\n                body {\r\n                height: 100%;\r\n                font-family: Arial;\r\n                font-size: 10pt;\r\n                margin-top: .5in;\r\n                margin-left: .5in;\r\n                margin-right: .5in;\r\n                margin-bottom: .5in;\r\n                }\r\n                \r\n                h1 {\r\n                text-align: center;\r\n                font-family: Arial;\r\n                font-size: 14pt;\r\n                }\r\n                \r\n                h2 {\r\n                text-align: left;\r\n                font-family: Arial;\r\n                font-size: 12pt;\r\n                }\r\n                \r\n                h3, h4, h5, h6 {\r\n                text-align: left;\r\n                font-family: Arial;\r\n                font-size: 11pt;\r\n                }\r\n                \r\n                \r\n                table {\r\n                border: solid 1pt black;\r\n                border-collapse: collapse;\r\n                empty-cells: show;\r\n                }\r\n                \r\n                \r\n                td {\r\n                padding: 5pt;\r\n                border: solid 1pt black;\r\n                border-collapse: collapse;\r\n                font-family: Arial;\r\n                font-size: 10pt;\r\n                vertical-align: top;\r\n                text-align: left;\r\n                font-weight: normal;\r\n                }\r\n                \r\n                div .project .body {\r\n                margin: 0;\r\n                }\r\n                \r\n                div .body {\r\n                margin-top: .25in;\r\n                margin-left: .25in;\r\n                margin-right: .25in;\r\n                margin-bottom: .25in;\r\n                }\r\n                \r\n                .stakeholders table {\r\n                margin-top: .25in;\r\n                margin-left: .25in;\r\n                margin-right: .25in;\r\n                margin-bottom: .25in;\r\n                }\r\n                \r\n                th {\r\n                padding: 4pt;\r\n                border: solid 1pt black;\r\n                border-collapse: collapse;\r\n                font-family: Arial;\r\n                font-size: 10pt;\r\n                vertical-align: bottom;\r\n                text-align: left;\r\n                font-weight: bold;\r\n                }\r\n                \r\n                .issue {\r\n                }\r\n                \r\n                .note {\r\n                }\r\n                \r\n                .p {\r\n                padding-bottom: 10pt;\r\n                }\r\n                </style>	\r\n                </xsl:template>\r\n                </xsl:stylesheet>','HTML Specification',1,1);
/*!40000 ALTER TABLE `reports` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `reports_annotations` WRITE;
/*!40000 ALTER TABLE `reports_annotations` DISABLE KEYS */;
/*!40000 ALTER TABLE `reports_annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `reports_glossary_terms` WRITE;
/*!40000 ALTER TABLE `reports_glossary_terms` DISABLE KEYS */;
/*!40000 ALTER TABLE `reports_glossary_terms` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `scenario_steps` WRITE;
/*!40000 ALTER TABLE `scenario_steps` DISABLE KEYS */;
INSERT INTO `scenario_steps` VALUES (1,2,0);
/*!40000 ALTER TABLE `scenario_steps` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `scenarios` WRITE;
/*!40000 ALTER TABLE `scenarios` DISABLE KEYS */;
INSERT INTO `scenarios` VALUES ('com.rreganjr.requel.project.Scenario',1,'2025-10-14 00:00:00',11,'The primary scenario for creating a project; shared with the project\'s scenario list.','Create a new project','Primary',1,1);
INSERT INTO `scenarios` VALUES ('com.rreganjr.requel.project.Step',2,'2025-10-14 00:00:00',8,NULL,'collect the members','Primary',1,1);
/*!40000 ALTER TABLE `scenarios` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `scenarios_annotations` WRITE;
/*!40000 ALTER TABLE `scenarios_annotations` DISABLE KEYS */;
INSERT INTO `scenarios_annotations` VALUES (2,85);
INSERT INTO `scenarios_annotations` VALUES (1,112);
INSERT INTO `scenarios_annotations` VALUES (2,112);
INSERT INTO `scenarios_annotations` VALUES (2,201);
INSERT INTO `scenarios_annotations` VALUES (2,202);
INSERT INTO `scenarios_annotations` VALUES (1,249);
INSERT INTO `scenarios_annotations` VALUES (2,249);
INSERT INTO `scenarios_annotations` VALUES (2,250);
INSERT INTO `scenarios_annotations` VALUES (2,251);
/*!40000 ALTER TABLE `scenarios_annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `scenarios_glossary_terms` WRITE;
/*!40000 ALTER TABLE `scenarios_glossary_terms` DISABLE KEYS */;
/*!40000 ALTER TABLE `scenarios_glossary_terms` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `stakeholder_permissions` WRITE;
/*!40000 ALTER TABLE `stakeholder_permissions` DISABLE KEYS */;
INSERT INTO `stakeholder_permissions` VALUES (5,'com.rreganjr.requel.annotation.Annotation','Delete');
INSERT INTO `stakeholder_permissions` VALUES (3,'com.rreganjr.requel.annotation.Annotation','Edit');
INSERT INTO `stakeholder_permissions` VALUES (4,'com.rreganjr.requel.annotation.Annotation','Grant');
INSERT INTO `stakeholder_permissions` VALUES (11,'com.rreganjr.requel.project.Actor','Delete');
INSERT INTO `stakeholder_permissions` VALUES (9,'com.rreganjr.requel.project.Actor','Edit');
INSERT INTO `stakeholder_permissions` VALUES (10,'com.rreganjr.requel.project.Actor','Grant');
INSERT INTO `stakeholder_permissions` VALUES (17,'com.rreganjr.requel.project.GlossaryTerm','Delete');
INSERT INTO `stakeholder_permissions` VALUES (15,'com.rreganjr.requel.project.GlossaryTerm','Edit');
INSERT INTO `stakeholder_permissions` VALUES (16,'com.rreganjr.requel.project.GlossaryTerm','Grant');
INSERT INTO `stakeholder_permissions` VALUES (8,'com.rreganjr.requel.project.Goal','Delete');
INSERT INTO `stakeholder_permissions` VALUES (6,'com.rreganjr.requel.project.Goal','Edit');
INSERT INTO `stakeholder_permissions` VALUES (7,'com.rreganjr.requel.project.Goal','Grant');
INSERT INTO `stakeholder_permissions` VALUES (1,'com.rreganjr.requel.project.Project','Edit');
INSERT INTO `stakeholder_permissions` VALUES (2,'com.rreganjr.requel.project.Project','Grant');
INSERT INTO `stakeholder_permissions` VALUES (29,'com.rreganjr.requel.project.ReportGenerator','Delete');
INSERT INTO `stakeholder_permissions` VALUES (27,'com.rreganjr.requel.project.ReportGenerator','Edit');
INSERT INTO `stakeholder_permissions` VALUES (28,'com.rreganjr.requel.project.ReportGenerator','Grant');
INSERT INTO `stakeholder_permissions` VALUES (26,'com.rreganjr.requel.project.Scenario','Delete');
INSERT INTO `stakeholder_permissions` VALUES (24,'com.rreganjr.requel.project.Scenario','Edit');
INSERT INTO `stakeholder_permissions` VALUES (25,'com.rreganjr.requel.project.Scenario','Grant');
INSERT INTO `stakeholder_permissions` VALUES (14,'com.rreganjr.requel.project.Stakeholder','Delete');
INSERT INTO `stakeholder_permissions` VALUES (12,'com.rreganjr.requel.project.Stakeholder','Edit');
INSERT INTO `stakeholder_permissions` VALUES (13,'com.rreganjr.requel.project.Stakeholder','Grant');
INSERT INTO `stakeholder_permissions` VALUES (20,'com.rreganjr.requel.project.Story','Delete');
INSERT INTO `stakeholder_permissions` VALUES (18,'com.rreganjr.requel.project.Story','Edit');
INSERT INTO `stakeholder_permissions` VALUES (19,'com.rreganjr.requel.project.Story','Grant');
INSERT INTO `stakeholder_permissions` VALUES (23,'com.rreganjr.requel.project.UseCase','Delete');
INSERT INTO `stakeholder_permissions` VALUES (21,'com.rreganjr.requel.project.UseCase','Edit');
INSERT INTO `stakeholder_permissions` VALUES (22,'com.rreganjr.requel.project.UseCase','Grant');
/*!40000 ALTER TABLE `stakeholder_permissions` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `stakeholders` WRITE;
/*!40000 ALTER TABLE `stakeholders` DISABLE KEYS */;
INSERT INTO `stakeholders` VALUES ('com.rreganjr.requel.project.UserStakeholder',1,'2009-01-04 00:00:00',1,NULL,NULL,1,1,3,NULL);
INSERT INTO `stakeholders` VALUES ('com.rreganjr.requel.project.UserStakeholder',2,'2009-01-05 00:00:00',1,NULL,NULL,1,1,2,NULL);
INSERT INTO `stakeholders` VALUES ('com.rreganjr.requel.project.NonUserStakeholder',3,'2009-01-05 00:00:00',2,'Harvard University',NULL,1,1,NULL,NULL);
INSERT INTO `stakeholders` VALUES ('com.rreganjr.requel.project.UserStakeholder',4,'2009-02-11 00:00:00',2,NULL,NULL,1,1,1,NULL);
/*!40000 ALTER TABLE `stakeholders` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `stakeholders_annotations` WRITE;
/*!40000 ALTER TABLE `stakeholders_annotations` DISABLE KEYS */;
/*!40000 ALTER TABLE `stakeholders_annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `stakeholders_glossary_terms` WRITE;
/*!40000 ALTER TABLE `stakeholders_glossary_terms` DISABLE KEYS */;
/*!40000 ALTER TABLE `stakeholders_glossary_terms` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `stakeholders_goals` WRITE;
/*!40000 ALTER TABLE `stakeholders_goals` DISABLE KEYS */;
INSERT INTO `stakeholders_goals` VALUES (3,10);
/*!40000 ALTER TABLE `stakeholders_goals` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `stakeholders_permissions` WRITE;
/*!40000 ALTER TABLE `stakeholders_permissions` DISABLE KEYS */;
INSERT INTO `stakeholders_permissions` VALUES (4,1);
INSERT INTO `stakeholders_permissions` VALUES (4,2);
INSERT INTO `stakeholders_permissions` VALUES (4,3);
INSERT INTO `stakeholders_permissions` VALUES (4,4);
INSERT INTO `stakeholders_permissions` VALUES (4,5);
INSERT INTO `stakeholders_permissions` VALUES (4,6);
INSERT INTO `stakeholders_permissions` VALUES (4,7);
INSERT INTO `stakeholders_permissions` VALUES (4,8);
INSERT INTO `stakeholders_permissions` VALUES (4,9);
INSERT INTO `stakeholders_permissions` VALUES (4,10);
INSERT INTO `stakeholders_permissions` VALUES (4,11);
INSERT INTO `stakeholders_permissions` VALUES (4,12);
INSERT INTO `stakeholders_permissions` VALUES (4,13);
INSERT INTO `stakeholders_permissions` VALUES (4,14);
INSERT INTO `stakeholders_permissions` VALUES (4,15);
INSERT INTO `stakeholders_permissions` VALUES (4,16);
INSERT INTO `stakeholders_permissions` VALUES (4,17);
INSERT INTO `stakeholders_permissions` VALUES (4,18);
INSERT INTO `stakeholders_permissions` VALUES (4,19);
INSERT INTO `stakeholders_permissions` VALUES (4,20);
INSERT INTO `stakeholders_permissions` VALUES (4,21);
INSERT INTO `stakeholders_permissions` VALUES (4,22);
INSERT INTO `stakeholders_permissions` VALUES (4,23);
INSERT INTO `stakeholders_permissions` VALUES (4,24);
INSERT INTO `stakeholders_permissions` VALUES (4,25);
INSERT INTO `stakeholders_permissions` VALUES (4,26);
INSERT INTO `stakeholders_permissions` VALUES (4,27);
INSERT INTO `stakeholders_permissions` VALUES (4,28);
INSERT INTO `stakeholders_permissions` VALUES (4,29);
/*!40000 ALTER TABLE `stakeholders_permissions` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `stories` WRITE;
/*!40000 ALTER TABLE `stories` DISABLE KEYS */;
INSERT INTO `stories` VALUES (1,'2009-01-05 00:00:00',71,'Buddy the controller of Bailey Pet Supply has a problem; it takes over a month to close the books each quarter. Due to new disclosure requirements, Alex the CFO needs to submit quarterly results to the board of directors one week after the end of the quarter. Buddy tells Theresa, the accounting manager, that they need to close the books each quarter in less than a week. Theresa tells Buddy that the problem stems from the order processing system not being integrated with the accounting package and at the end of each month data must be entered into both systems to reconcile them manually.\n                Buddy decides that the systems must be integrated before the end of the next quarter so that the one week turn-around for reporting can be met. Buddy meets with Dave the IT manager to discuss the work. Dave assigns Tim, one of the IT project managers, to work with Buddy to determine the requirements and estimate the work. Tim meets with Buddy and they generate a list of high level requirements. Tim gives the requirements and a rough estimate of two months to Dave, who in turn gives them to Ron, an MIS programmer, to design and develop. Ron reviews the requirements and goes back to Dave and tells him there are a lot of vague areas and he needs to get clarification from Tim and Buddy.\n                Ron and Tim have a meeting to discuss the requirements. Ron asks Tim \"What is a purchase order?\" Tim response \"It’s a record in the purchase system that Linda enters based on information from a sales person.\" Ron asks \"What information is in the purchase order?\" Tim doesn’t know and tells Ron he will ask Buddy and get back to him. Tim sends an email to Buddy and calls it a day.\n                Buddy replies to Tim’s email that he should talk to Linda to get the details of a purchase order. Tim has a meeting with Linda and gets the details on the data for a purchase order. Tim sends Ron an email with the information. Ron replies to Tim’s email \"Is there a database with data that I can look at for a sample?\"','Poor Communication in Requirements','Success',1,1);
INSERT INTO `stories` VALUES (2,'2009-01-20 00:00:00',48,'Rich logs in to the system and it displays a list of his active projects and the option to create a new project. Rich chooses to create a new project and the system displays the new project interface. Rich enters \"Mobile Media Portal\" for the project name and enters a short description. Rich looks over the list of existing customers and doesn\'t see what he\'s looking for and enters \"Virile Mobile\".\n\n                The system creates the new project and adds Rich as a stakeholder with permission to edit all project entities and grant permission to other users to edit entities. The system closes the new project interface and adds the new project in the project navigation interface.','Rich creates a new project','Success',1,1);
INSERT INTO `stories` VALUES (3,'2009-01-05 00:00:00',42,'Rich opens a web browser to the Requel system address. The system doesn’t detect an existing user session and presents the login screen. Rich enters his username and password. The system verifies the username and password combination is correct. The system logs that Rich has successfully logged into the system and shows him a list of his open projects and an overview of recent project changes.','Rich logs in to the system','Success',1,1);
INSERT INTO `stories` VALUES (4,'2009-01-05 00:00:00',44,'Ron opens a web browser to the Requel system address. The system doesn’t detect an existing user session and presents the login screen. Ron doesn’t have a username and password yet so he chooses the help option. The system displays a message that the system requires a distinct username and password and that the user should contact Dave at extension 500 or via email to dave@requel if they need a new account or are having a problem connecting. The system also displays a help message describing what to do if the user forgot their account information. Ron sends Dave an email to create him a new account.','Ron fails to log in to the system','Exception',1,1);
INSERT INTO `stories` VALUES (5,'2009-01-20 00:00:00',46,'When Theresa logs in, the system determines that she has permission to create projects and displays the \"Create Project\" option in the system interface. Theresa chooses to create a new project and the system displays the new project interface. Theresa enters \"Purchase Order System\" for the project name, leaves the description blank, and leaves \"Bailey Pet Supply\", her default automatically supplied, as the customer.\n\n                The system creates the new project and adds Theresa as a stakeholder with permission to edit all project entities and grant permission to other users to edit entities. The system closes the new project interface and displays the project navigation interface to add and edit the project\'s entities.','Theresa creates a new project','Success',1,1);
INSERT INTO `stories` VALUES (6,'2009-01-05 00:00:00',60,'Theresa connects to the Requel system via a web browser from a link in an email message sent by the Requel system when Dave the system administrator created an account for her. The system displays the new user setup interface, prompting Theresa to confirm the personal information Dave entered about her and filling in any missing information, such as name, phone number, email address, a username and a password. When Theresa completes entering the information the system forwards her to the login screen.\n\n                Theresa enters her username and password, and the system verifies it as correct. The system logs that Theresa has successfully logged into the system, and because Theresa is a novice user, the system displays the novice user interface to help her get started.','Theresa logs in to the system','Success',1,1);
/*!40000 ALTER TABLE `stories` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `stories_annotations` WRITE;
/*!40000 ALTER TABLE `stories_annotations` DISABLE KEYS */;
INSERT INTO `stories_annotations` VALUES (3,1);
INSERT INTO `stories_annotations` VALUES (4,1);
INSERT INTO `stories_annotations` VALUES (6,1);
INSERT INTO `stories_annotations` VALUES (5,2);
INSERT INTO `stories_annotations` VALUES (2,3);
INSERT INTO `stories_annotations` VALUES (6,8);
INSERT INTO `stories_annotations` VALUES (2,11);
INSERT INTO `stories_annotations` VALUES (1,13);
INSERT INTO `stories_annotations` VALUES (5,16);
INSERT INTO `stories_annotations` VALUES (1,17);
INSERT INTO `stories_annotations` VALUES (2,18);
INSERT INTO `stories_annotations` VALUES (3,18);
INSERT INTO `stories_annotations` VALUES (4,19);
INSERT INTO `stories_annotations` VALUES (4,22);
INSERT INTO `stories_annotations` VALUES (5,24);
INSERT INTO `stories_annotations` VALUES (6,24);
INSERT INTO `stories_annotations` VALUES (4,27);
INSERT INTO `stories_annotations` VALUES (4,28);
INSERT INTO `stories_annotations` VALUES (6,29);
INSERT INTO `stories_annotations` VALUES (2,30);
INSERT INTO `stories_annotations` VALUES (3,30);
INSERT INTO `stories_annotations` VALUES (4,31);
INSERT INTO `stories_annotations` VALUES (4,32);
INSERT INTO `stories_annotations` VALUES (2,33);
INSERT INTO `stories_annotations` VALUES (5,33);
INSERT INTO `stories_annotations` VALUES (6,34);
INSERT INTO `stories_annotations` VALUES (6,35);
INSERT INTO `stories_annotations` VALUES (4,37);
INSERT INTO `stories_annotations` VALUES (2,40);
INSERT INTO `stories_annotations` VALUES (2,42);
INSERT INTO `stories_annotations` VALUES (5,42);
INSERT INTO `stories_annotations` VALUES (4,43);
INSERT INTO `stories_annotations` VALUES (6,44);
INSERT INTO `stories_annotations` VALUES (3,45);
INSERT INTO `stories_annotations` VALUES (4,45);
INSERT INTO `stories_annotations` VALUES (6,45);
INSERT INTO `stories_annotations` VALUES (2,46);
INSERT INTO `stories_annotations` VALUES (5,46);
INSERT INTO `stories_annotations` VALUES (6,50);
INSERT INTO `stories_annotations` VALUES (6,51);
INSERT INTO `stories_annotations` VALUES (3,52);
INSERT INTO `stories_annotations` VALUES (4,52);
INSERT INTO `stories_annotations` VALUES (3,53);
INSERT INTO `stories_annotations` VALUES (6,54);
INSERT INTO `stories_annotations` VALUES (6,57);
INSERT INTO `stories_annotations` VALUES (2,60);
INSERT INTO `stories_annotations` VALUES (2,66);
INSERT INTO `stories_annotations` VALUES (5,66);
INSERT INTO `stories_annotations` VALUES (3,67);
INSERT INTO `stories_annotations` VALUES (6,68);
INSERT INTO `stories_annotations` VALUES (3,71);
INSERT INTO `stories_annotations` VALUES (5,75);
INSERT INTO `stories_annotations` VALUES (3,76);
INSERT INTO `stories_annotations` VALUES (4,76);
INSERT INTO `stories_annotations` VALUES (6,77);
INSERT INTO `stories_annotations` VALUES (5,78);
INSERT INTO `stories_annotations` VALUES (5,79);
INSERT INTO `stories_annotations` VALUES (4,81);
INSERT INTO `stories_annotations` VALUES (6,82);
INSERT INTO `stories_annotations` VALUES (2,83);
INSERT INTO `stories_annotations` VALUES (3,84);
INSERT INTO `stories_annotations` VALUES (4,84);
INSERT INTO `stories_annotations` VALUES (6,84);
INSERT INTO `stories_annotations` VALUES (2,87);
INSERT INTO `stories_annotations` VALUES (5,87);
INSERT INTO `stories_annotations` VALUES (6,88);
INSERT INTO `stories_annotations` VALUES (6,90);
INSERT INTO `stories_annotations` VALUES (6,91);
INSERT INTO `stories_annotations` VALUES (2,93);
INSERT INTO `stories_annotations` VALUES (5,93);
INSERT INTO `stories_annotations` VALUES (2,94);
INSERT INTO `stories_annotations` VALUES (5,94);
INSERT INTO `stories_annotations` VALUES (5,96);
INSERT INTO `stories_annotations` VALUES (6,101);
INSERT INTO `stories_annotations` VALUES (5,102);
INSERT INTO `stories_annotations` VALUES (3,105);
INSERT INTO `stories_annotations` VALUES (1,112);
INSERT INTO `stories_annotations` VALUES (6,139);
INSERT INTO `stories_annotations` VALUES (2,140);
INSERT INTO `stories_annotations` VALUES (2,141);
INSERT INTO `stories_annotations` VALUES (1,199);
INSERT INTO `stories_annotations` VALUES (1,200);
INSERT INTO `stories_annotations` VALUES (1,201);
INSERT INTO `stories_annotations` VALUES (1,202);
INSERT INTO `stories_annotations` VALUES (1,203);
INSERT INTO `stories_annotations` VALUES (2,204);
INSERT INTO `stories_annotations` VALUES (2,205);
INSERT INTO `stories_annotations` VALUES (2,206);
INSERT INTO `stories_annotations` VALUES (2,207);
INSERT INTO `stories_annotations` VALUES (2,208);
INSERT INTO `stories_annotations` VALUES (2,209);
INSERT INTO `stories_annotations` VALUES (2,210);
INSERT INTO `stories_annotations` VALUES (2,211);
INSERT INTO `stories_annotations` VALUES (3,212);
INSERT INTO `stories_annotations` VALUES (3,213);
INSERT INTO `stories_annotations` VALUES (3,214);
INSERT INTO `stories_annotations` VALUES (3,215);
INSERT INTO `stories_annotations` VALUES (3,216);
INSERT INTO `stories_annotations` VALUES (3,217);
INSERT INTO `stories_annotations` VALUES (3,218);
INSERT INTO `stories_annotations` VALUES (3,219);
INSERT INTO `stories_annotations` VALUES (3,220);
INSERT INTO `stories_annotations` VALUES (3,221);
INSERT INTO `stories_annotations` VALUES (3,222);
INSERT INTO `stories_annotations` VALUES (4,223);
INSERT INTO `stories_annotations` VALUES (4,224);
INSERT INTO `stories_annotations` VALUES (4,225);
INSERT INTO `stories_annotations` VALUES (6,226);
INSERT INTO `stories_annotations` VALUES (4,227);
INSERT INTO `stories_annotations` VALUES (4,228);
INSERT INTO `stories_annotations` VALUES (4,229);
INSERT INTO `stories_annotations` VALUES (4,230);
INSERT INTO `stories_annotations` VALUES (4,231);
INSERT INTO `stories_annotations` VALUES (4,232);
INSERT INTO `stories_annotations` VALUES (4,233);
INSERT INTO `stories_annotations` VALUES (5,234);
INSERT INTO `stories_annotations` VALUES (5,235);
INSERT INTO `stories_annotations` VALUES (5,236);
INSERT INTO `stories_annotations` VALUES (5,237);
INSERT INTO `stories_annotations` VALUES (5,238);
INSERT INTO `stories_annotations` VALUES (5,239);
INSERT INTO `stories_annotations` VALUES (5,240);
INSERT INTO `stories_annotations` VALUES (6,241);
INSERT INTO `stories_annotations` VALUES (6,242);
INSERT INTO `stories_annotations` VALUES (6,243);
INSERT INTO `stories_annotations` VALUES (6,244);
INSERT INTO `stories_annotations` VALUES (6,245);
INSERT INTO `stories_annotations` VALUES (6,246);
INSERT INTO `stories_annotations` VALUES (6,247);
INSERT INTO `stories_annotations` VALUES (6,248);
/*!40000 ALTER TABLE `stories_annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `stories_glossary_terms` WRITE;
/*!40000 ALTER TABLE `stories_glossary_terms` DISABLE KEYS */;
/*!40000 ALTER TABLE `stories_glossary_terms` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `story_actors` WRITE;
/*!40000 ALTER TABLE `story_actors` DISABLE KEYS */;
/*!40000 ALTER TABLE `story_actors` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `story_goals` WRITE;
/*!40000 ALTER TABLE `story_goals` DISABLE KEYS */;
/*!40000 ALTER TABLE `story_goals` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `story_storycontainers` WRITE;
/*!40000 ALTER TABLE `story_storycontainers` DISABLE KEYS */;
INSERT INTO `story_storycontainers` VALUES (2,'com.rreganjr.requel.project.UseCase',1);
INSERT INTO `story_storycontainers` VALUES (3,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `story_storycontainers` VALUES (4,'com.rreganjr.requel.project.UseCase',2);
INSERT INTO `story_storycontainers` VALUES (5,'com.rreganjr.requel.project.UseCase',1);
INSERT INTO `story_storycontainers` VALUES (6,'com.rreganjr.requel.project.UseCase',2);
/*!40000 ALTER TABLE `story_storycontainers` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `team_stakeholders` WRITE;
/*!40000 ALTER TABLE `team_stakeholders` DISABLE KEYS */;
/*!40000 ALTER TABLE `team_stakeholders` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `teams` WRITE;
/*!40000 ALTER TABLE `teams` DISABLE KEYS */;
/*!40000 ALTER TABLE `teams` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `teams_annotations` WRITE;
/*!40000 ALTER TABLE `teams_annotations` DISABLE KEYS */;
/*!40000 ALTER TABLE `teams_annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `teams_glossary_terms` WRITE;
/*!40000 ALTER TABLE `teams_glossary_terms` DISABLE KEYS */;
/*!40000 ALTER TABLE `teams_glossary_terms` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `terms` WRITE;
/*!40000 ALTER TABLE `terms` DISABLE KEYS */;
/*!40000 ALTER TABLE `terms` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `terms_annotations` WRITE;
/*!40000 ALTER TABLE `terms_annotations` DISABLE KEYS */;
/*!40000 ALTER TABLE `terms_annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `terms_glossary_terms` WRITE;
/*!40000 ALTER TABLE `terms_glossary_terms` DISABLE KEYS */;
/*!40000 ALTER TABLE `terms_glossary_terms` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `terms_referers` WRITE;
/*!40000 ALTER TABLE `terms_referers` DISABLE KEYS */;
/*!40000 ALTER TABLE `terms_referers` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `usecase_actors` WRITE;
/*!40000 ALTER TABLE `usecase_actors` DISABLE KEYS */;
/*!40000 ALTER TABLE `usecase_actors` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `usecase_goals` WRITE;
/*!40000 ALTER TABLE `usecase_goals` DISABLE KEYS */;
INSERT INTO `usecase_goals` VALUES (2,9);
INSERT INTO `usecase_goals` VALUES (2,10);
/*!40000 ALTER TABLE `usecase_goals` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `usecase_stories` WRITE;
/*!40000 ALTER TABLE `usecase_stories` DISABLE KEYS */;
INSERT INTO `usecase_stories` VALUES (1,2);
INSERT INTO `usecase_stories` VALUES (2,3);
INSERT INTO `usecase_stories` VALUES (2,4);
INSERT INTO `usecase_stories` VALUES (1,5);
INSERT INTO `usecase_stories` VALUES (2,6);
/*!40000 ALTER TABLE `usecase_stories` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `usecases` WRITE;
/*!40000 ALTER TABLE `usecases` DISABLE KEYS */;
INSERT INTO `usecases` VALUES (1,'2009-01-20 00:00:00',16,NULL,'A user creates a new project',1,1,4,1);
INSERT INTO `usecases` VALUES (2,'2009-01-20 00:00:00',18,NULL,'A user logs in to the system',1,1,2,1);
/*!40000 ALTER TABLE `usecases` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `usecases_annotations` WRITE;
/*!40000 ALTER TABLE `usecases_annotations` DISABLE KEYS */;
INSERT INTO `usecases_annotations` VALUES (2,1);
INSERT INTO `usecases_annotations` VALUES (1,5);
INSERT INTO `usecases_annotations` VALUES (2,5);
INSERT INTO `usecases_annotations` VALUES (1,33);
INSERT INTO `usecases_annotations` VALUES (1,112);
INSERT INTO `usecases_annotations` VALUES (2,112);
INSERT INTO `usecases_annotations` VALUES (1,201);
INSERT INTO `usecases_annotations` VALUES (2,201);
INSERT INTO `usecases_annotations` VALUES (1,202);
INSERT INTO `usecases_annotations` VALUES (2,202);
INSERT INTO `usecases_annotations` VALUES (1,252);
INSERT INTO `usecases_annotations` VALUES (1,253);
INSERT INTO `usecases_annotations` VALUES (1,254);
INSERT INTO `usecases_annotations` VALUES (2,255);
INSERT INTO `usecases_annotations` VALUES (2,256);
INSERT INTO `usecases_annotations` VALUES (2,257);
INSERT INTO `usecases_annotations` VALUES (2,258);
/*!40000 ALTER TABLE `usecases_annotations` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `usecases_glossary_terms` WRITE;
/*!40000 ALTER TABLE `usecases_glossary_terms` DISABLE KEYS */;
/*!40000 ALTER TABLE `usecases_glossary_terms` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `user_role_permissions` WRITE;
/*!40000 ALTER TABLE `user_role_permissions` DISABLE KEYS */;
INSERT INTO `user_role_permissions` VALUES (1,'createProjects','com.rreganjr.requel.project.ProjectUserRole');
/*!40000 ALTER TABLE `user_role_permissions` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES ('com.rreganjr.requel.user.SystemAdminUserRole',1,1,NULL);
INSERT INTO `user_roles` VALUES ('com.rreganjr.requel.project.ProjectUserRole',2,2,2);
INSERT INTO `user_roles` VALUES ('com.rreganjr.requel.project.ProjectUserRole',3,2,3);
INSERT INTO `user_roles` VALUES ('com.rreganjr.requel.project.ProjectUserRole',4,3,1);
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `user_roles_active_projects` WRITE;
/*!40000 ALTER TABLE `user_roles_active_projects` DISABLE KEYS */;
INSERT INTO `user_roles_active_projects` VALUES (2,1);
INSERT INTO `user_roles_active_projects` VALUES (3,1);
INSERT INTO `user_roles_active_projects` VALUES (4,1);
/*!40000 ALTER TABLE `user_roles_active_projects` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `user_roles_permissions` WRITE;
/*!40000 ALTER TABLE `user_roles_permissions` DISABLE KEYS */;
INSERT INTO `user_roles_permissions` VALUES (2,1);
INSERT INTO `user_roles_permissions` VALUES (4,1);
/*!40000 ALTER TABLE `user_roles_permissions` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,_binary '','rreganjr@users.sourceforge.net','21232f297a57a5a743894a0e4a801fc3','System Administrator','','admin',2,1);
INSERT INTO `users` VALUES (2,_binary '','rreganjr@users.sourceforge.net','46f86faa6bbf9ac94a7e459509a20ed0','Builtin Project User',NULL,'project',1,1);
INSERT INTO `users` VALUES (3,_binary '\0','rreganjr@users.sourceforge.net','f549cd73f694aa6f5541b4ae30894eea','Analysis Assistant',NULL,'assistant',1,1);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `users_user_roles` WRITE;
/*!40000 ALTER TABLE `users_user_roles` DISABLE KEYS */;
INSERT INTO `users_user_roles` VALUES (1,1);
INSERT INTO `users_user_roles` VALUES (2,2);
INSERT INTO `users_user_roles` VALUES (3,3);
INSERT INTO `users_user_roles` VALUES (1,4);
/*!40000 ALTER TABLE `users_user_roles` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;


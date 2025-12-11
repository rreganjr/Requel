
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `actors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `actors` (
  `id` bigint NOT NULL,
  `date_created` datetime(6) DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `actors_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `actors_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `annotations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `annotations` (
  `annotation_type` varchar(255) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime(6) DEFAULT NULL,
  `grouping_object_type` varchar(255) DEFAULT NULL,
  `grouping_object_id` bigint NOT NULL,
  `text` longtext,
  `version` int NOT NULL,
  `must_be_resolved` bit(1) DEFAULT NULL,
  `resolved_date` datetime(6) DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `arguments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `arguments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime(6) DEFAULT NULL,
  `support_level` tinyint DEFAULT NULL,
  `text` varchar(255) DEFAULT NULL,
  `version` int NOT NULL,
  `created_by_id` bigint NOT NULL,
  `position_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKns6tce30pmr9ied2ug1nd4u00` (`created_by_id`),
  KEY `FKi1tev5vfub48gv4cpdg48xucy` (`position_id`),
  CONSTRAINT `FKi1tev5vfub48gv4cpdg48xucy` FOREIGN KEY (`position_id`) REFERENCES `positions` (`id`),
  CONSTRAINT `FKns6tce30pmr9ied2ug1nd4u00` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `arguments_chk_1` CHECK ((`support_level` between 0 and 4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `categorydef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categorydef` (
  `categoryid` bigint NOT NULL,
  `name` varchar(32) DEFAULT NULL,
  `pos` varchar(2) DEFAULT NULL,
  PRIMARY KEY (`categoryid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `goal_relations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_relations` (
  `id` bigint NOT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  `relation_type` enum('Conflicts','Supports') DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `goal_relations_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goal_relations_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `goals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goals` (
  `id` bigint NOT NULL,
  `date_created` datetime(6) DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `goals_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `goals_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `linkdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `linkdef` (
  `linkid` bigint NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `recurses` char(1) NOT NULL,
  PRIMARY KEY (`linkid`),
  CONSTRAINT `linkdef_chk_1` CHECK ((`recurses` in (_utf8mb4'N',_utf8mb4'Y')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `morphdef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `morphdef` (
  `morphid` int NOT NULL,
  `lemma` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`morphid`),
  UNIQUE KEY `UK5t08hpbkoi02uo9mr567irq6l` (`lemma`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `morphref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `morphref` (
  `morphid` int NOT NULL,
  `pos` varchar(2) NOT NULL,
  `wordid` int NOT NULL,
  PRIMARY KEY (`morphid`,`pos`,`wordid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `organizations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organizations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `version` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKp9pbw3flq9hkay8hdx3ypsldy` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `organizations_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `organizations_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pods` (
  `type` varchar(255) NOT NULL,
  `id` bigint NOT NULL,
  `date_created` datetime(6) DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pods_glossary_terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pods_glossary_terms` (
  `abstract_project_or_domain_id` bigint NOT NULL,
  `glossary_terms_id` bigint NOT NULL,
  PRIMARY KEY (`abstract_project_or_domain_id`,`glossary_terms_id`),
  UNIQUE KEY `UKj54xkakrtq7tq60uquyds04gi` (`glossary_terms_id`),
  CONSTRAINT `FKkkp7ng3katt9x7woa21jpy0yw` FOREIGN KEY (`glossary_terms_id`) REFERENCES `terms` (`id`),
  CONSTRAINT `FKlye5funcs8w3gr6g3ad7p2bur` FOREIGN KEY (`abstract_project_or_domain_id`) REFERENCES `pods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pods_scenarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pods_scenarios` (
  `abstract_project_or_domain_id` bigint NOT NULL,
  `scenarios_id` bigint NOT NULL,
  PRIMARY KEY (`abstract_project_or_domain_id`,`scenarios_id`),
  UNIQUE KEY `UKlh55nupebwco0pqkqjjvx7786` (`scenarios_id`),
  CONSTRAINT `FKa0syiqlptvhp8736banstk9ts` FOREIGN KEY (`abstract_project_or_domain_id`) REFERENCES `pods` (`id`),
  CONSTRAINT `FKn8xfht833jryogwufjgge3lib` FOREIGN KEY (`scenarios_id`) REFERENCES `scenarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `pods_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pods_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `positions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `positions` (
  `position_type` varchar(255) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `date_created` datetime(6) DEFAULT NULL,
  `text` varchar(255) DEFAULT NULL,
  `version` int NOT NULL,
  `proposed_word` varchar(255) DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKns7yikt55e472ljvqsf8apn1l` (`created_by_id`),
  CONSTRAINT `FKns7yikt55e472ljvqsf8apn1l` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `id` bigint NOT NULL,
  `date_created` datetime(6) DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `reports_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `scenarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenarios` (
  `type` varchar(255) NOT NULL,
  `id` bigint NOT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `text` longtext,
  `name` varchar(255) NOT NULL,
  `scenario_type` enum('Alternative','Exception','Optional','PreCondition','Primary') DEFAULT NULL,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKphgdfddctt1c221pi5ix9w9xn` (`projectordomain_id`,`name`),
  KEY `FKtd16saic0mqbcs86jkatolnpj` (`created_by_id`),
  CONSTRAINT `FKoplmulkyjqc4foqfjtcwjxm2x` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`),
  CONSTRAINT `FKtd16saic0mqbcs86jkatolnpj` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `scenarios_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `scenarios_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `semcor_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semcor_file` (
  `id` bigint NOT NULL,
  `file` varchar(255) DEFAULT NULL,
  `section` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `semcor_file_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semcor_file_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `semcor_sentence`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semcor_sentence` (
  `id` bigint NOT NULL,
  `snum` bigint DEFAULT NULL,
  `file_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKb77sj8vsvh7y6t5ynmysay3sx` (`file_id`),
  CONSTRAINT `FKb77sj8vsvh7y6t5ynmysay3sx` FOREIGN KEY (`file_id`) REFERENCES `semcor_file` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `semcor_sentence_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semcor_sentence_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `semcor_sentence_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semcor_sentence_word` (
  `id` bigint NOT NULL,
  `word_index` int DEFAULT NULL,
  `parse_tag` enum('ADJP','ADVP','CC','CD','CONJP','DT','EX','FRAG','FW','IN','INTJ','JJ','JJR','JJS','LS','LST','MD','NAC','NN','NNP','NNPS','NNS','NP','NX','PDT','POS','PP','PRN','PRP','PRP$','PRT','PUNC_DOLLAR','PUNC_DQUOTE','PUNC_NON_TERMINATOR','PUNC_SQUOTE','PUNC_TERMINATOR','QP','RB','RBR','RBS','ROOT','RP','RRC','S','SBAR','SBARQ','SINV','SQ','SYM','TO','UCP','UH','VB','VBD','VBG','VBN','VBP','VBZ','VP','WDT','WHADJP','WHAVP','WHNP','WHPP','WP','WP$','WRB','X') DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `semcor_sentence_word_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `semcor_sentence_word_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stakeholder_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stakeholder_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `entity_type` varchar(255) NOT NULL,
  `permission_type` enum('Delete','Edit','Grant') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKn2011ixwswp04atw4etb0fspb` (`entity_type`,`permission_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stakeholders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stakeholders` (
  `stakeholder_type` varchar(255) NOT NULL,
  `id` bigint NOT NULL,
  `date_created` datetime(6) DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stakeholders_goals`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stakeholders_goals` (
  `abstract_stakeholder_id` bigint NOT NULL,
  `goals_id` bigint NOT NULL,
  PRIMARY KEY (`abstract_stakeholder_id`,`goals_id`),
  UNIQUE KEY `UKk2eufeougg9qi1ipm2nvesemk` (`goals_id`),
  CONSTRAINT `FKkgv2u3ibvxjc61d4xam2ss9ww` FOREIGN KEY (`abstract_stakeholder_id`) REFERENCES `stakeholders` (`id`),
  CONSTRAINT `FKod1o4d27iru8jqjwivkw86mda` FOREIGN KEY (`goals_id`) REFERENCES `goals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stakeholders_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stakeholders_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stories` (
  `id` bigint NOT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `text` longtext,
  `name` varchar(255) NOT NULL,
  `story_type` enum('Exception','Success') NOT NULL,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKa3xtdh1wq2y6w3t1ntdgyn79u` (`projectordomain_id`,`name`),
  KEY `FKcmqxed8auy56vw8wab7dkxgyu` (`created_by_id`),
  CONSTRAINT `FK7w91ak547kj9rkhamf28tyh0u` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`),
  CONSTRAINT `FKcmqxed8auy56vw8wab7dkxgyu` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `stories_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stories_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `synset_definition_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `synset_definition_word` (
  `id` bigint NOT NULL,
  `word_index` int DEFAULT NULL,
  `parse_tag` enum('ADJP','ADVP','CC','CD','CONJP','DT','EX','FRAG','FW','IN','INTJ','JJ','JJR','JJS','LS','LST','MD','NAC','NN','NNP','NNPS','NNS','NP','NX','PDT','POS','PP','PRN','PRP','PRP$','PRT','PUNC_DOLLAR','PUNC_DQUOTE','PUNC_NON_TERMINATOR','PUNC_SQUOTE','PUNC_TERMINATOR','QP','RB','RBR','RBS','ROOT','RP','RRC','S','SBAR','SBARQ','SINV','SQ','SYM','TO','UCP','UH','VB','VBD','VBG','VBN','VBP','VBZ','VP','WDT','WHADJP','WHAVP','WHNP','WHPP','WP','WP$','WRB','X') DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `synset_definition_word_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `synset_definition_word_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `teams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teams` (
  `id` bigint NOT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  `version` int NOT NULL,
  `name` varchar(255) NOT NULL,
  `created_by_id` bigint NOT NULL,
  `projectordomain_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjm062ymiehxltopyrl8g7wfiu` (`projectordomain_id`,`name`),
  KEY `FKcq9jk9qh4ox827y0d161rabce` (`created_by_id`),
  CONSTRAINT `FKcq9jk9qh4ox827y0d161rabce` FOREIGN KEY (`created_by_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKnsujlcjs3hmo4q541g3d3495g` FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `teams_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `teams_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `terms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terms` (
  `id` bigint NOT NULL,
  `date_created` datetime(6) DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `terms_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `terms_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `usecases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usecases` (
  `id` bigint NOT NULL,
  `date_created` datetime(6) DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `usecases_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usecases_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role_permissions` (
  `id` bigint NOT NULL,
  `name` varchar(50) NOT NULL,
  `role_type` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmnjpg0c4hhycrot1lemlcf4x3` (`name`,`role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_role_permissions_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role_permissions_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `role_type` varchar(255) NOT NULL,
  `id` bigint NOT NULL,
  `version` int NOT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhfh9dx7w3ubf1co1vdev94g3f` (`user_id`),
  CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_roles_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL,
  `editable` bit(1) NOT NULL,
  `email_address` varchar(255) NOT NULL,
  `hashed_password` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `password_encrypting_algorithm_name` varchar(255) DEFAULT NULL,
  `password_encrypting_iterations` int DEFAULT NULL,
  `password_salt` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  `version` int NOT NULL,
  `organization_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  KEY `FKqpugllwvyv37klq7ft9m8aqxk` (`organization_id`),
  CONSTRAINT `FKqpugllwvyv37klq7ft9m8aqxk` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `users_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `users_user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users_user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  UNIQUE KEY `UKiol7ycdodk96wfv6xsc2m1hmu` (`role_id`),
  CONSTRAINT `FKkeoi5kuvb2tl5j7wa87jvlnyq` FOREIGN KEY (`role_id`) REFERENCES `user_roles` (`id`),
  CONSTRAINT `FKkfth240mxf8yd3ukhjmscs62w` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnframedef`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnframedef` (
  `frameid` bigint NOT NULL,
  `description1` varchar(64) DEFAULT NULL,
  `description2` varchar(64) DEFAULT NULL,
  `number` varchar(16) DEFAULT NULL,
  `semantics` mediumtext NOT NULL,
  `syntax` mediumtext NOT NULL,
  `xtag` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`frameid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnframeref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnframeref` (
  `framerefid` bigint NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnframeref_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnframeref_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnroleref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnroleref` (
  `rolerefid` bigint NOT NULL,
  `selrestrs` varchar(255) NOT NULL,
  `classid` bigint NOT NULL,
  `roletypeid` bigint NOT NULL,
  PRIMARY KEY (`rolerefid`),
  KEY `FK4orv3ls5fnlv2aacg1ocjauv8` (`classid`),
  KEY `FKmyyhld9vhyhlb06a5v0g3qil1` (`roletypeid`),
  CONSTRAINT `FK4orv3ls5fnlv2aacg1ocjauv8` FOREIGN KEY (`classid`) REFERENCES `vnclass` (`classid`),
  CONSTRAINT `FKmyyhld9vhyhlb06a5v0g3qil1` FOREIGN KEY (`roletypeid`) REFERENCES `vnroletype` (`roletypeid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnroleref_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnroleref_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnroleselres`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnroleselres` (
  `roleselresid` bigint NOT NULL,
  `include` varchar(255) DEFAULT NULL,
  `rolerefid` bigint NOT NULL,
  `roletypeid` bigint NOT NULL,
  PRIMARY KEY (`roleselresid`),
  KEY `FKa5lmytnqffufrlows0c9cotkh` (`rolerefid`),
  KEY `FKaeshx7ywhc9f4hc8aslxoob04` (`roletypeid`),
  CONSTRAINT `FKa5lmytnqffufrlows0c9cotkh` FOREIGN KEY (`rolerefid`) REFERENCES `vnroleref` (`rolerefid`),
  CONSTRAINT `FKaeshx7ywhc9f4hc8aslxoob04` FOREIGN KEY (`roletypeid`) REFERENCES `vnselres` (`vnselresid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnroleselres_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnroleselres_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnroletype`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnroletype` (
  `roletypeid` bigint NOT NULL,
  `semrole` enum('ACTOR','ACTOR1','ACTOR2','AGENT','ASSET','ATTRIBUTE','BENEFICIARY','CAUSE','DESTINATION','EXPERIENCER','EXTENT','INSTRUMENT','LOCATION','MATERIAL','OBLIQUE','PARTICIPANT','PATIENT','PATIENT1','PATIENT2','PREDICATE','PRODUCT','PROPOSITION','RECIPIENT','SOURCE','STIMULUS','THEME','THEME1','THEME2','TIME','TOPIC','VALUE','VERB') NOT NULL,
  `type` varchar(255) NOT NULL,
  PRIMARY KEY (`roletypeid`),
  UNIQUE KEY `UK578wkfgqijt8r6xis08c3029x` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnroletype_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnroletype_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnselres`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnselres` (
  `vnselresid` bigint NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `linkid` bigint DEFAULT NULL,
  `synsetid` bigint NOT NULL,
  PRIMARY KEY (`vnselresid`),
  KEY `FKhwp3q3biwna2i1np24r2glk6m` (`linkid`),
  KEY `FK5ripniqudusosvvfbx9p8f8nk` (`synsetid`),
  CONSTRAINT `FK5ripniqudusosvvfbx9p8f8nk` FOREIGN KEY (`synsetid`) REFERENCES `synset` (`synsetid`),
  CONSTRAINT `FKhwp3q3biwna2i1np24r2glk6m` FOREIGN KEY (`linkid`) REFERENCES `linkdef` (`linkid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `vnselres_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vnselres_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `word` (
  `wordid` bigint NOT NULL,
  `lemma` varchar(80) NOT NULL,
  `phonetic_code` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`wordid`),
  UNIQUE KEY `UK1ri15gaw0ns42jjs3bphg377f` (`lemma`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `word_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `word_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
--
-- WARNING: can't read the INFORMATION_SCHEMA.libraries table. It's most probably an old server 8.4.6.
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

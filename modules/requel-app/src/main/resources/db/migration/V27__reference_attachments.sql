-- Issue #273: reference attachments and document authority, on #272's record.
--
-- Every external_sources row is one of the project's references. An entity cites one with an
-- entity_source_links row of relation CITES (the column is already varchar(20), so that needs no
-- DDL). A source's note says why it matters, and a source_authority row says one source defers
-- to another: where the two disagree the superior wins, and both stay current. This is
-- precedence, not obsolescence.
--
-- None of it is read by a model: like the rest of the provenance record, these are side tables
-- that no entity graph walk reaches.

ALTER TABLE `external_sources` ADD COLUMN `note` varchar(1000) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `source_authority` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `subordinate_id` bigint NOT NULL,
  `superior_id` bigint NOT NULL,
  `note` varchar(1000) DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_source_authority` (`subordinate_id`, `superior_id`),
  KEY `idx_sa_superior` (`superior_id`),
  KEY `idx_sa_project` (`project_id`),
  CONSTRAINT `fk_sa_subordinate` FOREIGN KEY (`subordinate_id`) REFERENCES `external_sources` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sa_superior` FOREIGN KEY (`superior_id`) REFERENCES `external_sources` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

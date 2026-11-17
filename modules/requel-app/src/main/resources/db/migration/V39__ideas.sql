-- Ideas: a rough requirement idea (text, optionally from an uploaded .txt file) submitted to a
-- project with the SubmitIdea command. See doc/work/backlog/jama-requirement-assistant-on-requel.md
-- (section 4.2). No foreign keys, like ignored_findings (V21): the project and the user are
-- referred to by id, and DeleteProject removes a project's ideas explicitly.

CREATE TABLE IF NOT EXISTS `ideas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `idea_text` longtext NOT NULL,
  `source_file_name` varchar(255) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ideas_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

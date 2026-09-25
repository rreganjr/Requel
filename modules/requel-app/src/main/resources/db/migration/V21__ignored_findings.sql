-- Issue #320: an assistant finding a user ignored, kept apart from the resolved issue.
--
-- Before #320 the only record of an "Ignore" was the resolved issue itself: deleting it made the
-- next run raise the finding again, and an ignore could not be listed or undone. The key is the
-- assistant finding's idempotency key (assistant:type:id:finding-type[:property]:subject), per
-- entity and property; key_lower is it lower-cased, so an ignore is case-insensitive.
--
-- No foreign keys, like assistant_findings: the project, the entity and the issue are soft
-- references, and the project and entity delete paths remove the rows explicitly.
CREATE TABLE IF NOT EXISTS `ignored_findings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `target_type` varchar(80) NOT NULL,
  `target_id` bigint NOT NULL,
  `assistant_id` varchar(200) NOT NULL,
  `finding_type` varchar(120) NOT NULL,
  `property_name` varchar(255) DEFAULT NULL,
  `key_suffix` varchar(255) NOT NULL,
  `key_lower` varchar(255) NOT NULL,
  `subject` varchar(500) DEFAULT NULL,
  `annotation_id` bigint DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_ignored_findings_key` (`key_lower`),
  KEY `idx_ignored_findings_project` (`project_id`),
  KEY `idx_ignored_findings_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- The assistants' "Ignore this word." / "Ignore this phrase." positions become IgnorePosition, the
-- marker type the resolve hook recognizes. Positions are shared by text within a project (#284),
-- so this retypes one row per project and text, not one per issue.
UPDATE `positions`
   SET `position_type` = 'com.rreganjr.requel.annotation.impl.IgnorePosition'
 WHERE `position_type` = 'com.rreganjr.requel.annotation.impl.PositionImpl'
   AND `text` IN ('Ignore this word.', 'Ignore this phrase.');

-- Backfill an ignore for every finding whose issue was resolved with one of them, including an
-- issue shared across entities before #320 fixed the lookups, so what users see today doesn't
-- change. One row per lower-cased key (the first finding by id wins); a key that doesn't start
-- with its own assistant:type:id: prefix can't be rebuilt on import, so it is left out.
INSERT INTO `ignored_findings` (`project_id`, `target_type`, `target_id`, `assistant_id`,
                                `finding_type`, `property_name`, `key_suffix`, `key_lower`,
                                `subject`, `annotation_id`, `created_by_id`, `date_created`)
SELECT f.`project_id`, f.`target_type`, f.`target_id`, f.`assistant_id`, f.`finding_type`,
       a.`annotatable_entity_property_name`,
       SUBSTRING(f.`idempotency_key`,
                 CHAR_LENGTH(CONCAT(f.`assistant_id`, ':', f.`target_type`, ':', f.`target_id`, ':')) + 1),
       LOWER(f.`idempotency_key`),
       LEFT(COALESCE(a.`word`, f.`summary`), 500),
       a.`id`, a.`resolved_by_user_id`, a.`resolved_date`
  FROM `assistant_findings` f
  JOIN (SELECT MIN(f2.`id`) AS `id`
          FROM `assistant_findings` f2
          JOIN `annotations` a2 ON a2.`id` = f2.`applied_annotation_id`
          JOIN `positions` p2 ON p2.`id` = a2.`resolved_by_position_id`
         WHERE p2.`position_type` = 'com.rreganjr.requel.annotation.impl.IgnorePosition'
           AND f2.`project_id` IS NOT NULL
           AND LEFT(f2.`idempotency_key`,
                    CHAR_LENGTH(CONCAT(f2.`assistant_id`, ':', f2.`target_type`, ':', f2.`target_id`, ':')))
               = CONCAT(f2.`assistant_id`, ':', f2.`target_type`, ':', f2.`target_id`, ':')
         GROUP BY LOWER(f2.`idempotency_key`)) pick ON pick.`id` = f.`id`
  JOIN `annotations` a ON a.`id` = f.`applied_annotation_id`
 WHERE NOT EXISTS (SELECT 1 FROM `ignored_findings` i
                    WHERE i.`key_lower` = LOWER(f.`idempotency_key`));

-- Issue #272: entity provenance. An external source (a Jira ticket, a guide, a review) is recorded
-- once per project, and each entity built from it carries a DERIVED_FROM link naming the fragment
-- it came from. Requel is authoritative: a source is a pointer that Requel never reads and never
-- passes to a model. #273 adds a citation relation and supersedes on the same two tables.
--
-- Soft references, like ignored_findings: the project and the target entity are referred to by
-- id, and the project and entity delete paths remove the rows explicitly. external_id and
-- fragment_key use a binary collation because they are matched exactly — they have to round-trip
-- to the source system, which is one of the things tags could not do (#255).

CREATE TABLE IF NOT EXISTS `external_sources` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `source_system` varchar(40) NOT NULL,
  `external_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `locator_type` varchar(20) DEFAULT NULL,
  `locator` varchar(2048) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `kind` varchar(40) DEFAULT NULL,
  `content_hash` varchar(128) DEFAULT NULL,
  `last_ingested_at` datetime(6) DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_external_sources` (`project_id`, `source_system`, `external_id`),
  KEY `idx_external_sources_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `entity_source_links` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `relation` varchar(20) NOT NULL,
  `target_type` varchar(80) NOT NULL,
  `target_id` bigint NOT NULL,
  `fragment` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `fragment_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `fragment_hash` varchar(64) DEFAULT NULL,
  `source_hash_seen` varchar(128) DEFAULT NULL,
  `entity_fingerprint` varchar(64) DEFAULT NULL,
  `ingested_at` datetime(6) DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_entity_source_links` (`source_id`, `relation`, `target_type`, `target_id`, `fragment_key`),
  KEY `idx_esl_target` (`target_type`, `target_id`),
  KEY `idx_esl_fragment` (`source_id`, `fragment_key`),
  KEY `idx_esl_project` (`project_id`),
  CONSTRAINT `fk_esl_source` FOREIGN KEY (`source_id`) REFERENCES `external_sources` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------------------------
-- Convert #71's provenance notes. upsertGoalFromRequirement recorded where a goal came from as a
-- NOTE holding a fenced ```requel-provenance JSON block. A note is discussion furniture: it went
-- into every assistant context pack for the goal, source URL included, which is exactly what
-- #272's AC4 forbids. Each such note becomes a source row plus a DERIVED_FROM link, and the note
-- is deleted.
--
--  * fragment = criterionRef, or "hash:" + the first 12 of criterionHash without one — #71's
--    identity, so upsertGoalFromRequirement keeps resolving to the goals it made (#272 P4);
--  * fragment_hash = criterionHash, the hash of the criterion as ingested;
--  * entity_fingerprint = NULL: #71 never recorded the goal's text at ingest. The first
--    re-ingest infers it — the goal counts as unedited if its text still hashes to
--    criterionHash (#272 P6).
--
-- Only a well-formed block with sourceSystem, sourceRef and criterionHash converts; any other
-- note, a person's included, is left as it is. Idempotent: a second run finds no notes.
-- Ordinary tables rather than TEMPORARY ones, as in V17 (a temporary table cannot be referenced
-- twice in one statement), with the schema's collation spelled out so the joins against
-- external_sources cannot hit an illegal mix on a server with another default. Dropped at the end.
-- ---------------------------------------------------------------------------------------------
DROP TABLE IF EXISTS v26_provenance_notes;

CREATE TABLE v26_provenance_notes (
    note_id BIGINT NOT NULL,
    goal_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    created_by_id BIGINT NULL,
    date_created DATETIME(6) NULL,
    body JSON NOT NULL,
    PRIMARY KEY (note_id, goal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO v26_provenance_notes (note_id, goal_id, project_id, created_by_id, date_created, body)
SELECT a.id, ga.goal_impl_id, g.projectordomain_id, a.created_by_id, a.date_created,
       CAST(TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(a.text, '```requel-provenance', -1), '```', 1))
            AS JSON)
FROM annotations a
JOIN goals_annotations ga ON ga.annotations_id = a.id
JOIN goals g ON g.id = ga.goal_impl_id
WHERE a.annotation_type = 'com.rreganjr.requel.annotation.Note'
  AND a.text LIKE '%```requel-provenance%'
  AND JSON_VALID(TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(a.text, '```requel-provenance', -1),
                                      '```', 1)));

-- The block's fields as columns, trimmed, with JSON null read as SQL NULL.
DROP TABLE IF EXISTS v26_provenance;

CREATE TABLE v26_provenance (
    note_id BIGINT NOT NULL,
    goal_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    created_by_id BIGINT NULL,
    date_created DATETIME(6) NULL,
    source_system VARCHAR(255) NULL,
    source_ref VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
    source_url TEXT NULL,
    criterion_ref VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
    criterion_hash VARCHAR(255) NULL,
    PRIMARY KEY (note_id, goal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO v26_provenance (note_id, goal_id, project_id, created_by_id, date_created,
                            source_system, source_ref, source_url, criterion_ref, criterion_hash)
SELECT n.note_id, n.goal_id, n.project_id, n.created_by_id, n.date_created,
       LOWER(TRIM(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(n.body, '$.sourceSystem')), 'null'))),
       TRIM(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(n.body, '$.sourceRef')), 'null')),
       TRIM(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(n.body, '$.sourceUrl')), 'null')),
       NULLIF(TRIM(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(n.body, '$.criterionRef')), 'null')), ''),
       TRIM(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(n.body, '$.criterionHash')), 'null'))
FROM v26_provenance_notes n
WHERE JSON_TYPE(n.body) = 'OBJECT';

-- What cannot be a source or a link is not converted, and its note stays.
DELETE FROM v26_provenance
WHERE source_system IS NULL OR source_system = '' OR CHAR_LENGTH(source_system) > 40
   OR source_ref IS NULL OR source_ref = '' OR CHAR_LENGTH(source_ref) > 255
   OR criterion_hash IS NULL OR criterion_hash = '' OR CHAR_LENGTH(criterion_hash) > 64
   OR CHAR_LENGTH(COALESCE(criterion_ref, '')) > 255;

-- One source per project, system and reference. The URL becomes the locator only when it is an
-- http(s) URL that fits (#272 P7); otherwise the source has no locator.
INSERT INTO external_sources (project_id, source_system, external_id, locator_type, locator,
                              created_by_id, date_created, last_ingested_at)
SELECT p.project_id, p.source_system, p.source_ref,
       CASE WHEN MAX(p.url_ok) IS NOT NULL THEN 'URL' END,
       MAX(p.url_ok),
       MIN(p.created_by_id), MIN(p.date_created), MAX(p.date_created)
FROM (SELECT project_id, source_system, source_ref, created_by_id, date_created,
             CASE WHEN (LOWER(source_url) LIKE 'http://%' OR LOWER(source_url) LIKE 'https://%')
                       AND CHAR_LENGTH(source_url) <= 2048
                  THEN source_url END AS url_ok
      FROM v26_provenance) p
GROUP BY p.project_id, p.source_system, p.source_ref
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO entity_source_links (source_id, project_id, relation, target_type, target_id,
                                 fragment, fragment_key, fragment_hash, source_hash_seen,
                                 entity_fingerprint, ingested_at, created_by_id)
SELECT s.id, p.project_id, 'DERIVED_FROM', 'Goal', p.goal_id,
       COALESCE(p.criterion_ref, CONCAT('hash:', LEFT(p.criterion_hash, 12))),
       COALESCE(p.criterion_ref, CONCAT('hash:', LEFT(p.criterion_hash, 12))),
       p.criterion_hash, NULL, NULL, p.date_created, p.created_by_id
FROM v26_provenance p
JOIN external_sources s
  ON s.project_id = p.project_id AND s.source_system = p.source_system
 AND s.external_id = p.source_ref
ON DUPLICATE KEY UPDATE entity_source_links.id = entity_source_links.id;

-- The converted notes go, from every table that can link a note to an entity.
DELETE j FROM goals_annotations j
  JOIN v26_provenance p ON p.note_id = j.annotations_id;
DELETE j FROM actors_annotations j
  JOIN v26_provenance p ON p.note_id = j.annotations_id;
DELETE j FROM goal_relations_annotations j
  JOIN v26_provenance p ON p.note_id = j.annotations_id;
DELETE j FROM project_annotations j
  JOIN v26_provenance p ON p.note_id = j.annotations_id;
DELETE j FROM reports_annotations j
  JOIN v26_provenance p ON p.note_id = j.annotations_id;
DELETE j FROM scenarios_annotations j
  JOIN v26_provenance p ON p.note_id = j.annotations_id;
DELETE j FROM stakeholders_annotations j
  JOIN v26_provenance p ON p.note_id = j.annotations_id;
DELETE j FROM stories_annotations j
  JOIN v26_provenance p ON p.note_id = j.annotations_id;
DELETE j FROM teams_annotations j
  JOIN v26_provenance p ON p.note_id = j.annotations_id;
DELETE j FROM terms_annotations j
  JOIN v26_provenance p ON p.note_id = j.annotations_id;
DELETE j FROM usecases_annotations j
  JOIN v26_provenance p ON p.note_id = j.annotations_id;
DELETE j FROM annotation_annotatable j
  JOIN v26_provenance p ON p.note_id = j.annotation_id;
DELETE a FROM annotations a
  JOIN (SELECT DISTINCT note_id FROM v26_provenance) p ON p.note_id = a.id;

DROP TABLE v26_provenance;
DROP TABLE v26_provenance_notes;

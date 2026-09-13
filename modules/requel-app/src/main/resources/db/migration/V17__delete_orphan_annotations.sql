-- #279 Assistant/DeleteProject race: collect annotations left grouped under a project that
-- no longer exists.
--
-- An assistant run is two transactions (#247): a slow "analyze" that writes nothing, then a
-- short "apply" that writes the findings. A project deleted between the two left the apply
-- inserting rows for a project row that was already gone. Nothing stopped it:
-- annotations.grouping_object_id is an @Any soft reference with NO foreign key (see
-- V1__init.sql), as is annotation_annotatable.annotatable_id, so InnoDB accepted the writes
-- instead of rejecting them. The result is rows that no cascade will ever reach again -
-- DeleteProjectCommandImpl's own sweep runs off the project, and the project is gone.
--
-- The code fix (a write lock on the pods row taken first by both paths) closes the window.
-- This migration collects what the window already let through. It is idempotent and a no-op
-- on a clean database.
--
-- 'Project' rather than the FQCN: V2__identity_cleanup.sql already normalized
-- grouping_object_type from 'com.rreganjr.requel.project.Project' to 'Project'.

-- Index first. The orphan predicate is a NOT EXISTS against pods on grouping_object_id,
-- which had no index of its own - a full scan of annotations at every step below. The index
-- is worth keeping afterwards regardless: grouping_object_id is the soft FK this whole bug
-- hinges on, and JpaAnnotationRepository.findAnnotationIdsByGroupingObject (which
-- DeleteAnnotationGroupCommand runs on every project delete) filters on exactly these two
-- columns. MySQL 8 builds it online, so it does not block writes.
-- MySQL has no CREATE INDEX IF NOT EXISTS; use the prepared-statement guard
-- V2__identity_cleanup.sql already established in this schema.
SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'annotations'
      AND index_name = 'idx_annotations_grouping_object');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_annotations_grouping_object ON annotations (grouping_object_id, grouping_object_type)',
    'SELECT 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- The orphan set, materialized once: re-deriving it after each delete below would be
-- correct but repeats the anti-join several times, and MySQL cannot read from the table it
-- is deleting from in a subquery anyway.
--
-- Ordinary tables rather than TEMPORARY ones: MySQL cannot reference a temporary table more
-- than once in a single statement (ERROR 1137 "Can't reopen table"), and the orphan_positions
-- insert below needs v17_orphan_annotations twice. Dropped at the end.
DROP TABLE IF EXISTS v17_orphan_annotations;
DROP TABLE IF EXISTS v17_orphan_positions;

CREATE TABLE v17_orphan_annotations (
    id BIGINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO v17_orphan_annotations (id)
SELECT a.id
FROM annotations a
WHERE a.grouping_object_type = 'Project'
  -- (a) the project it is filed under is gone.
  AND NOT EXISTS (SELECT 1 FROM pods p WHERE p.id = a.grouping_object_id)
  -- (b) and it does not still annotate anything that is alive. The race files findings
  -- against entities that were deleted with the project, so normally there is nothing live
  -- here - but this migration deletes rows, and an annotation still attached to a live
  -- entity must survive even if its group is broken. Type-blind on purpose: an id has to be
  -- absent from EVERY table it could name, which errs toward keeping rows.
  AND NOT EXISTS (
        SELECT 1 FROM annotation_annotatable aa
        WHERE aa.annotation_id = a.id
          AND (   EXISTS (SELECT 1 FROM pods           t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM actors         t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM goals          t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM goal_relations t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM scenarios      t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM stories        t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM usecases       t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM terms          t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM reports        t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM stakeholders   t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM teams          t WHERE t.id = aa.annotatable_id)))
  -- (c) and no surviving entity-side join row still owns it. These carry real foreign keys,
  -- so a row here means a live entity claims the annotation.
  AND NOT EXISTS (SELECT 1 FROM project_annotations        j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM actors_annotations         j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM goals_annotations          j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM goal_relations_annotations j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM scenarios_annotations      j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM stories_annotations        j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM usecases_annotations       j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM terms_annotations          j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM reports_annotations        j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM stakeholders_annotations   j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM teams_annotations          j WHERE j.annotations_id = a.id);

-- Positions reachable only from orphan issues. positions is shared - PositionImpl.issues is
-- a @ManyToMany, so one position can answer several issues - hence the NOT EXISTS guard: a
-- position that still answers a live issue must survive. Captured BEFORE position_issue is
-- thinned, because afterwards the surviving links are what tell them apart.
CREATE TABLE v17_orphan_positions (
    id BIGINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO v17_orphan_positions (id)
SELECT DISTINCT pi.position_id
FROM position_issue pi
WHERE pi.issue_id IN (SELECT id FROM v17_orphan_annotations)
  AND pi.position_id NOT IN (
        SELECT position_id FROM position_issue
        WHERE issue_id NOT IN (SELECT id FROM v17_orphan_annotations))
  -- ...and it is not the recorded resolution of an annotation that survives. Clearing a live
  -- annotation's resolved_by_position_id to make room for this delete would be data loss;
  -- keep the position instead.
  AND pi.position_id NOT IN (
        SELECT resolved_by_position_id FROM annotations
        WHERE resolved_by_position_id IS NOT NULL
          AND id NOT IN (SELECT id FROM v17_orphan_annotations));

-- 1) arguments -> positions (FK position_id). Only for positions about to go.
DELETE FROM arguments WHERE position_id IN (SELECT id FROM v17_orphan_positions);

-- 2) the @ManyToAny link rows from each orphan annotation to whatever it annotated.
DELETE FROM annotation_annotatable WHERE annotation_id IN (SELECT id FROM v17_orphan_annotations);

-- 3) the inverse join tables (FK annotations_id -> annotations). Condition (c) above means
-- an orphan has no rows here at all, so these are no-ops - kept as a belt-and-braces step so
-- the delete in 6 cannot be blocked by a row that appeared between the two statements.
DELETE FROM actors_annotations         WHERE annotations_id IN (SELECT id FROM v17_orphan_annotations);
DELETE FROM goal_relations_annotations WHERE annotations_id IN (SELECT id FROM v17_orphan_annotations);
DELETE FROM goals_annotations          WHERE annotations_id IN (SELECT id FROM v17_orphan_annotations);
DELETE FROM project_annotations        WHERE annotations_id IN (SELECT id FROM v17_orphan_annotations);
DELETE FROM reports_annotations        WHERE annotations_id IN (SELECT id FROM v17_orphan_annotations);
DELETE FROM scenarios_annotations      WHERE annotations_id IN (SELECT id FROM v17_orphan_annotations);
DELETE FROM stakeholders_annotations   WHERE annotations_id IN (SELECT id FROM v17_orphan_annotations);
DELETE FROM stories_annotations        WHERE annotations_id IN (SELECT id FROM v17_orphan_annotations);
DELETE FROM teams_annotations          WHERE annotations_id IN (SELECT id FROM v17_orphan_annotations);
DELETE FROM terms_annotations          WHERE annotations_id IN (SELECT id FROM v17_orphan_annotations);
DELETE FROM usecases_annotations       WHERE annotations_id IN (SELECT id FROM v17_orphan_annotations);

-- 4) position <-> issue links for the orphan issues.
DELETE FROM position_issue WHERE issue_id IN (SELECT id FROM v17_orphan_annotations);

-- 5) annotations.resolved_by_position_id is an FK into positions. By construction the only
-- rows still pointing at a doomed position are themselves orphans about to be deleted in 6,
-- but MySQL checks the constraint per statement, so clear it first.
UPDATE annotations
SET resolved_by_position_id = NULL
WHERE resolved_by_position_id IN (SELECT id FROM v17_orphan_positions)
  AND id IN (SELECT id FROM v17_orphan_annotations);

DELETE FROM positions WHERE id IN (SELECT id FROM v17_orphan_positions);

-- 6) finally the orphan annotations themselves.
DELETE FROM annotations WHERE id IN (SELECT id FROM v17_orphan_annotations);

DROP TABLE IF EXISTS v17_orphan_positions;
DROP TABLE IF EXISTS v17_orphan_annotations;

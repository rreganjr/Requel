-- Guarded migration: only touch tables if they already exist (upgrade path).
-- This avoids failures on fresh schemas where annotation tables are not yet created.

-- Check for annotation_annotatable table
SET @anno_exists = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'annotation_annotatable'
);

-- Helper to run an update only if table exists
SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Project''            WHERE annotatable_type = ''com.rreganjr.requel.project.Project''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''ProjectTeam''        WHERE annotatable_type = ''com.rreganjr.requel.project.ProjectTeam''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Goal''               WHERE annotatable_type = ''com.rreganjr.requel.project.Goal''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''GoalRelation''       WHERE annotatable_type = ''com.rreganjr.requel.project.GoalRelation''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''UseCase''            WHERE annotatable_type = ''com.rreganjr.requel.project.UseCase''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Scenario''           WHERE annotatable_type = ''com.rreganjr.requel.project.Scenario''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Step''               WHERE annotatable_type = ''com.rreganjr.requel.project.Step''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Story''              WHERE annotatable_type = ''com.rreganjr.requel.project.Story''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''Actor''              WHERE annotatable_type = ''com.rreganjr.requel.project.Actor''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''GlossaryTerm''       WHERE annotatable_type = ''com.rreganjr.requel.project.GlossaryTerm''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''NonUserStakeholder'' WHERE annotatable_type = ''com.rreganjr.requel.project.NonUserStakeholder''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@anno_exists > 0, 'UPDATE annotation_annotatable SET annotatable_type = ''UserStakeholder''    WHERE annotatable_type = ''com.rreganjr.requel.project.UserStakeholder''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Check for annotations table
SET @ann_exists = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'annotations'
);

SET @sql = IF(@ann_exists > 0, 'UPDATE annotations SET grouping_object_type = ''Project'' WHERE grouping_object_type = ''com.rreganjr.requel.project.Project''', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


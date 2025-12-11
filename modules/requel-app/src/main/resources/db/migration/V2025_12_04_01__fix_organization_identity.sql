-- Guarded migration: make organizations.id auto-increment only if table exists.
-- Avoids failures on fresh databases and keeps statements single for PREPARE.

SET @org_exists = (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'organizations'
);

-- Disable FK checks if table exists
SET @sql = IF(@org_exists > 0, 'SET FOREIGN_KEY_CHECKS=0', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Alter the column if table exists
SET @sql = IF(@org_exists > 0, 'ALTER TABLE organizations MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Re-enable FK checks
SET @sql = IF(@org_exists > 0, 'SET FOREIGN_KEY_CHECKS=1', 'SELECT 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

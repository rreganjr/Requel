-- Ensure organizations.id is auto-incrementing so inserts can omit the id
-- Works for MySQL and H2 (in MySQL mode). Disable FKs temporarily so MySQL
-- allows the column alteration while referenced.
SET FOREIGN_KEY_CHECKS=0;
ALTER TABLE organizations MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
SET FOREIGN_KEY_CHECKS=1;

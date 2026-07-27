-- Cross-cutting entity tagging / categorization (issue #112, Phase 1).
-- Design & model: doc/project-entity-categorization.md (§3.3).
-- Plan: doc/112-entity-categorization-plan.md (Phase 1).
--
-- `tag` is the reusable tag/category vocabulary; `tag_taggable` is the polymorphic
-- assignment join, mirroring `annotation_annotatable` (composite PK, soft reference
-- to the tagged entity via a discriminator + id). Soft FKs (no constraint) are used
-- for the cross-module references (created_by_id -> users, project_id -> pods) so the
-- tagging tables can be added without coupling their schema to user-jpa / project-jpa
-- migration ordering, consistent with the assistant tables (V8).

CREATE TABLE tag (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    version       INT          NOT NULL DEFAULT 1,
    category      VARCHAR(255) NULL,
    `value`       VARCHAR(255) NOT NULL,
    project_id    BIGINT       NULL,
    color         VARCHAR(16)  NULL,
    created_by_id BIGINT       NULL,
    date_created  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_project_category_value (project_id, category, `value`),
    KEY idx_tag_project (project_id),
    KEY idx_tag_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tag_taggable (
    tag_id        BIGINT       NOT NULL,
    taggable_type VARCHAR(255) NOT NULL,
    taggable_id   BIGINT       NOT NULL,
    PRIMARY KEY (tag_id, taggable_type, taggable_id),
    CONSTRAINT fk_tag_taggable_tag FOREIGN KEY (tag_id) REFERENCES tag (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

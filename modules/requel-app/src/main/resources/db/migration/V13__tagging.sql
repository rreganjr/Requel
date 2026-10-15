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

-- Typed categories (issue #112, Phase 6). Optional rules overlay keyed by (project_id, name):
-- exclusivity, allowed entity types, a controlled value list, and a fallback colour. Purely
-- additive — a tag whose `category` has no matching row here behaves exactly as before.
CREATE TABLE tag_category (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    version       INT          NOT NULL DEFAULT 1,
    project_id    BIGINT       NULL,
    name          VARCHAR(255) NOT NULL,
    exclusive     BIT          NOT NULL DEFAULT 0,
    color         VARCHAR(16)  NULL,
    created_by_id BIGINT       NULL,
    date_created  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_category_project_name (project_id, name),
    KEY idx_tag_category_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Allowed entity-type discriminators for a category; empty = any entity type.
CREATE TABLE tag_category_allowed_type (
    tag_category_id BIGINT       NOT NULL,
    entity_type     VARCHAR(255) NOT NULL,
    PRIMARY KEY (tag_category_id, entity_type),
    CONSTRAINT fk_tcat_allowed_type FOREIGN KEY (tag_category_id) REFERENCES tag_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Controlled value list for a category; empty = any value (curate-later).
CREATE TABLE tag_category_value (
    tag_category_id BIGINT       NOT NULL,
    tag_value       VARCHAR(255) NOT NULL,
    PRIMARY KEY (tag_category_id, tag_value),
    CONSTRAINT fk_tcat_value FOREIGN KEY (tag_category_id) REFERENCES tag_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

# Phase 6 — Typed categories (design note)

Tracking issue: https://github.com/rreganjr/Requel/issues/112
Plan: `doc/112-entity-categorization-plan.md` (Phase 6, optional).
Status: proposal for approval before implementation.

## Summary

Promote a tag's `category` from a free string into an optional **typed category** that can carry
rules: exclusivity (single-value dimension), allowed entity types, a controlled value list, and a
fallback colour. Implemented as an **overlay** — `tag.category` stays a string; a new
`tag_category` table holds the rules keyed by (scope, name). Where a matching category row exists its
rules apply; where none exists, behaviour is exactly as today. Nothing in Phases 1–5 (the tag entity,
commands, DTOs, by-name XML tokens, Angular) changes shape.

Reminder of the three axes so naming stays clear:

- **entity type** — what is tagged (`Goal`, `Actor`, `Project`), on `tag_taggable.taggable_type`.
- **category** — the tag's dimension/grouping (`type`, `projectKind`), on `tag.category`.
- **value** — the label within the dimension (`business-rule`, `product`), on `tag.value`.

## v1 rules (agreed)

- **Exclusivity** — an entity may hold at most one tag from an exclusive category. Assigning a second
  value in that category **replaces** the existing one.
- **Allowed entity types** — a category may restrict which entity types its tags attach to (e.g.
  `projectKind` only on `Project`).
- **Controlled value list (curate-later)** — a category may carry a fixed set of allowed values.
  Graceful: a category with **no** values defined accepts any value (today's behaviour); once values
  are curated, off-list values are rejected.
- **Colour fallback** — `tag_category.color` is a default; a tag's own `tag.color` overrides it.
- **Ordering** — alphabetical everywhere; no display-order column.

## Data model (overlay)

`tag` is unchanged. New tables:

```
tag_category
  id             bigint PK
  version        int
  project_id     bigint NULL         -- NULL = global category
  name           varchar(255)        -- the category name, e.g. "type"
  exclusive      bit                 -- single-value dimension
  color          varchar(16) NULL    -- fallback colour
  created_by_id  bigint NULL, date_created datetime
  unique (project_id, name)

tag_category_allowed_type              -- optional; empty = any entity type
  tag_category_id bigint, entity_type varchar(255)   -- discriminator, e.g. "Goal"
  PK (tag_category_id, entity_type)

tag_category_value                     -- optional; empty = any value (curate-later)
  tag_category_id bigint, value varchar(255)         -- normalized slug
  PK (tag_category_id, value)
```

The `tag_category` tables are added directly to `V13__tagging.sql` (nothing is deployed yet — all
#112 work is on the `112-*` branch), alongside `tag`/`tag_taggable`. No backfill: existing tags keep
their string categories and simply have no rules until an admin defines a matching `tag_category` row.

### Scope resolution

A tag's governing category is resolved by name against the tag's scope, mirroring tag scoping: look
for a **project** category `(tag.projectId, name)`, else a **global** category `(null, name)`, else
none (no rules). So a project can specialise a category, and global categories apply everywhere.

## Enforcement points (command layer)

- **EditTag** (create/update): after normalising `category`/`value`, resolve the governing category.
  If it defines a value list and `value` is not in it → reject (`EntityValidationException` → 422).
- **AssignTag**: resolve the assigned tag's category.
  - If it has allowed entity types and the taggable's discriminator is not among them → reject.
  - If it is exclusive: before attaching, detach any tag already on that entity whose category is the
    same (replace-on-exclusive). Uses `findTagsOnEntity(entityType, id)` filtered by category.

Rule lookups are indexed `(project_id, name)` reads on the small `tag_category` table, on writes
only — no read-path impact.

## API / CQRS

Mirror the tag command/query surface (Phase 2), so categories are managed the same way:

- `tagging-domain`: `TagCategory` interface + `EditTagCategoryCommand` / `DeleteTagCategoryCommand` +
  factory methods. Rules resolution helper (`TagCategoryResolver`-style) used by the tag commands.
- `tagging-jpa`: `TagCategoryImpl` + child collections; command impls; repository
  (`findCategory(projectId, name)`, `findCategoriesForProject`).
- `service-api` / `service-impl`: `TagCategoryDto`, a registrar for the category commands, and
  `/api/tag-categories` reads (categories for a project + global, with their rules) so the selector
  can enforce client-side.

## Angular

- Category admin: manage global categories in the existing Global Tags admin area, and project
  categories alongside the project tag UI (create category, toggle exclusive, set allowed entity
  types, curate values, set colour).
- Tag selector enforcement: for an exclusive category, selecting a value **replaces** the current
  one; when a value list exists, restrict the value input to those options; render the category
  colour as the chip default (tag colour wins if set).

## Implementation chunks (each independently green)

1. **6a — model + enforcement (backend).** `tag_category` (+ child tables) added to `V13`,
   domain/jpa entity + repository, and rule enforcement wired into EditTag/AssignTag, with ITs for
   exclusivity, allowed-entity-types, and value-list (categories created via the repository in the
   tests). No management API/UI yet.
2. **6b — category management CQRS.** Edit/Delete category commands, DTO, registrar, and
   `/api/tag-categories` queries, with MockMvc/service tests.
3. **6c — Angular.** Category admin UI + selector enforcement (exclusive replace, value-list
   restriction, colour), with specs.

## Acceptance

`mvn clean verify` and `ng test` green; ITs prove exclusivity + allowed-entity-type + value-list
enforcement; the selector prevents two values from an exclusive category. Additive only — Phases 1–5
behaviour is unchanged when no category rows exist.

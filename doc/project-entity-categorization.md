# Project Entity Categorization (Tags & Categories)

Status: Draft / proposal for discussion. Not yet tied to a release issue.
Author: Ron (with Claude)
Related patterns: `annotation-domain` / `annotation-jpa`, `AnnotatableTypeRegistry`, quik `tag` / `tagEntity`.

## Summary

Add a small, cross-cutting **tagging/categorization** capability that can attach tags to any
Requel project entity (`Goal`, `Project`, `Actor`, `Story`, `Scenario`, `Stakeholder`, `UseCase`,
…). The primary driver is segmenting **Goals** by kind — a goal is generic prose that might be a
business rule, a performance expectation, or a technical guideline — without spawning a new
near-duplicate entity type for each variant. The same mechanism categorizes **Projects** (e.g.
`product` vs `feature`) and is open for user-defined categories we haven't anticipated.

Recommendation: **build a generic tagging module, not new sibling entities.** This document argues
why, surveys prior art, and proposes a data model, rules, and an implementation approach that reuses
the decoupled polymorphic-attachment pattern the annotation module already proves out in this
codebase.

Two product decisions are settled and baked into this plan:

- **Scope:** tags are **per-project, plus an optional global (system) set** shared across projects.
- **Shape:** tags are **both flat and namespaced** — a tag may be a plain label (`performance`) or
  carry an optional **category key** (`type=business-rule`, `projectKind=product`).

## 1. Is tagging better than new entities?

Yes, for this problem. "A goal is prose; a business-rule-goal is a goal that happens to be a
business rule" is a *classification*, not a new aggregate. Modeling each variant as its own
`@Entity` (a `BusinessRuleGoal`, `PerformanceGoal`, `TechnicalGuidelineGoal`) would mean:

- Duplicated persistence, repositories, commands, DTOs, and query endpoints per variant.
- A rigid, developer-only taxonomy — every new category is a schema change and a release.
- Awkward cross-cutting queries ("all goals of any performance-ish kind").
- Combinatorial blow-up when a second dimension appears (kind × priority × source).

A tag/category layer instead gives you:

- **One** reusable mechanism across all entity types (Goals today, Projects, Actors, Stories next).
- **Data-driven** taxonomy — users/admins add categories without code changes.
- **Multi-dimensional** classification (a goal can be `type=business-rule` *and* `domain=billing`).
- Clean segmentation queries ("goals where `type=performance`").

When a "category" would carry real behavior, structured fields, its own lifecycle, or relationships
of its own, it deserves a first-class entity — tags are the wrong tool there. That is the one line
to hold: **tags classify; entities model behavior and structure.** Goal-kind, project-kind, and
ad-hoc user labels are classification, so they belong in tags.

## 2. Prior art (how others do this)

The dominant open-source pattern is exactly what you proposed — a **tag definition** table plus a
**polymorphic mapping** table:

- **acts-as-taggable-on** (Rails): a `tags` table and a polymorphic `taggings` join table
  (`taggable_id` + `taggable_type` + optional `context`/`tagger`). The `context` column is a
  built-in *namespace* — the same idea as our category key (e.g. `:skills`, `:interests`).
- **django-taggit**: a `Tag` model plus a `TaggedItem` "through" model using Django's generic
  foreign key (`content_type` + `object_id`) — a soft-polymorphic reference. It explicitly supports
  swapping in a custom through model to add fields (e.g. "official") or real FKs — the same
  extension seam we want.
- **GitLab / Jira / GitHub labels**: a label definition scoped to a project (or group), attached to
  issues/MRs via a join. **GitLab scoped labels** (`key::value`, e.g. `priority::high`) are the
  productized version of namespaced tags and mutually-exclusive dimensions — directly analogous to
  our `type=business-rule`.
- **Discourse tags**: flat tags plus **tag groups**, where a group can enforce "only one tag from
  this group" — again, category-as-dimension with optional exclusivity.

Our own **quik** backend already ships this pattern (`com.platformq.tag`): a `tag` definition table
(unique name, `siteId` scope, soft-delete, audit columns) and a single polymorphic `tagEntity`
junction (`type` discriminator enum `EVENT|PRESENTATION|SITE|INSTANCE` + loose `entity_id`, unique
on `(type, tag_id, entity_id)`), with a scope hierarchy resolved as tag *inheritance* at query time.
Requel should mirror the shape but improve two things quik left on the table: give the category/key
a real home (quik has no namespace, color, or description columns), and make the polymorphic binding
type-safe via Hibernate `@ManyToAny` + a registry rather than raw native SQL against a soft
reference.

## 3. Proposed model

### 3.1 Concepts

- **Tag** — a reusable definition: an optional **category key** + a **value/label**, a scope
  (project or global), plus audit/soft-delete metadata. Examples: `(type, business-rule)`,
  `(type, performance)`, `(projectKind, product)`, or plain `(null, performance)`.
- **Category** (a.k.a. key / dimension / namespace) — for the first cut this is simply the `key`
  string on a Tag (like acts-as-taggable-on `context` or a GitLab scoped-label key). We can promote
  it to its own `tag_category` table later if we need per-category rules (exclusivity, allowed
  entity types, display order, controlled value lists). Starting with a string keeps v1 small and
  matches the "users define their own" goal.
- **Tag assignment** — the polymorphic link between one Tag and one taggable entity.

### 3.2 The reusable attachment pattern (reuse, don't reinvent)

The annotation module already solves "attach a cross-cutting thing to any project entity without the
cross-cutting module depending on `project-jpa` impls." We copy that exact seam:

- A generic marker interface `Taggable` (analogous to `Annotatable`), which the project entities
  opt into. Note every `ProjectOrDomainEntity` *already* implements `Annotatable`; `Taggable` is the
  same one-method style marker.
- A Hibernate `@ManyToAny` mapping on the assignment side with `@AnyDiscriminator(STRING)` +
  `@AnyKeyJavaClass(Long.class)` and **no inline `@AnyDiscriminatorValue` entries** — the
  discriminator→class map is injected at Hibernate bootstrap.
- A runtime SPI registry `TaggableTypeRegistry` (copy of `AnnotatableTypeRegistry` /
  `DefaultAnnotatableTypeRegistry`) resolving `discriminator string ⇄ Class` for controllers.
- A `TagMetadataContributor implements org.hibernate.boot.spi.AdditionalMappingContributor` (copy of
  `ProjectAnnotatableMetadataContributor`) that walks the tagging persistent classes and injects the
  discriminator metadata at boot — registered via
  `requel-app/src/main/resources/META-INF/services/org.hibernate.boot.spi.AdditionalMappingContributor`.
- A single `@Configuration` in `project-jpa` (copy of `ProjectAnnotatableRegistryConfiguration`)
  that declares which project impls are taggable and pushes that map both to the contributor
  (static, for Hibernate boot) and the runtime registry (via `InitializingBean`).

This deliberately adopts the **decoupled contributor** style over the older coupled inline
`@AnyDiscriminatorValue` style still seen in `GoalImpl.getReferers()`. The contributor style is what
keeps `tagging-jpa` free of any dependency on `project-jpa` impl classes.

### 3.3 Tables

Two tables, mirroring quik but with a real category column and a proper polymorphic join.

`tag` — the definition (the vocabulary):

| column | notes |
|---|---|
| `id` bigint PK | `GenerationType.IDENTITY`, matching every other entity |
| `version` int | optimistic locking, init `1` |
| `category` varchar(255) NULL | the key / namespace / dimension; NULL = a flat tag |
| `value` varchar(255) NOT NULL | the label |
| `project_id` bigint NULL | owning project; NULL = global/system tag |
| `description` varchar(...) NULL | optional human description |
| `color` varchar(16) NULL | optional UI hint (quik lacks this; cheap to add now) |
| `created_by` / `created_on` / soft-delete cols | follow existing audit conventions |
| unique `(project_id, category, value)` | see rules below |

`tag_taggable` — the polymorphic assignment (copy `annotation_annotatable` shape):

| column | notes |
|---|---|
| `tag_id` bigint | FK → `tag.id` |
| `taggable_type` varchar(255) | discriminator string (`Goal`, `Project`, …) from the registry |
| `taggable_id` bigint | the tagged entity's id (soft reference, like the annotation join) |
| PK `(tag_id, taggable_type, taggable_id)` | one assignment per (tag, entity) |

Flyway migration lands as the next version in
`modules/requel-app/src/main/resources/db/migration/` (currently
`V13__tagging.sql`; confirm the highest existing `V` at implementation time).

### 3.4 Segmentation examples

- **Goal kinds:** tags `(type, business-rule)`, `(type, performance)`,
  `(type, technical-guideline)` assigned to `GoalImpl` rows. "Show all performance goals" =
  goals joined to a `type=performance` tag. A goal may hold several `type` values only if we allow
  it; typically `type` is a single-value dimension (see exclusivity rule).
- **Project kinds:** tags `(projectKind, product)`, `(projectKind, feature)` on `ProjectImpl`.
- **Ad-hoc user labels:** flat tag `(null, billing)` across goals, stories, and actors that all
  touch billing — the multi-entity, multi-dimension case that new entities can't express.

## 4. Rules & guardrails

Naming / identity

- A tag's identity is `(category, value)` within its scope. `category` is optional; `value` is
  required and non-empty.
- **Normalize** on write: trim; store `category` and `value` in a canonical case (recommend
  lowercase-with-hyphens slugs, e.g. `business-rule`) while keeping a display form if desired. quik
  only trims and relies on convention — we should enforce it in the command layer.
- **Uniqueness** is `(project_id, category, value)`. A project may shadow a global tag value only if
  we decide to; default is that global and project tags are distinct rows and the resolver prefers
  project-scoped.

Scope

- Every tag is either **global** (`project_id` NULL) or **project-scoped**. Project entities may be
  tagged with either their own project's tags or global tags.
- Deleting a project soft-deletes (or cascades) its project-scoped tags and their assignments;
  global tags are untouched.

Assignment

- Assignments are polymorphic via the registry; a controller turns a request's `entityType` string
  into a `Class` and loads the entity (same trick `AnnotationCommandRegistrar.loadAnnotatable` uses).
- Prefer **incremental add/remove** of individual assignments over quik's full delete-then-reinsert
  "set the whole tag set" call, to avoid the race conditions quik had to guard against (CON-3330).
  A "replace set" convenience command can exist on top.
- Assignment carries audit (`created_by`, `created_on`).

Categories / dimensions

- v1: `category` is a free string; the set of categories is discovered from existing tags (for
  autocomplete). No exclusivity enforcement yet.
- v2 (optional): promote to a `tag_category` table to support **single-value dimensions**
  (a goal has at most one `type`), **allowed entity types** per category (`projectKind` only on
  Projects), controlled value lists, ordering, and color-at-the-category level. This is the
  GitLab-scoped-label / Discourse-tag-group upgrade path; defer until a real need appears.

Authorization

- Route tag commands through the existing `AuthorizingCommandHandler` chain. Editing a
  project-scoped tag requires project edit rights; managing global tags is an admin capability.
  Define the requirements the same way other `AuthorizableCommand`s do (see `doc/AUTH_ARCH.md`).

Import/export

- Tags and assignments must round-trip through the JAXB project XML (`doc/samples/project.xsd`) if
  they are considered part of a project's content. Add mappings + a round-trip regression test
  alongside the existing `ProjectXmlStreamingRoundTripIT`. Global tags referenced by a project need
  an export strategy (embed vs reference-by-key) — flag for decision.

## 5. Module & wiring plan

Follow the domain+jpa pair convention exactly as annotation does.

1. **`tagging-domain`** — `Taggable` marker; `Tag` / `TagAssignment` interfaces (extend
   `CreatedEntity` / `Describable`); `TagCommandFactory extends CommandFactory`; command interfaces
   (`EditTagCommand`, `DeleteTagCommand`, `AssignTagCommand`, `UnassignTagCommand`); `spi/`
   `TaggableTypeRegistry` + `DefaultTaggableTypeRegistry`. Depends only on `platform-core` /
   `platform-identity`.
2. **`tagging-jpa`** — `AbstractTag` / `TagImpl` `@Entity`; the `@ManyToAny` assignment mapping;
   `TagMetadataContributor`; `JpaTagRepository extends AbstractJpaRepository`; command impls under
   `.../impl/command/`; `TagCommandFactoryImpl extends AbstractCommandFactory`.
3. **`project-jpa`** — `TaggableRegistryConfiguration` (`@Configuration`) declaring
   `Goal→GoalImpl, Project→ProjectImpl, Actor→ActorImpl, Story→StoryImpl, Scenario→ScenarioImpl,
   Stakeholder→…, UseCase→UseCaseImpl`; project impls implement `Taggable`.
4. **`requel-app`** — add both modules to the reactor `<modules>` and as dependencies; append the
   contributor to the `META-INF/services/...AdditionalMappingContributor` file; add
   `V13__tagging.sql`. Component-scan + `@EntityScan("com.rreganjr.requel")` pick up the rest
   automatically.
5. **CQRS API** — a `TagCommandRegistrar` (`@Component` + `@PostConstruct`, copy
   `AnnotationCommandRegistrar`) registering each command with its input DTO + result extractor;
   DTOs (`TagDto`, `EditTagInput`, `AssignTagInput`) in `service-api`; a
   `TagQueryController @RequestMapping("/api/tags")` resolving `entityType` via
   `TaggableTypeRegistry` and exposing "tags on entity" / "entities with tag" / "tags in project".
   No change to the generic `CommandController` is needed.
6. **Angular** — a tag chip/selector component with category-aware autocomplete; a Goal-list filter
   by `type`; reuse in the Project view for `projectKind`.

Suggested phasing: (1) module skeleton + tables + `Taggable` on Goal + assign/unassign +
`/api/tags` reads; (2) namespaced categories + Goal segmentation UI; (3) extend to Project/others +
global tags admin; (4) JAXB round-trip + export decision; (5) optional `tag_category` promotion.

## 6. Open questions

- Do we need the `tag_category` table in v1, or is a free-string `category` enough to start?
- Are global tags exported inside each project's XML, or referenced by key?
- Should `type` (goal kind) be a *required* single-value dimension from day one, or stay optional?
- Do we want per-user private tags eventually? (Deferred; the model leaves room via a nullable
  `owner`/scope discriminator if we add it.)

## 7. References in this codebase

- Attachment pattern: `annotation-jpa/.../impl/AbstractAnnotation.java`,
  `project-jpa/.../impl/config/ProjectAnnotatableMetadataContributor.java`,
  `project-jpa/.../impl/config/ProjectAnnotatableRegistryConfiguration.java`,
  `annotation-domain/.../spi/AnnotatableTypeRegistry.java`,
  `requel-app/src/main/resources/META-INF/services/org.hibernate.boot.spi.AdditionalMappingContributor`.
- CQRS: `service-impl/.../service/command/CommandController.java`,
  `.../command/AnnotationCommandRegistrar.java`, `.../query/AnnotationQueryController.java`,
  `service-api/.../service/api/CommandRegistration.java`.
- Persistence: `platform-core/.../repository/jpa/AbstractJpaRepository.java`,
  `platform-core/.../command/AbstractCommandFactory.java`.
- Anti-pattern to avoid (coupled inline discriminators): `project-jpa/.../impl/GoalImpl.java`
  `getReferers()`.
- Migrations: `modules/requel-app/src/main/resources/db/migration/` (see `V1__init.sql` for the
  `annotation_annotatable` DDL to mirror).

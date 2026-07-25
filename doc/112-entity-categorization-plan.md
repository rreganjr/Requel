# Entity Categorization (Tags & Categories) — Implementation Plan

Design rationale and model: see `doc/project-entity-categorization.md`.
Tracking issue: https://github.com/rreganjr/Requel/issues/112

## Summary

Deliver a cross-cutting `tagging` capability that attaches tags to any project entity, driven first
by segmenting **Goals** by kind (business-rule / performance / technical-guideline) and categorizing
**Projects** (`product` / `feature`). Tags are **per-project plus an optional global set**, and may
be **flat** (`performance`) or **namespaced** (`type=business-rule`). The polymorphic attachment
reuses the annotation module's decoupled pattern (`@ManyToAny` + `TaggableTypeRegistry` +
`AdditionalMappingContributor`), so the tagging modules never depend on `project-jpa` impls.

Work is split into 6 phases behind a stable data model, each landing on its own ticket branch off
`release/2.0` with `mvn clean verify` (and `ng test` when `requel-angular/` changes) green before
commit. Phases 1–3 are the MVP (create tags, tag Goals, segment Goals in the UI); 4–6 extend to all
entity types, JAXB round-trip, and the optional typed-category upgrade.

## Guiding constraints

- **Domain purity:** `tagging-domain` depends only on `platform-core` / `platform-identity`; no JPA,
  no project impls. `tagging-jpa` never imports `project-jpa` impl classes — the discriminator map is
  contributed from `project-jpa` via a `@Configuration` + Hibernate `AdditionalMappingContributor`.
- **Reuse the proven seam:** copy `AbstractAnnotation`'s `@ManyToAny` mapping,
  `AnnotatableTypeRegistry` / `DefaultAnnotatableTypeRegistry`,
  `ProjectAnnotatableMetadataContributor`, and `ProjectAnnotatableRegistryConfiguration`. Do **not**
  copy the coupled inline `@AnyDiscriminatorValue` style from `GoalImpl.getReferers()`.
- **CQRS unchanged:** register commands through a `TagCommandRegistrar` (copy
  `AnnotationCommandRegistrar`); the generic `CommandController` needs no edits. Authorize via the
  existing `AuthorizingCommandHandler` chain.
- **Every phase is releasable:** the schema from Phase 1 is stable; later phases add columns/tables
  additively, never rewrite Phase 1 tables.

## Phase 0 — Decisions to lock before coding

Resolve the open questions from the design doc so Phase 1 doesn't churn:

- `tag_category` table in v1, or free-string `category`? **Proposed: free-string for v1**, promote in
  Phase 6.
- Global tags in project XML export: embed vs reference-by-key? **Proposed: reference-by-key**,
  finalize in Phase 5.
- Is Goal `type` a required single-value dimension? **Proposed: optional in v1**, enforce via typed
  categories in Phase 6.
- Per-user private tags? **Deferred**; leave a nullable owner/scope seam.

Acceptance: decisions recorded as a comment on the tracking issue.

## Phase 1 — Module skeleton, schema, and polymorphic attachment

Goal: a `tagging-domain` / `tagging-jpa` pair wired into `requel-app`, tables created, and a Goal can
be attached to a tag at the persistence layer (proven by an integration test). No API/UI yet.

Scope:

- New modules `modules/tagging-domain` and `modules/tagging-jpa` (parent `Requel-parent`), added to
  the reactor `<modules>` and as `requel-app` dependencies.
- `tagging-domain`: `Taggable` marker interface; `Tag` and `TagAssignment` domain interfaces
  (extend `CreatedEntity` / `Describable`); `spi/TaggableTypeRegistry` +
  `DefaultTaggableTypeRegistry` (`@Component`, copy of the annotation registry).
- `tagging-jpa`: `AbstractTag` / `TagImpl` `@Entity` (id `GenerationType.IDENTITY`, `@Version`);
  the `@ManyToAny` assignment mapping with `@AnyDiscriminator(STRING)` + `@AnyKeyJavaClass(Long.class)`
  and **no inline discriminator values**; `TagMetadataContributor implements
  AdditionalMappingContributor` (copy of `ProjectAnnotatableMetadataContributor`, changing the
  package-prefix filter); `JpaTagRepository extends AbstractJpaRepository`.
- `project-jpa`: `TaggableRegistryConfiguration` (`@Configuration`, copy of
  `ProjectAnnotatableRegistryConfiguration`) declaring the taggable impls; project impls implement
  `Taggable` (start with `GoalImpl` and `ProjectImpl`).
- `requel-app`: append `com.rreganjr.requel.tagging.impl.config.TagMetadataContributor` to
  `src/main/resources/META-INF/services/org.hibernate.boot.spi.AdditionalMappingContributor`.
- Flyway `Vnn__tagging.sql` (next version after the current highest — presently V12, so **V13**;
  confirm at build time): `tag` and `tag_taggable` tables per the design doc §3.3.

Files (new unless noted): `modules/tagging-domain/**`, `modules/tagging-jpa/**`,
`modules/project-jpa/.../impl/config/TaggableRegistryConfiguration.java`,
`modules/project-jpa/.../impl/GoalImpl.java` (add `implements Taggable`),
`modules/project-jpa/.../impl/ProjectImpl.java` (add `implements Taggable`),
`modules/requel-app/src/main/resources/META-INF/services/org.hibernate.boot.spi.AdditionalMappingContributor`,
`modules/requel-app/src/main/resources/db/migration/V13__tagging.sql`, root `pom.xml`,
`modules/requel-app/pom.xml`.

Acceptance / verification:

- `mvn clean verify` green.
- New IT `TagAssignmentIT` (H2, mirrors `AnnotationAnyMappingTest` style): create a project + goal,
  create a `type=business-rule` tag, assign it, reload, assert the assignment resolves back to the
  goal via the registry; assign the same tag to a `ProjectImpl` and assert the polymorphic
  discriminator distinguishes them.
- App boots with the new Hibernate contributor (no mapping errors at startup).

## Phase 2 — Tag CRUD + assignment commands and query API

Goal: full CQRS surface to create/edit/delete tags and assign/unassign them, resolved polymorphically.

Scope:

- `tagging-domain`: `TagCommandFactory extends CommandFactory`; command interfaces `EditTagCommand`,
  `DeleteTagCommand`, `AssignTagCommand`, `UnassignTagCommand` (+ optional `ReplaceEntityTagsCommand`).
- `tagging-jpa`: command impls under `.../impl/command/` (extend an `AbstractTagCommand`);
  `TagCommandFactoryImpl extends AbstractCommandFactory`. Normalize on write (trim; canonical
  lowercase-hyphen slug for `category`/`value`); enforce `(project_id, category, value)` uniqueness.
- `service-api`: DTOs/records `TagDto`, `EditTagInput`, `AssignTagInput`, `UnassignTagInput`
  (name DTOs `*Dto` so the `CommandController` SSE refresh works for free).
- `service-impl`: `TagCommandRegistrar` (`@Component` + `@PostConstruct`, copy
  `AnnotationCommandRegistrar`) registering each command with its input applicator + result
  extractor, resolving `entityType` via `TaggableTypeRegistry` (mirror `loadAnnotatable`).
  `TagQueryController @RequestMapping("/api/tags")`: tags-in-project, tags-on-entity,
  entities-with-tag, distinct categories (for autocomplete).
- Authorization: define `getAuthorizationRequirement()` on the commands — project-scoped tag edits
  require project edit rights; global tag management requires an admin capability (see
  `doc/AUTH_ARCH.md`).

Acceptance / verification:

- `mvn clean verify` green.
- MockMvc tests (mirror the annotation REST tests): POST `EditTag`, `AssignTag`, `UnassignTag`,
  `DeleteTag`; GET each query endpoint; assert 422 on blank value / duplicate `(project,category,
  value)`, 403 on unauthorized global-tag edit, 409 on optimistic-lock conflict.
- Add tag command types to any gateway allow/deny list check as needed.

## Phase 3 — Goal segmentation UI (MVP complete)

Goal: users can tag goals and filter the goal list by tag / category in the Angular app.

Scope (`requel-angular/`):

- A reusable tag chip/selector component with category-aware autocomplete (distinct categories +
  values from `/api/tags`), used on the Goal detail view.
- Goal-list filter by `type` (and free tags); show tag chips on goal rows.
- Wire to the command API via the existing command dispatch client; honor `PermissionService`.

Acceptance / verification:

- `cd requel-angular && ng test --watch=false` green.
- Manual: create `type` tags, tag several goals, filter the list to `type=performance`, confirm
  chips render and permissions gate editing.

## Phase 4 — Extend to remaining entity types + global tags admin

Scope:

- Register `Actor`, `Story`, `Scenario`, `Stakeholder`, `UseCase` impls as `Taggable` in
  `TaggableRegistryConfiguration`; add `implements Taggable` to each impl.
- Project categorization UI (`projectKind=product|feature`) on the Project view.
- Admin surface for managing the global tag set (create/edit/retire), gated by the admin capability.

Acceptance / verification: `mvn clean verify` + `ng test` green; ITs cover assignment to each newly
taggable type; manual check of Project categorization and global-tag admin.

## Phase 5 — JAXB import/export round-trip

Scope:

- Extend the JAXB mappings and `project.xsd` so tags and assignments round-trip; implement the
  chosen global-tag export strategy (reference-by-key per Phase 0).
- Follow the AggregateAssembler / ImportUnitOfWork pattern; no repository access from constructors.

Acceptance / verification: extend `ProjectXmlStreamingRoundTripIT` with a project carrying flat +
namespaced tags and global-tag references; assert lossless round-trip; `mvn clean verify` green.

## Phase 6 — (Optional) Typed categories

Scope:

- Promote `category` to a `tag_category` table supporting single-value dimensions (a goal has at
  most one `type`), allowed entity types per category, controlled value lists, ordering, and
  category-level color. Additive migration; existing string categories migrate to rows.
- Enforce dimension exclusivity at the command layer; surface it in the selector UI.

Acceptance / verification: migration + ITs for exclusivity and allowed-entity-type validation; UI
prevents picking two values from an exclusive dimension; `mvn clean verify` + `ng test` green.

## Cross-cutting testing & docs

- New IT classes live in `modules/requel-app/src/test/` alongside `AnnotationAnyMappingTest`,
  `AuthorizationIT`, and `ProjectXmlStreamingRoundTripIT`.
- Update `doc/MODULARIZATION_PLAN.md` with the new module pair and dependency edges.
- Keep `doc/project-entity-categorization.md` in sync if the model changes during implementation.

## Per-phase Git workflow (per CLAUDE.md)

For each phase: cut `<issue#>-<slug>` off `release/2.0` → implement → `mvn clean verify`
(+ `ng test --watch=false` if `requel-angular/` changed) → write `commit.md` with a `Closes #<n>`
line → commit + push **only when told** → `gh pr create --base release/2.0` → after squash-merge,
`gh issue close <n>` explicitly (auto-close doesn't fire because PRs target `release/2.0`, not the
default branch). Consider one umbrella issue with a sub-issue per phase, mirroring `issue_69_subtasks.md`.

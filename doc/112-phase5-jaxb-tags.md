# Phase 5 — Tags in the Project XML round-trip (revised design)

Tracking issue: https://github.com/rreganjr/Requel/issues/112
Plan: `doc/112-entity-categorization-plan.md` (Phase 5).
Status: proposal for approval before implementation. Supersedes the earlier entity-embedded draft.

## Summary

Tags survive a project export → import round-trip by being written **by name** as simple string
tokens on the entities they're assigned to — not as embedded tag entities. This keeps the persistence
and JAXB-marshal graph free of any tag class, so the Phase 1 decoupling seam holds and no
`project-jpa → tagging-jpa` dependency is introduced. Export still uses JAXB marshalling (unchanged);
import stays StAX. A round-trip IT proves it.

## Decisions (locked in discussion)

- **By-name tokens**, not embedded tag definitions.
- **Token format:** `category:value[color]`, where `:` (category) and `[color]` are optional →
  `value`, `value[color]`, `category:value`, `category:value[color]`.
- **No description** on tags — removed from the model entirely (prerequisite refactor, below).
- **No cross-scope name collision:** a project may not define a tag whose `(category,value)` already
  exists as a global tag. Enforced in `EditTag`. Different projects may independently define the same
  key (they never share XML). This makes a token resolve unambiguously on import.
- **Unassigned project tags are not preserved.** The project's tag vocabulary is reconstructed from
  assignments only; a project-scoped tag with zero assignments has nothing to reference it and is
  dropped on round-trip. Accepted.

## Token grammar

```
token   := [ category ":" ] value [ "[" color "]" ]
category := [a-z0-9-]+        ; optional namespace/dimension
value    := [a-z0-9-]+        ; required label
color    := [^\]]+            ; optional UI hint, e.g. #1d4ed8 or a name
```

`category` and `value` are already normalized to `[a-z0-9-]` by `TagNormalizer` (it turns any other
character, including `:` and `[`, into `-`), so the delimiters can never occur inside them. Parse:
strip a trailing `[...]` as `color` first, then split the remainder on its single optional `:`.

## Prerequisite refactor — remove `description`

Small cleanup folded in before the round-trip work (no UI ever collected it):

- Drop the `description` column from `V13__tagging.sql` directly (the migration hasn't run
  anywhere yet, so it's edited in place rather than adding a V14 drop).
- Remove `descriptionText` from `TagImpl`, `description` from `EditTagInput` / `TagDto`, the mapping
  in `TagCommandRegistrar.toTagDto`, and `description` from the Angular `TagService.editTag` +
  `TagDto`. Tags keep `category`, `value`, `color`.

Status: this prerequisite is **done** (committed as its own chunk); the sections below are the
remaining round-trip work.

## XML representation

Each taggable entity carries a `<tags>` child holding its assigned tags as tokens (mirrors how
`annotationRef` sits on the entity, but as plain strings):

```xml
<goal id="GOAL_10" ...>
  ...
  <tags>
    <tag>type:business-rule</tag>
    <tag>billing</tag>
    <tag>projectKind:product[#1d4ed8]</tag>   <!-- a global tag looks identical; resolved by key -->
  </tags>
</goal>
```

Because tokens are strings, a **global** tag assignment looks the same as a project one — there's
nothing to embed and no scope in the XML. On import the resolver decides scope (global-if-exists,
else project). The tokens live on the entity via a transient (non-persistent) JAXB collection added
to the taggable base classes (`AbstractTextEntity`, `AbstractStakeholder`, `StepImpl`, and
`ProjectImpl`); it holds `String`s only, so no base class references a tag type.

## Why this keeps tagging a leaf

- **Persistence / marshal graph:** only `String`s. `ProjectImpl`, the entities, and
  `ExportProjectCommandImpl.CLASSES_FOR_JAXB` never name `TagImpl`. The Phase 1 seam
  (`project-jpa` depends on `tagging-domain` only) is preserved.
- **Orchestration:** the "gather tokens for export" and "resolve/create/assign on import" steps —
  which do need tagging + a repository — live in `service-impl` (which already depends on both
  project and tagging), not in the `project-jpa` commands. So no `project-jpa → tagging-jpa` edge.

## Export flow

1. `service-impl` (the layer that triggers export) loads the project's tag assignments
   (`TagRepository`) and sets each entity's transient token set — project-scoped and global tags
   both become `category:value[color]` tokens on the entities they're assigned to.
2. `ExportProjectCommandImpl` marshals `ProjectImpl` as today; JAXB writes the `<tags>` tokens.
   No change to the JAXB class list.

## Import flow

1. New `TagStaxImporter` (in `utils-jaxb`, string-only) reads each entity element's `<tags>` tokens,
   producing `(entityType, entityExternalId, tokens[])`.
2. `ImportProjectStreamingCommandImpl` already builds an external-id → new-entity map in its unit of
   work. It uses that to translate each token record to `(entityType, newEntityId, tokens[])` and
   surfaces that list as a side output of the import (it references only `utils-jaxb` + strings, not
   `tagging-jpa`).
3. The `service-impl` import orchestrator consumes that list and, per token: parse → resolve a global
   tag by `(category,value)`; else find-or-create a project-scoped tag in the target project; then
   assign it to the entity (reusing the Phase 2 `EditTag`/`AssignTag` commands or `TagRepository`).

If surfacing the id map from the streaming command proves awkward in practice, the fallback is to run
the tag assembly inside the import command with a `TagAssembler` — that would add
`project-jpa → tagging-jpa`, which is consistent with the command already depending on
`annotation-jpa` for annotation assembly. Preference is the `service-impl` post-pass to keep the leaf.

## project.xsd

Add an optional `tags` element (sequence of `tag` string tokens) to the taggable entity types. Keep
it optional so existing tag-free exports still validate.

## Round-trip IT

Extend `ProjectXmlStreamingRoundTripIT`: build a project with two goals and an actor; create a
namespaced `type=business-rule` and a flat `billing` project tag and assign them; create a global
`projectKind=product` tag and assign it to a goal; export → import into a fresh project; assert every
assignment comes back with the right `(category,value)`, that the global one resolved to the existing
global row (no duplicate, still `project_id = null`), and that a color token round-trips.

## Order of work

1. Prerequisite: remove `description` (V14 + code, both Java and Angular).
2. Transient token collections on the taggable bases + `TagNormalizer`-based token format helper
   (parse/format), with unit tests.
3. Export enrichment in `service-impl`.
4. `TagStaxImporter` + import orchestration/assignment in `service-impl`.
5. `project.xsd` + round-trip IT.

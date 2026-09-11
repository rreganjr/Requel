# Gateway write-surface bugs found building the PlatformQ Roundtable project

Each block below becomes one issue. Sliced on `---` by `scripts/create-gateway-bug-issues.sh`.
Context: `doc/pq-roundtable-case-study.md`.

Two things first reported as bugs turned out not to be, and are recorded here so they are not
re-filed: **step sharing works as designed** (`StepImpl.usingScenarios` is
`@ManyToMany(mappedBy="steps")`; reuse a step by passing its `stepId`), and **`canDelete: false` in
a command result is a deliberate hardcode** in `ProjectCommandRegistrar.toDto()` with a comment
saying so — only the list query resolves the caller.

---

# EditScenario throws NPE on a null name when editing an existing scenario by id

`EditScenarioInput.name` is nullable, but `EditScenarioCommandImpl` calls `getName()` into a
uniqueness lookup and the entity setter without a null check.

**Reproduce:** call `EditScenario` with `projectName`, an existing `scenarioId`, `text`, and no
`name`.

**Actual:** `Command 'EditScenario' failed: Cannot invoke "String.trim()" because "name" is null`.

**Expected:** editing by id with no name supplied should keep the scenario's current name — or, if
name is genuinely required, `EditScenarioInput.name` should be `@NotBlank` and the failure should be
a field-level validation error, not an NPE surfaced as a command failure.

**AC**
- Editing an existing scenario by id without a name preserves the current name, or returns a
  field-level validation error naming the field.
- No `NullPointerException` reaches the caller.
- Covered by a test.

---

# EditScenarioStep is exposed as a typed MCP tool with an empty input schema

The generated typed tool for `EditScenarioStep` advertises **no properties at all**, so it cannot be
called meaningfully. Steps are only reachable through `EditScenario`'s `steps` array.

**Reproduce:** inspect the MCP tool list; `EditScenarioStep` has `{"properties": {}, "type":
"object"}`.

**Expected:** either the tool carries its real input schema (matching `EditStepInput`: `stepId`,
`name`, `text`, `scenarioTypeName`, `isScenario`, plus the project), or it is removed from the
catalog if editing a step standalone is not supported. An uncallable tool in the catalog is worse
than an absent one — a client cannot tell it apart from a working one until it fails.

**AC**
- `EditScenarioStep` either exposes a complete input schema or is not offered.
- The write-catalog lockstep test covers the distinction.

---

# EditPosition leaks a CGLIB proxy class name into the API response

`EditPosition` returns `"positionType": "PositionImpl$$EnhancerByCGLIB$$1f1035a5"` — the Spring
proxy class name, including a build-specific hash, rather than a stable type value.

**Reproduce:** call `EditPosition` with `projectName`, an existing `issueId`, and `text`.

**Expected:** a stable domain value, or the field omitted. As it stands the value changes between
runs, is meaningless to a client, and exposes an implementation detail. Worth grepping for the same
pattern elsewhere — any DTO deriving a string from `getClass().getName()` on a proxied entity has
the same defect.

**AC**
- `positionType` is a stable value from the domain model, or the field is removed.
- A test asserts the response contains no `$$EnhancerBy` substring.
- Other DTO mappers are checked for the same pattern.

---

# A step-name collision blames the parent scenario, and steps can only be reused by id

Steps are shared across scenarios by design (`StepImpl.usingScenarios` is
`@ManyToMany(mappedBy = "steps")`), and step names are unique within a project — a step name *is*
its identity. Both correct. The write surface makes that nearly undiscoverable.

**Reproduce:** create a scenario whose `steps` array contains an entry with `name` equal to an
existing step in the project and no `stepId`.

**Actual:** `Command 'EditScenario' failed: The name conflicts with an existing
ProjectOrDomainEntity`. The message names neither the offending step nor the entity it collided
with, and attributes the failure to the scenario. During the roundtable build this cost two failed
attempts at renaming the *scenario* before the real cause was isolated by creating one with no
steps.

**Expected:**
- The error names the colliding step and the existing entity's id, and identifies it as a step.
- `EditScenario` accepts a step by **name** as a reference to the existing step, not only by
  `stepId`. An ingest client working from prose has names, not ids, and requiring a lookup round
  trip per step to reuse "End the room" is the single biggest friction in building a project through
  the gateway.

**AC**
- A collision error names the entity type, the colliding name, and the existing entity id.
- A step entry with a `name` matching an existing step and no `stepId` reuses that step rather than
  failing, or the error explicitly tells the caller to pass its `stepId`.
- Reusing a step across two scenarios is covered by a test asserting both appear in
  `usingScenarios`.

---

# Tag category names and values are silently lower-cased

`EditTagCategory` with `name: "Source"` and `values: ["CON-3685", "CON-3686"]` returns
`name: "source"` and `values: ["con-3685", "con-3686"]`. No warning, no note in the tool
description.

This matters beyond cosmetics: tags are the cheapest available carrier for provenance (see
`doc/entity-provenance-notes.md`), and a tag that cannot round-trip the canonical form of an
external identifier cannot be used to reconstruct a link or compared against the source system.

**Expected:** either preserve the supplied case and compare case-insensitively, or document the
normalisation in the command and tool description so a caller knows the stored value differs from
the supplied one.

**AC**
- Either display case is preserved with case-insensitive uniqueness, or the normalisation is
  documented in `EditTagCategoryInput`, the MCP tool description, and the UI.
- A test pins whichever behaviour is chosen.

---

# No path to delete projects the caller is not a stakeholder on

`canDelete` is `callerHoldsProjectDelete(project, user)` — the caller must be a `UserStakeholder` on
that project holding a `Project[Delete]` permission. Working as designed, and it blocks the use case
that motivated the delete-project epic.

On the current dev instance, **377 of 524 projects report `canDelete: false`**, all created by
`System Administrator [admin]`: E2E fixtures and imports created through paths that never added the
admin as a stakeholder with Delete. The 490 `e2e-*` projects and `Imported Project (2..30)` are
exactly the rubbish #239 set out to remove, and most of them cannot be removed by anyone.

**Options, in preference order:**
1. A system-administrator capability that satisfies `Project[Delete]` without per-project
   stakeholder membership — the smallest change, and it makes ops cleanup possible generally.
2. Make every project-creating path (including import and the E2E fixtures) add the creator as a
   `UserStakeholder` with full permissions, and backfill existing rows.
3. A maintenance command that purges by name pattern, authorised at the system level.

**AC**
- A system administrator can delete a project they are not a stakeholder on, through the audited
  command path.
- The deletion is audited and attributed.
- The 490 `e2e-*` projects and `Imported Project (2..30)` can be removed without hand-written SQL.

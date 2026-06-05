# Issue tracker → Goals workflow + v1 reconciliation

Part of the v2.0 MCP command-gateway series (ticket 3 of 5). Design doc:
`doc/local_mcp_bridge.md`. Depends on the Command Gateway ticket (series ticket 1).

## Goal

Deliver the motivating end-to-end workflow: read an issue from an issue tracker, derive its
acceptance criteria / discrete requirements, and create one goal per requirement on a target
Requel project, each carrying a provenance note back to the source issue. Include a first-cut
("v1") reconciliation so re-runs update rather than blindly duplicate.

The workflow is **source-agnostic**. Jira is the primary example, but GitHub Issues, Linear, or
any tracker works the same way: the AI client reads the issue via its own connector and hands
Requel discrete requirement statements. Requel never talks to the tracker directly, so the only
source-specific data is a small provenance descriptor.

## Background and key constraint

Ticket 1 provides the gateway and write tools (`requel.editGoal`, `requel.editNote`, etc.).

**Correctness constraint surfaced by code review:** `EditGoal` is *not* an upsert-by-name.
`EditGoalCommandImpl` throws a uniqueness conflict when creating a goal whose name already exists
in the project/domain, and it only updates when a goal id is supplied; the API registrar sets an
existing goal solely when `goalId` is provided. So "call `EditGoal` by name and let it
find-or-create" does **not** work. The convenience tool must first resolve an existing goal id
(by query and/or by reading provenance notes) and pass that id to update; otherwise it creates a
new goal, which fails if the derived name collides.

`EditNote` similarly creates a new note when `noteId` is null (reusing only on an exact
grouping-object/annotatable/text match), so updating provenance requires finding and passing the
existing note's id.

Current DTOs:

```java
public record EditGoalInput(String projectName, Long goalId, String name,
                            String text, Integer version) {}   // registrar does NOT yet apply version
public record EditNoteInput(String projectName, String entityType, Long entityId,
                            Long noteId, String text) {}
```

## Scope

In scope:

- **One goal per acceptance criterion / requirement, flat** at the project level. All generated
  goals attach to the `Project` (`GoalContainer`); the source issue does **not** become a parent
  goal/container in v1 (that's a later assistant pass). State this explicitly so users don't
  assume hierarchy is preserved.
- **Requirement extraction (client-side):** use an explicit "Acceptance Criteria" section when
  present; otherwise infer discrete requirement statements from the issue body. Requel receives
  discrete statements regardless of source format.
- **Provenance note** (a `NOTE`) on each goal in a **machine-parseable format** — a JSON block
  inside the note text behind a stable marker (e.g. a fenced ```requel-provenance block) —
  recording: source client, `sourceSystem`, `sourceRef`, `sourceUrl`, criterion reference, and a
  normalized `criterionHash`. Plain prose is insufficient because ticket 4 scopes matches by
  parsing these notes.
- **A convenience tool** `requel.upsertGoalFromRequirement(projectName, name, text, sourceSystem,
  sourceRef, sourceUrl, criterionHash)` that:
  1. resolves an existing goal id (exact name match via a goal query, and/or a provenance-note
     match on `sourceSystem`+`sourceRef`+`criterionHash`);
  2. calls `EditGoal` with that `goalId` to update, or creates a new goal when none matches;
  3. finds the existing provenance note id and updates it, or creates one when absent.
- **v1 reconciliation:** id-based update via the lookup above. Exact name match (or matching
  provenance) updates in place; otherwise create.

Out of scope:

- Smart/fuzzy matching of edited requirements (ticket 4). v1 only matches exact name or exact
  provenance key.
- Grouping/relating per-requirement goals into a parent feature goal.

## Design

- Client-orchestrated: resolve project → **load existing goals (ids, names, text, provenance)** →
  for each requirement, `upsertGoalFromRequirement` → report created vs updated.
- The convenience tool owns the goal-id lookup so callers never hit the uniqueness conflict.
- Goal `name` derivation: deterministic from the requirement (normalized leading clause),
  capped at a defined max length, with a documented collision rule (e.g. append a short
  disambiguator) for two requirements that derive the same name.
- `criterionHash`: a defined algorithm (e.g. SHA-256) over canonically normalized criterion text
  (documented normalization: trim, collapse whitespace, lowercase, strip trailing punctuation).
- Provenance note format and marker are documented and covered by parser tests (shared with
  ticket 4).

### Known limitation (accepted for v1)

A minor edit to a requirement changes both the derived name and the `criterionHash`, so v1's
exact-match lookup will **not** recognize it as the same requirement: it creates a new goal and
leaves the prior one as a discoverable orphan (same `sourceRef`, different `criterionHash`).
Fuzzy resolution is ticket 4.

## Security & Privacy

- Writes are the authenticated user's, bounded by command authorization (unchanged from
  ticket 1).
- Provenance is mandatory for auto-generated goals.
- Cap goal name/text/note lengths at the tool boundary.

## Implementation Steps

1. Define the machine-parseable provenance-note format + marker; add a parser and tests.
2. Define deterministic goal-name derivation (max length, collision rule) and the `criterionHash`
   algorithm + canonical normalization.
3. Implement `requel.upsertGoalFromRequirement`: goal-id resolution (name query + provenance
   match) → `EditGoal` (create or update-by-id) → provenance `EditNote` (create or update-by-id).
4. Document the client workflow (resolve → load existing goals → upsert per requirement → report),
   including AC-section vs inferred-requirement extraction.
5. Tests below.

## Testing Strategy

- Idempotency: run the same requirement set twice; assert update-by-id, no duplicates, no
  uniqueness-conflict thrown.
- Negative test: a create with a colliding name (no prior id resolved) surfaces the uniqueness
  conflict cleanly, proving the lookup path is required and exercised.
- Provenance: assert a parseable provenance block (sourceSystem/ref/url + criterionHash) is
  attached, and that re-runs update the same note id rather than appending duplicates.
- Provenance parser tests over the exact documented format.
- Edited-requirement behavior: assert the documented v1 limitation (new goal + discoverable
  orphan).
- Source-agnostic: a fixture issue without an explicit AC section yields discrete goals from
  inferred requirements.

## Acceptance Criteria

- A documented, source-agnostic workflow produces one provenance-noted goal per requirement,
  with Jira and at least one other tracker (e.g. GitHub Issues) as worked examples.
- `requel.upsertGoalFromRequirement` resolves an existing goal id before editing, updates by id,
  never relies on name-based auto-create, and updates (not duplicates) the provenance note on
  re-run.
- The provenance note is machine-parseable and covered by parser tests.
- The edited-requirement limitation is documented and covered by a test asserting current
  behavior.

## Dependencies

- Series ticket 1 (Command Gateway + write tools, incl. a goal query that returns ids/names/text).

## Open Questions

- Should `requel.upsertGoalFromRequirement` ship as a distinct tool, or be a documented
  multi-call client pattern (`getProjectContext`/goal-query → `editGoal` → `editNote`)?
- Provenance format: JSON-in-note behind a marker (recommended) vs a structured annotation type
  if one is added later.
- Version handling on update: rely on loaded entity state, or require `version` once ticket 1
  wires it?

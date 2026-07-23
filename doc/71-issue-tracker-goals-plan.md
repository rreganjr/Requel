# Issue #71 — Issue Tracker → Goals Workflow + v1 Reconciliation (Implementation Plan)

Design doc: `doc/local_mcp_bridge.md`. Ticket 3 of 5 in the v2.0 MCP command-gateway series.
Depends on ticket 1 (Command Gateway + write tools + goal query), which is landed on
`release/2.0` (gateway-api, service-impl in-process impl, gateway-rest-client, mcp-server typed
tools generated from `GatewayCommandCatalog`, read tools in `McpReadService`/`QueryGateway`).

## Summary

Deliver the motivating end-to-end workflow: an AI client reads an issue from any tracker,
extracts discrete requirement statements, and hands them to Requel, which creates **one flat goal
per requirement** on a target project. Each goal carries a machine-parseable **provenance note**
back to the source issue. A first-cut ("v1") **reconciliation** makes re-runs update existing
goals by id rather than blindly duplicate them.

The workflow is **source-agnostic**. Requel never talks to the tracker; the client reads the
issue via its own connector and passes Requel discrete statements plus a small source descriptor
(`sourceSystem`, `sourceRef`, `sourceUrl`).

## Agreed design decisions

1. **Distinct typed tool** — ship `requel.upsertGoalFromRequirement(...)` as a real gateway tool
   that owns goal-id resolution server-side, so no caller can hit the `EditGoal` uniqueness
   conflict. (Chosen over a documented multi-call client recipe.)
2. **Provenance = JSON-in-note behind a stable marker** — a fenced ` ```requel-provenance ` block
   inside a `NOTE`'s text. Uses the existing `EditNote` command; no new annotation type, no
   migration. Ticket 4 (smart matching) parses the same block.
3. **Version handling: rely on loaded entity state for v1.** The upsert tool loads the goal to
   resolve its id immediately before updating, so the lock window is small. Explicit
   caller-supplied optimistic-lock enforcement on `Edit*` is deferred to **#108**.
4. **Name derivation** — deterministic from the requirement: first clause/sentence, trim +
   collapse whitespace, cap at 120 chars; on collision within the project append `"-" +
   first 6 chars of criterionHash`.
5. **criterionHash** — SHA-256 (hex) over canonically normalized criterion text: trim → collapse
   internal whitespace to single spaces → lowercase → strip trailing punctuation. Kept minimal so
   it stays predictable and identical across this ticket and #108/#4.

## Correctness constraint (why the tool owns lookup)

`EditGoalCommandImpl` is **not** an upsert-by-name: it throws a uniqueness conflict when creating
a goal whose name already exists in the project/domain, and updates only when a `goalId` is
supplied. `EditNoteCommandImpl` likewise creates a new note when `noteId` is null (reusing only on
an exact grouping-object/annotatable/text match). Therefore the convenience tool must resolve an
existing goal id (and an existing note id) **before** editing, and pass those ids to update.

Current DTOs (unchanged by this ticket):

```java
public record EditGoalInput(String projectName, Long goalId, String name,
                            String text, Integer version) {}   // version not enforced yet — see #108
public record EditNoteInput(String projectName, String entityType, Long entityId,
                            Long noteId, String text) {}
```

## Provenance note format

A note whose text contains exactly one fenced block behind the marker `requel-provenance`:

````
```requel-provenance
{
  "v": 1,
  "client": "claude-desktop",
  "sourceSystem": "jira",
  "sourceRef": "PROJ-123#ac2",
  "sourceUrl": "https://.../browse/PROJ-123",
  "criterionRef": "AC-2",
  "criterionHash": "<sha256-hex>"
}
```
````

- `v` is a format version for forward compatibility.
- Human-readable prose may precede the block; only the fenced `requel-provenance` block is
  parsed. Plain prose alone is insufficient (ticket 4 scopes matches by parsing this block).
- The parser and its exact format are covered by unit tests **shared with ticket 4**.

## Goal-id resolution (v1 reconciliation)

Given `projectName, criterionText, sourceSystem, sourceRef, sourceUrl, criterionRef` (with
optional `name` / `text` / `criterionHash` overrides — derived from `criterionText` when omitted):

1. **Provenance match:** enumerate the project's goals (`QueryGateway.searchProjectEntities` with
   an empty query returns all goals as type+id+name) and read each one's notes
   (`getAnnotations`), parse `requel-provenance` blocks, and match on `sourceSystem` +
   `sourceRef` + `criterionHash`. A match is the same requirement on a re-run → **update that
   goal by id** and update its matched provenance note by id.
2. **No match → create:** create a new goal (no `goalId`). If the derived name already belongs to
   some *other* goal, apply the collision rule (`disambiguate`: append `-` + first 6 chars of the
   `criterionHash`) so the create can never trip the `EditGoal` uniqueness conflict, then attach a
   fresh provenance note.

**Design note (refinement of the ticket's candidate keys).** The ticket lists "exact name match →
update" as a v1 key alongside provenance. We fold name into the *create* path rather than the
*update* path: matching purely on name is unsafe (two distinct requirements can derive the same
name, and a name-only update would silently collapse them or hijack a manually-created goal). So
**provenance (sourceSystem + sourceRef + criterionHash) is the sole update key**, and an exact name
collision on the create path triggers disambiguation instead of an update. This satisfies the AC —
resolve an existing id before editing, update by id, never rely on name-based auto-create — while
being collision-safe.

No read-side changes are required: goal enumeration/name lookup uses `searchProjectEntities`,
provenance uses `getAnnotations`. (Full goal *text* is not exposed by current queries — that is a
ticket 4 concern for similarity matching, not needed for v1 exact-key matching.) The provenance
scan reads each goal's notes (O(#goals) reads per upsert); ticket 4 adds a candidate query to
narrow it.

Then: call `EditGoal` with the resolved id (or none) → on the returned goal id, `EditNote` with the
matched provenance note id to update it, or with a null id to create one.

### Known limitation (accepted for v1)

A minor edit to a requirement changes both the derived name and the `criterionHash`, so v1's
exact-key lookup will not recognize it as the same requirement: it creates a new goal and leaves
the prior one as a discoverable orphan (same `sourceRef`, different `criterionHash`). Fuzzy
resolution is ticket 4 (`requel.findBestGoalMatch`).

## Where the code goes

- **Provenance format + parser + hash + name derivation:** a small value/util set in
  `gateway-api` (pure, no Spring/JPA), so all front-ends and both gateway impls share it and the
  ticket-4 matcher can reuse the parser.
- **`upsertGoalFromRequirement` orchestration:** in the gateway (uses `CommandGateway` for
  `EditGoal`/`EditNote` and `QueryGateway` for lookup). Exposed as a typed tool via the same
  catalog/registrar path the other typed tools already use, and reachable from `requel-cli`.
- **No domain or command changes** — reuses existing `EditGoal`/`EditNote` and read tools.

## Implementation steps

1. Provenance-note format + marker; parser + tests (shared with #4).
2. Deterministic name derivation (max length, collision rule) + `criterionHash` algorithm and
   canonical normalization; unit tests.
3. `requel.upsertGoalFromRequirement`: goal-id resolution (provenance match + exact name) →
   `EditGoal` (create or update-by-id) → provenance `EditNote` (create or update-by-id).
4. Document the client workflow (resolve project → load existing goals → upsert per requirement →
   report created vs updated), covering AC-section vs inferred-requirement extraction, with Jira
   and GitHub Issues as worked examples.
5. Tests (below).

## Testing strategy

- **Idempotency:** run the same requirement set twice; assert update-by-id, no duplicates, no
  uniqueness conflict thrown.
- **Negative:** a create with a colliding name and no resolved id surfaces the uniqueness conflict
  cleanly — proving the lookup path is required and exercised.
- **Provenance round-trip:** a parseable block is attached; a re-run updates the same note id
  rather than appending a duplicate.
- **Provenance parser:** unit tests over the exact documented format (shared with #4).
- **Edited-requirement:** assert the documented v1 limitation (new goal + discoverable orphan).
- **Source-agnostic:** a fixture issue with no explicit AC section yields discrete goals from
  inferred requirements.

## Boundaries / security

- Writes are the authenticated user's, bounded by existing command authorization (unchanged from
  ticket 1). Provenance is mandatory for auto-generated goals.
- Cap goal name/text/note lengths at the tool boundary before dispatch.
- No user/identity management; goals attach only to the `Project` (`GoalContainer`) — the source
  issue does **not** become a parent goal/container in v1 (a later assistant pass may add
  hierarchy).

## Related issues

- **#69** — command gateway + write tools (dependency, landed).
- **#104 / #107** — shared `GatewayCommandCatalog` + generated typed write tools (landed).
- **#108** — wire optimistic-lock `version` through `Edit*`; this ticket relies on loaded entity
  state and defers explicit version enforcement to it.
- **Ticket 4** — `requel.findBestGoalMatch` smart/fuzzy reconciliation; consumes this ticket's
  provenance parser and lifts the edited-requirement limitation.

## Open questions — resolved

- Distinct tool vs. multi-call recipe → **distinct typed tool**.
- Provenance format → **JSON-in-note behind a marker**.
- Version handling on update → **rely on loaded state; enforcement deferred to #108**.

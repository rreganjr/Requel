# Issue Tracker → Goals Workflow

How an external client (an AI assistant such as Claude Desktop/Code, or a script) turns an issue
from any tracker into one provenance-noted goal per requirement on a Requel project, and how
re-runs reconcile instead of duplicating. Companion to the design in `doc/local_mcp_bridge.md` and
the implementation plan in `doc/71-issue-tracker-goals-plan.md` (issue #71).

## Principle: source-agnostic

Requel never talks to the tracker. The **client** reads the issue through its own connector (Jira,
GitHub Issues, Linear, Azure Boards, a pasted description — anything), extracts discrete
requirement statements, and hands each one to Requel together with a small, generic source
descriptor:

- `sourceSystem` — the tracker family, e.g. `jira`, `github`, `linear`.
- `sourceRef` — a source-specific reference to the item (and, where useful, the specific
  criterion), e.g. `PROJ-123` or `PROJ-123#ac2`.
- `sourceUrl` — an openable link to the source item (optional).

Because only this descriptor is source-specific, the same workflow works for every tracker; the
worked examples below differ only in how the client reads the issue.

## What Requel provides

A single convenience capability, `requel.upsertGoalFromRequirement`, backed by
`RequirementGoalUpserter`. Given one requirement plus its source descriptor it:

1. resolves whether a goal for this requirement already exists (by provenance),
2. creates the goal or updates the existing one **by id**, and
3. creates or updates a machine-parseable **provenance note** on that goal.

Inputs (only the first four are required; the rest are derived or optional):

| field | required | meaning |
|-------|----------|---------|
| `projectName` | yes | target project |
| `criterionText` | yes | the requirement / acceptance-criterion statement |
| `sourceSystem` | yes | tracker family |
| `sourceRef` | yes | reference to the source item/criterion |
| `name` | no | explicit goal name (derived from `criterionText` if omitted) |
| `text` | no | goal body (defaults to `criterionText`) |
| `sourceUrl` | no | link to the source item |
| `criterionRef` | no | human-readable criterion label, e.g. `AC-2` |
| `client` | no | external-client id for audit attribution, e.g. `claude-desktop` |
| `criterionHash` | no | precomputed reconciliation key (computed from `criterionText` if omitted) |

Result: the goal id, its final name (possibly disambiguated), the provenance note id, a
`created` flag (created vs updated), and the `criterionHash`.

### How to invoke it

Two front-ends expose the same `RequirementGoalUpserter` core:

- **MCP write tool** `upsertGoalFromRequirement` — advertised in `tools/list` only when gateway
  writes are enabled (`requel.gateway.write.enabled=true`). The tool arguments are the fields in
  the table above except `client`, which is taken from the MCP client context (the
  `X-Requel-Client` identity), not the arguments. This is the path AI clients (Claude Desktop /
  Code, the remote connector) use.
- **CLI** `requel upsert-goal` — for scripting/bulk import, e.g.
  `requel upsert-goal --project "Acme Auth" --criterion "The system shall allow login." --source-system jira --source-ref PROJ-123#ac1 --source-url https://acme.atlassian.net/browse/PROJ-123 --criterion-ref AC-1`.
  Prints created-vs-updated with the goal and provenance-note ids (or the full result with
  `--output json`). The CLI reports its client id as `requel-cli`.

Both go through the same gateway command/authorization/audit path; there is no separate write
path.

The goal `name` is derived deterministically from `criterionText`: the leading clause (up to the
first line break, then the first sentence terminator), whitespace-collapsed, trailing punctuation
stripped, capped at 120 characters. The `criterionHash` is SHA-256 over a canonical normalization
of the text (trim → collapse whitespace → lowercase → strip trailing `. , ; : ! ?`).

### Scope in v1

All generated goals attach **flat** to the project (a `GoalContainer`). The source issue does
**not** become a parent goal or container, and per-requirement goals are not grouped into a parent
feature goal — that is a later assistant pass. Do not assume any hierarchy from the issue is
preserved.

## Requirement extraction (client-side)

The client decides what counts as a requirement before calling Requel:

- **Explicit acceptance-criteria section.** If the issue has an "Acceptance Criteria" (or
  equivalent) section, use each listed item as one `criterionText`, and set `criterionRef` to a
  stable label if the source has one (e.g. `AC-1`, `AC-2`).
- **Inferred requirements.** If there is no AC section, infer discrete requirement statements from
  the issue body — one clear, testable statement per goal. Requel receives discrete statements
  regardless of source format.

Keep each `criterionText` to a single requirement; the goal name is derived from its leading
clause, so front-load the statement with the essential capability.

## The workflow

1. **Resolve the target project** — `requel.getProject` / `requel.listProjects`.
2. **Load existing goals** — `requel.getProjectContext` (or `requel.searchProjectEntities`) so you
   can report created-vs-updated meaningfully. (The upsert tool also does its own resolution; this
   load is for your report and for a human preview.)
3. **For each requirement, upsert** — call `requel.upsertGoalFromRequirement` with the requirement
   and source descriptor. The tool resolves an existing goal id and updates it, or creates a new
   goal, and (re)writes its provenance note.
4. **Report** — collect each result's `created` flag, goal id, final name, and `sourceRef`, and
   present a created-vs-updated summary for review.

## Reconciliation (v1)

- **Update key is provenance:** `sourceSystem` + `sourceRef` + `criterionHash`. A re-run of the
  same requirement matches its existing goal and **updates in place** (same goal id, same
  provenance note id) — no duplicates, and no `EditGoal` uniqueness conflict.
- **Name collisions on create are disambiguated:** if a new requirement's derived name already
  belongs to a different goal, the tool appends a short disambiguator (`-` + the first six
  characters of the `criterionHash`) so distinct requirements never collapse into one goal.

### Known limitation (accepted for v1)

A minor edit to a requirement changes both its derived name and its `criterionHash`, so it matches
neither existing provenance nor an existing name. The tool creates a **new** goal and leaves the
prior one as a discoverable orphan (same `sourceRef`, different `criterionHash`). Fuzzy resolution
of edited requirements is ticket 4 (`requel.findBestGoalMatch`). If you need to clean up an orphan
in the meantime, find goals with the same `sourceRef` but a stale hash and delete or merge them
manually.

## Provenance note format

Each generated goal carries a `NOTE` containing a fenced block behind the marker
`requel-provenance`:

````
Auto-generated from a source tracker item. Do not edit the block below — it is used by Requel to
reconcile this goal on re-runs.

```requel-provenance
{
  "v" : 1,
  "client" : "claude-desktop",
  "sourceSystem" : "jira",
  "sourceRef" : "PROJ-123#ac2",
  "sourceUrl" : "https://acme.atlassian.net/browse/PROJ-123",
  "criterionRef" : "AC-2",
  "criterionHash" : "…sha256 hex…"
}
```
````

The block is the machine-parseable contract used for reconciliation (and by ticket 4). Human prose
may precede it; only the fenced block is parsed. Unknown fields are ignored, so notes written by a
newer format version still parse.

## Worked example: Jira

Source ticket `PROJ-123` with an explicit acceptance-criteria section:

> **Acceptance Criteria**
> 1. The system shall allow a user to sign in with email and password.
> 2. A failed sign-in shows an error without revealing which field was wrong.

The client reads the ticket via its Jira connector, then calls the tool once per criterion:

```jsonc
// criterion 1
{
  "projectName": "Acme Auth",
  "criterionText": "The system shall allow a user to sign in with email and password.",
  "sourceSystem": "jira",
  "sourceRef": "PROJ-123#ac1",
  "sourceUrl": "https://acme.atlassian.net/browse/PROJ-123",
  "criterionRef": "AC-1",
  "client": "claude-desktop"
}
// criterion 2
{
  "projectName": "Acme Auth",
  "criterionText": "A failed sign-in shows an error without revealing which field was wrong.",
  "sourceSystem": "jira",
  "sourceRef": "PROJ-123#ac2",
  "sourceUrl": "https://acme.atlassian.net/browse/PROJ-123",
  "criterionRef": "AC-2",
  "client": "claude-desktop"
}
```

First run creates two goals ("The system shall allow a user to sign in with email and password",
"A failed sign-in shows an error without revealing which field was wrong"), each with a provenance
note. Re-running after editing the ticket's *wording* of criterion 2 creates a third goal and
leaves the original criterion-2 goal as an orphan (the v1 limitation); re-running with the ticket
*unchanged* updates both goals in place.

## Worked example: GitHub Issues

Source issue `acme/app#42` with **no** acceptance-criteria section — just a prose body:

> We need CSV export on the reports page. Users should be able to pick a date range, and the
> export should include the same columns shown on screen.

The client infers discrete requirements from the prose and calls the tool per inferred statement:

```jsonc
{
  "projectName": "Acme Reports",
  "criterionText": "Users can export the reports page to CSV.",
  "sourceSystem": "github",
  "sourceRef": "acme/app#42",
  "sourceUrl": "https://github.com/acme/app/issues/42",
  "client": "claude-desktop"
}
{
  "projectName": "Acme Reports",
  "criterionText": "The CSV export supports selecting a date range.",
  "sourceSystem": "github",
  "sourceRef": "acme/app#42",
  "sourceUrl": "https://github.com/acme/app/issues/42",
  "client": "claude-desktop"
}
{
  "projectName": "Acme Reports",
  "criterionText": "The CSV export includes the same columns shown on screen.",
  "sourceSystem": "github",
  "sourceRef": "acme/app#42",
  "sourceUrl": "https://github.com/acme/app/issues/42",
  "client": "claude-desktop"
}
```

Because there is no per-criterion id in the source, all three share `sourceRef` `acme/app#42`; the
`criterionHash` (derived from each statement) is what distinguishes them for reconciliation.

## Prerequisites

- Gateway **writes enabled** for the deployment (`requel.gateway.write.enabled=true`), ideally
  per-project. Default is off.
- The caller is authenticated; every write executes as that user and is bounded by the same
  stakeholder authorization and audit as the Angular UI. Provenance is mandatory for
  auto-generated goals.

# Codex review: issue_tracker_to_goals_workflow.md

Review target: `doc/issue_tracker_to_goals_workflow.md`

## Findings

### 1. `EditGoal` is not currently idempotent by name

The doc says `EditGoal` finds-or-creates a goal by name and that v1 reconciliation can use
name-based upsert. Current `EditGoalCommandImpl` checks for an existing goal by name and throws
a uniqueness conflict when creating a new goal with an existing name. The API registrar only
sets an existing goal when `goalId` is provided; it does not look up by name and turn the call
into an update.

This is the largest correctness issue in the spec. `requel.upsertGoalFromRequirement` cannot
be implemented as "call `EditGoal` by name" unless the convenience tool first searches/loads
the existing goal id or a new command/API behavior is added.

### 2. The workflow needs goal ids and versions for updates

Since current `EditGoalInput` updates by `goalId`, the workflow must load existing goals and
carry ids into `EditGoal`. The doc says "resolve project -> load existing goals" but does not
state that the convenience tool must select an existing id before editing. It also does not
address version handling, and current command registration ignores the DTO `version` field.

Acceptance criteria should include update-by-id behavior and conflict handling after a prior
goal has been edited concurrently.

### 3. Provenance note idempotency is incomplete

The doc says the convenience tool performs `EditGoal` then `EditNote`, idempotent by goal name.
Current `EditNoteCommandImpl` creates or reuses a note by exact grouping object, annotatable,
and text when `noteId` is null. If the criterion hash/source URL changes, calling `EditNote`
without an existing `noteId` will create another note, not update the previous provenance note.

The provenance workflow needs a way to find the existing provenance note for a source key and
pass its `noteId`, or it should accept multiple provenance notes as intended history.

### 4. Provenance as plain text is hard to query reliably

The next ticket depends on source-ref scoping using provenance notes. Current annotations expose
plain `NoteDto.text`; there is no structured metadata. If this workflow uses formatted text,
the doc must define a strict machine-parseable format, preferably JSON inside the note text or
a clear marker block, and tests should cover parsing.

### 5. One-goal-per-requirement at project level may skip existing relationship capabilities

The doc intentionally scopes goals flat at the project level. That is acceptable for v1, but
Requel has goal containers and association commands (`AddGoalToGoalContainer`,
`RemoveGoalFromGoalContainer`) that can attach goals to stories/use cases/actors/project. The
doc should explicitly say whether all generated goals are attached only to `Project`, or
whether the source issue/feature can later become a parent goal/container. Without that, users
may assume hierarchy is preserved.

## Completeness gaps

- Define deterministic name derivation with max length and collision handling.
- Define duplicate handling when two requirements derive the same name but different text.
- Define the exact `criterionHash` algorithm and canonical text normalization.
- Include the current `EditNoteInput` shape, including `projectName`.
- Add negative tests for the current uniqueness-conflict behavior if the upsert lookup is not
  implemented.


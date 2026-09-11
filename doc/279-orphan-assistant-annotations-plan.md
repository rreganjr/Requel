# Issue #279 — Orphan assistant annotations after DeleteProject

Reference: https://github.com/rreganjr/Requel/issues/279
Relates to: #247 (DeleteProject cascade, `DeleteAnnotationGroupCommand`)

## Summary

A background assistant run can apply its findings after the project it was dispatched for has been
deleted, leaving `annotations` rows grouped under a nonexistent project. `DeleteProject` sweeps the
annotation group at delete time and cannot see a write that has not happened yet, and
`annotations.grouping_object_id` has no foreign key to stop it. The fix is a freshness check at
apply time plus a delete-side sweep for runs already in flight, backed by a test that manufactures
the race, and a one-off cleanup for databases that already carry orphans.

## Evidence

32 rows on a dev database, spanning six deleted projects and six days of e2e runs. All
`LexicalIssue`, all `source = ASSISTANT:legacy-lexical*`, `grouping_object_type = 'Project'`,
`grouping_object_id` resolving nowhere. 26 of them still carry an `annotation_annotatable` row
pointing at a deleted entity; the six newest carry none. Detail and the diagnostic queries are in
`tmp/orphan-annotations-diagnose.sql`.

## Mechanism

Established from the code:

1. `AnalysisInvokingCommandHandler.execute(...)` runs after the edit command returns and calls
   `AnalysisRequestDispatcher.dispatch(target, user)`.
2. `AnalysisRequestDispatcher` builds `projectRef = EntityRef.of("Project", projectOrDomain.getId())`
   — the source of the `Project` discriminator seen on every orphan — and calls
   `AssistantDispatcher.dispatch(request)`.
3. `AssistantDispatcherImpl.dispatch(...)` queues an `assistant_runs` row and hands the work to
   `assistantTaskExecutor`, returning immediately. From here the run is unordered with respect to
   anything the user does next.
4. `DeleteProjectCommandImpl` steps 1–9 delete the children, step 11 runs
   `DeleteAnnotationGroupCommand` over the project's group, step 12 deletes the project row.
5. `AssistantRunWorker` later calls `CommandBackedAssistantResultApplicator.apply(...)`, which
   executes annotation commands that insert rows grouped under the now-dead project.

Not yet established, and the **first task** of the ticket: the applicator already skips actions
whose annotatable "did not resolve", yet 26 orphans carry an `annotation_annotatable` row. Either
the target resolved through a stale first-level-cache / detached reference rather than a fresh
read, or those rows were written before the delete and only their *group* sweep was missed. The
two lead to different fixes, so confirm before building.

Confirm by instrumenting a single reproduction (§ Step 1) and reading which branch of
`resolveAnnotatable` is taken, rather than reasoning further from the data.

## Decisions to lock before coding

- **Where the guard lives.** Preference: `CommandBackedAssistantResultApplicator`, as a single
  precondition on `context.projectRef()` checked inside the applying transaction — one place, and
  it covers every assistant, legacy and AI alike. The alternative (each assistant checks) spreads
  the same check across `assistant-legacy-nlp` and `assistant-ai` and will rot.
- **What a stale run records.** Proposal: a terminal `AssistantRunStatus` distinct from failure —
  the run did not error, its subject went away. Needs a name and a decision on whether
  `error_kind` carries it or it gets its own status value.
- **Belt and braces on the delete side.** Whether `DeleteProject` should also mark in-flight runs
  for the project as cancelled (cheap: `assistant_runs` already has `project_id` and a status
  column) so a worker that has not started simply never runs. Recommended, but it is a second
  change and could be split.
- **Module boundaries.** `assistant-core` must not gain a dependency on `project-jpa`. The
  existence check goes through an existing SPI — `AssistantTargetLoader` is the natural seam —
  not a new repository import.

## Step-by-step

1. **Reproduce and instrument.** An IT that dispatches a run, deletes the project, then drives the
   applicator by hand. Log which guard fires. Settles the open question above.
2. **Add the freshness precondition.** In the applicator, before applying any action, re-resolve
   the run's `projectRef` inside the transaction; if it is gone, apply nothing and return an empty
   `AppliedAssistantResult`. Ensure the resolve is a fresh read, not a cache hit.
3. **Record the outcome on the run.** Terminal status per the locked decision, so
   `assistant_runs` explains the empty result instead of looking like a silent success.
4. **(If kept in scope) cancel in-flight runs on delete.** In `DeleteProjectCommandImpl`, mark
   queued/running `assistant_runs` for the project before step 12.
5. **Sweep existing orphans.** Decide migration vs. operational script. A Flyway migration is
   tempting but this is data cleanup on a condition that should never recur — lean toward a
   checked-in script plus a `RELEASE.md` note, and revisit if the orphan count is nonzero on any
   real database. `tmp/delete-orphan-annotations.sql` is the working version to promote.
6. **Verify** per the gate in `CLAUDE.md`: `mvn clean verify`, plus the MySQL IT.

## Test plan

- `CommandBackedAssistantResultApplicatorTest` — unit: result whose `projectRef` no longer resolves
  applies nothing and produces no findings.
- `DeleteProjectIT` (H2) — dispatch, delete, apply late; assert zero annotations grouped under the
  deleted project id.
- `DeleteProjectMySqlIT` (Testcontainers) — same, on the real schema with FKs and InnoDB locking.
  Per `CLAUDE.md`'s testing notes, the H2 run proves the logic and the MySQL run proves the
  constraints; orphan-row failures only show on MySQL.
- Manufacture the race with `JdbcTemplate` where the applicator cannot be driven late enough
  naturally — the `DeleteCascadeIT` pattern.
- After an e2e run, assert the orphan query returns 0 rather than trusting a green Playwright run
  (the `deleteProject` helper swallows failures).

## Out of scope

- Making assistant dispatch synchronous, or adding a real job queue.
- A foreign key on `annotations.grouping_object_id` — it is an `@Any` column by design.
- The soft-FK leftovers (`tag`, `tag_taggable`, `assistant_*`, `command_audit_log`) that
  `DeleteProjectCommandImpl` documents as deliberately not cleaned; that is its own ticket.

## Risks

- **The open question changes the fix.** If the 26 linked rows turn out to be a stale-cache read
  rather than a late write, step 2 is necessary but not sufficient. Hence step 1 first.
- **A project-existence read on every apply** adds a query per run. Negligible against an NLP or
  LLM call, but worth confirming it is not inside a loop over actions.
- **Cancelling in-flight runs** (step 4) touches the dispatcher's state machine; if it looks like
  it wants its own design discussion, split it rather than growing this ticket.

## AC mapping

| AC | Steps |
| --- | --- |
| Stale run applies nothing, records why | 2, 3 |
| Zero orphans after delete + late run | 2, 4 |
| H2 + MySQL regression tests | 1, 6 and Test plan |
| Sweep for existing databases | 5 |
| Orphan query returns 0 after e2e | 6 |

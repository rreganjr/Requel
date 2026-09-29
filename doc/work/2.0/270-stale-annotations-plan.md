# #270 Annotations are never invalidated when their entity's text changes — implementation plan

Issue: https://github.com/rreganjr/Requel/issues/270 (child 3 of epic #267)
Branch: `270-stale-annotations`, cut from `release/2.0` @ `0a767b9e`

## Summary

A machine finding records a fingerprint of the text it was derived from, stored on its
`assistant_findings` row. A finding is **stale** when the entity's current text no longer matches
that fingerprint, or when a later run stopped reporting it but a human's discussion kept it open
(`SUPERSEDED`). Staleness is worked out when read, not written when the entity is edited, so it
is correct from the moment the edit commits. Every read carries `stale` and `source`: REST, the
gateway, MCP (`getOpenIssues`, `getProjectContext`) and the CLI. The UI badges stale issues and
notes, mutes them, sorts them after fresh ones of the same severity, and has a "Hide stale" toggle.

Retiring findings stays with the re-run. Lexical assistants already re-run after every edit, so
their untouched stale findings disappear within seconds. AI findings stay, flagged, until the
next review or until a human acts on them. The ticket also fixes a data-loss bug: auto-resolve and
#268's legacy removal delete issues that carry human positions and arguments.

## Review against the tree (`release/2.0` @ `0a767b9e`)

The ticket was written before the assistant SPI's finding state machine, #302 and #268. Some of it
is already done and some of it would not work as written.

**Already true.**

- `AnalysisInvokingCommandHandler` dispatches every enabled assistant after an Edit command
  commits, for Goal, Story, Actor, UseCase, Scenario, Step and GlossaryTerm. All four lexical
  assistants are `AUTO_RESOLVE_IF_UNTOUCHED`, so `reconcileStaleFindings` removes a spelling
  finding whose word is gone on the run the edit triggered.
- `EntityContextPackBuilder.isAssistantSourced` already leaves every `ASSISTANT:` annotation out
  of context packs. So "stale annotations do not appear in context packs" already holds for
  sourced findings.
- The "Zzz" issue looks like old-path output (no source, no finding row). #268's
  `LegacyLexicalIssues.withRemovals` and "Re-run analysis" remove those. AC5 becomes a check.

**Wrong or missing in the ticket.**

1. *"Record, on each machine-generated annotation, the entity version it was derived from"*
   won't work. Annotations are `@ManyToAny` across annotatables, and #268 made glossary
   candidates one issue per phrase per project. The per-target record is the `assistant_findings`
   row (`target_type`, `target_id`, `applied_annotation_id`), so the fingerprint goes there.
2. **Data loss (AC3 is currently violated).** `isUntouched` checks only `source` and
   `isResolved()`. `removeLegacyAnnotation` also ignores positions. Both call
   `RemoveAnnotationFromAnnotatableCommand`, and that command deletes the issue once it has no
   annotatables left, positions and arguments included. The `CleanupPolicy.AUTO_RESOLVE_IF_UNTOUCHED`
   javadoc promises "no human edits, replies, or non-assistant positions". The `isUntouched`
   javadoc says a position's author can't be distinguished. Since #302 that is out of date:
   assistant writes are made as the `assistant` user (`resolveAssistantUser`), and so were the old
   path's (`AbstractAssistant.assistantUser`).
3. **The real staleness gap is AI findings.** `REQUIREMENTS_REVIEW` runs only when started by
   hand and uses the default `MARK_SUPERSEDED`. After an edit, its findings stay `ACTIVE` with
   nothing to mark them. `SUPERSEDED` findings also leave their annotation open with no visible
   sign.
4. **Nothing surfaces it.** `IssueDto`, `NoteDto` and `OpenIssueDto` carry neither `source` nor
   any stale flag. `getProjectContext.openIssues`, the read the epic measured, returns everything.
5. **Legacy leak into packs.** Old-path issues have `source = null` and are written by the
   `assistant` user. `isAssistantSourced` misses them, so they reach the model as if a human had
   written them.
6. **Analyze and apply run in separate transactions** (`AssistantRunWorker.analyzeTransaction` /
   `applyTransaction`). The fingerprint must be taken when the target is loaded for analysis. If
   it were taken at apply, an edit between the two would be hidden.
7. **Layering.** `service-impl` (the DTO mappers) does not depend on `assistant-core`. The read
   side needs an interface in `annotation-domain`, the same pattern as
   `AnnotatableTextEditRegistry`.
8. **Minor.** `CleanupPolicy.MARK_SUPERSEDED` javadoc says omitted findings are "left untouched",
   but `reconcileStaleFindings` marks them `SUPERSEDED`. `FindingResolutionTrackingCommandHandler`
   moves only `ACTIVE` findings to `MANUALLY_RESOLVED`, so a human resolving a `SUPERSEDED`
   finding's issue leaves the finding `SUPERSEDED`.

## Locked decisions (2026-09-29)

| # | Decision |
|---|---|
| D1 | Staleness is **derived at read**. There is no STALE state and no write on edit. The "same transaction" AC holds by construction. |
| D2 | Fingerprint = SHA-256 hex of the entity's **name + text**. Whitespace is trimmed and runs of it collapsed, case is kept, and null is read as `""`. Tag, relation, primary-actor or step edits do not mark findings stale. |
| D3 | **Surface, don't retire on edit.** Retiring happens only on a re-run, under each assistant's `CleanupPolicy`. |
| D4 | **Human discussion** = the issue is resolved, or it has any position or argument whose `createdBy` is not the `assistant` user. It blocks both `autoResolveIfUntouched` and `removeLegacyAnnotation`. Positions the assistant suggested (ChangeSpelling, AddWord, Ignore, glossary, actor) do not count. |
| D5 | **Flag everywhere.** Every read carries `stale` and `source`, and nothing is filtered on the server. The `GET_PROJECT_CONTEXT` and `GET_OPEN_ISSUES` descriptions tell callers what `stale` means. |
| D6 | **Old-path rows** (source null, created by `assistant`, no finding) count as machine-generated. They are kept out of entity context packs and never flagged stale. #268's re-run removes the untouched ones. |
| D7 | **A finding with a null fingerprint** (recorded before V25) reads as not stale. The next run of that assistant on that entity fills it in. |
| D8 | **UI:** a "May no longer apply" badge with a tooltip, the row muted, stale after fresh within a severity (P3), and a "Hide stale" toggle (off by default) on the annotations section and the project Open Issues view. |

**Proposed in this plan (needs sign-off):**

- **P1 (approved 2026-09-29).** When auto-resolve is blocked by D4, mark the finding **`SUPERSEDED`** instead of leaving
  it `ACTIVE`. Otherwise a human-discussed finding that the re-run no longer reports stays `ACTIVE`
  with a fresh-looking fingerprint and never reads as stale. `upsertFinding` already reactivates a
  `SUPERSEDED` finding that is reported again.
- **P2 (approved 2026-09-29).** `FindingResolutionTrackingCommandHandler` moves `ACTIVE` **and `SUPERSEDED`** findings to
  `MANUALLY_RESOLVED`.
- **P3 (approved 2026-09-29, revised).** Lists sort by **severity first (HIGH, MEDIUM, LOW), then fresh
  before stale**, then the existing #271 tiebreakers. This applies server-side and in the UI. A stale HIGH
  still outranks a fresh LOW: it *may* no longer apply, but it is a HIGH and a human should look at it.
  The #271 "highest severity first" order in `GET_OPEN_ISSUES` still holds.
- **P4 (approved 2026-09-29).** Replace the hard-coded `"assistant"` username (five places) with one constant, since D4
  and D6 add two more readers.

## Contracts

### Domain (`annotation-domain`)

New interface `spi/AnnotationFreshness`:

```java
public interface AnnotationFreshness {
    /** Annotation ids on {@code annotatable} whose machine finding for it is stale. */
    Set<Long> staleAnnotationIds(Annotatable annotatable);

    /** Batch form for project-wide lists: annotatable -> stale annotation ids. */
    Map<Annotatable, Set<Long>> staleAnnotationIds(Collection<? extends Annotatable> annotatables);

    AnnotationFreshness NONE = ...; // no-op default when no assistant-core bean is present
}
```

**Stale rule.** Take the findings with `target = annotatable`, `applied_annotation_id = a.id` and
state `ACTIVE` or `SUPERSEDED`. The annotation is stale on that annotatable when there is at least
one such finding and **every** one of them is either `SUPERSEDED`, or `ACTIVE` with a non-null
fingerprint that differs from the annotatable's current one. A fresh `ACTIVE` finding from any
assistant means the annotation still applies. No findings (a human annotation or an old-path row)
means not stale.

### Persistence

- `AssistantFindingEntity.targetFingerprint` → `target_fingerprint CHAR(64) NULL`.
- `AssistantFindingRepository.findByTargetTypeAndTargetIdInAndStateIn(...)` (or a `@Query` keyed
  by project) so the batch form runs one query per project, not one per entity.

### Schema (`V25__assistant_finding_target_fingerprint.sql`)

```sql
-- Issue #270: fingerprint of the name + text a finding was derived from. NULL = recorded before
-- #270; read as not stale until the next run on that entity fills it in.
ALTER TABLE `assistant_findings` ADD COLUMN `target_fingerprint` char(64) DEFAULT NULL;
```

No backfill (D7).

### Assistants (`assistant-core`)

- `TargetFingerprint.of(ProjectOrDomainEntity)` is a static utility and the only place the D2 rule
  lives. It is used by the write side and the read side.
- `AssistantRunWorker` computes the fingerprint of the loaded target inside `analyzeTransaction`
  and passes it to `apply` through `AssistantContext.attributes` (`targetFingerprint`).
- `upsertFinding` sets `targetFingerprint` on create **and** on touch. For an action whose
  `targetRef` differs from the dispatch target, it uses the fingerprint of that entity as loaded
  at apply (best effort; noted under Risks).
- `isUntouched(annotation, assistantUser)` returns true when the annotation is assistant-owned
  (source `ASSISTANT:` **or** source null and created by `assistant`), unresolved, and has no
  position or argument by anyone else (D4). The javadoc is corrected to match.
- `removeLegacyAnnotation` makes the same human-discussion check. A blocked removal is logged and
  skipped.
- P1: `autoResolveIfUntouched` marks the finding `SUPERSEDED` when blocked (stamping the run id).
- `CleanupPolicy` javadoc for `MARK_SUPERSEDED` and `AUTO_RESOLVE_IF_UNTOUCHED` is corrected to
  match the code.
- `FindingFreshness implements AnnotationFreshness` (`@Component`) contains the stale rule above.
- `EntityContextPackBuilder`: `isAssistantSourced` also leaves out source-null annotations created
  by `assistant` (D6).

### App (`requel-app`)

- P2 in `FindingResolutionTrackingCommandHandler`.
- A `NONE` fallback bean for contexts without assistant-core, if any test slice needs it.

### API (`service-api`, `service-impl`)

- `IssueDto` and `NoteDto` gain `String source` and `boolean stale`. `OpenIssueDto` gains
  `boolean stale` and `String source`. These are additive record components, so every
  constructor call site is updated.
- `AnnotationCommandRegistrar.toIssueDto/toNoteDto` take the stale set (or a boolean). Callers:
  `AnnotationQueryController` (per annotatable) and `ProjectQueryController.getOpenIssues`
  (batch). Command responses (create/edit) report `stale=false`, because a just-written annotation
  has not been analyzed against a newer text.
- P3 sort order: severity desc, then `stale` false before true, then the existing tiebreakers.

### MCP / gateway (`mcp-server`, `gateway-*`, `requel-cli`)

- The DTOs carry the fields, so there is no mapper work beyond the records. `getProjectContext`
  reuses `getOpenIssues` and gets them for free.
- `QueryDescriptions.GET_OPEN_ISSUES` / `GET_PROJECT_CONTEXT` gain: "An issue with `stale: true`
  was raised by an assistant against text that has since changed, or a later run no longer reports
  it; it may no longer apply."
- The CLI table output gains a `stale` column if it prints open issues as a table (to be checked
  while implementing).

### Angular

- `models/annotation.ts`: `source?: string | null; stale: boolean` on `IssueDto` / `NoteDto` /
  open-issue model.
- `shared/annotations-section.ts`: a `p-tag` "May no longer apply" with the tooltip "Raised by
  the assistant against earlier text, or no longer reported by it. Re-run analysis or resolve
  it.", a muted row class, stale after fresh within each severity (P3), and a "Hide stale" toggle that is shown only when at
  least one item is stale.
- `features/open-issues/open-issues.ts`: the same badge, mute, sort and toggle.
- The toggle state is component-local and not persisted.

## Step by step

1. V25 migration + `AssistantFindingEntity.targetFingerprint` + repository query.
2. `TargetFingerprint` + unit tests.
3. Worker → context attribute → `upsertFinding` writes the fingerprint on create and touch.
4. D4 human-discussion check in `isUntouched` and `removeLegacyAnnotation`; P1; javadoc fixes;
   P4 constant.
5. P2 in `FindingResolutionTrackingCommandHandler`.
6. `AnnotationFreshness` interface + `NONE` + `FindingFreshness`.
7. DTO fields, mappers, controllers, P3 sort (severity, then freshness); `QueryDescriptions` text.
8. D6 in `EntityContextPackBuilder`.
9. Angular model, annotations section, open issues view, specs.
10. `tmp/270-verify.sh` (`mvn clean verify`, affected Angular specs, both `tsc` checks, dev
    build).
11. AC5 check on the local roundtable project (id 621): Re-run analysis, then confirm the "Zzz"
    issue is gone.

## Test plan

**Unit (`assistant-core`)**

- `TargetFingerprint`: same input gives the same hash; whitespace-only edits give the same hash;
  a name change and a text change each change it; null name/text; case is kept.
- Applicator: the fingerprint is written on create and refreshed on touch. It comes from the
  analyze-time value even when the entity changed before apply.
- `isUntouched`: an assistant-only position → untouched; a human position → touched; a human
  argument on an assistant position → touched; resolved → touched; source-null issue created by
  `assistant` → assistant-owned.
- Auto-resolve blocked by a human position → annotation kept, finding `SUPERSEDED` (P1).
- `removeLegacyAnnotation` skips an issue with a human position.
- `FindingFreshness`: text changed → stale; tag-only change → not stale; null fingerprint → not
  stale; `SUPERSEDED` → stale; two findings with one fresh → not stale; human annotation → not
  stale; the batch form matches the single form.
- `EntityContextPackBuilder`: an old-path issue created by `assistant` is excluded; a human issue
  is kept.

**Integration (`requel-app`, own context where needed)**

- A fake `MARK_SUPERSEDED` assistant raises an issue on a goal. EditGoal changes the text. The
  annotations read taken straight after the command returns shows `stale=true`, before any re-run
  (covers the "same transaction" AC).
- The same test with EditGoal changing only a tag or relation shows `stale=false`.
- A lexical spelling finding plus a human position; edit the text to remove the word; after the
  triggered run the issue still exists and reads stale (AC3 regression for the data-loss bug).
- The same finding without a human position is removed by the run (existing behavior kept).
- P2: resolving a `SUPERSEDED` finding's issue → `MANUALLY_RESOLVED`.
- `GET /api/projects/{name}/open-issues` and MCP `getOpenIssues` / `getProjectContext` carry
  `stale` and `source`, sorted by severity then freshness.
- The V25 migration applies on MySQL (existing Testcontainers migration IT) and on H2.

**Angular**

- annotations-section spec: badge and muted class when stale; stale after fresh within a severity; the toggle hides
  and restores; the toggle is absent when nothing is stale.
- open-issues spec: the same.
- Sort spec: a stale HIGH comes before a fresh LOW; within one severity, fresh comes before stale.
- e2e: no route or page-object change. The toggle is new UI, so add one case in the annotations
  e2e only if a stale fixture can be produced cheaply through the gateway; otherwise the unit
  specs cover it (CI decides).

## Out of scope

- Re-running analysis on every edit for AI assistants (the ticket already rules this out).
- Per-property staleness (a Name edit also marks Text findings stale). Lexical re-runs resolve
  that within seconds.
- Staleness for human-written issues and notes.
- Staleness driven by related entities (steps under a scenario, relations, primary actor).
- Server-side `stale` filtering on the reads. That can go with #338's severity filter.
- Showing which run or what text a stale finding was derived from.

## Risks

- **Cross-target actions.** A finding whose `targetRef` is not the dispatch target gets its
  fingerprint at apply, so an edit to that other entity between analyze and apply is not
  detected. Rare today. Glossary candidates are project-wide but keep a per-target finding.
- **P1 changes finding history.** Findings that used to stay `ACTIVE` behind a human discussion
  will now go `SUPERSEDED`. Nothing reads that difference today except the new stale rule.
- **Batch query cost** on `getOpenIssues` for large projects. It is one indexed query per
  project; add an index on (`project_id`, `state`) if the plan shows none exists.
- **Old-path rows with human discussion** are now kept by `removeLegacyAnnotation` and are never
  flagged stale (D6). They stay until a human resolves or deletes them. That is intended, and this
  ticket does not remove human discussion.

## AC mapping

| AC | How it is met |
|---|---|
| Editing text marks untouched machine annotations stale in the same transaction | D1: derived from the fingerprint, so it is true as soon as the edit commits; IT reads straight after the command |
| Stale is visibly distinguished in the API and UI | `stale` + `source` on `IssueDto` / `NoteDto` / `OpenIssueDto`; badge, mute, sort, toggle |
| An annotation with positions or arguments is never auto-retired | D4 check in `isUntouched` and `removeLegacyAnnotation`; P1; regression IT. **Reworded (approved 2026-09-29):** "…with a position or argument by anyone other than the assistant…" |
| Stale annotations do not appear in context packs | Already true for sourced findings; D6 closes the old-path leak. MCP reads flag rather than drop them (D5) |
| The "Zzz" finding is gone after a re-analysis | #268's legacy removal / auto-resolve; step 11 checks it |
| "Decide explicitly, on the ticket, between retiring and surfacing" | D3; recorded on the issue with the decisions table above |

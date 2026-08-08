# Implementation Plan — #173 Create-flow wizards for project, actor, stakeholder, scenario, use-case

Companion to `doc/132-reactive-forms-plan.md` (§1.1 step tables, §3 version contract, §10.3
acceptance criteria) and `doc/158-form-wizard-field.md` (§2 version rule, the `app-form-wizard` API).
This document exists because verifying §3's command list against the source changed three of its
conclusions, and because the scope is larger than the issue title implies.

## Summary

Five editors — `project`, `actor`, `stakeholder`, `scenario`, `use-case` — gate authorable content
behind `@if (!isNew())`, so creating one produces a stub the user must navigate back into. #173
converts each create flow to an `app-form-wizard`.

Three findings from source verification, each of which changes the work:

1. **None of the five is reactive yet.** #173 is not "add wizard chrome to reactive editors"; it is
   the full #132 conversion (reactive forms, `app-field` rows, `applyCommandErrors`, delete
   `trackChanges`) across 2,758 lines, *plus* the wizard, *plus* the version contract.
2. **Every client-reachable association command bumps the parent; only three return it.** §3 was
   right that the Goal/Story precedent does not carry over, and right to ask for a refetch — six of
   the nine send no entity back at all, because their registration declares no result extractor.
   See §2 for the table and for the correction to an earlier revision of this document.
3. **`scenario-editor` is not the shape §3 assumes.** Its steps are not association commands at all.

## Decisions (locked)

1. **One PR, sliced commits** — `173-create-flow-wizards`, one commit per editor in risk order.
   §1.1 suggests `use-case-editor` get its own PR; overridden to keep CLAUDE.md's one-issue /
   one-branch / one-PR rule. Mitigated by commit slicing and by landing use-case last.
2. **Backend fixes land in this branch** where a command bumps a version needlessly. Per §4 below
   no such fix is currently required, so this is a contingency, not a planned change. Anything
   found gets its own commit in the slice that surfaced it, never folded into an Angular commit.
3. **Write the five `applyCommandErrors` maps now**, following #132's pattern exactly. #176 deletes
   all eleven at once later; #173 takes no dependency on it.
4. **Branch is cut from `171-bean-validation`**, not `release/2.0` — the five editors need
   `ARTIFACT_NAME_MAX_LENGTH`, which only exists there. Rebase after #171 squash-merges:
   `git rebase --onto origin/release/2.0 171-bean-validation 173-create-flow-wizards`.

## 1. What exists today

| Editor | Lines | ngModel | FormGroup | app-field | trackChanges | `!isNew()` gates |
|---|---|---|---|---|---|---|
| `project-editor` | 368 | 3 | 0 | 0 | 0 | 2 |
| `actor-editor` | 431 | 4 | 0 | 0 | 3 | 2 |
| `stakeholder-editor` | 518 | 8 | 0 | 0 | 5 | 1 |
| `scenario-editor` | 692 | 10 | 0 | 0 | 4 | 2 |
| `use-case-editor` | 749 | 6 | 0 | 0 | 4 | 3 |

None imports `applyCommandErrors`. All five are template-driven. This is the same starting point
#132's six editors had, so the conversion pattern is established — but it is the bulk of the work,
and the 8-point estimate was set before this was confirmed. **Re-point after slice 1 lands**, when
the per-editor cost is measured rather than guessed.

## 2. Correction to §3, part one — the command verification table

§3 asked that each command be verified rather than assumed. Done, against
`modules/project-jpa/.../impl/command/`:

| Command | Merges parent (bumps `@Version`) | Returns parent to the client | Wizard must |
|---|---|---|---|
| `AddGoalToGoalContainer` | yes | **no** | refetch |
| `RemoveGoalFromGoalContainer` | yes | **no** | refetch |
| `AddStoryToStoryContainer` | yes | **no** | refetch |
| `RemoveStoryFromStoryContainer` | yes | **no** | refetch |
| `AddActorToActorContainer` | yes | **no** | refetch |
| `RemoveActorFromActorContainer` | yes | **no** | refetch |
| `AddScenarioToUseCase` | yes | yes | read `result.entity` |
| `RemoveScenarioFromUseCase` | yes | yes | read `result.entity` |
| `SetPrimaryScenarioOnUseCase` | yes | yes | read `result.entity` |
| `AssignTag` | no — mutates `Tag` | n/a | nothing |

**Correction, 2026-08-07.** An earlier revision of this document claimed all nine returned the
merged parent and that §3's refetch branch was dead code. That was wrong, and the error is worth
recording because it is invisible from the command implementations alone.

`AddGoalToGoalContainerCommandImpl` really does end with `setGoalContainer(addingContainer)` — but
that is the *Java command object*, not the HTTP response. `CommandController` builds the response
body from `ApiCommandFactory.extractResult`, which returns `reg.resultExtractor().apply(command)`
if the registration declared one and **`null` otherwise**. Six of these register through the 4-arg
`CommandRegistry.register` overload, which passes `null` for both the file applicator and the
result extractor, so the merged container never reaches the client. `AddScenarioToUseCase` and its
two siblings pass the 6-arg overload with `cmd -> ProjectQueryController.toUseCaseDetailDto(...)`,
which is why those three differ.

Consequence: **§3's "or refetch when the command does not return the parent" branch is required for
six of the nine, not dead.** Read `result.entity` only for the three that populate it.

Second consequence, worth its own ticket: `CommandController.publishEntityChangedIfPresent` derives
its SSE target from the result DTO's `id()`. A null result means **no targeted SSE event fires** for
those six commands, so another session with the same actor/story/use-case open is never told to
refresh after a goal, story or actor association changes. Registering result extractors would fix
the version problem and this one together, but the container is polymorphic (`GoalContainer` is an
Actor, Story, UseCase or Stakeholder), so a single extractor has to switch on the concrete type —
which is why this plan takes the client-side refetch and leaves the backend change to a follow-up.

## 3. Correction to §3, part two — four commands are unreachable from the client

§3 lists `EditScenarioStep`, `DeleteScenarioStep`, `CopyScenarioStep` and `ConvertStepToScenario`
as commands this ticket must verify. **The Angular client never invokes any of them.** The only
commands `scenario-editor.ts` issues are `EditScenario`, `CopyScenario` and `DeleteScenario`; a
repo-wide grep for `EditScenarioStep` and `stepCommands` across `requel-angular/` returns nothing.

They are server-internal: `CopyScenarioStep` is driven by `CopyScenarioCommandImpl`, and
`EditScenarioStepCommand` is a *sub-command* carried in `EditScenarioCommand.getStepCommands()`.

This matters because two of them would otherwise have been the hard cases —
`DeleteScenarioStepCommandImpl` mutates `scenarioReferer.getSteps()` on every using scenario, and
`ConvertStepToScenarioCommandImpl` removes and re-adds into `scenario.getSteps()`; both dirty the
parent without returning it, so both would have forced the refetch path. Since neither is reachable
from a wizard, **no refetch path is needed anywhere in #173.** Leave them alone; if they are ever
exposed to the client, that ticket owns the problem.

## 4. Correction to §3, part three — `scenario-editor` is a different shape

`EditScenarioCommandImpl.execute()` executes its `stepCommands`, calls `checkExpectedVersion`
(#108), merges the scenario, then does:

```java
scenarioImpl.getSteps().clear();
for (EditScenarioStepCommand executedCommand : stepEditCommands) { ... }
setScenario(getProjectRepository().merge(scenarioImpl));
```

Steps are a **whole-list replace inside the scenario's own save**, not incremental associations. The
client matches this: `scenario-editor` holds steps in a `stepNodes` signal, edits them locally, and
submits everything in one `EditScenario` call, with an explicit "Steps have unsaved changes" hint.

So the scenario wizard is the #158 two-mutation case, not the §3 many-mutation case:

- Step 1 `Details` → `EditScenario` with no steps. Capture `id` and `version` from the result.
- Step 2 `Steps` → `EditScenario` again, full step list, using the version from step 1. One command,
  one version spend, and `checkExpectedVersion` enforces it.
- Because step 2 re-sends name/text/type, a user who goes back to step 1 and edits the name must
  have that change carried into the step-2 payload. Source the payload from the form, not from a
  snapshot taken at step-1 commit.

`editingStep() !== null` already participates in the SSE guard (`scenario-editor.ts:435`); that
guard must survive into the wizard alongside the `hasUnsavedChanges()` condition.

## 5. Per-editor plans

### 5.1 `scenario-editor` — slice 1, highest value

2 steps · Details → Steps. Gating steps means create yields an empty scenario, which is the whole
finding. Commands: `EditScenario` only (see §4). Existing `scenario-editor.a11y.spec.ts` is the only
a11y spec among the five — extend it rather than starting one.

### 5.2 `actor-editor` — slice 2

2 steps · Details → Goals. Commands: `AddGoalToGoalContainer`, `RemoveGoalFromGoalContainer` — both
bump, both return. "Referenced By" (use cases, stories) is derived and read-only: stays gated,
stays outside the wizard, per decision 2 of the 132 plan.

### 5.3 `stakeholder-editor` — slice 3

2 steps · Details (incl. Permissions) → Goals. The `isUserType()` mode selector stays on step 1 and
stays `[disabled]="!isNew()"`. Permissions is already visible on create, so only Goals is new
chrome. Two save commands by mode: `EditUserStakeholder` / `EditNonUserStakeholder`. Eight ngModel
bindings, including `perm.checked` inside an iteration — the fiddliest reactive conversion of the
five.

### 5.4 `project-editor` — slice 4

2 steps · Details → Tags. Commands: `EditProject`, plus `AssignTag`, which is the one clean command
in the table — the Tags step does *not* spend the project's version. The odd one out in another way
too: it is the only one of the five not created inside an existing project context, so there is no
parent project to resolve for routing or permissions.

### 5.5 `use-case-editor` — slice 5, last

4 steps · Details → Scenarios → Goals & Stories → Actors. Largest file, five gated sections, and
the only editor touching all eight bumping commands. Landing it last means the version-refresh
helper is already proven by four smaller wizards.

## 6. Slice order

1. `scenario-editor` — establishes the two-mutation wizard and the version-refresh helper.
2. `actor-editor` — establishes the association-command pattern (`AddGoalToGoalContainer`).
3. `stakeholder-editor` — adds the mode-selector and permissions complexity.
4. `project-editor` — adds the clean-command case and the no-parent-project case.
5. `use-case-editor` — four steps, all patterns combined.

Each commit: reactive conversion + `app-field` rows + `applyCommandErrors` map + wizard + specs, for
one editor. A slice that cannot pass `npm test -- --watch=false` does not get committed.

## 7. Tests

Per §10.3, per wizard: **create → advance to the last step → mutate an association → navigate back
to step 1 → edit the name → Continue succeeds with no 409.** This is the test that catches a stale
held version, and it is the reason the table in §2 exists.

Also per editor: an entity created through the wizard is identical to one created by the previous
form; `hasUnsavedChanges()` derives from `form.dirty` with `trackChanges()` deleted; a 409 keeps the
step, refreshes, and renders in the wizard's `role="alert"` region; axe clean with fields in the
error state. Four of the five have no a11y spec today — those are new files.

## 8. Ticket state (verified 2026-08-06)

Read back from `gh issue view 173`:

- **All thirteen §10.3 acceptance criteria are on the issue, verbatim.** Despite
  `scripts/update-ui-ux-subissues.sh` having no `173` entry, they were added by hand. The issue is
  usable as the implementation checklist.
- Milestone `v2.0`, project "Requel 2.0 (Todo)", **no labels**. Blocked-by #132 and blocking #138
  are both linked.
- **#171, #172, #173 and #176 were never sub-issues of epic #124** — the four split out of #132 were
  added to the rollup doc but not to the epic's native list, which is why `trackedInIssues` came
  back empty. Added 2026-08-06; the epic now returns 33, matching the rollup's count. This also
  required updating `ORDER` in `scripts/update-ui-ux-subissues.sh` from 29 to 33 entries, because
  `reorder_subissues()` exits 1 when `ORDER` and the live sub-issue set differ. The two must always
  change together. That edit rides on this branch.
- Two comments carry decisions not in the issue body:
  - **Error maps** — #176 fixes violation naming at source and deletes all the maps. If #176 lands
    first, skip them here and call `applyCommandErrors(form, result.violations)` with no third
    argument. Otherwise add them. A missing entry degrades to a page-level message rather than
    breaking, so it blocks nothing either way. Consistent with decision 3 above.
  - **#171 dependency** — inherit `ARTIFACT_NAME_MAX_LENGTH` from `validation-limits.ts` rather than
    inventing client-side caps. Confirms decision 4 above.

**The issue body reproduces §1.1 and §3 verbatim, including all three errors corrected in §2–§4 of
this document.** Anyone implementing straight from the ticket will build the refetch path that is
never needed and go looking for four commands the client cannot reach. Correct the ticket before
starting, or at minimum comment the verification table onto it.

## 9. Open questions

- **Re-point the issue** once slice 1 measures the true per-editor cost. 8 was set before the
  reactive conversion was known to be in scope.
- **§3 should be corrected in `doc/132-reactive-forms-plan.md`** as well as on the ticket, or the
  error outlives both. Cheapest fix is a pointer from §3 to this file.

Part of the UI/UX remediation epic #124. Blocked by #132 (merged) and #171 (in review). Blocks #138.

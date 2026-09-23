# 325 — Primary actor and scenario steps: leave unchanged vs clear

Implementation plan for https://github.com/rreganjr/Requel/issues/325.
Base: `release/2.0` @ `07d5b71e`. Branch: `325-primary-actor-and-steps`.

## Summary

#316 made null mean "leave it as it is" for the scalar properties of the `Edit*Command`
updates. This ticket does the same for the two properties #316 left out, primary actor and
scenario steps, and fixes what the review found around them:

- **Every use-case save in the UI wipes its primary scenario's steps and renames the scenario
  back to the use-case name.** The `EditUseCase` API mapping always passes an empty step list,
  and the use-case update always runs a nested `EditScenario` with it and with
  `setName(getName())`. Confirmed with a throwaway IT on `07d5b71e`.
- **Dropped steps become orphans.** `EditScenario` clears the step list and re-adds what was
  sent, so a removed plain step is never deleted. It keeps its `projectordomain_id` and its name
  in the shared step/scenario unique key, and `DeleteProject`, which only reaches steps by
  walking scenarios, leaves it behind and then fails on MySQL
  (`FKoplmulkyjqc4foqfjtcwjxm2x`, the `[e2e cleanup] … leaving project behind` lines).
- **The #305 spelling-fix routing hits both.** A correction on a scenario or use case clears
  its steps; on a story it drops the primary actor.

Backend, one Flyway migration, Angular. Gates: `mvn clean verify`, Angular unit + typecheck +
dev build, e2e in CI.

## Locked decisions

1. **Primary actor on Story.** `primaryActorName` null leaves the actor as it is; `""` (or
   blank) clears it and removes the actor's referer row; a name is resolved as today.
2. **Primary actor on UseCase is required.** `usecases.primary_actor_id` is `NOT NULL` and the
   mapping is `optional = false`, so `""` is refused with a field error ("a use case needs a
   primary actor") rather than cleared. Null leaves it as it is. The use-case editor marks the
   actor required and drops its clear (×) button, which silently did nothing. Decided in review.
3. **A story actor name that doesn't resolve is refused** with a field error on
   `primaryActorName`, on create and update. Today create silently saves no actor and update
   silently *clears* it, while logging "leaving unchanged". The UI only offers existing actors,
   so this surfaces only for API/MCP callers and races. This is the one create-side change.
   UseCase keeps auto-creating an unknown actor.
4. **Steps.** On update, null step commands leave the step list as it is; an empty list removes
   all steps. Create with null is an empty list, as today. The `EditScenario` API mapping passes
   null when `steps` is absent, and the `EditUseCase` mapping (whose input has no `steps` field)
   passes null instead of an empty list.
5. **A dropped plain step is deleted if no other scenario uses it**, through
   `DeleteScenarioStepCommand` (annotation and glossary cleanup), run authorization-exempt: the
   caller already holds `Scenario[Edit]` for the edit that dropped it. A dropped sub-scenario is
   left alone, since it's a standalone scenario. Decided in review.
6. **Use-case rename syncs the scenario name only if they matched.** On update, the nested
   `EditScenario` runs only when step commands are supplied, when the use case has no primary
   scenario yet (the editor's "create primary scenario" button relies on this), or for the name
   sync: when the use-case name actually changes and the scenario's name equals the *old*
   use-case name, the scenario takes the new name. A scenario with a different name is never
   renamed, and a save without a rename never touches it. The nested update no longer forces
   `scenarioTypeName = Primary` (null leaves the type; only Primary scenarios can be chosen as a
   use case's primary scenario anyway). Decided in review.
7. **Existing orphans: DeleteProject sweep + Flyway V19.** `DeleteProject` also deletes every
   step row carrying the project's `projectordomain_id`, found by a repository query, so
   orphans can't block it. `V19__delete_orphan_steps.sql` deletes the orphans already in the
   database: plain steps (`type = 'com.rreganjr.requel.project.Step'`) with no `scenario_steps`
   row, plus their `annotation_annotatable`, `terms_referers` (a `@ManyToAny` join with no FK,
   where a row naming a deleted step makes loading the term's referers throw),
   `scenarios_annotations`, `scenarios_glossary_terms` and `pods_scenarios` rows. The annotations themselves stay, as
   they do when `DeleteScenarioStep` runs; the project's annotation sweep removes them on
   project delete. Steps aren't taggable, so there are no `tag_taggable` rows. Decided in
   review.

## Contracts

| Input (update) | null / absent | `""` | value |
|---|---|---|---|
| Story `primaryActorName` | unchanged | clears | resolve; unknown name → 422 on `primaryActorName` |
| UseCase `primaryActorName` | unchanged | 422 on `primaryActorName` | resolve; unknown name auto-creates the actor (as today) |
| Scenario `steps` | unchanged | — | replaces the list (`[]` removes all); dropped unused plain steps are deleted |

`EditStoryInput`/`EditUseCaseInput`/`EditScenarioInput` javadoc states the contract.

## Step by step

**Commit 1 — Story actor.** `EditStoryCommandImpl`: resolve only a non-blank name, throwing
`EntityValidationException.validationFailed(Story.class, "primaryActorName", …)` when it
doesn't resolve. On update, skip actor handling entirely when the name is null; blank clears
(existing remove-join-row + `setPrimaryActor(null)`).

**Commit 2 — UseCase actor + nested scenario.** `EditUseCaseCommandImpl`: blank name → the
field error; on update remember the old name before `setName`, then run the nested
`EditScenario` per decision 6.

**Commit 3 — Scenario steps.** `EditScenarioCommandImpl`: skip the step replacement on update
when step commands are null; otherwise remember the previous list, replace it, and delete each
dropped plain step that `ProjectRepository.isStepUsedByAnotherScenario(step, scenario)` says no
other scenario holds. New repository method in `ProjectRepository`/`JpaProjectRepository`
(JPQL `count(s) … where :step member of s.steps and s <> :scenario`).

**Commit 4 — API mapping.** `ProjectCommandRegistrar`: `EditScenario` sets step commands only
when `i.steps() != null`; `EditUseCase` stops passing an empty list.

**Commit 5 — DeleteProject sweep + V19.** New `ProjectRepository.findStepsByProjectOrDomain`;
step 2 of `DeleteProjectCommandImpl` seeds its set from it as well as from the scenario walk.
New `V19__delete_orphan_steps.sql`, idempotent and a no-op on a clean database.

**Commit 6 — Angular.** `story-editor.ts` sends `primaryActorName ?? ''` (the p-select clear
sets null). `use-case-editor.ts`: `Validators.required` on `primaryActorName` with a
"A use case needs a primary actor." message, no `showClear`, and both save paths send the
value as-is. Drop the now-dead `clearPrimaryActor` from `UseCaseEditorPage`.

**Commit 7 — tests** (below). Keep `tmp/325-verify.sh`.

## Test plan

**Java** (`modules/requel-app/src/test/.../project/impl/command/` unless noted):

| Class | Cases |
|---|---|
| `StoryCommandTest` | update without `primaryActorName` keeps the actor; `""` clears it and the actor no longer lists the story as a referer; unknown name refused on update (actor unchanged) and on create |
| `StoryPrimaryActorMappingTest` | the clear case now sends `""` |
| `UseCaseCommandTest` | update without actor keeps it; `""` refused; a save without steps keeps the primary scenario's steps and name (fails on `07d5b71e`); rename syncs a matching scenario name; rename leaves a differently named scenario alone |
| `ScenarioCommandTest` | update with null step commands keeps the steps; `[]` removes them and deletes the plain steps; a dropped step another scenario uses survives; a dropped sub-scenario survives |
| `project/DeleteProjectIT` | an orphan plain step (manufactured with `JdbcTemplate` by deleting its `scenario_steps` row) doesn't block `DeleteProject`, and its row is gone. `DeleteProjectMySqlIT` inherits it, which is where the FK actually bites |
| `project/DeleteProjectMySqlIT` | Testcontainers MySQL: manufacture an orphan with its join rows, re-run `V19__delete_orphan_steps.sql` (the existing `runV17` pattern, generalised to `runMigration`), assert the orphan and its join rows are gone and a used step survives; running it twice is harmless. Added here rather than in a new class so it shares the one MySQL container |
| `CommandGatewayIT` or the existing registrar mapping test | `EditScenario` without `steps` keeps the steps; `EditUseCase` save keeps the primary scenario's steps |

**Angular unit:** story editor sends `''` after clearing the actor; use-case editor refuses to
save without an actor and sends the actor as-is.

**e2e:** `use-cases.e2e.ts` — add steps to a use case's primary scenario, save the use case,
the steps are still there (read back through the API; new `addStepsToUseCaseScenario` and
`getScenarioStepCount` helpers). `stories.e2e.ts` already clears the actor and reloads.
The scenario specs' cleanup should no longer print `leaving project behind`.

## Out of scope

- Deleting annotations left without an annotatable (existing behaviour of `DeleteScenarioStep`).
- Additional-scenario management on use cases (`AddScenarioToUseCase` etc.), untouched.
- Making the use-case primary actor optional (decision 2).

## Risks

- **An API caller that relied on "omit steps = clear".** Now it keeps them; `[]` clears. Called
  out in the Input javadoc and the PR.
- **Deleting a dropped step the caller meant to re-add elsewhere in the same request.** The
  delete runs after the new list is in place and checks every other scenario, so a step moved
  to another scenario in a separate later request is gone; one re-sent in this request is kept.
- **V19 on a big database.** Anti-join over `scenarios`/`scenario_steps`, both indexed on the
  ids involved; same materialize-then-delete shape as V17.

## AC mapping

| AC | Commit | Proven by |
|---|---|---|
| Omitting `primaryActorName` leaves the actor | 1, 2 | Story/UseCase tests |
| `""` clears Story's actor and its referer row; UseCase refuses (decision 2) | 1, 2 | Story/UseCase tests |
| null `steps` leaves the list; `[]` removes all | 3, 4 | Scenario tests, gateway test |
| Create semantics unchanged (except decision 3) | 1–3 | existing create tests |
| Angular sends `""` to clear the story actor; use-case actor required | 6 | Angular specs, `stories.e2e.ts` |
| One partial-update test per case | 7 | table above |
| (comment) dropped steps deleted; DeleteProject not blocked; existing orphans removed | 3, 5 | Scenario tests, `DeleteProjectIT`, `OrphanStepMigrationMySqlIT` |
| (review) use-case save keeps its scenario's steps and name | 2, 4 | UseCase tests, e2e |

# #247 DeleteProject cascade hardening — implementation plan

Follow-up to #240 / #246. Grounded in `doc/240-delete-project-cascade-hardening.md`
(the diagnosis this ticket was filed from). This plan records the confirmed
mechanism, the fix applied for each symptom, the residual risks, and the
verification protocol. `mvn verify` cannot catch any of this (H2, Flyway
disabled, Hibernate `create-drop`); only full-stack e2e on MySQL does — so each
change must be verified with the targeted e2e specs below, run individually so a
500 stack trace is visible, **before** pushing (per the agreed workflow).

## Confirmed mechanism (read from the code, release/2.0 @ #246)

### Symptom #2 — detached `IssueImpl` poisons the session (HTTP 500) — the crux
Every annotatable's annotation collection cascades PERSIST:
`AbstractProjectOrDomainEntity.getAnnotations()` and `ProjectImpl.getAnnotations()`
are both `@ManyToMany(cascade = { PERSIST, REFRESH })`.

`RemoveAnnotationFromAnnotatableCommandImpl` deleted the `annotation_annotatable`
join row via native SQL (a documented Hibernate 6.5 `@ManyToAny` workaround) and
then called `em.detach(annotation)` + `find(...)`. Inside a multi-entity delete
(DeleteProject / DeleteUseCase) the **same** annotation instance is still
referenced by other *managed* cascade-PERSIST collections (other annotatables'
`getAnnotations()`, `project.getAnnotations()`). A detached entity reachable
through cascade PERSIST makes the **next** auto-flush throw
`detached entity passed to persist: com.rreganjr.requel.annotation.impl.IssueImpl`.
The trigger is whatever flushes next — the annotation-removal path
(`...:110`) or the later actor-removal native query (`...:129`) — but the cause
is the lingering detached annotation.

Decisive evidence: the sibling `RemoveActorFromActorContainerCommandImpl` already
uses `em.refresh(removedActor)` (keeps it managed) after its own native
join-table delete and does **not** itself cause the 500. Only the annotation
command used `detach`. So the fix is to align the annotation command with the
working actor command.

### Symptom #1 — orphan use-case scenarios block `delete(project)` (HTTP 409)
A use-case's primary scenario (`UseCase.getScenario()`, FK `use_cases.scenario_id`)
and additional scenarios (`getAdditionalScenarios()`, join `usecase_scenarios`)
are NOT in `project.getScenarios()`. `DeleteUseCaseCommandImpl` only detached them
(`// TODO: delete the main scenario?`) and `DeleteProjectCommandImpl` deletes only
`project.getScenarios()`, so those scenarios survive as orphans that still
reference the project via `scenarios.projectordomain_id -> pods`, and the final
`delete(project)` is refused.

Important schema fact: `StepImpl` is `@Table(name = "scenarios")` with SINGLE_TABLE
inheritance and `ScenarioImpl extends StepImpl` — so a scenario's **steps** are
rows in the same `scenarios` table carrying the same pods FK. Deleting a use-case
scenario without its steps only converts a scenario-orphan into step-orphans that
still block `delete(project)`. The fix must delete the steps too.

### Symptom #3 — `DeleteGoal` and `goals_annotations` (HTTP 409)
`DeleteGoalCommandImpl` already iterates `goal.getAnnotations()` and removes each
via `RemoveAnnotationFromAnnotatableCommand`, which clears the `goals_annotations`
@ManyToMany row. Its failures were the same session poisoning as #2 (deterministic
case) plus an assistant-timing race (`AnalysisInvokingCommandHandler` annotates the
goal after the command read `goal.getAnnotations()`). The deterministic case is
resolved by the #2 fix. The race is environmental (dev profile, assistants active)
and is tracked as a residual item below, not fixed here.

## What the MySQL run actually showed (2026-09-09, goals + stories e2e, dev profile, app log
## at `tmp/requel-e2e.log` + `SHOW ENGINE INNODB STATUS`)

The #240 diagnosis above was right about symptoms #1 and #2 but missed the mechanism
behind the failures that remained after the `refresh()` and DeleteUseCase fixes:

- **No pool exhaustion** (Hikari `waiting=0` throughout) but `active` climbing 1 → 6:
  connections were being *held* by blocked transactions, not queued for.
- **InnoDB deadlocks and 40+ second lock waits.** `SHOW ENGINE INNODB STATUS` showed both
  sides of the latest deadlock to be
  `delete from annotation_annotatable where annotatable_id = ?` — the "proactive cleanup"
  in `DeleteActorCommandImpl`. `annotation_annotatable`'s only index was the PK
  `(annotation_id, annotatable_type, annotatable_id)`, so that statement full-scanned and
  X-locked every row of the table (one transaction held 2,257 row locks). Two concurrent
  deletes deadlocked; every assistant insert and every targeted
  `RemoveAnnotationFromAnnotatable` delete queued behind whichever was running. The
  statement also lacked `annotatable_type`, so it unlinked every entity type sharing the
  actor's numeric id.
- **`RetryOnLockFailuresCommandHandler` retried at every nesting level** (eight nested
  retries in 300 ms inside one DeleteProject) in an already rolled-back transaction, and a
  `DeleteProject` the client had given up on after 30 s kept its locks for minutes
  (still on "retrying attempt: 2" 76 s later), wedging every later test.
- **The 409s were races with the assistant.** Every `goals_annotations` /
  `stories_annotations` FK failure landed 3–6 s after the entity's `Edit*`, while
  `assistantTaskExecutor-N` was still parsing that entity's text. Annotations use
  `IDENTITY` ids, so an assistant's inserts hit MySQL immediately and their locks (incl.
  the InnoDB shared lock an FK insert takes on the parent `goals`/`stories`/`pods` row)
  were held until the multi-second `REQUIRES_NEW` run committed. A delete that loaded the
  entity before that commit saw no annotations; Hibernate then skips the join-table delete
  for a collection whose loaded snapshot was empty, and the row the assistant committed in
  between fails the FK. MySQL's "LATEST FOREIGN KEY ERROR" caught the mirror image live: an
  assistant inserting `stories_annotations` for a story the test had already deleted.
- **Scenario ownership.** `ScenarioImpl`'s constructor adds every scenario — a use case's
  primary one included — to `project.getScenarios()`; additional scenarios are created at
  project level and attached via `usecase_scenarios` (owned by the use case, no inverse
  mapping). DeleteProject deleted scenarios *before* use cases, so a use case's primary
  scenario went while `usecases.scenario_id` still referenced it, and `DeleteScenario`
  never detached a scenario from `usecase_scenarios`. In the DeleteUseCase fix, the
  `getUsingUseCases().isEmpty()` "shared" check also ran before the use-case delete had
  been flushed (collection initialization does not auto-flush), so it saw the deleted use
  case and skipped every owned scenario.

## Changes on this branch

1. **`V16__index_annotation_annotatable_by_annotatable.sql`** — index on
   `annotation_annotatable (annotatable_id, annotatable_type)`; mirrored as `@Index` on the
   `@ManyToAny` `@JoinTable` in `AbstractAnnotation` so H2 create-drop schemas match.
2. **`RemoveAllAnnotationsFromAnnotatableCommand`** (annotation-domain / annotation-jpa)
   backed by `AnnotationRepository.unlinkAllAnnotations(annotatable)`: two index-backed
   native deletes against the *database* state — `annotation_annotatable` restricted to the entity's registered discriminator(s)
   (`AnnotatableTypeRegistry`, plus class names for legacy rows) and the entity-owned
   `<table>_annotations` table, located through the Hibernate mapping metamodel so no table
   or column name is hand-maintained — then every annotation that was linked is
   `refresh`ed (so a `@ManyToAny` collection loaded earlier in the cascade no longer holds
   the entity being removed) and `DeleteIssue`/`DeleteNote` runs for any left with no
   annotatables. `AbstractProjectCommand.removeAllAnnotationsBeforeDelete()`
   calls it, auth-exempt, immediately before `delete(entity)` in every `Delete*Command`
   (actor, glossary term, goal, goal relation, report generator, scenario, step,
   stakeholder, story, use case, team, project). `DeleteActorCommandImpl`'s untyped
   full-scan delete is removed.
3. **`RetryOnLockFailuresCommandHandler`** retries only when no transaction is active
   (`TransactionSynchronizationManager.isActualTransactionActive()`); nested cascade
   sub-commands propagate the failure to the outer command, whose retry starts a fresh
   transaction. Unit test added (platform-core gains `spring-boot-starter-test`).
4. **`AssistantRunWorker`** runs a queued run as two transactions via `TransactionTemplate`
   (REQUIRES_NEW each): *analyze* (loads target, runs every matching assistant, writes
   nothing) commits before *apply* (short; re-checks the target still exists and skips the
   run — findings discarded — if it was deleted meanwhile). `runInNewTransaction(UUID)` is
   kept as a deprecated alias of `run(UUID)`; dispatcher and tests updated; two worker tests
   added (target vanishing between phases; phase ordering).
5. **Cascade order / scenario ownership**: `DeleteProjectCommandImpl` deletes use cases
   first, then the remaining steps and scenarios. `DeleteUseCaseCommandImpl` flushes after
   `delete(usecase)` and excludes the use case being deleted from the shared-primary check.
   `DeleteScenarioCommandImpl` removes the scenario from every use case's
   `getAdditionalScenarios()` before deleting it.
6. **Tests**: `DeleteProjectIT` gains `deleteProjectCascadesUseCaseScenariosStepsAndAnnotations`
   (use case with primary scenario + two steps, an additional scenario attached from
   project level, an annotated goal, an annotated use case, a story with a primary actor,
   an actor). **`DeleteProjectMySqlIT extends DeleteProjectIT`** re-runs every case on a
   Testcontainers `mysql:8.4` with Flyway building the schema (`ddl-auto=none`) and
   `innodb_lock_wait_timeout = 10` so contention fails fast; skipped, not failed, when
   Docker is unavailable (`org.testcontainers:mysql` / `junit-jupiter` test deps, versions
   from the Boot BOM).
7. **Stale-reference guards** (added after the first MySQL rerun of goals + stories, which
   passed 20/20 with no lock waits over 2 s but left one swallowed
   `DeleteProject ... TransientObjectException: persistent instance references an unsaved
   transient instance of 'null'` in the app log — the best-effort `deleteProject` e2e helper
   hides it, so the suite was green while a project survived). Not reproducible on H2 even
   after extending `DeleteProjectIT` with a shared note across two stories and a goal, a
   story primary actor, and actor/story goal-container membership, so every place a
   cascade could re-attach a removed entity was closed instead: `Remove{Actor,Goal,Story}
   From*Container` no longer `merge()`s a container the session has already removed (a merge
   of a removed entity re-persists it, and its collections then reference rows deleted
   natively in the same transaction); `DeleteStory` refreshes the primary actor after the
   native `actors_actorcontainers` delete so its `@ManyToAny` referers set is current;
   `DeleteActor` detaches the goals it holds through `RemoveGoalFromGoalContainer` before the
   referer sweep; and the annotation sweep refreshes every linked annotation (item 2). The
   unused `countAnnotatableLinks` was dropped from `AnnotationRepository`.
8. **REFRESH cascade cut** (the actual cause of the 30 s `DeleteGoal` / 80 s `DeleteProject`
   the second MySQL rerun still showed, and of the TransientObjectException). Hibernate session
   metrics for one `DeleteProject`: *304,705 JDBC statements, 68 s executing, 40 flushes over
   ~8,000 entities* — the whole database, in a database of ~10k entities. Every
   `em.refresh(entity)` (the `Remove*Container` commands, `RemoveAnnotationFromAnnotatable`,
   the #247 sweep) cascaded `REFRESH` upward: `entity.projectOrDomain` → `project.createdBy` →
   `user.userRoles` (EAGER) → `ProjectUserRole.activeProjects` (EAGER) → every project the user
   is on (467 leaked `e2e-*` projects on the dev database) → their actors/goals/stories/scenarios
   → annotations → issues ↔ positions, one select per row, repeated per `refresh()` call. With
   the whole database in the session, the 160 `annotation_annotatable` rows whose Story/Goal/
   Actor was deleted by the pre-#247 assistant race are cascade-checked at flush and fail as
   "persistent instance references an unsaved transient instance of 'null'" — which is why a
   fresh H2 schema never reproduced it. Removed `CascadeType.REFRESH` from the three
   upward/outward edges: `AbstractProjectOrDomainEntity.projectOrDomain`, `UserImpl.userRoles`,
   `ProjectUserRole.activeProjects`. A refresh now reloads the entity and what it owns. The lock
   contention that followed (a 50 s `EditActor` 1205 on `actor_actorcontainers` from a *new*
   project) was only the next-key gap lock of DeleteActor's `DELETE ... WHERE actor_id = ?`
   held for the 80 s the transaction lived; it disappears with the transaction time.
   After the cut the stories spec passed 14/14 in 1.2 min (`remove goal` 35 s → 7.7 s) and the
   worst session fell from 304,705 to ~5,100 statements (the admin's `ProjectUserRole.
   activeProjects` is still EAGER over 467 leaked projects — a follow-up, not a #247 blocker).
9. **`DeleteAnnotationGroupCommand`** (annotation-domain / annotation-jpa) — the
   TransientObjectException that survived item 8, now fully explained: the trace is
   `Cascade.cascadeToOne` with entity name `'null'`, i.e. an `@Any`, and the only `@Any`
   to-one is `AbstractAnnotation.groupingObject` → the project. Annotations *grouped under*
   the project but no longer linked to any of its entities (an assistant's findings applied
   after their target was deleted; links left dangling by the pre-#247 race — the dev DB
   had 21 under project 579 and 160 dangling links overall) survive the entity cascade; if
   one is in the session when `delete(project)` flushes, the commit fails. `DeleteProject`
   step 11 now runs the new command: `findAnnotationIdsByGroupingObject(project)`, then per
   annotation a native `unlinkAnnotation(id)` (so Hibernate never has to resolve a link to a
   missing row), `refresh`, and `DeleteIssue`/`DeleteNote` (auth-exempt). `DeleteProjectIT`
   gains `deleteProjectDeletesOrphanedAnnotationsGroupedUnderIt`, which manufactures both
   orphan flavours with `JdbcTemplate` and asserts they go with the project.

Verified in an offline copy: all platform-core / assistant-core unit tests, `DeleteProjectIT`
(7/7 on H2), `DeleteProjectMySqlIT` skipping cleanly without Docker, and the command test
classes touching deletes (Actor/Goal/UseCase/Scenario/ScenarioStep/Story/Stakeholder/
GoalRelation/Copy/ScenarioContainer/Annotation/EditProject/StoryPrimaryActorMapping,
NlpDisabledDispatch) — 103 tests green. The full `mvn verify` (dictionary-dependent tests,
`DeleteProjectMySqlIT` with Docker) runs on the developer machine.

## Residual risks / follow-ups
- A `DeleteStory`/`DeleteGoal` can still hit a 1213 deadlock against an assistant *apply*
  committing findings for the same row (seen once per stories run). That is expected: the
  apply transaction is now short and the outermost-only retry re-runs the command, logging
  `<Command> succeeded after retry`. Count `Deadlock found` against `succeeded after retry`
  in the app log; they should match. Eliminating it would mean serializing the assistant's
  apply against user commands on the target row - not worth it for a retry that works.
- The legacy `AssistantTaskRunner` path (`EditScenario`, `EditGlossaryTerm`, import's
  `analyzeProject`) still runs analysis and writes in one `REQUIRES_NEW` transaction; every
  entity edit dispatched through `AnalysisRequestSource` (goal, story, actor, use case,
  step) now uses the split worker. Migrate the rest or accept the narrower window.
- `DeleteScenario` on a scenario that is some use case's *primary* scenario still leaves the
  `usecases.scenario_id` TODO (only reachable standalone; DeleteProject deletes use cases
  first).
- Orphaned annotations the sweep deletes are gone for good; if assistant findings should
  survive their target's deletion, `assistant_findings.applied_annotation_id` stays a soft
  reference to a missing row (as before).
- Consider a dev-profile `innodb_lock_wait_timeout` well under Playwright's 30 s so a
  blocked delete fails visibly instead of outliving the client.
- The dev database carries 467 leaked `e2e-*` projects and 160 dangling
  `annotation_annotatable` rows (targets deleted by the pre-#247 assistant race). They no
  longer get loaded, but a one-off cleanup (`DELETE FROM annotation_annotatable aa WHERE NOT
  EXISTS (...)` per annotatable type, then deleting the `e2e-%` projects through the API) would
  make the e2e database representative again. Separate ticket.
- `mvn verify` prints two `<Edit*Command> failed due to a failure to get a lock, retrying
  attempt: 1/2` lines per `edit…WithStaleVersionIsRejected` test. Pre-existing and expected:
  the handler retries `EntityLockException` (which a stale `expectedVersion` also raises) twice
  before rethrowing, and the tests assert the rethrow. Retrying a stale version is pointless;
  a follow-up should make version-validation failures non-retryable like
  `EntityValidationException`.
- The remaining 54 `CascadeType.REFRESH` declarations are downward (project → entities,
  entity → annotations, issue ↔ position). Still worth an audit: `refresh(project)` on a real
  project reloads everything under it.

## Verification protocol (developer, local MySQL + dev profile)
1. `mvn clean verify` — includes `DeleteProjectMySqlIT` (needs Docker; ~1 min extra).
2. Rebuild the jar (`mvn -pl modules/requel-app -am -DskipTests package` — *without*
   `-DskipAngularBuild`, or the jar has no UI and every spec fails) and restart the app with
   the log going to `tmp/requel-e2e.log`, then the two specs that failed deterministically:
   `npx playwright test e2e/goals.e2e.ts e2e/stories.e2e.ts --workers=1 --retries=0`.
   Expect no 409s, no `afterAll` timeouts, and `RetryOnLockFailures` silent in the log.
3. A green Playwright run is not enough on its own — `deleteProject` in the e2e helpers is
   best-effort. Always grep the app log afterwards:
   `grep -n "Command execution failed\|TransientObject\|Deadlock\|Lock wait" tmp/requel-e2e.log`
   must come back empty. If a `DeleteProject` failure is there, rerun the spec with
   `--logging.level.org.hibernate.engine.internal.Cascade=TRACE
   --logging.level.org.hibernate.event.internal=TRACE` to name the entity.
4. Then the full suite (`npx playwright test --workers=1 --retries=0`), the same log grep,
   then push and let CI run it.

## Branch / commit (developer runs; Claude never runs state-changing git)
    # already on 247-delete-project-cascade-hardening (cut from release/2.0)
    git add \
      doc/247-delete-project-cascade-hardening-plan.md \
      modules/annotation-domain/src/main/java/com/rreganjr/requel/annotation/command/AnnotationCommandFactory.java \
      modules/annotation-domain/src/main/java/com/rreganjr/requel/annotation/command/DeleteAnnotationGroupCommand.java \
      modules/annotation-domain/src/main/java/com/rreganjr/requel/annotation/command/RemoveAllAnnotationsFromAnnotatableCommand.java \
      modules/annotation-jpa/src/main/java/com/rreganjr/requel/annotation/AnnotationRepository.java \
      modules/annotation-jpa/src/main/java/com/rreganjr/requel/annotation/impl/AbstractAnnotation.java \
      modules/annotation-jpa/src/main/java/com/rreganjr/requel/annotation/impl/JpaAnnotationRepository.java \
      modules/annotation-jpa/src/main/java/com/rreganjr/requel/annotation/impl/command/AnnotationCommandFactoryImpl.java \
      modules/annotation-jpa/src/main/java/com/rreganjr/requel/annotation/impl/command/DeleteAnnotationGroupCommandImpl.java \
      modules/annotation-jpa/src/main/java/com/rreganjr/requel/annotation/impl/command/RemoveAllAnnotationsFromAnnotatableCommandImpl.java \
      modules/annotation-jpa/src/main/java/com/rreganjr/requel/annotation/impl/command/RemoveAnnotationFromAnnotatableCommandImpl.java \
      modules/assistant-core/src/main/java/com/rreganjr/requel/assistant/core/AssistantDispatcherImpl.java \
      modules/assistant-core/src/main/java/com/rreganjr/requel/assistant/core/AssistantRunWorker.java \
      modules/assistant-core/src/test/java/com/rreganjr/requel/assistant/core/AssistantRunWorkerTest.java \
      modules/platform-core/pom.xml \
      modules/platform-core/src/main/java/com/rreganjr/command/RetryOnLockFailuresCommandHandler.java \
      modules/platform-core/src/test/java/com/rreganjr/command/RetryOnLockFailuresCommandHandlerTest.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/ProjectUserRole.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/AbstractProjectOrDomainEntity.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/AbstractProjectCommand.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/DeleteActorCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/DeleteGlossaryTermCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/DeleteGoalCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/DeleteGoalRelationCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/DeleteProjectCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/DeleteReportGeneratorCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/DeleteScenarioCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/DeleteScenarioStepCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/DeleteStakeholderCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/DeleteStoryCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/DeleteUseCaseCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/RemoveActorFromActorContainerCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/RemoveGoalFromGoalContainerCommandImpl.java \
      modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/RemoveStoryFromStoryContainerCommandImpl.java \
      modules/requel-app/pom.xml \
      modules/requel-app/src/main/resources/db/migration/V16__index_annotation_annotatable_by_annotatable.sql \
      modules/requel-app/src/test/java/com/rreganjr/requel/ai/AiReviewDispatchIT.java \
      modules/requel-app/src/test/java/com/rreganjr/requel/assistant/LexicalSpellingDispatchTest.java \
      modules/requel-app/src/test/java/com/rreganjr/requel/assistant/NlpDisabledDispatchTest.java \
      modules/requel-app/src/test/java/com/rreganjr/requel/project/DeleteProjectIT.java \
      modules/requel-app/src/test/java/com/rreganjr/requel/project/DeleteProjectMySqlIT.java \
      modules/user-jpa/src/main/java/com/rreganjr/requel/user/impl/UserImpl.java
    git commit -F commit.md
    git push -u origin 247-delete-project-cascade-hardening
    # NEVER `git add -A` — the repo has ~80 CRLF-debt files that always show modified

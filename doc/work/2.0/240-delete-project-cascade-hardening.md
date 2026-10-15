# DeleteProject cascade hardening (follow-up to #240)

`DeleteProject` (#240) shipped able to delete a *simple* project, but the full-stack
e2e suite (once #241 made the `deleteProject` e2e helper a real call instead of a
no-op) shows it **cannot reliably delete a project that contains a realistic entity
graph** against **MySQL**. `mvn verify` never caught this: tests run on **H2 with
Flyway disabled and Hibernate `create-drop`**, so neither the MySQL foreign keys nor
the Hibernate `@ManyToAny` session behaviour below reproduce there. Only the e2e
suite on MySQL exercises it.

This is a backend command-hardening effort spanning `project-jpa`, `annotation-jpa`,
and `platform-core` — deliberately split out of #241 (the UI ticket), whose delete
UI is complete and verified on simple projects.

## Symptoms (from CI + local e2e, clean MySQL, dev profile)

Failing specs, all in `afterAll`/cleanup calling `deleteProject`:
`scenario-steps-143`, `scenarios`, `use-cases`, `actors`, and `goals` (goals fails in
the test body on `DeleteGoal`, see #3).

### 1. Orphan scenarios block `delete(project)` — HTTP 409
```
Cannot delete or update a parent row: a foreign key constraint fails
(`requel`.`scenarios`, CONSTRAINT `FKoplmulkyjqc4foqfjtcwjxm2x`
 FOREIGN KEY (`projectordomain_id`) REFERENCES `pods` (`id`))
```
Cause: a use-case's **primary** scenario (`UseCase.getScenario()`) and **additional**
scenarios (`getAdditionalScenarios()`, the `usecase_scenarios` join) are NOT in
`project.getScenarios()`, and `DeleteUseCaseCommandImpl` only **detaches** them
(literal `// TODO: delete the main scenario?`). `DeleteProjectCommandImpl` deletes
only `project.getScenarios()`, so those use-case scenarios survive as orphans that
still reference the project via `scenarios.projectordomain_id -> pods`, and the final
`delete(project)` is refused.

An attempted fix (collect use-case scenarios; delete use-cases before scenarios)
resolved the 409 but exposed / triggered symptom #2 — see the caution below.

### 2. Detached `IssueImpl` poisons the session — HTTP 500 (the systemic one)
```
InvalidDataAccessApiUsageException: detached entity passed to persist:
com.rreganjr.requel.annotation.impl.IssueImpl
```
Observed via **two different triggers**, same exception:
- `DeleteUseCaseCommandImpl:110` -> `RemoveAnnotationFromAnnotatableCommandImpl`
  -> `JpaAnnotationRepository.removeAnnotatableFromAnnotationJoinTable`
- `DeleteUseCaseCommandImpl:129` -> `RemoveActorFromActorContainerCommandImpl`
  -> `JpaProjectRepository.removeActorContainerFromActorJoinTable`

Both go through `platform-core` `DomainObjectWrappingAdvice.wrapReturnedObject`
(which triggers a flush) and blow up on the SAME detached `IssueImpl`.

Root-cause hypothesis: `RemoveAnnotationFromAnnotatableCommandImpl` has a documented
Hibernate 6.5 `@ManyToAny` workaround — it deletes the join row via native SQL then
`em.detach(annotation)` + reload. Inside the large multi-entity `DeleteProject`
transaction that leaves a **detached `IssueImpl` still referenced by a managed
collection with cascade PERSIST**, so the *next* flush (any join-table native query
in a later sub-delete) tries to cascade-persist the detached Issue and fails. The
trigger is whatever flushes next; the bug is the lingering detached annotation.

### 3. `DeleteGoal` cannot delete an annotated goal — HTTP 409
```
Cannot delete or update a parent row: a foreign key constraint fails
(`requel`.`goals_annotations`, CONSTRAINT `FK6h4g9443aedwj7wcgw6pjc9q1`
 FOREIGN KEY (`goal_impl_id`) REFERENCES `goals` (`id`))
```
Same annotation-removal family: deleting a goal that has annotations doesn't clear
`goals_annotations` first. Also reachable from the goal-delete UI, not just cascade.

**Timing/flakiness note:** even a goal created with no annotations hits this if a
background assistant (`AnalysisInvokingCommandHandler` fires NLP/AI after writes)
annotates it before the delete. So `goals.e2e.ts:92` fails locally under the dev
profile (assistants active) but passed in the last clean CI run — it is flaky, not
deterministic. `DeleteGoalCommand` must remove the goal's annotations before deleting.

## Suggested approach (for whoever picks this up, with a debugger + local MySQL)

1. **Session management is the crux (symptom #2).** Options to evaluate:
   - `flush()` + `clear()` (or evict the detached annotation and everything holding
     it) between sub-deletes in `DeleteProjectCommandImpl` / after each annotation
     removal, so no detached `@ManyToAny` entity survives to the next flush;
   - or fix `RemoveAnnotationFromAnnotatableCommandImpl` so the detached annotation
     is removed from every managed collection that still references it (hard with
     shared `@ManyToAny`), or reloaded/merged instead of left detached;
   - or revisit why `DomainObjectWrappingAdvice.wrapReturnedObject` forces a flush on
     a repo method whose only job is a native join-table delete.
2. **Symptom #1**: delete use-case primary + additional scenarios as part of the
   cascade. Deleting use-cases *before* their scenarios avoids the `use_cases`
   primary FK, but interacts with #2 — do it only once the session issue is solved.
3. **Symptom #3**: `DeleteGoalCommand` must remove the goal's annotations (same
   pattern as #2) before deleting the goal.
4. **Regression coverage**: add a `DeleteProjectIT` that builds a rich project
   (use-case with primary + additional scenarios and steps, actors, an annotated
   goal, an annotated use-case) and deletes it. NOTE: H2 will not reproduce the MySQL
   FKs or necessarily the `@ManyToAny` flush behaviour, so a MySQL-backed IT (or the
   e2e) is what actually guards this. Consider a Testcontainers MySQL IT.

## Interim state (#241 / PR #246)
The `deleteProject` e2e helper is **best-effort** (swallows + warns) so teardown
failures don't fail unrelated specs; the delete UI (#241) is verified on simple
projects. The V15 audit-FK migration (already merged into #246) fixed the
`command_audit_log` orphan. This ticket tracks the remaining cascade/session bugs.

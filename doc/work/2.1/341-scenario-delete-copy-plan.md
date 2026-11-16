# #341 — DeleteScenario on a primary scenario, orphan steps, CopyScenario flattening

https://github.com/rreganjr/Requel/issues/341

Branch `341-scenario-delete-copy` from `release/2.1`, PR to `release/2.1`. Size 3.

## Summary

Four defects in the scenario commands, all still present on `release/2.1`:

1. `DeleteScenario` of a use case's primary scenario walks `getUsingUseCases()` with an empty
   `// TODO` body and then deletes, so it fails on `usecases.scenario_id`.
2. `DeleteScenario` leaves the scenario's own plain steps in `scenarios`, attached to nothing,
   holding their names in the shared step/scenario unique key (the #325 orphan, from a second
   path).
3. `CopyScenario` copies a nested scenario through `CopyScenarioStepCommand`, which always builds a
   plain `StepImpl`, so the copy loses the nested scenario's steps.
4. `CopyScenario`'s free-name search is scenario-only (`findScenarioByProjectOrDomainAndName`) while
   the unique key spans steps and scenarios, so a plain step named "X 1" makes the copy fail.

## Locked decisions (2026-10-29)

1. **Refuse** deleting a primary scenario with an `EntityValidationException` on `scenarioId`
   (INVALID_INPUT, 422) naming the use case and pointing to `SetPrimaryScenarioOnUseCase`. The check
   is a query (`findUseCasesWithPrimaryScenario`), not the lazy `getUsingUseCases()`, so a use case
   deleted earlier in the same transaction (DeleteUseCase, DeleteProject) is not counted.
2. **Own steps are deleted** with the scenario unless another scenario uses them, reusing #325's
   rule (`isStepUsedByAnotherScenario` + `DeleteScenarioStepCommand` as a cascaded step). Nested
   scenarios are only unlinked.
3. **A nested scenario is linked, not copied**, by `CopyScenario` (as `EditScenario` links one sent
   with `isScenario: true`). Plain steps are still copied under numbered names.
4. **The copy's name search** uses `findStepByProjectOrDomainAndName`, which covers both.
5. **Folded in:** `DeleteActor`'s "is the primary actor for ..." refusal throws an
   `EntityValidationException` on `actorId` instead of a `RuntimeException` (which callers saw as
   `EXECUTION_ERROR`).
6. **`V39__delete_orphan_steps_again.sql`** reruns V19's orphan-step cleanup for steps
   `DeleteScenario` left behind since V19. Same statements, its own scratch table; V19 is applied
   and unchanged.

## Changes

- `ProjectRepository` / `JpaProjectRepository`: `findUseCasesWithPrimaryScenario(Scenario)`.
- `DeleteScenarioCommandImpl`: the refusal; own plain steps deleted; the empty TODO loop removed.
- `CopyScenarioCommandImpl`: link nested scenarios; name search through `findStepBy...`.
- `DeleteActorCommandImpl`: validation error.
- `DeleteScenarioInput`, `CopyScenarioInput` descriptions.
- `V39__delete_orphan_steps_again.sql`.

## Test plan

`tmp/341-verify.sh`: `mvn clean verify -pl modules/coverage-report -am` (backend; the MySQL IT
needs Docker, as `DeleteProjectMySqlIT` does).

- `ScenarioDeleteCopyIT` (new, gateway): primary scenario refused naming the use case and still
  there; own plain step deleted, shared step kept; copy links the nested scenario (same id, its step
  intact) and copies the plain step as "name 1"; a plain step named "X 1" makes the copy "X 2";
  DeleteActor on a primary actor is INVALID_INPUT.
- `ScenarioDeleteCopyMySqlIT` (new): the same tests on MySQL 8.4 via Testcontainers, schema from
  Flyway (so V39 runs too).
- `DeleteProjectIT` / `DeleteProjectMySqlIT` keep passing: DeleteUseCase and DeleteProject delete
  scenarios after their use cases, which the query-based check does not count.

## Out of scope

- A deep-copy option for nested scenarios.
- Choosing the copy's name (CopyScenarioInput has no name field).

## AC mapping

| Issue AC / item | Covered by |
|---|---|
| Primary-scenario delete refused with a message naming the use case | decision 1; both ITs |
| Own plain steps deleted unless shared | decision 2; both ITs |
| Nested scenario copied as a scenario (link decided) | decision 3; both ITs |
| Free-name search covers steps | decision 4; both ITs |
| Descriptions updated | `DeleteScenarioInput`, `CopyScenarioInput` |
| FK and orphan cases on MySQL | `ScenarioDeleteCopyMySqlIT` |

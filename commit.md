Add scenarios to use case — phase 7a

Adds the ability to associate scenarios with a use case: one primary (auto-created on save)
and zero or more additional scenarios picked from a popup selector.

## Backend

**New join table**
- `V5__usecase_scenarios.sql` — Flyway migration creating the `usecase_scenarios` many-to-many join table

**Domain**
- `UseCase.java` — added `getAdditionalScenarios()` to the interface
- `UseCaseImpl.java` — added `additionalScenarios` as a `@ManyToMany` collection backed by the join table
- `ProjectCommandFactory.java` — added factory methods for the two new commands
- `AddScenarioToUseCaseCommand.java` — new command interface: links an existing scenario to a use case
- `RemoveScenarioFromUseCaseCommand.java` — new command interface: unlinks a scenario from a use case

**Command implementations**
- `AddScenarioToUseCaseCommandImpl.java` — fetches use case and scenario by ID, adds to `additionalScenarios`, merges
- `RemoveScenarioFromUseCaseCommandImpl.java` — removes scenario from `additionalScenarios`, merges
- `ProjectCommandFactoryImpl.java` — wired up the two new command implementations
- `EditUseCaseCommandImpl.java` — fixed: after executing `EditScenarioCommand`, now links the new scenario back to the use case when `scenario` was previously null (fixes existing use cases imported with `scenario_id = NULL`)

**API**
- `AddScenarioToUseCaseInput.java` — new DTO: `projectName`, `useCaseId`, `scenarioId`
- `RemoveScenarioFromUseCaseInput.java` — new DTO: `projectName`, `useCaseId`, `scenarioId`
- `UseCaseDto.java` — added `additionalScenarios: List<ScenarioDto>`
- `ProjectCommandRegistrar.java` — registered `AddScenarioToUseCase` and `RemoveScenarioFromUseCase` commands; fixed `EditUseCase` registration to set an empty `stepCommands` list (prevented NPE in `EditScenarioCommandImpl`)
- `ProjectQueryController.java` — `toUseCaseDetailDto` now populates `additionalScenarios`; fixed `toUseCaseSummaryDto` constructor arity

## Angular

**Models**
- `entity-reference.ts` — added optional `typeName` field to `EntityReferenceDto`
- `use-case.ts` — added `additionalScenarios: ScenarioDto[] | null` to `UseCaseDto`

**Shared**
- `entity-selector-dialog.ts` — added sortable Type column (shown only when entities have types); added `excludeTypes` input to filter entire type classes from the list (used to block adding a second Primary); added type filter dropdown in the search bar for narrowing by type; Scenario case populates `typeName` from `scenarioType`

**Use-case editor**
- `use-case-editor.ts` — added Scenarios section with Name/Type table and X button (Primary row is not removable); `+ Add Scenario` opens the entity selector; `allScenarios` computed merges primary scenario row with `additionalScenarios`; `excludeScenarioTypes` computed passes `['Primary']` to the selector once a primary exists

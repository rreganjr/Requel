# #468 — A step's actor as a role, rendered per use case

https://github.com/rreganjr/Requel/issues/468

Branch `468-step-actor-role` from `release/2.2`, PR to `release/2.2`. Sized 8 in the release plan;
with scenario names in scope (decision 3) it is nearer 13.

## Summary

Today a step's actor is part of its wording ("Operator ends the room"). That wording is wrong in a
use case whose primary actor is someone else, and a scenario in no use case has no actor to name at
all. This ticket gives a step, and a scenario name, an **actor role**: the use case's primary actor,
the system, or a named actor. The rest of the name is the action ("ends the room"). The name is
rendered with the actor for each use case it is shown in, and the step remains one shared step.

## Locked decisions (2026-11-24)

1. **Existing steps keep their wording, with no role.** No upgrade job parses them. The assistant
   adds a LOW finding with a one-click position that sets the role and trims the subject
   ("Operator ends the room" becomes primary actor + "ends the room") — **only where the wording
   is wrong**: the named actor is the primary actor of some of the step's use cases but not of
   others. A step whose wording is right everywhere, or "The system ...", gets nothing, so a
   well-written step carries no open issue and no warning icon. (Refined during implementation:
   raising it on every step put an open issue on almost every step row.)
2. **The action name alone stays unique per project.** The `(projectordomain_id, name)` key is
   unchanged, so "ends the room" exists once whatever its role: one action, one shared step. A
   role position whose trimmed name is already taken is refused, and the refusal names the step
   that holds the name.
3. **Scenario names get a role too.** The role lives on `StepImpl`, the shared table, so it covers
   scenarios. A nested scenario used as a step renders like any step. A command-form scenario name
   ("End a room") still raises nothing.
4. **A named actor that isn't in a use case** keeps today's `actor-not-in-usecase` issue, now read
   from the role rather than the parsed subject. A one-click "Add <actor> to <use case>" position
   sits beside Do nothing.
5. **Deleting an actor that a step names by role** turns that step back into plain wording
   ("Operator ends the room"). If the plain wording is already another step's name, the delete is
   refused and the refusal names that step.
6. **New steps default to no role.** The assistant offers the role.
7. **Rendering has one rule:**
   - When the step reaches exactly one primary actor, its name shows that actor.
   - When the step reaches use cases with different primary actors, or no use case, its name
     shows the role form ("Primary actor ends the room").
   - A use case's own view always uses that use case's primary actor.
   - A use case with no primary actor shows the role form.

## Contracts

- **Domain** (`project-domain`):
  - New enum `ActorRole { PRIMARY_ACTOR, SYSTEM, NAMED }`.
  - `Step` gains `getActorRole()`/`setActorRole()` and `getActor()`/`setActor()`; the actor is
    set only for NAMED. A null role means the wording names the actor, which is today's behaviour.
  - New pure helper `StepNames`:
    - `render(Step, Actor primary)`;
    - `renderFor(Step, Collection<UseCase>)`;
    - the role labels "Primary actor" and "The system".
  - The `useCasesOf`/`useCasesOfScenario` walk moves here from `StepStructureAssistant` so the
    renderer and both assistants share it (`useCasesContaining` keeps the assistant's old
    from-the-using-scenarios walk).
- **Persistence**:
  - Migration `V45__step_actor_role.sql`:
    - `scenarios.actor_role varchar(32) NULL`;
    - `scenarios.actor_id bigint NULL` with a foreign key to `actors`;
    - the columns the new positions need, by name rather than id so a position outlives what it
      names (`proposed_actor_role`, `proposed_actor_name`, `proposed_use_case_name`).
  - Before choosing V45, check that `master` has no V45 (RELEASE_PROCESS, Flyway numbering).
  - No cascade from step to actor. The actor delete handles its steps (decision 5).
- **API** (`service-api`):
  - Read side: `StepDto`, `StepContentDto`, `StepDetailDto`, `StepSummaryDto`, `ScenarioDto` and
    `ScenarioContentDto` gain `actorRole`, `actorId`, `actorName` and `displayName` (rendered by
    rule 7). `name` stays the stored text. `StepDetailDto` and the scenario detail read also get
    `readsIn` (one `UseCaseReadingDto` per use case: its name, primary actor and how the name
    reads there), and the scenario detail read `primaryActorName` (the one its use cases share),
    so the editors can show how a role would read before it is saved. `UseCaseDto.scenarioName`
    reads with the use case's primary actor.
  - Write side: `EditScenarioStepInput`, `EditStepInput` and `EditScenarioInput` gain
    `actorRole` (`PRIMARY_ACTOR`, `SYSTEM`, `NAMED`, or `NONE` to clear) and `actorName` for
    NAMED. Partial-update rules apply: a null role leaves the role unchanged.
  - The `@CommandDescription`/`QueryDescriptions` text changes with the inputs, so
    `McpToolCatalogLockstepIT` stays green.
- **XML**:
  - `step` gains `actorRole` (attribute) and `actorRef` (IDREF to an actor). `project.xsd` and the
    JAXB mapping change to match; a patcher resolves the reference after unmarshal, with no
    repository access from the entity.
  - XML without either attribute imports with no role, so old exports still load.
- **Positions**:
  - `SET_STEP_ACTOR` creates a `SetStepActorPosition` (`project-jpa`, modelled on
    `MergeStepsPosition`). It holds the trimmed action name (`proposed_word`), the expected name
    (`expected_name`), the role and the actor.
  - `ADD_ACTOR_TO_USE_CASE` creates an `AddActorToUseCasePosition`, which runs
    `AddActorToActorContainerCommand`.
  - Both are wired in `CommandBackedAssistantResultApplicator`, have resolve commands, and get XSD
    types.

## Steps

1. **Domain and persistence**:
   - add `ActorRole`, the `Step` accessors, `StepImpl` columns and `StepNames`;
   - move the use-case walk;
   - add V45.
2. **Commands**:
   - `EditScenarioStep` and `EditScenario` steps set and clear the role, checking that a NAMED
     actor exists in the project;
   - `CopyScenarioStep`, `CopyScenario`, `ConvertStepToScenario` and the merge-steps resolve carry
     the role (a merge keeps the first step's role);
   - `DeleteActor` applies decision 5.
3. **API**: add the DTO fields and `displayName`, the input fields, the mappers and the MCP
   descriptions.
4. **XML**: add the JAXB mapping, the patcher and `project.xsd`; check every bundled sample still
   validates.
5. **`StepStructureAssistant`**:
   - With a role set, skip parsing the subject. With NAMED, run the membership check (decision 4,
     adding the new position).
   - With no role and no subject, raise no-actor. Its positions become `SET_STEP_ACTOR`:
     - primary actor;
     - the system;
     - each auxiliary actor of the step's use cases.
     These are offered in all three cases. The shared-primaries and no-use-case cases gain the
     primary-actor option, which #459 could not offer. The messages change to match.
   - With no role and a subject that is a known actor or "the system", raise the decision-1
     advisory:
     - the primary role when the subject is the primary actor of every reached use case;
     - otherwise NAMED (or SYSTEM).
6. **`StepSequenceAssistant`** reads the action name and the role rather than parsing a subject
   from the rendered name. The `step-sequence` and `usecase-scenarios` context providers give the
   AI assistants rendered names.
7. **Reports**: the built-in report XSLT renders steps per use case from the new attributes. The
   built-ins render the bundled resource directly (#275), so no upgrade step is needed.
8. **Angular**:
   - In the step editor and the scenario editor name field, a "Who does it" select (In the wording
     / Primary actor / The system / each project actor) beside the action name, with a "Reads as"
     line for each use case.
   - Scenario editor step rows and the add/edit row get the same select.
   - The use case editor renders its scenario's steps with its primary actor.
   - Lists, selectors, breadcrumbs and relationship sections show `displayName`.

## Test plan

- **Unit**: `StepNames` covers each role × one, several and no use cases, and a use case with no
  primary actor.
- **ITs**:
  - the AC1 step renders per use case and stays one row;
  - role set and cleared through `EditScenarioStep` and `EditScenario`;
  - a NAMED actor not in the project is refused;
  - decision-2 clash refusal;
  - decision-5 actor delete, plain and clashing;
  - copy, convert and merge carry the role;
  - the DTOs carry the role fields and `displayName`.
- **XML**:
  - `ProjectXmlStreamingRoundTripIT` and `ProjectXmlRoundTripIT` round-trip each role (AC3);
  - an old export with no role imports.
- **`StepStructureAssistantIT`**:
  - a role step in a no-use-case scenario raises no no-actor (AC2);
  - each no-actor case offers `SET_STEP_ACTOR` positions, and choosing one sets the role;
  - the advisory on "Operator ends the room", and its clash refusal;
  - NAMED actor not in a use case, with Add-to-use-case.
- **MySQL**: the migration runs on Testcontainers MySQL (existing MySQL ITs); actor delete on MySQL
  (`DeleteCascadeIT` pattern).
- **Angular specs**: the role select, the "Reads as" lines, `displayName` in lists, and axe.
- **e2e** (CI): update `scenario-steps-143`, `step-page-476`, `step-reuse-483`, `scenarios` and
  `use-cases` where they read step names. Add one flow: set a step's role, open two use cases and
  see two renderings.
- **Ticket report**: `Project2TicketXsltTest` renders each role from the fixture.
- **Gate**: `mvn clean verify` and the whole unit suite, typecheck and dev build.
- User guide: requirements.html (Who does it) and issues.html (the new check).

## Out of scope

- Merging two existing steps that trim to the same action. The clash refusal names the other step;
  merging them stays a manual job (or #472's merge position).
- Gherkin export (#496) and the steps list role column (#505), which build on this.

## Risks

- Rendering touches every place a step or scenario name is shown. Missing one shows the bare action
  ("ends the room"), which reads oddly but loses nothing.
- V45 must not collide with a hotfix migration on `master`.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| One "ends the room" step with the primary-actor role reads "Operator …" and "Conduit Engineer …" in the two use cases and stays one step | Rule 7, `StepNames`, use case editor; IT and e2e |
| A scenario in no use case shows the role; no no-actor issue on its steps | Rule 7; step 5; `StepStructureAssistantIT` |
| The role round-trips through export and import | Step 4; XML round-trip ITs |

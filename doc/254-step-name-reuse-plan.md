# 254 — step-name collisions name the wrong thing (plan)

Issue: https://github.com/rreganjr/Requel/issues/254

## What is actually happening

Steps have no pre-flight name guard. Every other `Edit*CommandImpl` — goal, story, actor, use
case, glossary term, scenario, stakeholder, project, report generator — looks the name up first and
throws `EntityException.uniquenessConflict(Goal.class, existing, FIELD_NAME, …)`, naming the type
and the existing entity. The step path has none, so the write reaches the database, the unique
constraint fires, and `JpaProjectRepository` hands it to a `ConstraintViolationExceptionAdapter`
registered against `ProjectOrDomainEntity.class`. That registration key is where the useless
"ProjectOrDomainEntity" in the message comes from; the adapter never saw the colliding row.

The collision is also wider than the ticket implies. `StepImpl` and `ScenarioImpl` share one table:

    @Table(name = "scenarios", uniqueConstraints = {@UniqueConstraint(
        columnNames = {"projectordomain_id", "name"})})
    @Inheritance(SINGLE_TABLE) @DiscriminatorColumn(name = "type")

So a step name collides with a **scenario** name too, not only with another step's.

## Decision: refuse both, and say exactly what to send instead

Implicit reuse-by-name is not the fix. Steps are shared (`StepImpl.getUsingScenarios()` is
`@ManyToMany(mappedBy = "steps")`) and scenarios nest inside scenarios, so a bare name match is
genuinely ambiguous: the caller may mean "link the existing one" or "I did not realise this name was
taken". Guessing rewrites shared content — the step's text is shared by every scenario using it, so
applying a caller's approximate prose to a name-matched step edits it everywhere at once.

So a step entry with no `stepId` whose name is already taken is refused, with an error that names
the type, the name and the existing id, and tells the caller which id to send:

- name matches an existing **step** → "pass its stepId to link the existing step"
- name matches an existing **scenario** → "pass its id as stepId with isScenario true to nest the
  existing scenario"

That satisfies AC 2 through its second branch ("or the error explicitly tells the caller to pass its
`stepId`"), and it keeps AC 1's error informative for the case that remains.

**Both link paths already work**, so the advice is actionable rather than aspirational. The
`EditScenario` applicator has four branches, and two of them are exactly what the message asks for:
`isScenario` with a `stepId` resolves through `findScenarioById` and binds with `setStep`, and a
plain entry with a `stepId` resolves through `findStepByIdAcrossScenarios` and binds the same way.
Nothing new is needed for linking — only for telling the caller that linking is what they want.

## Changes

**`modules/project-jpa/.../command/EditScenarioStepCommandImpl.java`** — pre-flight guard on create
and on rename, matching the shape of `EditGoalCommandImpl`. `ProjectRepository` already has both
finders it needs: `findStepByProjectOrDomainAndName` and `findScenarioByProjectOrDomainAndName`.
The guard must not fire when the name belongs to the step being edited.

**Error text** — carries the entity type, the colliding name, the existing id, and the remedy.
Built with `EntityException.uniquenessConflict(Step.class, existing, FIELD_NAME, …)` where the
existing entity is a step, so the type in the message is real rather than a registration key.

**`modules/service-api/.../dto/EditScenarioInput.java` / `EditStepInput.java`** — `@CommandDescription`
on `EditScenarioInput` (the #255 mechanism) stating that step names are unique across steps and
scenarios in a project, that a step is linked by `stepId` and a nested scenario by its id with
`isScenario`, and that the steps array is replaced wholesale on save. This is the discoverability
half of the ticket: an ingest client reads the tool description, not the source.

## Tests

- `ScenarioStepCommandTest` — creating a step whose name matches an existing step is refused, and
  the message names `Step`, the name, and the existing id. Same for a name matching a scenario.
- `CommandGatewayIT` — the same two refusals through `EditScenario`'s steps array, which is the
  surface the ticket is actually about, asserting the message is specific rather than
  "ProjectOrDomainEntity".
- **AC 3, and the one with real risk**: a step linked by `stepId` into a second scenario appears in
  both scenarios' `getSteps()` and in the step's `getUsingScenarios()`. `ScenarioImpl.getSteps()` is
  a `@ManyToMany` and `EditScenarioCommandImpl` clears and re-adds it per scenario, so sharing
  should hold structurally — but `getUsingScenarios()` is the lazy inverse side, and
  `ScenarioStepCommandTest.convertStepToScenario` already carries a comment that this side "depends
  on the bidirectional mapping being initialized". Expect to need a flush and reload to observe both.
  If the inverse genuinely does not populate, that is a finding to report rather than something to
  paper over in the test.

## Out of scope

- Implicit reuse by name. Decided against above.
- Teaching `ConstraintViolationExceptionAdapter` to recover an entity id from a SQL constraint name.
  Constraint names differ between H2 and MySQL; the pre-flight guard is the house pattern and covers
  the path this ticket is about.
- The other entity types' collision messages. They already name their type and id.

## Gate

`mvn clean verify`. No frontend change.

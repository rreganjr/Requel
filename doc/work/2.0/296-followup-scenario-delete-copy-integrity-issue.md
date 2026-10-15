Found while writing the scenario and use-case descriptions for #296.

### What happens

1. **Deleting a use case's primary scenario fails on a foreign key.** `DeleteScenarioCommandImpl.execute` walks `scenario.getUsingUseCases()` with an empty `// TODO` body, then deletes the scenario. `usecases.scenario_id` references it, so MySQL refuses the delete with a constraint error rather than a clean message.
2. **Deleting a scenario leaves its own plain steps behind.** Nothing deletes them and there is no cascade, so the rows stay, holding their names in the step/scenario namespace. This is the same orphan #325 fixed for `EditScenario` (`V19__delete_orphan_steps.sql`).
3. **`CopyScenario` flattens nested scenarios.** `CopyScenarioStepCommandImpl` always builds a `StepImpl`, so a nested sub-scenario is copied as one plain step and its own steps are lost.
4. **`CopyScenario`'s free-name search checks scenarios only**, while the unique key spans steps and scenarios. A plain step already named `"<name> 1"` makes the copy fail on the constraint.

### Work

- `DeleteScenario` of a use case's primary scenario is refused with a validation error naming the use case, telling the caller to use `SetPrimaryScenarioOnUseCase` first. (Alternatively give the use case a new empty primary; refusing is simpler and matches `DeleteActor`.)
- `DeleteScenario` deletes its plain steps that no other scenario uses, as `EditScenario` does for dropped steps.
- `CopyScenario` copies a nested scenario as a scenario (deep, or linked; decide), not a plain step.
- The copy's free-name search covers steps and scenarios.
- Update the `DeleteScenario` and `CopyScenario` descriptions.

### AC

- Each case above has an IT; the FK and orphan cases also run on MySQL (`@Testcontainers`, `DeleteProjectMySqlIT` pattern).

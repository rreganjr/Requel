# #455 — Check that a scenario's steps alternate between an actor and the system

https://github.com/rreganjr/Requel/issues/455

Branch `455-step-alternation` from `release/2.1`, PR to `release/2.1`. Size 3.

## Summary

#349 checks how one step reads: "<actor> does something" or "the system does something". This
adds a check on a whole scenario: an actor acts, the system responds. It reads the scenario's steps
in order and raises one advisory LOW issue on the scenario for each break in that pattern.

## Locked decisions (2026-11-04)

1. **What raises an issue:**
   - The scenario starts with a system step.
   - Two system steps in a row.
   - The same actor twice in a row: usually one step written as two ("Member fills in the form",
     "Member submits it").

   Different actors in a row raise nothing. It's a hand-off outside the system ("Member hands the
   tool to the clerk", "Clerk inspects it").
2. **How a step counts.** Each step is read with #349's rules:
   - "the system ..." is a system step.
   - "<actor> ..." counts as that actor. The actor is a known actor matched by `namesActor`, or
     otherwise the subject as written without its article.
   - A command ("Download the MP4") counts as the primary actor of the scenario's use cases, when
     they have exactly one. Otherwise the step is unknown.
   - A sub-scenario used as a step counts as the actor that starts it: its name is read the same
     way, so a command counts as the primary actor.
   - An unreadable step, or one naming the project as its actor, is unknown.
3. **An unknown step breaks the sequence.** No pair is checked across it, and a scenario that
   starts with one isn't checked for a system start.
4. **One issue per scenario.** It lists every break, so a re-run updates it in place. When the
   breaks are fixed, the issue is removed if nobody argued on it. The only position is
   "Do nothing."; merging steps isn't offered.
5. **When it runs:**
   - On a scenario edit, which covers adding, removing and reordering steps.
   - On whole-project analysis.
   - When a step is edited or deleted, for each scenario that uses it. A step's text changes how
     it counts in every scenario that uses it, and without this re-run the scenario's issue
     would be wrong until the scenario itself was edited.
   - When a scenario is edited or deleted, for the scenarios that use it as a step (its name is
     how it counts there) and, on an edit, the other scenarios that share its steps.

## Design

### A separate assistant

`StepSequenceAssistant` (`legacy-step-sequence`) is a new assistant in assistant-legacy-nlp. It
targets `Scenario`, so a plain step never matches it. It is a separate assistant rather than more
code in `StepStructureAssistant` because:

- **Clean-up is per assistant and target.** A sequence-only re-run of a scenario would otherwise
  supersede the step-structure findings on the scenario's own name.
- **It can clean up after itself.** It uses the `AUTO_RESOLVE_IF_UNTOUCHED` clean-up policy, so a
  fixed sequence removes the issue. `StepStructureAssistant` keeps the default, which leaves
  superseded issues in place.
- **Projects can switch it off.** It is the most opinionated check on the list. It is
  `projectSwitchable`, with the display name "Actor and system turns".

### Sharing #349's reading of a step

`StepReading` (new, package-private) holds #349's reading of one step: `UNREADABLE`, `COMMAND`,
`SYSTEM`, `PROJECT`, or `ACTOR` plus a subject.

- `StepStructureAssistant` is moved onto it with no change in behaviour. The existing tests are
  the guard.
- `useCasesOf` gains a variant that starts from a scenario itself. The current walk starts from
  the scenarios that *use* a step, so for a scenario it would skip the use cases the scenario is
  directly in.

### Messages

One issue per scenario, key `legacy-step-sequence:Scenario:<id>:sequence`, `findingType`
`step-sequence`, LOW, `mustResolve` false. Its text is `The steps of "{0}" don't alternate between
an actor and the system:` followed by each break, joined with "; ":

- `it starts with the system step "{1}"`
- `"{1}" and "{2}" are both system steps with no actor step between them`
- `"{1}" and "{2}" are both by {3} with nothing from the system between them; combine them unless
  something outside the system happens in between`

### Re-checking the scenarios of an edited step

- **New task type.** A run with task type `STEP_SEQUENCE` is served only by
  `StepSequenceAssistant`. It also serves the ordinary post-edit run (null task), so scenario edits
  and whole-project analysis need nothing new.
- **`AnalysisRequestSource`** (project-domain) gets
  `default List<ProjectOrDomainEntity> getStepSequenceTargets()`, returning an empty list.
- **The commands collect the scenarios**, inside `execute` while the session is open:
  - `EditScenarioStepCommandImpl`: the scenarios that use the step. Not when analysis is off,
    which is how `EditScenario` runs its own step commands.
  - `EditScenarioCommandImpl`: the scenarios that use this one as a step, and the other scenarios
    that share its plain steps. The scenario itself is the analysis target, so it isn't listed.
  - `DeleteScenarioStepCommandImpl` and `DeleteScenarioCommandImpl`: the scenarios that used the
    step or scenario, collected before the delete. They implement the interface with a null
    analysis target. Not when the delete is a step of a bigger one (a scenario, a use case, a
    project, seen as a cascade's authorizing command): those scenarios are being deleted or are
    analyzed by the command that removed the step.
- **The handler dispatches them.** `AnalysisInvokingCommandHandler` dispatches each scenario with
  `STEP_SEQUENCE`, after the command's own target and independently of it. Failures are logged
  and never roll back the command, the same as now.
- **The other assistants don't run.** The run worker already routes by `handlesTask`, so the
  lexical and AI assistants skip a `STEP_SEQUENCE` run.

## Changes

assistant-legacy-nlp
- `StepReading` (new): the reading above, and a `classify(...)` taking the NLP factory.
- `StepStructureAssistant`: uses `StepReading`; `useCasesOf` gains the scenario-rooted variant.
  No behaviour change.
- `StepSequenceAssistant` (new):
  - targets `Scenario`;
  - `handlesTask(null or STEP_SEQUENCE)`;
  - `AUTO_RESOLVE_IF_UNTOUCHED`, `projectSwitchable`;
  - checks the scenario's direct steps in order;
  - on a failure, logs and skips the issue, as `StepStructureAssistant` does.

assistant-api
- `StepSequenceAssistant.TASK_TYPE = "STEP_SEQUENCE"` lives in the assistant. requel-app passes the
  string through `AnalysisRequestDispatcher.dispatch(target, user, taskType)`, which exists. No API
  change.

project-domain / project-jpa
- `AnalysisRequestSource.getStepSequenceTargets()` (default empty).
- `EditScenarioStepCommandImpl`, `EditScenarioCommandImpl`, `DeleteScenarioStepCommandImpl`,
  `DeleteScenarioCommandImpl`: collect the scenarios to re-check.

requel-app
- `AnalysisInvokingCommandHandler`: dispatches the step-sequence targets.

requel-angular
- None. The project's assistants panel lists the switchable assistants the server returns, by
  display name.

Tests
- `ProjectAssistantSettingsIT` expects the exact set of switchable assistants, and
  `AuthorizationIT` their count. Both gain `legacy-step-sequence`.

## Steps

1. `StepReading`, and `StepStructureAssistant` moved onto it; its tests stay green.
2. `StepSequenceAssistant` and its unit test.
3. `getStepSequenceTargets`, the two commands, the handler.
4. ITs; `tmp/455-verify.sh`.

## Test plan

`tmp/455-verify.sh`: the backend `mvn clean verify -pl modules/coverage-report -am` and the
coverage-modules check. No Angular change.

`StepSequenceAssistantTest` (stubbed readings):
- Actor, system, actor, system: nothing.
- Two system steps: one issue naming both.
- Starts with a system step: the issue.
- Two steps by the same actor: the issue, naming the actor.
- Two different actors: nothing.
- A command step:
  - with one primary actor across the use cases, it counts as that actor (command after a
    primary-actor step: the same-actor break);
  - with no single primary actor, it is unknown.
- A sub-scenario as a step: counts by its name.
- Unknown in the middle: no pair checked across it. Unknown first: no system-start check.
- Several breaks: one issue listing all of them, in order.
- An empty scenario, or one with one step: nothing.
- `handlesTask`: null and `STEP_SEQUENCE` are served; `REQUIREMENTS_REVIEW` isn't.

`StepSequenceAssistantIT` (requel-app, real NLP):
- The #455 AC:
  - "Host starts the livestream" / "The system shows the stream" alternating: nothing;
  - two system steps: the issue naming both;
  - a scenario that opens with a system step: the issue;
  - the same actor twice: the issue naming the actor.
- Re-run of an unchanged scenario: the same issue, same id, no duplicate.
- Creating a scenario queues no step-sequence run; its own analysis covers it.
- Renaming a step from a system step to an actor step, through `EditScenarioStep`: the queued
  step-sequence run removes the issue, without editing the scenario.
- Deleting a step that caused the break: the queued run removes the issue.
- Whole-project analysis raises it.
- A project with the assistant switched off: nothing.
- `AnalysisInvokingCommandHandlerTest`: step-sequence targets are dispatched with `STEP_SEQUENCE`,
  including when the command's own target is null, and a dispatch failure doesn't fail the
  command.

## Out of scope

- How one step reads (#349) and its rewrites (#459).
- Merging two steps into one as a position.
- Steps inside a sub-scenario: each scenario is checked on its own, and a sub-scenario used as a
  step counts by its name.
- Copying or converting a step (`CopyScenarioStep`, `ConvertStepToScenario`). Their scenarios are
  re-checked on the next scenario edit or project analysis.

## Risks

- **Classifying each step is NLP work.** A scenario check classifies every direct step, and an
  edit to a step used by many scenarios fans out. Scenarios have a handful of steps, and a
  step-sequence run does no lexical or AI work.
- **Refactoring `StepStructureAssistant`.** Moving it onto `StepReading` can change behaviour by
  accident. Its unit tests and ITs run unchanged.
- **The "same actor" rule may be noisy** on fixtures written step by step. It's advisory LOW and
  the project can switch it off.

## AC mapping

| AC | Covered by |
|---|---|
| Host acts / system responds alternation raises nothing | StepSequenceAssistantTest, StepSequenceAssistantIT |
| Two consecutive system steps: an issue naming both | StepSequenceAssistantTest, StepSequenceAssistantIT |
| A scenario opening with a system step: an issue | StepSequenceAssistantTest, StepSequenceAssistantIT |
| Runs on scenario edits and whole-project analysis; re-runs update in place | StepSequenceAssistantIT |

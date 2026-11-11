# #483 — rc4: reuse an existing step from the scenario editor

https://github.com/rreganjr/Requel/issues/483

Branch `483-step-reuse` from `release/2.1` after #482 merges (both change the scenario editor),
PR to `release/2.1`. Size 3. Found in the rc4 acceptance test (#420); ships in rc5 with #482.

## Summary

A plain step can be shared by several scenarios (#476's "Used in"), but the scenario editor can
only add a step as a new one. Naming it like an existing step is refused with the API's hint, "To
reuse that step, send its id as stepId". This lets the editor suggest existing steps as a new
step's name is typed and link the one picked, and says something a user can act on when a new
step's name is taken.

## Locked decisions (2026-11-11)

1. **Suggestions in the new step's name field.** A new (unsaved) step's name input becomes an
   autocomplete. Typing suggests the project's plain steps whose names contain the text (case
   doesn't matter), up to 10, each showing its type and "used in N scenarios". Typing a name no
   step has still makes a new step.
2. **Picking links the step.** The row takes the step's id, name, notes and type and is no longer
   new: it shows as a saved step (its pencil opens the step page once the scenario is saved,
   #476/#478). Saving sends it by `stepId`, which `EditScenario` already accepts. Its notes are
   sent as they are, so the link doesn't clear them.
3. **Steps already in the scenario aren't suggested.** Nor are scenarios: a sub-scenario is still
   added with Add Sub-scenario.
4. **The suggestions are loaded once per editor visit,** on the first focus of a new step's name,
   and filtered in the browser. A project's step list is small, and this keeps typing fast.
5. **The editor's own message for a taken name.** When a save is refused with 409 `CONFLICT`
   (#482) and a new step's name matches an existing step, the editor says: "Another step in this
   project is already named '<name>'. Pick it from the suggestions to reuse it, or choose a
   different name." It matches by the loaded list, not by parsing the server's text. Any other
   conflict shows the server's message. The server's message stays as it is for API and MCP
   callers. A new step named like a *scenario* gets: "A scenario in this project is already named
   '<name>'. Use Add Sub-scenario to include it as a step, or choose a different name." (decided
   during implementation; the server's message for it is API-speak too).
6. **A new read for the list.** `GET /api/projects/{name}/steps` returns the project's plain
   steps, sorted by name, as `StepSummaryDto(id, name, text, scenarioType, usedIn)`, where
   `usedIn` is the number of scenarios using the step. Access is checked as for the other reads.
   MCP and the CLI keep using ids, so the gateway is unchanged.

## Changes

service-api / service-impl
- `StepSummaryDto` (new).
- `ProjectQueryController.listSteps` (`GET /{name}/steps`).

requel-angular
- `models/scenario.ts`: `StepSummaryDto`.
- `ScenarioService.listSteps(projectName)`.
- `scenario-editor.ts`:
  - a new step's name is a `p-autoComplete` (`data-testid="scenario-step-name"` kept, suggestions
    `scenario-step-suggestion`);
  - `onStepSuggestionSelected(group, step)` links the row;
  - the taken-name message on a 409 `CONFLICT`.
- `website/guide/requirements.html`: reusing a step.

## Test plan

`tmp/483-verify.sh` runs the backend verify, the coverage-modules check, the Angular typechecks, a
dev build and the full Angular unit suite.

- `ProjectQueryControllerTest`: `listSteps` returns plain steps only, sorted, with `usedIn`; 404
  for an unknown project; 403 without access.
- `scenario-editor.spec`:
  - typing suggests matching steps, without the scenario's own steps or scenarios;
  - picking one links the row (id, name, notes, type, not new), and the save payload sends its
    `stepId` and notes;
  - a 409 `CONFLICT` with a new step named like an existing step, or like a scenario, shows the
    editor's message; any other conflict shows the server's.
  - a new step's name field is the autocomplete, and its input keeps `data-testid="scenario-step-name"`.
- `scenario.service.spec`: `listSteps` calls the endpoint.
- e2e `step-reuse-483.e2e.ts`:
  - in a second scenario, type part of an existing step's name, pick it, save; the step page's
    "Used in" lists both scenarios;
  - a new step named like an existing one, not picked, is refused with the editor's message.
- Local build on the rc4 database before the PR.

## Out of scope

- Reusing steps through MCP or the CLI (they already pass `stepId`).
- Suggesting existing steps when renaming a saved step (that renames the step itself).

## Risks

- **PrimeNG autocomplete in a reorderable row.** The row already stops key events from reaching
  the reorder handler; the autocomplete keeps that, and e2e covers typing and picking.

## AC mapping

| AC | Covered by |
|---|---|
| Typing suggests matching steps; picking links it; "Used in" lists both scenarios | scenario-editor.spec, e2e |
| A linked step isn't duplicated | e2e, the save payload test |
| Steps already in the scenario aren't suggested | scenario-editor.spec |
| A taken name shows the editor's message | scenario-editor.spec, local build |
| Specs, an IT for the list, e2e | the above |

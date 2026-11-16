# #476 — rc1: a step page, so step issues can be seen and resolved

https://github.com/rreganjr/Requel/issues/476

Branch `476-step-page` from `release/2.1` (at `2.1.0-rc1`), PR to `release/2.1`. Size 5. Found
in the rc1 acceptance test (#420); the fix ships in rc2.

## Summary

The step checks added in 2.1 (#349, #459) put issues on a scenario's steps, and no page shows a
step's annotations. A step's issue appears in Open Issues with no link, its positions can't be
seen, and it can't be resolved. This adds a page for a step, opens it from the step's pencil in
the scenario editor, marks rows whose step has open issues, and links step issues from Open
Issues.

## Locked decisions (2026-11-05)

1. **A full step page** at `projects/:name/steps/:stepId`, for a plain step. A scenario used as a
   step already has its own page. It shows:
   - name, type and notes (what the popup shows today), saved on their own with
     `EditScenarioStep`;
   - **Used in**: the scenarios that use the step, linked, with a note that an edit changes the
     step in all of them;
   - the annotations section (issues, positions, Resolve, add issue or note);
   - the Sources section.
2. **The pencil opens the page** for a saved step. It is disabled while the scenario has unsaved
   changes (the same rule as "Review with AI", #387), because the page saves the step on its own.
   A new step, not yet saved, keeps the current popup.
3. **A warning icon beside the pencil**, not replacing it, on a row whose step or sub-scenario has
   open issues:
   - colored by the highest open severity;
   - its tooltip gives the count and that severity;
   - clicking it opens the step page, or the sub-scenario's page.
4. **Open Issues links step issues** to the step page.
5. **Folded in:** the `$$MYSQL_ROOT_PASSWORD` fix in the release guide's compose example.

## Contracts

Backend (service-api / service-impl)

- **`StepDto`** gains `openIssues` (int) and `highestOpenSeverity` (`LOW`/`MEDIUM`/`HIGH`, absent
  when there are none). Both are computed in `toScenarioDetailDto` from each step's annotations.
  An open issue is one with no resolving position, as Open Issues counts them.
- **`StepDetailDto`** (new): `id`, `version`, `name`, `text`, `scenarioType`, `createdBy`, and
  `usedIn` (a list of `EntityReferenceDto("Scenario", id, name)`, sorted by name).
- **`GET /api/projects/{name}/steps/{stepId}`** returns `StepDetailDto` for a plain step in the
  project. It returns 404 for an unknown id, a step in another project, or a scenario (which has
  its own read). Access is checked as for the other reads.
- **`EditScenarioStepInput`** (new record): `projectName`, `stepId` (required), `version`,
  `name` (`@Size` as `EditStepInput`), `text`, `scenarioTypeName`. It follows the partial-update
  contract: null leaves a field unchanged, and "" clears `text`. `EditScenarioStep` is
  registered with it and returns the `StepDetailDto`.
  - **On MCP and the CLI too** (decided 2026-11-05). `EditScenarioStep` joins the gateway
    allowlist, with a caller-facing description and an authorization hint, as #296 asks of every
    allowlisted command. Through `EditScenario` a caller has to resend every step to change one,
    and a step it leaves out is deleted, so a single-step edit is the safer call for an agent.
    `CopyScenarioStep` and `DeleteScenarioStep` stay off the gateway; removing a step from a
    scenario is still done through `EditScenario`. The `GatewayPolicyConfig` comment and
    `doc/guides/local_mcp_bridge.md` say so.
  - Authorization is unchanged: Scenario[Edit].

Angular

- **`StepEditorComponent`** (new, `features/steps/step-editor.ts`), following the actor
  editor's structure:
  - the details card, with Save and the dirty guard;
  - the Used in table;
  - the annotations section (`entityType="Step"`);
  - the Sources section;
  - a live refresh on the step's own events, as the other editors do.
- **The route**: `projects/:name/steps/:stepId` (`artifactType: 'step'`, title "Step"),
  `canDeactivate: [dirtyCheckGuard]`. `artifactNameResolver` and `ENTITY_ROUTES` get `Step` →
  `steps`. Steps have no list page, so the breadcrumb goes from the project to the step.
- **`ScenarioService.getStep`**; Save sends `EditScenarioStep` through `CommandService`, as the
  other editors do.
- **The scenario editor:**
  - The pencil on a plain step with a `stepId` navigates to the step page, disabled with
    tooltip "Save the scenario first" while `hasUnsavedChanges()`. A step with no `stepId`
    opens the popup as now.
  - The warning icon (`pi pi-exclamation-triangle`, `data-testid="scenario-step-issues"`) shows
    when `openIssues > 0`:
    - its tone comes from `issueSeverityTone(highestOpenSeverity)`;
    - its `ariaLabel` and tooltip read "2 open issues, highest MEDIUM";
    - it links to the step page (or sub-scenario page). The app has no anchor scrolling, so it
      opens the page rather than a section of it.
  - Sub-scenario rows show it too.
  - The counts come with each scenario load, and returning from the step page reloads the
    scenario.
- **Open Issues** needs no code beyond `ENTITY_ROUTES`.

Docs
- `doc/guides/local_mcp_bridge.md`: `EditScenarioStep` in the command list.
- `doc/guides/RELEASE_PROCESS.md`: `$$MYSQL_ROOT_PASSWORD` in the example healthcheck. The repo's
  compose files are checked for the same pattern.
- `website/guide/requirements.html`: the step row's pencil opens the step's page; the warning
  icon.

## Steps

1. Backend: `StepDto` counts, `StepDetailDto`, the read, `EditScenarioStepInput` and its
   registration.
2. Angular: model and service, the step editor and route, resolver and `ENTITY_ROUTES`.
3. Scenario editor: the pencil and the icon.
4. Docs; tests; `tmp/476-verify.sh`.

## Test plan

`tmp/476-verify.sh` runs:
- backend `mvn clean verify -pl modules/coverage-report -am` and the coverage-modules check;
- the Angular typechecks, a development build (new route) and the full Angular unit suite;
- the touched e2e specs against a local stack, if you want them before CI; CI runs them all.

**Backend:**
- `ProjectQueryControllerIT` (or the closest existing read IT):
  - `GET .../steps/{id}` returns the step with `usedIn` listing both scenarios that share it;
  - 404 for a scenario id, an unknown id, and another project's step;
  - 403 without access;
  - the scenario read's `StepDto` carries `openIssues` and `highestOpenSeverity` for a step
    with a LOW and a MEDIUM open issue, and nothing for a clean step;
  - a resolved issue doesn't count.
- **`EditScenarioStep` over the gateway** (`CommandGatewayIT`): renames a step; no Scenario[Edit]
  is refused. `McpToolCatalogLockstepIT` lists the new tool.
- **`EditScenarioStep` over `/api/commands`:**
  - renames a step, and the change shows in both scenarios;
  - a stale `version` is refused;
  - no Scenario[Edit] → 403.

**Angular unit:**
- `step-editor.spec.ts`:
  - loads and shows the details, Used in and annotations;
  - Save sends `EditScenarioStep`;
  - the dirty guard;
  - read-only without edit permission.
- `scenario-editor.spec.ts`:
  - the pencil on a saved step routes to the step page, and is disabled while dirty;
  - a new step opens the popup;
  - the icon shows only when `openIssues > 0`, with the tooltip text and a link;
  - a sub-scenario row shows the icon too.
- `open-issues.spec.ts`: a `Step` issue has a link.
- `artifact-name.resolver` spec: `step`.

**e2e:**
- `step-page-476.e2e.ts`:
  - Create a scenario with a command step ("Download the receipt").
  - Its row shows the icon; the pencil opens the step page, which shows the issue and its
    rewrite position.
  - Choosing the rewrite renames the step. Back on the scenario, the icon is gone and the
    step has the new name.
  - The pencil is disabled while the scenario has unsaved changes.
  - Open Issues links a step issue to the step page.
- `scenarios.e2e.ts`: the step popup test edits a new, unsaved step.

## Out of scope

- Steps inline in the scenario editor showing their issues (the icon and the page cover it).
- Resolving from the Open Issues list itself.
- Delete or copy on the step page; removing a step from a scenario stays in the scenario
  editor.

## Risks

- **Analysis timing.** A step's #349 issue comes from its post-edit run, which can land after
  the scenario page has loaded. The icon then shows on the next load, not live. The step page
  itself refreshes live through the annotations section.
- **Shared steps.** An edit on the step page changes every scenario that uses the step. The Used
  in list and its note make that visible, and it's how the popup already behaves.

## AC mapping

| AC | Covered by |
|---|---|
| Icon with severity color and count tooltip; none when clean | scenario-editor.spec, ProjectQuery IT, e2e |
| Pencil opens the step page with the issue and positions; a rewrite resolves it and the icon goes | e2e step-page-476 |
| Used in lists every scenario, linked | step-editor.spec, IT |
| Editing on the step page saves; every scenario shows the new name | EditScenarioStep IT, e2e |
| Pencil disabled while dirty; new step uses the popup | scenario-editor.spec |
| Sub-scenario row icon opens its page | scenario-editor.spec |
| Open Issues links step issues | open-issues.spec, e2e step-page-476 |
| Release guide compose has no unset-variable warning | the guide's example |
| e2e covers the route and links | the e2e specs above |

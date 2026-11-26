# #505 — Steps list page with open issues and usage

https://github.com/rreganjr/Requel/issues/505

Branch `505-steps-list` from `release/2.2`, PR to `release/2.2`. Size 2.

## Summary

Steps can only be reached from a scenario. The step page (#476) lists the scenarios that use a
step, but a step's open issues show only as an icon inside the scenario being viewed (#478), so
there is no project-wide view of steps that need attention. This ticket adds a Steps list page
with usage, open issues and the step's actor (#468), a step count on the project, and Steps in
the overview and the sidebar.

Review against the tree: still needed. The ACs stand. #468 has merged, so the actor column the
issue made conditional is in scope.

## Locked decisions

1. **Actor column.** A **Who** column shows Primary actor, System, the named actor, or nothing
   when the step's name says who does it. The Name column shows the stored name.
2. **Sidebar too.** Steps, with its count, goes in the sidebar's project list between Scenarios
   and Use Cases, as well as on the overview.
3. **Counting.** `stepCount` comes from one count query in the project repository, not from
   walking scenarios. `ProjectDto` is built once per project on the project list page.
4. **Back from a step.** A step opened from the list goes back to the list with its filters,
   via `history.back()` when the navigation state says it came from the list. Opened any other
   way, Back keeps today's behaviour: the only scenario using the step, else history.
5. **Filters live in the URL** as query params (`q`, `type`, `issues`, `shared`).
   `history.back()` restores the list with its filters only if they are in the URL.

## Contract

- `StepSummaryDto` gains `int openIssues` and `String highestOpenSeverity`, computed as for
  `StepDto` (unresolved issues on the step, highest severity by `severityRank`). Both DTOs use
  one shared helper. The scenario editor's suggestions ignore the new fields.
- `ProjectDto` gains `int stepCount`: plain steps only, so scenarios used as steps aren't
  counted.
- `ProjectRepository.countPlainSteps(Project)`: JPQL
  `select count(e) from StepImpl e where e.projectOrDomain = :project and type(e) = StepImpl`.
- The CLI project summary prints `steps=N`.
- `GET /api/projects/{name}/steps` is unchanged apart from the two new fields.

## Steps

1. Backend: the DTO fields and the shared open-issue helper, `countPlainSteps` in the
   repository and its JPA implementation, `stepCount` in both `ProjectDto` builders
   (`ProjectQueryController.toProjectDto`, `ProjectCommandRegistrar.toDto`), and the CLI
   summary.
2. Angular model: `stepCount` on the project model, and the two new fields on the step summary
   model.
3. `StepListComponent` at `projects/:name/steps`, on `app-data-table`:
   - columns: Name, Who, Type, Used in (count), Open issues (count with a severity tag);
   - in the toolbar: the search box (name), a Type select, and "Has open issues" and "Shared"
     toggles, all kept in query params;
   - clicking a row opens the step page with navigation state `{ fromStepList: true }`;
   - an empty state for no steps, and a separate one for no matches.
4. Step page: Back goes to the list when the state says it came from there (decision 4).
5. A Steps card on the overview (`project-workspace.ts`) and a Steps entry in the sidebar
   (`sidebar-nav.ts`), both with `stepCount`.
6. The route in `projects.routes.ts` with breadcrumb "Steps" and artifact type `step`.

## Test plan

- **`StepListApiIT`** (requel-app, H2), on a fixture with a step shared by two scenarios, a
  single-use step, a step with a HIGH open issue and a sub-scenario:
  - the list has the three plain steps, with usage counts 2, 1 and 1, and the HIGH step has
    `openIssues` 1 and `highestOpenSeverity` HIGH;
  - the sub-scenario is in neither the list nor `stepCount`;
  - `stepCount` is 3 on the project read and on the project list.
- `ProjectQueryControllerTest` and the CLI test: the new fields.
- Specs:
  - the list: columns, the severity tag, each filter, query-param round trip, row navigation
    with state, empty and no-match states, axe;
  - the step page: Back with and without the state;
  - the overview card and the sidebar entry.
- e2e `step-list-505`: the overview's Steps card opens the list, "Has open issues" and "Shared"
  filter it, a row opens the step page, and Back returns to the filtered list.

## Out of scope

- Creating steps from the list (steps are created in a scenario).
- Bulk edit or delete.

## Risks

- The list walks every scenario's steps and each step's annotations. That is fine for one
  project and is what the page needs anyway.
- `ProjectDto` gains a field, so every constructor call and test stub must change. It is a
  compile-time break, caught by the build.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| Shared, single-use and HIGH-issue steps listed with the right counts and badge | Step 1; `StepListApiIT`, list spec |
| "Has open issues" shows only the issue step; "shared" only the shared step | Step 3; list spec, e2e |
| Clicking a row opens the step page, which lists its scenarios | Step 3; list spec, e2e |
| The overview's Steps card shows the plain-step count and links to the list | Step 5; workspace spec, e2e |
| Sub-scenarios not in the list or the count | Step 1; `StepListApiIT` |
| e2e: route, Steps card and row navigation | `step-list-505` |

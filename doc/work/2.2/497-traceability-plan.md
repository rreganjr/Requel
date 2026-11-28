# #497 — Traceability page: goal matrix and gaps

https://github.com/rreganjr/Requel/issues/497

Branch `497-traceability` from `release/2.2`, PR to `release/2.2`. Size 3.

## Summary

A project page with two parts. The goal matrix shows what references each goal. The gaps list
names the requirements nothing traces to or from. Both are computed from links the model already
has, with no AI, and the one read behind them is exposed on REST, the gateway (MCP) and the CLI.
Gaps are shown, never raised as issues; that is a later decision, as the ticket says.

Review against the tree: still needed. The ACs stand. #498 is a closed duplicate. Since the
ticket was written, #468 lets a step or a scenario's name carry an actor, and that now counts as
using the actor (decision 3).

## Locked decisions

1. **Sub-scenarios.** A scenario is covered if a use case reaches it directly, as its main or
   an additional scenario, or through any chain of parent scenarios that use it as a step.
   Cycles are guarded.
2. **Stakeholders.** Every stakeholder with no goals is a gap: user stakeholders as well as
   named ones.
3. **Step actors.** An actor is in use if a story or a use case lists it, a use case has it as
   primary actor, or a step or scenario names it as who does it (`ActorRole.NAMED`, #468).
4. **Gaps as a flat list** of `{kind, entity}`. The page groups them by kind, and the CLI and
   MCP get one simple shape. Kinds, in this order: `GOAL_NOT_REALIZED`, `ACTOR_NOT_USED`,
   `STORY_WITHOUT_GOAL`, `STORY_NOT_IN_USE_CASE`, `USE_CASE_WITHOUT_GOAL`,
   `SCENARIO_NOT_IN_USE_CASE`, `STAKEHOLDER_WITHOUT_GOAL`. Within a kind, entities are in
   creation (id) order.
5. **"Realized"** means a story or a use case references the goal. Stakeholders and actors
   referencing it show in the matrix but don't realize it. A goal that isn't realized is marked
   in the matrix and listed under `GOAL_NOT_REALIZED`.
6. **The overview links to the page** with a "Traceability" button among its header actions.
   It isn't a sidebar entry, because it is a view of the project rather than a kind of entity.

## Contract

- `TraceabilityDto(List<GoalTraceDto> goals, List<TraceGapDto> gaps)`.
- `GoalTraceDto(Long id, String name, boolean realized, List<EntityReferenceDto> stakeholders,
  List<EntityReferenceDto> actors, List<EntityReferenceDto> stories,
  List<EntityReferenceDto> useCases)`. Goals are in creation (id) order, and each list is sorted
  by name.
- `TraceGapDto(String kind, EntityReferenceDto entity)`.
- The REST read is `GET /api/projects/{name}/traceability`: 404 when there is no such project,
  403 without read access.
- **Gateway:**
  - `QueryGateway.getTraceability(projectName)` is a default method that throws when a gateway
    doesn't support it, as `listSources` does;
  - in-process, it unwraps the controller;
  - REST client: `GatewayQueryController` serves `/projects/{name}/traceability` and
    `RestQueryGateway` calls it.
- **MCP:** a `getTraceability` tool with a `projectName` argument, described in
  `QueryDescriptions`. `McpToolCatalogLockstepIT` keeps the catalog and the gateway in step.
- **CLI:** `requel traceability <project> [--format json]`. The text output has one line per
  goal, listing who references it and marking a goal that isn't realized, then the gaps grouped
  by kind.

## Steps

1. The DTOs in service-api.
2. `TraceabilityService` (service-impl, query package): walks the project's goals, actors,
   stories, use cases, scenarios, steps and stakeholders once each. Wired into
   `ProjectQueryController` as a new endpoint.
3. The gateway, MCP tool, CLI command and descriptions.
4. **Angular:**
   - `TraceabilityService` client and model;
   - `TraceabilityComponent` at `projects/:name/traceability`:
     - the matrix on `app-data-table`: Goal, Stakeholders, Actors, Stories and Use cases columns,
       each cell a list of links, and a "Not realized" tag on rows without a story or use case;
     - the gaps list grouped by kind, with a count per kind and links;
     - an empty state when there are no gaps, and another when the project has no goals;
   - the route, and the overview's header button.
5. User guide: a "Traceability" section in projects.html.

## Test plan

- **`TraceabilityIT`** (requel-app, H2):
  - **One of each gap.** A fixture project with exactly one of each gap kind returns each
    exactly once. Covered and uncovered cases sit side by side:
    - a sub-scenario under a covered scenario is not a gap, but a scenario in no use case is;
    - an actor named only by a step is not a gap;
    - a user stakeholder with no goals is a gap.
  - **No gaps.** A fully linked project returns none.
  - **The matrix.** Every goal, in creation order, with each referencing entity in its column.
    A goal referenced only by a stakeholder isn't realized.
  - **The gateway** answers the same as REST.
- `ProjectQueryControllerTest`: 404 and 403.
- CLI: a `QueryCommandsTest` case for the text and JSON output. MCP: a `McpReadServiceTest` case
  with a stub, plus the lockstep IT.
- Specs:
  - the page: the matrix cells and links, the "Not realized" tag, the grouped gaps and their
    links, both empty states, axe;
  - the overview button;
  - the route list.
- **e2e `traceability-497`:**
  - the overview button opens the page;
  - a goal with no story shows "Not realized" and appears under its gap;
  - a gap's link opens the entity.

## Out of scope

From the ticket: raising gap issues, switchable matrix axes and CSV export.

## Risks

- **Load.** The read touches every entity in the project once. That is the same order of work
  as the project content read, and it runs only when the page or tool asks for it.
- **Scenario reachability** walks down from each use case's main and additional scenarios into
  the scenarios they use as steps. A visited set stops cycles, so the walk stays linear. Walking
  upward with a per-scenario cache was rejected: a result cached while a cycle was being cut
  could be wrong.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| One of each gap kind returned exactly once, and listed on the page | `TraceabilityIT`, page spec, e2e |
| A fully linked project returns no gaps | `TraceabilityIT`, page spec (empty state) |
| The matrix shows every goal, each referencing entity in its column, linked | `TraceabilityIT`, page spec |
| No read access → 403 | `ProjectQueryControllerTest` |
| Works over the gateway and the CLI | `TraceabilityIT` (gateway), CLI test, MCP test, lockstep IT |

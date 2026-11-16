# #387 — Request an AI review from the editor and over MCP

https://github.com/rreganjr/Requel/issues/387

Branch `387-ai-review-editor-mcp` from `release/2.1`, PR to `release/2.1`. Size 5 (re-estimated
from 3 at review: UI, availability, a gateway interface, MCP, CLI and docs).

## Summary

Since #43 a single-item AI review can be requested only with `POST /api/ai/reviews`, and the guide
documents it with curl. This adds a "Review with AI" button to the editors, an MCP tool pair and a
`requel review` command. All three go through one service with the same permission rule (a
stakeholder on the project, or a system admin). Along the way the request learns three things it
lacks today: it refuses when no review can run, it doesn't queue a duplicate while one is in
flight, and it returns the id of the run it queued.

## Locked decisions (2026-10-30)

1. **The button lives in the Annotations section** (`shared/annotations-section.ts`), the
   "Annotations" heading with Add Note / Add Issue that lists an entity's notes and issues. It is
   one component on the goal, story, actor, use case, scenario and glossary term editors, so one
   change covers them all. It shows the run's status, and reloads the annotations when the run
   finishes. Follow-up is by polling `GET /api/ai/reviews`; no SSE.
2. **Steps have no UI button.** Steps are edited in a dialog in the scenario editor, and their
   issues show only in Open Issues, which doesn't link steps. Step stays reviewable over REST,
   MCP and the CLI.
3. **Availability.** `GET /api/ai/reviews/availability?entityType=&entityId=` returns
   `{available, reason}`. The reason is one of these:
   - AI is off on the server (`requel.ai.enabled=false`).
   - The project isn't on `requel.ai.projectAllowlist`.
   - The review definition that would run for the type is switched off in the project's
     assistants.

   The button is hidden unless the review is available. A POST that can't run is refused with
   `409 AI_REVIEW_UNAVAILABLE` naming the reason, instead of queuing a run that does nothing.
4. **No duplicate runs.** While the entity's latest `REQUIREMENTS_REVIEW` run is `QUEUED` or
   `RUNNING`, a request returns that run instead of dispatching another. The same applies to the
   policy pass.
5. **The POST returns the run ids**: `202 {runId, policyRunId}` (`policyRunId` is null when no
   policy applies). `AssistantDispatcher.dispatch` already returns an `AssistantRunHandle`;
   `AnalysisRequestDispatcher` stops discarding it.
6. **MCP** has two tools, each taking `projectName`, `entityType` and `entityId`. `projectName` is
   required, and the entity must be in it (#342's `requireSameScope` rule).
   - `requestAiReview` is listed only when `requel.ai.enabled` and `requel.gateway.write.enabled`
     are both true: its findings are written as issues, and it spends provider money.
   - `getAiReview` (optional `taskType`: `REQUIREMENTS_REVIEW` or `POLICY_REVIEW`) is a read
     tool, listed whenever `requel.ai.enabled` is true.
7. **CLI**: `requel review request PROJECT TYPE ID [--wait]` and
   `requel review show PROJECT TYPE ID [--policy]`. They go through new gateway endpoints under
   `/api/gateway/ai/reviews` (the gateway security chain, client id and audit). A new
   `AiReviewGateway` interface in `gateway-api` has an in-process implementation (MCP) and a REST
   one (CLI). `AiReviewDto` moves to `service-api`.
8. **Docs**: `AI_ASSISTANT_SETUP.md` §5 is rewritten around the button, the MCP tools and the
   CLI, with curl kept as a short "from scripts" note. The reviewable types list gains
   GlossaryTerm, and the "the UI has no review button yet" line goes.
9. **One ticket, size 5.**

## Contracts

SPA REST (`/api/**` chain, unchanged paths):

| Call | Result |
|---|---|
| `GET /api/ai/reviews/availability?entityType=&entityId=` (new) | `200 {available: bool, reason: string\|null}`; 400 for a type that isn't reviewable, 403 without project access, 404 for an unknown entity |
| `POST /api/ai/reviews?entityType=&entityId=` | `202 {runId, policyRunId}`, which can be a run already in flight; `409 {"error":"AI_REVIEW_UNAVAILABLE","message":<reason>}`; 400 / 403 / 404 as today |
| `GET /api/ai/reviews?...` | unchanged |

Gateway REST (`/api/gateway/**` chain, new):

| Call | Result |
|---|---|
| `POST /api/gateway/ai/reviews/{projectName}?entityType=&entityId=` | as the SPA POST. 403 "write commands are disabled" when `requel.gateway.write.enabled=false`, as `GatewayCommandController` does. An entity outside `projectName` is refused |
| `GET /api/gateway/ai/reviews/{projectName}?entityType=&entityId=&taskType=` | the latest run (`AiReviewDto`), 204 never reviewed |

`AiReviewGateway` (gateway-api):

```java
AiReviewRequestDto requestReview(String projectName, String entityType, long entityId);
Optional<AiReviewDto> latestReview(String projectName, String entityType, long entityId, String taskType);
```

Failures are `GatewayException`s: NOT_FOUND, INVALID_INPUT (a type that isn't reviewable, an
unknown task type, wrong project), UNAUTHORIZED, and CONFLICT (unavailable, with the reason).

## Changes

Backend
- `assistant-core` `SimpleAssistantRegistry`: a public `reviewsFor(Object target, Long projectId)`
  returning the review definitions that would run after the project's switches, beside
  `policiesFor`. It reuses `definitionsFor`.
- `requel-app` `AiReviewService`:
  - `availability(entityType, entityId)` checks the AI switch (`AiProperties`, optional bean),
    the allowlist and `reviewsFor`.
  - `requestReview` refuses when unavailable (`AiReviewUnavailableException`), returns the
    in-flight run when there is one (decision 4), and otherwise returns the ids
    `AnalysisRequestDispatcher` now hands back.
  - An overload of both takes a `projectName` and checks the entity is in it (for the gateway).
- `AnalysisRequestDispatcher.dispatch(target, user, taskType)` returns the queued run id (or null
  for the no-op cases); the existing callers ignore it.
- `AiReviewController`: `/availability`, the 202 body, the 409 mapping.
- `service-api`: `AiReviewDto` (moved from requel-app, package changes) and `AiReviewRequestDto`,
  plus `AiReviewAvailabilityDto`.
- `gateway-api`: `AiReviewGateway`, and descriptions for the tools and CLI (`QueryDescriptions` or
  a sibling `AiReviewDescriptions`).
- `requel-app`:
  - `InProcessAiReviewGateway` (service → `GatewayException` kinds).
  - `GatewayAiReviewController` at `/api/gateway/ai/reviews`, write-gated for POST.
- `mcp-server` `McpReadService`: an optional `AiReviewGateway` and the two flags; the tool
  descriptors and `callTool` arms. Errors map as the write tools' do (CONFLICT and INVALID_INPUT →
  invalid params with the message).
- `gateway-rest-client`: `RestAiReviewGateway`.
- `requel-cli`: `ReviewCommand` with `request` (`--wait` polls every 3 s until `SUCCEEDED` /
  `FAILED`, prints the findings, and gives up after 5 minutes with the run id) and `show`
  (`--policy`). Registered in `RequelCli`.

Angular
- `shared/annotations-section.ts`:
  - For a reviewable `entityType` (`REVIEWABLE_TYPES` less `Step`), it asks for availability on
    load. Available: a "Review with AI" button (`pi pi-sparkles`,
    `data-testid="annotation-ai-review"`) in the header actions, for anyone with project access.
  - A status line (`role="status"`, `aria-live="polite"`, `data-testid="annotation-ai-review-status"`)
    shows "AI review queued…", "AI review running…", "AI review done: 2 issues, 1 note" (or "no
    findings") and "AI review failed: <errorSummary>".
  - On load it also reads the latest run, so a review in flight is picked up again after
    navigating away.
  - The button is disabled while a run is followed. Polling is every 3 s, and stops when the run
    is terminal or after 5 minutes ("still running; check back later"). When the run succeeds the
    annotations reload.
  - A 409 shows its reason and hides the button.
- A small `AiReviewService` (core) for the three calls.

Docs
- `doc/guides/AI_ASSISTANT_SETUP.md` §5 and the line near §7 (decision 8).
- `doc/guides/VS_CODE_MCP.md` / `mcp_remote_connection.md`: name the two tools where the tool list
  is given, if it is.

## Test plan

`tmp/387-verify.sh`:
- `mvn clean verify -pl modules/coverage-report -am` plus the coverage-modules check.
- In `requel-angular`: the full unit suite, both `tsc` typechecks and a development build.

Backend
- `AiReviewDispatchIT` (extended, noop provider):
  - The POST returns the queued run ids.
  - A second POST while the run is in flight returns the same id.
  - It is unavailable (409 with the reason) when the type's definition is switched off in the
    project.
  - Availability is true when the review can run.
- `AiReviewAvailabilityTest` (unit, mocked collaborators): each reason (AI off, not allowlisted,
  switched off), and the specific-versus-fallback coverage rule.
- `SimpleAssistantRegistry` test: `reviewsFor` honours the switches and the fallback rule.
- `AiReviewGatewayIT` (new):
  - In-process: request and read as a stakeholder; a non-stakeholder is refused; another
    project's entity is refused; an unknown entity is NOT_FOUND; unavailable is CONFLICT.
  - `/api/gateway/ai/reviews` over MockMvc with a PAT: POST refused when writes are disabled.
- `McpReadServiceTest`:
  - The tools are listed only under the right flags.
  - Both tools pass their arguments through.
  - CONFLICT becomes invalid params with the reason.
- `RestAiReviewGatewayTest`, and `ReviewCommandTest` (request, `--wait` to SUCCEEDED, show,
  `--policy`).
- `AuthorizationIT`: availability and POST for a non-stakeholder are 403.
- `AiReviewDtoTest` follows the move.

Angular
- `annotations-section.spec.ts`:
  - The button is hidden when unavailable, and for Report and Step.
  - It is shown when available.
  - Clicking posts, then polls queued → running → done and reloads the annotations.
  - A failed run shows the reason.
  - A 409 hides the button and shows the reason.
  - A run already in flight on load is followed.
  - Polling stops after the cap and when the component is destroyed.
  - axe.
- `e2e/annotations.e2e.ts`: CI runs with AI off, so the case stubs the three `/api/ai/reviews`
  calls with `page.route`: available, 202, RUNNING then SUCCEEDED. It checks the button, the
  status line and the annotations reload on a real goal editor. A second case checks that with AI
  off (no stub) the button is absent.

## Out of scope

- A UI button for steps (decision 2).
- Reviewing several entities at once, and project-wide runs (#268 has its own path).
- SSE for run status.
- Changing what a review does or which definitions run.

## Risks

- **The duplicate check races two near-simultaneous POSTs**: both may see no run in flight and
  dispatch. It's acceptable, since the second click is disabled in the UI and the cost is one
  extra run.
- **The `AiReviewDto` package move** touches its importers in requel-app and its test; there are
  no external clients of the class.
- **Availability depends on optional beans** (`AiProperties`, the executor factory). With AI off
  they're absent, which reads as "off on the server", the right answer.

## AC mapping

| AC | Covered by |
|---|---|
| A review can be requested and followed from each editor | Decision 1; `annotations-section.spec.ts`, `annotations.e2e.ts` |
| Shown only when AI review is on for the server and the project | Decision 3; `AiReviewAvailabilityTest`, the spec's hidden cases |
| Requested and followed over MCP, same permission rules | Decision 6; `AiReviewGatewayIT`, `McpReadServiceTest` |
| `requel` subcommand | Decision 7; `ReviewCommandTest`, `RestAiReviewGatewayTest` |
| The guide shows the button instead of the curl call | Decision 8 |

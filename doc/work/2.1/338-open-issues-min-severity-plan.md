# #338 — Filter open issues by minimum severity

https://github.com/rreganjr/Requel/issues/338

Branch `338-open-issues-min-severity` from `release/2.1`, PR to `release/2.1`. Size 3.

## Summary

#271 gave issues a `LOW | MEDIUM | HIGH` severity and sorts every issue list by it, but nothing
filters by it. On a project full of `LOW` lexical issues, a reviewer or an MCP client still
receives and pages through all of them. This adds an optional minimum severity to the open-issues
read everywhere it is exposed: REST, gateway, MCP, CLI, and the Angular Open Issues page.

## Locked decisions (2026-10-30)

1. **`minSeverity` is optional on both open-issues endpoints**: the SPA's
   `GET /api/projects/{name}/open-issues` and the gateway's
   `GET /api/gateway/query/projects/{name}/open-issues`. It is case-insensitive
   (`IssueSeverity.parse`). If it is absent or blank, every issue is returned, as today. An
   unknown value is refused with an error naming `minSeverity`: 400 on REST, invalid input on MCP,
   and an option error on the CLI.
2. **`QueryGateway` gets `getOpenIssues(projectName, minSeverity)`**. The one-argument method
   becomes a default that passes `null`, so existing callers don't change. The in-process and REST
   gateways implement the two-argument form.
3. **`getProjectContext` takes the same optional `minSeverity`**, and it filters only the
   context's `openIssues`. The project, tree and glossary are unchanged. The gateway `/context`
   endpoint, the MCP tool and `requel context` all take it.
4. **The Angular Open Issues filter runs in the browser.** The toolbar gets a severity selector
   (All / Medium and up / High) next to Hide stale. Changing it filters the rows already loaded,
   with no reload. The must-resolve badge still counts every open issue, as it does when stale
   issues are hidden.
5. **Out of scope:** the per-entity annotations list (the issue's "optionally") and filtering by
   anything other than severity.

## Contracts

| Surface | Before | After |
|---|---|---|
| SPA REST | `GET /api/projects/{name}/open-issues` | `...?minSeverity=MEDIUM`; unknown value → 400 `{"error":"BAD_REQUEST","message":"Unsupported minSeverity value: X; expected LOW, MEDIUM or HIGH"}` |
| Gateway REST | `GET /api/gateway/query/projects/{name}/open-issues`, `/context` | both take `?minSeverity=`; unknown value → 400 |
| `QueryGateway` | `getOpenIssues(p)`, `getProjectContext(p)` | adds `getOpenIssues(p, min)`, `getProjectContext(p, min)`; the old methods become defaults passing `null` |
| MCP | `getOpenIssues {projectName}`, `getProjectContext {projectName}` | both add an optional `minSeverity` with `enum: [LOW, MEDIUM, HIGH]`; unknown value → INVALID_PARAMS |
| CLI | `requel open-issues PROJECT`, `requel context PROJECT` | both add `--min-severity LOW\|MEDIUM\|HIGH`; unknown value → picocli option error (exit 2) naming `--min-severity` |

The filtered result keeps today's order: severity, then fresh before stale, then entity type,
entity name and text.

## Changes

Backend
- `ProjectQueryController.getOpenIssues`: add `@RequestParam(required = false) String
  minSeverity`. Parse it once. A non-blank value that doesn't parse throws
  `IllegalArgumentException` (`ApiExceptionHandler` turns it into the 400). The loop skips issues
  ranked below the minimum. The sort is unchanged.
- `QueryDescriptions`: add `ISSUE_SEVERITIES` (`List.of("LOW", "MEDIUM", "HIGH")`) and
  `MIN_SEVERITY` (the parameter's description). Add a sentence to `GET_OPEN_ISSUES` and
  `GET_PROJECT_CONTEXT` saying a minimum severity narrows the issues.
- `QueryGateway`: the two new methods. The old ones become defaults.
- `InProcessQueryGateway`: implement both. Wrap the controller call in `serviceRead`, so the
  `IllegalArgumentException` becomes a 400 `ResponseStatusException` carrying its message, as
  `getProjectContent` does for `annotations`. `getProjectContext` passes `minSeverity` to the
  open-issues read only.
- `GatewayQueryController`: `minSeverity` request param on `/open-issues` and `/context`.
- `RestQueryGateway`: send `minSeverity` as a query param when it isn't null.
- `McpReadService`: an `openIssuesSchema()` (`projectName` required, `minSeverity` optional enum),
  used for both tools. `callTool` checks `minSeverity` against `ISSUE_SEVERITIES` (ignoring case)
  and throws `McpInvalidParamsException` naming it, as the write tools do for bad fields, then
  passes it on. Nothing in the read path maps a gateway 400 to invalid params today, so the check
  is made here rather than relying on the gateway's refusal.
- CLI `OpenIssuesCommand` and `ContextCommand`: `@Option(names = "--min-severity")`. A value that
  isn't one of `ISSUE_SEVERITIES` (ignoring case) raises a `ParameterException` naming the option
  before any call is made.
- Test doubles that implement `QueryGateway` (`InMemoryGateway`, `StubProjectQueryGateway`):
  implement the two-argument forms.

Angular
- `features/open-issues/open-issues.ts`: a `minSeverity` signal (`'ALL' | 'MEDIUM' | 'HIGH'`,
  default `'ALL'`) and a small select or segmented control in `toolbarActions`
  (`data-testid="open-issues-min-severity"`). `issues()` applies both filters. `staleCount` and
  `mustResolveCount` still count every issue. The empty-state message tells "no open issues"
  apart from "no issues at this severity".
- No service or model change: the page keeps loading everything once.

## Test plan

`tmp/338-verify.sh`:
- `mvn clean verify -pl modules/coverage-report -am` plus the coverage-modules check.
- In `requel-angular`: the open-issues specs (`CI=1 npx ng test --watch=false`), both `tsc`
  typechecks, and a development build.

Backend
- `IssueSeverityIT` (#271's IT, extended):
  - REST `minSeverity=MEDIUM` returns only `MEDIUM` and `HIGH` issues, in severity order.
  - `high` (lower case) works.
  - Absent or blank returns everything.
  - `bogus` is 400 with a message naming `minSeverity`.
  - Over the in-process gateway, the same filter applies.
  - `getProjectContext` with `HIGH` filters `openIssues` and leaves project, tree and glossary
    equal to the unfiltered call.
- `ProjectQueryControllerTest`: the filter on a unit-level fixture.
- `McpReadServiceTest`:
  - Both schemas list the `minSeverity` enum and keep it optional.
  - `callTool` passes the value through.
  - An unknown value becomes an invalid-params error.
- `RestQueryGatewayTest`: the query param is sent only when set.
- `QueryCommandsTest`:
  - `--min-severity medium` is passed through.
  - An unknown value is an option error naming `--min-severity`, and the gateway is not called.
- `GatewayQueryControllerContentTest` (or a sibling): the gateway endpoints bind `minSeverity`.

Angular
- `open-issues.spec.ts`:
  - The selector filters to Medium and up, then High.
  - All restores every row.
  - It combines with Hide stale.
  - The badge count is unchanged.
  - The empty state at a severity is distinct from no open issues at all.
- `e2e/open-issues.e2e.ts`: seed a `HIGH`, a `MEDIUM` and a `LOW` issue, choose High, see one row,
  choose All, see three.

## Out of scope

- The per-entity annotations list.
- Filtering by anything but severity.
- Server-side filtering for the SPA page (decision 4).
- Any change to unfiltered behavior.

## Risks

- **Interface change on `QueryGateway`.** Adding abstract methods breaks every implementation,
  including the test doubles. Keeping the old signatures as defaults limits the change to the two
  real gateways and the two stubs.
- **The 400 message lost in-process.** `unwrap` rebuilds a bare `ResponseStatusException` from a
  non-2xx response. Validation therefore throws before any `ResponseEntity` is built, so the
  message survives to the gateway REST caller and `requel` over REST.
- **Three copies of the level list** (the domain enum, `QueryDescriptions.ISSUE_SEVERITIES` for
  MCP and the CLI, the Angular model). The gateway-api and CLI modules don't depend on
  annotation-domain, so they can't share the enum. A unit test asserts `ISSUE_SEVERITIES` equals
  `IssueSeverity.values()` by name.

## AC mapping

| AC | Covered by |
|---|---|
| `minSeverity=MEDIUM` returns `MEDIUM` and `HIGH` only, in severity order; absent returns all | `IssueSeverityIT`, `ProjectQueryControllerTest` |
| An unknown `minSeverity` is refused with an error naming it, on REST, MCP and CLI | `IssueSeverityIT`, `McpReadServiceTest`, `QueryCommandsTest` |
| `getProjectContext` with `minSeverity` filters `openIssues` only | `IssueSeverityIT` |
| Angular filter covered by unit specs and an e2e case | `open-issues.spec.ts`, `e2e/open-issues.e2e.ts` |

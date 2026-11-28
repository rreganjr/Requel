# #507 — Bulk actions on Open Issues: ignore, apply fix, set severity, mark advisory

https://github.com/rreganjr/Requel/issues/507

Branch `507-bulk-issue-actions` from `release/2.2`, PR to `release/2.2`. Size 3.

## Summary

Act on many open issues at once from the Open Issues page. A server-side `BulkResolveIssues`
command takes a project, up to 500 issue ids and an action, and runs each issue through the same
command a single resolution uses. It reports a per-issue outcome. The page gets row checkboxes, a
bulk toolbar, a confirmation and a result summary.

Review against the tree: still needed, and the ACs stand. Since the ticket was written, #468
added two automatic fixes, `SetStepActorPosition` and `AddActorToUseCasePosition`. Decision 2
includes them.

## Locked decisions

From the ticket (planning, 2026-11-17):

1. All four actions: Ignore, Apply suggested fix, Set severity and Mark advisory.
   `BulkResolveIssues` is a server-side command, so MCP and the CLI can use it.

Settled in this review:

2. **"Suggested fix" means any model-changing position.** That is every position type with its
   own resolve command: the ticket's six, #468's two, and future ones such as #515's domain
   option. Ignore and plain positions never count. In code: `PositionTypes.typeNameOf(position)`
   is neither `Position` nor `IgnorePosition`. An issue with exactly one such position gets it
   applied; with more than one it is `SKIPPED_AMBIGUOUS`; with none it is
   `SKIPPED_NOT_APPLICABLE`.
3. **One transaction per issue.** Every command runs in one transaction (`DefaultCommandHandler`
   is `REQUIRED`). So `BulkResolveIssues` does not call the per-issue commands inside its own
   transaction: each issue's command (`ResolveIssue`, `EditIssue`) goes through the full handler
   chain (authorization, validation, analysis, auditing, lock retry) in a `REQUIRES_NEW`
   transaction. One issue failing never rolls back another.
4. **Checked before anything runs.** A missing id, an id that isn't an issue, or an issue from
   another project fails the whole request before any issue is touched. Over 500 ids, or a
   severity missing or given for the wrong action, also fails it.
5. **Outcomes.**
   - `DONE`.
   - `SKIPPED_NOT_APPLICABLE`: no Ignore position, no fix position, or the issue is already
     resolved.
   - `SKIPPED_AMBIGUOUS`.
   - `REFUSED`: an authorization failure from the per-issue command.
   - `FAILED`: any other exception, with the reason (a stale version, an entity deleted
     meanwhile).

   Set severity to the severity an issue already has, or Mark advisory on an issue already
   advisory, is still `DONE`: nothing is wrong, and the user asked for that state.
6. **The CLI uses the generic runner:**
   `requel run BulkResolveIssues --input '{"projectName":...,"issueIds":[...],"action":"IGNORE"}'`.
   That is the same path as every other write. No dedicated subcommand.

## Contract

- **Input:** `BulkResolveIssuesInput(String projectName, List<Long> issueIds, String action,
  String severity)`.
  - `action` is `IGNORE`, `APPLY_FIX`, `SET_SEVERITY` or `MARK_ADVISORY`.
  - `severity` is required for `SET_SEVERITY` (LOW, MEDIUM or HIGH) and refused for the others.
  - `issueIds`: 1 to 500 ids, duplicates collapsed.
- **Result:** `BulkResolveIssuesResultDto(String action, int done, int skipped, int refused,
  int failed, List<BulkIssueOutcomeDto> outcomes)`.
- **Per issue:** `BulkIssueOutcomeDto(Long issueId, String outcome, String reason, Long positionId)`.
  `positionId` is the position applied, for `IGNORE` and `APPLY_FIX`.
- **Command:** registered with the command factory as `BulkResolveIssues`, so it is served at
  `POST /api/commands/BulkResolveIssues`, over the gateway and MCP (added to the gateway
  policy's allowed commands), and by `requel run`.
- **Per-issue mapping:**
  - IGNORE → `ResolveIssue` with the issue's Ignore position;
  - APPLY_FIX → `ResolveIssue` with its single fix position;
  - SET_SEVERITY → `EditIssue` with the issue's text, required flag and the new severity;
  - MARK_ADVISORY → `EditIssue` with `mustBeResolved=false`.

## Steps

1. The DTOs in service-api.
2. `BulkIssueResolver` (service-impl). It loads and checks all the ids first (decision 4), then
   for each issue builds the per-issue command through the command factory and runs it via the
   handler chain inside a `TransactionTemplate` with `REQUIRES_NEW`, mapping exceptions to
   outcomes.
3. Register `BulkResolveIssues` in `AnnotationCommandRegistrar` (an `ApiCommand` that delegates
   to `BulkIssueResolver` and returns the result DTO), add it to `GatewayPolicyConfig`'s allowed
   list, and describe it on its input with `@CommandDescription` (the MCP catalog reads it).
4. **Angular:**
   - `IssueService.bulkResolve`;
   - on the Open Issues page:
     - row checkboxes and select-all on the current page;
     - a bulk toolbar shown while something is selected: Ignore, Apply suggested fix, Set
       severity with a picker, Mark advisory, and the selected count;
     - a confirmation naming the action and the count;
     - a result summary (done / skipped / refused / failed), expandable to the issues;
     - the list reloaded and the selection cleared after a run.
5. User guide: the Open Issues section in issues.html.

## Test plan

- **`BulkResolveIssuesIT`** (requel-app, H2):
  - **AC1:** five spelling issues, two with one suggestion and three with several. Apply fix
    fixes the two and skips three as ambiguous.
  - **AC2:** Ignore on lexical issues and on an issue without an Ignore position (standing in
    for an AI issue) resolves the first with Ignore and skips the second. That the ignored word
    isn't raised again on re-analysis is #320's behaviour of the Ignore resolution, which the
    bulk action reaches through ResolveIssue; #320's own tests cover it. The IT checks the issue
    was resolved by its Ignore position.
  - **AC3:** Set severity and Mark advisory change every selected issue, as the open-issues read
    shows.
  - **AC4:** a user without Edit on one target entity gets `REFUSED` for that issue and `DONE`
    for the rest. That checks the per-issue transactions.
  - **AC5:** an id from another project fails the request, and nothing changes.
  - **Bounds:** over 500 ids, a missing or wrong severity, an unknown action.
- **AC6:** every `BulkResolveIssuesIT` case runs over the in-process gateway, the path MCP and
  `requel run` take. `requel run` passes any command's input through unchanged and is already
  tested, so it gets no new test. The MCP catalog lockstep IT covers MCP, and `CommandGatewayIT`
  checks the new allowlisted command is authorizable.
- Open Issues spec:
  - selection, select-all on the page, the toolbar showing and hiding;
  - the confirmation text, the request sent, the summary counts and the expandable list;
  - the reload and the cleared selection afterwards;
  - axe.
- e2e `bulk-issues-507`: select two issues, Mark advisory, confirm; the summary says 2 done and
  the list reflects it.

## Out of scope

From the ticket: choosing a specific position across many issues, bulk delete, and bulk actions
inside an entity's annotations section.

## Risks

- **Nested transactions.** `REQUIRES_NEW` inside the command's own transaction holds a second
  connection while each issue runs. That costs one extra pooled connection per bulk request,
  for one issue at a time. The outer transaction writes nothing.
- **Analysis.** Each per-issue command still triggers `AnalysisInvokingCommandHandler`. 500
  issues means up to 500 analysis passes. That matches what resolving them one by one does,
  and stays bounded by the cap.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| Five spelling issues: two fixed, three skipped as ambiguous | `BulkResolveIssuesIT` |
| Ignore resolves lexical issues, skips ones without Ignore; not raised again on re-analysis | `BulkResolveIssuesIT` |
| Set severity and Mark advisory change every selected issue; the list reflects it | `BulkResolveIssuesIT`, Open Issues spec, e2e |
| No Edit on one target → REFUSED for it, DONE for the rest | `BulkResolveIssuesIT` |
| An issue from another project fails the request without changing anything | `BulkResolveIssuesIT` |
| Works over the gateway and the CLI with the same results | `BulkResolveIssuesIT` (gateway), `CommandGatewayIT`, MCP lockstep |

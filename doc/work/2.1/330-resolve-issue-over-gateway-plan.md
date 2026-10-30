# #330 — Allow ResolveIssue over the MCP gateway

https://github.com/rreganjr/Requel/issues/330

Branch `330-resolve-issue-over-gateway` from `release/2.1`, PR to `release/2.1`. Size 2.

## Summary

`ResolveIssue` is on the gateway's denylist, so an agent working through MCP or the CLI can read,
create and edit issues but not resolve one. Its denylist reason (not independently authorized)
stopped being true with #305/#312. This allows it, and fixes three gaps found reading the
resolve path, which matter more once it is reachable from the gateway.

## Locked decisions (2026-10-30)

1. **The position must belong to the issue.** Nothing checks it today, in the UI either: any
   position id resolves any issue, another project's included, and a position whose type doesn't
   fit the issue (Add as Actor on a non-lexical issue) crashes the resolver's cast with a 500.
   `ResolveIssueCommandImpl` refuses a position not among the issue's positions with an
   `EntityValidationException` on `positionId` (INVALID_INPUT; 422 on REST). Every resolver
   subclass validates through it, so UI and gateway both get the check.
2. **`projectName` is required and checked.** The builder ignores it today (the UI already sends
   it). The issue must be in that project, refused like #342's other annotation commands
   (`requireSameScope`).
3. **Add as Actor / Add to Glossary's nested commands are authorized as the caller**:
   `EditActor` / `EditGlossaryTerm` run through the handler chain, so the caller needs
   `Actor[Edit]` / `GlossaryTerm[Edit]` as well as `Annotation[Edit]`, and is refused as a whole
   without them, like Fix Spelling. A test confirms it; if the nested command turns out to skip
   the check, that is fixed here.
4. **`ResolveIssueInput` gets a `@CommandDescription`** (the gateway catalog needs one for every
   allowed command) and field constraints, saying what accepting each position type does. The
   `EditIssueInput` sentence "Issues are resolved in the Requel UI, not through the gateway"
   changes to point at `ResolveIssue`.

## Changes

- `GatewayPolicyConfig`: `ResolveIssue` moves from `DENIED` to `ALLOWED`; the denylist comment
  keeps the other annotation commands and says why.
- `ResolveIssueInput`: description; `@NotBlank projectName`, `@NotNull issueId`, `@NotNull
  positionId`.
- `AnnotationCommandRegistrar` (ResolveIssue builder): check the issue is in `projectName`.
- `ResolveIssueCommandImpl.validate()`: the position-belongs-to-issue check.
- `EditIssueInput` description.
- The MCP/gateway catalog expectations that list allowed or denied commands.

## Test plan

`tmp/330-verify.sh`: `mvn clean verify -pl modules/coverage-report -am` + the coverage check.

- `ResolveIssueGatewayIT` (new, gateway as a user): a user with `Annotation[Edit]` resolves an
  issue with each position type (Ignore, Fix Spelling, Add to Dictionary, Add as Actor, Add to
  Glossary) and the result matches what the REST command does; without `Annotation[Edit]` it is
  refused; Fix Spelling on an entity the caller can't edit is refused as a whole; Add as Actor
  without `Actor[Edit]` is refused and creates nothing; a position from another issue is
  INVALID_INPUT on `positionId`; an issue from another project, or an unknown one, is refused;
  the other denylisted annotation commands are still refused; the audit row carries the client.
- `AuthorizationIT`: a REST resolve with a position from another issue is 422.
- Gateway policy / MCP catalog tests updated for the move.

## Out of scope

- Unresolving an issue, and resolving one that is already resolved (it re-resolves, as today).
- The other denylisted annotation commands.

## AC mapping

| AC | Covered by |
|---|---|
| Resolve with each position type over MCP, matching the UI | `ResolveIssueGatewayIT` |
| No `Annotation[Edit]` refused; Fix Spelling all-or-nothing | `ResolveIssueGatewayIT` |
| A position not on the issue is a field error, not a 500 | Decision 1; both ITs |
| `runCommand` works; other denylisted commands still refused | `ResolveIssueGatewayIT`, catalog tests |
| `AuthorizationIT` / gateway policy tests cover the move | `AuthorizationIT`, catalog tests |

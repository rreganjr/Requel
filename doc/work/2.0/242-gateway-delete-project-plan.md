# 242 — Expose DeleteProject on the gateway (MCP + CLI)

Epic #239 · child #242, the last open child (#240 backend command, #241 UI, #247 cascade hardening
are merged). Surfaces the existing `DeleteProject` command through the write gateway: the typed MCP
tool set, the generic `runCommand`, and the `requel-cli` command list. Backend-only Java work.

## Summary

Everything downstream of the allowlist is already derived from it, so the production change is one
entry in one `Set<String>`. The work is really: make the exposure change, then prove the four ACs
with tests that will keep holding.

## Verified against `release/2.0` (48aed5d4)

The issue's file list was written before #240 merged; here is what the tree actually does now.

- **`GatewayPolicyConfig.ALLOWED`** (`modules/service-impl/.../service/gateway/GatewayPolicyConfig.java`)
  is the single curated allowlist. `DENIED` wins over it; `DefaultCommandPolicy` is default-deny;
  `NonUserStakeholderDeletePolicy` decorates it and only engages for `DeleteStakeholder`.
- **`GatewayCommandCatalogImpl`** is *derived*: it iterates `GatewayPolicyConfig.ALLOWED`, skips
  anything not in the `CommandRegistry`, and takes the input DTO from `ApiCommandFactory`. So there
  is **no hand-listed descriptor to add** — the draft plan's open question is settled.
- **MCP typed tools** are generated from the catalog in `McpWriteService.toolDescriptors()`, gated on
  `requel.gateway.write.enabled` (default false); `call()` also rejects every write tool when the
  flag is off.
- **`requel-cli`** builds its command list from `RestGatewayCatalog.descriptors()` (the REST
  `/api/gateway/commands/descriptors` endpoint over the same catalog). No CLI-side list to edit;
  `CommandsCommandTest` stubs the catalog, so no CLI test needs changing either.
- **`DeleteProject`** is registered in `ProjectCommandRegistrar` with input
  `DeleteProjectInput { @NotBlank String projectName, Integer version }`, and
  `DeleteProjectCommandImpl implements AuthorizableCommand` returning
  `RequiresStakeholderPermission(Project.class, "Delete")`.
- **Existing lockstep guards already generalise over the set**: `GatewayCommandCatalogImplTest`
  (catalog == ALLOWED), `McpToolCatalogLockstepIT` (typed tools == catalog ⊆ ALLOWED),
  `CommandGatewayIT.everyAllowlistedCommandIsRegisteredAndAuthorizable` (every allowlisted command is
  registered and `AuthorizableCommand`), `allowAndDenyListsAreDisjointAndIdentitySafe`.
  `McpWriteCatalogLockstepTest`/`McpWriteServiceSchemaTest`/`McpToolNamingTest` run against the
  `McpTestCatalog.sample()` fixture, so they only cover `DeleteProject` if we add it there.

## Locked decisions

- One-line exposure change: add `"DeleteProject"` to `ALLOWED` under the existing `// Project`
  section. No new mechanism, no descriptor table, no CLI edit.
- **No input-aware policy guard.** Whole-project delete is governed solely by `Project[Delete]`,
  enforced at the command layer (#240). The gateway decides *exposure*, not authorization.
- **Not added to `DENIED`.** It is project-scoped, not identity or file-transfer.
- Stays behind `requel.gateway.write.enabled` like every other write — no special-casing.
- **No export-first over MCP** (that is #241's UI affordance) and no bulk delete — per the issue.

## Changes

1. `modules/service-impl/.../service/gateway/GatewayPolicyConfig.java` — `ALLOWED` gains
   `"DeleteProject"` next to `"EditProject"`, with a short comment that whole-project delete is
   exposed and governed by `Project[Delete]` at the command layer.
2. `modules/mcp-server/src/test/.../McpTestCatalog.java` — add `DeleteProject` →
   `DeleteProjectInput` to `sample()`, so the unit-level lockstep, schema and tool-naming tests
   exercise the real destructive command rather than only edit-shaped ones.
3. `modules/mcp-server/src/test/.../McpWriteCatalogLockstepTest.java` — a schema case for
   `DeleteProject`: properties exactly `projectName` + `version`, `required == [projectName]`,
   `additionalProperties == false`.
4. `modules/mcp-server/src/test/.../McpWriteServiceTest.java` — write-flag coverage aimed at the
   destructive tool: with the flag off, `DeleteProject` is absent from `toolDescriptors()` and
   `call("DeleteProject", …)` throws `McpInvalidParamsException`; with it on, the typed tool
   forwards `projectName`/`version` to the gateway unchanged.
5. `modules/requel-app/src/test/.../mcp/McpToolCatalogLockstepIT.java` — assert `DeleteProject` is
   explicitly present in the real wired typed-tool set and on the real allowlist (the existing
   assertions are set-general; this pins the ticket's AC).
6. `modules/requel-app/src/test/.../service/CommandGatewayIT.java` — two dispatch tests:
   - a user holding `Project[Delete]` deletes a project through the gateway and it is gone;
   - `editorUsername` (who holds `Project[Edit]` but **not** `Project[Delete]`) is rejected — the
     policy allows the type, so the failure comes from the command layer's authorization — and the
     project still exists afterwards.

7. `modules/mcp-server/src/test/.../WriteInputDtoConstructionTest.java` — add `DeleteProjectInput`
   to the hand-maintained list of allowlisted write input DTOs whose canonical constructor and
   generated members are pinned.

   **Fixture note:** the class fixture is `@BeforeAll` / `PER_CLASS` and every other test in the
   file shares `projectName`. Each DeleteProject test therefore creates its own throwaway project
   (+ a stakeholder with the permissions under test) inside the test method, so test order can never
   let a delete pull the shared fixture out from under the rest of the class.

## Test plan (gate: `mvn clean verify`, script `tmp/242-verify.sh`)

`mvn clean verify` is the whole gate — no `requel-angular/**` change, so no vitest/tsc/e2e.
Focused runs while iterating:

```bash
mvn -pl modules/mcp-server test -Dtest='McpWrite*Test,McpToolNamingTest'
mvn -pl modules/requel-app -am test -Dtest='McpToolCatalogLockstepIT,CommandGatewayIT,GatewayCommandCatalogImplTest'
```

## AC mapping

| AC | Covered by |
| --- | --- |
| Writes on → typed MCP tool + `runCommand` type + CLI list, and it deletes | `McpToolCatalogLockstepIT` (tool + allowlist), `GatewayCommandCatalogImplTest` (catalog == ALLOWED, which is what `/descriptors` and the CLI read), `CommandGatewayIT` delete-succeeds test |
| Writes off → not offered, not dispatchable | `McpWriteServiceTest` flag-off case (absent from `toolDescriptors()`, `call()` rejected) |
| Caller lacking `Project[Delete]` rejected at the command layer, and audited | `CommandGatewayIT` authorization-rejection test (policy allows, command layer refuses, project survives); the audit row is the `AuditingCommandHandler` chain #240 already exercises in `AuthorizationIT`/`DeleteProjectIT` |
| Lockstep + schema tests pass with `DeleteProject`; name matches `^[a-zA-Z0-9_-]{1,64}$` | `McpWriteCatalogLockstepTest` (incl. new schema case), `McpToolNamingTest`, `WriteInputDtoConstructionTest` — all via `McpTestCatalog` / the DTO list |

## Out of scope

Bulk deletion; export-before-delete (#241, UI); any change to #240's command semantics; MCP-side
confirmation prompts.

## Risks

- **This makes an irreversible whole-project delete reachable by an external agent** whenever writes
  are enabled. Mitigations are the ones already in place: the flag defaults off, `Project[Delete]`
  is checked per stakeholder at the command layer, the call is audited with the triggering user, and
  the optimistic-lock `version` is honoured. Worth stating plainly in the PR body.
- Adding `DeleteProject` to `McpTestCatalog.sample()` widens what every test using that fixture
  asserts (`writeToolsListedWhenEnabled`, naming, schema) — expected, and the point of the change.
- `CommandGatewayIT` is a shared-context IT: the new tests must not assume ids or an empty table,
  and must not delete the shared fixture project (see fixture note above).

## Process (CLAUDE.md)

Branch `242-gateway-delete-project` off `release/2.0`; `tmp/242-verify.sh` → `mvn clean verify`;
`commit.md` with `Closes #242`; `pr.md` for the PR body; the developer runs every `git`/`gh` command.

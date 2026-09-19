# [Epic] Delete Project — backend command, UI action, and MCP gateway tool

## Problem

Requel has no way to delete a project. The gateway exposes `Delete*` for every *child* entity
(goals, stories, actors, use-cases, scenarios, stakeholders, glossary terms, annotations, tags),
but there is no `DeleteProject`: no domain command, no repository/service deletion path, and no
Angular UI action. `EditProjectCommandImpl` creates and edits; nothing removes.

The gap is visible now: `listProjects` returns 375 projects, ~359 of them auto-generated E2E test
fixtures ("E2E Test Org") that can never be cleaned up through the app. Today the only way to purge
a project is a hand-written MySQL delete across every child + audit table — error-prone and off the
audited command path.

## Goal

Add first-class project deletion across all three layers, on the audited CQRS command path, with the
same authorization and audit guarantees as every other mutation — and make it safe by offering an XML
export (backup) before the destructive step.

## Scope (child issues)

1. **Backend `DeleteProject` command** — new domain command + JPA impl with full child cascade,
   project-level authorization, optimistic locking, and audit. The foundation; UI and MCP depend on it.
2. **UI delete action with export-first prompt** — a Delete control in the project workspace/list, a
   PrimeNG ConfirmDialog, and an offer to download the project's XML export before deleting.
3. **Expose `DeleteProject` on the gateway (MCP + CLI)** — add to the command allowlist and typed
   catalog so the tool appears (writes opt-in), keeping the MCP write-catalog lockstep test green.

## Cross-cutting notes

- **Export already exists and round-trips.** `GET /api/projects/{name}/export`
  (`ProjectXmlController` → `ExportProjectCommand`, Angular `ProjectService.downloadProjectXml`) writes
  the project as XML, and `ImportProject` restores it. So "export before delete" is a backup with a
  real restore path — the UI just calls the existing download before dispatching the delete; no new
  export code needed.
- **Authorization pattern to mirror.** `EditProjectCommandImpl.getAuthorizationRequirement()` returns
  `RequiresStakeholderPermission(Project.class, "Edit")`. `DeleteProject` should require a
  `Project.class` `"Delete"` permission (add the permission if it doesn't exist) and run through
  `AuthorizingCommandHandler` like every other command.
- **Gateway is opt-in and audited.** Writes appear only with `requel.gateway.write.enabled=true`
  (`McpWriteService`), and every call already produces an MCP-call audit row plus a command-audit row.

---

# Child 1 — Backend: `DeleteProject` command with child cascade, auth, and audit

## What exists today

- `EditProjectCommandImpl` (`modules/project-jpa/.../command/EditProjectCommandImpl.java`) creates and
  edits projects; `getAuthorizationRequirement()` → `RequiresStakeholderPermission(Project.class,
  "Edit")`.
- Child deletes exist and extend `AbstractEditProjectCommand`: `DeleteGoalCommandImpl`,
  `DeleteStoryCommandImpl`, `DeleteActorCommandImpl`, `DeleteUseCaseCommandImpl`,
  `DeleteScenarioCommandImpl`, `DeleteScenarioStepCommandImpl`, `DeleteGlossaryTermCommandImpl`,
  `DeleteStakeholderCommandImpl`, `DeleteReportGeneratorCommandImpl`, and the annotation deletes
  (`DeleteNote/Issue/Position/Argument`).
- No `DeleteProjectCommand` interface, impl, factory method, repository delete, or command-registry
  entry anywhere (`find modules -iname '*DeleteProject*'` → nothing in main).

## Work

- Add `DeleteProjectCommand` (interface in `project-domain`, impl in `project-jpa` extending the
  project command base), a `ProjectCommandFactory.newDeleteProjectCommand()`, and register it.
- Cascade-delete every project-owned entity so no orphan or FK-violating rows remain: stakeholders
  (user and non-user), goals + goal relations, stories, actors, use-cases, scenarios + scenario steps,
  glossary terms, report generators, annotations (notes/issues/positions/arguments), tags + tag
  assignments, and the project's `command_audit_log` / MCP-call-audit rows as appropriate. Prefer JPA
  cascade / orphan-removal on the aggregate where it already exists; add explicit repository deletes
  where it doesn't. (Audit the actual FK graph before implementing — the count of child tables is the
  real scope here.)
- Authorization: `getAuthorizationRequirement()` → `RequiresStakeholderPermission(Project.class,
  "Delete")`. Add the `"Delete"` project permission to the role/permission model if absent.
- Optimistic locking: honor the project's `@Version` (reject a stale delete the way edits do).
- Emit the command through `AuthorizingCommandHandler` so it audits and (if applicable) publishes the
  `Project:0` sidebar broadcast for a removed project.

## Acceptance criteria

- `DeleteProject` deletes the project and every child entity in one transaction; re-`listProjects`
  no longer returns it and no orphaned child/annotation/tag/audit rows remain.
- A user without the `Project[Delete]` permission is rejected exactly as other unauthorized commands
  are; the deletion does not occur.
- A stale `version` is rejected with the standard optimistic-lock error; nothing is deleted.
- The delete produces a command-audit record attributed to the acting user.
- Deleting a non-existent / already-deleted project id returns a clean not-found error, not a 500.
- Integration test covers: full cascade (assert child tables emptied for that project), auth denial,
  and stale-version rejection.

## Not in scope

- UI and MCP exposure (child issues 2 and 3).
- Soft-delete / trash-and-restore — this is a hard delete; the XML export (child 2) is the backup path.
- Bulk / multi-project delete.

---

# Child 2 — UI: Delete Project action with export-first backup prompt

## What exists today

- No `deleteProject` / `removeProject` in `requel-angular/src` (grep → none).
- Project export is wired: `ProjectService.getExportUrl()` and `downloadProjectXml()`
  (`core/project.service.ts`) hit `GET /api/projects/{name}/export`; import via
  `ProjectService.importProject()` (`ImportProject`) is already surfaced in `project-list.ts` through
  `FileUploadButtonComponent`.
- Destructive-confirmation convention: PrimeNG `ConfirmDialog` (see UI/UX review finding 4.5).

## Work

- Add a **Delete** action for a project (project workspace header and/or the project list row/toolbar),
  visible only when the user has `Project[Delete]`.
- On activation, open a confirmation flow that (a) names the project and warns the delete is permanent,
  and (b) **offers to export first**: an "Export a backup before deleting" option that calls
  `downloadProjectXml()` and waits for the download to resolve before proceeding. Default the toggle to
  on; deletion proceeds only on explicit confirm.
- Require a deliberate confirm for a populated project — e.g. type-to-confirm the project name, or a
  clearly-worded ConfirmDialog with an explicit "Delete permanently" accept label and a Cancel default.
- Dispatch the `DeleteProject` command via `CommandService`; on success, route away from the deleted
  project (back to the project list) and surface a success toast; on failure, show an inline error.
- Accessibility: real `<button>`, ConfirmDialog with `aria-modal` + labelled title + focus return
  (consistent with the epic's a11y direction), and an accessible name that includes the project.

## Acceptance criteria

- A permitted user can delete a project from the UI; it disappears from the list and the sidebar
  without a manual refresh (SSE `Project:0` broadcast).
- The confirm step offers an export-first backup that downloads a valid XML file (re-importable via the
  existing Import flow) before the delete runs; declining it still allows the delete.
- Deletion requires an explicit, deliberate confirmation; Cancel/Escape aborts with nothing deleted.
- Users without `Project[Delete]` never see the action.
- A backend failure (auth, stale version) shows a clear inline error and leaves the project intact.
- Playwright e2e: export-first-then-delete happy path, and cancel-aborts path.

## Not in scope

- New export/import code — reuse `downloadProjectXml()` / `ImportProject` as-is.
- Restore-from-trash UI (no soft delete).

---

# Child 3 — Expose `DeleteProject` on the gateway (MCP + CLI)

## What exists today

- Gateway policy + catalog live in `modules/gateway-api/.../DefaultCommandPolicy.java`,
  `CommandPolicy.java`, `GatewayCommandCatalog.java`; write tools are surfaced by
  `modules/mcp-server/.../McpWriteService.java`, opt-in via `requel.gateway.write.enabled` (default
  false).
- The typed tool set is derived from the same allowlist the policy enforces, and
  `McpWriteCatalogLockstepTest` asserts the MCP catalog stays in lockstep with it.
- `DeleteProject` is absent from the allowlist/catalog, so no `DeleteProject` MCP tool or CLI command
  exists.

## Work

- Add `DeleteProject` to the gateway command allowlist / `GatewayCommandCatalog` so the generic
  `runCommand` accepts it and a typed `DeleteProject` tool is generated, with its JSON schema derived
  from the command's input DTO (project id/name + `version`).
- Keep the policy the safety boundary: `DeleteProject` is authorized as the acting user's
  `Project[Delete]` permission (child 1); it does not touch user/identity management, so it stays clear
  of the existing user-management denylist. Confirm it is only reachable when writes are enabled.
- Update `McpWriteCatalogLockstepTest` and the write-service schema tests to include `DeleteProject`
  so catalog parity stays enforced.

## Acceptance criteria

- With `requel.gateway.write.enabled=true`, `DeleteProject` appears as both a typed MCP tool and a
  `runCommand` command type (and the `requel-cli` command list), and deletes a project the acting user
  has `Project[Delete]` on.
- With writes disabled, `DeleteProject` is not offered and cannot be dispatched.
- A user lacking `Project[Delete]` is rejected at the command layer (not merely hidden), and the call
  is audited.
- `McpWriteCatalogLockstepTest` and schema tests pass with `DeleteProject` included; the tool name
  matches `^[a-zA-Z0-9_-]{1,64}$`.

## Not in scope

- Bulk delete over MCP.
- A separate "export via MCP before delete" step — export-first is a UI affordance (child 2); an MCP
  client can call the existing export/read tools itself if it wants a backup.

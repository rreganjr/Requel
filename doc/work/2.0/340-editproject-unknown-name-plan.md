# #340 — EditProject creates a project when projectName is misspelled

https://github.com/rreganjr/Requel/issues/340

Patch release: `v2.0.2`. Branch `340-editproject-unknown-name` from `origin/master`, PR to `master`.

## Summary

`EditProjectInput.projectName` selects the project to edit, but the `EditProject` binder in
`ProjectCommandRegistrar` catches `NoSuchProjectException` and leaves the project null, which is
the create path. A caller who misspells the project it meant to edit or rename gets a second
project named `name`, and success. The old project is untouched.

The fix refuses a non-null `projectName` that names no project, the way every other
project-scoped command (`DeleteProject`, `EditGoal`, ...) already does. Creating stays
`projectName` null.

The Angular editor already sends `projectName: null` on create and the name it loaded with on
edit, and the e2e `createProject` helper sends null, so nothing in the UI or e2e changes. No test
creates a project through a non-null `projectName`.

## Locked decisions

1. **Same error as every other project command.** An unknown `projectName` throws
   `NoSuchProjectException` from the binder. Over REST that is **409 CONFLICT** (`CommandController`
   maps `EntityException`); at the gateway it is **EXECUTION_ERROR** (MCP `INTERNAL_ERROR`, CLI
   the same through `RestCommandGateway`). A real NOT_FOUND for unknown names would change the
   error kinds callers get back from every command, which is an API contract change: added to
   #345 (v2.1). The issue's AC is reworded from "not-found error" to "refused, creates nothing".
2. **`id` stays, still ignored.** Removing it from `EditProjectInput` changes the API, which a
   patch release may not do (`doc/guides/RELEASE_PROCESS.md`, "Hotfixes"). The Angular editor
   keeps sending it. Removal is added to #345. The issue's `id` AC is reworded to match.
3. **Patch-safe.** No change to any input record's fields, `project.xsd`, configuration or
   Flyway. Only behavior on an input that was wrong to accept, and the description text.

## Contracts

- `projectName` null → create, as today (`createProjects` role permission).
- `projectName` names a project → edit or rename it, as today (`Project[Edit]`). `name` is the
  new name; a rename is `projectName` = current name, `name` = new name.
- `projectName` non-null and names no project → `NoSuchProjectException` from the binder; nothing
  is created. Lookup is `findProjectByName`, which since #398 matches with `=` (case-insensitive
  under the MySQL collation), as given and then trimmed.

## Step by step

1. **`ProjectCommandRegistrar` `EditProject` binder** (`service-impl`): drop the `try/catch` around
   `findProjectByName` and the "otherwise leave null for create" comment, so a miss propagates.
2. **`EditProjectInput`** (`service-api`):
   - Javadoc: `projectName` null creates; a name that matches no project is refused.
   - `@CommandDescription`: replace "when it is null or names no project, a new project is
     created, so a misspelled projectName creates a project rather than failing" with "null
     creates a new project; a projectName that names no project is refused". To rename, pass the
     current name as projectName and the new one as name. Keep "The id field is ignored." and the
     `authorization` string as written.

## Test plan

`tmp/340-verify.sh` runs `mvn clean verify` (backend only; no `requel-angular/**` change).

- **`AuthorizationIT`** (REST, through `CommandController` and the security chain):
  - Admin sends `EditProject` with a misspelled `projectName` → 409, and
    `findProjectByName(name)` still throws: no project created. Admin holds `createProjects`, so
    before the fix this request would have created the project and the test would fail.
  - The editor persona renames its project (`projectName` = current, `name` = new) → 200, the
    project carries the new name and the old name no longer resolves. Rename it back in the test so
    the shared fixture keeps its name.
  - A persona with `createProjects` sends `projectName` null → 200, a project is created.
- **`CommandGatewayIT`** (in-process gateway, the MCP path):
  - Admin (holds `createProjects`) sends `EditProject` with an unknown `projectName` →
    `GatewayException`, no project created. The kind is not asserted; #345 changes it.
  - A stakeholder with `Project[Edit]` renames its project by the correct name → succeeds.
- The CLI goes through `RestCommandGateway` → REST, so the REST cases cover it.
- `McpWriteCatalogLockstepTest` and the description well-formedness checks still pass after the
  description edit.

## Out of scope

- NOT_FOUND as a distinct error kind, and removing `id`: #345 (v2.1), decisions 1 and 2.
- Project lookups by `id`.
- The 2.0.2 version bump. It rides on whichever 2.0.2 PR merges last
  (`doc/guides/RELEASE_PROCESS.md`, "Hotfixes", step 3).

## Risks

- **A script that relied on create-by-unknown-name.** It is refused now. Sending a non-null
  `projectName` to create was never the documented path, and the description warned about it.
- **Name existence leaks before authorization.** The binder runs before
  `AuthorizingCommandHandler`, so a caller without access gets 409 for an unknown name and 403 for
  a real one. Every other project-scoped command already behaves this way; not changed here.

## AC mapping

| Issue AC | Covered by |
|---|---|
| Unknown `projectName` is refused and creates nothing, over REST, MCP and CLI (reworded, decision 1) | `AuthorizationIT`, `CommandGatewayIT`; CLI shares the REST path |
| `projectName` null still creates a project | `AuthorizationIT` |
| `id` stays ignored in 2.0.2; removal moves to #345 (reworded, decision 2) | no code change; #345 |
| (added) A rename by the correct name still works | `AuthorizationIT`, `CommandGatewayIT` |

# 256 — No path to delete projects the caller is not a stakeholder on

Plan for issue #256, milestone v2.0. Decisions below were taken with Ron before implementation;
where the ticket as filed disagrees with what the code actually does, the correction is recorded
here and the ticket body should be updated to match.

## What the ticket got wrong

**Option 2's forward half is already implemented.** There are exactly two `new ProjectImpl(...)`
call sites in main code, and both already add the creator as a `UserStakeholder` and grant every
row returned by `findAvailableStakeholderPermissions()`:

- `EditProjectCommandImpl.createProject()` (also adds an `assistant` stakeholder)
- `ImportProjectStreamingCommandImpl.addUserAsStakeholder()` (grants any missing permission)

The E2E fixtures are not a third path: `e2e/fixtures/api-helper.ts#createProject` posts
`EditProject` with the admin token, so it runs the first path above. So "make every project-creating
path add the creator" has no forward-looking work left in it — only the historical rows are broken.

**A backfill already exists, but not one that can help.**
`StakeholderPermissionsInitializer.backfillProjectDeletePermission()` (added in #245) runs every boot
and grants `Project[Delete]` to every `UserStakeholder` already holding `Project[Edit]`. It cannot
help a project where the user has no stakeholder row at all, which is the shape of the 377.

**AC 3 is no longer verifiable.** The `e2e-*` and `Imported Project (2..30)` rows were removed with
hand-written SQL before work started, so "can be removed without hand-written SQL" cannot be
demonstrated. Replaced below.

## Decisions

1. **Narrow admin capability.** A system administrator gains exactly one new power: delete a whole
   project without holding a stakeholder row on it. Admin does *not* become a project superuser.
   This preserves the invariant `AuthorizationIT` pins today — "SystemAdminUserRole does NOT grant
   project access; admin needs Goal[Edit]" — and its `adminWithoutGoalEditPermissionCannotEditGoal`
   test passes unchanged.
2. **Only the Delete affordance is special-cased in the UI.** `my-permissions` keeps returning
   `isStakeholder: false` for a non-member admin and carries a `{Project: [Delete]}` map.
   `isStakeholder` has no UI consumer (only the `PermissionService` getter and the model field) and
   `hasPermission()` does not gate on it, so exactly the Delete control lights up.
3. **The backfill ships as an admin-triggered maintenance command**, not a startup initializer or a
   Flyway migration.
4. **Nothing in this ticket touches the `assistant` stakeholder.** All assistant work — including
   restoring a missing assistant row — belongs to #302. The repair command here deals only with the
   project's creator.

## Not a security boundary relaxation beyond its stated scope

The server still authorizes every command. What changes is that one requirement type can be
satisfied two ways, and only `DeleteProjectCommandImpl` declares it. No other command's
authorization changes.

## Part A — admin can delete any project

**A1.** `AuthorizationRequirement` (platform-identity, sealed): add a fourth record

```
RequiresStakeholderPermissionOrSystemRole(Class<?> entityType, String permissionType,
                                          Class<? extends Role> roleType)
```

**A2.** `AuthorizingCommandHandler`: handle the new variant — pass when `user.hasRole(roleType)`,
otherwise run the identical stakeholder check. Extract the existing `RequiresStakeholderPermission`
body into one private method both branches call, so the two cannot drift.

**A3.** `DeleteProjectCommandImpl.getAuthorizationRequirement()` returns the new variant with
`SystemAdminUserRole.class`. project-jpa already imports that class (`ExportProjectCommandImpl`), so
no new module dependency. Javadoc updated.

**A4.** `ProjectQueryController.callerHoldsProjectDelete()` returns true for admin. This feeds
`ProjectDto.canDelete`, which drives the Delete item in `project-list.ts`. Its javadoc claims it
"mirrors the gate enforced by the DeleteProject command" — that claim only stays true if this
changes with A3.

**A5.** `ProjectQueryController.getMyPermissions()` adds `Project -> {Delete}` for an admin,
including in the non-stakeholder branch that currently returns an empty map. This feeds
`PermissionService`, which drives `project-workspace.ts`'s `canDelete()`.

Three mirrors, one behavior. A2 enforces, A4 and A5 advertise. Missing either advertiser leaves
admin with no button; missing the enforcer leaves admin with a button that 403s.

## Part B — `RepairProjectStakeholders` maintenance command

**B1.** `RepairProjectStakeholdersCommand` (project-domain) + `Impl` (project-jpa).
Auth: `RequiresSystemRole(SystemAdminUserRole.class)`. For each project — or one named project —
resolve `getCreatedBy()`; if it is a non-null Requel user, ensure a `UserStakeholder` exists for
them and grant every available permission not already held. Idempotent. Projects whose `createdBy`
is null or resolves to no user are counted and logged, not failed. The `assistant` stakeholder is out
of scope entirely (decision 4) — see #302, which owns the question of what permissions the assistant
should hold and will bring `createProject()`, the import path and this command into line together.

**B2.** Factory method on `ProjectCommandFactory` / `ProjectCommandFactoryImpl`.

**B3.** `RepairProjectStakeholdersInput` in service-api carrying an optional `projectName`
(null = every project), annotated with `@CommandDescription` (#255).

**B4.** Registered in `ProjectCommandRegistrar` with a result mapper returning counts:
projects scanned, stakeholders created, permissions granted, projects skipped.

**B5.** Added to `GatewayPolicyConfig.DENIED`. The denylist's own stated rationale is
"identity / user management — out of scope by design" and `EditUserStakeholder` already sits there;
a command that creates stakeholder rows and grants permissions is that same category. Denying it
also keeps it off the MCP tool surface, so `McpToolCatalogLockstepIT` needs no change. Admin runs it
against `/api/commands/RepairProjectStakeholders` with an admin token.

### B6 — the admin UI

A new **Maintenance** page under the existing admin area, not a button bolted onto an existing page.
The three admin pages today are each *about* an entity you browse (users, global tags, tag
categories); repairing stakeholders is an action, not a listing, and it has no entity of its own. A
page named for maintenance also gives #302's follow-ups and any future repair somewhere obvious to
land instead of growing a bespoke corner of the Users page.

- **Route** — one more entry in `features/admin/admin.routes.ts`, matching the shape of the existing
  four: `path: 'maintenance'`, `canActivate: [adminGuard]`, `title: 'Maintenance'`,
  `data: routeData({ section: 'admin', breadcrumb: 'Maintenance' })`, lazy `loadComponent`.
  `adminGuard` is the same gate the other admin routes use — the command's own
  `RequiresSystemRole(SystemAdminUserRole.class)` remains the real enforcement; the guard only keeps
  the page out of the nav for non-admins.
- **Nav** — one more `<a class="sidebar-link">` inside the existing `@if (isAdmin())` Admin accordion
  panel in `shared/sidebar-nav.ts`, after Tag Categories: `routerLink="/maintenance"`,
  `aria-label="Run maintenance tasks"`, icon `pi pi-wrench`. No spec change — `sidebar-nav.spec.ts`
  does not enumerate the admin links.
- **Page** — `features/admin/maintenance.ts`, standalone + `OnPush`, following `global-tags.ts`'s
  shape (`PageHeaderComponent`, `ButtonModule`, `InputText`, `SubmitErrorComponent`,
  `MessageService`). One card per task; one task for now, "Repair project stakeholders":
  - a one-line description of what it does,
  - an optional project-name input — blank means every project,
  - a Run button, disabled while in flight,
  - a `ConfirmationService` confirm **only** when the name is blank, since that writes across every
    project (the house pattern for destructive/bulk actions — see `goal-editor.ts` et al.),
  - the result rendered on the page, not only as a toast: projects scanned, stakeholders created,
    permissions granted, projects skipped. A toast alone disappears before an admin has read four
    numbers.
  - errors through `SubmitErrorComponent`, as the other admin pages do.
- **Service** — `core/maintenance.service.ts` with
  `repairProjectStakeholders(projectName?: string)` delegating to `CommandService.execute(...)`,
  mirroring how `tag.service.ts` wraps its commands. A new service rather than a method on
  `ProjectService` so #302's future tasks have the same home as this one.
- **Model** — `models/maintenance.ts` for the result DTO.
- **Specs** — `maintenance.spec.ts` (runs with and without a project name; renders the four counts;
  the button is disabled in flight; a failure surfaces through `SubmitErrorComponent`) and
  `maintenance.a11y.spec.ts`. Both existing admin pages carry an a11y spec, so skipping one here
  would break that convention.

Six new files and two edited ones. No E2E case proposed — `admin.e2e.ts` exists and one could be
added, but the page is thin and fully covered by the unit specs; say so if you want it anyway.

## Tests

- `DeleteProjectIT`: admin holding no stakeholder row on the project deletes it; the audit
  attributes the deletion to that admin.
- `AuthorizationIT`: new case asserting the same admin still gets 403 editing a goal on a project
  they are not a stakeholder of. Pins the narrowness rather than leaving it incidental.
- `GatewayPolicyTest`: `RepairProjectStakeholders` is denied.
- New IT for the repair command: strip a creator's stakeholder row, run as admin, assert it is
  restored with the full permission set; a second run is a no-op; a non-admin caller raises
  `AuthorizationException`.
- `ProjectQueryController` tests: `canDelete` true for a non-member admin; `my-permissions` returns
  `{Project: [Delete]}` with `isStakeholder: false`.
- Angular, Part A: no production change — both delete surfaces are server-driven. One
  `permission.service.spec` case asserting `canDelete('Project')` is true while `isStakeholder` is
  false, so the shape A5 emits is pinned on the client side too.
- Angular, Part B: `maintenance.spec.ts` and `maintenance.a11y.spec.ts` as described in B6.

## Acceptance criteria (replacing the ticket's)

- A system administrator can delete a project on which they hold no stakeholder row, through the
  audited command path.
- The deletion is audited and attributed to that administrator.
- The same administrator still cannot edit entities within a project they are not a stakeholder on.
- An administrator can repair a project whose creator lost their stakeholder row, without
  hand-written SQL, and can do it from the admin UI rather than by hand-rolling an HTTP request.

## Verification before implementing

Create one project and run one import on dev, and confirm neither reports `canDelete: false`. If
both come back true, the forward paths are confirmed clean and Part B is purely historical repair.

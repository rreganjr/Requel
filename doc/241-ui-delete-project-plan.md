# 241 — UI: Delete Project action with export-first backup prompt

Epic #239 · child #241. Consumes the backend `DeleteProject` command shipped in #240; sibling of
#242 (MCP/CLI gateway). Angular 21 / PrimeNG 21 work in `requel-angular`, plus a small backend
scope expansion (option A below) so the list row can gate the action without an extra round-trip.

> **Status:** integration surface fully confirmed on `release/2.0` (compiler-verified APIs). Scope A
> chosen (server-computed `canDelete` on the project list DTO). Decisions locked
> (go-with-recommendations).

## Summary

Add a **Delete Project** action, visible only to users who hold `Project[Delete]`, that runs a guarded
flow: name + permanence warning -> **export-first backup** toggle (default on) that downloads the
project XML and waits for it -> explicit **"Delete permanently"** -> dispatch `DeleteProject`. On
success the list refreshes (and the sidebar drops the project via `notifyTreeChanged()` + the existing
`Project:0` SSE broadcast), the user gets a success message, and the workspace routes back to the list;
on failure an inline error shows and nothing is deleted.

## Backend contract (certain — #240, merged as PR #245)

- `POST /api/commands/DeleteProject`, body `{ projectName: string, version?: number }`.
- Auth `RequiresStakeholderPermission(Project, "Delete")` -> **403** if the user lacks it (no
  system-admin bypass; project creator holds all perms; backfill grants Delete to every holder of
  `Project[Edit]`).
- Honors project `@Version`; stale -> **409**; omit `version` to skip the check.
- Unknown name -> **404**.

## Verified frontend integration surface (`release/2.0`)

- `core/command.service.ts` — `execute<T>(commandType, input): Promise<CommandResult<T>>` POSTs
  `${apiBaseUrl}/commands/${commandType}`; normalises errors and carries HTTP `status` through
  (409 distinguishable). Dispatch: `commandService.execute('DeleteProject', { projectName, version })`.
- `core/project.service.ts` — `downloadProjectXml(name): Promise<Blob>` (awaitable, JWT via
  interceptor); `listProjects()`, `getProject(name)`, `getMyPermissions(name)`; `notifyTreeChanged()`
  (Subject the sidebar/tree consume — the delete flow calls it, mirroring goal delete).
- `core/permission.service.ts` — stateful: `loadForProject(name)`, `canDelete('Project')`.
- `models/command.ts` — `CommandResult { success, entityType, entity, error, violations, status? }`.
- `models/project.ts` — `ProjectDto` has `version` (optimistic-lock value). **No** per-project perm
  flag today -> scope A adds `canDelete`.
- `features/projects/project-list.ts` — standalone, signals, inline template; `rowActions: RowAction[]`
  (currently just Open) drives the shared `app-data-table` `...` menu. `RowAction` supports
  `visible: (row) => boolean` and `disabled` — the list-row Delete gates via `visible: p => p.canDelete`.
- `features/projects/project-workspace.ts` — `/projects/:name` overview; header has an "Edit project"
  link in `.ws-header`. Loads `getProject` in `load()`; does **not** currently load permissions ->
  add `PermissionService.loadForProject` + gate the header Delete on `canDelete('Project')`.
- `shared/app-data-table.ts` `RowAction<T>`: `{ label, icon?, command:(row)=>void, visible?, disabled? }`
  — the `visible` predicate omits the item per-row. `rowActions` **replaces** the default menu.
- `shared/app-submit-error.ts` — inline `role="alert"` banner, `[message]`, `testid`, `retryable`,
  `(retry)`. Reused for the dialog's error line.
- PrimeNG house dialog pattern (goal-editor): `p-dialog [visible] (visibleChange) [modal]="true"
  [focusOnShow]="true" [dismissableMask]="true" closeAriaLabel appendTo="body" [header]`. Delete
  confirms elsewhere use `ConfirmationService`, but the export-first two-step needs custom content,
  so a bespoke `p-dialog` (like goal-editor's Relation Type dialog) is the right vehicle.
- Checkbox: `import { CheckboxModule } from 'primeng/checkbox'`, `<p-checkbox [(ngModel)]="..."
  [binary]="true" />`.

## Locked decisions (go-with-recommendations)

1. **Confirmation** — explicit **"Delete permanently"** button; no type-to-confirm.
2. **Placement** — **both** list row (gated per-row by `canDelete`) and workspace header (gated by
   `loadForProject` + `canDelete('Project')`).
3. **Backup gate** — `await projectService.downloadProjectXml(name)`, save the Blob via a temp anchor,
   then enable Delete. (Awaitable; no manual-checkbox fallback needed.)
4. **Refresh** — after success: list -> `loadProjects()`; workspace -> `router.navigate(['/projects'])`;
   both call `notifyTreeChanged()` so the sidebar drops the node immediately.
5. **Errors** — read `CommandResult.success`; on failure show `result.error` inline; project untouched.
6. **List-row gating = scope A** — add a server-computed `canDelete` to the project list DTO reflecting
   the caller's `Project[Delete]` on each project. Same gate the command enforces; zero extra
   round-trips per row.

## Design — `DeleteProjectDialogComponent`

Reusable standalone component (`DialogModule` + `ButtonModule` + `CheckboxModule` + FormsModule +
`SubmitErrorComponent`), opened from both placements.

- `@Input() project: { name: string; version: number } | null`; `@Input() visible` (two-way via
  `visibleChange`); `@Output() deleted`.
- Signals: `exportFirst=true`, `exporting`, `exported`, `deleting`, `errorMessage`.
- "Delete permanently" **disabled** until `(!exportFirst() || exported()) && !deleting()`.
- **Export step** (when `exportFirst` and not yet exported): `exporting.set(true)`; `blob = await
  downloadProjectXml(name)`; save via temp anchor (`URL.createObjectURL`, `download=`${name}.xml``,
  click, `revokeObjectURL`); `exported.set(true)`; on throw -> `errorMessage`.
- **Delete step**: `deleting.set(true)`; `r = await execute('DeleteProject', { projectName, version })`;
  `r.success` -> `notifyTreeChanged()`, `deleted.emit()`, reset; else `errorMessage.set(r.error ?? ...)`.
- **Reset on open/close**: clears the signals so a reopened dialog starts fresh; Cancel/Esc aborts with
  no dispatch.
- **A11y**: modal, focus-trapped `p-dialog`; real buttons; `aria-label`s; danger styling on the confirm.

## Backend scope A — `canDelete` on the list DTO

- `service-api ProjectDto` — add trailing `boolean canDelete` (primitive; unaffected by
  `@JsonInclude(NON_NULL)`).
- `service-impl ProjectQueryController.toDto` — take the resolved `User` and set `canDelete =
  callerHoldsProjectDelete(project, user)`: find the caller's `UserStakeholder`; true iff it holds a
  `StakeholderPermission` with `Project.class` + `StakeholderPermissionType.Delete`. `listProjects`
  already resolves the user; `getProject` captures the `User` from `requireProjectAccess`.
- `models/project.ts` `ProjectDto` — add `canDelete: boolean`.
- Backend test: list DTO's `canDelete` reflects the caller's `Project[Delete]` (true for creator/admin
  on own project; false for a stakeholder without Delete).

## Files to add / change

Add:
- `requel-angular/src/app/features/projects/delete-project-dialog.ts` — the dialog (inline template).
- `requel-angular/src/app/features/projects/delete-project-dialog.spec.ts` — unit tests.
- `requel-angular/e2e/delete-project.e2e.ts` — Playwright (`.e2e.ts` suffix, matches the suite).

Change:
- `features/projects/project-list.ts` — inject `CommandService`; import the dialog; add a Delete row
  action gated by `visible: p => p.canDelete`; host the dialog; on `deleted` -> `loadProjects()`.
- `features/projects/project-workspace.ts` — inject `PermissionService`; `loadForProject` in `load()`;
  add a header Delete button gated by `canDelete('Project')`; host the dialog; on `deleted` ->
  `router.navigate(['/projects'])`.
- `models/project.ts` — `ProjectDto.canDelete`.
- `service-api ProjectDto` + `service-impl ProjectQueryController` (+ backend test).
- `e2e/fixtures/api-helper.ts` — replace the no-op `deleteProject` placeholder with a real
  `DeleteProject` command call and fix its stale comment (the backend command now exists).

## Test plan (gate)

- **Backend:** `mvn clean verify` (scope A touches `modules/**`) — plus a focused
  `ProjectQueryControllerTest`/IT asserting `canDelete` per caller.
- **Frontend unit:** `cd requel-angular && CI=1 npx ng test --watch=false --include=...delete-project-dialog.spec.ts`
  - dialog: export-on disables Delete until the awaited export resolves; export-off enables immediately;
    confirm calls `execute('DeleteProject', {projectName, version})`; `success` emits `deleted` +
    `notifyTreeChanged`; failure sets `errorMessage`, no emit; Cancel/Esc aborts, no dispatch.
  - gating: workspace header hidden unless `canDelete('Project')`; list row hidden unless row `canDelete`.
- **Typecheck:** `npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit`;
  dev build `npx ng build --configuration development`.
- **e2e (Playwright, CI):** export-then-delete (admin: keep export on -> confirm -> project gone from
  list, success message); cancel-abort (open -> Cancel -> still listed). Unauthorized-persona e2e is
  deferred — there is no stakeholder-permission e2e helper to seed a non-Delete stakeholder; that gate
  is covered deterministically by the unit tests. (Noted as a fast-follow if a permission helper lands.)

## Out of scope
New export/import code (reuse `downloadProjectXml`); the backend command (#240) and gateway (#242);
soft delete/restore; bulk delete; an unauthorized-persona e2e (pending a permission-seeding helper).

## Risks
- **Blob save** is a native download (no completion event) — awaiting the HTTP/blob is the defensible
  "backup done" signal.
- **Stale `version` -> 409** — send `ProjectDto.version`; on lock error, surface it, let the user refresh.
- **Scope A perm check** must exactly mirror the command's gate (Project.class + Delete) so the UI never
  shows an action the backend would 403.

## Process (CLAUDE.md)
Branch `241-ui-delete-project` from `release/2.0`; this doc at `doc/241-ui-delete-project-plan.md`;
`tmp/241-verify.sh` runs backend + frontend gates; `commit.md`/`pr.md` gitignored; developer runs all
git/gh.

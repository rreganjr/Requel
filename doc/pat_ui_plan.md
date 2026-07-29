# Personal Access Token UI — plan (#73, Slice 6)

> Frontend for the PAT backend (Slices 1–3: store, `/api/auth/tokens` REST, filter validation).
> Lets a project user mint, view, and revoke their own tokens from the Angular app.

## API contract (already built, Slice 2)

- `POST /api/auth/tokens` `{ name, expiresInDays? }` → `201 { token: "reqpat_…", tokenInfo: {...} }`
  — the `reqpat_…` plaintext is returned **once**.
- `GET /api/auth/tokens` → `[ { id, name, createdAt, lastUsedAt, expiresAt, status } ]`
  (status = `ACTIVE` | `EXPIRED` | `REVOKED`).
- `DELETE /api/auth/tokens/{id}` → `204` (revoke; own-tokens-only, else `404`).

## Where it lives (decision)

**Recommended: a "Personal Access Tokens" section on the existing Settings page** (`/settings` →
`SettingsComponent`), reachable from the existing top-right Account menu → *Settings*. It's the
natural home (account-level self-service), avoids a new top-level route, and keeps the menu tidy.

Alternative considered: a dedicated `/settings/tokens` (or `/account/tokens`) route + its own
top-right menu item. Cleaner separation and deep-linkable, at the cost of another route/guard and a
busier menu. Easy to split out later if the Settings section grows. (Either way the table/dialog
component is the same; only the host differs.)

## Visibility / gating (decision)

PATs are a **user-level** capability, not project-scoped, so gate them with a **user role**, not a
stakeholder permission (see "Permission question" below). Gate in three places, mirroring the
existing `adminGuard` / `SidebarNavComponent.isAdmin` pattern:

1. **Menu/section render** — show the Tokens section/item only when
   `authService.user()?.roles?.includes('ProjectUserRole')`.
2. **Route guard** (if a dedicated route) — a `projectUserGuard` (CanActivateFn) cloned from
   `adminGuard`, redirecting non-project-users to `/`.
3. **Backend defense-in-depth** — role-gate the `/api/auth/tokens` endpoints so a direct API call
   from a non-project user is rejected too (UI hiding alone isn't security). Proposed rule: allow
   `ProjectUserRole` **or** `SystemAdminUserRole` (admins likely want tokens too) via an
   `@PreAuthorize`/method-security check or an `ApiSecurityConfig` matcher. Decide the exact role
   set during implementation.

## Permission model — decision

**v1 (this slice): role-gate on `ProjectUserRole`** — gate the UI section and the `/api/auth/tokens`
endpoints on the role the user already holds (`UserDto.roles`, already present). No migration, no
`UserDto` change, no touch of the permission cache. Use is unguarded (a PAT only authenticates as
its owner — what it can do is governed by the owner's roles + stakeholder permissions at request
time). NOT a StakeholderPermission (PATs aren't project-scoped — wrong layer).

**Fast-follow (separate ticket): a `ManageApiTokens` `UserRolePermission`** for finer, per-user
control. Deferred because it's a multi-part rollout (below) with known cache fragility; do it once
the v1 UI is proven.

How the permission model works here (discovered while planning), and the resulting work for the
fast-follow:

- `ProjectUserRole` declares static `UserRolePermission` constants (e.g. `createProjects`) and registers
  the *available* set in a static block; `UserRolePermissionsInitializer` persists/caches them.
- Crucially, holding the role does **not** auto-grant a permission. Each user's `ProjectUserRole`
  **instance** has an explicit granted set (`user_roles_permissions` join table); permissions are
  granted per-user via `EditUserCommand.addUserRolePermissionName(role, permissionName)`
  (e.g. `ProjectUserInitializer` grants `createProjects` to the seeded `project` user).

So `ManageApiTokens` is a rollout, not a one-liner:

1. **Declare** `ProjectUserRole.manageApiTokens` + add to the available set in the static block;
   add `canManageApiTokens()`.
2. **Grant to existing users** — a one-time initializer/migration that grants `manageApiTokens` to
   every existing `ProjectUserRole` instance (the join table). (New users get it via whatever grants
   their role permissions at creation; decide whether creation auto-grants it or admins opt in.)
3. **Enforce** on the token endpoints — resolve the current user and check
   `getPermissionStrings(user).contains("manageApiTokens")` (works for both JWT and PAT callers,
   which load the user live), returning 403 otherwise. (Simpler than mapping permissions to Spring
   authorities, which the filter doesn't currently do.)
4. **Expose to the UI** — `UserDto` currently carries `roles` but not `permissions`; add a
   `permissions` list (populate from `getPermissionStrings`) so the Settings section can render only
   when the user holds `manageApiTokens`.

**Risk flag:** the static `AbstractUserRole.userRoleTypePermissions` cache + the
`user_roles_permissions` rows interacted badly during the #76 upgrade ("UserRolePermission is out of
date" stale-state errors across test contexts). Adding a permission must be done carefully and
re-tested across the full suite.

### Implemented (#85) — decisions

The fast-follow was implemented in #85 with these decisions (supersede the open questions above):

- **Opt-in, admin-granted.** `manageApiTokens` is declared on `ProjectUserRole` only and is **not**
  auto-granted — neither to existing users nor on new-user creation. An admin grants it per user.
- **No backfill.** Step 2's one-time grant to existing `ProjectUserRole` instances was dropped;
  existing project users have no PAT access until an admin grants it (the seeded `project`/`admin`
  users included). `UserRolePermissionsInitializer` still persists/caches the new *available*
  permission so it can be granted.
- **Admin toggle is already generic.** The user create/edit UI (`UserEditorComponent`) renders
  per-role permission checkboxes from `GET /api/users/roles` (`availablePermissions`) and submits
  `userRolePermissionNames`; `EditUserCommandImpl.updateRoles()` grants/revokes them. So declaring
  the permission surfaces the toggle automatically — no bespoke UI.
- **`UserDto.permissions` already existed** (added since this plan was written), so step 4 reduced
  to the Angular gate: `SettingsComponent.canManageTokens()` now checks
  `permissions.includes('manageApiTokens')` instead of the role.
- **Enforcement** lives in `ApiTokenController` (`getPermissionStrings(user).contains(...)` → 403);
  the `.hasRole("ProjectUserRole")` matcher on `/api/auth/tokens` in `ApiSecurityConfig` was relaxed
  to `.authenticated()`.
- **Admins** get PAT access only when they also hold `ProjectUserRole` and are granted the
  permission — there is no `manageApiTokens` on `SystemAdminUserRole`.

## Angular pieces

- `models/api-token.ts` — `ApiTokenDto`, `CreateApiTokenRequest`, `CreateApiTokenResponse`.
- `core/token.service.ts` — `list()`, `create(req)`, `revoke(id)` over `environment.apiBaseUrl`
  (the existing `auth.interceptor` attaches the bearer), mirroring `user.service.ts` /
  `project.service.ts`.
- UI: a PrimeNG `p-table` of tokens (name, status badge, created, last used, expires) with a
  per-row **Revoke** (confirm dialog); a **New token** dialog (name + optional expiry days). On
  create, show the one-time `reqpat_…` plaintext in a highlighted, copy-to-clipboard block with a
  clear "you won't see this again" warning (no plaintext is ever re-fetchable). Use the existing
  `MessageService`/toast for success/errors. Hosted as a section in `SettingsComponent` (or the
  dedicated route if chosen).

## UX details

- Status rendered as a colored tag (ACTIVE green / EXPIRED grey / REVOKED red).
- Revoked/expired rows stay listed (history) but show no actions except maybe "remove from list" if
  we later add hard-delete; v1 keeps them visible with the status.
- Empty state: short explainer + a link to `doc/mcp_remote_connection.md` usage.
- Copy button on the one-time token; dialog can't be dismissed without an explicit "I've copied it".

## Testing

- `token.service` unit test (HTTP calls shape) — Vitest, mirroring existing service specs.
- Component spec: renders the list, opens the create dialog, shows the one-time token, calls revoke.
- Guard spec for `projectUserGuard` (if added), mirroring `admin.guard.spec.ts`.
- Backend: extend `ApiTokenIT` (or a new IT) to assert the role-gate (a non-project user is rejected
  from `/api/auth/tokens`) once the role rule is added.

## Open questions — resolved

- **Settings section vs dedicated route** → Settings-page section (`SettingsComponent`). No new route.
- **Backend role set for the endpoint gate** → `ProjectUserRole` **only**. PATs act against project
  data, so a pure system admin (no project role) has no use for one; a system admin who *also* holds
  `ProjectUserRole` still qualifies through that role. Both the endpoint matcher and the UI gate use
  `ProjectUserRole` alone.
- **UI expiry** → fixed preset dropdown of 30 / 90 / 180 / 365 days, **defaulting to 90**. No
  free-form entry and no "never" from the UI (the API still accepts a null expiry for other callers).
- **`ManageApiTokens` `UserRolePermission`** → deferred to a fast-follow ticket (see "Permission
  model" above). v1 ships role-gated.

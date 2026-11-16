# #381 — Force a password change on the built-in admin's first login

https://github.com/rreganjr/Requel/issues/381

Branch `381-force-admin-password-change` from `release/2.1`, PR to `release/2.1`. Size 5 (raised
from 3 at review: backend, both security chains, PATs, the OAuth form, the admin user editor, a
new page and e2e).

## Summary

Every install starts with `admin`/`admin` and nothing makes anyone change it. This adds a
per-user "must change password" flag. A flagged user can sign in, but until they change their
password the API answers only the change-password call and `/api/auth/me`, and the UI goes
straight to a change-password page. The built-in admin gets the flag whenever its password is
still `admin`; an admin can set it on any user.

## Locked decisions (2026-10-29)

1. **A dedicated `POST /api/auth/password`** (`currentPassword`, `newPassword`,
   `confirmPassword`). It checks the current password, refuses a new one equal to it, clears the
   flag and returns a fresh JWT + `UserDto` (the login response shape). `EditUser` is not used:
   it has no current-password check.
2. **Enforcement:**
   - Login puts a `pwdChange` claim in the JWT when the flag is set, so the JWT path still makes
     no per-request database hit.
   - The PAT path (`JwtAuthenticationFilter`) and the MCP OAuth path
     (`McpJwtAuthenticationConverter`) already load the user live; they check the flag there.
   - All three give a flagged caller the single authority `PASSWORD_CHANGE_REQUIRED` instead of
     the user's roles. A new `PasswordChangeRequiredFilter`, in both the `/api/**` chain and the
     `/api/mcp/**` + `/api/gateway/**` chain, answers 403 `PASSWORD_CHANGE_REQUIRED` to such a
     caller on every request except `POST /api/auth/password` and `GET /api/auth/me`.
   - Logout is client-side (stateless JWT); nothing to allow.
   - Known limit: a flag an admin sets on a user who is already signed in applies at that user's
     next sign-in (their current JWT has no claim). PATs and MCP tokens pick it up at once.
3. **The OAuth sign-in form** (MCP clients, `requel login --oauth`) refuses a flagged user:
   `RequelUserAuthenticationProvider` throws `CredentialsExpiredException`, and the page shows
   "You must change your password in Requel before connecting an application."
4. **Startup check, every startup:** a new `DefaultAdminPasswordInitializer` (order 110, after
   `AdminUserInitializer`'s 100) sets the flag on `admin` when its password is still `admin`. One
   mechanism covers fresh installs and upgrades, and re-flags an admin set back to `admin`. An
   upgraded install whose admin password was changed is untouched.
5. **`requel.admin.force-password-change`** (default `true`) turns the startup check off. It is
   `false` in `application-test.properties` and `application-dev.properties`, so ITs and local
   dev restarts aren't blocked. `/api/dev/reset-admin` also clears the flag.
6. **An admin can set the flag** on `NewUser` / `EditUser` (`mustChangePassword`, admin-only like
   `editable`), shown as a checkbox in the user editor, unchecked by default.
7. **Out of scope:** the Account page asking for the current password on a change.

## Changes

### Backend

- `V40__users_must_change_password.sql`:
  `ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE`.
- `User` / `UserImpl`: `isMustChangePassword()` / `setMustChangePassword(boolean)`; XML type
  unchanged (export/import ignores it).
- `EditUserCommand(Impl)`: `setMustChangePassword(Boolean)`; applied on create and on an admin's
  update, ignored on a self-edit.
- `EditUserInput`: `Boolean mustChangePassword` with its description; `UserCommandRegistrar` maps
  it. `UserDto` + `UserDtoMapper`: `mustChangePassword`.
- `JwtService.generateToken`: the `pwdChange` claim; a reader for it.
- `AuthController`: login unchanged apart from the claim; new `POST /api/auth/password` through a
  new `@Transactional` `PasswordChangeService` (service-impl). Failures are 422 with violations on
  `currentPassword` ("is incorrect"), `newPassword` ("must differ from the current password",
  the existing password rules) and `confirmPassword` ("doesn't match").
- `JwtAuthenticationFilter`, `McpJwtAuthenticationConverter`: the restricted authority.
- `PasswordChangeRequiredFilter` (new) in `ApiSecurityConfig` and `McpResourceServerConfig`.
- `RequelUserAuthenticationProvider` + `OAuth2LoginPageController`: the refusal and its message
  (`?error=password_change`).
- `DefaultAdminPasswordInitializer` (new, user-jpa, `@Transactional`, conditional on the
  property).
- `DevAdminResetController`: clears the flag.

### Angular

- `UserDto` model: `mustChangePassword`.
- `AuthService.changePassword(...)`: posts, then stores the returned token and user.
- `features/auth/change-password.ts` (new): top-level route `change-password` (outside the
  layout, like `login`; requires a token). Current / new / confirm, server violations inline,
  and a Sign out button.
- `authGuard`: a flagged user is sent to `/change-password`. `login.ts`: lands there after a
  flagged sign-in.
- `auth.interceptor`: a 403 `PASSWORD_CHANGE_REQUIRED` navigates to `/change-password`.
- `user-editor.ts`: the "Must change password at next sign-in" checkbox.

### Docs

- `doc/guides/INSTALL.md`, `DOCKER_HUB.md`: first sign-in now forces the change; the property.

## Test plan

`tmp/381-verify.sh`: `mvn clean verify -pl modules/coverage-report -am`, then the Angular unit
suite once (`CI=1 npm test -- --watch=false`), the two `tsc --noEmit` typechecks and
`ng build --configuration development`. e2e runs in CI.

- `ForcePasswordChangeIT` (new, MockMvc, a user created with the flag):
  - login returns `mustChangePassword: true`;
  - with that JWT: `GET /api/projects`, a `/api/commands/...` call, `/api/gateway/...`,
    `/api/mcp` and `POST /api/auth/tokens` are 403 `PASSWORD_CHANGE_REQUIRED`; `GET /api/auth/me`
    is 200;
  - a PAT issued before the flag was set is 403 on the same calls;
  - change-password: wrong current, new = current, and mismatched confirm are 422 on the right
    field; success returns a token that reaches `/api/projects`, and the flag is cleared.
- `RequelUserAuthenticationProviderTest`: a flagged user gets `CredentialsExpiredException`.
- `DefaultAdminPasswordInitializerIT` (new): `admin`/`admin` gets flagged; a changed password
  doesn't; property off does nothing.
- `EditUser` ITs: an admin sets and clears the flag; a self-edit can't.
- Existing MySQL ITs run V40 through Flyway.
- Angular specs: `auth.guard`, `auth.service`, `auth.interceptor`, `login`, `change-password`
  (+ a11y), `user-editor`.
- e2e `change-password.e2e.ts` (new): admin creates a flagged user over the API; signing in as
  that user lands on Change password, `/` redirects back there, a wrong current password and the
  same password are refused, success lands on the dashboard, and the new password signs in
  cleanly.

## Out of scope

- Account page current-password check (decision 7).
- Ending other live sessions when the flag is set.

## Risks

- **Locking ITs/e2e out:** the property is off in test and dev, and `reset-admin` clears the
  flag, so the many tests that sign in as `admin`/`admin` keep working.
- **The MCP write-catalog lockstep test** sees the new `EditUser` field; update its expectation.
- **Stored user in localStorage** predates `mustChangePassword`; the guard treats a missing value
  as false, and the server's 403 still routes a flagged session to the page.

## AC mapping

| AC | Covered by |
|---|---|
| Fresh install's first admin login lands on change-password; nothing else works until done | Decisions 2, 4; `ForcePasswordChangeIT`, `DefaultAdminPasswordInitializerIT`, e2e |
| Upgraded install with a changed admin password is unaffected | Decision 4; `DefaultAdminPasswordInitializerIT` |
| MCP and API-token paths can't skip it | Decisions 2, 3; `ForcePasswordChangeIT` (PAT, gateway, MCP), provider test |

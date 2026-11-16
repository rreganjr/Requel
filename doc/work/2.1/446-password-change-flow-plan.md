# #446 — Password changes need the current password; older sign-ins end

https://github.com/rreganjr/Requel/issues/446

Branch `446-password-change-flow` from `release/2.1`, PR to `release/2.1`. Size 5.

## Summary

`EditUser` changes the password of the caller's own account when the two copies match, without
the current password. So anyone holding a login token or a personal access token can take over
the account, and the current-password check on `/api/auth/password` (#381) can be bypassed. And a
password change leaves every earlier sign-in working. This routes your own password changes
through the Change Password page only, keeps the admin reset, and ends sign-ins from before a
change.

## Locked decisions (2026-11-02)

1. **`EditUser` never changes the caller's own password**, admins included. A self-edit that
   sends a password is refused with 422 on `password`: "Change your own password on the Change
   Password page." The other fields still save in a request without a password.
2. **The admin reset stays.** An admin editing another user's password through `EditUser` works
   as today. The "must change password" checkbox (#381) stays the admin's choice.
3. **A password change ends older sign-ins.**
   - New column `users.password_changed_at` (migration `V41`), set by a real change: your own
     (`PasswordChangeService`) or an admin reset. The rehash a sign-in does when the stored
     algorithm is out of date doesn't set it. Creating the account sets it, which is harmless: no
     token can be older than the account.
   - A login JWT whose `iat` is before it gets 401 (`JwtAuthenticationFilter`), to the second, so
     the fresh token the change returns still works. The same check covers OAuth access tokens
     (`McpJwtAuthenticationConverter`).
   - The user's stored OAuth authorizations (`oauth2_authorization`, by principal name) are
     deleted, so their refresh tokens stop working.
   - **Personal access tokens:** your own change keeps them; an admin reset revokes every one of
     the user's tokens (a reset usually means the account was compromised).
4. **Angular.**
   - Edit Account: the password rows become a "Change password" link to `/change-password`.
   - Admin user editor: when the admin edits their own account, the password rows are hidden and
     the same link shows. Editing anyone else, they stay.
5. **Out of scope:** reauthentication before an email change. Requel has no email-based password
   recovery, so the email address isn't a way into the account.
6. **Plain wording** in code, tests, commit and PR.

## Changes

user-domain / user-jpa
- `User` / `UserImpl`: `getPasswordChangedAt()` (an `Instant`, null for never). The public
  `resetPassword` (the constructor uses it too) sets it, truncated to the second; the private
  `_resetPassword` the sign-in rehash calls doesn't.
- `EditUserCommandImpl.updateUser`:
  - when the user being edited is the caller (`editedBy`) and a password is supplied, throw an
    `EntityValidationException` on `password` with the sentence above;
  - otherwise, an admin reset as today, and the command records it (`isPasswordReset()`, new on
    `EditUserCommand`).
- `V41__users_password_changed_at.sql`: `ALTER TABLE users ADD COLUMN password_changed_at
  DATETIME NULL`.

service-impl
- `SignInRevocation` (new, `service.auth`):
  - `afterPasswordChange(User user, boolean revokeApiTokens)` deletes the user's OAuth
    authorizations (`JdbcTemplate` on `oauth2_authorization` where `principal_name` is the
    username);
  - when asked, it marks all the user's personal access tokens revoked.
- `PasswordChangeService`: after the change, `afterPasswordChange(user, false)`.
- A command-handler decorator next to `CurrentUserCommandHandler`: after an `EditUserCommand`
  with `isPasswordReset()`, calls `afterPasswordChange(user, true)`. (`user-jpa` can't reach
  `ApiToken` or the OAuth tables, so the follow-up lives in `service-impl`.)
- `JwtAuthenticationFilter.authenticateJwt`: after loading the user, refuse when
  `issuedAt < passwordChangedAt`.
- `McpJwtAuthenticationConverter`: the same, throwing `InvalidBearerTokenException`.
- `CommandController` already maps `EntityValidationException` to 422 with field violations.
  It's checked by test, and changed only if the field isn't carried.

requel-angular
- `features/users/edit-account.ts`: drop the password controls and rows; add the link. The form
  no longer sends `password` / `repassword`.
- `features/users/user-editor.ts`: when the edited user is the signed-in user, hide the password
  rows and show the link.
- Specs for both. `e2e/account.e2e.ts`: the password-change case goes through
  `/change-password`, and the page objects lose the password rows.

## Steps

1. Branch.
2. `UserImpl.passwordChangedAt`, the migration and the `EditUserCommandImpl` refusal.
3. The token checks in the filter and the MCP converter.
4. `SignInRevocation` and the two places that call it.
5. Angular pages, specs and e2e.
6. Tests.

## Test plan

`tmp/446-verify.sh`: `mvn clean verify -pl modules/coverage-report -am`, the coverage check, the
Angular typechecks and unit tests. The e2e changes run in CI.

- `EditUserCommandTest`:
  - a self-edit with a password is refused on `password`, for a user and for an admin;
  - a self-edit without one saves;
  - an admin editing another user's password changes it, sets `passwordChangedAt` and reports
    `isPasswordReset()`.
- `UserImplTest`: `resetPassword` sets `passwordChangedAt`; a sign-in that rehashes an old
  password doesn't.
- `PasswordChangeFlowIT` (new, MockMvc):
  - `POST /api/commands/EditUser` on your own account with a password: 422, and the password is
    unchanged (the old one still signs in);
  - the same with a personal access token: 422;
  - your own change through `/api/auth/password`: the returned token works, a token from before
    the change gets 401, your personal access token still works, and your OAuth authorization
    rows are gone;
  - an admin reset of another user: that user's old login token gets 401 and their personal
    access tokens are revoked.
- `McpJwtAuthenticationConverterTest`: an access token issued before the change is refused.
- The migration: `UsersPasswordChangedAtMigrationMySqlIT` (or an existing Flyway MySQL IT) shows
  the column exists and is null for existing users.
- Angular: `edit-account.spec.ts` (no password controls, the link is there, the request carries
  no password); `user-editor.spec.ts` (rows hidden for your own account, shown for another).
- `account.e2e.ts` updated; `change-password.e2e.ts` checks that after a change, a request with
  the old token gets 401.

## Out of scope

- Email-change reauthentication (decision 5).
- The admin choosing to force a change after a reset (it's the existing checkbox).
- Revoking sessions on a role change (#445 already loads roles live).

## Risks

- **Other devices sign out.** A password change ends the user's other sessions. That's the point;
  the Angular interceptor already sends a 401 to the login page.
- **Clock skew between `iat` and the database time.** Both come from the one server's clock,
  compared to the second.
- **e2e.** The global setup's admin reset (`/api/dev/reset-admin`) now ends the admin's earlier
  tokens. The setup signs in after the reset, so the run is unaffected; checked in CI.

## AC mapping

| AC | Covered by |
|---|---|
| Non-admin `EditUser` can't change the password without the current-password flow | Decision 1; `EditUserCommandTest`, `PasswordChangeFlowIT` |
| Self-service changes need the correct current password and matching new values | #381's `PasswordChangeService`, unchanged; `PasswordChangeFlowIT` |
| The admin reset stays explicitly authorized and working | Decision 2; `EditUserCommandTest`, `PasswordChangeFlowIT` |
| Tests with ordinary JWTs and PATs show the other path doesn't change the password | `PasswordChangeFlowIT` |
| A password change or admin reset revokes login JWTs issued before it (Flyway migration) | Decision 3; `PasswordChangeFlowIT`, the migration IT |
| The UI change and e2e coverage | Decision 4; the specs and e2e |

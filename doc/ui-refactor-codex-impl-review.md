# UI Refactor Implementation Review (Codex)

Date: 2026-03-11

## Scope Reviewed
- Plan: `doc/UI_REFACTOR_PLAN.md`
- Implementation diff: `master...38-migrate-ui-from-echo2-to-angular`
- Focus areas requested:
  - Server-side authentication and authorization for new API endpoints
  - PII exposure checks (user data visible only to admins or the user themself)
  - Spring Boot / Spring Security conventions for new endpoints

## Executive Summary
- Authentication baseline is in place (JWT login, JWT filter, stateless API chain), but authorization is not safely implemented for several high-risk mutation/read paths.
- The most serious issue is user-account mutation: non-admin users can create users and modify other users via `EditUser` command dispatch.
- Project detail and SSE subscription paths also miss required access checks and can expose data across users.

## Findings (Ordered by Severity)

### Critical

1. `EditUser` command path allows unauthorized user creation and cross-user edits (including role assignment on create).
- Evidence:
  - Security restricts only `/api/commands/NewUser`, but no `NewUser` command is registered: `modules/service-impl/src/main/java/com/rreganjr/requel/service/config/ApiSecurityConfig.java:44`
  - User creation/update is wired through `EditUser` and lookup-by-username fallback-to-create: `modules/service-impl/src/main/java/com/rreganjr/requel/service/command/UserCommandRegistrar.java:38-50`
  - `EditUserCommandImpl.createUser()` performs create + `updateRoles()` without admin check: `modules/user-jpa/src/main/java/com/rreganjr/requel/user/impl/command/EditUserCommandImpl.java:95-103,142-160`
  - `EditUserCommandImpl.updateUser()` applies name/email/phone/password/org changes before any admin gate, and does not enforce `editedBy == targetUser`: `modules/user-jpa/src/main/java/com/rreganjr/requel/user/impl/command/EditUserCommandImpl.java:108-129`
- Impact:
  - Any authenticated user can modify another user’s PII and password.
  - Any authenticated user can create accounts (and set roles/permissions at create time).
- Recommendation:
  - Immediately gate `EditUser` server-side with explicit policy: admin, or self-only with tightly scoped editable fields.
  - Split create vs update semantics (`NewUser` admin-only, `EditMyAccount` self-only, `EditUser` admin-only) or enforce equivalent checks in one command.

2. Planned command-level authorization is effectively inactive.
- Evidence:
  - `AuthorizingCommandHandler` only enforces checks for `AuthorizableCommand`: `modules/requel-app/src/main/java/com/rreganjr/requel/command/AuthorizingCommandHandler.java:36-44`
  - No command implementation currently implements `AuthorizableCommand` / `ProjectScopedCommand` (code search only finds interface/handler declarations).
- Impact:
  - Most `/api/commands/{commandType}` operations are protected only by “authenticated user”, not by role/stakeholder permissions described in the plan.
- Recommendation:
  - Implement `AuthorizableCommand` requirements for each exposed command before treating endpoints as production-secure.
  - Add deny-by-default policy for command types lacking declared requirements.

### High

3. `GET /api/projects/{name}` bypasses project access control.
- Evidence:
  - List endpoint filters by user role/project membership: `modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:40-55`
  - Detail endpoint returns project by name with no stakeholder/admin check: `modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:61-69`
- Impact:
  - Any authenticated user can request project metadata for projects they should not access.
- Recommendation:
  - Apply the same access checker to detail endpoints as list endpoints (ideally centralized `ProjectAccessChecker`).

4. SSE stream/session endpoints are not bound to authenticated principal or authorized targets.
- Evidence:
  - Stream open allows arbitrary `sessionId` reattach and does not pass/validate user context: `modules/service-impl/src/main/java/com/rreganjr/requel/service/stream/StreamController.java:29-36`
  - Subscription add/remove/close trust only `X-Session-Id`: `modules/service-impl/src/main/java/com/rreganjr/requel/service/stream/StreamController.java:41-67`
  - Session store tracks only `sessionId -> subscriptions`, no owner user: `modules/service-impl/src/main/java/com/rreganjr/requel/service/stream/StreamService.java:49-76`, `modules/service-impl/src/main/java/com/rreganjr/requel/service/stream/StreamSessionStore.java:14-58`
- Impact:
  - Session hijack/manipulation risk and unauthorized subscription to targets.
- Recommendation:
  - Bind session to principal on creation and enforce ownership on all session operations.
  - Validate each subscription target against the caller’s access rights.

5. Non-API security chain currently permits all endpoints, including exposed actuator metrics.
- Evidence:
  - Global chain allows any request: `modules/requel-app/src/main/java/com/rreganjr/requel/Application.java:96`
  - Metrics/prometheus endpoints are exposed: `modules/requel-app/src/main/resources/application.properties:10`
- Impact:
  - Operational metadata is publicly reachable unless separately blocked upstream.
- Recommendation:
  - Add explicit security policy for `/actuator/**` (at minimum restrict `metrics`/`prometheus`; keep only `health` public if desired).

### Medium

6. 500 responses from command dispatch leak internal exception class/message.
- Evidence:
  - `INTERNAL_ERROR` returns `e.getClass().getSimpleName() + ": " + e.getMessage()`: `modules/service-impl/src/main/java/com/rreganjr/requel/service/command/CommandController.java:109-113`
- Impact:
  - Internal implementation details may leak to clients/log aggregators.
- Recommendation:
  - Return generic error text to clients; keep details in server logs/correlation IDs.

7. API contract/convention drift vs plan and common Spring API practice.
- Evidence:
  - List endpoints return raw `List<T>` instead of paged envelope from plan: `modules/service-impl/src/main/java/com/rreganjr/requel/service/query/UserQueryController.java:35-40`, `modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:40-55`
  - Path keys use mutable names/usernames rather than stable IDs (`/{name}`, `/{username}`): `modules/service-impl/src/main/java/com/rreganjr/requel/service/query/ProjectQueryController.java:61`, `modules/service-impl/src/main/java/com/rreganjr/requel/service/query/UserQueryController.java:46`
  - `UserDto.version` is hardcoded to `0`: `modules/service-impl/src/main/java/com/rreganjr/requel/service/auth/UserDtoMapper.java:49`
- Impact:
  - Higher risk of client/server contract drift and weaker optimistic-lock/update semantics.
- Recommendation:
  - Normalize to documented response contracts and stable resource identifiers.

8. Missing automated tests for new API/security behavior.
- Evidence:
  - No service-api/service-impl tests found for authz, endpoint access, or PII constraints.
- Impact:
  - Regressions in authorization and data exposure likely as implementation proceeds.
- Recommendation:
  - Add integration tests for:
    - role-based access per endpoint,
    - self-vs-admin user data rules,
    - command authorization matrix,
    - project membership checks,
    - SSE session ownership/subscription authorization.

## What Looks Good
- API security chain is isolated to `/api/**`, stateless, and JWT-filtered: `modules/service-impl/src/main/java/com/rreganjr/requel/service/config/ApiSecurityConfig.java:36-50`
- JWT login flow uses existing password verification (`user.isPassword`) and returns 401 on bad credentials: `modules/service-impl/src/main/java/com/rreganjr/requel/service/auth/AuthController.java:41-60`
- `/api/users/**` read endpoints are at least path-gated to admin role in security configuration (with explicit exception for organizations): `modules/service-impl/src/main/java/com/rreganjr/requel/service/config/ApiSecurityConfig.java:42-44`

## Direct Answer To Requested Checks
- Are authentication and authorization properly applied on new server endpoints?
  - Authentication: mostly yes (JWT chain for `/api/**`).
  - Authorization: no, not yet sufficient for production due critical gaps above.
- Are we leaking PII from new endpoints?
  - Yes, materially via unauthorized `EditUser` mutation path (and related cross-user modification capability).
- Is user data only visible to admin or the user themself?
  - Not enforced end-to-end. Read endpoints are admin-gated, but mutation path allows non-admin cross-user edits.
- Are Spring Boot/Spring Security conventions followed?
  - Partially. Foundation is good, but key convention gaps remain (resource authorization centralization, endpoint contract consistency, actuator exposure policy, and missing security tests).

# UI Refactor Plan — Cursor Review

This document holds follow-up reviews of [doc/UI_REFACTOR_PLAN.md](UI_REFACTOR_PLAN.md) after plan updates and the addition of [doc/AUTH_ARCH.md](AUTH_ARCH.md).

---

## Follow Up Review

*Review date: after plan updates (environment/versions, expanded §3.4 Authentication, §3.6 Authorization, Phase 0 auth/SSE/authorization tasks) and inclusion of AUTH_ARCH.md.*

### 1. Resolution of Original Review Gaps

The plan has been updated to address the gaps called out in the initial completeness review (doc/ui-refactor-review.md). Summary:

| Original gap | Status |
|--------------|--------|
| **Backend versions** | Addressed. §3.2 "Versions" table lists Java 17, Spring Boot 3.3.x, Spring Security (from starter), jjwt, Maven 3.6.3+. |
| **JWT library** | Addressed. jjwt with HS256, 8-hour expiry, claims (sub, roles, exp) specified in versions table and §3.4. |
| **CORS** | Addressed. Environment Configuration table: dev `http://localhost:4200`, production same-origin / disabled. Phase 0 explicitly calls out CORS. |
| **Environment variables** | Addressed. Environment Configuration table covers JWT secret, API base URL (dev proxy, prod same-origin), CORS. |
| **Current user for commands** | Addressed. §3.4 "Resolving current user for commands" and Phase 0: `CurrentUserResolver` resolves JWT principal to domain `User` via `UserRepository.findUserByUsername`; `CommandController` calls `command.setEditedBy(currentUser)` before execute. |
| **Role-based UI in Angular** | Addressed. §3.6 "Angular: Role-based visibility" table: Users tab and role editing require SystemAdmin; route guard and conditional visibility. |
| **Stakeholder-based permissions** | Addressed. §3.6 describes domain-level authorization (AuthorizingCommandHandler, ProjectAccessChecker) and defers full design to AUTH_ARCH.md. |
| **Request-level security** | Addressed. §3.4 lists public (`POST /api/auth/login`, actuator/health) vs authenticated (all other `/api/**`). |
| **JWT filter placement** | Addressed. First filter in Spring Security chain; validate, set SecurityContext; 401 on invalid/missing for protected paths. |
| **Login / password verification** | Addressed. Login endpoint uses `UserRepository.findUserByUsername` and `user.isPassword(rawPassword)`; password storage unchanged. |
| **Angular token handling** | Addressed. §3.4 "Angular: Token Handling and Auth Flow": AuthService signal, login/logout, guard (presence + exp), 401 interceptor, `/api/auth/me` when token in memory. |
| **SSE and token in URL** | Addressed. Plan uses **fetch-based streaming** instead of EventSource; JWT sent in `Authorization` header, so no token in URL or logs. |

The only remaining nuance from the original review is **password storage**: the plan states "password storage format is unchanged" and defers hashing to a separate task. That is an acceptable explicit deferral.

---

### 2. AUTH_ARCH.md — Assessment and Fit with the Plan

**Purpose and scope:** AUTH_ARCH.md describes the **authorization** model (who can do what), not authentication (who you are). It fits the plan as the single place for command/query/authz design.

**Strengths:**

- **Current state is accurate.** Two-layer model (system roles + stakeholder permissions), the fact that most enforcement is in Echo2 panels today, and that only `EditUserCommandImpl` does an explicit auth check in a command are correctly stated. This makes the migration risk clear.
- **AuthorizingCommandHandler in the chain** is the right place for command authz: one enforcement point, before the transactional boundary, with a clear contract (`AuthorizableCommand`, `AuthorizationRequirement`). The diagram in the plan (§3.1) now shows this handler in the chain.
- **Sealed `AuthorizationRequirement`** (RequiresSystemRole, RequiresRolePermission, RequiresStakeholderPermission) keeps requirements explicit and avoids ad-hoc checks inside each command.
- **ProjectScopedCommand** cleanly provides project context for stakeholder permission checks without pushing repository access into every command.
- **Query authorization** via `ProjectAccessChecker` (requireStakeholder / canAccessProject) and the example controller usage (`@AuthenticationPrincipal`, resolve user, then requireStakeholder) give a repeatable pattern for read endpoints.
- **Angular side** is well thought through: system roles and role permissions from JWT; project permissions from `GET /api/projects/{projectId}/my-permissions`; `PermissionService` with signals and `hasPermission(projectId, entityType, permissionType)`; 403 handling (notification, no redirect). This matches the plan’s “roles for UX only; backend enforces.”
- **Migration approach** (Phase 0 infra, then per-command and per-query as API is enabled, Angular PermissionService in Phase 9) is consistent with the plan’s phased migration.

**Alignment with UI_REFACTOR_PLAN.md:**

- Plan §3.6 correctly summarizes endpoint-level (Spring Security, admin endpoints) vs domain-level (AuthorizingCommandHandler + ProjectAccessChecker) and points to AUTH_ARCH.md for detail.
- Phase 0 backend work lists the same authz building blocks as AUTH_ARCH §6: AuthorizableCommand, AuthorizationRequirement, ProjectScopedCommand, AuthorizingCommandHandler, AuthorizationException, ProjectAccessChecker.
- The plan does not duplicate AUTH_ARCH’s code samples or handler chain XML; the reference is sufficient.

**Minor observations on AUTH_ARCH:**

- **JWT claim names:** AUTH_ARCH §4.1 shows `"roles": ["PROJECT_USER"]` and `"permissions": ["createProjects"]`. The plan’s §3.4 mentions "roles = user's domain roles" in the JWT. Ensure the plan or AUTH_ARCH explicitly states how domain role classes (e.g. `SystemAdminUserRole`) map to JWT role strings (e.g. `"SYSTEM_ADMIN"`) and that role-level permissions are included in the token if Angular is to use them for UX (e.g. "New Project" via createProjects). AUTH_ARCH already assumes they are in the token; the plan could add one sentence on the claim shape for roles and permissions.
- **403 payload:** AUTH_ARCH §5.1 gives a structured 403 body. The plan’s CommandResult and validation failure shapes are in §3.1; it doesn’t define a global error DTO for 403. If the API should return a consistent error shape for both validation and authz failures, that could be stated in the plan or in AUTH_ARCH (e.g. shared `error`, `message`, `timestamp`).
- **Handler chain config:** AUTH_ARCH §2.9 uses XML and a package `com.rreganjr.command` / `com.rreganjr.requel.command`. The codebase may use Java config or a different package layout; the concepts (order of handlers, ExceptionMapping wrapping Authorizing so AuthorizationException becomes 403) are what matter and are clear.

**Conclusion:** AUTH_ARCH is coherent, matches the plan’s authz summary, and is the right place for the detailed authorization design. No structural changes are needed; the two docs are consistent.

---

### 3. Remaining Gaps and Suggestions

**3.1 User Guide**

The plan still does not say what "User Guide" in the main layout is (static link, in-app route, or external URL). **Suggestion:** One sentence in the plan or in the Phase 0 frontend work: e.g. "User Guide links to [static doc / route / external URL]."

**3.2 JWT role and permission claim shape**

For implementers, a single place that defines the JWT payload (e.g. `sub`, `roles`, `permissions`, `exp`) and how domain roles map to role strings avoids mismatches between backend and Angular. **Suggestion:** In the plan §3.4 or in AUTH_ARCH §4.1, add a short "JWT claim convention" note: e.g. `roles` = list of authority strings (e.g. `SYSTEM_ADMIN`, `PROJECT_USER`), `permissions` = list of role-level permission names (e.g. `createProjects`), and that these are derived from the domain `User` when issuing the token.

**3.3 GET /api/projects — filtering**

The plan says "GET /api/projects returns only projects where the user is a stakeholder (or all for SystemAdmin)." AUTH_ARCH describes `ProjectAccessChecker.canAccessProject` and `requireStakeholder`. **Suggestion:** In the plan’s query list or in Phase 2, explicitly state that the projects list endpoint uses `ProjectAccessChecker` (or equivalent) to filter by current user (stakeholder or SystemAdmin). This is already implied; making it explicit helps test and review.

**3.4 Phase 9 and my-permissions**

Phase 9 in the plan is "Open Issues" (and no longer a separate "Angular Auth" phase). AUTH_ARCH §6 says "Phase 9 (Angular Auth): Implement PermissionService, add GET my-permissions, wire permission checks." **Suggestion:** Ensure the plan’s phase list either (a) includes the Angular PermissionService and `GET /api/projects/{projectId}/my-permissions` in an explicit phase (e.g. Phase 0 or when project UI is first built, or a dedicated "Authorization UI" step), or (b) states that PermissionService and my-permissions are implemented incrementally as each feature (e.g. goals, stakeholders) is added. Right now Phase 0 does not list my-permissions; AUTH_ARCH’s "Phase 9" may be logical (all authz UI) rather than the plan’s phase index. Aligning the two (e.g. "PermissionService and my-permissions introduced in Phase 2 when project tree and project-scoped UI appear") would avoid ambiguity.

---

### 4. Summary

- The plan now covers **environment, versions, authentication, current-user resolution, Angular token flow, and SSE (fetch-based, no token in URL)**. The original review’s gaps are closed.
- **AUTH_ARCH.md** is the right and sufficient place for authorization design. It aligns with the plan’s §3.6 and Phase 0 authz tasks; no structural changes recommended.
- **Remaining nits:** Define User Guide behavior; document JWT role/permission claim convention; make projects-list filtering explicit; clarify which phase delivers PermissionService and my-permissions so the plan and AUTH_ARCH phase references stay in sync.

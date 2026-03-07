# UI Refactor Plan — Completeness Review

This document reviews [doc/UI_REFACTOR_PLAN.md](UI_REFACTOR_PLAN.md) for gaps and suggests additions. It focuses on: environment and versions, Echo2 vs Angular behavior, authentication/authorization (Spring Security and Angular), and token handling.

---

## 1. What the Plan Covers Well

- **Motivation and current state** — Clear rationale, screen inventory, Echo2 panel architecture, and panel counts by module.
- **Target architecture** — CQRS API (command dispatch + query endpoints), composite CommandFactory, DTO and response conventions.
- **Frontend stack** — Angular 21 and PrimeNG 21 are specified with version table, conventions (standalone, signals, Vitest), and PrimeNG rationale; project structure and shared components are listed.
- **Migration phases** — Screen-by-screen phases with backend/frontend work items and Echo2 panels replaced.
- **API design** — Command type inventory, query list, SSE event stream, special cases (login, import).
- **Decisions** — PrimeNG vs alternatives, monorepo, JWT (stateless, in-memory), SSE vs WebSocket, deployment (static from Spring Boot).

---

## 2. Environment, Libraries, and Versions

### 2.1 Gaps

| Area | Gap | Suggestion |
|------|-----|------------|
| **Backend versions** | Plan does not list Spring Boot, Java, or Maven. | Add a “Backend versions” subsection: **Spring Boot** 3.3.x (from parent POM), **Java** 17, **Maven** 3.x. Note that `service-api` / `service-impl` will inherit from the same parent. |
| **JWT library** | JWT generation/validation library is not named. | Specify dependency (e.g. **jjwt** `io.jsonwebtoken:jjwt-api` + impl) and that HS256, 8-hour expiry, and claim set (sub, roles, exp) are implemented in a small utility used by the auth endpoint and the JWT filter. |
| **Spring Security version** | Not called out. | State that Spring Security comes from `spring-boot-starter-security` (Spring Boot 3.3.x) and that the JWT filter integrates with the servlet filter chain. |
| **CORS** | Phase 0 mentions “CORS configuration for Angular dev server” but not shape. | Specify that in dev, `allowedOrigins` includes `http://localhost:4200` and that in production (same-origin after Phase 10) CORS can be disabled or restricted to the single origin. |
| **Environment variables** | No mention of config for API base URL, JWT secret, or env-specific settings. | Add: **JWT signing secret** from config (e.g. `requel.jwt.secret`) or env var, not hardcoded; **Angular** `environment` or config for API base URL (dev: `http://localhost:8081`, prod: relative or same-origin). Optionally Node LTS policy (e.g. Node 20 LTS for CI). |

### 2.2 Summary Recommendation

Add a short **“Environment and versions”** section (or subsections under §2 and §3) that lists:

- Backend: Java 17, Spring Boot 3.3.x, Spring Security (from starter), jjwt (or chosen library).
- Frontend: Angular 21.x, PrimeNG 21.x, Node 18+ (or 20 LTS).
- Config: JWT secret, API base URL, CORS for dev vs prod.

---

## 3. Echo2 vs Angular — Behavioral Gaps

### 3.1 How Echo2 Works Today (for comparison)

- **Current user** — Echo2 holds the logged-in user in **app state** (`getApp().getUser()`), set after `LoginCommand` succeeds via `LoginOkEvent` / `LoginOkController`. It is **not** HTTP session or Spring Security context; the existing `WebSecurityConfig` uses `anyRequest().permitAll()` and an `InMemoryUserDetailsManager` that is not used for Echo2 login.
- **Login** — `LoginController` → `LoginCommand` (username/password) → `UserRepository.findUserByUsername` + `user.isPassword(password)`. No Spring Security authentication in the Echo2 path.
- **Logout** — `EchoPMLogoutServlet` at `/logout`; clears Echo2 app state.
- **Authorization in domain** — Commands and panels use **domain roles**: `user.hasRole(SystemAdminUserRole.class)`, `hasRole(ProjectUserRole.class)`, and **stakeholder context** (e.g. `project.getUserStakeholder(user)` for edit permissions). These are not HTTP-level checks.

### 3.2 Gaps in the Plan

| Gap | Detail | Suggestion |
|-----|--------|------------|
| **Current user on the backend** | Echo2 passes “current user” into every command via `setEditedBy(getCurrentUser())`. The plan does not say how the API gets “current user” from the JWT and into the command. | Specify that after JWT validation the filter (or a custom `Authentication` implementation) sets the principal (e.g. username or a minimal User identity). The **CommandController** (or a shared service) must **resolve the principal to a domain `User`** and call `command.setEditedBy(currentUser)` before `commandHandler.execute(command)`. Document where this resolution lives (e.g. `CurrentUserResolver` backed by `UserRepository.findUserByUsername`). |
| **Role-based UI in Angular** | Plan says JWT carries “username and roles” but does not describe how Angular uses roles (e.g. hide Users tab for non-admin, disable role editing for non–SystemAdmin). | Add a short “Authorization in the Angular app” note: e.g. `UserDto` (or `/api/auth/me`) includes `roles: string[]`; Angular uses a guard or directive to show/hide routes or controls by role; list which routes/actions are restricted (e.g. Users tab, Edit User roles, NLP). |
| **Stakeholder-based permissions** | Echo2 uses `project.getUserStakeholder(user)` to decide if the user can edit a project/entity. Plan does not describe how the API enforces “user is stakeholder of this project” for commands/queries. | Clarify: either (a) **backend** enforces per-command (load project, check stakeholder, then execute), or (b) **query endpoints** filter by “projects the user is stakeholder of” and commands accept only IDs the user is allowed to touch. Prefer (a) for security. Mention that this is the same rule as Echo2 (stakeholder = can edit), and that role checks (e.g. SystemAdmin) remain in command implementations. |

### 3.3 Summary Recommendation

- Add **“Backend: resolving current user for commands”** — JWT → principal → domain `User` → `command.setEditedBy(currentUser)` before execute.
- Add **“Angular: role-based visibility”** — roles from token/me, guards/directives, and which features are role-restricted.
- Add **“Stakeholder and role authorization”** — that API enforces project/stakeholder and that domain role checks (`hasRole`, etc.) remain in command layer; no need to duplicate every check in the plan if the principle is stated.

---

## 4. Authentication and Authorization (Spring Security)

### 4.1 Gaps

| Gap | Detail | Suggestion |
|-----|--------|------------|
| **Request-level security** | Plan says “JWT filter” and “stateless” but does not define which paths require authentication. | Specify: **Public:** `POST /api/auth/login` (and optionally health/actuator). **Authenticated:** all other `/api/**` (including `GET /api/auth/me`, `GET /api/events/stream`). Use `requestMatchers("/api/auth/login").permitAll()` and `requestMatchers("/api/**").authenticated()` (or equivalent). |
| **JWT filter placement** | Filter order relative to Spring Security matters. | State that the JWT filter runs **before** Spring Security’s filter chain (or as the first security filter), validates the token, and on success sets `SecurityContextHolder.getContext().setAuthentication(...)` with an `Authentication` whose principal is the username (or a UserDetails/domain User adapter). On invalid/missing token for protected paths, return 401. |
| **Login endpoint and password verification** | Plan says “accepts `{ username, password }`” but not how the backend verifies the password. | Specify: login endpoint loads user by username (e.g. `UserRepository.findUserByUsername`), verifies password using the **existing** mechanism (e.g. `user.isPassword(rawPassword)`), then issues JWT. If identity is later moved to `platform-identity` (see IDENTITY_MIGRATION.md), password verification will use that layer; for now, align with current `LoginCommandImpl` behavior. |
| **Password storage** | Not stated whether passwords are hashed in the DB. | Note that the plan assumes the current storage format (hashed or not) is unchanged; JWT login reuses the same verification as `LoginCommand`. If passwords are not hashed, recommend a separate task to introduce hashing before or during the migration. |
| **Role-based endpoint security** | Plan does not say if any API endpoints are restricted by role (e.g. only ADMIN for user management). | Decide and document: either (a) **endpoint-level** (e.g. `@PreAuthorize("hasRole('ADMIN')")` on user-admin endpoints) or (b) **command-level only** (existing `hasRole` checks inside commands). If (a), list which endpoints or command types are admin-only (e.g. NewUser, EditUser, GET /api/users). |

### 4.2 Summary Recommendation

Add a subsection **“Spring Security and JWT (backend)”** that covers:

- Public vs authenticated paths (`/api/auth/login` vs `/api/**`).
- JWT filter: validate token, set `SecurityContext`, return 401 for invalid/missing on protected paths.
- Login: load user, verify password (same as current LoginCommand), then issue JWT.
- Optional: endpoint-level role restrictions and where they apply.
- Reference to resolving JWT principal to domain `User` for `setEditedBy` (can link to “Current user on the backend” above).

---

## 5. Angular: Token Handling and Auth Flow

### 5.1 What the Plan Already Says

- JWT stored in memory (service field); not localStorage/cookies.
- Route guards check token presence and expiry (decode `exp` client-side).
- On 401, redirect to login.
- HTTP interceptor adds `Authorization: Bearer <token>`.
- SSE: token passed as query param because `EventSource` does not support headers.

### 5.2 Gaps

| Gap | Detail | Suggestion |
|-----|--------|------------|
| **Where the token is stored** | “Service field” is correct; the plan could be more explicit for implementers. | Specify: e.g. `AuthService` holds `private readonly token = signal<string \| null>(null)`; `login()` calls `POST /api/auth/login`, then `this.token.set(response.token)` and stores `user` (e.g. for display); `logout()` calls `this.token.set(null)`. No persistence. |
| **Expiry handling** | “Route guards check … expiry” is good; the exact behavior is not specified. | Specify: guard reads token from AuthService, decodes JWT (e.g. base64 payload) and checks `exp` (in seconds); if missing or expired, redirect to login and clear token. Optionally: on any 401 response, interceptor clears token and redirects to login so expired tokens are handled even without navigation. |
| **Restoring state after refresh** | Plan says token is cleared on refresh; it does not mention `/api/auth/me` for “restoring state on page load if token is still in memory.” With in-memory only, after refresh there is no token, so no restore. | Clarify: with **in-memory-only** storage, after a full page refresh the user must log in again; `GET /api/auth/me` is used when the app **already** has a token (e.g. after reconnecting or on initial load from a tab that kept the app alive) to refresh the current user DTO. So: on app init, if there is a token, call `GET /api/auth/me` to get `UserDto` for header/layout; if that returns 401, clear token and redirect to login. |
| **SSE and token in URL** | Passing JWT in query param has security implications (logs, Referer). | Note the tradeoff: EventSource does not support custom headers; so either pass token in query param (simpler, but token in URL) or use a different transport. Plan already chooses query param; add one sentence that the token appears in server logs and URL bar and that for this app the risk is accepted, or that SSE URL is not logged. |

### 5.3 Summary Recommendation

Add a short **“Angular auth and token flow”** subsection that describes:

- Where the token lives (e.g. `AuthService` signal), and that it is not persisted.
- Login: set token and user; logout: clear token (and disconnect SSE).
- Guard: validate token and `exp`; redirect to login and clear token if invalid/expired.
- 401 interceptor: clear token and redirect to login.
- Use of `GET /api/auth/me` when a token is already in memory (e.g. on init) to load current user; on 401 from me, clear token and redirect.
- SSE: token in query param by necessity; brief note on logging/URL exposure.

---

## 6. Other Minor Gaps

- **User Guide** — Main layout mentions “User Guide”; plan does not say whether it remains a static link, a route in the Angular app, or an external URL. Recommend one sentence.
- **NLP panel** — Plan correctly states it is not migrated; no gap.
- **Document generation (XSLT)** — Decided to keep; no gap.

---

## 7. Suggested Additions to UI_REFACTOR_PLAN.md

1. **§2.5 or §3.x** — “Environment and versions”: backend (Java 17, Spring Boot 3.3.x, Security, jjwt), frontend (Angular 21, PrimeNG 21, Node), config (JWT secret, API URL, CORS).
2. **§3.4** — Expand “Authentication” into:
   - **Backend:** Public vs authenticated paths; JWT filter (validate, set SecurityContext, 401); login (password verification same as current LoginCommand); resolving JWT principal to domain `User` for `setEditedBy`; optional endpoint-level role restrictions.
   - **Angular:** Token storage (e.g. AuthService signal); login/logout; guard (presence + expiry); 401 interceptor; use of `/api/auth/me` when token present; SSE token in query param and caveat.
3. **§3.x (new)** — “Authorization”: backend enforcement of stakeholder/project and existing domain role checks; Angular role-based visibility (roles from token/me, guards/directives, list of restricted features).
4. **Phase 0 backend** — Explicit task: “Resolve JWT principal to domain User and set `command.setEditedBy(currentUser)` before execute in CommandController or a dedicated service.”

This review does not change the overall architecture or phase order; it fills in implementation and security detail so the plan is complete enough for implementation and code review.

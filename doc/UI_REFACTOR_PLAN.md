# UI Refactor Plan: Echo2 → Angular

## 1. Motivation

The Echo2 framework is a legacy Java RIA that renders server-side components over HTTP. It has no community, no updates, and requires a custom javax→jakarta transform to work with Spring Boot 3. Replacing it with Angular gives us:

- A modern, maintainable frontend with active community support
- Clean separation between backend API and frontend rendering
- A command-based API that maps directly to the existing domain command pattern
- Query endpoints for reads, command dispatch for writes (CQRS)
- Independent development cycles (Angular dev server on port 4200, Spring Boot on 8081) with single-artifact deployment

## 2. Current State

### 2.1 Screens Inventory (from live app inspection)

| Screen | Echo2 Panel(s) | Description |
|--------|---------------|-------------|
| **Login** | `LoginController` | Username/password form |
| **Main Layout** | `RequelMainScreen`, `MainScreenTabbedNavigation` | Header bar (Edit Account, User Guide, Logout), left nav tree, right content area, tab strip |
| **Users Tab** | `UserAdminNavigatorPanel` | Tree of all users, "New User" button |
| **Edit User** | `UserEditorPanel` | Form: username, password, name, org, email, phone, role tree (checkboxes) |
| **Edit Account** | `UserEditorPanel` (reused) | Same form for current user's own account |
| **Projects Tab** | `ProjectNavigatorPanel` | Tree of projects with sub-items, "New Project"/"Import Project" buttons |
| **New Project** | `ProjectOverviewPanel` (create mode) | Form: name, description, organization (combo) |
| **Import Project** | `ProjectImportPanel` | Form: rename field, file upload, enable analysis checkbox |
| **Edit Project** | `ProjectOverviewPanel` (edit mode) | Form: name, description, org, createdBy, annotations table, Export button |
| **Stakeholders List** | `StakeholderNavigatorPanel` | Table: Name, User?, Team, Email, Phone, Created By, Date. Buttons: Add Non-User, Add User |
| **Edit User Stakeholder** | `UserStakeholderEditorPanel` | Stakeholder details for a system user |
| **Edit Non-User Stakeholder** | `NonUserStakeholderEditorPanel` | Stakeholder details for an external authority |
| **Goals List** | `GoalNavigatorPanel` | Table: Name, Created By, Date. Button: Add |
| **Edit Goal** | `GoalEditorPanel` | Form: name, text, goal relations, annotations table |
| **Goal Relations** | `GoalRelationEditorPanel` | Supporting/conflicting goal relationships |
| **Stories List** | `StoryNavigatorPanel` | Table: Name, Type, Created By, Date. Button: Add |
| **Edit Story** | `StoryEditorPanel` | Form: name, text, type selector, annotations table |
| **Actors List** | `ActorNavigatorPanel` | Table: Name, Description, Created By, Date. Button: Add |
| **Edit Actor** | `ActorEditorPanel` | Form: name, description, annotations table |
| **Use Cases List** | `UseCaseNavigatorPanel` | Table: Name, Primary Actor, Created By, Date. Button: Add |
| **Edit Use Case** | `UseCaseEditorPanel` | Form: name, description, primary actor selector, goals, stories, scenarios, annotations table |
| **Scenarios List** | `ScenarioNavigatorPanel` | Table: Top Level, Name, Type, Created By, Date. Button: Add |
| **Edit Scenario** | `ScenarioEditorPanel` | Form: name, type, scenario steps (tree), annotations table |
| **Terms (Glossary) List** | `GlossaryTermNavigatorPanel` | Table: Name, Definition, Canonical Term, Created By, Date. Button: Add |
| **Edit Term** | `GlossaryTermEditorPanel` | Form: name, definition, canonical term reference, annotations table |
| **Documents List** | `ReportGeneratorNavigatorPanel` | Table: Name, Created By, Date. Buttons: Edit, Run |
| **Edit Document** | `ReportGeneratorEditorPanel` | Document template configuration |
| **Open Issues** | `ProjectOpenIssuesNavigatorPanel` | Table: Annotatables, Text, Created By, Date (read-only) |
| **Edit Issue** | `IssueEditorPanel` | Form: text, status, must-be-resolved, positions list |
| **Edit Position** | `PositionEditorPanel` | Form: text, arguments list |
| **Edit Argument** | `ArgumentEditorPanel` | Form: text, supports/opposes |
| **Edit Note** | `NoteEditorPanel` | Form: text |
| **Selector Dialogs** | `*SelectorPanel` (6 total) | Modal tables for picking actors, goals, stories, use cases, scenarios, terms |
| **NLP Parser** | `ParserPanel`, `NLPNavigatorPanel` | NLP analysis tool (admin/debug) — not migrated to Angular, see Section 8 |

### 2.2 Existing API Surface

Only one REST endpoint exists today:

- `GET /projectxml?project=<name>` — exports project as XML (`ProjectXmlController`)

Everything else is Echo2 server-side rendering with no HTTP API.

### 2.3 Echo2 Panel Architecture

```
Panel → CommandFactory → Command → CommandHandler → Repository
  ↑                                                      |
  └──── refresh via events (UpdateEntityEvent) ──────────┘
```

- **Editor panels** handle CRUD for a single entity
- **Navigator panels** display lists (tables or trees) for browsing
- **Selector panels** are modal pickers for entity references
- All mutations go through the **Command pattern** (never direct repository writes)
- Cross-panel communication uses **Echo2 events** (UpdateEntityEvent, DeletedEntityEvent, OpenPanelEvent)

### 2.4 Echo2 Panel Counts by Module

| Module | Editors | Navigators | Selectors | Other | Total |
|--------|---------|------------|-----------|-------|-------|
| project-ui | 8 | 8 | 6 | 5 controllers, 4 factories | 31 |
| annotation-ui | 4 | 0 | 0 | 2 tables, 1 base | 7 |
| user-ui | 1 | 1 | 0 | 2 controllers, 2 factories | 6 |
| nlp-ui | 1 | 1 | 0 | 1 factory | 3 |
| ui-core | 0 | 0 | 0 | 5 base classes, 2 containers | 7 |

## 3. Target Architecture

### 3.1 Backend: CQRS API Module (`service-api` + `service-impl`)

New Maven modules that expose domain operations as a hybrid CQRS API — a command dispatch endpoint for writes and simple query endpoints for reads:

```
service-api/         → ApiCommand interface, CommandRegistry, CommandRegistration,
                       input DTOs (Java records), query response DTOs, CommandResult
service-impl/        → Composite CommandFactory facade, CommandController,
                       query controllers, security config
```

#### Architecture Overview

The CQRS split separates the write path (command dispatch) from the read path (query endpoints). The key design decision is that the existing domain Command pattern is preserved — the new API is an adapter layer on top of it, not a replacement. Existing Command classes implement an `ApiCommand<T>` interface, which keeps field-mapping logic inside the Command where domain knowledge lives.

The write side uses a **composite CommandFactory** pattern: a top-level `CommandFactory` facade delegates to per-domain factories (`ProjectCommandFactory`, `UserCommandFactory`, `AnnotationCommandFactory`). Each domain factory registers its command type mappings at startup — command type string → input DTO class + factory method. The top-level factory provides a single `newCommand(type, input)` entry point that handles lookup, creation, and input application. This avoids a separate `CommandDispatcher` and `CommandRegistry` while preserving domain boundaries.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Angular SPA (requel-angular/)                                      │
│                                                                     │
│  CommandService ──POST──┐         QueryService ──GET──┐             │
└─────────────────────────┼─────────────────────────────┼─────────────┘
                          │                             │
                          ▼                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│  Spring Boot  (service-api / service-impl)                          │
│                                                                     │
│  ┌──────────────── WRITE SIDE ────────────────┐  ┌── READ SIDE ──┐ │
│  │                                            │  │               │ │
│  │  POST /api/commands/{commandType}          │  │  GET /api/... │ │
│  │       │                                    │  │       │       │ │
│  │       ▼                                    │  │       ▼       │ │
│  │  CommandController                         │  │  Query         │ │
│  │       │                                    │  │  Controllers  │ │
│  │       ▼                                    │  │       │       │ │
│  │  CommandFactory (composite facade)         │  │       ▼       │ │
│  │       │                                    │  │  Repositories │ │
│  │       ├─ getInputType("EditGoal")          │  │       │       │ │
│  │       │   → EditGoalInput.class            │  │       ▼       │ │
│  │       │   (for JSON deserialization)       │  │  DTOs → JSON  │ │
│  │       │                                    │  │               │ │
│  │       ├─ newCommand("EditGoal", inputDTO)  │  └───────────────┘ │
│  │       │   → delegates to                   │                    │
│  │       │     ProjectCommandFactory          │                    │
│  │       │     .newEditGoalCommand()          │                    │
│  │       │   → command.applyInput(inputDTO)   │                    │
│  │       │   → returns ready-to-execute cmd   │                    │
│  │       │                                    │                    │
│  │       ▼                                    │                    │
│  │  CommandHandler.execute(command)            │                    │
│  │       │                                    │                    │
│  │       └─ return CommandResult (DTO + type)  │                    │
│  │                                            │                    │
│  └────────────────────────────────────────────┘                    │
│                                                                     │
│  ┌─────────────── DOMAIN LAYER ──────────────────────────────────┐ │
│  │  Per-domain factories register at startup:                    │ │
│  │    ProjectCommandFactory  → EditGoal, NewGoal, NewStory, ... │ │
│  │    UserCommandFactory     → NewUser, EditUser                 │ │
│  │    AnnotationCommandFactory → NewIssue, NewNote, ...          │ │
│  │                                                               │ │
│  │  Commands ──implement──▶ ApiCommand<T>.applyInput(T input)    │ │
│  │  Handler chain: RetryOnLockFailures → ExceptionMapping       │ │
│  │    → AuthorizingCommandHandler → AnalysisInvoking → Default  │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

#### Write Side: Composite CommandFactory

All mutations go through a single dispatch endpoint:

```
POST /api/commands/{commandType}
```

The body is a JSON payload specific to the command type. A `CommandController` receives the request, asks the composite `CommandFactory` for the input DTO type (for deserialization), then calls `newCommand(type, input)` to get a ready-to-execute command, and passes it to `CommandHandler`.

Each existing Command class implements a new `ApiCommand<T>` interface:

```java
public interface ApiCommand<T> {
    void applyInput(T input);
}
```

The input type `T` is a simple DTO (Java record) that matches the JSON shape the Angular frontend sends.

The composite `CommandFactory` is a facade over the existing per-domain factories. Each domain factory registers its command types at startup:

```java
// ProjectCommandFactory registers its commands
@PostConstruct
void registerCommands() {
    registry.register("NewGoal",  NewGoalInput.class,  this::newNewGoalCommand);
    registry.register("EditGoal", EditGoalInput.class,  this::newEditGoalCommand);
    registry.register("NewStory", NewStoryInput.class,  this::newNewStoryCommand);
    // ...
}
```

The top-level factory provides a unified entry point:

```java
// CommandFactory facade
public Class<?> getInputType(String commandType) {
    return registry.lookup(commandType).inputClass();
}

public Command newCommand(String commandType, Object input) {
    CommandRegistration<?> reg = registry.lookup(commandType);
    Command cmd = reg.factoryMethod().get();       // create via domain factory
    ((ApiCommand) cmd).applyInput(input);           // set fields from DTO
    return cmd;                                     // ready to execute
}
```

**Example — editing a goal:**
```
POST /api/commands/EditGoal
{ "projectId": 42, "goalId": 7, "name": "Easy to use", "text": "..." }

→ CommandController receives request
→ commandFactory.getInputType("EditGoal")  → EditGoalInput.class
→ Deserialize JSON body as EditGoalInput
→ commandFactory.newCommand("EditGoal", editGoalInput)
    → delegates to ProjectCommandFactory.newEditGoalCommand()
    → command.applyInput(editGoalInput)    // sets name, text on the command
→ commandHandler.execute(command)          // persists via repository
→ Returns CommandResult { success: true, entity: GoalDto, entityType: "Goal" }
```

**Command response format:**
```json
{
  "success": true,
  "entity": { "id": 7, "name": "Easy to use", "text": "...", "createdBy": "admin", ... },
  "entityType": "Goal"
}
```

**Validation failure:**
```json
{
  "success": false,
  "error": "Validation failed",
  "violations": [
    { "field": "name", "message": "a name is required." }
  ]
}
```

#### Read Side: Query Endpoints

Reads are simple GET endpoints that call Repositories and return DTOs. No command infrastructure needed:

```
GET /api/projects                         → list projects
GET /api/projects/{id}                    → project detail
GET /api/projects/{id}/goals              → goals in project
GET /api/projects/{id}/goals/{gid}        → single goal
GET /api/annotations?entityType=&entityId= → annotations for any entity
```

**List response format:**
```json
{
  "items": [ { "id": 7, "name": "Easy to use", ... }, ... ],
  "total": 12,
  "page": 0,
  "pageSize": 25
}
```

Single-entity endpoints return the DTO directly. All entities include `id`, `createdBy`, `dateCreated`, and `version` (for optimistic locking).

This mirrors how the Echo2 panels already work — they use CommandFactory/CommandHandler for writes and Repositories for reads. The CQRS API simply makes that existing split explicit over HTTP.

#### Special Cases (Not Through Command Dispatch)

```
POST /api/auth/login                  → returns JWT + UserDto
POST /api/projects/import             → multipart file upload
```

These don't fit the command dispatch pattern — auth is infrastructure, and import involves file upload that needs multipart handling. Logout is entirely client-side (discard the JWT from memory); no server endpoint is needed since tokens are stateless.

### 3.2 Frontend: Angular SPA (`requel-angular/`)

Standalone Angular project (separate from Maven build) served as static assets. Uses **Angular 21** and **PrimeNG 21**.

#### Versions

| Layer | Library | Version | Notes |
|---|---|---|---|
| **Backend** | Java | 17 | Required by Spring Boot 3.x |
| | Spring Boot | 3.3.x | From parent POM. `service-api`/`service-impl` inherit from the same parent. |
| | Spring Security | (from starter) | `spring-boot-starter-security`, servlet filter chain |
| | JWT | jjwt (`io.jsonwebtoken:jjwt-api` + impl) | HS256 signing, 8-hour expiry, claims: sub, roles, exp |
| | Maven | 3.6.3+ | Build tool |
| **Frontend** | Angular | 21.x | Standalone components, signals, Vitest |
| | PrimeNG | 21.x | Tracks Angular major. Requires `@angular/core ^21.0`. |
| | Node.js | 18+ (20 LTS recommended) | Required by Angular CLI |

PrimeNG aligns its major version with Angular — PrimeNG 19 targets Angular 19, PrimeNG 20 targets Angular 20, etc. Always use matching majors.

#### Environment Configuration

| Setting | Dev | Production | Source |
|---|---|---|---|
| **JWT signing secret** | Config default (e.g. `requel.jwt.secret` in `application.properties`) | Environment variable or external config — **never hardcoded** | Spring Boot config |
| **API base URL** | `http://localhost:8081` (Angular `proxy.conf.json` proxies `/api` to Spring Boot) | Same-origin (static from JAR) — no config needed | Angular `environment.ts` / proxy |
| **CORS allowed origins** | `http://localhost:4200` (Angular dev server) | Disabled or restricted to single origin (same-origin after Phase 10) | Spring Security CORS config |

#### Angular 21 Conventions

The Angular project uses modern Angular 21 conventions throughout:

- **Standalone components** — no `NgModule` declarations. Each component imports its dependencies directly via the `imports` array in `@Component()`. PrimeNG 21 components are imported the same way.
- **Signals** — use `signal()`, `computed()`, and `effect()` for reactive state instead of RxJS `BehaviorSubject` where possible. PrimeNG 21 is signal-aware.
- **`app.config.ts`** — application configuration via `provideRouter()`, `provideHttpClient()`, `provideAnimationsAsync()`, `providePrimeNG()`. No `AppModule`.
- **Functional guards and interceptors** — route guards and HTTP interceptors as functions, not classes.
- **Vitest** — default test runner in Angular 21, replaces Karma/Jasmine.

#### Why PrimeNG

PrimeNG (MIT license, maintained by PrimeTek) is chosen because Requel's UI is data-table-heavy — every navigator screen is a sortable, filterable table, and several screens need tree or tree-table views. PrimeNG provides these out of the box:

- **`p-table`** — sorting, filtering, pagination, row expansion, lazy loading, virtual scrolling, column reorder, Excel export. Replaces all NavigatorPanel tables.
- **`p-treeTable`** — hierarchical data in tabular format with sorting/filtering/pagination. Maps to scenario step tree-tables.
- **`p-tree`** — tree view with built-in drag-and-drop (`draggableNodes`/`droppableNodes`). Maps to project sidebar tree and scenario step reordering.
- **`p-dialog`** — modal dialogs for entity selectors.
- **`p-autoComplete`** — typeahead for organization combo and entity search.
- **`p-tabView`** — tab strip for open editor panels.

PrimeNG is design-agnostic with free themes (Aura, Lara, Nora) and ships Material/Bootstrap/Fluent theme presets. All 90+ components are included in the open-source MIT package — premium add-ons (theme designer, PrimeBlocks templates) are optional and not needed.

#### Reference Links

**Angular 21:**
- Docs: https://angular.dev/
- Releases: https://angular.dev/reference/releases
- Signals guide: https://angular.dev/guide/signals

**PrimeNG 21:**
- Official docs: https://primeng.org/
- Installation: https://primeng.org/installation
- Table: https://primeng.org/table
- TreeTable: https://primeng.org/treetable
- Tree: https://primeng.org/tree
- LTS support: https://primeng.org/lts
- GitHub: https://github.com/primefaces/primeng
- npm: `npm install primeng @primeng/themes primeicons`

#### Project Structure

```
requel-angular/
  src/
    app/
      core/           → auth service, HTTP interceptors, guards, CommandService, EventStreamService
      shared/         → annotation components, entity table wrapper, selector dialog
      features/
        auth/         → login, edit-account
        users/        → user list, user editor
        projects/     → project list, new/import/edit project
        stakeholders/ → stakeholder list, user/non-user editors
        goals/        → goal list, goal editor, goal relations
        stories/      → story list, story editor
        actors/       → actor list, actor editor
        use-cases/    → use-case list, use-case editor
        scenarios/    → scenario list, scenario editor, step tree
        terms/        → glossary list, term editor
        documents/    → document list, document editor
        issues/       → open issues, issue/position/argument editors
      models/         → TypeScript interfaces matching API DTOs
```

### 3.3 Shared Components (Angular + PrimeNG)

These patterns repeat across nearly every screen and should be built once. Each maps to specific PrimeNG components:

| Shared Component | PrimeNG Base | Description |
|---|---|---|
| **Entity list table** | `p-table` with sorting, pagination, selection | Sortable columns, pagination, Edit/Add/Delete toolbar. Wraps `p-table` with standard column config and action buttons. Replaces all NavigatorPanels. |
| **Annotations section** | `p-table` with row expansion | Issue/note table with Add Issue/Add Note. Row expansion shows positions; nested expansion shows arguments. Appears on every editor. |
| **Entity selector dialog** | `p-dialog` + `p-table` with global filter | Modal with searchable, selectable table for picking related entities (goals, actors, stories, etc.). Replaces all SelectorPanels. |
| **Organization combo** | `p-autoComplete` | Typeahead dropdown that searches existing orgs or allows free-text entry. Used on user and project forms. |
| **Project tree nav** | `p-tree` | Sidebar tree with project → sub-item hierarchy. Click navigates to the entity list or editor. |
| **Tab strip** | `p-tabView` | Open editor panels as closeable tabs, matching the current Echo2 tab bar behavior. |
| **Scenario step tree** | `p-tree` with `draggableNodes` | Drag-and-drop reorderable tree for scenario steps with type indicators (normal, alternate, exception). |

### 3.4 Authentication

Replace Echo2's app-level login (which bypasses Spring Security entirely — the existing `WebSecurityConfig` uses `anyRequest().permitAll()`) with real HTTP-level JWT authentication.

#### Backend: Spring Security and JWT

**Request-level security:**
```
Public (no token required):
  POST /api/auth/login
  GET  /actuator/health          (if exposed)

Authenticated (valid JWT required):
  All other /api/**              (commands, queries, SSE stream, /api/auth/me)
```

Configuration: `requestMatchers("/api/auth/login").permitAll()` and `requestMatchers("/api/**").authenticated()`. Static resources (Angular app at `/`) are public.

**JWT filter:**
- Runs as the first filter in the Spring Security filter chain (before `UsernamePasswordAuthenticationFilter`).
- On each request to `/api/**`: extracts `Authorization: Bearer <token>` header, validates signature (HS256) and expiry using jjwt.
- On success: creates an `Authentication` (e.g. `UsernamePasswordAuthenticationToken`) with the username as principal and roles as authorities, sets `SecurityContextHolder.getContext().setAuthentication(...)`.
- On invalid/missing token for authenticated paths: returns **401 Unauthorized**.

**Login endpoint — password verification:**
- `POST /api/auth/login` accepts `{ username, password }`.
- Loads user via `UserRepository.findUserByUsername(username)`.
- Verifies password using the **existing** mechanism: `user.isPassword(rawPassword)` — same as the current `LoginCommandImpl`.
- On success: generates JWT and returns `{ token, user: UserDto }`.

**JWT claim convention:**
```json
{
  "sub": "ron",
  "roles": ["SYSTEM_ADMIN"],
  "permissions": ["createProjects", "inviteUsers"],
  "exp": 1741305600
}
```
- `sub` — username (matches `UserRepository.findUserByUsername`)
- `roles` — authority strings derived from domain role classes: `SystemAdminUserRole` → `"SYSTEM_ADMIN"`, `ProjectUserRole` → `"PROJECT_USER"`
- `permissions` — role-level permission names from `UserRolePermission.getName()` (e.g. `"createProjects"`, `"inviteUsers"`). Angular uses these for UX decisions (show "New Project" button)
- `exp` — expiry (now + 8 hours, seconds since epoch)
- On failure: returns **401 Unauthorized**.
- Password storage format is unchanged from the current system. If passwords are not currently hashed, that is a separate task outside this migration.

**Resolving current user for commands:**
Echo2 passes the current user into every command via `setEditedBy(getCurrentUser())`. The API must do the same:
- After JWT validation, the filter sets the principal (username) in `SecurityContext`.
- The `CommandController` (or a shared `CurrentUserResolver` service) resolves the principal to a domain `User` via `UserRepository.findUserByUsername(principal)`.
- Before calling `commandHandler.execute(command)`, sets `command.setEditedBy(currentUser)`.
- Query controllers use the same resolution when filtering by user (e.g. "projects for current user").

#### Angular: Token Handling and Auth Flow

**Token storage:** `AuthService` holds the JWT as `private readonly token = signal<string | null>(null)`. Not persisted — cleared on page refresh, user must re-login.

**Login flow:**
1. User submits credentials → `POST /api/auth/login`
2. On success: `this.token.set(response.token)`, store `UserDto` in a `currentUser` signal (for header display, role checks)
3. Connect `EventStreamService` to SSE stream

**Logout flow:**
1. `this.token.set(null)`, clear `currentUser`
2. Disconnect `EventStreamService`
3. Navigate to login page

**App initialization (tab still alive after backgrounding):**
1. If token is in memory (no page refresh occurred), call `GET /api/auth/me` to refresh the `UserDto`
2. If `/api/auth/me` returns 401 (token expired), clear token and redirect to login

**HTTP interceptor (`authInterceptor`):**
- Adds `Authorization: Bearer <token>` to every outgoing `/api` request
- On 401 response from any endpoint: clears token, disconnects SSE, redirects to login

**Route guard (`authGuard`):**
- Reads token from `AuthService`, decodes JWT payload (base64), checks `exp` claim (seconds since epoch)
- If token missing or expired: clear token, redirect to login
- If valid: allow navigation

**SSE authentication:**
The SSE stream uses **fetch-based streaming** (not `EventSource`), so the JWT is sent in the standard `Authorization: Bearer` header — no token in URL. This eliminates the `EventSource` header limitation that would otherwise expose the JWT in server access logs and browser history. See Section 3.5 for the full fetch-based streaming architecture.

#### Endpoints

```
POST /api/auth/login    → { username, password } → { token, user: UserDto }
GET  /api/auth/me       → validates JWT → UserDto (current user)
```
No logout endpoint — client discards the token from memory.

### 3.5 Real-Time Updates: SSE Event Stream

User-initiated commands return results synchronously — the `CommandResult` gives immediate feedback. But background processing (NLP `AnalysisInvokingCommandHandler` adding issues/notes after writes, or other users editing concurrently) needs a push mechanism so clients see changes without manual refresh.

#### Approach: Commands return results + SSE for background events

```
User clicks "Save Goal"
  → POST /api/commands/EditGoal
  → returns CommandResult { entity: GoalDto } immediately
  → UI updates from the response

Meanwhile, AnalysisInvokingCommandHandler runs NLP in the background...
  → NLP agent adds an Issue to the Goal
  → SSE pushes: { eventType: "Data", targetType: "Goal", targetId: 7, payload: GoalDto }
  → UI merges the payload or re-fetches the entity
```

The command response is the user's "receipt" for their own action. The SSE stream carries events the user didn't initiate — background processing results, other users' edits, analysis completion.

#### Architecture Overview

The SSE system uses a **session-based subscription model**. The client opens a single HTTP streaming connection, subscribes to specific targets (e.g., a project or entity), and receives events only for those targets. The server tracks sessions and subscriptions and pushes events to the correct `SseEmitter` instances.

Key concepts:
- **Stream session** — a server-side session (UUID) that owns one `SseEmitter` and a set of subscriptions. Created when the client opens the stream, persisted in-memory.
- **Subscription** — a `targetType:targetId` pair (e.g., `Project:1`, `Goal:7`). Subscriptions can be included in the initial stream URL or added/removed dynamically via REST endpoints while the stream is open.
- **Event envelope** — all SSE data is JSON with `eventType`, `targetType`, `targetId`, and `payload` fields.

#### SSE Endpoints

```
GET    /api/events/stream?subscribe=Project:1&subscribe=Goal:7
                                     → opens SSE connection, creates session, returns SseEmitter
                                     → server sends Session event first, then initial payloads
                                     → authenticated via JWT in Authorization header (native fetch,
                                       not EventSource — see Angular Implementation below)

POST   /api/events/stream/subscriptions
       Header: X-Session-Id: {sessionId}
       Body:   { "targetType": "Goal", "targetId": 7 }
                                     → add subscription to existing session
                                     → server sends initial payload for the new target on the stream

DELETE /api/events/stream/subscriptions
       Header: X-Session-Id: {sessionId}
       Body:   { "targetType": "Goal", "targetId": 7 }
                                     → remove subscription from session

DELETE /api/events/stream/connection
       Header: X-Session-Id: {sessionId}
                                     → graceful server-side close: completes SseEmitter so browser
                                       gets a clean end-of-stream (avoids half-closed connections
                                       in Safari/Firefox)
```

#### Event Envelope

All SSE data is sent as JSON in `data:` lines:

```json
{ "eventType": "Session",          "payload": { "sessionId": "uuid-here" } }
{ "eventType": "Data",             "targetType": "Goal",    "targetId": 7,  "payload": { ... } }
{ "eventType": "TargetDeleted",    "targetType": "Story",   "targetId": 15 }
{ "eventType": "SESSION_EXPIRED" }
```

Event types:
- **Session** — sent immediately after connection opens, carries the server-assigned `sessionId` for subsequent subscription management calls.
- **Data** — a target was created or updated. Payload is the full DTO (invalidate-and-refetch is still an option, but full payloads avoid the extra round-trip).
- **TargetDeleted** — a subscribed target was deleted. Payload is null.
- **SESSION_EXPIRED** — JWT has expired (with grace period). Client should re-authenticate.

#### Server-Side Architecture

**`StreamService`** — manages stream sessions, `SseEmitter` lifecycle, keep-alive, and session expiry.

```java
@Service
public class StreamService {

    // In-memory map: sessionId → EmitterHolder (SseEmitter + scheduled futures)
    private final ConcurrentHashMap<String, EmitterHolder> localEmitters = new ConcurrentHashMap<>();

    /**
     * Create a new stream session or reattach to an existing one.
     * Sends Session event first, then initial payloads for each subscription.
     */
    public SseEmitter createStream(String existingSessionId, User user,
                                    Long jwtExpiresAtEpochMs,
                                    List<SubscriptionRef> initialSubscriptions) {
        String sessionId = existingSessionId != null
                ? existingSessionId : UUID.randomUUID().toString();

        SseEmitter emitter = new SseEmitter(-1L); // no timeout (Jakarta Servlet 6.0+)

        // Schedule 30-second keep-alive comments to prevent proxy/browser timeouts
        ScheduledFuture<?> keepAlive = scheduler.scheduleAtFixedRate(
                () -> sendKeepAlive(sessionId), new Date(), 30_000L);

        // Schedule session expiry: JWT expiry + 5-minute grace period
        ScheduledFuture<?> expiryFuture = jwtExpiresAtEpochMs != null
                ? scheduler.schedule(() -> sendSessionExpiredAndClose(sessionId),
                                     new Date(jwtExpiresAtEpochMs + 5 * 60_000))
                : null;

        localEmitters.put(sessionId, new EmitterHolder(emitter, keepAlive, expiryFuture));

        // Send Session event, then initial payloads
        sendEvent(sessionId, StreamEventEnvelope.session(sessionId));
        for (SubscriptionRef ref : initialSubscriptions) {
            sendInitialPayload(sessionId, ref);
        }

        // Register callbacks for cleanup
        emitter.onCompletion(() -> onEmitterDone(sessionId));
        emitter.onTimeout(() -> onEmitterDone(sessionId));
        emitter.onError(e -> onEmitterDone(sessionId));

        return emitter;
    }
}
```

Key behaviors:
- **Keep-alive** — sends SSE comment (`:`-prefixed) every 30 seconds to prevent proxies and browsers from closing idle connections.
- **Session expiry** — when the JWT expires (plus a 5-minute grace period), sends a `SESSION_EXPIRED` event and completes the emitter. The client re-authenticates and reconnects.
- **Graceful close** — `DELETE /api/events/stream/connection` calls `emitter.complete()`, which sends a clean end-of-stream. This is critical for Safari and Firefox, which leave connections in a half-closed state when the client aborts a `ReadableStream` via `AbortController`.
- **Stale holder guard** — `onEmitterDone` only cleans up if the holder is still the active one for that session (avoids a stale callback wiping a new connection on reconnect).

**`StreamSessionStore`** — tracks sessions and their subscriptions. For Requel (single instance), this is in-memory. We could use Redis for multi-instance deployments, but Requel doesn't need that complexity:

```java
// In-memory session store (could be upgraded to Redis later if needed)
@Service
public class StreamSessionStore {
    private final ConcurrentHashMap<String, StreamSessionData> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> targetToSessions = new ConcurrentHashMap<>();

    // targetToSessions: "Project:7" → { "session-uuid-1", "session-uuid-2" }
    // Used by pushToSubscribedSessions to find which emitters to push to.
}
```

**`StreamEventPublisher`** — called by domain code (command handlers, NLP assistants) to push events to subscribed sessions:

```java
public interface StreamEventPublisher {
    void publishTargetUpdate(String targetType, Long targetId, Object payload);
    void publishTargetDeleted(String targetType, Long targetId);
}
```

**Publishing flow:**
```
AnalysisInvokingCommandHandler → command.invokeAnalysis()
  → NLP assistant creates Issue on Goal
  → streamEventPublisher.publishTargetUpdate("Goal", 7, goalDto)
  → StreamService.pushToSubscribedSessions("Goal", 7, goalDto)
    → looks up sessionIds subscribed to "Goal:7"
    → sends Data event via each session's SseEmitter
```

#### Angular Implementation: Fetch-Based Streaming

We use **native `fetch()` with `ReadableStream`** instead of the browser's `EventSource` API. This solves the key limitation of `EventSource`: it does not support custom headers (Authorization), forcing JWT into the URL. With fetch-based streaming, the JWT stays in the `Authorization` header.

The implementation shall follow the algorithm using Angular signals. It handles:

- **Connection lifecycle** — `idle → connecting → open → closed/error` state machine
- **Session management** — tracks server-assigned `sessionId` from the Session event
- **Dynamic subscriptions** — subscribe/unsubscribe while connected (POST/DELETE to subscriptions endpoint)
- **Late subscriber reconciliation** — subscribers added between URL construction and Session event receipt are registered via POST after the session is established
- **Exponential backoff reconnect** — starts at 1s, doubles each attempt, caps at 30s. Resets on successful connection.
- **Generation counter** — prevents stale `getConnection()` calls from interfering with newer ones (e.g., old call's cleanup triggering reconnect after a new connection has opened)
- **Graceful server-side disconnect** — `DELETE /api/events/stream/connection` asks the server to `emitter.complete()`, giving the browser a clean end-of-stream. This avoids the half-closed connection state Safari and Firefox leave behind when `AbortController.abort()` is used alone.
- **Identity-safe unsubscribe** — the unsubscribe closure only removes its own Map entry, preventing a stale unsubscribe (from component teardown during navigation) from deleting a newer subscription.
- **Cache-busting** — `_t` timestamp param prevents browsers from reusing stale connections from previously aborted streams.

**Connection state machine:**
```
idle ──subscribe()──→ connecting ──Session event──→ open
 ↑                        ↑                          │
 │                        │                    (stream ends)
 └──(0 subscribers)───── closed ←──error/timeout─────┘
                            │
                     (backoff reconnect)
                            │
                            └──→ connecting
```

**`EventStreamService`:**

```typescript
@Injectable({ providedIn: 'root' })
export class EventStreamService {
  private authService = inject(AuthService);

  // Connection state
  readonly connectionState = signal<'idle' | 'connecting' | 'open' | 'closed' | 'error'>('idle');
  readonly sessionId = signal<string | null>(null);

  // Internal state
  private subscribers = new Map<string, SubscriptionEntry>();
  private abortController: AbortController | null = null;
  private activeReader: ReadableStreamDefaultReader<Uint8Array> | null = null;
  private reconnectTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private sessionExpired = false;
  private reconnectAttempt = 0;
  private connectionGeneration = 0;
  private urlSubscriberKeys: Set<string> | null = null;

  /**
   * Subscribe to events for a target. Opens the stream if not connected.
   * Returns an unsubscribe function (call in component's DestroyRef).
   */
  subscribe<T>(
    targetType: string,
    targetId: string | number,
    handlers: StreamSubscribeHandlers<T>
  ): () => void {
    const key = `${targetType}:${targetId}`;
    const entry = { targetType, targetId, handlers };
    this.subscribers.set(key, entry);

    if (this.connectionState() === 'open' && this.sessionId()) {
      this.addSubscriptionServer(targetType, targetId);
    } else if (this.connectionState() !== 'connecting') {
      this.getConnection();
    }
    // else: connecting — subscriber is in the Map and will be picked up by
    // buildStreamUrl() or late-subscriber reconciliation after Session event.

    // Identity-safe unsubscribe: only remove if this is the same entry
    return () => {
      if (this.subscribers.get(key) !== entry) return; // stale
      this.subscribers.delete(key);
      if (this.sessionId()) {
        this.removeSubscriptionServer(targetType, targetId);
      }
      if (this.subscribers.size === 0) {
        this.closeConnection();
      }
    };
  }

  /**
   * Open the SSE stream using native fetch + ReadableStream.
   * Reads SSE text blocks, parses JSON, dispatches to subscribers.
   */
  private async getConnection(): Promise<void> {
    if (this.subscribers.size === 0) return;
    const myGeneration = ++this.connectionGeneration;
    this.connectionState.set('connecting');

    // Graceful teardown of previous connection
    this.disconnectServer();
    this.sessionId.set(null);
    await this.releaseReader();
    this.abortController?.abort();

    if (myGeneration !== this.connectionGeneration) return; // superseded

    this.abortController = new AbortController();
    const url = this.buildStreamUrl();
    this.urlSubscriberKeys = new Set(this.subscribers.keys());

    const token = this.authService.token();
    const res = await fetch(url, {
      signal: this.abortController.signal,
      headers: {
        'Accept': 'text/event-stream',
        'Authorization': `Bearer ${token}`
      },
      cache: 'no-store'
    });

    if (myGeneration !== this.connectionGeneration) return;
    if (!res.ok || !res.body) { /* error → reconnect */ return; }

    this.activeReader = res.body.getReader();
    await this.readStream(this.activeReader, myGeneration);

    // Stream ended — reconnect if still current generation and have subscribers
    if (myGeneration === this.connectionGeneration && this.subscribers.size > 0) {
      this.sessionId.set(null);
      this.scheduleReconnect();
    }
  }

  /**
   * Read SSE text from the stream, split on double-newline, parse JSON.
   */
  private async readStream(
    reader: ReadableStreamDefaultReader<Uint8Array>,
    generation: number
  ): Promise<void> {
    const decoder = new TextDecoder();
    let buffer = '';
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split(/\r?\n\r?\n/);
      buffer = blocks.pop() ?? '';
      for (const block of blocks) {
        if (!block.trim()) continue;
        const parsed = this.parseSSEBlock(block);
        if (parsed && !this.processEvent(parsed)) return;
      }
    }
  }
}
```

**Component usage:**
```typescript
@Component({ /* ... */ })
export class GoalEditorComponent implements OnInit {
  private eventStream = inject(EventStreamService);
  private destroyRef = inject(DestroyRef);
  projectId = input.required<number>();
  goalId = input.required<number>();

  ngOnInit() {
    // Subscribe to goal-level events
    const unsub = this.eventStream.subscribe<GoalDto>(
      'Goal', this.goalId(),
      {
        onData: (payload, messageType) => {
          if (messageType === 'deleted') {
            // navigate away or show message
          } else if (payload) {
            this.goal.set(payload); // update the signal with new data
          }
        },
        onError: (err) => console.error('Stream error', err),
      }
    );
    this.destroyRef.onDestroy(unsub);
  }
}
```

#### Why SSE over WebSocket

- **Unidirectional** — the client only needs to receive push events; it already sends commands via POST. No need for bidirectional WebSocket.
- **Simpler infrastructure** — works through HTTP proxies and load balancers without special configuration. No WebSocket upgrade handshake.
- **Spring Boot native** — `SseEmitter` in servlet stack. No additional dependencies.

#### Why Fetch-Based Streaming over EventSource

- **Authorization header** — `EventSource` does not support custom headers. With fetch, the JWT stays in the `Authorization` header rather than being exposed in a URL query parameter.
- **Full control** — `AbortController` for cancellation, `ReadableStream` for incremental parsing, generation counters for stale connection handling.
- **Browser compatibility** — native `fetch` with `ReadableStream` is supported in all modern browsers. The SSE text format (`data:` lines separated by `\n\n`) is trivial to parse manually.

#### Backend: Publishing Events

When a command executes, domain code publishes events through `StreamEventPublisher`. This keeps event publishing decoupled from the SSE transport:

```
AnalysisInvokingCommandHandler.execute(command)
  → command.execute() + command.invokeAnalysis()
  → NLP assistant adds Issue to Goal
  → streamEventPublisher.publishTargetUpdate("Goal", 7, goalDto)
  → StreamService looks up sessions subscribed to "Goal:7"
  → sends Data event via each session's SseEmitter
```

For Requel (single server instance), `StreamEventPublisher` pushes directly to local `SseEmitter` instances. The interface is designed so that a Redis pub/sub layer can be added later for multi-instance deployments without changing the publishing code.

### 3.6 Authorization

Echo2 enforces authorization at the domain level — commands and panels check domain roles (`user.hasRole(SystemAdminUserRole.class)`) and stakeholder context (`project.getUserStakeholder(user)` for edit permissions). These are not HTTP-level checks. The Angular migration preserves this approach and adds HTTP-level gating.

#### Backend: Two layers

**1. Endpoint-level (Spring Security):**
- User administration endpoints (`GET /api/users`, `POST /api/commands/NewUser`, `POST /api/commands/EditUser`) require the `SystemAdmin` role — enforced via `@PreAuthorize("hasRole('SYSTEM_ADMIN')")` or equivalent.
- All other authenticated endpoints are open to any logged-in user; fine-grained checks happen at the domain layer.

**2. Domain-level (command handler chain + query filtering):**
- Currently, most authorization is enforced by Echo2 UI panels (`isReadOnlyMode()`, `isShowDelete()`), **not** in commands. Only `EditUserCommandImpl` explicitly checks `hasRole(SystemAdminUserRole)`. When the UI becomes Angular, this server-side panel enforcement disappears.
- The new architecture adds an **`AuthorizingCommandHandler`** to the command handler chain. Each command declares its authorization requirement (system role, role permission, or stakeholder permission) via an `AuthorizableCommand` interface. The handler checks the requirement before the command enters the transactional boundary.
- Query endpoints filter results by user access — e.g. `GET /api/projects` returns only projects where the user is a stakeholder (or all projects for SystemAdmin). A `ProjectAccessChecker` service enforces this.
- See `doc/AUTH_ARCH.md` for the full design.

This means the API layer gates admin endpoints at the Spring Security level and delegates fine-grained authorization to the `AuthorizingCommandHandler` (for commands) and `ProjectAccessChecker` (for queries).

#### Angular: Role-based visibility

The `UserDto` returned by `POST /api/auth/login` and `GET /api/auth/me` includes `roles: string[]`. Angular uses these for UI-level visibility:

| Feature | Required Role | Implementation |
|---|---|---|
| Users tab (list, create, edit users) | `SystemAdmin` | Route guard on `/users/**` |
| Role editing in user form | `SystemAdmin` | Conditional field visibility |
| All project features | Any authenticated user | Default — stakeholder filtering is backend-only |

Role checks in Angular are for **UX only** (hiding irrelevant UI). The backend enforces all authorization; Angular never trusts client-side role checks for security.

#### Detailed Authorization Architecture

The full authorization design — including `AuthorizingCommandHandler` in the command handler chain, `AuthorizableCommand` / `ProjectScopedCommand` interfaces, `AuthorizationRequirement` sealed types, query-level `ProjectAccessChecker`, and Angular `PermissionService` — is documented in **`doc/AUTH_ARCH.md`**.

## 4. Migration Strategy: Screen-by-Screen

Each phase delivers a working increment. The Angular app and Echo2 app can coexist during migration — serve Angular on a different port or path, both talking to the same database.

### Phase 0: Foundation (API + Angular scaffold)

**Goal:** Establish the CQRS API modules and Angular project with auth working end-to-end.

**Backend work:**
1. Create `service-api` module with:
   - `ApiCommand<T>` interface
   - `CommandRegistration` record (commandType, inputClass, factoryMethod)
   - `CommandRegistry` interface — per-domain factories register into this
   - `CommandResult` response wrapper (success + updated entity, or error + validation details)
   - Base DTOs: `UserDto`, `ProjectSummaryDto`, error response DTOs
2. Create `service-impl` module with:
   - `CommandFactory` composite facade — delegates to per-domain factories, provides `getInputType(type)` and `newCommand(type, input)`
   - `CommandController` — `POST /api/commands/{commandType}`, uses `CommandFactory` + `CommandHandler`
   - Spring Security config: stateless, JWT filter before `UsernamePasswordAuthenticationFilter`, public path for `/api/auth/login`, authenticated for all other `/api/**`
   - JWT utility (jjwt): token generation (HS256, 8-hour expiry, claims: sub/roles/exp), validation, claim extraction
   - `CurrentUserResolver` — resolves JWT principal (username) to domain `User` via `UserRepository.findUserByUsername`; used by `CommandController` to call `command.setEditedBy(currentUser)` before execute
   - CORS configuration: `allowedOrigins` includes `http://localhost:4200` for dev; same-origin in production
3. Update existing per-domain factories (`ProjectCommandFactory`, `UserCommandFactory`, `AnnotationCommandFactory`) to register their command types via `@PostConstruct`
4. Implement auth endpoints:
   - `POST /api/auth/login` — loads user by username, verifies password via existing `user.isPassword()`, issues JWT
   - `GET /api/auth/me` — validates JWT, returns `UserDto`
5. SSE event stream infrastructure (session-based subscription model, see Section 3.5):
   - `StreamEventEnvelope` — event envelope DTO with `eventType`, `targetType`, `targetId`, `payload` (factory methods: `session()`, `data()`, `targetDeleted()`, `sessionExpired()`)
   - `StreamSessionStore` — in-memory session + subscription tracking (`ConcurrentHashMap<sessionId, SessionData>`, reverse index `targetType:targetId → Set<sessionId>`)
   - `StreamService` — manages `SseEmitter` lifecycle, keep-alive (30s comment), session expiry (JWT exp + 5 min grace), graceful close (`emitter.complete()`), push-to-subscribed-sessions
   - `StreamEventPublisher` interface + `StreamEventPublisherImpl` — called by domain code to push target updates and deletes to subscribed sessions
   - `StreamController` — endpoints: `GET /api/events/stream` (open stream with initial subscriptions), `POST /api/events/stream/subscriptions` (add), `DELETE /api/events/stream/subscriptions` (remove), `DELETE /api/events/stream/connection` (graceful close)
   - `StreamConfig` — dedicated `TaskScheduler` for keep-alive and session expiry (thread pool, prefix "stream-")
6. Authorization infrastructure (see `doc/AUTH_ARCH.md`):
   - `AuthorizableCommand` interface — commands declare `getAuthorizationRequirement()`
   - `AuthorizationRequirement` sealed interface — `RequiresSystemRole`, `RequiresRolePermission`, `RequiresStakeholderPermission`
   - `ProjectScopedCommand` interface — exposes `getProject()` for stakeholder permission checks
   - `AuthorizingCommandHandler` — added to handler chain between `ExceptionMappingCommandHandler` and `AnalysisInvokingCommandHandler`
   - `AuthorizationException` — mapped to 403 Forbidden
   - `ProjectAccessChecker` — verifies stakeholder membership for query endpoints

**Frontend work:**
1. Scaffold Angular 21 project: `ng new requel-angular --style=scss --routing` (standalone by default, no NgModules)
2. Install PrimeNG 21: `npm install primeng @primeng/themes primeicons`
3. Configure `app.config.ts`:
   ```typescript
   provideRouter(routes),
   provideHttpClient(withInterceptors([authInterceptor])),
   provideAnimationsAsync(),
   providePrimeNG({ theme: { preset: Aura } })
   ```
4. `CommandService` — injectable service using `HttpClient` + signals for state. Generic `execute<T>(commandType, input): Signal<CommandResult<T>>` method for `POST /api/commands/{type}` calls.
5. Auth: login page (standalone component importing PrimeNG `InputText`, `Password`, `Button`), `AuthService` (JWT stored as `signal<string | null>`), functional HTTP interceptor (`authInterceptor`), functional route guard (`authGuard`)
6. `EventStreamService` — fetch-based SSE streaming service (see Section 3.5). Connection lifecycle (idle/connecting/open/closed/error), session tracking, dynamic subscribe/unsubscribe, late-subscriber reconciliation, exponential backoff reconnect, generation counter for stale connection prevention, graceful server-side disconnect, identity-safe unsubscribe, SSE text parsing via `ReadableStream`.
7. Main layout: standalone `AppLayout` component with `p-menubar` (Edit Account, User Guide, Logout), sidebar `p-tree`, content area with `p-tabView`
8. Verify login → protected route → SSE connection → logout flow works end-to-end

**Auth endpoints (these remain conventional REST, not commands):**
```
POST   /api/auth/login          → { username, password } → { token, user: UserDto }
GET    /api/auth/me             → validates JWT → UserDto (current user)
```
No logout endpoint — the Angular app discards the JWT from memory.

**Echo2 panels replaced:** `LoginController`, `RequelMainScreen` (layout only)

---

### Phase 1: User Administration

**Goal:** Replace the Users tab — list, create, edit users.

**Backend work — Commands:**
1. Add `ApiCommand<NewUserInput>` to `NewUserCommand` — fields: username, password, name, email, org, phone, roles
2. Add `ApiCommand<EditUserInput>` to `EditUserCommand` — same fields, plus userId, version
3. Register both in `UserCommandFactory.registerCommands()`

**Backend work — Queries:**
1. `GET /api/users` → paginated list of all users
2. `GET /api/users/{id}` → single user detail
3. `GET /api/organizations` → org list for dropdown
4. `GET /api/roles` → available role definitions

**Frontend work:**
1. Users feature module: user list table, user editor form
2. Organization typeahead component (shared)
3. Role tree checkbox component
4. Edit Account page (reuses user editor, sends `EditUser` command)

**Echo2 panels replaced:** `UserAdminNavigatorPanel`, `UserEditorPanel`

---

### Phase 2: Project CRUD + Navigation

**Goal:** Replace the Projects tab — list, create, import, edit projects. Build the project tree sidebar.

**Backend work — Commands:**
1. Add `ApiCommand<NewProjectInput>` to `NewProjectCommand` — fields: name, description, organizationName
2. Add `ApiCommand<EditProjectInput>` to `EditProjectCommand` — fields: projectId, name, description, organizationName, version
3. `ImportProject` command — special handling: multipart upload via `POST /api/projects/import` (not through command dispatch since it involves file upload)

**Backend work — Queries:**
1. `GET /api/projects` → paginated list, filtered by `ProjectAccessChecker`: returns only projects where current user is a stakeholder (or all projects for SystemAdmin)
2. `GET /api/projects/{id}` → full project detail (requires stakeholder membership or SystemAdmin)
3. `GET /api/projects/{id}/tree` → tree structure for sidebar (child entity counts)
4. `GET /api/projects/{id}/export` → XML download (adapt existing `ProjectXmlController`)
5. `GET /api/projects/{id}/my-permissions` → current user's stakeholder permissions for this project (see `doc/AUTH_ARCH.md` §4.2)

**Frontend work:**
1. Project list page with New/Import buttons ("New Project" shown only if `canCreateProjects` from JWT permissions)
2. New Project form (name, description, org combo)
3. Import Project form (file upload, rename, enable analysis)
4. Edit Project form (name, description, org, createdBy, annotations — annotations placeholder until Phase 7)
5. Project tree sidebar component — clicking items routes to sub-pages
6. Export button triggers download
7. `PermissionService` — fetches `GET /api/projects/{id}/my-permissions` on project load, caches per project, exposes `hasPermission(projectId, entityType, permissionType)` as signals. System roles and role-level permissions read from JWT. See `doc/AUTH_ARCH.md` §4.3.

**Echo2 panels replaced:** `ProjectNavigatorPanel`, `ProjectOverviewPanel`, `ProjectImportPanel`, `ProjectNavigatorTreeNodeFactory`

---

### Phase 3: Stakeholders

**Goal:** Stakeholder list and edit forms within a project.

**Backend work — Commands:**
1. Add `ApiCommand<AddUserStakeholderInput>` to `AddUserStakeholderCommand` — fields: projectId, userId
2. Add `ApiCommand<AddNonUserStakeholderInput>` to `NewNonUserStakeholderCommand` — fields: projectId, name, description
3. Add `ApiCommand<EditUserStakeholderInput>` / `EditNonUserStakeholderInput` to edit commands — include version
4. Add `ApiCommand<RemoveStakeholderInput>` to remove command — include version

**Backend work — Queries:**
1. `GET /api/projects/{id}/stakeholders` → paginated list

**Frontend work:**
1. Stakeholder list table (Name, User?, Team, Email, Phone, Created By, Date)
2. Add User Stakeholder dialog (pick from system users)
3. Add Non-User Stakeholder form (name, description, goals)
4. Edit stakeholder form

**Echo2 panels replaced:** `StakeholderNavigatorPanel`, `UserStakeholderEditorPanel`, `NonUserStakeholderEditorPanel`

---

### Phase 4: Goals + Stories

**Goal:** Goal and Story CRUD with cross-entity relationships.

**Backend work — Commands:**
1. `NewGoal` → `ApiCommand<NewGoalInput>` — fields: projectId, name, text
2. `EditGoal` → `ApiCommand<EditGoalInput>` — fields: goalId, name, text, version
3. `EditGoalRelations` → `ApiCommand<EditGoalRelationsInput>` — fields: goalId, supportingGoalIds[], conflictingGoalIds[], version
4. `DeleteGoal` → `ApiCommand<DeleteGoalInput>` — fields: goalId, version
5. `NewStory` → `ApiCommand<NewStoryInput>` — fields: projectId, name, text, type
6. `EditStory` → `ApiCommand<EditStoryInput>` — fields: storyId, name, text, type, version
7. `DeleteStory` → `ApiCommand<DeleteStoryInput>` — fields: storyId, version

**Backend work — Queries:**
1. `GET /api/projects/{id}/goals` → paginated list
2. `GET /api/projects/{id}/goals/{gid}` → single goal with relations
3. `GET /api/projects/{id}/stories` → paginated list
4. `GET /api/projects/{id}/stories/{sid}` → single story

**Frontend work:**
1. Goals list table + goal editor form
2. Goal relations section (select supporting/conflicting goals via selector dialog)
3. Stories list table + story editor form (with type dropdown)
4. Entity selector dialog (shared) — modal with searchable table for picking goals/stories

**Echo2 panels replaced:** `GoalNavigatorPanel`, `GoalEditorPanel`, `GoalRelationEditorPanel`, `GoalSelectorPanel`, `StoryNavigatorPanel`, `StoryEditorPanel`, `StorySelectorPanel`

---

### Phase 5: Actors + Use Cases

**Goal:** Actors and use cases, which tie together actors, goals, stories, and scenarios.

**Backend work — Commands:**
1. `NewActor` → `ApiCommand<NewActorInput>` — fields: projectId, name, description
2. `EditActor` → `ApiCommand<EditActorInput>` — fields: actorId, name, description, version
3. `DeleteActor` → `ApiCommand<DeleteActorInput>` — fields: actorId, version
4. `NewUseCase` → `ApiCommand<NewUseCaseInput>` — fields: projectId, name, description, primaryActorId
5. `EditUseCase` → `ApiCommand<EditUseCaseInput>` — fields: useCaseId, name, description, primaryActorId, goalIds[], storyIds[], scenarioIds[], version
6. `DeleteUseCase` → `ApiCommand<DeleteUseCaseInput>` — fields: useCaseId, version

**Backend work — Queries:**
1. `GET /api/projects/{id}/actors` → list
2. `GET /api/projects/{id}/actors/{aid}` → single actor
3. `GET /api/projects/{id}/use-cases` → list
4. `GET /api/projects/{id}/use-cases/{uid}` → single use case with related entities

**Frontend work:**
1. Actors list + editor
2. Use cases list + editor with:
   - Primary actor selector (reuse entity selector dialog)
   - Goals sub-table (add/remove goals)
   - Stories sub-table (add/remove stories)
   - Scenarios sub-table (add/remove scenarios)
3. Actor selector dialog (shared)

**Echo2 panels replaced:** `ActorNavigatorPanel`, `ActorEditorPanel`, `ActorSelectorPanel`, `UseCaseNavigatorPanel`, `UseCaseEditorPanel`, `UseCaseSelectorPanel`, add/remove controllers

---

### Phase 6: Scenarios

**Goal:** Scenario CRUD with the step tree editor (the most complex UI).

**Backend work — Commands:**
1. `NewScenario` → `ApiCommand<NewScenarioInput>` — fields: projectId, name, type
2. `EditScenario` → `ApiCommand<EditScenarioInput>` — fields: scenarioId, name, type, steps[] (tree as nested DTOs), version
3. `DeleteScenario` → `ApiCommand<DeleteScenarioInput>` — fields: scenarioId, version

**Backend work — Queries:**
1. `GET /api/projects/{id}/scenarios` → list
2. `GET /api/projects/{id}/scenarios/{sid}` → single scenario with step tree

**Frontend work:**
1. Scenarios list + editor
2. Scenario step tree component — drag-and-drop reordering, indent/outdent for alternate/exception paths
3. Step type indicators (normal, alternate, exception)
4. Scenario selector dialog (shared)

**Echo2 panels replaced:** `ScenarioNavigatorPanel`, `ScenarioEditorPanel`, `ScenarioSelectorPanel`, `ScenarioEditorTreeNodeFactory`, `ScenarioStepEditorTreeNodeFactory`

**Note:** The step tree is the most complex UI component in Requel. Use PrimeNG's `p-tree` with `draggableNodes`/`droppableNodes` as the foundation. Start simple — flat ordered list with type labels — and iterate toward a full drag-and-drop tree editor.

---

### Phase 7: Annotations (Issues, Positions, Arguments, Notes)

**Goal:** The IBIS discussion system that appears on nearly every entity.

**Backend work — Commands:**
1. `NewIssue` → `ApiCommand<NewIssueInput>` — fields: entityType, entityId, text, mustBeResolved
2. `EditIssue` → `ApiCommand<EditIssueInput>` — fields: issueId, text, status, mustBeResolved, version
3. `NewNote` → `ApiCommand<NewNoteInput>` — fields: entityType, entityId, text
4. `EditNote` → `ApiCommand<EditNoteInput>` — fields: noteId, text, version
5. `NewPosition` → `ApiCommand<NewPositionInput>` — fields: issueId, text
6. `NewArgument` → `ApiCommand<NewArgumentInput>` — fields: positionId, text, supports (boolean)

**Backend work — Queries:**
1. `GET /api/annotations?entityType=goal&entityId={id}` → annotations for any entity

**Frontend work:**
1. Annotations table component (shared) — appears on every editor
2. Issue editor (inline or dialog): text, status, must-be-resolved
3. Position editor: text, arguments list
4. Argument editor: text, supports/opposes toggle
5. Note editor: text
6. Retrofit annotations into all existing editor pages (goals, stories, actors, etc.)

**Echo2 panels replaced:** `IssueEditorPanel`, `PositionEditorPanel`, `ArgumentEditorPanel`, `NoteEditorPanel`, `AnnotationsTable`, `AnnotationRefererTable`

**Note:** This phase touches every editor screen from previous phases. Plan for a pass through all existing Angular editors to wire in the shared annotations component.

---

### Phase 8: Terms (Glossary) + Documents

**Goal:** Glossary and document generation.

**Backend work — Commands:**
1. `NewGlossaryTerm` → `ApiCommand<NewGlossaryTermInput>` — fields: projectId, name, definition, canonicalTermId
2. `EditGlossaryTerm` → `ApiCommand<EditGlossaryTermInput>` — fields: termId, name, definition, canonicalTermId, version
3. `DeleteGlossaryTerm` → `ApiCommand<DeleteGlossaryTermInput>` — fields: termId, version
4. `NewDocument` → `ApiCommand<NewDocumentInput>` — fields: projectId, name, templateConfig
5. `EditDocument` → `ApiCommand<EditDocumentInput>` — fields: documentId, name, templateConfig, version
6. `RunDocument` → `ApiCommand<RunDocumentInput>` — fields: documentId (triggers generation)

**Backend work — Queries:**
1. `GET /api/projects/{id}/terms` → list
2. `GET /api/projects/{id}/terms/{tid}` → single term
3. `GET /api/projects/{id}/documents` → list
4. `GET /api/projects/{id}/documents/{did}` → single document
5. `GET /api/projects/{id}/documents/{did}/output` → generated HTML/PDF

**Frontend work:**
1. Terms list + editor (with canonical term selector)
2. Term selector dialog (shared)
3. Documents list with Edit/Run buttons
4. Document editor form
5. Document viewer (display generated HTML)

**Echo2 panels replaced:** `GlossaryTermNavigatorPanel`, `GlossaryTermEditorPanel`, `GlossaryTermSelectorPanel`, `ReportGeneratorNavigatorPanel`, `ReportGeneratorEditorPanel`

---

### Phase 9: Open Issues

**Goal:** Project-wide open issues view.

**Backend work — Commands:**
1. `AnalyzeProject` → `ApiCommand<AnalyzeProjectInput>` — fields: projectId (triggers NLP analysis)

**Backend work — Queries:**
1. `GET /api/projects/{id}/open-issues` → aggregated open issues across all entities

**Frontend work:**
1. Open Issues page (read-only table linking to annotated entities)

**Echo2 panels replaced:** `ProjectOpenIssuesNavigatorPanel`

**Not migrated:** `ParserPanel`, `NLPNavigatorPanel` — the NLP parser is an admin/debug tool that doesn't need an Angular equivalent. NLP analysis continues to run automatically via `AnalysisInvokingCommandHandler` after writes; the parser UI is dropped.

---

### Phase 10: Cleanup + Echo2 Removal

**Goal:** Remove Echo2 entirely.

1. Remove Echo2 servlet registration from `Application.java`
2. Remove Echo2 UI Maven modules: `ui-core`, `project-ui`, `annotation-ui`, `user-ui`, `nlp-ui`, `ui-assets`
3. Remove Echo2 transform scripts and `exec-maven-plugin` configuration
4. Remove Echo2 JARs from dependencies (echo2, echopm, echopointng, echo2-filetransfer)
5. Integrate Angular build into Maven: copy `ng build` output to `src/main/resources/static/` (via `frontend-maven-plugin` or build script) so the JAR serves the Angular app at `/`
6. Configure Spring Boot to forward non-API routes to `index.html` (Angular client-side routing)
7. Update `CLAUDE.md`, `RELEASE.md`, `README.md`

## 5. API Design Conventions

### CQRS Split

**Writes — Command dispatch:**
```
POST /api/commands/{commandType}    → JSON body specific to the command
```

All mutations go through this single path. The `commandType` is the command name (e.g., `EditGoal`, `NewStory`, `NewIssue`). The body shape varies per command type.

**Reads — Query endpoints:**
```
GET /api/auth/me                              → current user
GET /api/users[/{id}]                         → user admin
GET /api/organizations                        → org lookup
GET /api/roles                                → available roles
GET /api/projects[/{id}]                      → projects
GET /api/projects/{id}/tree                   → sidebar tree structure
GET /api/projects/{id}/stakeholders           → stakeholders
GET /api/projects/{id}/goals[/{gid}]          → goals
GET /api/projects/{id}/stories[/{sid}]        → stories
GET /api/projects/{id}/actors[/{aid}]         → actors
GET /api/projects/{id}/use-cases[/{uid}]      → use cases
GET /api/projects/{id}/scenarios[/{sid}]      → scenarios
GET /api/projects/{id}/terms[/{tid}]          → glossary terms
GET /api/projects/{id}/documents[/{did}]      → documents
GET /api/projects/{id}/documents/{did}/output → generated document
GET /api/projects/{id}/open-issues            → aggregated open issues
GET /api/projects/{id}/export                 → XML export
GET /api/annotations?entityType=&entityId=    → annotations for any entity
```

**Event stream (SSE — session-based subscription model, see Section 3.5):**
```
GET    /api/events/stream?subscribe=Type:id   → opens SSE stream with initial subscriptions
POST   /api/events/stream/subscriptions       → add subscription (X-Session-Id header)
DELETE /api/events/stream/subscriptions       → remove subscription (X-Session-Id header)
DELETE /api/events/stream/connection          → graceful server-side close (X-Session-Id header)
```

**Special cases (not through command dispatch):**
```
POST /api/auth/login                  → returns JWT + UserDto
POST /api/projects/import             → multipart file upload
```

### Composite CommandFactory

See Section 3.1 for the full architecture, code examples, and flow diagrams. In summary:
- Per-domain factories register command types at startup via `@PostConstruct`
- Top-level `CommandFactory` facade provides `getInputType(type)` and `newCommand(type, input)`
- Thin `CommandController` handles deserialization and delegates to the facade + `CommandHandler`

### Command Response

See Section 3.1 for response format examples. In summary:
- Success: `{ success: true, entity: { ... }, entityType: "Goal" }`
- Validation failure: `{ success: false, error: "...", violations: [{ field, message }] }`

### Query Response Patterns
- List endpoints return the standard paginated envelope: `{ items: T[], total: number, page: number, pageSize: number }`. All list endpoints in phases (e.g. `GET /api/users`, `GET /api/projects/{id}/goals`) use this envelope, not raw arrays.
- Single-entity endpoints return the DTO directly
- All entities include `id`, `createdBy`, `dateCreated`, `version` (for optimistic locking)

### Optimistic Locking

All mutating command inputs (`Edit*`, `Delete*`, relation edits) must include the entity's `version` field. The backend checks the version against the current database value before applying changes.

**On version mismatch (concurrent edit detected):**
```json
HTTP 409 Conflict
{
  "success": false,
  "error": "Conflict",
  "message": "Entity was modified by another user. Please reload and try again.",
  "currentVersion": 5,
  "yourVersion": 3
}
```

The Angular client handles 409 by prompting the user to reload the entity before retrying. This prevents lost updates when the NLP agent or another user modifies an entity concurrently.

### DTO Approach
- DTOs are flat where possible (no deep nesting)
- References to other entities use `{ id, name }` summary objects
- Collections (e.g., a use case's goals) return summary lists; full details require separate requests
- Annotations are loaded separately via the `/api/annotations` endpoint to avoid bloating every response

### Command Type Inventory

Organized by domain, these are the command types needed (mapping to existing Command classes):

**User commands:**
`NewUser`, `EditUser`

**Project commands:**
`NewProject`, `EditProject`

**Stakeholder commands:**
`AddUserStakeholder`, `AddNonUserStakeholder`, `EditUserStakeholder`, `EditNonUserStakeholder`, `RemoveStakeholder`

**Goal commands:**
`NewGoal`, `EditGoal`, `EditGoalRelations`, `DeleteGoal`

**Story commands:**
`NewStory`, `EditStory`, `DeleteStory`

**Actor commands:**
`NewActor`, `EditActor`, `DeleteActor`

**Use Case commands:**
`NewUseCase`, `EditUseCase`, `DeleteUseCase`

**Scenario commands:**
`NewScenario`, `EditScenario`, `DeleteScenario`

**Glossary commands:**
`NewGlossaryTerm`, `EditGlossaryTerm`, `DeleteGlossaryTerm`

**Document commands:**
`NewDocument`, `EditDocument`, `RunDocument`

**Annotation commands:**
`NewIssue`, `EditIssue`, `NewNote`, `EditNote`, `NewPosition`, `NewArgument`

**Analysis commands:**
`AnalyzeProject`

## 6. Tradeoffs and Decisions

### Angular vs. other frameworks
Angular is chosen because:
- Ron has experience with it and appreciates its structural similarities to Java/Spring
- Strong typing with TypeScript aligns with the domain-driven approach
- Built-in routing, forms, HTTP client, and dependency injection reduce library shopping
- Component architecture maps cleanly to the existing panel structure

### PrimeNG vs. other UI component libraries
PrimeNG is chosen over Angular Material and Taiga UI because:
- **Tables are the heart of this app.** Every navigator screen is a sortable, filterable, paginated data table. PrimeNG's `p-table` provides all of this out of the box; Angular Material's `mat-table` requires manual assembly of sorting/filtering/pagination from CDK primitives.
- **Tree-table support.** Scenario steps and hierarchical data need a tree-table component. PrimeNG has `p-treeTable` built in; Angular Material has no native tree-table (open issue [#13616](https://github.com/angular/components/issues/13616) never resolved). Taiga UI has no tree-table either.
- **Tree drag-and-drop.** PrimeNG's `p-tree` supports `draggableNodes`/`droppableNodes` natively for scenario step reordering; Angular Material's `mat-tree` requires custom CDK drag integration.
- **Design-agnostic.** PrimeNG doesn't impose Material Design — free themes (Aura, Lara, Nora) plus Material/Bootstrap/Fluent presets available.
- **MIT licensed, fully open source.** All 90+ components free. Premium add-ons (theme designer, PrimeBlocks) are optional and not needed.
- **Tradeoff: bundle size.** PrimeNG is larger than Angular Material or Taiga UI, but tree-shakeable. For a requirements management tool, initial load size is not a primary concern.

### Monorepo vs. separate repo for Angular
**Recommendation: subdirectory within this repo** (`requel-angular/`). Keeps everything together for a single-developer project. The Angular project has its own `package.json` and build pipeline, independent of Maven.

### Coexistence during migration
Both Echo2 and Angular can run simultaneously during migration:
- Echo2 continues serving on `/` (existing behavior)
- Angular dev server runs on a different port (e.g., 4200)
- Both hit the same Spring Boot backend
- Once a screen is fully replaced in Angular, remove the Echo2 panel

### JWT authentication (stateless, no sessions)
JWT with `Authorization: Bearer` header, no server-side sessions. This keeps the backend fully stateless — no session store, no sticky sessions, no shared session data if multiple instances run behind a load balancer. Token expiry is 8 hours with no refresh token; users re-authenticate when the token expires. JWT is stored in memory (Angular service field), not `localStorage` or cookies, so it's cleared on page refresh. The tradeoff is that a page refresh requires re-login, which is acceptable for a requirements tool that isn't used in long uninterrupted sessions.

### Real-time updates: SSE with session-based subscriptions
Commands return results synchronously — the user gets immediate feedback for their own actions. Background events (NLP agent adding annotations, other users' edits) push to the client via Server-Sent Events (SSE) rather than WebSocket. SSE is unidirectional (server→client), works through proxies, and requires no additional dependencies beyond Spring Boot's `SseEmitter`. The client uses **fetch-based streaming** (not `EventSource`) so the JWT stays in the Authorization header rather than a URL query parameter. The stream uses a **session-based subscription model** the client subscribes to specific targets (`Project:1`, `Project:7`), and the server pushes only events for those targets. Subscriptions can be added/removed dynamically via REST endpoints while the stream is open. Events carry full DTO payloads where feasible, reducing round-trips. The client handles reconnect with exponential backoff, generation counters for stale connection prevention, and graceful server-side disconnect to avoid Safari/Firefox half-closed connection issues. See Section 3.5 for the complete architecture. The alternative — reading all results from the stream (pure event-sourcing style) — was rejected because it adds command correlation complexity, latency on the write path, and error handling for missing events, all without UX benefit for a synchronous command-based system.

### Deployment: static from Spring Boot
Angular build output (`ng build` → `dist/`) is served as static resources from the Spring Boot JAR. Single artifact, single process, single Docker container. No CORS configuration needed in production since frontend and API are same-origin. The alternative — deploying Angular separately via nginx/S3/CDN — adds infrastructure complexity (reverse proxy, CORS, two deploy targets) without benefit for a single-developer, single-server tool. During development, Angular's dev server runs on port 4200 with hot reload and proxies API calls to Spring Boot — the dev experience is the same either way.

### Identity migration dependency
The UI refactor relies on the current user/password/role model. A separate `doc/IDENTITY_MIGRATION.md` plans to extract `platform-identity` with new `User`/`Role` interfaces, Spring Security `UserDetails` alignment, and role-to-authority remapping. To avoid duplicate auth work or contract churn mid-migration:
- **The UI refactor pins to the current identity model.** Phase 0 auth (JWT filter, `CurrentUserResolver`, `user.isPassword()`) uses the existing `UserRepository` and domain `User` directly.
- **If the identity migration runs concurrently**, the `CurrentUserResolver` and JWT claim mapping are the only touchpoints that need updating. The `ApiCommand<T>` interface and command inputs are identity-agnostic.
- **Recommended sequencing:** complete at least Phases 0–2 of the UI refactor before starting the identity migration, so the auth layer is stable before the identity model changes underneath it.

### Cutover and rollback
During migration, Echo2 and Angular coexist — both hit the same Spring Boot backend and same database. The schema does not change (no new tables or migrations for the Angular frontend). This makes rollback straightforward:
- **Rollback during Phases 0–9:** revert to the previous JAR (without `service-api`/`service-impl` modules). Echo2 continues to work as before. No data migration needed.
- **Phase 10 (Echo2 removal) is the point of no return.** Before executing Phase 10:
  - All screens must be verified in Angular (see Definition of Done below)
  - Run the full integration test suite
  - Confirm import/export round-trip works through the new API
  - Keep the last Echo2-capable JAR as a rollback artifact for one release cycle
- **No runtime toggle needed.** Echo2 on `/` and Angular on port 4200 (dev) or a different path are sufficient for coexistence. Feature flags add complexity without benefit for a single-developer project.

### Definition of Done (per phase)
Each phase is complete when:
1. **Backend tests pass** — new command and query endpoint tests added, existing integration tests still green (`mvn -pl modules/requel-app -am test`)
2. **Frontend tests pass** — component tests for new Angular screens (Vitest)
3. **Smoke test** — manually verify the end-to-end flow for the phase's screens (create, edit, delete, list) through the Angular UI
4. **Echo2 parity** — the Angular screen handles the same data and actions as the Echo2 panel it replaces (field-by-field comparison against the screen inventory in Section 2.1)
5. **No regressions** — Echo2 panels not yet replaced still function correctly

### Phasing priorities
The phases are ordered by dependency (auth first, then entities, then cross-cutting features). The annotation system (Phase 7) is deliberately late because it touches every editor — building it after the editors exist avoids rework. However, editor forms should include an "Annotations" placeholder from Phase 2 onward so the layout is correct.

## 7. Effort Estimates by Phase

| Phase | Scope | Commands | Queries | Angular Components | Relative Size |
|-------|-------|----------|---------|-------------------|---------------|
| 0 | Foundation | 0 | 1 (auth/me) + SSE stream (4 endpoints) | 5 (login, layout, guard, CommandService, EventStreamService) | Medium-Large (infrastructure) |
| 1 | Users | 2 | 4 | 4 (list, editor, org combo, roles) | Small |
| 2 | Projects | 2 (+import) | 5 | 7 (list, new, import, edit, tree, export, PermissionService) | Medium |
| 3 | Stakeholders | 4 | 1 | 4 (list, user-add, non-user-add, edit) | Small |
| 4 | Goals + Stories | 7 | 4 | 5 (2 lists, 2 editors, selector dialog) | Medium |
| 5 | Actors + Use Cases | 6 | 4 | 4 (2 lists, 2 editors) | Medium |
| 6 | Scenarios | 3 | 2 | 3 (list, editor, step tree) | Large (step tree) |
| 7 | Annotations | 6 | 1 | 5 (table, issue, position, argument, note) | Large (retrofitting) |
| 8 | Terms + Docs | 6 | 5 | 5 (2 lists, 2 editors, doc viewer) | Medium |
| 9 | Open Issues | 1 | 1 | 1 (issues view) | Small |
| 10 | Cleanup | 0 | 0 | 0 | Small (deletion) |

**Totals:** ~37 command types, ~28 query endpoints + SSE stream, ~42 Angular components

## 8. Open Questions

1. ~~**UI component library** — Angular Material, PrimeNG, or Taiga UI?~~ **Decided: PrimeNG.** Best fit for data-table-heavy UI. See Section 6 tradeoffs.
2. ~~**Real-time updates** — Echo2 has server-push for concurrent editing. Do we need WebSocket support for multi-user scenarios, or is polling/manual refresh acceptable initially?~~ **Decided: SSE event stream with session-based subscriptions.** Commands return results synchronously; SSE pushes background events (NLP, other users). Fetch-based streaming (not EventSource) keeps JWT in Authorization header. Session/subscription model. See Sections 3.5 and 6.
3. ~~**NLP panel priority** — The NLP parser is an admin/debug tool. It could be deferred indefinitely or built as a minimal page.~~ **Decided: Not migrated.** NLP analysis runs automatically via `AnalysisInvokingCommandHandler`; the parser debug UI is dropped from the Angular app.
4. ~~**Document generation** — The current "Run" button generates HTML via XSLT. Decide whether to preserve XSLT-based generation or move to a template engine.~~ **Decided: Keep XSLT for now.** The Angular app will call the existing XSLT-based generation and display the output. Replacing XSLT with a different template engine is a separate future effort.
5. ~~**Deployment model** — Serve Angular as static resources from Spring Boot (`/static/`), or deploy separately (nginx, S3, etc.)?~~ **Decided: Static from Spring Boot.** Angular build output served from the Spring Boot JAR. Single artifact, single process, no CORS in production, no extra infrastructure. During development, Angular dev server runs separately on port 4200 with hot reload. See Section 6.
6. **Password hashing** — Are passwords currently hashed in the database? The JWT login reuses the existing `user.isPassword()` verification, so the current format is unchanged. If passwords are stored in plaintext, introducing proper hashing (e.g. bcrypt) should be a separate task before or during the migration.
7. **User Guide link** — The main layout header includes a "User Guide" link (carried over from Echo2). Is this a static external URL, a route within the Angular app, or something to be built? Needs clarification before Phase 0 layout work.
8. **Observability** — Should we add baseline telemetry (command latency/error rates by type, query latency, active SSE connections, auth failure rates)? Spring Boot Actuator + Micrometer could provide this with minimal effort, but it's not required for initial functionality. Decide scope before or during Phase 0.
9. **Optimistic locking — existing JPA support** — Do the current JPA entities already have a `@Version` field? If not, adding one requires a Flyway migration to add a `version` column to each entity table. This is a prerequisite for the 409 conflict handling described in Section 5.

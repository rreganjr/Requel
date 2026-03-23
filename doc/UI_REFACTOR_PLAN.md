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

#### Multipart Command Dispatch

Commands that accept file uploads use `multipart/form-data` through the same
`/api/commands/{commandType}` endpoint. Spring's content-type routing dispatches
to a separate handler method:

- `application/json` → standard JSON body dispatch (existing)
- `multipart/form-data` → JSON input as `@RequestPart("input")`, file as `@RequestPart("file")`

Both paths converge into the same factory → handler chain flow. The registrar's
input applicator bridges `MultipartFile` → domain command setters (e.g.,
`MultipartFile.getInputStream()` → `ImportProjectCommand.setInputStream()`).
`MultipartFile` never leaks into domain interfaces.

The `CommandRegistration` record adds an optional `fileApplicator`
(`BiConsumer<Command, MultipartFile>`) for commands that accept files. The
`ApiCommandFactory` applies it when a file is present.

On the Angular side, `CommandService` provides `executeWithFile()` which sends
`FormData` with a JSON `input` part and a `file` part.

#### Special Cases (Not Through Command Dispatch)

```
POST /api/auth/login                  → returns JWT + UserDto
```

Auth doesn't fit the command dispatch pattern — it's infrastructure. Logout is entirely client-side (discard the JWT from memory); no server endpoint is needed since tokens are stateless.

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
| **Entity selector dialog** | `p-dialog` + `p-table` with global filter | Modal with searchable, selectable table for picking related entities (goals, actors, stories, etc.). Replaces all SelectorPanels. Future: add `allowCreate` + `createLabel` inputs so callers can inline-create new entities from within the dialog (Phase 6 design decision deferred). |
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
0. Create Maven modules `service-api` and `service-impl` under `modules/`: add `pom.xml` for each inheriting from the parent POM, declare them as `<module>` entries in the parent, and wire dependencies (`service-impl` depends on `service-api`, `project-jpa`, `user-jpa`, `annotation-jpa`; `requel-app` depends on `service-impl`)
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
7. Observability baseline:
   - Add `spring-boot-starter-actuator` dependency (includes Micrometer)
   - Command execution timer/error counter by type (instrument in `CommandHandler` chain)
   - Active SSE connections gauge (from `StreamService`)
   - Auth failure counter (in JWT filter)
   - Query endpoint latency (auto-instrumented by Spring MVC + Micrometer)
   - Expose `/actuator/metrics` and `/actuator/health`

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
8. Configure `proxy.conf.json` to proxy `/api` requests to Spring Boot (port 8081) during development; add `environment.ts` / `environment.prod.ts` for any environment-specific settings
9. Verify login → protected route → SSE connection → logout flow works end-to-end

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
3. `ImportProject` command — registered with `ImportProjectInput` DTO (name override, enable analysis) and a `fileApplicator` that bridges `MultipartFile.getInputStream()` → `ImportProjectCommand.setInputStream()`. Dispatched via `POST /api/commands/ImportProject` with `multipart/form-data` (JSON input part + file part) through the standard handler chain, including audit logging

**Backend work — Queries:**
1. `GET /api/projects` → paginated list, filtered by `ProjectAccessChecker`: returns only projects where current user is a stakeholder (or all projects for SystemAdmin). Sorted by per-user recency (see activity tracking below). Supports a `limit` parameter for sidebar tree cap.
2. `GET /api/projects/{id}` → full project detail (requires stakeholder membership or SystemAdmin)
3. `GET /api/projects/{id}/tree` → tree structure for sidebar (child entity counts)
4. `GET /api/projects/{id}/export` → XML download (adapt existing `ProjectXmlController`)
5. `GET /api/projects/{id}/my-permissions` → current user's stakeholder permissions for this project (see `doc/AUTH_ARCH.md` §4.2)
6. `GET /api/user-preferences` → current user's UI preferences
7. `PUT /api/user-preferences` → update current user's UI preferences

**Backend work — Command Audit Log:**

All successful command executions are logged to a `command_audit_log` table for both
audit trail purposes and to drive project activity tracking (sidebar recency).

1. `CommandMetadata` class (platform-core) — holds commandType (the URL dispatch string)
   and the typed input DTO. A single `CommandMetadataAware` interface exposes
   `get/setCommandMetadata()`. The `AbstractCommand` base class implements it, so all
   commands in the hierarchy automatically carry metadata.
2. `ApiCommandFactory.newCommand()` creates a `CommandMetadata` with the commandType and
   input DTO, then stamps it on the command via `CommandMetadataAware`.
3. `SensitiveFieldRedactor` utility — generic JSON key scanner that replaces values for
   keys matching patterns like `password`, `repassword`, `secret`, `token`, `credential`
   with `"[REDACTED]"`. No command-type-specific branching.
4. `AuditingCommandHandler` — outermost wrapper in the handler chain. On successful
   execution: extracts commandType and input from `CommandMetadata`, project ID from
   `ProjectScopedCommand`, user ID from `EditCommand.getEditedBy()`, serializes the input
   to JSON with redaction, and inserts a row. Uses `@Transactional(REQUIRES_NEW)` so
   logging failures can't roll back the command. On exception: re-throws without logging.
5. Handler chain becomes: `Auditing → CurrentUser → RetryOnLockFailures → ExceptionMapping → Authorizing → AnalysisInvoking → Default`
6. `command_audit_log` table: `(id, user_id BIGINT, executed_at TIMESTAMP, command_type VARCHAR, command_class VARCHAR, project_name VARCHAR NULL, request_payload TEXT)` — user_id references user table for referential integrity if username changes. Uses project_name (not project_id) because the Project domain interface doesn't expose a public getId(); the API identifies projects by name throughout.

**Backend work — Project Activity Tracking:**

The sidebar tree sorts projects by recent activity, filtered to entity types the
user has stakeholder permissions for (see `doc/UI_DESIGN_GUIDE.md` §3.2.1).

1. Activity data is derived from `command_audit_log` — the audit log's `project_id` and
   `executed_at` columns provide project-level recency. The `command_type` can be mapped
   to entity types for per-entity-type filtering.
2. For performance, a materialized view or summary table `project_entity_activity`
   (`project_id, entity_type, last_activity_at`) can be maintained from audit log data.
3. The `GET /api/projects` query joins the user's stakeholder permissions against activity
   data to compute `MAX(last_activity_at)` across only the entity types the user has
   permissions for. This becomes the sort key.

**Backend work — User Preferences:**

User preferences are a separate concern from the `User` entity (which handles
identity, auth, and contact info). See `doc/UI_DESIGN_GUIDE.md` §3.2.3.

1. New `UserPreferences` entity — its own aggregate, persisted in `user_preferences` table, 1:1 with User but not navigated from User
2. New `UserPreferencesRepository` — simple CRUD
3. Initial fields: `sidebarProjectLimit` (int, default 10), `sidebarProjectStaleness` (enum, default `3_MONTHS` — options: `1_MONTH`, `3_MONTHS`, `6_MONTHS`, `9_MONTHS`, `12_MONTHS`, `ALWAYS`)
4. Query and update endpoints (items 6–7 above)

**Frontend work:**
1. **Sidebar panel visibility**: Projects accordion panel visible only when user has `ProjectUserRole` (JWT role `PROJECT_USER`); Admin panel visible only for `SystemAdminUserRole` (JWT role `SYSTEM_ADMIN`)
2. Project list page with New/Import buttons — both shown only if `canCreateProjects` from JWT permissions (import creates a new project)
3. **Project tree filtering and ordering**: sidebar tree shows only projects where the current user is a `UserStakeholder` (admins see all), ordered by recent activity on entity types the user can see, capped at the user's `sidebarProjectLimit` preference (default 10), and filtered by the `sidebarProjectStaleness` threshold (projects with no relevant activity older than the threshold are hidden). "Show all" link below the tree opens the full list table.
4. New Project form (name, description, org combo)
5. Import Project form (file upload, rename, enable analysis)
6. Edit Project form (name, description, org, createdBy, annotations — annotations placeholder until Phase 7)
7. Project tree sidebar component — clicking items routes to sub-pages
8. Export button triggers download
9. `PermissionService` — fetches `GET /api/projects/{id}/my-permissions` on project load, caches per project, exposes `hasPermission(projectId, entityType, permissionType)` as signals. System roles and role-level permissions read from JWT. See `doc/AUTH_ARCH.md` §4.3.
10. Load `UserPreferences` on login, cache client-side. Provide settings UI (future — initially just backend defaults).

**Echo2 panels replaced:** `ProjectNavigatorPanel`, `ProjectOverviewPanel`, `ProjectImportPanel`, `ProjectNavigatorTreeNodeFactory`

---

### Phase 3: Stakeholders

**Goal:** Stakeholder list and edit forms within a project.

**DTO Design — Nested Polymorphism (Option C):**

Stakeholders have two subtypes (`UserStakeholder`, `NonUserStakeholder`) with divergent
fields. Rather than a flat DTO with many nullable fields (Option A) or fully separate DTOs
and endpoints (Option B), we use a shared base with type-specific nested objects:

```java
record StakeholderDto(
    Long id, int version, String name, String type,   // "user" or "non-user"
    String createdBy, String dateCreated,
    UserStakeholderDetails userDetails,                // null for non-user
    NonUserStakeholderDetails nonUserDetails            // null for user
)
record UserStakeholderDetails(String username, String emailAddress,
    String phoneNumber, String teamName, List<String> permissionKeys)
record NonUserStakeholderDetails(String text)
```

**Why nested over flat:** TypeScript can't narrow a flat interface based on a string
discriminator — all type-specific fields remain `T | null` even after checking `type`.
With nesting, checking `stakeholder.userDetails != null` narrows the entire nested object
to its well-typed shape. Angular templates use `?.` (safe navigation) naturally:
`stakeholder.userDetails?.emailAddress` renders nothing when null, no explicit branching.
The null boundary is at one point (the nested object) rather than spread across many fields.
See `doc/UI_DESIGN_GUIDE.md` §14 "Polymorphic DTOs" for the general pattern.

Input DTOs are inherently separate because create/edit flows differ by type:
- `EditUserStakeholderInput` — projectName, username, teamName, permissionKeys[], version
- `EditNonUserStakeholderInput` — projectName, name, text, version
- `DeleteStakeholderInput` — stakeholderId, version

**Backend work — Commands:**
1. Wire `EditUserStakeholder` input applicator + result extractor — fields: projectName,
   username, teamName, permissionKeys[], version. Resolve project by name, user by
   username. For edit: resolve existing stakeholder by project + user.
2. Wire `EditNonUserStakeholder` input applicator + result extractor — fields: projectName,
   name, text, version. For edit: resolve existing stakeholder by project + name.
3. Wire `DeleteStakeholder` input applicator — fields: stakeholderId, version. Look up
   stakeholder by id from the project's stakeholder set.

**Backend work — Queries:**
1. `GET /api/projects/{name}/stakeholders` → list of `StakeholderDto` (both types, unified)
2. `GET /api/projects/{name}/stakeholders/{id}` → single `StakeholderDto` with full details

**Frontend work:**
1. `StakeholderDto` and detail interfaces in `models/stakeholder.ts`
2. `stakeholder.service.ts` — list and get queries
3. Stakeholder list component — unified table (Name, Type, Team, Email, Phone, Created By)
   with "Add User" and "Add Non-User" buttons gated by `hasPermission(Stakeholder, Edit)`
4. User stakeholder editor — username dropdown (from system users), team combo, permissions
   checklist, goals placeholder
5. Non-user stakeholder editor — name, description text, goals placeholder
6. Routes: `/projects/:name/stakeholders` (list), `/projects/:name/stakeholders/:id` (edit)
7. Sidebar tree: clicking "Stakeholders" group navigates to the stakeholder list

**Echo2 panels replaced:** `StakeholderNavigatorPanel`, `UserStakeholderEditorPanel`, `NonUserStakeholderEditorPanel`

---

### Phase 4: Goals + Stories

**Goal:** Goal and Story CRUD with cross-entity relationships. Introduces the shared
entity selector dialog and the "Referenced by" pattern for cross-entity visibility.

**Design Decisions:**
- **Goal relations are inline** on the goal editor — a table of supporting/conflicting
  goals with add/remove. Adding opens the shared entity selector to pick a goal and
  relation type. No separate route for GoalRelation editing.
- **"Referenced by" section** on the goal detail — lists all GoalContainers (Project,
  UseCase, Scenario, Story, Actor, Stakeholder) that reference this goal. Cross-entity
  visibility is a core feature of the system.
- **All GoalContainers show associated goals** — the story editor includes a goals
  sub-table (add/remove via entity selector). Same pattern will apply to actors, use
  cases, scenarios, and stakeholders as their editors are built.
- **Copy commands included** — both goals and stories support copy with auto-generated
  unique names.
- **Shared entity selector dialog** — built now as a reusable component. Takes an entity
  type, fetches candidates from the project, provides search/filter, returns the selected
  entity. Will be extended for new entity types in later phases.

**DTO Design:**

GoalDto includes relations and referers for the detail view:
```java
record GoalDto(Long id, int version, String name, String text, String createdBy,
    List<GoalRelationDto> relationsFromThisGoal,
    List<GoalRelationDto> relationsToThisGoal,
    List<EntityReferenceDto> referencedBy)
record GoalRelationDto(Long id, int version, Long goalId, String goalName,
    String relationType)
record EntityReferenceDto(String entityType, Long id, String name)
```

StoryDto includes goals and actors for the detail view:
```java
record StoryDto(Long id, int version, String name, String text, String storyType,
    String createdBy, List<EntityReferenceDto> goals, List<EntityReferenceDto> actors)
```

`EntityReferenceDto` is a lightweight cross-entity pointer reused wherever entities
reference other entities — goals, actors, stories, containers, etc.

**Backend work — Commands:**
1. Wire `EditGoal` — fields: projectName, name, text, version. Resolve project,
   find existing goal by name for edit.
2. Wire `EditGoalRelation` — fields: projectName, fromGoalName, toGoalName, relationType,
   version. Resolve project and goals by name.
3. Wire `DeleteGoal` — fields: projectName, goalId, version.
4. Wire `DeleteGoalRelation` — fields: projectName, goalRelationId, version.
5. Wire `CopyGoal` — fields: projectName, goalId, newGoalName (optional).
6. Wire `EditStory` — fields: projectName, name, text, storyTypeName, version.
7. Wire `DeleteStory` — fields: projectName, storyId, version.
8. Wire `CopyStory` — fields: projectName, storyId, newStoryName (optional).

**Backend work — Queries:**
1. `GET /api/projects/{name}/goals` → list of GoalDto (summary: no relations/referers)
2. `GET /api/projects/{name}/goals/{id}` → single GoalDto with relations + referencedBy
3. `GET /api/projects/{name}/stories` → list of StoryDto (summary)
4. `GET /api/projects/{name}/stories/{id}` → single StoryDto with goals + actors

**Frontend work:**
1. **Shared entity selector dialog** — PrimeNG Dialog + Table, takes entity type and
   project context, fetches candidates, provides search, emits selection. Reusable for
   goals, stories, actors in future phases.
2. **Goal list** — table (Name, Text preview, Created By), New/Copy buttons
3. **Goal editor** — name, text, inline relations table (add/remove with entity selector),
   "Referenced by" read-only list, copy/delete actions
4. **Story list** — table (Name, Type, Text preview, Created By), New/Copy buttons
5. **Story editor** — name, type dropdown, text, goals sub-table (add/remove via entity
   selector), copy/delete actions
6. Routes: `/projects/:name/goals`, `/projects/:name/goals/:id`,
   `/projects/:name/stories`, `/projects/:name/stories/:id`
7. Sidebar tree: clicking Goals/Stories group navigates to the list

**Echo2 panels replaced:** `GoalNavigatorPanel`, `GoalEditorPanel`, `GoalRelationEditorPanel`, `GoalSelectorPanel`, `StoryNavigatorPanel`, `StoryEditorPanel`, `StorySelectorPanel`

---

### Phase 5: Actors

**Goal:** Actor CRUD with goals sub-section. Actors are referenced by use cases and stories;
the "Referenced By" section is deferred to Phase 7 when use cases are built.

**Design Decisions:**
- **Goals sub-section** — same add/remove pattern as the stakeholder editor (`actor_goals` join
  table; not `@ManyToAny`, so no Hibernate 6.5 workaround needed).
- **"Referenced By" deferred** — actors are referenced by use cases and stories via
  `actor_actorcontainers` (`@ManyToAny`). That section will be added in Phase 7 once use
  cases exist in the Angular app.
- **No separate NewActor command** — `EditActor` handles both create (actor=null) and update,
  matching the existing pattern for goals and stories.

**Backend work — Commands:**
1. Wire `EditActor` — fields: projectName, actorId (null = create), name, description, version.
2. Wire `DeleteActor` — fields: projectName, actorId, version.

**Backend work — Queries:**
1. `GET /api/projects/{name}/actors` → list of ActorDto (summary)
2. `GET /api/projects/{name}/actors/{id}` → single ActorDto with goals list

**Frontend work:**
1. Actor list — table (Name, Description preview, Created By), New button
2. Actor editor — name, description, goals sub-table (add/remove via entity selector)
3. Routes: `/projects/:name/actors`, `/projects/:name/actors/:id`
4. Sidebar tree: clicking Actors group navigates to the list
5. Extend `findGoalContainerById()` in `ProjectCommandRegistrar` to include actors

**Echo2 panels replaced:** `ActorNavigatorPanel`, `ActorEditorPanel`

---

### Phase 6: Scenarios

**Goal:** Scenario CRUD with the step tree editor (the most complex UI). Must be built before
Use Cases because every UseCase has exactly one associated Scenario created at insert time.

**Design Decisions:**
- **Step type defaults to parent scenario type** — new steps inherit the parent scenario's
  `scenarioType` at creation time. Steps are independent after that; changing the parent's
  type does not retroactively update existing steps.
- **Step type not shown inline in tree node** — type is edited via a per-step edit popup
  (pencil button) along with the optional long-text description. This keeps the tree
  uncluttered: just the step name is visible inline.
- **Sub-scenario nodes are read-only in parent tree** — clicking navigates to that scenario's
  own editor (router-based, URL changes, browser back works). Sub-scenarios are leaf nodes in
  the parent tree; their steps are only visible when navigated into.
- **Adding a sub-scenario uses `ScenarioSelectorDialogComponent`** — a scenario-specific
  selector dialog that lists existing scenarios (excluding the current one and direct cycle
  candidates) and includes an inline "New Scenario" creation form (name + type). This is
  intentionally separate from `EntitySelectorDialogComponent` to keep cycle-detection and
  creation logic out of the shared component.
- **`EntitySelectorDialogComponent` future enhancement (Option A)** — a future phase should
  add an optional `allowCreate: boolean` input and `createLabel: string` input to the shared
  entity selector dialog. When enabled, a "New [type]" button at the top reveals an inline
  creation form. This would allow goals, stories, and other entity selectors to create new
  entities inline without leaving the parent editor. Not implemented in Phase 6 to keep the
  shared component focused.
- **Full step list on save** — `EditScenarioCommand` takes a full ordered list of
  `EditScenarioStepCommand`s (and nested `EditScenarioCommand`s for sub-scenarios). The
  Angular editor sends the complete ordered step list on every save. Step IDs are preserved
  for existing steps (passed to `setStep()`); new plain steps have null stepId; new
  sub-scenarios have `isScenario: true, stepId: null` (created inline by the registrar).
- **Sub-scenario references in save payload** — for existing sub-scenario entries in the
  step list, the registrar passes the existing `ScenarioImpl` to an `EditScenarioCommand`
  with unchanged name/text/type, effectively a no-op that preserves the step list reference
  after the parent's `getSteps().clear()` + rebuild.
- **Copy included** — `CopyScenario` wired, same pattern as goals and stories.

**Backend work — DTOs (new):**
- `ScenarioDto` — `id, version, name, text, scenarioType, createdBy, steps: List<StepDto>`
- `StepDto` — `id, version, name, text, scenarioType, isScenario, scenarioId`
  (summary list omits steps)
- `EditScenarioInput` — `projectName, scenarioId, name, text, scenarioTypeName, version,
  steps: List<EditStepInput>`
- `EditStepInput` — `stepId, name, text, scenarioTypeName, isScenario`
- `DeleteScenarioInput` — `projectName, scenarioId, version`

**Backend work — Commands:**
1. Wire `EditScenario` — builds `List<EditScenarioStepCommand>` from `EditStepInput` list:
   - `isScenario: false, stepId: null` → new `EditScenarioStepCommand` (creates `StepImpl`)
   - `isScenario: false, stepId: N` → `EditScenarioStepCommand` with existing `StepImpl`
   - `isScenario: true, stepId: null` → `EditScenarioCommand` with null scenario (creates
     empty `ScenarioImpl`); added to parent's step list after execute
   - `isScenario: true, stepId: N` → `EditScenarioCommand` referencing existing `ScenarioImpl`
     (no-op update preserving name/text/type; keeps reference in step list after rebuild)
2. Wire `DeleteScenario` — fields: projectName, scenarioId, version.
3. Wire `CopyScenario` — fields: projectName, scenarioId.

**Backend work — Queries:**
1. `GET /api/projects/{name}/scenarios` → list of ScenarioDto (summary, no steps)
2. `GET /api/projects/{name}/scenarios/{id}` → single ScenarioDto with flat step list
   (each entry includes `isScenario` flag and `scenarioId` for sub-scenario navigation)

**Frontend work:**
1. `ScenarioListComponent` — table: Name, Type, Created By; New button
2. `ScenarioEditorComponent`:
   - Name, type (p-select), text fields with change tracking
   - Step tree using `p-tree` with `draggableNodes`/`droppableNodes`
   - Plain step nodes: inline name `<input>`, edit popup button (name + type + text),
     delete button, "add step below" (+) button
   - Sub-scenario nodes: name as router link + navigate icon, delete-from-parent button
   - Toolbar: "Add Step" (appends empty inline row), "Add Sub-scenario" (opens selector dialog)
   - Save sends full ordered step list
3. `ScenarioSelectorDialogComponent` — lists existing scenarios, excludes self and direct
   cycle candidates; inline "New Scenario" creation form (name + type); reused by Phase 7
   use case editor
4. Routes: `/projects/:name/scenarios`, `/projects/:name/scenarios/:scenarioId`
5. Sidebar nav: Scenarios click handler

**Echo2 panels replaced:** `ScenarioNavigatorPanel`, `ScenarioEditorPanel`, `ScenarioSelectorPanel`, `ScenarioEditorTreeNodeFactory`, `ScenarioStepEditorTreeNodeFactory`

**Note:** The step tree is the most complex UI component in Requel. Use PrimeNG's `p-tree`
with `draggableNodes`/`droppableNodes` as the foundation.

---

### Phase 7: Use Cases

**Goal:** Use cases tie together actors, goals, stories, and scenarios. Depends on actors
(Phase 5) and scenarios (Phase 6) being in place.

**Design Decisions:**
- **Primary actor — `p-autoComplete` with auto-create** — as the user types, filter existing
  project actors client-side (loaded list, no extra query). If the user finishes typing with
  no selection, `EditUseCaseCommandImpl` auto-creates the actor on the server. This is the
  first use of `p-autoComplete` in the Angular app. Better UX than forcing the user to leave
  the editor to create the actor first.
- **Scenario section — link + summary, start simple** — show the use case's linked scenario
  as a navigation link with its step count and type. The linked scenario is always present
  (auto-created by `EditUseCaseCommandImpl` on insert). Do not embed the scenario editor
  inline for Phase 7; refine later if needed. The primary scenario's sub-scenarios carry
  `Alternative`/`Exception` types — the full scenario tree is accessible via the scenario
  editor route.
- **Goals/actors/stories sub-tables** — same add/remove pattern as the goals section in the
  stakeholder editor: entity selector dialog to add, remove button per row.
- **Scenario auto-created on insert** — every UseCase creates an empty linked Scenario at
  insert time (domain invariant). The scenario becomes editable via the scenario editor route.
- **"Referenced By" on actor editor** — the actor editor gets its "Referenced By" section
  (use cases and stories) in this phase, now that use cases exist in the Angular app.
- **All three sub-tables use regular `@ManyToMany`** — `usecase_goals`, `usecase_actors`,
  `usecase_stories` are not `@ManyToAny`, so no Hibernate 6.5 native-query workaround needed.

**Backend work — DTOs (new in `service-api`):**
- `UseCaseDto` — `id, version, name, text, primaryActorName, createdBy, scenarioId,
  scenarioName, goals: List<GoalDto>, actors: List<ActorDto>, stories: List<StoryDto>`
  (summary list omits goals/actors/stories)
- `EditUseCaseInput` — `projectName, useCaseId, name, text, primaryActorName, version`
- `DeleteUseCaseInput` — `projectName, useCaseId, version`
- `CopyUseCaseInput` — `projectName, useCaseId`
- `AddStoryToStoryContainerInput` — `projectName, storyContainerId, storyId`
- `RemoveStoryFromStoryContainerInput` — `projectName, storyContainerId, storyId`
- `AddActorToActorContainerInput` — `projectName, actorContainerId, actorId`
- `RemoveActorFromActorContainerInput` — `projectName, actorContainerId, actorId`

**Backend work — Commands (in `ProjectCommandRegistrar`):**
1. Wire `EditUseCase` — fields: projectName, useCaseId (null = create), name, text,
   primaryActorName (auto-create if not found by `EditUseCaseCommandImpl`), version.
2. Wire `DeleteUseCase` — fields: projectName, useCaseId, version.
3. Wire `CopyUseCase` — fields: projectName, useCaseId.
4. Wire `AddStoryToStoryContainer` / `RemoveStoryFromStoryContainer` — currently registered
   as no-arg stubs. Need `AddStoryToStoryContainerInput` DTO and a `findStoryContainerById`
   helper (searches `Stakeholder`s, `UseCase`s, and `Project` itself — all implement
   `StoryContainer`).
5. Wire `AddActorToActorContainer` / `RemoveActorFromActorContainer` — same pattern; need
   `AddActorToActorContainerInput` DTO and a `findActorContainerById` helper (searches
   `UseCase`s and `Project` — actors do not live on stakeholders).
6. Extend `findGoalContainerById` to include `UseCase`s (currently has
   `// UseCases (Phase 7) not yet handled` comment).

**Backend work — Queries:**
1. `GET /api/projects/{name}/use-cases` → list of UseCaseDto (summary, no sub-tables)
2. `GET /api/projects/{name}/use-cases/{id}` → single UseCaseDto with goals, actors, stories,
   and linked scenario reference (id + name)

**Frontend work:**
1. `UseCaseListComponent` — table: Name, Primary Actor, Created By; New button
2. `UseCaseEditorComponent`:
   - Name + text fields with change tracking
   - Primary actor field: `p-autoComplete` filtering project actors client-side; free-text
     allowed (server auto-creates if no match)
   - Scenario section: read-only link to associated scenario (name + step count badge),
     navigates to scenario editor
   - Goals sub-table (add via `EntitySelectorDialogComponent`, remove button per row)
   - Stories sub-table (add via `EntitySelectorDialogComponent`, remove button per row)
   - Actors sub-table — additional actors beyond primary (add/remove)
   - Copy, Delete, Save with change tracking
3. `UseCaseSelectorDialogComponent` — lists existing use cases; reused by future phases
4. Routes: `/projects/:name/use-cases`, `/projects/:name/use-cases/:useCaseId`
5. Sidebar nav: Use Cases click handler
6. Add "Referenced By" section to actor editor (use cases + stories)

**Echo2 panels replaced:** `UseCaseNavigatorPanel`, `UseCaseEditorPanel`, `UseCaseSelectorPanel`, add/remove controllers

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
GET /api/projects/{id}/my-permissions         → current user's stakeholder permissions
GET /api/annotations?entityType=&entityId=    → annotations for any entity
```

**Event stream (SSE — session-based subscription model, see Section 3.5):**
```
GET    /api/events/stream?subscribe=Type:id   → opens SSE stream with initial subscriptions
POST   /api/events/stream/subscriptions       → add subscription (X-Session-Id header)
DELETE /api/events/stream/subscriptions       → remove subscription (X-Session-Id header)
DELETE /api/events/stream/connection          → graceful server-side close (X-Session-Id header)
```

**Multipart commands (file upload through command dispatch):**
```
POST /api/commands/ImportProject      → multipart/form-data (JSON input + file)
```

**Special cases (not through command dispatch):**
```
POST /api/auth/login                  → returns JWT + UserDto
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

**Version excludes system-generated changes:** The `@Version` field only increments on user-initiated modifications. System-generated changes — such as annotations and glossary term associations added by the NLP `AnalysisInvokingCommandHandler` — are excluded from version checks via `@OptimisticLock(excluded = true)` on the `annotations` and `glossaryTerms` collections. This ensures that NLP analysis running after a command does not inflate the version, so the version returned to the client accurately reflects user edits only.

### Entity References: ID-Based, Not Name-Based

The shift from name-based to id-based references is a recurring pattern when moving from server-side UI (Echo2) to client-server API (Angular + REST). Echo2 worked with live Hibernate-managed entity references — identity was implicit in the object graph. The API layer serializes to JSON, so identity must be explicit. Every entity reference crossing the API boundary should use `id` for unambiguous identification, with `name` only for display or creating new entities.

**Guidelines:**
- All DTOs for persisted entities must include `id` and `version`
- Input DTOs for edits/deletes must include `id` (to identify the target) and `version` (for optimistic locking)
- References to related entities (e.g., a project's organization) use `id`, not `name` — names can change, and concurrent renames create race conditions
- Dropdown/selector components should bind to `{ id, name }` objects, sending `id` on save
- Creating a new related entity (e.g., typing a new organization name) is the one case where `name` is sent without an `id`

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
6. ~~**Password hashing** — Are passwords currently hashed in the database?~~ **Resolved: Yes, passwords are already hashed.** `UserImpl._resetPassword()` delegates to `PasswordHasher.hashWithPreferredSettings()`, which hashes with a per-user salt, configurable algorithm, and iteration count (stored in `passwordSalt`, `passwordEncryptingAlgorithmName`, `passwordEncryptingIterations` columns). `isPassword()` re-hashes the candidate password with the stored parameters and compares. The DB column is `hashed_password`. No migration needed; the JWT login endpoint simply calls `user.isPassword(rawPassword)` as-is.
7. ~~**User Guide link** — The main layout header includes a "User Guide" link (carried over from Echo2).~~ **Resolved: Static PDF download.** The Echo2 UI serves `doc/UserGuide.pdf` via a `DownloadButton` (classpath-relative path `../../doc/UserGuide.pdf`). The Angular app will include a "User Guide" link in the header that opens/downloads this PDF from a static resource path (e.g. `/assets/UserGuide.pdf`). The PDF content will need updating after the UI migration to reflect the new Angular interface, but the basic concepts remain the same.
8. ~~**Observability** — Should we add baseline telemetry?~~ **Decided: Minimal baseline in Phase 0.** Add Spring Boot Actuator (already a starter dependency) + Micrometer (included with Actuator, no extra dependency) for low-effort baseline metrics. Scope: command execution latency/error counts by type (via `CommandHandler` timing), query endpoint latency (Spring MVC auto-instrumented), active SSE connections (gauge from `StreamService`), auth failure rate (counter in JWT filter). All open-source/free. Expose via `/actuator/metrics` and `/actuator/health` (health is already public per §3.4). No external dashboarding tool required initially — metrics are queryable from the actuator endpoint. A Prometheus or Grafana stack can be added later if needed.
9. ~~**Optimistic locking — existing JPA support** — Do the current JPA entities already have a `@Version` field?~~ **Resolved: Yes, fully in place.**

## 9. Known Issues and Workarounds

### Hibernate 6.5 `@ManyToAny` collection removal bug

**Symptom:** `UnknownParameterException: Unable to locate parameter '<table>.<column>' for RESTRICT - DELETE` thrown on transaction commit when any element is removed from a `@ManyToAny`-mapped collection.

**Root cause:** Hibernate 6.5 generates a parameterized DELETE for `@ManyToAny` join table rows that uses a column-qualified parameter name (e.g. `goals_goalcontainers.goalcontainer_id`) which the JDBC layer cannot resolve. This is a regression in Hibernate 6.5.

**Affected collections in this codebase:**

| Collection | Entity | Join table | Columns |
|---|---|---|---|
| `AbstractAnnotation.annotatables` | `AbstractAnnotation` | `annotation_annotatable` | `annotation_id`, `annotatable_id` |
| `GoalImpl.referers` | `GoalImpl` | `goals_goalcontainers` | `goal_id`, `goalcontainer_id` |

More collections exist (`Actor.referers`, `Story.referers`, `UseCase.referers`, etc.) and will exhibit the same bug when remove operations are implemented for those entities.

**Workaround pattern (applied to `RemoveAnnotationFromAnnotatableCommandImpl` and `RemoveGoalFromGoalContainerCommandImpl`):**

1. Add a `removeXxxFromYyyJoinTable(Long xId, Long yId)` method to the relevant Repository interface.
2. Implement it in the JPA repository using `getEntityManager().createNativeQuery("DELETE FROM ...")`.
3. In the command `execute()`, instead of `collection.remove(element)`:
   - Get entity IDs via `PersistenceUnitUtil.getIdentifier()`
   - Call the native query method
   - `em.refresh(entity)` to reload the `@ManyToAny` collection from DB (keeps the entity managed, so other collections referencing it remain valid)
4. Continue with the refreshed entity for any subsequent `isEmpty()` checks or merges.
   - **Do not use `detach + find`**: detaching leaves stale references in any other collection that already holds the entity, causing "detached entity passed to persist" on the next merge.

**When this will appear again:** Any command that removes an element from a `@ManyToAny` collection — e.g. `RemoveActorFromActorContainer`, `RemoveStoryFromStoryContainer` (if those exist) — will need the same treatment. All entity base classes (`AbstractProjectOrDomain`, `AbstractProjectOrDomainEntity`, `UserImpl`, `AbstractUserRole`, `OrganizationImpl`, `AbstractAnnotation`, `PositionImpl`, `ArgumentImpl`, `GoalRelationImpl`) have `@Version protected int version`. The Flyway V1__init.sql migration includes ``version` int NOT NULL` on all entity tables. No additional migration needed — the 409 conflict handling described in Section 5 can rely on the existing optimistic locking infrastructure.

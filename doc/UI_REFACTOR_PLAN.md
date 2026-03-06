# UI Refactor Plan: Echo2 → Angular

## 1. Motivation

The Echo2 framework is a legacy Java RIA that renders server-side components over HTTP. It has no community, no updates, and requires a custom javax→jakarta transform to work with Spring Boot 3. Replacing it with Angular gives us:

- A modern, maintainable frontend with active community support
- Clean separation between backend API and frontend rendering
- A command-based API that maps directly to the existing domain command pattern
- Query endpoints for reads, command dispatch for writes (CQRS)
- Independent deployment and development cycles for frontend and backend

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
│  │  CommandHandler ──▶ Repository ──▶ JPA                        │ │
│  │  AnalysisInvokingCommandHandler triggers NLP after writes     │ │
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
POST /api/auth/logout                 → client-side only (discard token)
POST /api/projects/import             → multipart file upload
```

These don't fit the command dispatch pattern — auth is infrastructure, and import involves file upload that needs multipart handling. Note that logout is entirely client-side (discard the JWT); no server endpoint is needed since tokens are stateless.

### 3.2 Frontend: Angular SPA (`requel-angular/`)

Standalone Angular project (separate from Maven build) served as static assets. Uses **Angular 21** and **PrimeNG 21**.

#### Versions

| Library | Version | Notes |
|---|---|---|
| **Angular** | 21.x | Current stable. Standalone components by default (no NgModules). Signals-based reactivity. Vitest as default test runner. |
| **PrimeNG** | 21.x | Tracks Angular's major version. Requires `@angular/core ^21.0`. Uses standalone component imports and signals. |
| **Node.js** | 18+ | Required by Angular CLI |

PrimeNG aligns its major version with Angular — PrimeNG 19 targets Angular 19, PrimeNG 20 targets Angular 20, etc. Always use matching majors.

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
      core/           → auth service, HTTP interceptors, guards, CommandService
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

Replace Echo2 session-based login with stateless JWT authentication:

- **Backend:** Spring Security configured as stateless (no `HttpSession`). A JWT filter extracts and validates the token from the `Authorization: Bearer <token>` header on every request. Token is signed with a server-side secret (HS256) and carries the username and roles as claims. Expiry: 8 hours — no refresh tokens, user re-authenticates when the token expires.
- **Frontend:** Angular `HttpInterceptor` adds the `Authorization` header to every outgoing request. JWT stored in memory (service field) — not `localStorage`, not cookies — so it's cleared on page refresh. Route guards check token presence and expiry (decode the `exp` claim client-side). On 401 response, redirect to login.
- **Endpoints:**
  - `POST /api/auth/login` — accepts `{ username, password }`, returns `{ token, user }`
  - `GET /api/auth/me` — validates JWT, returns `UserDto` (for restoring state on page load if token is still in memory)
  - No logout endpoint — client discards the token

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
  → SSE pushes: { event: "AnnotationAdded", entityType: "Goal", entityId: 7, ... }
  → UI merges the new annotation or re-fetches the entity
```

The command response is the user's "receipt" for their own action. The SSE stream only carries events the user didn't initiate — background processing results, other users' edits, analysis completion.

#### SSE Endpoint

```
GET /api/events/stream    → SSE connection, authenticated via JWT (passed as query param
                            since EventSource doesn't support Authorization headers)
```

The connection is established on login and held open. Spring Boot implements this via `SseEmitter` (servlet) or `Flux<ServerSentEvent>` (WebFlux). SSE auto-reconnects on network interruption.

#### Event Types

```json
{ "event": "EntityUpdated",    "entityType": "Goal",    "entityId": 7,   "updatedBy": "assistant" }
{ "event": "EntityCreated",    "entityType": "Issue",   "entityId": 42,  "parentType": "Goal", "parentId": 7 }
{ "event": "EntityDeleted",    "entityType": "Story",   "entityId": 15 }
{ "event": "AnnotationAdded",  "entityType": "Goal",    "entityId": 7,   "annotationId": 42 }
{ "event": "AnalysisComplete", "entityType": "Project", "entityId": 1 }
```

Events are lightweight notifications, not full entity payloads. The Angular client handles them by either:
- **Invalidate and re-fetch** — mark the affected entity as stale, re-fetch via its GET endpoint. Simpler, more robust, avoids duplicating DTO shapes in events.
- **Merge directly** — if the event includes the changed entity data. Faster but couples event shape to DTO shape.

Recommendation: **invalidate and re-fetch** for simplicity. The data volumes are small (project-scoped entity lists in the tens to low hundreds), so the extra GET is negligible.

#### Angular Implementation

An `EventStreamService` connects on login, parses SSE events, and exposes them as a signal or observable that feature components subscribe to:

```typescript
@Injectable({ providedIn: 'root' })
export class EventStreamService {
  private eventSource: EventSource | null = null;
  private _events = signal<ServerEvent | null>(null);
  readonly events = this._events.asReadonly();

  connect(token: string) {
    this.eventSource = new EventSource(`/api/events/stream?token=${token}`);
    this.eventSource.onmessage = (e) => this._events.set(JSON.parse(e.data));
  }

  disconnect() {
    this.eventSource?.close();
    this.eventSource = null;
  }
}
```

Feature components use `effect()` to react to events relevant to their current view — e.g., the goal editor watches for `AnnotationAdded` events where `entityType === 'Goal'` and `entityId` matches, then re-fetches annotations.

#### Why SSE over WebSocket

- **Unidirectional** — the client only needs to receive push events; it already sends commands via POST. No need for bidirectional WebSocket.
- **Auto-reconnect** — the `EventSource` API reconnects automatically on network interruption.
- **Simpler infrastructure** — works through HTTP proxies and load balancers without special configuration. No WebSocket upgrade handshake.
- **Spring Boot native** — `SseEmitter` in servlet stack, `Flux<ServerSentEvent>` in WebFlux. No additional dependencies.

#### Backend: Publishing Events

When a command executes, the `CommandHandler` (or `AnalysisInvokingCommandHandler`) publishes a domain event to an in-process event bus (Spring `ApplicationEventPublisher`). An SSE adapter listens for these events and pushes them to connected `SseEmitter` instances. This keeps event publishing decoupled from the SSE transport:

```
CommandHandler.execute(command)
  → repository.save(entity)
  → applicationEventPublisher.publishEvent(new EntityUpdatedEvent("Goal", 7, "assistant"))
  → SseEventBridge listens, pushes to all connected SseEmitters
```

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
   - Spring Security config (stateless, no session) with JWT filter
   - JWT utility: token generation (HS256, 8-hour expiry), validation, claim extraction
   - CORS configuration for Angular dev server
3. Update existing per-domain factories (`ProjectCommandFactory`, `UserCommandFactory`, `AnnotationCommandFactory`) to register their command types via `@PostConstruct`
4. Implement auth endpoints: `POST /api/auth/login` → `{ token, user }`, `GET /api/auth/me` → `UserDto`
5. SSE event stream infrastructure:
   - `ServerEvent` record (event type, entityType, entityId, updatedBy)
   - `SseEventBridge` — Spring `@EventListener` that listens for domain events and pushes to connected `SseEmitter` instances
   - `GET /api/events/stream` endpoint — authenticated via JWT query param, returns `SseEmitter`

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
6. `EventStreamService` — connects to `GET /api/events/stream` on login, exposes events as a signal. Disconnects on logout/token expiry.
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
2. Add `ApiCommand<EditUserInput>` to `EditUserCommand` — same fields, plus userId
3. Register both in `UserCommandFactory.registerCommands()`

**Backend work — Queries:**
1. `GET /api/users` → list all users (UserDto[])
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
2. Add `ApiCommand<EditProjectInput>` to `EditProjectCommand` — fields: projectId, name, description, organizationName
3. `ImportProject` command — special handling: multipart upload via `POST /api/projects/import` (not through command dispatch since it involves file upload)

**Backend work — Queries:**
1. `GET /api/projects` → list all projects for current user
2. `GET /api/projects/{id}` → full project detail
3. `GET /api/projects/{id}/tree` → tree structure for sidebar (child entity counts)
4. `GET /api/projects/{id}/export` → XML download (adapt existing `ProjectXmlController`)

**Frontend work:**
1. Project list page with New/Import buttons
2. New Project form (name, description, org combo)
3. Import Project form (file upload, rename, enable analysis)
4. Edit Project form (name, description, org, createdBy, annotations — annotations placeholder until Phase 7)
5. Project tree sidebar component — clicking items routes to sub-pages
6. Export button triggers download

**Echo2 panels replaced:** `ProjectNavigatorPanel`, `ProjectOverviewPanel`, `ProjectImportPanel`, `ProjectNavigatorTreeNodeFactory`

---

### Phase 3: Stakeholders

**Goal:** Stakeholder list and edit forms within a project.

**Backend work — Commands:**
1. Add `ApiCommand<AddUserStakeholderInput>` to `AddUserStakeholderCommand` — fields: projectId, userId
2. Add `ApiCommand<AddNonUserStakeholderInput>` to `NewNonUserStakeholderCommand` — fields: projectId, name, description
3. Add `ApiCommand<EditUserStakeholderInput>` / `EditNonUserStakeholderInput` to edit commands
4. Add `ApiCommand<RemoveStakeholderInput>` to remove command

**Backend work — Queries:**
1. `GET /api/projects/{id}/stakeholders` → list (StakeholderDto[])

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
2. `EditGoal` → `ApiCommand<EditGoalInput>` — fields: goalId, name, text
3. `EditGoalRelations` → `ApiCommand<EditGoalRelationsInput>` — fields: goalId, supportingGoalIds[], conflictingGoalIds[]
4. `DeleteGoal` → `ApiCommand<DeleteGoalInput>` — fields: goalId
5. `NewStory` → `ApiCommand<NewStoryInput>` — fields: projectId, name, text, type
6. `EditStory` → `ApiCommand<EditStoryInput>` — fields: storyId, name, text, type
7. `DeleteStory` → `ApiCommand<DeleteStoryInput>` — fields: storyId

**Backend work — Queries:**
1. `GET /api/projects/{id}/goals` → list (GoalDto[])
2. `GET /api/projects/{id}/goals/{gid}` → single goal with relations
3. `GET /api/projects/{id}/stories` → list (StoryDto[])
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
2. `EditActor` → `ApiCommand<EditActorInput>` — fields: actorId, name, description
3. `DeleteActor` → `ApiCommand<DeleteActorInput>`
4. `NewUseCase` → `ApiCommand<NewUseCaseInput>` — fields: projectId, name, description, primaryActorId
5. `EditUseCase` → `ApiCommand<EditUseCaseInput>` — fields: useCaseId, name, description, primaryActorId, goalIds[], storyIds[], scenarioIds[]
6. `DeleteUseCase` → `ApiCommand<DeleteUseCaseInput>`

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
2. `EditScenario` → `ApiCommand<EditScenarioInput>` — fields: scenarioId, name, type, steps[] (tree as nested DTOs)
3. `DeleteScenario` → `ApiCommand<DeleteScenarioInput>`

**Backend work — Queries:**
1. `GET /api/projects/{id}/scenarios` → list
2. `GET /api/projects/{id}/scenarios/{sid}` → single scenario with step tree

**Frontend work:**
1. Scenarios list + editor
2. Scenario step tree component — drag-and-drop reordering, indent/outdent for alternate/exception paths
3. Step type indicators (normal, alternate, exception)
4. Scenario selector dialog (shared)

**Echo2 panels replaced:** `ScenarioNavigatorPanel`, `ScenarioEditorPanel`, `ScenarioSelectorPanel`, `ScenarioEditorTreeNodeFactory`, `ScenarioStepEditorTreeNodeFactory`

**Note:** The step tree is the most complex UI component in Requel. Consider using a tree library (e.g., Angular CDK drag-drop) or a custom component. Start simple — flat ordered list with type labels — and iterate toward a tree editor.

---

### Phase 7: Annotations (Issues, Positions, Arguments, Notes)

**Goal:** The IBIS discussion system that appears on nearly every entity.

**Backend work — Commands:**
1. `NewIssue` → `ApiCommand<NewIssueInput>` — fields: entityType, entityId, text, mustBeResolved
2. `EditIssue` → `ApiCommand<EditIssueInput>` — fields: issueId, text, status, mustBeResolved
3. `NewNote` → `ApiCommand<NewNoteInput>` — fields: entityType, entityId, text
4. `EditNote` → `ApiCommand<EditNoteInput>` — fields: noteId, text
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
2. `EditGlossaryTerm` → `ApiCommand<EditGlossaryTermInput>` — fields: termId, name, definition, canonicalTermId
3. `DeleteGlossaryTerm` → `ApiCommand<DeleteGlossaryTermInput>`
4. `NewDocument` → `ApiCommand<NewDocumentInput>` — fields: projectId, name, templateConfig
5. `EditDocument` → `ApiCommand<EditDocumentInput>` — fields: documentId, name, templateConfig
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

**Event stream (SSE):**
```
GET /api/events/stream?token={jwt}            → SSE connection for real-time background events
```

**Special cases (not through command dispatch):**
```
POST /api/auth/login                  → returns JWT + UserDto
POST /api/projects/import             → multipart file upload
```

### Composite CommandFactory

The write side is built around a composite `CommandFactory` that unifies command type lookup, creation, and input application into a single facade. This replaces a separate dispatcher + registry with one cohesive component.

**Registration** — each per-domain factory registers its command types at startup:

```java
// In ProjectCommandFactory
@PostConstruct
void registerCommands() {
    registry.register("NewGoal",  NewGoalInput.class,  this::newNewGoalCommand);
    registry.register("EditGoal", EditGoalInput.class,  this::newEditGoalCommand);
    registry.register("DeleteGoal", DeleteGoalInput.class, this::newDeleteGoalCommand);
    // ...
}

// In UserCommandFactory
@PostConstruct
void registerCommands() {
    registry.register("NewUser",  NewUserInput.class,  this::newNewUserCommand);
    registry.register("EditUser", EditUserInput.class,  this::newEditUserCommand);
}

// In AnnotationCommandFactory
@PostConstruct
void registerCommands() {
    registry.register("NewIssue", NewIssueInput.class, this::newNewIssueCommand);
    // ...
}
```

**Facade** — the top-level `CommandFactory` provides the unified entry point:

```java
public class CommandFactory {
    private final CommandRegistry registry;  // populated by per-domain factories

    public Class<?> getInputType(String commandType)   // for JSON deserialization
    public Command newCommand(String commandType, Object input)  // create + applyInput
}
```

**Controller** — thin HTTP layer:

```java
@PostMapping("/api/commands/{commandType}")
public CommandResult execute(@PathVariable String commandType,
                             @RequestBody JsonNode body) {
    Class<?> inputType = commandFactory.getInputType(commandType);
    Object input = objectMapper.treeToValue(body, inputType);
    Command cmd = commandFactory.newCommand(commandType, input);
    return commandHandler.execute(cmd);
}
```

This keeps domain boundaries clean — each domain factory owns its registrations, the facade just delegates, and the controller is pure infrastructure.

### Command Response

All commands return a `CommandResult`:
```json
{
  "success": true,
  "entity": { ... },          // updated entity as DTO (type varies per command)
  "entityType": "Goal"        // discriminator for the Angular client
}
```

On validation failure:
```json
{
  "success": false,
  "error": "Validation failed",
  "violations": [
    { "field": "name", "message": "a name is required." }
  ]
}
```

### Query Response Patterns
- List endpoints return `{ items: T[], total: number, page: number, pageSize: number }`
- Single-entity endpoints return the DTO directly
- All entities include `id`, `createdBy`, `dateCreated`, `version` (for optimistic locking)

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

### Real-time updates: SSE with invalidate-and-refetch
Commands return results synchronously — the user gets immediate feedback for their own actions. Background events (NLP agent adding annotations, other users' edits) push to the client via Server-Sent Events (SSE) rather than WebSocket. SSE is unidirectional (server→client), auto-reconnects, works through proxies, and requires no additional dependencies beyond Spring Boot's `SseEmitter`. Events are lightweight notifications (entity type + ID), not full payloads — the client re-fetches affected entities via their existing GET endpoints. This avoids coupling event shapes to DTO shapes and keeps the stream simple. The alternative — reading all results from the stream (pure event-sourcing style) — was rejected because it adds command correlation complexity, latency on the write path, and error handling for missing events, all without UX benefit for a synchronous command-based system.

### Deployment: static from Spring Boot
Angular build output (`ng build` → `dist/`) is served as static resources from the Spring Boot JAR. Single artifact, single process, single Docker container. No CORS configuration needed in production since frontend and API are same-origin. The alternative — deploying Angular separately via nginx/S3/CDN — adds infrastructure complexity (reverse proxy, CORS, two deploy targets) without benefit for a single-developer, single-server tool. During development, Angular's dev server runs on port 4200 with hot reload and proxies API calls to Spring Boot — the dev experience is the same either way.

### Phasing priorities
The phases are ordered by dependency (auth first, then entities, then cross-cutting features). The annotation system (Phase 7) is deliberately late because it touches every editor — building it after the editors exist avoids rework. However, editor forms should include an "Annotations" placeholder from Phase 2 onward so the layout is correct.

## 7. Effort Estimates by Phase

| Phase | Scope | Commands | Queries | Angular Components | Relative Size |
|-------|-------|----------|---------|-------------------|---------------|
| 0 | Foundation | 0 | 1 (auth/me) + SSE stream | 5 (login, layout, guard, CommandService, EventStreamService) | Medium-Large (infrastructure) |
| 1 | Users | 2 | 4 | 4 (list, editor, org combo, roles) | Small |
| 2 | Projects | 2 (+import) | 4 | 6 (list, new, import, edit, tree, export) | Medium |
| 3 | Stakeholders | 4 | 1 | 4 (list, user-add, non-user-add, edit) | Small |
| 4 | Goals + Stories | 7 | 4 | 5 (2 lists, 2 editors, selector dialog) | Medium |
| 5 | Actors + Use Cases | 6 | 4 | 4 (2 lists, 2 editors) | Medium |
| 6 | Scenarios | 3 | 2 | 3 (list, editor, step tree) | Large (step tree) |
| 7 | Annotations | 6 | 1 | 5 (table, issue, position, argument, note) | Large (retrofitting) |
| 8 | Terms + Docs | 6 | 5 | 5 (2 lists, 2 editors, doc viewer) | Medium |
| 9 | Open Issues | 1 | 1 | 1 (issues view) | Small |
| 10 | Cleanup | 0 | 0 | 0 | Small (deletion) |

**Totals:** ~37 command types, ~27 query endpoints + SSE stream, ~41 Angular components

## 8. Open Questions

1. ~~**UI component library** — Angular Material, PrimeNG, or Taiga UI?~~ **Decided: PrimeNG.** Best fit for data-table-heavy UI. See Section 6 tradeoffs.
2. ~~**Real-time updates** — Echo2 has server-push for concurrent editing. Do we need WebSocket support for multi-user scenarios, or is polling/manual refresh acceptable initially?~~ **Decided: SSE event stream.** Commands return results synchronously; SSE pushes background events (NLP, other users). Invalidate-and-refetch pattern. See Sections 3.5 and 6.
3. ~~**NLP panel priority** — The NLP parser is an admin/debug tool. It could be deferred indefinitely or built as a minimal page.~~ **Decided: Not migrated.** NLP analysis runs automatically via `AnalysisInvokingCommandHandler`; the parser debug UI is dropped from the Angular app.
4. ~~**Document generation** — The current "Run" button generates HTML via XSLT. Decide whether to preserve XSLT-based generation or move to a template engine.~~ **Decided: Keep XSLT for now.** The Angular app will call the existing XSLT-based generation and display the output. Replacing XSLT with a different template engine is a separate future effort.
5. ~~**Deployment model** — Serve Angular as static resources from Spring Boot (`/static/`), or deploy separately (nginx, S3, etc.)?~~ **Decided: Static from Spring Boot.** Angular build output served from the Spring Boot JAR. Single artifact, single process, no CORS in production, no extra infrastructure. During development, Angular dev server runs separately on port 4200 with hot reload. See Section 6.

# Requel 2.0 Test Plan

**Goal:** Establish a comprehensive, automated test suite that gives confidence in a 2.0 release
across three layers — Java unit/integration, Angular component, and full-stack browser automation.

---

## 1. Current State

### Java

Tests live in `modules/requel-app/src/test/`. Framework is **JUnit 4** via the JUnit Vintage
engine. H2 in-memory database (MySQL mode) is used for integration tests; Flyway is disabled in
test profile and Hibernate drops/recreates the schema on each run.

| Existing test class | What it covers |
|---|---|
| `ProjectXmlRoundTripIT` | Full JAXB import → DB → export round-trip |
| `ProjectXmlStreamingRoundTripIT` | Streaming importer variant of the above |
| `ProjectUserCreationIT` | Admin user bootstrap, project creation via commands |
| `AnnotationAnyMappingTest` | Hibernate `@Any`/`@ManyToAny` discriminator mapping |
| `EditProjectCommandImplTest` | EditProject command happy path |
| `ImportProjectCommandTest` / `ImportProjectStreamingCommandTest` | Import commands |
| `ProjectJAXBTest` | JAXB marshalling/unmarshalling |
| `UserImplTest` / `UserCollectionImplTest` / `UserImplMarshallingTest` | User domain |
| `GoalAssistantTest` / `ProjectAssistantTest` | NLP assistant integration |
| NLP tests (`NLPTests`, `LemmatizerTests`, etc.) | Stanford CoreNLP / OpenNLP |

**Gaps:** No command tests for goals, stories, actors, use-cases, scenarios, stakeholders; no REST
API (MockMvc) tests; no authorization tests; no audit log tests.

### Angular

Framework: **Angular 21** with `@angular/build:unit-test` (backed by **Vitest** + **jsdom** —
the Angular-native replacement for Karma as of Angular 19+). `jsdom` is already in
`devDependencies`.

**Gaps:** Zero spec files exist. No E2E tooling installed.

---

## 2. Java Test Strategy

### 2.1 Framework and conventions

- Stay on **JUnit 4 Vintage** for now; migrate to **JUnit 5** incrementally (new tests can use
  JUnit 5 directly — both coexist through the Vintage engine).
- Use **Spring Boot Test** (`@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest`) to get managed
  context injection without manual wiring.
- Use **Mockito** for unit tests that isolate a single class.
- Integration tests that need a database use the existing `application-test.properties` (H2,
  `spring.jpa.hibernate.ddl-auto=create-drop`, Flyway disabled).
- Naming convention: `*Test.java` for unit tests (no Spring context), `*IT.java` for integration
  tests (Spring context, H2).

### 2.2 Command tests

Each edit command should have an integration test that:
1. Creates the minimum prerequisite data (project, user)
2. Executes the command via the `CommandHandler` (respects the full handler chain: authorization,
   analysis invocation, audit)
3. Asserts the result entity reflects the input
4. For edit commands, asserts name uniqueness is enforced (duplicate name throws)
5. For delete commands, asserts the entity is removed from its project

| Test class to create | Commands covered |
|---|---|
| `EditGoalCommandImplTest` | `EditGoal`, `CopyGoal`, `DeleteGoal` |
| `EditStoryCommandImplTest` | `EditStory` (incl. `primaryActorName`), `CopyStory`, `DeleteStory` |
| `EditActorCommandImplTest` | `EditActor`, `CopyActor`, `DeleteActor` |
| `EditUseCaseCommandImplTest` | `EditUseCase` (incl. `primaryActorName`), `CopyUseCase`, `DeleteUseCase` |
| `EditScenarioCommandImplTest` | `EditScenario`, `EditScenarioStep`, `CopyScenario`, `DeleteScenario` |
| `EditStakeholderCommandImplTest` | `EditUserStakeholder`, `EditNonUserStakeholder`, `DeleteUserStakeholder`, `DeleteNonUserStakeholder` |
| `ActorContainerCommandImplTest` | `AddActorToActorContainer`, `RemoveActorFromActorContainer` for Project, UseCase, Story |
| `GoalContainerCommandImplTest` | `AddGoalToGoalContainer`, `RemoveGoalFromGoalContainer` for Project, UseCase, Story |
| `StoryContainerCommandImplTest` | `AddStoryToStoryContainer`, `RemoveStoryFromStoryContainer` |
| `ScenarioContainerCommandImplTest` | `SetPrimaryScenarioOnUseCase`, `AddScenarioToUseCase`, `RemoveScenarioFromUseCase` |
| `GoalRelationCommandImplTest` | `EditGoalRelation`, `RemoveGoalRelation` |
| `AnnotationCommandImplTest` | `EditIssue`, `EditPosition`, `EditArgument`, `DeleteIssue`, `DeletePosition`, `DeleteArgument` |
| `EditUserCommandImplTest` | `EditUser`, `DeleteUser`, `ChangePassword` |
| `AuditingCommandHandlerTest` | Background commands skipped; API commands write row; `projectId` resolved correctly |

### 2.3 REST API tests (MockMvc)

Use `@WebMvcTest` + `MockMvc` to test the HTTP layer in isolation, with the command/query services
mocked. This layer verifies: routing, request deserialization, authorization header enforcement,
error response shapes, and HTTP status codes.

| Test class to create | Controllers covered |
|---|---|
| `CommandControllerTest` | `POST /api/commands/{type}` — happy path, unknown type 400, unauthorized 403, validation 422, conflict 409 |
| `ProjectQueryControllerTest` | All `GET /api/projects/**` endpoints: list, detail, goals, stories, actors, use-cases, scenarios, stakeholders, open-issues |
| `UserQueryControllerTest` | `GET /api/users`, `GET /api/users/{id}` |
| `AuthControllerTest` | `POST /api/auth/login` (success, bad credentials), `POST /api/auth/logout`, JWT token structure |
| `EventStreamControllerTest` | `GET /api/events/stream` — verifies SSE content-type and that subscription is registered |
| `UserPreferencesControllerTest` | `GET`/`PUT /api/user-preferences` |

### 2.4 Repository tests

Use `@DataJpaTest` with H2. These verify the JPA mappings and named queries without running the
full application context.

| Test class to create | What to verify |
|---|---|
| `ProjectRepositoryTest` | `findProjectByName`, `findGoalById`, `findStoryById`, `findActorByProjectOrDomainAndName`, uniqueness constraints |
| `AnnotationRepositoryTest` | `@Any`/`@ManyToAny` load and save for Issue → Position → Argument chains on each entity type |
| `StoryPrimaryActorMappingTest` | `primary_actor_id` FK — set, clear, verify lazy load |
| `UserRepositoryTest` | `findByUsername`, role-based queries |

### 2.5 Authorization tests

Use the full Spring context with `@SpringBootTest` + `MockMvc`. Log in as different user types and
assert permission enforcement.

| Scenario | Expected |
|---|---|
| Unauthenticated `POST /api/commands/EditGoal` | 401 |
| Authenticated user without edit permission on project | 403 |
| Admin editing any project entity | 200 |
| User editing their own account | 200 |
| User editing another account | 403 |

### 2.6 XML import/export

Extend the existing `ProjectXmlRoundTripIT` and `ProjectXmlStreamingRoundTripIT` to cover the
new V2.0 fields:

- Story `primaryActor` survives round-trip (XML → DB → XML)
- Use-case `primaryActor` unchanged
- All V4–V7 schema additions are backwards-compatible with existing XML files

---

## 3. Angular Test Strategy

### 3.1 Framework and tooling

Angular 21's `@angular/build:unit-test` builder uses **Vitest** under the hood with **jsdom** as
the browser environment. This replaces the old Karma/Jasmine setup. Tests are `.spec.ts` files
co-located with the source files they test.

**Required packages to add:**

```bash
npm install --save-dev @testing-library/angular @testing-library/user-event @testing-library/jest-dom
```

`@testing-library/angular` wraps Angular's `TestBed` with a DOM-first API that encourages testing
behavior rather than implementation. It pairs well with Vitest.

**Configure `vitest` in `angular.json`** (the `@angular/build:unit-test` builder accepts a
`vitest` config key):

```json
"test": {
  "builder": "@angular/build:unit-test",
  "options": {
    "include": ["src/**/*.spec.ts"],
    "browser": false,
    "setupFiles": ["src/test-setup.ts"]
  }
}
```

`src/test-setup.ts`:
```ts
import '@testing-library/jest-dom';
```

### 3.2 Service tests

Services are the backbone of the Angular app. Tests mock `HttpClient` using
`provideHttpClientTesting()` and verify the request/response shape.

| Spec file | What to verify |
|---|---|
| `auth.service.spec.ts` | `login()` sends `POST /api/auth/login`; stores JWT; `logout()` clears token; `isLoggedIn()` reflects token state; token expiry |
| `command.service.spec.ts` | `execute()` sends `POST /api/commands/{type}`; returns `CommandResult`; propagates 400/422/409 errors as structured `CommandResult.error` |
| `project.service.spec.ts` | `listProjects()` returns array; `notifyTreeChanged()` emits on `treeChanged$`; cache invalidation |
| `goal.service.spec.ts` | `listGoals()`, `getGoal()` — correct URL construction and response mapping |
| `story.service.spec.ts` | Same; verify `primaryActorName` is included in `StoryDto` |
| `actor.service.spec.ts` | `listActors()`, `getActor()` |
| `use-case.service.spec.ts` | `listUseCases()`, `getUseCase()` |
| `scenario.service.spec.ts` | `listScenarios()`, `getScenario()` |
| `permission.service.spec.ts` | `canEdit()` / `canDelete()` return correct values after `loadForProject()`; permissions cached per project |
| `event-stream.service.spec.ts` | `addSubscription()` registers; `removeSubscription()` deregisters; `events$` emits parsed SSE envelope |
| `auth.guard.spec.ts` | Redirects unauthenticated user to `/login`; passes authenticated user through |
| `dirty-check.guard.spec.ts` | Returns `true` when no unsaved changes; shows confirm dialog when `hasUnsavedChanges()` is true |
| `auth.interceptor.spec.ts` | Attaches `Authorization: Bearer <token>` header; redirects to `/login` on 401 response |

### 3.3 Shared component tests

| Spec file | What to verify |
|---|---|
| `list-page.spec.ts` | Renders `[title]` in header; shows `[actions]` slot; emits `(search)` event on input; renders `<ng-content>` in body |
| `entity-selector-dialog.spec.ts` | Hidden when `[visible]="false"`; shows list when visible; emits `(selected)` with correct `EntityReferenceDto` on row click; emits `(closed)` on cancel |
| `annotations-section.spec.ts` | Renders no annotations when empty; renders issue/position/argument tree; Add Issue button visible when `[canEdit]="true"`, hidden otherwise |
| `sidebar-nav.spec.ts` | Renders project list; project nodes expand to show entity types; active route is highlighted; `notifyTreeChanged()` triggers reload |
| `scenario-selector-dialog.spec.ts` | Filters by `[includeTypes]` and `[excludeIds]` |

### 3.4 Feature page tests (editors)

For editor components, the key behaviors to test are: loading state, save behavior, dirty tracking,
and error display. Mock the injected services.

| Spec file | Key scenarios |
|---|---|
| `login.spec.ts` | Submit with valid credentials → navigates to `/projects`; invalid credentials → shows error message |
| `project-list.spec.ts` | Renders project rows; search filters visible rows; New Project button navigates to `/projects/new` |
| `project-editor.spec.ts` | Loads existing project by name; Save sends `EditProject` command; delete confirmation dialog |
| `goal-editor.spec.ts` | New goal form is empty; save sends `EditGoal` with correct payload; dirty flag set on input change; dirty flag cleared after save; hasChanges blocks navigation when dirty; Add/Remove Goal relationship works |
| `story-editor.spec.ts` | Primary actor `p-select` is populated with actors; selecting actor sets `primaryActorName` in payload; clearing actor sends `null`; Additional Actors section shows/hides Add button per permissions |
| `actor-editor.spec.ts` | Loads actor; saves; Copy button triggers confirm + `CopyActor` command + navigation |
| `use-case-editor.spec.ts` | Primary actor dropdown populated; primary scenario create/select flows; Additional Scenarios add/remove; Goals and Stories tables show associated entities |
| `scenario-editor.spec.ts` | Step table renders; Add Step appends row; drag-to-reorder (if implemented); Save sends steps array |
| `stakeholder-editor.spec.ts` | User stakeholder: user selector shown; Non-user stakeholder: name input shown; correct command type sent for each |
| `user-editor.spec.ts` | Admin sees all fields; non-admin editing self sees limited fields; password change form validation |
| `open-issues.spec.ts` | Issues render with type badge; click navigates to annotated entity editor |

### 3.5 List page tests

List pages are simpler — verify data loading, search filtering, and navigation.

| Spec file | Key scenarios |
|---|---|
| `goal-list.spec.ts` | Goals loaded on init; search filters by name; row click navigates to editor |
| `story-list.spec.ts` | Same pattern; storyType column visible |
| `actor-list.spec.ts` | Same |
| `use-case-list.spec.ts` | No search bar (confirm `[showSearch]="false"` in template) |
| `scenario-list.spec.ts` | Same |
| `stakeholder-list.spec.ts` | Both user and non-user stakeholders shown |

---

## 4. End-to-End Browser Automation

### 4.1 Tool recommendation: Playwright

**[Playwright](https://playwright.dev/)** is the recommended E2E tool for Requel 2.0.

Why Playwright over alternatives:
- **Multi-browser** — Chromium, Firefox, and WebKit from one test suite
- **Headless by default**, full browser available with `--headed` flag for debugging
- **TypeScript-native** — test files are `.ts`, no extra config
- **Built-in waiting** — auto-waits for elements to be visible, no `sleep()` calls
- **Angular-aware** — Playwright's `page.getByRole()` / `page.getByLabel()` APIs work well with Angular's accessibility attributes
- **Tracing and screenshots** on failure built in
- **API request interception** — useful for testing error states without a real backend

The main alternative is **Cypress**, which has a more polished developer UI but runs only in
Chromium and has architectural limitations around multiple tabs (relevant for SSE multi-session
tests).

### 4.2 Installation

```bash
cd requel-angular
npm install --save-dev @playwright/test
npx playwright install chromium firefox webkit
```

Add to `package.json`:
```json
"scripts": {
  "e2e": "playwright test",
  "e2e:headed": "playwright test --headed",
  "e2e:report": "playwright show-report"
}
```

`playwright.config.ts` at repo root (or `requel-angular/`):
```ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,          // Requel has shared DB state; run serially
  retries: 1,
  use: {
    baseURL: 'http://localhost:8081',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox',  use: { ...devices['Desktop Firefox'] } },
  ],
});
```

### 4.3 E2E test scenarios

Tests live in `requel-angular/e2e/`. The Spring Boot backend + MySQL must be running before
the E2E suite runs (use `docker-compose up` for CI).

#### Authentication

| File | Scenario |
|---|---|
| `auth.e2e.ts` | Login with valid credentials → lands on projects page |
| | Login with bad credentials → error message shown, no navigation |
| | Accessing `/projects` while logged out → redirected to `/login` |
| | JWT expiry → subsequent API call redirects to login |

#### Project management

| File | Scenario |
|---|---|
| `projects.e2e.ts` | Create new project → appears in sidebar and project list |
| | Edit project name → new name shown in header and sidebar |
| | Delete project → removed from list; navigates back to project list |
| | Import project XML → all imported entities appear in the correct lists |
| | Export project XML → file downloads; re-import round-trips cleanly |

#### Goals

| File | Scenario |
|---|---|
| `goals.e2e.ts` | Create goal → appears in goal list |
| | Rename goal → new name persists after save and page reload |
| | Add/remove goal relation → relation visible on both goals |
| | Delete goal → removed from list |
| | Navigate to goal from use-case goals table → opens correct editor |
| | Dirty guard → navigate away with unsaved changes → confirm dialog; cancel stays on page |

#### Stories

| File | Scenario |
|---|---|
| `stories.e2e.ts` | Create story → appears in list |
| | Set primary actor via dropdown → actor name shown in form after reload |
| | Clear primary actor → field shows placeholder after reload |
| | Add/remove additional actor |
| | Change story type → persists after reload |

#### Actors

| File | Scenario |
|---|---|
| `actors.e2e.ts` | Create actor; edit name; copy actor → new actor with modified name; delete actor |
| | Actor appears in primary actor dropdown for story and use-case |

#### Use Cases

| File | Scenario |
|---|---|
| `use-cases.e2e.ts` | Create use case; set primary actor; create primary scenario → navigates to scenario editor |
| | Select existing scenario as primary → scenario linked |
| | Add/remove additional scenario, goal, story, actor |

#### Scenarios

| File | Scenario |
|---|---|
| `scenarios.e2e.ts` | Create scenario; add steps; reorder steps; delete step |
| | Edit step text → persists |

#### Annotations (IBIS)

| File | Scenario |
|---|---|
| `annotations.e2e.ts` | Add issue to a goal → issue appears in annotations section |
| | Add position to issue → position nested under issue |
| | Add argument to position → argument nested |
| | Resolve issue → status changes |
| | Open-issues page shows unresolved issues; click navigates to annotated entity |

#### Administration

| File | Scenario |
|---|---|
| `admin.e2e.ts` | Create user; set roles; user can log in |
| | Non-admin cannot see admin controls |
| | Change own password → can log in with new password |

#### SSE live refresh

| File | Scenario |
|---|---|
| `sse-refresh.e2e.ts` | Open goal editor in browser context A; edit and save the same goal via API call (or second browser context B); goal editor in A reloads automatically without manual refresh |

This test requires Playwright's multi-context support — two independent browser contexts
against the same backend.

### 4.4 CI integration

For GitHub Actions or similar:

```yaml
- name: Start Requel
  run: docker-compose up -d
  
- name: Wait for health
  run: |
    for i in {1..30}; do
      curl -sf http://localhost:8080/actuator/health && break || sleep 2
    done

- name: Run E2E
  run: cd requel-angular && npm run e2e

- name: Upload Playwright report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: playwright-report
    path: requel-angular/playwright-report/
```

---

## 5. Coverage Priorities

Not everything needs to be written at once. Recommended order:

| Priority | Layer | What to write first |
|---|---|---|
| 1 | Java | Command tests for all entity CRUD operations — these are the most business-critical paths and the hardest to debug through the UI |
| 2 | Java | `CommandControllerTest` (MockMvc) — verifies the HTTP contract that the Angular app depends on |
| 3 | Angular | Service specs (`auth.service`, `command.service`, `permission.service`) — services used by every component |
| 4 | Angular | `dirty-check.guard.spec.ts`, `auth.guard.spec.ts`, `auth.interceptor.spec.ts` — cross-cutting concerns |
| 5 | Angular | Editor component specs for the main entity types (goal, story, use-case) |
| 6 | E2E | Auth flow + full project lifecycle (create project → goal → story → use-case → scenario) |
| 7 | Java | Repository tests and authorization tests |
| 8 | E2E | Annotations, SSE live refresh, admin flows |
| 9 | Angular | List page specs, remaining editor specs |

---

## 6. Running the test suites

```bash
# Java unit + integration tests
mvn test

# Java integration tests only
mvn -pl modules/requel-app test -Dtest="*IT"

# Angular unit tests (Vitest)
cd requel-angular && npm test

# Angular unit tests with coverage
cd requel-angular && npm test -- --coverage

# E2E tests (requires backend running on :8081)
cd requel-angular && npm run e2e

# E2E headed (debug mode)
cd requel-angular && npm run e2e:headed
```

# E2E Codex Review

## Scope

Reviewed the Playwright E2E suite under `requel-angular/e2e` with two goals:

- identify tests that are likely to break after minor HTML / component markup changes
- identify repeated patterns that should be extracted into common helpers or base page objects

This review is focused on test robustness and maintainability, not on product correctness.

## Main Findings

### 1. Several tests are coupled to table markup instead of user-visible behavior

This is the highest fragility theme in the suite.

Examples:

- `requel-angular/e2e/actors.e2e.ts:99-107`
- `requel-angular/e2e/pages/ActorEditorPage.ts:25-43`
- `requel-angular/e2e/pages/GoalEditorPage.ts:20-35`
- `requel-angular/e2e/pages/UseCaseEditorPage.ts:18-29`
- `requel-angular/e2e/pages/ScenarioEditorPage.ts:18-29`

Problems:

- Many list pages click rows via `p-table td` plus `hasText(...)`, usually with `.first()`.
- `copy actor` asserts `p-table td:not([colspan])` has count `6`, which assumes:
  - exactly 2 rows
  - exactly 3 visible columns
  - no extra hidden/action columns
  - no markup restructuring inside the table

Why this is fragile:

- adding a column, action cell, or helper text breaks the test without changing behavior
- duplicate text in another column can make `.first()` click the wrong cell
- row rendering changes in PrimeNG can break these selectors even if the screen still works

Better direction:

- anchor row selection on a row-level locator, not a cell count
- prefer assertions like “row for X exists twice” rather than “there are 6 non-colspan `td`s”
- add stable test hooks on row containers or names if the UI does not expose good accessible structure

### 2. Several editor actions use generic button names without enough scope

Examples:

- `requel-angular/e2e/pages/ScenarioEditorPage.ts:50-58`
- `requel-angular/e2e/pages/UseCaseEditorPage.ts:53-56`
- `requel-angular/e2e/annotations.e2e.ts:78-81`
- `requel-angular/e2e/annotations.e2e.ts:112-115`

Problems:

- many helpers call `getByRole('button', { name: 'Save' })` or `Delete` at page scope
- this is safe only while there is exactly one visible button with that label in the relevant area
- as soon as a form gets another Save button, nested card, expandable section, or dialog, the selector can hit the wrong element

Why this is fragile:

- adding a secondary Save/Delete action is a minor UI change that can silently redirect test clicks
- nested annotation flows already show multiple action areas, which increases this risk

Better direction:

- scope actions to the form/editor root, for example `app-goal-editor`, `app-scenario-editor`, dialog root, or a known toolbar container
- where possible, use accessible names that are unique at the component level

### 3. PrimeNG-internal selectors are heavily used

Examples:

- `requel-angular/e2e/pages/SettingsPage.ts:19-34`
- `requel-angular/e2e/pages/StoryEditorPage.ts:82-96`
- `requel-angular/e2e/pages/ScenarioEditorPage.ts:96-148`
- `requel-angular/e2e/pages/UserEditorPage.ts:65-101`

Problems:

- selectors depend on internals like:
  - `p-inputnumber input`
  - `p-select`
  - `li.p-select-option`
  - `[data-pc-section="clearicon"]`
  - `.edit-popup-content`
  - `.checkbox-label`
  - `.step-row`
  - `.add-step-row`
  - `button:has(.pi-times)`
  - `button:has(.pi-pencil)`

Why this is fragile:

- these are implementation details of PrimeNG and current local markup
- library upgrades or template refactors can break tests without changing the actual UX
- the selectors often assume exact DOM nesting rather than intended semantics

Better direction:

- prefer `getByLabel`, `getByRole`, and editor-root-scoped locators when possible
- when PrimeNG does not expose stable accessibility hooks, add explicit `data-testid` attributes at the app layer instead of depending on PrimeNG internals

### 4. The scenario step tests are especially sensitive to markup changes

Examples:

- `requel-angular/e2e/pages/ScenarioEditorPage.ts:104-179`

Problems:

- the step editor uses index-based locators and icon-based buttons
- interactions depend on:
  - `.step-row`
  - `.step-name-input`
  - `.drag-handle`
  - `.edit-popup-overlay`
  - icon classes `.pi-times` and `.pi-pencil`

Why this is fragile:

- inserting any extra button or changing icon classes can break remove/edit actions
- changing row structure can invalidate all step helpers at once
- index-based editing becomes brittle if sorting, filtering, or helper rows change

This area is already known to be difficult, based on the comments in the file. It would benefit the most from explicit test IDs.

### 5. Annotation tests are structurally correct, but still tied to CSS class names

Examples:

- `requel-angular/e2e/annotations.e2e.ts:43-55`
- `requel-angular/e2e/annotations.e2e.ts:71-85`
- `requel-angular/e2e/annotations.e2e.ts:103-117`
- `requel-angular/e2e/annotations.e2e.ts:144-148`

Good:

- these tests correctly scope to the specific issue/position text because NLP creates extra annotations automatically

Fragility:

- they still depend on classes like `.annotations-section`, `.issue-item`, `.position-item`, `.argument-item`, `.issue-badge`, `.resolved-badge`
- placeholders like `Position text...` and `Argument text...` are also part of the selector contract

Why this matters:

- annotations are a nested UI where minor presentational changes are likely
- class-based selectors here are understandable, but they should ideally be app-owned test hooks

### 6. The suite still has repeated reload-and-wait patterns that are easy to get wrong

Examples:

- `requel-angular/e2e/goals.e2e.ts:73-76`
- `requel-angular/e2e/scenarios.e2e.ts:72-77`
- `requel-angular/e2e/settings.e2e.ts:28-38`
- `requel-angular/e2e/admin.e2e.ts:149-156`

Problems:

- many tests manually recreate the pattern:
  - register a response listener
  - reload with `domcontentloaded`
  - wait for the API GET
  - assert persisted values

Why this is fragile:

- subtle differences in URL filter logic can cause false positives or races
- the pattern is duplicated enough that future tests will likely reintroduce timing bugs

Better direction:

- extract a shared helper like `reloadAndWaitForJson(page, matcher)` or page-specific `reloadAndWaitForData()`

### 7. Test data cleanup is uneven

Examples:

- `requel-angular/e2e/fixtures/api-helper.ts:113-121`

Observation:

- `deleteProject()` is a no-op, so project fixtures accumulate permanently
- many suites rely on unique names with `Date.now()` to avoid collisions

Why this matters:

- not an immediate HTML fragility problem, but it weakens long-term determinism
- over time it increases the chance of search-based selectors finding stale data if a filter is not narrow enough

## Refactor Opportunities

### 1. Extract a shared base class for list pages

The following page objects are almost the same shape:

- `ActorListPage`
- `GoalListPage`
- `StoryListPage`
- `ScenarioListPage`
- `UseCaseListPage`
- `ReportListPage`
- `TermListPage`
- `ProjectsPage`
- `UserListPage`

Common behavior:

- `goto()`
- search box handling
- click “New ...”
- click row by entity name
- expect entity present / absent in table

Recommendation:

- create a generic `ListPage` base with configurable route, search placeholder, create button name, and row locator strategy
- keep only entity-specific differences in child classes

### 2. Extract a shared base class for simple name/text editors

The following editor pages repeat the same patterns:

- `ActorEditorPage`
- `GoalEditorPage`
- `StoryEditorPage`
- `ScenarioEditorPage`
- `UseCaseEditorPage`
- `ReportEditorPage`
- `TermEditorPage`
- parts of `ProjectsPage`

Common behavior:

- `fillName()`
- `fillDescription()` or `fillText()`
- `save()`
- `delete()`
- `copy()`
- `navigateBack()`
- `expectNameValue()`

Recommendation:

- create a base editor helper for:
  - common field fill methods
  - confirm dialog accept
  - command-response waiting
  - common back/delete/copy flows

### 3. Generalize the selector-dialog helper already present in `UseCaseEditorPage`

`UseCaseEditorPage.addViaSelector()` is a good pattern:

- `requel-angular/e2e/pages/UseCaseEditorPage.ts:86-103`

This same pattern also appears in:

- goal relation selection
- story additional actor selection
- scenario / actor / goal / story association flows

Recommendation:

- move this into a reusable helper, for example `selectEntityFromDialog(...)`
- apply it consistently across page objects

### 4. Extract common persistence helpers for reload validation

Repeated pattern:

- save
- reload
- wait for a specific GET
- assert values

Recommendation:

- add helpers like:
  - `reloadAndWaitForUser(username)`
  - `reloadAndWaitForPreferences()`
  - `reloadAndWaitForCurrentEntity(apiFragment)`

This would remove repeated timing comments and reduce test-specific race fixes.

### 5. Replace manual `newPage()` / `page.close()` repetition with a helper wrapper

Most specs follow the same structure:

- `const page = await adminContext.newPage()`
- instantiate page objects
- run test
- `await page.close()`

Recommendation:

- use a helper such as `withPage(adminContext, async page => { ... })`
- or add a custom Playwright fixture that yields a fresh page and auto-closes it

This would simplify almost every spec file.

### 6. Consolidate fixture lifecycle for project-scoped suites

Most entity suites repeat:

- `beforeAll(createProject)`
- `afterAll(deleteProject)`
- `afterEach(best-effort cleanup of one or more fixtures)`

Recommendation:

- create a small suite helper for project-scoped tests:
  - `withProjectSuite('actors', ({ projectName, cleanup }) => { ... })`
- or at least a shared cleanup registry so specs stop carrying multiple `*ToCleanup` variables

### 7. Consider explicit app-owned test IDs in the most dynamic areas

Highest-value targets:

- scenario steps
- annotations
- sub-table action rows
- settings controls using PrimeNG wrappers

Recommendation:

- add `data-testid` at the app template layer for key controls
- avoid relying on PrimeNG DOM internals where a stable app-owned marker is possible

## Priority Recommendations

### High priority

- replace table-cell-count assertions like `td:not([colspan])`
- stop using generic page-wide `Save` / `Delete` selectors where a component-scoped selector is available
- add stable hooks for scenario-step controls

### Medium priority

- build shared `ListPage` and `EditorPage` base abstractions
- extract common reload-and-wait helpers
- generalize entity-selector dialog flows

### Lower priority

- remove repetitive `newPage()` / `page.close()` ceremony
- improve project cleanup strategy in API helpers

## Bottom Line

The suite is in better shape than a raw UI-driven E2E suite because much of it already uses page objects and API-based fixture setup. The main remaining weakness is selector design: too many tests still depend on PrimeNG DOM structure, raw table markup, or unscoped button labels.

The highest-return work is:

- stabilize selectors around rows, forms, and app-owned test hooks
- extract base page-object behavior that is currently duplicated across most CRUD screens

That combination should reduce both false failures from harmless UI churn and the ongoing maintenance cost of the suite.

# E2E Coverage Improvement Plan

## Status (2026-05-05)

| Phase | Status | Notes |
| --- | --- | --- |
| Phase 1: Small-screen branch coverage | ✅ Complete | `open-issues`, `project-list`, `report-list` all have the recommended state-variation tests. |
| Phase 2: Project and account state coverage | ✅ Complete | `project-editor` covered (incl. dirty-switch and 3 save-failure paths added 2026-05-05). `edit-account` has 4 failure-path tests (validation, generic error, network failure, dirty-guard). |
| Phase 3: Representative failure-path coverage | ✅ Complete | `open-issues` load failure, project import failure, project import-no-file warning, project save validation/error/network failure, report list load failure, report run failure, edit-account validation/error/network failure. |

### Verified branch coverage (rerun)

Last full coverage rerun: **2026-05-01**. The numbers below predate the 2026-05-05
additions (project-list import-no-file warning, project-editor save validation/error/network
failure tests, restricted-user hidden-actions test). A fresh rerun is expected to push
`project-editor.ts` and `project-list.ts` materially higher.

| File | Before | Target | Actual (2026-05-01) |
| --- | ---: | ---: | ---: |
| `open-issues/open-issues.ts` | 37.50% | ≥60% | **87.50%** ✅ |
| `projects/project-list.ts` | 30.30% | ≥55% | **75.76%** ✅ (pre 05-05) |
| `projects/project-editor.ts` | 53.73% | — | **63.77%** (pre 05-05) |
| `reports/report-list.ts` | 42.86% | — | **85.71%** |
| `users/edit-account.ts` | 43.48% | — | **58.70%** |

All §Targets numbers exceeded. Lines and functions are at or near 100% for every file in the table except `project-editor.ts`.

## Current State

The JavaScript E2E coverage report is now source-level and materially better than the earlier baseline. The previous `stories` gap has mostly been addressed.

Current feature totals from `requel-angular/coverage/lcov.info`:

| Feature | Lines | Functions | Branches |
| --- | ---: | ---: | ---: |
| `stories` | 93.75% | 95.12% | 69.62% |
| `use-cases` | 86.58% | 84.55% | 62.13% |
| `scenarios` | 85.14% | 69.32% | 65.77% |
| `goals` | 86.53% | 79.03% | 58.78% |
| `actors` | 79.39% | 65.67% | 58.14% |
| `reports` | 82.82% | 75.56% | 60.94% |
| `terms` | 80.45% | 75.56% | 56.44% |
| `users` | 93.68% | 86.54% | 60.61% |
| `projects` | 78.41% | 68.89% | 46.00% |
| `open-issues` | 83.64% | 70.59% | 37.50% |

The lowest branch coverage is now concentrated in a much smaller set of files, especially:

| File | Lines | Functions | Branches |
| --- | ---: | ---: | ---: |
| `src/app/features/projects/project-list.ts` | 72.22% | 66.67% | 30.30% |
| `src/app/features/open-issues/open-issues.ts` | 83.64% | 70.59% | 37.50% |
| `src/app/features/reports/report-list.ts` | 82.69% | 77.78% | 42.86% |
| `src/app/features/users/edit-account.ts` | 91.55% | 83.33% | 43.48% |
| `src/app/features/projects/project-editor.ts` | 82.69% | 70.37% | 53.73% |

## What Changed Since The Last Plan

The old plan centered on `stories` because coverage there was genuinely weak. That is no longer true.

The current report says:

- `stories` is no longer the best place to spend the next E2E effort
- the biggest remaining weakness is branch coverage, not broad line coverage
- the lowest-value gap is no longer selector fragility
- the highest-value gap is state/branch variety in smaller list and utility screens

So the next plan should shift from “exercise large editors more” to “exercise alternate UI states more deliberately.”

## Is Clicking The Problem?

No, not in the main sense.

The report does not look like Playwright is failing to click. It looks like many conditional branches are simply never being visited.

The remaining low-coverage files are dominated by branches like:

- action visibility versus hidden state
- success versus error banners
- empty list versus populated list
- required versus optional issue markers
- import success versus import failure
- missing file versus selected file
- known entity route versus unknown entity route

Those are scenario-shape gaps, not click reliability gaps.

## Highest-Value Remaining Gaps

### 1. `open-issues.ts`

This is now the weakest branch target in terms of payoff per test.

Likely uncovered branches in [open-issues.ts](./Requel/requel-angular/src/app/features/open-issues/open-issues.ts:1):

- `mustResolveCount() > 0` badge shown versus hidden
- `errorMessage()` branch
- empty success message versus populated table
- per-row `mustBeResolved` true versus false
- `navigateTo()` when entity type is known versus unknown
- route-param reload branch when project name changes

Recommended tests:

1. Project with no open issues shows the success empty-state message and no badge.
2. Project with both required and optional issues shows the badge and both row states.
3. Clicking an entity link navigates to the correct editor for at least two entity types.
4. Route-intercept one `open-issues` load to force the error banner.

### 2. `project-list.ts`

This is currently the lowest-branch file in the feature set.

Likely uncovered branches in [project-list.ts](./Requel/requel-angular/src/app/features/projects/project-list.ts:1):

- `canCreateProjects()` true versus false
- import button flow with file selected versus no file selected
- import success message versus import failure message
- row select event with actual row data versus no row data
- empty project list versus populated project list
- admin role versus permission-based project creation gate

Recommended tests:

1. Admin sees `New Project` and `Import`; restricted user does not.
2. Importing a valid project file shows the success banner and refreshes the list.
3. Importing an invalid project file shows the error banner.
4. Project row click navigates to the project dashboard.
5. A user with no projects sees the empty-state row.

### 3. `reports` and `projects` secondary screens

The next branch tier after the two files above is:

- `report-list.ts`
- `project-editor.ts`
- `edit-account.ts`

These are not as weak as `open-issues` and `project-list`, but they still have obvious uncovered conditional states.

## Updated Priorities

### Phase 1: Small-screen branch coverage — ✅ Complete

Focus first on files where a few tests should move branch coverage quickly:

1. ✅ `open-issues.ts` — 4 tests in `open-issues.e2e.ts` covering empty state, mixed required/optional, entity-link navigation, and load failure.
2. ✅ `project-list.ts` — admin/project-user create-action visibility, empty state, import success, import failure.
3. ✅ `report-list.ts` — permission-gated New Document, empty state, list load failure, run-from-list failure.

Why this order:

- these are the lowest branch files
- they are smaller and cheaper to cover than the large editors
- they should improve the overall report faster than another deep editor pass

### Phase 2: Project and account state coverage — ✅ Complete

Then target:

1. ✅ `project-editor.ts` — create, edit, cancel, dirty-guard, dirty-switch via sidebar (Save & Switch).
2. ✅ `edit-account.ts` — happy-path (username pre-fill, change password, change name) plus 4 new failure-path tests: validation violations, generic error response, network failure, dirty-guard navigate-away.
3. ✅ remaining `reports` editor/list state branches — covered by the new list-side error/empty/permission tests in Phase 1.

This phase should focus on:

- permission-gated actions
- success/error messages
- empty versus populated states
- non-happy-path UI outcomes

### Phase 3: Representative failure-path coverage — ✅ Complete

Use a few targeted error injections rather than broad synthetic coverage chasing.

Good candidates:

- ✅ `open-issues` load failure — `load failure shows error banner` in `open-issues.e2e.ts`.
- ✅ project import failure — `import project failure shows error banner` in `projects.e2e.ts`.
- ✅ representative editor save failure — `edit-account` covers all three failure response shapes (`violations`, `error`, thrown exception) in `account.e2e.ts`.

Recommendation:

- use Playwright request interception for these paths
- keep this intentionally small

## Concrete Next Tests

### Open Issues

1. ✅ `no open issues -> success empty-state message shown`
2. ✅ `mixed required and optional issues -> badge count and row markers shown`
3. ✅ `click issue entity link -> navigates to actor editor` (combined with #4 in `clicking issue entity link navigates to actor and story editors`)
4. ✅ `click issue entity link -> navigates to story editor` (see #3)
5. ✅ `load failure -> error banner shown`

### Project List

1. ✅ `admin user -> New Project and Import buttons visible`
2. ✅ `restricted user -> New Project and Import buttons hidden` — covered by `restricted project user without createProjects permission hides New Project and Import actions` in `projects.e2e.ts` (creates a fresh `ProjectUserRole` user without granting `createProjects`).
3. ✅ `select project row -> navigates to project` — covered implicitly by `clickProject` in the edit/cancel/dirty-guard tests.
4. ✅ `valid import -> success banner and project appears`
5. ✅ `invalid import -> error banner shown`
6. ✅ `no file selected on import change -> no action taken` — covered by `import change with no selected file shows warning and does not call import` in `projects.e2e.ts`. The component now sets a `warningMessage` signal ("Select a project XML file to import.") rendered as a new `p-message[severity="warn"]` (`data-testid="project-list-warning"`) instead of silently no-op'ing on the empty-file branch.

## Targets

The near-term target should stay branch-focused.

Recommended short-term goals:

- `open-issues.ts` branches: move from `37.50%` to at least `60%`
- `project-list.ts` branches: move from `30.30%` to at least `55%`
- overall `projects` feature branches: move from `46.00%` to at least `55%`

The broader suite already has strong line coverage in several feature areas. The next gains should come from better state variation, not more repetitions of the same happy path.

## Recommendation

Do not spend the next pass on `stories`; that work already paid off.

The next useful E2E coverage work is:

1. ✅ `open-issues` state coverage
2. ✅ `project-list` visibility/import/error coverage
3. ✅ a small number of representative failure-banner tests

That should raise the weakest branch numbers with relatively little new test code.

## Remaining Work

After the 2026-05-05 round, all of the previously listed branch-coverage gaps are now covered:

1. ✅ **`project-list.ts` restricted-user hidden buttons** — covered by the new restricted-user test in `projects.e2e.ts` that creates a `ProjectUserRole` user without `createProjects` permission and asserts both action buttons are hidden.
2. ✅ **`project-list.ts` import with no file selected** — the empty-file branch now sets a `warningMessage` signal and renders `p-message[severity="warn"]` (`data-testid="project-list-warning"`); covered by `import change with no selected file shows warning and does not call import`.
3. ✅ **`project-editor.ts` save-failure path** — three new tests intercept `EditProject` to exercise the three cold result-shape branches:
   - `project save validation failure shows violation messages` — `success: false` with a `violations` array; asserts joined messages render in the editor's error `p-message`.
   - `project save generic failure shows error banner` — `success: false` with a top-level `error` string; asserts that string renders.
   - `project save network failure shows fallback error banner` — aborts the request and asserts the `catch` branch produces a visible error banner.

A fresh coverage rerun is needed to record the new numbers for `project-editor.ts` and `project-list.ts`; the §Verified branch coverage (rerun) table above flags this explicitly.

### Selector hygiene: replace class-based selectors with `data-testid`

The suite started with three class-based locators left over from the early migration. All three are now replaced:

| File | Selector | Replacement |
| --- | --- | --- |
| `e2e/account.e2e.ts:203` | `a.header-brand` | ✅ Replaced with `getByTestId('header-brand')` (added `data-testid="header-brand"` to `auth/layout.ts`). |
| `e2e/projects.e2e.ts:402` | `.sidebar-tree` | ✅ Replaced with `getByTestId('sidebar-tree')` (added `data-testid="sidebar-tree"` to the `<p-tree>` in `shared/sidebar-nav.ts`). |
| `e2e/admin.e2e.ts:49` | `.roles-section` | ✅ Replaced with `getByTestId('user-roles-section')` (the `data-testid` was already present on `users/user-editor.ts`; only the test selector needed swapping). |

These were individually low-yield but reduce future test fragility — class names move with styling refactors, testids do not.

## Next Steps

With the originally scoped Phase 1–3 work and all listed remaining gaps closed, the next useful E2E coverage pass should:

1. **Rerun coverage** with `scripts/e2e-with-coverage.sh` to capture post-2026-05-05 numbers. The file-level branch table above is a snapshot from 2026-05-01; the project-editor and project-list rows are stale.
2. **Reassess feature-level branches** from the fresh `lcov.info`. The previous lowest features were `projects` (46.00%), `open-issues` (37.50%), and several mid-tier features around 56–60% (`terms`, `actors`, `goals`, `users`). After today's additions, `projects` and `open-issues` should no longer be the floor; the next candidates are likely `actors`, `terms`, or `goals`, but pick from data, not memory.
3. **Pick the new lowest file** the same way Phase 1 did: target a small set of state-variation tests (empty / populated, success / error, permission gates) rather than another deep editor pass.

# E2E Coverage Improvement Plan

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

Likely uncovered branches in [open-issues.ts](/Users/rregan_platformq/gh-acc/rreganjr/Requel/requel-angular/src/app/features/open-issues/open-issues.ts:1):

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

Likely uncovered branches in [project-list.ts](/Users/rregan_platformq/gh-acc/rreganjr/Requel/requel-angular/src/app/features/projects/project-list.ts:1):

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

### Phase 1: Small-screen branch coverage

Focus first on files where a few tests should move branch coverage quickly:

1. `open-issues.ts`
2. `project-list.ts`
3. `report-list.ts`

Why this order:

- these are the lowest branch files
- they are smaller and cheaper to cover than the large editors
- they should improve the overall report faster than another deep editor pass

### Phase 2: Project and account state coverage

Then target:

1. `project-editor.ts`
2. `edit-account.ts`
3. any remaining `reports` editor/list state branches

This phase should focus on:

- permission-gated actions
- success/error messages
- empty versus populated states
- non-happy-path UI outcomes

### Phase 3: Representative failure-path coverage

Use a few targeted error injections rather than broad synthetic coverage chasing.

Good candidates:

- `open-issues` load failure
- project import failure
- one representative editor save failure

Recommendation:

- use Playwright request interception for these paths
- keep this intentionally small

## Concrete Next Tests

### Open Issues

1. `no open issues -> success empty-state message shown`
2. `mixed required and optional issues -> badge count and row markers shown`
3. `click issue entity link -> navigates to actor editor`
4. `click issue entity link -> navigates to story editor`
5. `load failure -> error banner shown`

### Project List

1. `admin user -> New Project and Import buttons visible`
2. `restricted user -> New Project and Import buttons hidden`
3. `select project row -> navigates to project`
4. `valid import -> success banner and project appears`
5. `invalid import -> error banner shown`
6. `no file selected on import change -> no action taken`

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

1. `open-issues` state coverage
2. `project-list` visibility/import/error coverage
3. a small number of representative failure-banner tests

That should raise the weakest branch numbers with relatively little new test code.

# Post-#124 UI Punch List

Running list of UI issues found while walking the app after the
[#124 — Angular UI/UX, Accessibility, and Front-End Architecture Remediation](https://github.com/rreganjr/Requel/issues/124)
epic wrapped. Captured here first, then triaged into one or more GitHub issues.

This is a working doc only. It does not change any GitHub state. Items graduate to
sub-issues under the epic's conventions (milestone `v2.0`, project **Requel 2.0**,
labels `ui-ux-review` + `priority:{high,medium,low}`) once triaged.

Priorities below are proposed (my read), to be confirmed at triage.

## Items

### Login screen (`requel-angular/src/app/features/auth/login.ts`)

| # | What's wrong | Notes / likely cause | Priority | Issue |
|---|---|---|---|---|
| L1 | Form sits too low / not truly vertically centered; looks bad on mobile | `.login-container` uses `min-height:100vh` + `align-items:center`, so it should center — the "low" look is likely the card's own top content + default body margin. Revisit centering and mobile layout together. | high | |
| L2 | Vertical scrollbar always present on login | Total content height edges just past the viewport (`100vh` + card padding/body margin). Switch to `100dvh` and/or trim padding/margin so no scrollbar appears unless the window is genuinely shrunk. | med | |
| L3 | Shrinking the window distorts the form | `.login-card` is `width:100%; max-width:400px` with no min-width, so it collapses arbitrarily. Add a min-width; let the container show horizontal + vertical scrollbars past that floor instead of distorting. | med | |
| L4 | "Project list updated." text appears on-screen (bottom) at narrow width (~208px) | **Confirmed cause:** CDK `LiveAnnouncer` (announcer.service.ts) renders a `.cdk-visually-hidden` `aria-live` region, but the CDK a11y visually-hidden styles are **not included anywhere** in the app, so that class has no clipping CSS and the announcement text renders visibly. Source of this string: `sidebar-nav.ts:317` (SSE project update) — so it's the authed shell, not login. Affects *all* announcements, not just this one. Fix: include CDK's `a11y-visually-hidden` mixin (or an equivalent `.cdk-visually-hidden` rule) in `styles.scss`. One-rule global fix. | med | |
| L5 | Add the Requel robot logo to the form | Use the top-bar logo `logo_robot.png` (served at `/images/logo_robot.png`). Place with the wordmark — see L7. | high | |
| L6 | Password field is visibly shorter than the username field | `p-password` wraps its own `<input>`; the `width:100%` reaches the wrapper but not the inner input, so it renders short. Make both inputs the same length; move the show/hide toggle to hover at the right edge of the input rather than splitting the row. | high | |
| L7 | "Requel" wordmark too small; want logo + name on one line, tagline below | Scale the wordmark to roughly the logo height, put logo and "Requel" on the same line, and drop "Requirements Elicitation System" tagline underneath. | med | |
| L8 | Too much white space around the form; want the muted-canvas + raised-card look the editors have | Reference: the goal editor — light blue-gray canvas (`--rq-canvas-bg` = `--p-surface-50`) with content on a raised white `app-card` (`--rq-card-bg` + `--rq-card-shadow`). Login already uses `app-card`, but its container background is `--p-surface-ground`. Switch `.login-container` background to `--rq-canvas-bg` and confirm the card shadow reads as raised, so the white form sits on a distinct muted canvas. | low | |

### Top bar / app shell (`requel-angular/src/app/features/auth/layout.ts`)

| # | What's wrong | Notes / likely cause | Priority | Issue |
|---|---|---|---|---|
| T2 | Top menu bar should stick to the top of the window | `.app-header` isn't sticky; on viewports where the page (not just `.main-content`) scrolls, it scrolls away. Add `position:sticky; top:0` + z-index so it pins. | med | |
| T3 | Search button isn't clickable and there's no search; unclear what it would search | **Decision: remove** the disabled placeholder (`search-placeholder`) — a dead affordance implying a feature that doesn't exist. A real top-bar **global artifact search** (jump to any goal/story/term/issue across projects) is a legitimate future feature but needs a backend search endpoint/index — out of scope for this UI punch list; captured under Future ideas below. | low | |

### Left nav / sidebar (`requel-angular/src/app/shared/sidebar-nav.ts`)

| # | What's wrong | Notes / likely cause | Priority | Issue |
|---|---|---|---|---|
| S1 | With many projects, scrolling down the Projects tree and clicking a project leaves the main body scrolled off — have to scroll the sidebar way back up; the whole sidebar is one long scroll | Today the sidebar scrolls as a single unit (`aside.sidebar` `overflow-y:auto` in layout.ts). Want: sidebar capped at viewport height with **per-section** scrollbars — each open accordion panel gets its own scroll region, closed panels collapse to header height and hand their space to open ones. Should generalize past today's 2 sections. Approach: make the accordion a flex column filling the sidebar; each open `p-accordion-content` `flex:1` + `overflow-y:auto`. | high | |

### Data table / list pages (`requel-angular/src/app/shared/app-data-table.ts`)

| # | What's wrong | Notes / likely cause | Priority | Issue |
|---|---|---|---|---|
| D1 | On the user/project list pages the table runs past the window height, so the paging footer is below the fold — have to scroll down to reach paging | `p-table` renders the full page of rows with the paginator below it and no `scrollable`/`scrollHeight`. Fix matches your instinct: make the table `scrollable` with `scrollHeight="flex"` so the header row and paginator stay put and only the data rows scroll between them — row count per page stays fixed, only the visible window scrolls. | high | |
| D2 | The search-box magnifying-glass icon has no space before the input and sits outside it (touching), on the table toolbar and the list-page search | Legacy `p-input-icon-left` wrapper (in `app-data-table` `.dt-search` and `list-page` `.search-field`) is a no-op in PrimeNG 21 (replaced by IconField/InputIcon), so the `<i>` renders flush-left of the input (gap 0px). Fix: overlay the icon inside the input via CSS (wrapper `position:relative`; icon `position:absolute; left:~0.75rem; pointer-events:none`; input `padding-left:~2.25rem`) in both wrappers — restores the intended in-field search look. | low | |

### Form fields — textareas (shared: `app-field` / editors)

| # | What's wrong | Notes / likely cause | Priority | Issue |
|---|---|---|---|---|
| F1 | Textarea fields render small/narrow initially and always have to be manually resized; want a good default width with balanced whitespace around them | No shared textarea sizing. `rows` is inconsistent across editors (2, 4, 5, 6, 8, 10, 15, 20, 25), `[autoResize]` is used in exactly one place (project-editor), and full-width is set per-editor (e.g. term-editor local `app-field textarea { width:100% }`) rather than globally — so some textareas fall back to the narrow browser default. Fix: standardize textarea defaults centrally (app-field / global styles) — full field width, a sensible default min-height, consistent padding, and consider `autoResize` as the default so it grows with content. This is a shared-primitive gap like the #124 N-series. | med | |

### Annotations UI (`requel-angular/src/app/shared/annotations-section.ts`)

| # | What's wrong | Notes / likely cause | Priority | Issue |
|---|---|---|---|---|
| A1 | The annotations section is nicely styled but gets very long — an issue with a resolution plus positions and arguments stacks into a tall block, and a list of them is unwieldy | Today every issue renders fully expanded (issue → resolution row → positions → arguments), no collapse state. Add collapse/expand: (a) per-issue collapse/open, and (b) a global collapse-all that reduces each item to a single line showing as much text as fits, truncated with an ellipsis, toggled by one button. Styling stays; this is a disclosure/summarization layer over the existing tree. | med | |

### Breadcrumbs & project routing (`breadcrumb.ts`, `editor-actions.ts`)

| # | What's wrong | Notes / likely cause | Priority | Issue |
|---|---|---|---|---|
| B1 | Clicking the project name in the breadcrumb (e.g. "Imported Project (10)") errors: "Failed to load the project workspace" — the URL shows `Imported%20Project%20%2810%29` | **Confirmed bug, breadcrumb only.** `breadcrumb.ts` builds `url: '/' + prefix.join('/')` from the **already-encoded** URL segments, then binds `[routerLink]="crumb.url"` as a string — the router encodes it *again* (`%20`→`%2520`), so the workspace receives the literal `Imported%20Project%20%2810%29` and `getProject()` fails. Everywhere else (editor-actions, workspace cards) uses the array form `['/projects', projectName]` with the decoded name and works. Fix: give the breadcrumb a routerLink **commands array of decoded segments** and bind that, instead of a pre-encoded string. | high | |
| B2 | Editor pages show "Overview" / "Open issues" quick-nav links at the top that feel redundant and look out of place | `editor-actions.ts` (added in #154/#128) links a deep-linked editor back to the project workspace + open issues so it's navigable without the sidebar. **Not broken** (array form, decoded input). Decision: keep (and restyle if they read as ugly), or remove as redundant with the breadcrumb project link (once B1 works) + left-nav buttons. | low | |

## Triage / grouping

**Deferred → I8 (Phase 6):** D2 (search-box icon spacing) is its own end-of-epic "table/list polish" ticket. Scope grew from the two noted surfaces to **three** — the data-table toolbar (`.dt-search`), the list-page search (`.search-field`), and the entity-selector dialog search — all sharing the dead `p-input-icon-left` wrapper. Fix is a migration of all three to PrimeNG 21 `IconField`/`InputIcon` (the idiomatic v21 replacement), not the CSS-overlay patch first sketched in the D2 row above.

Split follows the #124 convention of focused, per-component ticket branches (rather than
one sprawling PR). Six candidate issues. Shared-primitive fixes are called out because
they pay off across many screens.

### Proposed issues

**I1 — Login screen overhaul** (`login.ts`) — L1, L2, L3, L5, L6, L7, L8.
Everything visual/structural on the login page: true vertical centering + mobile layout
(L1), no-scrollbar viewport sizing via `dvh` (L2), min-width floor with scrollbars past it
(L3), logo + wordmark on one line with tagline below (L5, L7), equal-length fields with the
password toggle hovering at the right edge (L6), muted canvas + raised card (L8).
*Note:* L6's "p-password renders short" half is a **shared** fix — apply the full-width rule
globally so `edit-account.ts` and `user-editor.ts` benefit too; only the toggle/equal-length
layout is login-local.

**I2 — App shell / top bar polish** (`layout.ts`) — L4, T2, T3.
Announcer live-region leaking visible text pre-auth / at narrow width (L4 — confirm it's the
`AnnouncerService` sr-only region, not `p-toast`, which isn't on login), sticky header (T2),
and removing the dead search placeholder (T3). Small, cohesive.
*(T1 dropped — brand is a working home link; the no-op on the home page and a transient cursor
flicker were the confusion. Watch for the flicker; re-add if it recurs.)*

**I3 — Sidebar per-section scroll** (`sidebar-nav.ts` + `layout.ts`) — S1.
Flex-column accordion capped at viewport height; each open panel owns its scroll, closed
panels hand space to open ones. Standalone layout rework.

**I4 — Data-table scrollable body** (`app-data-table.ts`) — D1.
`scrollable` + `scrollHeight="flex"` so header + paginator pin and only rows scroll.
**Shared** — one change improves all ~13 list pages. Highest leverage.

**I5 — Textarea sizing default** (`app-field` / global styles) — F1.
Centralize textarea defaults (full width, min-height, padding, `autoResize`). **Shared** —
replaces the ad-hoc per-editor `rows`/width scattered across every editor.

**I6 — Annotations collapse/expand** (`annotations-section.ts`) — A1.
Per-issue collapse plus a global collapse-all to one ellipsized line. Enhancement /
disclosure layer over the existing tree.

### Cross-cutting theme — scroll & viewport containment

L1–L3, T2, S1, D1 are all one idea: the shell stays fixed and inner regions own their scroll,
using `dvh` and flex scroll containers so content fits the viewport and the *right* thing
scrolls. They live in different tickets (different files/PRs) but should share one approach so
they compose cleanly — e.g. a sticky header (T2) assumes the page itself doesn't scroll, which
is also what the sidebar (S1) and table (D1) fixes rely on. Worth doing I4 + I3 + T2 with the
same mental model even if in separate PRs.

### Suggested sequencing

1. **I4** (data-table) and **I5** (textarea) first — shared primitives, broad payoff, low risk.
2. **I1** (login) — most visible, self-contained.
3. **I2** (app shell) — small; T2 sets up the scroll model the others assume.
4. **I3** (sidebar) — meatier layout; do after the scroll model is settled.
5. **I6** (annotations) — enhancement, independent, do anytime.

### Alternative framing — by area of concern

If you'd rather organize by concern than by screen: (A) **Layout & scroll/viewport**
(L1–L3, T2, S1, D1), (B) **Login visual identity** (L5–L8), (C) **App-shell affordances**
(L4, T3), (D) **Shared form primitive: textarea** (F1), (E) **Annotations disclosure**
(A1). Cleaner conceptually, but (A) spans four files/PRs, which cuts against the focused-branch
convention — hence the per-component split above is the recommendation.

## Future ideas (out of scope for this punch list)

- **Search — two surfaces, scoped by concern** (origin: the removed T3 top-bar placeholder). The top bar reads as *global* stuff, so search there and in the left nav should mean different things:
  - **Top-bar global search** = everything the current user is permitted to see, across concerns. Results are **permission-scoped**: a project user finds project artifacts (goals, actors, stories, scenarios, use-cases, stakeholders, terms, issues); an admin-only user (no project role) would only see users. This scoping principle is what makes a single global box coherent instead of confusing.
  - **Left-nav "across all projects" search** = project-scoped search over project artifacts, placed in the Projects section near the New / Import / List buttons. Narrower and more discoverable for the common "find a goal/story across my projects" case.
  - Either way it's a **feature** needing a backend search endpoint (likely an index), not UI polish — out of scope for this punch list. Worth its own design ticket if pursued; the two surfaces can share one backend search API with different scope filters.

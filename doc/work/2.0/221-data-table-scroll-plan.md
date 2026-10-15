# #221 — Data-table: scrollable body with pinned header + paginator — plan

Ticket: [#221](https://github.com/rreganjr/Requel/issues/221) (I4, Phase 2 of the
Post-#124 UI polish epic #219). Punch-list item **D1**. Branch: `221-data-table-scroll`
off `release/2.0`. Frontend-only.

## Scope / locked decisions

- **Goal (D1):** on list pages the table runs past the viewport, so the paginator is below
  the fold. Make the **table body scroll** between a pinned header row and the paginator, so
  the page fits the viewport and only the data rows scroll. Rows-per-page unchanged.
- **Approach (confirmed): full flex-height, opt-in.** Use PrimeNG `scrollable` +
  `scrollHeight="flex"`, backed by an unbroken bounded-height flex column from the shell down
  to the table. Opt-in flags keep every non-list-page table (dialogs, admin tag pages, the
  form wizard, editor sub-panels) on today's behavior.

## Why it needs a flex chain (not one line)

`scrollHeight="flex"` only fills/scrolls if every ancestor from a bounded-height box down to
`p-table` is a flex column with `min-height:0`. The current chain
(`main-content` → *page host* → `list-page` → `app-card` → `app-data-table` → `p-table`) is
all block flow, and `app-card` is shared by editor forms / annotations / the login card — so
fill behavior must be opt-in.

## Changes

### Shared primitives (opt-in; default behavior unchanged)

1. **`layout.ts` — shell height + `.main-content`:** change `.layout` from
   `min-height:100vh` to **`height:100vh`** so the shell is bounded to the viewport (this is the
   bounded ancestor the whole fill chain needs — without it `.layout` grows to content and the
   page scrolls); and make `.main-content` a flex column (keep `overflow-y:auto`, padding,
   background). Result: the shell (header + sidebar) stays fixed and `main-content` owns the
   scroll. Safe for editors — their content now scrolls inside `main-content` instead of the
   window (verified). *(This is the shell-scroll-model piece I4 depends on; discovered during
   in-browser verification — the plan originally assumed `main-content` was already bounded.)*
2. **`app-data-table.ts`:** add `@Input() scrollHeight?: string`. When set, render
   `[scrollable]="true" [scrollHeight]="scrollHeight"` and add host class `dt-fill`
   → `:host.dt-fill { display:flex; flex-direction:column; flex:1; min-height:0 }` (+ minimal
   `::ng-deep` so the inner `.p-datatable` flexes). Unset (default) = today's non-scrollable table.
3. **`app-card.ts`:** add `@Input() fill = false` + host class `app-card--fill`
   → in fill mode `:host`, `.app-card`, and `.app-card-body` become `flex:1` flex columns with
   `min-height:0` (header stays fixed). Default false = unchanged surface.
4. **`list-page.ts`:** add `@Input() fill = false`; pass `[fill]="fill"` to its inner
   `<app-card>`; in fill mode make `:host` + `.list-page-wrap` a `flex:1` flex column with
   `min-height:0` (page-header + toolbar fixed, card flexes).

### The 11 list pages (opt in)

goal-list, project-list, term-list, open-issues, stakeholder-list, use-case-list, story-list,
user-list, actor-list, scenario-list, report-list. Each:
- add `:host { display:flex; flex-direction:column; flex:1; min-height:0 }` (fills `main-content`
  and propagates height past the feature-component host — the layer Angular inserts between
  `main-content` and `app-list-page`),
- set `[fill]="true"` on `<app-list-page>`,
- set `scrollHeight="flex"` on `<app-data-table>`.

Before adding the `:host` rule, confirm each page's template root is a single `<app-list-page>`
(a sibling overlay like `<p-confirmDialog>` is fine — it's teleported). Adjust if a page has
real multi-root block content.

### Explicitly out of scope (stay on default table)

`features/admin/tag-categories.ts`, `features/admin/global-tags.ts` (don't use `list-page` —
different layout; follow-up if needed), `shared/app-form-wizard.ts`, and any dialog/sub-panel
table. These never set `scrollHeight`, so they're byte-unaffected.

## Test plan (the verify gate)

- **Unit:** app-data-table spec — a `scrollHeight` input renders `scrollable` + `dt-fill`;
  default (unset) renders neither. app-card spec — `fill` toggles `app-card--fill`. list-page
  spec — `fill` toggles the class and forwards to app-card. Keep existing specs green.
- **Typecheck:** `tsc` app + spec.
- **Dev build (AOT):** compiles all 11 pages + shared components.
- **Manual/visual (the real proof):** on a project/user list longer than the viewport, the
  page shows **no** window scrollbar; the header row and paginator stay put; only the rows
  scroll. Confirm a non-list table (a tag admin page or a wizard step) is unchanged.
- **e2e:** list navigation/paging flows run in CI; a scrollable body can change how Playwright
  reaches rows/paginator — read the report and fix real breakages (update locators only where
  the behavior legitimately changed).

## Risks

- Flexbox `min-height:0` must be set at every level or a tall table overflows and `main-content`
  double-scrolls. Mitigated by setting it at each level and the visual check.
- `app-card` is shared — fill is gated behind an opt-in input, so non-list surfaces are inert.
- Per-page `:host` flex may interact with a page that has multi-root content; checked per page.
- Estimate: filed at 3 pts; the flex chain + 11 pages makes it realistically ~5. Flagging, not
  re-pointing without your say.

## AC mapping

- Header + paginator stay visible without page scroll on a standard viewport → flex chain +
  `scrollHeight="flex"` (visual + e2e).
- Only the row region scrolls; rows-per-page fixed → `[rows]` unchanged; body-only scroll.
- Non-list tables unaffected → opt-in inputs; out-of-scope list verified unchanged.

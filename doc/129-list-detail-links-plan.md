# #129 — 2.2 List/detail patterns: name-as-real-link — Implementation Plan

Issue: https://github.com/rreganjr/Requel/issues/129
Part of the look-and-feel remediation epic (`doc/124-remediation-rollup.md`).
Blocked-by #157 (`app-data-table`) — **now merged** (commit `1b73b53`), which changes this ticket's scope (below).

## Summary

#129 asked to standardize list/detail affordances across the entity lists: shared data-table,
consistent search, shared empty states, real name links, and no reliance on row-click alone.
Sibling ticket **#157** (the `app-data-table` component + list-page migration) has since merged and
already delivered most of that. This ticket is therefore **reduced to the one unmet piece**: making
navigable entity **names real links** (AC #2), which in turn removes row-click as the *sole*
navigation method (AC #3).

## What #157 already delivered (not re-done here)

- **AC #1** — all 13 list pages migrated onto `app-data-table`; each row has a keyboard-accessible
  `⋯` menu with **Open / Edit / Delete** (or a page-specific `rowActions` set).
- **AC #4** — search is consolidated in the table's toolbar (one visible, consistent search box).
- **AC #5** — shared `app-empty-state` renders the empty case on every list.
- `open-issues` already renders its entity cell as a real `<a [routerLink]>` (with a plain-text
  fallback when no route resolves) — the exact pattern this ticket generalizes.

## Reduced scope (this ticket = AC #2 + AC #3)

Make the entity **name/username column a real `<a [routerLink]>`** on the navigable lists that still
render it as plain text, so a name is a first-class link (right-clickable, middle-clickable,
open-in-new-tab) rather than a click-target that only works via row selection.

Navigable lists to update (name is currently plain text):
`actors, goals, projects, reports, scenarios, stakeholders, stories, terms, use-cases` (the nine
enumerated) **+ `users`** (link on the `username` column, its nav key).

Already compliant: `open-issues`. Out of scope (non-navigable admin inline-edit, no detail route):
`global-tags`, `tag-categories`.

## Locked decisions

1. **Link support is a first-class column feature of `app-data-table`**, not per-page boilerplate.
   Add an optional `link` factory to `DataTableColumn`; the default text cell renders an anchor when
   it resolves, else falls back to plain text. One place to style and a11y-test; minimal drift.
2. **Anchor click stops propagation** so it never double-fires the row's `rowClick`, and native
   modifier-click / open-in-new-tab behave correctly.
3. **Row-click stays as a convenience** (matches #157 decision 4). Real links + the `⋯` menu are the
   discoverable/keyboard paths; whole-row click remains redundant belt-and-suspenders. No routing
   targets change — additive only.
4. **Reports** already has `[rowClickable]="false"`; it simply gains the name link (its `⋯` menu
   stays). No behavior removed.

## Contract — `DataTableColumn` change (`requel-angular/src/app/shared/app-data-table.ts`)

Add to the interface:

```ts
/** When present and it returns a non-null commands array, the default cell
 *  renders `<a [routerLink]="link(row)">value</a>`; otherwise plain text. */
link?: (row: T) => unknown[] | null | undefined;
```

Default cell branch becomes: `cellTemplate` > resolved `link` anchor > plain text. Import
`RouterLink` into the component. Add a shared `.dt-link` style (theme tokens; visible focus ring).

## Step-by-step

1. `app-data-table.ts`: add `link?` to `DataTableColumn`; import `RouterLink`; render the anchor
   branch with `(click)="$event.stopPropagation()"`; add `.dt-link` styling.
2. Add `link:` to the name column on the 9 enumerated lists + `username` on `users`. Each is a
   one-line arrow closing over the same route the page's open-handler already navigates to:
   - actors `a => ['/projects', this.projectName, 'actors', a.id]`
   - goals `g => ['/projects', this.projectName, 'goals', g.id]`
   - projects `p => ['/projects', p.name]`
   - reports `r => ['/projects', this.projectName, 'reports', r.id]`
   - scenarios `s => ['/projects', this.projectName, 'scenarios', s.id]`
   - stakeholders `s => ['/projects', this.projectName, 'stakeholders', s.id]`
   - stories `s => ['/projects', this.projectName, 'stories', s.id]`
   - terms `t => ['/projects', this.projectName, 'terms', t.id]`
   - use-cases `uc => ['/projects', this.projectName, 'use-cases', uc.id]`
   - users (on `username`) `u => ['/users', u.username]`
3. Leave `rowClick`, `⋯` menus, search, and empty states untouched.

## Test plan

- **Unit (`app-data-table.spec.ts`):** a column with `link` renders an `<a>` carrying the expected
  `routerLink` and the field text; without `link` renders plain text; anchor click does **not** emit
  `rowClick` (stopPropagation).
- **Per-page specs:** confirm existing name-cell assertions still pass (text still present inside the
  anchor); add a link-presence assertion on one representative page.
- **Typecheck:** `npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit`.
- **e2e:** routing targets are unchanged (additive), so existing nav e2e stay green; update the list
  page objects to click the name link where they currently click the row, and assert the anchor is
  present/focusable. Runs in CI.

## Out of scope

Anything already shipped by #157 (data-table, search, empty states, `⋯` menu); admin tag pages
(non-navigable); removing whole-row click; new bulk actions.

## Risks

- **Double-nav / row-click bubbling** — mitigated by `stopPropagation` on the anchor.
- **Column built before `projectName` is set** — `link` is a closure evaluated at render time, so it
  reads the current `this.projectName`; safe whether columns are class-field or `ngOnInit` built.
- **Spec brittleness** — specs that matched the name via a text-only selector still match text inside
  the anchor; only add, don't rewrite, assertions.

## AC mapping

| AC | Status |
|----|--------|
| 1 — standardize affordances across 9+ entity types | Delivered by #157 (all lists on `app-data-table`) |
| 2 — names are real links + optional action buttons | **This ticket** — name/username `<a routerLink>` on 10 lists |
| 3 — eliminate row-click as the sole nav method | **This ticket** — real links + existing `⋯` menu |
| 4 — consistent visible search/filter | Delivered by #157 (table toolbar search) |
| 5 — shared empty-state component | Delivered by #157 (`app-empty-state`) |

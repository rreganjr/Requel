# N4 — Data-table pattern component (`app-data-table`) — Implementation Plan

Issue: https://github.com/rreganjr/Requel/issues/157
Part of the look-and-feel adoption in `doc/124-lookandfeel-plan.md` (sub-issue N4 of epic #124).
Concretizes #129 (list/detail consistency) + #146 (shared primitives).

## Summary

Build a standalone, reusable `app-data-table` component that wraps PrimeNG `Table`, and
migrate every list page onto it. The component is self-contained so it can be reused
outside the list-page shell (dialogs, editor sub-panels, detail tabs), not just on full
list pages. Its dependencies already exist in the repo: `app-tag`/`app-chip` (N2),
`app-card` (N3), `app-empty-state`/`app-error-state`/`app-loading-state` (#131), and the
`app-list-page` shell.

## Resolved decisions

1. **Scope — all list pages.** Migrate the seven enumerated pages
   (goal, story, actor, stakeholder, scenario, use-case, term) **and** the remaining
   table pages: `project-list`, `user-list`, `report-list`, `open-issues`, and admin
   `tag-categories` / `global-tags`.
2. **Tag column renders tag chips, not status.** The enumerated entities have no status
   field (only `project`/`api-token` do). The `app-tag`/`app-chip` column renders each
   row's existing tag chips (as `goal-list` does today). A page that has a real status
   may still map it to a tag tone via a custom cell template.
3. **`app-data-table` owns search; `app-list-page` search is opt-out.** Add
   `[showSearch]="false"` support to `app-list-page` so full list pages let the table
   drive search. This consolidates search in one place and avoids two search boxes.
4. **Row actions: default `⋯` menu + keep row-click open.**
   - Default menu is **Open / Edit / Delete**, permission-gated.
   - Row-click (whole row) still opens the entity — belt and suspenders.
   - Menu items are real `<button>`s with accessible names (satisfies #136 / #137).
5. **Action override model — replace, never merge.**
   - Declarative tweak (most pages): a `[rowActions]` input, an array of
     `{ label, icon, command, visible?, disabled? }`. Passing it **replaces** the
     open/edit/delete defaults.
   - Full override (odd pages): a projected `<ng-template #rowActions let-row>`. Because
     each row's menu needs its row data, this is an `ng-template` with row context, not a
     plain `ng-content` slot. When present it **replaces** the default menu.
   - Merge semantics are intentionally not supported (complexity), keeping one mental
     model: a page either takes the default or fully owns its actions.
6. **Selection column built now.** Ship the optional checkbox multi-select column even
   though no bulk action consumes it yet; expose the selection via an output so a future
   bulk-action toolbar can bind to it.

## Component API (`app-data-table`)

Standalone Angular component, `selector: 'app-data-table'`, generic over the row type.

Inputs:

- `value: T[]` — rows.
- `columns: DataTableColumn<T>[]` — column config (see below).
- `loading = false` — drives the loading state.
- `paginator = true`, `rows = 20` — client-side pagination (all rows loaded; matches
  current behavior on every page).
- `selectable = false` — renders the leading checkbox selection column when true.
- `rowActions?: RowAction<T>[]` — declarative row-action menu; replaces the default
  Open/Edit/Delete set when provided.
- `showToolbar = true`, `title = ''`, `searchPlaceholder = 'Search...'` — the internal
  toolbar (title + search + projected primary-action slot). Search filters the table
  internally via PrimeNG global filter.
- `globalFilterFields?: string[]` — fields the search box filters on.

Outputs:

- `rowClick: EventEmitter<T>` — whole-row open affordance.
- `selectionChange: EventEmitter<T[]>` — for a future bulk-action consumer.
- Default action outputs (`open`/`edit`/`delete`) OR the `command` callbacks carried on
  `rowActions` items.

Content-projection slots:

- `[toolbarActions]` — primary action button(s), e.g. the `New` button, and any
  per-page filters (the goal-list tag `p-select`).
- `<ng-template #rowActions let-row>` — full row-action override (replaces default menu).
- Per-column cell templates for custom rendering (chips, sliced text previews) via the
  column config `cellTemplate`.
- `[empty]` / default empty state — falls through to `app-empty-state` when no template
  is supplied.

Supporting types:

```ts
interface DataTableColumn<T> {
  field: string;
  header: string;
  sortable?: boolean;
  cellTemplate?: TemplateRef<{ $implicit: T }>; // custom cell (chips, previews, tags)
  class?: string;
}

interface RowAction<T> {
  label: string;
  icon?: string;
  command: (row: T) => void;
  visible?: (row: T) => boolean;   // permission / state gating
  disabled?: (row: T) => boolean;
}
```

## Relationship to `app-list-page`

- Full list pages keep `app-list-page` for page chrome (page-header, eyebrow, card) and
  nest `app-data-table` inside it, with `<app-list-page [showSearch]="false">` so the
  table owns search.
- Standalone contexts (dialogs, sub-panels) use `app-data-table` directly; its internal
  toolbar provides title + search + primary-action slot without needing `app-list-page`.
- `app-list-page` change: add `@Input() showSearch = true;` gate around the existing
  search toolbar (component already reads `showSearch` — verify and wire the template).

## Per-page migration notes

All pages currently use PrimeNG `p-table` and load the full list client-side, so search /
sort / paginate stay client-side.

| Page | Row-click open | Default actions | Notes |
|---|---|---|---|
| `goal-list` | yes | Open/Edit/Delete (gated by `canEdit`) | Keep tag-chip column + tag `p-select` filter in `[toolbarActions]`; text preview cell template. |
| `story-list` | yes | Open/Edit/Delete | Standard. |
| `actor-list` | yes | Open/Edit/Delete | Standard. |
| `stakeholder-list` | yes | Open/Edit/Delete | Standard. |
| `scenario-list` | yes | Open/Edit/Delete | Currently sets `showSearch` explicitly — confirm search fields. |
| `use-case-list` | yes | Open/Edit/Delete | Currently sets `showSearch` explicitly — confirm search fields. |
| `term-list` | yes | Open/Edit/Delete | Standard. |
| `project-list` | yes | Open/Edit/Delete | Has a real `status` field — may map to an `app-tag` tone via a cell template. No `canEdit` gate today; confirm permissions. |
| `user-list` | yes | Open/Edit/Delete | Admin context; no `canEdit` gate today — confirm permission model. |
| `report-list` | no | Open/Edit/Delete? | No row-select today; confirm intended actions. |
| `open-issues` | no | **Custom** | Use `<ng-template #rowActions>` override; no row-nav today. |
| `admin/tag-categories` | no | **Custom (Delete inline)** | Not on `app-list-page` today; use table's own toolbar or adopt the shell; custom actions via template override. |
| `admin/global-tags` | no | **Custom (Delete inline)** | Same as tag-categories. |

## Accessibility (#136 / #137)

- Row-action menu items are `<button>`s with visible or `aria-label` names.
- The `⋯` trigger has an accessible name (e.g. `aria-label="Row actions"`).
- Keyboard: menu is openable and navigable by keyboard; row-click open must not be the
  only affordance (the `⋯` menu provides the explicit, focusable path).
- Empty state via `app-empty-state`; loading via `app-loading-state`; error via
  `app-error-state`.

## Testing (acceptance)

- Unit tests for `app-data-table`: renders columns, sorts on a sortable header, paginates,
  filters via search, emits `rowClick`, renders default vs. `[rowActions]` vs.
  `<ng-template #rowActions>` (override replaces default), toggles the selection column and
  emits `selectionChange`.
- Migrate + update specs for at least the migrated pages already carrying `.spec.ts`
  (goal, project, stakeholder, use-case, story, user, actor, scenario) so search + sort +
  paginate are covered per the acceptance criteria.
- a11y spec for the row-action menu (accessible names, keyboard) alongside existing
  `*.a11y.spec.ts` patterns.

## Build order

1. Add `[showSearch]` opt-out to `app-list-page`.
2. Build `app-data-table` (+ types, + unit/a11y specs).
3. Migrate the seven enumerated pages (they share the standard shape).
4. Migrate `project-list` / `user-list` / `report-list`.
5. Migrate the custom pages (`open-issues`, admin `tag-categories` / `global-tags`) using
   the template override.
6. Verify: `cd requel-angular && npm test -- --watch=false` green.

## Open items (non-blocking) — resolved during implementation

- `report-list`: kept its inline **Edit** / **Run** buttons via a `<ng-template #rowActions>`
  override; rows do not navigate (`[rowClickable]="false"`).
- `project-list` / `user-list`: kept the existing single **Open** action (they had no
  list-level edit/delete); `project-list` New/Import stay gated by `canCreateProjects`.
- Admin tag pages (`tag-categories` / `global-tags`): kept their own page chrome (custom
  page-header + add-row form) and used `app-data-table` with `[showToolbar]="false"`; the
  per-row **Delete** stays via a `<ng-template #rowActions>` override.
- `open-issues`: no row actions (`[defaultActions]="false"`) and no row nav
  (`[rowClickable]="false"`); entity links and the Required Yes/No labels render via cell
  templates; initial sort via `sortField`/`sortOrder`.

## Component API additions made during implementation

Beyond the API in the section above, the build added three inputs to `app-data-table`:
`rowClickable` (default true), `sortField`, and `sortOrder` — needed by the custom pages.

## Status

All 13 list pages migrated (`goal`, `story`, `actor`, `stakeholder`, `scenario`,
`use-case`, `term`, `project`, `user`, `report`, `open-issues`, admin `tag-categories`,
admin `global-tags`). Full frontend suite green (71 files / 418 tests).

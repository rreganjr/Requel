# #234 — Search icon: migrate the three search boxes to PrimeNG 21 IconField — plan

Ticket: [#234](https://github.com/rreganjr/Requel/issues/234) (I8, Phase 6 of the
Post-#124 UI polish epic #219). Punch-list item D2. Branch: `234-search-icon-iconfield`
off `release/2.0`. Frontend-only.

## Scope / locked decisions

Three search boxes render the magnifying-glass icon flush against the input (gap 0) because
they use the legacy `p-input-icon-left` wrapper span, which is a no-op in PrimeNG 21 (replaced
by the IconField / InputIcon components). Migrate all three to the idiomatic v21 components,
removing the dead wrappers:

- `app-data-table.ts` — the table toolbar search (`.dt-search`).
- `list-page.ts` — the list-page toolbar search (`.search-field`).
- `entity-selector-dialog.ts` — the entity-picker dialog search.

Confirmed against the installed PrimeNG (21.1.3): `<p-iconfield>` (default
`iconPosition="left"`) wraps `<p-inputicon styleClass="pi pi-search" />` before the input; both
support `styleClass`. IconField insets the icon inside the field and pads the input clear of it.

## Changes (per component)

For each file:
- **Imports:** add `IconField` from `primeng/iconfield` and `InputIcon` from `primeng/inputicon`
  to the file imports and the component `imports:` array.
- **Template:** replace
  ```
  <span class="p-input-icon-left [dt-search|search-field|]">
    <i class="pi pi-search"></i>
    <input pInputText ... />
  </span>
  ```
  with
  ```
  <p-iconfield [styleClass]="…keep the sizing hook…">
    <p-inputicon styleClass="pi pi-search" />
    <input pInputText ... />
  </p-iconfield>
  ```
  Every attribute on the existing `<input>` is preserved verbatim — `ngModel` / `[value]`,
  `(input)`, `placeholder`, `[attr.aria-label]`, and the `data-testid`s
  (`data-table-search`, `entity-selector-search`) that specs and e2e rely on.
- **Styles:** the old `.dt-search` / `.search-field` rules were `display:inline-flex;
  align-items:center` for the removed span. Keep the class as a hook on `p-iconfield` (via
  `styleClass`) only if a width/layout rule still needs it; otherwise drop the now-dead rule.
  Final call made against the live render (the input should keep its toolbar width and the icon
  sit inside it).

## Test plan (the verify gate)

- **Unit:** the specs target the search **input** by its `data-testid`, which is unchanged, so
  `app-data-table.spec`, `list-page.spec` (if present), and `entity-selector-dialog.spec` stay
  green. No spec references the old `p-input-icon-left` / `pi pi-search` markup (grep-confirmed).
- **Typecheck:** `tsc` app + spec.
- **Dev build (AOT):** compiles all three components with the new components.
- **Manual/visual:** on a list page (table toolbar search), a `list-page` search, and the
  entity-selector dialog — the magnifying glass sits **inside** the input with the text padded
  clear of it; typing still filters; toolbar width unchanged.
- **e2e:** search/filter flows run in CI; the wrapper change can shift how Playwright reaches the
  icon (never the input) — read the report, fix only real locator breakages.

## Risks

- IconField/InputIcon are new to this codebase (first use); the API is confirmed against the
  installed version, and the AOT build + visual check cover regressions.
- Preserve each input's `data-testid` and bindings exactly, or specs/e2e break — enforced by
  copying the `<input>` verbatim.
- Estimate: filed at 2 pts; three near-identical template swaps — should land at or under.

## AC mapping

- Search icon sits inside the input with correct spacing on all three surfaces → IconField.
- No `p-input-icon-left` markup remains → all three wrappers replaced.
- Input behavior, accessible names, and test hooks unchanged → `<input>` copied verbatim.

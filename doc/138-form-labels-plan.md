# Implementation Plan — #138 4.4 Form labels and error associations

Part of the UI/UX remediation epic **#124**. Source: `doc/UI_UX_REVIEW.md` Finding 4.4. Phase 3.
Branch: `138-form-labels` off `release/2.0` (after #199 merged).
Blocked by #132, #133, #173, #158 — all merged. This is the labelling *sweep* over the
surfaces `app-field` does not cover.

> The structural half of Finding 4.4 is already shipped; this plan narrows #138 to what
> remains. §5 drafts the revised issue body/AC (posted separately for approval).

## 0. Already delivered — NOT in scope

- `app-field` (#158) stamps `id`, `aria-invalid`, `aria-describedby` (helper + error) and
  `aria-required` on every projected control; rolled out to all reactive-form editors by
  #132 and the create-flow wizards #173. So AC "controls set aria-invalid" and
  "errors/helper linked via aria-describedby" are satisfied for every `app-field` surface.
- `app-submit-error` (#133) announces blocking form/command errors via `role="alert"`.

## 1. Scope (locked decisions)

1. **Mini-form add-rows get a `<fieldset>` + `<legend>`** (chosen over per-input visible
   labels, to keep the compact inline add-row layout). The inputs keep their existing
   `aria-label`s as accessible names; the legend gives the group a visible, programmatic name.
2. **Search labels via an `@Input`.** `list-page` gains `@Input() searchAriaLabel`; the
   entity-selector-dialog derives an entity-specific name from `entityType`.
3. **Placeholder-as-instruction rework is OUT of scope** (tracked separately if wanted).

## 2. Inventory (grounded in the current tree)

### 2.1 Mini-form add-rows -> fieldset/legend
| Surface | Add-row inputs | Legend |
|---|---|---|
| `shared/tag-selector.ts` | category (optional), value | "Add tag" |
| `features/admin/global-tags.ts` | category (optional), value | "Add global tag" |
| `features/admin/tag-categories.ts` | name, exclusive, allowed types, values, color | "Add tag category" |
| `shared/annotations-section.ts` | add-note / add-issue / add-position / add-argument forms | per form: "Add note", "Add issue", "Add position", "Add argument" |

Each add-row's container becomes a `<fieldset class="…">` with a `<legend>`; existing
styling is preserved (fieldset reset: no border/margin, legend styled or visually-hidden
where a visible legend would crowd the toolbar — see §3).

### 2.2 Search labels
- `shared/entity-selector-dialog.ts` — search input has generic `aria-label="Search"` and
  `placeholder="Search…"`. Give it `aria-label="Search {{ entityType }}s"` (e.g. "Search
  goals") so the picker's search names what it filters. This is the only *live* search today.
- `shared/list-page.ts` — add `@Input() searchAriaLabel` (falls back to `searchPlaceholder`
  or "Search"), bound onto the input's `aria-label`. Note: **every** current `app-list-page`
  caller sets `[showSearch]="false"`, so no per-list wiring is needed now; this makes the
  shared control correct for whenever search is turned on.

### 2.3 Audit
- Confirm no reactive-form control lost its `app-field` wrapper (aria-invalid /
  aria-describedby still present). No expected changes — this is a verification pass with a
  spec assertion, not edits.

## 3. Notes on the fieldset approach

- Add a shared, minimal fieldset reset so `<fieldset>`/`<legend>` don't introduce the
  browser's default border/inset — the add-rows must look identical to today.
- Where a visible legend would clutter a dense inline toolbar, use a visually-hidden legend
  (`.rq-visually-hidden`, screen-reader-only) so the group still has an accessible name.
  Decide per surface in review; default to visible legends for the admin forms
  (global-tags, tag-categories) and visually-hidden for the inline tag-selector add-row.
- Keep the inputs' `aria-label`s — with a fieldset the group is named by the legend and each
  field by its aria-label, which is the accessible pattern the review asks for.

## 4. Step-by-step (each step its own PR to `release/2.0`, squash-merged)

- **Step 1 — fieldset/legend for the two admin forms** (global-tags, tag-categories) with a
  shared visually-hidden helper class if not already present; specs updated.
- **Step 2 — fieldset/legend for the shared widgets** (tag-selector add-row,
  annotations-section add-forms); specs + a11y specs updated.
- **Step 3 — search labels** (entity-selector-dialog entity-specific label; `searchAriaLabel`
  input on list-page); specs.
- **Step 4 — audit + tests + AC** (assert app-field aria wiring on a representative editor;
  post the revised #138 AC).

Small ticket; 3+4 can be one PR. `entity-selector-dialog` and `annotations-section` are used
inside editor pages, so their a11y specs already exist to extend.

## 5. Testing

- a11y specs (axe) on tag-selector, annotations-section, global-tags, tag-categories confirm
  no violations and that each add-row is a group with an accessible name (legend).
- entity-selector-dialog spec: search input exposes "Search {entityType}s".
- Reuse the `@axe-core/playwright` smoke pattern; unit specs assert the fieldset/legend DOM
  and the search aria-label.

## 6. Revised #138 issue body / AC

Posted to the ticket: https://github.com/rreganjr/Requel/issues/138 (body updated + a
scope-narrowing comment). AC narrows to: fieldset/legend grouping for the mini-form add-rows;
entity-specific search label on the dialog + `searchAriaLabel` input on list-page; an audit
that app-field's aria wiring holds. Delivered-elsewhere items (aria-invalid/aria-describedby
via #158/#132/#173; role="alert" via #133) are called out as done, not re-done.

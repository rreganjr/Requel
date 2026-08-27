# #130 — 2.3 Dialog & relationship flows: `app-relationship-section` — Implementation Plan

Issue: https://github.com/rreganjr/Requel/issues/130
Part of the look-and-feel remediation epic (`doc/124-remediation-rollup.md`, Phase 4).

## Summary

#130 asked to make dialog and relationship flows consistent and accessible. Sibling tickets have
since closed most of it:

- **AC 3 (accessible dialogs) — done by #139.** No hand-rolled modals remain; every modal is a
  `p-dialog` with `[modal]`, `[focusOnShow]`, `closeAriaLabel`.
- **AC 2 (create-and-link only where unambiguous) — satisfied.** There are no inline create-and-link
  flows; every relationship add is "select an existing entity" via the shared selector dialog.
- **AC 5 (eliminate duplicate modal logic) — modal components already deduped.** `app-entity-selector-dialog`
  (used by all five editors) and `app-scenario-selector-dialog` are single shared components.

What remains — and what this ticket delivers — is **AC 1 + AC 4**, which also finishes the *residue*
of AC 5 (the copy-pasted scaffolding around those shared dialogs): build a shared
**`app-relationship-section`** and adopt it across the five relationship editors, giving every
add/list/remove block one structure, one Add-button and remove-icon style, focus return after
add/remove, and an aria-live status announcement.

## Scope

Convert the **nine editable relationship sections** across the five editors onto `app-relationship-section`:

| Editor | Editable relationship section(s) |
|--------|----------------------------------|
| goal-editor | "This Goal's Relations" (two-step add: select goal → pick relation type) |
| use-case-editor | Additional Scenarios, Goals, Stories, Additional Actors |
| story-editor | Goals, Additional Actors |
| stakeholder-editor | Goals |
| actor-editor | Goals |

## Locked decisions

1. **Boundary — chrome + focus/status; editor keeps the dialog and the commands.**
   The component owns the section header (title + Add button), the list wrapper + column headers, the
   per-row remove control, the empty/unsaved messaging, focus return, and the aria-live region. The
   editor supplies the row-cell template, owns which selector dialog opens, and issues the add/remove
   commands. This cleanly fits goal's two-step add and the varied row shapes.
2. **Standardize, don't parameterize, the inconsistency the issue calls out.** One Add-button style
   (`p-button label icon="pi pi-plus" size="small"`) and one remove control (icon-only
   `pi pi-trash`, `[text]` `[rounded]`, `size="small"`, with an `ariaLabel`). This removes the
   current per-editor drift (primary vs secondary/outlined Add; `pi pi-trash` vs `pi pi-times`).
3. **Focus + status live in the component.** After a successful add or remove the editor calls
   `announceAdded(name)` / `announceRemoved(name)`; each sets the polite aria-live text and returns
   focus to the Add button. Generalises the focus handling that currently exists only in goal-editor.
4. **Preserve every `data-testid`.** The component takes `addTestid` / `removeTestid` (and a `testid`
   stem) and forwards them, so existing unit/e2e selectors (`goal-add-relation`,
   `goal-remove-relation`, etc.) keep working. Row nav stays `routerLink` (stakeholder's programmatic
   `(click)` nav becomes a real link, matching #129).
5. **Reuse, don't re-solve, the list.** The row list is a plain accessible `<table>` inside the
   component (header row from a `headers` input, body rows from the projected `#row` template + the
   component's remove cell). We are NOT routing this through `app-data-table` — these lists want a
   direct remove button, not the data-table's `⋯` menu, and no search/paginator toolbar.

## Component API — `app-relationship-section`

Standalone, `selector: 'app-relationship-section'`, generic over the row type `T`.

Inputs:
- `title: string`, `showHeading = true`, `headingLevel: 2 | 3 = 2` — header (gated for wizard reuse).
- `items: T[]` — the linked rows.
- `headers: string[]` — column header labels (the remove column header is added automatically, visually hidden).
- `canAdd = false` — gates the Add button (editor passes `canEdit() && entityId != null`).
- `addLabel = 'Add'`, `addTestid?`, `removeTestid?`, `testid = 'relationship-section'`.
- `removeAriaLabel: (row: T) => string` — accessible name for each remove button.
- `trackBy: (row: T) => unknown = (r) => r` — row identity for `@for`.
- `emptyText = 'Nothing linked yet.'` — shown when addable but empty.
- `unsavedHint?` — shown instead of the list/empty when `!canAdd` because the parent is unsaved.

Content projection:
- `<ng-template #row let-item>` — the editor's data `<td>` cells (name link, type, …). The component
  renders the row `<tr>`, outlets this, then appends the remove `<td>`.

Outputs:
- `(add)` — Add button clicked (editor opens its selector dialog).
- `(remove)` — emits the row to delete (editor issues the Remove command).

Public methods (called by the editor after a successful command):
- `announceAdded(name: string)` / `announceRemoved(name: string)` — set aria-live text + focus the Add button.

## Step-by-step

1. Build `requel-angular/src/app/shared/app-relationship-section.ts` per the API above (GPL header,
   OnPush, standalone; imports ButtonModule + `RouterLink` is on the editor side via the projected
   template, so the component itself needs only ButtonModule + NgTemplateOutlet).
2. Adopt in **goal-editor** first (the two-step case) to validate the API; wire `(add)` → existing
   `showRelationSelector`, keep the relation-type `p-dialog`, call `announceAdded/Removed` after
   `EditGoalRelation` / `DeleteGoalRelation`. Remove the old `.section-header`/table markup and the
   now-redundant `@ViewChild('addRelationBtn')` focus code (the component owns it).
3. Adopt in **use-case** (4 sections), **story** (2), **stakeholder** (1), **actor** (1). Each: pass
   `items`, `headers`, `removeAriaLabel`, testids; move the row cells into a `#row` template; wire
   `(add)`/`(remove)` to the existing handlers; call the announce methods on success.
4. Leave untouched (out of scope): read-only "Referenced By" lists (goal, actor), the "Related To
   This Goal" incoming list, use-case's "Primary Scenario" single-value card, and scenario-editor's
   "Steps" FormArray. Empty-state text stays plain (no `app-empty-state` migration this ticket).

## Test plan

- **New `app-relationship-section.spec.ts`:** header/title render + heading gating; Add button shown
  only when `canAdd`, emits `(add)`, carries `addTestid`; projected row cells render; remove button
  per row emits `(remove)` with the row and exposes `removeAriaLabel`; empty vs unsaved messaging;
  `announceAdded/Removed` set the aria-live text and focus the Add button.
- **New `app-relationship-section.a11y.spec.ts`:** Add/remove buttons have accessible names; the
  live region is present and polite (mirrors the existing `shared/*.a11y.spec.ts` pattern).
- **Editor specs:** update the five editors' existing specs where they assert relationship markup;
  keep `data-testid`s stable so assertions and e2e keep matching. Add one "announce on add/remove"
  assertion in goal + one other editor.
- **Gate:** `tsc -p tsconfig.app.json` + `tsconfig.spec.json`; `ng test` for the new component + the
  five editor specs; `ng build --configuration development`. No `modules/**` changes → no `mvn`.
- **e2e (CI):** relationship add/remove flows — behavior-preserving; testids stable → existing specs
  stay green. Row nav becomes a real link in stakeholder (was programmatic) — additive.

## Out of scope

Read-only Referenced By / incoming lists; use-case Primary Scenario card; scenario Steps FormArray;
`app-empty-state` migration; the shared selector dialogs (already deduped); dialog accessibility
(#139, done).

## Risks

- **Row-template + component-owned remove cell in one `<tr>`** — verified approach: component renders
  `<tr>`, `ngTemplateOutlet` the editor cells, then its own `<td>` remove. Keep `<td>` counts aligned
  with `headers` + 1.
- **Testid drift breaking e2e** — mitigated by forwarding `addTestid`/`removeTestid` verbatim.
- **Goal two-step focus** — the relation-type dialog's `onHide` currently restores focus; after the
  refactor the component's `announceAdded` (called on confirm) owns focus return; ensure the cancel
  path also returns focus (editor calls a focus method or the component exposes `focusAdd()`).
- **Wizard vs edit reuse** — the `showHeading` input replaces the current `heading` template flag;
  verify both the wizard step (`showHeading=false`) and edit view (`showHeading=true`) render right.

## AC mapping

| AC | Status |
|----|--------|
| 1 — consistent add/list/remove in ≥2 editors | **This ticket** — all five relationship editors onto `app-relationship-section` |
| 2 — create-and-link only where unambiguous | Verified satisfied (no inline create-link flows) |
| 3 — accessible dialog primitive everywhere | Verified done (#139) |
| 4 — add/remove keep focus + status messaging | **This ticket** — component owns focus return + aria-live announce |
| 5 — eliminate duplicate modal logic | Selector dialogs already shared; **this ticket** removes the residual per-editor section/remove scaffolding |

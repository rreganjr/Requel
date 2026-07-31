# Issue #139 — Custom dialogs and overlays miss modal accessibility guarantees

Source: `doc/UI_UX_REVIEW.md` Finding 4.5. Priority: High. Effort: Medium (2–4 days).
WCAG: 2.1.2 No Keyboard Trap, 2.4.3 Focus Order, 2.4.7 Focus Visible, 4.1.2 Name/Role/Value.

## Summary

Two handcrafted fixed-overlay "dialogs" (goal relation-type picker, scenario step-detail editor)
render as plain `<div>`s with no dialog semantics, no focus trap, no Escape handling, and no
focus restoration. They will be replaced with PrimeNG `p-dialog [modal]="true"`, which on
PrimeNG 21 supplies `role="dialog"`, `aria-modal`, focus trap, `closeOnEscape`, and
focus-restore-to-opener by default. The three existing `p-dialog` usages
(entity selector, scenario selector, PAT creation) are already largely compliant and get an
audit/hardening pass. Verification is done two ways: per-dialog behavior specs (role, aria-modal,
Escape, focus restore) and an axe-core static scan wired into the Vitest suite.

Scope was confirmed with the issue owner: full audit of all five dialogs; both hand-written
behavior specs and axe-core automated checks; the ConfirmDialog recommendation in the finding is
general guidance, not a required change for this ticket.

## Current state (audit)

### Custom overlays — must be converted

**Goal relation-type dialog** — `features/goals/goal-editor.ts:178`–`190`.
`.relation-type-dialog` fixed overlay gated by `@if (pendingRelationGoal())`. Closes only via
overlay click or the Cancel button. No `role`, no `aria-modal`, no focus trap, no Escape, no focus
restore. **Opened programmatically** from `onRelationGoalSelected()` (`goal-editor.ts:423`) after
the entity selector closes — so the element focused at open time is inside the closing selector,
not a stable opener. Focus restore must be set explicitly to the "Add Relation" button
(`goal-editor.ts:97`).

**Scenario step-detail dialog** — `features/scenarios/scenario-editor.ts:203`–`224`.
`.edit-popup-overlay` fixed overlay gated by `@if (editingStep())`. Same missing guarantees.
Opened from a per-step edit button via `openStepEdit(step)` (`scenario-editor.ts:172`), a direct
click, so PrimeNG's default restore-to-opener will work once converted. Note the SSE-reload guard
at `scenario-editor.ts:410` keys off `editingStep() !== null`; keep that signal as the source of
truth for "dialog open" so the guard still holds after conversion.

### Existing p-dialogs — audit / harden only

**Entity selector** — `shared/entity-selector-dialog.ts:53`. Uses `[modal]="true"`,
`[header]="'Select ' + entityType"`, `appendTo="body"`, `(onHide)="closed.emit()"`. Compliant.
Add an explicit `ariaLabelledBy`/`role` only if the axe scan flags it. Minor pre-existing smell:
`[(visible)]="visible"` two-way-binds onto an `@Input()` (child writes a parent-owned input) —
not an a11y defect; leave as-is unless we choose to clean it up separately.

**Scenario selector** — `shared/scenario-selector-dialog.ts:51`. Same shape and same notes as the
entity selector. Compliant.

**PAT creation** — `features/users/api-tokens.ts:91`. `[modal]="true"`, static header,
label/`for` associations on fields, `[(visible)]` bound to a local signal (correct). Compliant.

## Implementation

1. **Goal relation-type dialog → `p-dialog`.** Replace the `.relation-type-dialog` markup with
   `<p-dialog [(visible)]="…" [modal]="true" [focusOnShow]="true" header='Relation to "…"'>`.
   Drive visibility from the existing `pendingRelationGoal` signal (e.g. a `relationDialogVisible`
   computed, or bind visibility to `pendingRelationGoal() !== null` and clear it on hide). On hide,
   call `.focus()` on the "Add Relation" button (template ref) to guarantee focus return, since the
   opener is not the last-focused element. Remove the `.relation-type-dialog`/`.dialog-overlay`/
   `.dialog-content` styles.

2. **Scenario step-detail dialog → `p-dialog`.** Replace `.edit-popup-overlay` with
   `<p-dialog [(visible)]="…" [modal]="true" [focusOnShow]="true" header="Step Details">`, driven
   by the `editingStep` signal. Preserve `closeStepEdit()` semantics (called on hide). Keep
   `editingStep()` as the open-state signal the SSE guard reads. Remove the overlay styles.

3. **Audit pass on the three existing dialogs.** Confirm each renders with `role="dialog"` +
   `aria-modal="true"`, has an accessible name (header), traps focus, closes on Escape, and
   restores focus. Add `ariaLabelledBy` or explicit labels only where the axe scan or manual check
   flags a gap.

4. **Escape / outside-click parity.** `p-dialog` defaults (`closable`, `closeOnEscape`,
   `dismissableMask` when `[modal]`) give keyboard users Escape-to-close equivalent to the old
   outside-click. Set `[dismissableMask]="true"` if we want to preserve click-outside-to-close.

## Verification

Manual gate per `CLAUDE.md`: `mvn clean verify` green, and (frontend changed)
`cd requel-angular && ng test --watch=false` green.

### (a) Behavior specs (Vitest + TestBed, matching existing `*.spec.ts` pattern)

For each of the five dialogs, add/extend specs asserting:

- the rendered dialog container has `role="dialog"` and `aria-modal="true"`;
- it has an accessible name (header text / `aria-labelledby`);
- Escape (or the close control) hides it;
- on close, focus returns to the opener — for the goal relation-type dialog, assert
  `document.activeElement` is the "Add Relation" button.

All dialogs use `appendTo="body"` (the correct pattern — keeps the overlay clear of ancestor
`overflow`/`z-index` traps; keep it as-is everywhere). The only consequence is a test convention:
query the dialog from the document, not `fixture.nativeElement`. Add one shared helper —
`getOpenDialog()` returning `document.querySelector('[role="dialog"]')` — and route every dialog
spec and the axe scan through it. Tear the fixture down in each spec (`fixture.destroy()`) and
assert the dialog node is removed, so a body-appended overlay can't leak and make the next test's
`[role="dialog"]` query pass falsely. Reuse the existing setup idiom (`provideNoopAnimations()`,
`vi.fn()` service mocks, `SimpleChange` for `ngOnChanges`).

### (b) axe-core static scan

- Add `axe-core` as a dev dependency; add a small `expectNoAxeViolations(el)` helper (or
  `vitest-axe`) under the test setup.
- Run against the dialog's rendered container in `document.body` (not the component subtree,
  because of `appendTo="body"`).
- Disable the `color-contrast` rule for these unit runs — jsdom has no layout/`getComputedStyle`
  box model, so contrast results are unreliable; contrast is governed separately under Finding 4.7.
- Assert zero violations for each dialog in its open state.

## Risks / open items

- jsdom cannot evaluate focus-trap cycling or true tab order the way a browser can; behavior specs
  will assert focus-restore and initial-focus, but full keyboard-trap verification should be spot
  checked manually (or deferred to e2e).
- `appendTo="body"` is intentional and stays; the only implication is the test convention above
  (query the document via the shared helper, destroy the fixture between specs to avoid overlay
  leakage). Not an app-code change.
- PrimeNG 21 focus-restore relies on the last-focused element at open; the goal relation-type
  dialog needs the explicit `.focus()` fallback described above.

## Workflow (per CLAUDE.md — actions taken only when explicitly told)

Branch `139-modal-a11y-dialogs` from `release/2.0`; implement; run the verify gate; write
`commit.md` with `Closes #139`; commit/push and open a PR against `release/2.0` when instructed;
close the issue manually after squash-merge.

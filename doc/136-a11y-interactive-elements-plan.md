# Issue #136 — Interactive elements: keyboard, roles, focus (folds in Finding 4.3)

Source: `doc/UI_UX_REVIEW.md` Findings 4.2 and 4.3.
Issue: https://github.com/rreganjr/Requel/issues/136 (Finding 4.2, High, 3–5 days).
Folded in: Finding 4.3 (icon-only button accessible names, High, 1–2 days) — same files/lines, done in one pass to avoid double edits and merge conflicts.

WCAG addressed: 2.1.1 Keyboard, 2.1.3 Keyboard (No Exception), 2.4.7 Focus Visible, 4.1.2 Name/Role/Value, 2.5.3 Label in Name.

## Summary

Make every navigation and action affordance a real, keyboard-operable element with an accessible name and a visible keyboard focus ring:

1. Convert click-only `<a class="entity-link">` anchors to real `[routerLink]` anchors.
2. Convert clickable `<div class="add-step-row">` controls to real `<button type="button">`.
3. Normalize the tag-remove control and mark its glyph decorative.
4. Add a global `:focus-visible` outline (Aura-matched) so custom controls show a keyboard focus ring.
5. (4.3) Add `ariaLabel` to every icon-only `p-button`, with row context where possible; mark decorative icons `aria-hidden`.

All navigation targets are already clean routes of the form `['/projects', projectName, <segment>, id]`, so the anchor conversions carry no behavior change.

## Focus-visible spec (locked)

Add one global rule to `requel-angular/src/styles.scss`. Rationale: the app has no existing focus styling, and PrimeNG's Aura preset applies its own focus ring via **`outline`** (primitive `focusRing: { width: 1px, style: solid, color: {primary.color}, offset: 2px, shadow: none }`). Using `outline` for our custom controls therefore matches native PrimeNG controls exactly — no box-shadow mismatch.

```scss
:focus-visible {
  outline: var(--p-focus-ring-width, 1px) var(--p-focus-ring-style, solid)
           var(--p-focus-ring-color, var(--p-primary-color));
  outline-offset: var(--p-focus-ring-offset, 2px);
}
```

- `:focus-visible` (not `:focus`) → ring on keyboard focus only, hidden on mouse click.
- Aura tokens with literal fallbacks that mirror the Aura primitive → tracks theme changes, matches PrimeNG.
- Global scope is intentional so future interactive elements are covered automatically; PrimeNG components keep their own ring (this only complements them).

## Change inventory

### 4.2a — Anchor → `[routerLink]`

Each `<a class="entity-link" (click)="navigateX(...)">` becomes `<a class="entity-link" [routerLink]="['/projects', projectName, '<segment>', id]">`. The `(click)` handler and its `navigateX` method are removed once no longer referenced. `RouterLink` must be added to each component's standalone `imports` (none import it today).

| File | Lines (current) | Route segment |
|------|-----------------|---------------|
| `features/goals/goal-editor.ts` | 112, 138 | `goals` |
| `features/stories/story-editor.ts` | 138, 172 | `goals`, `actors` |
| `features/actors/actor-editor.ts` | 117, 157, 170 | `goals`, `use-cases`, `stories` |
| `features/use-cases/use-case-editor.ts` | 136, 184, 244, 282, 321 | `scenarios`, `goals`, `stories`, `actors` |
| `features/scenarios/scenario-editor.ts` | 152 | `scenarios` |
| `features/open-issues/open-issues.ts` | 91 | see special case below |

**Special case — `open-issues.ts`.** `navigateTo` maps `entityType → segment` via `ENTITY_ROUTES` and no-ops when unmapped. This is not a literal `[routerLink]`. Replace with an anchor bound to a computed route, e.g. a `routeFor(issue)` helper returning `['/projects', projectName, ENTITY_ROUTES[issue.entityType], issue.entityId]` or `null`; render a plain `[routerLink]` anchor when a route exists and non-link text when it does not (preserves the current "unmapped = not clickable" behavior).

### 4.2b — Clickable `div` → `<button>`

`features/scenarios/scenario-editor.ts` lines 135 (`scenario-add-step-top`) and 188 (`scenario-add-step-bottom`):

```html
<button type="button" class="add-step-row" data-testid="scenario-add-step-top" (click)="addStepAt(0)">
  <i class="pi pi-plus" aria-hidden="true"></i>
  <span>Add step</span>
</button>
```

Update the `.add-step-row` styles (lines ~274–282) so the `<button>` matches the previous full-width look: reset `button` UA styling (`border`, `background`, `width: 100%`, `text-align: left`, `font: inherit`, `cursor: pointer`), keep the existing hover rule. Keep the `data-testid`s so specs still resolve. (Do not remove the drag-and-drop wiring on the step rows.)

### 4.2c — Tag remove control

`shared/tag-selector.ts` (lines ~50–52 markup, ~94–95 `.chip-x` styles): already a `<button type="button">` with `aria-label="Remove tag"`. Make the `×` glyph decorative and give clearer context:

```html
<button type="button" class="chip-x" data-testid="tag-remove"
        [attr.aria-label]="'Remove tag ' + label(t)" (click)="removeTag(t)">
  <span aria-hidden="true">×</span>
</button>
```

Focus ring is covered by the global `:focus-visible` rule (the `.chip-x` reset currently strips any default outline).

### 4.3 — Accessible names on icon-only `p-button`s

Add `[ariaLabel]` (with row context where a name is in scope) to each icon-only button. Tooltips are not a substitute. Mark purely decorative standalone icons `aria-hidden="true"`.

| File | Lines (current) | Note |
|------|-----------------|------|
| `shared/annotations-section.ts` | 95, 118, 147, 163 | trash/remove buttons |
| `features/stories/story-editor.ts` | 140, 174 | remove relation |
| `features/use-cases/use-case-editor.ts` | 189, 249, 288, 326 | remove relation (also has tooltips) |
| `features/actors/actor-editor.ts` | 122 | remove relation |
| `features/admin/global-tags.ts` | 74 | delete row |
| `features/admin/tag-categories.ts` | 82 | delete row |
| `features/scenarios/scenario-editor.ts` | 155, 169, 173, 177 | remove/edit/add-below/remove step |

Example: `[ariaLabel]="'Remove goal ' + goal.name"`.

## Testing

Add/extend the `.spec.ts` for each touched component, using existing `data-testid` hooks:

- Entity links: assert the element is an `<a>` exposing the expected `routerLink` (via `RouterLinkWithHref` / `By.directive(RouterLink)` or the rendered `href`), and that the old click-navigation method is gone.
- Add-step controls: assert the `scenario-add-step-*` elements are `<button>` and invoke `addStepAt(0)` / `addStep()` on click.
- Tag remove: assert `tag-remove` is a `<button>` with a contextful `aria-label`.
- Icon-only buttons (4.3): assert each target `p-button` renders a non-empty `aria-label` on its host button.
- `open-issues`: assert a mapped entity type renders a `routerLink` anchor and an unmapped type renders non-link text.

Keep `RouterTestingModule` / `provideRouter([])` in the relevant specs so `routerLink` resolves.

## Verification / acceptance criteria

- `cd requel-angular && ng test --watch=false` is green (includes the new assertions).
- `mvn clean verify` is green.
- Manual keyboard pass: every entity link, add-step button, tag-remove, and icon-only action is reachable by Tab, activates with Enter/Space, and shows a visible focus outline; screen reader announces a meaningful name for each (no bare "button").
- No behavior regressions in navigation or drag-and-drop step reordering.

## Workflow (per CLAUDE.md — actions only when told)

1. Branch from `release/2.0`: `136-a11y-interactive-elements`.
2. Implement 4.2 + 4.3 + focus rule; run `ng test` and `mvn verify`.
3. Write `commit.md` with `Closes #136` (and reference Finding 4.3 / its issue if one exists).
4. Commit + push on the ticket branch; open PR `--base release/2.0`.
5. After squash-merge, close #136 (and the 4.3 issue) explicitly — `release/2.0` is not the default branch, so auto-close will not fire.

# Implementation Plan — #156 N3 Card / content-surface primitive (`app-card`)

Part of the look-and-feel adoption epic #124 (see `doc/124-lookandfeel-plan.md`, item N3).
Extract the repeated card container into a single shared `app-card` primitive driven by
`--rq-card-*` tokens, and sweep every content surface onto it so there is no per-view
card CSS left.

## Decisions (locked)

1. **Tokens alias/map existing scale.** `--rq-card-*` are defined in `styles.scss` but
   resolve to the existing scale rather than new literals — `--rq-card-radius` aliases
   `--rq-radius-md` (6px), `--rq-card-pad` maps to a `--rq-space-*` value. One source of
   truth; no parallel magic numbers.
2. **Component API mirrors `list-page`.** `@Input() title` (string) + a projected
   `[actions]` slot + default `<ng-content />` for the body. Consistent with the existing
   shared primitives.
3. **Full sweep.** Every content surface migrates in this ticket — no "at least one"
   minimum, no deferred follow-ups.
4. **One card, never nested.** A surface is a single `app-card`. Inner bordered blocks
   (e.g. `annotations-section`'s `.annotation` / `.add-form` rows) stay plain bordered
   elements inside the one card — they do not become nested `app-card`s.
5. **Option A — `list-page` composes `app-card`.** `app-card` wraps `list-page`'s default
   content slot, so all 11 list pages get the card surface from a single edit. The
   title/actions/search toolbar stay *above* the card (card hugs the table/content
   region), matching the plan's data-table pattern. Editors, which don't use `list-page`,
   wrap their form shell in `<app-card>` directly.

## 1. The token set (`src/styles.scss`)

Add a card-surface block to the existing `:root` `--rq-*` declarations:

```scss
/* Card / content surface (issue #156). Values alias the shared scale so the
   card has no independent magic numbers. */
--rq-card-bg:     var(--p-surface-0);      /* white card */
--rq-card-border: var(--p-surface-200);    /* hairline border / divider */
--rq-card-radius: var(--rq-radius-md);     /* 6px */
--rq-card-pad:    var(--rq-space-4);       /* 1rem */
--rq-card-shadow: 0 1px 2px rgba(15, 23, 42, 0.06),
                  0 1px 3px rgba(15, 23, 42, 0.04); /* very soft */
```

Notes:
- `--rq-card-bg`/`--rq-card-border` point at PrimeNG surface tokens (already the Slate
  ramp from the #125 preset), so light/future-dark theming flows through automatically.
- Shadow is the one card-specific literal (there is no shadow token yet). Keep it as the
  single definition here; if a shadow scale is introduced later, `--rq-card-shadow`
  re-aliases to it.

## 2. The component (`src/app/shared/app-card.ts`)

New standalone component, following `page-header`/`list-page` structure and license header.

```ts
@Component({
  selector: 'app-card',
  standalone: true,
  template: `
    <section class="app-card" [class.has-header]="title || hasActions">
      @if (title || hasActions) {
        <header class="app-card-header">
          @if (title) { <h2 class="app-card-title">{{ title }}</h2> }
          <div class="app-card-actions"><ng-content select="[actions]" /></div>
        </header>
      }
      <div class="app-card-body"><ng-content /></div>
    </section>
  `,
  styles: [`
    .app-card {
      background: var(--rq-card-bg);
      border: 1px solid var(--rq-card-border);
      border-radius: var(--rq-card-radius);
      box-shadow: var(--rq-card-shadow);
      padding: var(--rq-card-pad);
    }
    .app-card-header {
      display: flex; justify-content: space-between; align-items: center;
      gap: var(--rq-space-4); margin-bottom: var(--rq-space-4);
    }
    .app-card-title {
      margin: 0;
      font-size: var(--rq-text-section-title-size);
      font-weight: var(--rq-text-section-title-weight);
      line-height: var(--rq-text-section-title-line);
      color: var(--rq-text-heading-color);
    }
    .app-card-actions { display: flex; align-items: center; gap: var(--rq-space-2); }
  `]
})
export class AppCardComponent {
  @Input() title = '';
  // Header renders when a title is set; card title uses the section-title type token.
}
```

Details:
- Title is a **section-level** heading (`<h2>`, section-title type token) — the page title
  stays owned by `page-header`, so headings don't collide.
- The header row only renders when a `title` is supplied (most editor/list cards will pass
  no title and rely on `page-header` above the card). `[actions]` projection is available
  for cards that want inline actions in the card header itself.
- Detecting projected `[actions]` content to toggle `has-header` when there's no title is
  optional polish; simplest correct behavior is: header renders iff `title` is set.
  (If we want actions-without-title, use a `@ContentChild`/`@ViewChild` check — decide at
  build time; not required for the sweep since editors/lists don't use card-header actions.)

## 3. Option A wiring — `list-page` composes `app-card`

`src/app/shared/list-page.ts`: import `AppCardComponent`, and wrap the trailing default
slot:

```html
<!-- was: <ng-content /> -->
<app-card><ng-content /></app-card>
```

Result: all 11 list pages (`goal-list`, `project-list`, `term-list`, `open-issues`,
`stakeholder-list`, `use-case-list`, `story-list`, `user-list`, `actor-list`,
`scenario-list`, `report-list`) render their table inside the card with zero per-page
edits. Header + search toolbar remain above the card.

## 4. Editor sweep (11 editors)

Each editor is a `<div class="*-editor" data-testid="*-editor">` shell. Wrap the form
region in `<app-card>` and delete the now-redundant local wrapper CSS (background,
border, radius, shadow, padding), keeping the `data-testid` on the outer element so tests
still find it.

Editors: `goal-editor`, `project-editor`, `term-editor`, `stakeholder-editor`,
`use-case-editor`, `story-editor`, `user-editor`, `actor-editor`, `scenario-editor`,
`report-editor`.

Per editor:
- Replace the hand-rolled card `<div class="*-editor">` styling with `<app-card>`, or keep
  the outer `div` (for `data-testid`) and place `<app-card>` immediately inside it.
- Leave `.form-grid` / `.form-actions` layout CSS alone — that's field layout (#132),
  not card surface.
- `scenario-editor`: the `.step-list` inner block (`border + radius`) stays as a plain
  bordered inner element — it is content inside the one card, not a nested card (decision 4).

## 5. Other content surfaces

- **`shared/annotations-section.ts`** → wrap the section in one `<app-card>`; the inner
  `.annotation` / `.add-form` rows keep their own borders as list items (decision 4). Do
  **not** turn those rows into cards.
- **`features/auth/login.ts`** → the centered auth card (`border-radius: 8px`,
  `box-shadow`) becomes `<app-card>` for token consistency. Note it currently uses an 8px
  radius; adopting `--rq-card-radius` moves it to 6px — acceptable and on-spec.

Explicitly **out of scope** (not content-surface cards):
- `tag-selector` / `goal-list .tag-chip` chips → belong to N2 (`app-tag`/`app-chip`).
- `sidebar-nav`, `auth/layout` → app chrome (N1), not cards.
- `api-tokens` `.token-display code` → an inline code affordance, not a card.

## 6. Tests

- **New:** `app-card.spec.ts` — renders projected content; shows title when set and hides
  header when not; projects `[actions]`; surface styles read from `--rq-card-*` (assert
  class presence / computed usage rather than literal values).
- **Regression:** run the existing editor + list specs (incl. `*.a11y.spec.ts`). The main
  risk is DOM-structure assertions and `data-testid` lookups shifting when the wrapper
  changes — fix any spec that queried the old wrapper class, preserving `data-testid`s.
- `annotations-section.spec.ts` and `goal-editor.a11y.spec.ts` are the most likely to need
  touch-ups (heading structure: ensure the new card `<h2>` doesn't break landmark/heading
  order checks from #135).

## 7. Verification gate (per CLAUDE.md)

- Frontend only (no backend change): `cd requel-angular && npm test -- --watch=false` green.
- Manual smoke: one list page (Goals) and one editor (Goal) render inside the card;
  hairline border + soft shadow + 6px radius visible; no double border/shadow anywhere
  (catch accidental nested cards); annotations section is a single card.
- Grep guard: no remaining component-local card CSS —
  `grep -rnE "box-shadow|border-radius: (6|8)px" src/app/features src/app/shared` should
  only surface chips/nav/inner-row cases listed in §5, not content-surface wrappers.

## 8. Acceptance mapping (issue #156)

- `app-card` supports title + action/content slots, padding, border, radius, shadow
  tokens → §2.
- At least one list and one editor surface use `app-card` → §3/§4 (all of them).
- Radius/shadow/border/background/padding come from tokens → §1.
- No nested-card pattern introduced → decision 4, §4 (scenario step-list), §5
  (annotations rows).
- No per-view card CSS duplication → §3 (single `list-page` edit) + §4 (editor CSS
  deleted) + §7 grep guard.

## 9. Workflow

Branch `156-app-card` off `release/2.0` when 141 clears CI. Frontend-only change →
`npm test -- --watch=false` is the gate. `commit.md` closes #156, PR targets
`release/2.0`, close the issue manually after squash-merge (release/2.0 is not the default
branch). No git actions until explicitly told.

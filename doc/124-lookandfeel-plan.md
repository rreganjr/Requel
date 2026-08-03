# Look-and-Feel Adoption — UI Epic #124

Proposed additions and updates to the sub-issues of
[#124 — Angular UI/UX, Accessibility, and Front-End Architecture Remediation](https://github.com/rreganjr/Requel/issues/124).

This is a planning doc only. It does not change any GitHub state. Apply it with the
`gh` commands / helper script in §6–§7 when ready. All new/updated sub-issues keep the
epic's conventions: parent = #124, milestone `v2.0`, project **Requel 2.0**, labels
`ui-ux-review` + `priority:{high,medium,low}`.

## Summary

This plan pins Requel's target visual language and the concrete component patterns that
implement it, and maps them onto the existing UI epic. It fits cleanly:
`requel-angular` already runs PrimeNG 21 with `@primeuix/themes`, so the target look is
achievable through a custom `definePreset` plus a small set of shared layout primitives
— no new UI framework.

The existing epic already covers *that we need* a design system, shared primitives,
reactive forms, and consistent list/detail patterns (findings 1.1–5.6). What it does
not yet capture is *the concrete visual target*. This plan fixes that by (a) updating a
handful of existing sub-issues with specific tokens and acceptance criteria, and
(b) adding component-level sub-issues for patterns the epic does not yet spell out
(app-shell chrome, tag/chip system, card surface, data-table pattern, multi-step form
wizard).

## 1. Target design language

### 1.1 Design tokens

| Token | Target value | Note |
|---|---|---|
| Font family | `Figtree, sans-serif` | clean geometric sans; loaded from Google Fonts or bundled |
| Base font size | `14px` | denser than PrimeNG default 16px |
| Primary color | `#3b82f6` (blue-500) | hover/active `#2563eb` (blue-600) |
| Content border radius | `6px` | applied to inputs, buttons, chips, cards |
| Surface palette | cool, blue-tinted neutral — 12 locked stops (see §1.1.1) | surface-0 = white cards, canvas at surface-50/100 |
| Text color | slate-blue (`~#61759d`), muted lighter | not pure gray; ≈ surface-500 |
| Content background | white cards on a light blue-gray canvas | |

#### 1.1.1 Surface ramp (locked)

The surface ramp is specified as explicit values, not a "mix toward white" rule — a
mix is not reproducible (ratios, color space, and step curve all change the output) and
`definePreset` requires concrete stops anyway. `#1e3a8a` stays the primary/accent hue;
it is **not** the surface neutral (undiluted it reads vivid blue, not "blue-tinted
neutral"). We adopt Tailwind **Slate** — a validated, accessible cool blue-gray ramp
that is also one of Aura's built-in surface options — as the locked palette:

| Stop | Hex | Role |
|---|---|---|
| surface-0 | `#ffffff` | white cards |
| surface-50 | `#f8fafc` | light blue-gray canvas |
| surface-100 | `#f1f5f9` | canvas / subtle fills |
| surface-200 | `#e2e8f0` | card hairline borders, dividers |
| surface-300 | `#cbd5e1` | disabled borders |
| surface-400 | `#94a3b8` | placeholder / icon muted |
| surface-500 | `#64748b` | muted text (≈ target `#61759d`) |
| surface-600 | `#475569` | secondary text |
| surface-700 | `#334155` | body text |
| surface-800 | `#1e293b` | headings |
| surface-900 | `#0f172a` | strong text |
| surface-950 | `#020617` | dark-mode base (reserved) |

Generation method: Tailwind Slate, adopted verbatim. If a bluer ramp is wanted later,
regenerate deterministically (PrimeNG `palette('#1e3a8a')` or an OKLCH lightness sweep),
paste the 12 resulting literals here, and record the generator + input hex — never leave
it as a runtime mix.

### 1.2 Layout & component patterns

- **App shell.** Fixed left sidebar with grouped, labelled sections, icon + label
  items, and collapsible groups. Top bar with a back button + breadcrumb on the left
  and search / notifications / account / sidebar-toggle on the right. Content sits on a
  light blue-gray canvas.
- **Card surfaces.** Every content block is a white rounded card with a hairline
  border and very soft shadow, generous padding, and a section title at top-left.
- **Tags (severity).** Soft-tinted background + matching colored text, in three
  variants — **default** (rounded rect), **pill** (fully rounded), **icon** (leading
  icon) — across `primary / success / info / warning / danger`. Used inline as status
  chips (e.g. Active = green, Inactive = red).
- **Chips.** Rounded pill, neutral background; variants with leading icon, avatar, or
  image; optional trailing remove (×).
- **Data table.** Card wrapper; toolbar row with the list title on the left and a
  search box + primary **New** button on the right; per-row checkbox selection;
  first cell = avatar/icon + name; secondary columns in muted text; a status **Tag**
  column; sortable column headers; a trailing `⋯` row-actions menu; a centered
  paginator with first/prev/page/next/last controls.
- **Form wizard.** Two-column card: left = vertical step nav; right = the active
  step's fields. Each field is a row: **label + helper text on the left**, **input on
  the right**, rows separated by hairline dividers. A dedicated upload control where an
  image/attachment is needed. Footer actions: subtle **Cancel** + primary
  **Save/Continue**.

## 2. Mapping to the existing epic

| Existing sub-issue | Action | Addition |
|---|---|---|
| #125 — 1.1 stock Aura, no brand layer | **Update** | Pin the preset to the target tokens (Figtree 14px, `#3b82f6`, blue-tinted surfaces, 6px radius) |
| #126 — 1.2 component-local CSS fragments consistency | **Done (PR #162)** | Descoped to the mechanical de-`ng-deep` / de-inline-style pass only; chip/badge/card **color** remediation and the severity-tint tokens were explicitly deferred to #155 (N2) and #156 (N3) |
| #127 — 1.3 typography too flat | **Update** | Adopt Figtree + a defined type scale |
| #128 — 2.1 project context hidden in sidebar | **Update** | Fold in the top-bar breadcrumb + grouped/collapsible sidebar chrome |
| #129 — 2.2 inconsistent list/detail, over-relies on row selection | **Update** | Adopt the data-table pattern (toolbar + New, checkbox select, status tag, row `⋯` menu, paginator) |
| #132 — 3.1 forms template-driven, weak validation | **Update** | Adopt the field-row layout (label+helper / input / divider) and Cancel/Save footer |
| #146 — 5.5 shared components too thin | **Update** | Name the concrete primitives (see new issues N1–N5) as the shared set to build |
| #131 — 2.4 loading/empty/failure states | leave | Empty/skeleton styling folds in as a styling detail |

The remaining sub-issues (a11y 4.x, architecture 5.1/5.3/5.4/5.6, errors 3.2/3.3) are
orthogonal to visual styling and need no change here.

## 3. Proposed updates to existing sub-issues

Append the following to each issue body (do not remove existing content). Keep each
issue's current priority label. The exact `gh issue edit` commands are in §6.

**#125 (1.1):**
> Target look-and-feel. `src/app/theme/requel-preset.ts` `definePreset` sets: primary
> `#3b82f6` (hover `#2563eb`); the locked 12-stop surface ramp (Tailwind Slate, see
> plan §1.1.1) with white cards (surface-0) on a light blue-gray canvas
> (surface-50/100); content border-radius `6px`; base font Figtree at 14px. `#1e3a8a`
> is the accent hue, not the surface neutral. No component may hard-code these — all
> read from tokens. Light mode first; leave hooks for a later dark mode.

**#126 (1.2):**
> Replace hard-coded chip/badge/header colors with the preset's semantic tokens and a
> shared card-surface token set (`--rq-card-bg`, `--rq-card-border`, `--rq-card-radius`,
> `--rq-card-shadow`, `--rq-card-pad`). No `#1a1a7e`/`#3b82f6` literals left in
> component styles.

**#127 (1.3):**
> Load Figtree; define a type scale (page title, card title, field label, helper,
> body, caption) as tokens and apply through shared primitives (bold slate titles,
> muted helper text).

**#128 (2.1):**
> Restructure the app shell: top bar shows back + breadcrumb (project → section →
> entity) on the left and search / notifications / account / sidebar-toggle on the
> right; sidebar uses grouped, labelled, collapsible sections.

**#129 (2.2):**
> Build/adopt a single data-table pattern: card wrapper; toolbar with title + search +
> primary **New** action; optional checkbox multi-select; a status **Tag** column;
> sortable headers; a trailing `⋯` row-actions menu; a centered paginator. List pages
> stop using whole-row click as the only affordance.

**#132 (3.1):**
> Reactive-forms migration targets a field-row layout: label + helper text on the left,
> control on the right, hairline dividers between rows, sticky footer with subtle
> **Cancel** + primary **Save/Continue**. Inline errors sit under the control.

**#146 (5.5):**
> Concrete shared primitives to build (tracked as new sub-issues): app-shell chrome,
> `app-tag`/`app-chip` severity system, `app-card` surface, `app-data-table`,
> `app-form-wizard` + `app-field`. These are the reusable base the repeated
> project/goal/story/actor/scenario/use-case views compose from.

## 4. Proposed new sub-issues

Each is a child of #124, milestone `v2.0`, labels `ui-ux-review` + the priority shown.
Full body text is in the helper script (see §6).

### N1 — App shell: top bar + grouped collapsible sidebar · priority:medium
Reshape `layout` + `sidebar-nav`. Top bar: left back button + breadcrumb reflecting the
route (project → section → entity); right cluster for search, notifications (future),
account menu, and a sidebar collapse toggle. Sidebar: grouped labelled sections,
icon+label items, collapsible groups, active-item highlight. Content canvas is the light
blue-gray surface token.
**Acceptance.** Breadcrumb is keyboard-navigable and reflects route params; sidebar
collapse persists; header color comes from tokens (no `#1a1a7e` literal); passes the
a11y landmark checks from #135.
*Overlaps #128 — do this as the shell-chrome half; #128 keeps the project-context/IA half.*

### N2 — Tag & Chip severity system as shared primitives · priority:medium
Add `app-tag` and `app-chip` wrappers over PrimeNG Tag/Chip: soft-tinted background +
colored text; variants default / pill / icon. Replace the ad-hoc hard-coded tag colors
in `goal-list`, `annotations-section`, and `tag-selector`.

**Tokens (owned by N2).** #126 was descoped and did **not** ship the severity-tint
tokens — N2 defines them. Add a `--rq-tag-{tone}-bg` / `--rq-tag-{tone}-fg` (and matching
`--rq-chip-*`) scale to `styles.scss`, sourced from the #125 preset ramps. The components
hold no color literals — all tint/text color reads from these tokens.

**Tones (6).** `primary | success | info | warning | danger | neutral`. `neutral` is
added beyond the original five because `Position` badges and `Neutral`-support arguments
are genuinely gray today (`surface-200`) and must stay visually distinct from the blue
`info` Note badge.

**Chips.** Leading icon **or avatar or image**, plus optional trailing remove (×). The
avatar/image variants are built now (not deferred) — they are needed for upcoming
stakeholder/user chips. Remove controls are real `<button>`s with an accessible name and
an AA target size.

**Generic tone → icon map (for the `icon` variant):**

| Tone | Tint | Icon |
|---|---|---|
| `primary` | brand blue | `pi pi-tag` |
| `success` | green | `pi pi-check-circle` |
| `info` | sky | `pi pi-info-circle` |
| `warning` | amber | `pi pi-exclamation-triangle` |
| `danger` | red | `pi pi-times-circle` |
| `neutral` | gray | `pi pi-minus-circle` |

**Domain concept → tone / icon map** (replaces the current hard-coded badges):

| Concept | Tone | Icon |
|---|---|---|
| Annotation: Note | `info` | `pi pi-comment` |
| Issue (open) | `warning` | `pi pi-exclamation-triangle` |
| Issue (resolved) | `success` | `pi pi-check-circle` |
| Position | `neutral` | `pi pi-flag` |
| Argument For / StronglyFor | `success` | `pi pi-thumbs-up` |
| Argument Against / StronglyAgainst | `danger` | `pi pi-thumbs-down` |
| Argument Neutral | `neutral` | `pi pi-minus-circle` |
| Must Resolve | `danger` | `pi pi-exclamation-circle` |
| Entity status — Active | `success` | `pi pi-check-circle` |
| Entity status — Inactive | `danger` | `pi pi-ban` |

**Acceptance.** One component renders every tone/variant from tokens; used for entity
status, annotation kind, and tag chips; avatar/image chip variants implemented; remove
controls have accessible names and AA target size; color is never the only signal (icon
or text label always present) to satisfy #141; no tag/chip color literals remain in
`goal-list`, `annotations-section`, or `tag-selector`.

### N3 — Card / content-surface primitive (`app-card`) · priority:low
Extract the repeated card container (title slot, padding, border, radius, soft shadow)
into `app-card` with tokens `--rq-card-*`. Adopt across list and editor shells.
**Acceptance.** List pages and editors render inside `app-card`; no per-view card CSS
duplication; radius/shadow/border come from tokens.

### N4 — Data-table pattern component (`app-data-table`) · priority:medium
Implement the table pattern as a reusable component over PrimeNG Table: toolbar
(title + search + primary action), optional selection column, sortable headers, a
status-tag column via `app-tag`, a `⋯` row-actions menu, and a centered paginator. Drive
the goal/story/actor/stakeholder/scenario/use-case/term list pages from it.
**Acceptance.** At least two existing list pages migrated; search + sort + paginate
work; row actions are real buttons with accessible names (#136/#137); empty state via
the standard empty component (#131).
*Concretizes #129 + #146.*

### N5 — Multi-step entity-create wizard (`app-form-wizard` + `app-field`) · priority:medium
Build the two-column wizard: left vertical step nav, right the active step's fields.
`app-field` renders the label+helper-left / control-right row with divider and inline
error slot. Footer: Cancel + primary Continue/Save; step nav shows completion state. Use
it for a representative create flow (e.g. new Goal or Story).
**Acceptance.** Built on reactive forms (#132); labels/errors associated (#138); step
nav keyboard-operable; one create flow migrated end-to-end.
*Concretizes #132 + #146.*

### N6 — (Optional) Theme switcher + dark mode via config panel · priority:low
Add a config panel (gear in the top bar) to toggle light/dark and optionally the primary
color, backed by the preset's dark token set.
**Acceptance.** Dark mode reads entirely from tokens; preference persists; contrast
passes AA in both modes (#141). Defer if out of scope for v2.0.

## 5. Sequencing

Slots into the epic's existing phases:

- **Phase 2 (design-system foundation):** #125, #126, #127 updates + N2, N3, N6 (N6 optional).
- **Phase 4 (IA & workflow polish):** #128, #129 updates + N1, N4.
- **Phase 3 (forms):** #132 update + N5.

These phase assignments are wired into `phase_of()` in `scripts/epic-rollup-comment.sh`,
so the rollup comment groups N1–N6 automatically.

N2/N3 are low-risk and unblock N4/N5, so build the preset (#125) → tags/card
(N2/N3) → table/wizard (N4/N5) → shell (N1).

## 6. Applying this plan

Everything in §3 (updates) and §4 (new sub-issues) is applied by the companion helper
`scripts/create-ui-ux-lookandfeel.sh`. Run from a clone with `gh` authenticated:

```bash
DRY_RUN=1 bash scripts/create-ui-ux-lookandfeel.sh   # preview every gh command
bash scripts/create-ui-ux-lookandfeel.sh             # apply
```

The script does two things, and the whole script is safe to rerun:

1. **Updates existing sub-issues (§3).** Appends the "Target look-and-feel" block to
   #125, #126, #127, #128, #129, #132, #146. Self-healing and idempotent — it strips
   any existing block (matched by its marker heading) and rewrites it, so a rerun
   replaces rather than duplicates and repairs any earlier bad render.
2. **Creates the new sub-issues (§4).** Creates N1–N6, each linked to #124 with
   `--parent`, labelled `ui-ux-review` + priority, on milestone `v2.0`. Guarded against
   duplicates — it reads the epic's existing sub-issue titles first and skips any that
   already exist.

**Do not rerun `scripts/create-ui-ux-epic.sh`.** It has no existence guard — every run
calls `gh issue create` unconditionally, so rerunning it would create a *duplicate*
epic and 23 duplicate children, not update #124 or append the new ones. That is why the
new work lives in a separate script.

> If your `gh` build lacks `--parent`, link each child to the epic with the GraphQL
> `addSubIssue` mutation against the epic's and child's node IDs.

## 7. Project, points, and rollup

- **Project.** #124 is already in the **Requel 2.0** project, and sub-issues are pulled
  in automatically (they render nested under their parent in the project's hierarchy —
  cosmetic, not a problem). No manual add needed. `set-points.sh` (below) also re-adds
  each item, which is a no-op if it is already present.
- **Story Points.** Set initial estimates with `scripts/set-lookandfeel-points.sh`
  (delegates to `set-points.sh`). Proposed Fibonacci values — edit before running:
  N1 = 5, N2 = 3, N3 = 2, N4 = 8, N5 = 8, N6 = 3. Retro is left unset (issues are open).

  ```bash
  bash scripts/set-lookandfeel-points.sh
  ```

- **Phase rollup comment.** `phase_of()` in `scripts/epic-rollup-comment.sh` now maps
  N1–N6 (N2/N3/N6 → Phase 2, N5 → Phase 3, N1/N4 → Phase 4). Update the existing rollup
  comment in place instead of posting a new one:

  ```bash
  DRY_RUN=1 bash scripts/epic-rollup-comment.sh                  # preview the regenerated body
  COMMENT_ID=5113771759 bash scripts/epic-rollup-comment.sh      # edit the existing comment in place
  ```

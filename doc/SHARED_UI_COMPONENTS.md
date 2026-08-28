# Shared UI Components — the Requel Angular component layer

This is the architecture and adoption reference for the shared UI layer in
`requel-angular/src/app/shared/` (plus the cross-cutting services in `core/`). It is the
component-layer companion to `doc/UI_DESIGN_GUIDE.md` — that guide covers the *visual* system
(layout, color, typography, spacing); this one covers *which components exist, when to reach for
them, and the contracts they uphold*.

It was written as the consolidation capstone of the look-and-feel epic (`doc/124-remediation-rollup.md`,
issue #146). Most of the shared layer was built incrementally by the epic's N-primitive and
remediation tickets; this document ratifies that layer so future work composes it instead of
re-deriving page headers, form grids, action rows, relationship tables, and error handling.

## 1. The layer boundary

There are three homes for front-end code. Put a thing in the lowest layer it can live in.

- **`shared/`** — reusable, **domain-agnostic** UI. A shared component knows about *presentation*
  (a table, a field, a banner, a dialog shell) and emits events; it does **not** know about goals,
  scenarios, or the command API. If a component would import a domain service or a domain DTO by
  name to *do* something (not just to type a row), it does not belong here.
- **`features/**`** — **domain-specific** screens and widgets: the editors, the list pages, and any
  piece that speaks to a specific aggregate. Features *compose* shared components and own the
  commands, the routing, and the domain wiring.
- **`core/`** — app-wide **services** and guards (auth, event stream, preferences, and the
  `AnnouncerService`). Cross-cutting behavior with no template of its own lives here.

Two consequences worth stating outright:

- **Shared components emit, hosts command.** `app-relationship-section` raises `(add)`/`(remove)`;
  the editor decides which dialog opens and issues the `AddGoalToGoalContainer` command. This keeps
  the shared layer testable in isolation and reusable across aggregates.
- **A shared component never reaches into a domain service.** The one seam is *data in, events
  out*, plus projected templates for the parts only the host knows how to render (a row's cells).

## 2. Principles the layer enforces

Every shared UI component is expected to hold to these. New ones should too (see §7).

- **Standalone + `OnPush`.** No NgModules; `changeDetection: ChangeDetectionStrategy.OnPush`. State
  arrives as inputs/signals so change detection stays cheap and predictable.
- **Theme tokens only.** Colors, spacing, and radii come from the design tokens
  (`--rq-*`, `--p-*`); no hard-coded hex. See `UI_DESIGN_GUIDE.md` §5, §8.
- **`data-testid` is forwarded, never hard-coded away.** Components take testid inputs and forward
  them to their interactive elements, so unit and e2e selectors survive a refactor. This is why the
  #129/#130 migrations could swap markup without touching e2e.
- **Accessibility is a contract, not a nicety** (see §5). Icon-only controls carry accessible names;
  dialogs are real modals; status is announced through a live region; every component ships an axe
  spec.
- **No domain logic.** Row identity, labels, and commands are supplied by the host via inputs and
  projected templates.

## 3. Component inventory

Grouped by role. Selector, one-line purpose, when to reach for it, and the epic issue/plan that
introduced it.

### Shell & layout

| Component | Selector | When to use | Origin |
|-----------|----------|-------------|--------|
| `ListPageComponent` | `app-list-page` | The frame for any list screen: title, optional search slot, projected table/content. Pair with `app-data-table`. | app shell #154/#128 |
| `PageHeaderComponent` | `app-page-header` | The title row for an editor/detail page (title + projected actions). | app shell #154/#128 |
| `EditorActionsComponent` | `app-editor-actions` | The standard Save/Cancel (and friends) action row for editors. | app shell #154/#128 |
| `BreadcrumbComponent` | `app-breadcrumb` | The top-bar breadcrumb trail resolved from the route. | app shell #154/#128 |
| `SidebarNavComponent` | `app-sidebar-nav` | The grouped, collapsible project/entity navigation tree. | app shell #154/#128 |

### Form primitives

| Component | Selector | When to use | Origin |
|-----------|----------|-------------|--------|
| `AppFieldComponent` (+ `appFieldControl` directive) | `app-field` / `[appFieldControl]` | One labelled form field with consistent label/hint/error wiring. Wrap the control; put `appFieldControl` on the input. | #158 (N5) |
| `AppFieldGroupComponent` | `app-field-group` | A two-column row layout for `app-field`s (label/control grid). | #172 (3.1a) |
| `AppWizardStepComponent` / form wizard | `app-wizard-step` | Multi-step create flows (details → tags → relations). Steps commit as the user advances. | #158 (N5) |
| `form-errors` (util) | — | Maps reactive-form validation errors to display messages; used by the field components. | #132/#133 |
| `validation-limits` (util) | — | Client-side size/format caps that mirror the backend bean-validation constraints. | #171 |

### Data display

| Component | Selector | When to use | Origin |
|-----------|----------|-------------|--------|
| `AppDataTableComponent` | `app-data-table` | Any list/table surface: sortable columns, client search, paginator, row actions, empty state, and real name links (`link` column). Used by all list pages. | #157 (N4), links #129 |
| `RelationshipSectionComponent` | `app-relationship-section` | An add/list/remove block for linked entities in an editor (header + Add + row list + remove). Owns focus return + status announce. | #130 (2.3) |
| `AppCardComponent` | `app-card` | The content-surface/card wrapper for a panel or form section. | #156 (N3) |
| `AppTagComponent` / `AppChipComponent` | `app-tag` / `app-chip` | Status tags and value chips with the shared severity system. | #155 (N2) |
| `severity` (util) | — | Maps a domain status/tone to a tag/chip severity. | #155 (N2) |

### Feedback & state

| Component | Selector | When to use | Origin |
|-----------|----------|-------------|--------|
| `EmptyStateComponent` | `app-empty-state` | The "nothing here yet" state — title, guidance, optional primary action. | #131 (2.4) |
| `LoadingStateComponent` | `app-loading-state` | The skeleton/loading state for a panel or list. | #131 (2.4) |
| `ErrorStateComponent` | `app-error-state` | The retryable "failed to load" state (replaces the content). | #131 (2.4) |
| `SubmitErrorComponent` | `app-submit-error` | The inline save/API error surface at the top of an editor, with retry. | #133 (3.2) |
| `InlineErrorComponent` | `app-inline-error` | A small inline error message (e.g. under a control or mini-form). | #133/#134 |
| `UpdateBannerComponent` | `app-update-banner` | Non-modal "a newer version is available" banner when a background update lands on unsaved edits. | #140 (4.6) |
| `AnnouncerService` (core) | — service — | Polite live-region announcer for async/SSE status (wraps CDK `LiveAnnouncer`); `announce` + coalescing `announceThrottled`. | #140 (4.6) |

### Dialogs & selectors

| Component | Selector | When to use | Origin |
|-----------|----------|-------------|--------|
| `EntitySelectorDialogComponent` | `app-entity-selector-dialog` | Pick an existing entity to link (goal/actor/story/scenario…), with type/exclude filters. Accessible `p-dialog`. | dialogs #139/#136 |
| `ScenarioSelectorDialogComponent` | `app-scenario-selector-dialog` | Pick a scenario (sub-scenario flows). Accessible `p-dialog`. | dialogs #139/#136 |

### Domain sections (shared widgets used across editors)

| Component | Selector | When to use | Origin |
|-----------|----------|-------------|--------|
| `AnnotationsSectionComponent` | `app-annotations-section` | The IBIS annotation/discussion block attached to a domain entity. | annotations layer |
| `TagSelectorComponent` | `app-tag-selector` | The tag add/remove control for an entity. | tag work |
| `FileUploadButtonComponent` | `app-file-upload-button` | A styled, accessible file-picker button. | shared |

> These "domain sections" sit at the edge of the boundary: they are reused across features but
> lean on domain services. Keep new ones here only when they are genuinely cross-feature; a widget
> used by a single editor belongs in that feature.

## 4. Adoption guidelines — reach for the primitive, not the hand-rolled markup

When you catch yourself writing one of the left-hand patterns, use the right-hand component instead.
As of #146 the app has **no** remaining hand-rolled instances of these — keep it that way.

| Repeated pattern | Use |
|------------------|-----|
| A page title + actions row | `app-page-header` |
| A Save/Cancel button row | `app-editor-actions` |
| A list/table screen frame | `app-list-page` |
| A data table (sort/search/paginate/row actions/name links) | `app-data-table` |
| A labelled form field | `app-field` (+ `appFieldControl`); rows via `app-field-group` |
| A multi-step "create" flow | `app-form-wizard` / `app-wizard-step` |
| An add/list/remove list of linked entities | `app-relationship-section` |
| A "pick an existing entity" modal | `app-entity-selector-dialog` / `app-scenario-selector-dialog` |
| A panel/section surface | `app-card` |
| A status tag / value chip | `app-tag` / `app-chip` |
| Empty / loading / failed-to-load states | `app-empty-state` / `app-loading-state` / `app-error-state` |
| A save/API error banner | `app-submit-error`; a field-level error → `app-inline-error` |
| Async/SSE status for assistive tech | `AnnouncerService`; a "newer version" prompt → `app-update-banner` |

## 5. Accessibility contract (AC3)

Accessibility is enforced, not assumed:

- **Every shared UI component ships a `*.a11y.spec.ts`** that renders the component and asserts **no
  axe-core violations**, using the shared helper `shared/testing/a11y.ts`
  (`expectNoAxeViolations(element)`, and `getOpenDialog()` for modal checks). This is the a11y
  baseline the CI unit suite enforces; there are 13 such specs today.
- **Icon-only controls carry an accessible name** (`ariaLabel` / visually-hidden text) — remove
  buttons, the data-table `⋯` trigger, the update-banner dismiss, etc.
- **Modals are real modals** — all dialog content uses PrimeNG `p-dialog` with `[modal]`,
  `[focusOnShow]`, and a `closeAriaLabel` (issue #139); no hand-rolled overlays remain.
- **Background status reaches assistive tech** — async/SSE updates announce through the polite live
  region behind `AnnouncerService` rather than silently mutating the DOM (issue #140,
  WCAG 4.1.3 / 2.2.2).
- **Table headers are never empty** — an actions column still renders a visually-hidden `<th>` label.

When you add or change a shared component, add or update its a11y spec in the same PR.

## 6. Worked examples (AC2)

Two canonical, fully-composed pages. Read these to see "how a screen is built now."

**`features/goals/goal-list.ts` — a list page.** Frames with `app-list-page`, renders rows with
`app-data-table` (sortable columns, a real `link` name column per #129, an empty state via
`app-empty-state`, and a `⋯` row-action menu). The component owns no table markup of its own.

**`features/goals/goal-editor.ts` — an editor.** Header via `app-page-header`; actions via
`app-editor-actions`; the form built from `app-field` inside `app-card`, with the create path driven
by `app-form-wizard`/`app-wizard-step`; the "This Goal's Relations" block is
`app-relationship-section`; save/API errors surface through `app-submit-error`;
loading/failed states use `app-loading-state`/`app-error-state`; a cross-session update raises the
`app-update-banner` and announces via `AnnouncerService` (#140). The only bespoke markup left is the
goal-specific relation-type dialog — everything structural is a shared primitive.

Every other editor and list page follows the same composition; goal is simply the reference.

## 7. Adding a new shared component

Checklist, so the layer stays coherent:

1. It is genuinely reusable and domain-agnostic (else it belongs in a feature). No new N1–N5
   primitive was needed for #146 — the bar for "new shared component" is high.
2. Standalone, `OnPush`, theme tokens only, `data-testid` inputs forwarded to interactive elements.
3. Data in via inputs; behavior out via `@Output`; host-specific rendering via projected templates.
4. A unit spec **and** a `*.a11y.spec.ts` (axe) in the same PR.
5. Add a row to the §3 inventory and, if it introduces a pattern, a line to the §4 adoption table.

## Cross-references

- `doc/UI_DESIGN_GUIDE.md` — the visual system (layout, color, typography, spacing, PrimeNG config).
- `doc/124-remediation-rollup.md` — the epic rollup and per-ticket plans.
- Per-component plans: `156-app-card-plan.md`, `157-data-table-plan.md`, `158-form-wizard-field.md`,
  `130-relationship-section-plan.md`, `140-async-sse-announcements-plan.md`, and the #131/#155/#172
  entries in the rollup.
- `doc/AUTH_ARCH.md` — the authorization model the command hosts rely on.

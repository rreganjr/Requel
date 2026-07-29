#!/usr/bin/env bash
#
# create-ui-ux-epic.sh
# Creates one Epic parent issue + 23 child issues (one per finding in
# doc/UI_UX_REVIEW.md), each linked to the epic as a real sub-issue.
#
# Requirements:
#   - gh CLI v2.94.0+ (for the native --parent flag). Check: gh --version
#   - Authenticated: gh auth status
#
# Usage:
#   bash create-ui-ux-epic.sh          # do it
#   DRY_RUN=1 bash create-ui-ux-epic.sh  # print the gh commands without running
#
set -euo pipefail

REPO="rreganjr/Requel"
DRY_RUN="${DRY_RUN:-0}"

# --- helpers ---------------------------------------------------------------

run() {
  if [[ "$DRY_RUN" == "1" ]]; then
    printf '%q ' "$@"; echo
  else
    "$@"
  fi
}

# Create an issue and echo its number (trailing digits of the printed URL).
create_issue() {  # args: title body [extra gh flags...]
  local title="$1"; local body="$2"; shift 2
  if [[ "$DRY_RUN" == "1" ]]; then
    printf 'gh issue create --repo %s --title %q --body <…> %s\n' "$REPO" "$title" "$*" >&2
    echo "NNN"   # placeholder issue number (stdout)
    return
  fi
  gh issue create --repo "$REPO" --title "$title" --body "$body" "$@" \
    | grep -oE '[0-9]+$'
}

# --- labels (idempotent) ---------------------------------------------------

run gh label create "Epic"          --repo "$REPO" --color 6f42c1 --description "Epic: parent tracking issue" 2>/dev/null || true
run gh label create "ui-ux-review"  --repo "$REPO" --color 0e8a16 --description "From doc/UI_UX_REVIEW.md" 2>/dev/null || true
run gh label create "priority:high"   --repo "$REPO" --color b60205 --description "High priority" 2>/dev/null || true
run gh label create "priority:medium" --repo "$REPO" --color fbca04 --description "Medium priority" 2>/dev/null || true
run gh label create "priority:low"    --repo "$REPO" --color c2e0c6 --description "Low priority" 2>/dev/null || true

# --- epic parent -----------------------------------------------------------

EPIC=$(create_issue \
"[Epic] Angular UI/UX, Accessibility, and Front-End Architecture Remediation" \
"$(cat <<'EOF'
Umbrella epic tracking the remediation work identified in `doc/UI_UX_REVIEW.md` — a review of the Angular 21 / PrimeNG 21 SPA (`requel-angular/src`) against the CQRS `/api/**` backend.

Scope covers five areas, broken out into child issues (one per finding):

1. Visual Design and Theming — findings 1.1–1.3
2. Layout, Flow, and Information Architecture — findings 2.1–2.4
3. Forms, Validation, and Error Messaging — findings 3.1–3.3
4. Accessibility (WCAG 2.2 AA) — findings 4.1–4.7
5. Front-End Architecture and Efficiency — findings 5.1–5.6

Highest-impact themes: establish a Requel design system (custom `definePreset` + tokens), extract shared page/editor/field primitives, migrate to reactive forms with a command-error adapter, close critical WCAG 2.2 AA gaps, make task flows project-aware, standardize loading/empty/error states, modernize Angular idioms (OnPush, `takeUntilDestroyed`), and add axe + Playwright accessibility regression tests.

A suggested phased roadmap (Phase 1 a11y blockers → Phase 5 architecture refactors) is in `doc/UI_UX_REVIEW.md`.

Child issues are linked below as sub-issues.
EOF
)" \
--label Epic --label ui-ux-review)

echo "Epic = #$EPIC"

# Create a child linked to the epic.
child() {  # args: title priority-label body
  local title="$1"; local prio="$2"; local body="$3"
  local num
  num=$(create_issue "$title" "$body" --label ui-ux-review --label "$prio" --parent "$EPIC")
  echo "  child #$num  $title"
}

# ===========================================================================
# Section 1 — Visual Design and Theming
# ===========================================================================

child "1.1 App uses stock Aura with no Requel brand layer" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 1.1.

**Priority:** High. **Effort:** Medium (3–5 days for first usable theme + token pass).

**What exists today**
- PrimeNG is configured with uncustomized Aura (`app.config.ts:25`, `app.config.ts:36`).
- Global CSS only imports PrimeIcons and applies PrimeNG font/background/text variables to `html, body` (`styles.scss:1`, `styles.scss:3`–`styles.scss:8`).
- The header is hard-coded to `#1a1a7e` and white, outside PrimeNG semantic tokens (`layout.ts:73`–`layout.ts:80`).
- Chip/badge colors are repeatedly hard-coded as token fallbacks: goal tags (`goal-list.ts:93`–`95`), annotations (`annotations-section.ts:241`–`249`), tags (`tag-selector.ts:90`–`92`).

**Problems**
- The UI inherits a generic PrimeNG look with no recognizable Requel identity.
- Color, radius, density, and typography have no single source of truth.
- Dark mode is undefined; any future dark mode inherits Aura plus conflicting hard-coded colors.
- Component-local colors make contrast/semantic consistency hard to audit.

**Recommendations**
- Add `src/app/theme/requel-preset.ts` and replace the raw Aura preset with a Requel preset via `definePreset`.
- Define semantic colors for primary actions, content, success/warn/error/info, focus rings, form fields, chips, and panels.
- Add app-level CSS design tokens in `styles.scss` for spacing, radius, type scale, layout widths; keep PrimeNG token overrides in the preset and app layout tokens in CSS variables.
- Use theme tokens in component styles only through semantic variables (`--rq-space-3`, `--rq-radius-sm`, `--rq-chip-bg`, `--rq-chip-fg`).

Full starter `RequelPreset`, `app.config.ts`, and `styles.scss` examples are in the review doc.
EOF
)"

child "1.2 Component-local CSS fights PrimeNG and fragments visual consistency" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 1.2.

**Priority:** High. **Effort:** Medium (4–7 days to extract common patterns without redesigning all pages).

**What exists today**
- `:host ::ng-deep` used for account-trigger color and tree styling (`layout.ts:107`, `sidebar-nav.ts:178`, `sidebar-nav.ts:182`, `entity-selector-dialog.ts:98`).
- Forms repeat custom grid definitions with different label widths/max widths (`goal-editor.ts:213`, `story-editor.ts:214`, `actor-editor.ts:191`, `scenario-editor.ts:246`, `use-case-editor.ts:377`, `term-editor.ts:138`, `report-editor.ts:102`, `stakeholder-editor.ts:185`).
- Inline styles for hidden file inputs and table widths (`project-list.ts:43`–`45`, `report-editor.ts:76`–`77`, `goal-editor.ts:106`, `use-case-editor.ts:148`).
- Table header markup is inconsistent: some use `ng-template #header`, others `pTemplate="header"` (`project-list.ts:62`, `use-case-list.ts:49`).

**Problems**
- The same concept looks slightly different depending on which editor is open.
- `ng-deep` and inline styles are brittle and make PrimeNG upgrades riskier.
- PrimeNG could carry much of the styling through a small shared layer; the current code duplicates CSS instead.

**Recommendations**
- Introduce shared CSS utilities and wrappers: `app-page-header`, `app-form-grid` (`labelWidth`/`density`/responsive), `app-section` (`h3` + actions + empty state), `app-entity-link` (real `a [routerLink]`), `app-chip`/`p-tag`.
- Replace `:host ::ng-deep` with PrimeNG `pt` pass-through config or `styleClass` + global classes in `styles.scss`.
- Replace hidden file inputs with a shared `app-file-upload-button` that owns labeling and focus behavior.
EOF
)"

child "1.3 Typography and hierarchy are too flat" "priority:medium" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 1.3.

**Priority:** Medium. **Effort:** Small (1–2 days for tokens + shell updates; larger to apply everywhere).

**What exists today**
- Layout header title uses `font-size: 1.25rem` and letter spacing (`layout.ts:96`–`100`).
- Most pages use a bare `h2` in a local `.page-header` with no subtitle, breadcrumbs, metadata, or consistent body max width (`list-page.ts:30`–`35`, `dashboard.ts:32`–`33`).
- Login page has a simple card with `h2` and subtitle (`login.ts:35`–`39`).

**Problems**
- Artifact editors do not convey where the user is in the requirements model.
- Dense tables and multi-section editors lack scan-friendly hierarchy beyond repeated `h3`s.
- Search/filter/action density differs across list pages.

**Recommendations**
- Standardize: page title (artifact/collection name), eyebrow/context (project name + artifact type), metadata (status, counts, permissions, unsaved state), primary action (right-aligned, consistent severity).
- Add a compact toolbar pattern for filters and list actions.
EOF
)"

# ===========================================================================
# Section 2 — Layout, Flow, and Information Architecture
# ===========================================================================

child "2.1 Navigation is complete but project context is hidden in the sidebar" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 2.1.

**Priority:** High. **Effort:** Medium (3–5 days).

**What exists today**
- Authenticated shell has a fixed header, sidebar, and main area (`layout.ts:42`–`63`).
- Project artifact routes nested under `/projects/:name/...` (`app.routes.ts:43`–`60`).
- Sidebar tree maps each project to Stakeholders, Goals, Stories, Actors, Scenarios, Use Cases, Glossary, Reports, Open Issues (`sidebar-nav.ts:361`–`379`).
- Dashboard is only a placeholder telling users to select a project (`dashboard.ts:24`–`33`).

**Problems**
- Main pages show no breadcrumbs or active project, so deep links have weak context.
- The primary model flow is split across sidebar groups, tables, and editor sub-tables without a clear project workspace.
- The dashboard does not help resume work, see recent projects, or surface open issues.

**Recommendations**
- Add a project workspace route at `/projects/:name` with a compact overview: counts, open issues, recent changes, next actions.
- Add breadcrumbs to all project-scoped pages: `Projects / {project} / Goals / {goal}`.
- Add project-aware action groups in editor headers (Back to Goals, Open Issues, Related Stories, Related Use Cases).
- Keep the sidebar as navigation, not the only IA surface.

An `app-page-shell` example (title/eyebrow/breadcrumbs/pageActions) is in the review doc.
EOF
)"

child "2.2 List/detail patterns are inconsistent and over-rely on row selection" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 2.2.

**Priority:** High. **Effort:** Medium (3–5 days).

**What exists today**
- Many lists use `p-table` with row selection to navigate (`project-list.ts:59`–`61`, `goal-list.ts:58`–`60`, `story-list.ts:50`–`52`, `stakeholder-list.ts:52`–`54`).
- Reports use explicit Edit/Run actions instead (`report-list.ts:62`–`69`).
- Use Cases and Scenarios disable search (`use-case-list.ts:36`, `scenario-list.ts:37`), while most lists use global search (`list-page.ts:36`–`43`).
- Empty states are plain table messages (`project-list.ts:86`–`88`, `goal-list.ts:83`–`85`, `use-case-list.ts:63`–`65`).

**Problems**
- Row click/select is discoverable for mouse users only after trial and error, and not consistently represented as a link.
- Some lists are searchable, others not, without visible rationale.
- Empty states do not guide users to create the first artifact or explain prerequisites.

**Recommendations**
- Use explicit link cells for names, with row hover as a secondary affordance.
- Standardize: name column is a real link; optional row-actions column; search on by default (disable only with a visible reason); empty state includes title, short guidance, and a primary action if permitted.
- Add a shared `EntityListPageComponent` over `p-table`.
EOF
)"

child "2.3 Dialog and relationship flows need clearer progression" "priority:medium" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 2.3.

**Priority:** Medium. **Effort:** Medium (3–6 days).

**What exists today**
- Relationships added via entity selector dialogs (`entity-selector-dialog.ts:53`–`93`).
- Scenario sub-scenarios use a specialized dialog that can create-and-add inline (`scenario-selector-dialog.ts:51`–`115`).
- Goal relation type is collected in a custom overlay rather than `p-dialog` (`goal-editor.ts:177`–`190`).
- Scenario step details use a custom fixed overlay (`scenario-editor.ts:201`–`224`).

**Problems**
- Some flows create new entities inline; others force users to leave the editor.
- Custom overlays miss PrimeNG dialog behavior and accessibility hooks.
- Add/remove relationship sections repeat across actors, stories, stakeholders, use cases, goals, and scenarios with inconsistent visual structure.

**Recommendations**
- Build a reusable `app-relationship-section` with `title`, `items`, `addLabel`, `emptyText`, `linkFactory`, `remove`.
- Use `p-dialog` for all modal/popup content.
- Add "Create new" support to entity selector dialogs where the next step is obvious.
- After add/remove, keep focus near the action and announce status.
EOF
)"

child "2.4 Loading, empty, and failure states are under-specified" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 2.4.

**Priority:** High. **Effort:** Small (2–3 days for common components and first pass).

**What exists today**
- Lists usually bind `[loading]` to tables (`project-list.ts:59`, `goal-list.ts:58`, `stakeholder-list.ts:52`).
- Editors often define `loading` signals but do not render skeletons or loading affordances (`project-editor.ts:125`, `user-editor.ts:150`, `scenario-editor.ts:317`).
- Supplemental loads silently fail for tags and annotations (`tag-selector.ts:171`–`172`, `annotations-section.ts:298`–`299`).

**Problems**
- Users can see blank forms or stale sections during async loading.
- Silent failures hide lost capabilities, especially tags/annotations central to requirements triage.
- Empty states lack calls to action.

**Recommendations**
- Create shared states: `app-loading-state` (skeleton/spinner + label), `app-error-state` (message, retry, support detail), `app-empty-state` (title, body, optional action).
- Use inline warnings for supplemental section failures instead of silent ignores.
EOF
)"

# ===========================================================================
# Section 3 — Forms, Validation, and Error Messaging
# ===========================================================================

child "3.1 Forms are mostly template-driven and lack consistent validation" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 3.1.

**Priority:** High. **Effort:** Large (1–2 weeks for high-risk forms, 3–4 weeks for full migration).

**What exists today**
- Login uses `FormsModule`, signals, `[(ngModel)]`, disables submit only when fields empty (`login.ts:44`–`59`).
- Project and user editors are template-driven `NgForm` forms (`project-editor.ts:66`–`93`, `user-editor.ts:52`–`128`).
- Most artifact editors use loose `div.form-grid` + `[(ngModel)]`, not real `<form>` submission (`goal-editor.ts:76`–`87`, `story-editor.ts:79`–`113`, `scenario-editor.ts:93`–`113`, `use-case-editor.ts:83`–`111`, `term-editor.ts:64`–`82`, `report-editor.ts:65`–`86`, `stakeholder-editor.ts:75`–`134`).
- Few fields have native validators; project name has `required` (`project-editor.ts:70`); email fields use `type="email"` but show no field errors (`user-editor.ts:67`, `edit-account.ts:66`).
- Required-name validation is sometimes imperative in save handlers (`term-editor.ts:277`–`280`, `report-editor.ts:199`–`202`), while many editors submit empty names to the server (`goal-editor.ts:348`–`359`, `story-editor.ts:365`–`378`, `scenario-editor.ts:564`–`578`).

**Problems**
- Users discover validation problems after save, not inline.
- Assistive tech is not told which fields are invalid.
- Disabled Save states vary (dirty-only, change-tracked, required-only, always enabled).
- Mutable class fields and signals are mixed, causing manual `trackChanges()` and timing-issue comments (`story-editor.ts:344`–`350`, `settings.ts:122`–`125`, `user-editor.ts:155`–`156`).

**Recommendations**
- Standardize on reactive forms for all editors.
- Common validation helper: required name for all named artifacts, max length per backend constraints, email format, password confirmation, at-least-one-role where required.
- Disable Save when `form.invalid || form.pristine || saving()`.
- Render field-level errors with `aria-describedby` and `aria-invalid`.
- Convert command `violations` to field errors when field names are available; otherwise a page-level alert.

Before/after code (reactive `FormGroup`, `applyCommandErrors`, `app-field` template) is in the review doc.
EOF
)"

child "3.2 API and command errors are surfaced inconsistently" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 3.2.

**Priority:** High. **Effort:** Medium (3–5 days).

**What exists today**
- A root `<p-toast />` exists in the authenticated layout (`layout.ts:41`).
- Some components set inline `p-message` errors (`project-list.ts:49`–`57`, `project-editor.ts:59`–`64`).
- Some use toasts for success or nested action errors (`goal-editor.ts:361`, `tag-selector.ts:183`, `annotations-section.ts:311`).
- `ProjectEditor`, `UserEditor`, `EditAccount` join backend violations into one semicolon-delimited string (`project-editor.ts:260`–`264`, `user-editor.ts:273`–`277`, `edit-account.ts:178`–`181`).
- `CommandService` normalizes HTTP failures only to command-level errors, not field-level (`command.service.ts:72`–`93`).

**Problems**
- Users must hunt for whether a result appears inline or as a toast.
- Toasts can disappear before screen-reader/keyboard users notice them.
- Field-specific backend violations are lost when concatenated.

**Recommendations**
- Inline messages for blocking page/form errors; toasts for non-blocking confirmations only.
- Add a command error adapter: `violations` field path → `form.controls[field].setErrors({ server: message })`; command-level error → `submitError`; unexpected network failure → retryable inline alert.
- Add `role="alert"`/`aria-live="assertive"` for blocking errors and `aria-live="polite"` for success.
EOF
)"

child "3.3 Mini-forms (annotations, tags, admin, dialogs) need the same validation contract" "priority:medium" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 3.3.

**Priority:** Medium. **Effort:** Medium (4–6 days).

**What exists today**
- Tag add row has category/value inputs and silently returns when value is empty (`tag-selector.ts:62`–`80`, `tag-selector.ts:176`–`179`).
- Annotation note/issue/position/argument forms silently return when text is blank (`annotations-section.ts:303`–`305`, `320`–`322`, `357`–`359`, `383`–`385`).
- Global tags and tag categories silently return for missing value/name (`global-tags.ts:119`–`122`, `tag-categories.ts:132`–`135`).
- Scenario selector create form disables create until name exists and shows a simple create-error paragraph (`scenario-selector-dialog.ts:69`–`78`).
- API token creation disables create until name exists, but create errors appear outside the dialog in the parent message slot (`api-tokens.ts:91`–`107`, `api-tokens.ts:198`–`213`).

**Problems**
- Blank submissions sometimes do nothing with no explanation.
- Error messages are not associated with their fields.
- Dialog-level errors can be visually separated from the dialog where the failure occurred.

**Recommendations**
- Use a shared `InlineCreateForm` pattern for add-row forms.
- On blank submit, mark the field touched and show "Value is required."
- Keep dialog errors inside the dialog.
- Use `aria-describedby` for helper and error text.
EOF
)"

# ===========================================================================
# Section 4 — Accessibility (WCAG 2.2 AA)
# ===========================================================================

child "4.1 Skip navigation and heading structure are incomplete" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 4.1.

**Priority:** High. **Effort:** Small (1 day).
**WCAG:** 2.4.1 Bypass Blocks, 2.4.6 Headings and Labels, 1.3.1 Info and Relationships.

**What exists today**
- The shell uses semantic `header`, `aside`, `main` (`layout.ts:43`, `57`, `60`).
- No skip link before the header/sidebar (`layout.ts:41`–`63`).
- Page titles are usually `h2`, not `h1`; the dashboard starts with `h2` (`list-page.ts:31`, `dashboard.ts:32`, `login.ts:37`).

**Problems**
- Keyboard/screen-reader users must tab through header/sidebar before content on every route.
- Pages lack a consistent `h1`.

**Recommendations**
- Add a skip link as the first focusable element in the layout.
- Give `<main>` `id="main-content"` and `tabindex="-1"`.
- Standardize one `h1` per route in the page shell.
EOF
)"

child "4.2 Several interactive elements are mouse-only or not real links/buttons" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 4.2.

**Priority:** High. **Effort:** Medium (3–5 days).
**WCAG:** 2.1.1 Keyboard, 2.1.3 Keyboard No Exception, 2.4.7 Focus Visible, 4.1.2 Name/Role/Value.

**What exists today**
- Many nav affordances are `<a>` without `href`/`routerLink` and only `(click)`: goal relation links (`goal-editor.ts:111`, `137`), story links (`story-editor.ts:137`, `171`), actor links (`actor-editor.ts:116`, `156`, `169`), use-case links (`use-case-editor.ts:135`, `183`, `243`, `281`, `320`), open issues (`open-issues.ts:91`), scenario sub-scenario links (`scenario-editor.ts:149`–`151`).
- Scenario add-step controls are clickable `div`s (`scenario-editor.ts:134`–`136`, `187`–`189`).
- Tag remove uses a custom button with a multiply character but no PrimeNG sizing/target pattern (`tag-selector.ts:51`–`52`, `94`–`95`).

**Problems**
- Click-only anchors are not keyboard-operable by default and don't expose a link destination.
- Clickable `div`s lack role, accessible name, keyboard activation.
- Focus styling for custom controls is undefined.

**Recommendations**
- Replace click-only anchors with `[routerLink]` anchors.
- Replace clickable `div` controls with real `<button type="button">`.
- Use `aria-hidden="true"` on decorative icons and let button labels provide the name.
EOF
)"

child "4.3 Icon-only buttons often lack accessible names" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 4.3.

**Priority:** High. **Effort:** Small (1–2 days).
**WCAG:** 4.1.2 Name/Role/Value, 2.5.3 Label in Name.

**What exists today**
- Many icon-only `p-button`s omit `ariaLabel`: annotations delete/remove (`annotations-section.ts:95`, `118`, `147`, `163`), story relationships (`story-editor.ts:139`, `173`), use-case relationships (`use-case-editor.ts:188`, `248`, `287`, `325`), actor relationships (`actor-editor.ts:121`), admin rows (`global-tags.ts:73`, `tag-categories.ts:81`), scenario steps (`scenario-editor.ts:154`, `168`, `172`, `176`).
- Some buttons have tooltip text, but tooltips are not a substitute for accessible names (`scenario-editor.ts:156`, `170`, `use-case-editor.ts:190`).

**Problems**
- Screen readers may announce only "button" or the icon class, making destructive actions unclear.

**Recommendations**
- Add `ariaLabel` to every icon-only button, with row context where possible (e.g. `[ariaLabel]="'Remove goal ' + goal.name"`).
EOF
)"

child "4.4 Form labels and error associations are incomplete" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 4.4.

**Priority:** High. **Effort:** Large (tied to reactive-forms migration — see 3.1).
**WCAG:** 1.3.1, 3.3.1 Error Identification, 3.3.2 Labels or Instructions, 3.3.3 Error Suggestion, 4.1.3 Status Messages.

**What exists today**
- Most primary fields have visible labels (`goal-editor.ts:77`, `story-editor.ts:80`, `scenario-editor.ts:94`, `use-case-editor.ts:84`).
- Some mini-form inputs use `aria-label` with no visible label (`tag-selector.ts:63`–`72`, `global-tags.ts:49`–`52`, `tag-categories.ts:50`–`61`).
- Page-level `p-message` shows errors, but fields do not set `aria-invalid`/`aria-describedby`.
- Search fields use generic `aria-label="Search"` in `list-page` and `entity-selector-dialog` (`list-page.ts:40`–`42`, `entity-selector-dialog.ts:58`–`60`).

**Problems**
- Screen-reader users hear that a form has an error but not which field.
- Multiple "Search" controls are ambiguous.
- Placeholder text is used as instruction and disappears as users type.

**Recommendations**
- Visible labels for add-row mini-forms, or group under a labeled fieldset/legend.
- Specific search labels: "Search goals", "Search entities", "Search open issues".
- Add field errors with `aria-describedby`.
- Add `role="alert"` on blocking `p-message` or wrap with a live region.
EOF
)"

child "4.5 Custom dialogs and overlays miss modal accessibility guarantees" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 4.5.

**Priority:** High. **Effort:** Medium (2–4 days).
**WCAG:** 2.1.2 No Keyboard Trap, 2.4.3 Focus Order, 2.4.7 Focus Visible, 4.1.2 Name/Role/Value.

**What exists today**
- PrimeNG `p-dialog` is used for entity selectors and PAT creation (`entity-selector-dialog.ts:53`, `scenario-selector-dialog.ts:51`, `api-tokens.ts:91`).
- Goal relation type uses a handcrafted `.relation-type-dialog` fixed overlay (`goal-editor.ts:177`–`190`).
- Scenario step details use a handcrafted `.edit-popup-overlay` fixed overlay (`scenario-editor.ts:201`–`224`).

**Problems**
- Custom overlays do not declare `role="dialog"`, `aria-modal`, labelled-by relationships, focus trap, Escape behavior, or focus restore.
- Outside click closes the dialog, but keyboard users may lack equivalent behavior.

**Recommendations**
- Replace custom overlays with `p-dialog [modal]="true" [focusOnShow]="true"`.
- Add `ariaLabelledBy`/header and return focus to the opener after close.
- For destructive confirmations, keep PrimeNG ConfirmDialog with explicit accept/reject labels.
EOF
)"

child "4.6 Async and SSE updates are not announced" "priority:medium" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 4.6.

**Priority:** Medium. **Effort:** Medium (3–5 days).
**WCAG:** 4.1.3 Status Messages; 2.2.2 Pause/Stop/Hide where continuous updates distract.

**What exists today**
- The layout opens the SSE connection on init (`layout.ts:157`–`160`).
- Sidebar reloads project counts on Project stream events (`sidebar-nav.ts:268`–`272`).
- Editors subscribe to entity streams and reload data (`goal-editor.ts:328`–`334`, `actor-editor.ts:302`–`308`, `scenario-editor.ts:426`–`432`, `use-case-editor.ts:513`–`519`).
- The event stream exposes connection-state signals (`event-stream.service.ts:38`–`40`), but no UI displays or announces it.

**Problems**
- Users don't know when a background update changed counts/content.
- Screen-reader users receive no status messages.
- Some reload paths avoid overwriting unsaved edits but don't tell the user a newer version exists (`goal-editor.ts:314`–`317`, `scenario-editor.ts:404`–`409`).

**Recommendations**
- Add a global live-region service for status messages (`aria-live="polite"`).
- Announce "Project list updated", "Goal updated by another user", "New version available; save or reload."
- Show non-modal inline update banners in editors when SSE updates are skipped due to local edits.
EOF
)"

child "4.7 Color contrast, color-only meaning, reduced motion, and target size need policy" "priority:medium" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 4.7.

**Priority:** Medium. **Effort:** Medium (3–5 days for policy and fixes; ongoing in design system).
**WCAG:** 1.4.1 Use of Color, 1.4.3 Contrast Minimum, 1.4.11 Non-text Contrast, 2.3.3 Animation from Interactions, 2.5.8 Target Size (Minimum).

**What exists today**
- Hard-coded/fallback badge colors encode meaning (`annotations-section.ts:241`–`249`, `open-issues.ts:113`–`114`).
- Drag/drop transitions exist in scenario steps (`scenario-editor.ts:265`–`266`, `278`).
- Header white-on-`#1a1a7e` likely passes contrast but is outside semantic token governance (`layout.ts:79`–`80`).
- Some small text/icon controls are likely below comfortable target size: chip remove, small text buttons (`tag-selector.ts:90`–`95`, `annotations-section.ts:240`).

**Problems**
- Without tokenized contrast pairs, future theme changes can break AA.
- Some meanings rely partly on color (issue/resolved/argument support).
- Reduced-motion users get drag/drop and hover transitions without a motion policy.

**Recommendations**
- Verify token color pairs with axe and manual contrast tooling.
- Add labels/icons in addition to color for state.
- Add `@media (prefers-reduced-motion: reduce)` CSS to neutralize animations/transitions.
- Set minimum hit areas for custom controls: at least 24×24 CSS px (WCAG 2.2 AA), preferably 36–40 px for app ergonomics.
EOF
)"

# ===========================================================================
# Section 5 — Front-End Architecture and Efficiency
# ===========================================================================

child "5.1 Standalone/lazy routes are good, but route groups need structure" "priority:medium" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 5.1.

**Priority:** Medium. **Effort:** Medium (3–5 days).

**What exists today**
- Standalone components and route-level `loadComponent` for most features (`app.routes.ts:37`–`61`).
- Login, layout, dashboard are eager imports (`app.routes.ts:25`–`27`, `30`–`36`).
- Build budgets are configured (`angular.json:39`–`49`).

**Problems**
- Flat route configuration is long and hard to scan.
- Feature route constants per domain would improve ownership.
- No route `data` for titles, breadcrumbs, or required permissions that a page shell could consume.

**Recommendations**
- Split route arrays by domain (`projectRoutes`, `adminRoutes`, `accountRoutes`).
- Add route `data` for title, section, artifact type, breadcrumb metadata.
- Keep lazy `loadComponent`; consider lazy route children if sections grow.
EOF
)"

child "5.2 Signals are used, but form/state hygiene is mixed" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 5.2.

**Priority:** High. **Effort:** Large (paired with the reactive-forms migration in 3.1).

**What exists today**
- Many components use signals for loading, errors, permissions, entity state (`goal-editor.ts:229`–`236`, `use-case-editor.ts:391`–`404`).
- Many form fields are mutable class fields so `[(ngModel)]` works (`user-editor.ts:155`–`163`, `goal-editor.ts:238`–`239`).
- Some components force `detectChanges()` due to PrimeNG timing + mutable state (`user-editor.ts:226`–`230`, `settings.ts:122`–`125`).

**Problems**
- State is split across signals, plain fields, `NgForm`, and manual dirty flags.
- Dirty checking is implemented differently per editor.
- Signal benefits are limited when templates bind to mutable fields.

**Recommendations**
- Use reactive forms for mutable form state.
- Keep server entity and permissions in signals.
- Derive dirty/valid/submittable state from form controls.
- Remove manual `trackChanges()` where the form can provide `dirty`.
EOF
)"

child "5.3 Change detection and subscriptions are not modernized" "priority:medium" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 5.3.

**Priority:** Medium. **Effort:** Medium (4–7 days).

**What exists today**
- No `ChangeDetectionStrategy.OnPush` in source (`rg` found none).
- No `provideZoneChangeDetection` or zoneless configuration in app config (`app.config.ts:30`–`37`).
- Components manually store and unsubscribe `Subscription`s (`sidebar-nav.ts:252`–`253`, `goal-editor.ts:252`–`253`, `scenario-editor.ts:341`–`342`, `use-case-editor.ts:436`–`437`).

**Problems**
- Default change detection is simpler but less efficient for a data-heavy table/editor app.
- Manual subscription cleanup repeats boilerplate and can be missed in future code.
- Zoneless readiness is blocked by mutable forms and manual `detectChanges()`.

**Recommendations**
- Add `changeDetection: ChangeDetectionStrategy.OnPush` to leaf components first.
- Replace manual subscriptions with `takeUntilDestroyed(inject(DestroyRef))`.
- Consider `toSignal()` for route params and stream-derived state where practical.
- Evaluate zoneless only after OnPush + reactive forms + PrimeNG behavior are stable.
EOF
)"

child "5.4 SSE service is thoughtful but disconnected from UX and app-level state" "priority:medium" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 5.4.

**Priority:** Medium. **Effort:** Medium (4–6 days).

**What exists today**
- `EventStreamService` uses `fetch` with an authorization header because native `EventSource` cannot send JWT headers (`event-stream.service.ts:27`–`33`).
- Tracks connection state and session id with signals (`event-stream.service.ts:38`–`40`).
- Handles reconnect with exponential backoff (`event-stream.service.ts:246`–`260`).
- Editors reload on stream events; some avoid overwriting unsaved changes (`goal-editor.ts:314`–`317`, `scenario-editor.ts:404`–`409`).

**Problems**
- Subscription requests do not check response status (`event-stream.service.ts:90`–`123`).
- Reconnect retains only the initial subscription list; dynamic additions depend on server session continuity and may be lost if a new session is created.
- UI does not surface connection problems or skipped updates.

**Recommendations**
- Track active subscriptions client-side in a signal/set and replay on reconnect.
- Check add/remove subscription response status and expose recoverable errors.
- Add a stream status badge in the layout only when degraded.
- Add live-region announcements for background updates.
EOF
)"

child "5.5 Shared components exist but are too thin for the app's repeated patterns" "priority:high" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 5.5.

**Priority:** High. **Effort:** Large (2–4 weeks incrementally).

**What exists today**
- `ListPageComponent` wraps title, actions, search (`list-page.ts:28`–`47`).
- Entity selector, scenario selector, tag selector, annotations section are shared (`entity-selector-dialog.ts:48`, `scenario-selector-dialog.ts:46`, `tag-selector.ts:34`, `annotations-section.ts:32`).
- Editors still repeat page headers, form grids, action rows, relationship tables, errors, and confirmation behavior.

**Problems**
- The shared layer does not enforce accessibility or visual consistency.
- Repeated sections increase bug surface area and slow design changes.

**Recommendations**
- Build a shared UI/pattern layer: `PageShellComponent`, `EntityListComponent`, `EntityEditorShellComponent`, `FieldComponent`, `RelationshipSectionComponent`, `EmptyStateComponent`, `CommandMessageComponent`, `StatusLiveRegionService`.
- Make the shared layer responsible for headings/breadcrumbs, action placement, responsive layout, field error markup, empty/loading/error states, and icon-button labels.
EOF
)"

child "5.6 Bundle and dependency posture is reasonable but should be measured" "priority:low" "$(cat <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 5.6.

**Priority:** Low. **Effort:** Small (1 day).

**What exists today**
- Dependencies are limited: Angular, CDK, PrimeNG, PrimeIcons, RxJS (`package.json:17`–`30`).
- Playwright and testing libraries are already available (`package.json:36`–`46`).
- Production build budgets exist (`angular.json:39`–`49`).

**Problems**
- No visible bundle-analyzer script.
- PrimeNG imports are per component (good), but repeated component code may increase compiled template size.

**Recommendations**
- Add `npm run build -- --stats-json` (or Angular equivalent) and inspect output periodically.
- Add a release gate for production budget warnings.
- Prefer shared components/patterns to reduce template duplication.
EOF
)"

echo
echo "Done. Epic #$EPIC with 23 child issues."
echo "Verify: gh issue view $EPIC --repo $REPO --json title,subIssues"

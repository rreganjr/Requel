#!/usr/bin/env bash
#
# update-ui-ux-subissues.sh
# Tightens the UI/UX epic (#124) child issues after review:
#   - appends missing acceptance criteria to each child issue
#   - narrows overlapping tickets so concrete primitive work lives in the later
#     N-tickets and adoption/integration tickets reference those prerequisites
#   - rewrites the phase rollup comment in dependency order
#   - reorders the native sub-issue list to match that order
#   - adds native GitHub "blocked by" issue dependency relationships
#
# Requirements:
#   - gh CLI authenticated with repo write access.
#   - GitHub issue dependencies API. See:
#     https://docs.github.com/en/rest/issues/issue-dependencies
#
# Usage:
#   bash scripts/update-ui-ux-subissues.sh
#   DRY_RUN=1 bash scripts/update-ui-ux-subissues.sh
#
set -euo pipefail

REPO="${REPO:-rreganjr/Requel}"
EPIC="${EPIC:-124}"
COMMENT_ID="${COMMENT_ID:-5113771759}"
DRY_RUN="${DRY_RUN:-0}"
SUBISSUE_API_VERSION="2022-11-28"
DEPENDENCY_API_VERSION="2026-03-10"

ORDER=(
  # Phase 1 - accessibility blockers
  135 136 137 139

  # Phase 2 - design system and shared primitives
  125 126 127 141 156 155 131 157 158

  # Phase 3 - forms and validation
  132 133 134 138 143

  # Phase 4 - IA/workflow adoption
  142 154 128 129 130 140 146

  # Phase 5 - deeper architecture and optional polish
  144 145 147 159
)

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

run() {
  if [[ "$DRY_RUN" == "1" ]]; then
    printf 'DRY_RUN:'
    printf ' %q' "$@"
    echo
  else
    "$@"
  fi
}

api() {
  if [[ "$DRY_RUN" == "1" ]]; then
    printf 'DRY_RUN: gh api'
    printf ' %q' "$@"
    echo
  else
    gh api "$@"
  fi
}

issue_id() {
  gh api -H "X-GitHub-Api-Version: $DEPENDENCY_API_VERSION" \
    "/repos/$REPO/issues/$1" --jq '.id'
}

append_acceptance() {
  local issue="$1"
  local body_file="$tmpdir/issue-$issue.md"
  local criteria_file="$tmpdir/issue-$issue-criteria.md"

  cat > "$criteria_file"
  gh issue view "$issue" --repo "$REPO" --json body --jq '.body' > "$body_file"

  if grep -q '^## Acceptance criteria' "$body_file"; then
    echo ">> #$issue already has acceptance criteria"
    return
  fi

  {
    cat "$body_file"
    printf '\n\n## Acceptance criteria\n'
    cat "$criteria_file"
  } > "$body_file.next"

  echo ">> append acceptance criteria to #$issue"
  run gh issue edit "$issue" --repo "$REPO" --body-file "$body_file.next"
}

replace_body() {
  local issue="$1"
  local body_file="$tmpdir/issue-$issue-replacement.md"
  cat > "$body_file"
  echo ">> replace body for #$issue"
  run gh issue edit "$issue" --repo "$REPO" --body-file "$body_file"
}

add_blocked_by() {
  local issue="$1"
  local blocker="$2"
  local blocker_id
  local already

  blocker_id="$(issue_id "$blocker")"
  already="$(gh api -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: $DEPENDENCY_API_VERSION" \
    "/repos/$REPO/issues/$issue/dependencies/blocked_by" \
    --jq ".[] | select(.id == $blocker_id) | .id" || true)"

  if [[ -n "$already" ]]; then
    echo ">> #$issue already blocked by #$blocker"
    return
  fi

  echo ">> add dependency: #$issue blocked by #$blocker"
  api -X POST \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: $DEPENDENCY_API_VERSION" \
    "/repos/$REPO/issues/$issue/dependencies/blocked_by" \
    -F issue_id="$blocker_id" >/dev/null
}

patch_rollup_comment() {
  local body_file="$tmpdir/rollup.md"
  cat > "$body_file" <<'EOF'
## Remediation rollup by phase

Grouped from `doc/UI_UX_REVIEW.md` findings 1.1-5.6 plus the look-and-feel items (N1-N6, see `doc/124-lookandfeel-plan.md`). Closed sub-issues are auto-checked; the rest are checked as each PR squash-merges to `release/2.0`.

**Execution order matters - do issues top-to-bottom.** The list below is the intended build order, and the epic's native sub-issue list is sorted to match. Key dependency notes:

- Phase 1 closes app-wide accessibility blockers that later components must not reintroduce.
- Phase 2 creates tokens and shared primitives. N4 #157 owns `app-data-table`; N5 #158 owns `app-field`/wizard primitives only, not full form migration.
- Phase 3 migrates forms using the N5 primitives. #132 is blocked by #158; #138 is blocked by #132 and the command-error adapter #133.
- Phase 4 applies the primitives to IA/workflow tickets. #146 is an adoption/integration ticket after N1-N5, not a catch-all primitive build.
- Phase 5 handles lower-level Angular/SSE/bundle work plus optional dark-mode config.

### Phase 1 - Accessibility blockers

- [ ] #135 - 4.1 Skip navigation and heading structure are incomplete
- [ ] #136 - 4.2 Several interactive elements are mouse-only or not real links/buttons
- [x] #137 - 4.3 Icon-only buttons often lack accessible names
- [x] #139 - 4.5 Custom dialogs and overlays miss modal accessibility guarantees

### Phase 2 - Design system and shared primitives

- [ ] #125 - 1.1 App uses stock Aura with no Requel brand layer
- [ ] #126 - 1.2 Component-local CSS fights PrimeNG and fragments visual consistency
- [ ] #127 - 1.3 Typography and hierarchy are too flat
- [ ] #141 - 4.7 Color contrast, color-only meaning, reduced motion, and target size need policy
- [ ] #156 - N3 Card / content-surface primitive (app-card)
- [ ] #155 - N2 Tag & Chip severity system as shared primitives
- [ ] #131 - 2.4 Loading, empty, and failure states are under-specified
- [ ] #157 - N4 Data-table pattern component (app-data-table)
- [ ] #158 - N5 Multi-step entity-create wizard (app-form-wizard + app-field)

### Phase 3 - Forms and validation remediation

- [ ] #132 - 3.1 Forms are mostly template-driven and lack consistent validation
- [ ] #133 - 3.2 API and command errors are surfaced inconsistently
- [ ] #134 - 3.3 Mini-forms (annotations, tags, admin, dialogs) need the same validation contract
- [ ] #138 - 4.4 Form labels and error associations are incomplete
- [ ] #143 - 5.2 Signals are used, but form/state hygiene is mixed

### Phase 4 - Information architecture and workflow adoption

- [ ] #142 - 5.1 Standalone/lazy routes are good, but route groups need structure
- [ ] #154 - N1 App shell: top bar + grouped collapsible sidebar
- [ ] #128 - 2.1 Navigation is complete but project context is hidden in the sidebar
- [ ] #129 - 2.2 List/detail patterns are inconsistent and over-rely on row selection
- [ ] #130 - 2.3 Dialog and relationship flows need clearer progression
- [ ] #140 - 4.6 Async and SSE updates are not announced
- [ ] #146 - 5.5 Shared components exist but are too thin for the app's repeated patterns

### Phase 5 - Deeper Angular architecture and optional polish

- [ ] #144 - 5.3 Change detection and subscriptions are not modernized
- [ ] #145 - 5.4 SSE service is thoughtful but disconnected from UX and app-level state
- [ ] #147 - 5.6 Bundle and dependency posture is reasonable but should be measured
- [ ] #159 - N6 Theme switcher + dark mode via config panel (optional)
EOF

  echo ">> patch rollup comment $COMMENT_ID"
  api -X PATCH -H "X-GitHub-Api-Version: $SUBISSUE_API_VERSION" \
    "/repos/$REPO/issues/comments/$COMMENT_ID" \
    -f body="$(cat "$body_file")" >/dev/null
}

reorder_subissues() {
  local current_file
  local want_file
  local first_id
  local prev_id
  local id
  local num

  current_file="$tmpdir/current-subissues.txt"
  want_file="$tmpdir/want-subissues.txt"

  echo ">> verify sub-issue set for #$EPIC"
  gh api --paginate \
    -H "X-GitHub-Api-Version: $SUBISSUE_API_VERSION" \
    "/repos/$REPO/issues/$EPIC/sub_issues" --jq '.[].number' | sort -n > "$current_file"
  printf '%s\n' "${ORDER[@]}" | sort -n > "$want_file"

  if ! diff "$want_file" "$current_file" >/dev/null; then
    echo "ORDER does not match the epic's current sub-issue set:" >&2
    diff "$want_file" "$current_file" >&2 || true
    exit 1
  fi

  first_id="$(gh api -H "X-GitHub-Api-Version: $SUBISSUE_API_VERSION" \
    "/repos/$REPO/issues/$EPIC/sub_issues" --jq '.[0].id')"

  prev_id=""
  for num in "${ORDER[@]}"; do
    id="$(gh api -H "X-GitHub-Api-Version: $SUBISSUE_API_VERSION" \
      "/repos/$REPO/issues/$num" --jq '.id')"
    if [[ -z "$prev_id" ]]; then
      if [[ "$id" != "$first_id" ]]; then
        echo ">> move #$num to top"
        api -X PATCH -H "X-GitHub-Api-Version: $SUBISSUE_API_VERSION" \
          "/repos/$REPO/issues/$EPIC/sub_issues/priority" \
          -F sub_issue_id="$id" -F before_id="$first_id" >/dev/null
      fi
    else
      echo ">> place #$num after previous"
      api -X PATCH -H "X-GitHub-Api-Version: $SUBISSUE_API_VERSION" \
        "/repos/$REPO/issues/$EPIC/sub_issues/priority" \
        -F sub_issue_id="$id" -F after_id="$prev_id" >/dev/null
    fi
    prev_id="$id"
  done
}

# ---------------------------------------------------------------------------
# Scope corrections for tickets that overlapped other tickets.
# ---------------------------------------------------------------------------

replace_body 129 <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 2.2.

**Priority:** High. **Effort:** Medium (3-5 days).

**What exists today**
- Many lists use `p-table` with row selection to navigate (`project-list.ts:59`-`61`, `goal-list.ts:58`-`60`, `story-list.ts:50`-`52`, `stakeholder-list.ts:52`-`54`).
- Reports use explicit Edit/Run actions instead (`report-list.ts:62`-`69`).
- Use Cases and Scenarios disable search (`use-case-list.ts:36`, `scenario-list.ts:37`), while most lists use global search (`list-page.ts:36`-`43`).
- Empty states are plain table messages (`project-list.ts:86`-`88`, `goal-list.ts:83`-`85`, `use-case-list.ts:63`-`65`).

**Problems**
- Row click/select is discoverable for mouse users only after trial and error, and not consistently represented as a link.
- Some lists are searchable, others not, without visible rationale.
- Empty states do not guide users to create the first artifact or explain prerequisites.

**Recommendations**
- Apply the `app-data-table` pattern from #157 to the remaining list/detail surfaces.
- Use explicit link cells for names, with row hover as a secondary affordance.
- Standardize: name column is a real link; optional row-actions column; search on by default; empty state includes title, short guidance, and a primary action if permitted.

## Acceptance criteria
- At least the project, goal, story, actor, stakeholder, scenario, use-case, term, and report list pages use the same list/detail affordance rules.
- Every entity name that navigates is a real link or is paired with an explicit action button.
- Whole-row click is no longer the only way to open details.
- Search/filter behavior is consistent or a visible empty/disabled reason is provided.
- Empty states use the shared empty-state component from #131.

Blocked by #157.
EOF

replace_body 145 <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 5.4.

**Priority:** Medium. **Effort:** Medium (4-6 days).

**What exists today**
- `EventStreamService` uses `fetch` with an authorization header because native `EventSource` cannot send JWT headers (`event-stream.service.ts:27`-`33`).
- Tracks connection state and session id with signals (`event-stream.service.ts:38`-`40`).
- Handles reconnect with exponential backoff (`event-stream.service.ts:246`-`260`).
- Editors reload on stream events; some avoid overwriting unsaved changes (`goal-editor.ts:314`-`317`, `scenario-editor.ts:404`-`409`).

**Problems**
- Subscription requests do not check response status (`event-stream.service.ts:90`-`123`).
- Reconnect retains only the initial subscription list; dynamic additions depend on server session continuity and may be lost if a new session is created.
- The service does not expose enough structured state for UI tickets to show degraded or skipped-update states consistently.

**Recommendations**
- Track active subscriptions client-side in a signal/set and replay on reconnect.
- Check add/remove subscription response status and expose recoverable errors.
- Expose structured stream health/update metadata that #140 can render and announce.
- Keep user-facing live-region announcements in #140; this ticket owns the service contract and reliability.

## Acceptance criteria
- Dynamic subscriptions are replayed after reconnect/new session.
- Add/remove subscription failures are detectable by callers instead of silently ignored.
- Connection state distinguishes connecting, open, degraded/error, closed, and session-expired paths where useful.
- Existing editor reload behavior continues to avoid clobbering unsaved local edits.
- Unit tests cover reconnect replay and failed subscription responses.

Related: #140 owns user-facing live-region announcements and skipped-update banners.
EOF

replace_body 146 <<'EOF'
Part of the UI/UX remediation epic. Source: `doc/UI_UX_REVIEW.md` Finding 5.5.

**Priority:** High. **Effort:** Large (2-4 weeks incrementally).

**What exists today**
- `ListPageComponent` wraps title, actions, and search (`list-page.ts:28`-`47`).
- Entity selector, scenario selector, tag selector, and annotations section are shared (`entity-selector-dialog.ts:48`, `scenario-selector-dialog.ts:46`, `tag-selector.ts:34`, `annotations-section.ts:32`).
- Editors still repeat page headers, form grids, action rows, relationship tables, errors, and confirmation behavior.

**Problems**
- The shared layer does not enforce accessibility or visual consistency.
- Repeated sections increase bug surface area and slow design changes.
- This ticket previously overlapped the concrete primitive tickets; those implementations now live in N1-N5.

**Recommendations**
- After N1-N5 land, define the shared UI/pattern layer boundary and adoption rules.
- Compose pages from the primitives instead of adding new one-off wrappers.
- Make the shared layer responsible for headings/breadcrumbs, action placement, responsive layout, field error markup, empty/loading/error states, and icon-button labels.

## Acceptance criteria
- A short shared UI architecture note exists in `requel-angular/src/app/shared` or `doc/`, naming each primitive and when to use it.
- At least one representative editor and one representative list compose from the new shared primitives without local duplicate header/form/section CSS.
- New shared components include accessibility contracts in code comments or tests.
- No concrete N1-N5 primitive implementation remains scoped to this ticket; this ticket is integration/adoption only.
- Existing tests continue to pass for migrated representative pages.

Blocked by #154, #155, #156, #157, #158, and #131.
EOF

replace_body 158 <<'EOF'
Part of the look-and-feel adoption in `doc/124-lookandfeel-plan.md`.

Build the `app-field` and `app-form-wizard` primitives that later reactive-form migration tickets can consume.

**Scope**
- `app-field`: label + helper text on the left, control on the right, hairline divider, inline error slot, required/optional affordance, and stable responsive layout.
- `app-form-wizard`: vertical step navigation with completion state, active-step content slot, and footer slots for Cancel / Continue / Save.
- Provide a lightweight demo/test host or one low-risk pilot that proves the primitives, but do not migrate the full artifact form set here.

**Not in scope**
- Full reactive-forms migration of existing editors; that belongs to #132.
- Backend command-error mapping; that belongs to #133.
- Full form label/error remediation across the app; that belongs to #138.

## Acceptance criteria
- `app-field` exposes IDs/slots needed for `aria-describedby`, `aria-invalid`, helper text, and inline errors.
- `app-form-wizard` step navigation is keyboard-operable and exposes the active/current step to assistive tech.
- The primitives are styled from theme/design tokens, not component-local color/spacing literals.
- Unit tests or component tests cover required/error rendering and wizard keyboard step changes.
- A small demo/pilot proves compatibility with Angular reactive forms without migrating all editors.

Blocked by #125, #126, and #127.
EOF

# ---------------------------------------------------------------------------
# Acceptance criteria for the remaining child issues.
# ---------------------------------------------------------------------------

append_acceptance 125 <<'EOF'
- `src/app/theme/requel-preset.ts` defines a Requel `definePreset` over Aura.
- `app.config.ts` uses the Requel preset and keeps a dark-mode selector hook.
- `styles.scss` defines app-level spacing, radius, typography, layout, and focus tokens.
- Header, chip, badge, and card colors read from semantic tokens instead of literals.
- A short theme README or code comments document token names and intended usage.
EOF

append_acceptance 126 <<'EOF'
- `:host ::ng-deep` usages in the reviewed UI shell/shared components are removed or justified.
- Hidden file input and table-width inline styles are replaced by shared utilities/components.
- Repeated form-grid/page-header/section CSS is reduced through shared classes or components.
- PrimeNG styling customizations use `styleClass`, pass-through config, or global token classes.
- No new component-local hard-coded brand colors are introduced.
EOF

append_acceptance 127 <<'EOF'
- A tokenized type scale exists for page title, section title, body, label, helper, and caption text.
- Route pages have a consistent title hierarchy that can support one `h1` per page.
- Project/entity context is visually available through shell/page primitives.
- List toolbar density and action placement are consistent across at least two migrated list pages.
EOF

append_acceptance 128 <<'EOF'
- Project-scoped pages show active project context without relying only on the sidebar.
- Breadcrumbs or equivalent context are present for project, section, and entity pages.
- The project workspace route gives counts, open issues, and next actions.
- Deep-linked artifact editor pages are understandable without expanding the sidebar.
- Existing route guards and dirty-check behavior still work.
EOF

append_acceptance 130 <<'EOF'
- Relationship sections use a consistent add/list/remove pattern across at least two artifact editors.
- Create-and-link flows are available only where the next entity type is unambiguous.
- All modal/popup content uses PrimeNG dialog or another accessible dialog primitive.
- Add/remove actions restore or preserve useful focus and produce a status message.
- The goal relation-type flow and scenario sub-scenario flow no longer duplicate modal logic.
EOF

append_acceptance 131 <<'EOF'
- Shared loading, empty, and error-state components or patterns exist.
- List pages and at least two editor pages use the shared states.
- Supplemental tag/annotation failures show non-blocking inline warnings instead of failing silently.
- Empty states include guidance and an action when the user has permission.
- Loading states expose readable labels for assistive tech.
EOF

append_acceptance 132 <<'EOF'
- Login, project editor, account editor, and user editor are migrated to reactive forms.
- Named artifact editors use a shared form contract or migration plan with at least one representative artifact completed.
- Save is disabled when the form is invalid, pristine, or submitting.
- Required name, email, password confirmation, and backend-backed max-length constraints display inline errors.
- Dirty checking is derived from the reactive form for migrated editors.
- At least one create flow consumes the `app-field` primitive from #158 end-to-end.
EOF

append_acceptance 133 <<'EOF'
- A shared command-error adapter maps field violations to reactive form control errors.
- Command-level errors render in a blocking inline message.
- Toasts are reserved for non-blocking success/info confirmations.
- Blocking errors use `role="alert"` or assertive live-region behavior.
- Tests cover field violation mapping and command-level fallback errors.
EOF

append_acceptance 134 <<'EOF'
- Tag, annotation, admin tag/category, scenario selector, and PAT mini-forms use the shared inline-create validation contract or have tracked follow-up exclusions.
- Blank submissions mark fields touched and show an inline required message.
- Dialog-level create errors render inside the dialog.
- Helper/error text is associated with inputs via `aria-describedby`.
- Existing successful create/add flows still work.
EOF

append_acceptance 135 <<'EOF'
- A skip link is the first useful focus target in the authenticated shell.
- Main content has a stable target ID and receives focus when the skip link is activated.
- Pages expose one logical `h1` through the page shell or route page.
- Landmark roles/semantics are validated by an axe Playwright smoke test.
EOF

append_acceptance 136 <<'EOF'
- Click-only anchors are replaced with `routerLink` anchors or buttons.
- Clickable `div` controls are replaced with real buttons.
- Keyboard activation works for scenario add-step, entity links, and open-issue links.
- Focus indicators remain visible after the conversion.
- Tests cover at least one keyboard-only navigation path.
EOF

append_acceptance 137 <<'EOF'
- Every icon-only `p-button` has an accessible name via visible text or `ariaLabel`.
- Destructive row actions include row context in the accessible name where possible.
- Tooltip-only labels are not the sole accessible name.
- A test or lint-style search verifies no reviewed icon-only buttons remain unnamed.
EOF

append_acceptance 138 <<'EOF'
- Migrated form controls set `aria-invalid` when invalid and touched/submitted.
- Field errors and helper text are connected with `aria-describedby`.
- Mini-form labels are visible or grouped under accessible fieldsets/legends.
- Search labels are specific to the current page/dialog.
- Blocking form errors are announced to assistive tech.
EOF

append_acceptance 139 <<'EOF'
- Goal relation-type and scenario step-detail overlays use accessible dialog primitives.
- Dialogs have labels/headings, focus trap, Escape handling, and focus restore.
- Outside-click behavior has a keyboard equivalent.
- Confirm dialogs use explicit accept/reject labels for destructive actions.
EOF

append_acceptance 140 <<'EOF'
- A global or page-scoped live-region mechanism exists for async status messages.
- Project/entity SSE updates can announce meaningful status without overwhelming users.
- Editors show a non-modal "new version available" or equivalent banner when updates are skipped due to unsaved edits.
- Stream degraded/closed state can be surfaced to users if available from #145.
- Tests cover at least one status announcement.
EOF

append_acceptance 141 <<'EOF'
- Theme token color pairs used by text, surfaces, chips, tags, and alerts meet WCAG AA contrast.
- Color-coded states also include text or icon affordances.
- Reduced-motion CSS is present and covers app-defined transitions/animations.
- Custom controls meet at least WCAG 2.2 AA target-size guidance.
- Axe/manual contrast checks are documented for the token palette.
EOF

append_acceptance 142 <<'EOF'
- Routes are grouped by domain or otherwise made easier to scan.
- Route `data` exists for title, section, artifact type, and breadcrumb metadata where needed by the shell.
- Lazy `loadComponent` behavior is preserved.
- Auth/admin/dirty-check guards continue to apply to the same routes.
- Route changes are covered by focused route config tests or smoke tests.
EOF

append_acceptance 143 <<'EOF'
- Migrated editors no longer split form state across mutable fields and signals unnecessarily.
- Manual `trackChanges()` is removed where reactive forms provide dirty/pristine state.
- Server entity, permission, and async state remain signal-based where appropriate.
- Manual `detectChanges()` workarounds are removed from migrated areas or documented if still required.
- Dirty-check guard behavior remains correct.
EOF

append_acceptance 144 <<'EOF'
- Low-risk shared components and leaf pages use `ChangeDetectionStrategy.OnPush`.
- Manual subscription fields are replaced with `takeUntilDestroyed` where practical.
- Route-param and stream subscriptions are cleaned up automatically.
- Zoneless readiness blockers are documented, but zoneless is not enabled unless verified.
- Existing unit/e2e tests pass after the change-detection updates.
EOF

append_acceptance 147 <<'EOF'
- A documented command exists to produce Angular build stats or equivalent bundle output.
- CI or local release docs call out production budget warnings/errors.
- PrimeNG imports remain per-component or otherwise tree-shake friendly.
- Any bundle-growth findings become follow-up issues with concrete targets.
EOF

append_acceptance 154 <<'EOF'
- Breadcrumbs are keyboard-navigable and route-param aware.
- Sidebar groups are collapsible, labelled, and preserve collapse state.
- Header/sidebar colors and surfaces come from theme tokens.
- The shell passes the landmark/skip-link checks from #135.
- The implementation does not take over the project-workspace IA work owned by #128.
EOF

append_acceptance 155 <<'EOF'
- `app-tag`/`app-chip` render every supported severity and variant from tokens.
- Annotation kind, entity status, and tag chips use the shared primitives in at least one representative page.
- Remove controls have accessible names and target sizes.
- Color is paired with visible text or icon meaning.
- No ad-hoc tag/chip color literals remain in migrated components.
EOF

append_acceptance 156 <<'EOF'
- `app-card` supports title, action/content slots, padding, border, radius, and shadow tokens.
- At least one list and one editor surface use `app-card`.
- Radius, shadow, border, background, and padding come from tokens.
- No nested-card pattern is introduced.
EOF

append_acceptance 157 <<'EOF'
- `app-data-table` wraps PrimeNG Table with toolbar, search, sort, pagination, optional selection, status tags, and row actions.
- At least two list pages are migrated.
- Row actions are buttons with accessible names.
- Empty/loading/error states use shared components or slots.
- Search, sort, and pagination are covered by tests for a migrated page.
EOF

append_acceptance 159 <<'EOF'
- Dark mode uses the token preset and does not require component-local overrides.
- The user preference persists and is restored on reload.
- Light and dark modes meet AA contrast for core surfaces/components.
- The feature is behind a clear optional/release-scope decision if not included in v2.0.
EOF

# ---------------------------------------------------------------------------
# Native "blocked by" dependency relationships.
# ---------------------------------------------------------------------------

add_blocked_by 126 125
add_blocked_by 127 125
add_blocked_by 141 125
add_blocked_by 156 125
add_blocked_by 155 125
add_blocked_by 131 125
add_blocked_by 157 125
add_blocked_by 157 131
add_blocked_by 157 136
add_blocked_by 157 137
add_blocked_by 157 155
add_blocked_by 157 156
add_blocked_by 158 125
add_blocked_by 158 126
add_blocked_by 158 127
add_blocked_by 132 158
add_blocked_by 133 158
add_blocked_by 134 133
add_blocked_by 134 158
add_blocked_by 138 132
add_blocked_by 138 133
add_blocked_by 138 158
add_blocked_by 143 132
add_blocked_by 154 125
add_blocked_by 154 135
add_blocked_by 154 142
add_blocked_by 128 142
add_blocked_by 128 154
add_blocked_by 129 157
add_blocked_by 130 136
add_blocked_by 130 137
add_blocked_by 130 139
add_blocked_by 146 131
add_blocked_by 146 154
add_blocked_by 146 155
add_blocked_by 146 156
add_blocked_by 146 157
add_blocked_by 146 158
add_blocked_by 144 132
add_blocked_by 144 143
add_blocked_by 159 125
add_blocked_by 159 141

patch_rollup_comment
reorder_subissues

echo ">> Done. UI/UX epic sub-issues updated."

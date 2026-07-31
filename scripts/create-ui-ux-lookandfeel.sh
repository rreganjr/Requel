#!/usr/bin/env bash
#
# create-ui-ux-lookandfeel.sh
# Applies the look-and-feel plan (doc/124-lookandfeel-plan.md) to the existing UI/UX
# epic #124:
#   1. Appends the §3 "Target look-and-feel" block to existing sub-issues
#      #125, #126, #127, #128, #129, #132, #146 (idempotent — safe to rerun).
#   2. Creates the new N1–N6 sub-issues, each linked to #124 as a real sub-issue.
#
# NOTE: This does NOT rerun create-ui-ux-epic.sh (that would duplicate the epic and its
# 23 existing children). The updates are guarded by a marker so reruns won't
# double-append; the N1–N6 creation step is NOT guarded, so run it once.
#
# Requirements:
#   - gh CLI v2.94.0+ (for the native --parent flag). Check: gh --version
#   - Authenticated: gh auth status
#
# Usage:
#   bash scripts/create-ui-ux-lookandfeel.sh           # do it
#   DRY_RUN=1 bash scripts/create-ui-ux-lookandfeel.sh # print gh commands without running
#
set -euo pipefail

REPO="rreganjr/Requel"
EPIC="124"
MILESTONE="v2.0"
DRY_RUN="${DRY_RUN:-0}"

# --- helpers ---------------------------------------------------------------

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

# Create a child linked to the epic.
child() {  # args: title priority-label body
  local title="$1"; local prio="$2"; local body="$3"
  if grep -qxF "$title" <<<"$EXISTING_TITLES"; then
    echo "  child already exists — skipping: $title"
    return
  fi
  local num
  num=$(create_issue "$title" "$body" \
        --label ui-ux-review --label "$prio" \
        --milestone "$MILESTONE" --parent "$EPIC")
  echo "  child #$num  $title"
}

# Append/refresh the look-and-feel block on an existing issue body. Idempotent AND
# self-healing: if the block is already present it is stripped and rewritten, so a
# rerun replaces (never duplicates) it and repairs any earlier bad render.
MARKER="## Target look-and-feel (see doc/124-lookandfeel-plan.md)"
append_to_issue() {  # args: issue-number body-block
  local num="$1"; local block="$2"
  local current base
  current=$(gh issue view "$num" --repo "$REPO" --json body -q .body 2>/dev/null || echo "")
  base="${current%%"$MARKER"*}"                                 # body before any existing block
  while [[ "$base" == *$'\n' ]]; do base="${base%$'\n'}"; done  # trim trailing newlines
  if [[ "$DRY_RUN" == "1" ]]; then
    printf 'gh issue edit %s --repo %s --body-file <base body + look-and-feel block>\n' "$num" "$REPO" >&2
    return
  fi
  local tmp; tmp=$(mktemp)
  printf '%s\n\n%s\n%s\n' "$base" "$MARKER" "$block" > "$tmp"
  gh issue edit "$num" --repo "$REPO" --body-file "$tmp"
  rm -f "$tmp"
  echo "  updated #$num"
}

# Titles already linked under the epic — makes the create step (§4) idempotent.
EXISTING_TITLES=$(gh issue view "$EPIC" --repo "$REPO" --json subIssues \
  -q '.subIssues.nodes[].title' 2>/dev/null || echo "")

# ===========================================================================
# Step 1 — Append the §3 look-and-feel block to existing sub-issues
# ===========================================================================
echo "Updating existing sub-issues (§3) ..."

append_to_issue 125 "$(cat <<'EOF'
Target the look-and-feel tokens. `src/app/theme/requel-preset.ts` `definePreset` sets: primary `#3b82f6` (hover `#2563eb`); a surface ramp from a cool blue base (`#1e3a8a` mixed toward white) with white cards on a light blue-gray canvas; content border-radius `6px`; base font Figtree at 14px. No component may hard-code these — all read from tokens. Light mode first; leave hooks for a later dark mode.
EOF
)"

append_to_issue 126 "$(cat <<'EOF'
Replace hard-coded chip/badge/header colors with the preset's semantic tokens and a shared card-surface token set (`--rq-card-bg`, `--rq-card-border`, `--rq-card-radius`, `--rq-card-shadow`, `--rq-card-pad`). No `#1a1a7e`/`#3b82f6` literals left in component styles.
EOF
)"

append_to_issue 127 "$(cat <<'EOF'
Load Figtree; define a type scale (page title, card title, field label, helper, body, caption) as tokens and apply through shared primitives (bold slate titles, muted helper text).
EOF
)"

append_to_issue 128 "$(cat <<'EOF'
Restructure the app shell: top bar shows back + breadcrumb (project → section → entity) on the left and search / notifications / account / sidebar-toggle on the right; sidebar uses grouped, labelled, collapsible sections. Shell-chrome build is tracked in N1.
EOF
)"

append_to_issue 129 "$(cat <<'EOF'
Build/adopt a single data-table pattern: card wrapper; toolbar with title + search + primary **New** action; optional checkbox multi-select; a status **Tag** column; sortable headers; a trailing `⋯` row-actions menu; a centered paginator. List pages stop using whole-row click as the only affordance. Component build is tracked in N4.
EOF
)"

append_to_issue 132 "$(cat <<'EOF'
Reactive-forms migration targets a field-row layout: label + helper text on the left, control on the right, hairline dividers between rows, sticky footer with subtle **Cancel** + primary **Save/Continue**. Inline errors sit under the control. Wizard/field component build is tracked in N5.
EOF
)"

append_to_issue 146 "$(cat <<'EOF'
Concrete shared primitives to build (tracked as new sub-issues): app-shell chrome (N1), `app-tag`/`app-chip` severity system (N2), `app-card` surface (N3), `app-data-table` (N4), `app-form-wizard` + `app-field` (N5). These are the reusable base the repeated project/goal/story/actor/scenario/use-case views compose from.
EOF
)"

# ===========================================================================
# Step 2 — Create the new N1–N6 sub-issues
# ===========================================================================
echo "Adding look-and-feel sub-issues to epic #$EPIC ..."

# ===========================================================================
# N1 — App shell
# ===========================================================================
child "N1 App shell: top bar + grouped collapsible sidebar" "priority:medium" "$(cat <<'EOF'
Part of the look-and-feel adoption in `doc/124-lookandfeel-plan.md`.

Reshape `layout` + `sidebar-nav` to the target app shell.

**Top bar**
- Left: back button + breadcrumb reflecting the current route (project → section → entity).
- Right: search, notifications (future), account menu, sidebar collapse toggle.

**Sidebar**
- Grouped, labelled sections; icon + label items; collapsible groups; active-item highlight.

**Canvas**
- Light blue-gray surface token; content in white cards.

**Acceptance**
- Breadcrumb is keyboard-navigable and reflects route params.
- Sidebar collapse state persists.
- Header color comes from tokens (no `#1a1a7e` literal).
- Passes the a11y landmark checks from #135.

Overlaps #128 — this is the shell-chrome half; #128 keeps the project-context/IA half.
EOF
)"

# ===========================================================================
# N2 — Tag & Chip severity system
# ===========================================================================
child "N2 Tag & Chip severity system as shared primitives" "priority:medium" "$(cat <<'EOF'
Part of the look-and-feel adoption in `doc/124-lookandfeel-plan.md`.

Add `app-tag` and `app-chip` wrappers over PrimeNG Tag/Chip.

- Soft-tinted background + matching colored text.
- Tag variants: default (rounded rect), pill (fully rounded), icon (leading icon).
- Severities: `primary | success | info | warning | danger`.
- Chips: leading icon/avatar/image, optional trailing remove (×).
- Replace ad-hoc hard-coded tag colors in `goal-list`, `annotations-section`, `tag-selector`.

**Acceptance**
- One component renders every severity/variant from tokens.
- Used for entity status, annotation kind, and tag chips.
- Color is never the only signal (icon or text label present) — satisfies #141.
EOF
)"

# ===========================================================================
# N3 — Card / content-surface primitive
# ===========================================================================
child "N3 Card / content-surface primitive (app-card)" "priority:low" "$(cat <<'EOF'
Part of the look-and-feel adoption in `doc/124-lookandfeel-plan.md`.

Extract the repeated card container into `app-card`.

- Title slot, padding, hairline border, 6px radius, soft shadow.
- Tokens: `--rq-card-bg`, `--rq-card-border`, `--rq-card-radius`, `--rq-card-shadow`, `--rq-card-pad`.
- Adopt across list and editor shells.

**Acceptance**
- List pages and editors render inside `app-card`.
- No per-view card CSS duplication.
- Radius/shadow/border come from tokens.
EOF
)"

# ===========================================================================
# N4 — Data-table pattern component
# ===========================================================================
child "N4 Data-table pattern component (app-data-table)" "priority:medium" "$(cat <<'EOF'
Part of the look-and-feel adoption in `doc/124-lookandfeel-plan.md`.

Implement the table pattern as a reusable component over PrimeNG Table.

- Toolbar: title + search + primary action.
- Optional checkbox selection column.
- Sortable column headers.
- Status column via `app-tag`.
- Trailing `⋯` row-actions menu.
- Centered paginator (first/prev/page/next/last).
- Drive goal/story/actor/stakeholder/scenario/use-case/term list pages from it.

**Acceptance**
- At least two existing list pages migrated.
- Search + sort + paginate work.
- Row actions are real buttons with accessible names (#136/#137).
- Empty state via the standard empty component (#131).

Concretizes #129 + #146.
EOF
)"

# ===========================================================================
# N5 — Multi-step entity-create wizard
# ===========================================================================
child "N5 Multi-step entity-create wizard (app-form-wizard + app-field)" "priority:medium" "$(cat <<'EOF'
Part of the look-and-feel adoption in `doc/124-lookandfeel-plan.md`.

Build the two-column create wizard.

- Left: vertical step nav with completion state.
- Right: the active step's fields.
- `app-field`: label + helper text on the left, control on the right, hairline divider, inline error slot.
- Footer: subtle Cancel + primary Continue/Save.
- Use for a representative create flow (e.g. new Goal or Story).

**Acceptance**
- Built on reactive forms (#132).
- Labels/errors associated (#138).
- Step nav keyboard-operable.
- One create flow migrated end-to-end.

Concretizes #132 + #146.
EOF
)"

# ===========================================================================
# N6 — (Optional) Theme switcher + dark mode
# ===========================================================================
child "N6 Theme switcher + dark mode via config panel (optional)" "priority:low" "$(cat <<'EOF'
Part of the look-and-feel adoption in `doc/124-lookandfeel-plan.md`.

Add a config panel (gear in the top bar) to toggle light/dark and optionally the
primary color, backed by the preset's dark token set.

**Acceptance**
- Dark mode reads entirely from tokens.
- Preference persists.
- Contrast passes AA in both modes (#141).

Defer if out of scope for v2.0.
EOF
)"

echo "Done. Verify: gh issue view $EPIC --repo $REPO --json title,subIssues"

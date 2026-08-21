#!/usr/bin/env bash
#
# epic-rollup-comment.sh
# Reads the epic's sub-issues from GitHub, groups the UI/UX findings (1.1–5.6)
# under the review's five roadmap phases, and posts a task-list checklist comment
# so the epic doubles as a phase rollup view.
#
# Portable to macOS's stock bash 3.2 (no associative arrays / mapfile).
#
# Requirements: gh CLI v2.94.0+ (sub-issue JSON), authenticated.
#
# Usage:
#   bash scripts/epic-rollup-comment.sh                       # post a new comment on #124
#   COMMENT_ID=5113771759 bash scripts/epic-rollup-comment.sh # edit the EXISTING rollup comment in place
#   EPIC=124 bash scripts/epic-rollup-comment.sh              # override epic number
#   DRY_RUN=1 bash scripts/epic-rollup-comment.sh             # print the comment, do nothing
#
set -euo pipefail

REPO="rreganjr/Requel"
EPIC="${EPIC:-124}"
DRY_RUN="${DRY_RUN:-0}"

# Finding -> phase mapping (from doc/UI_UX_REVIEW.md "Proposed Phased Roadmap").
phase_of() {
  case "$1" in
    4.1|4.2|4.3|4.5|4.7)              echo 1 ;;   # quick wins & a11y blockers
    1.1|1.2|1.3|N2|N3|N6)             echo 2 ;;   # design-system foundation (+ look-and-feel N2/N3/N6)
    3.1|3.1a|3.1b|3.2|3.3|4.4|5.2|N5) echo 3 ;;   # forms & validation (+ 3.1a/3.1b form splits, look-and-feel N5)
    2.1|2.2|2.3|2.4|4.6|5.5|N1|N4)    echo 4 ;;   # IA & workflow polish (+ 5.5/#146 primitive adoption, look-and-feel N1/N4)
    5.1|5.3|5.4|5.6)                  echo 5 ;;   # deeper architecture refactors
    *)                                echo 0 ;;   # unmapped (should not happen)
  esac
}

# Some sub-issues carry no finding-id prefix (server-side backing split out of
# #132, see doc/132-reactive-forms-plan.md). Map those to their phase by number
# so they don't fall into the "Unmapped" bucket. Returns empty for everything
# else, so phase_of() (by finding id) stays the default.
phase_by_number() {
  case "$1" in
    171|176) echo 3 ;;   # bean-validation (#171) + command-error field-name backing (#176) for Phase 3
    *)       echo ""  ;;
  esac
}

phase_name() {
  case "$1" in
    1) echo "Phase 1 — Quick wins & accessibility blockers" ;;
    2) echo "Phase 2 — Design-system foundation" ;;
    3) echo "Phase 3 — Forms & validation remediation" ;;
    4) echo "Phase 4 — Information architecture & workflow polish" ;;
    5) echo "Phase 5 — Deeper Angular architecture refactors" ;;
    0) echo "Unmapped" ;;
  esac
}

TAB="$(printf '\t')"

# Pull sub-issues as "number<TAB>title<TAB>state" lines.
tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT
gh issue view "$EPIC" --repo "$REPO" --json subIssues \
  --jq '.subIssues.nodes[] | "\(.number)\t\(.title)\t\(.state)"' > "$tmp"

if [ ! -s "$tmp" ]; then
  echo "No sub-issues found on #$EPIC — nothing to roll up." >&2
  exit 1
fi

body="## Remediation rollup by phase

Grouped from \`doc/UI_UX_REVIEW.md\` findings 1.1–5.6 plus the look-and-feel items (N1–N6, see \`doc/124-lookandfeel-plan.md\`). Closed sub-issues are auto-checked; the rest are checked as each PR squash-merges to \`release/2.0\`.
"

for p in 1 2 3 4 5 0; do
  section=""
  while IFS="$TAB" read -r num title state; do
    [ -n "$num" ] || continue
    id="${title%% *}"                 # leading "N.N" / "NN" token
    pn="$(phase_by_number "$num")"    # number override for prefix-less titles (#171/#176)
    if [ "${pn:-$(phase_of "$id")}" = "$p" ]; then
      box="[ ]"
      [ "$state" = "CLOSED" ] && box="[x]"
      section="$section- $box #$num $title
"
    fi
  done < "$tmp"
  if [ -n "$section" ]; then
    body="$body
### $(phase_name "$p")
$section"
  fi
done

if [ "$DRY_RUN" = "1" ]; then
  printf '%s\n' "$body"
elif [ -n "${COMMENT_ID:-}" ]; then
  gh api -X PATCH "/repos/$REPO/issues/comments/$COMMENT_ID" -f body="$body" >/dev/null
  echo "Rollup comment #$COMMENT_ID updated in place on #$EPIC."
else
  gh issue comment "$EPIC" --repo "$REPO" --body "$body"
  echo "Rollup comment posted to #$EPIC."
fi

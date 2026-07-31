#!/usr/bin/env bash
#
# reorder-ui-ux-subissues.sh
# Reorders the sub-issues of the UI/UX epic (#124) into the intended build
# order and (optionally) rewrites the "Remediation rollup by phase" comment to
# match. The desired order is the phased roadmap in doc/124-remediation-rollup.md.
#
# Why this exists: sub-issues were added in creation order (#125..#147, then
# #154..#159), and within Phase 3 the shared app-field primitive (N5 #158) sat
# *after* the form tickets (#132/#133/#134/#138) that depend on it. This sorts
# the epic's sub-issue list so it reads top-to-bottom in dependency order.
#
# Requirements:
#   - gh CLI, authenticated (gh auth status) with repo write access.
#   - The GitHub sub-issues REST API (issues/{n}/sub_issues/priority).
#
# Usage:
#   bash scripts/reorder-ui-ux-subissues.sh              # reorder sub-issues only
#   bash scripts/reorder-ui-ux-subissues.sh --comment    # also patch the rollup comment
#   DRY_RUN=1 bash scripts/reorder-ui-ux-subissues.sh    # print API calls, change nothing
#
set -euo pipefail

REPO="${REPO:-rreganjr/Requel}"
EPIC="${EPIC:-124}"
COMMENT_ID="${COMMENT_ID:-5113771759}"          # the "Remediation rollup by phase" comment
COMMENT_FILE="${COMMENT_FILE:-doc/124-remediation-rollup.md}"
API_VERSION="2022-11-28"
DRY_RUN="${DRY_RUN:-0}"

PATCH_COMMENT=0
[[ "${1:-}" == "--comment" ]] && PATCH_COMMENT=1

# Desired build order (issue numbers), top = do first. Must contain every
# sub-issue of the epic exactly once. Grouped by phase for readability.
ORDER=(
  # Phase 1 — Quick wins & accessibility blockers
  135 136 137 139 141
  # Phase 2 — Design-system foundation
  125 126 127 146 155 156 159
  # Phase 3 — Forms & validation (N5 app-field FIRST; 4.4 depends on 3.1)
  158 132 133 134 138 143
  # Phase 4 — Information architecture & workflow polish
  128 129 130 131 140 154 157
  # Phase 5 — Deeper Angular architecture refactors
  142 144 145 147
)

api() {  # thin wrapper so DRY_RUN prints instead of calling
  if [[ "$DRY_RUN" == "1" ]]; then
    printf 'gh api'; printf ' %q' "$@"; echo
  else
    gh api "$@"
  fi
}

# --- sanity: desired order must match the epic's actual sub-issues ----------

echo ">> Fetching current sub-issues of #$EPIC ..." >&2
mapfile -t CURRENT < <(gh api --paginate \
  -H "X-GitHub-Api-Version: $API_VERSION" \
  "/repos/$REPO/issues/$EPIC/sub_issues" --jq '.[].number' | sort -n)

mapfile -t WANT < <(printf '%s\n' "${ORDER[@]}" | sort -n)

if [[ "${#ORDER[@]}" -ne "${#CURRENT[@]}" ]]; then
  echo "!! Count mismatch: ORDER has ${#ORDER[@]}, epic has ${#CURRENT[@]} sub-issues." >&2
fi
if ! diff <(printf '%s\n' "${WANT[@]}") <(printf '%s\n' "${CURRENT[@]}") >/dev/null; then
  echo "!! ORDER does not match the epic's sub-issue set. Diff (want vs actual):" >&2
  diff <(printf '%s\n' "${WANT[@]}") <(printf '%s\n' "${CURRENT[@]}") >&2 || true
  echo "   Fix the ORDER array (or the epic) and re-run." >&2
  exit 1
fi
echo ">> ${#ORDER[@]} sub-issues verified against the epic." >&2

# --- resolve issue number -> database id (needed by the priority API) -------

declare -A ID
for num in "${ORDER[@]}"; do
  ID[$num]=$(gh api -H "X-GitHub-Api-Version: $API_VERSION" \
    "/repos/$REPO/issues/$num" --jq '.id')
done

# Current first sub-issue (by list position) — used to anchor the first item.
FIRST_ID=$(gh api -H "X-GitHub-Api-Version: $API_VERSION" \
  "/repos/$REPO/issues/$EPIC/sub_issues" --jq '.[0].id')

# --- reprioritize: place each item after its predecessor --------------------
# The priority endpoint moves one sub-issue relative to another. Chaining every
# item after the previous one fully determines the order (all items are in the
# chain, so nothing floats).

prev_id=""
for num in "${ORDER[@]}"; do
  id="${ID[$num]}"
  if [[ -z "$prev_id" ]]; then
    if [[ "$id" != "$FIRST_ID" ]]; then
      echo ">> move #$num to top (before current first)" >&2
      api -X PATCH -H "X-GitHub-Api-Version: $API_VERSION" \
        "/repos/$REPO/issues/$EPIC/sub_issues/priority" \
        -F sub_issue_id="$id" -F before_id="$FIRST_ID"
    fi
  else
    echo ">> place #$num after previous" >&2
    api -X PATCH -H "X-GitHub-Api-Version: $API_VERSION" \
      "/repos/$REPO/issues/$EPIC/sub_issues/priority" \
      -F sub_issue_id="$id" -F after_id="$prev_id"
  fi
  prev_id="$id"
done
echo ">> Sub-issue order updated." >&2

# --- optional: rewrite the rollup comment to match --------------------------

if [[ "$PATCH_COMMENT" == "1" ]]; then
  if [[ ! -f "$COMMENT_FILE" ]]; then
    echo "!! $COMMENT_FILE not found; cannot patch comment $COMMENT_ID." >&2
    exit 1
  fi
  echo ">> Patching comment $COMMENT_ID from $COMMENT_FILE ..." >&2
  api -X PATCH -H "X-GitHub-Api-Version: $API_VERSION" \
    "/repos/$REPO/issues/comments/$COMMENT_ID" \
    -f body="$(cat "$COMMENT_FILE")"
  echo ">> Comment updated." >&2
fi

echo ">> Done."

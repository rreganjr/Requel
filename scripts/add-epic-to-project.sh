#!/usr/bin/env bash
#
# add-epic-to-project.sh
# Adds the epic (#124) and all of its child sub-issues to the release/2.0
# GitHub Project. Adding items does not change project dates/iteration fields,
# and re-running is safe (Projects v2 won't duplicate an existing issue).
#
# Requirements: gh CLI authenticated with the "project" scope
#   (if it errors on scope: gh auth refresh -s project).
#
# Usage:
#   bash scripts/add-epic-to-project.sh              # auto-detect project by title "…2.0…"
#   PROJECT=7 bash scripts/add-epic-to-project.sh    # force a specific project number
#   DRY_RUN=1 bash scripts/add-epic-to-project.sh    # print actions, add nothing
#
set -euo pipefail

REPO="rreganjr/Requel"
OWNER="rreganjr"
EPIC="${EPIC:-124}"
DRY_RUN="${DRY_RUN:-0}"

# --- resolve the project number -------------------------------------------
if [ -n "${PROJECT:-}" ]; then
  PROJ="$PROJECT"
else
  PROJ="$(gh project list --owner "$OWNER" --format json \
    --jq '.projects[] | select(.title | test("2\\.0")) | .number' | head -n1)"
fi

if [ -z "${PROJ:-}" ]; then
  echo "Could not auto-detect a release/2.0 project. Re-run with PROJECT=<number>." >&2
  echo "Available projects:" >&2
  gh project list --owner "$OWNER" >&2
  exit 1
fi
echo "Target project: #$PROJ"

# --- collect epic + child issue URLs ---------------------------------------
urls="$(gh issue view "$EPIC" --repo "$REPO" --json url,subIssues \
  --jq '[.url] + [.subIssues.nodes[].url] | .[]')"

count=0
while IFS= read -r url; do
  [ -n "$url" ] || continue
  if [ "$DRY_RUN" = "1" ]; then
    echo "gh project item-add $PROJ --owner $OWNER --url $url"
  else
    if out="$(gh project item-add "$PROJ" --owner "$OWNER" --url "$url" 2>&1)"; then
      echo "added $url"
    elif printf '%s' "$out" | grep -qi "already exists"; then
      echo "skipped (already in project) $url"
    else
      echo "ERROR adding $url: $out" >&2
    fi
  fi
  count=$((count + 1))
done <<EOF
$urls
EOF

echo "Done: $count issues (epic #$EPIC + children) targeted for project #$PROJ."

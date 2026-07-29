#!/usr/bin/env bash
#
# set-epic-milestone.sh
# Assigns the release milestone (the one whose title matches "2.0") to the epic
# (#124) and all of its child sub-issues. Re-running is safe (setting the same
# milestone is a no-op).
#
# Requirements: gh CLI authenticated.
#
# Usage:
#   bash scripts/set-epic-milestone.sh                 # auto-detect milestone by title "…2.0…"
#   MILESTONE="release/2.0" bash scripts/set-epic-milestone.sh   # force an exact milestone title
#   DRY_RUN=1 bash scripts/set-epic-milestone.sh       # print actions, change nothing
#
set -euo pipefail

REPO="rreganjr/Requel"
EPIC="${EPIC:-124}"
DRY_RUN="${DRY_RUN:-0}"

# --- resolve milestone title ----------------------------------------------
if [ -n "${MILESTONE:-}" ]; then
  MS="$MILESTONE"
else
  MS="$(gh api "repos/$REPO/milestones?state=all" \
    --jq '.[] | select(.title | test("^(v|release/)?2\\.0$")) | .title' | head -n1)"
fi

if [ -z "${MS:-}" ]; then
  echo "Could not auto-detect a 2.0 milestone. Re-run with MILESTONE=\"<exact title>\"." >&2
  echo "Existing milestones:" >&2
  gh api "repos/$REPO/milestones?state=all" --jq '.[] | "  \(.title) (\(.state), due \(.due_on // "none"))"' >&2
  exit 1
fi
echo "Milestone: $MS"

# --- collect epic + child issue numbers ------------------------------------
nums="$(gh issue view "$EPIC" --repo "$REPO" --json number,subIssues \
  --jq '[.number] + [.subIssues.nodes[].number] | .[]')"

count=0
while IFS= read -r n; do
  [ -n "$n" ] || continue
  if [ "$DRY_RUN" = "1" ]; then
    echo "gh issue edit $n --repo $REPO --milestone \"$MS\""
  else
    gh issue edit "$n" --repo "$REPO" --milestone "$MS" >/dev/null
    echo "set milestone on #$n"
  fi
  count=$((count + 1))
done <<EOF
$nums
EOF

echo "Done: milestone \"$MS\" set on $count issues (epic #$EPIC + children)."

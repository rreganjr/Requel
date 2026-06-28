#!/usr/bin/env bash
#
# retro-lib.sh — shared config + helpers for the per-release story-point scripts.
# Source it; do not run directly.   . "$(dirname "$0")/retro-lib.sh"
#
# One project per release, matched by milestone. A single RELEASE drives everything
# by naming convention:
#     RELEASE=2.0  ->  milestone "v2.0"  ->  project titled "Requel 2.0"
# Override any of these via environment variables if your naming differs.

OWNER="${REQUEL_OWNER:-rreganjr}"
REPO="${REQUEL_REPO:-rreganjr/Requel}"
RELEASE="${REQUEL_RELEASE:-2.0}"
MILESTONE="${REQUEL_MILESTONE:-v$RELEASE}"
PROJECT_TITLE="${REQUEL_PROJECT_TITLE:-Requel $RELEASE}"

# repo root, for git history (works whether sourced from scripts/ or elsewhere)
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Look up the project NUMBER for PROJECT_TITLE. Empty if it doesn't exist yet.
resolve_project_number() {
  gh project list --owner "$OWNER" --format json \
    | jq -r --arg t "$PROJECT_TITLE" '.projects[] | select(.title==$t) | .number' | head -1
}

# Issue state: "OPEN" or "CLOSED" (uppercase). Used to gate retro writes —
# an issue must be done before it earns a retro value.
issue_state() {   # usage: issue_state 43
  gh issue view "$1" --repo "$REPO" --json state --jq '.state' 2>/dev/null
}

# Distinct days with a commit referencing issues/<n> (this repo puts the full issue
# URL on commit line 1, so match the URL form with a word boundary).
commit_days() {   # usage: commit_days 43
  git -C "$REPO_DIR" log --all -E --grep="issues/$1(\$|[^0-9])" \
    --format='%ad' --date=short 2>/dev/null | sort -u | grep -c . || true
}

# Snap a day count to the nearest Fibonacci rung (ties round up).
snap_fib() {      # usage: snap_fib 4 -> 5
  local d="$1"; local fib=(1 2 3 5 8 13 21 34 55 89); local best=0 bd=999999 diff
  for f in "${fib[@]}"; do
    diff=$(( f > d ? f - d : d - f ))
    if [ "$diff" -lt "$bd" ] || { [ "$diff" -eq "$bd" ] && [ "$f" -gt "$best" ]; }; then
      bd="$diff"; best="$f"
    fi
  done
  echo "$best"
}

# Closed issue numbers in the release's milestone, newest-closed first.
milestone_closed_issues() {
  gh issue list --repo "$REPO" --milestone "$MILESTONE" --state closed --limit 300 \
    --json number,closedAt --jq 'sort_by(.closedAt) | reverse | .[].number'
}

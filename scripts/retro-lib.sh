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

# --- ProjectsV2 auth --------------------------------------------------------
# ProjectsV2 mutations require a CLASSIC PAT with the `project` scope;
# fine-grained PATs can read projects but cannot write them. Keep that token out
# of the ambient environment: it is injected only for `gh project` calls, so
# `gh issue`, git, and everything else keep using the repo-scoped fine-grained
# GH_TOKEN that direnv exports for this tree.
#
# Store it (chmod 600, never committed) at:
#   ~/.config/gh-tokens/rreganjr-projects
# Override the location with REQUEL_PROJECT_TOKEN_FILE.
PROJECT_TOKEN_FILE="${REQUEL_PROJECT_TOKEN_FILE:-$HOME/.config/gh-tokens/rreganjr-projects}"

_project_token() {
  if [ ! -r "$PROJECT_TOKEN_FILE" ]; then
    echo "ERROR: no ProjectsV2 token at $PROJECT_TOKEN_FILE" >&2
    echo "       Create a CLASSIC PAT with only the 'project' scope at" >&2
    echo "       https://github.com/settings/tokens and write it there (chmod 600)." >&2
    return 1
  fi
  tr -d '\r\n' < "$PROJECT_TOKEN_FILE"
}

# Intercept `gh project ...` only; every other gh invocation passes through
# untouched. `command gh` avoids recursing into this function.
gh() {
  if [ "${1:-}" = project ]; then
    local _tok
    _tok="$(_project_token)" || return 1
    GH_TOKEN="$_tok" GITHUB_TOKEN="$_tok" command gh "$@"
  else
    command gh "$@"
  fi
}

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

# True (exit 0) if the issue carries the "Epic" label. Epics are rollup/container
# issues — their child sub-issues carry the effort — so they never earn a retro
# (or an initial estimate). Any epic, present or future, is caught by the label,
# so nothing here is hardcoded to #124.
is_epic() {   # usage: is_epic 124 && echo "it's an epic"
  gh issue view "$1" --repo "$REPO" --json labels \
    --jq 'any(.labels[]?; .name=="Epic")' 2>/dev/null | grep -qx true
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

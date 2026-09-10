#!/usr/bin/env bash
#
# set-points.sh — add an issue to a release's project and set its Story Points
# (initial) and Story Points (Retro). Retro auto-derives from commit-days when
# omitted.
#
# The release determines which project to write to (project "Requel <release>").
# Set it with REQUEL_RELEASE=... or rely on the default (2.0).
#
# Usage:
#   ./set-points.sh <issue#> <initial> [retro]
#   REQUEL_RELEASE=2.1 ./set-points.sh 105 0      # target the 2.1 project
# Examples:
#   ./set-points.sh 43 0          # initial 0, retro auto-computed from commits
#   ./set-points.sh 73 5 3        # initial 5, retro forced to 3
#
set -euo pipefail
. "$(cd "$(dirname "$0")" && pwd)/retro-lib.sh"

ISSUE="${1:?usage: set-points.sh <issue#> <initial> [retro]}"
POINTS="${2:?usage: set-points.sh <issue#> <initial> [retro]}"
RETRO="${3:-}"

# Warm the shared issue index in THIS shell before reading it through `$(...)`,
# which would otherwise build it in a subshell and throw it away. Under
# backfill-points.sh it is already in the environment and this is a no-op.
load_issue_index

# Retro is only for finished work. Check state up front; if the issue is still
# open, we never set a retro value (initial Story Points may still be set).
STATE=$(issue_state "$ISSUE")
if [[ "${STATE^^}" != "CLOSED" ]]; then
  if [[ -n "$RETRO" ]]; then
    echo "==> #$ISSUE is ${STATE:-OPEN} — ignoring the retro value (retro is for closed issues only)."
  fi
  RETRO=""
elif [[ -z "$RETRO" ]]; then
  DAYS=$(commit_days "$ISSUE")
  RETRO=$(snap_fib "$DAYS")
  if [[ "${DAYS:-0}" -gt 0 ]]; then
    echo "==> Auto-retro for #$ISSUE: $DAYS commit-day(s) -> $RETRO"
  else
    # Closed with no commit referencing it: a config/board/duplicate close, or a
    # fix that rode along under another issue. That is 0 effort, not unknown
    # effort — record it, so the issue reads OK on the audit instead of sitting
    # as MISSING forever with no script able to resolve it.
    echo "==> No commits reference issues/$ISSUE; closed without committed work -> retro 0"
  fi
fi

# Epics are rollup containers — their sub-issues carry the effort — so they never
# get a retro, even if one was passed on the command line or auto-computed above.
if is_epic "$ISSUE"; then
  [[ -n "$RETRO" ]] && echo "==> #$ISSUE is an Epic — epics don't carry a retro; ignoring it."
  RETRO=""
fi

NUM=$(resolve_project_number)
if [[ -z "$NUM" ]]; then
  echo "ERROR: no project titled '$PROJECT_TITLE'. Run: ./setup-project.sh $RELEASE" >&2
  exit 1
fi

# Warm these in this shell too, for the same reason, then read them. Cached in
# retro-lib and inherited from a parent backfill run, so a loop over many issues
# resolves the project and its fields once rather than once per issue.
project_id >/dev/null
field_id "Story Points" >/dev/null
field_id "Story Points (Retro)" >/dev/null
PROJECT_ID=$(project_id)
SP_ID=$(field_id "Story Points")
RETRO_ID=$(field_id "Story Points (Retro)")
[[ -z "$SP_ID" || "$SP_ID" == "null" ]] && { echo "ERROR: 'Story Points' field missing in '$PROJECT_TITLE'." >&2; exit 1; }

# A caller that already knows the board item id passes it in REQUEL_ITEM_ID;
# otherwise item-add returns it (and is a no-op when the issue is already there).
ITEM_ID="${REQUEL_ITEM_ID:-}"
if [[ -z "$ITEM_ID" ]]; then
  ITEM_ID=$(gh project item-add "$NUM" --owner "$OWNER" \
    --url "https://github.com/$REPO/issues/$ISSUE" --format json | jq -r '.id')
fi

echo "==> #$ISSUE in '$PROJECT_TITLE': Story Points (initial) = $POINTS"
gh project item-edit --project-id "$PROJECT_ID" --id "$ITEM_ID" --field-id "$SP_ID" --number "$POINTS"

if [[ -n "$RETRO" && -n "$RETRO_ID" && "$RETRO_ID" != "null" ]]; then
  echo "==> #$ISSUE: Story Points (Retro) = $RETRO"
  gh project item-edit --project-id "$PROJECT_ID" --id "$ITEM_ID" --field-id "$RETRO_ID" --number "$RETRO"
fi

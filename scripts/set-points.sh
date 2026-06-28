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
  if [[ "${DAYS:-0}" -gt 0 ]]; then
    RETRO=$(snap_fib "$DAYS")
    echo "==> Auto-retro for #$ISSUE: $DAYS commit-day(s) -> $RETRO"
  else
    echo "==> No commits reference issues/$ISSUE; leaving retro unset"
  fi
fi

NUM=$(resolve_project_number)
if [[ -z "$NUM" ]]; then
  echo "ERROR: no project titled '$PROJECT_TITLE'. Run: ./setup-project.sh $RELEASE" >&2
  exit 1
fi

PROJECT_ID=$(gh project view "$NUM" --owner "$OWNER" --format json | jq -r '.id')
FIELDS=$(gh project field-list "$NUM" --owner "$OWNER" --format json)
SP_ID=$(echo "$FIELDS" | jq -r '.fields[] | select(.name=="Story Points") | .id')
RETRO_ID=$(echo "$FIELDS" | jq -r '.fields[] | select(.name=="Story Points (Retro)") | .id')
[[ -z "$SP_ID" || "$SP_ID" == "null" ]] && { echo "ERROR: 'Story Points' field missing in '$PROJECT_TITLE'." >&2; exit 1; }

ITEM_ID=$(gh project item-add "$NUM" --owner "$OWNER" \
  --url "https://github.com/$REPO/issues/$ISSUE" --format json | jq -r '.id')

echo "==> #$ISSUE in '$PROJECT_TITLE': Story Points (initial) = $POINTS"
gh project item-edit --project-id "$PROJECT_ID" --id "$ITEM_ID" --field-id "$SP_ID" --number "$POINTS"

if [[ -n "$RETRO" && -n "$RETRO_ID" && "$RETRO_ID" != "null" ]]; then
  echo "==> #$ISSUE: Story Points (Retro) = $RETRO"
  gh project item-edit --project-id "$PROJECT_ID" --id "$ITEM_ID" --field-id "$RETRO_ID" --number "$RETRO"
fi

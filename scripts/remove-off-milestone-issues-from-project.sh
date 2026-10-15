#!/usr/bin/env bash
#
# remove-off-milestone-issues-from-project.sh — the inverse of
# add-milestone-issues-to-project.sh: take OPEN issues off a release's board once
# they no longer belong to that release's milestone.
#
# Moving an issue to another milestone does not take it off the old board: the
# milestone and the ProjectsV2 board are independent, and nothing here ever removes
# an item. An issue moved v2.0 -> v2.1 (or back to the backlog) therefore sits on
# both boards, and audit-retros.sh 2.0 keeps walking it.
#
# What belongs on a board (doc/guides/RELEASE_PROCESS.md, "Milestones, boards and
# the backlog"):
#     board "Requel 2.0"  <-  milestone v2.0, and any patch milestone v2.0.<n>
# An OPEN issue on the board with any other milestone, or none, is removed.
#
# Closed issues are never removed, only reported: their board item carries the
# recorded Story Points (Retro), and deleting the item deletes that value with it.
# Draft items and pull requests are left alone.
#
# Removal deletes the board item, which also drops any field values on it (an
# initial Story Points estimate, say). Re-set it on the new board with
# ./scripts/set-points.sh <n> <estimate> if it matters.
#
# Usage:
#   ./remove-off-milestone-issues-from-project.sh              # release 2.0, dry-run
#   ./remove-off-milestone-issues-from-project.sh 2.0          # same
#   ./remove-off-milestone-issues-from-project.sh 2.0 --apply  # actually remove
#
# Typical use, after moving issues between milestones:
#   ./scripts/add-milestone-issues-to-project.sh 2.1
#   ./scripts/remove-off-milestone-issues-from-project.sh 2.0 --apply
#
set -euo pipefail
[[ "${1:-}" =~ ^[0-9] ]] && export REQUEL_RELEASE="$1" && shift
. "$(cd "$(dirname "$0")" && pwd)/retro-lib.sh"
APPLY="${1:-}"

NUM="$(resolve_project_number)"
[[ -z "$NUM" ]] && { echo "ERROR: no project titled '$PROJECT_TITLE'." >&2; exit 1; }

echo "==> Checking '$PROJECT_TITLE' (#$NUM) for open issues not on v$BOARD_RELEASE or v$BOARD_RELEASE.<n>."

BOARD="$(gh project item-list "$NUM" --owner "$OWNER" --limit "$ITEM_LIMIT" --format json)"
warn_if_truncated "$BOARD"

# number<TAB>STATE<TAB>milestone ("-" for none), for every issue in the repo, one call.
ISSUES="$(gh issue list --repo "$REPO" --state all --limit 1000 --json number,state,milestone \
  | jq -r '.[] | [ (.number|tostring), (.state|ascii_upcase), (.milestone.title // "-") ] | @tsv')"

removed=0 kept=0 closed=0
# itemId<TAB>issueNumber<TAB>title for issue items from this repo.
while IFS=$'\t' read -r ITEM_ID NUMBER TITLE; do
  [[ -z "$ITEM_ID" ]] && continue
  STATE="" MS=""
  IFS=$'\t' read -r STATE MS < <(awk -F'\t' -v n="$NUMBER" '$1 == n { print $2 "\t" $3; exit }' <<<"$ISSUES") || true
  MS="${MS:--}"
  if [[ "$MS" == "v$BOARD_RELEASE" || "$MS" =~ ^v${BOARD_RELEASE//./\\.}\.[0-9]+$ ]]; then
    kept=$((kept+1)); continue
  fi
  if [[ -z "$STATE" ]]; then
    echo "    [skip] #$NUMBER not found in the issue list - state unknown, left in place" >&2
    continue
  fi
  if [[ "$STATE" == "CLOSED" ]]; then
    echo "    [skip] #$NUMBER is closed (milestone $MS) - left in place to keep its retro"
    closed=$((closed+1)); continue
  fi
  if [[ "$APPLY" == "--apply" ]]; then
    echo "    removing #$NUMBER (milestone $MS): $TITLE"
    gh project item-delete "$NUM" --owner "$OWNER" --id "$ITEM_ID" >/dev/null
  else
    echo "    [dry-run] would remove #$NUMBER (milestone $MS): $TITLE"
  fi
  removed=$((removed+1))
done < <(jq -r --arg repo "https://github.com/$REPO/" '
    .items[] | select(.content.type == "Issue")
             | select((.content.url // "") | startswith($repo))
             | "\(.id)\t\(.content.number)\t\(.content.title)"' <<<"$BOARD")

echo "==> Done. ${removed} to remove, ${kept} on the milestone, ${closed} closed and skipped."
[[ "$APPLY" == "--apply" ]] || echo "Dry-run only. Re-run with --apply to remove them."

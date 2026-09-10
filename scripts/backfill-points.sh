#!/usr/bin/env bash
#
# backfill-points.sh — fill in Story Points (Retro) for CLOSED issues in a release's
# milestone that do not have one yet.
#
# Issues are selected by MILESTONE (e.g. v2.0) — no hardcoded list. Retro is
# auto-computed from commit-days.
#
# BACKFILL MEANS FILL WHAT IS EMPTY. Two things this script must never do:
#   1. Overwrite a retro that is already set. Some values are deliberate overrides
#      of the commit-day calc; a blind pass would silently flatten every one of them
#      back to the computed number.
#   2. Zero out an initial Story Points estimate. It used to pass a hardcoded 0 for
#      the estimate, which set-points.sh always writes — so a run would have wiped
#      every real pre-work estimate on the board (a 13 and eight 5s among them).
#      The existing estimate is now read off the board and passed back unchanged.
# Pass --recompute to deliberately re-derive retros that are already set (estimates
# are still preserved).
#
# Prereqs: ./setup-project.sh <release> has created the project + fields.
#          Token: classic, scopes project + repo + read:org.
# Usage:
#   ./backfill-points.sh              # release 2.0, only issues missing a retro
#   ./backfill-points.sh 2.1          # release 2.1
#   ./backfill-points.sh 2.0 --recompute
#
set -euo pipefail
[[ "${1:-}" =~ ^[0-9] ]] && export REQUEL_RELEASE="$1" && shift
RECOMPUTE=""
[[ "${1:-}" == "--recompute" ]] && RECOMPUTE=1 && shift
DIR="$(cd "$(dirname "$0")" && pwd)"
. "$DIR/retro-lib.sh"

echo "==> Backfilling retro points for closed issues in milestone '$MILESTONE'"
if [[ -n "$RECOMPUTE" ]]; then
  echo "    (project '$PROJECT_TITLE'; --recompute: existing retros WILL be re-derived)"
else
  echo "    (project '$PROJECT_TITLE'; only issues with no retro yet)"
fi

ISSUES=$(milestone_closed_issues)
if [[ -z "$ISSUES" ]]; then
  echo "No closed issues found in milestone '$MILESTONE'." >&2
  echo "Check the milestone name (override with REQUEL_MILESTONE=...)." >&2
  exit 1
fi

PROJECT_NUM=$(resolve_project_number)
if [[ -z "$PROJECT_NUM" ]]; then
  echo "ERROR: no project titled '$PROJECT_TITLE'. Run: ./setup-project.sh $RELEASE" >&2
  exit 1
fi
# One board read for the whole run: what is already recorded decides what we touch.
BOARD=$(gh project item-list "$PROJECT_NUM" --owner "$OWNER" --limit "$ITEM_LIMIT" --format json)

# Current value of a board field for an issue, or "" when the issue is not on the
# board or the field is unset. A recorded 0 comes back as "0", not "" — zero is a
# value (an issue closed with no committed work), so it must not read as missing.
board_field() {   # usage: board_field 240 "story Points (Retro)"
  jq -r --argjson n "$1" --arg f "$2" \
    '[.items[] | select(.content.type=="Issue" and .content.number==$n) | (.[$f] // null)]
     | if length == 0 or .[0] == null then "" else (.[0] | tostring) end' <<<"$BOARD"
}

filled=0 skipped=0 epics=0

for n in $ISSUES; do
  echo; echo "### issue #$n"
  if is_epic "$n"; then
    echo "    Epic — rollup container, skipping (no points, no retro)."
    epics=$((epics + 1))
    continue
  fi

  RETRO_NOW=$(board_field "$n" "story Points (Retro)")
  if [[ -n "$RETRO_NOW" && -z "$RECOMPUTE" ]]; then
    echo "    Retro already recorded ($RETRO_NOW) — leaving it alone."
    skipped=$((skipped + 1))
    continue
  fi

  # Preserve whatever estimate is on the board; 0 only when there is none.
  INITIAL=$(board_field "$n" "story Points")
  [[ -z "$INITIAL" ]] && INITIAL=0
  "$DIR/set-points.sh" "$n" "$INITIAL"
  filled=$((filled + 1))
done

echo
echo "==> $filled written, $skipped left alone (retro already set), $epics epic(s) skipped."
echo "    Review the '$PROJECT_TITLE' project on GitHub, or re-run ./audit-retros.sh $RELEASE."

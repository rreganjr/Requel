#!/usr/bin/env bash
#
# audit-retros.sh — read-only health check of a release's project board. Flags:
#   [VIOLATION] open issue that has a retro value         (should be cleared)
#   [MISSING]   closed issue with no retro value          (backfill candidate)
#   [DRIFT]     closed issue whose retro != calc, unacknowledged  (worth a look)
#   [OVERRIDE]  closed issue whose retro != calc, acknowledged in retro-overrides.tsv
#   [OK]        closed issue whose retro matches the calc
# Changes nothing. Use clear-open-retros.sh / backfill-points.sh to act on it.
#
# Usage:  ./audit-retros.sh            # release 2.0
#         ./audit-retros.sh 2.0
#
set -euo pipefail
[[ "${1:-}" =~ ^[0-9] ]] && export REQUEL_RELEASE="$1" && shift
. "$(cd "$(dirname "$0")" && pwd)/retro-lib.sh"

NUM=$(resolve_project_number)
[[ -z "$NUM" ]] && { echo "ERROR: no project titled '$PROJECT_TITLE'." >&2; exit 1; }

echo "==> Auditing '$PROJECT_TITLE' (#$NUM). Key: gh lowercases only the first letter -> 'story Points (Retro)'."
printf "%-9s %-6s %-7s %-6s %-6s %s\n" "STATUS" "ISSUE" "STATE" "RETRO" "CALC" "FLAG"

# Build the issue index once, here, before the pipeline below: its `while` body is
# a subshell and each `$(issue_state …)` inside it is another, so without this the
# index would be rebuilt for every board item.
load_issue_index

violations=0 missing=0 drift=0 ok=0

BOARD=$(gh project item-list "$NUM" --owner "$OWNER" --limit "$ITEM_LIMIT" --format json)
warn_if_truncated "$BOARD"

jq -r '.items[] | select(.content.type=="Issue")
       | [(.content.number), (.["story Points (Retro)"] // "")] | @tsv' <<<"$BOARD" \
  | while IFS=$'\t' read -r NUMBER RETRO; do
      STATE=$(issue_state "$NUMBER")
      if [[ "${STATE^^}" != "CLOSED" ]]; then
        if [[ -n "$RETRO" ]]; then
          printf "%-9s #%-5s %-7s %-6s %-6s %s\n" "VIOLATION" "$NUMBER" "open" "$RETRO" "-" "clear it"
        fi
      elif is_epic "$NUMBER"; then
        if [[ -n "$RETRO" ]]; then
          printf "%-9s #%-5s %-7s %-6s %-6s %s\n" "EPIC" "$NUMBER" "closed" "$RETRO" "-" "clear it (epic)"
        else
          printf "%-9s #%-5s %-7s %-6s %-6s %s\n" "EPIC" "$NUMBER" "closed" "-" "-" "skip (container)"
        fi
      else
        CALC=$(snap_fib "$(commit_days "$NUMBER")")
        if [[ -z "$RETRO" ]]; then
          printf "%-9s #%-5s %-7s %-6s %-6s %s\n" "MISSING" "$NUMBER" "closed" "-" "$CALC" "backfill"
        elif [[ "$RETRO" != "$CALC" ]]; then
          ACKNOWLEDGED=$(retro_override "$NUMBER")
          if [[ -n "$ACKNOWLEDGED" && "$ACKNOWLEDGED" == "$RETRO" ]]; then
            printf "%-9s #%-5s %-7s %-6s %-6s %s\n" "OVERRIDE" "$NUMBER" "closed" "$RETRO" "$CALC" \
                   "$(retro_override_reason "$NUMBER")"
          else
            printf "%-9s #%-5s %-7s %-6s %-6s %s\n" "DRIFT" "$NUMBER" "closed" "$RETRO" "$CALC" "review"
          fi
        else
          printf "%-9s #%-5s %-7s %-6s %-6s %s\n" "OK" "$NUMBER" "closed" "$RETRO" "$CALC" ""
        fi
      fi
  done

echo
echo "VIOLATION -> ./clear-open-retros.sh $RELEASE --apply   |   MISSING -> ./backfill-points.sh $RELEASE"
echo "(DRIFT: the recorded retro differs from the calc and is not acknowledged -> review it,"
echo " then record the reviewed value in scripts/retro-overrides.tsv to settle it.)"
echo "(OVERRIDE: differs on purpose, acknowledged. A different value on the board drifts again.)"
echo "(EPIC is skipped: epics are rollup containers; their sub-issues carry the retro.)"

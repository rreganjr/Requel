#!/usr/bin/env bash
#
# audit-retros.sh — read-only health check of a release's project board. Flags:
#   [VIOLATION] open issue that has a retro value         (should be cleared)
#   [MISSING]   closed issue with no retro value          (backfill candidate)
#   [DRIFT]     closed issue whose retro != commit-day calc (worth a look)
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

violations=0 missing=0 drift=0 ok=0

gh project item-list "$NUM" --owner "$OWNER" --format json \
  | jq -r '.items[] | select(.content.type=="Issue")
           | [(.content.number), (.["story Points (Retro)"] // "")] | @tsv' \
  | while IFS=$'\t' read -r NUMBER RETRO; do
      STATE=$(issue_state "$NUMBER")
      if [[ "${STATE^^}" != "CLOSED" ]]; then
        if [[ -n "$RETRO" ]]; then
          printf "%-9s #%-5s %-7s %-6s %-6s %s\n" "VIOLATION" "$NUMBER" "open" "$RETRO" "-" "clear it"
        fi
      else
        CALC=$(snap_fib "$(commit_days "$NUMBER")")
        if [[ -z "$RETRO" ]]; then
          printf "%-9s #%-5s %-7s %-6s %-6s %s\n" "MISSING" "$NUMBER" "closed" "-" "$CALC" "backfill"
        elif [[ "$RETRO" != "$CALC" ]]; then
          printf "%-9s #%-5s %-7s %-6s %-6s %s\n" "DRIFT" "$NUMBER" "closed" "$RETRO" "$CALC" "review"
        else
          printf "%-9s #%-5s %-7s %-6s %-6s %s\n" "OK" "$NUMBER" "closed" "$RETRO" "$CALC" ""
        fi
      fi
  done

echo
echo "VIOLATION -> ./clear-open-retros.sh $RELEASE --apply   |   MISSING -> ./backfill-points.sh $RELEASE"
echo "(DRIFT is informational: the recorded retro differs from the current commit-day calc.)"

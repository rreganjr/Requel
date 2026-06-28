#!/usr/bin/env bash
#
# clear-open-retros.sh — find issues in a release's project that are still OPEN
# but have a Story Points (Retro) value, and clear that value. Retro is for
# finished work only; this fixes any that were set prematurely.
#
# Initial Story Points are left untouched (estimating before work is fine).
#
# Usage:
#   ./clear-open-retros.sh            # release 2.0, dry-run (lists only)
#   ./clear-open-retros.sh 2.0 --apply
#
set -euo pipefail
[[ "${1:-}" =~ ^[0-9] ]] && export REQUEL_RELEASE="$1" && shift
. "$(cd "$(dirname "$0")" && pwd)/retro-lib.sh"
APPLY="${1:-}"

NUM=$(resolve_project_number)
[[ -z "$NUM" ]] && { echo "ERROR: no project titled '$PROJECT_TITLE'." >&2; exit 1; }
PROJECT_ID=$(gh project view "$NUM" --owner "$OWNER" --format json | jq -r '.id')
RETRO_ID=$(gh project field-list "$NUM" --owner "$OWNER" --format json \
  | jq -r '.fields[] | select(.name=="Story Points (Retro)") | .id')

echo "==> Scanning '$PROJECT_TITLE' for OPEN issues with a retro value..."
# emit "itemId<TAB>issueNumber<TAB>retro" for issue items that have a retro set
# NOTE: gh lowercases only the FIRST letter of a field name in --format json,
# so "Story Points (Retro)" -> key "story Points (Retro)" (capital P).
gh project item-list "$NUM" --owner "$OWNER" --format json \
  | jq -r '.items[] | select(.content.type=="Issue")
           | select((.["story Points (Retro)"] // "") != "")
           | "\(.id)\t\(.content.number)\t\(.["story Points (Retro)"])"' \
  | while IFS=$'\t' read -r ITEM_ID NUMBER RETRO; do
      STATE=$(issue_state "$NUMBER")
      [[ "${STATE^^}" == "CLOSED" ]] && continue
      if [[ "$APPLY" == "--apply" ]]; then
        echo "    clearing retro=$RETRO on OPEN #$NUMBER"
        gh project item-edit --project-id "$PROJECT_ID" --id "$ITEM_ID" --field-id "$RETRO_ID" --clear
      else
        echo "    [dry-run] would clear retro=$RETRO on OPEN #$NUMBER"
      fi
  done

[[ "$APPLY" == "--apply" ]] || echo "Dry-run only. Re-run with --apply to clear them."

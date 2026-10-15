#!/usr/bin/env bash
#
# restore-points.sh — re-apply Story Points + Story Points (Retro) from a backup
# TSV made by ./backup-project.sh. Use it to undo an accidental overwrite
# (e.g. a bulk run that zeroed initial points).
#
# It re-writes the RECORDED numbers. It does not clear a field that was empty at
# backup time (gh can't blank a number field here) — so it restores lost values,
# it doesn't roll back values that were newly *added*. Preview first with DRY_RUN=1.
#
# Usage:
#   DRY_RUN=1 ./restore-points.sh tmp/retro-backups/2.0-YYYYMMDD-HHMMSS.tsv
#   ./restore-points.sh          tmp/retro-backups/2.0-YYYYMMDD-HHMMSS.tsv
#
set -euo pipefail
. "$(cd "$(dirname "$0")" && pwd)/retro-lib.sh"
TSV="${1:?usage: restore-points.sh <backup.tsv>}"
[[ -r "$TSV" ]] || { echo "ERROR: cannot read $TSV" >&2; exit 1; }

NUM=$(resolve_project_number)
[[ -z "$NUM" ]] && { echo "ERROR: no project titled '$PROJECT_TITLE'." >&2; exit 1; }
PROJECT_ID=$(gh project view "$NUM" --owner "$OWNER" --format json | jq -r '.id')
FIELDS=$(gh project field-list "$NUM" --owner "$OWNER" --format json)
SP_ID=$(echo "$FIELDS"    | jq -r '.fields[]|select(.name=="Story Points")|.id')
RETRO_ID=$(echo "$FIELDS" | jq -r '.fields[]|select(.name=="Story Points (Retro)")|.id')

restored=0
while IFS=$'\t' read -r NUMBER URL SP RETRO STATUS; do
  [[ -n "${NUMBER:-}" ]] || continue
  if [[ -n "${DRY_RUN:-}" ]]; then
    echo "[dry-run] #$NUMBER -> Story Points=${SP:-<skip>}  Retro=${RETRO:-<skip>}"
    continue
  fi
  ITEM_ID=$(gh project item-add "$NUM" --owner "$OWNER" --url "$URL" --format json | jq -r '.id')
  [[ -n "$SP"    ]] && gh project item-edit --project-id "$PROJECT_ID" --id "$ITEM_ID" --field-id "$SP_ID"    --number "$SP"
  [[ -n "$RETRO" && -n "$RETRO_ID" && "$RETRO_ID" != null ]] && \
     gh project item-edit --project-id "$PROJECT_ID" --id "$ITEM_ID" --field-id "$RETRO_ID" --number "$RETRO"
  echo "restored #$NUMBER: Story Points=${SP:-<skip>}  Retro=${RETRO:-<skip>}"
  restored=$((restored+1))
done < "$TSV"

[[ -z "${DRY_RUN:-}" ]] && echo "==> Restored $restored issue(s) from $TSV"

#!/usr/bin/env bash
#
# backup-project.sh — snapshot a release's project board before any bulk edit.
# Dumps every issue item with its Story Points, Story Points (Retro) and status
# to a timestamped JSON (full fidelity) + TSV (human-readable / restore input),
# under tmp/retro-backups/ (gitignored). Read-only; changes nothing.
#
# Run this BEFORE ./backfill-points.sh, a bulk ./set-points.sh loop, or
# ./clear-open-retros.sh --apply. Restore with ./restore-points.sh <tsv>.
#
# Usage:
#   ./backup-project.sh            # release 2.0
#   ./backup-project.sh 2.1        # release 2.1
#
set -euo pipefail
[[ "${1:-}" =~ ^[0-9] ]] && export REQUEL_RELEASE="$1" && shift
. "$(cd "$(dirname "$0")" && pwd)/retro-lib.sh"

NUM=$(resolve_project_number)
[[ -z "$NUM" ]] && { echo "ERROR: no project titled '$PROJECT_TITLE'." >&2; exit 1; }

STAMP=$(date +%Y%m%d-%H%M%S)
OUT="$REPO_DIR/tmp/retro-backups"
mkdir -p "$OUT"
RAW="$OUT/$RELEASE-$STAMP.json"
TSV="$OUT/$RELEASE-$STAMP.tsv"

# Full board dump (all fields, restorable source of truth).
gh project item-list "$NUM" --owner "$OWNER" --format json --limit 1000 > "$RAW"

# Flatten to: number  url  StoryPoints  Retro  status   (gh lowercases 1st letter)
jq -r '.items[] | select(.content.type=="Issue")
       | [ (.content.number),
           (.content.url),
           (.["story Points"]        // ""),
           (.["story Points (Retro)"]// ""),
           (.status // "") ] | @tsv' "$RAW" | sort -n > "$TSV"

echo "==> Snapshot of '$PROJECT_TITLE' (#$NUM): $(wc -l < "$TSV" | tr -d ' ') issues"
echo "    raw JSON : ${RAW#$REPO_DIR/}"
echo "    table    : ${TSV#$REPO_DIR/}"
echo
echo "    Restore with:  ./restore-points.sh ${TSV#$REPO_DIR/}"

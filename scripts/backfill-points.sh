#!/usr/bin/env bash
#
# backfill-points.sh — backfill Story Points (Retro) for every CLOSED issue in a
# release's milestone, into that release's project.
#
# Issues are selected by MILESTONE (e.g. v2.0) — no hardcoded list. Initial
# Story Points are left at 0 (we did not formally pre-estimate; doc/ plans are
# design docs, not effort estimates). Retro is auto-computed from commit-days.
#
# Prereqs: ./setup-project.sh <release> has created the project + fields.
#          Token: classic, scopes project + repo + read:org.
# Usage:
#   ./backfill-points.sh              # release 2.0
#   ./backfill-points.sh 2.1          # release 2.1
#
set -euo pipefail
[[ "${1:-}" =~ ^[0-9] ]] && export REQUEL_RELEASE="$1" && shift
DIR="$(cd "$(dirname "$0")" && pwd)"
. "$DIR/retro-lib.sh"

echo "==> Backfilling retro points for closed issues in milestone '$MILESTONE'"
echo "    (project '$PROJECT_TITLE'; initial Story Points = 0)"

ISSUES=$(milestone_closed_issues)
if [[ -z "$ISSUES" ]]; then
  echo "No closed issues found in milestone '$MILESTONE'." >&2
  echo "Check the milestone name (override with REQUEL_MILESTONE=...)." >&2
  exit 1
fi

for n in $ISSUES; do
  echo; echo "### issue #$n"
  "$DIR/set-points.sh" "$n" 0
done

echo; echo "All set. Review the '$PROJECT_TITLE' project on GitHub."

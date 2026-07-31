#!/usr/bin/env bash
#
# set-lookandfeel-points.sh — set INITIAL Story Points for the look-and-feel
# sub-issues (N1–N6, #154–#159) in the "Requel 2.0" project, via set-points.sh.
#
# The values below are proposed estimates (Fibonacci) — edit the here-doc to taste
# before running. Retro is left unset (these issues are still open; set-points.sh
# only sets Retro for CLOSED issues).
#
# Prereqs: same as set-points.sh (classic token with project + repo + read:org).
#
# Usage:
#   bash scripts/set-lookandfeel-points.sh
#   REQUEL_RELEASE=2.0 bash scripts/set-lookandfeel-points.sh   # target the 2.0 project (default)
#
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"

# issue  points  # rationale
while read -r num pts _rest; do
  [ -n "${num:-}" ] || continue
  case "$num" in \#*|"") continue ;; esac   # skip comments/blank lines
  echo "==> #$num  Story Points (initial) = $pts"
  "$DIR/set-points.sh" "$num" "$pts"
done <<'EOF'
154 5   # N1 App shell: top bar + breadcrumb + grouped collapsible sidebar (+ a11y, persistence)
155 3   # N2 Tag & Chip severity system (two wrappers, variants, replace 3 usages)
156 2   # N3 Card / content-surface primitive
157 8   # N4 Data-table pattern component + migrate >=2 list pages
158 8   # N5 Form wizard + app-field + migrate one create flow to reactive forms
159 3   # N6 Theme switcher + dark mode (optional)
EOF

echo "Done. Review Story Points in the 'Requel 2.0' project."

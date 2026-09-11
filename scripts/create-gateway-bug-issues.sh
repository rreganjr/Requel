#!/usr/bin/env bash
#
# create-gateway-bug-issues.sh
# Files the six gateway write-surface issues found building the PlatformQ Roundtable
# project. Bodies are sliced from doc/gateway-write-surface-bugs.md on its `---`
# separators: block 0 is the preamble (not filed), blocks 1..6 are the issues.
#
# Requirements: gh CLI, authenticated (gh auth status).
#
# Usage:
#   DRY_RUN=1 bash scripts/create-gateway-bug-issues.sh    # print, create nothing
#   bash scripts/create-gateway-bug-issues.sh              # do it
#   MILESTONE="" bash scripts/create-gateway-bug-issues.sh # no milestone
#
set -euo pipefail

REPO="rreganjr/Requel"
MILESTONE="${MILESTONE-v2.0}"
DRY_RUN="${DRY_RUN:-0}"

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SPEC="$REPO_DIR/doc/gateway-write-surface-bugs.md"
[[ -r "$SPEC" ]] || { echo "ERROR: spec not found: $SPEC" >&2; exit 1; }

BODY_DIR="$REPO_DIR/tmp/gateway-bug-issues"
mkdir -p "$BODY_DIR"
awk -v d="$BODY_DIR" 'BEGIN{n=0} /^---$/{n++; next} {print > (d "/part-" n ".md")}' "$SPEC"
for i in 1 2 3 4 5 6; do
  [[ -s "$BODY_DIR/part-$i.md" ]] || { echo "ERROR: $BODY_DIR/part-$i.md missing/empty" >&2; exit 1; }
done

run() { if [[ "$DRY_RUN" == "1" ]]; then printf '%q ' "$@"; echo; else "$@"; fi; }

run gh label create "gateway" --repo "$REPO" --color 0e8a16 --description "MCP command gateway / write surface" 2>/dev/null || true
run gh label create "bug"     --repo "$REPO" --color d73a4a --description "Something is broken" 2>/dev/null || true

file() {  # args: part-file title extra-label
  local part="$1" title="$2" extra="${3:-}"
  local args=(gh issue create --repo "$REPO" --title "$title"
              --body-file "$BODY_DIR/$part" --label gateway)
  [[ -n "$extra" ]] && args+=(--label "$extra")
  [[ -n "$MILESTONE" ]] && args+=(--milestone "$MILESTONE")
  if [[ "$DRY_RUN" == "1" ]]; then printf '%q ' "${args[@]}"; echo
  else echo "  $("${args[@]}")  $title"; fi
}

file part-1.md "EditScenario throws NPE on a null name when editing by id" bug
file part-2.md "EditScenarioStep typed MCP tool has an empty input schema" bug
file part-3.md "EditPosition leaks a CGLIB proxy class name in positionType" bug
file part-4.md "Step-name collision blames the parent scenario; steps reusable only by id" bug
file part-5.md "Tag category names and values are silently lower-cased" bug
file part-6.md "No path to delete projects the caller is not a stakeholder on"

echo "Done. 6 issues filed${MILESTONE:+ on milestone '$MILESTONE'}."

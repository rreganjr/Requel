#!/usr/bin/env bash
#
# create-delete-project-epic.sh
# Creates the "Delete Project" Epic + 3 child sub-issues on the v2.0 milestone,
# links each child to the epic, and adds them all to the Requel 2.0 project board.
#
# Issue bodies are sliced from doc/delete-project-epic.md (the canonical spec)
# on its `---` separators: block 0 = epic, blocks 1..3 = the children. Keeping
# the prose in the doc means this script stays small and the spec has one home.
#
# Idempotency: if an open epic with the same title already exists, this refuses
# to run (so a re-run can't duplicate #239). Override with FORCE=1 only if you
# really intend to create a second copy.
#
# Requirements:
#   - gh CLI v2.94.0+ (native --parent for sub-issues). Check: gh --version
#   - Authenticated: gh auth status  (project step needs the `project` scope /
#     classic-PAT wrapper used by scripts/add-epic-to-project.sh)
#
# Usage:
#   bash scripts/create-delete-project-epic.sh                 # do it
#   DRY_RUN=1 bash scripts/create-delete-project-epic.sh       # print, create nothing
#   MILESTONE="2.0" bash scripts/create-delete-project-epic.sh # override milestone title
#   FORCE=1 bash scripts/create-delete-project-epic.sh         # allow a duplicate epic
#
# NOTE: The epic is already live as #239 (children #240/#241/#242). This script
# is committed for provenance and reuse (e.g. a fresh repo/fork); it will no-op
# against the current repo unless FORCE=1.
#
set -euo pipefail

REPO="rreganjr/Requel"
MILESTONE="${MILESTONE:-v2.0}"
DRY_RUN="${DRY_RUN:-0}"
FORCE="${FORCE:-0}"

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SPEC="$REPO_DIR/doc/delete-project-epic.md"
EPIC_TITLE="[Epic] Delete Project — backend command, UI action, and MCP gateway tool"

[[ -r "$SPEC" ]] || { echo "ERROR: spec not found: $SPEC" >&2; exit 1; }

run() {
  if [[ "$DRY_RUN" == "1" ]]; then printf '%q ' "$@"; echo; else "$@"; fi
}

# --- guard against a duplicate epic ---------------------------------------
if [[ "$FORCE" != "1" ]]; then
  existing="$(gh issue list --repo "$REPO" --state all --search "in:title \"$EPIC_TITLE\"" \
    --json number --jq '.[0].number' 2>/dev/null || true)"
  if [[ -n "${existing:-}" ]]; then
    echo "Epic already exists as #$existing — refusing to create a duplicate."
    echo "Re-run with FORCE=1 to override, or DRY_RUN=1 to preview."
    exit 0
  fi
fi

# --- slice the spec into per-issue body files (tmp/ is gitignored) --------
BODY_DIR="$REPO_DIR/tmp/delete-project-epic"
mkdir -p "$BODY_DIR"
awk -v d="$BODY_DIR" 'BEGIN{n=0} /^---$/{n++; next} {print > (d "/part-" n ".md")}' "$SPEC"

# --- labels (idempotent) ---------------------------------------------------
run gh label create "Epic"           --repo "$REPO" --color 6f42c1 --description "Epic: parent tracking issue" 2>/dev/null || true
run gh label create "delete-project" --repo "$REPO" --color 5319e7 --description "Project deletion epic" 2>/dev/null || true

# --- epic ------------------------------------------------------------------
if [[ "$DRY_RUN" == "1" ]]; then
  printf 'gh issue create --repo %s --title %q --body-file %s --label Epic --label delete-project --milestone %q\n' \
    "$REPO" "$EPIC_TITLE" "$BODY_DIR/part-0.md" "$MILESTONE"
  EPIC="NNN"
else
  EPIC=$(gh issue create --repo "$REPO" \
    --title "$EPIC_TITLE" \
    --body-file "$BODY_DIR/part-0.md" \
    --label Epic --label delete-project --milestone "$MILESTONE" | grep -oE '[0-9]+$')
fi
echo "Epic = #$EPIC"

# --- children (linked as sub-issues) --------------------------------------
child() {  # args: part-file title
  local part="$1"; local title="$2"
  if [[ "$DRY_RUN" == "1" ]]; then
    printf 'gh issue create --repo %s --title %q --body-file %s --label delete-project --milestone %q --parent %s\n' \
      "$REPO" "$title" "$BODY_DIR/$part" "$MILESTONE" "$EPIC"
  else
    local num
    num=$(gh issue create --repo "$REPO" --title "$title" \
      --body-file "$BODY_DIR/$part" \
      --label delete-project --milestone "$MILESTONE" --parent "$EPIC" | grep -oE '[0-9]+$')
    echo "  child #$num  $title"
  fi
}

child part-1.md "Backend: DeleteProject command with child cascade, auth, and audit"
child part-2.md "UI: Delete Project action with export-first backup prompt"
child part-3.md "Expose DeleteProject on the gateway (MCP + CLI)"

# --- add epic + children to the Requel 2.0 project board ------------------
if [[ "$DRY_RUN" == "1" ]]; then
  echo "EPIC=$EPIC bash scripts/add-epic-to-project.sh"
else
  EPIC="$EPIC" bash "$REPO_DIR/scripts/add-epic-to-project.sh"
fi

echo "Done. Epic #$EPIC + 3 children on milestone '$MILESTONE'."

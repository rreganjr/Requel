#!/usr/bin/env bash
#
# create-ai-assistants-epic.sh
# Creates the "AI analysis: data-driven assistant definitions" Epic + 8 child sub-issues on
# the v2.0 milestone, links each child to the epic, and adds them all to the Requel 2.0
# project board.
#
# Issue bodies are sliced from doc/ai-per-type-assistants-epic.md (the canonical spec)
# on its `---` separators: block 0 = epic, blocks 1..8 = the children. Same pattern as
# scripts/create-delete-project-epic.sh.
#
# Idempotency: if an epic with the same title already exists, this refuses to run.
# Override with FORCE=1 only if you really intend to create a second copy.
#
# Requirements:
#   - gh CLI v2.94.0+ (native --parent for sub-issues). Check: gh --version
#   - Authenticated: gh auth status  (project step needs the `project` scope /
#     classic-PAT wrapper used by scripts/add-epic-to-project.sh)
#
# Usage:
#   DRY_RUN=1 bash scripts/create-ai-assistants-epic.sh       # print, create nothing
#   bash scripts/create-ai-assistants-epic.sh                 # do it
#   MILESTONE="2.0" bash scripts/create-ai-assistants-epic.sh # override milestone title
#   FORCE=1 bash scripts/create-ai-assistants-epic.sh         # allow a duplicate epic
#
set -euo pipefail

REPO="rreganjr/Requel"
MILESTONE="${MILESTONE:-v2.0}"
DRY_RUN="${DRY_RUN:-0}"
FORCE="${FORCE:-0}"

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SPEC="$REPO_DIR/doc/ai-per-type-assistants-epic.md"
EPIC_TITLE="[Epic] AI analysis: data-driven assistant definitions, typed context providers, and a keyless dev provider"

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
BODY_DIR="$REPO_DIR/tmp/ai-per-type-assistants-epic"
mkdir -p "$BODY_DIR"
awk -v d="$BODY_DIR" 'BEGIN{n=0} /^---$/{n++; next} {print > (d "/part-" n ".md")}' "$SPEC"

for i in 0 1 2 3 4 5 6 7 8; do
  [[ -s "$BODY_DIR/part-$i.md" ]] || { echo "ERROR: $BODY_DIR/part-$i.md missing or empty — check the spec's --- separators" >&2; exit 1; }
done

# --- labels (idempotent) ---------------------------------------------------
run gh label create "Epic"           --repo "$REPO" --color 6f42c1 --description "Epic: parent tracking issue" 2>/dev/null || true
run gh label create "ai-assistants"  --repo "$REPO" --color 1d76db --description "AI assistant analysis, prompts and providers" 2>/dev/null || true

# --- epic ------------------------------------------------------------------
if [[ "$DRY_RUN" == "1" ]]; then
  printf 'gh issue create --repo %s --title %q --body-file %s --label Epic --label ai-assistants --milestone %q\n' \
    "$REPO" "$EPIC_TITLE" "$BODY_DIR/part-0.md" "$MILESTONE"
  EPIC="NNN"
else
  EPIC=$(gh issue create --repo "$REPO" \
    --title "$EPIC_TITLE" \
    --body-file "$BODY_DIR/part-0.md" \
    --label Epic --label ai-assistants --milestone "$MILESTONE" | grep -oE '[0-9]+$')
fi
echo "Epic = #$EPIC"

# --- children (linked as sub-issues) --------------------------------------
child() {  # args: part-file title
  local part="$1"; local title="$2"
  if [[ "$DRY_RUN" == "1" ]]; then
    printf 'gh issue create --repo %s --title %q --body-file %s --label ai-assistants --milestone %q --parent %s\n' \
      "$REPO" "$title" "$BODY_DIR/$part" "$MILESTONE" "$EPIC"
  else
    local num
    num=$(gh issue create --repo "$REPO" --title "$title" \
      --body-file "$BODY_DIR/$part" \
      --label ai-assistants --milestone "$MILESTONE" --parent "$EPIC" | grep -oE '[0-9]+$')
    echo "  child #$num  $title"
  fi
}

child part-1.md "Dev-only cli AI provider: shell out to an authenticated claude/codex CLI"
child part-2.md "Assistant definitions: model, storage, and a per-project registry"
child part-3.md "Typed context providers"
child part-4.md "Close the redaction gap: a real RedactionPolicy and honoured dataHandlingFlags"
child part-5.md "Seed and tune the bundled per-type review definitions"
child part-6.md "Project-authored definitions: command path, API, permissions, Angular UI"
child part-7.md "Cross-cutting policy definitions: multi-type scope and a single composed pass"
child part-8.md "Corpus analyses: findings about relationships between entities"

# --- add epic + children to the Requel 2.0 project board ------------------
if [[ "$DRY_RUN" == "1" ]]; then
  echo "EPIC=$EPIC bash scripts/add-epic-to-project.sh"
else
  EPIC="$EPIC" bash "$REPO_DIR/scripts/add-epic-to-project.sh"
fi

echo "Done. Epic #$EPIC + 8 children on milestone '$MILESTONE'."
echo "Next: comment on #43 linking the epic."

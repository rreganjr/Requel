#!/usr/bin/env bash
#
# add-milestone-issues-to-project.sh — add every issue in a release's milestone
# to that release's project board, skipping any already on the board.
#
# One project per release, matched by milestone/title via retro-lib.sh:
#     RELEASE=2.0  ->  milestone "v2.0"  ->  project titled "Requel 2.0"
# Issues are selected by MILESTONE (open + closed) — no hardcoded list. Adding is
# idempotent (Projects v2 dedupes by content URL), and the diff check keeps the
# output clean and avoids needless API calls.
#
# Prereqs: gh + jq. Token needs scopes: project + repo + read:org.
#          If the token is missing the 'project' scope this script will try to
#          fix it (see ensure_project_scope below) before adding anything.
# Usage:
#   ./add-milestone-issues-to-project.sh            # release 2.0
#   ./add-milestone-issues-to-project.sh 2.1        # release 2.1
#   DRY_RUN=1 ./add-milestone-issues-to-project.sh  # show what would be added
#
set -euo pipefail
[[ "${1:-}" =~ ^[0-9] ]] && export REQUEL_RELEASE="$1" && shift
. "$(cd "$(dirname "$0")" && pwd)/retro-lib.sh"

# gh reads GH_TOKEN from the environment in preference to its stored login, and
# `gh auth refresh` cannot add scopes to an env token. So if a GH_TOKEN is set
# but lacks the 'project' scope, we can't fix it here — clear it and fall back to
# the browser login, which we can refresh.
ensure_project_scope() {
  # Cheap probe: listing projects requires read:project; adding requires project.
  if gh project list --owner "$OWNER" --limit 1 >/dev/null 2>&1; then
    # Read works; confirm write scope is present too.
    if gh auth status 2>&1 | grep -qiE "'project'|\bproject\b"; then
      return 0
    fi
  fi

  echo "==> gh is missing the 'project' scope; attempting to fix..." >&2
  if [[ -n "${GH_TOKEN:-}" ]]; then
    echo "    GH_TOKEN is set and overrides the stored login; unsetting it for this run." >&2
    unset GH_TOKEN
  fi
  if ! gh auth status >/dev/null 2>&1; then
    gh auth login
  fi
  gh auth refresh -s project --hostname github.com
}

ensure_project_scope

NUM="$(resolve_project_number)"
[[ -z "$NUM" ]] && { echo "ERROR: no project titled '$PROJECT_TITLE'." >&2; exit 1; }

echo "==> Adding '$MILESTONE' issues to project '$PROJECT_TITLE' (#$NUM) under @$OWNER."

# URLs already on the board.
in_project="$(gh project item-list "$NUM" --owner "$OWNER" --format json --limit 500 \
  | jq -r '.items[].content.url // empty' | sort -u)"

added=0 skipped=0
# All milestone issues (open + closed); add any not already on the board.
while read -r url; do
  [[ -z "$url" ]] && continue
  if grep -qxF "$url" <<<"$in_project"; then
    echo "Already in project: $url"; skipped=$((skipped+1)); continue
  fi
  if [[ -n "${DRY_RUN:-}" ]]; then
    echo "[dry-run] would add: $url"
  else
    echo "Adding $url"
    gh project item-add "$NUM" --owner "$OWNER" --url "$url"
  fi
  added=$((added+1))
done < <(gh issue list --repo "$REPO" --milestone "$MILESTONE" --state all --limit 500 \
           --json url -q '.[].url' | sort -u)

echo "==> Done. ${added} to add, ${skipped} already present."

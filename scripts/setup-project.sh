#!/usr/bin/env bash
#
# setup-project.sh — create (or reuse) the GitHub Project for a RELEASE and add
# the Story Points + Story Points (Retro) number fields, then link it to the repo.
#
# One project per release. The release drives the project title by convention:
#   RELEASE=2.0 -> project "Requel 2.0", milestone "v2.0".
#
# Prereqs:
#   - gh CLI logged in with a CLASSIC token: scopes  project + repo + read:org
#     (fine-grained tokens CANNOT write user-owned Projects v2). If GH_TOKEN is
#     set in the env it overrides gh auth login — update the env var's value.
#   - jq installed.
#
# Usage:
#   ./setup-project.sh                 # release defaults to 2.0
#   ./setup-project.sh 2.1             # set up the Requel 2.1 project
#   ./setup-project.sh 2.1 --add-milestone   # also add that milestone's issues
#
set -euo pipefail

# allow first positional arg to set the release before sourcing the lib
[[ "${1:-}" =~ ^[0-9] ]] && export REQUEL_RELEASE="$1" && shift
. "$(cd "$(dirname "$0")" && pwd)/retro-lib.sh"

echo "==> Release '$RELEASE'  ->  project '$PROJECT_TITLE', milestone '$MILESTONE'"

if ! gh auth status >/dev/null 2>&1; then
  echo "ERROR: not logged in. Run: gh auth login" >&2; exit 1
fi

NUM=$(resolve_project_number)
if [[ -n "$NUM" ]]; then
  echo "==> Reusing existing project '$PROJECT_TITLE' (#$NUM)"
else
  echo "==> Creating project '$PROJECT_TITLE'..."
  NUM=$(gh project create --owner "$OWNER" --title "$PROJECT_TITLE" --format json | jq -r '.number')
  echo "    created #$NUM"
fi

echo "==> Ensuring number fields exist (idempotent)..."
EXISTING=$(gh project field-list "$NUM" --owner "$OWNER" --format json)
create_field() {
  if echo "$EXISTING" | jq -e --arg n "$1" '.fields[] | select(.name==$n)' >/dev/null; then
    echo "    = '$1' already exists"
  else
    echo "    + creating '$1'"
    gh project field-create "$NUM" --owner "$OWNER" --name "$1" --data-type NUMBER
  fi
}
create_field "Story Points"
create_field "Story Points (Retro)"

echo "==> Linking project to repo $REPO"
gh project link "$NUM" --owner "$OWNER" --repo "$REPO" || true

if [[ "${1:-}" == "--add-milestone" ]]; then
  echo "==> Adding closed issues in milestone '$MILESTONE'..."
  milestone_closed_issues | while read -r n; do
    echo "    + #$n"
    gh project item-add "$NUM" --owner "$OWNER" --url "https://github.com/$REPO/issues/$n"
  done
fi

echo
echo "Done. Project '$PROJECT_TITLE' (#$NUM)."
echo "Next: ./backfill-points.sh $RELEASE"

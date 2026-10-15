#!/usr/bin/env bash
# Create the v2.0 MCP command-gateway series issues from their markdown bodies.
# Title is taken from each file's H1; the body is everything after line 1.
# Requires: gh (authenticated), and the "v2.0" milestone existing in the repo.
set -euo pipefail

REPO="rreganjr/Requel"
MILESTONE="v2.0"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

FILES=(
  "issue_mcp_command_gateway.md"        # ticket 1: command gateway + write tools + stdio bridge
  "issue_cli_and_remote_connector.md"   # ticket 2: requel-cli + remote connector
  "issue_tracker_to_goals_workflow.md"  # ticket 3: issue tracker -> goals + v1 reconciliation
  "issue_smart_goal_reconciliation.md"  # ticket 4: smart reconciliation
  "issue_api_jwt_tokens.md"             # ticket 5: user-mintable API tokens
)

for name in "${FILES[@]}"; do
  f="$DIR/$name"
  title=$(sed -n '1s/^# //p' "$f")
  echo "Creating: $title"
  gh issue create --repo "$REPO" --milestone "$MILESTONE" \
    --title "$title" --body-file <(tail -n +2 "$f")
done

#!/usr/bin/env bash
# Update the already-created v2.0 MCP command-gateway issues (#69-73) from their markdown bodies.
# Title is taken from each file's H1; body is everything after line 1. Milestone already set.
# Requires: gh (authenticated).
set -euo pipefail

REPO="rreganjr/Requel"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# issue number -> body file
declare -A ISSUES=(
  [69]="issue_mcp_command_gateway.md"
  [70]="issue_cli_and_remote_connector.md"
  [71]="issue_tracker_to_goals_workflow.md"
  [72]="issue_smart_goal_reconciliation.md"
  [73]="issue_api_jwt_tokens.md"
)

for num in "${!ISSUES[@]}"; do
  f="$DIR/${ISSUES[$num]}"
  title=$(sed -n '1s/^# //p' "$f")
  echo "Updating #$num: $title"
  gh issue edit "$num" --repo "$REPO" \
    --title "$title" \
    --body-file <(tail -n +2 "$f")
done

#!/usr/bin/env bash
# Post each codex review as a comment on its corresponding v2.0 MCP issue (#69-73).
# The full review markdown file is used as the comment body.
# Requires: gh (authenticated).
set -euo pipefail

REPO="rreganjr/Requel"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# issue number -> review file
declare -A REVIEWS=(
  [69]="issue_mcp_command_gateway-codex-review.md"
  [70]="issue_cli_and_remote_connector-codex-review.md"
  [71]="issue_tracker_to_goals_workflow-codex-review.md"
  [72]="issue_smart_goal_reconciliation-codex-review.md"
  [73]="issue_api_jwt_tokens-codex-review.md"
)

for num in "${!REVIEWS[@]}"; do
  f="$DIR/${REVIEWS[$num]}"
  if [[ ! -f "$f" ]]; then
    echo "SKIP #$num: review file not found: $f" >&2
    continue
  fi
  echo "Commenting on #$num with $(basename "$f")"
  gh issue comment "$num" --repo "$REPO" --body-file "$f"
done

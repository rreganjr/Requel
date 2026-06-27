#!/usr/bin/env bash
#
# probe-project.sh — diagnostic. Dumps the project's field names and the raw
# JSON of a couple of items so we can see exactly how gh represents custom-field
# values (esp. Story Points / Story Points (Retro)) and the issue's open/closed
# state. Read-only; changes nothing.
#
# Usage:  ./probe-project.sh           # release 2.0 by convention
#         ./probe-project.sh 2.0
#         REQUEL_PROJECT_TITLE="Requel" ./probe-project.sh   # if your project is titled just "Requel"
#
set -euo pipefail
[[ "${1:-}" =~ ^[0-9] ]] && export REQUEL_RELEASE="$1" && shift
. "$(cd "$(dirname "$0")" && pwd)/retro-lib.sh"

echo "=== projects on $OWNER ==="
gh project list --owner "$OWNER" --format json | jq -r '.projects[] | "#\(.number)\t\(.title)"'

NUM=$(resolve_project_number)
echo
echo "=== resolved '$PROJECT_TITLE' -> #${NUM:-<none>} ==="
[[ -z "$NUM" ]] && { echo "No project matched that title. Pick the number above and re-run with REQUEL_PROJECT_TITLE set."; exit 0; }

echo
echo "=== field names ==="
gh project field-list "$NUM" --owner "$OWNER" --format json | jq -r '.fields[] | "\(.name)\t(\(.type))"'

ITEMS=$(gh project item-list "$NUM" --owner "$OWNER" --format json)

echo
echo "=== per-item: #number  status  | guessed keys: 'story points' / 'story points (retro)' ==="
echo "$ITEMS" | jq -r '.items[]
  | [(.content.number // "?"), (.status // "-"),
     (.["story Points"] // "-"), (.["story Points (Retro)"] // "-")] | @tsv'

echo
echo "=== raw JSON of up to 3 items that have ANY key containing 'point' (reveals real key name) ==="
echo "$ITEMS" | jq '[.items[] | select([to_entries[].key] | any(test("point"; "i")))][0:3]'

echo
echo "=== count of items whose keys include a point-ish field ==="
echo "$ITEMS" | jq '[.items[] | select([to_entries[].key] | any(test("point"; "i")))] | length'

#!/usr/bin/env bash
#
# retro-lib.sh — shared config + helpers for the per-release story-point scripts.
# Source it; do not run directly.   . "$(dirname "$0")/retro-lib.sh"
#
# One project per release, matched by milestone. A single RELEASE drives everything
# by naming convention:
#     RELEASE=2.0  ->  milestone "v2.0"  ->  project titled "Requel 2.0"
# Override any of these via environment variables if your naming differs.

OWNER="${REQUEL_OWNER:-rreganjr}"
REPO="${REQUEL_REPO:-rreganjr/Requel}"
RELEASE="${REQUEL_RELEASE:-2.0}"
MILESTONE="${REQUEL_MILESTONE:-v$RELEASE}"
PROJECT_TITLE="${REQUEL_PROJECT_TITLE:-Requel $RELEASE}"

# repo root, for git history (works whether sourced from scripts/ or elsewhere)
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# --- ProjectsV2 auth --------------------------------------------------------
# ProjectsV2 mutations require a CLASSIC PAT with the `project` scope;
# fine-grained PATs can read projects but cannot write them. Keep that token out
# of the ambient environment: it is injected only for `gh project` calls, so
# `gh issue`, git, and everything else keep using the repo-scoped fine-grained
# GH_TOKEN that direnv exports for this tree.
#
# Store it (chmod 600, never committed) at:
#   ~/.config/gh-tokens/rreganjr-projects
# Override the location with REQUEL_PROJECT_TOKEN_FILE.
PROJECT_TOKEN_FILE="${REQUEL_PROJECT_TOKEN_FILE:-$HOME/.config/gh-tokens/rreganjr-projects}"

# --- board paging ------------------------------------------------------------
# `gh project item-list` defaults to 30 items and truncates SILENTLY - no warning,
# no error, just a short list. A board past 30 items therefore audits as clean while
# the newest work is invisible (#240/#241/#242/#247 were all missing from the 2.0
# audit for exactly this reason). Every item-list call passes this.
#
# Raising the limit is FREE, and this was measured rather than assumed: a 2.0 audit
# costs 208 GraphQL points with --limit 200 and 208 with --limit 500. gh pages
# item-list at 100 items, each pulling fieldValues(first:100) - 10,000 nodes, so 100
# points per page - and stops once it runs out of items. The 107-item board is two
# pages either way; --limit only decides when paging stops. So set it high enough
# that truncation stays unlikely, and let warn_if_truncated catch the rest.
ITEM_LIMIT="${REQUEL_ITEM_LIMIT:-500}"

# Loudly flag a board read that hit the limit. Silence is what made the 30-item
# default so expensive to diagnose; a truncated read must never look like a clean one.
warn_if_truncated() {   # usage: warn_if_truncated "$BOARD"
  local total have
  total="$(jq -r '.totalCount // 0' <<<"$1")"
  have="$(jq -r '.items | length' <<<"$1")"
  if [ "${total:-0}" -gt "${have:-0}" ]; then
    echo "WARNING: the board holds $total items but only $have were read" >&2
    echo "         (REQUEL_ITEM_LIMIT=$ITEM_LIMIT). RESULTS BELOW ARE INCOMPLETE -" >&2
    echo "         re-run with REQUEL_ITEM_LIMIT=$((total + 50))." >&2
  fi
}

_project_token() {
  if [ ! -r "$PROJECT_TOKEN_FILE" ]; then
    echo "ERROR: no ProjectsV2 token at $PROJECT_TOKEN_FILE" >&2
    echo "       Create a CLASSIC PAT with only the 'project' scope at" >&2
    echo "       https://github.com/settings/tokens and write it there (chmod 600)." >&2
    return 1
  fi
  tr -d '\r\n' < "$PROJECT_TOKEN_FILE"
}

# Intercept `gh project ...` only; every other gh invocation passes through
# untouched. `command gh` avoids recursing into this function.
gh() {
  if [ "${1:-}" = project ]; then
    local _tok
    _tok="$(_project_token)" || return 1
    GH_TOKEN="$_tok" GITHUB_TOKEN="$_tok" command gh "$@"
  else
    command gh "$@"
  fi
}

# --- lookup caching ----------------------------------------------------------
# GraphQL is billed at 5,000 points/hour and these scripts used to spend that on
# repetition: set-points.sh re-resolved the project and re-listed every field on
# EVERY issue, and audit-retros.sh asked for each item's state and labels one call
# at a time. A single audit ran ~215 calls and a full backfill several hundred, so
# an evening's work hit the limit mid-run.
#
# Every lookup below is resolved once and reused. The answers are exported, so a
# child process (backfill-points.sh -> set-points.sh) inherits them rather than
# re-resolving per issue. Override any of them in the environment to skip the
# lookup entirely.
#
# CACHING AND SUBSHELLS: a cache set inside `$(...)` dies with that subshell, so
# reading these as `X=$(project_id)` caches nothing on its own. Every script
# therefore WARMS the lookups with plain calls (`project_id >/dev/null`) in its own
# shell first; after that the `$(...)` reads and any loop below cost nothing, and
# child processes inherit the exported values.

# Project NUMBER for PROJECT_TITLE. Empty if the project doesn't exist yet.
resolve_project_number() {
  if [ -z "${REQUEL_PROJECT_NUM:-}" ]; then
    REQUEL_PROJECT_NUM="$(gh project list --owner "$OWNER" --format json \
      | jq -r --arg t "$PROJECT_TITLE" '.projects[] | select(.title==$t) | .number' | head -1)"
    export REQUEL_PROJECT_NUM
  fi
  echo "$REQUEL_PROJECT_NUM"
}

# Project NODE id (PVT_…), which item-edit wants.
project_id() {
  if [ -z "${REQUEL_PROJECT_ID:-}" ]; then
    # Plain call, not $(resolve_project_number): a substitution would resolve the
    # number in a subshell and throw the cached value away, costing a second
    # `project list` when field_id resolves it again.
    resolve_project_number >/dev/null
    REQUEL_PROJECT_ID="$(gh project view "$REQUEL_PROJECT_NUM" --owner "$OWNER" \
      --format json | jq -r '.id')"
    export REQUEL_PROJECT_ID
  fi
  echo "$REQUEL_PROJECT_ID"
}

# Field NODE id by field name. The field list is fetched at most once per process;
# the two fields these scripts write are additionally cached in exported vars.
field_id() {      # usage: field_id "Story Points (Retro)"
  local name="$1" var=""
  case "$name" in
    "Story Points")         var=REQUEL_SP_FIELD_ID ;;
    "Story Points (Retro)") var=REQUEL_RETRO_FIELD_ID ;;
  esac
  if [ -n "$var" ] && [ -n "${!var:-}" ]; then
    echo "${!var}"
    return
  fi
  if [ -z "${REQUEL_FIELDS_JSON:-}" ]; then
    resolve_project_number >/dev/null
    REQUEL_FIELDS_JSON="$(gh project field-list "$REQUEL_PROJECT_NUM" --owner "$OWNER" \
      --limit 100 --format json)"
  fi
  local id
  id="$(jq -r --arg n "$name" '.fields[] | select(.name==$n) | .id' <<<"$REQUEL_FIELDS_JSON" \
    | head -1)"
  if [ -n "$var" ]; then
    printf -v "$var" '%s' "$id"
    export "$var"
  fi
  echo "$id"
}

# One query for every issue's state and labels, cached in a TSV that child
# processes inherit via REQUEL_ISSUE_INDEX. Replaces two gh calls per issue.
# Written to TMPDIR and deliberately not deleted: a child would otherwise remove
# the file its parent is still reading. It is a few KB and TMPDIR is transient.
load_issue_index() {
  if [ -n "${REQUEL_ISSUE_INDEX:-}" ] && [ -r "${REQUEL_ISSUE_INDEX}" ]; then
    return
  fi
  REQUEL_ISSUE_INDEX="$(mktemp "${TMPDIR:-/tmp}/requel-issue-index.XXXXXX")"
  export REQUEL_ISSUE_INDEX
  gh issue list --repo "$REPO" --state all --limit 1000 --json number,state,labels \
    | jq -r '.[] | [ (.number|tostring),
                     (.state|ascii_upcase),
                     (if any(.labels[]?; .name=="Epic") then "epic" else "-" end) ] | @tsv' \
    > "$REQUEL_ISSUE_INDEX"
}

# Column 2 = state, column 3 = epic marker. Empty when the number is not an issue
# in this repo (a PR number, say) — callers treat that as "not closed".
_issue_index_field() {   # usage: _issue_index_field 43 2
  load_issue_index
  awk -F'\t' -v n="$1" -v c="$2" '$1 == n { print $c; exit }' "$REQUEL_ISSUE_INDEX"
}

# Issue state: "OPEN" or "CLOSED" (uppercase). Used to gate retro writes —
# an issue must be done before it earns a retro value.
issue_state() {   # usage: issue_state 43
  _issue_index_field "$1" 2
}

# True (exit 0) if the issue carries the "Epic" label. Epics are rollup/container
# issues — their child sub-issues carry the effort — so they never earn a retro
# (or an initial estimate). Any epic, present or future, is caught by the label,
# so nothing here is hardcoded to #124.
is_epic() {   # usage: is_epic 124 && echo "it's an epic"
  [ "$(_issue_index_field "$1" 3)" = "epic" ]
}

# --- acknowledged retro overrides -------------------------------------------
# A recorded retro that differs from the calc is a deliberate override (CLAUDE.md:
# "Override only deliberately"). Left alone it reports DRIFT on every later audit,
# so the flag goes quiet through familiarity rather than through review. Listing it
# in retro-overrides.tsv pins the value that WAS reviewed: the audit can then say
# "differs, and someone looked" and keep DRIFT meaning "differs, and nobody has".
#
# The acknowledgement is (issue, value). If the board later holds a different retro
# the ack no longer applies and the row drifts again — silencing one reviewed number
# is the point; silencing an issue forever is not.
RETRO_OVERRIDES_FILE="${RETRO_OVERRIDES_FILE:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/retro-overrides.tsv}"

_retro_override_field() {   # usage: _retro_override_field 43 2
  [ -r "$RETRO_OVERRIDES_FILE" ] || return 0
  awk -F'\t' -v n="$1" -v c="$2" '$1 !~ /^#/ && $1 == n { print $c; exit }' \
    "$RETRO_OVERRIDES_FILE"
}

# The acknowledged retro for an issue, or empty when there is no acknowledgement.
retro_override() {          # usage: retro_override 43
  _retro_override_field "$1" 2
}

# Why it was acknowledged, for the audit to print.
retro_override_reason() {   # usage: retro_override_reason 43
  _retro_override_field "$1" 3
}

# Distinct days with a commit that claims issue <n> as its own work: the full issue
# URL at the START OF A LINE. Line 1 of every ticket commit here is that URL, and a
# GitHub squash merge re-emits it as "* <url>", so both forms count.
#
# The line anchor is load-bearing. Matching the URL anywhere in the message also
# counts a commit that merely *mentions* the issue in prose - a tooling commit whose
# body explains what went wrong with #26 scored a work-day against #26 and pushed it
# into DRIFT. Anchoring changes no other count on the 2.0 board (verified across every
# issue on it, calibration cases #38=39, #43=10, #69=4, #73=3 included).
commit_days() {   # usage: commit_days 43
  git -C "$REPO_DIR" log --all -E \
    --grep="^(\* )?https://github\.com/$REPO/issues/$1(\$|[^0-9])" \
    --format='%ad' --date=short 2>/dev/null | sort -u | grep -c . || true
}

# Snap a day count to the nearest Fibonacci rung (ties round up).
# Zero is a value, not a rung: an issue closed with no committed work scores 0,
# so callers can record "done, cost nothing" instead of leaving the field blank.
snap_fib() {      # usage: snap_fib 4 -> 5 ; snap_fib 0 -> 0
  local d="$1"
  [ "${d:-0}" -le 0 ] && { echo 0; return; }
  local fib=(1 2 3 5 8 13 21 34 55 89); local best=0 bd=999999 diff
  for f in "${fib[@]}"; do
    diff=$(( f > d ? f - d : d - f ))
    if [ "$diff" -lt "$bd" ] || { [ "$diff" -eq "$bd" ] && [ "$f" -gt "$best" ]; }; then
      bd="$diff"; best="$f"
    fi
  done
  echo "$best"
}

# Closed issue numbers in the release's milestone, newest-closed first.
milestone_closed_issues() {
  gh issue list --repo "$REPO" --milestone "$MILESTONE" --state closed --limit 300 \
    --json number,closedAt --jq 'sort_by(.closedAt) | reverse | .[].number'
}

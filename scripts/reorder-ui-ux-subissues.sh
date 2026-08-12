#!/usr/bin/env bash
#
# reorder-ui-ux-subissues.sh
# Reorders the sub-issues of the UI/UX epic (#124) into the intended build order
# and (optionally) rewrites the "Remediation rollup by phase" comment to match.
#
# doc/124-remediation-rollup.md is the single source of truth. The build order is
# the order its `- [ ] #NNN` / `- [x] #NNN` lines appear, top to bottom.
#
# Why this exists: sub-issues were added in creation order (#125..#147, then
# #154..#159, then the #132 follow-ons #171/#172/#173/#176), so the epic's native
# list does not read in dependency order. Phase 3 is the sharp case — the shared
# app-field primitive (N5 #158) sat *after* the form tickets that depend on it,
# and the server-side backing tickets landed at the bottom regardless of when
# they need to ship.
#
# This script used to carry the order in a hand-maintained ORDER array, which
# silently went stale twice: it never learned about #171/#172/#173/#176, and its
# phase grouping still reflected the pre-N1..N6 roadmap. Deriving the order from
# the rollup doc removes the second copy, so the doc and the epic cannot drift
# apart without someone editing the doc.
#
# --sync-checks additionally reconciles the doc's checkboxes and progress line
# with live issue state before patching, which is the other half of the same
# drift: #171 and #173 shipped and stayed unchecked for weeks because nothing
# re-derived them.
#
# Requirements:
#   - bash 4+ (associative arrays, mapfile). macOS stock /bin/bash is 3.2 —
#     `brew install bash` and run under that, or run this on Linux.
#   - gh CLI, authenticated (gh auth status) with repo write access.
#   - The GitHub sub-issues REST API (issues/{n}/sub_issues[/priority]).
#
# Usage:
#   bash scripts/reorder-ui-ux-subissues.sh                          # reorder sub-issues only
#   bash scripts/reorder-ui-ux-subissues.sh --comment                # also patch the rollup comment
#   bash scripts/reorder-ui-ux-subissues.sh --sync-checks            # refresh the doc's boxes, then reorder
#   bash scripts/reorder-ui-ux-subissues.sh --sync-checks --comment  # the full housekeeping pass
#   DRY_RUN=1 bash scripts/reorder-ui-ux-subissues.sh --sync-checks --comment
#
# DRY_RUN=1 prints the write calls and leaves both GitHub and the doc untouched
# (with --sync-checks it prints a diff of what it would rewrite).
#
set -euo pipefail

REPO="${REPO:-rreganjr/Requel}"
EPIC="${EPIC:-124}"
COMMENT_ID="${COMMENT_ID:-5113771759}"          # the "Remediation rollup by phase" comment
COMMENT_FILE="${COMMENT_FILE:-doc/124-remediation-rollup.md}"
API_VERSION="2022-11-28"
DRY_RUN="${DRY_RUN:-0}"

if [[ "${BASH_VERSINFO[0]:-0}" -lt 4 ]]; then
  echo "!! This script needs bash 4+ (found ${BASH_VERSION:-unknown})." >&2
  echo "   macOS ships bash 3.2: brew install bash, then run it under that bash." >&2
  exit 1
fi

PATCH_COMMENT=0
SYNC_CHECKS=0
for arg in "$@"; do
  case "$arg" in
    --comment)     PATCH_COMMENT=1 ;;
    --sync-checks) SYNC_CHECKS=1 ;;
    -h|--help)     sed -n '2,45p' "$0"; exit 0 ;;
    *) echo "!! Unknown argument: $arg (see --help)" >&2; exit 2 ;;
  esac
done

if [[ ! -f "$COMMENT_FILE" ]]; then
  echo "!! $COMMENT_FILE not found — it is the source of the build order." >&2
  exit 1
fi

api() {  # thin wrapper so DRY_RUN prints instead of calling
  if [[ "$DRY_RUN" == "1" ]]; then
    printf 'gh api'; printf ' %q' "$@"; echo
  else
    gh api "$@"
  fi
}

# --- the epic's live sub-issues: number, state, database id in one call -----
# The priority endpoint addresses sub-issues by database id, not issue number,
# and this response already carries both — no per-issue lookups needed.

echo ">> Fetching sub-issues of #$EPIC ..." >&2
subs="$(mktemp)"; doc_nums="$(mktemp)"; states="$(mktemp)"
trap 'rm -f "$subs" "$doc_nums" "$states"' EXIT

gh api --paginate -H "X-GitHub-Api-Version: $API_VERSION" \
  "/repos/$REPO/issues/$EPIC/sub_issues" \
  --jq '.[] | "\(.number) \(.state) \(.id)"' > "$subs"

if [[ ! -s "$subs" ]]; then
  echo "!! No sub-issues found on #$EPIC." >&2
  exit 1
fi

declare -A ID STATE
FIRST_ID=""
while read -r num state id; do
  [[ -n "$num" ]] || continue
  ID["$num"]="$id"
  STATE["$num"]="$(printf '%s' "$state" | tr '[:lower:]' '[:upper:]')"
  [[ -z "$FIRST_ID" ]] && FIRST_ID="$id"
done < "$subs"

# --- build order, read from the rollup doc ---------------------------------

mapfile -t ORDER < <(sed -n 's/^- \[[ xX]\] #\([0-9][0-9]*\).*/\1/p' "$COMMENT_FILE")

if [[ "${#ORDER[@]}" -eq 0 ]]; then
  echo "!! No '- [ ] #NNN' checklist lines found in $COMMENT_FILE." >&2
  exit 1
fi

# --- sanity: the doc's set must equal the epic's set -----------------------

printf '%s\n' "${ORDER[@]}" | sort -n > "$doc_nums"
cut -d' ' -f1 "$subs" | sort -n > "$states"

if ! diff "$doc_nums" "$states" >/dev/null; then
  echo "!! $COMMENT_FILE does not match the epic's sub-issue set." >&2
  echo "   '<' = in the doc only, '>' = a sub-issue the doc never lists:" >&2
  diff "$doc_nums" "$states" >&2 || true
  echo "   Fix the doc (or the epic's sub-issues) and re-run." >&2
  exit 1
fi

dupes="$(uniq -d "$doc_nums" || true)"
if [[ -n "$dupes" ]]; then
  echo "!! $COMMENT_FILE lists these issues more than once: $(echo $dupes)" >&2
  exit 1
fi
echo ">> ${#ORDER[@]} sub-issues verified against $COMMENT_FILE." >&2

# --- optional: re-derive the doc's checkboxes and progress line ------------
# Closed sub-issues are checked, open ones are not. The trailing phase note on
# the progress line ("(Phase 1 done, ...)") is prose and is left alone.

if [[ "$SYNC_CHECKS" == "1" ]]; then
  echo ">> Syncing checkboxes in $COMMENT_FILE from live issue state ..." >&2
  : > "$states"
  for num in "${ORDER[@]}"; do
    printf '%s %s\n' "$num" "${STATE[$num]}" >> "$states"
  done

  synced="$(mktemp)"
  awk -v statefile="$states" '
    BEGIN {
      while ((getline line < statefile) > 0) {
        split(line, f, " "); st[f[1]] = f[2]
      }
    }
    {
      lines[NR] = $0
      if ($0 ~ /^- \[[ xX]\] #[0-9]+/) {
        match($0, /#[0-9]+/)
        num = substr($0, RSTART + 1, RLENGTH - 1)
        if (num in st) {
          total++
          box = (st[num] == "CLOSED") ? "[x]" : "[ ]"
          if (st[num] == "CLOSED") done++
          sub(/^- \[[ xX]\]/, "- " box, lines[NR])
        }
      }
    }
    END {
      for (i = 1; i <= NR; i++) {
        line = lines[i]
        if (line ~ /Progress: *[0-9]+ *\/ *[0-9]+ *complete/) {
          sub(/Progress: *[0-9]+ *\/ *[0-9]+ *complete/, \
              "Progress: " done " / " total " complete", line)
        }
        print line
      }
      printf(">> %d / %d closed.\n", done, total) > "/dev/stderr"
    }
  ' "$COMMENT_FILE" > "$synced"

  if diff -q "$COMMENT_FILE" "$synced" >/dev/null; then
    echo ">> Checkboxes already match GitHub; $COMMENT_FILE unchanged." >&2
    rm -f "$synced"
  elif [[ "$DRY_RUN" == "1" ]]; then
    echo ">> DRY_RUN — would rewrite $COMMENT_FILE:" >&2
    diff -u "$COMMENT_FILE" "$synced" >&2 || true
    rm -f "$synced"
  else
    mv "$synced" "$COMMENT_FILE"
    echo ">> $COMMENT_FILE rewritten (review the diff before committing)." >&2
  fi
fi

# --- reprioritize: place each item after its predecessor -------------------
# The priority endpoint moves one sub-issue relative to another. Chaining every
# item after the previous one fully determines the order (all items are in the
# chain, so nothing floats).

prev_id=""
for num in "${ORDER[@]}"; do
  id="${ID[$num]}"
  if [[ -z "$prev_id" ]]; then
    if [[ "$id" != "$FIRST_ID" ]]; then
      echo ">> move #$num to top (before current first)" >&2
      api -X PATCH -H "X-GitHub-Api-Version: $API_VERSION" \
        "/repos/$REPO/issues/$EPIC/sub_issues/priority" \
        -F sub_issue_id="$id" -F before_id="$FIRST_ID"
    fi
  else
    echo ">> place #$num after previous" >&2
    api -X PATCH -H "X-GitHub-Api-Version: $API_VERSION" \
      "/repos/$REPO/issues/$EPIC/sub_issues/priority" \
      -F sub_issue_id="$id" -F after_id="$prev_id"
  fi
  prev_id="$id"
done
echo ">> Sub-issue order updated." >&2

# --- optional: rewrite the rollup comment to match ------------------------

if [[ "$PATCH_COMMENT" == "1" ]]; then
  echo ">> Patching comment $COMMENT_ID from $COMMENT_FILE ..." >&2
  api -X PATCH -H "X-GitHub-Api-Version: $API_VERSION" \
    "/repos/$REPO/issues/comments/$COMMENT_ID" \
    -f body="$(cat "$COMMENT_FILE")"
  echo ">> Comment updated." >&2
fi

echo ">> Done."

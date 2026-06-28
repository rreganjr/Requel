---
name: issue-retro
version: 2.0.0
description: Retroactively assign Story Points (Retro) to closed GitHub issues for a Requel release, derived from how many distinct days work was actually committed for each issue. Use whenever someone asks to "do an issue retro", "retro my issues", "add retro points", "score the closed issues for 2.0", "backfill story points for release X", "what should I point issue #N at", or wants to fill the Story Points (Retro) field on a release's GitHub Project. Takes a RELEASE as input (defaults to 2.0) and matches issues by milestone. Do NOT use this to set the initial Story Points estimate (that stays the developer's call / 0 when not pre-estimated), to point still-open work, or to write any value without review — it always proposes numbers first and writes only what is approved. Even if someone just says "what's the retro for #43?", use this skill.
---

# Issue Retro (GitHub Projects, per-release)

Find closed Requel issues in a release that are missing a **Story Points (Retro)** value, propose one for
each from the **number of distinct days work was committed referencing that issue**, review the numbers,
and write only the approved values into that release's GitHub Project.

The retro value is **measured from git history, not guessed** — that's what makes it trustworthy for a
points-based retro. Validated by hand against `rreganjr/Requel` in June 2026.

## Project model: one project per release, matched by milestone

A single **RELEASE** input drives everything by naming convention:

| Input | Derives | Example (RELEASE=2.0) |
|---|---|---|
| `RELEASE` | — | `2.0` |
| milestone | `v<RELEASE>` | `v2.0` |
| project title | `Requel <RELEASE>` | `Requel 2.0` |

So you only ever pass the release; the milestone (which issues belong to it) and the project (where points
get written) follow. All three are overridable via env vars (`REQUEL_RELEASE`, `REQUEL_MILESTONE`,
`REQUEL_PROJECT_TITLE`) if naming ever diverges. Config + helpers live in `scripts/retro-lib.sh`.

## Why commit-days

Ron commits every day he works on an issue, and this repo's commit convention puts the **full issue URL**
(`https://github.com/rreganjr/Requel/issues/N`) on commit line 1. So "distinct days with a commit
referencing issue N" is a faithful proxy for "days of work on N". Ron's scale: **~1 working day = 1 story
point**, snapped to Fibonacci. Idle gaps drop out automatically — only days with a commit are counted.

## Core rules (defaults — confirm only if the user hints otherwise)

- **Scope:** **closed** issues whose **milestone = `v<RELEASE>`**. Matching is by milestone, not by date —
  so milestone hygiene matters (every release issue must have its milestone set). If milestones are spotty,
  fall back to the closed-date heuristic and say so.
- **Closed-only is enforced in code, not just here.** Retro is for finished work — an open issue must never
  get a retro value. `set-points.sh` checks `issue_state` and refuses to write retro on an OPEN issue (it
  still sets the initial estimate if asked). `backfill-points.sh` only ever selects `--state closed`. If
  you find open issues with retro values (e.g. set before this guard existed, or reopened later), run
  `scripts/clear-open-retros.sh <release>` to clear them.
- **Health check:** `scripts/audit-retros.sh <release>` is a read-only pass that flags VIOLATION (open
  issue with a retro), MISSING (closed issue with none), and DRIFT (recorded retro ≠ current commit-day
  calc). Run it first when asked to "check" or "audit" the board, then act with the clear/backfill scripts.
- **Field to write:** **Story Points (Retro)** only. Do **not** touch the initial **Story Points** field —
  it's the pre-work estimate and stays **0** unless the developer gives a real upfront number.
- **Retro method:** `retro = snap_fib(commit_days(issue))`. See `retro-lib.sh`.
- **Epics:** umbrella issues spanning many phases (e.g. **#38**, Echo2→Angular, ~39 commit-days → 34) blow
  past the normal ceiling. Flag them; offer 34 or leave blank/tag rather than treating them as a story.
- **Review gate:** always present proposed numbers and get explicit approval before any write. Only a
  scheduled propose-only run is exempt (see below).

## Prerequisites (one-time)

1. **`gh` with a CLASSIC token**, scopes **`project` + `repo` + `read:org`**. Fine-grained tokens cannot
   write user-owned Projects v2 (read works, `createProjectV2Field`/`item-edit` fail with "Resource not
   accessible by personal access token"). `read:org` is what lets `gh project` resolve the owner ("unknown
   owner type" = it's missing). If `GH_TOKEN` is set, it overrides `gh auth login` — update the env value.
2. **The release's project + fields exist.** If not, run `scripts/setup-project.sh <release>` (creates
   "Requel <release>", adds the two NUMBER fields, links the repo). It's idempotent and reuses an existing
   same-titled project.

Verify access first:

```bash
gh project list --owner rreganjr        # lists projects without a permission error
```

## Procedure

All commands run from the repo root. Source the shared helpers once:

```bash
. scripts/retro-lib.sh          # exports RELEASE/MILESTONE/PROJECT_TITLE + functions
# override the release for this run if needed:
# REQUEL_RELEASE=2.1 . scripts/retro-lib.sh
```

### 1. Candidate set — closed issues in the milestone
```bash
milestone_closed_issues          # issue numbers, newest-closed first
# detail view for the proposal table:
gh issue list --repo "$REPO" --milestone "$MILESTONE" --state closed --limit 300 \
  --json number,title,closedAt --jq '.[] | "\(.number)\t\(.closedAt[0:10])\t\(.title)"'
```

### 2. Skip issues that already have a retro value
```bash
NUM=$(resolve_project_number)
gh project item-list "$NUM" --owner "$OWNER" --format json \
  | jq -r '.items[] | select(.content.type=="Issue")
           | "\(.content.number)\t\(.["story Points (Retro)"] // "—")"'
```
**Key-name gotcha (verified June 2026):** in `gh project item-list --format json`, custom-field values are
top-level keys with only the **first letter lowercased** — so "Story Points (Retro)" is the key
`story Points (Retro)` (capital P), and "Story Points" is `story Points`. All-lowercase keys match nothing.
Only propose for issues where it's empty (`—`).

### 3. Propose
For each candidate: `days=$(commit_days N)`, `retro=$(snap_fib $days)`. Present:

| Issue | Title | Closed | Commit-days | Proposed Retro | Note |
|---|---|---|---|---|---|

Link each as `https://github.com/rreganjr/Requel/issues/N`. Flag epics. **Write nothing yet.**
Reference points (June 2026): #40=1d→1, #73=3d→3, #69=4d→5, #43=10d→8, #38=39d→34.

### 4. Review
Ask the user to approve, adjust, or drop rows. Offer all-or-subset. Honour scale tweaks.

### 5. Apply approved values
```bash
./scripts/set-points.sh <issue#> 0            # initial 0, retro auto-computed
./scripts/set-points.sh <issue#> 0 <retro>    # override the computed retro
```
`set-points.sh` writes to the project for the current `REQUEL_RELEASE`. To backfill an entire release at
once: `./scripts/backfill-points.sh <release>` (loops the milestone's closed issues).
Leave **initial** Story Points at **0** unless the user gives an actual pre-work estimate.

### 6. Summarize
List what was set (issue → retro), what was skipped (already valued, or no milestone), and any epics.

## Weekly scheduled run (propose-only)

Scheduled runs **must not auto-write**. Produce the proposal table (steps 1–3) for the release's closed
issues missing a retro value, and tell the developer to reply in a session to approve and apply.

Suggested prompt:
> "Run the issue-retro skill for release 2.0 in propose-only mode: list closed issues in milestone v2.0
> missing Story Points (Retro), with commit-day counts and proposed Fibonacci values. Do not write."

## Notes / lessons

- Commit convention is the **full issue URL**, not `#N`. Grepping `#38` misses everything and false-matches
  `issues/40`; always match `issues/N` with a word boundary (the lib does this).
- Migration work (#38) was committed incrementally, not squashed — 82 commits across 39 days all carry the
  `issues/38` URL, so the commit-day count captures it correctly.
- Commit-days can **double-count** a calendar day across two issues touched the same day. Fine per-issue;
  don't treat the column sum as exact person-days.
- New release: `make a milestone v<X>`, set it on that release's issues, then
  `./scripts/setup-project.sh <X>` and `./scripts/backfill-points.sh <X>`. Nothing else changes.

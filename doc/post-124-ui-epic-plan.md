# Post-#124 UI Polish — Epic Plan

Follow-on epic to
[#124 — Angular UI/UX, Accessibility, and Front-End Architecture Remediation](https://github.com/rreganjr/Requel/issues/124),
covering UI issues found walking the app after #124 closed. Source findings:
`doc/post-124-ui-punch-list.md` (items L1–L8, T2–T3, S1, D1, F1, A1).

This is a planning doc only — it changes no GitHub state. The developer runs every
`gh`/`git` command; Claude prepares them. Conventions match #124: parent = the new epic,
milestone `v2.0`, project **Requel 2.0** (#2), labels `ui-ux-review` + `priority:{high,medium,low}`.

## Ticket granularity

**Six sub-issues, not thirteen.** The ticket unit is the candidate group (I1–I6), because
each group is one cohesive per-component PR. Bundled items share a file and ship together;
splitting them would create multiple tiny PRs against the same file. Items that could later
split out if they grow are noted (e.g. L4).

## Phases and sub-issues

Order = build order (top-to-bottom), same as the #124 rollup. Estimates are **proposed**
Story Points (Fibonacci 1/2/3/5/8), to confirm.

### Phase 1 — Bug fixes (do first — users hit these now)

| Ticket | Title | Items | Files | Shared? | Est. |
|---|---|---|---|---|---|
| I7 | Breadcrumb project-link fix + editor quick-nav decision | B1, B2 | `breadcrumb.ts`, `editor-actions.ts` | shell/editor nav | 2 |

### Phase 2 — Shared primitives (broad payoff, no dependencies)

| Ticket | Title | Items | Files | Shared? | Est. |
|---|---|---|---|---|---|
| I4 | Data-table: scrollable body with pinned header + paginator | D1 | `app-data-table.ts` | yes — ~13 list pages | 3 |
| I5 | Textarea sizing default | F1 | `app-field` / global styles + editor sweep | yes — all editors | 3 |

### Phase 3 — App shell & scroll/viewport model

| Ticket | Title | Items | Files | Shared? | Est. |
|---|---|---|---|---|---|
| I2 | App shell / top bar polish | L4, T2, T3 | `layout.ts` | shell-wide | 3 |
| I3 | Sidebar per-section scroll regions | S1 | `sidebar-nav.ts` + `layout.ts` | shell-wide | 5 |

### Phase 4 — Screen polish

| Ticket | Title | Items | Files | Shared? | Est. |
|---|---|---|---|---|---|
| I1 | Login screen overhaul | L1, L2, L3, L5, L6, L7, L8 | `login.ts` (+ global p-password rule) | L6 width fix is shared | 5 |

### Phase 5 — Enhancement

| Ticket | Title | Items | Files | Shared? | Est. |
|---|---|---|---|---|---|
| I6 | Annotations collapse/expand | A1 | `annotations-section.ts` | per-entity | 3 |

**Proposed total: 24 points.**

## Sequencing rationale

- **Phase 1 (bug fix) first** — I7's breadcrumb bug breaks a navigation path users hit today;
  fix it before the polish work.
- **Phase 2** — I4/I5 are pure shared primitives with zero dependencies and the
  broadest payoff (one change touches every list / every editor), lowest risk.
- **Phase 2** — the cross-cutting *scroll & viewport* theme: T2 (sticky header) establishes
  the "shell fixed, inner regions scroll" model that S1 (sidebar) and D1 (already shipped in
  Phase 1 via `scrollHeight="flex"`) assume. Build these with one mental model.
- **Phase 3** — I1 (login) is self-contained and independent of the scroll model; it's placed
  after the shared work but could move earlier if login polish is a priority.
- **Phase 4** — I6 is an independent enhancement; do anytime.

Notes:
- L4 confirmed cause: CDK `LiveAnnouncer`'s `.cdk-visually-hidden` region has no clipping CSS
  because the CDK a11y visually-hidden styles were never included. One-rule fix in `styles.scss`
  (include CDK's `a11y-visually-hidden` mixin). ~10 min → stays bundled in I2. Note it fixes
  *all* announcements rendering visibly, not just the sidebar's "Project list updated."
- I1's L6 "p-password renders short" fix should be a **global** rule so `edit-account.ts` and
  `user-editor.ts` benefit; only the toggle/equal-length layout is login-local.
- I7 B1 is a confirmed bug: the breadcrumb pre-joins already-encoded URL segments into a string
  and hands it to `routerLink`, which double-encodes it — the fix is a routerLink **commands
  array of decoded segments** (as editor-actions and the workspace cards already do). B2 is the
  decision to keep/restyle/remove the editor quick-nav links, now that B1 makes the breadcrumb
  project link work.

## GitHub setup — developer-run command plan

Numbers below are assigned by GitHub at creation. Create the **epic first**, capture its
number as `EPIC`, then create sub-issues and capture each number. `release/2.0` conventions
apply. (All commands run by the developer.)

```bash
REPO=rreganjr/Requel

# 1. Create the epic
gh issue create --repo $REPO \
  --title "Post-#124 UI polish" \
  --label ui-ux-review \
  --milestone v2.0 \
  --body-file doc/post-124-ui-epic-body.md    # (draft this from the phase list once agreed)
# -> record the number as EPIC

# 2. Create the six sub-issues (one per group). Title format "<concise>", body from a
#    per-ticket section (draft under doc/ per the #124 pattern). Capture each number.
#    Suggested labels: I4/I5/I1/S1 high or med per punch list; set priority:* to match.
gh issue create --repo $REPO --title "Data-table: scrollable body with pinned header + paginator" \
  --label ui-ux-review --label priority:high --milestone v2.0 --body "..."   # I4
gh issue create --repo $REPO --title "Textarea sizing default (shared app-field)" \
  --label ui-ux-review --label priority:medium --milestone v2.0 --body "..." # I5
gh issue create --repo $REPO --title "App shell / top bar polish (sticky header, CDK announcer visually-hidden fix, remove search)" \
  --label ui-ux-review --label priority:medium --milestone v2.0 --body "..." # I2
gh issue create --repo $REPO --title "Sidebar per-section scroll regions" \
  --label ui-ux-review --label priority:high --milestone v2.0 --body "..."   # I3
gh issue create --repo $REPO --title "Login screen overhaul" \
  --label ui-ux-review --label priority:high --milestone v2.0 --body "..."   # I1
gh issue create --repo $REPO --title "Annotations collapse/expand" \
  --label ui-ux-review --label priority:medium --milestone v2.0 --body "..." # I6

# 3. Link each as a sub-issue of the epic (GitHub sub-issues REST API, as #124 uses)
#    for each child number CHILD:
#    gh api repos/$REPO/issues/$EPIC/sub_issues -f sub_issue_id=<node-or-num per API>
#    (mirror the exact call scripts/reorder-ui-ux-subissues.sh already uses)

# 4. Add all seven (epic + 6) to project #2 and set Story Points (estimate)
OWNER=rreganjr; NUM=2
PROJECT_ID=$(gh project view "$NUM" --owner "$OWNER" --format json | jq -r '.id')
EST_ID=$(gh project field-list "$NUM" --owner "$OWNER" --limit 100 --format json \
         | jq -r '.fields[] | select(.name=="Story Points") | .id')
# for each issue N with estimate E:
#   ITEM_ID=$(gh project item-add "$NUM" --owner "$OWNER" --url https://github.com/$REPO/issues/N --format json | jq -r '.id')
#   gh project item-edit --project-id "$PROJECT_ID" --id "$ITEM_ID" --field-id "$EST_ID" --number E
```

## Rollup + tracking

Mirror #124: once numbers exist, add a `doc/post-124-ui-rollup.md` (build-order checklist by
phase) as the source of truth, and — if wanted — a rollup comment on the epic. The existing
`scripts/reorder-ui-ux-subissues.sh` is hard-wired to #124; adapting it (or a copy) to the new
epic number is optional and can wait until the epic exists.

## Decisions (confirmed 2026-08-29)

1. **Epic title:** "Post-#124 UI polish".
2. **Phases:** as laid out — Phase 1 shared primitives (I4, I5), Phase 2 shell/scroll (I2, I3),
   Phase 3 login (I1), Phase 4 annotations (I6).
3. **Estimates:** I7=2, then 3/3/3/5/5/3 = 24 points total.
5. **I7 added (2026-08-29):** breadcrumb bug (B1) + editor quick-nav decision (B2), placed first
   as Phase 1 bug fixes ahead of the polish work.
4. **L4:** stays bundled in I2 — confirmed a ~10-min one-rule CSS fix (missing CDK
   visually-hidden styles), not worth a separate 20-min CI run.

## Next step

Draft the epic body file (`doc/post-124-ui-epic-body.md`) and the six per-ticket bodies, then
hand the developer the finalized `gh` command sequence above with real titles/bodies filled in.

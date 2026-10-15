# #226 — Annotations: collapse/expand — plan

Ticket: [#226](https://github.com/rreganjr/Requel/issues/226) (I6, Phase 5 of the
Post-#124 UI polish epic #219). Punch-list item A1. Branch: `226-annotations-collapse`
off `release/2.0`. Frontend-only, single component.

## Scope / locked decisions

The annotations section (`annotations-section.ts`) renders every issue fully expanded —
issue → resolution → positions → arguments — so a handful of issues stacks into a very tall
block. Add a disclosure layer over the existing tree, styling unchanged:

- **(a) Per-issue collapse/expand.** A chevron toggle on each issue row hides that issue's
  body (resolution row, positions, arguments, and the inline add-position / add-argument
  forms), leaving just the one-line issue row. Default stays fully expanded (today's
  behavior).
- **(b) Collapse-all / expand-all.** One header button toggles every issue at once. When
  collapsed, each issue is its single issue row with the text on one line, truncated with an
  ellipsis. The button label/icon flips between "Collapse all" and "Expand all".

Notes are already single-row, so collapse applies to **issues** only; the collapse-all button
shows only when there is at least one issue, and is available to read-only users too (it is a
view convenience, not an edit).

## Changes (all in `annotations-section.ts`)

### State (signals)

- `collapsedIssueIds = signal<Set<number>>(new Set())` — ids of collapsed issues (default
  empty = all expanded).
- `isCollapsed(id)` — membership check used in the template.
- `toggleIssue(id)` — add/remove one id (new Set each time for OnPush).
- `allIssuesCollapsed = computed(...)` — true when there are issues and every one is collapsed.
- `toggleAll()` — collapse every current issue id, or clear the set when already all collapsed.

Stale ids for deleted issues are harmless (membership-only) and ignored by `allIssuesCollapsed`
(which reads the current issue list), so no pruning needed on reload; a collapsed issue stays
collapsed across the reloads that follow add/delete actions.

### Template

- **Header:** add a `Collapse all` / `Expand all` `p-button` (text, secondary,
  `data-testid="annotation-collapse-all"`, `aria-*`) shown when `annotations().issues.length > 0`,
  beside the existing Add Note / Add Issue buttons but outside the `@if (canEdit)` guard.
- **Each issue:** prepend a chevron toggle `p-button`
  (`data-testid="annotation-toggle-issue"`, `pi-angle-right` when collapsed / `pi-angle-down`
  when open, `aria-expanded`, `aria-label`) to `.annotation-row`; wrap the issue body
  (resolution row, positions `@for`, add-position form, Add Position button) in
  `@if (!isCollapsed(issue.id))`. Add `[class.collapsed]="isCollapsed(issue.id)"` on
  `.issue-item`.

### Styles (add, nothing removed)

- `.annotation-text { min-width: 0; }` so it can shrink for ellipsis inside the flex row.
- `.issue-item.collapsed .annotation-row { flex-wrap: nowrap; }` and
  `.issue-item.collapsed .annotation-text { white-space: nowrap; overflow: hidden;
  text-overflow: ellipsis; }` — the one-line ellipsized summary. Existing rules untouched.

## Test plan (the verify gate)

- **Unit (annotations-section.spec, +new):** keep the 27 existing tests green; add: a chevron
  toggle hides/reveals an issue's positions; `collapse-all` collapses every issue (bodies gone)
  and flips to `expand-all`, which restores them; the collapse-all button is absent with zero
  issues.
- **Typecheck:** `tsc` app + spec.
- **Dev build (AOT):** compiles.
- **Manual/visual:** on an editor with a multi-issue annotation set, collapse one issue → only
  its row remains; collapse-all → every issue one ellipsized line; expand-all restores; adding a
  position/argument still works after expanding; read-only view can still collapse.
- **e2e:** annotation flows run in CI; the new chevron/body-gating can shift how Playwright
  reaches a position/argument control — read the report and fix real locator breakages only.

## Risks

- The collapse gate must not hide the add-position/add-argument forms while they're open — those
  live inside the issue body, so collapsing an issue mid-add hides the open form; acceptable
  (re-expand to continue), and the common path collapses issues you're done with.
- Ellipsis in a flex row needs `min-width: 0` on the text; included.
- Estimate: filed at 3 pts; single-component disclosure layer — should land at or under.

## AC mapping

- Issues collapse/expand individually → per-issue chevron + body `@if`.
- A collapse-all button toggles one-line ellipsized summaries → header button + `.collapsed`
  ellipsis styles.
- Existing styling preserved → only additive rules + a chevron; no existing rule changed.

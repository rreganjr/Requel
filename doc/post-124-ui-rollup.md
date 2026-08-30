## Post-#124 UI polish — rollup by phase

Build-order checklist for the **Post-#124 UI polish** epic. Grouped from
`doc/post-124-ui-punch-list.md` (items L1–L8, T2–T3, S1, D1, F1, A1, B1–B2) and planned in
`doc/post-124-ui-epic-plan.md`. Closed sub-issues are auto-checked; the rest are checked as
each PR squash-merges to `release/2.0`.

**This doc is the single source of truth for build order.** `scripts/reorder-post-124-ui-subissues.sh`
reads the order of the `- [ ] #NNN` lines below, top to bottom, and reorders the epic's
sub-issues (and `--sync-checks` re-derives the boxes) to match. The epic is #219; its
sub-issues (#220–226) are listed below.

### Phase 1 — Bug fixes

- [x] #220 — I7 Breadcrumb project-link fix + editor quick-nav decision (B1, B2) — 2

### Phase 2 — Shared primitives

- [x] #221 — I4 Data-table: scrollable body with pinned header + paginator (D1; shared, ~13 list pages) — 3
- [ ] #222 — I5 Textarea sizing default (F1; shared app-field / global styles) — 3

### Phase 3 — App shell & scroll/viewport model

- [ ] #223 — I2 App shell / top bar polish: sticky header, CDK announcer visually-hidden fix, remove search (L4, T2, T3) — 3
- [ ] #224 — I3 Sidebar: per-section scroll regions (S1) — 5

### Phase 4 — Screen polish

- [ ] #225 — I1 Login screen overhaul (L1, L2, L3, L5, L6, L7, L8) — 5

### Phase 5 — Enhancement

- [ ] #226 — I6 Annotations: collapse/expand (A1) — 3

**Progress: 2 / 7 complete** (proposed 24 points total).

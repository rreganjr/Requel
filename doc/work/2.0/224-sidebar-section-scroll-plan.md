# #224 — Sidebar: per-section scroll (accordion groups fill and scroll) — plan

Ticket: [#224](https://github.com/rreganjr/Requel/issues/224) (I3, Phase 3 of the
Post-#124 UI polish epic #219). Punch-list item **S1**. Branch:
`224-sidebar-section-scroll` off `release/2.0`. Frontend-only.

## Scope / locked decisions

- **Goal (S1):** with many projects the whole sidebar scrolls, so opening a project
  leaves the body scrolled off and the tree's tail sits below the fold. Make the sidebar a
  fixed-height column where each accordion **group scrolls its own body** instead of the
  whole sidebar scrolling. The tall group (Projects, holding the project tree) claims the
  leftover height and scrolls internally; short groups (Admin) keep their natural height.
- **Approach (confirmed): full flex-height, like I4's data-table.** A bounded-height flex
  column from `.sidebar` down through the PrimeNG accordion, with the Projects group's tree
  region scrolling. New/Import/List stay pinned above the scrolling tree.

## Why it needs a flex chain (and a PrimeNG-specific twist)

`.sidebar` today is `overflow-y:auto` — the whole sidebar is the scroller. To scroll each
group instead, the chain `.sidebar` → `app-sidebar-nav` (:host) → `.sidebar-nav-root` →
`.p-accordion` must become a bounded flex column with `min-height:0` at every level, and the
active Projects panel must be a `flex:1 1 0` column whose content region scrolls.

The twist (found in-browser): **PrimeNG 21's accordion collapse breaks under a flex layout.**
The panel content height is driven by `p-motion` measuring `offsetHeight` into a
`--pui-motion-height` var; once `.p-accordion` is `display:flex`, the content becomes a flex
item whose `flex-basis:auto` reads *max-content*, ignoring the collapsed grid track — so
closed panels stay full height. Overriding `grid-template-rows` or the motion var does not
win. The fix that does: explicitly force **inactive** panels' content to
`height:0; overflow:hidden` so a collapsed group renders as its header only. Open panels are
untouched, so PrimeNG's open animation still plays; close is effectively instant (acceptable).

## Changes

### `layout.ts` — `.sidebar` becomes a bounded flex column

Change `.sidebar` from `overflow-y:auto` to `display:flex; flex-direction:column;
overflow:hidden`. Width/flex-shrink/border/background unchanged. This is the bounded ancestor
the fill chain needs; the sidebar itself no longer scrolls.

### `sidebar-nav.ts` — component host + root flex, accordion deep overrides

- `:host { display:flex; flex-direction:column; flex:1; min-height:0 }` (fills `.sidebar`).
- `.sidebar-nav-root` changes from `display:block` to the same flex column.
- `::ng-deep` rules scoped under `:host` for the PrimeNG accordion:
  - `.p-accordion` → flex column, `flex:1 1 auto`, `min-height:0`.
  - `.p-accordionpanel` → `flex:0 0 auto` (a group sizes to its header + open content).
  - **inactive panel content** (`:not(.p-accordionpanel-active) > .p-accordioncontent`) →
    `height:0; min-height:0; overflow:hidden` (collapse fix above).
  - **active Projects panel** (`[value="projects"].p-accordionpanel-active`) → `flex:1 1 0;
    min-height:0`; its `.p-accordioncontent` → `flex:1 1 0; min-height:0;
    grid-template-rows:1fr`; `p-motion` / `.p-accordioncontent-wrapper` →
    `height:100%; min-height:0; overflow:hidden`; `.p-accordioncontent-content` →
    `height:100%; min-height:0; overflow:hidden; display:flex; flex-direction:column` so it
    can pin the actions and hand the leftover height to the tree.
  - Inside that content: `.panel-actions` → `flex:0 0 auto` (pinned); `p-tree` →
    `flex:1 1 0; min-height:0; overflow-y:auto` (the group's own scrollbar).

The Projects group is targeted by its `value="projects"` (co-located with the template in the
same file). Admin (short, fixed set of links) keeps natural height; a non-admin user sees only
the Projects group, which fills the sidebar.

## Test plan (the verify gate)

- **Unit:** existing sidebar-nav spec stays green (no DOM/logic change; this is CSS-only).
  Add an assertion only if a cheap one fits (e.g. the root keeps its `sidebar-nav-root` class).
- **Typecheck:** `tsc` app + spec.
- **Dev build (AOT):** compiles the component.
- **Manual/visual (the real proof, done in-browser during design):** with many projects and
  both groups open — Admin sits at natural height, Projects fills the rest with New/Import/List
  pinned and only the tree scrolling; the sidebar as a whole does not scroll. Collapse Admin →
  Projects grows; collapse a group → header only; reopen → restores. No horizontal scroll.
- **e2e:** sidebar nav flows run in CI; a scroll container can change how Playwright reaches a
  deep project node — read the report, fix real locator breakages only.

## Risks

- The collapse fix depends on PrimeNG's accordion markup (`.p-accordioncontent`, `p-motion`,
  `.p-accordioncontent-content`). A PrimeNG major upgrade could change these class names — the
  rules are scoped and commented so a future upgrade knows where to look. Mitigated by the
  visual check across open/closed/reopened states.
- `grid-template-rows:1fr !important` on the active Projects content overrides PrimeNG's motion
  var for the open state; verified it does not fight the open animation (open still animates).
- Estimate: filed at 5 pts; the PrimeNG collapse investigation consumed real time but the code
  is CSS-only across two files — landing within estimate.

## AC mapping

- Opening a project no longer scrolls the sidebar body off → sidebar is fixed-height,
  `overflow:hidden`; only the tree region scrolls.
- Each section scrolls its own content; sections size to the screen → flex chain + inactive
  collapse; Projects fills leftover height, Admin natural.
- New/Import/List remain reachable while scrolling the tree → actions pinned, tree scrolls.

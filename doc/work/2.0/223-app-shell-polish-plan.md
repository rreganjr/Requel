# #223 — App shell / top bar polish — plan

Ticket: [#223](https://github.com/rreganjr/Requel/issues/223) (I2, Phase 3 of the
Post-#124 UI polish epic #219). Punch-list items **L4, T2, T3**. Branch:
`223-app-shell-polish` off `release/2.0`. Frontend-only.

## Scope / locked decisions

- **T2 (sticky header): already done by #221.** #221 changed `.layout` to `height:100vh`, so the
  shell is a fixed viewport and `.app-header` (flex-shrink:0, above the scrolling `main-content`)
  never scrolls. Verified in-browser during #221. **No change needed** — no `position:sticky`.
- **L4 (announcer text visible):** CDK `LiveAnnouncer` renders a `.cdk-visually-hidden` polite
  region, but the CDK a11y visually-hidden styles were never bundled, so the class has no clip and
  announcements ("Project list updated.", "This goal was updated.") render on-screen at narrow
  width. **Fix:** add `.cdk-visually-hidden` to the app's existing `.rq-visually-hidden` clip rule.
- **T3 (dead search placeholder):** remove the disabled "Search (coming soon)" button from the top
  bar. (Global artifact search is parked as a future idea — see the punch list.)

## Changes

- `styles.scss` — the visually-hidden rule becomes `.rq-visually-hidden, .cdk-visually-hidden { … }`
  (the same proven clip-rect pattern), so the CDK announcer region is clipped.
- `layout.ts` — delete the `<button … data-testid="header-search">` search placeholder from
  `.header-right` (appearance menu + account menu remain).
- `layout.spec.ts` — the top-bar test drops the disabled-search assertions and instead asserts
  `header-search` is absent.

## Test plan (the verify gate)

- **Unit:** `layout.spec.ts` green (updated search assertion). tsc app + spec.
- **Dev build (AOT):** compiles.
- **Visual (pending `ng serve` restart):** trigger an announcement (e.g. save an entity, or the
  sidebar "Project list updated." on an SSE event) at narrow width — the announcer text no longer
  appears on-screen; the top bar stays fixed while content scrolls; no search button.
- **e2e:** any spec asserting the search placeholder is updated in CI if present.

## Out of scope / notes

- Global artifact search (future feature, parked).
- The dev-server visual check for L4 was deferred because `ng serve` went down mid-session; the fix
  is code-verified (dev build + unit) and is a copy of the working `.rq-visually-hidden` rule.

## AC mapping

- L4 → CDK announcer region clipped (a `.cdk-visually-hidden` probe computes to position:absolute,
  1px — verify on a live server; unit/build confirm the rule ships).
- T2 → header already fixed (from #221); no code change.
- T3 → search placeholder removed (spec asserts absence).

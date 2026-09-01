# Bundle & dependency posture

How we measure the Angular production bundle, the budgets that gate it, and the
convention that keeps it small (issue #147, Finding 5.6).

## Dependencies
The runtime dependency surface is intentionally small: Angular, Angular CDK,
PrimeNG, PrimeIcons, RxJS. Testing (Playwright, Vitest) and lint
(eslint / typescript-eslint) are dev-only. Keep it that way — a new runtime
dependency is a bundle decision, not just a code decision.

## Measuring the bundle

```bash
cd requel-angular
npm run build:stats      # ng build --stats-json -> dist/requel-angular/stats.json
```

The production build prints a per-chunk size table (raw + estimated transfer).
For a visual treemap, upload the emitted `stats.json` to
<https://esbuild.github.io/analyze/> (the app builder is esbuild-based; this is
the esbuild metafile analyzer). A plain `npm run build` prints the same size
table without the stats file.

## Budgets & the release gate (AC2)
Production budgets live in `angular.json` (`configurations.production.budgets`):

| Budget | Warning | Error |
|--------|---------|-------|
| initial | 1 MB | 2 MB |
| anyComponentStyle | 4 kB | 8 kB |

These are **enforced in CI**: `mvn verify` runs the frontend-maven-plugin, which
runs `npm run build` (`ng build`, production by default). A bundle over an
**error** budget fails the build; a bundle over a **warning** budget prints a
`Warning` line in the build log but does not fail. So a regression past 2 MB
initial breaks CI; a regression past 1 MB shows up as a warning to act on. The
GitHub Actions lint step (below) additionally guards the import convention.

## Tree-shakeable PrimeNG imports (AC3)
Always import PrimeNG from its **per-component entrypoint** — `primeng/button`,
`primeng/table`, `primeng/api` — never the bare `primeng` barrel, which pulls
the whole library in and defeats tree-shaking.

This is enforced by ESLint (`eslint.config.mjs`): a `no-restricted-imports` rule
errors on `from 'primeng'`. Run it locally with `npm run lint`; CI runs it too
(`.github/workflows/ci.yml`, "Angular lint" step). The config is deliberately
scoped to this one guardrail so it stays green on the existing code; broadening
the ruleset is its own future lint-adoption task.

## Baseline (production, 2026-08-31, after #217)
Captured with `npm run build:stats`:

- **Initial total: 722.14 kB raw / 172.98 kB estimated transfer** — ~72% of the
  1 MB warning budget, ~278 kB of headroom.
- Largest lazy chunks: ~283.8 kB (shared vendor chunk, unnamed — see below),
  the authenticated shell chunk (layout + sidebar, now lazy), scenario-editor
  ~95 kB, the SSR/`browser` chunk ~66 kB, use-case-editor ~32 kB, and the
  remaining feature editors 13–22 kB each.

### How #217 restored the headroom
The prior baseline (929.59 kB on 2026-08-28, and ~976 kB by the end of the
Post-#124 UI-polish epic) carried the **entire authenticated shell in the initial
bundle**: `app.routes.ts` statically imported `LayoutComponent` and
`DashboardComponent`, pulling the sidebar Tree, accordion, scroller, top-bar Menu,
Toast, and the appearance-menu Dialog into first paint — even for a logged-out user
at the login form. Making **layout + dashboard lazy** (`loadComponent`) while keeping
**login eager** moved that shell into a chunk that loads at login. One-line-per-route
change, −254 kB initial, no UX cost (login still paints instantly; the shell chunk
loads on the login → dashboard navigation).

**Convention:** keep the authenticated shell and every feature route lazy. Only the
login route (first paint) and the framework/theme providers belong in the initial
bundle. A new eager `component:` on a route is a bundle decision — prefer
`loadComponent`.

### The ~284 kB shared lazy vendor chunk
Investigated under #217. It is `primeng/table` (~95 kB) + **`primeng/datepicker`
(~81 kB)** + `@primeuix/styles` (~48 kB) + paginator/selectbutton/togglebutton/
radiobutton + table icons, shared by ~22 list/editor routes. The datepicker is the
fattest piece and has **zero direct app usage** — PrimeNG's Table imports it
transitively for its column-filter UI, so it cannot be split out at the app level
without PrimeNG-internal changes. It is lazy (never in the initial bundle), so it does
not affect the gated budget; left as a documented finding rather than a risky split.

## Findings become follow-ups (AC4)
When a build shows a chunk crossing a budget — or trending toward one — file a
GitHub issue with a **concrete target**, don't just note it here. Current state:
nothing is over budget, but the initial bundle's thin headroom is worth a
tracked follow-up (target: restore headroom by investigating the ~290 kB shared
lazy vendor chunk and keeping initial under ~850 kB). That follow-up was filed as
**#217 and is now resolved** — see the baseline section above: initial is back to
722 kB (well under 850) by lazy-loading the authenticated shell, and the shared
lazy vendor chunk was investigated (its bulk is `datepicker`, pulled transitively by
`table`; lazy, not app-removable).

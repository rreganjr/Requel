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

## Baseline (production, 2026-08-28)
Captured with `npm run build:stats`:

- **Initial total: 929.59 kB raw / 217.46 kB estimated transfer** — within both
  budgets (warning 1 MB, error 2 MB).
- Largest lazy chunks: ~290.6 kB (shared vendor chunk, unnamed), scenario-editor
  97.8 kB, the SSR/`browser` chunk 67.8 kB, use-case-editor 32.4 kB, and the
  remaining feature editors 15–22 kB each.

**Watch item:** the initial bundle sits at ~91% of the 1 MB warning budget, so
there is little headroom. See AC4 below.

## Findings become follow-ups (AC4)
When a build shows a chunk crossing a budget — or trending toward one — file a
GitHub issue with a **concrete target**, don't just note it here. Current state:
nothing is over budget, but the initial bundle's thin headroom is worth a
tracked follow-up (target: restore headroom by investigating the ~290 kB shared
lazy vendor chunk and keeping initial under ~850 kB). That follow-up is proposed
alongside this ticket; link it here once filed.

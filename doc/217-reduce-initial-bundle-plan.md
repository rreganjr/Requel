# #217 — Reduce initial bundle headroom — plan

Ticket: [#217](https://github.com/rreganjr/Requel/issues/217) (5.6 follow-on to #147, under
the UI/UX remediation epic #124). Branch: `217-reduce-initial-bundle` off `release/2.0`.
Frontend-only. Reference: `doc/BUNDLE_AND_DEPENDENCIES.md`.

## Targets (from the ticket)

1. Production initial bundle under ~850 kB raw (restore >150 kB headroom to the 1 MB budget).
2. Investigate the ~290 kB shared lazy vendor chunk; split/defer what isn't needed on first paint.

## Diagnosis (measured, `npm run build:stats` + stats.json analysis)

- **Initial was 976.80 kB** raw / 228.71 kB transfer — ~95% of the 1 MB warning budget (up from
  the 929.59 kB #147 baseline; the Post-#124 epic added ~47 kB).
- Composition of the initial bundle: framework (@angular/core+router+common+forms ~315 kB),
  PrimeNG theme runtime (@primeuix/themes+styles+utils+styled ~200 kB), and **PrimeNG components
  ~298 kB**. `provideAnimationsAsync()` is already in use (animations are 0 kB in initial).
- **Root cause:** `app.routes.ts` statically imports `LoginComponent`, `LayoutComponent`, and
  `DashboardComponent`. The authenticated shell (`LayoutComponent` → sidebar Tree 40 kB,
  accordion 12, scroller 24, top-bar Menu 22, Toast 14, appearance-menu Dialog 24, …) is
  therefore in the **initial** bundle even for a logged-out user at the login form. Feature
  editors are already lazy; the shell is not.
- **The ~284 kB shared lazy chunk** = `table` 95 + **`datepicker` 81** + `@primeuix/styles` 48 +
  paginator/toggles, shared by ~22 routes. `datepicker` has zero direct app usage — Table pulls
  it transitively for column-filter UI; not app-removable, and lazy (not in the initial budget).

## Change

`app.routes.ts` only: make the authenticated shell lazy, keep login eager.

- Drop the static `import { LayoutComponent }` and `import { DashboardComponent }`.
- `path: ''` → `loadComponent: () => import('./features/auth/layout').then(m => m.LayoutComponent)`.
- child `path: ''` → `loadComponent: () => import('./features/auth/dashboard').then(m => m.DashboardComponent)`.
- `LoginComponent` stays an eager `component:` so the login form (true first paint) is instant.

Everything else (auth guard, per-domain route spreads, feature editors) is unchanged and already
lazy. Result: the shell + its PrimeNG (Tree/accordion/scroller/Menu/Toast/Dialog) load as a chunk
on the login → dashboard navigation.

## Result (measured)

- **Initial 976.80 kB → 722.14 kB raw** (transfer 228.71 → 172.98 kB): **−254 kB**, ~72% of the
  1 MB budget, ~278 kB headroom. Target (1) met with margin.
- Target (2): the shared lazy chunk investigated and documented (datepicker via table; lazy,
  not app-removable) — no risky split attempted since it doesn't touch the gated budget.

## Test plan (the verify gate)

- **Unit:** `app.routes.spec` (7) and the auth specs — login/layout/dashboard (33) — stay green
  with the `component:` → `loadComponent` change. `tsc` app + spec.
- **Build:** `npm run build:stats` → confirm initial < 850 kB (measured 722 kB). (Build to a path
  outside the mounted folder to dodge the device bridge's no-delete EPERM on `dist/*.map`.)
- **Manual/visual (done):** at `/login` the form paints (eager); logging in loads the shell chunk
  and renders the dashboard ("Welcome, …") with the sidebar/top-bar — no chunk-load error.
- **e2e:** the login → app flow runs in CI and exercises exactly this route boundary.

## Risks

- Lazy shell adds one chunk fetch on the login → dashboard hop; invisible on a normal connection,
  and login itself is unaffected (eager). No provider moved (theme/animations/HTTP stay in
  `app.config.ts`, eager).
- A future eager `component:` on a route would silently re-inflate the initial bundle — captured
  as a convention in `BUNDLE_AND_DEPENDENCIES.md`.

## AC mapping

- Initial under ~850 kB raw → 722 kB via lazy shell (measured).
- Investigate the ~290 kB shared lazy chunk → documented (datepicker-via-table; lazy, not
  app-removable) in `BUNDLE_AND_DEPENDENCIES.md`.

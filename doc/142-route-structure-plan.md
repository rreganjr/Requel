# Implementation Plan — #142 5.1 Route structure & metadata

Part of the UI/UX remediation epic **#124**. Source: `doc/UI_UX_REVIEW.md` Finding 5.1. Phase 4.
Branch: `142-route-groups` off `release/2.0`. Priority: Medium, effort Medium (3–5 days).

> Behavior-preserving refactor: split the one flat route array into per-feature files and add
> route metadata (`title` + typed `data`). No route paths, guards, or lazy loading change. The
> metadata's eventual consumer is the app shell (#154); this ticket lays the typed groundwork and
> wires the one consumer that exists today — the document title.

## 0. Current state

`app.routes.ts` is a single flat `Routes` array: `login` (eager) + a `LayoutComponent` shell
(`authGuard`) whose children are ~28 lazy `loadComponent` routes, then `** → ''`. There is **no**
route `data` anywhere (only `sidebar-nav.ts` has unrelated `data`), and `provideRouter(routes)` has
**no `TitleStrategy`**. The shell (`features/auth/layout.ts`) renders a header + `app-sidebar-nav` +
content but has no breadcrumb/page-title bar — so nothing consumes route metadata yet.

## 1. Scope (locked decisions)

1. **Per-feature `*.routes.ts` files** composed by `app.routes.ts` (not domain arrays in one file).
2. **Full typed metadata now:** native `title` on every route (immediate document-title consumer via
   a `TitleStrategy`) **and** a typed `data` schema (`section`, `artifactType?`, `breadcrumb?`) on
   every route, as groundwork for #154.
3. **Static titles/breadcrumbs only.** Dynamic, param-derived labels (the project's name, an
   artifact's name) need resolvers + the breadcrumb component → **#154** (see §8).
4. **Strict parity:** identical paths, identical guards (`authGuard`/`adminGuard`/`dirtyCheckGuard`)
   on identical routes, identical lazy `loadComponent`, identical match order.

## 2. The `RouteData` contract

`requel-angular/src/app/core/route-data.ts`:

```ts
export type RouteSection = 'dashboard' | 'account' | 'admin' | 'project';
export type ArtifactType =
  | 'project' | 'goal' | 'story' | 'actor' | 'scenario' | 'use-case'
  | 'term' | 'report' | 'stakeholder' | 'open-issue' | 'user';

/** Typed route `data` the app shell (#154) consumes for section, breadcrumbs, and page chrome. */
export interface RequelRouteData {
  section: RouteSection;
  artifactType?: ArtifactType;   // list/editor routes; drives icon + breadcrumb noun in #154
  breadcrumb?: string;           // STATIC label only; param-derived labels are #154
  [key: string]: unknown;        // Angular Route.data is Data (index signature)
}
```

Routes are authored with a small helper so `data` stays type-checked despite `Route.data` being
loosely typed:

```ts
export function routeData(d: RequelRouteData): RequelRouteData { return d; }
```

## 3. Per-feature route files (the split)

New files under `requel-angular/src/app/features/**`, each exporting a `Routes` (or `Route[]`) const:

- **`account.routes.ts`** — `account` (dirtyCheck), `settings`.
- **`admin.routes.ts`** — `users`, `users/:username` (dirtyCheck), `global-tags`, `tag-categories`;
  every entry keeps `canActivate: [adminGuard]`.
- **`projects.routes.ts`** — `projects` (list); the `projects/:name/*` artifact list + editor pairs
  (stakeholders, goals, stories, actors, scenarios, use-cases, terms, reports) each with
  `canDeactivate: [dirtyCheckGuard]` on the editor; `projects/:name/open-issues`; and
  **`projects/:name` (project editor) LAST** (see §9 — order is load-bearing).

`app.routes.ts` becomes the composition root:

```ts
export const routes: Routes = [
  { path: 'login', component: LoginComponent, title: 'Sign in', data: routeData({ section: 'account' }) },
  {
    path: '', component: LayoutComponent, canActivate: [authGuard],
    children: [
      { path: '', component: DashboardComponent, title: 'Dashboard', data: routeData({ section: 'dashboard' }) },
      ...accountRoutes,
      ...adminRoutes,
      ...projectRoutes,   // MUST stay a single spread in this order; see §9
    ],
  },
  { path: '**', redirectTo: '' },
];
```

Each child route gains `title` + `data`, e.g. `projects/:name/goals/:goalId` →
`title: 'Goal'`, `data: routeData({ section: 'project', artifactType: 'goal' })`.

## 4. Titles (native `title` + `TitleStrategy`)

- `core/requel-title-strategy.ts`: `RequelTitleStrategy extends TitleStrategy` — `updateTitle(state)`
  reads `buildTitle(state)` and sets `Title.setTitle(t ? `${t} · Requel` : 'Requel')`.
- Provide it in `app.config.ts`: `{ provide: TitleStrategy, useClass: RequelTitleStrategy }`.
- Titles are static section/artifact nouns ("Goal", "Projects", "Users", "Account"). This gives the
  `title` half of AC-2 a real, testable consumer today without waiting on #154.

## 5. Guards & lazy — parity (AC-3, AC-4)

No guard or `loadComponent` changes. `authGuard` stays on the shell; `adminGuard` on every admin
route; `canDeactivate: [dirtyCheckGuard]` on the same editor routes it's on today. The split only
moves route objects into files and adds `title`/`data` keys.

## 6. Step-by-step (each its own PR to `release/2.0`, squash-merged)

- **Step 0 — route-config test first.** Add `app.routes.spec.ts` asserting today's behavior (paths,
  guards, order) against the *current* flat array, so the refactor is provably behavior-preserving.
- **Step 1 — `RouteData` + `TitleStrategy`.** Add the contract, the strategy, provide it; add
  `title`/`data` to the still-flat array. Extend the spec to assert `title`/`data.section` on each.
- **Step 2 — split into per-feature files.** Move routes into `account/admin/projects.routes.ts`;
  `app.routes.ts` composes. Spec unchanged and still green (that's the point).

## 7. Test plan

`app.routes.spec.ts` (new, AC-5):
- **Coverage:** flatten the composed config; assert the exact set of paths matches the expected list.
- **Guards:** shell has `authGuard`; each admin path has `adminGuard`; each editor path has
  `dirtyCheckGuard` in `canDeactivate`.
- **Metadata:** every child has a `title` and `data.section`; artifact routes carry `artifactType`.
- **Order:** `projects/:name` appears **after** every `projects/:name/*` route (index assertion).
- **Lazy:** each `loadComponent` is a function (not eagerly imported).

`requel-title-strategy.spec.ts`: `updateTitle` sets `document.title` from a snapshot title, and
falls back to `'Requel'` when a route has none.

Run: `bash tmp/143-verify.sh`-style — `npx tsc -p tsconfig.spec.json --noEmit` +
`npx ng test --include='src/app/app.routes.spec.ts'` + the title-strategy spec. Full `ng build`
once, since a broken lazy import only surfaces at build/route-load time.

## 8. Out of scope → #154

- **Dynamic titles/breadcrumbs** (project name, artifact name) — need route resolvers and the
  breadcrumb component; they belong with the shell that renders them (#154).
- **Rendering** the section/breadcrumb metadata (a page-title bar, grouped sidebar highlight) —
  #154 is the consumer. This ticket only *produces* the typed data.
- **Permissions in `data`** — AC-4 keeps the inline guards; no declarative-permission refactor here.

## 9. Risks & edge cases

- **Route order is load-bearing.** `projects/:name` (project editor) must stay **after** the
  `projects/:name/*` children. Angular matches leaf routes on the full remaining URL so a 3-segment
  URL won't match the 2-segment `projects/:name`, but the current file keeps it last deliberately —
  the split must preserve that order, and the spec pins it.
- **`Route.data` is loosely typed** (`Data`), so a typo in `section` won't fail the compiler on its
  own — the `routeData()` helper + the spec's `data.section` assertion are what catch it.
- **`**` wildcard and `login`** stay in `app.routes.ts`, not a feature file.
- **No behavior change is the acceptance bar:** if any existing e2e (`e2e/*.e2e.ts`) route
  navigation breaks, the split changed matching — treat as a regression, not a test to update.

## 10. Acceptance criteria (from #142)

1. Routes grouped by domain / easier to scan — per-feature `*.routes.ts` composed by `app.routes.ts`.
2. Route `data` for title, section, artifact type, breadcrumb — typed `RequelRouteData` on every
   route; `title` wired to a `TitleStrategy`.
3. Lazy `loadComponent` preserved.
4. `authGuard`/`adminGuard`/`dirtyCheckGuard` apply to the same routes.
5. Route changes covered by `app.routes.spec.ts` + `requel-title-strategy.spec.ts`.

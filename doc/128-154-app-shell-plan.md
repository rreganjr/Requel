# Implementation Plan — App shell: chrome (#154 N1) + project context/IA (#128 2.1)

Part of the UI/UX remediation epic **#124**, look-and-feel adoption (`doc/124-lookandfeel-plan.md`).
Combined chunk: **#154** (shell chrome, priority:medium) + **#128** (project context/IA, priority:high).
Phase 4. Branch: `128-154-app-shell` off `release/2.0`.

> These two were split in the epic as "shell-chrome half" (#154) and "project-context/IA half"
> (#128) of the same app shell. We build them together so the breadcrumb never ships in a
> static-only interim: #154 renders the top bar + breadcrumb + grouped sidebar, #128 wires the
> breadcrumb to dynamic route data and adds the workspace overview + editor-header context. Both
> issues stay open; each PR references the ACs it satisfies (see §11).

## 0. Current state

- **Shell** (`features/auth/layout.ts`): fixed 48px header (brand + a single "Menu" account
  dropdown — no back button, no breadcrumb, no search, no collapse toggle) over a 280px `aside`
  sidebar + `<main id="main-content" tabindex="-1">`. Skip-link + `<main>` focus target already
  present and asserted by `layout.spec.ts` (#135, closed). `.main-content` has **no** surface
  background (inherits) — the canvas token is not applied yet.
- **Sidebar** (`shared/sidebar-nav.ts`): a PrimeNG **accordion** with an Admin panel and a
  Projects panel; the Projects panel holds a `p-tree` mapping each project to its artifact
  sections. Expanded *project* names persist to `localStorage` under
  `requel_sidebar_expanded_projects`. This is the accordion/tree, **not** the target
  "grouped, labelled, collapsible sections with icon+label items + active-item highlight."
- **Routes** (post-#142): per-feature `*.routes.ts`; every route carries a native `title` and
  typed `RequelRouteData` (`section`, `artifactType?`, `breadcrumb?`). Titles/`data` are **static**
  — #142 explicitly deferred param-derived labels here. `RequelTitleStrategy` is live.
- **`/projects/:name`** currently maps to `ProjectEditorComponent` (dirtyCheck, matched **last**).
- **Dashboard** (`features/dashboard`) is a placeholder ("select a project").
- **`app-page-header`** (`shared/page-header.ts`) exists: an in-content `<h1>` + eyebrow + metadata
  slot. This is **not** the top-bar breadcrumb; the breadcrumb is new chrome that sits above it.
- **Tokens**: `--rq-header-bg` (= `--p-primary-900`), the locked Slate surface ramp
  (`theme/requel-preset.ts`, surface-50/100 = canvas), card tokens (`--rq-card-*`) all exist.

## 1. Scope (locked decisions)

1. **One branch, stacked PRs** (§9) to `release/2.0`, not one giant merge.
2. **Shell chrome (#154):** top bar (back + breadcrumb region left; search + account + sidebar
   collapse right), grouped collapsible sidebar (labelled sections, icon+label items, active-item
   highlight), canvas surface token on the content area, a11y landmarks.
3. **Project context/IA (#128):** breadcrumb wired **dynamic** (resolvers turn `:name`/`:id` into
   labels), a **workspace overview** at `/projects/:name`, and **editor-header action groups**.
4. **Search is a disabled placeholder** in this chunk (like notifications, which the ticket marks
   "future"). A working search is not in scope — confirm before build if that changed.
5. **Parity for guards + dirty-check** (#128 AC-5, #142's bar): no existing guard or
   `canDeactivate` weakens; the route move in §2 preserves both.

## 2. Headline decision — `/projects/:name` (overview vs editor)

#128 wants the workspace overview at `/projects/:name`; that path is the project **editor** today.
**Recommended (Option A, matches the review doc):**

- `/projects/:name` → **`ProjectWorkspaceComponent`** (new overview: counts, open issues, recent
  changes, next actions). No dirty-check (read-only landing).
- Project editor moves to **`/projects/:name/edit`**, keeping `canDeactivate: [dirtyCheckGuard]`
  and its `RequelRouteData`.
- Keep `/projects/:name` (now the overview) **matched-last** among `projects/:name/*` for the same
  ordering reason #142 pinned; `projects/:name/edit` is just another `projects/:name/*` leaf.
- Any internal navigation / links that assumed `/projects/:name` == editor are repointed to
  `/projects/:name/edit`. The `app.routes.spec.ts` ordering + coverage assertions are updated to
  the new set (this is the one intentional route change, not a regression).

Alternative (Option B) — overview at `/projects/:name/overview`, editor stays put — is less
disruptive but contradicts the review doc's `/projects/:name` recommendation and leaves the
"landing on a project" URL pointing at a form. **Plan assumes A; flag if you prefer B.**

## 3. Top bar (#154 chrome + #128 dynamic breadcrumb)

`layout.ts` header becomes a two-region top bar:

- **Left:** back button (`Location.back()`, hidden/disabled at shell root) + **breadcrumb**.
- **Right:** search (disabled placeholder), account menu (existing), **sidebar collapse toggle**.

**Breadcrumb** — new `shared/breadcrumb.ts` (`app-breadcrumb`), driven by the `Router` +
`ActivatedRoute` snapshot chain, reading each segment's `RequelRouteData`:

- Static segments come from `data.breadcrumb` / `title` (already on every route from #142).
- **Dynamic** segments (`{project}`, `{entity}`) come from **route resolvers** (§4) that resolve
  `:name` → project display name and `:id` → artifact name, exposed on the snapshot so the
  breadcrumb renders `Projects / {project} / Goals / {goal}` without each page wiring it.
- Keyboard-navigable: rendered as a `<nav aria-label="Breadcrumb">` + ordered list of links; the
  current page is `aria-current="page"` and not a link.

## 4. Resolvers (#128 dynamic labels)

`core/resolvers/` — small functional resolvers:

- `projectNameResolver` — `:name` → project display label (from `ProjectService`, cached).
- `artifactNameResolver` — `:artifactType` + `:id` → entity label, per editor route.

Attached to the relevant routes in the per-feature `*.routes.ts`; results surface via
`snapshot.data`/`title` so both the breadcrumb (§3) and `RequelTitleStrategy` can upgrade from
static to dynamic titles ("Goal · Requel" → "{goal name} · Requel"). Resolvers must fail soft
(missing/deleted entity → fall back to the static label, never block navigation).

## 5. Grouped collapsible sidebar (#154)

Replace the accordion chrome with grouped, labelled sections (icon + label items, active-item
highlight via `routerLinkActive`). The **project tree stays** as the Projects group's content —
its IA is #128's territory and is not rewritten here; #154 only reshapes the chrome around it.

- **Two persisted states, two keys, both distinct from the existing project-expansion key:**
  - whole-sidebar collapse (rail) toggle → `requel_sidebar_collapsed`.
  - per-group open/closed → `requel_sidebar_groups`.
  - `requel_sidebar_expanded_projects` (project tree) is left exactly as-is.
- All three wrapped in try/catch (private mode / quota), same pattern as today.

## 6. Canvas surface (#154)

Apply the light blue-gray canvas token to the content area and confirm list/editor content sits on
white cards (`--rq-card-*` already exists). Add `--rq-canvas-bg: var(--p-surface-50)` (or reuse an
existing surface alias) and set `.main-content { background: var(--rq-canvas-bg); }`. The
"header color from tokens, no `#1a1a7e` literal" AC is **already satisfied** — assert it stays so.

## 7. Workspace overview + editor headers (#128)

- **`ProjectWorkspaceComponent`** at `/projects/:name` (§2): compact overview — artifact **counts**,
  **open issues**, **recent changes**, **next actions**. Reuses `app-page-header` + card tokens.
- **Editor-header action groups:** add project-aware actions to artifact editor headers (Back to
  {section}, Open Issues, Related Stories/Use Cases) so a deep-linked editor is understandable
  without the sidebar (#128 AC-4). Prefer a small shared header primitive over per-editor copies.

## 8. A11y (#154 AC, #135 parity)

- Keep `layout.spec.ts`'s skip-link + `<main>` assertions green.
- New landmarks labelled: `<header>` region, `<nav aria-label="Primary">` (sidebar),
  `<nav aria-label="Breadcrumb">`. Collapse toggle + back button have discernible names.
- axe pass on the shell (expanded + collapsed sidebar) and on the workspace overview.

## 9. Step-by-step (stacked PRs, each to `release/2.0`, squash-merged)

- **PR 1 — chrome (#154):** top bar (back + breadcrumb **static** slot + collapse), grouped
  collapsible sidebar + persistence, canvas token, landmarks/a11y. Breadcrumb renders static
  labels only (data already exists from #142).
- **PR 2 — dynamic breadcrumb + route move (#128 + §2):** resolvers, breadcrumb upgraded to
  dynamic, `/projects/:name` → overview / editor → `/projects/:name/edit`, `app.routes.spec.ts`
  updated, `RequelTitleStrategy` picks up resolved titles.
- **PR 3 — workspace overview + editor headers (#128):** `ProjectWorkspaceComponent` + editor
  action groups.

Each PR references the issue(s) whose ACs it advances; #128 and #154 close when their ACs are all
met (see §11).

## 10. Test plan

- `layout.spec.ts` — extend: top-bar regions, back button behavior, collapse toggle + persistence
  (mock `localStorage`), landmark labels; keep #135 assertions.
- `breadcrumb.spec.ts` (new) — builds the trail from a snapshot chain; static + resolved segments;
  `aria-current` on the leaf; keyboard-navigable markup.
- resolver specs — `projectNameResolver` / `artifactNameResolver` resolve + fail-soft.
- `app.routes.spec.ts` — update coverage/order for the `/projects/:name` (overview) +
  `/projects/:name/edit` (editor, dirtyCheck) change; assert dirtyCheck now on `.../edit`.
- `project-workspace.spec.ts` (new) — renders counts / open issues / next actions from mocked data.
- Full `ng build` once (lazy route/resolver wiring only surfaces at build/route-load).
- Regression bar: no existing `e2e/*.e2e.ts` route navigation breaks; a break means the route move
  changed matching — treat as a regression, and repoint page objects that addressed
  `/projects/:name` as the editor.

## 11. Acceptance criteria mapping

**#154:** breadcrumb keyboard-navigable + route-param aware (§3–§4) · sidebar groups collapsible,
labelled, collapse persists (§5) · header/sidebar/surface from tokens (§6) · passes #135
landmark/skip-link checks (§8) · does **not** take over #128's IA — the project tree content is
untouched (§5).

**#128:** project context without relying on the sidebar (breadcrumb + editor headers, §3/§7) ·
breadcrumbs for project/section/entity (§3–§4) · workspace route with counts/open issues/next
actions (§7) · deep-linked editors understandable without the sidebar (§7) · existing guards +
dirty-check still work (§2 route move preserves `canDeactivate`).

## 12. Risks & edge cases

- **The `/projects/:name` move (§2)** is the one real behavior change; everything downstream
  (links, e2e page objects, the routes spec, the matched-last ordering) must move with it.
- **Resolvers must fail soft** — a deleted/renamed entity can't block navigation or throw in the
  breadcrumb; fall back to the static label.
- **Collapse-state keys** must not collide with `requel_sidebar_expanded_projects`; three
  independent persisted concerns.
- **Sidebar reshape vs #128 IA** — #154 reshapes chrome only; the project tree's content/behavior
  stays put so the two halves don't fight over the same markup.
- **Search placeholder** must be visibly non-functional (disabled + no submit) so it doesn't read
  as broken.

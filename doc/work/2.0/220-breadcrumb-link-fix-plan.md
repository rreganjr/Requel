# #220 — Breadcrumb project-link fix + remove editor quick-nav — plan

Ticket: [#220](https://github.com/rreganjr/Requel/issues/220) (I7, Phase 1 of the
Post-#124 UI polish epic #219). Punch-list items **B1** (bug) and **B2** (decision).
Branch: `220-breadcrumb-link-fix` off `release/2.0`. Frontend-only.

## Scope / locked decisions

- **B1 (bug):** the top-bar breadcrumb's project (and section) links fail for project names
  with spaces/parens — clicking "Imported Project (10)" errors "Failed to load the project
  workspace" and the URL shows `Imported%20Project%20%2810%29`.
- **B2 (decision — confirmed: REMOVE):** delete the editor-top `app-editor-actions`
  "Overview / Open issues" quick-nav and remove it from all 8 editors. Redundant with the
  now-working breadcrumb project link and the left-nav buttons.

## Root cause (B1)

`breadcrumb.ts#build()` sets `url: '/' + prefix.join('/')` from the **already-encoded** URL
segments, and the template binds `[routerLink]="crumb.url"` — a *string*. Angular's router
re-encodes that string, so `%20`/`%28` become `%2520`/`%2528`; navigation lands on a param that
decodes to the literal `Imported%20Project%20%2810%29`, and `getProject()` fails. Every other
link in the app (editor-actions, workspace cards) uses the **array** form
`['/projects', projectName]` with the decoded name, which the router encodes exactly once — those
work. The breadcrumb is the only offender.

## Changes

### B1 — `requel-angular/src/app/shared/breadcrumb.ts`

- Add `commands: string[]` to the `Crumb` interface (routerLink command array of **decoded**
  segments). Keep `url: string` (now the decoded path) for the `@for … track` key and existing
  `.url` assertions.
- In `build()`, compute `const decoded = prefix.map(safeDecode);` and push
  `{ label, commands: ['/', ...decoded], url: '/' + decoded.join('/'), current }`.
- Template: `[routerLink]="crumb.commands"` (was `crumb.url`); `track crumb.url` unchanged.
- Net effect: the router encodes each segment once → correct single-encoded href → workspace
  loads for names with spaces/parens.

### B2 — remove `app-editor-actions`

- Delete `requel-angular/src/app/shared/editor-actions.ts` (no spec exists).
- In each of the 8 editors — goal, term, stakeholder, use-case, story, actor, scenario, report —
  remove: the `EditorActionsComponent` import line, its entry in the `imports:` array, and the
  `<app-editor-actions [projectName]="projectName" />` element from the template. Leave
  `projectName` and any independent `RouterLink` usage intact.

## Test plan (the verify gate)

- **Unit** (`breadcrumb.spec.ts`): existing cases stay green (`.url` for `Acme` unchanged, decoded
  label case unchanged). **Add** a case: navigate to a name with space **and** parens
  (e.g. `/projects/My%20Proj%20%2810%29/goals`), read the project crumb's rendered `<a href>`, and
  assert it is single-encoded (`/projects/My%20Proj%20(10)` style) and contains **no** `%25`
  (guards against the double-encode regression); assert the section crumb likewise.
- **Grep gate:** `grep -r 'app-editor-actions\|EditorActionsComponent' src` returns nothing after
  B2 (no dangling import/usage).
- **Typecheck:** `tsc -p tsconfig.app.json --noEmit && tsc -p tsconfig.spec.json --noEmit`.
- **Unit run (non-watch):** `CI=1 npx ng test --watch=false --include='src/app/**/breadcrumb.spec.ts'`;
  then the 8 touched editors' specs if any assert on editor-actions (none expected).
- **Dev build** (template/wiring): `npx ng build --configuration development`.
- **e2e:** navigation changed, so the breadcrumb/project-link flow is e2e-relevant — runs in CI;
  read the report and fix real regressions. No backend (`modules/**`) change → no `mvn verify`.

## Out of scope

- The broader "project name in the URL is fragile" concern (route uses `:name`); unchanged here.
- Global artifact search (parked future idea). Other epic tickets (I4/I5/I2/I3/I1/I6).

## Risks

- Removing `app-editor-actions` from 8 editors is a wide but mechanical diff; the grep gate + dev
  build catch a missed import/usage. Low risk.
- B1 change is tiny and self-contained; main risk is an existing `.url` assertion — verified the
  two that exist stay green.

## AC mapping

- B1 → breadcrumb project/section links load the workspace for names with spaces/parens
  (new unit test + e2e).
- B2 → `app-editor-actions` gone from the codebase and all 8 editors (grep gate + build).

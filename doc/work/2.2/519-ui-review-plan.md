# #519 — UI review and cleanup: walkthrough, theme colours, data tables

https://github.com/rreganjr/Requel/issues/519

Branch `519-ui-review` from `release/2.2`, PR to `release/2.2`. Size 3.

## Summary

Walk every route of the Angular UI in light and dark, wide and narrow, with an axe check on each
page. Fix the small things found on the way, file the rest, and record every finding with an
outcome in `doc/work/2.2/519-ui-review.md`. Two known fixes ship here: colours that don't follow
dark mode, and the two raw tables moved to `app-data-table`. The report also decides whether the
eight entity editors get a shared editor shell (a sized follow-on ticket if so).

## Locked decisions (2026-11-21)

1. **Colour rule, wider than the issue's list.** Outside `src/app/theme/` and `src/styles.scss`,
   components use no colour literal (hex, `rgb()`, `hsl()`) and no fixed palette stop
   (`--p-blue-50`, `--p-primary-50`, `--p-green-600`, ...). Fixed stops don't change in dark mode;
   only the surface ramp is inverted. Most hex values in the issue's files are dead fallbacks in
   `var(--token, #hex)`; they go too. Colours come from semantic tokens: the preset's
   (`--p-text-muted-color`, `--p-content-border-color`, ...) or Requel's `--rq-*` in
   `styles.scss`, with a light and a `.rq-dark` value, added where one is missing (status colours,
   highlight/drop-target backgrounds). Files, from a scan of the tree:
   - the issue's nine: `scenario-editor`, `annotations-section`, `api-tokens`,
     `project-definitions`, `project-definition-editor`, `project-assistants-panel`,
     `project-corpus-panel`, `app-relationship-section`, `app-update-banner`;
   - six more found by the scan: `layout` (a raw `rgba()`), `sidebar-nav`,
     `scenario-selector-dialog`, `delete-project-dialog`, `term-editor`, `open-issues`.
2. **No screenshots in the repo.** The walk can regenerate them any time, and a screenshot of a
   fixed problem goes stale on merge. The walk writes to `requel-angular/ui-review/`
   (gitignored); the report describes each finding in words with its page, theme and width. A
   filed issue can carry a screenshot dragged in by hand. This replaces the issue's "a screenshot
   per page and state, checked in with the report".
3. **The walk is a committed Playwright script, not run in CI:** `e2e/ui-review/ui-review.walk.ts`
   with its own `playwright.ui-review.config.ts` (`testMatch: '**/*.walk.ts'`, so the e2e config's
   `*.e2e.ts` never picks it up) and `npm run ui-review`. It reuses the e2e global setup (dev reset
   endpoints, stored admin and project sign-ins). axe runs in the page from the `axe-core` already
   in devDependencies (`page.addScriptTag`), so no new dependency.
4. **Views and states.** Each route in four views: light and dark (set through the theme
   service's stored preference before load), 1440×900 and 390×844 (below the 768px point where
   the sidebar collapses). States:
   - populated: every route, against `doc/samples/Requel.xml` imported fresh each run (every
     entity type, so every detail route has a target);
   - empty: each list page, against a new empty project;
   - error: one detail page with an id that doesn't exist.
   Loading states are skipped (too brief to capture reliably).

## Walk output

`requel-angular/ui-review/`: a PNG per route, state and view, `axe/<page>-<view>.json`, and a
`summary.md` listing each page's axe violations by impact. The run doesn't fail on violations:
it's a report, read against the acceptance criteria.

## Steps

1. Walk script, config, `npm run ui-review`, `ui-review/` in `requel-angular/.gitignore`.
2. First walk: you start the jar with the dev profile and run `npm run ui-review`; I review the
   screenshots and axe output from the folder and draft the findings.
3. Colours (decision 1): new `--rq-*` tokens where needed, the 15 files moved onto tokens.
4. `api-tokens` and `app-relationship-section` onto `app-data-table`; their specs and e2e page
   objects updated where the markup they match changed.
5. Small findings fixed here; larger ones filed (I draft the `gh issue create` commands).
6. Second walk to confirm: dark mode on the fixed pages, no serious or critical axe violation left
   unfiled.
7. Report `doc/work/2.2/519-ui-review.md`: findings table (page, state, view, severity, outcome:
   fixed here / filed as #… / won't fix and why) and the editor-shell recommendation.

**What counts as small:** a fix in one component's template or styles, under about half an hour,
that doesn't change behaviour a test asserts. Anything else is filed.

## Test plan

- Unit: `api-tokens` and `app-relationship-section` specs pass on the new markup; the existing
  `*.a11y.spec.ts` pass; `theme/requel-preset.spec.ts` still passes with any new tokens.
- A guard spec over the component sources fails on a colour literal or fixed palette stop
  outside `theme/` and `styles.scss`, so the rule holds after this ticket.
- Typecheck (`tsconfig.app.json`, `tsconfig.spec.json`) and a development build.
- Whole unit suite, non-watch.
- e2e in CI: the account/settings flows (API tokens) and the editors with relationship sections
  (goals, actors, stories, use cases, stakeholders).
- Second walk: summary shows no serious or critical violation without a filed issue.

## Out of scope

- Extracting the shared editor shell (follow-on if the report recommends it).
- New features or redesigns.
- Running the walk in CI.

## Risks

- **Finding volume.** A first full walk can turn up more than a size-3 ticket can fix. The
  small-fix rule keeps the rest as filed issues.
- **Tables move to new markup.** e2e page objects that locate rows in the two raw tables need
  their locators updated; that is intended, not a regression.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| Every route in the report, both themes and both widths | Walk (decision 4), report |
| No hard-coded colours outside `theme/` | Step 3 (decision 1), guard spec |
| API tokens and relationship section on `app-data-table`, tests and e2e pass | Step 4, CI |
| No serious or critical axe violation remains, or each is filed | Steps 5–6, summary |
| Every finding has an outcome | Report |
| Screenshot per page and state checked in | Dropped (decision 2) |

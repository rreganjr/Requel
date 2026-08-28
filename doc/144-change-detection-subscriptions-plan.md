# Ticket #144 (5.3) — Change detection & subscriptions modernization — implementation plan

## Scope (confirmed with Ron): full sweep, one PR
OnPush across every change-detection-safe component, migrate every manual
subscription to `takeUntilDestroyed`/`toSignal`, document zoneless blockers
(without enabling zoneless), keep all tests green.

## Why the sweep is low-risk now (AC drift)
The ticket's "Current State" predates #143/#145. Today:
- Every **feature** component already holds its view state in signals/computed
  (#143). Under OnPush, signal reads and input changes drive CD, so the subscribe
  callbacks (which write to signals) refresh the view correctly. OnPush is safe.
- Two shared components are already OnPush (`app-update-banner` #140,
  `app-relationship-section` #130) — skip them.
- `sidebar-nav.loadProjects()` writes the `projects` **signal** → OnPush-safe.
- No `NgZone` usage anywhere in the app.

## Migration recipes

### A. OnPush
Add to each `@Component({...})`: `changeDetection: ChangeDetectionStrategy.OnPush`
(import `ChangeDetectionStrategy` from `@angular/core`).

### B. Subscription cleanup (AC2/AC3)
Inject once per component: `private readonly destroyRef = inject(DestroyRef);`
(import `DestroyRef` from `@angular/core`, `takeUntilDestroyed` from
`@angular/core/rxjs-interop`). Then:
```
// before
this.paramSub = this.route.paramMap.subscribe(cb);
// ... ngOnDestroy() { this.paramSub?.unsubscribe(); }

// after
this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(cb);
// paramSub field removed; ngOnDestroy drops the unsubscribe line
```
`takeUntilDestroyed()` is called outside a constructor here (in ngOnInit /
ngAfterContentInit), so it MUST receive the explicit `this.destroyRef`.
Remove now-empty `ngOnDestroy`/`OnDestroy`; keep it where it still does other
work (e.g. the editors' `eventStreamService.removeSubscription(...)` app-level
cleanup — that is not an rxjs subscription).

### C. toSignal (AC "where feasible")
The 18 `route.paramMap.subscribe(async params => …)` sites run imperative async
loads; `toSignal` + an async `effect` would change semantics and is not a clean
fit, so those use recipe **B**. Cleanup is still automatic, satisfying AC3. Where
a param feeds only a pure derived value we may use `toSignal`; not forced.

## Component inventory

### Subscription migration (recipe B) — 22 files
- **paramMap (+events$ where noted):** goal-editor (+events$), goal-list,
  project-editor, project-workspace, term-list, term-editor (+events$),
  open-issues, stakeholder-list, stakeholder-editor (+events$), use-case-editor
  (+events$), story-editor (+events$), story-list, user-editor, actor-list,
  actor-editor (+events$), scenario-editor (+events$), scenario-list,
  report-list, report-editor.
- **shared:** `sidebar-nav` (onTreeChanged + events$ → takeUntilDestroyed;
  `loadProjects` already signal-backed), `app-field-group`
  (`cells.changes` → takeUntilDestroyed; its callback `markCells()` mutates host
  classes imperatively, not template-bound data, so OnPush-safe),
  `app-form-wizard` (`stepQuery.changes` → takeUntilDestroyed **and** convert the
  plain `stepList` field it mutates to a signal so the step strip refreshes under
  OnPush — the one real OnPush landmine).

### OnPush sweep (recipe A) — all remaining components
All feature editors/lists above, plus the components with no manual subs:
`use-case-list`, `project-list`, `user-list`, `admin/tag-categories`,
`admin/global-tags`, `users/api-tokens`, `users/settings`, `users/edit-account`,
`auth/login`, `auth/dashboard`, `auth/layout`, root `app`, and the presentational
shared set (all `@Input`-driven, sig-free, OnPush-ideal): `app-card`, `app-chip`,
`app-data-table`, `app-field`, `app-inline-error`, `app-submit-error`, `app-tag`,
`editor-actions`, `empty-state`, `error-state`, `file-upload-button`, `list-page`,
`loading-state`, `page-header`, `breadcrumb`, `annotations-section`,
`entity-selector-dialog`, `scenario-selector-dialog`, `tag-selector`.
**Skip (already OnPush):** `app-update-banner`, `app-relationship-section`.

### Audit before flipping OnPush
- `app-form-wizard`: `stepList` → signal (above).
- `users/settings` and `users/user-editor` call `markForCheck`/`detectChanges` —
  confirm whether these compensate for a plain-field pattern; keep the call if it
  still guards a non-signal binding, or remove if the state is already signals.
- Spot-check each converted component's template for any binding that reads a
  plain mutable field or a zero-arg method returning changing data; convert such
  reads to signals/`computed` before flipping OnPush (expected to be rare — most
  are signals post-#143).

## AC4 — zoneless readiness doc (not enabling zoneless)
New `doc/ZONELESS_READINESS.md` enumerating blockers found and the checklist:
- `setTimeout` state updates in `announcer.service`, `event-stream.service`,
  `project-editor`, `api-tokens` — must write signals (event-stream already does).
- Remaining `markForCheck`/`detectChanges` sites (settings, user-editor) to retire.
- PrimeNG components' CD behavior under a zoneless provider — unverified; needs a
  spike.
- `app-field-group` imperative host-class marking — safe but noted.
Conclusion: OnPush first (this ticket); zoneless deferred until the checklist clears.

## Verification gate (frontend-only; no modules/** → no mvn)
```
cd requel-angular
CI=1 npm test -- --watch=false                 # full unit suite — OnPush regressions surface here
npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit
npx ng build --configuration development
# e2e: dirty-guard + a11y specs (exercise editors/lists/dialogs under OnPush)
```
Captured in `tmp/144-verify.sh`. Because OnPush stale-view bugs can slip past unit
tests, the developer also smoke-checks a couple of live screens (a list, an editor
with an open SSE stream, the create wizard).

## Out of scope
- Enabling zoneless / `provideExperimentalZonelessChangeDetection`.
- Any refactor beyond change-detection strategy and subscription cleanup.
- Server/`modules/**` — none touched.

## Risks
- **OnPush stale views** from a missed plain-field binding — mitigated by the
  per-component template audit, the signal-based baseline (#143), and the e2e/unit
  gate; smoke-test as backstop.
- **QueryList.changes timing** (`app-form-wizard`, `app-field-group`) — keep the
  `ngAfterContentInit` hook; only the cleanup mechanism changes.
- **PrimeNG under OnPush** — PrimeNG drives its own CD; overlays/tables already
  work with the two existing OnPush components, low risk. Watch p-table/p-dialog
  in the gate.
- Large diff — many files, each a tiny mechanical change; reviewer can scan by the
  two recipes.

## AC mapping
| AC | Coverage |
|----|----------|
| Low-risk shared + leaf pages use OnPush | recipe A across the inventory |
| Manual subscription fields → takeUntilDestroyed | recipe B, 22 files |
| Route-param & stream subs auto-cleaned | takeUntilDestroyed(destroyRef) |
| Zoneless blockers documented; not enabled | doc/ZONELESS_READINESS.md |
| Existing unit/e2e tests pass | full gate incl. e2e + smoke |

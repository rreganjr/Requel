# #140 — 4.6 Async & SSE updates are announced — Implementation Plan

Issue: https://github.com/rreganjr/Requel/issues/140
Part of the look-and-feel remediation epic (`doc/124-remediation-rollup.md`, Phase 4).
WCAG 4.1.3 Status Messages, 2.2.2 Pause/Stop/Hide. Related: #145 (SSE service state — Phase 5).

## Summary

Background updates are currently silent: SSE-driven refreshes update counts and entity data,
and editors quietly skip resetting a dirty form, with nothing announced to assistive tech. This
adds a polite live-region announcer, announces cross-session SSE updates, shows a non-modal
"newer version" banner in editors when a background update lands on unsaved edits, and lightly
announces stream connection changes.

## What exists today

- A global `<p-toast>` + `MessageService` live in the shell (`layout.ts`) — for foreground
  actions, not background status; toasts risk overwhelm (2.2.2), so status uses a polite region.
- `@angular/cdk` (21) is a dependency → use its `LiveAnnouncer` as the AC1 mechanism.
- `EventStreamService` exposes `connectionState` (`idle|connecting|open|closed|error`) and
  `isConnected` signals (helps AC4).
- **Self-echo is already suppressed:** `command.service` sends the caller's `X-Session-Id`, so an
  editor does NOT receive SSE events for its own writes. Every `Data`/`TargetDeleted` event an
  editor gets is a cross-session change — so announcing it is meaningful by construction (this is
  what keeps AC2 "without overwhelm" honest; no own-change filtering needed).
- Editors already subscribe to `events$` and call `loadX(false)` on a matching target; `loadX`
  takes the new version but only resets the form when the form is clean (preserves unsaved edits).

## Locked decisions

1. **AC1 — `AnnouncerService` wrapping CDK `LiveAnnouncer`.** Polite, visually-hidden region;
   `announce(message)` plus `announceThrottled(key, message, delayMs=1500)` that coalesces bursts
   (per-key timer) so a flurry of SSE events yields one message.
2. **AC2 — announce cross-session SSE updates.** Editors announce on received `Data`/`TargetDeleted`
   for the current target; `sidebar-nav` announces (throttled) when a background event refreshes the
   project tree/counts. Messages are short ("This goal was updated.", "Project list updated.").
3. **AC3 — shared `app-update-banner`.** Non-modal, dismissible inline banner (role="status")
   with a message + a "Reload" action. Driven by an `updateAvailable` signal an editor sets when a
   background update arrives while the form is dirty (so the form was NOT reset). "Reload"
   re-fetches and resets the form; dismiss hides it.
4. **Adopt in all seven SSE editors:** goal, actor, story, stakeholder, use-case, scenario, term.
5. **AC4 — light announce only.** A shell `effect` on `connectionState` announces "Live updates
   paused" / "Live updates restored" on transitions (debounced). Richer reconnect UX is #145.

## Contracts

`core/announcer.service.ts` (new, providedIn root):
- `announce(message: string): void` — delegates to CDK `LiveAnnouncer.announce(message, 'polite')`.
- `announceThrottled(key: string, message: string, delayMs = 1500): void` — coalesces per key.

`shared/app-update-banner.ts` (new, standalone, OnPush):
- Inputs: `message`, `reloadLabel='Reload'`, `testid`. Output: `(reload)`, `(dismiss)`.
- Renders a non-modal `role="status"` bar (PrimeNG Message/inline) with Reload + close buttons.

Editor SSE handler pattern (each editor):
- On a `Data` event for the current target:
  - form dirty  → set `updateAvailable=true`; `announce('This <entity> was changed elsewhere.')`.
  - form clean  → existing reload applies; `announceThrottled('<entity>:<id>', 'This <entity> was updated.')`.
- On `TargetDeleted` for the current target → `announce('This <entity> was deleted.')` (+ leave
  existing handling; full redirect is out of scope).
- Banner `(reload)` → force re-fetch + reset form, clear `updateAvailable`.

## Step-by-step

1. `AnnouncerService` (+ spec) and `app-update-banner` (+ spec, + a11y spec).
2. Wire goal-editor first (validate the pattern): render `<app-update-banner>` bound to
   `updateAvailable`, extend the `events$` handler, add reload. Announce clean-update + dirty-update.
3. Roll the same wiring to actor, story, stakeholder, use-case, scenario, term editors.
4. `sidebar-nav`: announce (throttled) on background project refresh.
5. `layout.ts`: `effect` on `EventStreamService.connectionState` → announce transitions.
6. Tests (AC5): announcer spec; banner spec + a11y; one editor test (dirty SSE → updateAvailable +
   announce) and clean SSE → announce; a connection-state announce test.

## Test plan

- `announcer.service.spec.ts`: `announce`/`announceThrottled` call CDK LiveAnnouncer; throttle
  coalesces within the window (fake timers).
- `app-update-banner.spec.ts` + `.a11y.spec.ts`: renders message + Reload/close, emits outputs,
  role="status", axe clean.
- Editor spec (goal + one more): a `Data` event while dirty sets `updateAvailable` and announces;
  while clean, announces the update; `(reload)` re-fetches and clears the banner.
- Shell: `connectionState` transition announces paused/restored.
- Gate: `tsc` app+spec; `ng test` for the new units + touched editors; `ng build` dev. No `modules/**`.

## Out of scope

Rich SSE reconnect/degraded UX and retry controls (#145); `TargetDeleted` navigation/redirect flows;
routing background list refreshes (list pages already re-query on navigation); converting the
existing foreground toasts.

## Risks

- **Announcement volume** — mitigated by self-echo suppression (events are cross-session) + throttle
  coalescing; editors announce only for the current target.
- **Per-editor dirty signal differs** — each editor already has a dirty check (goal
  `hasUnsavedChanges()`); use each editor's existing one; term/others may name it differently.
- **CDK LiveAnnouncer in tests** — provided by `@angular/cdk/a11y`; TestBed-friendly. Spec asserts
  via a LiveAnnouncer spy rather than DOM timing.
- **Banner placement** — top of the editor card, above the form; must not shift focus (role=status,
  not alertdialog).

## AC mapping

| AC | Status |
|----|--------|
| 1 — global/page live-region mechanism | **This ticket** — `AnnouncerService` over CDK LiveAnnouncer |
| 2 — meaningful SSE announcements w/o overwhelm | **This ticket** — cross-session events, throttled |
| 3 — non-modal "new version available" banner | **This ticket** — `app-update-banner` in 7 editors |
| 4 — surface stream degraded/closed | **This ticket (light)** — connectionState announce; rich UX → #145 |
| 5 — test coverage for ≥1 announcement | **This ticket** — announcer + editor + banner + shell specs |

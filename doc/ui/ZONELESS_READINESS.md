# Zoneless change-detection readiness

Status as of #144 (5.3). This documents what stands between the app and
`provideExperimentalZonelessChangeDetection()`. **Zoneless is not enabled** — the
app still runs Zone.js. #144 did the prerequisite work (OnPush everywhere, signal-
based view state, automatic subscription teardown); the items below must clear
first.

## What #144 already established
- `ChangeDetectionStrategy.OnPush` on every component. A component that renders
  correctly under OnPush is most of the way to zoneless, because both refresh the
  view from the same triggers: signal reads, input changes, template events, and
  explicit marks.
- View state is signals/`computed` (from #143), so CD no longer depends on Zone
  monkey-patching to notice mutations.
- Manual `Subscription` bookkeeping replaced by `takeUntilDestroyed`; route-param
  and SSE subscriptions tear down with the component. No `NgZone` usage anywhere.

## Blockers to verify before enabling zoneless
1. **`setTimeout` / `setInterval` callbacks must write signals, not plain fields.**
   Under zoneless a timer callback does not itself trigger CD; only the signal
   writes inside it do. Audit sites:
   - `core/event-stream.service.ts` — reconnect backoff timer; already writes the
     `connectionState`/`sessionId` signals (#145). OK.
   - `core/announcer.service.ts` — throttle/debounce timers; drive the CDK
     LiveAnnouncer (imperative DOM), no view binding, OK — but confirm no bound
     field is set from a timer.
   - `features/projects/project-editor.ts`, `features/users/api-tokens.ts` —
     confirm any timer that changes view state writes a signal.
2. **PrimeNG under a zoneless provider (spike required).** `p-table`, `p-dialog`,
   `p-tree`, `p-inputNumber`, `p-accordion`, overlays. These drive their own CD;
   confirm the installed PrimeNG version schedules updates without Zone, or wrap
   the gaps. This is the largest unknown and needs a dedicated spike before any
   flip.
3. **Test configuration.** Enabling zoneless in the app also means the unit suite
   must run with `provideExperimentalZonelessChangeDetection()` (or the zoneless
   test providers); the whole suite must be re-greened under it.
4. **`app-field-group` host-class marking** stamps classes imperatively via the
   element ref. It does not read/write bound state, so it is safe under zoneless;
   noted only so a future reader does not mistake it for a Zone dependency.

## Non-blockers (already handled)
- Async route-param loads use `takeUntilDestroyed(...).subscribe(...)` and write
  results into signals — those signal writes tick the view under zoneless.
- No `ChangeDetectorRef.detectChanges()`/`markForCheck()` calls remain (the two
  historical ones were removed in #143; only explanatory comments reference them).

## Recommendation
Defer `provideExperimentalZonelessChangeDetection()` until (2) the PrimeNG spike
and (1) the timer audit are complete and the suite is re-greened under (3). OnPush
adoption (#144) captures the efficiency win now and de-risks the eventual flip.

# Ticket #145 (5.4) — SSE service reliability & UX/state integration — implementation plan

## Problem (from the ticket)
The SSE layer (`EventStreamService`) is well-built but disconnected from UX and
app-level state in three concrete ways:

1. **Dynamically-added subscriptions are lost on reconnect.** `connect()` seeds the
   connection with an initial subscription array; `addSubscription()`/`removeSubscription()`
   mutate server-side subscriptions but do **not** update anything the client replays.
   On any reconnect, `scheduleReconnect()` → `startConnection()` re-sends only the
   *initial* array, so a Goal/Story/etc. subscription an editor added at runtime silently
   stops delivering events after a drop.
2. **Subscription failures are invisible.** `add/removeSubscription()` fire-and-forget a
   `fetch` with no status check and return `Promise<void>`, so a 4xx/5xx or network error
   is swallowed — the caller and the UX have no way to know a subscription didn't take.
3. **The connection-state model is too coarse for UX.** `StreamConnectionState` collapses
   "we dropped and are actively recovering" and "auth session ended" into a single
   `'error'`. The shell (#140) can only distinguish open/not-open, so it can't tell the
   user *why* live updates paused or that they need to sign in again.

## Locked decisions (confirmed with Ron)
- **AC3 state model:** replace the 5-state enum with
  `'idle' | 'connecting' | 'open' | 'degraded' | 'closed' | 'expired'`.
  - `'degraded'` replaces `'error'` and means *dropped and actively reconnecting* (backoff in flight).
  - `'expired'` is the terminal state after `SESSION_EXPIRED` (auth ended; no auto-retry).
  - `isConnected` stays `=== 'open'` — no caller churn.
- **AC2 failure API:** `add/removeSubscription()` return `Promise<boolean>` (true = server
  accepted) and set a new `lastSubscriptionError = signal<string | null>(null)` on failure
  (cleared on success). All 8 editor call sites already `void` the promise, so this is
  backward-compatible — no editor changes required.

## AC-drift note (important)
- **AC4 (editor reload preserves unsaved local changes) is already satisfied by #140.**
  Every editor's `loadX(false)` gate resets the form only when `!hasUnsavedChanges()`, and
  each editor shows an `app-update-banner` + `reloadFromExternalChange()`. No work here.
- This ticket is therefore AC1 (subscription replay), AC2 (failure detection), AC3 (state
  model + shell announce refinement), AC5 (tests).

## Contracts / API changes
`src/app/models/stream.ts`
```ts
export type StreamConnectionState =
  'idle' | 'connecting' | 'open' | 'degraded' | 'closed' | 'expired';
```
(`'error'` removed; `'degraded'` + `'expired'` added.)

`EventStreamService` (public surface)
```ts
readonly lastSubscriptionError = signal<string | null>(null);   // NEW
addSubscription(targetType: string, targetId: number): Promise<boolean>;    // was Promise<void>
removeSubscription(targetType: string, targetId: number): Promise<boolean>; // was Promise<void>
```

## Step-by-step

### 1. Model — `models/stream.ts`
Swap the enum to the 6-state union above.

### 2. Service — `core/event-stream.service.ts`
- Add `private readonly liveSubscriptions = new Set<string>();` as the single source of
  truth for what must be (re)subscribed. Keys are the existing `"Type:Id"` strings.
- `connect(subscriptions)`: `liveSubscriptions.clear()`, add all initial subs, then start.
- Stop threading a `subscriptions` array through `startConnection` / `readStream` /
  `scheduleReconnect`; each rebuilds `subscribe=` params from `Array.from(liveSubscriptions)`.
  This is the AC1 fix — reconnect always replays the *current* live set, initial + dynamic.
- `addSubscription`: check `res.ok`; on success add key to the set, clear
  `lastSubscriptionError`, return `true`; on non-ok or thrown error set
  `lastSubscriptionError` and return `false`. Missing sessionId → set error, return `false`.
- `removeSubscription`: on success delete key from the set, clear error, return `true`; on
  failure set error, return `false`, and keep the key (unknown server state → replay is safe).
- State transitions:
  - fresh connect / first attempt → `'connecting'`; success → `'open'` (reset `reconnectAttempt`).
  - fetch throw or non-ok while retrying is intended → `'degraded'` + `scheduleReconnect`.
  - graceful stream end (`readStream` done) → `'closed'` then `scheduleReconnect` (the
    scheduled retry sets `'degraded'`); `'closed'` is the brief "server closed, about to retry" blip.
  - `SESSION_EXPIRED` → tear down (abort + clear timer, no auto-retry), set `'expired'`,
    `authService.logout()`. (`disconnect()` still lands on `'idle'` for explicit disconnects.)
  - `isConnected` unchanged.

### 3. Shell announce — `features/auth/layout.ts` (refine #140 effect)
Read `connectionState()` instead of the `isConnected()` boolean so the polite live-region
announce distinguishes cause:
- `'degraded'` → "Live updates interrupted. Reconnecting…"
- `'open'` after an interruption → "Live updates restored."
- `'expired'` → "Session expired. Please sign in again."
Still skips the initial establishment; `'closed'` stays silent (transient before degraded).

### 4. Tests — AC5
- `core/event-stream.service.spec.ts` (extend existing):
  - **Reconnect replays all live subscriptions** (the AC1 regression): fake timers + mocked
    `fetch`; connect with `['Project:0']`, drive a `Session` event to set sessionId,
    `addSubscription('Goal', 7)`, simulate a stream drop, advance timers, assert the
    reconnect `fetch` URL contains both `subscribe=Project%3A0` and `subscribe=Goal%3A7`.
  - **addSubscription failure**: non-ok `Response` → resolves `false`, `lastSubscriptionError`
    set, key not added; ok `Response` → `true`, error cleared, key present.
  - **State transitions**: drop → `'degraded'`; `SESSION_EXPIRED` → `'expired'` (+ logout called).
- `features/auth/layout.spec.ts`: update the SSE mock to expose a `connectionState` signal
  and assert the degraded/restored/expired announcements (existing mock currently stubs
  `isConnected`).

## Verification gate (frontend-only; no `modules/**` → no mvn)
```
cd requel-angular
CI=1 npx ng test --watch=false \
  --include='src/app/core/event-stream.service.spec.ts' \
  --include='src/app/features/auth/layout.spec.ts'
npx tsc -p tsconfig.app.json --noEmit && npx tsc -p tsconfig.spec.json --noEmit
npx ng build --configuration development
```
Captured in `tmp/145-verify.sh`.

## Out of scope
- No editor component changes (AC4 done by #140; AC2 backward-compatible).
- No server/API changes — the subscribe/unsubscribe endpoints are unchanged; the fix is
  entirely client replay + status handling.
- No change to backoff timing or keep-alive parsing.

## Risks
- **`'error'` string references elsewhere.** Removing `'error'` from the union will surface
  any code/tests that compared against it as a type error — that's the intended safety net;
  grep + typecheck before commit and migrate each to `'degraded'`.
- **NG0100 in the layout effect.** The effect only calls the announcer (a side effect, not a
  template binding), so no ExpressionChanged risk; keep it out of any render-read path.
- **Reconnect resubscribe idempotency.** Re-sending live subs as `subscribe=` params on a
  reconnect that reuses the existing sessionId may re-add already-present server subs;
  server treats add as idempotent, so this is safe.

## AC mapping
| AC | Coverage |
|----|----------|
| AC1 subscriptions survive reconnect | `liveSubscriptions` set replayed by `startConnection` |
| AC2 subscription failures detected | `Promise<boolean>` + `lastSubscriptionError` signal |
| AC3 richer connection-state model | 6-state enum + shell announce refinement |
| AC4 editor reload preserves edits | already satisfied by #140 (no work) |
| AC5 tests | reconnect-replay, failure, state-transition, announce specs |

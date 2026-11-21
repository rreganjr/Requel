# #450 — Event stream: session and subscription handling

https://github.com/rreganjr/Requel/issues/450

Branch `450-event-stream-sessions` from `release/2.2`, PR to `release/2.2`. Size 3 (2 in the
issue; raised at review for the subscription access check folded in at triage).

## Summary

Three problems in the server-sent event stream (`/api/events/stream`):

1. **Reconnect takeover.** `GET /api/events/stream?sessionId=X` handed the caller's `X` straight to
   `StreamService.createStream`, and `StreamSessionStore.createSession` overwrote the session's
   owner with whoever called. A user who learned another user's session ID took the session over:
   its owner, its subscriptions and its events. The other routes check ownership, but only after
   the reconnect had already replaced it. An unknown ID was accepted as given.
2. **Leaked timers on a same-user reconnect.** The new emitter replaced the old one in the map,
   but the old emitter was never completed and its keep-alive and expiry tasks were never
   cancelled; the old keep-alive kept firing, at the new emitter, for the life of the server.
3. **Subscriptions weren't access-checked.** Neither `subscribe=` on open nor
   `POST /api/events/stream/subscriptions` checked that the caller could read the target, so any
   signed-in user could watch when entities in someone else's project changed. Events carry only
   `{"type":"refresh"}` plus the target type and id.

## Locked decisions (2026-11-20)

1. **Unknown session id on reconnect** (expired, or the server restarted): a new session with a
   server-generated id. The client already takes its id from the `Session` event, so no client
   change.
2. **Another user's session id:** 403, nothing changed (owner, subscriptions, emitter, tasks).
3. **Own session id:** the new emitter is swapped in atomically, the old one is completed and its
   tasks cancelled, and the subscriptions are kept. Concurrent reconnects end with one live
   emitter.
4. **Unreadable targets in `subscribe=` on open or reconnect:** skipped and logged; the stream
   still opens. (A user removed from a project keeps a working sidebar; the client replays its
   subscriptions on every reconnect.)
5. **`POST /subscriptions` to an unreadable, unknown-type or nonexistent target:** 403, the same
   for all three, so it doesn't reveal whether an entity exists.
6. **What may be subscribed:** the broadcasts `Project:0` and `Permissions:0` by anyone signed in;
   an annotatable entity (the registry's types: `Project`, `Goal`, `Story`, `Actor`, `UseCase`,
   `Scenario`, `Step`, `GlossaryTerm`, `Stakeholder`, ...) whose project the caller can read;
   nothing else.
7. **Out of scope:** dropping subscriptions a user already holds when they lose access to a
   project. They end at the next reconnect (decision 4).

## Steps

1. **`StreamSessionStore.claimSession(requestedId, owner)`:** the requested id when it exists and
   belongs to `owner`; `null` when it belongs to someone else; otherwise a new UUID session for
   `owner`. `createSession` stops overwriting an existing owner.
2. **`StreamService`:**
   - One lifecycle lock around claiming a session and swapping its emitter, and around
     `onEmitterDone`'s check-and-remove, so a reconnect and the old emitter's completion can't
     interleave and leave a live emitter with no owner.
   - `createStream`: filter the initial subscriptions through the access check (outside the lock,
     it reads the database); claim the session (403 when `null`); schedule the new tasks; swap the
     holder in; add the allowed subscriptions; then cancel the replaced holder's tasks and
     complete its emitter. Its completion callback finds it is no longer current and does nothing.
   - `onEmitterDone` always cancels the holder's tasks.
   - `verifyCanSubscribe(username, targetKey)`: 403 unless allowed.
3. **`StreamTargetAccess`** (interface) and **`ProjectStreamTargetAccess`** (`@Transactional(readOnly
   = true)`): broadcasts allowed; otherwise parse `Type:id`, resolve the type through
   `AnnotatableTypeRegistry`, load the entity, find its project and apply `ProjectReadAccess`.
   Any parse or lookup failure is "not allowed".
4. **`ProjectReadAccess`** becomes public (with `canRead` and `projectOf`) so the stream package
   can use it.
5. **`StreamController`:** `POST /subscriptions` checks ownership, then `verifyCanSubscribe`.

## Test plan

- **`StreamSessionStoreTest`:** `claimSession` for a new id, an unknown id, the owner's id and
  another user's id; another user's claim leaves the owner and subscriptions unchanged.
- **`StreamServiceTest`** (mocked scheduler, permissive or restrictive access):
  - another user's reconnect throws 403 and leaves owner, subscriptions, emitter and tasks alone;
  - a same-user reconnect cancels the old holder's tasks, keeps subscriptions, and leaves one
    emitter;
  - an unknown id gets a new server-generated id;
  - unreadable initial subscriptions are skipped, readable ones added;
  - concurrent same-user reconnects (threads) end with one live emitter and every replaced
    holder's tasks cancelled.
- **`StreamControllerTest`:** `POST /subscriptions` returns 403 when the access check refuses.
- **`StreamAccessIT`** (MockMvc, real security and database): a non-member's subscription to a
  goal in another project is 403; a member's succeeds; the broadcasts are allowed; an unknown type
  and a nonexistent id are 403; user B reconnecting with user A's session id gets 403.
- Verify gate: `mvn clean verify` (backend only). The e2e suite (CI) exercises the live stream.

## Acceptance criteria → where

| AC | Covered by |
|---|---|
| Reconnect verifies ownership atomically before changing state | Steps 1–2, `StreamServiceTest`, `StreamAccessIT` |
| A different user is rejected without changing owner, subscriptions, emitter or tasks | Decision 2, `StreamServiceTest` |
| Same-user reconnect replaces the old emitter and cancels its tasks | Decision 3, `StreamServiceTest` |
| Tests cover concurrent reconnects and a cross-user reconnect | `StreamServiceTest`, `StreamAccessIT` |
| (triage) Subscriptions check project read access; broadcasts stay open | Decisions 4–6, `StreamAccessIT` |

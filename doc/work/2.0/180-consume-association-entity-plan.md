# 180 — Consume the entity returned by association commands instead of refetching

## Summary

#178 made the six association commands return their merged container as `result.entity`
(`AddGoalToGoalContainer`, `RemoveGoalFromGoalContainer`, `AddStoryToStoryContainer`,
`RemoveStoryFromStoryContainer`, `AddActorToActorContainer`, `RemoveActorFromActorContainer`).
The four editors that call those commands still ignore that entity and re-read the container over
HTTP — a version-only GET (actor, stakeholder), a full refetch plus a second scenario GET
(use case), or a full refetch behind a sequence guard (story). This ticket deletes that follow-up
read everywhere and takes both the optimistic-lock `version` and the affected collections straight
off `result.entity`.

Net effect: one fewer round trip per association, and the editor stays in sync from the response
rather than from a follow-up read. The principle being settled is that every mutating command
returns its entity and the client consumes it — no command-specific refresh strategy.

## Decisions

1. **One authoritative rule.** On a successful association, set `this.version` from
   `result.entity.version` and replace the affected collection signal(s) from `result.entity`.
   Delete every GET-based refresh (`refreshVersionAfterAssociation`, `refreshCollections` usage in
   the association handlers, `refreshAfterAssociation`) and the optimistic list patch
   (`this.goals.update(...)`) — the response is the single source of truth.

2. **On failure, leave state unchanged.** Keep the current behaviour: set `errorMessage` and touch
   nothing else. A stale held `version` is self-correcting — the next save 409s into the existing
   `recoverFromStaleVersion()` / stale-version recovery path. We do not refetch on failure.

3. **Concurrency: version-monotonic guard, applied uniformly.** Two associations can be in flight
   at once (e.g. two quick remove clicks); their responses can arrive out of order, and an older
   snapshot landing last would silently restore a row the user just removed. Guard every apply with
   `entity.version > heldVersion` (apply, and advance the held version, only when the response is
   newer). Because each successful merge increments `@Version`, the highest version is by definition
   the last-committed / most-complete state — so this keys off the server's own commit order, which
   is strictly more correct than story-editor's current `storyReadSeq` (which keys off client issue
   order). This replaces `storyReadSeq` and is added to all four editors, since after decision 1 all
   four now apply server lists and share the same out-of-order exposure. #178 already excludes the
   originating session from SSE, so an editor never races its own change against an echo.

4. **use-case-editor drops `refreshCollections()` from the association path.** The six association
   handlers stop calling `refreshCollections()` and apply the returned `UseCaseDto` directly. That
   method refetched the use case *and* re-read the primary scenario, so removing it from the
   association path saves two GETs per association; it stays in place solely for
   `createPrimaryScenario()`, which still needs the freshly-created scenario.

Testing is covered in the Test plan below, including the SSE e2e negative assertion #178 deferred to
this ticket (the acting session issues no second container GET after an association).

## Verified starting state (all on `release/2.0`, post-#178)

- `command.service.ts` returns `CommandResult<T>` with `entity` populated from the server's
  primary result extractor; the six commands now return their container DTO.
- DTO shapes the editors will read: `ActorDto { version; goals: EntityReferenceDto[] | null }`,
  `StakeholderDto { version; goals?: EntityReferenceDto[] }`,
  `StoryDto { version; goals: EntityReferenceDto[] | null; actors: EntityReferenceDto[] | null }`,
  `UseCaseDto { version; goals: GoalDto[] | null; stories; actors; additionalScenarios; scenarioId }`.
- `actor-editor.ts` — `onGoalSelected`/`onRemoveGoal` optimistically patch `goals` then call
  `refreshVersionAfterAssociation()` (version-only GET, ~L503).
- `stakeholder-editor.ts` — same pattern; `refreshVersionAfterAssociation()` ~L637.
- `use-case-editor.ts` — `addGoal/removeGoal/addStory/removeStory/addActorToList/removeActor` call
  `refreshCollections()` (~L877), a full `getUseCase` GET that also refetches the primary scenario.
  `refreshCollections` is also used by `createPrimaryScenario` (not an association — stays).
- `story-editor.ts` — the four handlers call `refreshAfterAssociation()` (~L563), a full `getStory`
  GET guarded by `storyReadSeq`.

## Work items

### Shared shape of every association handler (all four editors)

On `result.success`:
- `const entity = result.entity as <Dto> | null;`
- `if (entity && (this.version == null || entity.version > this.version)) { this.version = entity.version; <set affected signals from entity>; }`
- keep the existing success toast; keep the `else`/`catch` → `errorMessage` untouched.

### 4.1 actor-editor.ts
Replace the optimistic `goals.update(...)` + `await refreshVersionAfterAssociation()` in
`onGoalSelected` and `onRemoveGoal` with the guarded apply, setting `goals` from
`entity.goals ?? []`. Delete `refreshVersionAfterAssociation()` and its now-unused
`actorService.getActor` import if nothing else uses it (check: `loadActor` still does).

### 4.2 stakeholder-editor.ts
Same as 4.1, setting `goals` from `entity.goals ?? []`. Delete
`refreshVersionAfterAssociation()`; keep `stakeholderService.getStakeholder` if `loadStakeholder`
still uses it (check).

### 4.3 use-case-editor.ts
In the six association handlers, replace `await refreshCollections()` with the guarded apply,
setting `version`, `useCase`, `goals`, `stories`, `actors`, `additionalScenarios` from the returned
`UseCaseDto`. Do **not** touch `primaryScenario` — associations never change it, which also drops
the second `getScenario` GET. Keep `refreshCollections()` (still used by `createPrimaryScenario`).

### 4.4 story-editor.ts
Replace `await refreshAfterAssociation()` in the four handlers with the guarded apply, setting the
`story` signal and `version` from the returned `StoryDto`. Delete `refreshAfterAssociation()` and
the `storyReadSeq` field (the monotonic version guard replaces it).

## Test plan

- **Unit — each editor spec** (actor, stakeholder, use-case, story): a successful association
  applies `version` and the affected list(s) from `result.entity` and issues **no** follow-up
  container GET (assert the query service GET is not called). Add an out-of-order case: a stale
  (lower-version) response arriving last is ignored. Update/remove specs that asserted the old
  refresh methods.
- **Unit — failure**: on `result.success === false`, state is unchanged and `errorMessage` is set;
  no GET issued.
- **e2e — `sse-refresh.e2e.ts`**: extend the association test so the acting context issues no second
  detail GET after the association (the negative assertion deferred from #178), while a second
  context still refreshes via SSE.
- Gate: `mvn clean verify` (unaffected, but run) and `npm test`; Playwright e2e as available.

## Acceptance criteria

- No association handler in any of the four editors issues a follow-up GET on success.
- After add/remove, the held `version` and the on-screen collections match the command response,
  and an immediate save does not 409.
- Two rapid associations cannot leave a stale list on screen (monotonic guard).
- `refreshVersionAfterAssociation` (actor, stakeholder), the association usage of
  `refreshCollections` (use case), `refreshAfterAssociation` and `storyReadSeq` (story) are gone.

## Not in scope

- Client-side serialization / disabling controls while an association is in flight (Option C in the
  concurrency discussion) — a possible follow-up, not needed given the monotonic guard.
- The rarer server-side case where two truly-concurrent association transactions collide into a 409
  on the second — unchanged behaviour, surfaces through the existing error path.
- Any backend change: #178 already returns the entity.

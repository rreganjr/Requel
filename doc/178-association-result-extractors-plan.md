# 178 — Result extractors for the six association commands

Plan for https://github.com/rreganjr/Requel/issues/178. Written 2026-08-18, before any code.

## Summary

Six association commands register through the 4-arg `CommandRegistry.register`, which passes `null`
for the result extractor. `ApiCommandFactory.extractResult` therefore returns `null`, so
`CommandController.publishEntityChangedIfPresent` returns early and no targeted SSE event fires, and
`result.entity` is empty so the client cannot read the container's bumped `@Version`. Registering
extractors that return the merged container's **detail** DTO fixes both.

Three things ride along, decided 2026-08-18 rather than deferred:

1. The event must **not** go back to the session that issued the command.
2. The associated child (the goal / story / actor) needs its own event, because its editor's
   *Referenced By* table is what goes stale.
3. The MCP gateway path must publish the same events as the HTTP path.

The client-side half — deleting `refreshVersionAfterAssociation()` and the association
`refreshCollections()` calls — stays in #180 and lands after this.

## Decisions

| Question | Decision |
|---|---|
| The issuing session receives its own targeted event | **Exclude the originator.** |
| Container is the project itself (`ProjectOrDomain` is a `GoalContainer`, `StoryContainer` and `ActorContainer`) | **Extractor returns `null`.** No component subscribes to `Project:<realId>`; the sidebar's channel is the fixed `Project:0` broadcast, which `publishProjectChangedIfScoped` already fires for these project-scoped commands. Nothing to target, so nothing to publish. |
| Publish an event for the associated child too | **In scope** (§4.3). |
| MCP gateway (`InProcessCommandGateway`) parity | **In scope** (§4.5). |
| Detail or summary DTO | **Detail** — `toActorDetailDto` / `toStoryDetailDto` / `toUseCaseDetailDto` / `toStakeholderDetailDto`. #180 needs the collections and `version`, both of which the detail form carries. |

## Verified starting state

Checked against `release/2.0` on 2026-08-18. Line numbers are for orientation, not contracts.

| Fact | Where |
|---|---|
| The six 4-arg registrations | `ProjectCommandRegistrar` 326, 337, 390, 400, 430, 440 |
| The three that already work, for contrast | same file, 490, 508, 519 — each passes `cmd -> ProjectQueryController.toUseCaseDetailDto(...)` |
| Targeted publish derives type from the DTO class name and id from `id()` | `CommandController.publishEntityChangedIfPresent`, 178–191 |
| `Project:0` broadcast — fixed channel, id `0` is not an entity | `CommandController.PROJECT_BROADCAST_ID` 61, publish at 222; client joins it in `layout.ts:175` (`connect(['Project:0'])`); consumed in `sidebar-nav.ts:259` |
| No originator exclusion anywhere in the push path | `StreamService.pushToSubscribedSessions` 161–181 iterates every subscribed session |
| The client already has a session id, and already sends it — but only on subscribe / unsubscribe | `event-stream.service.ts` 39, 99, 118, 268; `command.service.ts` sends no such header |
| Editors reload unconditionally on a matching envelope | `actor-editor.ts:477`, `story-editor.ts:534`, `use-case-editor.ts:745`, `stakeholder-editor.ts:574` |
| Detail DTOs carry `version` and the association collections | `toActorDetailDto` (`ProjectQueryController` 696) returns `version`, `goals`, `referencedByUseCases`, `referencedByStories` |
| The child's editor really does render the stale table | `goal-editor.ts:153` renders `goal().referencedBy`, fed by `goal.getReferers()` in `toGoalDetailDto` |
| The gateway extracts a result but publishes nothing | `InProcessCommandGateway` 100–105 |

## 4. Work items

### 4.1 Polymorphic container → DTO helper

One static helper, next to the other mappers in `ProjectQueryController` (it already owns every
`toXxxDetailDto` and is already imported by the registrar):

```java
/** null when the container is the project itself — see the plan doc, §Decisions. */
public static Object toContainerDetailDto(Object container)
```

Arms, most specific first, because `UseCase` and `Story` implement several of these interfaces:
`UseCase` → `toUseCaseDetailDto`, `Story` → `toStoryDetailDto`, `Actor` → `toActorDetailDto`,
`Stakeholder` → `toStakeholderDetailDto`, `ProjectOrDomain` → `null`. Anything else → `null` plus a
`log.warn`, so a future container type shows up in the logs instead of silently going quiet.

Order matters and is not incidental: `Stakeholder` extends `GoalContainer` only, while `UseCase`
extends `GoalContainer`, `ActorContainer` and `StoryContainer`. Write the test to pin the order.

### 4.2 Register the extractors

The six registrations move from the 4-arg to the 5-arg overload, passing `null` for the file
applicator and `cmd -> ProjectQueryController.toContainerDetailDto(((XCommand) cmd).getYContainer())`.
`AddScenarioToUseCase` at 490 is the shape to copy.

Nothing in `CommandController` changes for this item — `publishEntityChangedIfPresent` derives
`"Actor"` from `ActorDto` on its own, and returning `null` for the project case lands on the existing
early return.

### 4.3 Publish for the associated child as well

The response body must stay the container, because #180 reads `result.entity` for the container's
version. So the child is **publish-only** and never enters the response.

Add an eighth component to `CommandRegistration`, `Function<Command, Object> secondaryResultExtractor`,
plus one new full `register` signature. Every existing overload default passes `null`, so no other
registration changes. `ApiCommandFactory` gains `extractSecondaryResult`; `CommandController` publishes
for both extractions, skipping the second when it resolves to the same `(type, id)` as the first.

For these six, the child is on the command already — `getGoal()`, `getStory()`, `getActor()` — so the
extractors are `cmd -> toGoalDetailDto(((AddGoalToGoalContainerCommand) cmd).getGoal())` and siblings.
Mapping a whole detail DTO only to read its `id()` is wasteful, but it keeps one code path in
`publishEntityChangedIfPresent` and the mapping is in-session and cheap. Revisit only if it shows up
in a profile.

### 4.4 Exclude the originating session

Half the plumbing exists: the client holds a `sessionId` and already sends `X-Session-Id` on the
subscribe and unsubscribe calls. The command POST does not send it.

- `command.service.ts` — add `X-Session-Id` from `eventStreamService.sessionId()` when it is
  non-null. Omit the header entirely when null (no stream open yet) rather than sending an empty
  value.
- `CommandController` — `@RequestHeader(value = "X-Session-Id", required = false) String sessionId`
  on both `@PostMapping`s, threaded into `dispatch`.
- `StreamEventPublisher.publishTargetUpdate` — new overload taking `String excludeSessionId`; the
  3-arg form delegates with `null`. `StreamService.pushToSubscribedSessions` skips that id.
- **The `Project:0` broadcast keeps no exclusion.** The acting session's own sidebar counts do need
  to change when it adds an association, so only the *targeted* events are filtered.
- The gateway has no HTTP session, so it excludes nothing (passes `null`).

A missing or unknown header must degrade to "exclude nobody" — never to "publish nothing".

### 4.5 Gateway parity

`InProcessCommandGateway` (100–105) executes and extracts but never publishes. Add the same two
publishes — targeted primary, targeted secondary, and the `Project:0` broadcast — so an association
made over MCP refreshes open browser sessions. The two call sites now share enough logic to be worth
lifting into one collaborator that both the controller and the gateway hold; that refactor is
preferable to a second copy of the reflection block in `publishEntityChangedIfPresent`.

Note #188 edits the catch arms of this same `execute` method. Whichever lands second rebases; the
changes do not overlap in substance (publishing after the try, classification inside the catches).

## 5. Test plan

Unit, `ProjectQueryControllerTest` (or a new `ContainerDtoMappingTest`):

- One case per arm of §4.1, including `UseCase` resolving to `toUseCaseDetailDto` rather than to a
  `StoryContainer`/`ActorContainer` arm, and `ProjectOrDomain` → `null`.

Unit, `CommandControllerTest` (already mocks `streamEventPublisher` — see its existing
`publishTargetUpdate` verifications):

- Per command, the extractor returns the expected DTO type and `result.entity` carries `version`.
- A targeted event is published for the container with the expected `(type, id)`.
- A targeted event is published for the child.
- With `X-Session-Id` present, that session is excluded and the others still receive the event.
- With the header absent, nobody is excluded.
- Container is the project → no targeted event, no throw, `Project:0` still published.

Unit, `StreamServiceTest`: `pushToSubscribedSessions` with an excluded id skips exactly that session.

Unit, Angular: `command.service.spec.ts` — header present when `sessionId()` is set, absent when null.

e2e, extending `sse-refresh.e2e.ts`: two sessions on the same actor; session A adds a goal; session B's
Goals table updates without a reload; session A issues **no** second detail GET. The negative
assertion is the point of §4.4 and should fail if the exclusion regresses.

Whole-suite gate per CLAUDE.md step 3 before any commit.

## 6. Acceptance criteria

- All six commands return the merged container as its detail DTO in `result.entity`, carrying the
  bumped `version`.
- A session with the container open, other than the issuing one, receives exactly one targeted
  refresh for that container per association.
- The issuing session receives no targeted event for the container, and does not reload its form.
- Sidebar counts still refresh in every session including the issuing one — the `Project:0`
  broadcast is unchanged.
- A session with the associated goal, story or actor open receives a targeted refresh, so its
  *Referenced By* table no longer goes stale.
- An association issued through the MCP gateway publishes the same events as the HTTP path.
- When the container is the project itself, no targeted event fires and nothing throws.
- A missing `X-Session-Id` excludes nobody rather than suppressing the event.

## 7. Not in scope

- Deleting `refreshVersionAfterAssociation()` and the association `refreshCollections()` calls —
  that is #180, and it lands after this.
- Editor form-reset guarding on SSE reload — #185 / #186. §4.4 reduces how often that guard is
  reached; it does not replace it.
- `AssignTag` (mutates `Tag`, no parent merge) and the four commands unreachable from the client per
  `doc/173-create-flow-wizards-plan.md` §3.
- Multi-instance fan-out. `StreamEventPublisherImpl` is deliberately local-only; its own comment
  names Redis pub/sub as the eventual replacement.
- How the gateway classifies a bad id (`EXECUTION_ERROR` vs `INVALID_INPUT`) — #188. §4.5 only adds
  publishing to that method; it does not touch the catch arms.
- The two container-lookup defects in §8 — both are pre-existing, neither blocks this work. Filed
  as #187 and #189.

## 8. Defects found while scoping, each wanting its own ticket

**8.1 `findStoryContainerById` casts a `Stakeholder` to `StoryContainer`. Filed as #187.**
`ProjectCommandRegistrar` 780–782 loops the project's stakeholders and casts a match. But
`Stakeholder extends ProjectOrDomainEntity, GoalContainer` — not `StoryContainer` — and
`AbstractStakeholder implements Stakeholder` alone, holding `Set<Goal> goals` and no stories. A
stakeholder holds goals; a story's actors are a different relationship, on `Story`, which does
extend `ActorContainer`. So that branch can only ever throw `ClassCastException`, and it is
reachable: ids are per-table auto-increment and the stakeholder loop runs *before* the use-case
loop, so a stakeholder sharing an id with the intended use-case wins the lookup. Delete the branch.

**8.2 Story / actor container lookups have no `containerType`, so ids collide. Filed as #189.**
`AddGoalToGoalContainerInput` carries `containerType` and `findGoalContainerById` (734) switches on
it — its comment names the reason: "avoid ID collisions between entity types (all tables use
per-table auto-increment)". `AddStoryToStoryContainerInput` and `AddActorToActorContainerInput` carry
no such field, and `findActorContainerById` (789) scans use-cases before stories. Adding an actor to
Story #5 therefore resolves to UseCase #5 when one exists — wrong entity, silently. Add
`containerType` to both inputs and switch on it, matching the goal path.

Both matter to this ticket only in that an extractor is only as correct as the container handed to
it. Fixing them here would mean touching four input DTO records and every client call site, which is
a different change with a different blast radius.

Sequencing: #187 (delete the dead branch) → #189 (rewrite the helpers around `containerType`) →
this ticket, which only reads whatever container those helpers return. None of the three blocks the
others, but landing them in that order avoids rewriting the same helper twice.

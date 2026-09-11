# 253 — `positionType` leaks a CGLIB proxy class name

Milestone **v2.0**, labels `bug` / `gateway`. Backend-only Java work, no Angular change. The
reported symptom is `"positionType": "PositionImpl$$EnhancerByCGLIB$$1f1035a5"` on the `EditPosition`
response; the audit below widens that to `EditIssue` and narrows AC 3 to two call sites.

## Summary

One DTO mapper derives a string from `getClass()` on an entity that the repository/command advice
has always wrapped in a CGLIB proxy. The fix is to stop asking the object what it is and ask the
persisted discriminator instead, through a helper that already exists in spirit inside
`AnnotationCommandFactoryImpl`. The real work is extracting that helper, applying it to the two call
sites that need it, and adding the Java-side tests that do not exist for this DTO at all today.

## Verified against `release/2.0` (`ba2d307f`)

- **The write path leaks deterministically, not intermittently.** `DomainObjectWrappingAdvice`'s
  pointcut is `execution(* AbstractJpaRepository+.*(..)) || execution(* Command+.get*(..))`.
  `AnnotationCommandRegistrar:162` maps the `EditPosition` response with
  `toPositionDto(((EditPositionCommand) cmd).getPosition())` — a `Command.get*` call — so the mapper
  never sees an unwrapped position.
- **`EditIssue` has the same defect in every nested position.** `AnnotationCommandRegistrar:136`
  maps `toIssueDto(((EditIssueCommand) cmd).getIssue())`, and `DomainObjectWrapper` re-wraps
  collection entries on traversal (`wrapPersistentEntities` → `wrapCollectionEntries` →
  `wrapEntity`), so `issue.getPositions()` inside `toIssueDto:261` yields wrapped positions too.
  Fixing only `EditPosition` leaves this live.
- **`GET /api/annotations` is clean of CGLIB.** `AnnotationQueryController:73` loads through
  `entityManager.find(...)`, not `AbstractJpaRepository`, so the advice never fires there. The
  residual risk on that path is a Hibernate proxy.
- **No live UI mislabel today.** `annotations-section.ts` re-`load()`s over the GET after every
  mutation (11 call sites) and never reads `positionType` out of a command response, so the
  `default: 'Ignore'` fallback is not firing in the browser. The damage is confined to the API
  contract, which is where the MCP gateway clients live. Both `EditIssue` and `EditPosition` are on
  `GatewayPolicyConfig.ALLOWED`.
- **`wrapEntity` unwraps a `HibernateProxy` before wrapping** (`DomainObjectWrapper:126-129`) and
  builds the proxy with `Enhancer.setSuperclass(entityType)`, so on the write path the CGLIB proxy
  is a subclass of the *concrete* position class. Tier 3 alone would therefore fix the reported bug —
  the other tiers are what make the helper correct on the read path and for lazy associations.
- **`PositionImpl.getType()` is public** (`:136`, mapped to the `position_type` discriminator column
  with `insertable = false, updatable = false`); only `setType` is protected. A helper outside the
  `impl` package can read it.
- **AC 3 audit — `positionType` is the only offender.** Every sibling `*Type` DTO field is already
  proxy-safe: `StoryDto.storyType` and `ScenarioDto`/`StepDto.scenarioType` use enum `.name()`,
  `StakeholderDto.type` is a literal, `OpenIssueDto.entityType` and `ProjectQueryController:1002`
  use `getProjectOrDomainEntityInterface().getSimpleName()`, `TagQueryController:134` already uses
  `ClassUtils.getUserClass` plus the registry discriminator. `CommandEventPublisher:87` reflects
  over a DTO record, never an entity. `CommandEventPublisher:125`,
  `ProjectQueryController:1053` and `JpaAssistantRunStore:134` are log/message text only.
  The single other reachable site is `IssueContextPackBuilder:163`.
- **No Java-side coverage exists.** Nothing under `modules/*/src/test` references `PositionDto` or
  `positionType`; the only fixture is Angular's `annotations-section.spec.ts:119`. There is also no
  test anywhere for `AnnotationCommandFactoryImpl.newResolveIssueCommand`, whose three-tier logic
  this ticket promotes to shared code.
- **`annotation-jpa` has no test sources and no test-scoped dependencies** — the helper's unit test
  is the first one in that module and needs a pom entry.

## Locked decisions

1. **The field stays.** "or the field is removed" is struck from AC 1: `annotations-section.ts:560
   resolveLabel()` switches on it and `models/annotation.ts:34` types it as a required `string`.
2. **The value is the persisted discriminator, pretty-named** — strip the package, strip a trailing
   `Impl`. Not a new domain enum.
3. **One shared helper**, extracted from `AnnotationCommandFactoryImpl.newResolveIssueCommand`, used
   by both the resolver and the DTO mapper so they can never disagree about what a position is.
4. **AC 3 is done in this ticket**, and the audit above bounds it to two call sites.
5. **Java-side tests are part of this ticket**, including the first tests in `annotation-jpa`.
6. **No Angular change.** The four subclass values the UI switch names are unchanged; only the base
   case moves, and it lands on the same `default`.

## Contracts

### `positionType` values

| discriminator (`position_type`) | `positionType` |
| --- | --- |
| `…project.impl.AddActorPosition` | `AddActorPosition` |
| `…project.impl.AddGlossaryTermPosition` | `AddGlossaryTermPosition` |
| `…annotation.impl.AddWordToDictionaryPosition` | `AddWordToDictionaryPosition` |
| `…annotation.impl.ChangeSpellingPosition` | `ChangeSpellingPosition` |
| `…annotation.impl.PositionImpl` | `Position` |

The four subclass values match what `resolveLabel()` already expects. `PositionImpl` → `Position`
drops the last implementation detail out of the API and still hits the switch's `default`
("Ignore"), so UI behaviour is identical.

### Helpers

Two pieces, split so that neither module reaches somewhere it should not:

```java
// platform-core: com.rreganjr.repository.jpa.ProxyTypes — entity-agnostic
public static Object unwrap(Object candidate);        // EntityProxy → entity, HibernateProxy → impl
public static Class<?> userClassOf(Object candidate); // unwrap, then ClassUtils.getUserClass
```

```java
// annotation-jpa: com.rreganjr.requel.annotation.impl.PositionTypes — position-specific
public static String typeNameOf(Position position);   // the table above
```

`PositionTypes.typeNameOf` keeps the existing three-tier order, with the pretty-name applied to
whichever tier answers:

1. `HibernateProxy` → `getHibernateLazyInitializer().getEntityName()`
2. `PositionImpl.getType()` (the discriminator), when non-null
3. `ProxyTypes.userClassOf(position).getName()`

`platform-core` already imports both `HibernateProxy` (`DomainObjectWrapper:127`) and
`ClassUtils`, and owns `EntityProxyInterceptor.unwrap`, so `ProxyTypes` adds no dependency.
`service-impl` and `assistant-core` both already depend on `annotation-jpa` and `platform-core`.

## Changes

1. **`modules/platform-core/.../repository/jpa/ProxyTypes.java`** (new) — `unwrap` /
   `userClassOf` as above, delegating to `EntityProxyInterceptor.unwrap`, the `HibernateProxy`
   initializer and `ClassUtils.getUserClass`, in that order.
2. **`modules/annotation-jpa/.../annotation/impl/PositionTypes.java`** (new) — `typeNameOf`, the
   three tiers, and the private pretty-name function (strip package, strip trailing `Impl`).
3. **`modules/annotation-jpa/.../annotation/impl/command/AnnotationCommandFactoryImpl.java`** —
   `newResolveIssueCommand` keeps its behaviour but sources its tier-1/tier-2/tier-3 lookups from
   the shared code rather than inline blocks. `entityNameToResolver` stays keyed on the FQCN, so
   the helper must expose the *raw* resolved name to the resolver and the pretty name to the DTO;
   `typeNameOf` returns the pretty name and `rawTypeNameOf` returns the FQCN. Both are public: the
   factory sits in `annotation.impl.command`, a different package from `annotation.impl`, so
   package-private would not reach it.
4. **`modules/service-impl/.../service/command/AnnotationCommandRegistrar.java:285`** — replace
   `position.getClass().getSimpleName()` with `PositionTypes.typeNameOf(position)`. This fixes the
   `EditPosition` response and, through `toIssueDto:261`, the `EditIssue` response's nested
   positions in the same line.
5. **`modules/service-api/.../dto/PositionDto.java`** — update the record's javadoc, which currently
   documents the buggy contract ("the simple class name … e.g. `PositionImpl`") and would otherwise
   be the only surviving description of the old behaviour.
6. **`modules/assistant-core/.../context/IssueContextPackBuilder.java:163`** — `simpleType`'s
   fallback becomes `ProxyTypes.userClassOf(entity).getSimpleName()`. The
   `getProjectOrDomainEntityInterface()` preference is unchanged and still wins when non-null.
7. **`modules/annotation-jpa/pom.xml`** — add `spring-boot-starter-test` at test scope (first test
   sources in the module).

### Tests

8. **`modules/annotation-jpa/src/test/.../annotation/impl/PositionTypesTest.java`** (new) — the
   helper across all three tiers, no Spring context:
   - plain `PositionImpl` → `Position`; plain `AddWordToDictionaryPosition` and
     `ChangeSpellingPosition` → their own names;
   - a CGLIB subclass built with `Enhancer.setSuperclass(...)` (the same shape
     `DomainObjectWrapper.getEntityFactory` produces) over each of those → identical results, and
     the returned string contains no `$$` and no `.`;
   - a mocked `HibernateProxy` whose `LazyInitializer.getEntityName()` returns
     `…project.impl.AddActorPosition` → `AddActorPosition`, proving the tier that covers a type this
     module cannot import;
   - a `PositionImpl` whose discriminator is a subclass FQCN → the subclass name, pinning tier 2;
   - `null` → `null`, since `toPositionDto` is null-guarded but the helper should not blow up.
9. **`modules/requel-app/src/test/.../service/GatewayPositionTypeIT.java`** (new, extends
   `AbstractIntegrationTestCase`) — the AC 2 guard, end to end through `CommandGateway`:
   - dispatch `EditIssue` then `EditPosition`, serialize each `GatewayResult` with the application's
     `ObjectMapper`, and assert **two different things at two different scopes** (see "Scoping the
     `$$` assertion" below): every `positionType` value in the parsed tree matches
     `^[A-Za-z0-9]+$`, and the serialized document contains no `$$EnhancerBy`;
   - give one position `text` a literal `$$` (e.g. `"cost is $$ per seat"`), assert it round-trips
     byte-for-byte in the response, and assert neither of the two checks above trips on it;
   - assert the base position's `positionType` is exactly `Position`;
   - create an `AddWordToDictionaryPosition` through
     `getAnnotationCommandFactory().newEditAddWordToDictionaryPositionCommand()` and
     `getCommandHandler()`, attach it to the same issue, re-dispatch `EditIssue`, and assert the
     nested position's `positionType` is `AddWordToDictionaryPosition` — this is the assertion that
     would have caught the `EditIssue` leak, and the one that proves a subclass survives proxying;
   - assert the same over `GET /api/annotations` for the same issue, so the read path is pinned
     against a future regression even though it is clean today.
10. **`modules/requel-app/src/test/.../annotation/ResolveIssueCommandSelectionIT.java`** (new) —
    `newResolveIssueCommand` still selects the right resolver for a plain position, a CGLIB-wrapped
    position and a subclass position after the extraction. There is no existing test for this and
    the refactor in change 3 is otherwise unguarded.

### Scoping the `$$` assertion

Nothing in this change rewrites text. `toPositionDto` copies `position.getText()` through verbatim
(`AnnotationCommandRegistrar:289`) and the pretty-name function is applied only to the type string
resolved from the discriminator, so a `$$` a user types into an issue or position survives
untouched. The risk is in the *test*, not the code: a blanket "no `$$` anywhere in the response
body" sweep would turn any future fixture containing `$$` in a text value into a red build that
looks like a proxy leak. So the two checks are deliberately scoped differently:

- **Field-scoped, strict.** Parse the response and assert every `positionType` value in the tree
  matches `^[A-Za-z0-9]+$`. This is where a proxy name would actually appear, and the pattern
  rejects `$$`, package dots and the ByteBuddy `$HibernateProxy$` shape alike — it is stricter than
  the AC's substring check and cannot be tripped by user text.
- **Document-scoped, narrow.** Assert the serialized body contains no `$$EnhancerBy`, which is the
  literal AC. That exact string appearing in a text value would have to be deliberate, so a
  document-wide sweep for it is safe where a sweep for bare `$$` is not — and the `$$` fixture above
  proves the two do not collide.

**Shared-context discipline** (per CLAUDE.md): both ITs create their own project, issue and
positions inside the test method, assume no id values, and never read "the first row".

## Test plan

Gate: `mvn clean verify`, wrapped in `tmp/253-verify.sh`. No `requel-angular/**` change, so no
vitest, no tsc, no e2e.

```bash
mvn -pl modules/annotation-jpa test -Dtest='PositionTypesTest'
mvn -pl modules/requel-app -am test -Dtest='GatewayPositionTypeIT,ResolveIssueCommandSelectionIT'
mvn clean verify
```

## What verification changed

The plan's test design survived; three fixture assumptions in it did not, and each is now commented
in the test rather than left as tribal knowledge.

- **A subclass position cannot hang off a plain issue.** `EditAddWordToDictionaryPositionCommandImpl:58`
  and `EditAddActorToProjectPositionCommandImpl:80` both cast the issue to `LexicalIssue` and read
  its `word`. So the subclass case is a separate test that builds a lexical issue the way spell-check
  analysis does, and the `EditIssue` nesting case uses two plain positions instead.
- **Position texts must be unique within a grouping object.** `EditPositionCommandImpl.execute`
  looks up an existing position by text and, on the found path, throws the result away and
  dereferences a null. Pre-existing and unrelated to this ticket — filed as #281.
- **The read-path test needs `@Transactional`.** `AnnotationQueryController` walks a lazy
  `annotations` collection; called in-process it has no session. `TagApiIT` solves this the same way.

## Revised AC mapping

| AC | Covered by |
| --- | --- |
| 1. `positionType` is the pretty-named discriminator on every response carrying a position, nested ones included | Change 4; `GatewayPositionTypeIT` (`EditPosition`, `EditIssue` nested, `GET /api/annotations`) |
| 2. No write response contains `$$EnhancerBy` | `GatewayPositionTypeIT` — field-scoped `positionType` pattern check plus a document-scoped `$$EnhancerBy` sweep |
| 3. One shared helper behind both the DTO mapper and the resolver | Changes 1-3; `PositionTypesTest`, `ResolveIssueCommandSelectionIT` |
| 4. `IssueContextPackBuilder`'s fallback goes through the helper | Change 6 |
| 5. Java-side unit coverage across all three tiers | `PositionTypesTest` |

## Out of scope

- A `PositionType` domain enum on the `Position` interface, and any Angular change.
- The `resolveLabel()` switch itself — the four values it names are unchanged.
- `CommandEventPublisher:125`, `ProjectQueryController:1053`, `JpaAssistantRunStore:134`: log and
  message text, deliberately left alone.
- Removing or narrowing `DomainObjectWrappingAdvice`. The wrapping is load-bearing (it is what keeps
  lazy associations loadable outside a transaction); this ticket makes one consumer proxy-safe, it
  does not relitigate the aspect.
- The `TODO(#43)` hardcoded `0` version in the same mappers.

## Risks

- **`positionType` is an API contract change for the base case** (`PositionImpl` → `Position`). No
  in-tree consumer depends on the old value — the Angular switch never names it and falls through to
  `default` either way — but an external MCP client that special-cased the string would see the
  change. It is a bug fix to an unstable value, so this is the right moment; call it out in the PR
  body.
- **Change 3 is a refactor of live resolver logic with no existing test.** Test 10 is written first,
  against the current behaviour, so the extraction is verified rather than assumed.
- **Tier ordering matters.** `DomainObjectWrapper.wrapEntity` unwraps a `HibernateProxy` before
  wrapping, so tier 1 fires on the read path and tier 3 on the write path. The unit test covers each
  tier in isolation so a future reordering fails loudly.
- **First test sources in `annotation-jpa`** — surefire is configured with `failIfNoTests=false`, so
  the module builds today with none; adding the pom dependency is the only change needed, but it
  does put that module into the test lifecycle for the first time.

## Process (CLAUDE.md)

Branch `253-position-type` off `release/2.0`; `tmp/253-verify.sh` → `mvn clean verify`; `commit.md`
with `Closes #253`; `pr.md` for the PR body; the developer runs every `git`/`gh` command.

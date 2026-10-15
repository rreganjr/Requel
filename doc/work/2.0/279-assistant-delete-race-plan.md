# 279 — an assistant run that applies after `DeleteProject` leaves orphan annotations

## Summary

`AssistantRunWorker` splits a run into two transactions (#247): *analyze* (slow, writes
nothing) then *apply* (short, writes the findings). Between them the user can delete the
project. The apply phase's only guard is a re-check of the **target** entity:

```java
if (loadTarget(request).isEmpty()) {
    return "Target ... no longer exists; findings discarded";
}
```

That is not enough, for two reasons:

1. It checks the target, never the **grouping object**. `DeleteProject` deletes children
   before the project row, so there is a window in which the goal/story the run targeted
   is already gone *and* one in which it still exists while the project is mid-cascade.
2. Even when the check passes, nothing stops the delete from committing one microsecond
   later. The check and the inserts are not serialized against the delete at all.

The rows that survive are genuinely unreachable: `annotations.grouping_object_id` is an
`@Any` soft FK with **no** database constraint (`V1__init.sql:98`), and
`annotation_annotatable.annotatable_id` likewise. So MySQL happily accepts an annotation
filed under a `pods` row that no longer exists, plus its link rows pointing at a deleted
goal. `DeleteProjectCommandImpl` step 11 already sweeps exactly this shape
(`DeleteAnnotationGroupCommand`) — it just runs *before* the losing assistant writes.

Fix: make the project row a serialization point both paths take first, add a `CANCELLED`
terminal state for a run that loses the race, and ship a Flyway migration that cleans up
the orphans already in production databases.

## Verified against `release/2.0` (`59abe7bf`)

| Claim | Evidence |
| --- | --- |
| apply re-checks only the target | `AssistantRunWorker.java:249-266` |
| both phases are `REQUIRES_NEW`, i.e. independent transactions | `AssistantRunWorker.java:118-123` |
| `CANCELLED` already exists in the enum and is **used nowhere** | `AssistantRunStatus.java:32`; `grep -rn CANCELLED` returns that one line |
| `status` is `VARCHAR(20)`, so `CANCELLED` (9) needs no column change | `V8__assistant_runs.sql:22`, `AssistantRunEntity.java:93` |
| run status is not exposed to `service-api`/`gateway-api`/Angular | `grep -rln AssistantRunStatus modules/service-* modules/gateway-api` → no hits |
| `annotations.grouping_object_id` has no FK | `V1__init.sql:93-116` — FKs only on `created_by_id`, `resolved_by_position_id`, `resolved_by_user_id` |
| `annotation_annotatable.annotatable_id` has no FK | `V1__init.sql:82-88` — FK only on `annotation_id` |
| `DeleteProject` already sweeps the grouping object last | `DeleteProjectCommandImpl.java:265-275` (step 11), project row deleted at step 12 |
| the project row is read unlocked at the top of the cascade | `DeleteProjectCommandImpl.java:135` `getRepository().get(getProject())` |
| `AnalysisRequest.projectRef` is **nullable** | compact ctor null-checks every field *except* `projectRef`; `AnalysisRequestDispatcher.java:80-83` passes `null` when `target.getProjectOrDomain()` is null |
| the project table is `pods` | `AbstractProjectOrDomain.java:90` |
| there is no pessimistic locking anywhere in the codebase today | `grep -rni "LockModeType\|PESSIMISTIC\|for update" modules/*/src/main` → only prose about *optimistic* locking |
| `Repository` is deliberately JPA-free | `grep -rn jakarta.persistence modules/platform-core/src/main/java/com/rreganjr/repository/*.java` → no hits |
| `DeleteProjectMySqlIT` inherits every parent test method | `DeleteProjectMySqlIT.java:43` `extends DeleteProjectIT` |
| that IT already pins a short lock wait | `DeleteProjectMySqlIT.java` `connection-init-sql` → `SET SESSION innodb_lock_wait_timeout = 10` |
| next Flyway version is `V17` | `ls db/migration` → highest is `V16` |

## Where this ticket came from

Issue #279 was written off an earlier investigation, which lived on the
`279-orphan-assistant-annotations` branch as `doc/279-orphan-assistant-annotations-plan.md`
plus two scripts in the (gitignored) `tmp/`. That branch is being deleted now that this one
supersedes it, so the parts worth keeping are folded in below — they are the only record of
what the bug actually looked like in a database, and they caught two data-loss bugs in the
first draft of `V17`.

### Evidence from a dev database

**32 orphan rows**, spanning **six deleted projects** and six days of e2e runs. Every one:

- `annotation_type` = `LexicalIssue`
- `source` = `ASSISTANT:legacy-lexical*` (the `source` column added in `V8__assistant_runs.sql`)
- `grouping_object_type` = `'Project'`
- `grouping_object_id` resolving nowhere

That last pair is what makes `V17`'s `grouping_object_type = 'Project'` filter the right one:
the observed orphans really are project-grouped, not grouped under a goal or story that the
pods-only check would have mis-flagged. **26 of the 32** still carried an
`annotation_annotatable` row pointing at a deleted entity; the six newest carried none.

### The open question this plan does not answer

The earlier plan's *first* task was to establish why those 26 have link rows at all, given
that the applicator already skips actions whose annotatable "did not resolve". Two candidate
explanations, and it warned they lead to different fixes:

1. the target resolved through a stale first-level-cache / detached reference rather than a
   fresh read; or
2. those rows were written *before* the delete, and only their **group** sweep was missed.

This work does not settle it, and does not need to: the gate means nothing is written at all
once the project row is gone, whichever branch `resolveAnnotatable` took, and `V17` collects
both shapes. But if the answer is (2), the cause is a separate defect in the sweep that
nothing here touches. Worth its own issue rather than being quietly closed with this one.

### Where this plan diverges from the earlier one

| earlier plan | here | why |
| --- | --- | --- |
| guard in `CommandBackedAssistantResultApplicator`, per action | gate in `AssistantRunWorker.apply()`, once per run | one level up: it covers every action including the first, and satisfies that plan's own risk note about not putting an existence read inside a loop over actions |
| re-resolve `projectRef` as a freshness *check* | take the project row's **write lock**, which also answers the question | a check alone is still racy — the delete can commit between the check and the inserts |
| reuse `AssistantTargetLoader` as the seam | new `AssistantProjectGate` SPI | the loader answers "does this target exist", not "hold this row". The module constraint it was protecting (`assistant-core` must not depend on `project-jpa`) is respected either way |
| mark in-flight runs cancelled in `DeleteProjectCommandImpl` | not done | you chose the lock over a cancellation registry when we locked decisions; the lock subsumes it |
| lean toward a checked-in script + `RELEASE.md` note rather than Flyway | Flyway `V17` | your call, made deliberately — noting the earlier reasoning existed |

### What the scripts caught

`tmp/delete-orphan-annotations.sql` below is the hand-written sweep `V17` was eventually
written to replace. Reading it *after* writing `V17` found two ways the migration could have
destroyed live data, both since fixed:

1. **A broken group was the only deletion condition.** An annotation filed under a deleted
   project but still annotating a *live* entity was collected, taking a live entity's
   annotation with it. `V17` now also requires that nothing live still references it.
2. **`resolved_by_position_id` was cleared on any annotation pointing at a doomed position**,
   including surviving ones — silently erasing a live annotation's recorded resolution to make
   room for the delete. Positions that still resolve a surviving annotation are now excluded
   from the orphan set instead.

The type-blind style is deliberate and carried into `V17`: an id must be absent from *every*
table it could name, which errs toward keeping rows. `AbstractAnnotation` declares no
`@AnyDiscriminatorValue`, so the `*_type` strings are Hibernate defaults and not worth
trusting for a destructive operation.

### `tmp/orphan-annotations-diagnose.sql`

Read-only. Still the right tool for answering "did this actually work" against a real
database, before and after the migration — run it on a dev or staging copy and expect query 3
to return 0.

```sql
-- Diagnose the 32 rows the previous script's "orphan annotations" check reported.
-- Read-only. mysql -h 127.0.0.1 -P 3307 -u root -p'pa33w0rd' requel < tmp/orphan-annotations-diagnose.sql

-- 1. What kind of grouping object do they claim? This is the key question: if
--    grouping_object_type is a Project/ProjectOrDomain type, they are real
--    leftovers. If it names a UseCase/Story/etc, the pods-only check was simply
--    the wrong check and they may be fine.
SELECT a.grouping_object_type,
       a.annotation_type,
       COUNT(*)             AS n,
       MIN(a.date_created)  AS oldest,
       MAX(a.date_created)  AS newest
FROM annotations a
WHERE NOT EXISTS (SELECT 1 FROM pods p WHERE p.id = a.grouping_object_id)
GROUP BY a.grouping_object_type, a.annotation_type
ORDER BY n DESC;

-- 2. Does the grouping object resolve in ANY project-scoped table?
SELECT a.id,
       a.annotation_type,
       a.grouping_object_type,
       a.grouping_object_id,
       a.source,
       a.date_created,
       CASE
         WHEN EXISTS (SELECT 1 FROM actors       t WHERE t.id = a.grouping_object_id) THEN 'actors'
         WHEN EXISTS (SELECT 1 FROM goals        t WHERE t.id = a.grouping_object_id) THEN 'goals'
         WHEN EXISTS (SELECT 1 FROM scenarios    t WHERE t.id = a.grouping_object_id) THEN 'scenarios'
         WHEN EXISTS (SELECT 1 FROM stories      t WHERE t.id = a.grouping_object_id) THEN 'stories'
         WHEN EXISTS (SELECT 1 FROM usecases     t WHERE t.id = a.grouping_object_id) THEN 'usecases'
         WHEN EXISTS (SELECT 1 FROM terms        t WHERE t.id = a.grouping_object_id) THEN 'terms'
         WHEN EXISTS (SELECT 1 FROM reports      t WHERE t.id = a.grouping_object_id) THEN 'reports'
         WHEN EXISTS (SELECT 1 FROM stakeholders t WHERE t.id = a.grouping_object_id) THEN 'stakeholders'
         WHEN EXISTS (SELECT 1 FROM teams        t WHERE t.id = a.grouping_object_id) THEN 'teams'
         ELSE 'NOWHERE'
       END                                                                   AS grouping_object_found_in,
       (SELECT COUNT(*) FROM annotation_annotatable aa WHERE aa.annotation_id = a.id) AS annotatable_rows,
       (SELECT COUNT(*) FROM position_issue pi WHERE pi.issue_id = a.id)              AS position_rows,
       LEFT(COALESCE(a.text, ''), 60)                                                 AS text_head
FROM annotations a
WHERE NOT EXISTS (SELECT 1 FROM pods p WHERE p.id = a.grouping_object_id)
ORDER BY a.id;

-- 3. Of their annotation_annotatable rows, how many point at something alive?
--    0 live means the annotations are dangling on both ends.
SELECT COUNT(*) AS annotatable_rows_pointing_at_live_entity
FROM annotation_annotatable aa
JOIN annotations a ON a.id = aa.annotation_id
WHERE NOT EXISTS (SELECT 1 FROM pods p WHERE p.id = a.grouping_object_id)
  AND (   EXISTS (SELECT 1 FROM pods         t WHERE t.id = aa.annotatable_id)
       OR EXISTS (SELECT 1 FROM actors       t WHERE t.id = aa.annotatable_id)
       OR EXISTS (SELECT 1 FROM goals        t WHERE t.id = aa.annotatable_id)
       OR EXISTS (SELECT 1 FROM scenarios    t WHERE t.id = aa.annotatable_id)
       OR EXISTS (SELECT 1 FROM stories      t WHERE t.id = aa.annotatable_id)
       OR EXISTS (SELECT 1 FROM usecases     t WHERE t.id = aa.annotatable_id)
       OR EXISTS (SELECT 1 FROM terms        t WHERE t.id = aa.annotatable_id)
       OR EXISTS (SELECT 1 FROM reports      t WHERE t.id = aa.annotatable_id)
       OR EXISTS (SELECT 1 FROM stakeholders t WHERE t.id = aa.annotatable_id)
       OR EXISTS (SELECT 1 FROM teams        t WHERE t.id = aa.annotatable_id)
       OR EXISTS (SELECT 1 FROM goal_relations t WHERE t.id = aa.annotatable_id));
```

### `tmp/delete-orphan-annotations.sql`

Superseded by `V17` for the cleanup itself. Kept here for its candidate-set query, which is
the part `V17` adopted, and because its dry-run/verify scaffolding is useful for rehearsing
the migration against a copy of a real database before release.

```sql
-- ===========================================================================
-- delete-orphan-annotations.sql
--
-- Sweep annotations that are dangling on BOTH ends: their @Any grouping object
-- no longer exists, AND none of their annotation_annotatable rows point at a
-- live entity. This is the #247 leftover class -- annotations.grouping_object_id
-- has no foreign key, so a row whose project (or whose entity) was deleted
-- before the annotation was linked just stays behind.
--
-- Deliberately type-blind: it never reads grouping_object_type or
-- annotatable_type (AbstractAnnotation declares no @AnyDiscriminatorValue, so
-- those strings are Hibernate defaults and not worth guessing). Instead an id
-- must be absent from EVERY table it could possibly name. That is conservative
-- in the safe direction: an id that collides with a live row in an unrelated
-- table is kept, never deleted.
--
--   mysql -h 127.0.0.1 -P 3307 -u root -p'pa33w0rd' requel \
--     < tmp/delete-orphan-annotations.sql
--
-- TRIAL RUN: change COMMIT to ROLLBACK. Foreign key checks stay ON.
-- ===========================================================================

DROP TABLE IF EXISTS zz_orphan_annotations, zz_orphan_positions;

-- ---------------------------------------------------------------------------
-- 0. Candidate set.
-- ---------------------------------------------------------------------------
CREATE TABLE zz_orphan_annotations (id BIGINT PRIMARY KEY);

INSERT INTO zz_orphan_annotations (id)
SELECT a.id
FROM annotations a
WHERE NOT EXISTS (SELECT 1 FROM pods           t WHERE t.id = a.grouping_object_id)
  AND NOT EXISTS (SELECT 1 FROM actors         t WHERE t.id = a.grouping_object_id)
  AND NOT EXISTS (SELECT 1 FROM goals          t WHERE t.id = a.grouping_object_id)
  AND NOT EXISTS (SELECT 1 FROM goal_relations t WHERE t.id = a.grouping_object_id)
  AND NOT EXISTS (SELECT 1 FROM scenarios      t WHERE t.id = a.grouping_object_id)
  AND NOT EXISTS (SELECT 1 FROM stories        t WHERE t.id = a.grouping_object_id)
  AND NOT EXISTS (SELECT 1 FROM usecases       t WHERE t.id = a.grouping_object_id)
  AND NOT EXISTS (SELECT 1 FROM terms          t WHERE t.id = a.grouping_object_id)
  AND NOT EXISTS (SELECT 1 FROM reports        t WHERE t.id = a.grouping_object_id)
  AND NOT EXISTS (SELECT 1 FROM stakeholders   t WHERE t.id = a.grouping_object_id)
  AND NOT EXISTS (SELECT 1 FROM teams          t WHERE t.id = a.grouping_object_id)
  -- ...and nothing alive is still annotated by it.
  AND NOT EXISTS (
        SELECT 1 FROM annotation_annotatable aa
        WHERE aa.annotation_id = a.id
          AND (   EXISTS (SELECT 1 FROM pods           t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM actors         t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM goals          t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM goal_relations t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM scenarios      t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM stories        t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM usecases       t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM terms          t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM reports        t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM stakeholders   t WHERE t.id = aa.annotatable_id)
               OR EXISTS (SELECT 1 FROM teams          t WHERE t.id = aa.annotatable_id)))
  -- ...and no surviving entity-side join row still owns it.
  AND NOT EXISTS (SELECT 1 FROM project_annotations        j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM actors_annotations         j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM goals_annotations          j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM goal_relations_annotations j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM scenarios_annotations      j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM stories_annotations        j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM usecases_annotations       j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM terms_annotations          j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM reports_annotations        j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM stakeholders_annotations   j WHERE j.annotations_id = a.id)
  AND NOT EXISTS (SELECT 1 FROM teams_annotations          j WHERE j.annotations_id = a.id);

-- Positions reachable only from those issues, and not still referenced as the
-- resolution of a surviving annotation.
CREATE TABLE zz_orphan_positions (id BIGINT PRIMARY KEY);
INSERT INTO zz_orphan_positions (id)
SELECT DISTINCT pi.position_id
FROM position_issue pi
WHERE pi.issue_id IN (SELECT id FROM zz_orphan_annotations)
  AND pi.position_id NOT IN (
        SELECT position_id FROM position_issue
        WHERE issue_id NOT IN (SELECT id FROM zz_orphan_annotations))
  AND pi.position_id NOT IN (
        SELECT resolved_by_position_id FROM annotations
        WHERE resolved_by_position_id IS NOT NULL
          AND id NOT IN (SELECT id FROM zz_orphan_annotations));

-- ---------------------------------------------------------------------------
-- 1. Dry run.
-- ---------------------------------------------------------------------------
SELECT 'orphan annotations' AS entity, COUNT(*) AS n FROM zz_orphan_annotations
UNION ALL SELECT 'their positions', COUNT(*) FROM zz_orphan_positions
UNION ALL SELECT 'their arguments', COUNT(*) FROM arguments WHERE position_id IN (SELECT id FROM zz_orphan_positions);

SELECT a.id, a.annotation_type, a.grouping_object_type, a.grouping_object_id,
       a.source, a.date_created, LEFT(COALESCE(a.text, ''), 60) AS text_head
FROM annotations a JOIN zz_orphan_annotations o ON o.id = a.id
ORDER BY a.id;

-- ---------------------------------------------------------------------------
-- 2. Delete.
-- ---------------------------------------------------------------------------
START TRANSACTION;

DELETE FROM annotation_annotatable WHERE annotation_id IN (SELECT id FROM zz_orphan_annotations);
DELETE FROM position_issue         WHERE issue_id      IN (SELECT id FROM zz_orphan_annotations)
                                      OR position_id   IN (SELECT id FROM zz_orphan_positions);
DELETE FROM annotations            WHERE id            IN (SELECT id FROM zz_orphan_annotations);
DELETE FROM arguments              WHERE position_id   IN (SELECT id FROM zz_orphan_positions);
DELETE FROM positions              WHERE id            IN (SELECT id FROM zz_orphan_positions);

COMMIT;

-- ---------------------------------------------------------------------------
-- 3. Verify. Both should be 0.
-- ---------------------------------------------------------------------------
SELECT 'annotations left' AS check_name, COUNT(*) AS n
  FROM annotations WHERE id IN (SELECT id FROM zz_orphan_annotations)
UNION ALL SELECT 'positions left', COUNT(*)
  FROM positions   WHERE id IN (SELECT id FROM zz_orphan_positions);

DROP TABLE IF EXISTS zz_orphan_annotations, zz_orphan_positions;
```

## Locked decisions (yours)

1. **Lock**, not a cancellation registry.
2. **`CANCELLED`** is the terminal status for a run that loses the race (distinct from
   `SKIPPED`, which means "there was nothing to do").
3. **Flyway migration** to clean up orphans already in the database.

## How we avoid deadlocking

This was your first question and it is the load-bearing part of the design.

`DeleteProjectCommandImpl` deletes **children first, the project row last** (steps 1–12).
So the delete's natural lock order is *children → project*. If the apply side naively took
a lock on the project and then wrote annotations, its order would be *project → children* —
the textbook ABBA deadlock, and under InnoDB it would fire under real e2e load, not in a
single-threaded IT.

The fix is to have exactly **one ordered gate** that both paths take **first**:

- **Delete side.** `DeleteProjectCommandImpl.execute()` line 135 becomes a locking read of
  the `pods` row, *before* step 1 touches a single child. The delete's order becomes
  *project → children*.
- **Apply side.** `AssistantRunWorker.apply()` takes the same `pods` row lock before it
  writes anything. Its order is also *project → children*.

Same resource, same order, both paths, so no cycle can form. Every other command in the
system is unchanged and takes no project lock at all — they contend on child rows only,
exactly as today.

Both orderings of the race are then correct, and neither needs the apply side to give up:

| Who gets the `pods` lock first | What happens |
| --- | --- |
| **apply** | the delete blocks; apply writes its findings and commits; the delete proceeds and its **existing** step 11 `DeleteAnnotationGroupCommand` sweep removes them. No orphan, no new code. |
| **delete** | apply blocks until the delete commits; the same statement that takes the lock then returns **zero rows** (the project is gone); apply records `CANCELLED` and writes nothing. |

So waiting is the *desired* behaviour here, not a cost to be tuned away.

### Exclusive, not shared

I said `PESSIMISTIC_READ` for the apply side when we discussed this. On inspection that is
the wrong call and I want to flag the change rather than slide it into the plan:

- H2 has no `FOR SHARE`; its row locking is exclusive-only. A shared-lock design would be
  untestable on the H2 profile — which is where all but one of these tests run.
- Hibernate maps `PESSIMISTIC_READ` to plain `FOR UPDATE` on several dialects anyway, so
  the "shared locks don't block each other" argument would not have held end to end.

Both sides therefore take `FOR UPDATE`. The cost is that two assistant applies for the
same project serialize. That is acceptable: #247 moved all the slow work into the analyze
phase, so an apply is a handful of inserts — single-digit milliseconds — while the thing
we are serializing against (the full delete cascade) is the rare operation.

### Lock wait, not lock timeout

No `NOWAIT` and no JPA timeout hint in v1:

- MySQL 8 supports `FOR UPDATE NOWAIT`; H2 2.x does not, so a hint would diverge the two
  profiles for no benefit.
- Waiting is what we want (see the table above). The wait is bounded by the database's own
  `innodb_lock_wait_timeout` — 50s by default in production, and already pinned to **10s**
  in `DeleteProjectMySqlIT`.
- The waiter is a worker thread on the assistant task executor, not a request thread, so a
  long wait costs throughput, never a user-visible hang.

A lock-wait timeout is still handled: any `PessimisticLockException` / `LockTimeoutException`
out of the gate is caught and recorded as `CANCELLED` with the exception as the reason,
which is the same safe outcome as losing the race. If the e2e suite ever shows workers
parked on this, tightening it to `NOWAIT` on MySQL is a one-line follow-up.

## Changes

### 1. `Repository` gains a lock verb — `platform-core`

`Repository` (`com.rreganjr.repository`) has no lock method and deliberately imports no
`jakarta.persistence` types. Keep it that way: add a **named** method rather than leaking
`LockModeType`.

```java
/**
 * Take an exclusive database lock on the row backing {@code entity}, blocking until it
 * is available. Used to serialize a cascading delete against concurrent writers that
 * reach the same rows by a different path (#279).
 */
public <T> T lockForUpdate(T entity) throws EntityException;
```

Implemented in `AbstractJpaRepository` as `entityManager.lock(attach(em, entity),
LockModeType.PESSIMISTIC_WRITE)`.

**The CGLIB advice is handled by reuse, not new code.** `DomainObjectWrappingAdvice`'s
pointcut is `execution(* AbstractJpaRepository+.*(..))`, so this method is advised like
every other and its argument arrives as a wrapped proxy. `attach(EntityManager, T)` already
ends with an `EntityProxyInterceptor.unwrap` for exactly this reason, so routing through it
- the same thing `get`, `delete` and `initialize` do - is all that is needed.

One guard beyond the plan as written: `attach` returns the *detached original* (with a
warning) when the row is gone, and `EntityManager.lock` throws on a detached instance. The
implementation therefore locks only when `entityManager.contains(attached)`. With no row
there is nothing to gate, and the caller fails on its own terms exactly as it did before.

### 2. `DeleteProjectCommandImpl` takes the gate first — `project-jpa`

```java
Project project = getRepository().get(getProject());
// #279: take the project row's write lock before the cascade touches a single child, so
// a concurrent assistant apply (which takes the same lock first) either commits before
// this delete starts - and is swept by step 11 - or blocks until the row is gone and
// cancels itself. Children-first without this lock gives the two paths opposite lock
// orders and an ABBA deadlock under InnoDB.
project = getRepository().lockForUpdate(project);
User editedBy = getRepository().get(getEditedBy());
```

Placed **before** the optimistic-version check, so a stale-version rejection still costs
nothing but also cannot race. Nothing else in the command moves.

### 3. The apply-side gate — `assistant-core`

New SPI, because `assistant-core` depends on `project-domain` and `annotation-jpa` but
**not** `project-jpa`, so it cannot see `ProjectImpl` to type a `find`/`lock` call:

```java
package com.rreganjr.requel.assistant.core;

/** Serializes an apply against a concurrent project delete (#279). */
public interface AssistantProjectGate {
    enum State { OPEN, GONE, BUSY }
    State acquire(EntityRef projectRef);
}
```

`JpaAssistantProjectGate` (in `assistant-core/.../persistence/`, next to
`JpaAssistantRunStore`) implements it with one native statement that **locks and checks at
the same time**:

```sql
SELECT id FROM pods WHERE id = ? FOR UPDATE
```

- a row comes back → `OPEN` (we hold the lock for the rest of the apply transaction)
- no row → `GONE`
- `PessimisticLockException` / `LockTimeoutException` / Spring's
  `PessimisticLockingFailureException` → `BUSY` (Hibernate raises one of the JPA pair;
  Spring's exception translation, where applied, rewraps it as the third)

The bean is annotated `@Transactional(propagation = MANDATORY)`, and that is load-bearing
rather than decorative: a row lock lives only as long as the transaction that took it. In
its own transaction the lock would be released at that commit — before the caller wrote a
single finding — and the gate would silently guarantee nothing. Requiring the caller's
transaction turns that mistake into a startup failure instead of an unreproducible race.

Native SQL rather than `em.find(ProjectImpl.class, ...)` is the price of not making
`assistant-core` depend on `project-jpa`; `pods` is stable (`AbstractProjectOrDomain:90`)
and the statement is valid on both MySQL 8 and H2 2.x.

### 4. `AssistantRunWorker.apply()` — `assistant-core`

```java
private Outcome apply(AssistantRunRecord record, Analysis analysis) {
    AnalysisRequest request = record.request();
    // #279: gate on the project row first. This both takes the lock that orders us
    // against DeleteProjectCommandImpl and answers whether the project still exists.
    EntityRef projectRef = request.projectRef();
    if (projectRef != null) {
        switch (projectGate.acquire(projectRef)) {
            case GONE -> return Outcome.cancelled("Project#" + projectRef.entityId()
                    + " was deleted while the analysis ran; findings discarded");
            case BUSY -> return Outcome.cancelled("Could not lock Project#"
                    + projectRef.entityId() + " before applying; findings discarded");
            case OPEN -> { /* fall through */ }
        }
    }
    if (loadTarget(request).isEmpty()) {
        return Outcome.skipped("Target ... no longer exists; findings discarded");
    }
    ...
}
```

`apply` currently returns `String` (null = applied, non-null = skip reason). It now has to
distinguish skip from cancel, so it returns a small private `Outcome` record carrying a
status plus a reason. `run(UUID)` branches on it.

**`projectRef` is nullable** — the compact constructor null-checks every other field, and
`AnalysisRequestDispatcher:80-83` passes `null` when `target.getProjectOrDomain()` is null.
When it is null there is no project to gate on, so behaviour is exactly today's: the
target-only re-check, and `SKIPPED`. Called out in a comment so it is not read as an
oversight.

### 5. `markCancelled` — `assistant-core`

- `AssistantRunStore` gains `void markCancelled(UUID runId, String reason)`.
- `JpaAssistantRunStore` → `update(runId, AssistantRunStatus.CANCELLED, reason, e -> e.setCompletedAt(clock.instant()))`, mirroring `markSkipped` at line 127.
- `InMemoryAssistantRunStore` → the matching one-liner at line 73.

No schema change: `status` is `VARCHAR(20)` and nothing outside `assistant-core` reads the
enum, so no DTO, no Angular, no API surface moves.

### 6. `V17__delete_orphan_annotations.sql` — `requel-app`

> Two mechanics discovered while writing it, both now in the migration's own comments:
> MySQL cannot reference a **temporary** table more than once in a single statement
> (ERROR 1137, "Can't reopen table"), and the shared-position query needs the orphan set
> twice — so the two working sets are ordinary `v17_*` tables, dropped at the end. And
> MySQL has no `CREATE INDEX IF NOT EXISTS`, so the index uses the
> `information_schema` + `PREPARE` guard that `V2__identity_cleanup.sql` already
> established in this schema, keeping the migration genuinely re-runnable.

Cleans up rows already orphaned by this bug (and by any earlier variant of it).

**Indexes `grouping_object_id` first.** The orphan predicate is a `NOT EXISTS` against
`pods` on a column with no index of its own, i.e. a full scan of `annotations`. The
migration therefore opens with

```sql
CREATE INDEX idx_annotations_grouping_object
    ON annotations (grouping_object_id, grouping_object_type);
```

which makes the scan an index range regardless of how large the table is, and leaves
behind an index worth having permanently: `grouping_object_id` is the soft FK this whole
bug hinges on, and `findAnnotationIdsByGroupingObject` (used by
`DeleteAnnotationGroupCommand`, i.e. on every project delete) filters on exactly it.

An annotation is only collected when it is dangling on **every** end — a broken group alone
is not grounds for deleting a row (see "What the scripts caught" above). All three must hold:

1. `grouping_object_type = 'Project'` and `grouping_object_id` is not an `id` in `pods`.
   `V2__identity_cleanup.sql:228` already normalized the type value from the FQCN to
   `'Project'`, and the 32 observed orphans all carried it, so the short form is correct.
2. no `annotation_annotatable` row of its own points at a row that still exists in any of the
   eleven annotatable tables — type-blind, so an id must be absent from *every* table it could
   name.
3. no `<entity>_annotations` join row still owns it. Those carry real foreign keys, so a row
   here means a live entity claims the annotation.

Condition 3 makes the eleven `<entity>_annotations` deletes below unreachable by construction.
They are kept only so the final delete cannot be blocked by a row that appears between the two
statements, and the migration's comment says exactly that rather than implying they do work.

Delete order, driven by the FKs that actually exist:

1. `arguments` — FK `position_id` → `positions`; only for positions that become deletable in 4.
2. `annotation_annotatable` — FK `annotation_id` → `annotations`.
3. the eleven `<entity>_annotations` inverse join tables (`actors_`, `goal_relations_`,
   `goals_`, `project_`, `reports_`, `scenarios_`, `stakeholders_`, `stories_`, `teams_`,
   `terms_`, `usecases_`), each FK `annotations_id` → `annotations`. Expected to be empty
   for orphans — those FKs are what made #247 fail loudly — but a leftover here would block
   the delete, so they are swept.
4. `position_issue` — FK `issue_id` → `annotations`; then any `positions` row left with no
   remaining `position_issue` rows. **Positions are shared** (`PositionImpl.issues` is a
   `@ManyToMany`), so two guards apply: a position answering a live issue as well must
   survive, *and* so must one that is still the recorded `resolved_by_position_id` of a
   surviving annotation.
5. `annotations` — clear `resolved_by_position_id`, scoped to rows that are themselves being
   deleted. That scoping is the fix for the second data-loss bug: clearing it on *any* row
   pointing at a doomed position would erase a live annotation's resolution. With the guard in
   4, no surviving row can point at a doomed position anyway; the `UPDATE` exists only because
   MySQL checks the FK per statement.

Written to be **idempotent** and to be a no-op on a clean database, in the V16 house style:
a long comment explaining the pathology, then the statements.

## Test plan

Your `#287` constraint drives the shape of this: that regression came from a `@SpringBootTest`
context doing expensive work, and CI's `Build & test` job went 8–11min → 19–23min. So:
**no new `@SpringBootTest` class, no new Testcontainers container, no new Spring context.**
`DeleteProjectMySqlIT extends DeleteProjectIT`, so a method added to the parent runs on both
profiles for free, and a method added to the subclass runs only on MySQL.

### `assistant-core` unit tests (no Spring context at all)

`AssistantRunWorkerTest` already drives the worker through its inline-transaction test
constructor and `InMemoryAssistantRunStore`. Add, with a stub `AssistantProjectGate`:

| test | gate returns | asserts |
| --- | --- | --- |
| `cancelsWhenTheProjectWasDeletedDuringAnalysis` | `GONE` | status `CANCELLED`, reason names the project, **`resultApplicator` never called** |
| `cancelsWhenTheProjectRowCannotBeLocked` | `BUSY` | status `CANCELLED`, applicator never called |
| `appliesWhenTheGateIsOpen` | `OPEN` | status `SUCCEEDED`, applicator called once per result |
| `skipsWithoutGatingWhenTheRequestHasNoProject` | (not called) | `projectRef == null` → gate never consulted, existing target-only behaviour preserved |

Plus `JpaAssistantRunStoreTest` / the in-memory store test: `markCancelled` writes
`"CANCELLED"` and the reason, and sets `completedAt` — mirroring the existing `SKIPPED`
assertions at `JpaAssistantRunStoreTest.java:178`.

These are module-local and fast, and they carry the coverage weight (jacoco has no
`report-aggregate`, so IT coverage of `assistant-core` from `requel-app` is not attributed
to it — the lesson from #253's 36% Codecov report).

### `DeleteProjectIT` — runs on H2 **and**, inherited, on MySQL

One new method, `assistantProjectGateOpensWhileTheProjectExistsAndReportsGoneOnceDeleted`.
It drives the gate itself rather than the whole worker, and that is deliberate: driving
`AssistantRunWorker.run(...)` against a deleted project never reaches the gate, because the
*analyze* phase's own `loadTarget` comes back empty first and short-circuits to `SKIPPED`.
Testing the gate directly is what actually exercises the new production path against a real
database. It asserts `OPEN` while the project exists, `GONE` once it is deleted, and that no
`annotations` row survives with that `grouping_object_id`.

The worker's `CANCELLED` behaviour is covered at unit level instead, where the gate's three
states can be driven independently — cheaper and more thorough than reproducing them through
a Spring context.

### `DeleteProjectMySqlIT` — MySQL only, two new methods

Added to the subclass so they do not run on H2, and needing no new context or container:

- `projectDeleteBlocksOnTheSameGateAnAssistantApplyTakes` — the real race, made
  deterministic. One thread takes the `pods` row exactly as an apply does and holds it; a
  second runs `DeleteProjectCommand` and must **block**. The test asserts it is still
  blocked, then releases and asserts the delete completes and leaves no orphans. That
  ordering is what proves the lock is actually taken before the cascade; a lock-order
  regression fails fast rather than hanging CI, because the class already pins
  `innodb_lock_wait_timeout` to 10s.
- `v17CleansOrphansButKeepsAnythingStillReferencedByLiveRows` — Flyway has already run `V17`
  against an empty schema by the time tests execute, so it cleaned nothing there. Seed an
  orphan annotation, a link row, a position that only answers it, a **shared** position that
  also answers a live issue, and an annotation filed under the same missing project that
  **still annotates a live entity**; re-execute `V17`'s statements read off the classpath (so
  the test cannot drift from what ships) and assert the orphans go while everything still
  referenced by a live row stays. That last case is the one that fails against the first draft
  of `V17` and passes against this one. This is also what makes the migration testable at all
  — H2 runs with **Flyway disabled** (`create-drop`), so it is never exercised there.

### Gates

`mvn clean verify` at the root. No Angular surface changes, so no vitest/tsc/e2e work.

## AC mapping

| AC | Covered by |
| --- | --- |
| an assistant run cannot write annotations for a deleted project | §2 + §3 + §4 (the gate), `cancelledAssistantApplyWritesNothingForADeletedProject`, `concurrentAssistantApplyAndProjectDeleteLeaveNoOrphans` |
| a run that loses the race is recorded, not silently dropped | §5 `markCancelled`; the four `AssistantRunWorkerTest` cases |
| `CANCELLED` is distinguishable from `SKIPPED` | §4 `Outcome`; `JpaAssistantRunStoreTest` |
| existing orphans are cleaned up | §6 `V17`; `v17CleansOrphansButKeepsAnythingStillReferencedByLiveRows` |
| the cleanup does not take live data with it | §6 conditions 2-3 and the position guards; the same test's survival assertions |
| no deadlock between the two paths | single ordered gate (§2/§3); `concurrentAssistantApplyAndProjectDeleteLeaveNoOrphans` with the 10s lock-wait timeout as the tripwire |

## Out of scope

- **Other `Delete*Command`s.** `DeleteGoal`, `DeleteStory` etc. have the same soft-FK shape
  at a smaller blast radius. The project gate does not cover a run whose target is deleted
  while the project survives — but that case is already handled by the existing
  `loadTarget` re-check *and* by the eventual `DeleteAnnotationGroupCommand` sweep when the
  project itself goes. Extending the gate per-entity is a separate ticket if we want it.
- **Adding a real FK on `annotations.grouping_object_id`.** That is the actual cure, and it
  cannot be done while the column is an `@Any` pointing at several tables. It would need the
  polymorphic grouping to be modelled differently — an epic, not this ticket.
- **Tagging orphans.** `V13__tagging.sql` uses the same soft-FK pattern and
  `DeleteProjectCommandImpl:143-150` explicitly defers it. Unchanged here.
- **Surfacing run status in the UI.** Nothing outside `assistant-core` reads it today.
- **`NOWAIT` / tuned lock timeouts.** Deliberately deferred; see "Lock wait, not lock timeout".

## Risks

1. **First pessimistic lock in the codebase.** There is no precedent to copy and no existing
   test that exercises row locking, which is exactly why the concurrent case goes in the
   MySQL IT rather than relying on H2. The 10s `innodb_lock_wait_timeout` already configured
   there is the tripwire that turns a lock-order mistake into a fast failure.
2. **A new advised repository method.** `lockForUpdate` goes through `DomainObjectWrappingAdvice`
   like everything else on `AbstractJpaRepository`; if the argument is not unwrapped before
   `EntityManager.lock`, Hibernate sees a CGLIB proxy and throws. Verify against
   `EntityProxyInterceptor.unwrap` during implementation — this is the same class of bug #253
   was about.
3. **Native SQL in `assistant-core`.** `SELECT id FROM pods ... FOR UPDATE` hard-codes a table
   name that JPA otherwise owns. Mitigated by a comment pointing at
   `AbstractProjectOrDomain:90` and by the MySQL IT, which would fail loudly if the table
   were ever renamed.
4. **`V17` on a large production database.** Addressed by creating
   `idx_annotations_grouping_object` as the migration's first statement (§6), so the orphan
   scan is an index range rather than a full scan of `annotations` at any size. Residual
   risk is the index build itself; MySQL 8 builds it online, so it does not block writes.
5. **Serializing applies on the project row.** Named as a tradeoff above rather than hidden.
   If assistant throughput regresses visibly, the follow-up is a shared lock on MySQL with
   the H2 profile falling back to exclusive — deliberately not built speculatively.

## Build status

Built and run against a clone of `release/2.0` (`59abe7bf`) with this change applied,
JDK 17 toolchain, `-DskipAngularBuild=true` (no frontend change in this ticket):

| | result |
| --- | --- |
| `mvn -pl modules/requel-app -am install` (all 25 modules, main + test compile) | SUCCESS |
| `assistant-core` unit tests | 39 run, 0 failures — includes the 4 gate cases and `markCancelled` |
| `requel-app` surefire | 478 run, 0 failures, 29 skipped |
| `requel-app` failsafe | 157 run, 0 failures, 12 skipped |
| `DeleteProjectIT` (H2) | 9 run, 0 failures — 8 pre-existing plus the new gate round-trip |
| `DeleteProjectMySqlIT` | 11 **skipped** — no Docker in that environment |

**The two MySQL-only tests have not been executed.** `DeleteProjectMySqlIT` is
`@Testcontainers(disabledWithoutDocker = true)` and no Docker daemon was available, so it
skipped cleanly (11 = 9 inherited + 2 new, the right count) but nothing in it actually ran.
Everything asserted about real InnoDB row locking and about V17's statements therefore rests
on reading, not on a green run. That is the part of `mvn clean verify` to watch locally, and
the reason to read `projectDeleteBlocksOnTheSameGateAnAssistantApplyTakes` and
`v17CleansOrphanAnnotationsButKeepsPositionsAnsweringLiveIssues` more carefully than the rest.

One environment note that is **not** a code problem: a full `mvn verify` in an 8 GB container
failed with 49 errors, all cascading from an H2 `Out of memory` (SQL error 90108) that hits
immediately after the CoreNLP-loading assistant tests and then closes the database for every
later test. Re-running with `-DargLine="-Xmx5g"` is green.

## Process (CLAUDE.md)

- Branch `279-assistant-delete-race` off `release/2.0` (`59abe7bf`) — already created.
- One commit, message `https://github.com/rreganjr/Requel/issues/279`.
- `mvn clean verify` green before the PR.
- PR targets `release/2.0`; CI must pass before merge.
- Retro: 1 point expected.

# #284 Duplicate position text breaks EditPosition — implementation plan

Follows #281 (PR #285). That fix stopped `EditPositionCommandImpl.execute` discarding its
`findPosition` result, which means the lookup now has to actually work. Where duplicate text already
exists it throws instead.

## Review against the tree (`release/2.0` @ `f767a3ec`)

Still needed exactly as filed — no edits or comments since 2026-09-12, and
`JpaAnnotationRepository.findPosition` is unchanged:

```java
"select object(position) from PositionImpl as position "
        + "inner join position.issues issue where position.text like :text "
        + "and issue.groupingObject = :groupingObject"
...
return (Position) query.getSingleResult();
```

Both defects in that one statement are confirmed:

- `getSingleResult()` throws `NonUniqueResultException` on two matches. The caller catches only
  `NoResultException` → `NoSuchPositionException`; everything else falls through to
  `convertException` and surfaces as a failed command, so #281's reuse path never gets a chance.
- `like` rather than `=`. With no wildcards in the value it behaves like equality, but a position
  whose text contains `%` or `_` matches *other* positions. A non-unique result therefore does not
  even require true duplicates.

What a merge has to move, verified in the mappings rather than taken from the issue text:

| field | mapping | merge action |
|---|---|---|
| `PositionImpl.issues` | `@ManyToMany(cascade = MERGE, PERSIST)` | union onto the winner |
| `PositionImpl.arguments` | `@OneToMany(mappedBy = "position", cascade = ALL)` | **reparent before deleting the loser, or the debate content is deleted with it** |
| `IssueImpl.resolvedByPosition` | `@ManyToOne(cascade = MERGE, ...)` | repoint any issue naming the loser |

Two things the issue does not mention that the design has to account for:

- **`ArgumentImpl.setPosition` is `protected`.** Reparenting therefore has to happen from inside
  `com.rreganjr.requel.annotation.impl`, or go native. That argues for putting the merge in the
  impl package rather than in the command.
- **`AnnotationRepository` already carries the native join-table delete helper** added by #247/#248,
  documented as "call immediately before `delete(entity)`" because Hibernate skips the join-table
  delete and a concurrently committed link leaves an orphan row. Deleting the loser must follow that
  precedent, not a bare `delete`.

`EditPositionReuseIT` (from #281) already builds two issues in one grouping object and asserts a
position is shared, so the new cases extend it rather than needing fresh fixtures.
`DeletePositionCommandImpl` exists and is driven by `DeleteIssueCommandImpl`, so there is a
precedent for removing a position through the command path.

## Locked decisions

1. **Lowest id wins.** Stage 1's lookup and stage 2's merge then agree by construction, and no
   second tiebreak is ever needed. Rejected: "the one with arguments" and "most issue links" — both
   are ambiguous when the candidates tie, both make the lookup load more to decide, and the link
   count can change between a lookup and a later merge, so the two stages could disagree about who
   won.
2. **Both stages ship in this ticket.** Stage 1 stops the bleeding; stage 2 merges what already
   exists.
3. **The merge runs on encounter, not as a bulk sweep.** `EditPosition` is already a write inside a
   transaction and is the only path that hits the problem, so when its lookup finds more than one
   match it merges them and proceeds. Duplicates nobody ever edits stay until touched. Rejected: a
   Flyway data migration — it would rewrite rows on every deployment's startup for a condition that
   may not exist there, with no way to review what it did. A maintenance command for a full sweep is
   noted as a follow-up, not built here.

## Steps

1. **`AnnotationRepository` / `JpaAnnotationRepository`** — add `findPositions(groupingObject, text)`
   returning `List<Position>` ordered by id ascending, using `=`:

   ```java
   "select object(position) from PositionImpl as position "
           + "inner join position.issues issue where position.text = :text "
           + "and issue.groupingObject = :groupingObject order by position.id"
   ```

   `findPosition` delegates: empty → `NoSuchPositionException` (unchanged contract), otherwise the
   first element. It never throws `NonUniqueResultException` again, and the resolution is documented
   as lowest-id.
2. **`PositionMerger`** (new, `com.rreganjr.requel.annotation.impl`) — merges losers into the
   winner, in this order: union the issue links; reparent each loser's arguments (possible from this
   package because `setPosition` is protected); repoint any `IssueImpl.resolvedByPosition` naming a
   loser; then the native join-table cleanup followed by `delete(loser)`, per the #247/#248 note.
3. **`EditPositionCommandImpl`** — when the lookup returns more than one, merge into the lowest-id
   winner and carry on with it. Separately, the explicit-`positionId` branch currently runs
   `position.setText(getText())` with no uniqueness check, which is one of the two ways duplicates
   are created; it now refuses an edit that would collide with another position in the same grouping
   object, naming the existing position's id.
4. **`EditAddWordToDictionaryPositionCommandImpl`** — the other creator. It looks up by *word*, not
   text, so it can still mint a position whose text equals a plain position's. Out of scope to
   restructure; noted in the ticket and covered by the merge.

## Test plan

- **`AnnotationCommandTest` / `EditPositionReuseIT`** (H2) — duplicates created natively with
  `JdbcTemplate` (single-threaded commands cannot make them, per CLAUDE.md's guidance on
  manufacturing race outcomes), then `EditPosition` merges them: lowest id survives, both issue
  links present, both sets of arguments present and pointing at the winner, any
  `resolvedByPosition` repointed, loser gone.
- **A `%` in position text** no longer matches other positions — the `like`→`=` half, which is
  otherwise invisible.
- **A text edit that would collide** is refused and names the existing id.
- **`EditPositionDedupMySqlIT`** (`@Testcontainers(disabledWithoutDocker = true)`, the
  `DeleteProjectMySqlIT` container config) — the same merge on MySQL with the Flyway schema. FK
  order and orphan `position_issue` rows behave differently there, which is the whole reason
  CLAUDE.md asks for it on deletes and join tables. `DeleteProjectMySqlIT` runs in ~12 s, so the
  cost is small — and unlike #288's first attempt, this IT asserts row counts on both sides of the
  merge, so it cannot pass while doing nothing.
- **Gate** — `mvn clean verify`; no frontend change, so no vitest/tsc/e2e.

## Out of scope

- A maintenance command that sweeps all existing duplicates project-wide. The merge here is
  encounter-driven; a sweep is a follow-up ticket if the dev instance turns out to hold many.
- Restructuring `EditAddWordToDictionaryPositionCommandImpl`'s word-keyed lookup.
- `findChangeSpellingPosition` and `findAddWordToDictionaryPosition`, which have the same
  `getSingleResult` shape — worth their own look, but they key on different columns and are not
  what #284 reports.

## Risks

- **The merge deletes a row.** `arguments` cascades ALL from the loser, so the reparent must happen
  and be flushed before the delete or the debate content goes with it. That ordering is the single
  most important thing for review to check, and the MySQL IT is what proves it.
- **Encounter-driven merging means a write happens inside what callers may think of as a lookup.**
  It is confined to `EditPosition`, which is already a write command in a transaction — but it is
  worth stating plainly rather than discovering later.
- **Lowest id is arbitrary where the higher-id position is the richer one.** Nothing is lost —
  arguments and links move to the winner — but the surviving row's id and creator are the older
  one's.

## AC mapping

| AC | Covered by |
|---|---|
| `findPosition` matches on equality; `%`/`_` no longer match other positions | Step 1 + its test |
| Resolves deterministically instead of throwing, choice documented | Step 1, lowest id (locked decision 1) |
| `EditPosition` refuses a colliding text edit, naming the existing id | Step 3 |
| Existing duplicates are merged — links unioned, arguments reparented, `resolvedByPosition` repointed, loser deleted | Step 2 + the H2 and MySQL tests |

# 281 — `EditPosition` NPEs when a position with the same text already exists

Milestone **v2.0**. Backend-only, no Angular change. Found while writing the `positionType` tests
for #253 (PR #280), where three tests reused one position text and the second one blew up.

## Summary

One missing assignment. `EditPositionCommandImpl.execute` performs the lookup its own comment
describes, then throws the result away, so the found path leaves `position` null and the next line
dereferences it. The not-found path works, which is why nothing caught it: every test and every
ordinary interaction used fresh text.

## The defect, verbatim

```java
if (position == null) {
    // look for existing position that matches the text and
    // reference it with the issue.
    if (issue != null) {
        try {
            getAnnotationRepository().findPosition(issue.getGroupingObject(), getText());
        } catch (NoSuchPositionException e) {
            position = getRepository().persist(new PositionImpl(getText(), editedBy));
        }
    }
    ...
}
if (issue != null) {
    position.getIssues().add(issue);   // null when findPosition succeeded
}
```

`findPosition` throwing is the *only* path that assigns `position`.

## Locked decisions

- **Reuse and link, not refuse.** Three independent signals agree, so this is not a judgment call:
  the comment in the code says "reference it with the issue"; `PositionImpl.getIssues()` is a
  `@ManyToMany(targetEntity = IssueImpl.class)` over the `position_issue` join table, so one
  position answering several issues is the schema's design; and the two sibling commands,
  `EditAddWordToDictionaryPositionCommandImpl:63` and
  `EditAddActorToProjectPositionCommandImpl:84`, run the same lookup-and-reuse shape and **do**
  assign the result.
- **Minimal fix.** Assign the lookup's result and cast to `PositionImpl`. No merge on the found
  path — the entity comes back from the repository already managed, and the text matched by
  definition, so there is nothing to update.
- **No change to the not-found path**, to `EditPosition`'s input DTO, or to the gateway.

## Changes

1. `modules/annotation-jpa/.../command/EditPositionCommandImpl.java` — keep the lookup's result.
   The comment is rewritten to say why reuse is correct rather than merely what the code does.
2. `modules/requel-app/src/test/.../service/EditPositionReuseIT.java` (new) — the found path, which
   had no coverage: two issues, one position text, and both issues end up referencing the same
   position id. A second case pins that distinct text still yields distinct positions, so the fix
   cannot over-reach and collapse genuine alternatives.
3. `modules/requel-app/src/test/.../service/GatewayPositionTypeIT.java` — removes the
   `positionText(tag)` workaround added by #253 and its per-test tags. Texts that must differ still
   differ, but now for a stated domain reason (two competing positions on one issue) rather than to
   route around this bug.

## Test plan (gate: `mvn clean verify`, script `tmp/281-verify.sh`)

No `requel-angular/**` change, so no vitest, no tsc, no e2e.

```bash
mvn -pl modules/requel-app -am test -Dtest='EditPositionReuseIT,GatewayPositionTypeIT'
mvn clean verify
```

## AC mapping

| AC | Covered by |
| --- | --- |
| Matching text links the existing position instead of throwing | Change 1; `EditPositionReuseIT.aPositionWhoseTextAlreadyExistsIsReusedByTheSecondIssue` |
| A test covers the found path: two issues, one text, one shared position | same test, asserting equal ids and both issues referencing it |
| The sibling commands are checked for the same defect | Both assign the lookup result already (`EditAddWordToDictionaryPositionCommandImpl:63`, `EditAddActorToProjectPositionCommandImpl:84`) — confirmed, no change needed |
| The `GatewayPositionTypeIT` workaround is removed | Change 3 |

## Out of scope

- What `findPosition(groupingObject, text)` matches. Two things about it are pre-existing and
  unchanged here, but both now sit on a path that is actually taken:
  - It resolves polymorphically, so a *subclass* position with the same text could be linked to a
    plain issue.
  - It uses `getSingleResult()`, so if two positions in one grouping object already share a text,
    `EditPosition` throws `NonUniqueResultException`. Before this fix that data could accumulate
    freely, because the lookup's result was discarded; after it, the duplicates are reachable. The
    fix removes the NPE and does not introduce this — but it makes existing duplicate text visible.
    Filed as #284, which also covers the `like` in that query: a position whose text contains `%`
    matches other positions, so a non-unique result does not even require true duplicates.
  `GatewayPositionTypeIT` hit exactly this while the tests were being simplified, which is why its
  subclass case keeps a text of its own.
- The `TODO(#43)` hardcoded `0` version in the annotation DTO mappers.

## Risks

- **Behaviour change on a path that used to throw.** Anything that relied on the NPE as a de-facto
  uniqueness guard would now silently succeed. Nothing can have: the caller got a
  `NullPointerException`, not a refusal, so there was no usable behaviour to depend on.
- Shared-context ITs: `EditPositionReuseIT` creates its own project, goal and stakeholder inside the
  fixture and asserts no id values, per CLAUDE.md.

## Process (CLAUDE.md)

Branch `281-edit-position-reuse` off `release/2.0`; `tmp/281-verify.sh` → `mvn clean verify`;
`commit.md` with `Closes #281`; the developer runs every `git`/`gh` command.

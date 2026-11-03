# #337 — Include issue severity in the AI assistant context packs

https://github.com/rreganjr/Requel/issues/337

Branch `337-severity-in-context-packs` from `release/2.1`, PR to `release/2.1`. Size 1.

## Summary

#271 gave issues a `LOW | MEDIUM | HIGH` severity and left it out of what the AI assistants see.
This puts it in the context packs, keeps the most important annotations when the entity pack is
cut short, and tells the bundled review prompts what it means.

## State of the tree (2026-11-03)

- `EntityContextPackBuilder` already leaves out annotations an assistant wrote, so the pack holds
  only human issues and notes. The ticket's case of LOW lexical findings crowding out a HIGH one
  can't happen; ordering still matters among human issues, which can carry any severity.
- `IssueContextPackBuilder` has no caller outside its own test.

## Locked decisions (2026-11-03)

1. **Issue context pack:** `IssueSnapshot` gets `severity`, for the AC. Nothing else changes in it,
   since nothing sends that pack.
2. **Entity pack order.** Annotations are ranked before the cap and the character budget apply:
   open issues by severity (HIGH, MEDIUM, LOW), then notes, then resolved issues. Within a group
   the current order is kept. An open LOW matters more than a resolved HIGH; notes have no
   severity.
3. **Prompt.** The 8 bundled review definitions whose rules mention the `annotations` list get one
   sentence after "Report only new problems." (or the generic definition's equivalent), and go
   from version 1 to 2 so the seeder upgrades them. A project's own copy keeps its text.
   > Each existing issue has a "severity" (LOW, MEDIUM or HIGH). Don't raise a finding that
   > repeats an open issue on the same point, especially a HIGH one.
4. **No scoring round.** The evaluation fixture's entities have no human issues, so the sentence
   never applies there.

## Changes

assistant-core
- `AnnotationSnapshot`: `String severity` (the `IssueSeverity` name; null for a note, and for an
  issue with none).
- `IssueSnapshot`: `String severity`.
- `EntityContextPackBuilder`: take the annotations that aren't machine-generated, sort them
  (decision 2, stable), then apply the cap and budget as now.
- `IssueContextPackBuilder`: pass the severity through.

assistant-ai
- `ai/definitions/requirements-review.json`, `review-actor`, `review-glossary-term`, `review-goal`,
  `review-scenario`, `review-step`, `review-story`, `review-usecase`: the sentence, version 2.

## Steps

1. Snapshots and builders.
2. Definitions.
3. Tests; `tmp/337-verify.sh`.

## Test plan

`tmp/337-verify.sh`: backend `mvn clean verify -pl modules/coverage-report -am` and the coverage
check (no Angular change).

- `EntityContextPackBuilderTest`:
  - an issue's severity is in its snapshot, and a note's is null;
  - with more annotations than the cap: every open HIGH is kept before any open LOW, open issues
    before notes, notes before resolved issues, and the current order within a group;
  - the same when the character budget cuts the list.
- `IssueContextPackBuilderTest`: severity on target and project issues.
- `PerTypeDefinitionsTest` (or the bundled-definition test): every review definition that mentions
  the annotations list carries the severity sentence and version 2.
- Existing context-pack and executor tests updated for the new fields.

## Out of scope

- Letting the AI change an existing issue's severity.
- Severity for the legacy NLP assistants (#268).
- Sending the issue context pack to any assistant.

## AC mapping

| AC | Covered by |
|---|---|
| Severity in the entity and issue context packs | EntityContextPackBuilderTest, IssueContextPackBuilderTest |
| Over the budget, every HIGH kept before any LOW | EntityContextPackBuilderTest |
| Notes carry no severity | EntityContextPackBuilderTest |
| Existing tests updated, a truncation test for the order | the above |

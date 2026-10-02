# #263 Seed and tune the per-type review definitions — implementation plan

Epic #258. Builds on #260 (definitions), #261 (context providers), #355 (fixture, `score.py`,
baseline). Folds in #360 (superseded AI issues left open).

## Summary

One bundled review definition per reviewable type, each with its own context providers,
vocabulary and instructions, tuned in rounds against the #355 fixture with the `cli` provider.
GlossaryTerm becomes reviewable. AI findings no longer pile up: a re-run removes untouched issues
it no longer reports, and a per-type definition retires the generic definition's findings on the
same entity. Extraction findings (content that belongs in another entity type) get their own
category.

## Review against the tree (`release/2.0` @ `e5309c76`)

- `RequirementsReviewAssistant` is already gone (#260); `ai-requirements-review` is the bundled
  fallback, scope empty, providers `["entity"]`.
- Reviewable: Goal, Story, Actor, UseCase, Scenario, Step. GlossaryTerm is not.
- Findings are keyed and retired per assistant id (definition key) and target
  (`CommandBackedAssistantResultApplicator.reconcileStaleFindings`). AI definitions use the default
  `MARK_SUPERSEDED`, so untouched superseded issues stay open (#360).
- `findingType` is a free string; nothing checks it against the definition's vocabulary.
- The output schema (`RequirementsReviewOutput` v1) has `additionalProperties: false`.
- Lexical checks already offer one-click positions (`ADD_ACTOR_TO_PROJECT`,
  `ADD_WORD_TO_GLOSSARY`) through position `kind` metadata.
- Definitions change only by editing classpath JSON and restarting.

## Locked decisions (2026-10-02)

1. Definitions for Goal, Story, UseCase, Scenario, Step, Actor and GlossaryTerm (made reviewable,
   with the GlossaryTerm provider moved from #261). `ai-requirements-review` stays as the fallback,
   unchanged.
2. A definition reviewing an entity also retires the active findings other definitions of the same
   task type left on it.
3. Fold in #360: AI definitions use `AUTO_RESOLVE_IF_UNTOUCHED` (untouched superseded issues are
   removed; discussed ones are kept and marked `SUPERSEDED`).
4. Overview: one toggle per definition, grouped under an "AI review" heading.
5. Tuning without a rebuild: dev-only `requel.ai.definitions.dir` plus a dev-profile reload.
6. Rounds: Ron runs `score.py`; Claude reads `raw.json`, edits the definitions, hands the next
   command. About 3 rounds: 1 run per entity for interim rounds, 3 for the final.
7. Unverified evidence: measured in round 1; keep or drop decided on that data.
8. Extraction findings:
   - their own `EXTRACT_` types (`EXTRACT_SCENARIO`, `EXTRACT_STORY`, `EXTRACT_ACTOR`,
     `EXTRACT_GLOSSARY_TERM`), tagged `category: extraction`, advisory, LOW, never must-resolve;
   - one-click positions where they exist (`EXTRACT_ACTOR` → add actor, `EXTRACT_GLOSSARY_TERM` →
     add glossary term);
   - prose positions for scenario and story;
   - instructions: only when the content clearly is another entity type, never alongside a quality
     finding on the same text.
9. The Ollama re-check moves to a v2.1 ticket.
10. Off-vocabulary finding types are kept and counted per run (`vocabulary_misses`), reported by
    `score.py`; `expectations.json` is widened for renamed types and the baseline re-scored.

## Contracts

### Bundled definitions (`assistant-ai/src/main/resources/ai/definitions/`)

| Key | Scope | Providers | Vocabulary (starting point) |
|---|---|---|---|
| `ai-review-goal` | Goal | entity, goal-relations, goal-siblings, goal-stakeholders | the issue's eleven Goal types, plus AMBIGUOUS, INCOMPLETE, UNTESTABLE, INCONSISTENT |
| `ai-review-story` | Story | entity, story-actors | AMBIGUOUS, INCOMPLETE, UNCLEAR_ACTOR, PERSON_NAMED_NOT_ROLE, ... |
| `ai-review-usecase` | UseCase | entity, usecase-scenarios | MISSING_PRECONDITION, MISSING_ERROR_CASE, UNCLEAR_ACTOR, PERSON_NAMED_NOT_ROLE, ... |
| `ai-review-scenario` | Scenario | entity, scenario-usecases | MISSING_ERROR_CASE, ORDERING, ... |
| `ai-review-step` | Step | entity, step-sequence | UNCLEAR_ACTOR, NOT_A_STEP, ... |
| `ai-review-actor` | Actor | entity, actor-references | PERSON_NAMED_NOT_ROLE, UNUSED_ACTOR, ... |
| `ai-review-glossary-term` | GlossaryTerm | entity, glossary-related | CIRCULAR_DEFINITION, CONFLICTING_DEFINITION, ... |

Each also lists the `EXTRACT_` types that fit its entity. Every set of instructions keeps the rules
in the current text: don't restate existing annotations, always set `suggestedIssueText`, return
empty rather than invent, cite evidence from the entity's own text. Vocabularies are tuned, not
settled.

### Output schema v2

`RequirementsReviewOutput` v2 = v1 plus an optional `suggestedEntityName` (the actor or term an
extraction finding proposes). Per-type definitions use v2; the fallback keeps v1 so its prompt is
unchanged.

### Vocabulary entries

`VocabularyEntry` gains an optional `category` (`quality` default, `extraction`). The executor
puts the category on finding metadata, forces extraction findings to LOW and not must-resolve, and
turns `EXTRACT_ACTOR` / `EXTRACT_GLOSSARY_TERM` with a `suggestedEntityName` into the existing
one-click positions.

### `glossary-related` provider

The term's canonical term and alternates, then other terms ranked by `TextSimilarity` on name and
text (summary), for circularity and conflict checks.

### Retirement across definitions

The executor's result carries the keys of the other definitions of its task type visible to the
project; after its own reconcile, the applicator reconciles those keys' active findings on the
same target under the same policy.

### Cleanup policy (#360)

`DefinitionExecutorAssistant.cleanupPolicy()` returns `AUTO_RESOLVE_IF_UNTOUCHED`.

### Vocabulary misses

Counted like unverified evidence: result metadata, `assistant_runs.vocabulary_misses` (V34), the
review read, `score.py`.

### Dev tuning

- `requel.ai.definitions.dir` (dev profile only): JSON files there override the bundled
  definitions with the same key at startup, whatever their version.
- `POST /api/dev/ai/definitions/reload` (dev profile only) re-reads the directory and evicts the
  definition cache.
- The tuned files are then copied back to `ai/definitions/` with version 1 (the first bundled
  version of each new key).

### Overview

`project-assistants-panel` groups the switches by kind: "Lexical checks" and "AI review".

## Tuning rounds

1. **Round 0** (code done): re-score the #355 baseline with the widened expectations.
2. **Round 1:** per-type definitions, 1 run per entity, with the unverified-evidence and
   vocabulary-miss counts.
3. **Round 2:** revised definitions, 1 run per entity.
4. **Final:** 3 runs per entity; zero schema failures; no restated annotations.

The log goes in `doc/work/2.0/263-tuning-log.md`: each round's change, scores per type, and what
was tried and rejected. `doc/work/2.0/263-what-varies-by-type.md` is the written input to #265 and
#266.

## Test plan

- Seeding: the seven definitions load and validate; each names providers that exist; the fallback
  is byte-identical (the #260 golden test still passes).
- GlossaryTerm review end to end (IT with the fake CLI).
- Retirement: a goal reviewed by the fallback, then by `ai-review-goal`, has the fallback's
  untouched finding removed and a discussed one marked `SUPERSEDED`.
- #360: two runs of one definition; an untouched finding the second run drops is removed.
- Extraction: an `EXTRACT_ACTOR` finding with a name becomes an issue with an add-actor position,
  LOW, not must-resolve.
- Vocabulary misses counted and recorded; `score.py` reports them.
- Dev reload: an edited file in the dev directory changes the next run's prompt; the reload endpoint
  is absent outside the dev profile.
- Overview grouping (Angular spec).

## Out of scope

- The Ollama re-check (v2.1 ticket).
- Positions that create scenarios or stories in one click.
- New task types; CI quality gates.

## Risks

- **Tuning takes rounds.** Each one needs your machine; the plan keeps it to about 3.
- **The add-actor position may need a lexical issue.** If it does, `EXTRACT_ACTOR` falls back to a
  prose position and that is noted.

## Implementation notes

- **Registry rule changed.** The registry now decides coverage before it applies the switches: a
  type with a per-type definition is never handed to the fallback. Switching `ai-review-goal` off
  stops AI review of goals; it does not bring back `ai-review-requirements`. The fallback covers
  only types with no definition (none of the seven reviewable types today, so it is effectively
  retired for them).
- **Dev tuning is keyed on the property, not the profile.** `DevDefinitionOverrides` and
  `DevDefinitionsController` exist only when `requel.ai.definitions.dir` is set. An override row is
  marked `updatedBy = dev-override`; the next start without the property puts the bundled
  definition back, whatever the versions.
- **Reload validates before it writes.** The files are validated against the bundled set merged
  with the overrides; an invalid file returns 400 with its problems and changes nothing.
- **Extraction one-click.** `EXTRACT_ACTOR` and `EXTRACT_GLOSSARY_TERM` with a
  `suggestedEntityName` become a lexical issue (`word` = the name) with an add-actor or
  add-to-glossary position, so they reuse the existing commands. `EXTRACT_SCENARIO` and
  `EXTRACT_STORY` stay prose positions. Every extraction finding is LOW and not must-resolve.
- **Vocabulary in the prompt.** A definition's instructions may contain `{{vocabulary}}`; the
  executor renders the finding types there (quality, then extraction). Without the placeholder the
  instructions are sent unchanged, so the fallback stays byte-identical.
- **Overview grouping.** `SwitchableAssistant` and `ProjectAssistantDto` carry a `group`; the panel
  shows headings only when there is more than one group. The "all off" hint ignores the AI review
  group.
- **Scoring.** `expectations.json` items gained `acceptTypes`: a finding of an accepted type matches
  whatever its wording. Additions only; the #355 patterns are unchanged.
- **Found in tuning round 1 (decided 2026-10-02):**
  - Import fix: `ScenarioImportDraft.stepRefs` was a `HashSet`, so imported scenarios lost their
    step order. Now a `List`; `FixtureExpectationsIT` checks the fixture's orders survive.
  - Evidence is checked against everything sent: the entity plus every string in its context
    sections. A scenario's flow is in its steps, so quoting a step was "unverified".
  - New provider `project-names`: the project's actor and glossary term names, no text, last in
    every definition with extraction types, so a suggestion is only made for something missing.
  - Fixture: the silent "Renew a loan" / "Renew from the account page" had real flaws (a
    precondition contradicted by a refusal clause, a reservation check with no branch, "7 days"
    from when) and the silent goal duplicated half of "Online reservations and a mobile app". They
    were rewritten to be clean; the goal is now "Members see what they have on loan".
  - Scoring: findings of an accepted type claim items first; leftover `EXTRACT_*` findings are
    suggestions; an entity's `alsoValid` list names real flaws it has beyond the targeted one.
- **Found in tuning round 2:** `actor-references` missed use cases and stories where the actor is
  only the primary actor (a primary actor is a separate link from the container's actors), so such
  an actor read as unused. The provider now also scans the project for primary-actor links.

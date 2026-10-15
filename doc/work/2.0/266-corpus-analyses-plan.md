# #266 Corpus analyses — implementation plan

Child 8 of epic #258. Review: 2026-10-03 against `release/2.0` @ `24a23b3e` (after #265).

## Summary

A **corpus analysis** looks at a set of entities and raises findings about how they relate. The
set is a whole project, or a goal or a use case with what hangs off it. The ticket is built in two
stages, with a checkpoint between them:

1. **Candidate finder (no AI).** Scores pairs in the set using #261's `TextSimilarity` plus WordNet
   synonyms. It flags possible conflicts from antonyms, negation, and differing numbers. Run on its
   own ("Find overlaps"), it writes advisory issues.
2. **Corpus analysis (AI).** A `CORPUS` definition gets one provider call. The call carries a
   summarised index of the set plus the finder's candidate pairs in full, and returns findings that
   name two or more participants.

A relationship finding is **one issue attached to every participant**, using #268's shared-issue
path. It is keyed on the definition, the finding type and the sorted participant refs. It goes
**stale on every participant when any one of them changes**. Neither stage ever runs from an edit.

## Review against the tree

- `ProjectContextPackBuilder` exists, and nothing outside its tests uses it.
  - It carries full text (up to the per-field max) and has no summary form.
  - It truncates lists with a note ("goals list truncated by total-character budget") instead of
    refusing.
- Requests and actions name one target each.
  - `AnalysisRequest` has one `targetRef`, and `AnnotationAction` has one `targetRef`.
  - `ProjectEntityTargetLoader` already loads `Project`, so a run can target a project or a root
    entity.
- **Multi-subject issues already exist.**
  - #268's `findSharedIssue` joins actions marked `scope=PROJECT` that share a `shareKey` into one
    open issue across entities. It reads the share key from the fourth part of the idempotency key
    (`<assistantId>:<Type>:<id>:<shareKey>`).
  - One finding row per participant keeps #270's per-target fingerprint, ignore and cleanup
    working.
- **Freshness is per entity by design.** `AnnotationFreshness` says "an annotation shared by several
  entities can be stale on one and not on another". Decision 3 needs a group rule for corpus
  findings, and the finding row has nothing that marks one.
- `assistant_findings.idempotency_key` is `VARCHAR(255)`. A list of sorted participant refs won't
  fit for larger findings, so the share key is a hash.
- **Cleanup is keyed by target.** `reconcileStaleFindings` covers the dispatch target plus the
  targets that produced keys. That has two consequences for corpus runs:
  - A participant that no longer appears in any corpus finding would never have its old finding
    retired.
  - A subset run must not retire findings about pairs outside its set.
- **Dictionary.**
  - `lexlinkref` and `linkdef` are loaded (`DictionarySQLInitializer`), including antonym links,
    and synsets give synonyms.
  - `DictionaryRepository` exposes neither.
  - `TextSimilarity` sits in assistant-core, which can't see dictionary-jpa.
- **Post-edit path.** `AnalysisInvokingCommandHandler` dispatches every enabled assistant with no
  task type after an edit. The corpus task types must never be in that list.
- `IssueDto` has no subjects. A shared issue shows on each entity with no sign of the others.
- `DefinitionKind` is `REVIEW` or `POLICY`. The `kind` column is `VARCHAR(20)`, so `CORPUS` needs
  no migration.

## Locked decisions (2026-10-03)

1. **One ticket, two stages.**
   - Splitting would buy smaller reviews and let the finder ship before the AI pass. It would also
     cost a second CI run and a second lifecycle round, and it wouldn't make development easier:
     the finder is a self-contained class either way.
   - Stage 1 is built and tuned first. The thresholds are agreed from the harness output (the
     checkpoint) before stage 2 starts, as in #268.
2. **One issue on all participants**, through #268's shared-issue path.
3. **Stale when any participant changes.** The issue shows stale on every participant.
4. **The goal review keeps its sibling-duplicate check** (#261 `goal-siblings`). The finder owns the
   exhaustive sweep. Both may raise the same pair; this is documented, not deduplicated.
5. **Tuning data.**
   - The synthetic fixture (#355) is extended and committed.
   - The roundtable and Requel projects run through a local harness.
   - Real-project output stays in `tmp/` (gitignored), as in #268. Only counts and the chosen
     thresholds go into committed docs.

## Contracts

### Sets

| Set | Members |
|---|---|
| `PROJECT` | every goal, story, actor, use case, scenario, step and glossary term |
| `GOAL` | the goal, the goals reachable through its #257 relations, and the stories and use cases linked to any of them |
| `USE_CASE` | the use case, its scenarios and their steps, and its primary actor |

- The run targets the root: the project for `PROJECT`, otherwise the goal or use case.
- The set kind travels in `AnalysisRequest.attributes` (`corpusSet`).
- "Changed since the last run" is a follow-up (see Out of scope).

### Stage 1: candidate finder

- **`WordRelations` SPI (assistant-core).**
  - `synonyms(lemma)` and `antonyms(lemma)`, as stems.
  - `WordRelations.NONE` when no dictionary is present.
  - `WordNetWordRelations` (assistant-legacy-nlp) implements it. Synonyms come from the first N
    senses' synsets, same part of speech. Antonyms come from `lexlinkref` antonym links. Both are
    cached.
- **`CorpusCandidateFinder` (assistant-core).**
  - Input: each set member's name and text. Output: `CandidatePair(a, b, score, hints)`.
  - **Score:** `TextSimilarity` with synonym expansion. A token also counts its synonyms' stems at
    weight `w`, and glossary folding is unchanged.
  - **Pairs:** every pair in the set, using sparse vectors.
  - **Hints on scored pairs:**
    - `ANTONYM`: a word in one, its antonym in the other.
    - `NEGATION`: opposite polarity on a shared verb (must / must not, can / cannot,
      always / never).
    - `NUMBER`: the same noun or unit with a different number ("5 checks" / "6 checks",
      "14 days" / "21 days").
  - **Pair kind:** `CONFLICT` when the score is at least `t_conflict` and there is a hint.
    Otherwise `OVERLAP` when the score is at least `t_overlap`.
  - Deterministic. The thresholds and `w` are set at the checkpoint.
- **Run alone.** The `corpus-finder` assistant (task `CORPUS_CANDIDATES`) is dispatched manually
  and raises advisory issues: not must-resolve, LOW severity.
  - Overlap text: "Possible overlap with Goal 'Members search the catalogue by name' (0.62)".
  - Conflict text: "Possible conflict: '14 days' here, '21 days' in Goal '…'".
  - Each issue is attached like any corpus finding (below). Cleanup is
    `AUTO_RESOLVE_IF_UNTOUCHED` within the set.
- **Harness (dev only, removed before merge, as in #268).**
  - `GET /api/dev/corpus-candidates?projectId=` returns TSV: pair, types, score with and without
    synonyms, hints, and the verdict at each candidate threshold. It is loopback-only and enabled
    by `requel.dev.corpus-harness.enabled`.
  - `tmp/266-harness.sh` saves the output to `tmp/266-harness-<project>.tsv`.

**Checkpoint:** run the harness on the fixture, roundtable and Requel. Agree on `t_overlap`,
`t_conflict`, `w` and the sense cut-off, record them in "Finder thresholds" below, then start
stage 2.

### Stage 2: corpus analysis

- **Definition.**
  - `kind CORPUS` is valid only with `taskType CORPUS_REVIEW`, and the reverse.
  - Scope lists the set kinds a definition accepts; empty means all.
  - The collision rule is the review rule: at most one enabled definition per set kind.
- **Context: `corpus-index` and `corpus-candidates`.** Only corpus definitions may name them. They
  aren't per-entity providers: `CorpusPackBuilder` builds both into one `CorpusPack`.
  - **Index:** every member gets its ref (`Type:id`) and name, plus text cut to
    `requel.ai.corpus.index-text-chars` (default 240). It also carries `linkedTo`, the structural
    links of `CorpusMembers`: referers, primary actors, use case to scenario, a scenario's steps,
    glossary alternates. Redaction goes through
    `RedactionPolicy.forProject`, as `ProjectContextPackBuilder` does.
  - **Candidates:** the finder's pairs in rank order, each with both texts in full and the hints.
- **Budget: `requel.ai.corpus.max-input-chars`.**
  - **Index over budget → refusal.** The run fails, with no findings and no cleanup:
    "The corpus index for Project 12 is 182,400 characters; the limit is 60,000
    (requel.ai.corpus.max-input-chars). Analyse a goal or use case instead, or raise the limit.
    Nothing was analysed."
  - **Candidates that don't fit** are left out in rank order. The run is then marked partial
    (`partial`, `candidatesSent`, `candidatesFound`), the summary says "Sent 40 of 63 candidate
    pairs", and the UI shows it as partial. It is never reported as complete.
- **Output schema `CorpusReviewOutput` v1.** Each finding has `findingType`, `participants`
  (`Type:id` strings, at least 2), `severity`, `confidence`, `evidenceReferences` (quotes),
  `suggestedIssueText` and `suggestedPositions`. The participant enum is filled per request from
  the set.
- **Mapping.**
  - A participant outside the set is dropped and counted (`unresolvedParticipants`).
  - Each quote must appear in the participants' text as sent; otherwise it counts towards
    `evidenceUnverified`, as in #263.
  - A finding left with fewer than 2 participants is dropped.
  - Within one run, findings with the same type and participant set are merged. This is what
    makes a term used with two meanings come out once.
- **Attachment.**
  - Each finding becomes one `CREATE_OR_UPDATE_ISSUE` per participant, marked `scope=PROJECT`.
  - `shareKey = <TYPE>-<first 16 hex of sha-256(sorted refs)>`.
  - Action key: `<definitionKey>:<Type>:<id>:<shareKey>`.
  - The applicator's `findSharedIssue` joins them into one issue. Positions hang off the first
    participant's action.
- **Idempotency.** A re-run on an unchanged set produces the same keys, which touch the existing
  findings and create nothing new.
- **Cleanup.**
  - Corpus definitions use `AUTO_RESOLVE_IF_UNTOUCHED`, like reviews (#360): a re-run removes an
    untouched issue it no longer reports, and one a person discussed reads superseded.
  - Reconcile covers every member of the analysed set, and only the definition's findings whose
    participants are all inside that set.
- **Staleness.**
  - V36 adds `assistant_findings.stale_together` (BOOLEAN, default false), which is set on corpus
    rows.
  - `FindingFreshness` checks the other participants of a `stale_together` annotation as well,
    loading them through the target loaders, one load per participant per read. If any one of them
    is stale, the annotation is stale on all of them.
- **Finder vs analysis.** An analysis run's result lists the `corpus-finder` keys of the pairs it
  sent (`retiresFindings`); the applicator auto-resolves those still active and untouched. The
  model's verdict replaces the hint.
- **Read.** `GET /api/ai/corpus` lists every finding the run reported, on any participant; the
  run read's findings gain `targetType` and `targetId`.
- **Cost.** One provider call per analysis run. The input budget is the ceiling, and the run
  records the characters sent.

### Bundled definition: `ai-corpus-relationships`

- **Scope:** all set kinds. **Providers:** `corpus-index`, `corpus-candidates`. Enabled, and a
  project can switch it off.
- **Vocabulary:**
  - `CONTRADICTORY_REQUIREMENTS`
  - `SAME_TERM_DIFFERENT_MEANING`
  - `SAME_CONCEPT_DIFFERENT_TERMS`: no glossary link; #265's terminology policy covers linked
    alternates.
  - `UNSATISFIABLE_BY_CHILDREN`
  - `DUPLICATE_AT_DIFFERENT_ABSTRACTION`
  - `ORDERING_DEPENDENCY_UNSTATED`
  - `OTHER`
- **Instructions:** the #263 shared rules (one finding per problem, quoted evidence, empty is
  good), plus these:
  - A finding must need two entities to see.
  - Judge each candidate pair: raise it or leave it.
  - Raise a pair that isn't a candidate only when the index alone shows it.

### Dispatch, API and UI

- `POST /api/ai/corpus` with `{projectId, set, rootId?, mode: CANDIDATES | ANALYSIS}`.
  - `CANDIDATES` needs no provider. `ANALYSIS` passes the #262 egress checks.
  - Dispatch is manual only. Both task types are left out of the no-task (post-edit) dispatch.
- `GET /api/ai/corpus?projectId=&set=&rootId=&mode=` returns the latest run, including partial and
  refusal details.
- `IssueDto` gains `subjects` (`{type, id, name}`) when the issue is on more than one entity, and
  `sourceKind` gains `CORPUS`.
- **Annotations section:** "Also on: Goal 'Member data stays private'", with links.
- **Project overview:** a "Corpus analysis" section with a set picker, the "Find overlaps" and
  "Analyse" buttons, and the last run's status.

### Eval

- **Fixture** (exact wording settled during implementation):
  - Existing pairs that become corpus expectations:
    - SMS reminders vs member privacy (`CONTRADICTORY_REQUIREMENTS`).
    - "Members find tools quickly" vs "Members search the catalogue by name"
      (`DUPLICATE_AT_DIFFERENT_ABSTRACTION`).
    - "Non-staff" vs "Member" (`SAME_CONCEPT_DIFFERENT_TERMS`).
  - New:
    - "hold" as a reservation and as an account block (`SAME_TERM_DIFFERENT_MEANING`).
    - Parent goal "returned within 14 days" vs a child allowing three 14-day renewals
      (`UNSATISFIABLE_BY_CHILDREN`).
    - Loan length 14 days vs 21 days (finder `NUMBER` hint, `CONTRADICTORY_REQUIREMENTS`).
    - A use case that relies on another's outcome without saying so
      (`ORDERING_DEPENDENCY_UNSTATED`).
  - Silent pairs: Member vs Staff, and the canonical "Member" / "Patron" glossary link.
- **`expectations.json`:**
  - `corpusExpect`: per expected relationship, the types with `acceptTypes`, and the participant
    names.
  - `corpusAlsoValid`.
  - `corpusSilent`: the pairs.
- **`score.py --corpus`.** It runs `CANDIDATES`, then `ANALYSIS`, N times on the fixture and reports:
  - finder recall on the expected pairs and precision on the silent ones;
  - analysis hit rate, spurious findings per run, and correct participants;
  - one issue per relationship;
  - a re-run that adds nothing.
- **Real projects:** harness counts, and one `ANALYSIS` run per project read by hand. The counts go
  in the tuning log.

## Finder thresholds

Checkpoint, 2026-10-03. Measured on the eval fixture (53 members) and roundtable (62 members); the
Requel project isn't in the local database. Details in `266-tuning-log.md`.

| Setting | Value | Why |
|---|---|---|
| Synonym weight `w` | 0.5 | Raises the fixture's "hold" pair (0.18 to 0.25) without the noise 1.0 adds ("put" / "place") |
| Synonym senses | ranks 1 to 2 | the common senses only |
| Conflict threshold | 0.20 | both planted number conflicts score 0.28 to 0.33 |
| Overlap threshold, Find overlaps | 0.35 | about 10 overlaps on roundtable, most of them real |
| Overlap threshold, AI candidates | 0.25 | the fixture's duplicate goals score 0.25; the model filters |

Changes the harness forced:

- **Overlaps only between types that can repeat each other**: same type, actor and glossary term,
  and goal, story and use case with each other. Steps never overlap on their own. Without this,
  roundtable's overlap list was mostly an actor or a term mentioned in a step.
- **Antonyms only from adjective and adverb senses** ranked 1 to 2. Verb and noun antonyms (start /
  stop, pass / fail, one / off) named the steps of a process: 45 roundtable hints at 0.15, none a
  conflict.
- **Glossary terms of one canonical term are linked**, so two alternates aren't an overlap.
- Negation found nothing on roundtable; number hints are kept.

## Tuning

- **Stage 1:** two harness rounds on the fixture and roundtable, then the checkpoint (above).
- **Stage 2:** round 1 (1 run), round 2 (2 runs), final (3 runs): 78% hit rate, 0.3 spurious per
  run. Log in `266-tuning-log.md`; final scores in `266-ai-eval-final.md` (+ raw).

## Test plan

- **Unit:**
  - `WordNetWordRelations`: synonyms and antonyms (increase / decrease), same part of speech
    only.
  - Finder:
    - synonyms raise the score;
    - each hint fires, and doesn't fire on unrelated numbers;
    - output is deterministic.
  - Index and pack:
    - summary form, relations, redaction;
    - the refusal message;
    - partial candidates.
  - Validator: the `CORPUS` / `CORPUS_REVIEW` pairing, and set-kind scope.
  - Mapper:
    - fewer than 2 participants is dropped;
    - a participant outside the set is dropped;
    - evidence is checked against its own participant;
    - findings are merged within a run;
    - the share key ignores participant order.
  - `FindingFreshness`: a group goes stale when any participant changes; non-group rows are
    unchanged.
- **IT through the worker (fake CLI):**
  - AC1: a contradiction names both participants.
  - AC2: a two-meaning term is raised once.
  - AC3: every finding has 2 or more refs, and each resolves.
  - AC4: a re-run adds no duplicates.
  - AC5: the refusal.
  - AC6: editing a goal queues no corpus run (`AnalysisInvokingCommandHandler`).
  - AC7: review and corpus findings coexist on one entity.
  - A subset run doesn't retire findings outside its set.
  - An analysis run resolves the finder's advisory issues on the pairs it sent.
- **Angular:** "Also on" links; the corpus section.
- **`test_score.py`:** corpus scoring.

## Build order

**Stage 1**

1. `WordRelations` and `WordNetWordRelations`.
2. `CorpusCandidateFinder`.
3. The harness. Run it on the fixture, roundtable and Requel → **checkpoint**.
4. Set resolution, the `corpus-finder` assistant, attachment, V36 and group freshness, the
   `IssueDto` subjects, the dispatch API, and the UI.

**Stage 2**

5. `CORPUS` kind, the validator, the schema, the index and candidate providers, and the budget
   refusal.
6. The corpus executor, the mapper, scoped cleanup, and replacing the finder's issues.
7. The bundled definition, the fixture, `expectations.json` and `score.py`.
8. Tuning. Docs: `AI_ASSISTANT_SETUP.md` §11b "Corpus analyses". Remove the harness. Write
   `commit.md` and `pr.md`.

## Out of scope

- Cross-project analysis.
- Automatic repair: the output is an issue with positions.
- Chunking or streaming for large projects. The refusal comes first.
- A "changed since the last run" set.
- Running the finder from the post-edit (lexical) path.

## Risks

- **The index cut hides the conflicting sentence.** Candidates carry full text. Relationships
  visible only in the index may be missed, which is measured on the fixture.
- **WordNet synonyms add noise** (bank / bank). Mitigations: same part of speech, the first N
  senses, and a tuned weight.
- **Pair count grows as n²** on large projects. Vectors are sparse, and the time is measured on
  roundtable.
- **Group freshness loads participants on read.** The load is bounded by the annotations shown.

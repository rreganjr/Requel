# #263 Tuning log: per-type review definitions

Each round scores the fixture in `scripts/ai-eval/` with the `cli` provider (claude). The baseline
is #355 (`355-ai-eval-baseline.md`): the fallback definition `ai-review-requirements@1` reviewing
every type, 3 runs per entity.

## Round 0: re-score the baseline with the widened expectations

`expectations.json` gained `acceptTypes` per item (a finding of an accepted type matches whatever
its wording). Re-scoring `355-ai-eval-baseline-raw.json` gives the same numbers as the baseline:
the fallback never emits the per-type finding types, so widening does not inflate the baseline.

| Type | Hit rate | Spurious per run | Silent kept silent | Confirmed (trap) |
|---|---|---|---|---|
| Goal | 49% (19/39) | 4.6 | 0% (0/3) | 0 |
| Story | 50% (6/12) | 4.1 | 0% (0/3) | 1 |
| UseCase | 100% (6/6) | 3.9 | 0% (0/3) | 0 |
| Scenario | 100% (6/6) | 5.1 | 0% (0/3) | 0 |
| Step | 100% (6/6) | 3.9 | 0% (0/3) | 0 |
| Actor | 100% (6/6) | 2.3 | 0% (0/3) | 0 |
| GlossaryTerm | skipped | - | - | - |
| **All** | 65% (49/75) | 4.2 | 0% (0/18) | 1 |

What to beat: Goal and Story hit rates, spurious findings per run, and silent entities kept
silent (none today).

## Round 1: the seven per-type definitions, 1 run per entity

`target/ai-eval/20261002-020227`. Scored with the round-2 matching (below), which only moves
findings between columns.

| Type | Hit rate | Spurious per run | Silent kept silent | Unverified evidence | Suggestions per run |
|---|---|---|---|---|---|
| Goal | 100% (13/13) | 1.3 | 0% (0/1) | 0 | 0.2 |
| Story | 100% (4/4) | 2.0 | 0% (0/1) | 0 | 0.4 |
| UseCase | 100% (2/2) | 4.3 | 0% (0/1) | 7 | 0.7 |
| Scenario | 100% (2/2) | 3.3 | 0% (0/1) | 13 | 0.7 |
| Step | 100% (2/2) | 0.3 | 100% (1/1) | 0 | 0.0 |
| Actor | 100% (2/2) | 0.7 | 100% (1/1) | 0 | 0.3 |
| GlossaryTerm | 100% (2/2) | 0.3 | 0% (0/1) | 0 | 0.0 |
| **All** | 100% (27/27) | 1.6 | 29% (2/7) | 20 | 0.3 |

No schema failures, no off-vocabulary types, the trap not confirmed.

What the details showed:

- **Import bug: scenario steps lost their order.** `ScenarioImportDraft` kept step references in
  a `HashSet`, so every imported scenario came out in hash order (`STP_8..11` became
  `9, 8, 11, 10`). The baseline's step-order findings on the silent "Renew from the account page"
  were the bug, not the review. Fixed in this ticket (a `List`), with an IT on the fixture.
- **Scoring: a wording match took the item from the finding that named the flaw.** On "Online
  reservations and a mobile app", a duplicate finding matched `goal-conjunctive`'s patterns first
  and the `CONJUNCTIVE_GOAL` finding went to spurious. Findings of an accepted type now claim
  items first.
- **Scoring: extraction findings are suggestions.** An unmatched `EXTRACT_*` finding is counted
  separately, not as spurious, and does not break a silent case. The baseline re-scores unchanged.
- **Story `UNCLEAR_ACTOR` on 4 of 5 stories**, all "the story has no actor linked". The fixture
  links none; the definition invited it ("its actors match the ones it lists").
- **`AMBIGUOUS` on 10 of 14 goals**, mostly detail left for later (timing, frequency, scope).
- **Several findings of one type** (two `MISSING_ERROR_CASE`, two `STEP_ORDER`) where one would do.
- **Unverified evidence only on use cases and scenarios**: the check compares against the
  entity's own text, and a use case's flow is in its scenario's steps.
- **Extraction claims without knowing the project**: "not defined in the glossary", "no Member
  actor" (there is one). The definitions don't see the project's actors or glossary.

## Round 2 changes

All seven definitions:

- One finding per problem, under the most specific type; gaps of one type in one finding.
- `AMBIGUOUS` only for a word or phrase with two readings that lead to different systems; detail
  left for later is not ambiguity.
- Suggest an actor or glossary term only when central to the target and not already shown in the
  context.

Story: linked actors are not reviewed; `UNCLEAR_ACTOR` is about the text; a `so that` clause with
any reason is a benefit.

Decided with Ron after round 1:

1. Evidence is checked against everything sent (the entity and its context sections), and the
   definitions say evidence may come from a context section.
2. New `project-names` provider (actor and glossary term names), added last to every definition
   with extraction types; suggest an actor or term only when it isn't listed.
3. The silent fixture cases that had real flaws were rewritten (see the plan's implementation
   notes). Their earlier numbers are not comparable.
4. `alsoValid` per entity in `expectations.json` for real flaws the fixture doesn't target:
   counted as "Also valid", neither a hit nor spurious.

With the new scoring the baseline re-scores at 65% hit rate, 4.1 spurious per run, 0/18 silent;
round 1 at 100%, 1.4 spurious per run, 2/7 silent.

## Round 2: one problem once, project names, evidence against everything sent

`target/ai-eval/20261002-065441`, 1 run per entity. Scored with the round-3 expectations.

| Type | Hit rate | Spurious per run | Silent kept silent | Unverified evidence | Suggestions per run | Also valid per run |
|---|---|---|---|---|---|---|
| Goal | 100% (13/13) | 0.4 | 100% (1/1) | 1 | 0.1 | 0.4 |
| Story | 75% (3/4) | 0.2 | 100% (1/1) | 0 | 1.0 | 0.2 |
| UseCase | 100% (2/2) | 0.7 | 0% (0/1) | 1 | 0.3 | 2.3 |
| Scenario | 100% (2/2) | 0.3 | 0% (0/1) | 0 | 0.3 | 1.0 |
| Step | 100% (2/2) | 0.3 | 0% (0/1) | 0 | 0.0 | 0.0 |
| Actor | 100% (2/2) | 0.7 | 0% (0/1) | 0 | 0.3 | 1.0 |
| GlossaryTerm | 100% (2/2) | 0.3 | 0% (0/1) | 0 | 0.0 | 0.0 |
| **All** | 96% (26/27) | 0.4 | 29% (2/7) | 2 | 0.3 | 0.6 |

Unverified evidence fell from 20 to 2. Extraction suggestions now name what is missing ("only
'Non-staff' exists").

What the details showed:

- **The trap's `UNSOURCED_CLAIM` was missed** (the story raised the backlog ambiguity instead).
- **Silent cases failing for the fixture's reasons:** no Staff actor although steps and scenarios
  use staff; "Pickup date" can mean the agreed or the actual day; "Non-staff" overlaps "Member", and
  the overlap was raised on Member.
- **Silent use case and scenario still asked for branches for their own preconditions.**
- **Bug: an actor that is only a primary actor read as unused.** A primary actor is its own link,
  not one of the use case's actors, so `actor-references` didn't list "Approve a purchase" for Dana.
  Fixed: the provider also scans the project's use cases and stories for the primary actor.
- **Flawed use cases and scenarios have more real flaws than the one they target** (one-sentence
  texts); those went to `alsoValid`.

## Round 3 changes

- Story `UNSOURCED_CLAIM`: names counts, baselines and targets; raise it even when the figures agree.
- Use case and scenario `MISSING_ERROR_CASE`: a stated precondition needs no branch.
- Actor `OVERLAPPING_ROLE`: raise it on the actor whose vague definition causes the overlap.
- Fixture: a Staff actor; "Pickup date" says it is the day chosen when reserving.
- `alsoValid` for the real extra flaws of "Check out a tool", "Approve a purchase", "Approve a
  purchase request", "Dana Whitfield" and "Non-staff".

## Round 3

`target/ai-eval/20261002-091551`, 1 run per entity.

| Type | Hit rate | Spurious per run | Silent kept silent | Unverified evidence | Suggestions per run | Also valid per run |
|---|---|---|---|---|---|---|
| Goal | 100% (13/13) | 0.4 | 100% (1/1) | 0 | 0.1 | 0.4 |
| Story | 100% (4/4) | 0.4 | 100% (1/1) | 0 | 0.4 | 0.2 |
| UseCase | 100% (2/2) | 0.0 | 100% (1/1) | 1 | 0.7 | 2.3 |
| Scenario | 100% (2/2) | 0.0 | 100% (1/1) | 0 | 0.0 | 1.0 |
| Step | 100% (2/2) | 0.0 | 100% (1/1) | 0 | 0.0 | 0.0 |
| Actor | 100% (2/2) | 0.0 | 100% (1/1) | 0 | 0.3 | 0.7 |
| GlossaryTerm | 100% (2/2) | 0.0 | 100% (1/1) | 0 | 0.0 | 0.0 |
| **All** | 100% (27/27) | 0.2 | 100% (7/7) | 1 | 0.2 | 0.5 |

The trap's `UNSOURCED_CLAIM` is back and every silent case is clean. The 7 spurious findings are
mostly defensible readings (what counts as a complaint, a measure that only tracks, who triages);
no definition change was made for them. Next: the final run, 3 per entity, same definitions.

## Final: 3 runs per entity

`target/ai-eval/20261002-093048`, 102 reviews, same definitions as round 3. Results and the
per-entity table are in `263-ai-eval-final.md`; the raw findings are checked in as
`263-ai-eval-final-raw.json`.

| | Baseline (#355, re-scored) | Final |
|---|---|---|
| Hit rate | 65% (49/75) | 100% (81/81) |
| Spurious per run | 4.0 | 0.2 |
| Silent kept silent | 0% (0/18) | 100% (21/21) |
| Trap confirmed | 1 of 3 | 0 of 3 |
| Schema failures, off-vocabulary types | 0, - | 0, 0 |

The first scoring said the trap was confirmed in 2 of 3 runs. Both were `UNSOURCED_CLAIM`
findings saying "the figures add up, but nothing says where they come from" — the right review —
matched by the `adds up` confirm pattern. The confirm rule now ignores a text that also matches
the trap item's patterns (it questions the figures); the baseline's real confirmation still counts.

Tried and kept: everything in rounds 1 to 3. Tried and rejected: nothing reverted; the remaining
spurious findings (mostly goal `AMBIGUOUS` on "the tool library" and "find", and a standing approval
rule read as a constraint) are defensible readings, so no rule was added against them.

The tuned files are the shipped ones (`tmp/263-defs` and `ai/definitions/` are identical), at
version 1, the first bundled version of each key.

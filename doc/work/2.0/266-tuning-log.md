# #266 Tuning log

Corpus analyses: the stage 1 candidate finder, then the stage 2 AI pass. Final scores go in
`266-ai-eval-final.md`. Roundtable is Medlive work content: its text stays in `tmp/` (gitignored),
and only counts are recorded here.

## Stage 1, round 1 (2026-10-03): the finder as first built

Harness `tmp/266-harness.sh` at synonym weights 0, 0.5 and 1.0. Settings: overlap 0.35, conflict
0.20, every type pair, antonyms from any sense.

### Fixture (53 members, WordNet antonyms loaded)

| Planted relationship | Score (w = 0.5) | Found |
|---|---|---|
| Two-week loans / Keep a tool for three weeks (14 / 21 days) | 0.28, NUMBER hint | conflict |
| Two-week loans / Renew a loan (14 / 7 days) | 0.33, NUMBER hint | conflict |
| Members find tools quickly / Members search the catalogue by name | 0.25 | below 0.35 |
| Unpaid fines put accounts on hold / Place a hold on a tool | 0.25 (0.18 without synonyms) | below 0.35 |
| Send overdue reminders by text message / Member data stays private | under 0.05 | no shared words |
| Non-staff / Member | 0.10 | no shared words |

The last two need the AI pass: the finder has nothing to compare.

### Roundtable (62 members)

| | w = 0 | w = 0.5 | w = 1.0 |
|---|---|---|---|
| pairs scoring 0.15 or more | 177 | 225 | 295 |
| of those linked | 35 | 44 | 45 |
| with a hint | 24 | 45 | 66 |
| overlaps at 0.35 (not linked) | 21 | 26 | 27 |
| conflicts at 0.20 | 14 | 20 | 29 |

Read by hand:

- **Hints.** Every hint was an antonym, and none a conflict: verbs and nouns naming the steps of a
  process (start / stop, pass / fail, live / recording) or loose senses (one / off, first /
  seconds). No negation hint; two number hints, both between steps of one recovery procedure.
- **Overlaps.** Most were an actor or a glossary term mentioned in a step or a scenario, or a step
  that performs a use case. The pairs worth raising were between goals, stories, use cases, actors
  and terms: a goal and the use case that realises it, a goal and a story saying the same thing,
  an actor and the glossary term for the same role, two alternates of one term (score 1.0).
- **Synonyms.** 0.5 helps the fixture and changes little on roundtable; 1.0 adds noise.

### Changes

- Overlaps only between types that can repeat each other; steps never overlap alone.
- Antonyms only from adjective and adverb senses ranked 1 to 2.
- The alternates of one canonical term are linked.

With the type rule, roundtable has about 10 overlaps at 0.35 (w = 0.5), most of them real, and
17 at 0.25.

## Stage 1, round 2 (2026-10-03): type pairs, adjective antonyms, linked alternates

Roundtable, after the round 1 changes:

| | w = 0 | w = 0.5 | w = 1.0 |
|---|---|---|---|
| overlaps at 0.25 (comparable, not linked) | 12 | 15 | 18 |
| overlaps at 0.35 | 6 | 7 | 8 |
| conflicts at 0.20 | 7 | 11 | 17 |

- **Overlaps.** The 7 at 0.35 (w = 0.5) are the pairs worth raising: a goal and the use case that
  realises it (twice), a goal and a story saying the same thing, two use cases sharing most of a
  flow, a viewers-are-invisible goal and the story behind it, two glossary terms for ending a
  room, the Viewer actor and the Attendee term. The two alternates of one term are gone.
- **Hints.** Still antonym-only, still not conflicts: adjectives that name different things in one
  text (live / recording, same / different, one / off).

### Change

An antonym hint now needs the two words to share a neighbour within 3 words: the same thing
said both ways ("data stays private" / "data is public"). On the fixture this drops "fewer" /
"more" (complaints against overdue tools) and keeps the number conflicts. Roundtable's hints are
re-checked with the stage 2 run.

### Agreed thresholds

Overlap 0.35 for Find overlaps, 0.25 for the pairs sent to the AI analysis, conflict 0.20,
synonym weight 0.5 (plan, "Finder thresholds").

## Stage 2, round 1 (2026-10-03): the AI analysis as first written

`tmp/266-eval.sh 'Roundtable' 1 "round 1"`: the harness on roundtable, then one Find overlaps run
and one analysis run on the fixture (cli/claude).

**Roundtable hints with the neighbour rule:** conflicts at 0.20 fell from 11 to 4 (29 at 0.15 to
9). Two are number hints between timing steps of one recovery procedure, worth a look; the rest
are adjective antonyms. Find overlaps would raise 7 overlaps and 4 conflicts on 62 entities.

**Fixture:**

| | Result |
|---|---|
| Find overlaps, expected pairs | 2/2 (both number conflicts), 0 silent pairs, 7 pairs |
| Analysis hit rate | 2/6 |
| Exact participants | 2/2 |
| Spurious | 1, re-scored to 0 (below) |
| Also valid | 2, then 3 |
| Duplicate issues, unverified evidence, partial runs | 0 |

- **Hits:** the 14 / 21 day contradiction and the two meanings of "hold".
- **Also valid:** "suspended" and "on hold" as two names for one account state; the patron story
  repeating the renewal use case.
- **The one spurious finding was real:** a hold, the renewal use case's "reserved", and the pickup
  date term as one concept under three names. Added to `alsoValid` as `hold-vs-reservation`;
  re-scored with `--rescore`, spurious is 0.
- **Misses:**
  - the renewal that pushes a loan past the goal's 14 days (a candidate; the model judged it
    consistent);
  - SMS reminders vs member privacy, and Non-staff vs Member (no shared words, so never
    candidates; the model didn't raise them from the index);
  - the duplicate search goals (a candidate at 0.25; not raised).

### Change

The instructions now tell the model to read the whole index, not only the candidates, and list
what to look for there: a means that breaks another rule, a value one entity changes that another
fixes (even step against outcome), overlapping actors or terms, a narrower version of a goal or
story. Tuned through `requel.ai.definitions.dir` (`tmp/266-defs`), so the version stays 1.

## Stage 2, round 2 (2026-10-03): read the whole index

`tmp/266-eval.sh 'Roundtable' 2 "round 2"`, the round 1 instructions plus the index rules.

| | Round 1 (1 run) | Round 2 (2 runs) |
|---|---|---|
| Analysis hit rate | 2/6 | 9/12 |
| Exact participants | 2/2 | 7/9 |
| Spurious per run | 1.0, re-scored 0 | 1.5, re-scored 0 |
| Silent pairs, duplicate issues, unverified evidence, partial runs | 0 | 0 |

| Expectation | Round 1 | Round 2 |
|---|---|---|
| contradiction-loan-length | 1/1 | 2/2 |
| renewal-breaks-loan-length | 0/1 | 2/2 |
| sms-vs-privacy | 0/1 | 0/2 |
| duplicate-search | 0/1 | 1/2 |
| hold-two-meanings | 1/1 | 2/2 |
| non-staff-member | 0/1 | 2/2 |

- **The renewal finding** names the goal, the use case, the renewal step and the patron story:
  more participants than expected (a hit, not exact), and all of them part of it.
- **The three "spurious" findings were real** and are now `alsoValid`: checkout never checks the
  suspended-member rule (goal against use case), the purchase scenario's steps in reverse order,
  and an after-hours return that can't follow the late-returns flow.
- **SMS vs privacy** is still missed. The text goes to the member themselves, and only the carrier
  is outside the library: debatable as a conflict, so the instructions are not fitted to it.

The round 2 instructions are the shipped ones (version 1).

## Final (2026-10-03)

`tmp/266-eval.sh 'Roundtable' 3 "final"`, the round 2 instructions unchanged: 78% hit rate
(14/18), 0.3 spurious per run, 3.3 also valid, 0 silent pairs, 0 duplicates, 0 unverified
evidence. Details in `266-ai-eval-final.md`. Roundtable's harness output was unchanged from
round 2.

The harness (`CorpusHarnessController`, `tmp/266-harness.sh`, `tmp/266-eval.sh`) is not part of
the commit; the controller is kept in `tmp/266-CorpusHarnessController.java` for the next tuning.

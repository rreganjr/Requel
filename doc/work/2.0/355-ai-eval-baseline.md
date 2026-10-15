# #355 AI review baseline

The yardstick for epic #258: today's single `RequirementsReviewAssistant` prompt, scored against
the evaluation fixture in `scripts/ai-eval/`. #260 is behaviour-preserving and must reproduce
these numbers within run-to-run noise; #261 and #263 report their change against them.

- **Date:** 2026-10-01
- **Code:** `355-eval-fixture` on `release/2.0` @ `c8bea0fb`
- **Provider:** `cli` with the `claude` CLI (`--label cli/claude`); the CLI's default model
- **Runs:** 3 per entity, 93 reviews, about 31 minutes (about 20 s per review)
- **Raw findings:** `doc/work/2.0/355-ai-eval-baseline-raw.json` (every run's findings and
  summary). Re-score it against changed patterns with
  `python3 scripts/ai-eval/score.py --rescore doc/work/2.0/355-ai-eval-baseline-raw.json`,
  so a later run and the baseline are always scored by the same patterns.

## Per type

| Type | Entities | Runs ok/total | Hit rate | Spurious per run | Silent kept silent | Schema failures | Other failures | Timeouts | Confirmed (trap) |
|---|---|---|---|---|---|---|---|---|---|
| Goal | 14 | 42/42 | 49% (19/39) | 4.6 | 0% (0/3) | 0 | 0 | 0 | 0 |
| Story | 5 | 15/15 | 50% (6/12) | 4.1 | 0% (0/3) | 0 | 0 | 0 | 1 |
| UseCase | 3 | 9/9 | 100% (6/6) | 3.9 | 0% (0/3) | 0 | 0 | 0 | 0 |
| Scenario | 3 | 9/9 | 100% (6/6) | 5.1 | 0% (0/3) | 0 | 0 | 0 | 0 |
| Step | 3 | 9/9 | 100% (6/6) | 3.9 | 0% (0/3) | 0 | 0 | 0 | 0 |
| Actor | 3 | 9/9 | 100% (6/6) | 2.3 | 0% (0/3) | 0 | 0 | 0 | 0 |
| GlossaryTerm | 0 (3 skipped) | 0/0 | - | - | - | 0 | 0 | 0 | 0 |
| **All** | 31 (3 skipped) | 93/93 | 65% (49/75) | 4.2 | 0% (0/18) | 0 | 0 | 0 | 1 |


## Per entity

| Type | Entity | Expects | Hits | Spurious (3 runs) |
|---|---|---|---|---|
| Goal | Members love the library | `goal-unmeasurable` | 3/3 | 9 |
| Goal | Send overdue reminders by text message | `goal-solution-not-outcome` | 0/3 | 17 |
| Goal | Fewer complaints about reminders | `goal-proxy-for-outcome` | 1/3 | 12 |
| Goal | Dana approves tool purchases | `goal-person-not-role` | 3/3 | 13 |
| Goal | Members find tools quickly | `goal-duplicate` | 0/3 | 15 |
| Goal | Members search the catalogue by name | `goal-duplicate` | 0/3 | 13 |
| Goal | Suspended members cannot borrow | `goal-negative-only` | 0/3 | 14 |
| Goal | Online reservations and a mobile app | `goal-conjunctive` | 3/3 | 14 |
| Goal | Member data stays private | `goal-constraint-not-goal` | 0/3 | 19 |
| Goal | Volunteers plan repairs weekly | `goal-not-a-system-goal` | 3/3 | 9 |
| Goal | Barcode scanning at checkout | `goal-implicit-invariant` | 3/3 | 10 |
| Goal | Tools come back on time | `goal-measure-without-baseline` | 3/3 | 11 |
| Goal | Condition alerts are acknowledged | `goal-vacuous-quantifier` | 0/3 | 20 |
| Goal | Members reserve tools up to 7 days ahead | *silent* | - | 18 |
| Story | Borrow a drill | `story-missing-benefit` | 3/3 | 9 |
| Story | Reserve and pay online | `story-conjunctive` | 3/3 | 15 |
| Story | Late returns | `story-is-a-scenario` | 0/3 | 14 |
| Story | Halve the repair backlog | `trap-unsourced` | 0/3, confirmed 1/3 | 16 |
| Story | Return a tool after hours | *silent* | - | 8 |
| UseCase | Check out a tool | `usecase-no-precondition-or-error` | 3/3 | 12 |
| UseCase | Approve a purchase | `usecase-person-not-role` | 3/3 | 8 |
| UseCase | Renew a loan | *silent* | - | 15 |
| Scenario | Check out at the desk | `scenario-no-failure-branch` | 3/3 | 14 |
| Scenario | Approve a purchase request | `scenario-step-order` | 3/3 | 9 |
| Scenario | Renew from the account page | *silent* | - | 23 |
| Step | The system or staff mark the tool as broken | `step-ambiguous-actor` | 3/3 | 9 |
| Step | Staff photograph the damage and email the member | `step-two-actions` | 3/3 | 13 |
| Step | Staff put the tool on the repair shelf | *silent* | - | 13 |
| Actor | Dana Whitfield | `actor-person-not-role` | 3/3 | 5 |
| Actor | Non-staff | `actor-defined-by-exclusion` | 3/3 | 8 |
| Actor | Member | *silent* | - | 8 |

## What the baseline shows

1. **Reliable on the obvious categories.** Unmeasurable aspiration, named person instead of a
   role, conjunctive goal and story, not-a-system goal, implicit invariant, measure without a
   baseline, missing benefit clause, missing precondition or error path, impossible step order,
   ambiguous step actor, two actions in one step, and an actor defined by exclusion were each
   raised in 3 of 3 runs. Proxy-for-outcome was raised once in 3.
2. **Blind to the categories the prompt never names.** Solution-not-outcome, negative-only
   specification, constraint-not-goal, the vacuous quantifier and story-is-really-a-scenario were
   raised in 0 of 3 runs. These are #263's vocabulary; the generic prompt reviews for
   completeness and ambiguity, so it never looks for them.
3. **Duplicates need context it doesn't have.** The duplicate goal pair scored 0/6: the context
   pack carries no sibling goals, so the model cannot see the duplicate. That is #261's job.
4. **It never stays quiet.** 0 of 18 runs on silent entities raised nothing, and every entity
   averaged about 4 findings a run. Many of those are fair observations (undefined terms, missing
   error cases) rather than errors, but a reviewer that always finds 4 to 7 things buries the
   finding that matters. Spurious per run is the number for #263 to bring down.
5. **The trap caught it.** No run asked where the figures came from. One run's summary said
   "its arithmetic is consistent (24 + 8 = 32, half = 16)", which is exactly the
   consistency-as-correctness failure the trap exists to punish. Every run did spot a real
   problem nearby (32 tools *logged* is intake, not a backlog), which shows the model reads the
   numbers closely but checks them only against each other, never against a source.
6. **The output contract holds.** Zero schema failures and zero other failures across 93 runs.

## Pattern tuning before recording

The first scoring of this run read 72% overall. Reading the matches showed false hits: the trap's
`record` and `measured` matched findings about triage and intake counts, and the negative-only
goal's `eligib` and `who can` matched findings about suspension rules. The patterns for those
items and a few others (`role`, `defin`, `already`, `team`, `who`) were tightened and the same
findings re-scored, giving the 65% above. No review was re-run. Later tuning should do the same:
change `expectations.json`, re-score this raw file, and compare like with like.

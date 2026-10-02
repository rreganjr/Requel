# #263 AI review: the per-type definitions, final run

The seven per-type review definitions tuned in #263 (`doc/work/2.0/263-tuning-log.md`), scored
against the evaluation fixture in `scripts/ai-eval/`. The comparison is the #355 baseline
(`355-ai-eval-baseline.md`), re-scored with the same expectations and rules.

- **Date:** 2026-10-02
- **Code:** `263-review-definitions` on `release/2.0` @ `e5309c76`, definitions loaded from
  `requel.ai.definitions.dir` (identical to the shipped ones)
- **Definitions:** `ai-review-goal`, `-story`, `-usecase`, `-scenario`, `-step`, `-actor`,
  `-glossary-term`, all version 1
- **Provider:** `cli` with the `claude` CLI (`--label "cli/claude #263 final"`); the CLI's default
  model
- **Runs:** 3 per entity, 102 reviews, about 28 minutes (about 16 s per review)
- **Raw findings:** `doc/work/2.0/263-ai-eval-final-raw.json`. Re-score with
  `python3 scripts/ai-eval/score.py --rescore doc/work/2.0/263-ai-eval-final-raw.json`.

## Against the baseline

| | Baseline (#355, re-scored) | #263 final |
|---|---|---|
| Hit rate | 65% (49/75) | 100% (81/81) |
| Spurious per run | 4.0 | 0.2 |
| Silent kept silent | 0% (0/18) | 100% (21/21) |
| Trap confirmed | 1 of 3 | 0 of 3 |
| Schema failures | 0 | 0 |
| Unverified evidence | 0 (entity text only) | 4 (entity and context) |
| GlossaryTerm | not reviewable | 6/6, silent 3/3 |

The fixture changed in #263 (a Staff actor; clean silent cases for Goal, UseCase, Scenario and
GlossaryTerm), so the silent and spurious figures are not a like-for-like comparison: the baseline
also failed on the old fixture's own flaws.

## Per type

| Type | Entities | Runs | Hit rate | Spurious per run | Silent kept silent | Unverified evidence | Suggestions per run | Also valid per run |
|---|---|---|---|---|---|---|---|---|
| Goal | 14 | 42 | 100% (39/39) | 0.4 | 100% (3/3) | 1 | 0.2 | 0.3 |
| Story | 5 | 15 | 100% (12/12) | 0.3 | 100% (3/3) | 0 | 0.5 | 0.2 |
| UseCase | 3 | 9 | 100% (6/6) | 0.1 | 100% (3/3) | 3 | 0.6 | 2.2 |
| Scenario | 3 | 9 | 100% (6/6) | 0.1 | 100% (3/3) | 0 | 0.0 | 0.7 |
| Step | 3 | 9 | 100% (6/6) | 0.0 | 100% (3/3) | 0 | 0.0 | 0.0 |
| Actor | 3 | 9 | 100% (6/6) | 0.1 | 100% (3/3) | 0 | 0.3 | 0.3 |
| GlossaryTerm | 3 | 9 | 100% (6/6) | 0.0 | 100% (3/3) | 0 | 0.0 | 0.0 |
| **All** | 34 | 102 | 100% (81/81) | 0.2 | 100% (21/21) | 4 | 0.2 | 0.5 |

## Per entity

| Type | Entity | Expects | Hits | Spurious (3 runs) | Also valid | Suggestions |
|---|---|---|---|---|---|---|
| Goal | Members love the library | `goal-unmeasurable` | 3/3 | 3 | 0 | 0 |
| Goal | Send overdue reminders by text message | `goal-solution-not-outcome` | 3/3 | 0 | 3 | 0 |
| Goal | Fewer complaints about reminders | `goal-proxy-for-outcome` | 3/3 | 0 | 0 | 0 |
| Goal | Dana approves tool purchases | `goal-person-not-role` | 3/3 | 3 | 0 | 0 |
| Goal | Members find tools quickly | `goal-duplicate` | 3/3 | 3 | 0 | 0 |
| Goal | Members search the catalogue by name | `goal-duplicate` | 3/3 | 0 | 0 | 1 |
| Goal | Suspended members cannot borrow | `goal-negative-only` | 3/3 | 0 | 3 | 3 |
| Goal | Online reservations and a mobile app | `goal-conjunctive` | 3/3 | 0 | 3 | 0 |
| Goal | Member data stays private | `goal-constraint-not-goal` | 3/3 | 3 | 2 | 3 |
| Goal | Volunteers plan repairs weekly | `goal-not-a-system-goal` | 3/3 | 0 | 0 | 0 |
| Goal | Barcode scanning at checkout | `goal-implicit-invariant` | 3/3 | 0 | 3 | 0 |
| Goal | Tools come back on time | `goal-measure-without-baseline` | 3/3 | 2 | 0 | 0 |
| Goal | Condition alerts are acknowledged | `goal-vacuous-quantifier` | 3/3 | 3 | 0 | 0 |
| Goal | Members see what they have on loan | *silent* | - | 0 | 0 | 1 |
| Story | Borrow a drill | `story-missing-benefit` | 3/3 | 0 | 0 | 0 |
| Story | Reserve and pay online | `story-conjunctive` | 3/3 | 0 | 0 | 0 |
| Story | Late returns | `story-is-a-scenario` | 3/3 | 2 | 0 | 2 |
| Story | Halve the repair backlog | `trap-unsourced` | 3/3, confirmed 0/3 | 3 | 3 | 4 |
| Story | Return a tool after hours | *silent* | - | 0 | 0 | 2 |
| UseCase | Check out a tool | `usecase-no-precondition-or-error` | 3/3 | 0 | 8 | 1 |
| UseCase | Approve a purchase | `usecase-person-not-role` | 3/3 | 1 | 12 | 4 |
| UseCase | Renew a loan | *silent* | - | 0 | 0 | 0 |
| Scenario | Check out at the desk | `scenario-no-failure-branch` | 3/3 | 1 | 0 | 0 |
| Scenario | Approve a purchase request | `scenario-step-order` | 3/3 | 0 | 6 | 0 |
| Scenario | Renew from the account page | *silent* | - | 0 | 0 | 0 |
| Step | The system or staff mark the tool as broken | `step-ambiguous-actor` | 3/3 | 0 | 0 | 0 |
| Step | Staff photograph the damage and email the member | `step-two-actions` | 3/3 | 0 | 0 | 0 |
| Step | Staff put the tool on the repair shelf | *silent* | - | 0 | 0 | 0 |
| Actor | Dana Whitfield | `actor-person-not-role` | 3/3 | 0 | 3 | 0 |
| Actor | Non-staff | `actor-defined-by-exclusion` | 3/3 | 1 | 0 | 0 |
| Actor | Member | *silent* | - | 0 | 0 | 3 |
| GlossaryTerm | Loan | `term-circular` | 3/3 | 0 | 0 | 0 |
| GlossaryTerm | Overdue tool | `term-circular` | 3/3 | 0 | 0 | 0 |
| GlossaryTerm | Pickup date | *silent* | - | 0 | 0 | 0 |

## What it shows

1. **Every targeted flaw, every run.** The categories the generic prompt never raised
   (solution-not-outcome, negative-only, constraint-not-goal, vacuous quantifier,
   story-is-a-scenario) are now 3 of 3; the duplicate goal pair is 6 of 6 with `goal-siblings`.
2. **It stays quiet on clean entities.** All 21 silent runs raised nothing but advisory
   suggestions.
3. **What is left is mostly defensible.** The 25 spurious findings across 102 runs are readings a
   reviewer could argue for: "the tool library" as software or service, "find" as catalogue entry
   or shelf, a standing approval rule read as a constraint, acknowledgement as a proxy. Goals carry
   most of them.
4. **The trap is caught without vouching for the figures.** Runs say the figures agree but have no
   source. The score's confirm rule was too blunt for that and was fixed (a text that also
   questions the source doesn't confirm).
5. **Extraction suggestions are sparing and grounded.** 0.2 per run, and with `project-names` they
   name what the project lacks (a Staff actor before the fixture had one, a repair-volunteer role,
   undefined terms such as "fine" or "triage").

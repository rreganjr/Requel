# #365 AI review on local models: results

https://github.com/rreganjr/Requel/issues/365

The per-type review definitions (#263) and the terminology policy (#265), scored on two local
Ollama models against the eval fixture in `scripts/ai-eval/`. The comparison is the `cli` result
from #265 (`doc/work/2.0/265-ai-eval-final.md`), re-scored with the current expectations.

- **Date:** 2026-11-03 to 2026-11-04
- **Code:** `365-local-model-eval` on `release/2.1` @ `7a2a8d6b`
- **Definitions:** the bundled ones, version 2 (#337 added one sentence about existing issues'
  severity. The fixture has no human issues, so the sentence changes nothing here; the `cli` run
  used version 1).
- **Hardware:** MacBook Pro, M3 Pro, 36 GB. Ollama 0.40.14 (mlx runner),
  `OLLAMA_CONTEXT_LENGTH=32768`.
- **Provider:** the `ai-ollama` profile, through Ollama's OpenAI-compatible endpoint, with the
  output schema embedded in the prompt (not enforced). `scripts/ai-eval/capture-proxy.py` was
  placed between Requel and Ollama and recorded every exchange.
- **Runs:**

  | Label | Model | Thinking | Runs per entity | Wall time |
  |---|---|---|---|---|
  | qwen off | `qwen3.6:27b` | off (`reasoning-effort=none`) | 3 | 2 h 25 min |
  | gemma off | `gemma4:26b` | off (`reasoning-effort=none`) | 3 | 34 min |
  | qwen on | `qwen3.6:27b` | on (the model's default) | 1 | 2 h 43 min |

- **Raw findings:** `365-local-model-eval-qwen3.6-nothink-raw.json`,
  `365-local-model-eval-gemma4-nothink-raw.json` and `365-local-model-eval-qwen3.6-think-raw.json`.
  To re-score one: `python3 scripts/ai-eval/score.py --rescore <file>`.

## Summary

- **`gemma4:26b` with thinking off is the local model to use.**
  - It found 80% of the planted flaws; the `cli` model finds 100%.
  - Every reply parsed as JSON.
  - The policy pass matched the `cli` result exactly.
  - A review took about 14 s, about the same as the `cli` model.
- **What a local model misses:** four goal flaws that need a second step of reasoning, missed in
  every run of both models:
  - a duplicate of a sibling goal;
  - a rule that only says who may *not* do something;
  - an assumption stated as fact;
  - an "every alert" goal that an empty set satisfies.

  The structural flaws (person instead of role, two actions in one step, circular definitions,
  missing benefit) were found in every run.
- **`qwen3.6:27b` is the weaker choice.**
  - It is about 5 times slower than gemma.
  - It raises more findings that aren't real, in the review and in the policy pass.
  - It returns an empty `{}` on some reviews that have nothing to report, which fails them as
    schema failures (7 of 234 calls).
- **Thinking on qwen** removes almost all of the findings that aren't real. It finds no more of the
  planted flaws, and makes each review about 17 times slower. The thinking counts against the
  4,000-token output cap, and 3 of 39 reviews ran out of tokens before answering.
- **No definition was changed.** The four goal misses are not instructions the model misread (see
  Notes).

## Against the `cli` result

Like for like: the 35 entities the `cli` run scored. The fixture has 4 more, which are covered in
the next section.

| | `cli` (#265) | gemma off | qwen off | qwen on (1 run) |
|---|---|---|---|---|
| Review runs ok | 105/105 | 105/105 | 102/105 | 32/35 |
| Hit rate | 100% (81/81) | 80% (65/81) | 84% (68/81) | 79% (19/24) |
| Spurious per run | 0.2 | 0.5 | 0.7 | 0.1 |
| Silent kept silent | 96% (23/24) | 67% (16/24) | 67% (14/21) | 100% (8/8) |
| Schema failures | 0 | 0 | 3 | 0 |
| Other failures | 0 | 0 | 0 | 3 (output cap) |
| Policy hit rate | 100% (3/3) | 100% (3/3) | 100% (3/3) | 100% (1/1) |
| Policy spurious per run | 0.0 | 0.0 | 0.4 | 0.0 |
| Policy failures | 0 | 0 | 2 | 0 |
| Median review time | about 16 s | 14 s | 68 s | 239 s |

Review time is the time from request to finished run. It includes the policy call queued beside
the review: Ollama serves one request at a time here.

## All 39 entities, per type

These are the reports' own tables. The 4 extra entities came from #266's corpus work: "Two-week
loans", "Unpaid fines put accounts on hold", "Keep a tool for three weeks" and "Place a hold on a
tool". All four are silent for the per-entity review. The `cli` column covers its 35 entities.

| Type | `cli` hit / spurious / silent | gemma off | qwen off | qwen on |
|---|---|---|---|---|
| Goal | 100% / 0.4 / 3/3 | 59% / 0.6 / 4/9 | 67% / 1.2 / 3/9 | 64% / 0.2 / 2/3 |
| Story | 100% / 0.2 / 5/6 | 100% / 0.2 / 12/12 | 100% / 0.4 / 8/9 | 100% / 0.0 / 4/4 |
| UseCase | 100% / 0.1 / 3/3 | 100% / 0.7 / 0/3 | 100% / 0.5 / 0/2 | 100% / 0.0 / 1/1 |
| Scenario | 100% / 0.3 / 3/3 | 100% / 1.2 / 0/3 | 100% / 0.8 / 0/3 | 50% / 0.0 / 1/1 |
| Step | 100% / 0.0 / 3/3 | 100% / 0.0 / 3/3 | 100% / 0.6 / 1/3 | 100% / 0.0 / 1/1 |
| Actor | 100% / 0.1 / 3/3 | 100% / 0.3 / 1/3 | 100% / 0.2 / 3/3 | 100% / 0.0 / 1/1 |
| GlossaryTerm | 100% / 0.0 / 3/3 | 100% / 0.0 / 3/3 | 100% / 0.0 / 3/3 | 100% / 0.0 / 1/1 |
| **All** | 100% / 0.2 / 23/24 | 80% / 0.5 / 23/36 | 84% / 0.7 / 18/32 | 79% / 0.1 / 11/12 |

## Per entity

Each cell is hits / spurious. The `cli`, gemma and qwen-off columns are over 3 runs; qwen on is
over 1 run. An X means a failed run.

| Type | Entity | Expects | `cli` | gemma off | qwen off | qwen on |
|---|---|---|---|---|---|---|
| Goal | Members love the library | `goal-unmeasurable` | 3/1 | 3/0 | 3/0 | 1/0 |
| Goal | Send overdue reminders by text message | `goal-solution-not-outcome` | 3/1 | 3/0 | 3/2 | 1/0 |
| Goal | Fewer complaints about reminders | `goal-proxy-for-outcome` | 3/1 | 2/2 | 3/3 | 1/0 |
| Goal | Dana approves tool purchases | `goal-person-not-role` | 3/3 | 3/0 | 3/4 | X |
| Goal | Members find tools quickly | `goal-duplicate` | 3/3 | **0**/3 | **0**/4 | **0**/0 |
| Goal | Members search the catalogue by name | `goal-duplicate` | 3/0 | **0**/3 | 2/5 | **0**/1 |
| Goal | Suspended members cannot borrow | `goal-negative-only` | 3/1 | **0**/3 | 1/5 | X |
| Goal | Online reservations and a mobile app | `goal-conjunctive` | 3/0 | 3/0 | 3/3 | 1/0 |
| Goal | Member data stays private | `goal-constraint-not-goal` | 3/4 | 3/3 | 2/3 | 1/0 |
| Goal | Volunteers plan repairs weekly | `goal-not-a-system-goal` | 3/0 | 3/3 | 3/4 | 1/0 |
| Goal | Barcode scanning at checkout | `goal-implicit-invariant` | 3/0 | **0**/0 | **0**/6 | **0**/0 |
| Goal | Tools come back on time | `goal-measure-without-baseline` | 3/0 | 3/3 | 3/3 | 1/0 |
| Goal | Condition alerts are acknowledged | `goal-vacuous-quantifier` | 3/3 | **0**/3 | **0**/5 | **0**/1 |
| Goal | Members see what they have on loan | *silent* | 0 | 0 | 0 | 0 |
| Goal | Two-week loans | *silent* | - | 3 | 7 | 1 |
| Goal | Unpaid fines put accounts on hold | *silent* | - | 2 | 3 | 0 |
| Story | Borrow a drill | `story-missing-benefit` | 3/0 | 3/0 | 3/0 | 1/0 |
| Story | Reserve and pay online | `story-conjunctive` | 3/0 | 3/0 | 3/0 | 1/0 |
| Story | Late returns | `story-is-a-scenario` | 3/1 | 3/6 | 3/4 | 1/0 |
| Story | Halve the repair backlog | `trap-unsourced` | 3/2 | 3/0 | 3/3 | X |
| Story | Return a tool after hours | *silent* | 1 | 0 | 0, X X | 0 |
| Story | Renew as a patron | *silent* | 0 | 0 | 0 | 0 |
| Story | Keep a tool for three weeks | *silent* | - | 0 | 1 | 0 |
| Story | Place a hold on a tool | *silent* | - | 0 | 0, X | 0 |
| UseCase | Check out a tool | `usecase-no-precondition-or-error` | 3/0 | 3/0 | 3/0 | 1/0 |
| UseCase | Approve a purchase | `usecase-person-not-role` | 3/1 | 3/1 | 3/0 | 1/0 |
| UseCase | Renew a loan | *silent* | 0 | 5 | 4, X | 0 |
| Scenario | Check out at the desk | `scenario-no-failure-branch` | 3/3 | 3/3 | 3/2 | **0**/0 |
| Scenario | Approve a purchase request | `scenario-step-order` | 3/0 | 3/2 | 3/1 | 1/0 |
| Scenario | Renew from the account page | *silent* | 0 | 6 | 4 | 0 |
| Step | The system or staff mark the tool as broken | `step-ambiguous-actor` | 3/0 | 3/0 | 3/1 | 1/0 |
| Step | Staff photograph the damage and email the member | `step-two-actions` | 3/0 | 3/0 | 3/0 | 1/0 |
| Step | Staff put the tool on the repair shelf | *silent* | 0 | 0 | 4 | 0 |
| Actor | Dana Whitfield | `actor-person-not-role` | 3/0 | 3/0 | 3/0 | 1/0 |
| Actor | Non-staff | `actor-defined-by-exclusion` | 3/1 | 3/0 | 3/2 | 1/0 |
| Actor | Member | *silent* | 0 | 3 | 0 | 0 |
| GlossaryTerm | Loan | `term-circular` | 3/0 | 3/0 | 3/0 | 1/0 |
| GlossaryTerm | Overdue tool | `term-circular` | 3/0 | 3/0 | 3/0 | 1/0 |
| GlossaryTerm | Pickup date | *silent* | 0 | 0 | 0 | 0 |

## What it shows

1. **The definitions hold up on a local model.**
   - Every finding type used was in the definition's vocabulary.
   - Evidence was nearly always taken from the entity (2 unverified quotes in gemma's 117 runs,
     4 in qwen's 113).
   - The trap was never confirmed.
   - On the story, use case, step, actor and glossary flaws, both models hit 100%.
2. **Goals are where the gap is.** gemma's goal hit rate is 59%, against 100% for the `cli`
   model. The misses are listed under Notes.
3. **Most of gemma's extra findings are defensible.**
   - On the silent "Renew a loan" and "Renew from the account page" it asks for a failure branch
     (the tool reserved between page load and renewal) and a confirm step.
   - It reads "Two-week loans" and "Unpaid fines put accounts on hold" as business rules rather
     than goals.
   - It notices that the actor Member overlaps the glossary's Patron, which #265's policy settles
     as an alternate name.

   A reviewer could argue for any of these. The fixture counts them as not real because the `cli`
   model held back.
4. **qwen's policy pass is noisy.** 50 `NON_CANONICAL_TERM` findings in 114 policy runs, almost
   all naming "member" or "Members" as a non-canonical form of "Member". gemma raised that type
   only on "Renew as a patron", where it is expected, as the `cli` model does.
5. **Thinking makes qwen precise but not more insightful.**
   - With thinking on, spurious findings fall from 0.7 to 0.1 per run, and the policy pass is
     clean.
   - It finds the same flaws and misses the same four goals. It also missed the scenario
     failure-branch once.
   - Its 3 failures were replies that ran out of tokens while thinking (see Schema failures).

## Schema failures, explained

The capture proxy recorded every reply (`json=` on each line of `proxy-*.log`).

- **gemma off:** none. All 234 replies were JSON with `finish=stop`.
- **qwen off:** all 234 replies were valid JSON. Seven were an empty `{}`: 4 reviews ("Return a
  tool after hours" twice, "Renew a loan", "Place a hold on a tool") and 3 policy calls. Requel
  needs a `summary`, so each was counted as a schema failure ("structured output missing
  summary").
  - All seven were on entities with nothing to report. The empty object is qwen's way of saying
    "no findings".
  - Enforcing the schema (decision 3) would probably fix it. It wasn't tried, because gemma is
    the better model on every other count.
- **qwen on:** three replies stopped at `finish=length`: the 4,000 output tokens
  (`REQUEL_AI_MAX_OUTPUT_TOKENS`) were all spent thinking. The answers were empty (2) or a cut-off
  fragment (1), and Spring AI failed the request.
  - Raising the cap would avoid this, but each of those calls already took 300–340 s.

## Notes: instructions a smaller model can't follow

The issue asks which instructions needed changing. None were changed.

The four goal flaws both models miss are the ones where the evidence is something the goal
doesn't say:

| Expectation | The `cli` model's reading | What gemma and qwen raised instead |
|---|---|---|
| `goal-duplicate` | "Members search the catalogue by name" is one way to "find any tool quickly"; the sibling goal from `goal-siblings` is the evidence | a missing baseline, or a proxy measure |
| `goal-negative-only` | the goal lists who is barred and never who may borrow | a constraint or business rule; a solution, not an outcome |
| `goal-implicit-invariant` | "every tool already carries exactly one barcode" is assumed, and no goal protects it | a solution, not an outcome (barcodes); qwen also called the barcode sentence a constraint |
| `goal-vacuous-quantifier` | "all alerts acknowledged" holds when the monitor raises none | a solution, not an outcome; a proxy measure; qwen also used `NEGATIVE_ONLY_SPECIFICATION` for an unrelated reading |

- The definitions already name and explain each of these types. The models used the types
  correctly elsewhere: qwen on "Suspended members cannot borrow" in 1 of 3 runs, and on
  "Members search the catalogue by name" in 2 of 3.
- Thinking on qwen didn't recover any of the four, so the misses aren't for lack of
  instructions to reason.
- Making those instructions shorter or blunter would risk the `cli` result that #263 tuned them
  for.

Two behaviours belong in the setup guide rather than the definitions:

- qwen's `{}` for "nothing to report";
- thinking spending the output cap.

`doc/guides/AI_ASSISTANT_SETUP.md` section 4c now covers both.

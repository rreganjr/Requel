# #265 AI review: the terminology policy, final run

The composed policy pass (#265) beside the per-type reviews (#263), scored against the evaluation
fixture in `scripts/ai-eval/`.

- **Date:** 2026-10-03
- **Code:** `265-policy-definitions` on `release/2.0` @ `9b1066a7`, definitions loaded from
  `requel.ai.definitions.dir` (identical to the shipped ones)
- **Policy:** `ai-policy-terminology@1`, the only one, so each pass composed one policy
- **Provider:** `cli` with the `claude` CLI (`--label "cli/claude #265 final"`)
- **Runs:** 3 per entity, 105 reviews and 105 policy runs
- **Raw findings:** `doc/work/2.0/265-ai-eval-final-raw.json`. Re-score with
  `python3 scripts/ai-eval/score.py --rescore doc/work/2.0/265-ai-eval-final-raw.json`.

## Policy pass

| Type | Runs | Hit rate | Spurious per run | Silent kept silent |
|---|---|---|---|---|
| Goal | 42 | - | 0.0 | 100% (42/42) |
| Story | 18 | 100% (3/3) | 0.0 | 100% (15/15) |
| UseCase | 9 | - | 0.0 | 100% (9/9) |
| Scenario | 9 | - | 0.0 | 100% (9/9) |
| Step | 9 | - | 0.0 | 100% (9/9) |
| Actor | 9 | - | 0.0 | 100% (9/9) |
| GlossaryTerm | 9 | - | 0.0 | 100% (9/9) |
| **All** | 105 | 100% (3/3) | 0.0 | 100% (102/102) |

No failures; every review request dispatched exactly one policy run.

- **"Renew as a patron"**: `NON_CANONICAL_TERM` in 3 of 3 runs, naming "Member" as the term to use.
- **"Suspended members cannot borrow"**: one `CONFLICTING_USAGE` in 3 runs. The glossary defines a
  Member as a person "who may borrow tools", and the goal says some members may not. That is a
  real conflict in the fixture, so it is listed as `policyAlsoValid` and isn't counted as
  spurious.
- Nothing on the other 33 entities in any run.

## Reviews beside it

The review numbers are #263's, with the new story included. Hit rate is 100% (81/81), spurious
findings 0.2 per run, and silent cases kept silent 96% (23/24). The miss is "Return a tool after
hours", which drew one `AMBIGUOUS` finding (when a drop-box return ends a loan). The review
definitions are unchanged in #265.

The #355 baseline re-scores at 65%, 4.0 spurious per run, 0/18 silent; the baseline has no
policy runs.

## What it shows

1. **One composed call costs nothing in precision here.** With the glossary in context, the policy
   flags only alternate names, and only where they are used.
2. **It catches what the per-type review can't.** The review of "Renew as a patron" thought Patron
   and Member were separate terms, because `project-names` gives names without the alternate
   relation. The policy reads `project-glossary` and named the canonical term.
3. **What one policy can't show.** Dilution across several composed policies, and the ceiling under
   load, can't be measured with a single bundled policy. #264's authoring needs this eval when
   projects add their own.

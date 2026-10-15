# #265 Tuning log: the terminology policy

Each round scores the #355 fixture with the `cli` provider (claude). Each review request now also
runs the policy pass. The fixture gained the glossary term "Member", its alternate "Patron", and the
story "Renew as a patron", which is the policy's one expected finding; every other entity is
policy-silent.

## Round 1: 1 run per entity

`target/ai-eval/20261003-114302`, `ai-policy-terminology@1`.

| | Policy pass |
|---|---|
| Hit rate | 100% (1/1) |
| Spurious per run | 0.0 |
| Silent kept silent | 100% (34/34) |
| Failures | 0 |

- The policy raised `NON_CANONICAL_TERM` on "Renew as a patron" and nothing anywhere else.
- Every entity got exactly one policy run.
- The finding named the canonical term and quoted both uses of "patron".
- The reviews scored as in #263's final: 100% hit rate, 0.3 spurious per run.
- Story silent cases came out 1/2. "Return a tool after hours" got an `AMBIGUOUS` finding about
  when a drop-box return ends the loan. That reading is defensible, and the review definitions
  didn't change in this ticket.
- The review of "Renew as a patron" said the project treats Patron and Member as separate terms.
  `project-names` gives the review names without the alternate relation, so it can't know they
  are one term. The finding is counted as also valid; the policy, which reads `project-glossary`,
  got it right.

No change to the policy. Next: the final run, 3 per entity.

## Final: 3 runs per entity

`target/ai-eval/20261003-121227`, 105 reviews and 105 policy runs, same policy as round 1.
Results are in `265-ai-eval-final.md`; the raw findings are checked in as
`265-ai-eval-final-raw.json`.

- Policy: 100% hit rate (3/3), 0.0 spurious per run, 102/102 silent runs kept silent, no failures.
- The first scoring counted one `CONFLICTING_USAGE` on "Suspended members cannot borrow" as
  spurious. It is right: the fixture's glossary defines a Member as a person "who may borrow
  tools", and the goal says some members may not. `score.py` gained `policyAlsoValid` (the policy
  counterpart of `alsoValid`), and the entity lists that conflict.
- Reviews: 100% hit rate (81/81), 0.2 spurious per run, 23/24 silent. The miss is the drop-box
  story's `AMBIGUOUS`, as in round 1.

The shipped policy is the tuned one (`tmp/265-defs` and `ai/definitions/` are identical), at
version 1.

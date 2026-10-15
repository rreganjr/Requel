# #263 What varies by type, and what doesn't

Written input to #265 (cross-cutting policy definitions) and #266 (corpus analyses), from tuning
the seven per-type review definitions (`263-tuning-log.md`, final scores in
`263-ai-eval-final.md`).

## Summary

- **Most of a definition is the same for every type.** The rules block, the output schema, the
  evidence and extraction rules, `AMBIGUOUS` and `OTHER` are shared word for word. What varies is
  one paragraph saying what a good entity of that type looks like, its quality vocabulary, and its
  context providers.
- **Several finding types recur across types.** These are the natural first policies for #265:
  person-not-role (4 types), extraction of actors and glossary terms (6 types), unsourced figures.
- **The findings that needed the most tuning were relationships.** They take two entities to see,
  and a per-entity review either raises them from both sides or needs a rule to pick one. That is
  #266's case.

## Shared by all seven

| Part | Notes |
|---|---|
| Output schema | `RequirementsReviewOutput` v2 (v1 plus `suggestedEntityName`) |
| Rules block | don't restate existing annotations; an empty result is a good result; one finding per problem under the most specific type, gaps of one type together; `AMBIGUOUS` only for two readings that lead to different systems; evidence quoted from the target or a context section |
| Extraction rule | six types (all but GlossaryTerm): use an extraction type only for text that is another kind of entity; suggest an actor or term only when `project-names` doesn't list it |
| `{{vocabulary}}` | rendered by the executor: quality types, then extraction types marked advisory |
| `OTHER` | all seven; `AMBIGUOUS` in all but GlossaryTerm |
| `project-names` | last provider in every definition with extraction types, so it is the first dropped over budget |

## Per type

| Type | Context providers | Quality types | Extraction types | What tuning changed |
|---|---|---|---|---|
| Goal | goal-relations, goal-siblings, goal-stakeholders, project-names | 15 | scenario, story, actor, glossary term | `AMBIGUOUS` narrowed (it fired on 10 of 14 goals); siblings make duplicates and conflicts visible |
| Story | story-actors, project-names | 8 | scenario, actor, glossary term | stopped reviewing linked actors; any "so that" reason is a benefit; unsourced figures raised even when they agree |
| UseCase | usecase-scenarios, project-names | 8 | actor, glossary term | a stated precondition needs no error branch; evidence from the scenario counts |
| Scenario | scenario-usecases, project-names | 7 | actor, glossary term | same precondition rule; evidence from the steps counts |
| Step | step-sequence, project-names | 6 | scenario, glossary term | none needed |
| Actor | actor-references, project-names | 7 | glossary term | overlap raised on the vaguer actor; references include primary-actor links |
| GlossaryTerm | glossary-related | 6 | none | none needed |

Goal is the heaviest: the most providers, the largest vocabulary, and most of the remaining
spurious findings (0.4 per run against 0.0 to 0.3 elsewhere).

## Finding types that recur (input to #265)

| Type | Defined in | As a policy |
|---|---|---|
| `PERSON_NAMED_NOT_ROLE` | Goal, Story, UseCase, Actor | one rule ("names a person where a role is meant") over four types; the strongest candidate |
| `EXTRACT_ACTOR`, `EXTRACT_GLOSSARY_TERM` | 5 and 6 types | already shared text; needs `project-names` wherever it runs |
| `UNSOURCED_CLAIM` | Story only | the trap is a story, but figures stated as fact can appear in goals and use cases too |
| `INCONSISTENT` | Goal, UseCase, Scenario, Step | means something different in each type (sibling conflict, text versus scenario, step order); not a policy, see below |
| `UNCLEAR_ACTOR` | Story, UseCase, Scenario | type-specific wording; keep per type |
| `MISSING_ERROR_CASE` | UseCase, Scenario | the same flaw seen from both sides of one relationship |

A composed policy pass (#265) would remove the duplicated person-not-role and extraction text from
four to six definitions, and let unsourced figures be checked on every type. Rule wording matters:
each per-type description was tuned against that type's false positives, so a shared rule needs
the narrowest wording that worked anywhere.

## Relationships seen from one side (input to #266)

Each of these was raised on one entity about another, often from both sides:

1. **Goal conflict.** "Send overdue reminders by text message" against "Member data stays private"
   (phone numbers go to a carrier): raised in 5 of 6 runs across the two goals (3 on one, 2 on
   the other), so one conflict becomes two issues.
2. **Duplicate goals.** "Members find tools quickly" and "Members search the catalogue by name":
   raised on each, 6 of 6.
3. **Overlapping actors.** "Non-staff" overlaps "Member". Without a rule it was raised on the clean
   actor; with a rule to raise it on the vaguer one it left Member (and in the final run wasn't
   raised at all). The rule is a heuristic standing in for "this is about the pair".
4. **Use case versus scenario.** Member versus Staff as the actor of checkout, shipping beyond the
   use case's outcome, and the purchase scenario's step order: each raised on the use case, the
   scenario, or both.
5. **Precondition versus branch.** The silent Renew use case and scenario kept asking for branches
   for conditions the use case states as preconditions, until a rule said a precondition needs
   none. The question spans both entities.

Two problems #266 would remove: duplicate issues for one relationship, and the side-picking rules
(3 and 5) that per-entity reviews need. A corpus finding with both subjects attached, raised once,
is the honest shape.

## Fixture and tooling notes for later tuning

- `alsoValid` in `expectations.json` lists real flaws the fixture doesn't target; the thin
  use cases have the most (2.2 per run).
- The trap's confirm rule ignores a text that also questions the figures' source.
- Import order of scenario steps and primary-actor references were bugs found by tuning, not by
  the build; the fixture is a useful regression check for the context the model sees.

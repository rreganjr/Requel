# #265 Cross-cutting policy definitions — implementation plan

Child 7 of epic #258. Review: 2026-10-02 against `release/2.0` @ `9b1066a7` (after #263).

## Summary

A **policy** is a definition whose rule applies across entity types. All enabled policies matching an
entity run in **one composed provider call** (`POLICY_REVIEW`), next to the per-type review
(`REQUIREMENTS_REVIEW`). Each finding names the policy that raised it, and from then on it is
handled like a finding of a definition with that key: identity, source, idempotency and cleanup.
One bundled policy is seeded: **terminology consistency** against the project glossary. It is
measured on the #355 fixture.

## Review against the tree

- `DefinitionKind.POLICY` is stored but the validator refuses it.
- The validator's collision rule (per task type, at most one fallback and one specific definition
  per entity type) fits reviews. Policies need the opposite, many per entity.
- #262's `AiProviderLocality` (LOCAL / REMOTE) and `DataHandlingGuard` already decide egress; a
  local-only policy is one more check against the same locality.
- `AiReviewService` dispatches `REQUIREMENTS_REVIEW` only, and the review read is keyed by task type.
- `AssistantRunWorker` takes one `AssistantResult` per assistant. The applicator keys identity,
  `source` (`ASSISTANT:<id>`), idempotency and cleanup on `result.assistantId()`.
- **Gap the ticket doesn't name:** a review sees only the glossary terms linked to the entity (base
  pack) and bare names (`project-names`). Terminology needs every term with its alternates and
  definition.
- The annotations UI already receives `source`. It shows `createdBy` (the assistant identity), but
  nothing that says "policy".

## Locked decisions (2026-10-02)

1. **One request runs both.** `POST /api/ai/reviews` dispatches the review and, when a policy
   applies to the entity, a `POLICY_REVIEW` run: two provider calls at most.
2. **Seed only the terminology policy.** Per-type definitions keep person-not-role and the
   extraction types. The other candidates found in #263 are left as they are.
3. **Measured on the fixture.** Policy expectations go into `expectations.json`; `score.py` scores
   policy runs.
4. **Relationship findings stay in #266.**
5. **Defaults:** the ceiling is a server property (`requel.ai.policies.max-composed`, default 8);
   the overview gets a "Policies" switch group; a policy finding shows "Policy: <name>".

## Contracts

### Definitions

- `kind POLICY` is valid only with `taskType POLICY_REVIEW`; `REVIEW` can't use `POLICY_REVIEW`.
- Empty scope means every reviewable type. The collision rule is skipped for `POLICY_REVIEW`.
- New `localOnly` (boolean, default false), carried through the record, entity, store and
  bundled JSON (V35 `assistant_definitions.local_only`). A local-only policy is left out of the
  composition when the active provider is REMOTE, and the run notes why.

### Composition

- For `POLICY_REVIEW` the registry returns one `ComposedPolicyAssistant` over the enabled matching
  policies: project switches applied, local-only filtered, in key order.
- **Over the ceiling** the run fails with "N policies apply to Goal 12; the limit is 8
  (requel.ai.policies.max-composed). No policy ran." Nothing is truncated.
- **One call.** The instructions are a shared preamble, then one block per policy (key, name,
  rule text, finding types). The context is the union of the policies' providers, in order. The
  output schema is `PolicyReviewOutput` v1: the review v2 finding plus a required `policyKey`,
  restricted at request time to the composed keys.
- **Attribution.** A new SPI, `ComposedAssistant.analyzeAll`, returns one result per policy, with
  `assistantId` set to the policy key, so the applicator is unchanged. A finding whose `policyKey`
  is not one of the composed keys is dropped and counted as a vocabulary miss. A finding of a type
  outside its policy's vocabulary is kept and counted, as for reviews.
- **Coexistence.** A policy never retires review findings and vice versa; they are different task
  types. Each policy uses `AUTO_RESOLVE_IF_UNTOUCHED` for its own findings.

### Context: `project-glossary`

Every glossary term with its alternates and definition (summary length), ranked by
`TextSimilarity` to the target and cut to the provider budget. The cut is noted in the pack.

### Bundled policy: `ai-policy-terminology`

- Scope: every reviewable type. Providers: `entity`, `project-glossary`. Enabled; a project can
  switch it off.
- Vocabulary:
  - `NON_CANONICAL_TERM`: uses an alternate name where the glossary has a canonical term.
  - `CONFLICTING_USAGE`: uses a term in a sense its definition contradicts.
  - `OTHER`.
- Prose positions only. A one-click replace is out of scope; `ReplaceGlossaryTermCommand` exists,
  but it works glossary-wide, not on one entity.

### API and UI

- `GET /api/ai/reviews?taskType=POLICY_REVIEW` reads the policy run; the default stays
  `REQUIREMENTS_REVIEW`.
- Issue and note DTOs gain `sourceName` and `sourceKind` (REVIEW / POLICY), resolved from the
  definition key in `source`.
- The annotations section tags policy findings "Policy: <name>".
- The overview has a third "Policies" group of switches.

### PII (documentation)

`AI_ASSISTANT_SETUP.md` section 11 explains:

- A PII-detection policy discloses what it looks for if it runs remotely, so it must be
  `localOnly`.
- `externalProviderAllowed=false` stops every remote call for the project. `localOnly` stops one
  policy from running remotely even when the project allows remote calls.

### Eval

- **Fixture:**
  - Glossary term "Member" ("A person with a current library membership.").
  - Alternate term "Patron", canonical "Member".
  - Story "Renew as a patron", which uses "patron". Its review expectation is silent; its policy
    expectation is `NON_CANONICAL_TERM`.
  - Existing entities are the policy's silent cases, except where they use an alternate.
- **`expectations.json`:** a `policyExpect` per entity (same item shape). Entities without one
  are policy-silent.
- **`score.py`:** after each review it also waits for the policy run, scores it against
  `policyExpect`, and adds a "Policies" table (hit rate, spurious per run, silent kept silent, one
  composed call per run). Re-scoring a raw file with no policy runs leaves that table empty.

## Tuning

- One round of 1 run per entity.
- A final run of 3 per entity.
- Log in `doc/work/2.0/265-tuning-log.md`.

## Test plan

- Validator:
  - kind and task type pairing;
  - empty scope;
  - no collision check for policies;
  - `localOnly` round-trips through store and JSON.
- Registry:
  - N matching policies give one composed assistant;
  - switches and scope respected;
  - local-only dropped when remote;
  - over the ceiling fails with the message.
- Executor (fake client):
  - exactly one provider call for N policies (the AC);
  - schema `policyKey` enum;
  - attribution per policy;
  - unknown key dropped and counted.
- IT through the worker (fake CLI):
  - a review and a policy on one entity: both sets of findings, attributed, neither retires the other;
  - a policy re-run cleans its own untouched findings;
  - `GET ?taskType=POLICY_REVIEW`;
  - DTO `sourceKind`.
- Angular:
  - annotations tag;
  - overview Policies group.
- `test_score.py`:
  - policy scoring;
  - the Policies table.

## Out of scope

- Bundled policies beyond terminology, and moving person-not-role or extraction into policies.
- Organization-level policy administration; authoring UI (#264).
- One-click term replacement.
- Relationship findings (#266).

## Risks

- **Cost:** a second provider call per review when a policy applies. The switch and `localOnly` are
  the controls.
- **Composed prompts dilute rules.** With one policy that can't be measured; the ceiling and
  per-policy blocks keep it bounded, and #264's authoring will need the same eval.

## Implementation notes

- **`ComposedAssistant` (assistant-core).** It is the one new SPI: `members()` plus
  `analyzeAll()`, which returns one result per member. The worker applies each result with its
  member's cleanup policy and records each member's definition. The applicator is unchanged.
- **Splitting `DefinitionExecutorAssistant`.** Its provider call and its finding mapping are now
  separate methods. `ComposedPolicyAssistant` builds one composed definition (the union of
  providers, the per-policy blocks), makes one call through it, and maps each finding with its
  own policy's executor.
- **The `policyKey` enum** is filled into a copy of `PolicyReviewOutput` v1 per request.
  `ReviewResult.Finding.policyKey` is omitted from the stored output when null, so review
  output is unchanged.
- **Retirement.** Policies never list other definitions to retire. The review's retirement list
  only names its own task's definitions, so neither pass touches the other's findings.
- **No policy, no run.** `AiReviewService` asks the registry (`policiesFor`) before dispatching
  `POLICY_REVIEW`. When every applicable policy is local-only on a remote provider, the run is
  one empty result under `ai-policies` whose summary says why.
- **UI source.** The DTO mappers are static, so `AnnotationSources` resolves
  `ASSISTANT:<id>` through the switchable-assistant catalog. It only knows enabled bundled
  definitions and switchable beans; #264's project definitions will need it to look further.

# #261 Typed context providers — implementation plan

Epic #258. Builds on #260 (definitions name context providers), #262 (redaction, author
pseudonyms), #257 (seven goal relation types), #272/#273 (provenance never reaches a model).

## Summary

A definition names context providers, and each one adds a typed section to the entity's context
pack under its own budget: goal relations, sibling goals ranked by similarity, the stakeholders
holding a goal, a use case's scenarios, a scenario's use cases, a step's sequence, a story's
actors, an actor's references. The bundled review keeps `["entity"]`, so its prompt is unchanged
and the #355 baseline holds; #263 attaches providers per type and measures them.

## Review against the tree (`release/2.0` @ `46ddb96f`)

- `EntityContextPackBuilder.build(target)` builds one pack: a snapshot, human annotations, the
  glossary terms the entity references, and metadata. `parents`/`children` are always empty.
- `AssistantDefinitionValidator.CONTEXT_PROVIDERS = {entity}`; at least one provider is required.
- The lazy-loading trap in the issue (`GoalAssistant`) is the legacy path. `AssistantRunWorker` runs
  the analyze phase inside a `REQUIRES_NEW` transaction, so providers can walk associations; the IT
  is a guard. #363 later moves the model call out of that transaction; providers stay inside it.
- Budget: `ContextPackSizeLimits.maxTotalCharacters` (100,000) is larger than
  `requel.ai.max-input-tokens` (16,000 tokens, about 64,000 characters). A pack over the token cap
  is skipped, not trimmed (`DefinitionExecutorAssistant`).
- Domain: `Goal.getRelationsFromThisGoal/ToThisGoal`, `Goal.getReferers()` (stakeholders, actors,
  stories, use cases are `GoalContainer`s), `UseCase.getScenario/getAdditionalScenarios`,
  `Scenario.getSteps/getUsingUseCases`, `Step.getUsingScenarios`, `Story.getPrimaryActor/getActors`,
  `Actor.getReferers/getGoals`. No actor-stakeholder link exists.
- GlossaryTerm is not reviewable, so its provider moves to #263.

## Locked decisions (2026-10-02)

1. The bundled `ai-requirements-review` stays `["entity"]`; the prompt is byte-identical.
2. One provider id per aspect, a no-op on other types, so a fallback definition can list several.
3. Stakeholders are not tied to actors. The actor provider carries the stories and use cases that
   reference the actor and the actor's goals. A new `goal-stakeholders` provider carries the
   stakeholders holding a goal and their other goals, for consistency and conflict checks.
   Non-user stakeholders by name and text; user stakeholders by #262 role (`user-N`) and team only.
4. GlossaryTerm provider: moved to #263 with making GlossaryTerm reviewable.
5. Added `scenario-usecases` (Scenario is reviewable and had none).
6. Budget: providers are trimmed, never the review skipped; each provider has its own share; a
   definition can override shares; what was cut is recorded.
7. Sibling goals are ranked by a non-AI similarity score (TF-IDF over stemmed words, glossary terms
   and their alternates as one token) after relations and shared stakeholders. A project-wide
   non-AI overlap and conflict finder is #266's first stage and reuses the scorer.
8. Incoming relations use the inverse label; symmetric relations appear once. A step shared by
   several scenarios shows each one (up to 5) with its previous and next step. Related entities
   carry name and text only: no annotations, no provenance or references. A definition naming only
   `entity` produces today's pack byte for byte.
9. Follow-on #363 (model call outside the transaction), v2.0, on the epic.

## Contracts

### SPI — `assistant-core`, package `context.provider`

```java
public interface ContextProvider {
    String id();                       // "goal-relations"
    boolean appliesTo(Object target);  // false = no section
    ContextSection contribute(Object target, ProviderBudget budget, ProviderContext context);
}

record ContextSection(String providerId, List<RelatedEntity> entities,
        int shown, int available, boolean truncated) {}
record RelatedEntity(EntityRef ref, String relation, String name, String text,
        List<RelatedEntity> children) {}
```

- `ProviderContext` carries the project's `RedactionPolicy`, the run's `AuthorPseudonyms`, and the
  `redacted`/`truncated` lists, so provider text is redacted and noted like base fields.
- `ContextProviderRegistry` (`@Component`) collects the beans; `entity` is the base pack and always
  built. The validator takes its known ids from the registry instead of the constant.

### Pack

- `EntityContextPack` gains `List<ContextSection> context`, `@JsonInclude(NON_EMPTY)`, so a pack
  with no providers serialises exactly as today (golden test).
- `EntityContextPackBuilder.build(target, PackSpec)`; `build(target)` stays and means "entity only".
  `PackSpec` = provider ids in definition order, their budgets, and the character cap.

### Budget

- Cap = `min(maxTotalCharacters, maxInputTokens × 4)`.
- Base pack first, then providers in the definition's order. Each gets
  `min(its share, what is left)`; default share 8,000 characters
  (`requel.assistant.context-pack.provider-budget`).
- After building, if the executor's estimate still exceeds the token cap (JSON overhead), drop
  provider sections from the last one back until it fits, and record each drop. The base pack is
  never dropped, so providers can no longer cause a skip.
- Truncation notes name the provider and counts: `goal-siblings: 40 of 212 shown`.

### Definition budgets (V32)

- `assistant_definitions.context_budgets_json` (TEXT NULL): `{"goal-siblings": 20000}`.
- `AssistantDefinition.contextBudgets` (`Map<String,Integer>`, empty = defaults). Validation: keys
  must be providers the definition names; values positive; their sum within the cap.
- Bundled JSON may carry `contextBudgets`; the seeder and store round-trip it.

### Providers

| Id | Applies to | Section |
|---|---|---|
| `goal-relations` | Goal | Outgoing relations (label) and incoming (inverse label), symmetric once; related goal name and text |
| `goal-siblings` | Goal | The project's other goals, summary (name, text cut to 300 chars), ranked: related, same stakeholder, similarity, rest |
| `goal-stakeholders` | Goal | Stakeholders holding the goal (non-user: name and text; user: `user-N` and team), each with their other goals in summary |
| `usecase-scenarios` | UseCase | Primary scenario (marked) and additional scenarios, with steps; nested scenarios to depth 2 |
| `scenario-usecases` | Scenario | Use cases using the scenario, with each one's primary actor |
| `step-sequence` | Step | Each using scenario (up to 5): its name, the previous and next step, and its use cases |
| `story-actors` | Story | Primary actor (marked) and the story's other actors, name and text |
| `actor-references` | Actor | Stories and use cases referencing the actor (summary), and the actor's goals |

### Similarity — `TextSimilarity` (assistant-core)

- TF-IDF over the project's goals: lower-case, stop words removed, Porter-stemmed (OpenNLP 2.5.9,
  added to assistant-core; already in the app), each glossary term and its alternates mapped to one
  token. Cosine similarity. Deterministic; no model.
- Built per run from the project's goals; 1,000 goals is well under a second.
- WordNet synonyms and conflict hints are #266's stage 1, which reuses this class.

## Step by step

1. SPI, registry, `ContextSection`/`RelatedEntity`, `PackSpec`; pack field with `NON_EMPTY`.
2. Budget: cap, shares, the drop-from-the-end fit, notes.
3. V32 + `contextBudgets` through record, entity, store, seeder, validator.
4. `TextSimilarity`.
5. The eight providers.
6. `DefinitionExecutorAssistant` passes the definition's providers and budgets.
7. Tests, then the #355 re-score check.

## Test plan

- **Golden:** a definition naming only `entity` produces the same pack JSON and prompt as today.
- **Per provider:** each contributes its associations, asserted against the captured request (fake
  CLI capturing stdin, as `AssistantDefinitionsIT`), with a test definition naming that provider.
- **Lazy loading:** every provider runs under `AssistantRunWorker` in an IT with no
  `LazyInitializationException`.
- **Budget:** a provider over its share is trimmed and noted; a 300-goal project still runs (not
  skipped) and records `goal-siblings: N of 299 shown`; total stays within the cap; a definition
  budget override takes effect; an invalid override is rejected.
- **Ranking:** a near-duplicate goal ranks above unrelated goals and survives trimming in a large
  project; related and same-stakeholder goals come first.
- **Stakeholders:** a user stakeholder appears as `user-N` and team, never a name or username.
- **Redaction:** an email in a related goal's text is masked and counted.
- **Validation:** an unknown provider id is rejected with the registered ones listed.
- **No provenance:** a goal with a source and references sends neither.
- **Baseline:** `score.py --rescore` of the #355 baseline is unchanged (the bundled review is).

## Out of scope

- GlossaryTerm provider and making GlossaryTerm reviewable (#263).
- Attaching providers to the bundled review, and tuning budgets (#263).
- The project-wide non-AI overlap/conflict finder, WordNet synonyms, conflict hints (#266 stage 1).
- Moving the model call out of the transaction (#363).
- Project- and issue-level packs; prompt content.

## Risks

- **Prompt drift** if the new field serialises when empty; the golden test pins it.
- **Large projects:** walking every goal for siblings is one query per run; fine at thousands, and
  the ranking cost is linear in goals.

## Implementation notes (2026-10-02)

Where the build differs from the plan above:

- **SPI package.** `ContextProvider`, `ContextSection`, `RelatedEntity`, `ProviderBudget`,
  `ProviderContext`, `PackSpec`, `ContextProviderRegistry` and `TextSimilarity` sit in
  `assistant.core.context` beside the builder, so `ProviderContext` can reuse its package-private
  redaction and pseudonym helpers. The eight providers are in `context.provider`.
- **Budget setting** is `requel.assistant.context-pack.provider-budget` (the existing
  `ContextPackSizeLimits` prefix), default 8,000.
- **Entity-only definitions keep the pack's own cap** (100,000), not the input-budget cap, so the
  bundled review's pack is exactly what it was. The input-budget cap applies once a definition
  names a provider.
- **Fit to the cap** happens in `DefinitionExecutorAssistant`: while the estimate is over
  `max-input-tokens`, `EntityContextPack.withoutLastSection()` drops the last provider section and
  notes it.
- **Fold-in: a goal can now belong to several stakeholders.** `AbstractStakeholder.getGoals()` was
  `@OneToMany`, whose join table makes `stakeholders_goals.goals_id` unique, so a second
  stakeholder adding the same goal failed with a constraint violation. Actors and stories already
  use `@ManyToMany`. It is now `@ManyToMany` on the same table and columns; V33 adds a plain index
  on `goals_id` (the foreign key needs one) and drops V1's unique key
  `UKk2eufeougg9qi1ipm2nvesemk`. `StakeholderGoalsMigrationMySqlIT` covers it where Docker runs.
- **Provenance:** no provider reads sources or references (they walk only the associations in
  the table above), so there is no separate IT for it.

Tests: `TextSimilarityTest`, `ContextProvidersTest` (each provider, composition, budget, trim,
drop, redaction), validator/store/bundled tests for `contextBudgets` and registry ids,
`DefinitionExecutorAssistantTest` (spec, cap, drop-not-skip), and `ContextProvidersIT` (every
provider through the run worker into the fake CLI's prompt, user stakeholders by role, redaction,
a 42-goal project trimmed with the near-duplicate kept).

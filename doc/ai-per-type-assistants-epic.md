# [Epic] AI analysis: data-driven assistant definitions, typed context providers, and a keyless dev provider

Relates to #43 (Modernize background analysis so it can use a current AI model). #43 built the
plumbing; this epic is the analysis-quality work that plumbing was for.

## Problem

Issue #43's AI plumbing is in place — `AiAnalysisClient` with noop and Spring AI clients,
`AssistantDispatcher` / `AssistantRunWorker`, context packs, `AssistantRun` / `AssistantUsage`, the
MCP server, and a manual dispatch endpoint at `POST /api/ai/reviews`. What is missing is everything
that makes the output good, and the means to work on it:

1. **One prompt for every entity type.** `RequirementsReviewAssistant.TASK_INSTRUCTIONS` is a single
   Java text block and the assistant targets `TextEntity`, so a goal, a user story, a use case, a
   scenario step, an actor and a glossary term are all reviewed with identical instructions. A goal
   needs checking for measurability and relations; a story for actor-role-benefit shape; a use case
   for preconditions, primary scenario and error paths; a glossary term for circularity. One prompt
   cannot do all of that. The legacy NLP layer already models this correctly — `project-jpa` has
   `GoalAssistant`, `StoryAssistant`, `UseCaseAssistant`, `ActorAssistant`, `ScenarioStepAssistant` —
   and the AI layer collapsed it back to one.
2. **Assistants are compile-time artifacts.** `RequelAssistant<T>` binds to one `targetType()`, and
   `SimpleAssistantRegistry` matches against a static list of Spring beans. Adding an analysis means
   a class, a release, and a restart — so prompt work is an edit-rebuild-restart loop, and a project
   can never have analysis of its own.
3. **No cross-cutting analyses are expressible.** An organization wanting one check applied across
   *every* entity type — secrets or PII that should not be in a project, banned terminology, a house
   language standard — has nowhere to put it. Scope is a property of the analysis, and today scope
   is a Java type parameter.
4. **No usable model to iterate against.** There are no OpenAI or Anthropic API keys, the Gemini
   profile fails for lack of tokens, and the bundled Ollama default (`qwen2.5:3b`) is not good enough
   at filling the structured output to tell a prompt problem from a model problem. The `claude` /
   `codex` CLIs *are* authenticated on the developer's machine — the one frontier model reachable
   today.
5. **PII can already leak.** `RedactionPolicy` exists in `assistant-core` as the designed seam and is
   called per text field by every context-pack builder, but the only implementation is
   `NoOpRedactionPolicy`, whose own javadoc says *"Replace by registering a project-aware
   RedactionPolicy bean before enabling external AI."* Nothing has. `AiAnalysisRequest.dataHandlingFlags`
   is plumbed and always `Map.of()`.

## Goal

Make an assistant a **definition** — data, stored in the database, scoped to the entity types it
applies to, editable per project — executed by one runtime, with the one genuinely code-shaped
concern (what context to gather for a given entity type) kept in code as registered providers. Give
the developer a keyless way to run the whole in-app path against a real model so the definitions can
actually be tuned. Close the redaction gap before any of it sends project text outside.

The architectural line from #43 holds throughout: **AI suggests, Requel applies.** Nothing here adds
a path that writes project state outside the existing command + authorization chain.

## Why data-driven (the trade-off, recorded)

Going through what varies between two assistants: prompt text, finding vocabulary, scope, task type,
gating and token budget are all data. The output schema is fixed by design — a finding whose shape
the applicator cannot map is worse than no finding. Finding-to-annotation mapping is shared. The one
axis that is genuinely code is **context selection**: "include the goal's relations", "include the
use case's scenarios and their steps", "include the actors this story references" means traversing
the domain graph inside a transaction under `ContextPackSizeLimits`. Expressing that as
configuration means inventing a traversal language, which is how this kind of design goes wrong.

So the line is drawn there: **definitions are data, context providers are code**, one executor runs
any definition, and a definition may name a custom executor bean for the rare assistant that needs
real logic — which is exactly what the legacy NLP assistants are.

What this costs, accepted deliberately: the compiler stops catching a bad entity-type name, so
definitions must be validated at save and at startup against registries of known types and
providers; `SimpleAssistantRegistry` has to resolve per run and per project instead of holding a
static bean list; and a definition authored by a user is near-executable configuration, so the fixed
output envelope, Requel-side validation, the no-tools rule and the command path for writes are
load-bearing rather than nice to have.

## Evidence from a worked example

Before writing any of this, two real, well-written tickets were converted by hand into goals and
reviewed. The full record is in `doc/pq-roundtable-case-study.md`; the results are what the scope
below is shaped around:

- **The generic vocabulary flattens everything.** Findings that actually mattered were *goal states
  a solution rather than an outcome*, *goal is a checkable proxy that can be fully true while the
  intent fails*, *goal names an individual where an actor role is meant*, *two goals are the same
  goal at different abstraction levels*, *goal specifies only who is excluded and never who is
  admitted*, *goal is really a standing constraint with no success state*, and *goal is about team
  process, not about the system under specification*. Today's `AMBIGUOUS` / `INCOMPLETE` /
  `UNTESTABLE` list collapses every one of those into two buckets. That is the concrete case for
  per-type definitions carrying their own vocabulary.
- **The best findings were not about one entity.** The highest-value results were a CI gate whose
  check list omitted the one test another ticket warned would silently skip, two lifecycle states
  (*end* and *archive*) used as if they were one, and a disposition vocabulary that did not match
  between parent and child. None is a property of any single entity, and nothing in the current
  architecture can express them — hence child 8.
- **Some findings are about the wrong entity type, not bad text.** A six-step acceptance criterion
  was not a badly-written AC; it was a Scenario squashed into one. A sentence in a problem statement
  was a Story. A person's name in a goal was an Actor and a UserStakeholder. The useful output is
  "extract this into an entity of type X", which maps onto commands Requel already has
  (`ConvertStepToScenario`, `AddScenarioToUseCase`, `upsertGoalFromRequirement`).
- **One check needed no model at all.** Every finding cited a phrase; asserting that the cited
  phrase actually occurs in the entity text catches fabricated evidence deterministically.

## Scope (child issues)

1. **Dev-only `cli` AI provider** — a fourth `AiAnalysisClient` shelling out to the locally
   authenticated `claude` / `codex` CLI. Unblocks everything below without an API key.
2. **Assistant definitions: model, storage, and a per-project registry** — the core change.
3. **Typed context providers** — code, one per entity type, referenced by id from definitions.
4. **Close the redaction gap** — a real `RedactionPolicy` and honoured `dataHandlingFlags`.
5. **Seed and tune the bundled per-type review definitions** — the analysis work.
6. **Project-authored definitions** — command path, API, permissions, Angular UI.
7. **Cross-cutting policy definitions** — multi-type scope and a single composed pass.
8. **Corpus analyses** — findings about relationships *between* entities, which belong to no single
   target and cannot be expressed by anything above.

Build order is 1 → 2 → 3 → 5 → 6 → 7 → 8, with **4 independent and parallelisable** (it touches
only `assistant-core`) but required before any non-local provider is enabled for real project data.

**Child 6 should not start until child 5 reports.** A single worked example (below) revised the
finding vocabulary and exposed a missing scope; building an authoring UI over a vocabulary and scope
model that tuning is about to change is the wrong order. Storing definitions in the database from
child 2 costs nothing here — none of that is rework — but 6's API and UI wait.

## Cross-cutting notes

- **The provider seam is closed and stays closed.** `AiAnalysisClient.analyze(AiAnalysisRequest)` is
  provider-neutral and the clients are mutually exclusive on
  `@ConditionalOnProperty(prefix="requel.ai", name="provider")`. Child 1 adds a fourth client and
  changes nothing else.
- **One assistant per (entity, *task type*)** — not per entity. Review definitions serve
  `REQUIREMENTS_REVIEW`; policy definitions serve their own task and therefore never collide with
  them. Within a single task type, two definitions claiming the same entity means duplicate findings,
  and the registry must prevent it.
- **The output envelope is not user-tunable.** Instructions and finding vocabulary become data; the
  structured contract (`summary` / `findings` / `warnings`, the `ReviewResult` binding and Requel-side
  `validate()`) stays code. `findingType` becomes a per-definition declared vocabulary rather than one
  global enum.
- **Prompt injection.** Context packs already carry user-authored project text; child 6 adds a second
  surface in user-authored instructions. The defence is identical both times: fixed output envelope,
  validation before mapping, the provider never granted tools, every applied change through the
  command chain.
- **Manual dispatch is the tuning loop.** `POST /api/ai/reviews` (`AiReviewController` →
  `AiReviewService`) already dispatches a `REQUIREMENTS_REVIEW` run for one entity as the current
  user; its `ENTITY_TYPES` map covers Goal, Story, Actor, UseCase. Children 3 and 5 extend it rather
  than building a separate harness.
- **Post-edit path stays off.** AI self-gates to manual dispatch so cost and latency never land on
  the edit hot path. Legacy NLP assistants keep serving the default post-edit task untouched.
- **Remediation stays annotation-shaped.** "Anonymize this PII", like any other fix, is a position
  proposing a concrete replacement, applied through the command path. No new write surface.

## Open questions

- **Do definitions travel with project XML export/import?** A project's authored assistants are
  arguably part of the project. If they do, import becomes another untrusted-instruction surface.
- **Organization-level definitions above project?** Child 6 is project-scoped. An org-wide house
  standard is the obvious next ask and adds a third resolution layer; deferred until asked for.
- **Do the legacy NLP assistants become definitions with custom executors,** or stay bean-registered
  indefinitely? The registry supports both; the question is whether one mechanism is worth the
  migration.
- **Does `requel.ai.enabled` survive?** With per-definition enablement stored per project, a global
  boolean and a `projectAllowlist` property may be redundant — or may be the right kill switch.
- **Per-type token budgets.** `maxInputTokens` is global; a use case with all its scenarios is much
  larger than a glossary term. Child 5 will show whether per-definition budgets are needed.
- **Where do explicit non-goals live?** Real specs carry them ("panelists do see the livestream
  indicator; that is expected and is not to be suppressed"). It is not a goal, not a use case, and
  a Note has no normative force — so an assistant has nowhere to attach a finding that a non-goal is
  being violated, and no way to tell one from an unimplemented requirement.
- **Can a position carry a structured proposal?** Extraction findings want to say "create a Scenario
  from these six steps", not just describe it in prose. Requel has the commands; the question is
  whether a position may reference one, and what authorization that implies.
- **Are external systems actors?** Zoom, IVS and MediaConvert participate in the flows but are not
  people. Whether they are modelled as actors changes what a use-case reviewer should demand. The
  working test for humans held up well and should be written into the Actor definition: if no use
  case contains a step this person performs, they are a stakeholder, not an actor.
- **Does the project know whether the system already exists?** The same sentence is a constraint for
  greenfield and an observation for brownfield — "must use AWS account 714494397341" is normative
  before the system is built and descriptive after. A reviewer cannot classify it without being
  told, so either the project carries that flag or definitions must ask.
- **Can one document be recorded as subordinate to another?** Both non-ticket sources in the case
  study state their own authority ("if the two disagree, the repository is correct"). A living
  document needs to hold that ordering; nothing in Requel does.
- **Does an entity need provenance?** The adjacent direction — ingest a ticket, analyse it, emit a
  corrected one, and keep the project as the live document behind it — needs an entity to know which
  external artifact it came from, so a re-ingest updates rather than duplicates. That capability is
  deliberately out of scope here (see the case study, §8), but adding a provenance reference later is
  much cheaper than retrofitting identity, so this epic should avoid foreclosing it.
- **Is `gemini` worth fixing?** It exists but fails for lack of tokens. If usable it is a second real
  provider for child 5's cross-check, which otherwise rests on Ollama alone.

---

# Child 1 — Dev-only `cli` AI provider: shell out to an authenticated `claude` / `codex` CLI

## What exists today

- `AiAnalysisClient` (`modules/assistant-ai/.../ai/AiAnalysisClient.java`) — the provider seam:
  `AiAnalysisResponse analyze(AiAnalysisRequest)`.
- `NoopAiAnalysisClient` — `@ConditionalOnProperty(prefix="requel.ai", name="provider",
  havingValue="noop", matchIfMissing=true)`.
- `SpringAiAnalysisClient` — registered by `SpringAiClientConfiguration` for `openai` /
  `openai-compat` / `anthropic`. It owns, as private members, the pieces any second real client
  needs: the `ReviewResult` record, `validate(ReviewResult)`, `toResponse(...)`, `findings(...)`,
  `messages(...)`, `usage(...)`.
- Provider profiles `application-ai-{openai,anthropic,gemini,ollama}.properties`, and
  `docker-compose.local-ai.yml` for a keyless Ollama run.
- Structured output on the Spring AI path is forced by `responseEntity(ReviewResult.class)` — a
  Spring AI facility a non-Spring-AI client does not have.

## Work

- **Refactor first:** lift `ReviewResult`, `validate`, and the response mapping out of
  `SpringAiAnalysisClient` into a package-level `ReviewResultMapper` in
  `com.rreganjr.requel.assistant.ai`, and have the Spring AI client delegate to it. Existing
  `SpringAiAnalysisClientTest` behaviour must be unchanged. Doing this before the new client is the
  point — two hand-rolled parsers of the same contract will drift.
- Add `CliAiAnalysisClient` in `com.rreganjr.requel.assistant.ai.cli`, registered for
  `requel.ai.provider=cli`, mutually exclusive with the other clients.
- Prompt assembly: reuse the Spring AI client's JSON prompt shape and — with no structured-output
  converter here — append the request's `outputSchema` (already carried on `AiAnalysisRequest`) with
  an explicit "reply with JSON matching this schema and nothing else" instruction. Strip any code
  fence before parsing.
- Process execution, all of which is load-bearing:
  - `ProcessBuilder` with an **argv list**; never `sh -c`, never string interpolation of prompt text
    into a command line. Context packs contain user-authored project text.
  - Prompt on **stdin**, not as an argument (argv limits, process-table exposure).
  - **Agent tools off.** These CLIs are coding agents, not completion endpoints. Non-interactive
    print mode, tools disabled, read-only sandbox — for `claude`, print mode with an empty
    allowed-tool set; for `codex`, non-interactive exec with a read-only sandbox. Verify the flags
    against the installed versions at implementation time; they move between releases, so the
    invocation must be configurable rather than hard-coded.
  - Working directory a fresh empty temp dir, not the repo.
  - Timeout with `destroyForcibly` on expiry, a cap on captured stdout/stderr bytes, and stderr
    drained so the child cannot block on a full pipe.
  - Non-zero exit, timeout, empty output and unparseable output all become `AiAnalysisException`,
    never a partial `AiAnalysisResponse`.
- Configuration under `requel.ai.cli.*`: `command` (absolute path), `args`, `timeout`,
  `maxOutputBytes`. No default pointing at a real binary.
- Usage: fill `AiUsage` from what the CLI reports (`claude -p --output-format json` returns usage and
  cost; codex reports less), leaving the rest null — the record permits it. Provider and model
  strings come from `AiProperties` so `AssistantUsage` rows stay attributable.
- New `application-ai-cli.properties`: `requel.ai.enabled=true`, `requel.ai.provider=cli`, and
  `spring.ai.model.chat=none` so no Spring AI chat autoconfiguration demands a key at boot.
- `doc/AI_ASSISTANT_SETUP.md`: a clearly-labelled **development-only** section stating this drives an
  interactively-authenticated CLI on the developer's own machine, is not a supported deployment
  provider, does not work in the Docker image, and must not be used to serve other users.

## Acceptance criteria

- With `--spring.profiles.active=dev,ai-cli` and a configured CLI path, `POST /api/ai/reviews` for a
  Goal returns findings produced by the CLI, and `AssistantRun` + `AssistantUsage` rows are written
  exactly as on the Spring AI path.
- With no `requel.ai.provider` set the noop client is still the only `AiAnalysisClient` bean; a test
  asserts the shipped `application.properties` does not select `cli`.
- A context pack containing shell metacharacters and quotes round-trips unchanged into the prompt and
  executes nothing — unit test over the argv/stdin construction.
- A CLI that exits non-zero, hangs past the timeout, or emits non-JSON each surfaces as
  `AiAnalysisException`, leaves no annotations, and records the failure on the run.
- Output exceeding `maxOutputBytes` is truncated and rejected rather than parsed.
- `ReviewResultMapper` is the only implementation of validation/mapping, used by both clients, with
  the existing Spring AI tests green.
- `mvn verify` passes with the CLI client present and unselected — its tests must fake the process
  boundary, never require a CLI binary.

## Not in scope

- Any non-dev deployment of this provider, or CI use.
- Streaming, multi-turn, or tool-using CLI sessions.
- Changing the Spring AI clients' behaviour.

---

# Child 2 — Assistant definitions: model, storage, and a per-project registry

## What exists today

- `RequelAssistant<T>`: `assistantId()`, `targetType()` returning `Class<T>`, `handlesTask(String)`,
  `analyze(AssistantContext, T)`. Scope is a compile-time type parameter.
- `AssistantRegistry.findAssistantsFor(Object target, AssistantContext context)` returns
  `List<RequelAssistant<?>>`; `SimpleAssistantRegistry` iterates an injected static list of beans and
  matches `assistant.targetType().isInstance(target)`. Multiple matches are already supported.
- `RequirementsReviewAssistant` — one bean, `@ConditionalOnProperty(requel.ai.enabled=true)`, holding
  the prompt as a constant, the schema as a classpath resource, and the whole run body.
- `AiAnalysisRequest.instructions()` already carries per-call task guidance to the provider client,
  falling back to the client's generic text when blank — the transport for data-sourced prompts
  exists; only the source is hard-coded.
- `AssistantRunWorker` routes by task type; `JpaAssistantRunStore` persists runs; `AssistantUsage`
  records provider/model/tokens.

## Work

- **`AssistantDefinition`** (JPA entity + DTO): stable key, display name, kind (`REVIEW` | `POLICY`),
  task type, **scope** (set of entity type names; empty means every reviewable `TextEntity`),
  context provider ids, instructions, finding vocabulary (list of `{type, description}`), output
  schema name + version (from a fixed allowed set), enabled flag, definition version, source
  (`BUNDLED` | `PROJECT`), owning project (null for bundled), optional `executorBean`, audit columns.
- **Seeding.** Bundled definitions ship as classpath resources (`ai/definitions/*.json`) and are
  upserted at startup by version, so the shipped baseline propagates on upgrade. A project-authored
  or project-forked definition is never clobbered by a seed; record which bundled version it forked
  from.
- **`DefinitionExecutorAssistant`** — one `RequelAssistant` implementation carrying a resolved
  definition and doing what `RequirementsReviewAssistant` does today: allowlist and task gating,
  input-token estimate, provider call, usage persistence, `AiFindingDraft` → `AnnotationAction`
  mapping, text caps. `assistantId()` returns the definition key so run records stay attributable.
- **Registry.** Extend/replace `SimpleAssistantRegistry` so resolution happens per run: read the
  definitions for the run's project, filter by scope and task type, wrap each in an executor, and
  return them **alongside** the statically-registered bean assistants so the legacy NLP path does not
  regress. A definition naming `executorBean` resolves that bean instead of the generic executor —
  the escape hatch.
- **Collision rule.** Within one task type, two definitions matching the same entity is a
  configuration error: reject on save, and fail startup for bundled definitions.
- **Validation**, at save and at startup: entity type names against a registry of known reviewable
  types, context provider ids against registered providers (child 3), instruction length cap counted
  against `maxInputTokens`, non-empty vocabulary, schema name in the allowed set.
- **Caching.** Definitions are read per run; cache per project and evict on write, so a bulk review
  does not issue a definition read per entity.
- **Evidence verification, free and deterministic.** Every finding carries `evidenceReferences`;
  assert each cited string actually occurs in the entity text (normalised for whitespace) before the
  finding is mapped. A finding citing evidence that is not there is dropped and recorded as a
  provider-quality warning on the run. This catches fabricated evidence with no model involved and
  gives child 5 a hard signal that is not a judgement call.
- Record the resolved definition key and version on the `AssistantRun` and in `providerMetadata`, so
  every finding traces to the definition that produced it. This is what makes child 5 measurable.
- Migrate the current behaviour: `RequirementsReviewAssistant`'s `TASK_INSTRUCTIONS` becomes the
  seeded `default` review definition verbatim, so this child is behaviour-preserving.

## Acceptance criteria

- With only the seeded `default` definition, a review produces output identical to the pre-change
  hard-coded assistant — a golden test asserting the resolved instructions equal the former constant.
- Two projects with different definitions produce different prompts in the same JVM with no restart.
- Enabling/disabling a definition takes effect on the next run, no restart.
- Legacy NLP assistants still run on the default post-edit task, unchanged.
- A definition with an unknown entity type, unknown context provider, or colliding scope is rejected
  on save; a bad bundled definition fails startup with a clear message.
- The run record and `providerMetadata` name the definition key and version.
- A bulk review of N entities in one project issues O(1) definition reads — asserted, not assumed.
- A finding whose `evidenceReferences` do not occur in the entity text is dropped, the run records
  it, and no annotation is created.
- A definition naming `executorBean` dispatches to that bean.

## Not in scope

- Any UI or REST surface (child 6) — seeding and direct repository/test access only.
- Context provider implementations (child 3); definitions may reference only the base pack until then.
- Policy composition (child 7).
- Writing good prompt content (child 5).

---

# Child 3 — Typed context providers

## What exists today

- `EntityContextPackBuilder` builds one `EntityContextPack` shape for any `TextEntity`;
  `ProjectContextPackBuilder` and `IssueContextPackBuilder` cover the other packs.
- `ContextPackSizeLimits` caps pack size; `ContextPackTextUtils` truncates; `ContextPackMetadata`
  carries `redactedFields()`.
- `RedactionPolicy` is called per text field by the builders (child 4 gives it teeth).
- The legacy precedent and its recorded trap: `GoalAssistant.analyzeGoalRelations` is unimplemented
  with a comment explaining exactly why — `relationsFromThisGoal` is `FetchType.LAZY` and the entity
  is already detached by the time the async run body executes, so the traversal must happen inside a
  `TransactionTemplate`.
- Definitions (child 2) can name context provider ids but nothing implements them yet.

## Work

- A `ContextProvider` SPI: stable id, the entity type it applies to, and a contribution step that
  runs inside a transaction and appends to the pack under its own budget.
- A `ContextProviderRegistry` keyed by id, which child 2's definition validation checks against.
- Implement the providers the review definitions need:
  - **Goal** — outgoing and incoming goal relations with the related goals' text, **plus the
    project's other goals in summary form**. Sibling goals are required, not optional: two goals
    that are the same goal at different abstraction levels are undetectable from one goal's text
    and its relation set, because the missing relation *is* the finding. Summary form (title plus
    truncated text) keeps this inside the budget.
  - **UseCase** — its scenarios and their steps, primary scenario marked.
  - **Step** — parent scenario and use case, plus neighbouring steps for sequence context.
  - **Story** — the actors the story references.
  - **Actor** — the stories and use cases that reference the actor, and the stakeholders associated
    with it. An actor is a role; the same human is often both an actor (they operate the system) and
    a user stakeholder (they have an interest in it, and domain authority over it). A reviewer that
    cannot see both cannot tell a missing role from a missing stakeholder, and cannot propose
    replacing a person's name in a requirement with the role that person fills.
  - **GlossaryTerm** — related and similarly-named terms, for circularity and conflict checks.
- Per-provider budget within `ContextPackSizeLimits`, so one provider cannot consume the whole pack;
  truncation recorded in pack metadata rather than silent.
- `EntityContextPackBuilder` composes the base pack plus the named providers for the run's
  definition.
- Every provider loads its associations inside a transaction — the `GoalAssistant` trap is the
  documented failure mode, and an integration test under the async run worker is the only thing that
  proves it.

## Acceptance criteria

- Each provider contributes its associations to the pack, asserted per provider against a captured
  `AiAnalysisRequest`.
- No `LazyInitializationException` for any provider under the async run worker — integration test,
  not unit test.
- A provider exceeding its budget truncates and records that in pack metadata; total pack size still
  respects `ContextPackSizeLimits`.
- A definition naming an unknown provider id is rejected (child 2's validation, exercised here with
  real providers registered).
- A definition naming no providers still produces the base pack unchanged.
- Redaction is applied to provider-contributed text, not just base fields.

## Not in scope

- New domain associations or schema changes — providers read what exists.
- Project- and issue-level packs.
- Prompt content (child 5).

---

# Child 4 — Close the redaction gap: a real RedactionPolicy and honoured dataHandlingFlags

## What exists today

- `RedactionPolicy` (`assistant-core/.../context/RedactionPolicy.java`) — called per text field by
  every context-pack builder with a stable `fieldPath` (`project.description`, `goal[42].text`,
  `annotation.text`), returning the value to use and appending notes that flow into
  `ContextPackMetadata.redactedFields()`. The seam is correct and complete.
- `NoOpRedactionPolicy` is the only implementation: passes everything through, `@ConditionalOnMissingBean`,
  javadoc reading *"Replace by registering a project-aware RedactionPolicy bean before enabling
  external AI."*
- `AiAnalysisRequest.dataHandlingFlags` is a constructor parameter, plumbed into the Spring AI
  prompt, and passed `Map.of()` at the only call site. A test asserts a flag named
  `externalProviderAllowed` — the intent exists; the enforcement does not.

## Work

- A real default `RedactionPolicy`: deterministic pattern detection for credential-shaped strings
  (API keys, bearer tokens, private key blocks, connection strings with passwords) and common direct
  identifiers (email addresses, phone numbers, national ID formats), masking rather than dropping
  where the surrounding sentence still carries meaning.
- Field-path scoping so rules differ per field, per the SPI's documented intent.
- Per-project configuration of the policy (which categories are on), consistent with where child 6
  puts project AI settings.
- Populate `dataHandlingFlags` for real from provider selection plus project settings — at minimum
  `externalProviderAllowed` — and **enforce** it at the client boundary: a client that reaches a
  remote endpoint refuses the request when the flag forbids it, rather than trusting the caller.
- Record redaction counts and categories on the run, so an operator can see what was stripped.
- Note in `doc/AI_ASSISTANT_SETUP.md` that AI detection of PII is not a substitute for this: sending
  text to an external provider to ask whether it contains PII has already disclosed it. Detection
  belongs on a local provider or a deterministic policy; this child is the deterministic half.

## Acceptance criteria

- With the default policy active, a goal whose text contains an API-key-shaped string and an email
  address reaches the provider with both masked — asserted at the client boundary with a capturing
  fake, not at the builder.
- `ContextPackMetadata.redactedFields()` names every redacted field path; the run records counts.
- A project configured to disallow external providers cannot dispatch to a remote client; the run
  fails closed with a clear message and no request is made.
- `dataHandlingFlags` is non-empty on every real request and reflects the actual provider.
- Redaction runs before size capping, so masking cannot be truncated away.
- No regression in existing context-pack tests; `NoOpRedactionPolicy` remains available and is what
  tests opt into explicitly.

## Not in scope

- AI-based PII detection as an analysis (child 7 can define one against a local provider).
- Redacting stored project data — this is egress-only.
- Org-wide data-handling policy administration.

---

# Child 5 — Seed and tune the bundled per-type review definitions

## What exists today

- After children 1–3: a keyless dev provider, definitions in the database with one seeded `default`
  carrying today's generic text, and typed context providers.
- `AiReviewService.ENTITY_TYPES` maps only `Goal`, `Story`, `Actor`, `UseCase`.
- No evaluation fixture. Nothing distinguishes "the prompt is wrong" from "the model is weak", which
  is the state that stalled this work.

## Work

- Seed one bundled review definition per reviewable type — Goal, Story, UseCase, Step, Actor,
  GlossaryTerm — each naming its context provider and declaring its own finding vocabulary, and
  retire `RequirementsReviewAssistant` (under a data model it is just the `default` definition).
  Keep `default` as the fallback for a type with no specific definition.
- Extend `AiReviewService.ENTITY_TYPES` with the newly supported types.
- Build a fixture project of deliberately-flawed entities, a small set per type, each with the
  findings a competent reviewer should raise: a goal stated as an unmeasurable aspiration; a story
  missing its benefit clause; a use case with no precondition and no error path; a step with an
  ambiguous actor; a circular glossary term; and one case per vocabulary entry above. Check it in as
  importable project XML so it is reproducible.
- **Include negative cases, and one trap.** The fixture needs entities where the correct answer is
  silence. It also needs the trap the worked example walked into: the ticket stated a finding count
  two ways, the arithmetic was consistent (32 = 24+8), the review pass verified it and said nothing
  — and a third source later showed the real figures were 26 and 6, the original being "a
  recollection, never a count". **Consistency checking cannot detect a shared wrong premise.** A
  definition must not phrase "the numbers are consistent" as "the numbers are right", and the
  fixture must contain a case that punishes it for doing so. A definition that cannot stay quiet is
  worse than one that misses; a definition that confidently confirms a shared error is worse than
  both.
- **Fixture provenance.** The practical way to build the fixture is to mine a real specification
  document, which is how the vocabulary above was derived — but the fixture is checked into a public
  GPL repository, so every entity in it must be synthetic or thoroughly anonymised. The taxonomy is
  the portable output of that exercise; the source text is not.
- Write each definition's instructions and vocabulary, carrying forward the hard-won rules already
  encoded in today's text: do not restate existing annotations, always set `suggestedIssueText`,
  return empty rather than inventing findings, cite evidence from the entity's own text.
- **Starting vocabulary for Goal**, derived from the worked example on the epic and to be revised by
  tuning rather than treated as settled: `SOLUTION_NOT_OUTCOME` (states a mechanism, so replacing
  the mechanism makes the goal unachievable while the intent stands); `PROXY_FOR_OUTCOME` (checkable
  and fully satisfiable while the stated intent fails); `PERSON_NAMED_NOT_ROLE` (names an individual
  where an actor role is meant — the fix is to reference the role, and the same human is usually
  also a stakeholder); `DUPLICATE_AT_DIFFERENT_ABSTRACTION` (needs sibling goals in context, see
  child 3); `NEGATIVE_ONLY_SPECIFICATION` (says who is excluded, never who is admitted, so the
  grant is untestable); `CONJUNCTIVE_GOAL` (two independently-completable goals welded into one, so
  sign-off covers both and progress cannot be tracked); `CONSTRAINT_NOT_GOAL` (a standing rule with
  no success state and an open temporal quantifier — usually belongs as a policy definition, child
  7); `NOT_A_SYSTEM_GOAL` (about team process or ops rather than the system under specification);
  `IMPLICIT_INVARIANT` (an existing property described as fact with no goal protecting it, so a
  future change can break it silently); `MEASURE_WITHOUT_BASELINE` (names an instrument but no
  baseline or threshold, so "met" is never decidable); `VACUOUS_QUANTIFIER` (quantified over a set
  the project also controls, so an empty set satisfies it perfectly — "all alarms route to a
  Conduit-owned destination" was signable while the stack created zero alarms).
- **Three finding categories, not one.** Alongside quality findings, the worked example produced
  *extraction* findings — the content is fine but lives in the wrong entity type (a six-step
  acceptance criterion that is a Scenario; a problem-statement sentence that is a Story; a named
  person that is an Actor plus a UserStakeholder). Decide during tuning whether these get their own
  vocabulary prefix and whether the suggested position names a Requel command
  (`ConvertStepToScenario`, `AddScenarioToUseCase`, `upsertGoalFromRequirement`) or stays prose —
  see the epic's open question on structured positions.
- Iterate: run `POST /api/ai/reviews` per fixture entity through the `cli` provider, score against
  expected findings, revise the definition (a data edit — no rebuild), repeat. Record what was tried
  and rejected in `doc/`; the negative results are the durable part.
- A scoring script under `scripts/` that runs the fixture set and reports, per type, expected
  findings hit, spurious findings, and schema-validation failures. Not CI-gated: it needs a provider
  and the output is nondeterministic.
- Re-check against a larger Ollama model via the `ai-ollama` profile, to catch instructions that only
  work because a frontier model is forgiving.
- Feed what tuning exposes back into code: schema fields never usefully filled, context the model
  repeatedly lacks, caps that truncate the wrong thing, per-definition token budgets if needed.
- This child is large; split it into stacked sub-PRs per the house pattern (fixture + scoring first,
  then definitions in batches).

## Acceptance criteria

- Every reviewable type has a tuned bundled definition; none still carries the generic default text.
- `RequirementsReviewAssistant` is gone, with its behaviour covered by the seeded definitions.
- The scoring script runs the fixture set end to end and reports per-type results.
- Documented before/after scores for at least one full iteration per type.
- Zero schema-validation failures across a fixture run on the tuned definitions.
- No finding in a fixture run merely restates an existing annotation on the entity.
- A written summary in `doc/` of what genuinely varies by type — the input to children 6 and 7.

## Not in scope

- Automated quality gates in CI.
- New task types beyond `REQUIREMENTS_REVIEW`.
- Model or provider benchmarking as an end in itself.

---

# Child 6 — Project-authored definitions: command path, API, permissions, Angular UI

## What exists today

- After children 2–5, definitions live in the database, are seeded from the bundled set, and drive
  every AI review — but nothing outside seeding and tests can create or change one.
- Per-project AI gating is `requel.ai.projectAllowlist`, a config property, not a project setting.
- The patterns to follow: `AuthorizingCommandHandler` and `RequiresStakeholderPermission`
  (`EditProjectCommandImpl` → `RequiresStakeholderPermission(Project.class, "Edit")`), optimistic
  locking via `@Version`, `AuditingCommandHandler` for the audit row, and the project settings
  surface in the Angular workspace.

## Work

- Commands on the audited path for create / edit / enable / disable / fork-from-bundled /
  revert-to-bundled, each project-scoped, with optimistic locking — not direct repository writes.
- A dedicated project permission for managing definitions (an org may want analysis authorship held
  more tightly than ordinary project editing); define it rather than reusing `"Edit"`.
- REST endpoints under the existing authenticated chain: list the definitions effective for a
  project (bundled + project, showing which wins), read a bundled baseline, and the mutations above.
- Angular UI in project settings alongside the AI toggle: list effective definitions by entity type,
  show bundled baseline versus project version, edit instructions / vocabulary / scope / enablement,
  fork, revert, and see which definition is currently in effect for a given type.
- Server-side constraints enforced on every write, not just in the UI: instructions length cap
  counted against `maxInputTokens`; vocabulary non-empty; scope names and provider ids validated;
  output schema name restricted to the allowed set and never free-form; collision with another
  definition in the same task type rejected.
- Surface the risk honestly wherever the permission is granted: a definition is prompt text a
  project member controls, so the guarantees come from the fixed output envelope, Requel-side
  validation and the command path — not from trusting the author.

## Acceptance criteria

- A project definition changes that project's review output and no other project's.
- Fork-from-bundled produces an editable copy recording the bundled version it came from; revert
  restores the bundled baseline exactly.
- The run record shows which definition was used, its source (bundled or project) and its version.
- A user without the permission can neither read nor write definitions; denial matches other
  unauthorized commands and is audited.
- A write that exceeds the length cap, names an unknown type or provider, redefines the output
  schema, or collides within a task type is rejected with a field-level error.
- A definition that induces malformed output produces a validation failure and zero annotations —
  never a partial or malformed write.
- A stale version is rejected with the standard optimistic-lock error.
- Angular component tests cover view / edit / fork / revert / permission-denied.

## Not in scope

- Organization-level definitions (open question on the epic).
- Version history or diffing beyond the current project definition and its bundled origin.
- User-level preferences.
- Carrying definitions in project XML export/import (open question on the epic).

---

# Child 7 — Cross-cutting policy definitions: multi-type scope and a single composed pass

## What exists today

- After child 2, a definition already carries a scope set and a kind, and `POLICY` is a declared kind
  with no runtime behind it.
- Review definitions serve `REQUIREMENTS_REVIEW` and are one-per-entity-per-task by the collision
  rule, which is why policies need their own task type rather than competing for that one.
- Child 4 provides the deterministic redaction half of the PII story; this child is the "flag what
  should not be in the project at all" half.

## Work

- A `POLICY_REVIEW` task type, dispatched alongside a review or on its own, for definitions whose
  scope spans types.
- **One composed pass, not one call per policy.** All enabled policies matching (project, entity
  type) are merged into a single provider call: each contributes its rule text and its own finding
  vocabulary, and every finding in the response carries the id of the policy that raised it, so
  findings are attributed back on the way out. Without this an org with six policies pays six
  provider calls per entity.
- Relax the collision rule for `POLICY_REVIEW`: many policies may match one entity, by design —
  that is what composition is for. The one-per-task rule still holds for `REQUIREMENTS_REVIEW`.
- A per-project ceiling on composed policies, so the merged prompt cannot silently blow the input
  budget; over the ceiling, fail with a clear message rather than truncating rules.
- Seed one bundled example policy — terminology consistency against the project glossary is the
  safest useful one, since it needs no external data and demonstrates multi-type scope.
- Document the PII case explicitly: a PII-detection policy is expressible here, but running it
  against a remote provider discloses the PII it is looking for. Such a definition must be pinned to
  a local provider, and the interaction with child 4's `externalProviderAllowed` flag must be
  spelled out.
- Findings from policies are annotated with their source in the UI so a reviewer can tell a policy
  finding from a requirements finding.

## Acceptance criteria

- N enabled policies matching one entity produce exactly one provider call, asserted.
- Every policy finding names the policy that raised it, through to the created annotation.
- A policy scoped to a subset of types runs only for those types; a policy with empty scope runs for
  every reviewable type.
- Policy findings and review findings coexist on one entity without duplication or interference.
- Exceeding the composed-policy ceiling fails with a clear message; no rules are silently dropped.
- A policy pinned to a local provider does not run when the active provider is remote.
- The bundled terminology policy raises a finding on a fixture entity using a term inconsistent with
  the project glossary, and stays silent on a consistent one.

## Not in scope

- A library of bundled policies beyond the single example.
- Organization-level policy administration.
- Automated remediation beyond positions proposing a fix.

---

# Child 8 — Corpus analyses: findings about relationships between entities

## What exists today

- Every assistant is `analyze(AssistantContext, T target)` returning findings about `target`. Both
  review definitions (child 5) and policy definitions (child 7) produce findings *on the entity they
  were dispatched for* — policies differ only in applying the same rule across many types.
- `ProjectContextPackBuilder` already exists and builds a project-level pack; nothing dispatches an
  analysis against it.
- The annotation layer attaches to any entity through the Hibernate `@Any` / `@ManyToAny`
  discriminator pattern, so an issue can in principle reference more than one subject.
- `AiReviewService` dispatches for exactly one entity.

## Why this is its own child

The highest-value findings from the worked example on the epic were all relationships, and none is a
property of any single entity:

- A CI gate specified as five checks, where a sibling requirement warns that a sixth check silently
  skips when its dependency is absent — so the gate as written is guaranteed to miss the failure the
  sibling flags. Both requirements are correct alone.
- Two lifecycle states used as if they were one across a parent and a child requirement, so an
  implementer reading either in isolation builds the wrong state machine.
- A disposition vocabulary that does not match between a parent's acceptance criterion and the
  child's own framing, making the parent unsatisfiable by the child's output.
- Three different terms for one concept across two documents.

Attempting any of these as a single-target analysis either produces nothing or produces a finding on
an arbitrarily-chosen entity, which is worse.

## Work

- A corpus task type dispatched against a **set** rather than an entity: a whole project, or a
  subset (a goal container, a use case with its scenarios, the entities changed since the last run).
- Context from `ProjectContextPackBuilder`, in summary form — titles plus truncated text plus
  relations — with a hard budget. A corpus pack that tries to carry full text will not fit, and the
  summarisation strategy is the real design work in this child.
- Definitions with `kind = CORPUS`: scope describes the *set* to gather rather than the types to
  match, and the vocabulary is relationship-shaped (`CONTRADICTORY_REQUIREMENTS`,
  `SAME_TERM_DIFFERENT_MEANING`, `SAME_CONCEPT_DIFFERENT_TERMS`, `UNSATISFIABLE_BY_CHILDREN`,
  `DUPLICATE_AT_DIFFERENT_ABSTRACTION`, `ORDERING_DEPENDENCY_UNSTATED`).
- **Attachment.** A finding names two or more entity references. Decide and document where the
  annotation lands: an issue on each participant cross-referencing the others, or one issue with
  multiple subjects. The `@Any` layer permits either; the UI and the dedupe rules do not treat them
  the same, and this decision is the one most likely to be regretted.
- Idempotency across runs: a corpus analysis re-run on an unchanged project must not create a second
  copy of the same relationship finding. Key on the definition plus the sorted participant refs.
- Dispatch is manual and explicit — a corpus run over a large project is the most expensive thing in
  this epic, and must never be triggered by an edit.
- Cost ceiling and a clear refusal when the summarised pack exceeds the input budget, rather than
  silently analysing a truncated project and reporting confident findings about the half it saw.

## Acceptance criteria

- A corpus run over a fixture project raises a contradiction between two requirements that are each
  individually well-formed, and names both participants.
- A term used with two meanings across entities is raised once, not once per occurrence.
- Every corpus finding names at least two entity references, and each reference resolves.
- Re-running against an unchanged project creates no duplicate annotations.
- A project whose summarised pack exceeds the input budget fails with a clear message naming the
  overflow; no partial analysis is reported as complete.
- A corpus run never fires from the post-edit path — asserted.
- Single-entity review findings and corpus findings coexist on an entity without duplication.

## Not in scope

- Cross-*project* analysis.
- Automatically repairing a contradiction; the output is an issue with positions, as everywhere else.
- Incremental or streaming corpus analysis over very large projects — the budget refusal above is
  the first answer, and a chunking strategy is a later ticket if refusal proves too blunt.

# Issue #43 Implementation Review

Reviewer pass against `doc/assistant-spi-plan.md`, `doc/ai-assistance-plan.md`, and
`doc/nlp-optional-plan.md`. Scope: confirm "Phase 4 and earlier" is in good shape
before moving to the next phase, and capture anything in the plans that has drifted
from the code.

Branch reviewed: `43-modernize-background-analysis` (HEAD `011a92e`).

## TL;DR

The module skeleton, SPI contracts, dispatcher/worker transactional boundary, run
persistence, context packs, the AI provider abstraction, and a first slice of the
MCP server are all present and largely faithful to the plans. Test coverage on the
parts that exist is good.

However, "Phase 4 and earlier complete" is optimistic. Three things the plans place
*inside* Phases 1 and 3 are not yet implemented, and they are exactly the pieces a
Phase 5 AI assistant needs to produce visible output:

1. **The real, command-backed `AssistantResultApplicator` does not exist** — only
   `NoOpAssistantResultApplicator`. No `AnnotationAction` is ever turned into an
   annotation. (ai-assistance-plan Phase 1: "Add idempotent AnnotationAction
   application.")
2. **The dispatcher is not wired into the command flow.** The new
   dispatcher/registry/worker pipeline is exercised only by unit tests. Live
   command-triggered analysis still runs entirely through the legacy
   `AssistantFacade` → `AssistantTaskRunner` path. No production code calls
   `AssistantDispatcher.dispatch(...)`. (Migration steps 5–6 not started.)
3. **No `RequelAssistant` implementation exists in production** (no legacy-NLP
   adapter, no AI assistant). The SPI has no live producers, so the finding state
   machine and idempotency machinery have nothing to act on yet.

Separately, the **`nlp-optional-plan` is essentially unimplemented** (no
`requel.nlp.enabled` flag, no conditional config, no `NoOpNLPProcessorFactory`).

My recommendation: this is a solid Phase 1–4 *foundation* but it is not yet an
end-to-end working pipeline. Before starting Phase 5 (first AI assistant), close the
applicator + dispatcher-wiring + one-adapter loop so there is a provably working
"command → dispatch → assistant → annotation" path to build the AI assistant on top
of. Details and a suggested ordering are at the end.

---

## Module shape

Plan target: `assistant-api`, `assistant-core`, `assistant-legacy-nlp`,
`assistant-openai`, `mcp-server`.

Actual modules: `assistant-api`, `assistant-core`, `assistant-ai`, `mcp-server`.

- `assistant-openai` was renamed to **`assistant-ai`**. Reasonable (provider-neutral
  name, holds both Noop and OpenAI clients), but the plan docs still say
  `assistant-openai` in several places (Target Module Shape; Resolved Decisions
  Phase 0). **Action: update the plan docs to match, or rename the module.**
- **`assistant-legacy-nlp` was never created.** The legacy assistants
  (`LexicalAssistant`, `GoalAssistant`, …, `AssistantTaskRunner`, `AssistantFacade`)
  remain in `project-jpa/.../impl/assistant` unchanged. This is consistent with the
  migration being unstarted, but it means the plan's "wrap one legacy assistant as a
  `RequelAssistant<T>` adapter" (Minimal Step 5) has not happened.

Module dependency boundaries (the most important invariant) are **clean**:

- `assistant-api` depends only on `platform-core`, `platform-identity`,
  `project-domain`, `annotation-domain`. No `-jpa`, no `service-impl`, no Spring MVC,
  no provider SDK — verified in both `pom.xml` and actual imports (all `java.*`).
- `assistant-core` depends on `service-api` (allowed) and not on `service-impl`
  (forbidden) — compliant.
- `assistant-ai` does not leak into `assistant-api`; `mcp-server` is not a dependency
  of `assistant-api`. Both forbidden edges are absent.

---

## What matches the plans (good to keep)

### assistant-api
- `RequelAssistant<T>`, `AssistantRegistry`, `AssistantDispatcher` signatures match
  the plan exactly. Assistants are stateless (`analyze(context, target)`), value
  types are immutable `record`s with builders and defensive copies.
- `AssistantContext`, `AssistantResult`, `AssistantMessage`, `ExternalAction` carry
  the fields the plan lists. `ExternalAction` is draft-only with no execution path.

### assistant-core
- **Transactional-proxy boundary is correct and preserves the issue #39 contract.**
  `AssistantDispatcherImpl.dispatch()` is non-transactional, persists a `QUEUED`
  row, hands off to `assistantTaskExecutor`, returns a handle.
  `AssistantRunWorker.runInNewTransaction(runId)` is a *separate bean* annotated
  `@Transactional(REQUIRES_NEW)`, reached via injected reference so the proxy fires,
  and reloads the target via `AssistantTargetLoader` before invoking assistants.
- **Run persistence matches the data-model spec field-for-field.**
  `assistant_runs`, `assistant_findings`, `assistant_usage` (V8 migration) include
  `template_source`, `body_capture_reason`, `body_retained_until`, the composed
  idempotency key, finding state, and the `annotations.assistant_idempotency_key` /
  `source` columns + index.
- **Context packs are well done.** `ProjectContextPack`, `EntityContextPack`,
  `IssueContextPack` live in `assistant-core` (not `service-api`), with size limits
  (`ContextPackSizeLimits`/`Budget`), redaction (`RedactionPolicy` +
  `NoOpRedactionPolicy` via `@ConditionalOnMissingBean`), and per-snapshot `version`
  stamps. Good unit coverage.
- No-op path: the worker treats empty results as "no findings" and records clean
  `SUCCEEDED`/`SKIPPED` rows.

### assistant-ai (Phase 4)
- `AiAnalysisClient.analyze(AiAnalysisRequest) throws AiAnalysisException` matches the
  plan. Request/response shapes carry task type, target/project refs, context packs,
  output schema name+version, locale, data-handling flags / summary, findings
  (`AiFindingDraft` with severity, confidence, evidence refs, suggested text,
  positions), usage, provider metadata.
- `NoopAiAnalysisClient` (`@ConditionalOnMissingBean`) present for dev/test.
- `OpenAiAnalysisClient` is **real, not a stub**: Responses API, strict
  `json_schema` structured output, model id from properties (not hardcoded),
  timeout, retry/backoff on 429/5xx, response schema validation, usage capture incl.
  cached tokens. API key from `OPENAI_API_KEY` env, not checked in. Backed by an
  `HttpServer`-based test.
- `AiProperties`: `requel.ai.enabled=false` default, provider, model, timeout,
  maxInput/OutputTokens, projectAllowlist.

### mcp-server (Phase 3, partial)
- Every tool/resource routes through the `ProjectQueryGateway` abstraction with an
  in-process implementation delegating to `ProjectQueryController` — the seam the
  plan wants for a future standalone bridge.
- JSON-RPC layer dispatches `initialize` / `tools.list` / `tools.call` /
  `resources.list` / `resources.read`.

---

## Deviations from the plans

1. **Module name `assistant-ai` vs `assistant-openai`** (see Module shape). Update
   docs or rename.
2. **`AnnotationAction` action types are coarser than the command-mapping table.**
   `ActionType` = `CREATE_NOTE`, `CREATE_ISSUE`, `CREATE_POSITION`,
   `CREATE_ARGUMENT`, `UPDATE_TEXT`, `RESOLVE_ISSUE`, `REMOVE_ANNOTATION`. The plan's
   mapping needs create/update/**delete** per type plus annotation-link. The plan
   explicitly allows the applicator to branch on metadata, so this is workable, but
   the enum alone can't distinguish "delete a note" from "delete a position." Worth
   confirming the (not-yet-written) applicator can recover the target kind.
3. **No first-class `evidence` field on `AnnotationAction`.** The plan lists evidence
   refs as required and the idempotency key derives from `normalizedEvidenceHash`.
   Today evidence would live in the untyped `metadata` map. Recommend promoting it to
   a typed field before the applicator and finding-hashing are built, since the
   idempotency key depends on it.
4. **MCP tool/resource names differ from the plan** (`requel.getProject` vs
   `get_project_context` / `get_entity`, etc.). Cosmetic, but pin the names down
   before any external client integrates.

---

## Gaps / incomplete (relative to "Phase 4 and earlier")

1. **Real `AssistantResultApplicator` missing.** Only `NoOpAssistantResultApplicator`
   (returns `AppliedAssistantResult(0, [])`, self-labeled "placeholder"). No
   reference to `CommandHandler` or `AnnotationCommandFactory` anywhere in
   `assistant-core`. The command-mapping table, idempotency-key lookup, version-stamp
   population, and execution-as-triggering-user are all unimplemented. *This is the
   load-bearing gap.*
2. **Finding state machine is modeled but inert.** `AssistantFindingState` enum and
   `AssistantFindingEntity`/repository exist, but no code performs any transition; no
   per-assistant cleanup policy (`MANUAL`/`MARK_SUPERSEDED`/`AUTO_RESOLVE_IF_UNTOUCHED`)
   is implemented or even attached to registration metadata. `SimpleAssistantRegistry`
   matches on `targetType` only.
3. **Dispatcher not integrated into the command flow.** No production caller of
   `dispatch(...)`. Legacy analysis still flows `AnalyzableEditCommand.invokeAnalysis()`
   → `AssistantFacade` → `AssistantTaskRunner` (unchanged). Migration steps 6 not
   begun; `AssistantFacade.analyzeX` not converted to emit `AnalysisRequest`s.
4. **No production `RequelAssistant`.** No legacy-NLP adapter and no AI assistant. The
   only implementors are test fakes. So Phase 5's `REQUIREMENTS_REVIEW` assistant has
   no precedent to follow, and the AI provider in `assistant-ai` is an island — not
   referenced by `assistant-core` or any assistant.
5. **MCP auth / dual identity entirely absent.** No `SecurityContext` population, no
   session token scoped to `(user, project, run, assistant_user)`, no triggering-user
   vs assistant-pseudo-user handling, no audit stamping. The controller comment only
   *asserts* the existing JWT chain is reused by virtue of the `/api/**` mount. This
   is the most security-sensitive part of the MCP design and is currently a stub.
6. **MCP surface is ~3 of 12.** Implemented: resources `projects`, `projects/{name}`,
   `projects/{name}/tree`; tools list/get/getTree. Missing: glossary, open-issues,
   entity-annotations resources; `search_project_entities`, `get_project_context`,
   `get_entity`, `get_entity_neighbors`, `get_annotations`, and `draft_annotation`
   tools. `draft_annotation` (the AnnotationAction-draft bridge) does not exist.
7. **`nlp-optional-plan` Scope 1 unimplemented.** No `requel.nlp.enabled` property
   anywhere; `NLPProcessorFactoryImpl` is still an unconditional `@Component`; no
   `NoOpNLPProcessorFactory`; no conditional auto-config; no disabled-NLP smoke test
   profile. If this plan is meant to ship with #43, it has not started; if it's been
   deferred to a later phase, the plan doc should say so.

---

## Concerns / bugs

1. **`JpaAssistantRunStore.rebuildRequest` fabricates data.** On read-back it
   synthesizes `EntityRef.of("Unknown", 0L)`, a placeholder `UserRef(0L,"unknown")`,
   and empty attributes. Because the worker reads the record back via
   `findRun(runId)` (`AssistantRunWorker:73,80-92`), with the JPA store in production
   the worker's `AssistantContext` gets **empty attributes and fake user refs** —
   `request.triggeringUser()` / `assistantUser()` / `attributes()` are not faithful.
   Harmless today (NoOp applicator), but it will silently break authorization
   ("execute as triggering user") and idempotency once the real applicator lands.
   Fix the round-trip persistence before building the applicator.
2. **Locale hardcoded.** `AssistantRunWorker:91` uses `Locale.getDefault()`;
   `AnalysisRequest` carries no locale, so the per-run locale the plan lists as a
   context field is lost.
3. **New annotation columns not JPA-mapped.** V8 adds
   `annotations.assistant_idempotency_key` and `annotations.source`, but the
   `annotation-jpa` entity maps neither. They are write-unreachable until mapped, so
   reverse-lookup and source labeling are non-functional. Also note tests use
   Hibernate `create-drop` (Flyway off), so these columns won't exist in the test
   schema — anything that maps/queries them later needs test-schema coverage.
4. **`EntityRef` is Long-id only**, so a `CREATE_POSITION` action can't reference an
   issue created earlier *in the same result* (no DB id yet). The plan's own lexical
   example (`IssueRef.byActionKey(...)`) needs an action-key reference. Add an
   action-key variant before wiring the applicator, or the lexical adapter can't
   express issue+position in one result.
5. **AI output not length-capped.** `maxInputTokens` is only an advisory prompt hint,
   not enforced; output field/array lengths are unbounded. Plan asks for size caps
   and "cap lengths." `projectAllowlist` is read by nothing yet.
6. **MCP JSON-RPC error codes imprecise** — unknown method / bad params surface as
   `-32603 INTERNAL_ERROR` instead of `-32601` / `-32602`. Minor protocol nit.
7. **Orphaned persistence.** `AssistantFinding*` and `AssistantUsage*` are referenced
   only within `persistence/`; `findings_count` is never incremented. Expected, since
   the applicator is the consumer — just flagging they're dead code until then.

---

## Recommendation

Don't jump straight to Phase 5. The foundation modules are good, but the
"command → dispatch → assistant → annotation" loop has never run end-to-end. I'd
close that loop first, in roughly this order:

1. Fix `JpaAssistantRunStore` round-trip so `AnalysisRequest` (refs + attributes +
   locale) survives queue→read-back. Add locale to `AnalysisRequest`.
2. Promote `evidence` to a typed field on `AnnotationAction` and add an action-key
   reference variant (or `IssueRef`) so in-result issue→position links work.
3. Implement the real `AssistantResultApplicator` over `CommandHandler` +
   `AnnotationCommandFactory` with idempotency-key lookup and execute-as-triggering-
   user; map the new annotation columns in `annotation-jpa`.
4. Wrap one legacy assistant (lexical) as a `RequelAssistant<TextEntity>` adapter and
   route one `analyzeX` path through `dispatch(...)` — proving migration steps 5–6 on
   a single path with an integration test asserting the same observable annotations.
5. Then layer the finding state machine + per-assistant cleanup policy.
6. Decide explicitly whether `nlp-optional-plan` Scope 1 ships in #43 or a later
   issue, and update that doc's status either way.

MCP (auth + remaining surfaces) and the Phase 5 AI assistant can proceed in parallel
once the applicator exists, since both ultimately depend on it (`draft_annotation`
and AI findings both become `AnnotationAction`s the applicator persists).

Also update the plan docs for the `assistant-ai` rename and final MCP tool names so
the docs and code agree before the next phase.

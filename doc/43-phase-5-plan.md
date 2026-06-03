# Phase 5 — First AI Assistant (REQUIREMENTS_REVIEW)

Issue #43. Phase 4.5 closed the command → dispatch → assistant → annotation loop on the
legacy lexical path and left the AI provider layer (`assistant-ai` / `assistant-openai`) and
context packs in place but **unwired**. Phase 5 ships the first AI-backed assistant:
`RequirementsReviewAssistant`, a `RequelAssistant<TextEntity>` that reviews a requirement
entity for quality issues via the provider abstraction and returns annotation drafts.

This doc consolidates the fine-grained slices; the master `doc/ai-assistance-plan.md`
(Phase 5) and `doc/assistant-spi-plan.md` remain the design references.

## What already exists (Phase 2 + Phase 4)

- Provider seam: `AiAnalysisClient.analyze(AiAnalysisRequest) : AiAnalysisResponse`
  (`assistant-ai`). Models: `AiAnalysisRequest` (assistantId, runId, taskType, targetRef,
  projectRef, locale, contextPacks, outputSchemaName/Version/schema, dataHandlingFlags,
  attributes), `AiAnalysisResponse` (summary, structuredOutput, `List<AiFindingDraft>`,
  messages, `AiUsage`, providerMetadata), `AiFindingDraft` (findingType, severity, confidence,
  evidenceReferences, suggestedIssueText, suggestedNoteText, suggestedPositions, metadata).
- `NoopAiAnalysisClient` (`@ConditionalOnMissingBean`, default — empty findings) and
  `OpenAiAnalysisClient` (`@ConditionalOnProperty requel.ai.provider=openai`, native HttpClient,
  structured output, retries, usage capture).
- `AiProperties` (`requel.ai.*`: enabled, provider, model, endpoint, timeout, maxRetries,
  maxInputTokens, maxOutputTokens, projectAllowlist, apiKey/env).
- Context packs + builders (`EntityContextPack`, `ProjectContextPack`, `IssueContextPack`),
  `ContextPackSizeLimits` / budget, `RedactionPolicy` (+ `NoOpRedactionPolicy`).
- Persistence: `AssistantRunEntity` (taskType, provider, model, template*, bodyCapture*),
  `AssistantUsageEntity` + `AssistantUsageRepository`.
- Applicator untrusted-output handling: text caps (`MAX_TEXT_LENGTH=4000`,
  `MAX_SUMMARY_LENGTH=500`), unknown action types skipped, per-assistant error isolation.

## Trigger model (decided)

**Manual trigger only for the first cut.** A `REQUIREMENTS_REVIEW` run is dispatched
explicitly for a chosen entity; the AI assistant **self-gates** to that task type and never
runs on ordinary post-edit saves (so the lexical post-edit path and provider cost/latency stay
separate). Auto-on-edit for opted-in projects is a later enhancement that builds on the same
gate. `SimpleAssistantRegistry` matches by `targetType` only, so the assistant must skip
(return an empty `AssistantResult`) unless its run is a `REQUIREMENTS_REVIEW` run.

## Slices

### 1. Thread `taskType` into `AssistantContext` (prereq)

`AnalysisRequest` and `AssistantRunEntity` carry `taskType`, but `AssistantContext` does not,
so an assistant cannot tell a `REQUIREMENTS_REVIEW` run from an ordinary edit. Add `taskType`
to `AssistantContext` (nullable String) and populate it in `AssistantRunWorker` from the
run's request. Existing assistants ignore it (no behaviour change). Tests: context carries the
task type; worker round-trips it.

### 2. `RequirementsReviewAssistant` skeleton, gated, Noop end-to-end

New `RequelAssistant<TextEntity>` (`assistantId = "ai-requirements-review"`,
`targetType = TextEntity.class`) in a new module `assistant-ai-review` (or `assistant-ai`)
that:
- returns an empty result unless `context.taskType() == "REQUIREMENTS_REVIEW"` **and**
  `requel.ai.enabled` **and** the project is allowed (allowlist empty = all allowed);
- builds an `EntityContextPack` (+ project/issue packs as budget allows) with redaction;
- assembles an `AiAnalysisRequest` (taskType, refs, packs, output schema name/version) and
  calls the injected `AiAnalysisClient`;
- maps the (empty, under Noop) response to an `AssistantResult` and returns it.
Tests (Noop client): an ordinary Goal edit run does **not** invoke it (skips); a
`REQUIREMENTS_REVIEW` run completes `SUCCEEDED` with zero findings.

### 3. `AiFindingDraft` → `AnnotationAction` mapping

Convert each draft to annotation actions: `suggestedIssueText` →
`CREATE_OR_UPDATE_ISSUE` (with `mustResolve`/severity/confidence + finding metadata),
`suggestedNoteText` → `CREATE_OR_UPDATE_NOTE`, `suggestedPositions` → child positions under the
issue; deterministic `actionKey` per finding for idempotency; evidence carried through. Tests
use a **fake `AiAnalysisClient`** bean returning sample `AiFindingDraft`s and assert the
applicator creates the expected annotations (no real provider).

### 3b. Task routing (added prerequisite)

`SimpleAssistantRegistry` matches by target type only, so a `REQUIREMENTS_REVIEW` run would
also invoke the lexical assistants (and they would otherwise run on the AI task). Added
`RequelAssistant.handlesTask(taskType)` (default: serves the `null`/post-edit task); the worker
filters matches by it (no handler → run `SKIPPED`). `RequirementsReviewAssistant` overrides it
to serve only `REQUIREMENTS_REVIEW`. This cleanly separates the manual review run (AI only)
from ordinary edits (lexical only) and must land before the manual trigger.

### 4. Output schema + manual trigger

- Add the `REQUIREMENTS_REVIEW` JSON output schema as a classpath resource
  (`outputSchemaName`/`outputSchemaVersion`), supplied on the `AiAnalysisRequest` so the
  provider can enforce structured output.
- Add a manual dispatch path (CQRS command or a small endpoint) that dispatches a
  `REQUIREMENTS_REVIEW` `AnalysisRequest` for a given entity (authorized as the triggering
  user). Integration test drives manual trigger → run → (Noop) result.

### 5. Usage persistence + input cap + output bounds

- Persist an `AssistantUsageEntity` per run from `AiAnalysisResponse.usage`
  (provider/model/tokens/latency/cost); body capture per the retention flags (metadata-only by
  default).
- Enforce `requel.ai.maxInputTokens` (cap/refuse oversize context packs) and bound AI output
  field lengths before they reach the applicator (review concern #5).

### 6. OpenAI wiring + project opt-in + gated e2e

- Config to select `requel.ai.provider=openai` + model + `projectAllowlist`; project opt-in
  gate (allowlist now; `AssistantProjectSettings` later).
- End-to-end test against the real provider, gated behind `OPENAI_API_KEY` (skipped when
  absent) so CI stays green offline.

## Out of scope for Phase 5

- Auto-on-edit dispatch for opted-in projects (later enhancement on the same gate).
- `AssistantPromptTemplate` DB-override table (classpath schema/prompt only for now).
- The MCP session-token + dual-identity flow (deferred with external AI rollout).
- A second AI task type (chosen after this pipeline is proven end-to-end).

## Exit criteria

1. A manually dispatched `REQUIREMENTS_REVIEW` run for a `TextEntity` flows
   target → context packs → `AiAnalysisClient` → `AnnotationAction`s → annotations.
2. The AI assistant does **not** run on ordinary post-edit dispatches.
3. With a fake client returning findings, the expected issue/note/position annotations are
   created; re-running is idempotent.
4. With Noop (default) the run succeeds with zero findings; with `requel.ai.enabled=false` the
   assistant skips.
5. `mvn clean verify` green across the reactor.

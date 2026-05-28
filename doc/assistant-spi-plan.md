# Assistant SPI Plan

## Purpose

Create a stable assistant extension point that supports the existing NLP/lexical assistants, future rules-based assistants, and AI-backed assistants described in `doc/ai-assistance-plan.md`.

The SPI should preserve Requel's current review workflow: assistants produce notes, issues, positions, messages, and draft actions; Requel applies accepted state changes through existing commands, authorization, audit, and notification paths.

## Alignment With Current Architecture

This plan has been updated for the modularized Spring Boot 3 / Java 17 direction:

- Domain and persistence modules are already split into `project-domain`, `project-jpa`, `annotation-domain`, `annotation-jpa`, `user-domain`, `user-jpa`, `platform-core`, and `platform-identity`.
- `service-api` and `service-impl` now provide authenticated REST APIs, command dispatch, query DTOs, audit logging, current-user resolution, and SSE updates.
- Assistant code still lives in `project-jpa` under `com.rreganjr.requel.project.impl.assistant`, but that is no longer the right long-term boundary. Analysis should move behind a separate assistant/analysis module boundary.
- AI/MCP work should build on the SPI rather than bypass it. MCP exposes controlled context retrieval; AI assistants consume context and return structured `AssistantResult`s.

## Goals

- Allow internal modules and third-party modules to contribute assistants without changing command implementations or `AssistantFacade`.
- Keep existing annotation-based assistant behavior working while moving it behind a result/action model.
- Support multiple analysis implementations: legacy NLP, deterministic rules, API-backed AI, local models, and no-op/fake test assistants.
- Keep project/domain modules free of NLP, AI provider, MCP, and UI dependencies.
- Apply all persisted changes through command handlers so authorization, validation, audit, optimistic locking, and SSE behavior remain consistent.
- Provide idempotency so repeated background runs update or reuse findings rather than creating duplicate issues.
- Keep the first AI integrations suggestion-only and annotation-centric; direct state mutation and external actions require explicit governance.

## Non-Goals For The First SPI Pass

- Generic autonomous agents that can freely inspect or mutate Requel.
- Direct database writes from assistants.
- Direct execution of external actions such as email, calendar, Jira, or GitHub updates.
- Provider-specific OpenAI/Codex APIs in the SPI module.
- UI-heavy assistant management. The first useful surface can be annotations plus run/activity history.

## Current State

- Analysis is invoked after successful edit commands by `AnalysisInvokingCommandHandler`, which calls `AnalyzableEditCommand.invokeAnalysis()`.
- Project command implementations call specific `AssistantFacade` methods such as `analyzeGoal`, `analyzeStory`, `analyzeActor`, `analyzeUseCase`, `analyzeScenario`, and `analyzeProject`.
- After issue #39, `AssistantFacade` is a thin async submission and notification layer only. It submits work onto `assistantTaskExecutor` and calls `UpdatedEntityNotifier` after each task completes. The analysis bodies themselves now live in `AssistantTaskRunner`, a `@Component` whose entry points are reached through a Spring transactional proxy. The runner reloads target entities in a new transaction, resolves the `assistant` pseudo-user, and constructs `LexicalAssistant` plus the concrete entity assistants per call. Issue #43 work must preserve this transactional-proxy boundary when migrating from `AssistantTaskRunner` to `AssistantDispatcher` so lazy Hibernate collections continue to have an active session during analysis.
- Assistants live in `project-jpa`:
  - `AbstractAssistant`
  - `LexicalAssistant`
  - `ProjectOrDomainEntityAssistant`
  - `TextEntityAssistant`
  - `GoalAssistant`, `StoryAssistant`, `ActorAssistant`, `UseCaseAssistant`, `ScenarioAssistant`, `ScenarioStepAssistant`, `ProjectAssistant`
- `AbstractAssistant` contains helper methods that directly create annotations via `AnnotationCommandFactory` and `CommandHandler`.
- `TypedAssistant<T>` exists but is effectively unused; the concrete assistants do not form a real SPI.
- Feedback is mostly persistent annotations: notes, issues, and positions.
- The service layer now has command audit and SSE infrastructure that assistant activity should reuse or integrate with.

## Target Module Shape

Decided shape (issue #43):

- `assistant-api`
  - Stable SPI contracts and simple value objects.
  - Depends only on `platform-core`, `platform-identity`, and interface types from `project-domain` / `annotation-domain` (per resolved decision; JPA modules remain forbidden).
  - Must not depend on `project-jpa`, `annotation-jpa`, `user-jpa`, `nlp-jpa`, `service-impl`, OpenAI SDKs, or Spring MVC.
- `assistant-core`
  - Dispatcher, registry, result applicator, run/activity persistence, idempotency support, finding state machine, and default no-op/fake implementations.
  - Integrates with `CommandHandler`, `AnnotationCommandFactory`, repositories, and transaction management.
  - Hosts the migrated `AssistantTaskRunner` body (refactored into `AssistantDispatcher` and `AssistantRunWorker`) so the issue #39 transactional-proxy boundary is preserved.
  - **May depend on `service-api`** (query interfaces and projection DTOs) so context-pack builders can read project state through stable, authenticated query contracts. **Must not depend on `service-impl`** (REST controllers, Spring MVC, JWT plumbing). If REST-shaped DTOs ever evolve in ways that are awkward for AI input, the migration path is to introduce a small `AssistantDataGateway` interface in `assistant-core` with an adapter in `service-impl` — analogous to the `ProjectQueryGateway` pattern used by `mcp-server`. That gateway is not introduced up front; the current direct dependency is acceptable because the context-pack builders translate `service-api` DTOs into assistant-shaped context packs (size caps, redaction, version stamps) before any provider call.
- `assistant-legacy-nlp` (also acceptable as `project-analysis` if other lexical/NLP code moves with it)
  - Adapters wrapping the existing `LexicalAssistant`, `TextEntityAssistant`, and concrete entity assistants as `RequelAssistant<T>` implementations.
  - Depends on project/annotation APIs and the NLP module, not the other way around.
- `assistant-openai`
  - Provider-specific `AiAnalysisClient` implementations and AI-backed assistants.
  - Depends on `assistant-api`, context-pack DTOs/services, and the provider SDK/client code.
- `mcp-server`
  - MCP protocol handlers, resources, and tools.
  - Bundled in-process with `requel-app` for the first cut. Every tool/resource calls a `ProjectQueryGateway` abstraction so a future standalone bridge can drop in a REST-backed gateway plus auth exchange without rewriting the tools. Must not become a dependency of `assistant-api`.

`assistant-api` and `assistant-core` are introduced together in the first implementation branch. `assistant-legacy-nlp`, `assistant-openai`, and `mcp-server` follow in later phases per `doc/ai-assistance-plan.md` rollout.

## Proposed SPI

### Core Contracts

```java
public interface RequelAssistant<T> {
    String assistantId();
    Class<T> targetType();
    AssistantResult analyze(AssistantContext context, T target) throws AssistantException;
}
```

Prefer passing context and target to `analyze(...)` rather than mutable `setContext` / `setTarget` methods. Stateless assistants are easier to run concurrently, test, and register as singletons.

```java
public interface AssistantRegistry {
    List<RequelAssistant<?>> findAssistantsFor(Object target, AssistantContext context);
}
```

```java
public interface AssistantDispatcher {
    CompletionStage<AssistantRunHandle> dispatch(AnalysisRequest request);
}
```

The SPI can remain synchronous at the assistant boundary. The dispatcher owns asynchronous execution, transactions, retries, cancellation, and run status.

### AssistantContext

`AssistantContext` should carry references and capabilities, not persistence objects that force JPA coupling:

- run id
- triggering user reference
- assistant user reference
- project/domain reference
- locale
- clock
- assistant configuration flags
- data-handling flags, such as external AI allowed
- optional service/capability access by interface

Use `platform-identity` identity abstractions or a small `UserRef` instead of `user-jpa` entities. If domain objects are passed, use domain interfaces from `project-domain` / `annotation-domain`, not JPA implementations.

### AssistantResult

`AssistantResult` should contain normalized outputs:

- `summary`
- `severity`
- `assistantId`
- `runId`
- `List<AnnotationAction>`
- `List<AssistantMessage>`
- `List<ExternalAction>`
- `metadata`, such as model/provider, elapsed time, usage, and confidence

Example:

```java
AssistantResult result = AssistantResult.builder()
    .assistantId("legacy-lexical")
    .summary("Lexical analysis completed")
    .annotationAction(AnnotationAction.createIssue(
        "legacy-lexical:Goal:42:unknown-word:datalaek",
        EntityRef.goal(42L),
        "Unknown word 'datalaek'",
        true))
    .annotationAction(AnnotationAction.createPosition(
        "legacy-lexical:Goal:42:unknown-word:datalaek:add-to-glossary",
        IssueRef.byActionKey("legacy-lexical:Goal:42:unknown-word:datalaek"),
        "Add 'data lake' to glossary"))
    .message(AssistantMessage.info("Candidate glossary term: data lake"))
    .build();
```

### AnnotationAction

`AnnotationAction` replaces direct calls from assistants to `AnnotationCommandFactory`.

Required fields:

- stable action key
- action type: create/update/remove note, issue, position, argument, or annotation link
- target entity reference
- grouping/project reference
- text and structured payload
- severity/must-resolve/confidence where relevant
- evidence references

The result applicator applies actions through existing command factories and `CommandHandler`. It must be idempotent:

- reuse/update an existing annotation when action key matches
- avoid duplicate issue text on the same annotatable where older annotations lack keys
- optionally resolve/remove stale assistant-owned findings when a later run omits them

### AssistantMessage

`AssistantMessage` is for non-persistent run information:

- informational summaries
- provider warnings
- analysis skipped messages
- configuration or privacy notices

Messages should be persisted in assistant run/activity history if they matter after the background thread ends.

### ExternalAction

`ExternalAction` is a draft side-effect intent. It is not executed automatically in the first implementation.

Examples:

- create Jira issue
- send email
- schedule meeting
- post webhook

External actions need an approval queue, authorization policy, audit trail, and per-integration execution adapters before being enabled.

## Integration Pattern

### Dispatch

Move from command-specific `invokeAnalysis()` methods toward central analysis requests:

1. Commands execute and commit successfully.
2. The command handler, command result, or domain/application event exposes `AnalysisRequest` targets.
3. `AssistantDispatcher` queues work after commit.
4. Dispatcher reloads target entities in its own transaction.
5. Dispatcher resolves enabled assistants for the target and context.
6. Results are validated and applied.
7. Run/activity records are written.
8. `UpdatedEntityNotifier` and/or `StreamEventPublisher` publishes affected entity updates.

### Dispatcher transactions and async boundaries

Async execution and transactional execution are split across two collaborating beans so the Spring proxy can open a new transaction on the executor thread, not on the caller thread:

- `AssistantDispatcher.dispatch(AnalysisRequest)` is the public entry point. It validates the request, persists a `QUEUED` `AssistantRun` row, hands work to `assistantTaskExecutor`, and returns an `AssistantRunHandle`. **`dispatch(...)` is not transactional and does not touch entity state directly.**
- The executor invokes `AssistantRunWorker.runInNewTransaction(runId)` on a separate Spring bean. That method is annotated `@Transactional(propagation = REQUIRES_NEW)` so the Spring proxy opens a fresh Hibernate session on the executor thread. The worker reloads targets via the appropriate repository, resolves the configured assistants, invokes them, and hands their `AssistantResult`s to `AssistantResultApplicator`.
- The worker is a distinct bean from the dispatcher so the proxy can intercept the call. A `@Transactional` self-invocation from inside `AssistantDispatcher.dispatch(...)` would bypass the proxy and lose the new transaction. Issue #39 was specifically about preserving this boundary; the dispatcher/worker split keeps it intact.
- `AssistantResultApplicator.apply(...)` calls `CommandHandler` inside that same `REQUIRES_NEW` transaction so authorization, validation, audit, optimistic locking, and SSE all behave consistently.

Short-term compatibility (migration from `AssistantTaskRunner`):

- Keep `AnalyzableEditCommand.invokeAnalysis()` as a bridge.
- Have existing `invokeAnalysis()` methods create `AnalysisRequest`s instead of directly calling type-specific facade methods.
- Mark the type-specific `AssistantFacade.analyzeX` methods as transitional.
- Introduce `AssistantDispatcher` and `AssistantRunWorker` per the boundary above. The worker replaces the body that currently lives in `AssistantTaskRunner`; the existing `AssistantTaskRunner` is the closest reference for the transactional-proxy pattern that must be preserved.
- Migrate one `analyzeX` path at a time. `AssistantFacade` continues to submit work onto `assistantTaskExecutor`; what it submits gradually changes from "analyze this entity by type" to "dispatch this `AnalysisRequest`". When every path has been migrated, the type-specific `analyzeX` methods are removed and `AssistantFacade` is collapsed into the dispatcher (or a small backwards-compat adapter).
- The legacy assistant implementations are wrapped as `RequelAssistant<T>` adapters in `assistant-legacy-nlp` (or `project-analysis`); their direct `AnnotationCommandFactory` calls move into `AnnotationAction` results applied by `AssistantResultApplicator`.

### Registry And Discovery

Support both:

- Spring bean discovery for the application.
- Container-neutral registration for CLI, tests, batch jobs, and future local MCP bridge usage.

A small in-memory registry plus optional `ServiceLoader` support keeps the SPI usable outside the web application.

### Result Application

Create an `AssistantResultApplicator` in an implementation module:

```java
public interface AssistantResultApplicator {
    AppliedAssistantResult apply(AssistantContext context, AssistantResult result);
}
```

The applicator:

- validates action schema and text length
- rejects stale entity versions
- enforces idempotency
- invokes annotation/project commands through `CommandHandler`
- records created/updated annotation ids
- emits notifications

## AI And MCP Alignment

The SPI is the foundation for `doc/ai-assistance-plan.md`:

- AI-backed assistants implement `RequelAssistant<T>`.
- Provider clients live behind an `AiAnalysisClient`, not in the SPI.
- Context packs use DTO/reference serializers so AI providers do not receive JPA graphs.
- MCP resources/tools expose controlled project context to AI clients; MCP does not replace `AssistantResult` or command-handler application.
- AI output is treated as untrusted input and must be structured, schema-validated, length-capped, and applied only through `AnnotationAction`/draft action paths.

The first AI-backed assistant should return annotation actions only. Automatic project edits and external actions can wait until activity history, approval, and governance are in place.

## Legacy NLP Migration

1. Wrap current assistants behind `RequelAssistant<T>` adapters without changing their behavior.
2. Move direct annotation creation out of assistant logic and into `AnnotationAction` results.
3. Extract lexical analysis behind a `TextAnalysisService` or `LexicalAnalysisService` interface.
4. Provide an NLP-backed implementation using the current `NLPProcessorFactory`.
5. Provide `NoopTextAnalysisService` for disabled NLP profiles and tests.
6. Move NLP-dependent code out of `project-jpa` when practical, following the modularization plan's `project-analysis` direction.

This should allow NLP to be optional and replaceable without forcing `project-domain` or command implementations to depend on NLP classes.

## Activity, Audit, And UI

Assistant runs should be visible enough to debug and govern. Concrete persistence is detailed in `Data Model and Applicator Contract` below; the high-level commitments are:

- `AssistantRun` rows are written for every dispatch, including skipped and failed ones, so a reviewer can always answer "did the assistant run, and what did it do?"
- `AssistantFinding` rows carry the idempotency key, evidence, applied-annotation pointer, and finding state. Annotation rows additionally carry the idempotency key for fast reverse lookup and a source label.
- Reuse or link to `CommandAuditLog` for command applications, but do not rely on command audit alone to explain skipped/failed AI runs — assistant activity must be readable on its own.
- Expose run history through service APIs before enabling external AI broadly.
- Use annotations as the first UI surface. Later, Angular can add project assistant settings, run history, and manual run controls.

Echo-specific UI work is no longer the primary planning target. New user-facing assistant controls should be designed for Angular/service APIs.

## Data Model and Applicator Contract

This section locks down the implementation specifics the review called out as missing before code starts.

### Persistence

`AssistantRun` (one row per dispatched run, including skipped/failed):

- `id` (uuid)
- `assistant_id` (e.g. `legacy-lexical`, `requirements-review-openai`)
- `assistant_user_id` (which pseudo-user attribution should use; one of `requel-ai-assistant`, `requel-legacy-nlp`, or a per-client external pseudo-user)
- `triggered_by_user_id`
- `project_id`, `target_type`, `target_id`
- `task_type` (nullable for non-AI runs; e.g. `REQUIREMENTS_REVIEW` for AI)
- `provider`, `model`, `template_id`, `template_version`, `template_source` (`resource` / `db_override:<row_id>` / `null`)
- `status` (`QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `SKIPPED`, `CANCELLED`)
- `started_at`, `completed_at`, `latency_ms`
- `error_kind`, `error_summary`
- `findings_count`
- `body_capture_reason` (`null`, `failure`, `project_capture_window`, `user_opt_in`, `sampled`)
- `body_retained_until` (when body capture is in effect)

`AssistantFinding` (one row per logical finding, surviving across runs via the idempotency key):

- `id` (uuid)
- `idempotency_key` = `assistantId + ':' + targetType + ':' + targetId + ':' + findingType + ':' + normalizedEvidenceHash`
- `assistant_id`
- `project_id`, `target_type`, `target_id`
- `finding_type` (free-form per assistant, e.g. `unknown-word`, `ambiguous-language`, `missing-actor`)
- `severity`, `confidence`
- `summary`
- `evidence_json`
- `applied_annotation_id` (nullable; set when a `NOTE`/`ISSUE` annotation was created)
- `state` (`ACTIVE`, `SUPERSEDED`, `AUTO_RESOLVED`, `MANUALLY_RESOLVED`, `DROPPED`)
- `created_run_id`, `last_seen_run_id`, `superseded_by_run_id`
- `created_at`, `last_seen_at`, `closed_at`

`AssistantUsage` (one row per provider call; an AI-backed `AssistantRun` may have one usage row per call):

- `id` (uuid)
- `run_id` → `AssistantRun`
- `provider`, `model`
- `input_tokens`, `output_tokens`, `cached_input_tokens`
- `cost_estimate`
- `latency_ms`
- `request_body` (nullable; populated only when `body_capture_reason` is non-null and TTL hasn't elapsed; encrypted at rest with a separate key)
- `response_body` (same)

Annotation rows are extended with two new fields:

- `assistant_idempotency_key` (nullable, indexed) — duplicates the value on `AssistantFinding` for fast reverse lookup.
- `source` (`HUMAN` / `ASSISTANT:<assistant_id>`) — drives source labeling in the UI without joining `AssistantFinding`.

Optional `AssistantProjectSettings`:

- `project_id`
- `ai_enabled` (per resolved decision: project-scoped only in Phase 1; global default + project override layered in later)
- `external_provider_allowed`
- `allowed_assistant_ids`
- `body_capture_window_until` (nullable; for the per-project capture-window opt-in)
- `body_capture_user_opt_in_ceiling` (max per-run user opt-ins allowed per day/week)
- budget settings

### Finding State Machine

Each `AssistantFinding` row moves through a small state machine. Transitions are owned by `AssistantResultApplicator`:

- `ACTIVE` → `SUPERSEDED`: a later run with the same `(assistant_id, target, finding_type)` and a *different* evidence hash, when the assistant's cleanup policy is `MARK_SUPERSEDED`. The applicator stamps `superseded_by_run_id`, posts a system position on the linked annotation noting the supersession, and leaves the annotation open.
- `ACTIVE` → `AUTO_RESOLVED`: a later run with the same `(assistant_id, target, finding_type)` omits this finding *and* the linked annotation has no human edits, replies, or non-assistant positions, *and* the assistant's cleanup policy is `AUTO_RESOLVE_IF_UNTOUCHED`. The applicator closes the annotation through the appropriate annotation command, stamps `closed_at`.
- `ACTIVE` → `MANUALLY_RESOLVED`: human user resolved the linked annotation. The applicator listens for annotation-close events and stamps the finding accordingly; the finding row stays for audit.
- `ACTIVE` → `DROPPED`: applicator rejected the finding at apply time (schema violation, stale entity version, length cap exceeded). No annotation is created. `error_summary` on the run records why.
- `ACTIVE` → `ACTIVE` (touched): a rerun of the *same* finding (same idempotency key) updates `last_seen_run_id` and `last_seen_at` only; no annotation churn.

Assistants whose cleanup policy is `MANUAL` skip both `SUPERSEDED` and `AUTO_RESOLVED` transitions; the applicator only ever moves them between `ACTIVE` and `DROPPED` / `MANUALLY_RESOLVED`.

### AssistantResultApplicator Command Mapping

The applicator never writes directly through repositories. Every `AnnotationAction` it accepts is translated into a call through `CommandHandler` using the existing `AnnotationCommandFactory` (in `annotation-domain`). The factory follows an "Edit" naming convention for both create and update — the same `Edit*Command` instance handles a fresh annotation (no id) and an existing one (id set). This preserves authorization, audit, optimistic locking, and SSE:

| `AnnotationAction` kind | Existing factory method |
| --- | --- |
| Create or update a note | `newEditNoteCommand()` |
| Delete a note | `newDeleteNoteCommand()` |
| Create or update a general issue | `newEditIssueCommand()` |
| Create or update a lexical issue (glossary/spelling/unknown-word) | `newEditLexicalIssueCommand()` |
| Resolve an issue | `newResolveIssueCommand(Position)` (Position determines the resolve variant) |
| Delete an issue | `newDeleteIssueCommand()` |
| Create or update a general position | `newEditPositionCommand()` |
| Create or update a "change spelling" position | `newEditChangeSpellingPositionCommand()` |
| Create or update an "add word to dictionary" position | `newEditAddWordToDictionaryPositionCommand()` |
| Delete a position | `newDeletePositionCommand()` |
| Create or update an argument | `newEditArgumentCommand()` |
| Delete an argument | `newDeleteArgumentCommand()` |
| Remove an annotation from a specific annotatable (keep elsewhere) | `newRemoveAnnotationFromAnnotatableCommand()` |

Notes:

- The `AnnotationAction` value types in `assistant-api` are kept abstract enough that the applicator can pick the right factory variant from action metadata. For example, a `CreatePosition` action against a lexical issue with `kind = CHANGE_SPELLING` resolves to `newEditChangeSpellingPositionCommand()`, not `newEditPositionCommand()`. The action type itself does not name the factory method.
- For updates, the applicator looks up the existing annotation via the idempotency key, populates the `Edit*Command` input with the existing id and the existing optimistic-lock version, then sets new field values. The command implementation handles validation, audit, and SSE.
- `AUTO_RESOLVE_IF_UNTOUCHED` cleanup uses `newResolveIssueCommand(Position)` when a resolve position exists, and `newRemoveAnnotationFromAnnotatableCommand()` when configured to remove the annotation from the target instead. Hard delete via `newDelete*Command()` is reserved for assistant lifecycle operations (e.g., clearing assistant-owned findings from a target during a project reset), never for normal stale-finding cleanup.
- Issues, positions, and arguments that map to the IBIS layer use the existing `Annotatable` discriminator dispatch through `AnnotatableTypeRegistry`; the applicator never builds annotation entities directly.

### Execution Identity and Authorization

The applicator does not introduce new command-handler infrastructure. Per the resolved cleanup decision:

- Commands are executed as the **triggering user** via the existing `AuthorizingCommandHandler` chain. Authorization mirrors what the triggering user could do through the REST API. An AI assistant cannot cause a write the triggering user could not have made themselves.
- Assistant attribution lives on **separate fields**, not on the command-handler "executing user":
  - `AssistantRun.assistant_user_id` records which assistant pseudo-user the run belongs to (`requel-ai-assistant`, `requel-legacy-nlp`, or a per-client external pseudo-user).
  - The new `source` column on the annotation row carries `ASSISTANT:<assistant_id>` (or `HUMAN` for non-assistant writes), so source labeling does not need to join `AssistantFinding` or `AssistantRun`.
  - `created_by` on the annotation row stays as the triggering user, which is also the row's optimistic-lock owner.
- If a future use case requires writes attributable in `CommandAuditLog` to the assistant pseudo-user (rather than the triggering user), the migration path is to extend the command metadata with a separate `executedAs` field. That is deferred until a concrete need surfaces; it is not in scope for the first implementation branch.

### Context Pack DTO Ownership

Context pack DTOs (`ProjectContextPack`, `EntityContextPack`, `IssueContextPack` from `doc/ai-assistance-plan.md`) live in `assistant-core`, not `service-api`.

Rationale: these DTOs are tuned for assistant input (size caps, redaction flags, version-id stamps, evidence references) and should not be coupled to REST response shapes that change for UI reasons. `service-api` already exposes the projection DTOs that the context-pack builders consume; the context-pack builders in `assistant-core` produce assistant-specific shapes from those. If a third format is needed (e.g. an `mcp-server` resource representation), it lives in `mcp-server` and is built from the same `service-api` projections, not from the context packs.

### No-op Behavior

When NLP or AI is disabled, `RequelAssistant` implementations return **explicit empty results** (`AssistantResult.builder().assistantId(...).summary("disabled").build()`), not `null`. The applicator must treat empty results as "this run produced no findings" rather than as an error, so that `requel.nlp.enabled=false` or `requel.ai.enabled=false` deployments still record clean `AssistantRun` rows. See also `doc/nlp-optional-plan.md` for the NLP-side no-op contract.

## Security And Privacy

- Assistant execution must respect project access rules and command authorization.
- External AI must be disabled by default and enabled globally plus per project or allowlist.
- Use separate audit fields for triggering user and assistant user. Multiple assistant pseudo-users coexist (internal `requel-ai-assistant`, `requel-legacy-nlp`, and one per registered external MCP client) so audit and policy can distinguish them.
- Redact sensitive data before context packing, provider calls, or MCP responses.
- Provider request/response retention follows the resolved policy in `doc/ai-assistance-plan.md`: metadata only by default; bodies retained for failed runs, project-scoped capture windows, per-run user opt-in within a project ceiling, and a configurable success-sampling rate. All retained bodies have a hard TTL and are encrypted at rest with a separate key. `AssistantRun.body_capture_reason` records why bodies were retained for any given run.
- Add rate limits, concurrency limits, and provider budgets before broad AI rollout. Limits are enforced per assistant pseudo-user so external MCP clients can be capped independently.
- Treat assistant output as untrusted text: validate, cap, escape, and reject unknown action types.

## Testing Strategy

- Unit tests for registry matching and dispatcher target selection.
- Unit tests for result validation and idempotent annotation action application.
- Adapter tests proving legacy assistants produce the same observable annotations after migration.
- Fake `RequelAssistant` and `TextAnalysisService` implementations for deterministic tests.
- No-network profile for CI.
- Integration tests that apply assistant results through `CommandHandler` and verify audit/SSE side effects.
- MCP contract tests live with the MCP/service module, not in `assistant-api`.

## Minimal Incremental Steps

1. Create `assistant-api` and `assistant-core` modules with immutable SPI contracts, reference/value types, and the `AssistantRun`/`AssistantFinding`/`AssistantUsage` persistence shapes from `Data Model and Applicator Contract`.
2. Add `AssistantRegistry`, `AnalysisRequest`, and an `AssistantDispatcher` skeleton in `assistant-core`. Reach the dispatcher through a Spring transactional proxy that mirrors the issue #39 `AssistantTaskRunner` boundary (new transaction + entity reload before invoking assistants). `AssistantFacade` stays as a compatibility submission/notification facade during migration.
3. Add `AssistantResultApplicator` that applies `AnnotationAction`s through `CommandHandler` per the applicator command mapping above, including the finding state machine and per-assistant cleanup policy hooks.
4. Add idempotency keys, `assistant_idempotency_key` and `source` columns on annotations, and `AssistantRun`/`AssistantFinding`/`AssistantUsage` persistence.
5. Wrap one legacy assistant, preferably `LexicalAssistant`/text analysis, as a `RequelAssistant<TextEntity>` adapter inside `assistant-legacy-nlp`. Direct `AnnotationCommandFactory` calls move into `AnnotationAction` results.
6. Convert existing `analyzeX` paths in `AssistantFacade` / `AssistantTaskRunner` to dispatch `AnalysisRequest`s one at a time. Remove the `analyzeX` methods once every path is migrated.
7. Add no-op/fake assistants for tests and disabled-analysis profiles. Returns must be explicit empty `AssistantResult`s, not `null`.
8. Extract NLP-heavy code into `assistant-legacy-nlp` (or `project-analysis`) following the modularization plan.
9. Add AI-backed assistants and MCP support following `doc/ai-assistance-plan.md`.

## Resolved Decisions

Resolved during issue #43 walkthrough. See the full record at <https://github.com/rreganjr/Requel/issues/43#issuecomment-4560006774>, and the AI-side decisions in `doc/ai-assistance-plan.md`.

- **Module shape.** `assistant-api` + `assistant-core` from day one. AI/provider modules (`assistant-openai`, etc.) layered in later. `assistant-api` holds pure contracts and value types; `assistant-core` holds dispatcher, registry, applicator, run persistence, and legacy adapters.
- **SPI dependencies.** `assistant-api` may depend on `project-domain` and `annotation-domain` interfaces. JPA modules (`project-jpa`, `annotation-jpa`, `user-jpa`) are forbidden. This allows strongly-typed signatures such as `RequelAssistant<Goal>` and `RequelAssistant<Scenario>`.
- **Idempotency key storage.** Dedicated `AssistantFinding` table is the source of truth. The idempotency key is also stamped on the annotation row for fast reverse-lookup and source labeling.
- **Stale-finding cleanup.** Configurable per assistant. Each assistant declares one of `MANUAL`, `MARK_SUPERSEDED`, or `AUTO_RESOLVE_IF_UNTOUCHED` in its registration metadata. Default is `MARK_SUPERSEDED` for assistants that do not declare a policy.
- **Assistant settings scope.** Project-scoped only in Phase 1. Global-default + project-override added later. No organization/tenant tier planned.
- **MCP identity.** Both identities carried on every MCP session. Authorization uses the triggering user; an internal `requel-ai-assistant` pseudo-user plus one pseudo-user per registered external MCP client (e.g. `claude-code`, `codex-cli`) provides audit attribution and per-client policy/rate-limit control. Internal and external assistants share the same data model.
- **Legacy NLP coexistence.** AI is additive: legacy NLP and AI-backed assistants run in parallel. Legacy NLP stays enabled by default. Deprecation will be revisited once usage and overlap data are available; legacy checks are expected to fade naturally as new assistants displace them.


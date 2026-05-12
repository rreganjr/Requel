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
- `AssistantFacade` queues work on `assistantTaskExecutor`, reloads entities through `ProjectRepository`, finds the `assistant` pseudo-user, manually constructs `LexicalAssistant` and concrete entity assistants, and calls `UpdatedEntityNotifier`.
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

Preferred modules:

- `assistant-api`
  - Stable SPI contracts and simple value objects.
  - Depends only on `platform-core`, `platform-identity`, and possibly small reference types from `project-domain` / `annotation-domain`.
  - Must not depend on `project-jpa`, `annotation-jpa`, `user-jpa`, `nlp-jpa`, `service-impl`, OpenAI SDKs, or Spring MVC.
- `assistant-core` or `assistant-impl`
  - Dispatcher, registry, result applicator interfaces, run/activity model, idempotency support, and default no-op/fake implementations.
  - Can integrate with `CommandHandler`, `AnnotationCommandFactory`, repositories, and transaction management.
- `project-analysis` or `assistant-legacy-nlp`
  - Adapters for the existing NLP assistants and `LexicalAssistant` behavior.
  - Depends on project/annotation APIs and the NLP module, not the other way around.
- `assistant-openai` or `assistant-ai`
  - Provider-specific `AiAnalysisClient` implementations and AI-backed assistants.
  - Depends on `assistant-api`, context-pack DTOs/services, and provider SDK/client code.
- `mcp-server` or `service-impl` extension
  - MCP resources/tools backed by authenticated query/application services.
  - Should feed the AI context path but not become a dependency of `assistant-api`.

If creating all modules at once is too much churn, start with `assistant-api` plus implementation classes in `project-jpa`, but keep package names and dependencies shaped so extraction is mechanical.

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

Short-term compatibility:

- Keep `AnalyzableEditCommand.invokeAnalysis()` as a bridge.
- Have existing `invokeAnalysis()` methods create `AnalysisRequest`s instead of directly calling type-specific facade methods.
- Mark the type-specific `AssistantFacade.analyzeX` methods as transitional.

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

Assistant runs should be visible enough to debug and govern:

- Add an `AssistantRun` or `AssistantActivity` record with assistant id, target, status, triggering user, assistant user, timestamps, error summary, and produced annotation ids.
- Add `AssistantFinding` records or equivalent metadata if idempotency and evidence need more than annotation fields can hold.
- Reuse or link to `CommandAuditLog` for command applications, but do not rely on command audit alone to explain skipped/failed AI runs.
- Expose run history through service APIs before enabling external AI broadly.
- Use annotations as the first UI surface. Later, Angular can add project assistant settings, run history, and manual run controls.

Echo-specific UI work is no longer the primary planning target. New user-facing assistant controls should be designed for Angular/service APIs.

## Security And Privacy

- Assistant execution must respect project access rules and command authorization.
- External AI must be disabled by default and enabled globally plus per project or allowlist.
- Use separate audit fields for triggering user and assistant user.
- Redact sensitive data before context packing, provider calls, or MCP responses.
- Store provider request/response bodies only behind an explicit debug flag.
- Add rate limits, concurrency limits, and provider budgets before broad AI rollout.
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

1. Create `assistant-api` with immutable SPI contracts and reference/value types.
2. Add `AssistantRegistry`, `AnalysisRequest`, and dispatcher skeleton while keeping `AssistantFacade` as a compatibility facade.
3. Add `AssistantResultApplicator` that applies `AnnotationAction`s through existing commands.
4. Add idempotency keys and activity/run persistence.
5. Wrap one legacy assistant, preferably lexical/text analysis, as a `RequelAssistant<TextEntity>`.
6. Convert existing facade methods to dispatch `AnalysisRequest`s.
7. Add no-op/fake assistants for tests and disabled-analysis profiles.
8. Extract NLP-heavy code toward `project-analysis` or an assistant legacy NLP module.
9. Add AI-backed assistants and MCP support following `doc/ai-assistance-plan.md`.

## Open Decisions

- Module names: `assistant-api` plus `assistant-core`, or a single `assistant-spi` module first?
- Should `assistant-api` reference `project-domain` / `annotation-domain` types, or should it use only generic `EntityRef` / `ProjectRef` value objects?
- Where should idempotency keys be stored: annotation metadata, dedicated `AssistantFinding`, or both?
- How much stale-finding cleanup should be automatic versus manual?
- Should assistant settings be project-scoped only, or also organization/global scoped?
- Should MCP authenticate as the triggering user, the assistant user, or both with separate audit context?
- Should legacy NLP stay enabled by default after AI assistance is available, or become one optional assistant bundle?


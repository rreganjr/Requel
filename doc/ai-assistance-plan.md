# AI Assistance Plan

## Goal

Modernize Requel's background analysis so it can use a current AI model, such as an OpenAI Codex-class model through an API, while preserving the existing workflow where assistants create reviewable notes, issues, and positions on project data. Add MCP support so AI clients can query Requel through controlled, auditable tools instead of scraping the database or relying on oversized prompts.

This plan builds on `doc/assistant-spi-plan.md`. The SPI should remain the local extension point; AI-backed assistants should be one implementation family, not a special path wired directly into commands.

## Current State

- Analysis is invoked after successful edit commands by `AnalysisInvokingCommandHandler`, which calls `AnalyzableEditCommand.invokeAnalysis()`.
- Project commands call `AssistantFacade.analyzeGoal`, `analyzeStory`, `analyzeActor`, `analyzeUseCase`, `analyzeScenario`, or `analyzeProject`.
- `AssistantFacade` queues work on `assistantTaskExecutor`, reloads entities through `ProjectRepository`, creates the `assistant` pseudo-user, manually constructs `LexicalAssistant` and entity assistants, and notifies the UI when analysis is done.
- Existing assistants live in `project-jpa` and primarily perform NLP/lexical checks using the legacy `NLPProcessorFactory`, WordNet/OpenNLP-era resources, and annotation commands.
- The useful user-facing output is already review-oriented: annotations, issues, and positions. That is a good governance model for AI suggestions and should be retained.
- The Angular/API migration has introduced `service-api` and `service-impl` modules with authenticated REST endpoints, DTOs, command dispatch, query controllers, JWT auth, and SSE updates. Those modules are the right place to expose MCP-facing query/application services without coupling AI providers to JPA internals.

## Target Architecture

```
Command executed
  -> analysis request/event
  -> AssistantDispatcher
  -> local assistants and AI-backed assistants
  -> AssistantResult
  -> result applicator creates/updates annotations through commands
  -> audit/activity record
  -> SSE/UI refresh

External AI client or hosted model tool call
  -> Requel MCP server
  -> authorized project query/application services
  -> DTO/resource/tool response
```

The main design rule: AI may suggest, summarize, classify, and draft changes, but Requel applies state changes through the same command and authorization path used by the UI.

## Assistant SPI Foundation

Implement the assistant SPI before adding the AI provider:

- Create a small `assistant-api` or `assistant-spi` module with `RequelAssistant<T>`, `AssistantContext`, `AssistantResult`, `AssistantMessage`, `AnnotationAction`, and optional `ExternalAction`.
- Keep the SPI synchronous and deterministic from the assistant's perspective. `AssistantFacade` or a new `AssistantDispatcher` owns threading, transactions, retry policy, and result application.
- Apply annotation changes through existing command factories and `CommandHandler`, not direct repository writes.
- Give every finding a stable idempotency key: `assistantId + targetType + targetId + findingType + normalizedEvidenceHash`. Repeated runs should update or leave existing findings, not create duplicates.
- Keep `ExternalAction` as an approval-queue concept for later. Do not let the first AI integration send emails, create tickets, or call arbitrary webhooks automatically.
- Add an `AssistantActivity` table or audit record for runs, status, token/cost metadata, failure details, model/provider, and created/updated annotation ids.

## AI Provider Layer

Add an AI provider abstraction beneath assistant implementations:

```java
public interface AiAnalysisClient {
    AiAnalysisResponse analyze(AiAnalysisRequest request) throws AiAnalysisException;
}
```

The request should include:

- assistant id and run id
- task type, such as `REQUIREMENTS_REVIEW`, `GLOSSARY_CANDIDATES`, `SCENARIO_CONSISTENCY`, `ACTOR_GOAL_ALIGNMENT`, `OPEN_ISSUE_SUMMARY`
- target entity references and compact DTO snapshots
- relevant neighboring context, capped and selected by Requel
- output schema name and version
- locale, user/project permissions, and data handling flags

The response should be structured JSON validated by Requel before any action is applied:

- summary
- findings with severity, confidence, evidence references, suggested issue/note text, and suggested positions
- suggested commands only as drafts, never as directly executable operations
- provider metadata, usage, latency, and model id

Initial implementations:

- `NoopAiAnalysisClient` for local/dev/test.
- `OpenAiAnalysisClient` using the OpenAI Responses API with a configurable model. Official OpenAI docs currently describe Codex-class models as usable through the Responses API, and current model pages also expose structured outputs/function calling/MCP support depending on model. Keep the exact model id in configuration instead of hardcoding it in assistant logic.
- Optional later: `LocalModelAnalysisClient` for a self-hosted model if privacy or cost requires it.

Configuration:

- `requel.ai.enabled=false` by default until the workflow is proven.
- `requel.ai.provider=openai|noop|local`
- `requel.ai.model=<model id>`
- `requel.ai.timeout`, `requel.ai.maxInputTokens`, `requel.ai.maxOutputTokens`
- `requel.ai.projectAllowlist` or per-project opt-in flag
- credentials from environment or secret manager, never from checked-in properties

## AI Assistant Use Cases

Start with read-only suggestions that map naturally to annotations:

1. **Requirements quality review**
   - Find ambiguity, unverifiable language, missing actors, hidden assumptions, and conflicting terms.
   - Output issues with positions such as "Clarify wording", "Split requirement", or "Do nothing".

2. **Scenario review**
   - Check scenario steps for missing actor/action/object, inconsistent ordering, invalid references, and likely alternate flows.
   - Preserve the existing `ScenarioStepAssistant` behavior as a baseline, then add AI findings where the legacy parser is weak.

3. **Glossary candidate extraction**
   - Suggest domain terms from stories, goals, scenarios, and use cases.
   - Create reviewable issues/positions rather than automatically creating glossary terms.

4. **Project consistency summary**
   - Compare actors, goals, stories, and use cases for mismatches.
   - Emit a compact project-level summary and focused issues attached to the relevant entity.

5. **Open issue triage**
   - Summarize unresolved issues and suggest grouping or priority.
   - Useful for a future assistant management panel, but not required for first integration.

Avoid first:

- automatic edits to project entities
- direct database writes
- external actions
- long-running autonomous multi-step agents operating without a bounded target

## Context Selection

Do not send entire projects blindly. Build explicit context packs:

- `ProjectContextPack`: project metadata, glossary, actors, high-level goal/story/use-case summaries.
- `EntityContextPack`: target entity DTO, parent/child relationships, annotations, related glossary terms, nearby scenario steps.
- `IssueContextPack`: unresolved annotations for the target and project-level open issues.

Each pack should be serialized from `service-api` DTOs or dedicated assistant DTOs. This keeps AI input stable and prevents accidental traversal of JPA object graphs.

Context packing requirements:

- enforce per-run token/size limits
- redact fields marked sensitive
- include stable entity references for evidence
- include version ids so stale AI output can be rejected
- log pack metadata, not full project text by default

## MCP Support

Add a Requel MCP server so AI clients can query the project through least-privilege tools.

Two complementary paths are useful:

1. **In-process MCP endpoint in `service-impl`**
   - Uses existing authentication and project authorization.
   - Exposes tools/resources backed by query services and DTO mappers.
   - Best for hosted AI providers that support MCP tools and for the Angular/server deployment.

2. **Local MCP bridge process**
   - A small CLI or Spring Boot mode that connects to a local Requel instance via `/api`.
   - Useful for Codex CLI or developer workflows where the AI runs outside the server process.

Initial MCP resources:

- `requel://projects` - list visible projects
- `requel://projects/{projectName}` - project metadata
- `requel://projects/{projectName}/tree` - project tree
- `requel://projects/{projectName}/glossary`
- `requel://projects/{projectName}/open-issues`
- `requel://entities/{entityType}/{entityId}/annotations`

Initial MCP tools:

- `search_project_entities(projectName, query, entityTypes, limit)`
- `get_project_context(projectName, includeGlossary, includeOpenIssues)`
- `get_entity(entityType, entityId)`
- `get_entity_neighbors(entityType, entityId, depth)`
- `get_annotations(entityType, entityId)`
- `draft_annotation(entityType, entityId, kind, text, positions)`

Tool policy:

- Query tools can be read-only and available to AI runs that have project access.
- `draft_annotation` should not persist directly at first. It returns a structured `AnnotationAction` draft to the assistant result applicator.
- Persisting tools, if added later, must require an assistant run id, user/project authorization, idempotency key, and command-handler execution.
- Never expose generic SQL, arbitrary repository access, filesystem access, or unrestricted command execution over MCP.

## Security And Privacy

- AI analysis is opt-in globally and preferably per project.
- Only send project data to external providers when the project has external AI enabled.
- Add a clear project setting for external AI data sharing.
- Resolve the assistant identity through `UserRepository.findUserByUsername("assistant")`, but record the triggering user and project permissions separately.
- Enforce the same project access rules used by REST query endpoints.
- Redact passwords, secrets, credentials, and any fields marked sensitive before prompt construction or MCP tool responses.
- Store provider request/response bodies only behind an explicit debug flag. Default logs should contain run ids, model/provider, status, usage, and finding counts.
- Add rate limits, concurrency limits, and per-project budgets before enabling analysis broadly.
- Treat AI output as untrusted input: validate schema, cap lengths, HTML-escape rendered text, and reject unknown command/action types.

## Runtime Flow

1. Command completes successfully.
2. Command emits or exposes `AnalysisRequest` targets rather than directly knowing assistant methods.
3. `AssistantDispatcher` loads enabled assistants for each target.
4. Dispatcher builds `AssistantContext` and context packs.
5. AI assistant calls `AiAnalysisClient` with a bounded task and expected output schema.
6. Requel validates the structured response.
7. Result applicator creates or updates annotations through commands using the assistant user.
8. Activity record is written.
9. `UpdatedEntityNotifier` and/or SSE publishes affected entity updates.

Keep failures non-blocking for the edit command, but visible:

- timeout/failure creates an activity record
- retries use exponential backoff and run id correlation
- repeated provider failures trip a circuit breaker and temporarily disable AI assistants

## Data Model Additions

Suggested tables/entities:

- `AssistantRun`
  - id, assistantId, provider, model, status, startedAt, completedAt, triggeredByUserId, projectId, targetType, targetId, errorSummary
- `AssistantFinding`
  - id, runId, idempotencyKey, severity, confidence, summary, evidenceJson, appliedAnnotationId
- `AssistantUsage`
  - runId, inputTokens, outputTokens, cachedInputTokens, costEstimate, latencyMs
- Optional `AssistantProjectSettings`
  - projectId, aiEnabled, externalProviderAllowed, allowedAssistantIds, budget settings

If the existing command audit log is enough for some fields, reuse it rather than duplicating audit data.

## API/UI Additions

Backend:

- `GET /api/projects/{name}/assistant-settings`
- `POST /api/commands/UpdateAssistantSettings`
- `GET /api/projects/{name}/assistant-runs`
- `GET /api/assistant-runs/{id}`
- Optional `POST /api/commands/RunAssistantAnalysis` for manual re-analysis

Angular:

- Project settings toggle for AI assistance.
- Assistant run history panel.
- Manual "Run analysis" command from project/entity views.
- Clear source label on AI-created annotations, including assistant id and run timestamp.

The first release can skip most UI and rely on annotations plus logs, but run history should exist before enabling external AI for real users.

## Testing Strategy

- Unit-test context pack builders with small fixture projects.
- Unit-test AI response schema validation, including malformed JSON, overlong fields, unknown action types, stale entity versions, and duplicate idempotency keys.
- Use `NoopAiAnalysisClient` and deterministic fake responses for assistant tests.
- Contract-test MCP resources/tools with authenticated and unauthorized users.
- Integration-test result application through `CommandHandler` to prove permissions, audit, and annotation behavior are preserved.
- Add a no-network CI profile. Real provider smoke tests should be opt-in and skipped unless credentials are present.

## Rollout Plan

### Phase 0 - Decisions

- Choose module names: likely `assistant-api`, `assistant-impl`, and optionally `assistant-openai`.
- Decide whether MCP lives first in `service-impl` or a separate `mcp-server` module.
- Define the first two AI tasks: recommended `REQUIREMENTS_REVIEW` and `SCENARIO_CONSISTENCY`.
- Decide initial opt-in policy and where project AI settings live.

### Phase 1 - SPI And Dispatcher

- Implement the assistant SPI from `doc/assistant-spi-plan.md`.
- Replace manual assistant construction in `AssistantFacade` with registry/dispatcher resolution.
- Add idempotent `AnnotationAction` application.
- Add `AssistantRun` activity records.

### Phase 2 - Context Packs

- Build DTO-based project/entity context pack serializers.
- Add redaction and size limits.
- Add tests for representative projects.

### Phase 3 - MCP Read Tools

- Implement read-only MCP resources/tools backed by existing query services.
- Reuse REST authorization concepts and current-user resolution.
- Add local MCP bridge if useful for Codex CLI/developer workflows.

### Phase 4 - AI Provider

- Add `AiAnalysisClient` abstraction and `NoopAiAnalysisClient`.
- Add `OpenAiAnalysisClient` with configurable model, timeout, retries, structured output validation, and usage capture.
- Keep exact model ids in properties and document how to change them.

### Phase 5 - First AI Assistant

- Implement one AI-backed `RequelAssistant<TextEntity>` for requirements quality review.
- Return annotation actions only.
- Run manually first, then enable post-command dispatch for opted-in projects.

### Phase 6 - Scenario And Project Analysis

- Add scenario consistency review.
- Add project-level consistency summaries once context packing and budget controls are proven.

### Phase 7 - UI And Operations

- Add project settings, run history, manual run controls, and clearer AI source labels.
- Add budget/rate-limit dashboards and provider health indicators.

## Resolved Decisions

Resolved during issue #43 walkthrough. See `43-comment.md` for the full record. Cross-cutting SPI decisions are also listed in `doc/assistant-spi-plan.md` under `Resolved Decisions`.

- **First AI assistant vs legacy NLP.** Both run in parallel — AI is additive. Legacy NLP stays enabled by default. The first AI assistant does not replace specific legacy checks; overlap is expected initially and will fade as new assistants displace older ones.
- **SPI dependencies on domain types.** `assistant-api` may depend on `project-domain` and `annotation-domain` interfaces (not JPA modules). `RequelAssistant<Goal>` / `RequelAssistant<Scenario>` are valid signatures so per-entity rules can stay strongly typed.
- **MCP identity.** MCP sessions carry both identities. Authorization uses the triggering user (the AI client never sees more than the human can). Each registered AI client gets its own assistant pseudo-user — `requel-ai-assistant` for the internal dispatcher, plus one per external MCP client (`claude-code`, `codex-cli`, etc.). This lets each be policy-controlled, rate-limited, and audited separately.
- **Prompt/schema template location.** Classpath resources in the provider module as defaults; DB overrides allowed by `(template_id, version)`. `AssistantRun` records the resolved template id, version, and source (`resource` or `db_override:<row_id>`) so every run is reproducible.
- **Provider request/response retention.** Default is metadata only. Bodies are stored unconditionally for failed runs (schema-validation failure, timeout, provider error). Per-project capture windows let an admin enable body retention for a specific project for a bounded time. Per-run user opt-in lets the triggering user flag a single run for retention, up to a project-admin-set ceiling. A configurable success-sampling rate (default 1%, off in dev/test profiles) captures bodies for ongoing quality monitoring. All body data has a hard TTL (default 14 days, configurable) and is encrypted at rest with a separate key. `AssistantRun` records whether bodies exist and why (`failure`, `project_capture_window`, `user_opt_in`, `sampled`).
- **Per-organization AI settings.** Not needed before per-project settings. Phase 1 is project-scoped only; global-default + project-override is added later. No org/tenant tier is planned.
- **MCP module placement.** New `mcp-server` Maven module, bundled in-process with `requel-app`. Every tool/resource goes through a `ProjectQueryGateway`-style abstraction so a future standalone bridge is mostly an auth-exchange + REST-client problem rather than a rewrite.
- **First two AI tasks.** Start with `REQUIREMENTS_REVIEW` only. Prove the full pipeline end-to-end against real projects before picking the second task type.

## Near-Term Recommendation

Start with the SPI/dispatcher and idempotent annotation application, then add read-only MCP tools before calling an external AI provider. That sequence gives the AI a clean, authorized way to retrieve context and keeps the first provider integration small: one assistant, one task, structured output, annotation-only actions, project-level opt-in.

## References

- Existing Requel assistant SPI notes: `doc/assistant-spi-plan.md`
- Existing optional NLP notes: `doc/nlp-optional-plan.md`
- OpenAI model documentation, checked while drafting on 2026-05-12:
  - `https://developers.openai.com/api/docs/models/gpt-5.3-codex`
  - `https://developers.openai.com/api/docs/models/compare`
  - `https://developers.openai.com/api/docs/models/gpt-4.1`

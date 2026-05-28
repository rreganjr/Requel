# AI Assistance Plan

## Goal

Modernize Requel's background analysis so it can use a current AI model, such as an OpenAI Codex-class model through an API, while preserving the existing workflow where assistants create reviewable notes, issues, and positions on project data. Add MCP support so AI clients can query Requel through controlled, auditable tools instead of scraping the database or relying on oversized prompts.

This plan builds on `doc/assistant-spi-plan.md`. The SPI should remain the local extension point; AI-backed assistants should be one implementation family, not a special path wired directly into commands.

## Current State

- Analysis is invoked after successful edit commands by `AnalysisInvokingCommandHandler`, which calls `AnalyzableEditCommand.invokeAnalysis()`.
- Project commands call `AssistantFacade.analyzeGoal`, `analyzeStory`, `analyzeActor`, `analyzeUseCase`, `analyzeScenario`, or `analyzeProject`.
- After issue #39, `AssistantFacade` is a thin async submission and notification layer. It submits work onto `assistantTaskExecutor` and calls `UpdatedEntityNotifier` after each task. The analysis bodies themselves live in `AssistantTaskRunner`, a `@Component` whose entry points are reached through a Spring transactional proxy. The runner reloads target entities in a new transaction, resolves the `assistant` pseudo-user, and constructs `LexicalAssistant` and the concrete entity assistants per call. Issue #43 must preserve this transactional-proxy boundary when migrating to `AssistantDispatcher`.
- Existing assistants live in `project-jpa` and primarily perform NLP/lexical checks using the legacy `NLPProcessorFactory`, WordNet/OpenNLP-era resources, and annotation commands.
- The useful user-facing output is already review-oriented: annotations, issues, and positions. That is a good governance model for AI suggestions and should be retained.
- The Angular/API migration has introduced `service-api` and `service-impl` modules with authenticated REST endpoints, DTOs, command dispatch, query controllers, JWT auth, and SSE updates. Those modules provide the authenticated query/application services that `mcp-server` (and `assistant-core` context-pack builders) delegate to; the MCP protocol handlers themselves live in `mcp-server`, not in `service-impl`.

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

- Create the `assistant-api` and `assistant-core` modules with `RequelAssistant<T>`, `AssistantContext`, `AssistantResult`, `AssistantMessage`, `AnnotationAction`, and optional `ExternalAction`. Module boundaries and dependencies are locked in `doc/assistant-spi-plan.md` (`Target Module Shape`).
- Keep the SPI synchronous and deterministic from the assistant's perspective. `AssistantDispatcher` in `assistant-core` owns threading, transactions, retry policy, and result application. It is reached through a Spring transactional proxy that preserves the issue #39 `AssistantTaskRunner` boundary so lazy Hibernate collections keep an active session during analysis.
- Apply annotation changes through existing command factories and `CommandHandler`, not direct repository writes. The applicator-to-command mapping and finding state machine are defined in `doc/assistant-spi-plan.md` (`Data Model and Applicator Contract`).
- Give every finding a stable idempotency key: `assistantId + targetType + targetId + findingType + normalizedEvidenceHash`. The key lives on `AssistantFinding` (source of truth) and is duplicated as `assistant_idempotency_key` on the annotation row for fast reverse lookup. Repeated runs update or leave existing findings; cleanup policy is per-assistant (`MANUAL` / `MARK_SUPERSEDED` / `AUTO_RESOLVE_IF_UNTOUCHED`).
- Keep `ExternalAction` as an approval-queue concept for later. Do not let the first AI integration send emails, create tickets, or call arbitrary webhooks automatically.
- Add `AssistantRun`, `AssistantFinding`, and `AssistantUsage` persistence for runs, status, token/cost metadata, failure details, model/provider, template id/version/source, body-capture reason, and created/updated annotation ids. Fields are spelled out in `doc/assistant-spi-plan.md`.

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

## Candidate AI Assistant Use Cases

**Scope note.** Only `REQUIREMENTS_REVIEW` (use case #1 below) is in scope for the first AI assistant per the resolved decision. The rest are backlog candidates for Phase 6 ("Second AI Task") and beyond, listed here so the design constraints they imply (output shape, target type, context-pack needs) inform the SPI and context-pack work in Phases 1–4.

All candidates produce read-only suggestions that map naturally to annotations:

1. **Requirements quality review** *(Phase 5, in scope)*
   - Find ambiguity, unverifiable language, missing actors, hidden assumptions, and conflicting terms.
   - Output issues with positions such as "Clarify wording", "Split requirement", or "Do nothing".

2. **Scenario review** *(backlog candidate)*
   - Check scenario steps for missing actor/action/object, inconsistent ordering, invalid references, and likely alternate flows.
   - Preserve the existing `ScenarioStepAssistant` behavior as a baseline, then add AI findings where the legacy parser is weak.

3. **Glossary candidate extraction** *(backlog candidate)*
   - Suggest domain terms from stories, goals, scenarios, and use cases.
   - Create reviewable issues/positions rather than automatically creating glossary terms.

4. **Project consistency summary** *(backlog candidate)*
   - Compare actors, goals, stories, and use cases for mismatches.
   - Emit a compact project-level summary and focused issues attached to the relevant entity.

5. **Open issue triage** *(backlog candidate)*
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

Add a Requel MCP server so AI clients can query the project through least-privilege tools. The server lives in a new `mcp-server` Maven module, bundled in-process with `requel-app` for the first cut.

### Module shape

- `mcp-server` owns MCP protocol handlers, resources, tools, and MCP-side auth.
- Every tool/resource calls a `ProjectQueryGateway`-style abstraction. The in-process implementation injects existing query/application services as Spring beans and runs in the Spring Security context of the authenticated MCP session.
- A future standalone bridge becomes additive: a second Spring Boot main class that wires a REST-backed gateway implementation plus an auth-exchange step against a running Requel. The MCP tools themselves do not change.
- `mcp-server` must not become a dependency of `assistant-api`. Assistants consume MCP through the `AssistantContext` capability surface, not by importing MCP types.

### Initial MCP resources

- `requel://projects` - list visible projects
- `requel://projects/{projectName}` - project metadata
- `requel://projects/{projectName}/tree` - project tree
- `requel://projects/{projectName}/glossary`
- `requel://projects/{projectName}/open-issues`
- `requel://entities/{entityType}/{entityId}/annotations`

### Initial MCP tools

- `search_project_entities(projectName, query, entityTypes, limit)`
- `get_project_context(projectName, includeGlossary, includeOpenIssues)`
- `get_entity(entityType, entityId)`
- `get_entity_neighbors(entityType, entityId, depth)`
- `get_annotations(entityType, entityId)`
- `draft_annotation(entityType, entityId, kind, text, positions)`

### Tool policy

- Query tools are read-only and available to MCP sessions that have project access through the triggering user.
- `draft_annotation` does not persist directly at first. It returns a structured `AnnotationAction` draft to the assistant result applicator.
- Persisting tools, if added later, must require an assistant run id, user/project authorization, idempotency key, and command-handler execution.
- Never expose generic SQL, arbitrary repository access, filesystem access, or unrestricted command execution over MCP.

### Authentication and Session Flow

Every MCP session carries two identities, per the resolved decision. Authorization uses the triggering user; per-client audit and policy use the assistant pseudo-user.

**Internal AI runtime → in-process `mcp-server`** (used when Requel dispatches its own AI assistants):

1. `AssistantDispatcher` creates an `AssistantRun` with `assistant_user_id = requel-ai-assistant` and `triggered_by_user_id = <human>`.
2. The dispatcher mints a short-lived MCP session token scoped to `(triggering_user_id, project_id, run_id, assistant_user_id)` and hands it to the `AiAnalysisClient` for use during the run.
3. Each MCP tool call hits the in-process `mcp-server`, which validates the session token, populates a Spring Security context as the triggering user (so query authz mirrors REST), and stamps `triggering_user_id`, `assistant_user_id`, and `run_id` on the MCP audit row.
4. Tools delegate through `ProjectQueryGateway`. The in-process gateway calls the existing service/query beans in that security context.
5. The session token expires when the run completes or after a short TTL, whichever comes first.

**External AI client → in-process `mcp-server`** (used when Claude Code, Codex CLI, Cursor, or a hosted model is connected as an MCP client):

1. The user installs/configures a Requel MCP integration in the external client.
2. On first use the client OAuths into Requel; the resulting token is bound to the user's Requel account and scoped to the registered AI-client pseudo-user (e.g. `claude-code`).
3. The token lives in the external client's credential store. Each MCP request carries it as a bearer token.
4. `mcp-server` validates the token, populates a Spring Security context as the user (authz mirrors REST), and stamps `triggering_user_id` plus the client's `assistant_user_id` on the audit row.
5. Per-client rate limits, allowed-tool sets, and quotas are looked up by `assistant_user_id`, so different external clients can have different ceilings.

**Future standalone bridge** (deferred until a real use case lands):

1. The bridge runs as its own Spring Boot process and accepts MCP traffic from a local AI client (Codex CLI, etc.).
2. The bridge authenticates the incoming MCP session as one of the registered external assistant pseudo-users.
3. The bridge calls Requel via REST. The auth-exchange step (one of: shared signing key with the server, a `/oauth/token-exchange` endpoint, or proxy-mode where the MCP client supplies a Requel API token directly) lives in the bridge's gateway implementation.
4. The choice between exchange modes is deferred to the phase that introduces the bridge; the in-process path does not depend on it.

## Security And Privacy

- AI analysis is opt-in. Phase 1 is project-scoped only; a global default with project override is added later. No organization/tenant tier is planned.
- Only send project data to external providers when the project has external AI enabled.
- Add a clear project setting for external AI data sharing.
- Resolve the assistant identity per registered AI client. The internal dispatcher writes as `requel-ai-assistant`; each registered external MCP client (e.g. `claude-code`, `codex-cli`) has its own pseudo-user. The triggering user is recorded separately on every `AssistantRun` and used for authorization.
- Enforce the same project access rules used by REST query endpoints. MCP tools run in the Spring Security context of the triggering user.
- Redact passwords, secrets, credentials, and any fields marked sensitive before prompt construction or MCP tool responses.
- **Provider request/response retention.** Default is metadata only on `AssistantRun` (`run_id`, `assistant_id`, `provider`, `model`, `task_type`, `status`, `input_tokens`, `output_tokens`, `latency_ms`, `findings_count`, `error_summary`). Bodies are stored:
  - **unconditionally for failed runs** (schema-validation failure, timeout, provider error); `body_capture_reason = failure`.
  - during a **per-project capture window** an admin enables for a bounded time; `body_capture_reason = project_capture_window`.
  - on **per-run user opt-in** by the triggering user, up to a project-admin-set ceiling; `body_capture_reason = user_opt_in`.
  - via **success sampling** at a configurable rate (default 1%, off in dev/test profiles); `body_capture_reason = sampled`.
  All retained bodies live on `AssistantUsage`, have a hard TTL (default 14 days, configurable), and are encrypted at rest with a separate key from the main DB. `AssistantRun.body_retained_until` records expiry; a background job purges expired rows.
- Add rate limits, concurrency limits, and per-assistant-user budgets before enabling analysis broadly. External MCP clients are capped independently of the internal dispatcher.
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

Concrete field lists are owned by `doc/assistant-spi-plan.md` (`Data Model and Applicator Contract`). The AI-specific additions on top of the SPI-side model are:

- `AssistantRun` carries `task_type` (e.g. `REQUIREMENTS_REVIEW`), `provider`, `model`, `template_id`, `template_version`, `template_source` (`resource` / `db_override:<row_id>` / `null`), `body_capture_reason`, and `body_retained_until`.
- `AssistantUsage` carries `request_body` and `response_body` columns. Both are nullable; populated only when `body_capture_reason` is non-null and the TTL has not elapsed. Encrypted at rest with a separate key.
- `AssistantProjectSettings` carries `body_capture_window_until` and `body_capture_user_opt_in_ceiling` to govern per-project body retention.
- A new `AssistantPromptTemplate` table backs the DB-override path for prompts and output schemas. Rows are keyed by `(template_id, version)`; if no row exists for a given key, the classpath resource in the provider module is used. `AssistantRun.template_source` records which one resolved.

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

### Phase 0 - Decisions (resolved)

All Phase 0 decisions are resolved (see `Resolved Decisions` below and <https://github.com/rreganjr/Requel/issues/43#issuecomment-4560006774>):

- Module names: `assistant-api`, `assistant-core`, `assistant-legacy-nlp`, `assistant-openai`, `mcp-server`.
- MCP lives in a new `mcp-server` module, bundled in-process with `requel-app`, with `ProjectQueryGateway` seams for a future standalone bridge.
- First AI task: `REQUIREMENTS_REVIEW` only. The second task is chosen after the full pipeline is proven end-to-end.
- Initial opt-in policy: project-scoped settings only in Phase 1; global default + project override added later.

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

### Phase 6 - Second AI Task

- Pick the second AI task based on what Phase 5 surfaced. Candidates carried forward from the use-case list: `SCENARIO_CONSISTENCY`, `GLOSSARY_CANDIDATES`, `ACTOR_GOAL_ALIGNMENT`, `OPEN_ISSUE_SUMMARY`.
- Add project-level consistency summaries once context packing and budget controls are proven.

### Phase 7 - UI And Operations

- Add project settings, run history, manual run controls, and clearer AI source labels.
- Add budget/rate-limit dashboards and provider health indicators.

## Resolved Decisions

Resolved during issue #43 walkthrough. See the full record at <https://github.com/rreganjr/Requel/issues/43#issuecomment-4560006774>. Cross-cutting SPI decisions are also listed in `doc/assistant-spi-plan.md` under `Resolved Decisions`.

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

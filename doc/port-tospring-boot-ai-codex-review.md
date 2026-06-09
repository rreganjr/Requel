# Codex review: Spring AI port plan

Date reviewed: 2026-06-09

## Summary verdict

The direction is sound: replacing the hand-rolled provider clients with Spring AI `ChatClient` is a good fit, and Spring AI's MCP server starters can remove a lot of transport/protocol code. The current plans are too optimistic in a few important places, mostly around version compatibility, structured-output guarantees, and MCP auth/identity.

The main correction: Requel is currently on Spring Boot `3.3.4` / Java `17` (`pom.xml`). Current Spring AI stable docs say Spring AI 1.x supports Spring Boot `3.4.x` and `3.5.x`, while Spring AI 2.0.x supports Boot `4.0.x` and `4.1.x`. So Spring AI is not a no-upgrade drop-in for the repo as it stands.

Recommended baseline:

- Upgrade Requel to Spring Boot `3.5.14` and keep Java `17`.
- Use Spring AI `1.1.7`.
- Provider-client-only port: Spring Boot `3.5.14` + Spring AI `1.1.7`.
- MCP transport/security port: Spring Boot `3.5.14` + Spring AI `1.1.7`; many of the MCP details cited in the docs are current/1.1 behavior, not Spring AI `1.0.x`.
- Do not plan Spring AI `2.x` unless we intentionally move to Boot 4. The Spring AI 2.0 docs explicitly put that line on Boot `4.0.x` / `4.1.x`.

## Findings

### 1. Version baseline in `port-tospring-boot-ai.md` is wrong for the current repo

`doc/port-tospring-boot-ai.md` says Requel already meets Spring AI's baseline and that Spring AI `1.0.x` fits with "no upgrade needed" (lines 8 and 21-29). The root POM is on Spring Boot `3.3.4` and Java `17`.

Spring AI's current Getting Started page says 1.x supports Boot `3.4.x` and `3.5.x`; Spring AI 2.0 says it supports Boot `4.0.x` and `4.1.x`. Boot `3.4` and `3.5` both keep Java `17` as the minimum, so the Java baseline is fine, but the Boot minor bump is required. The recommended target is Spring Boot `3.5.14` with Spring AI `1.1.7`.

Action:

- Change the version table to say Spring AI 1.x requires a Boot bump from `3.3.4`; for this project, use Spring Boot `3.5.14`.
- Pin Spring AI to `1.1.7`.
- Keep Spring AI 2.x out of scope unless there is a separate Boot 4 migration.

### 2. The docs mix Spring AI 1.0 and 1.1 MCP APIs; standardize on 1.1.7

The current plan says to pin Spring AI `1.0.x` and use `McpTransportContext` for stdio identity (`doc/issue_69_subtasks.md`, line 78; `doc/port-tospring-boot-ai.md`, lines 350-359). That is not consistent with the Spring AI docs:

- Spring AI `1.0.8` MCP server docs expose `@Tool` + `MethodToolCallbackProvider` / `ToolCallbackProvider` and `ToolContext`; they do not mention `@McpTool` or `McpTransportContext`.
- Current/1.1 docs add MCP annotations such as `@McpTool`, resource annotations, special parameters, and `McpTransportContext`.
- The MCP security page is marked WIP, community-driven, currently works with Spring AI `1.1.x` only, and is WebMVC-only.

Action:

- Pin Spring AI `1.1.7`, not `1.0.x`.
- Pair it with Spring Boot `3.5.14`.
- If staying on `1.0.8`, rewrite the MCP plan around `@Tool`, `ToolCallbackProvider`, low-level resource specs, Spring Security on the HTTP endpoints, and a custom stdio identity seam.
- Remove the stale warning that avoids `1.1.0-M1` as the main pinning argument. The relevant current question is stable `1.0.8` vs stable `1.1.7`.

### 3. Structured output is not a full replacement for Requel validation

`doc/port-tospring-boot-ai.md` says `.entity(ReviewResult.class)` "forces" the model output and removes hand validation (lines 92-100 and 135-137). `doc/issue_spring_ai_provider_clients.md` repeats this in acceptance criteria (lines 73-78).

Spring AI's structured output converter is still documented as best effort unless native structured output is explicitly enabled and supported by the provider/model. The docs also recommend application validation. Native structured output can be enabled through `AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT`, and the OpenAI chat properties also expose JSON schema response format options.

Action:

- Keep Requel-side response validation in the plan: schema shape, field lengths, enum values, evidence references, stale entity versions, duplicate idempotency keys, and unknown action types.
- Use `responseEntity(...)`, not just `entity(...)`, when Requel needs both the parsed result and `ChatResponse` metadata for model id, token usage, provider metadata, and errors.
- Treat native structured output as a configuration/adapter detail to verify per provider. Do not claim `.entity(...)` always maps to forced Anthropic tool use or OpenAI Structured Outputs.

### 4. `ai-assistance-plan.md` is now outdated for the provider layer

`doc/ai-assistance-plan.md` still describes `OpenAiAnalysisClient` using the OpenAI Responses API as the initial implementation (lines 75-91 and 327-331). That conflicts with the new Spring AI provider-client issue, which plans a single `SpringAiAnalysisClient` over `ChatClient`.

Action:

- Replace the initial provider implementation language with `SpringAiAnalysisClient` backed by `ChatClient`.
- Keep `AiAnalysisClient`, `AiAnalysisRequest`, `AiAnalysisResponse`, `AiFindingDraft`, and `NoopAiAnalysisClient`.
- Recast `assistant-openai` / `assistant-anthropic` as optional starter/dependency packaging choices, not necessarily separate hand-written client modules.
- If a hard requirement remains for OpenAI Responses API behavior, document it as a risk: Spring AI `ChatClient` primarily abstracts chat models, and the OpenAI chat starter's default path is chat completions.

### 5. MCP transport endpoints and security need explicit configuration

The docs say Spring AI WebMVC can sit behind the existing JWT/OAuth2 chain, which is directionally right, but the plan does not spell out endpoint placement. In Spring AI `1.0.8`, WebMVC defaults are `/sse` and `/mcp/message`, with `spring.ai.mcp.server.base-url` available. Current docs add protocol selection such as `SSE`, `STREAMABLE`, and `STATELESS`.

Action:

- Decide whether Requel wants to preserve `/api/mcp` semantics or adopt Spring AI's SSE/message endpoints under `/api`.
- Configure endpoint paths and Spring Security matchers explicitly. Do not assume adding the starter automatically inherits the same `/api/**` behavior as the current `@PostMapping`.
- If using Spring AI `1.0.8`, do not plan on Streamable HTTP/stateless transport unless verified in that exact version. Those are clearly documented in current/1.1 and 2.0 docs.

### 6. "Spring AI handles authn" is too broad

`doc/issue_69_subtasks.md` says Spring AI handles authn and coarse per-tool gating (line 58). That is only safe if the implementation uses the right Spring Security setup and, for the community MCP security module, the right Spring AI line.

For WebMVC HTTP/SSE, existing Spring Security can authenticate the endpoint. For stdio, there is no HTTP request and no inherited JWT chain. The stdio launcher must provide identity through a launch-time token, PAT, environment variable, or initialize metadata, and Requel still has to bind that to the triggering user plus assistant pseudo-user.

Action:

- Change the responsibility split to: Spring AI handles MCP protocol/transport and tool registration; Spring Security/Requel handle authentication; `CommandGateway` / `AuthorizingCommandHandler` keep fine-grained authorization.
- Keep `CommandGateway` as the authorization boundary. Spring method security or per-tool roles are only coarse gates.
- Add an implementation task for setting `SecurityContext` or equivalent current-user context inside MCP tool execution for both HTTP and stdio paths.

### 7. Slice 6 may not be unnecessary

`doc/issue_69_subtasks.md` says `gateway-client-rest` is likely unnecessary if stdio runs in-process (lines 64-78). That may be true for a local developer profile, but it is not obviously true for external clients that need to talk to an already-running Requel server.

An in-process stdio MCP server means the client launches a full Requel app subprocess. That subprocess needs DB config, app config, possibly a free web port, and a clear identity bootstrap. A thin stdio bridge over REST can still be the simpler operational shape for desktop tools.

Action:

- Reword Slice 6 as deferred/conditional, not unnecessary.
- Keep two target deployment shapes distinct:
  - Server-hosted MCP over WebMVC/Streamable HTTP for authenticated remote clients.
  - Local stdio bridge or full-app stdio profile for desktop clients.

### 8. Internal AI assistants may not need MCP as an in-process hop

`doc/ai-assistance-plan.md` routes internal AI runtime calls through the in-process MCP server with a minted session token (lines 189-195). That is a coherent audit model, but Spring AI also supports passing Spring tools directly to chat calls. For the first internal assistant, context packs plus direct tool callbacks may be simpler than making the app call its own MCP transport.

Action:

- Decide whether MCP is a public/external integration boundary only, or also the internal tool boundary for Requel's own assistants.
- If using MCP internally, keep the session-token design and audit semantics.
- If using direct Spring AI tools internally, document equivalent audit/rate-limit hooks so internal and external tool use remain comparable.

## File-specific notes

### `doc/port-tospring-boot-ai.md`

- Correct "Requel already meets its baseline" and "no upgrade needed."
- Update the version table:
  - Recommended: Spring AI `1.1.7` + Spring Boot `3.5.14` + Java `17+`.
  - Spring AI `1.0.8`: historical fallback only; not recommended for this plan because the MCP identity/security notes rely on current/1.1 behavior.
  - Spring AI `2.0.x`: Boot `4.0.x` / `4.1.x`, out of scope unless Boot 4 is accepted.
- Replace the claim that `.entity(...)` removes validation with "reduces provider-specific parsing, but Requel still validates."
- Separate Spring AI `1.0.8` MCP capabilities from current/1.1 capabilities.

### `doc/issue_69_subtasks.md`

- State the dependency baseline explicitly: Spring Boot `3.5.14` + Spring AI `1.1.7`.
- Slice 4 is broadly right if using Spring AI WebMVC, but add explicit endpoint/security configuration.
- Slice 5 should include setting/restoring the security/current-user context during tool execution.
- Slice 6 should stay conditional rather than likely unnecessary.
- Slice 7 can rely on `McpTransportContext` only under the chosen Spring AI `1.1.7` baseline.
- Replace "Spring AI handles authn" with "Spring Security/Requel auth config handles authn; Spring AI supplies transport/tool registration."

### `doc/issue_spring_ai_provider_clients.md`

- Fix the background path from `notes/port-tospring-boot-ai.md` to `doc/port-tospring-boot-ai.md`.
- Add Spring Boot `3.5.14` and Spring AI `1.1.7` as explicit dependency/acceptance criteria.
- Change "structured output is forced + validated via Spring AI" to "structured output is requested via Spring AI and validated by Requel."
- Add `responseEntity(...)` or equivalent metadata capture to the design, because `entity(...)` alone hides the `ChatResponse` metadata Requel needs for usage reporting.
- Keep the Anthropic cache-token verification. Current Anthropic docs expose cache metrics through native usage metadata, but the adapter must explicitly extract it.

### `doc/ai-assistance-plan.md`

- State the target dependency baseline explicitly: Spring Boot `3.5.14` + Spring AI `1.1.7`.
- Update Phase 4 and the provider section from OpenAI-specific `OpenAiAnalysisClient` / Responses API to Spring AI `ChatClient`.
- Keep `NoopAiAnalysisClient`.
- Clarify whether external MCP is Phase 3, while Spring AI MCP transport migration is a later refactor of that module.
- Revisit the internal MCP session-token path once the Spring AI tool-calling design is chosen.

## Sources checked

- Spring AI Getting Started, current stable docs: https://docs.spring.io/spring-ai/reference/getting-started.html
- Spring AI Getting Started, 1.0 docs: https://docs.spring.io/spring-ai/reference/1.0/getting-started.html
- Spring AI Getting Started, 2.0 docs: https://docs.spring.io/spring-ai/reference/2.0/getting-started.html
- Spring AI Structured Output Converter: https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html
- Spring AI ChatClient API: https://docs.spring.io/spring-ai/reference/api/chatclient.html
- Spring AI MCP Server Boot Starter, 1.0 docs: https://docs.spring.io/spring-ai/reference/1.0/api/mcp/mcp-server-boot-starter-docs.html
- Spring AI MCP Server Boot Starter, current docs: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html
- Spring AI MCP Security: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-security.html
- Spring AI OpenAI Chat: https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html
- Spring AI Anthropic Chat: https://docs.spring.io/spring-ai/reference/api/chat/anthropic-chat.html
- Spring Boot 3.4 system requirements: https://docs.spring.io/spring-boot/3.4/system-requirements.html
- Spring Boot 3.5 system requirements: https://docs.spring.io/spring-boot/3.5/system-requirements.html

# Port AI provider clients to Spring AI ChatClient

Part of the v2.0 AI/assistant work (follow-on to #43). Independent of the #69 MCP command-gateway
series. Background + analysis: `doc/port-tospring-boot-ai.md`.

## Goal

Replace the two hand-rolled AI provider clients with a single adapter over Spring AI's
`ChatClient`, keeping Requel's provider-neutral assistant contract unchanged. Net provider code
drops from ~800 lines to ~100, and adding new providers (Azure OpenAI, Bedrock, Vertex, Ollama)
becomes a starter swap instead of a new ~400-line client.

## Background

Today `assistant-openai` and `assistant-anthropic` each hand-roll, against
`java.net.http.HttpClient` + Jackson: request-payload construction, a retry/backoff loop, HTTP
send + status handling + key/endpoint resolution, response parsing, structured-output forcing +
schema validation, and usage/metadata mapping.

- `AnthropicAnalysisClient.java` — ~394 lines
- `OpenAiAnalysisClient.java` — ~402 lines
- `OpenAiCompatibleAnalysisClient.java` — ~439 lines (local/OpenAI-compatible servers)

The fragile parts (forcing guaranteed-shape JSON and validating it) are exactly what Spring AI's
structured-output support (`.entity(...)` / `BeanOutputConverter`) does out of the box.

## Scope

In scope:

- Add the Spring AI BOM + a chat-model starter (`spring-ai-starter-model-openai` and/or
  `-anthropic`), pinned to **1.1.7** on **Spring Boot 3.5.14** / Java 17 (depends on the Spring Boot upgrade ticket; no Boot 4).
- A single `SpringAiAnalysisClient implements AiAnalysisClient` backed by `ChatClient`, using a
  record that mirrors the existing output schema (`summary` / `findings` / `warnings`) bound via
  `.entity(...)`.
- Keep the provider-neutral contract: `AiAnalysisRequest` / `AiAnalysisResponse` /
  `AiFindingDraft`, `RequirementsReviewAssistant`, the context-pack builders, and the centralized
  task instructions.
- Map provider selection + retries to configuration (`spring.ai.*`, `spring.ai.retry.*`),
  replacing the `@ConditionalOnProperty(requel.ai.provider=...)` selection and the bespoke
  `requel.ai.*` HTTP plumbing. Slim `AiProperties` to what remains Requel-specific
  (enabled flag, project allowlist, token caps if still enforced app-side).
- Preserve the `noop` provider for disabled/test profiles.
- Verify the usage/metadata fields we currently report still map — specifically Anthropic
  `cache_read_input_tokens` (we read it off `usage.cache_read_input_tokens` today) through Spring
  AI's usage abstraction.

Out of scope:

- The MCP server / transport (handled separately; see `doc/port-tospring-boot-ai.md` §2 and the
  #69 series). This ticket is provider clients only.
- Boot 4 / Spring AI 2.x.

## Design

`SpringAiAnalysisClient.analyze()` builds a prompt (system = centralized instructions, user =
the context-pack JSON) and calls `chat.prompt()...call().entity(ReviewResult.class)`; the
`ReviewResult` record mirrors today's schema and is mapped to `AiAnalysisResponse` +
`AiFindingDraft` by the small glue we already own. Provider choice is the starter on the classpath plus `spring.ai.*` properties. Use `responseEntity(...)` (not just `entity(...)`) when Requel needs the parsed result plus `ChatResponse` metadata (model id, token usage, provider metadata). See the worked example in `doc/port-tospring-boot-ai.md` §1.

## Testing Strategy

- Unit-test the record→`AiAnalysisResponse`/`AiFindingDraft` mapping.
- Replace/port the existing client tests (`OpenAiCompatibleAnalysisClientTest`, etc.) to exercise
  the Spring AI adapter against a stubbed `ChatModel`/`ChatClient` (no live network).
- Keep `AiPropertiesTest` green; while here, make it hermetic so ambient `REQUEL_AI_*` env vars
  can't break the "defaults to disabled" assertion (a known footgun for the local-AI workflow).
- No-network CI profile; real-provider smoke tests opt-in.

## Acceptance Criteria

- One `SpringAiAnalysisClient` replaces the OpenAI/Anthropic/OpenAI-compatible clients; the
  provider-neutral contract and `RequirementsReviewAssistant` behavior are unchanged.
- Provider + retries are configured via `spring.ai.*`; `noop` still works for disabled/test.
- Structured output is **requested** via Spring AI and **validated by Requel** (schema shape, field lengths, enum values, evidence refs); native structured output is a per-provider config detail, not assumed.
- Reported usage fields (incl. Anthropic cache-read tokens) are verified to still map, or the
  change is documented.
- Spring AI pinned to **1.1.7** on **Spring Boot 3.5.14** / Java 17 (depends on the Boot upgrade ticket).

## Dependencies / relationships

- Follow-on to #43 (assistant SPI / AI providers).
- **Depends on the Spring Boot upgrade ticket** (Boot 3.5.14 + Spring AI 1.1.7 baseline).
- Independent of #69; can otherwise proceed in parallel.

## Open Questions

- Ship with one starter (e.g. OpenAI) first, or both OpenAI + Anthropic immediately?
- Does the OpenAI-compatible/local path (Ollama etc.) go through Spring AI's OpenAI starter
  pointed at a custom base-url, or a separate model starter?
- How much of `AiProperties` (token caps, project allowlist) stays app-enforced vs delegated to
  `spring.ai.*`?

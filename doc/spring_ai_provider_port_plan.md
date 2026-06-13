# Spring AI provider port — implementation plan

> Implements `doc/issue_spring_ai_provider_clients.md`. Background/worked example:
> `doc/port-tospring-boot-ai.md` §1. Follow-on to #43; depends on the Spring Boot 3.5.14 +
> Spring AI 1.1.7 baseline (done). Independent of the #69 MCP gateway series.

## Goal

Replace the three hand-rolled provider clients with a single `SpringAiAnalysisClient` over Spring
AI's `ChatClient`, keeping the provider-neutral contract and `RequirementsReviewAssistant`
unchanged. Net provider code drops from ~1,235 lines (Anthropic 394 + OpenAI 402 + OpenAI-compat
439) to ~100.

## Decisions (locked)

- **Hosted provider: OpenAI first.** Ship the `spring-ai-starter-model-openai` starter only;
  Anthropic is a fast-follow (its starter swap + cache-read usage mapping). The OpenAI starter
  does double duty — hosted OpenAI **and** every OpenAI-compatible local server.
- **Local / OpenAI-compatible: OpenAI starter + base-url.** Point `spring.ai.openai.base-url` at a
  local server (Ollama `/v1`, vLLM, LM Studio, LocalAI, …). One code path, universal coverage,
  faithful replacement for today's `OpenAiCompatibleAnalysisClient`. A **native Ollama starter is
  deferred** as an optional, purely additive follow-up (auto model-pull, native `format` structured
  output) — it would not replace this path.
- **Properties split.** Keep Requel-specific governance app-side in a slimmed `AiProperties`
  (`enabled`, `projectAllowlist`, and the `maxInputTokens`/`maxOutputTokens` caps if still enforced
  app-side). Delegate transport to Spring AI: drop `apiKey`, `apiKeyEnvironmentVariable`,
  `endpoint`, `timeout`, `maxRetries` in favor of `spring.ai.openai.*` and `spring.ai.retry.*`.
- **Structured output is requested via Spring AI and validated by Requel.** `.entity(...)` /
  `.responseEntity(...)` requests guaranteed-shape JSON; Requel still validates schema shape, field
  lengths, enum values, and evidence refs (native structured output is a per-provider config
  detail, not assumed).
- **`noop` preserved** for disabled/test profiles (unchanged gating).

## What stays unchanged

The provider-neutral boundary in `assistant-ai`: `AiAnalysisClient`, `AiAnalysisRequest`,
`AiAnalysisResponse`, `AiFindingDraft`, `AiUsage`, `AiAnalysisException`, and the
`RequirementsReviewAssistant` + context-pack builders + centralized task instructions.

## Module placement

`assistant-ai` is already a Spring module (spring-context, boot-autoconfigure) and the worked
example uses `com.rreganjr.requel.assistant.ai.spring`. So the single adapter lives in
`assistant-ai`, the OpenAI starter is added there, and the two provider modules
(`assistant-openai`, `assistant-anthropic`) are retired. `requel-app` currently depends on both
(pom lines ~345/~350); both modules depend on `assistant-ai`.

## Provider selection / bean gating

Keep the existing mutually-exclusive `requel.ai.provider` convention so `noop` gating is unchanged:

- `NoopAiAnalysisClient` — active when `requel.ai.provider` is `noop` or missing (unchanged).
- `SpringAiAnalysisClient` — active when `requel.ai.provider` is `openai` **or** `openai-compat`
  (both route to the one adapter; `openai-compat` is just OpenAI + a custom `base-url`).

The concrete model/key/base-url/retries come from `spring.ai.*`; `requel.ai.provider` only selects
adapter-vs-noop, so exactly one `AiAnalysisClient` bean exists.

## Slices

Each slice builds, tests, and commits independently; the build stays green throughout (old clients
remain, gated by mutually-exclusive properties, until Slice 3 retires them).

### Slice 1 — `SpringAiAnalysisClient` + OpenAI starter in `assistant-ai`
- Add `spring-ai-starter-model-openai` to `assistant-ai/pom.xml` (version from the managed BOM).
- New `com.rreganjr.requel.assistant.ai.spring.SpringAiAnalysisClient implements AiAnalysisClient`,
  gated on `requel.ai.provider` ∈ {`openai`, `openai-compat`}. Builds prompt (system = centralized
  `instructions(request)` with generic fallback; user = context-pack JSON), calls
  `chat.prompt()…call().responseEntity(ReviewResult.class)` to get the parsed result **plus**
  `ChatResponse` metadata.
- `ReviewResult` record mirrors today's schema (`summary` / `findings` / `warnings`, finding =
  findingType/severity/confidence/evidenceReferences/suggestedIssueText/suggestedNoteText/suggestedPositions).
- `toResponse()` glue maps `ReviewResult` → `AiAnalysisResponse` + `AiFindingDraft`, builds
  `structuredOutput` (`objectMapper.valueToTree`), and `AiUsage` from `ChatResponse` usage/metadata
  (model id, input/output tokens; cached-token + Anthropic cache-read handled when Anthropic lands).
- Port the existing Requel-side validation (shape/length/enum/evidence-ref).
- Test: `SpringAiAnalysisClientTest` against a **stubbed** `ChatClient`/`ChatModel` (no network) +
  record→response mapping unit test.

### Slice 2 — Config & properties
- Slim `AiProperties` per the decision; remove transport fields now owned by `spring.ai.*`.
- Wire provider/model/key/base-url via `spring.ai.openai.*`; retries via `spring.ai.retry.*`.
- Document the local base-url recipe (`spring.ai.openai.base-url=http://localhost:11434/v1`, dummy
  key, local model name) in `doc/AI_ASSISTANT_SETUP.md`.
- Make `AiPropertiesTest` hermetic so ambient `REQUEL_AI_*` env can't break "defaults to disabled".

### Slice 3 — Retire `assistant-openai` + `assistant-anthropic`
- Remove both `<module>` entries from the parent `pom.xml`; remove the two deps from
  `requel-app/pom.xml`; delete the module directories.
- Port any still-relevant assertions from `OpenAiAnalysisClientTest`,
  `OpenAiCompatibleAnalysisClientTest`, `AnthropicAnalysisClientTest` into the Slice 1 adapter test;
  drop the live ITs (or move to the opt-in smoke profile).
- Build green with the single adapter.

### Slice 4 — Docs + verification
- `doc/AI_ASSISTANT_SETUP.md`: OpenAI hosted + local base-url config; note Anthropic + native
  Ollama as fast-follows.
- Document usage-field mapping (and the Anthropic `cache_read_input_tokens` caveat — verified when
  Anthropic is added).
- CI default no-network (noop); real-provider smoke tests opt-in.
- Full `mvn -pl modules/assistant-ai,modules/requel-app -am verify`.

## Fast-follows (separate tickets)
- **Anthropic starter** — add `spring-ai-starter-model-anthropic`, route `requel.ai.provider=anthropic`
  to the same adapter, verify cache-read token mapping.
- **Native Ollama starter** — optional, additive; only for Ollama-native features.

## Acceptance criteria (from the ticket)
- One `SpringAiAnalysisClient` replaces the OpenAI/Anthropic/OpenAI-compatible clients; contract and
  `RequirementsReviewAssistant` behavior unchanged.
- Provider + retries configured via `spring.ai.*`; `noop` still works.
- Structured output requested via Spring AI, validated by Requel.
- Reported usage fields verified to map (or documented).
- Spring AI 1.1.7 on Boot 3.5.14 / Java 17.

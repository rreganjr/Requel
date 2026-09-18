# Porting Requel's AI + MCP layers to Spring AI

Working note — evaluating whether Spring AI could replace the hand-rolled AI
provider clients and the MCP server. Date: 2026-06-05.

## Summary

Yes, Spring AI can cover both layers, but **not as a no-upgrade drop-in**: Requel is on Spring Boot 3.3.4, and Spring AI 1.x needs Boot 3.4.x/3.5.x. Target baseline for all of this work is **Spring Boot 3.5.14 + Spring AI 1.1.7** (Java 17), with the Boot bump done first as its own ticket.

- **AI provider clients** — clear win. The two analysis clients are ~400 lines
  each of HTTP plumbing, retries, payload building, and structured-output
  extraction that Spring AI's `ChatClient` + structured-output converters
  collapse to a fraction of that.
- **MCP server** — viable, and newly attractive because **stdio** is a near-term
  goal that Spring AI's MCP server starter gives almost for free. The one thing
  to design around is our per-user authorization + audit model, which our
  current HTTP transport reuses from the `/api/**` JWT chain.

### Version baseline

Requel is currently on Spring Boot **3.3.4** / Java 17, so Spring AI is **not** a no-upgrade
drop-in — Spring AI 1.x requires Boot 3.4.x/3.5.x. Target baseline for all of this work:

| | Version |
|---|---|
| Spring Boot | **3.5.14** (bump from 3.3.4; Java 17 stays) |
| Spring AI | **1.1.7** (stable; the MCP APIs we rely on — `@McpTool`, `McpTransportContext`, the MCP security module — are 1.1 behavior, not in 1.0.8) |
| Java | 17 |

The Boot bump is a prerequisite, tracked as its own ticket (off `release/2.0`, before the AI/MCP
work). Spring AI 1.0.8 is a historical fallback only; Spring AI 2.0.x needs Boot 4 and is out of
scope.

---

## 1. AI provider clients — the strong case

### What we have today

A provider-neutral `AiAnalysisClient` interface, with implementations selected by
`@ConditionalOnProperty(requel.ai.provider=...)`:

| File | Lines |
|---|---|
| `AnthropicAnalysisClient.java` | 394 |
| `OpenAiAnalysisClient.java` | 402 |

Each one hand-rolls, against `java.net.http.HttpClient` + Jackson:

- request-payload construction (system/messages/tools, forced `tool_choice` for
  Anthropic; `text.format` Structured Outputs for OpenAI)
- a retry/backoff loop with retryable-status detection
- HTTP send + status handling + API-key/endpoint resolution
- response parsing, structured-output extraction, schema validation
- usage + provider-metadata mapping

The most fragile parts — forcing guaranteed-shape JSON and validating it — are
exactly what Spring AI's structured-output support does for you.

### What it looks like on Spring AI's `ChatClient`

The provider-neutral contract (`AiAnalysisRequest` / `AiAnalysisResponse` /
`AiFindingDraft`) stays. We define a record that mirrors the output schema and
let Spring AI's `BeanOutputConverter` force + bind it; provider choice moves to a
starter dependency + properties, so the two ~400-line clients become **one**
small adapter.

```java
package com.rreganjr.requel.assistant.ai.spring;

import com.rreganjr.requel.assistant.ai.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single AiAnalysisClient backed by Spring AI's ChatClient. The configured
 * ChatModel (Anthropic / OpenAI / Azure / Bedrock / Ollama...) is chosen by
 * which spring-ai-starter-* is on the classpath plus spring.ai.* properties —
 * so this one class replaces both AnthropicAnalysisClient and
 * OpenAiAnalysisClient.
 */
@Component
public class SpringAiAnalysisClient implements AiAnalysisClient {

    private final ChatClient chat;

    public SpringAiAnalysisClient(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    @Override
    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        // .entity(ReviewResult.class) registers the JSON schema, forces the
        // model to fill it (forced tool on Anthropic, Structured Outputs on
        // OpenAI), and binds the reply — no manual tool_choice / tool_use
        // extraction / hand validation.
        ReviewResult result = chat.prompt()
                .system(instructions(request))
                .user(promptJson(request))
                .call()
                .entity(ReviewResult.class);

        return toResponse(result); // map record -> AiAnalysisResponse + AiFindingDraft
    }

    /** Mirrors the existing output schema (summary / findings / warnings). */
    record ReviewResult(String summary, List<Finding> findings, List<String> warnings) {
        record Finding(String findingType, String severity, Double confidence,
                       List<String> evidenceReferences, String suggestedIssueText,
                       String suggestedNoteText, List<String> suggestedPositions) {}
    }

    // instructions(), promptJson(), toResponse() = the small provider-neutral
    // glue we already own.
}
```

Provider config (replaces `requel.ai.*` plumbing + the `@ConditionalOnProperty`
selection):

```properties
# pick a starter (spring-ai-starter-model-anthropic OR -openai) on the classpath
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.options.model=claude-...
spring.ai.anthropic.chat.options.max-tokens=4096
spring.ai.retry.max-attempts=3
```

### Line-count comparison

| | Today | On Spring AI |
|---|---|---|
| Anthropic client | 394 | — (folded in) |
| OpenAI client | 402 | — (folded in) |
| Unified adapter | — | ~70–100 (one class + record) |
| Retry / HTTP / key resolution | hand-rolled in each | framework (`spring.ai.retry.*`) |
| Structured-output forcing + validation | hand-rolled in each | `.entity(...)` |
| **Net provider code** | **~800** | **~100** |

What we keep regardless: `AiAnalysisRequest/Response/FindingDraft`,
`AiProperties` (slimmed), `ProjectContextPackBuilder`, the
`RequirementsReviewAssistant` orchestration. Bonus: adding Azure OpenAI,
Bedrock, Vertex, or Ollama later becomes a starter swap, not a new ~400-line
client.

### Trade-offs to note

- New transitive dependency surface, and the 1.x API moved fast pre-GA — pin a
  version.
- We lose some low-level control over the exact wire payload, but our needs are
  standard (chat + forced structured JSON), which is supported — but Spring AI's structured-output converter is **best-effort** unless native structured output is enabled per provider, so Requel keeps its own response validation (schema shape, field lengths, enum values, evidence refs, stale versions, idempotency keys) and uses `responseEntity(...)` when it needs the parsed result plus `ChatResponse` metadata (model id, usage).
- Provider-specific knobs (e.g. Anthropic cache-read token reporting we read off
  `usage.cache_read_input_tokens`) surface through Spring AI's usage/metadata
  abstraction — verify the fields we report still map.

---

## 2. MCP server — viable, and stdio makes it compelling

### What we have today

A hand-rolled JSON-RPC server over a single HTTP `@PostMapping`:

| File | Lines | Role |
|---|---|---|
| `McpJsonRpcController.java` | 50 | HTTP transport (`POST /api/mcp`) |
| `McpJsonRpcHandler.java` | 71 | method dispatch + audit |
| `McpJsonRpcRequest/Response/Error.java` | 87 | JSON-RPC envelopes |
| `McpInvalidParams/MethodNotFound Exception.java` | 64 | error mapping |
| `McpToolDescriptor / ResourceDescriptor / TextContent / ResourceContent` | ~ | protocol DTOs |
| `McpReadService.java` | 279 | **our tools + resources (business logic)** |
| `McpCallAudit* + InProcessProjectQueryGateway` | ~ | audit + data access |

The protocol/transport boilerplate (~270+ lines of controller + handler +
envelopes + exceptions + descriptor DTOs) is the part a framework owns. The
~279-line `McpReadService` is mostly *our* logic — the 11 tools, 4 resources,
and their JSON schemas — and largely survives any port.

### What it looks like on Spring AI's MCP server starter

Tools become `@Tool`-annotated methods; the protocol handshake (`initialize`,
`tools/list`, `tools/call`, `resources/*`), envelope handling, and **transport**
(stdio / SSE / streamable-HTTP) are provided by the starter. Schemas are derived
from method parameters instead of hand-built `Map.of(...)` schema literals.

```java
package com.rreganjr.requel.mcp.spring;

import com.rreganjr.requel.mcp.ProjectQueryGateway;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class RequelMcpTools {

    private final ProjectQueryGateway gateway;

    public RequelMcpTools(ProjectQueryGateway gateway) {
        this.gateway = gateway;
    }

    @Tool(description = "List projects visible to the current authenticated user.")
    public Object listProjects() {
        return gateway.listProjects();
    }

    @Tool(description = "Read one project summary by project name.")
    public Object getProject(@ToolParam(description = "Project name") String projectName) {
        return gateway.getProject(projectName);
    }

    @Tool(description = "Read the notes and issues attached to one entity.")
    public Object getAnnotations(String projectName, String entityType, long entityId) {
        return gateway.getAnnotations(projectName, entityType, entityId);
    }

    // ... the remaining read tools map 1:1 from McpReadService.callTool(...)

    @Tool(description = "Build a draft annotation (note or issue) WITHOUT persisting it.")
    public AnnotationAction draftAnnotation(String entityType, long entityId,
                                            String kind, String text, String severity) {
        // identical draft-only logic to today; returns, never persists
        ...
    }
}
```

Registration + transport selection is configuration:

```properties
# stdio transport (the near-term goal): local clients like Claude Code / Cursor
# launch the jar as a subprocess and speak MCP over stdin/stdout.
spring.ai.mcp.server.name=requel-mcp-server
spring.ai.mcp.server.version=2.0.0-dev
spring.ai.mcp.server.stdio=true

# or, for web/IDE clients, the webmvc/webflux starter exposes SSE /
# streamable-HTTP instead — both can coexist as separate run profiles.
```

Starters: `spring-ai-starter-mcp-server` (stdio), `-mcp-server-webmvc` (SSE over
MVC), `-mcp-server-webflux` (streamable-HTTP). Streamable mode keeps session
affinity by default; a stateless mode exists for HA/Kubernetes.

### The permission-model question (the real decision point)

This is why hand-rolling was reasonable so far, and the thing to design around:

- **Today**, MCP is mounted under `/api/mcp`, so it inherits the existing JWT
  security chain *and current-user resolution* — every tool call runs as the
  authenticated user, and `McpReadService` / `ProjectQueryGateway` naturally
  scope to "projects visible to the current user." Our `McpCallAuditor` (V10
  migration) records each call.
- **stdio has no HTTP request**, so there's no JWT/`SecurityContext` to inherit.
  A subprocess speaking over stdin/stdout is implicitly "whoever launched it."
  We'd need to establish identity another way — e.g. an API token / principal
  passed at process launch (env var or `initialize` params) that we resolve into
  the same authorization context the gateway expects.
- **SSE / streamable-HTTP** transports *can* sit behind the JWT filter chain
  (they're still HTTP), so the per-user model ports more directly there than to
  stdio.

Recommended approach if we adopt it:

1. Keep `ProjectQueryGateway` + the read logic as the authorization boundary
   (it already scopes by visible projects) — don't let transport choice leak
   into business logic.
2. Introduce a small `CurrentUserProvider` SPI so the tool layer gets its
   principal from either (a) the JWT chain (HTTP/SSE) or (b) a launch-time token
   (stdio). One seam, two implementations.
3. Preserve auditing by wrapping tool invocations (Spring AI exposes
   interception points / we can audit in the `@Tool` methods or via an advisor),
   so we don't lose the `McpCallAudit` trail.

If the stdio identity story proves awkward, a pragmatic middle path: adopt
Spring AI on the **client/structured-output side now**, expose stdio via the
starter for *single-user / local-dev* use, and keep the JWT-scoped HTTP endpoint
for multi-user server deployments until the `CurrentUserProvider` seam is proven.

### Line-count comparison (server)

| | Today | On Spring AI |
|---|---|---|
| Transport + JSON-RPC envelopes + dispatch + exceptions + descriptor DTOs | ~270+ | framework |
| Tools + resources (`McpReadService`) | 279 | ~200 as `@Tool` methods (schemas inferred) |
| stdio support | not available | config flag |
| Per-user auth | inherited from `/api/**` JWT | **needs `CurrentUserProvider` seam** |
| Audit | `McpCallAuditor` | re-wire via advisor / in-method |

---

## 3. Recommendation

1. **Port the provider clients to `ChatClient` first.** Biggest code reduction,
   lowest risk, no transport/auth entanglement, and it unlocks easy multi-provider
   support. ~800 → ~100 lines of provider code.
2. **Adopt the MCP server starter when stdio becomes a priority**, gated on
   building the `CurrentUserProvider` seam so the per-user authorization + audit
   model survives the move off the HTTP/JWT transport. Keep `ProjectQueryGateway`
   as the authorization boundary regardless.
3. Stay on the **Spring AI 1.x line** (Boot 3 / Java 17) — no Boot 4 upgrade.

---

## Assessment vs. current plans (#69 MCP command gateway) — 2026-06-05

Reviewed against the in-flight #69 gateway series and the #43 assistant work. Verdict: the two
halves are independent decisions and land very differently against our plans.

### AI provider clients → Spring AI `ChatClient`: yes, separate ticket
Pure #43 territory; no entanglement with #69. Collapses ~800 lines to ~100, keeps the
provider-neutral contract (`AiAnalysisRequest/Response/FindingDraft`, `RequirementsReviewAssistant`,
context packs), and makes new providers a starter swap. Low risk, high payoff. Diligence: pin a
Spring AI 1.x version; verify the Anthropic cache-token usage fields still map through Spring AI's
usage/metadata abstraction. Tracked as its own issue.

### MCP server → Spring AI MCP starter: adopt it for the transport layer (re-plan Slices 4/6/7)
Correcting the original framing: "read-only" described what *Requel currently exposes*, never a
Spring AI limitation. Verified against the Spring AI docs (June 2026):

- **Writes are fully supported.** MCP tools are arbitrary methods; the docs use an admin-only
  "thing-writer" tool as an example. Our Slice 4 write tools are a normal use of the framework.
- **Programmatic tool registration.** Tools register via `ToolCallbackProvider` /
  `MethodToolCallbackProvider` beans (auto-config merges multiple), so our descriptor-driven generic
  `runCommand` + typed tools (built from `CommandDescriptor`) register cleanly; the earlier
  "static `@Tool` mismatch" concern does not apply.
- **Transports for free:** stdio (`spring-ai-starter-mcp-server`), SSE (`-webmvc`), reactive SSE
  (`-webflux`), plus a stateless server starter for HA.
- **Security story exists:** OAuth2 (authorization-code / client-credentials / hybrid), API-key
  auth, **per-tool authorization** (reader vs admin), and a `McpTransportContext` /
  `McpTransportContextProvider` seam to carry identity — including stdio via a launch-time API key.

**Division of responsibility (the key design rule):** Spring AI owns *transport* + *tool registration* + *coarse per-tool gating* (e.g. write tools
require a scope/role); **Spring Security / Requel config owns authentication** (JWT/OAuth2 over
HTTP, a launch-time API key/PAT over stdio). Our
`CommandGateway` + `AuthorizingCommandHandler` keep owning *fine-grained per-project-stakeholder
authorization* (can this user edit/delete this entity on this project) — Spring AI's per-tool auth
is too coarse for that. Tools delegate to `CommandGateway`, so the authorization boundary is
unchanged, and Slice 1's transport-agnostic `gateway-api` is not wasted.

This collapses the hand-rolled MCP transport (~270 lines of JSON-RPC controller / handler /
envelopes / exceptions / descriptor DTOs) into framework + config, and gives stdio + SSE without
us building them.

### What changes in the #69 plan
- **Slice 4 (MCP server):** build on `spring-ai-starter-mcp-server-webmvc` (HTTP/SSE behind the
  existing JWT/OAuth2 chain). Expose read + write tools via a `ToolCallbackProvider` delegating to
  `QueryGateway` / `CommandGateway`. Re-wire `McpCallAudit` via a tool interceptor/advisor.
- **Remote connector (#70):** largely *subsumed* by the webmvc transport behind OAuth2 — less
  bespoke work there.
- **Slice 7 (stdio):** use `spring-ai-starter-mcp-server` (stdio) with API-key identity resolved
  through `McpTransportContext` into a Requel user. This **likely replaces the separate
  `mcp-bridge` process**.
- **Slice 6 (`gateway-client-rest`):** **may become unnecessary** — if stdio runs in-process via
  the Spring AI starter there is no REST round-trip, so the in-process `CommandGateway` (Slice 3) is
  used directly. Keep it only if a truly out-of-process client is still wanted.

### Issues / cautions
- **Version baseline:** Spring Boot **3.5.14** + Spring AI **1.1.7** (Java 17). 1.1.7 (not 1.0.8)
  is required because the MCP identity/security APIs we rely on (`@McpTool`, `McpTransportContext`,
  the MCP security module) are 1.1 behavior. The Boot bump from 3.3.4 is a prerequisite ticket.
- **stdio identity = local / API-key.** Spring AI treats stdio as local/private; identity comes
  from a launch-time API key (or PAT, #73) resolved to a Requel user. Fine-grained authz still runs
  in our gateway, so a missing or wrong identity fails closed at the command layer.
- **In-process stdio boots the full app** as a subprocess (heavier than a thin proxy), but removes
  the bridge + REST-client modules — net simpler to maintain.
- Audit re-wiring (`McpCallAudit` → advisor); a transition window where hand-rolled and Spring AI
  transports may coexist.

### Recommendation
1. File the AI-clients port as its own ticket; do it anytime (can run parallel to #69).
2. **Adopt Spring AI MCP server for the transport/tool layer** and re-plan Slices 4/6/7 + #70 as
   above, keeping `CommandGateway` as the fine-grained authorization boundary. Slices 1–3
   (gateway-api, auth hardening, in-process gateway) are unchanged.
3. Target Spring Boot **3.5.14** + Spring AI **1.1.7** (Java 17); the Boot bump is a prerequisite
   ticket off `release/2.0`, before the AI/MCP work. No Boot 4 / Spring AI 2.x.

---

## References

- Spring AI — Getting Started (versions, Java baseline): https://docs.spring.io/spring-ai/reference/getting-started.html
- Spring AI 1.0 GA announcement: https://spring.io/blog/2025/05/20/spring-ai-1-0-GA-released/
- Spring AI 2.0 / Spring Boot 4 (why 2.x is out of scope for now): https://usama.codes/blog/spring-ai-2-spring-boot-4-guide
- Model Context Protocol (MCP) — Spring AI Reference: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
- Spring AI MCP Boot Starters (transports: stdio / SSE / streamable-HTTP): https://spring.io/blog/2025/09/16/spring-ai-mcp-intro-blog/
- Getting Started with MCP — Spring AI Reference: https://docs.spring.io/spring-ai/reference/guides/getting-started-mcp.html

# Issue #98 — Streamable-only MCP migration plan

Reference: https://github.com/rreganjr/Requel/issues/98
Relates to: #82 (MCP cleanup / JSON-RPC controller decision), #83 (MCP OAuth 2.1)

## Summary

Requel's MCP server currently serves the **legacy HTTP+SSE** transport only, so interactive
clients reach it through the `mcp-remote` stdio bridge (often with a long-lived PAT), and Codex —
which speaks **Streamable HTTP only** — cannot connect natively at all. This work migrates the
server to **Streamable HTTP only** (the MCP spec's forward direction; HTTP+SSE has been deprecated
since the 2025-03-26 spec) and, as part of that, **retires the hand-rolled JSON-RPC controller**
that was intentionally kept under #82.

The original #98 was scoped as a spike whose central unknown was "can the Spring AI starter serve
SSE and Streamable at once?" We are deliberately answering that by **not needing it**: we drop SSE
and go Streamable-only. That removes the spike's biggest risk and forces the endpoint-collision
decision from #82 in the cleaner direction.

## Why Streamable-only is safe for our clients

Every interactive client Requel supports today speaks Streamable HTTP natively. We use SSE with
them now only because the *server* is SSE-only — not because any client requires SSE.

| Client we support | Native Streamable HTTP? | Can do SSE? | After migration |
| --- | --- | --- | --- |
| Codex (CLI / extension) | Yes — and *only* Streamable | No | Connects natively over OAuth; bridge + PAT dropped |
| Claude Code (CLI + VS Code ext) | Yes | Yes (deprecated) | Direct, native |
| VS Code / Copilot native MCP | Yes (`"type": "http"`) | Yes (fallback) | Direct, native |
| Claude Desktop / Cowork connectors | Yes | Yes (deprecating) | Direct remote connector |
| MCP Inspector | Yes (selectable) | Yes | Direct over Streamable |
| `mcp-remote` bridge itself | Yes — it *defaults* to Streamable | Yes (only via forced `--transport sse-only`) | Only needed for pure-stdio clients that can't do remote HTTP |

No currently-supported client is SSE-only. The residual risk is a **client-version floor**: native
Streamable support in Claude Code / VS Code is relatively recent, so a very old client build may
need updating — low risk, since those clients already probe Streamable first.

## Decisions locked by this migration

- **Transport:** Streamable HTTP only. Drop the SSE endpoints.
- **#82 endpoint collision:** resolved by **retiring** the hand-rolled JSON-RPC controller so
  Streamable can own `POST /api/mcp` (rather than parking Streamable on a secondary path).
- **Auditing:** unchanged in coverage. The Spring AI transport path already audits independently
  via `RequelMcpToolCallback.recordToolCall(...)`; only the JSON-RPC-specific audit overload goes
  away.
- **Security:** unchanged. `/api/mcp/**` stays behind the #83 OAuth 2.1 resource-server chain;
  OAuth and PAT both continue to authenticate; per-stakeholder authorization + auditing intact.

## Code changes

### Retire (delete)
- `modules/mcp-server/.../McpJsonRpcController.java` — the `POST /api/mcp` REST endpoint; removing
  it frees the path for the Streamable transport.
- `modules/mcp-server/.../McpJsonRpcHandler.java` — the hand-rolled JSON-RPC dispatch.
- `modules/mcp-server/.../McpJsonRpcRequest.java`, `McpJsonRpcResponse.java`,
  `McpJsonRpcError.java` — the JSON-RPC DTOs (used only by the handler/controller, the auditor
  overload, and the two ITs below).

### Refactor
- `modules/mcp-server/.../McpCallAuditor.java` — remove the
  `record(McpJsonRpcRequest, McpJsonRpcResponse, long)` overload (only the retired handler called
  it). Keep `recordToolCall(...)`, which the live transport uses.

### Keep (shared, not part of the JSON-RPC transport)
- `McpReadService`, `McpWriteService`, `RequelMcpToolCallback`, `RequelMcpServerConfig`,
  `McpCallAudit(+Repository)`.
- `McpInvalidParamsException`, `McpMethodNotFoundException`, `McpRateLimiter` /
  `NoOpMcpRateLimiter` / `McpRateLimitExceededException` — these are service-layer types used by
  `McpReadService`/`McpWriteService`, independent of the retired transport.

### Config (`modules/requel-app/src/main/resources/application.properties`)
- Enable Streamable: `spring.ai.mcp.server.protocol=STREAMABLE` and
  `spring.ai.mcp.server.streamable-http.mcp-endpoint=/api/mcp` (now free after the controller is
  retired).
- Remove/retire `spring.ai.mcp.server.sse-endpoint` and `spring.ai.mcp.server.sse-message-endpoint`.
- Confirm the `/api/mcp/**` OAuth2 resource-server chain still covers the Streamable sub-path.

## Test changes
- `McpJsonRpcHandlerTest` — delete (tests the retired handler).
- `RequelMcpEndToEndIT` — rewrite. Today it drives `tools/call` through `McpJsonRpcHandler` with
  `McpJsonRpcRequest`/`McpJsonRpcResponse`. Rewrite the capstone create/associate/annotate sequence
  onto the live transport — either the real Streamable HTTP endpoint (MCP client / MockMvc) or the
  `RequelMcpToolCallback` path in-process — and keep asserting BOTH audit surfaces (command-audit
  rows + MCP-call-audit rows).
- `McpCallAuditIT` — rewrite to drive the ToolCallback/Streamable path and assert the
  `recordToolCall` audit row plus the error-outcome path (replacing the handler-driven form).

## Docs
- `doc/mcp_remote_connection.md`, `doc/VS_CODE_MCP.md`, `doc/local_mcp_bridge.md`: drop the
  `--transport sse-only` bridge instructions; add native Streamable recipes — Codex native OAuth
  (`codex mcp add --transport http requel <url>` + `codex mcp login requel`), and direct Streamable
  for Claude Code / VS Code / Claude Desktop / MCP Inspector. Note the bridge is now only needed for
  pure-stdio clients that cannot do remote HTTP.

## Still to determine empirically (carried from #98)
- **Codex + gated DCR (#83):** Codex may attempt anonymous Dynamic Client Registration, which the
  gated `/connect/register` rejects. Confirm Codex accepts a pre-registered `client_id`; note that
  `mcp_oauth_callback_url` appends a server-specific callback ID, so the exact `redirect_uri` to
  register must be read from a first attempt.
- **Session/streaming semantics + diagnostics** for the Streamable transport.

## Acceptance criteria
- [ ] `protocol=STREAMABLE` enabled; Streamable serves `POST /api/mcp`; SSE endpoints removed.
- [ ] Hand-rolled JSON-RPC controller/handler/DTOs deleted; `McpCallAuditor` refactored; build green.
- [ ] `RequelMcpEndToEndIT` and `McpCallAuditIT` rewritten onto the live transport; `mvn clean
      verify` green.
- [ ] **Codex connects natively** over OAuth (`codex mcp add --transport http` + `codex mcp login`)
      with **no `mcp-remote` and no PAT**; tool calls run as the authenticated user (pre-registered
      client / callback port documented).
- [ ] Claude Code, VS Code/Copilot, Claude Desktop, and MCP Inspector all connect over Streamable
      HTTP (direct, no bridge).
- [ ] Security unchanged: `/api/mcp/**` behind the resource-server chain; OAuth + PAT both
      authenticate; per-stakeholder authorization + auditing intact.
- [ ] Docs updated: SSE-only bridge recipes replaced with native Streamable recipes; note where the
      bridge is still needed (pure-stdio clients only).

## Migration note (for the release)
SSE support is removed. Any client config still using `/api/mcp/sse` or `mcp-remote --transport
sse-only` must switch to the Streamable endpoint `/api/mcp` (`"type": "http"` for VS Code; native
`--transport http` for Codex; direct remote-connector URL for Claude Desktop).

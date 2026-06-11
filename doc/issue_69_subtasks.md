## Implementation plan — sliced sub-tasks

We'll implement this ticket as reviewable slices, each on the `69-mcp-command-gateway` branch. Order
reflects dependencies; slices 1–2 unblock the rest.

**Dependency baseline:** Spring Boot **3.5.14** + Spring AI **1.1.7** (Java 17). The Boot bump from 3.3.4 is a prerequisite ticket (do it off `release/2.0` first); Slices 4/6/7 build on Spring AI 1.1.7.

### Slice 1 — `gateway-api` (pure module)
`CommandGateway` / `QueryGateway` interfaces, `AllowDenyPolicy`, command/input **descriptors**
(schemas, not just a predicate), and value types. Depends only on `service-api`. Wire into the
Maven build; confirm no Spring MVC/JPA leak and no dependency cycle
(`mvn -pl modules/mcp-server,modules/service-impl -am test`).

### Slice 2 — Command authorization hardening (prerequisite)
Audit found many allowlisted commands do **not** implement `AuthorizableCommand`, so they pass
unchecked at the command-auth layer. Make each implement it with the right
`RequiresStakeholderPermission(EntityType, Operation)` (or gate it by gateway policy).

Already enforced: `EditProject`, `EditGoal`, `EditStory`, `EditActor`, `EditGlossaryTerm`,
`EditReportGenerator`, `EditIssue`, `DeleteGoal`, `DeleteStory`, `DeleteActor`.

Needs an authorization requirement (proposed mapping — confirm during implementation):

- Entity edits: `EditUseCase` → `UseCase[Edit]`, `EditScenario` → `Scenario[Edit]`,
  `EditScenarioStep` → `Scenario[Edit]`, `EditNonUserStakeholder` → `Stakeholder[Edit]`.
- Associations: `AddGoalToGoalContainer`/`RemoveGoalFromGoalContainer` → `Goal[Edit]`;
  `AddActorToActorContainer`/`Remove…` → `Actor[Edit]`;
  `AddStoryToStoryContainer`/`Remove…` → `Story[Edit]`;
  `AddScenarioToUseCase`/`Remove…` and `SetPrimaryScenarioOnUseCase` → `UseCase[Edit]`;
  `EditGoalRelation`/`DeleteGoalRelation` → `Goal[Edit]`; `ConvertStepToScenario` →
  `Scenario[Edit]`. (Open question: container-side vs child-side permission — decide one rule.)
- Copies: `Copy{Goal,Story,Actor,UseCase,Scenario,ScenarioStep}` → the entity's `[Edit]`.
- Deletes: `DeleteUseCase` → `UseCase[Delete]`, `DeleteScenario`/`DeleteScenarioStep` →
  `Scenario[Delete]`, `DeleteGlossaryTerm` → `GlossaryTerm[Delete]`, `DeleteReportGenerator` →
  `ReportGenerator[Delete]`, `DeleteStakeholder` → `Stakeholder[Delete]` (+ gateway guard:
  non-user stakeholders only).
- Annotations: `EditNote`/`EditPosition`/`EditArgument`/`ResolveIssue*`/lexical+spelling+
  dictionary position commands → `Annotation[Edit]`; `Delete{Note,Position,Argument,Issue}` →
  `Annotation[Delete]`.

Verify the `[Delete]` (and any new) permission types exist for every entity before relying on
them; add missing permission definitions as part of this slice.

**Cascade exemption (temporary, tracked by #75):** deletes cascade through detach commands (`RemoveGoalFromGoalContainer`, `DeleteGoalRelation`, etc.) that now require `Edit`, which would block a `Delete`-only stakeholder mid-delete. As a stop-gap, an `AuthorizationExemptable` flag lets a parent delete mark those internal sub-commands exempt from re-authorization; the proper permission-coherence model is #75.

### Slice 3 — in-process `CommandGateway` impl (`service-impl`)
Wrap `ApiCommandFactory` + `CommandHandler`; enforce the allow/deny policy and the
non-user-stakeholder delete guard. Resolve optimistic-lock handling (wire DTO `version` or prove
loaded-state suffices, with a test).

### Slice 4 — write MCP tools (`mcp-server`)
Refactor `mcp-server` tools to call `gateway-api` (fold in today's `ProjectQueryGateway`). Add the
generic `requel.runCommand` + typed convenience tools. Add the write opt-in flag
`requel.gateway.write.enabled` (default `false`; disabled write tools omitted from `tools/list`).
`ImportProject` (multipart) is **excluded** from `runCommand`; a local-file CLI import is a
separate future discussion.



_Updated (Spring AI): build the MCP transport on `spring-ai-starter-mcp-server-webmvc` (HTTP/SSE behind the existing JWT/OAuth2 chain) instead of the hand-rolled JSON-RPC; expose read + write tools via a `ToolCallbackProvider` that delegates to `QueryGateway`/`CommandGateway`; re-wire `McpCallAudit` via a tool advisor. The remote connector (#70) is largely subsumed by this transport. Fine-grained per-stakeholder authz stays in `CommandGateway`; **Spring Security/Requel config handles authentication**, Spring AI supplies transport + tool registration + coarse per-tool gating. Configure endpoint paths + Spring Security matchers explicitly (don't assume `/api/**` inheritance; Spring AI WebMVC defaults differ, e.g. `/sse` + `/mcp/message`). See `doc/port-tospring-boot-ai.md`._

_Split during implementation into two reviewable steps:_
- _**Slice 4a (done):** gateway-backed tool layer over the **existing** hand-rolled JSON-RPC transport — no new framework. Folded `ProjectQueryGateway` into the `gateway-api` `QueryGateway` (impl `InProcessQueryGateway` relocated to `service-impl`); added `McpWriteService` with `requel.runCommand` + curated typed tools (`createProject`, `createGoal`, `editGoal`, `addGoalToContainer`, `createNote`, `createIssue`) over `CommandGateway`; added the `requel.gateway.write.enabled` opt-in flag (default `false`; write tools omitted from `tools/list` and rejected when off). Writes already get both audits (MCP-call audit via `McpCallAuditor` + command audit via the chain)._
- _**Slice 4b (done, additive):** added `spring-ai-starter-mcp-server-webmvc` (first use of the Spring AI 1.1.7 baseline). A `ToolCallbackProvider` (`RequelMcpServerConfig`) is built from `McpReadService.listTools()`, with each `RequelMcpToolCallback` delegating `call()` to `McpReadService.callTool()` — so the Spring AI transport and the hand-rolled JSON-RPC server present identical tools from one implementation. Endpoints are mounted under `/api/mcp/*` (`spring.ai.mcp.server.sse-endpoint` / `sse-message-endpoint`) so the existing `/api/**` JWT chain authenticates them with no new matcher. Per decision, the hand-rolled `McpJsonRpc*` classes are kept alongside (a later cleanup retires them once the Spring AI path is verified end-to-end), and both SSE + Streamable HTTP were requested — SSE is wired and the Streamable toggle is documented in `application.properties` pending confirmation of dual-protocol support in 1.1.7. **Verify at run:** the MCP endpoints must appear under `/api/mcp/*` (401 without a token); if the 1.1.7 property names differ and they land at defaults (`/sse`), they would be outside the JWT chain._

### Slice 5 — identity + audit
Carry a per-client identity, reusing the existing nullable `McpCallAudit.assistantUserId`. Record
both the command-audit row and the MCP-call-audit row per write. Add a per-client rate-limit hook. Set/restore the Spring Security current-user context inside MCP tool execution for both HTTP and stdio paths so `CommandGateway` authz runs as the triggering user.

_Done (HTTP path): the Spring AI tool path now records an MCP-call-audit row (`McpCallAuditor.recordToolCall`, wrapped around `RequelMcpToolCallback.call`), so a write produces both the MCP-call audit row and the command-audit row (the latter already emitted by the chain). On the HTTP transports the triggering user flows automatically — MCP endpoints are under `/api/mcp/**`, the JWT filter populates the `SecurityContext` on the servlet thread the tool executes on, and `CommandGateway`/`CurrentUserCommandHandler` run as that user. A per-client `clientId` is captured from the `X-Requel-Client` header (`McpClientContextFilter` → `McpClientContext`) and carried into `GatewayRequest.clientId`; `assistantUserId` is intentionally left null until real per-client accounts arrive with #73. A no-op rate-limit hook (`McpRateLimiter` + `NoOpMcpRateLimiter`) is invoked once per tool call at the shared `McpReadService.callTool` chokepoint (covers both transports). **Deferred to Slice 7:** explicitly set/restore the current-user context for the stdio path (no servlet thread / JWT there) via `McpTransportContext`._

### Slice 6 — REST-backed gateway client lib
`gateway-client-rest` with login→JWT auth, for out-of-process front-ends.



_Updated (Spring AI): **conditional / deferred** (not unnecessary). In-process stdio launches a full Requel subprocess (needs DB/app config + identity bootstrap); a thin REST bridge may be the simpler operational shape for desktop tools. Keep two distinct deployment shapes: server-hosted WebMVC/SSE for authenticated remote clients, and local stdio (bridge or full-app profile) for desktop clients._

_**Dropped.** A REST client library is not needed. Local stdio clients (Claude/Codex) reach the existing HTTP MCP transport (`/api/mcp/sse`) through the off-the-shelf `mcp-remote` stdio↔HTTP proxy — no Requel-built bridge and no REST client. The only real gap for a persistently-configured local client is durable credentials, tracked as #73 (API key / PAT). See `doc/mcp_remote_connection.md`._

### Slice 7 — `mcp-bridge` stdio front-end + e2e
Standalone stdio MCP server over the REST gateway; decide credential storage. End-to-end smoke:
create a project, goal, association, non-user stakeholder, and note; verify both audit surfaces
and SSE.



_Updated (Spring AI): prefer `spring-ai-starter-mcp-server` (stdio) with API-key (or PAT, #73) identity resolved via `McpTransportContext` into a Requel user — this **likely replaces a separate `mcp-bridge` process**. Requires the Spring AI **1.1.7** baseline (`McpTransportContext` is 1.1 behavior, not in 1.0.8)._

_**Resolved.** The standalone `mcp-bridge` is dropped (covered by `mcp-remote`, above). The **end-to-end smoke test was kept and delivered** as `RequelMcpEndToEndIT` (requel-app): it drives `tools/call` through `McpJsonRpcHandler` as an authorized stakeholder — create goal, create non-user stakeholder (`runCommand`), associate goal→stakeholder, add a note, read project context — and asserts both audit surfaces (command-audit rows for `EditGoal`/`EditNonUserStakeholder`/`AddGoalToGoalContainer`/`EditNote` + MCP-call-audit rows). The full-app stdio profile is not built: the user runs the server for the UI, so the `mcp-remote` path is the local-access story; full-app stdio (`spring.ai.mcp.server.stdio`) remains an option later if a no-server-running deployment is ever needed._

### Closing #69
The gateway, the read/write tool surface, the Spring AI HTTP transport (SSE, verified), and
identity/audit are implemented and tested. Local client access is documented via `mcp-remote`
(`doc/mcp_remote_connection.md`). Write tools default on (`requel.gateway.write.enabled=true`),
with per-stakeholder authorization always enforced. Remaining work is tracked as separate tickets,
not part of #69: **#73** (durable API token/PAT — the real unlock for a persistently-configured
local client), the tracker→goals workflow (**#71**) and smart reconciliation (**#72**), and minor
cleanups (retire the hand-rolled `McpJsonRpc*` server now that the Spring AI transport is proven;
decide the Streamable-HTTP toggle — only SSE is verified).

### Resolved for this ticket
- `ImportProject` / multipart excluded from `runCommand`.
- Per-client identity reuses `assistantUserId` (no new column).
- Each write records both command audit and MCP call audit.

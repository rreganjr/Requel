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
- _**Slice 4b (next):** swap the transport to `spring-ai-starter-mcp-server-webmvc` + `ToolCallbackProvider` (first use of the Spring AI 1.1.7 baseline), retiring the hand-rolled `McpJsonRpc*` classes._

### Slice 5 — identity + audit
Carry a per-client identity, reusing the existing nullable `McpCallAudit.assistantUserId`. Record
both the command-audit row and the MCP-call-audit row per write. Add a per-client rate-limit hook. Set/restore the Spring Security current-user context inside MCP tool execution for both HTTP and stdio paths so `CommandGateway` authz runs as the triggering user.

### Slice 6 — REST-backed gateway client lib
`gateway-client-rest` with login→JWT auth, for out-of-process front-ends.



_Updated (Spring AI): **conditional / deferred** (not unnecessary). In-process stdio launches a full Requel subprocess (needs DB/app config + identity bootstrap); a thin REST bridge may be the simpler operational shape for desktop tools. Keep two distinct deployment shapes: server-hosted WebMVC/SSE for authenticated remote clients, and local stdio (bridge or full-app profile) for desktop clients._

### Slice 7 — `mcp-bridge` stdio front-end + e2e
Standalone stdio MCP server over the REST gateway; decide credential storage. End-to-end smoke:
create a project, goal, association, non-user stakeholder, and note; verify both audit surfaces
and SSE.



_Updated (Spring AI): prefer `spring-ai-starter-mcp-server` (stdio) with API-key (or PAT, #73) identity resolved via `McpTransportContext` into a Requel user — this **likely replaces a separate `mcp-bridge` process**. Requires the Spring AI **1.1.7** baseline (`McpTransportContext` is 1.1 behavior, not in 1.0.8)._

### Resolved for this ticket
- `ImportProject` / multipart excluded from `runCommand`.
- Per-client identity reuses `assistantUserId` (no new column).
- Each write records both command audit and MCP call audit.

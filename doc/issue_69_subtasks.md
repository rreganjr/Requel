## Implementation plan — sliced sub-tasks

We'll implement this ticket as reviewable slices, each on the `69-mcp-command-gateway` branch. Order
reflects dependencies; slices 1–2 unblock the rest.

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

### Slice 5 — identity + audit
Carry a per-client identity, reusing the existing nullable `McpCallAudit.assistantUserId`. Record
both the command-audit row and the MCP-call-audit row per write. Add a per-client rate-limit hook.

### Slice 6 — REST-backed gateway client lib
`gateway-client-rest` with login→JWT auth, for out-of-process front-ends.

### Slice 7 — `mcp-bridge` stdio front-end + e2e
Standalone stdio MCP server over the REST gateway; decide credential storage. End-to-end smoke:
create a project, goal, association, non-user stakeholder, and note; verify both audit surfaces
and SSE.

### Resolved for this ticket
- `ImportProject` / multipart excluded from `runCommand`.
- Per-client identity reuses `assistantUserId` (no new column).
- Each write records both command audit and MCP call audit.

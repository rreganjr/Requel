# MCP Command Gateway + write tools + local stdio bridge

Part of the v2.0 MCP command-gateway series (ticket 1 of 5). Design doc:
`doc/local_mcp_bridge.md`. Related: #43, `doc/assistant-spi-plan.md`.

## Goal

Add a controlled, audited **command gateway** that lets external clients — AI clients and
plain scripts — perform anything a project user can do in Requel: create/edit projects and all
project entities, and manage associations between them. Expose it through MCP tools and ship a
local **stdio MCP bridge** so the capability is demonstrable end-to-end (e.g. from Claude
Desktop / Codex CLI).

The gateway is a thin reach layer over the existing CQRS path. Every mutation goes through
`POST /api/commands/{commandType}` → the `CommandHandler` chain (incl. `AuthorizingCommandHandler`)
→ command → repository, reusing the same validation, audit, and SSE behavior as the Angular UI.
No new write path into the domain is introduced.

Two caveats the code review surfaced, which this ticket must close before claiming UI parity:

- **Authorization is not uniform.** `AuthorizingCommandHandler` only enforces commands that
  implement `AuthorizableCommand`; others pass unchecked at the command-auth layer, and
  `/api/commands/**` is otherwise protected only by `authenticated()`. Some annotation commands
  (e.g. `EditNote`) do not implement the project-stakeholder authorization path that project
  commands use. Each allowlisted command therefore needs an authorization audit.
- **Optimistic locking is not wired.** The DTO `version` field exists (e.g.
  `EditGoalInput.version`) but the current registrars do not propagate it to commands/entities,
  so optimistic-lock parity is not real yet.

This is the foundation ticket. The CLI front-end, the issue-tracker→goals workflow, and smart
reconciliation are tracked as follow-on issues (see "Out of Scope / Follow-ons").

## Background

`mcp-server` already provides an in-process JSON-RPC MCP endpoint (`POST /api/mcp`) with
read-only tools/resources and a `ProjectQueryGateway` abstraction explicitly designed so a
standalone bridge can drop in a REST-backed gateway. The CQRS layer already registers ~40
project command types and the annotation commands via `service-impl` registrars, dispatched by
`CommandController`. JWT auth (`JwtService`, `JwtAuthenticationFilter`, `CurrentUserResolver`,
`ApiSecurityConfig`) is in place. This ticket adds the missing **write** path and an
out-of-process front-end on top of what exists.

There are two audit surfaces today and the ticket must be explicit about both: the MCP call
audit (`mcp_calls` via `McpCallAuditor`) for the JSON-RPC tool call, and the command audit
(`AuditingCommandHandler`) for the underlying CQRS command.

## Scope

In scope:

- A new pure `gateway-api` module: `CommandGateway`, `QueryGateway`, command allow/deny policy,
  and command/input value types. Depends only on `service-api` (no Spring MVC, JPA, or provider
  SDKs).
- In-process `CommandGateway` implementation in `service-impl` (wraps `ApiCommandFactory` +
  `CommandHandler`).
- REST-backed `CommandGateway` / `QueryGateway` implementation (client lib) for out-of-process
  front-ends, including the login→JWT exchange. (Token *refresh* is not available on current
  login JWTs — see open questions; re-login or PAT support, the latter in ticket 5.)
- Write MCP tools: a generic `requel.runCommand(commandType, input)` (exposed to all front-ends,
  AI included; authorization bounds it to what the user could do in the UI) plus typed
  convenience tools (`requel.editGoal`, `requel.editStory`, `requel.editActor`,
  `requel.editUseCase`, `requel.editScenario`, `requel.editGlossaryTerm`,
  `requel.editNonUserStakeholder`, `requel.addEntityToContainer`, `requel.editNote`).
- **Authorization audit** of every allowlisted command before exposure: each either implements
  `AuthorizableCommand` with the correct requirement, or is explicitly gated by gateway policy.
  Annotation, association, delete, and copy commands especially.
- **Optimistic-lock resolution**: wire DTO `version` into commands/entities, or prove via tests
  that loaded-entity state is sufficient — before claiming optimistic-lock parity.
- **`requel.runCommand` input schema**: expose/validate the current input DTOs; decide explicitly
  whether multipart/file commands (e.g. `ImportProject`) are allowed or excluded.
- `mcp-server` refactored so its tools call `gateway-api` (subsuming today's `ProjectQueryGateway`).
- New `mcp-bridge` module: standalone stdio MCP server using the REST-backed gateway, including a
  credential-storage/refresh decision.
- Per-client external pseudo-user + per-client rate-limit hook (see identity note — not present
  today).
- Write opt-in flag: property name `requel.gateway.write.enabled`, default `false`; disabled
  write tools omitted from `tools/list`.

Out of scope (follow-on issues):

- `requel-cli` command-line front-end.
- Remote/HTTPS connector exposure for Cowork (tunnel/host, IP allowlist, Owner registration).
- The issue-tracker → goals workflow conventions (provenance-note format; goal-id-based update).
- Smart reconciliation (`requel.findBestGoalMatch` similarity tool).
- User/identity management of any kind.

## Design

### Module shape

```
gateway-api (pure)         depends on: service-api
  CommandGateway, QueryGateway, AllowDenyPolicy, command/input descriptors + value types

service-impl               in-process CommandGateway impl (ApiCommandFactory + CommandHandler)
gateway-client-rest (lib)  REST-backed CommandGateway/QueryGateway (JWT)

mcp-server   -> gateway-api (in-process)      remote connector front-end (existing endpoint)
mcp-bridge   -> gateway-api (REST-backed)     NEW stdio MCP front-end
```

Concrete Maven note (dependency-cycle check): `ApiCommandFactory` lives in `service-impl`, and
`mcp-server` currently depends on `service-api` + `service-impl`. `gateway-api` must stay free of
Spring MVC/JPA; the in-process impl lives in `service-impl`; `service-impl` must not depend back
on `mcp-server`. Build acceptance: `mvn -pl modules/mcp-server,modules/service-impl -am test`
passes with no dependency cycle. `gateway-api` must export command/input **descriptors** (schemas),
not just an allow/deny predicate, so CLI/typed-tool surfaces can be generated from one source.

### Command coverage via existing commands

Entity create/edit: `EditProject`, `EditGoal`, `EditStory`, `EditActor`, `EditUseCase`,
`EditScenario`, `EditScenarioStep`, `EditGlossaryTerm`, `EditReportGenerator`,
`EditNonUserStakeholder`.

Associations are polymorphic over container interfaces (`Story`/`UseCase`/`Actor`/`Stakeholder`/
`ProjectOrDomain` implement `GoalContainer`/`ActorContainer`/`StoryContainer`), so one command
covers many "add X to Y" cases:

- `AddGoalToGoalContainer` / `RemoveGoalFromGoalContainer`
- `AddActorToActorContainer` / `RemoveActorFromActorContainer`
- `AddStoryToStoryContainer` / `RemoveStoryFromStoryContainer`
- `AddScenarioToUseCase` / `RemoveScenarioFromUseCase` / `SetPrimaryScenarioOnUseCase`
- `EditGoalRelation`, `ConvertStepToScenario`, `Copy*`, entity `Delete*`

No new domain or command code is required for coverage — only tool surface, policy, and the
authorization audit above.

### Command allow/deny policy

- Denylist (never exposed): any system user/role command; `EditUserStakeholder`.
- Delete guard: `DeleteStakeholder` allowed only for non-user stakeholders; reject deletion of
  user stakeholders. This is a **new gateway-level policy**, not existing command behavior —
  current `DeleteStakeholder` resolves any stakeholder by id and deletes it.
- Allowlist: project + entity `Edit*`; `Add*/Remove*` association commands;
  `SetPrimaryScenarioOnUseCase`; `EditGoalRelation`; `ConvertStepToScenario`; `Copy*`; entity
  `Delete*`; `EditGlossaryTerm`; `EditNonUserStakeholder`; annotation commands.
- Policy is enforced in `gateway-api` before dispatch, for every front-end.

### Authentication & identity

- Writes execute as the **authenticated user carried by the JWT**, consistent with the existing
  API and with `McpCallAudit`, which already records `triggeringUserId`
  (`CurrentUserResolver.resolve()`). No shared service account; the JWT identifies the user, who
  is the owning/stakeholder identity for created entities.
- Out-of-process front-ends send `Authorization: Bearer <jwt>` (login→JWT today).
- A per-client external pseudo-user (`claude-desktop`, `codex-cli`, `requel-cli`, …) is intended
  for audit attribution and per-client rate limits. This is **not present today**: `McpCallAudit`
  has nullable `assistantUserId`/`runId` recorded as `null`, and there is no client-id resolution
  or rate-limit implementation. This ticket must define where the client identity enters the
  JSON-RPC/REST/stdio call and either reuse `assistantUserId` or add a model/schema field.
- Authorization for every write is the authenticated user's via `AuthorizingCommandHandler`
  (for commands that implement `AuthorizableCommand` — see the audit requirement); the gateway can
  never perform a write that user could not perform in the UI.
- Project creation: `EditProject` with `project == null` returns a `null` authorization
  requirement and defers to a role-permission check in `execute()`; it auto-creates the
  creator's `UserStakeholder` (the authenticated user, not a new login). The user must hold that
  role permission to create projects.

### Relevant input shapes (current code)

```java
// service-api
public record EditGoalInput(String projectName, Long goalId, String name,
                            String text, Integer version) {}   // registrar does NOT yet apply version
public record EditNoteInput(String projectName, String entityType, Long entityId,
                            Long noteId, String text) {}
```

`EditGoal` is **not** an upsert-by-name: `EditGoalCommandImpl` throws a uniqueness conflict when
creating with an existing name, and updates happen only when `goalId` is set. The workflow ticket
(#N+2) must look up an existing goal id before updating; the gateway just dispatches.

## Security & Privacy

- Write tools are opt-in and off by default (`requel.gateway.write.enabled=false`).
- Authorization is always the authenticated user's; no elevation. Denylist + delete guard enforce
  the no-user-management boundary independently of authorization.
- Treat client-supplied text as untrusted: cap name/text/note lengths and reject oversized
  payloads at the tool boundary before dispatch (the command layer validates as well). Map each
  limit to a concrete field and a specific error code.
- Per-client pseudo-user enables independent rate/concurrency caps so a misbehaving client can be
  throttled or disabled without affecting human users (once implemented per the identity note).
- No tool reaches repositories, SQL, the filesystem, or arbitrary execution — only command
  dispatch through `CommandHandler`.

## Implementation Steps

1. Create `gateway-api` with `CommandGateway`, `QueryGateway`, allow/deny policy, command/input
   descriptors, and value types; depend only on `service-api`.
2. Implement the in-process `CommandGateway` in `service-impl` over `ApiCommandFactory` +
   `CommandHandler`; wire the allow/deny policy and the delete guard.
3. Audit authorization for every allowlisted command (each implements `AuthorizableCommand` with
   the right requirement, or is gated by gateway policy). Resolve optimistic-lock handling (wire
   `version` or prove loaded-state suffices, with a test).
4. Implement the REST-backed `CommandGateway`/`QueryGateway` client lib with login→JWT auth.
5. Refactor `mcp-server` tools to call `gateway-api`; fold the existing `ProjectQueryGateway`
   into `QueryGateway`. Verify no Maven dependency cycle.
6. Add write MCP tools: generic `requel.runCommand` (define DTO schema exposure; decide
   multipart/`ImportProject` allowed vs excluded) + the typed convenience tools.
7. Define where the client identity enters calls; reuse `assistantUserId` or add a field; record
   both the command audit row and the MCP call audit row for each write. Add a per-client
   rate-limit hook and the write opt-in flag (`requel.gateway.write.enabled`, default `false`;
   disabled write tools omitted from `tools/list`).
8. Create the `mcp-bridge` standalone stdio MCP server using the REST-backed gateway; decide
   credential storage and refresh (re-login vs PAT).
9. End-to-end smoke: connect `mcp-bridge` from an MCP client; create a project, a goal, an
   association, a non-user stakeholder, and a note; verify both audit surfaces and SSE.

## Testing Strategy

- Unit tests for allow/deny policy and the delete guard using **real** `DeleteStakeholderInput`
  for both `UserStakeholder` (rejected) and `NonUserStakeholder` (allowed), not synthetic command
  names.
- Authorization-audit tests: confirm each allowlisted command is rejected for an unauthorized
  user (and that no allowlisted command silently bypasses auth by not implementing
  `AuthorizableCommand`).
- Optimistic-lock test: concurrent edit of a goal/entity behaves per the resolved version
  decision.
- Contract tests for `requel.runCommand` and each typed tool with authorized and unauthorized
  users.
- Integration tests proving writes flow through `CommandHandler` and produce the expected
  command-audit and MCP-call-audit rows + SSE, not direct repository writes.
- `mcp-bridge` transport test: stdio JSON-RPC round-trip against an embedded Requel test
  instance; no live network.
- No-network CI profile; real-client smoke tests opt-in.

## Acceptance Criteria

- A pure `gateway-api` module exists, depending only on `service-api`, with in-process and
  REST-backed implementations, and `mvn -pl modules/mcp-server,modules/service-impl -am test`
  passes with no dependency cycle.
- Every allowlisted command has a documented authorization outcome (enforced by
  `AuthorizableCommand` or gated by policy); denylisted commands (system user/role,
  `EditUserStakeholder`) are rejected before dispatch; the delete guard rejects deleting user
  stakeholders.
- Write MCP tools (generic + typed) are available only when `requel.gateway.write.enabled=true`
  and route through the command chain as the authenticated user.
- The `mcp-bridge` stdio server connects from an MCP client and can create/edit projects,
  entities, associations, non-user stakeholders, and notes end-to-end.
- Each MCP write produces the specified audit rows (command audit + MCP call audit) and SSE.
- Optimistic-lock behavior matches the resolved decision (version wired, or documented as
  loaded-state-sufficient with a test).
- Tests above pass on the no-network CI profile.

## Out of Scope / Follow-ons

- #N+1: `requel-cli` front-end + remote connector exposure.
- #N+2: issue-tracker → goals workflow + v1 reconciliation (provenance note; goal-id lookup, not
  name-upsert).
- #N+3: smart reconciliation (`requel.findBestGoalMatch`).
- #N+4: user-mintable, revocable API JWTs / personal access tokens for identifying users to
  remote tools.

## Open Questions

- Is `ImportProject` (multipart/file) exposed through `requel.runCommand`, or explicitly excluded?
- Reuse `assistantUserId` for the per-client pseudo-user, or add a dedicated client-id field/table?
- Which audit rows are mandatory for an MCP write — command audit, MCP call audit, or both
  (current lean: both)?

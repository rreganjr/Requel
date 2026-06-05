# Codex review: issue_mcp_command_gateway.md

Review target: `doc/issue_mcp_command_gateway.md`

## Findings

### 1. The document overstates existing authorization coverage

The doc says every mutation will inherit authorization, validation, audit, optimistic locking,
and SSE exactly as the Angular UI because writes go through
`POST /api/commands/{commandType}` -> `AuthorizingCommandHandler` -> command -> repository.
That is directionally right, but not fully accurate for the current code base:

- `AuthorizingCommandHandler` only checks commands implementing `AuthorizableCommand`.
  Non-authorizable commands pass through unchecked at the command-auth layer
  (`modules/requel-app/src/main/java/com/rreganjr/requel/command/AuthorizingCommandHandler.java`).
- The existing Spring API security protects most `/api/commands/**` only with
  `authenticated()`, except the non-existent `/api/commands/NewUser` rule
  (`modules/service-impl/src/main/java/com/rreganjr/requel/service/config/ApiSecurityConfig.java`).
- Several annotation commands, including `EditNote`, do not implement the same project
  stakeholder authorization path as project commands
  (`modules/annotation-jpa/src/main/java/com/rreganjr/requel/annotation/impl/command/EditNoteCommandImpl.java`).

The ticket should explicitly require an authorization audit of every allowlisted command before
exposing it through MCP, especially annotations and association/delete/copy commands.

### 2. Optimistic locking is claimed, but current DTO versions are not applied

The doc lists optimistic locking as inherited behavior and shows `EditGoalInput` with a
`version` field. Current `ProjectCommandRegistrar` ignores `EditGoalInput.version()` when
building `EditGoalCommand`; it sets project/goal/name/text only. The same pattern appears in
other command registrations.

Acceptance criteria should not claim optimistic locking for gateway writes until the API
registrars actually propagate versions to commands/entities or tests prove that the existing
loaded entity state is enough.

### 3. The `EditNoteInput` example is stale

The doc shows:

```java
public record EditNoteInput(String entityType, Long entityId, String text, Long noteId) {}
```

The current DTO is:

```java
public record EditNoteInput(String projectName, String entityType, Long entityId,
                            Long noteId, String text) {}
```

This matters because any generic gateway/tool schema generated from the document would omit
`projectName`, which the current API DTO exposes.

### 4. Module placement needs a dependency-cycle check

The proposed `gateway-api` depends only on `service-api`, and `service-impl` implements the
in-process command gateway over `ApiCommandFactory` and `CommandHandler`. That may be workable,
but the current `ApiCommandFactory` is in `service-impl`, while `mcp-server` currently depends
on both `service-api` and `service-impl`. Moving `mcp-server` to `gateway-api` will require
careful wiring so `gateway-api` does not pull Spring MVC/JPA types and `service-impl` does not
need to depend back on MCP.

The issue should call out the concrete intended dependency graph in Maven terms and include a
build acceptance criterion such as `mvn -pl modules/mcp-server,modules/service-impl -am test`.

### 5. Per-client pseudo-user support is not present in the current audit implementation

The doc relies on per-client pseudo-users for audit attribution and rate limits. Current
`McpCallAudit` has nullable `assistantUserId` and `runId`, while `McpCallAuditor` currently
records `assistantUserId` and `runId` as `null`. There is no current client-id/pseudo-user
resolution path or rate-limit implementation.

The implementation steps should include schema/model changes or a concrete reuse plan for
`assistantUserId`, plus where the client identity enters JSON-RPC/REST/stdio calls.

### 6. Audit wording conflates MCP call audit with command audit

The acceptance criteria say writes appear in the command audit log and emit SSE. The code has
an MCP audit table (`mcp_calls`) and an `AuditingCommandHandler`, but those are distinct audit
surfaces. The ticket should specify which audit rows must be produced for an MCP write:

- command audit for the underlying CQRS command,
- MCP call audit for the JSON-RPC tool call,
- or both.

### 7. Delete guard is underspecified against current command behavior

The doc correctly proposes a gateway-level guard preventing `DeleteStakeholder` for user
stakeholders. Current `DeleteStakeholder` registration resolves any stakeholder by id and passes
it to the command; the command path itself is not limited to non-user stakeholders. The guard is
therefore a required new gateway policy, not a restatement of existing command behavior.

The tests should include a real `DeleteStakeholderInput` for both `UserStakeholder` and
`NonUserStakeholder`, not only a policy-unit test with synthetic command names.

## Completeness gaps

- The generic `requel.runCommand` schema needs a way to expose/validate current input DTOs,
  including multipart/file commands such as `ImportProject` if they are intentionally allowed
  or explicitly excluded.
- The write opt-in flag should state its property name, default, and whether disabled tools are
  hidden from `tools/list` or listed but rejected.
- Payload size limits should be mapped to concrete fields and error codes.
- The stdio bridge needs a credential-storage decision; the ticket mentions login/JWT but not
  where the bridge stores or refreshes credentials.


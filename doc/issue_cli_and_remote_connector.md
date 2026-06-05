# MCP gateway front-ends: requel-cli + remote connector

Part of the v2.0 MCP command-gateway series (ticket 2 of 5). Design doc:
`doc/local_mcp_bridge.md`. Depends on the Command Gateway ticket (series ticket 1).

## Goal

Add the remaining front-ends over the `gateway-api` core delivered in ticket 1: a command-line
interface (`requel-cli`) for human/scripting use, and the operational work to expose the
in-process MCP endpoint as a remote connector usable from Cowork.

Both are thin layers over the existing gateway; this ticket adds reach and packaging, not new
domain or command behavior. The current code base has only a **generic** in-process MCP JSON-RPC
endpoint at `/api/mcp`; there is no provider-specific OAuth or remote-connector envelope yet, so
everything in the "remote connector" half is new work, not configuration of something present.

## Background

Ticket 1 delivers `gateway-api` (`CommandGateway`/`QueryGateway` + allow/deny policy + command
**descriptors/schemas**), an in-process implementation in `service-impl`, a REST-backed client
implementation, the write MCP tools (generic `requel.runCommand` + typed tools), and the
`mcp-bridge` stdio front-end. This ticket reuses all of that. Current API security accepts
Requel JWT bearer tokens from `/api/auth/login`; `JwtService` issues fixed-expiry JWTs and there
is **no refresh-token endpoint**.

## Scope

In scope:

- **`requel-cli`** — a new command-line module over the REST-backed gateway. Supports the generic
  `runCommand` (any allowlisted command type with a JSON/flag input) and convenience subcommands
  mirroring the typed tools (goal/story/actor/use-case/scenario/glossary-term/non-user-stakeholder
  edits, container associations, notes). Auth is login→JWT, with **re-login on expiry** (or PAT
  support once ticket 5 lands) — not JWT "refresh", which the current API does not support.
- **Generated command surface** — CLI subcommands are generated from ticket 1's command
  **descriptors/schemas**, not from an allow/deny predicate alone. This ticket depends on ticket 1
  exporting those descriptors.
- **Remote connector exposure** — operationalize the existing in-process `/api/mcp` (now with
  write tools) as a custom remote connector:
  - terminate TLS on a reachable host (deployed instance or tunnel; `localhost` is not reachable
    by Anthropic),
  - **OAuth user mapping** (explicit subtask): how an external OAuth identity maps to a Requel
    username/user id, how account linking is approved, and how the resulting request becomes a
    Spring Security authentication compatible with `CurrentUserResolver`,
  - specify how JSON-RPC clients send auth, how failed auth is reported, and whether write tools
    are hidden from `tools/list` or listed-and-rejected when `requel.gateway.write.enabled=false`,
  - allowlist Anthropic inbound IP ranges,
  - document the Owner-side registration (Organization settings → Connectors) for a
    Team/Enterprise account.

Out of scope:

- The issue-tracker→goals workflow and reconciliation (later tickets).
- User-mintable API tokens (ticket 5) — until then the connector uses OAuth and the CLI uses
  login→JWT.

## Design

- `requel-cli` depends on `gateway-api` + the REST gateway client; no Spring MVC server. The
  command surface is generated from ticket 1's command descriptors so CLI and MCP stay in
  lockstep; command discovery can be static (generated) and/or runtime (`tools/list`) — decide in
  this ticket.
- The remote connector is the same `mcp-server` endpoint from ticket 1; this ticket is the
  deployment/auth/ops envelope around it, plus docs. Provider-specific (Cowork/Anthropic)
  reachability and registration are explicitly new work.
- Authorization remains the authenticated user's via `AuthorizingCommandHandler`; the per-client
  pseudo-user (`requel-cli`, plus the connector's client id) provides audit attribution and rate
  limits (the pseudo-user/rate-limit mechanism itself is built in ticket 1).

## Security & Privacy

- CLI stores credentials/tokens per OS-appropriate secret conventions (documented location per
  OS), never in checked-in config.
- Remote connector: TLS-only, IP allowlist, OAuth with a defined Requel-user mapping; writes still
  opt-in (`requel.gateway.write.enabled`) and bounded by command authorization.
- Per-client pseudo-user enables independent rate/concurrency caps.

## Implementation Steps

1. Create the `requel-cli` module over the REST gateway client; generate `run` (generic) and
   convenience subcommands from ticket 1's command descriptors.
2. Add login→JWT auth, re-login-on-expiry (PAT later), and per-OS config/secret handling.
3. Define CLI exit codes and JSON/stdout output formats for scripting; define command discovery
   (static generated, runtime `tools/list`, or both).
4. Package/distribute the CLI (fat jar or native launcher).
5. Stand up a reachable TLS host or tunnel for `/api/mcp`; implement OAuth at the connector
   boundary with the Requel-user mapping and Spring Security integration.
6. Specify connector auth transport, failed-auth reporting, and write-tool visibility when the
   write flag is off.
7. Allowlist Anthropic inbound IPs; document Owner-side connector registration.
8. Smoke tests (below).

## Testing Strategy

- CLI command-parsing and descriptor-coverage unit tests.
- CLI integration tests against an embedded Requel instance (no live network), including exit
  codes and output formats.
- Generic-MCP staging smoke test (auth, a read, a gated write) — separate from provider-specific
  connector registration.
- Connector auth/round-trip + OAuth-user-mapping test in a staging environment.

## Acceptance Criteria

- `requel-cli` can perform the full allowlisted authoring surface against a running Requel via the
  gateway, with login→JWT auth and re-login on expiry, with documented exit codes/output formats.
- The `/api/mcp` endpoint is reachable as a remote connector over TLS with OAuth mapped to a
  Requel user, IP allowlisting, defined auth-failure behavior, and documented Owner registration.
- Write-tool visibility under the write flag (hidden vs rejected) is defined and tested.
- Authorization and audit behave identically to UI-driven writes.

## Dependencies

- Series ticket 1 (Command Gateway + write tools + stdio bridge; command descriptors;
  pseudo-user + rate-limit mechanism).

## Open Questions

- CLI distribution format (fat jar vs native image)?
- One connector client registration per environment, or per user?
- Command discovery: static generated, runtime `tools/list`, or both?

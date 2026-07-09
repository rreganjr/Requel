# MCP gateway front-ends: requel-cli + remote connector

Part of the v2.0 MCP command-gateway series (ticket 2 of 5). Design doc:
`doc/local_mcp_bridge.md`. Depends on the Command Gateway ticket (series ticket 1).

> **Status update (post-#83).** This ticket predates the OAuth work; the landscape has since shifted:
> - Series ticket 1 (**#69** — Command Gateway + write tools + stdio bridge + command descriptors)
>   is **merged**.
> - **#83** delivered OAuth 2.1 on `/api/mcp/**`: an embedded authorization server backed by the
>   Requel user store (authorization-code + PKCE, resource server, RFC 9728 discovery, gated DCR).
> - **#73** delivered user-mintable, revocable PATs.
>
> Consequences: the OAuth-boundary and token-"refresh" open items below are **resolved**, and the
> remote-connector auth design is largely **de-scoped** — the embedded AS *is* the Requel user
> store, so there is no external-identity→Requel-user mapping to invent (that only arises if an
> external IdP is added later, a separate ticket). Remaining work splits into two phases:
>
> - **Phase A — `requel-cli` (near-term, unblocked).** Builds on merged pieces (#69 in-process
>   gateway + catalog, #83/#73 for auth); no external infrastructure. Note: #69 shipped only the
>   *in-process* gateway — the **REST gateway client is built in Phase A** (own module
>   `gateway-rest-client`). **Implementation plan: `doc/70-requel-cli-plan.md`.**
> - **Phase B — remote MCP connector (deferred, ops-gated).** Operationalize the now
>   OAuth-protected `/api/mcp` as a Cowork/Anthropic custom connector: reachable TLS host, IP
>   allowlist, Owner-side registration. Deployment work with external dependencies, not code that
>   merges in one PR — kept separate so it can't block Phase A. May be split into its own issue.

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
  edits, container associations, notes). **Auth (updated for #73/#83):** prefer a **PAT** (#73) for
  headless/scripting use; offer **OAuth login** (authorization-code + PKCE, 30-day rotating refresh
  from #83) for interactive use. The original "login→JWT + re-login on expiry" is superseded — no
  fixed-expiry-JWT re-login needed.
- **Generated command surface** — CLI subcommands are generated from ticket 1's command
  **descriptors/schemas**, not from an allow/deny predicate alone. This ticket depends on ticket 1
  exporting those descriptors.
- **Remote connector exposure (Phase B)** — operationalize `/api/mcp` (now OAuth-protected via #83)
  as a custom remote connector:
  - terminate TLS on a reachable host (deployed instance or tunnel; `localhost` is not reachable
    by Anthropic),
  - ~~OAuth user mapping~~ — **resolved by #83**: the embedded AS authenticates against the Requel
    user store, so the OAuth user *is* a Requel user; the resource server maps the token subject to
    the user via `CurrentUserResolver` already. No external-identity linking unless an external IdP
    is added later.
  - specify how JSON-RPC clients send auth, how failed auth is reported, and whether write tools
    are hidden from `tools/list` or listed-and-rejected when `requel.gateway.write.enabled=false`,
  - allowlist Anthropic inbound IP ranges,
  - document the Owner-side registration (Organization settings → Connectors) for a
    Team/Enterprise account.
  - **Signing-key caveat:** the AS signing key is ephemeral today (#83 follow-up); a remotely
    registered connector needs a persistent keystore so tokens survive restarts.

Out of scope:

- The issue-tracker→goals workflow and reconciliation (later tickets).
- External identity provider support (OAuth account-linking to a third-party IdP) — a separate
  future ticket; the embedded AS from #83 covers the connector's auth needs.

## Design

- `requel-cli` depends on `gateway-api` + the REST gateway client; no Spring MVC server. The
  command surface is generated from ticket 1's command descriptors so CLI and MCP stay in
  lockstep; command discovery is static-generated + runtime-reconciled (see Decisions).
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
2. Add auth: PAT (#73) for headless + OAuth login (#83) for interactive, and per-OS config/secret
   handling.
3. Define CLI exit codes and JSON/stdout output formats for scripting; implement command discovery
   (static generated from #69 descriptors, reconciled at runtime against `tools/list` — see
   Decisions).
4. Package/distribute the CLI as a **fat jar** (+ a thin `requel` wrapper over `java -jar`).
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
  gateway, authenticating with a PAT (headless) or OAuth login (interactive), with documented exit
  codes/output formats.
- The `/api/mcp` endpoint is reachable as a remote connector over TLS with OAuth mapped to a
  Requel user, IP allowlisting, defined auth-failure behavior, and documented Owner registration.
- Write-tool visibility under the write flag (hidden vs rejected) is defined and tested.
- Authorization and audit behave identically to UI-driven writes.

## Dependencies

- **#69** (Command Gateway + write tools + stdio bridge; command descriptors; pseudo-user +
  rate-limit mechanism) — **merged**.
- **#83** (OAuth 2.1 on `/api/mcp`) and **#73** (PATs) — **merged**; provide the CLI/connector auth.

## Decisions (were open questions)

- **CLI distribution → fat jar.** Trivial with the existing Maven build, runs anywhere with a JVM,
  and a scripting CLI tolerates JVM startup. Native (GraalVM) image is deferred — the Spring/Jackson
  reflection config and per-OS builds aren't worth it for a single-maintainer tool yet; revisit only
  if it becomes a widely distributed end-user binary. Ship a thin `requel` wrapper over `java -jar`.
- **Connector registration → one per environment.** One registered OAuth client per deployed Requel;
  each user authenticates individually (per-user consent + tokens, single client) — the standard
  "one client, many users" model, and how an Org custom connector works. Not per-user client
  registration.
- **Command discovery → both, static-primary.** Generate static subcommands from #69's command
  descriptors (help, tab-completion, offline use, scripting ergonomics), and reconcile at runtime
  against the server's `tools/list`/descriptors so the CLI reflects what the connected server
  actually permits — notably the `requel.gateway.write.enabled` flag (write commands surface as
  clearly unavailable, not a raw 500). Generic `runCommand` is always the escape hatch. An MVP may
  start static-only and add the runtime check when drift / write-flag handling requires it.

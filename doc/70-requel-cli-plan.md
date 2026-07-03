# requel-cli — Phase A implementation plan (issue #70)

Phase A of #70: a command-line front-end over the gateway, buildable now (depends only on merged
#69 gateway + #83/#73 auth). Phase B (remote connector ops) is deferred — see the issue.

## Grounding: what actually exists today (verified in-tree)

Two things the #70 background text implies but the code does **not** yet have — both become Phase A
work:

- **No REST-backed gateway client.** Only in-process impls exist: `InProcessCommandGateway` /
  `InProcessQueryGateway` in `service-impl` (package `com.rreganjr.requel.service.gateway`). The
  `CommandGateway` interface's javadoc anticipates a REST impl that POSTs to
  `/api/commands/{commandType}`, but it isn't built. The CLI needs it — **build it in Phase A.**
- **No `mcp-bridge` module.** So there's no bridge front-end to "mirror"; `requel-cli` is its own
  module.
- **The command catalog is server-side only.** `GatewayCommandCatalog` (`gateway-api`) →
  `CommandDescriptor(commandType, inputType, title, description, write, authorizationHint)` is
  implemented in `service-impl` from the `CommandRegistration` entries. There is **no REST endpoint
  exposing it**. So a standalone CLI can't do true build-time static generation without new codegen
  infra; runtime discovery via a small new endpoint is the pragmatic route (below).

What we can lean on: the CQRS write path (`POST /api/commands/{type}`), the existing GET query
endpoints, `/api/auth/login` + `/api/auth/tokens` (PAT, #73), and the OAuth2 endpoints (#83).

## Module layout

- **New module `gateway-rest-client` (decided: its own module).** A REST-backed `CommandGateway` +
  `QueryGateway` implementing the `gateway-api` interfaces by calling Requel over HTTP with an
  injected bearer token. Its own module (not folded into `requel-cli`) so a future remote/other
  front-end can reuse it. Uses Spring's `RestClient` (already on the classpath via `spring-web`) +
  Jackson.
- **New module `requel-cli`**: the CLI app. Depends on `gateway-api` (interfaces + `CommandDescriptor`),
  `service-api` (input/output DTOs), the REST client above, `picocli`, and Jackson. **No Spring MVC
  server**; a minimal Spring context (or none) — prefer plain `main()` + picocli, wiring the REST
  client by hand, to keep startup fast and the jar lean.

## CLI framework: picocli

Nothing CLI-ish is on the classpath today (no picocli/jcommander/args4j). Use **picocli** — mature,
annotation-driven subcommands, generated `--help`, `@|...|@` styling, shell-completion generation,
and GraalVM-friendly if we ever want native (we don't for MVP). Add `info.picocli:picocli` to the
new module.

## Command surface & discovery

Two layers, so the CLI works offline *and* stays in lockstep with the server:

1. **Generic `run` (always available, no server round-trip to invoke):**
   `requel run <CommandType> --input @file.json` or `--input '{...json...}'`. Dispatches through the
   REST `CommandGateway` to `POST /api/gateway/commands/<CommandType>` (the server-side gateway
   facade — see below), so the allow/deny policy + authorization are enforced server-side. This is
   the reliable, scriptable core and needs no descriptors.
2. **Runtime catalog discovery (`requel commands`, Milestone 4 — done):** the CLI fetches the
   descriptor catalog from the server and lists the exposed write commands, so the operator sees
   exactly what the connected server allows — including the `requel.gateway.write.enabled` flag (the
   list is **empty** when writes are off, mirroring how the MCP server hides write tools), so it
   can't drift.
   - **Server-side addition (done):** `GET /api/gateway/commands/descriptors` returning the
     `GatewayCommandCatalog` as JSON (`commandType, inputType (simple name), title, description,
     write, authorizationHint`). Served by `GatewayCommandController`, gated by the write flag.
   - **Client side (done):** `RestGatewayCatalog` in `gateway-rest-client` fetches it;
     `requel commands` prints it (text or `--output json`).
   - **Deferred (richer form):** dynamically *registering picocli subcommands* per command (with
     per-field flags derived from the input DTO's JSON schema). MVP uses the generic
     `requel run <CommandType> --input` for invocation; `requel commands` for discovery.
   - Offline / server-unreachable: only `run` + built-ins show in `--help`; that's acceptable.

(This refines the earlier "static-primary" decision: because the catalog is server-side with no
static export, discovery is **runtime-primary** with the generic `run` as the offline fallback.
Build-time static generation stays a possible later enhancement if offline typed help matters.)

## Auth (PAT + OAuth, per #73/#83)

The REST client just needs a bearer token; the CLI manages obtaining/refreshing it.

- **PAT (headless/scripting, simplest):** `requel login --token reqpat_…` (or `REQUEL_TOKEN` env, or
  read from the credential store). Stored and sent as `Authorization: Bearer`. Mint via the UI or
  `POST /api/auth/tokens`.
- **OAuth login (interactive):** `requel login` runs authorization-code + PKCE against the #83 AS with
  a **loopback callback** (same pattern Claude Code uses), stores the access + refresh tokens, and
  refreshes automatically (1h access / 30d rotating refresh). On refresh-expiry, prompt to re-login.
  - **Server-side addition needed (small):** seed a public, loopback, PKCE `requel-cli` OAuth client
    (like the seeded dev client in `AuthorizationServerConfig`) so `requel login` works out of the box
    without the operator having to mint a DCR initial token. Gated DCR stays the default for other
    clients; this is one known first-party client.
- **Token storage:** per-OS, `chmod 600` file under `~/.config/requel/credentials` (documented), or
  OS keychain later. Never in checked-in config. Config precedence: flag > env > credential file.

## Config, output, exit codes

- **Server URL:** `--url` / `REQUEL_URL` / config; default `http://localhost:8080`.
- **Output:** `--output text|json` (default `text`; `json` prints the raw `GatewayResult`/query DTO
  for scripting).
- **Exit codes** (for scripting): `0` success; `2` usage error; `3` auth failure (401/expired);
  `4` not-allowed/unauthorized (gateway `NOT_ALLOWED`/`UNAUTHORIZED`); `5` server/execution error;
  `1` unexpected. Map `GatewayException.Kind` → codes.

## Packaging

- **Fat jar** via `spring-boot-maven-plugin` repackage (or `maven-shade-plugin`), main = the picocli
  entry point. Ship a thin `requel` wrapper (`exec java -jar …`). Native image explicitly out of
  scope (see issue Decisions).

## Server-side additions needed (small, in `service-impl`)

1. **Gateway REST facades (done in Milestone 1):** `GatewayCommandController`
   (`POST /api/gateway/commands/{type}`, delegates to the in-process `CommandGateway` so the
   allow/deny policy is enforced server-side; returns the exact `GatewayException.Kind` in the error
   body) and `GatewayQueryController` (`GET /api/gateway/query/**`, delegates to the in-process
   `QueryGateway`). The REST client targets these, not the raw `/api/commands` / UI query endpoints.
2. **Command catalog (done in Milestone 4):** `GatewayCommandCatalogImpl` (`service-impl`) implements
   the `GatewayCommandCatalog` interface — the previously-unimplemented single source of truth —
   derived from `GatewayPolicyConfig.ALLOWED` ∩ registered commands (input type via
   `ApiCommandFactory`), so it can't drift from the allow/deny policy. Exposed at
   `GET /api/gateway/commands/descriptors` (write-flag-gated). **Follow-up (separate ticket):** the MCP
   server still generates its typed tools from its own hard-coded list; migrating MCP tool generation
   onto this catalog (with an `MCP tools ⊆ catalog` lockstep test) changes a shipped tool surface, so
   it's deliberately out of Milestone 4.
3. Seed a `requel-cli` public/PKCE/loopback OAuth client in `AuthorizationServerConfig` (guarded by a
   flag, like the dev client), so interactive `requel login` works without a DCR initial token.
   (Milestone 5.)

## Milestones (incremental, each independently testable)

1. `gateway-rest-client`: REST `CommandGateway`/`QueryGateway` + a `RestClient` bearer holder; unit
   tests against a stubbed server.
2. `requel-cli` skeleton with picocli, `--url`/config, `requel run` generic dispatch, output + exit
   codes.
3. Auth: `requel login --token` (PAT) + credential store; wire bearer into the REST client.
4. Shared `GatewayCommandCatalog` impl + `GET /api/gateway/commands/descriptors` endpoint +
   `RestGatewayCatalog` client + `requel commands` discovery subcommand. (MCP tool-generation
   migration onto the catalog deferred to a separate ticket.)
5. OAuth `requel login` (code+PKCE, loopback, refresh) + the seeded `requel-cli` client.
6. Packaging (fat jar + wrapper) and docs.

## Testing strategy

- **Unit:** picocli parsing, `GatewayException.Kind`→exit-code mapping, descriptor→subcommand
  generation, credential-store round-trip.
- **Integration:** boot the app with `@SpringBootTest(webEnvironment = RANDOM_PORT)` and drive the CLI
  `main()` against it (PAT auth) — a real read (`listProjects`), a gated write (`createGoal` on a
  project the test user can edit), and a denied write → assert output + exit codes. No live network.
- **Auth:** PAT accepted; expired/invalid → exit 3. OAuth code+PKCE happy path can be an integration
  test against the embedded AS (reuse the #83 harness).

## Decided

- **Command discovery: runtime-primary** — fetch descriptors from
  `GET /api/gateway/commands/descriptors` (implemented as the `requel commands` listing); generic
  `run` is the offline fallback. Build-time static generation and dynamic per-command subcommand
  registration both deferred.
- **Shared catalog is authoritative (Milestone 4 decision "B").** `GatewayCommandCatalogImpl` is the
  single source of truth, built from the same `ALLOWED` set as the gateway policy. Migrating MCP's
  typed-tool generation onto it is a separate follow-up ticket (it alters a shipped tool surface).
- **`gateway-rest-client` is its own module** (not inlined in `requel-cli`), for reuse.

## Open sub-decisions (resolve during implementation)

- Typed subcommand flags derived from the input DTO's JSON schema vs. every typed subcommand also just
  taking `--input <json>` for MVP (recommend `--input` first; per-field flags later).
- Credential store: flat `chmod 600` file (MVP) vs. OS keychain (later).

## Out of scope (Phase B — deferred)

Remote MCP connector ops: reachable TLS host/tunnel, Anthropic IP allowlist, Owner-side connector
registration, and the persistent AS signing key (#83 follow-up) that a remote connector needs. See
the #70 issue body.

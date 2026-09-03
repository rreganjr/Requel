# Connecting a local Claude / Codex to Requel over MCP

This recipe connects a local MCP client (Codex, Claude Code, Claude Desktop, etc.) to a
**locally-running Requel server** so the client can read and write project data through the Requel
MCP command gateway (issue #69). It needs no plugin, no MCP registry entry, and no custom bridge
code — Requel serves the **Streamable HTTP** MCP transport directly (issue #98).

## How it fits together

```
Codex / Claude Code / VS Code  --Streamable HTTP (OAuth or PAT)-->  Requel  POST /api/mcp
                                                                    (your local server)
```

Requel serves MCP over **Streamable HTTP** at `POST /api/mcp` (issue #98). Streamable HTTP is the
MCP spec's forward direction; the legacy HTTP+SSE transport is deprecated and no longer served.
Every interactive client we use speaks Streamable natively, so they connect **directly** — no bridge.
The server runs each call through the gateway, so **per-stakeholder permissions and auditing apply
exactly as they do in the UI** — the client acts as whichever Requel user authenticated.

`/api/mcp/**` is an **OAuth 2.1 protected resource** (issue #83): on a 401 it advertises the
authorization server via RFC 9728 metadata, and a compliant client runs the discovery →
authorization-code + PKCE flow to obtain a token — no pre-shared secret. **Personal access tokens
(PATs) and the SPA login JWT are still accepted** on the same endpoints, for headless/CI use and
quick tests. Pick the auth style in "Authentication" below.

> **Pure-stdio clients only:** a client that cannot speak remote HTTP at all (only stdio) still needs
> the community [`mcp-remote`](https://www.npmjs.com/package/mcp-remote) stdio→HTTP proxy pointed at
> `http://localhost:8080/api/mcp`. `mcp-remote` defaults to Streamable HTTP, so no transport flag is
> needed. See "Pure-stdio fallback" at the end.

## Prerequisites

- A Requel server running locally (the same instance you use for the UI), e.g. on
  `http://localhost:8080`.
- Write tools: enabled by default (`requel.gateway.write.enabled=true`). To run the MCP server
  read-only, start Requel with `--requel.gateway.write.enabled=false`.
- For the pure-stdio fallback only: Node.js on PATH (so the client can run `npx mcp-remote`).

## Authentication

Two ways to authenticate the MCP endpoints; choose by how the client runs:

- **OAuth 2.1 (preferred for interactive clients — Codex, Claude Code, Claude Desktop/Cowork).**
  The client discovers the authorization server from the 401 and runs authorization-code + PKCE; you
  log in with your Requel credentials in the browser and approve a consent screen once. No token to
  paste or store. See "OAuth 2.1 connection" below. The full end-to-end walkthrough (and how to
  enable the AS + register a client) is in `doc/83-oauth-verification.md`.
- **Personal access token (PAT) — preferred for headless / CI / scripts, and fine for a quick test.**
  A durable, revocable bearer you mint once and put in the `Authorization` header. See "Get a token
  (PAT)".

Either way the client acts as **one Requel user**, and that user's per-stakeholder permissions govern
what the tools can read and write. Scope the user to what the assistant should do.

## OAuth 2.1 connection (preferred for interactive clients)

The MCP server (issue #83) is an OAuth 2.1 protected resource with an embedded authorization server
backed by the Requel user store. A client connects with no pre-shared token: it reads the RFC 9728
metadata from the 401, discovers the AS, and runs authorization-code + PKCE (you log in + consent in
the browser).

**Enabling and client registration** are covered step-by-step in `doc/83-oauth-verification.md`.
Requel uses Spring Authorization Server's OIDC Dynamic Client Registration, which is **gated by an
initial access token** (not anonymous); registered clients are forced to loopback redirect URIs,
PKCE, consent, and the `mcp` scope.

> Because DCR is gated, a client must either accept a **pre-registered `client_id`** (register one
> per the recipe below) or supply an initial access token — Requel does not allow anonymous
> registration. If a client can *only* self-register anonymously and can't be configured otherwise,
> use the PAT path instead.

### Codex over OAuth (native Streamable) — confirmed recipe

Codex speaks native remote MCP over OAuth on **Streamable HTTP**, which Requel serves. Codex performs
**anonymous** Dynamic Client Registration and cannot be handed a pre-registered `client_id`, so it
relies on the loopback-restricted anonymous DCR path added in #238. No `mcp-remote` bridge, no PAT,
and no manual client registration.

1. Start Requel with the anonymous-loopback DCR path enabled. The **dev profile turns this on**
   (`requel.oauth.dcr.allow-anonymous-loopback=true`), so `--spring.profiles.active=dev` is enough;
   otherwise pass `--requel.oauth.dcr.allow-anonymous-loopback=true`. (#238 also advertises
   `registration_endpoint` in the RFC 8414 `oauth-authorization-server` metadata — without it Codex
   reports "Dynamic client registration not supported" and never POSTs.)

2. Add the server and log in (ensure no `REQUEL_TOKEN` is set, so Codex authenticates via OAuth rather
   than sending a bearer):

   ```bash
   codex mcp add --transport http requel http://localhost:8080/api/mcp
   codex mcp login requel
   ```

   Codex discovers `/connect/register`, self-registers (anonymous, loopback callback), then opens a
   browser for your Requel login + one-time consent; on success `requel` shows connected and the tools
   appear. The registered client is public/PKCE, scope `mcp`, consent-required — the same policy as
   gated DCR.

> Verified working (issue #238): `codex mcp login requel` with no PAT and no pre-registration. If a
> future Codex build changes its callback host/path such that the loopback check rejects it, read the
> `redirect_uri` from the first login attempt and confirm it is a `127.0.0.1`/`localhost` loopback URI.

### Claude Code over OAuth — confirmed recipe

Claude Code accepts a pre-registered `client_id` and a fixed callback port, so it works with Requel's
gated DCR without anonymous registration.

1. Start Requel with the AS + DCR enabled and a registrar secret (as above).

2. Mint a single-use initial access token and register a client whose redirect URIs are Claude Code's
   loopback callback (register **both** host forms — Spring AS matches `redirect_uri` exactly):

   ```bash
   INIT=$(curl -s -u requel-registrar:<secret> -X POST http://localhost:8080/oauth2/token \
     -d grant_type=client_credentials -d scope=client.create | jq -r .access_token)
   curl -s -X POST http://localhost:8080/connect/register \
     -H "Authorization: Bearer $INIT" -H 'Content-Type: application/json' \
     -d '{"client_name":"claude-code","redirect_uris":["http://127.0.0.1:8899/callback","http://localhost:8899/callback"],"grant_types":["authorization_code","refresh_token"]}' | jq -r .client_id
   ```

3. Add the server (public/PKCE client — no secret) over Streamable HTTP and authenticate:

   ```bash
   claude mcp add --transport http requel http://localhost:8080/api/mcp \
     --callback-port 8899 --client-id <client_id> --scope user
   ```
   Then in an interactive Claude Code session run `/mcp`, select `requel`, and **Authenticate**
   (`claude mcp login requel` also works on recent Claude Code). A browser opens for Requel login +
   consent; on success the server shows connected and the tools appear.

If **Authenticate** fails with `invalid_redirect_uri`, Claude Code used a different callback path/host
— read it from the browser URL and re-register the client with that exact loopback URI, then re-add
with the new `client_id`.

## Get a token (PAT — headless / quick test)

The MCP endpoint accepts a bearer credential, same as any `/api/**` call. For a PAT — a durable,
revocable token you mint once and leave configured — first log in to get a short-lived JWT, then use
it to mint the PAT:

```bash
# one-time: log in, then mint a named PAT (optionally expiresInDays)
JWT=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"your-username","password":"your-password"}' | jq -r .token)

curl -s -X POST http://localhost:8080/api/auth/tokens \
  -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
  -d '{"name":"codex","expiresInDays":365}'
# -> {"token":"reqpat_…","tokenInfo":{...}}   (the reqpat_ value is shown ONCE)
```

Use the `reqpat_…` value as the bearer. It doesn't expire on the JWT's ~8h schedule, and you can
revoke it any time: `GET /api/auth/tokens` to list, `DELETE /api/auth/tokens/{id}` to revoke —
revocation takes effect on the token's next request.

**Quick alternative — the login JWT** (`…/api/auth/login` → `token`): fine for a one-off test, but it
expires in ~8h, so a left-configured client will stop working. Prefer the PAT for anything persistent.

Either way the client acts as **that user** — its stakeholder permissions on each project govern what
the tools can read and write. Use a user scoped to what the assistant should do (e.g. `Goal[Edit]`,
`Annotation[Edit]`), not an over-privileged admin.

## Configure the client (PAT / static header)

### VS Code / Copilot (native Streamable)

`.vscode/mcp.json` (or the user-profile config) — VS Code speaks Streamable HTTP with `"type":
"http"`:

```json
{
  "servers": {
    "requel": {
      "type": "http",
      "url": "http://localhost:8080/api/mcp",
      "headers": {
        "Authorization": "Bearer ${input:requel_token}",
        "X-Requel-Client": "vscode"
      }
    }
  }
}
```

### Claude Desktop / Cowork

Add a **remote connector** in Settings → Connectors pointing at `http://localhost:8080/api/mcp`
(Claude Desktop will not connect to remote servers configured via `claude_desktop_config.json`; use
the Connectors UI). Authenticate with OAuth in the browser, or supply the PAT as a bearer header if
the connector UI allows custom headers.

The `X-Requel-Client` header is optional; Requel records it for per-client audit attribution
(`GatewayRequest.clientId` / the MCP call audit). Tool names are bare — no `requel.` prefix — because
MCP tool names must match `^[a-zA-Z0-9_-]{1,64}$`; dots are rejected by spec-compliant clients.

## Verify

With the server running and a token in `$REQUEL_TOKEN`:

```bash
# Unauthenticated -> 401 (the MCP endpoint is behind the OAuth2 resource-server / JWT chain)
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'

# Authenticated initialize -> a JSON-RPC (or SSE-framed) result with serverInfo/capabilities
curl -s -X POST http://localhost:8080/api/mcp \
  -H "Authorization: Bearer $REQUEL_TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"curl","version":"0"}}}'
```

The easiest end-to-end check is the **MCP Inspector**: connect it to `http://localhost:8080/api/mcp`
over Streamable HTTP, list tools, and call `listProjects`. In a real client, list tools and call
`listProjects` to confirm reads, then (if writes are on) `EditGoal` against a project you have
`Goal[Edit]` on.

## Pure-stdio fallback (mcp-remote)

Only for a client that can't speak remote HTTP at all. `mcp-remote` bridges the client's stdio to
Requel's Streamable endpoint; it defaults to Streamable HTTP, so no transport flag is needed:

```json
{
  "mcpServers": {
    "requel": {
      "command": "npx",
      "args": [
        "mcp-remote",
        "http://localhost:8080/api/mcp",
        "--header", "Authorization: Bearer ${REQUEL_TOKEN}",
        "--header", "X-Requel-Client: stdio-bridge"
      ],
      "env": { "REQUEL_TOKEN": "<paste a PAT from 'Get a token'>" }
    }
  }
}
```

If a GUI-spawned client mangles `${VAR}` expansion in `args` (observed on some clients), use a tiny
gitignored wrapper script that loads the token from a gitignored `.env` and `exec`s the same
`npx mcp-remote … /api/mcp` command.

## Security notes

- **Permissions still apply.** The flag `requel.gateway.write.enabled` is only a coarse on/off for
  the whole write surface. Every command is still authorized per the acting user's stakeholder
  permissions, and identity/user-management commands are denied at the gateway regardless.
- **The client acts as the token's user.** Treat the token like a password. Scope the user's
  stakeholder permissions to what the assistant should be able to do.
- **Auditing.** Every tool call is recorded as an MCP-call audit row, and every write additionally
  produces a command-audit row attributed to the acting user.

## Durable credentials (PATs, #73)

The login JWT is short-lived (~8h by default, `requel.jwt.expiry-hours`), so it's a poor fit for a
left-configured client. Personal access tokens (#73) are the durable answer: mint a named, revocable
`reqpat_…` token for a chosen user, drop it in the `Authorization` header, and manage it via
`GET`/`DELETE /api/auth/tokens` (revocation takes effect on the token's next request, and only the
SHA-256 hash is stored server-side). Token *scoping* (read-only vs write-set) is a later enhancement;
today a PAT carries its owner's full rights. Interactive OAuth 2.1 for agent clients that drive the
discovery flow is implemented (#83) — see "OAuth 2.1 connection" above and
`doc/83-oauth-verification.md`; PATs remain the headless/CI path.

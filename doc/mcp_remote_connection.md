# Connecting a local Claude / Codex to Requel over MCP

This recipe connects a local MCP client (Claude Desktop, Codex, etc.) to a **locally-running
Requel server** so the client can read and write project data through the Requel MCP command
gateway (issue #69). It needs no plugin, no MCP registry entry, and no custom bridge code — it
reuses the HTTP MCP transport the server already exposes, with the community
[`mcp-remote`](https://www.npmjs.com/package/mcp-remote) tool bridging the client's stdio to it.

## How it fits together

```
Claude / Codex  --stdio-->  mcp-remote  --HTTP+SSE (JWT)-->  Requel  /api/mcp/sse
                (spawned by the client)                       (your local server)
```

Most desktop MCP clients only speak **stdio** (they spawn a server process and talk over its
pipes). Requel exposes MCP over **HTTP** (SSE / Streamable HTTP) under `/api/mcp`. `mcp-remote` is a
tiny stdio-to-HTTP proxy the client launches; it forwards MCP traffic to the server and either drives
the OAuth flow or attaches a static `Authorization` header. The server runs the calls through the
gateway, so **per-stakeholder permissions and auditing apply exactly as they do in the UI** — the
client acts as whichever Requel user authenticated.

`/api/mcp/**` is an **OAuth 2.1 protected resource** (issue #83): on a 401 it advertises the
authorization server via RFC 9728 metadata, and a compliant client runs the discovery →
authorization-code + PKCE flow to obtain a token — no pre-shared secret. **Personal access tokens
(PATs) and the SPA login JWT are still accepted** on the same endpoints, for headless/CI use and
quick tests. Pick the auth style in "Authentication" below.

There is intentionally no Requel-built stdio bridge and no separate REST client library: the HTTP
transport plus `mcp-remote` covers local access.

## Prerequisites

- A Requel server running locally (the same instance you use for the UI), e.g. on
  `http://localhost:8080`.
- Node.js available (so the client can run `npx mcp-remote`).
- Write tools: enabled by default (`requel.gateway.write.enabled=true`). To run the MCP server
  read-only, start Requel with `--requel.gateway.write.enabled=false`.

## Authentication

Two ways to authenticate the MCP endpoints; choose by how the client runs:

- **OAuth 2.1 (preferred for interactive clients — Cursor, Claude Code, Claude Desktop/Cowork).**
  The client discovers the authorization server from the 401 and runs authorization-code + PKCE; you
  log in with your Requel credentials in the browser and approve a consent screen once. No token to
  paste or store. See "OAuth 2.1 connection" below. The full end-to-end walkthrough (and how to
  enable the AS + register a client) is in `doc/83-oauth-verification.md`.
- **Personal access token (PAT) — preferred for headless / CI / scripts, and fine for a quick test.**
  A durable, revocable bearer you mint once and put in the `Authorization` header. See "1. Get a
  token (PAT)".

Either way the client acts as **one Requel user**, and that user's per-stakeholder permissions govern
what the tools can read and write. Scope the user to what the assistant should do.

## OAuth 2.1 connection (preferred for interactive clients)

The MCP server (issue #83) is an OAuth 2.1 protected resource with an embedded authorization server
backed by the Requel user store. A client connects with no pre-shared token: it reads the RFC 9728
metadata from the 401, discovers the AS, and runs authorization-code + PKCE (you log in + consent in
the browser).

**Enabling and client registration** are covered step-by-step in `doc/83-oauth-verification.md`
(start the server with the AS enabled, mint an initial access token from the registrar client, and
register the client — or use `mcp-remote --static-oauth-client-info` with a pre-registered
`client_id`). Requel uses Spring Authorization Server's OIDC Dynamic Client Registration, which is
**gated by an initial access token** (not anonymous); registered clients are forced to loopback
redirect URIs, PKCE, consent, and the `mcp` scope.

> Because DCR is gated, a client must either accept a **pre-registered `client_id`** (register one
> per the recipe below) or supply an initial access token — Requel does not allow anonymous
> registration. If a client can *only* self-register anonymously and can't be configured otherwise,
> use the PAT path below. (Claude Code accepts a pre-registered `client_id`; see the recipe.)

### Claude Code (VS Code) over OAuth — confirmed recipe

Verified end to end (issue #83, Part D). Claude Code accepts a pre-registered `client_id` and a fixed
callback port, so it works with Requel's gated DCR without anonymous registration.

1. Start Requel with the AS + DCR enabled and a registrar secret:
   `--requel.oauth.seed-dev-client=true --requel.oauth.dcr.enabled=true --requel.oauth.dcr.registrar-client-secret=<secret>`

2. Mint a single-use initial access token and register a client whose redirect URIs are Claude Code's
   loopback callback (register **both** host forms — Spring AS matches `redirect_uri` exactly):

   ```bash
   INIT=$(curl -s -u requel-registrar:<secret> -X POST http://localhost:8080/oauth2/token \
     -d grant_type=client_credentials -d scope=client.create | jq -r .access_token)
   curl -s -X POST http://localhost:8080/connect/register \
     -H "Authorization: Bearer $INIT" -H 'Content-Type: application/json' \
     -d '{"client_name":"claude-code","redirect_uris":["http://127.0.0.1:8899/callback","http://localhost:8899/callback"],"grant_types":["authorization_code","refresh_token"]}' | jq -r .client_id
   ```
   Do NOT send a `scope` field — Spring AS validates it against the initial token (`client.create`
   only); the client is stamped with scope `mcp` automatically.

3. Add the server (public/PKCE client — no secret) and authenticate:

   ```bash
   claude mcp add --transport sse requel http://localhost:8080/api/mcp/sse \
     --callback-port 8899 --client-id <client_id> --scope user
   ```
   Then in an interactive Claude Code session run `/mcp`, select `requel`, and **Authenticate**
   (`claude mcp login requel` also works on Claude Code ≥ v2.1.186). A browser opens for Requel login
   + consent; on success the server shows connected and the tools appear.

If **Authenticate** fails with `invalid_redirect_uri`, Claude Code used a different callback path/host
— read it from the browser URL and re-register the client with that exact loopback URI, then re-add
with the new `client_id`.

## 1. Get a token (PAT — headless / quick test)

The MCP endpoints accept a bearer credential, same as any `/api/**` call. For a PAT:

**A personal access token (PAT, #73):** a durable, revocable token you mint once and
leave configured. First log in to get a short-lived JWT, then use it to mint the PAT:

```bash
# one-time: log in, then mint a named PAT (optionally expiresInDays)
JWT=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"your-username","password":"your-password"}' | jq -r .token)

curl -s -X POST http://localhost:8080/api/auth/tokens \
  -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
  -d '{"name":"claude-desktop","expiresInDays":365}'
# -> {"token":"reqpat_…","tokenInfo":{...}}   (the reqpat_ value is shown ONCE)
```

Use the `reqpat_…` value as the bearer below. It doesn't expire on the JWT's ~8h schedule, and you
can revoke it any time: `GET /api/auth/tokens` to list, `DELETE /api/auth/tokens/{id}` to revoke —
revocation takes effect on the token's next request.

**Quick alternative — the login JWT** (`…/api/auth/login` → `token`): fine for a one-off test, but it
expires in ~8h, so a left-configured client will stop working. Prefer the PAT for anything persistent.

Either way the client acts as **that user** — its stakeholder permissions on each project govern what
the tools can read and write. Use a user scoped to what the assistant should do (e.g. `Goal[Edit]`,
`Annotation[Edit]`), not an over-privileged admin.

## 2. Configure the client

### Claude Desktop

In `claude_desktop_config.json` (Settings → Developer → Edit Config):

```json
{
  "mcpServers": {
    "requel": {
      "command": "npx",
      "args": [
        "mcp-remote",
        "http://localhost:8080/api/mcp/sse",
        "--transport", "sse-only",
        "--header", "Authorization: Bearer ${REQUEL_TOKEN}",
        "--header", "X-Requel-Client: claude-desktop"
      ],
      "env": { "REQUEL_TOKEN": "<paste the JWT from step 1>" }
    }
  }
}
```

Restart Claude Desktop. The Requel tools (`listProjects`, `getProject`, `createGoal`, `runCommand`,
…) appear in the tools list. Write tools only appear when the server has writes enabled.

`--transport sse-only` is important: `mcp-remote` defaults to Streamable HTTP and probes it first;
since Requel currently serves only SSE, forcing SSE avoids a wasted Streamable-HTTP probe. This is a
**transport** setting, not auth — it is independent of whether you authenticate via OAuth or a static
header (the example above passes a static PAT). (Tool names are bare — no `requel.` prefix — because
MCP tool names must match `^[a-zA-Z0-9_-]{1,64}$`; dots are rejected by spec-compliant clients.)

### Codex (or any stdio MCP client)

Point the client's MCP server command at the same proxy invocation:

```
npx mcp-remote http://localhost:8080/api/mcp/sse \
  --transport sse-only \
  --header "Authorization: Bearer $REQUEL_TOKEN" \
  --header "X-Requel-Client: codex"
```

### Cursor (and the robust wrapper-script pattern)

Some clients (Cursor observed) pre-expand `$VAR` references and mangle quoting in the `args` of
`mcp.json` before spawning the process, which produced an empty `Authorization: Bearer ` header.
The reliable pattern is a tiny wrapper script that the client launches; the shell reads everything
verbatim, loads the token from a gitignored `.env`, and execs the proxy. Keep the token only in
`.env` — never in the script or `mcp.json`.

`.cursor/mcp.json` (the per-developer config; gitignored):

```json
{ "mcpServers": { "requel": { "command": "/abs/path/to/requel-mcp.sh" } } }
```

`requel-mcp.sh` (also gitignored; make it executable):

```sh
#!/bin/sh
# Load nvm (GUI-spawned shells often lack it on PATH) and the JWT from .env, then exec the proxy.
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
set -a; . /abs/path/to/Requel/.env; set +a   # provides REQUEL_JWT
exec npx -y mcp-remote http://localhost:8080/api/mcp/sse \
  --transport sse-only \
  --header "Authorization: Bearer $REQUEL_JWT" \
  --header "X-Requel-Client: cursor"
```

The same wrapper works for Claude Code (`claude mcp add`) when inline header expansion misbehaves.

The `X-Requel-Client` header is optional; Requel records it for per-client audit attribution
(`GatewayRequest.clientId` / the MCP call audit). Per-developer `.cursor/` and `.claude/` configs
are gitignored — only this portable recipe is committed.

## 3. Verify

With the server running and a token in `$REQUEL_TOKEN`, you can sanity-check the endpoint directly
before wiring a client:

```bash
# Unauthenticated -> 401 (the MCP endpoints are behind the JWT chain)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/mcp/sse

# Authenticated SSE handshake -> an "event: endpoint" line with the session message URL
curl -N -H "Authorization: Bearer $REQUEL_TOKEN" http://localhost:8080/api/mcp/sse
```

In the client, list tools and call `listProjects` to confirm reads, then (if writes are on)
`createGoal` against a project you have `Goal[Edit]` on.

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
left-configured client. Personal access tokens (#73) are the durable answer: mint a named,
revocable `reqpat_…` token for a chosen user (step 1), drop it in the `Authorization` header, and
manage it via `GET`/`DELETE /api/auth/tokens` (revocation takes effect on the token's next request,
and only the SHA-256 hash is stored server-side). Token *scoping* (read-only vs write-set) is a
later enhancement; today a PAT carries its owner's full rights. Interactive OAuth 2.1 for agent
clients that drive the discovery flow is implemented (#83) — see "OAuth 2.1 connection" above and
`doc/83-oauth-verification.md`; PATs remain the headless/CI path.

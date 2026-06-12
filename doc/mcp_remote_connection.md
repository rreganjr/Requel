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
pipes). Requel exposes MCP over **HTTP** (SSE / Streamable HTTP) under `/api/mcp`, behind the same
JWT auth as the rest of `/api/**`. `mcp-remote` is a tiny stdio-to-HTTP proxy the client launches;
it forwards MCP traffic to the server and attaches the `Authorization` header. The server runs the
calls through the gateway, so **per-stakeholder permissions and auditing apply exactly as they do
in the UI** — the client acts as whichever Requel user the token belongs to.

There is intentionally no Requel-built stdio bridge and no separate REST client library: the HTTP
transport plus `mcp-remote` covers local access, and the per-client-identity work (durable tokens)
is tracked separately (see "Durable credentials" below).

## Prerequisites

- A Requel server running locally (the same instance you use for the UI), e.g. on
  `http://localhost:8080`.
- Node.js available (so the client can run `npx mcp-remote`).
- Write tools: enabled by default (`requel.gateway.write.enabled=true`). To run the MCP server
  read-only, start Requel with `--requel.gateway.write.enabled=false`.

## 1. Get a token

The MCP endpoints require a bearer credential, same as any `/api/**` call. Two options:

**Recommended — a personal access token (PAT, #73):** a durable, revocable token you mint once and
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
since Requel currently serves only SSE, the probe fails and `mcp-remote` falls into an OAuth flow our
server doesn't expose. Forcing SSE avoids that. (Tool names are bare — no `requel.` prefix — because
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

In the client, list tools and call `requel.listProjects` to confirm reads, then (if writes are on)
`requel.createGoal` against a project you have `Goal[Edit]` on.

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
clients that drive the discovery flow is a separate track (#83).

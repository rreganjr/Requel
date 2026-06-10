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

The MCP endpoints require a JWT, same as any `/api/**` call. Log in as the Requel user you want the
client to act as:

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"your-username","password":"your-password"}'
# -> {"token":"<JWT>", ...}
```

The client will act as **that user** — its stakeholder permissions on each project govern what the
tools can read and write. Use a user that is a stakeholder with the permissions you intend to grant
the assistant (e.g. `Goal[Edit]`, `Annotation[Edit]`), not an over-privileged admin.

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
        "--header", "Authorization: Bearer ${REQUEL_TOKEN}",
        "--header", "X-Requel-Client: claude-desktop"
      ],
      "env": { "REQUEL_TOKEN": "<paste the JWT from step 1>" }
    }
  }
}
```

Restart Claude Desktop. The Requel tools (`requel.listProjects`, `requel.getProject`,
`requel.createGoal`, `requel.runCommand`, …) appear in the tools list. Write tools only appear when
the server has writes enabled.

### Codex (or any stdio MCP client)

Point the client's MCP server command at the same proxy invocation:

```
npx mcp-remote http://localhost:8080/api/mcp/sse \
  --header "Authorization: Bearer $REQUEL_TOKEN" \
  --header "X-Requel-Client: codex"
```

The `X-Requel-Client` header is optional; Requel records it for per-client audit attribution
(`GatewayRequest.clientId` / the MCP call audit).

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

## Durable credentials (follow-on: #73)

The login JWT is short-lived (~8h by default, `requel.jwt.expiry-hours`), so a left-configured
client will need its token refreshed. Long-lived API keys / personal access tokens — minted for a
chosen user and dropped into the `Authorization` header — are tracked in **issue #73**; that is the
intended way to make this setup pleasant for a persistently-configured desktop client. Until then,
re-run step 1 to refresh the token when it expires.

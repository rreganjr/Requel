# Connecting VS Code to the Requel MCP server

This is the VS Code companion to [`doc/mcp_remote_connection.md`](mcp_remote_connection.md) (Codex
CLI, Claude Desktop, PAT minting). It explains how to drive Requel's MCP tools from inside VS Code
using the **Claude Code** extension, **VS Code's own (Copilot) MCP support**, or the **Codex**
extension.

## Summary

Requel serves MCP over **Streamable HTTP** at `http://localhost:8080/api/mcp` (issue #98), behind
the OAuth 2.1 resource-server chain on `/api/mcp/**` (issue #83). The legacy HTTP+SSE transport is
deprecated and no longer served. Two facts decide the VS Code setup:

1. **Requel serves Streamable HTTP only** (`spring.ai.mcp.server.protocol=STREAMABLE`,
   `streamable-http.mcp-endpoint=/api/mcp`). All three VS Code paths below speak Streamable natively,
   so they connect **directly** — no `mcp-remote` bridge.
2. **Auth is OAuth 2.1 or a static bearer.** Interactive clients can run the browser OAuth flow
   (issue #83); or attach a **PAT** (`reqpat_…`) / login JWT as an `Authorization: Bearer` header.

| VS Code path | Speaks Streamable HTTP? | How to connect to Requel |
| --- | --- | --- |
| **Claude Code** extension | Yes (`--transport http`) | Direct, native — recommended |
| **VS Code / Copilot** native MCP | Yes (`"type": "http"`) | Direct, native |
| **Codex** extension | Yes (native remote MCP) | Direct, native (OAuth or bearer) |

## Prerequisites

- A Requel server running locally (the same instance you use for the UI), e.g.
  `http://localhost:8080`.
- Auth: either run the OAuth flow (see [`doc/mcp_remote_connection.md`](mcp_remote_connection.md) and
  `doc/83-oauth-verification.md`), or mint a **personal access token (PAT)** once and reuse it:

  ```bash
  JWT=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"your-username","password":"your-password"}' | jq -r .token)

  curl -s -X POST http://localhost:8080/api/auth/tokens \
    -H "Authorization: Bearer $JWT" -H 'Content-Type: application/json' \
    -d '{"name":"vscode","expiresInDays":365}'
  # -> {"token":"reqpat_…", ...}   (shown ONCE; use this as the bearer below)
  ```

  The client acts as **that user**; its per-stakeholder permissions govern what the tools can read
  and write. Scope the user to what the assistant should do, not an admin. Write tools only appear
  when the server runs with `requel.gateway.write.enabled=true` (the default).

## Auto-loading the token with direnv

To have the PAT exported into your shell automatically whenever you `cd` into this folder — the same
way the per-account GitHub token is loaded — use [direnv](https://direnv.net) (already hooked in
`~/.zshrc` via `eval "$(direnv hook zsh)"`).

Keep the secret **outside the repo**, mirroring the GH token pattern
(`~/.config/gh-tokens/<account>`): store the PAT in `~/.config/requel-tokens/rreganjr`. A gitignored
`.envrc` at the repo root reads it:

```sh
# .envrc  (gitignored)
_requel_token_file="$HOME/.config/requel-tokens/rreganjr"
if [ -r "$_requel_token_file" ]; then
  export REQUEL_PAT="$(tr -d '\r\n' < "$_requel_token_file")"
  export REQUEL_TOKEN="$REQUEL_PAT"   # alias used by docs/scripts
fi
```

One-time setup (paste the `reqpat_…` value into the token file):

```sh
mkdir -p ~/.config/requel-tokens && chmod 700 ~/.config/requel-tokens
printf '%s' 'reqpat_…' > ~/.config/requel-tokens/rreganjr   # paste your PAT
chmod 600 ~/.config/requel-tokens/rreganjr
cd /Users/rregan_platformq/gh-acc/rreganjr/Requel && direnv allow
```

After that, `$REQUEL_TOKEN` is set in any terminal opened in the folder — including VS Code's
integrated terminal and the Claude Code CLI — so the `claude mcp add … "Bearer $REQUEL_TOKEN"`
commands below pick the token up with no extra steps. (VS Code's native MCP, Option B, reads the
token from its own `inputs` prompt rather than the shell, so direnv isn't required there.)

## Option A — Claude Code extension (recommended)

The Claude Code VS Code extension uses the Claude Code CLI underneath and shares its MCP config, so
this also works from a plain terminal. Claude Code speaks Streamable HTTP, so point it straight at
Requel — no bridge.

With a static PAT/JWT:

```bash
claude mcp add --transport http requel http://localhost:8080/api/mcp \
  --header "Authorization: Bearer $REQUEL_TOKEN" \
  --header "X-Requel-Client: vscode-claude-code"
```

Or over OAuth (no token to paste) — see the confirmed recipe in
[`doc/mcp_remote_connection.md`](mcp_remote_connection.md): `claude mcp add --transport http requel
http://localhost:8080/api/mcp --callback-port 8899 --client-id <client_id> --scope user`, then
`claude mcp login requel`.

Scope flags: add `--scope project` to write a shared `.mcp.json` in the repo, or `--scope user`
(default is `local`) to keep it private to you. The `X-Requel-Client` header is optional; Requel
records it for per-client audit attribution.

Verify inside VS Code with `/mcp` (or `claude mcp list` in a terminal), then ask the assistant to
list projects (the `listProjects` tool) — tool names are **bare**, no `requel.` prefix.

## Option B — VS Code's built-in (Copilot) MCP support

VS Code's native MCP support (Copilot agent mode — the "MCP servers" UI in Settings) speaks
Streamable HTTP with `"type": "http"`. Add a server in one of two places:

- **Workspace:** `.vscode/mcp.json` (note the `servers` key here is top-level, not under `mcp`).
- **User profile:** via *MCP: Open User Configuration* — keeps the token off the repo entirely.

```jsonc
// .vscode/mcp.json
{
  "servers": {
    "requel": {
      "type": "http",
      "url": "http://localhost:8080/api/mcp",
      "headers": {
        "Authorization": "Bearer ${input:requelToken}",
        "X-Requel-Client": "vscode"
      }
    }
  },
  "inputs": [
    {
      "id": "requelToken",
      "type": "promptString",
      "description": "Requel PAT (reqpat_…) or JWT",
      "password": true
    }
  ]
}
```

Using an `inputs` prompt keeps the token out of the file — important here because, unlike `.cursor/`
and `.claude/`, **`.vscode/` is not gitignored** in this repo. If you would rather hardcode the
token, put it in the **user-profile** config instead, or add `/.vscode/mcp.json` to `.gitignore`
first. Never commit a bearer token.

Then open the MCP servers view, start `requel`, and confirm the tools appear in Copilot's agent-mode
tool picker. (VS Code can also drive the OAuth flow against `/api/mcp` instead of a static header.)

## Option C — Codex extension

The Codex IDE extension shares `~/.codex/config.toml` with the Codex CLI and speaks native remote MCP
over Streamable HTTP, so it connects to `/api/mcp` **directly** — no `mcp-remote` bridge.

```toml
# ~/.codex/config.toml   (or .codex/config.toml in a trusted project)
[mcp_servers.requel]
url = "http://localhost:8080/api/mcp"
# Auth, pick one:
#   - OAuth:  run `codex mcp login requel` (see the #98 recipe in doc/mcp_remote_connection.md)
#   - Bearer: bearer_token_env_var = "REQUEL_TOKEN"
```

For the OAuth path, Codex attempts anonymous Dynamic Client Registration, which Requel's gated
`/connect/register` rejects — pre-register a client whose `redirect_uri` matches Codex's loopback
callback (read the exact value from a first `codex mcp login` attempt). The full recipe and the
runtime-verify caveat are in [`doc/mcp_remote_connection.md`](mcp_remote_connection.md).

## Verify (any option)

With the server up and the token in `$REQUEL_TOKEN`, sanity-check the endpoint before wiring a
client:

```bash
# Unauthenticated -> 401 (MCP endpoint sits behind the OAuth2 resource-server / JWT chain)
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/mcp \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}'

# Authenticated initialize -> a result with serverInfo/capabilities
curl -s -X POST http://localhost:8080/api/mcp \
  -H "Authorization: Bearer $REQUEL_TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"curl","version":"0"}}}'
```

The easiest check is the **MCP Inspector** pointed at `http://localhost:8080/api/mcp` over Streamable
HTTP. In the client, list tools and call `listProjects` to confirm reads; if writes are enabled, try
`EditGoal` against a project you have `Goal[Edit]` on.

## Security notes

- **The token is a password.** It carries its owner's full Requel rights (PAT scoping is a future
  enhancement). Keep it in a user-profile config, an `inputs` prompt, or a gitignored `.env` — never
  committed. `.vscode/` is **not** gitignored in this repo, so be deliberate about Option B.
- **Permissions still apply.** Every tool call runs through the gateway under the acting user's
  stakeholder permissions; identity/user-management commands are denied regardless.
  `requel.gateway.write.enabled` is only a coarse on/off for the whole write surface.
- **Auditing.** Every tool call is recorded as an MCP-call audit row; every write also produces a
  command-audit row attributed to the acting user.

## Related docs

- [`doc/mcp_remote_connection.md`](mcp_remote_connection.md) — Codex CLI / Claude Desktop, PAT
  minting, OAuth recipes, the pure-stdio `mcp-remote` fallback.
- [`doc/83-oauth-verification.md`](83-oauth-verification.md) — enabling the authorization server and
  registering a client for the OAuth 2.1 flow (issue #83).

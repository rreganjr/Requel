# Connecting VS Code to the Requel MCP server

This is the VS Code companion to [`doc/mcp_remote_connection.md`](mcp_remote_connection.md) (which
covers Claude Desktop, Codex CLI, and Cursor). It explains how to drive Requel's MCP tools from
inside VS Code now that Cursor is no longer available, using either the **Claude Code** extension,
**VS Code's own (Copilot) MCP support**, or the **Codex** extension.

## Summary

Requel exposes MCP over **HTTP SSE** at `http://localhost:8080/api/mcp/sse`, behind the same
JWT/PAT auth as the rest of `/api/**`. There is **no OAuth yet** (tracked in
[`doc/oauth_mcp_plan.md`](oauth_mcp_plan.md), issue #83), so every client must attach a static
bearer token and must not be allowed to fall into an OAuth discovery flow.

Two facts decide which VS Code path is easiest:

1. **Requel serves SSE only.** Streamable HTTP is configured-but-commented in
   `application.properties` (`spring.ai.mcp.server.protocol=STREAMABLE` is off). The live endpoints
   are `spring.ai.mcp.server.sse-endpoint=/api/mcp/sse` and
   `spring.ai.mcp.server.sse-message-endpoint=/api/mcp/message`.
2. **Auth is a static bearer header** (a PAT `reqpat_…` or a login JWT) — no OAuth.

| VS Code path | Speaks legacy SSE? | How to connect to Requel |
| --- | --- | --- |
| **Claude Code** extension | Yes (`--transport sse`) | Direct, native — recommended |
| **VS Code / Copilot** native MCP | Yes (`"type": "sse"`) | Direct, native |
| **Codex** extension | No — stdio + Streamable HTTP only | Via the `mcp-remote` stdio bridge |

Because Requel is SSE-only, Claude Code and VS Code's built-in MCP can talk to it **directly** (no
`mcp-remote` needed). Codex cannot speak legacy SSE, so it still needs the `mcp-remote` bridge from
the existing recipe.

## Prerequisites

- A Requel server running locally (the same instance you use for the UI), e.g.
  `http://localhost:8080`.
- A bearer token. Mint a **personal access token (PAT)** once and reuse it — see
  [`doc/mcp_remote_connection.md` §1](mcp_remote_connection.md). In short:

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
- For the Codex path only: Node.js on PATH (so `npx mcp-remote` works).

## Auto-loading the token with direnv

To have the PAT exported into your shell automatically whenever you `cd` into this folder — the same
way the per-account GitHub token is loaded — use [direnv](https://direnv.net) (already hooked in
`~/.zshrc` via `eval "$(direnv hook zsh)"`).

Keep the secret **outside the repo**, mirroring the GH token pattern
(`~/.config/gh-tokens/<account>`): store the PAT in `~/.config/requel-tokens/rreganjr`. Nothing
secret then lives in the working tree, so there's nothing for GitHub secret-scanning to flag and
nothing to commit by accident. A gitignored `.envrc` at the repo root reads it:

```sh
# .envrc  (gitignored)
_requel_token_file="$HOME/.config/requel-tokens/rreganjr"
if [ -r "$_requel_token_file" ]; then
  export REQUEL_PAT="$(tr -d '\r\n' < "$_requel_token_file")"
  export REQUEL_TOKEN="$REQUEL_PAT"   # alias: .cursor scripts use REQUEL_PAT, docs use REQUEL_TOKEN
fi
```

One-time setup (paste the `reqpat_…` value into the token file):

```sh
mkdir -p ~/.config/requel-tokens && chmod 700 ~/.config/requel-tokens
printf '%s' 'reqpat_…' > ~/.config/requel-tokens/rreganjr   # paste your PAT
chmod 600 ~/.config/requel-tokens/rreganjr
cd /Users/rregan_platformq/gh-acc/rreganjr/Requel && direnv allow
```

Run `direnv allow` again after any edit to `.envrc`. After that, both `$REQUEL_PAT` and
`$REQUEL_TOKEN` are set in any terminal opened in the folder — including VS Code's integrated
terminal and the Claude Code CLI — so the `claude mcp add … "Bearer $REQUEL_TOKEN"` and `mcp-remote`
commands below pick the token up with no extra steps. (VS Code's native MCP, Option B, reads the
token from its own `inputs` prompt rather than the shell, so direnv isn't required there.)

> The old `.env` at the repo root (used by the legacy `.cursor/` scripts) is no longer needed once
> the PAT lives in `~/.config`. Move it out and delete `.env` — see the migration command below.

## Option A — Claude Code extension (recommended)

The Claude Code VS Code extension uses the Claude Code CLI underneath and shares its MCP config, so
this also works from a plain terminal. Claude Code supports SSE natively, so point it straight at
Requel — no bridge.

```bash
claude mcp add --transport sse requel http://localhost:8080/api/mcp/sse \
  --header "Authorization: Bearer $REQUEL_TOKEN" \
  --header "X-Requel-Client: vscode-claude-code"
```

Scope flags: add `--scope project` to write a shared `.mcp.json` in the repo, or `--scope user`
(default is `local`) to keep it private to you. Note SSE is marked deprecated in Claude Code in
favour of Streamable HTTP, but it still works and Requel only serves SSE today, so `--transport sse`
is correct. The `X-Requel-Client` header is optional; Requel records it for per-client audit
attribution.

Verify inside VS Code with `/mcp` (or `claude mcp list` in a terminal), then ask the assistant to
list projects (the `listProjects` tool) — tool names are **bare**, no `requel.` prefix.

If inline header expansion ever misbehaves (it did under Cursor), fall back to the gitignored
wrapper-script + `mcp-remote` pattern documented in
[`doc/mcp_remote_connection.md` §2](mcp_remote_connection.md); the same wrapper works for
`claude mcp add`.

## Option B — VS Code's built-in (Copilot) MCP support

VS Code's native MCP support (Copilot agent mode — the "MCP servers" UI you saw in Settings) speaks
SSE directly. Add a server in one of two places:

- **Workspace:** `.vscode/mcp.json` (note the `servers` key here is top-level, not under `mcp`).
- **User profile:** via *MCP: Open User Configuration* — keeps the token off the repo entirely.

```jsonc
// .vscode/mcp.json
{
  "servers": {
    "requel": {
      "type": "sse",
      "url": "http://localhost:8080/api/mcp/sse",
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
tool picker. Two caveats with VS Code's SSE client: it tries Streamable HTTP first and falls back to
SSE (fine — Requel returns SSE), and some VS Code versions have had bugs sending custom `headers` on
the SSE `/message` leg (microsoft/vscode #246207, #264095). If your `Authorization` header isn't
being sent and you get 401s, use the Claude Code path (Option A) or the `mcp-remote` bridge
(Option C pattern) instead.

## Option C — Codex extension

The Codex IDE extension shares `~/.codex/config.toml` with the Codex CLI. Codex only speaks **stdio**
and **Streamable HTTP** — it does **not** implement the legacy SSE transport Requel serves — so a
direct `url = "…/api/mcp/sse"` entry will not work. Use the `mcp-remote` stdio bridge.

Codex is normally launched from the macOS GUI, so it won't inherit direnv's environment (or
necessarily node/npx and Homebrew on PATH). The robust pattern is a tiny gitignored wrapper script
that loads the toolchain and reads the PAT from `~/.config/requel-tokens/rreganjr` — no secret in
`config.toml`:

```sh
# /Users/rregan_platformq/gh-acc/rreganjr/Requel/scripts/requel-mcp.sh   (gitignored, chmod +x)
#!/bin/sh
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
export PATH="/opt/homebrew/bin:$HOME/.local/bin:$PATH"
REQUEL_TOKEN="$(tr -d '\r\n' < "$HOME/.config/requel-tokens/rreganjr")"; export REQUEL_TOKEN
exec npx -y mcp-remote http://localhost:8080/api/mcp/sse \
  --transport sse-only \
  --header "Authorization: Bearer $REQUEL_TOKEN" \
  --header "X-Requel-Client: codex"
```

```toml
# ~/.codex/config.toml   (or .codex/config.toml in a trusted project)
[mcp_servers.requel]
command = "/Users/rregan_platformq/gh-acc/rreganjr/Requel/scripts/requel-mcp.sh"
```

`--transport sse-only` is essential: without it `mcp-remote` probes Streamable HTTP first, fails, and
drops into an OAuth flow Requel doesn't expose. (Simpler alternative if you don't mind the token
living in `~/.codex/config.toml`: set `command = "npx"`, put the `mcp-remote …` flags in `args`, and
supply the token via a `[mcp_servers.requel.env]` `REQUEL_TOKEN` — `mcp-remote` expands `${REQUEL_TOKEN}`
in a `--header` value. The wrapper keeps the single source of truth in `~/.config` instead.)

When Requel's Streamable HTTP transport is eventually turned on (uncomment
`spring.ai.mcp.server.protocol=STREAMABLE` + the streamable endpoint), Codex could connect directly
with a `url = "…"` + `bearer_token_env_var` entry and the bridge would no longer be needed.

## Verify (any option)

With the server up and the token in `$REQUEL_TOKEN`, sanity-check the endpoint before wiring a
client:

```bash
# Unauthenticated -> 401 (MCP endpoints sit behind the JWT chain)
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/mcp/sse

# Authenticated SSE handshake -> an "event: endpoint" line with the session message URL
curl -N -H "Authorization: Bearer $REQUEL_TOKEN" http://localhost:8080/api/mcp/sse
```

In the client, list tools and call `listProjects` to confirm reads; if writes are enabled, try
`createGoal` against a project you have `Goal[Edit]` on.

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

- [`doc/mcp_remote_connection.md`](mcp_remote_connection.md) — Claude Desktop / Codex CLI / Cursor,
  PAT minting, the `mcp-remote` wrapper-script pattern.
- [`doc/oauth_mcp_plan.md`](oauth_mcp_plan.md) — planned OAuth 2.1 support that will let agent
  clients connect without a pre-shared token (issue #83).

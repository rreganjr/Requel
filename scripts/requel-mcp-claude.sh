#!/bin/sh
# Launches the Requel MCP stdio bridge for Claude Desktop.
#
# Claude Desktop's claude_desktop_config.json only spawns local stdio servers,
# so it can't point at Requel's remote Streamable HTTP endpoint directly. This
# wrapper runs the community `mcp-remote` proxy, which bridges stdio <-> the
# Streamable HTTP MCP endpoint Requel serves at POST /api/mcp (issue #98).
#
# Kept as a standalone script (not an inline command in the JSON) because
# GUI-spawned clients pre-expand ${VAR} and mangle quoting in the config args,
# which produced an empty "Authorization: Bearer " header. A real shell reads
# everything verbatim: it sources nvm (GUI shells lack node on PATH), loads the
# PAT from the out-of-repo token file, and execs the proxy.
#
# The credential never sits in the repo: it's a user-minted Personal Access
# Token (reqpat_..., issue #73) at ~/.config/requel-tokens/rreganjr, revocable
# from the Requel UI. The gateway resolves it to the owning user per request.

# Make node/npx available in a GUI-spawned shell (harmless if already on PATH).
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"

REQUEL_TOKEN_FILE="$HOME/.config/requel-tokens/rreganjr"
if [ ! -r "$REQUEL_TOKEN_FILE" ]; then
  echo "requel-mcp-claude: token file not found: $REQUEL_TOKEN_FILE" >&2
  exit 1
fi
REQUEL_PAT="$(tr -d '\r\n' < "$REQUEL_TOKEN_FILE")"

exec npx -y mcp-remote http://localhost:8080/api/mcp \
  --header "Authorization: Bearer $REQUEL_PAT" \
  --header "X-Requel-Client: claude-desktop"

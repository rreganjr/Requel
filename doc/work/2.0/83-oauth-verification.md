# Issue #83 — MCP OAuth 2.1 end-to-end verification runbook (Slice 5)

Manual verification for the embedded OAuth 2.1 authorization server (Slice 1), the `/api/mcp/**`
resource server (Slice 2), RFC 9728 metadata (Slice 3), and Dynamic Client Registration (Slice 4).
The unit/integration suites already pass; this runbook covers the parts that need a running server
and, for the final part, a real MCP client — which can't be automated in CI.

Run each part in order; record outcomes in the **Findings** table at the bottom. Part D is the
**DCR client-behavior gate** the plan calls for — its result decides whether an anonymous
registration shim is needed.

## 0. Start the server with the AS enabled

Local MySQL, dev profile (CORS for the SPA), AS signing key ephemeral (fine for verification),
dev client + DCR registrar seeded:

```bash
java -jar modules/requel-app/target/requel-app-2.0.0-dev.jar \
  --spring.profiles.active=dev --server.port=8080 \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --spring.datasource.username=root --spring.datasource.password=password \
  --requel.oauth.seed-dev-client=true \
  --requel.oauth.dcr.enabled=true \
  --requel.oauth.dcr.registrar-client-secret=registrar-secret
```

Expected in the log: `Seeded OAuth dev client 'requel-dev-client' …` and
`Seeded DCR registrar client 'requel-registrar' …`, plus the ephemeral-signing-key warning.

## A. Discovery metadata + 401 (deterministic; curl only)

```bash
# RFC 8414 — authorization server metadata
curl -s http://localhost:8080/.well-known/oauth-authorization-server | jq .
#   expect: issuer, authorization_endpoint (/oauth2/authorize), token_endpoint (/oauth2/token),
#           jwks_uri (/oauth2/jwks), code_challenge_methods_supported ["S256"]
#   note: registration_endpoint is NOT in this document — it appears in the OIDC discovery doc:
#     curl -s http://localhost:8080/.well-known/openid-configuration | jq .registration_endpoint
#     -> "http://localhost:8080/connect/register"

# RFC 9728 — protected resource metadata (both forms should return the same document)
curl -s http://localhost:8080/.well-known/oauth-protected-resource | jq .
curl -s http://localhost:8080/.well-known/oauth-protected-resource/api/mcp | jq .
#   expect: resource=…/api/mcp, authorization_servers=[issuer], scopes_supported=["mcp"],
#           bearer_methods_supported=["header"]

# MCP 401 must advertise the resource metadata (Slice 3) — show the status line AND the header
curl -s -i http://localhost:8080/api/mcp/sse | grep -iE '^HTTP/|www-authenticate'
#   expect: HTTP/1.1 401
#           WWW-Authenticate: Bearer resource_metadata="…/.well-known/oauth-protected-resource"
```

**Pass:** all three documents are correct and the 401 carries `resource_metadata`.

## B. Authorization code + PKCE with the seeded dev client (proves the AS issues a user token)

The seeded `requel-dev-client` is public (PKCE), scope `mcp`, consent required, with loopback
redirect URIs. Its redirect URI is fixed to `http://127.0.0.1:8080/login/oauth2/code/requel-dev-client`,
which has **no handler** in Requel (Requel is the auth server, not an OAuth client).

> **The 404 after login/consent is expected — it is the success case, not an error.** There is NO
> page that displays a token. The authorization `code` comes back in the browser **address bar** as a
> query parameter: `…/requel-dev-client?code=<CODE>&state=…`. Read the address bar, ignore the 404
> page body. Copy the `code=` value (up to `&state`). Real clients use their own loopback callback via
> DCR (Part C/D); this handler-less redirect is only for manual testing.
>
> **`$CV` and `$CC` must be from the same pair, in the same shell.** Generate CV/CC, build the
> authorize URL with **that** `$CC`, and exchange with **that** `$CV`. If you regenerate CV/CC, you
> must rebuild the authorize URL and log in again, or the exchange fails with `invalid_grant`. The
> code is also single-use and expires in ~1 minute — exchange it immediately.

```bash
# 1) PKCE pair
CV=$(openssl rand -base64 60 | tr -d '\n=+/' | cut -c1-64)
CC=$(printf '%s' "$CV" | openssl dgst -sha256 -binary | openssl base64 | tr '+/' '-_' | tr -d '=')

# 2) Open this in a browser (logged out), log in with a Requel user, approve consent:
echo "http://localhost:8080/oauth2/authorize?response_type=code&client_id=requel-dev-client\
&redirect_uri=http://127.0.0.1:8080/login/oauth2/code/requel-dev-client&scope=mcp\
&code_challenge=$CC&code_challenge_method=S256&state=verify123"
#   expect: redirect to /oauth2/login -> Requel login form -> consent page naming the client + 'mcp'
#   scope -> redirect to the (handler-less) redirect URI with ?code=…&state=verify123

# 3) Exchange the code (paste it) for tokens
CODE=<paste code from the redirect URL>
curl -s -X POST http://localhost:8080/oauth2/token \
  -d grant_type=authorization_code \
  -d client_id=requel-dev-client \
  -d "redirect_uri=http://127.0.0.1:8080/login/oauth2/code/requel-dev-client" \
  -d "code=$CODE" -d "code_verifier=$CV" | jq .
#   expect: access_token (JWT, alg RS256, sub=<your username>), refresh_token, expires_in≈3600

# 4) Call an MCP tool as that user (SSE handshake should succeed with the access token)
ACCESS=<paste access_token>
curl -N -H "Authorization: Bearer $ACCESS" http://localhost:8080/api/mcp/sse
#   expect: 'event: endpoint' with a session message URL (same as the PAT path)
```

**Pass:** token issued with `sub` = the Requel username; the access token authenticates against
`/api/mcp/sse`; consent screen appeared. Decode the JWT (`jwt.io` or `cut`+base64) and confirm
`sub` and `scope=mcp`.

## C. Dynamic Client Registration (gated; proves DCR + policy stamping)

```bash
# 1) Mint the single-use initial access token from the registrar (client.create ONLY)
INIT=$(curl -s -u requel-registrar:registrar-secret -X POST http://localhost:8080/oauth2/token \
  -d grant_type=client_credentials -d scope=client.create | jq -r .access_token)

# 2) Register a client with a loopback callback (this is what a real client does).
#    NOTE: do NOT send a "scope" field. Spring AS validates the *requested* registration scope
#    against the initial access token, which only carries client.create -> requesting "mcp" fails
#    with invalid_scope. Omit scope; our DcrRegisteredClientConverter forces scope=mcp on the
#    registered client regardless, so the response still comes back with "scope":"mcp".
curl -s -X POST http://localhost:8080/connect/register \
  -H "Authorization: Bearer $INIT" -H 'Content-Type: application/json' \
  -d '{"client_name":"my-agent","redirect_uris":["http://127.0.0.1:7777/callback"],
       "grant_types":["authorization_code","refresh_token"]}' | jq .
#   expect: client_id issued; response shows scope "mcp", token_endpoint_auth_method "none"
#           (public/PKCE). registration_access_token + registration_client_uri returned.

# 3) Non-loopback redirect must be rejected (mint a fresh INIT — the token is single-use)
INIT2=$(curl -s -u requel-registrar:registrar-secret -X POST http://localhost:8080/oauth2/token \
  -d grant_type=client_credentials -d scope=client.create | jq -r .access_token)
curl -s -X POST http://localhost:8080/connect/register \
  -H "Authorization: Bearer $INIT2" -H 'Content-Type: application/json' \
  -d '{"client_name":"bad","redirect_uris":["https://evil.example.com/cb"],
       "grant_types":["authorization_code"]}' | jq .
#   expect: error invalid_redirect_uri
```

**Pass:** loopback client registers and comes back forced to scope `mcp` + public/PKCE; a
non-loopback redirect is rejected; the initial token is single-use (a second `/connect/register`
with the same token fails).

## D. Real client behavior gate (decides the anonymous-DCR question)

Connect each client you care about and record **how it registers**. `mcp-remote` (used by most
desktop clients) supports OAuth discovery and, in recent versions, static client info via
`--static-oauth-client-info` — so a client that can't be handed an initial token can still use a
pre-registered `client_id`.

For each client, try in this order and note which works:

1. **OAuth, pre-registered client_id** — register a client via Part C using the client's own loopback
   callback URL as `redirect_uris`, then point the client at `http://localhost:8080/api/mcp/sse`
   with its OAuth client_id (e.g. `mcp-remote … --static-oauth-client-info '{"client_id":"…"}'`).
   Verify the browser login + consent, then tool calls run as the user.
2. **OAuth, anonymous DCR** — point the client at the endpoint with no client_id and let it
   self-register. This will **fail** against our gated `/connect/register` (needs an initial access
   token). If the client has no way to supply one, record that.
3. **Static bearer (PAT)** — fallback, still supported (see `mcp_remote_connection.md`).

### Claude Code (VS Code) — step by step

The VS Code extension shares the `claude mcp` config and the `/mcp` UI. Steps marked **(verify)** are
not confirmed in the Claude Code docs — establishing them is the point of Part D; record what
actually happens. Check `claude mcp add --help` for the exact flags on your version.

**D1 — Disconnect the existing PAT server.**
- List: `claude mcp list` (or run `/mcp` in Claude Code).
- Remove: `claude mcp remove requel` (use the name you configured).
- If it still shows in `/mcp` (a known quirk when a server exists at both user and project scope),
  delete the `requel` entry by hand from `~/.claude.json` (user) and/or the project `.mcp.json`, then
  restart Claude Code. Confirm `/mcp` no longer lists it.

**D2 — Add Requel as a remote OAuth server (no token).**
```
claude mcp add --transport sse requel http://localhost:8080/api/mcp/sse
```
SSE is the transport Requel serves today (`--transport http`/Streamable HTTP is not served yet). Do
NOT pass an `Authorization` header — the point is to let OAuth drive.

**D3 — Authenticate.** `claude mcp login requel` (Claude Code ≥ v2.1.186) or authenticate from the
`/mcp` UI. Claude Code hits `/api/mcp/sse`, gets the 401 + `resource_metadata`, discovers the AS, and
opens a browser; log in as your Requel user and approve consent. On success it returns to Claude
Code's loopback callback and `/mcp` shows the server connected.

**D4 — The DCR gate (the crux).** Before the browser step Claude Code needs an OAuth `client_id`, and
current Claude Code performs **anonymous** Dynamic Client Registration against `/connect/register`.
Requel's DCR is **gated** (needs an initial access token), so the anonymous attempt is expected to be
**rejected**. Record which of these holds:
- **Pre-registered client + fixed callback port (CONFIRMED available in Claude Code)** — `claude mcp
  add` supports `--callback-port` and `--client-id` (and `--client-secret`, which our public/PKCE
  clients don't need). Pick a port, register a client via Part C using that callback as loopback
  redirect URI(s) — register BOTH host forms, since Spring AS matches `redirect_uri` exactly:
  `redirect_uris:["http://127.0.0.1:8899/callback","http://localhost:8899/callback"]`. Then:
  `claude mcp add --transport sse requel http://localhost:8080/api/mcp/sse --callback-port 8899 --client-id <client_id> --scope user`
  and `claude mcp login requel`. If login fails with `invalid_redirect_uri`, Claude Code used a
  different callback path/host — read the exact `redirect_uri` from the browser URL and re-register
  the client with it (the converter accepts any loopback URI). This is the gated-DCR-compatible path,
  so no anonymous-registration support is required for Claude Code.
- **Anonymous-only, no static client option** (some other clients) — if a client can only self-register
  anonymously and cannot take a `client_id`, the OAuth connect fails at Requel's gated
  `/connect/register`. That would be the trigger for an anonymous-DCR shim follow-up; use the PAT path
  (D5) meanwhile. (Not the case for Claude Code, which accepts a pre-registered client_id.)

**D5 — PAT fallback (always works).** Re-add with a static header (see `mcp_remote_connection.md`):
`claude mcp add --transport sse requel http://localhost:8080/api/mcp/sse --header "Authorization: Bearer reqpat_…"`.

**Decision:** if every client of interest works via (1) or (3), gated DCR is sufficient — done.
If a required client only does (2) anonymous registration and can't supply an initial token or a
static client_id, open a follow-up to add a **loopback-restricted anonymous `/connect/register`
path** (permit unauthenticated POST for loopback redirect URIs only, same policy stamping). Do not
build it unless a client forces it.

## Findings

| Part | Client / check | Result | Notes |
|------|----------------|--------|-------|
| A | metadata + 401 | ✅ pass | RFC 8414 + 9728 docs correct; 401 carries resource_metadata |
| B | dev-client code+PKCE → SSE handshake | ✅ pass | access_token sub=admin, scope=mcp; /api/mcp/sse accepted it |
| C | DCR register + loopback reject | ✅ pass | register (no scope in request → forced mcp); non-loopback → invalid_redirect_uri |
| D | Claude Code (VS Code) | ✅ (1) pre-registered client | `claude mcp add --client-id --callback-port 8899`; `/mcp` authenticate → browser login+consent → connected |
| D | Codex CLI | ✅ (2) anonymous loopback DCR (#238) | `codex mcp login requel` with no PAT and no pre-registration → discovers `/connect/register` (now advertised in the RFC 8414 metadata) → anonymous register → browser login+consent → connected |
| D | Cursor | ☐ (1) / ☐ (2) / ☐ (3) | not yet tested |
| D | Claude Desktop / Cowork | ☐ (1) / ☐ (2) / ☐ (3) | not yet tested |

**Anonymous-DCR shim needed?** ✅ **yes — shipped in #238.** Codex CLI does anonymous DCR *only* and
cannot be handed a pre-registered client_id or an initial access token, so gated DCR was not sufficient
for it. #238 added a loopback-restricted anonymous `/connect/register` path (opt-in
`requel.oauth.dcr.allow-anonymous-loopback`, on in the dev profile) **and** advertises
`registration_endpoint` in the RFC 8414 `oauth-authorization-server` metadata (Spring AS listed it
only in the OIDC doc, which Codex does not read) — without that, Codex reported "Dynamic client
registration not supported" before ever POSTing. Verified: `codex mcp login requel` now connects with
no PAT and no pre-registration. Claude Code still works via its pre-registered client_id (gated path);
PAT remains the headless/CI option.

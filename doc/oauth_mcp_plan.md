# MCP OAuth 2.1 authorization — design plan

> Plan for spec-compliant authorization on the MCP endpoints so AI agent clients (Cursor, Claude
> Code, Claude Cowork/desktop) authenticate via the OAuth flow they expect, instead of a static
> Requel JWT. Surfaced while testing #69: those clients drive the MCP authorization spec's OAuth 2.1
> discovery flow on a 401 and can't use a pasted bearer token. Tracked as a v2.0 ticket.

## Goal

Make `/api/mcp/**` a spec-compliant MCP OAuth 2.1 protected resource so an agent client can connect
with no pre-shared token: it discovers the authorization server, runs authorization-code + PKCE
(optionally self-registering via dynamic client registration), gets an access token, and calls tools
that execute as the authenticated Requel user under the existing gateway authorization.

## Decision

**Embedded authorization server**: Requel runs its own OAuth 2.1 authorization server
(Spring Authorization Server) against its existing user store, and `/api/mcp/**` is an OAuth2
resource server validating tokens that AS issues. One deployable, no external IdP — fits the
self-hosted "run Requel locally + connect agents" model. (External-IdP and pluggable options were
considered and rejected for added ops/footprint.)

## What the MCP spec / RFCs require

- **RFC 9728 Protected Resource Metadata**: serve `/.well-known/oauth-protected-resource` for the MCP
  resource, listing the authorization server(s); emit `WWW-Authenticate: Bearer resource_metadata="…"`
  on 401 from the MCP endpoints.
- **OAuth 2.1 Authorization Server** with **RFC 8414** Authorization Server Metadata
  (`/.well-known/oauth-authorization-server`): authorization-code + PKCE, token endpoint, JWK set.
- **RFC 7591 Dynamic Client Registration** (optional but practically needed): agent clients
  self-register a client on first connect.

## Architecture

- **Authorization server** — `spring-boot-starter-oauth2-authorization-server` (Spring Authorization
  Server, compatible with Boot 3.5.x). Provides RFC 8414 metadata, authorization/token/JWK endpoints,
  and a login + consent flow. Back it with Requel's existing user store / `UserRepository` +
  authentication so users log in with their Requel credentials. Issues JWT access tokens with `sub`
  = the Requel username (or user id).
- **Resource server** — `spring-boot-starter-oauth2-resource-server` on a dedicated security filter
  chain for `/api/mcp/**`, validating the AS-issued JWTs via the AS's issuer/JWK set. Map the token
  subject to a Requel user and set the security context (reuse `CurrentUserResolver`), so
  `CommandGateway` / `CurrentUserCommandHandler` run per-stakeholder authorization as that user —
  exactly as the static-JWT path does today.
- **RFC 9728 Protected Resource Metadata** — a small endpoint serving
  `/.well-known/oauth-protected-resource` (Spring AS doesn't provide this; it's a resource concern),
  plus the `WWW-Authenticate` header on MCP 401s pointing at it.
- **Dynamic Client Registration** — enable Spring AS's client-registration endpoint so agent clients
  self-register; gate it (initial access token / allowed redirect patterns) to limit who can register.
  Support loopback (`http://127.0.0.1`/`localhost`) redirect URIs for desktop clients.
- **Coexistence with the existing JWT** — keep the current username/password → JWT chain for the SPA
  and scripts on `/api/**`; add OAuth only for `/api/mcp/**` via a higher-precedence filter chain.
  The AS's own endpoints get Spring AS's default security chain. Matcher ordering must be explicit
  (three chains: AS endpoints, `/api/mcp/**` resource server, existing `/api/**` JWT).

## Relationship to #73 (durable API tokens / PAT)

Complementary, not duplicate: **OAuth 2.1 = interactive agent clients** (they do the discovery +
code/PKCE dance); **PAT (#73) = scripts / CI / headless** (a long-lived token in a header). Both
resolve to a Requel user and run through the same gateway authorization. Decide during
implementation whether #73 folds in here or stays a separate non-interactive track; recommend
keeping both.

## Persistence / migration

Spring Authorization Server needs tables for registered clients, authorizations, and consents
(`JdbcRegisteredClientRepository` etc.). Add Flyway migrations for MySQL and mirror in the H2 test
config. Decide whether registered clients are seeded or created via DCR.

## Testing strategy

- Metadata: `/.well-known/oauth-authorization-server` (RFC 8414) and
  `/.well-known/oauth-protected-resource` (RFC 9728) return correct documents.
- Resource server: `/api/mcp/**` returns 401 + `WWW-Authenticate: …resource_metadata=…` without a
  token; accepts a valid AS-issued token and runs tools as the mapped user.
- Token → Requel user mapping drives gateway authorization (per-stakeholder permissions still
  enforced).
- An integration test exercising authorization-code + PKCE end to end against the embedded AS.
- DCR: a client can self-register (within the configured policy).

## Implementation slices

1. **Embedded AS**: add Spring Authorization Server, persistence + migrations, back it with the
   Requel user store; issue tokens; expose RFC 8414 metadata + login/consent.
2. **MCP resource server**: dedicated filter chain on `/api/mcp/**` validating AS tokens; map
   subject → Requel user into the security context; keep the SPA JWT chain intact.
3. **RFC 9728**: protected-resource metadata endpoint + `WWW-Authenticate` on MCP 401.
4. **Dynamic Client Registration**: enable + gate; loopback redirect URIs for desktop clients.
5. **End-to-end**: connect Cursor/Claude Code via OAuth (no static token) and verify discovery →
   code+PKCE → token → tool calls execute as the user; then drop the static-bearer recipe from
   `doc/mcp_remote_connection.md` (or mark it dev-only).

## Open questions

- DCR policy: open registration vs initial-access-token gating; which redirect URIs to allow.
- Consent screen: required for third-party agent clients, or auto-approve for first-party?
- Do we eventually unify SPA auth onto OAuth, or keep the username/password → JWT chain indefinitely?
- Token lifetimes + refresh-token policy for long-lived desktop client configs.
- Confirm Spring AS's DCR (OIDC client registration) satisfies what Cursor/Claude Code expect from
  RFC 7591.

## Risks

- Spring Authorization Server + the existing JWT chain interaction (filter-chain ordering, matchers).
- The MCP authorization spec is still evolving (RFC 9728 became mandatory in the 2025-06 revision);
  track spec changes.
- Complexity/footprint of standing up an AS in a single-maintainer app — mitigated by it being
  embedded and config-driven.

# MCP OAuth 2.1 Authorization 

# Design

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

# Implementation Plan

> Concrete, code-grounded plan for issue #83 (branch `83-oauth-2`). Maps the design slices above onto
> the current codebase, resolves the open questions with recommended decisions, and specifies the
> filter-chain ordering — the highest-risk part. Work lands on `83-oauth-2` per the standard
> branch/PR/verify workflow in `CLAUDE.md`.

## Current state (confirmed by reading the code)

- **One security chain today.** `ApiSecurityConfig` (`modules/service-impl/.../service/config/`)
  defines a single `SecurityFilterChain` at `@Order(1)` with `securityMatcher("/api/**")`,
  `STATELESS`, `anonymous` disabled, and `HttpStatusEntryPoint(401)`. It adds
  `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.
- **The MCP endpoints live under `/api/**` and inherit that chain today.**
  `McpJsonRpcController` is `@RequestMapping("/api/mcp")` (hand-rolled JSON-RPC POST, kept per #82),
  and `RequelMcpServerConfig` exposes the Spring AI transport at `/api/mcp/sse`. Neither has its own
  security config, so both currently require a Requel login JWT or PAT.
- **`JwtAuthenticationFilter` already branches on credential kind:** `ApiTokenService.isApiToken()`
  (prefix `reqpat_`) → PAT path (hash lookup, live authorities, `last_used_at`); otherwise
  `JwtService.parseToken()` → login-JWT path (HS256, authorities from `roles` claim). On any parse
  failure it clears the context so Spring returns 401.
- **User resolution is name-based.** `CurrentUserResolver.resolve()` reads the
  `SecurityContextHolder` `Authentication`, takes `auth.getName()`, and calls
  `userRepository.findUserByUsername(name)`. **This is the key integration seam:** any chain that
  sets an `Authentication` whose name is a valid Requel username makes the whole gateway/authorization
  stack work unchanged. The OAuth path only has to map the token `sub` → Requel username.
- **`McpClientContextFilter`** is scoped to `/api/mcp` via `shouldNotFilter` and is independent of
  which security chain runs — no change needed.
- **Flyway:** migrations in `modules/requel-app/src/main/resources/db/migration/`, latest is
  `V11__api_tokens.sql`; **next is `V12`**. H2 test schema mirrors MySQL.
- **Deps:** `service-impl/pom.xml` has `spring-boot-starter-security` + jjwt. OAuth starters are
  **not** yet present.

## Decisions on the open questions

| Question | Decision for this ticket |
|----------|--------------------------|
| DCR policy | **Enable RFC 7591 DCR, gated.** Allow **loopback** redirect URIs (`http://127.0.0.1[:*]`, `http://localhost[:*]`) for desktop clients out of the box. Registration is open by default for loopback-only clients (self-hosted, single-tenant), with an optional initial-access-token requirement toggled by `requel.oauth.dcr.require-initial-token` for exposed deployments. |
| Consent screen | **Require consent for all MCP clients, no auto-approve** (custom minimal consent page showing a readable client name + scope descriptions). Agent clients are third-party; consent is the human gate that pairs with open DCR and is the phishing backstop. Remembered per client+scope, so it's a one-time prompt per tool. |
| OAuth scopes | **Single coarse `mcp` scope** — "act as me through Requel's MCP tools." The real limits stay in the per-stakeholder gateway authorization; the token never grants more than the user already has. Finer `read`/`write` scopes deferred until a concrete need appears (additive, non-breaking later). |
| Unify SPA auth onto OAuth? | **No — out of scope.** Keep the username/password → JWT chain (`/api/auth/login` + `JwtAuthenticationFilter`) for the SPA and scripts indefinitely. |
| Token lifetimes / refresh | **Access token 1h; refresh token 30d, rotating**, reuse-detection on. Configurable via `requel.oauth.*`. Long-lived desktop configs rely on refresh, not long access tokens. |
| Does Spring AS DCR satisfy Cursor/Claude Code? | **Verify empirically in Slice 4/5.** Spring AS implements OIDC-flavored client registration; treat exact interop as a test gate, not an assumption. |
| PAT (#73) vs OAuth on MCP | **Keep both.** OAuth for interactive agent clients; PAT for headless/CI. The MCP chain accepts both (see ordering below). |

## Filter-chain ordering (the risky part)

Spring selects the **first** `SecurityFilterChain` whose `securityMatcher` matches, in `@Order`
sequence. `/api/mcp/**` is a subset of `/api/**`, so the MCP chain must come **before** the existing
API chain. Target: **four chains** (a dedicated interactive login/consent chain is required because
form login must serve `/login`, which is outside the AS endpoints matcher).

1. **`@Order(1)` — Authorization Server chain.** Built with
   `OAuth2AuthorizationServerConfigurer.authorizationServer()`, matched to its
   `getEndpointsMatcher()` (`/oauth2/**`, `/.well-known/oauth-authorization-server`,
   `/connect/register` for DCR), OIDC enabled, custom consent page at `/oauth2/consent`, and a
   `LoginUrlAuthenticationEntryPoint("/login")` for unauthenticated `text/html` requests.
2. **`@Order(2)` — interactive login/consent chain.** `securityMatcher("/login",
   "/oauth2/consent", "/logout")` with `formLogin` (Spring's generated login page) backed by
   `RequelUserAuthenticationProvider` (reuses `User.isPassword` + `UserDtoMapper.getRoleStrings`).
   Scoped so it never touches the SPA routes or `/api/**`. Sessions + CSRF are on (defaults) and
   shared with chain 1 via the session, so the consent form's CSRF token and the saved
   `/oauth2/authorize` request both carry across.
3. **`@Order(3)` — MCP resource-server chain (Slice 2).** `securityMatcher("/api/mcp/**")`,
   `STATELESS`, `oauth2ResourceServer(jwt(...))` validating **AS-issued** JWTs via the AS issuer/JWK
   set. Add the existing `JwtAuthenticationFilter` **before** the bearer filter so **PATs still
   work** on MCP: a `reqpat_` token is handled by the PAT path; a real AS JWT fails
   `JwtService.parseToken()` (HS256 vs the AS's RS256/JWK), leaves the context empty, and falls
   through to the resource-server bearer filter. Set a `BearerTokenAuthenticationEntryPoint` so 401s
   carry `WWW-Authenticate: Bearer`, then augment it with the `resource_metadata` param (Slice 3).
   Map `jwt.subject` → username so `CurrentUserResolver` resolves the Requel user; load authorities
   live from that user (reuse `UserDtoMapper.getRoleStrings`) rather than trusting token claims.
4. **`@Order(4)` — existing `/api/**` chain.** The current `ApiSecurityConfig` chain, renumbered
   from `@Order(1)` to `@Order(4)`. Unchanged otherwise; it no longer sees `/api/mcp/**` because
   chain 3 matches first.

> `@Order` values must be unique across chains. The AS chain is the highest precedence and is safe
> as long as its matcher is limited to AS endpoints and does not overlap `/api/**`. Add an
> integration test asserting a request to `/api/mcp` with no token hits chain 3 (401 +
> `WWW-Authenticate`), and `/api/projects` still hits chain 4.

## Slice-by-slice work

### Slice 1 — Embedded Authorization Server
- Add `spring-boot-starter-oauth2-authorization-server` to `service-impl/pom.xml`.
- New `AuthorizationServerConfig` (`service/config/`): `@Order(1)` chain (above),
  `RegisteredClientRepository`, `AuthorizationServerSettings` (issuer from `requel.oauth.issuer`),
  RSA `JWKSource` (generated at startup for dev; **document a keystore/persistent-key option** for
  prod so tokens survive restarts), `JwtDecoder` from the JWK source.
- Back interactive login with the **Requel user store**: expose a `UserDetailsService` (or an
  `AuthenticationProvider`) over `UserRepository` + the existing password hashing from
  `platform-identity`, so users log in with Requel credentials. Issue tokens with **`sub` = Requel
  username** to match `CurrentUserResolver`.
- Persistence: use `JdbcRegisteredClientRepository`, `JdbcOAuth2AuthorizationService`,
  `JdbcOAuth2AuthorizationConsentService`. Add `V12__oauth2_authorization_server.sql` from the
  official Spring AS schema (registered client / authorization / consent tables); verify H2
  compatibility in the test schema (the AS schema uses `blob`/`timestamp`; adjust types for H2 MySQL
  mode if needed).
- RFC 8414 metadata (`/.well-known/oauth-authorization-server`) is provided by the AS chain.

### Slice 2 — MCP resource server
- Add `spring-boot-starter-oauth2-resource-server`.
- New `McpResourceServerConfig` (`service/config/`): `@Order(2)` chain as specified above; renumber
  `ApiSecurityConfig` to `@Order(3)`.
- Subject→user mapping + live authorities via a small `JwtAuthenticationConverter`; confirm
  `CommandGateway`/`AuthorizingCommandHandler` per-stakeholder checks run as the mapped user
  (they already read `CurrentUserResolver`, so this should be transparent).
- CORS: extend the config so `/api/mcp/**` is covered (browser-based agent clients may preflight).

### Slice 3 — RFC 9728 Protected Resource Metadata
- New controller serving `/.well-known/oauth-protected-resource` (JSON: `resource`,
  `authorization_servers`, `bearer_methods_supported`, `scopes_supported`). Spring AS does not
  provide this — it's a resource-server concern.
- Emit `WWW-Authenticate: Bearer resource_metadata="<url>"` on MCP 401s via a custom
  `AuthenticationEntryPoint` on chain 2.

### Slice 4 — Dynamic Client Registration
- Enable Spring AS's client-registration endpoint (OIDC `/connect/register`), gated per the DCR
  decision (loopback redirect URIs allowed; optional initial-access-token).
- Configure default scopes for MCP clients and rotating refresh tokens.

### Slice 5 — End-to-end verification & docs
- Connect a real client (Cursor / Claude Code / Cowork) with **no** static token: discovery →
  authorization-code + PKCE → token → tool calls execute as the authenticated Requel user.
- Update `doc/mcp_remote_connection.md`: make OAuth the primary recipe; mark the static-bearer/PAT
  recipe dev-only or headless-only.

## Testing strategy (maps to `RELEASE_20_TEST_PLAN.md` conventions)
- **Metadata:** `/.well-known/oauth-authorization-server` (RFC 8414) and
  `/.well-known/oauth-protected-resource` (RFC 9728) return correct documents.
- **Chain routing:** unauthenticated `/api/mcp` → 401 + `WWW-Authenticate: Bearer …resource_metadata=…`;
  `/api/projects` still uses the login-JWT chain; a PAT still authenticates on `/api/mcp`.
- **Resource server:** a valid AS-issued token authenticates and runs a tool as the mapped user;
  per-stakeholder authorization is still enforced (reuse an `AuthorizationIT`-style scenario).
- **End-to-end:** authorization-code + PKCE against the embedded AS in an integration test.
- **DCR:** a loopback client self-registers within policy; a disallowed redirect URI is rejected.
- Backend gate: `mvn clean verify` green before commit. No Angular changes expected in this ticket.

## Out of scope / follow-ups
- #85 (`ManageApiTokens` per-user permission) and #75 (stakeholder permission coherence) are
  independent and not required here.
- Persistent signing-key management hardening beyond the documented keystore option.
- **SPA auth unification (future ticket).** Move the Angular app off the custom `/api/auth/login`
  → HS256 JWT path onto authorization-code + PKCE against the embedded AS, so there is one
  standards-based token system instead of two. Requel remains the identity source; users still log
  in with Requel credentials. This is a mostly-frontend effort (Angular auth surface: redirect/
  callback, token storage, silent refresh, logout, guards, interceptor), plus making the `/api/**`
  chain accept AS-issued tokens, reconciling the SSE JWT-expiry scheduling with OAuth refresh, and
  reworking the Playwright login/e2e flows. It also reintroduces a **first-party auto-approve**
  consent path (no consent screen for Requel's own SPA). Deferred from #83 to keep scope contained;
  #83's resource-server foundation is the stepping stone (applying the same resource-server pattern
  to `/api/**`).
- **External IdP support (future ticket).** Let a deploying org delegate authentication to their
  *existing* OAuth/OIDC provider (Okta, Keycloak, Auth0, corporate SSO) instead of Requel being the
  identity source — the "Requel plugs into your existing auth server" capability. Distinct from SPA
  unification (which stays on the embedded AS) and largely independent of it: adds an external issuer
  + login-federation / user-provisioning step. #83's standard `oauth2-resource-server` foundation
  makes this an extension rather than a rewrite.

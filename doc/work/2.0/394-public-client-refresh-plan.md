# #394: refresh tokens for public OAuth clients, plan

Issue: https://github.com/rreganjr/Requel/issues/394 · branch `394-public-client-refresh` · epic #375 · found by #378 on rc3.

## The problem

Every Requel OAuth client is public (PKCE, no secret): `requel-cli`, and the MCP clients that register themselves (Claude Code, Codex). `defaultTokenSettings()` asks for 1-hour access tokens and 30-day rotating refresh tokens, but Spring Authorization Server 1.5.7 gives public clients no refresh token:

- `OAuth2RefreshTokenGenerator` returns `null` for a public client on the authorization-code grant (`isPublicClientForAuthorizationCodeGrant`), so the login response has no `refresh_token`.
- `PublicClientAuthenticationConverter` only matches a PKCE code exchange (`matchesPkceTokenRequest`, which needs a `code_verifier`). A `refresh_token` request from a public client therefore authenticates no client and gets `invalid_client`.

So every OAuth session ended with its access token. On rc3, `requel-cli login --oauth` saved no `.refresh`, and Claude Code asked to sign in again a few hours after connecting. The existing tests mock the token endpoint, so none of them saw it.

## The fix

`PublicClientRefreshTokens` (service-impl, `config`), wired into the authorization-server chain in `AuthorizationServerConfig`:

- **`Generator`**: issues a refresh token whenever Spring asks for one (it asks only when the client has the `refresh_token` grant), public client or not. It is otherwise the same as Spring's generator: 96 random bytes, and the client's refresh TTL.
- **`tokenGenerator` bean**: Spring's default trio with that refresh generator: `JwtGenerator` over `jwkSource()`, `OAuth2AccessTokenGenerator`, `Generator`. It is set on the configurer explicitly.
- **`Converter` + `Provider`**: added to the token endpoint's client authentication. A `refresh_token` POST that names one `client_id` and presents no credentials (no Basic header, `client_secret` or `client_assertion`) becomes a `NONE`-method client authentication. The provider accepts it only if the client exists, is public and has the `refresh_token` grant; otherwise `invalid_client`. Every other client authentication is left to Spring.

Unchanged: rotation (`reuseRefreshTokens(false)`), so each refresh token works once; the 30-day TTL; and Spring's refresh provider, which still checks the refresh token was issued to the presenting client. OAuth 2.1 (section 4.3.1) allows refresh tokens for public clients when they rotate.

No client changes: `requel-cli` already sends `grant_type=refresh_token&client_id=requel-cli&refresh_token=…` and saves the rotated token (`CliTokenSource`, `OAuthClient.refresh`).

## Tests

`PublicClientRefreshTokenIT` (requel-app, H2 with `oauth2-schema-h2.sql`) runs the real flow through MockMvc. It authorizes as admin, approves consent, and exchanges the code with the RFC 7636 verifier. Then:

- the `requel-cli` client gets a refresh token and redeems it; the response carries a new one, the old one is then `invalid_grant`, and the new one works;
- the same for a client registered through loopback DCR;
- one client's refresh token presented by another client is `invalid_grant`;
- an unknown `client_id` is `invalid_client`;
- a refresh request with no `client_id` is 401.

Without the fix, login returns no `refresh_token` and every test fails.

# Issue #238 — Loopback-restricted anonymous DCR (implementation plan)

> Plan for #238 (branch `238-loopback-anon-dcr`, base `release/2.0`). Lets interactive agent clients
> (`codex mcp login requel`, `claude mcp login requel`) connect with no PAT and no hand-registered
> client, by allowing **anonymous** Dynamic Client Registration **only** for loopback clients.
> Background/design: `doc/oauth_mcp_plan.md` (Slice 4/5 DCR decision), `doc/83-oauth-verification.md`
> (Part D — the trigger), `doc/mcp_remote_connection.md`. Depends on #83 (embedded AS), #98
> (Streamable HTTP `/api/mcp`). This plan follows the CLAUDE.md workflow; code lands after sign-off.

## Current state (confirmed by reading the code)

- **The OIDC client-registration endpoint is always mounted.** `AuthorizationServerConfig` chain 1
  (`@Order(1)`, AS-endpoints matcher) configures `.oidc(o -> o.clientRegistrationEndpoint(...))`
  unconditionally and requires `.anyRequest().authenticated()`. So `/connect/register` is live even
  when `requel.oauth.dcr.enabled=false`; what `dcr.enabled` gates is only **seeding the registrar
  client** (`seedDcrRegistrarClient`). An anonymous POST therefore fails auth today — which is the
  `Dynamic client registration not supported` / rejected result codex hits.
- **Registration is gated inside Spring AS, not just at the filter.** Spring AS's
  `OidcClientRegistrationAuthenticationProvider` requires the caller's principal to carry an access
  token with scope `client.create` (minted from the `requel-registrar` client). Permitting the
  request at the security-filter layer alone is **not** sufficient — the provider still demands the
  scoped principal.
- **Policy stamping already exists and is loopback-aware.** `DcrRegisteredClientConverter.convert(...)`
  rejects non-loopback redirect URIs (`isLoopbackRedirectUri`: `127.0.0.1`, `[::1]`/`::1`,
  `localhost`), forces public/PKCE (`ClientAuthenticationMethod.NONE`, `requireProofKey(true)`),
  `requireAuthorizationConsent(true)`, scope `mcp`, and `defaultTokenSettings()` (1h access / 30d
  rotating refresh). This is the exact policy the anonymous path must apply — reuse it, do not
  re-implement.
- **A first-party loopback-client precedent exists.** `requel-cli` (`seed-cli-client`) registers a
  **port-less** `http://127.0.0.1/callback` and relies on Spring AS's RFC 8252 loopback **port
  relaxation** — but only the `127.0.0.1` IP literal is relaxed, **not** `localhost`. Relevant to
  redirect-URI matching (below).
- **Discovery.** `registration_endpoint` is advertised by Spring AS at `/connect/register` in the
  OIDC discovery doc (`/.well-known/openid-configuration`); clients POST there. RFC 9728
  protected-resource metadata + the `WWW-Authenticate` 401 challenge (Slice 3) are unchanged.
- **No schema change.** `registered_client` / authorization / consent tables exist since `V12`. No
  Flyway migration is needed.

## Locked decisions

1. **Approach: a scoped filter at `/connect/register` that fully handles the anonymous-loopback case
   and passes everything else through** to Spring AS's existing gated provider. Chosen over the two
   options in the issue because it (a) is auto-discovered — it lives at the same advertised URL, so
   codex/claude need no config; (b) does **not** couple to Spring AS's internal provider/principal
   shapes; (c) reuses the existing policy via a shared helper. The filter, only when the guard passes,
   parses the RFC 7591 body, builds the `RegisteredClient` via the shared policy helper, saves it via
   `RegisteredClientRepository`, and writes the RFC 7591 `201` response itself. On any non-qualifying
   request it does nothing and the normal gated flow runs. (Alternative considered — synthesize a
   `SCOPE_client.create` principal so Spring AS's own provider writes the response — rejected as
   primary for the internal-coupling risk; kept as a fallback if the hand-written response proves
   insufficient for a client. Decide finally after the Step 1 spike.)
2. **Guard = property AND request-peer loopback AND all redirect_uris loopback.** The anonymous path
   activates only when: `requel.oauth.dcr.allow-anonymous-loopback=true`; the HTTP request's remote
   address is loopback (`request.getRemoteAddr()`); and every `redirect_uri` in the body is loopback
   (reuse `isLoopbackRedirectUri`). Any miss → passthrough to the gated flow (which will 401 an
   anonymous caller, unchanged). The request-peer check is defense-in-depth for an AS bound to a
   non-loopback interface.
3. **New property is independent of `dcr.enabled`.** The anonymous path needs no registrar and no
   initial token, so `allow-anonymous-loopback` works without `dcr.enabled`. Default **false**
   (secure, #83-identical behavior when off). Both may be on together (gated for non-loopback +
   anonymous for loopback).
4. **Consent stays required.** The shared policy sets `requireAuthorizationConsent(true)` — the human
   phishing backstop, and per-stakeholder gateway authorization is unchanged (the client only ever
   acts as the user who logs in + consents).
5. **Redirect-URI matching:** register the **exact** loopback URIs the client sends (codex/claude use
   a fixed callback port), so matching is exact; document that `localhost` is not port-relaxed by
   Spring AS (only `127.0.0.1` is) in case a client varies its port.

## Contracts

- **Endpoint:** `POST /connect/register` (unchanged URL). Anonymous-loopback request body is standard
  RFC 7591: `{ "client_name"?, "redirect_uris":[...loopback...], "grant_types"?:["authorization_code",
  "refresh_token"] }`. `scope` is ignored/forced to `mcp` (as the gated path already does).
- **Response (201):** RFC 7591 JSON — at minimum `client_id`, `client_id_issued_at`, `redirect_uris`,
  `grant_types` (`authorization_code`,`refresh_token`), `token_endpoint_auth_method` (`none`), `scope`
  (`mcp`), `client_name`. Exact required fields for codex/claude confirmed in Step 1.
- **Rejections:** non-loopback redirect URI or non-loopback peer → the request does not qualify;
  Spring AS's gated flow returns its normal `401` (anonymous) or `invalid_redirect_uri` (with a
  token). Property off → identical to #83.
- **Unchanged:** gated/initial-token DCR, PATs (#73), SPA login JWT, `/api/mcp/**` resource server,
  discovery + RFC 9728 metadata.

## Step-by-step

1. **Spike — capture codex's and Claude Code's real registration exchange (do first).** Temporarily
   log the `POST /connect/register` request body (and desired response) by running each client against
   a dev server (a throwaway permit+log filter, or `requel.oauth.dcr.allow-anonymous-loopback` behind
   a verbose branch). Record: exact `redirect_uris` (host/port/path), `grant_types`,
   `token_endpoint_auth_method`, and which response fields each client requires to proceed to the
   authorize step. This confirms the response contract and the exact loopback URIs to register, and
   settles the Option-A-vs-hand-written-response question. Record findings in this doc + the #83
   runbook.
2. **Refactor the policy into a shared helper.** Extract the `RegisteredClient` build logic from
   `DcrRegisteredClientConverter.convert(...)` in `AuthorizationServerConfig` into a package-visible
   static helper (e.g. `buildLoopbackMcpClient(clientName, redirectUris)`) that both the existing
   converter and the new filter call, so policy (public/PKCE, consent, scope `mcp`, 1h/30d, loopback
   validation) is defined once. No behavior change to the gated path.
3. **Add the property accessor** `allowAnonymousLoopbackDcr()` (reads
   `requel.oauth.dcr.allow-anonymous-loopback`, default false) alongside the existing OAuth flag
   accessors, and add it to the startup `logResolvedOAuthConfig` line.
4. **Implement `AnonymousLoopbackDcrFilter`** (`service/auth/`): for `POST /connect/register` with no
   usable bearer, when the guard (decision 2) holds, read the body, call the shared helper, `save(...)`
   via `RegisteredClientRepository`, and write the RFC 7591 `201` JSON; otherwise `chain.doFilter(...)`
   (passthrough). Register it on chain 1 **before** Spring AS's registration endpoint filter (order
   within the AS chain). CSRF/stateless: treat as a bearer-style API POST (no CSRF token needed),
   matching how Spring AS treats `/connect/register`.
5. **Wire the filter into chain 1** in `AuthorizationServerConfig.authorizationServerSecurityFilterChain`
   (add via `http.addFilterBefore(...)`), guarded so it is inert unless the property is on.
6. **Docs:** make OAuth the primary codex recipe in `doc/mcp_remote_connection.md`; flip the Part D
   finding + "Anonymous-DCR shim needed?" decision in `doc/83-oauth-verification.md` to reflect codex
   (record ✅ over the no-token flow); document the new property.

## Test plan

- **Unit:** shared policy helper stamps public/PKCE + consent + scope `mcp` + 1h/30d and rejects
  non-loopback (moved/covered from the converter's current behavior); the gated converter still
  behaves identically after the refactor.
- **Integration (extend #83's DCR IT):**
  - property on + loopback peer + loopback redirect URIs → `201`, client persisted with the enforced
    policy; a subsequent authorization-code + PKCE against that client issues a user token.
  - property on + loopback redirect URIs but **non-loopback peer** → not handled anonymously (gated
    `401`).
  - property on + **non-loopback** redirect URI → rejected.
  - property **off** → anonymous `POST /connect/register` behaves exactly as #83 (gated `401`); the
    initial-token path still registers.
  - **authorize-hop redirect check:** after an anonymous loopback registration, an
    `/oauth2/authorize` request whose `redirect_uri` was **not** registered for that client is
    rejected — redirect URIs are validated at the authorize endpoint, not only at registration
    (raised in issue review; Spring AS does this by default, so this test guards the contract).
  - PAT and SPA login-JWT paths on `/api/mcp/**` and `/api/**` unaffected (spot check).
- **Manual (record in `doc/83-oauth-verification.md` Part D):** `codex mcp login requel` and
  `claude mcp login requel` both complete with no PAT and no pre-registration; consent screen shows;
  tool calls run as the user.
- **Gate:** `mvn clean verify` green before commit.

## AC mapping (issue #238)

| Acceptance criterion | Covered by |
|---|---|
| `codex mcp login requel` works, no PAT / no pre-registration | Steps 1,4,5 + manual Part D |
| `claude mcp login requel` works without `--client-id` | Steps 4,5 + manual Part D |
| Non-loopback redirect URIs still rejected | Decision 2 + IT (non-loopback reject) |
| Anonymous clients forced to scope `mcp`, public/PKCE, consent | Step 2 shared helper + unit/IT |
| Property off ⇒ #83 gated-only behavior (regression-safe) | Decision 3 + IT (property-off) |
| Gated DCR / PAT / SPA-JWT paths unaffected | Steps 2,5 + IT spot checks |

## Out of scope / follow-ups

- Anonymous registration for non-loopback / remote clients (stays gated).
- SPA auth unification onto the embedded AS; external-IdP support (both deferred in
  `doc/oauth_mcp_plan.md`).
- PAT (#73) — unchanged; remains the headless/CI path.
- Registration-management (RFC 7592 read/update/delete of a self-registered client) — not required by
  codex/claude; add only if the Step 1 spike shows a client needs it.

## Risks

- **Hand-written RFC 7591 response drift.** A client may need a field we omit. Mitigation: Step 1
  captures the exact required set before implementing; the synthetic-principal alternative (reuse
  Spring AS's own response writer) is the fallback.
- **Opening registration (even loopback) broadens local attack surface** (raised in issue review).
  Bounding the exposure, spelled out as the trust model in the docs:
  - **No shared client_id to co-opt.** Each anonymous registration mints a **fresh random**
    `client_id` (UUID), so there is no well-known client identity a second local process can
    impersonate; it would have to register its own client and drive its own consent.
  - **Any local port is acceptable by design.** RFC 8252 relaxes the **port** for `127.0.0.1`
    redirect URIs, so a loopback client may receive the code on any local port — inherent to
    native-app loopback OAuth. The **consent screen + Requel login are the backstop**: nothing
    acts as the user until they log in and approve, and the client is confined to scope `mcp`
    acting as that user.
  - **Redirect URIs are checked twice.** All must be loopback at registration (the guard), and
    Spring AS re-validates `redirect_uri` against the registered set at the authorize hop
    (covered by the authorize-hop IT above).
  - Off by default; the request-peer-loopback guard additionally blocks non-loopback callers.
- **Spring AS filter ordering.** The new filter must run before the registration endpoint filter and
  must passthrough cleanly for every non-qualifying request. Covered by the property-off + gated-path
  ITs.
- **`localhost` vs `127.0.0.1` port relaxation.** Only `127.0.0.1` is port-relaxed; if a client
  registers `localhost:PORT` and later varies the port, matching fails. Mitigation: register exact
  URIs (Step 1) and document the distinction.

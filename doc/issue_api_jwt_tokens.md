# User-mintable, revocable API tokens for remote tools

Part of the v2.0 MCP command-gateway series (ticket 5 of 5). Design doc:
`doc/local_mcp_bridge.md`. Related: series ticket 1 (gateway), ticket 2 (remote connector).

## Goal

Let a user create personal access tokens (PATs) to identify themselves to remote tools (the MCP
bridge, CLI, and remote connector), and revoke them ("delete to cancel authentication"). This
keeps the gateway's identity model — writes attributed to the real authenticated user — working
for unattended/remote clients without sharing a password.

## Background and current-code constraints

Today the API authenticates via JWTs minted at login. `JwtService` creates simple HS256 JWTs
with `sub`, `roles`, `permissions`, `iat`, `exp`; `JwtAuthenticationFilter` validates only the
signature/expiry before setting the security context. There is **no `jti`, token type, token
version, or repository lookup** in the filter, and no token-management API. `McpCallAudit`
records `triggeringUserId`, so PATs slot into the existing audit model once the filter can resolve
them.

Two consequences this ticket must design around:

- **Immediate revocation requires a per-request store check.** A self-contained signed JWT stays
  valid until expiry unless the filter looks it up. So PATs cannot be plain stateless JWTs.
- **Embedded role/permission claims go stale.** A long-lived token that carries `roles`/
  `permissions` would not reflect later role changes unless the filter reloads authorities from
  the user (or validates a user/security version) on each request.

## Decisions for this ticket

- **PATs are opaque tokens mapped server-side** (recommended over signed JWTs): the filter
  branches on credential type — short-lived login JWTs stay stateless; a PAT is hashed and looked
  up in the token store on **every** request, giving immediate revocation and a clean hashing
  story.
- **Authorities are loaded from the owning user at validation time**, not embedded in the token,
  so role/permission changes take effect immediately.
- **Token management is conventional REST** under `/api/auth/tokens` (create/list/revoke),
  enforcing an "own tokens only" boundary against `CurrentUserResolver` — not user administration.
- **Scoping is deferred.** v1 PATs carry the owner's full rights (same as the user). Read-only /
  write-set scoping is a later enhancement, so it is out of v1 acceptance criteria and tests.

## Scope

In scope:

- **Token issuance:** a user mints a named PAT bound to their identity, with optional expiry.
  Plaintext returned once; only a hash is stored.
- **Token store + migration:** persistent record (id, owner user, name, token hash, created,
  last-used, expiry, status) with a Flyway migration.
- **Revocation:** deleting/disabling a token invalidates it on the next request.
- **Validation path:** `JwtAuthenticationFilter` branches on credential type; for a PAT it hashes
  the presented token, looks it up, checks status/expiry, resolves the owning user via
  `CurrentUserResolver`, and loads current authorities from the user.
- **Management API + UI:** `/api/auth/tokens` REST endpoints and an Angular UI to create, list,
  and revoke tokens (showing the one-time plaintext at creation and last-used afterward).
- **Wire bridge/CLI** to accept a PAT as their bearer credential.

Out of scope:

- Token **scoping** (read-only vs write-set) — deferred.
- OAuth client management for the hosted remote connector (ticket 2).
- Org/tenant-level token administration.

## Design

- PAT format: opaque high-entropy string; store a hash (e.g. SHA-256) keyed for lookup. The
  filter distinguishes a PAT from a login JWT by prefix/format and routes accordingly.
- Authorization (`AuthorizingCommandHandler`) and audit (`McpCallAudit.triggeringUserId`) apply
  unchanged — a PAT only establishes the triggering user; authorities come from that user live.
- `last-used` update strategy is defined (recommended: throttled/async to avoid a write on every
  request).

## Security & Privacy

- Store only hashes; show plaintext once.
- Expiry + immediate (next-request) revocation; record last-used for auditing.
- Authorities resolved live from the user so revoked roles take effect immediately.
- Rate limits continue to apply per the per-client pseudo-user.

## Implementation Steps

1. Token entity + repository + Flyway migration (owner, name, hash, created, last-used, expiry,
   status).
2. `/api/auth/tokens` create/list/revoke endpoints enforcing "own tokens only".
3. Extend `JwtAuthenticationFilter` to branch on credential type, look up + validate PATs, resolve
   the owning user, and load authorities live; define the `last-used` update strategy.
4. Angular token-management UI (one-time plaintext on create; list with status/last-used; revoke).
5. Wire the stdio bridge / CLI to accept a PAT as their bearer credential.
6. Tests below.

## Testing Strategy

- A valid PAT resolves to the owning user; expired/revoked PATs are rejected on the next request.
- Revocation takes effect immediately (next request fails).
- A role change on the owning user is reflected on the next PAT request (no stale embedded
  authorities).
- Audit rows show the correct `triggeringUserId` for PAT-driven writes.
- "Own tokens only": a user cannot list/revoke another user's tokens.

## Acceptance Criteria

- A user can mint, list, and revoke their own PATs via `/api/auth/tokens` and the Angular UI;
  plaintext is shown once and only a hash is stored.
- A PAT authenticates remote tools as the owning user, with authorization and audit identical to
  login-based access, and authorities loaded live.
- Revocation and expiry are enforced on the next request.

## Dependencies

- Series ticket 1 (gateway identity model). Complements ticket 2 (remote connector).

## Open Questions

- Confirm opaque-token approach vs signed JWT with `jti` + per-request store check.
- Default expiry (and whether expiry is mandatory).
- Token-display model fields for the UI: id, name, created, lastUsed, expiresAt, status (+ scope
  once added).
- When scoping is added later, does it gate read-only vs the gateway write-set, and where is it
  enforced (filter vs gateway policy)?

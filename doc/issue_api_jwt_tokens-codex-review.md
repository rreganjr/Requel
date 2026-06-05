# Codex review: issue_api_jwt_tokens.md

Review target: `doc/issue_api_jwt_tokens.md`

## Findings

### 1. Current JWTs have no token id or server-side validation hook

The doc proposes revocable PATs by checking token id/version in the store. Current `JwtService`
creates simple HS256 JWTs with `sub`, `roles`, `permissions`, `iat`, and `exp`, and
`JwtAuthenticationFilter` validates only the signature/expiry before setting the security
context. There is no `jti`, token type, token version, or repository lookup in the filter.

The implementation steps should explicitly add a token identifier claim and a validation path
that checks persistent state on every PAT request.

### 2. "Immediate revocation" conflicts with pure self-contained JWTs unless every request checks storage

The design says PATs are JWTs and revocation is immediate. That is only true if the filter does
a store lookup on every request for PAT tokens. A signed JWT with no lookup remains valid until
expiry. The issue should make the JWT-vs-opaque choice concrete or require a token-type branch:

- login JWTs remain stateless and short-lived,
- PATs include `jti` and always hit the token store,
- or PATs are opaque and always mapped server-side.

### 3. Token hashing needs a lookup strategy

The doc says plaintext is shown once and only hashes are stored. If PATs are opaque, validation
can hash the presented token and look it up. If PATs are JWTs, the server must either store the
hash of the whole JWT and look it up after signature validation, or store a `jti` and separately
hash a secret component. The ticket currently does not specify enough for a safe implementation.

### 4. Management UI scope should include API endpoints/services, not only Angular

The scope includes an Angular UI but the implementation steps only mention issuance/revocation
endpoints/commands generally. The current API has `/api/auth/login`, `/api/auth/me`,
`/api/users/**`, and `/api/commands/**`; there is no token-management controller or command
surface. The ticket should define whether token management is conventional REST
(`/api/auth/tokens`) or CQRS commands, and how the "own tokens only" boundary is enforced.

### 5. Scoping is marked optional but tests and acceptance criteria depend on it

The scope says optional scoping restricts read-only or gateway writes. Testing strategy says
scoped tokens are denied out-of-scope operations. That creates ambiguity about whether scoping
is required for v1. Either make scoping required in acceptance criteria or remove it from the
required tests.

## Completeness gaps

- Define token display fields in the Angular model: id, name, created, lastUsed, expiresAt,
  status, scope, and one-time plaintext.
- Define whether PAT auth should refresh `last-used` synchronously, asynchronously, or with
  throttling to avoid a write on every request.
- Define migration/schema file expectations for the token table.
- Define how PATs interact with current role claims. If roles/permissions are embedded in a
  long-lived PAT, role changes will not take effect unless the filter reloads authorities or
  validates a user/security version.


# #448 — Sign-in attempt limits

https://github.com/rreganjr/Requel/issues/448

Branch `448-login-attempt-limits` from `release/2.1`, PR to `release/2.1`. Size 3.

## Summary

Nothing limits how often someone can guess a password: not the API login, not the OAuth sign-in
form, not the current-password check on the password-change route. A wrong username also fails
faster than a wrong password, because only a known user's password is hashed, so the timing shows
which usernames exist. This adds a shared limit on failed attempts to all three, makes every
failure do the same hashing work, and removes a fourth password check (the `Login` command) that
has no use.

## Locked decisions (2026-11-01)

1. **Counts are kept in memory.** Requel runs as one server. A restart clears them. The limits
   are properties under `requel.login-throttle.*`.
2. **Three limits, each counted over a sliding 15-minute window:**

   | Key | Limit | Then |
   |---|---|---|
   | username + source address | 5 failures | wait doubles with each further failure (1 s, 2 s, 4 s ...), at most 15 min |
   | source address, any username | 50 failures | refused for 15 min |
   | username, any address | 50 failures | wait doubles, at most 15 min |

   - Nothing locks an account for good: the most an attacker can cost a real user is a
     15-minute wait.
   - A count clears once a window passes with no failures.
   - A successful sign-in clears the username + source count.
   - The username is the one supplied, whether or not the account exists, so a refusal says
     nothing about existence.
3. **A refused attempt is answered without checking the password**, so it costs almost no CPU.
   - `/api/auth/login` and `/api/auth/password`: 429 with `Retry-After` (seconds) and
     `{"error":"TOO_MANY_ATTEMPTS","message":"Too many sign-in attempts. Try again in N
     minutes."}`.
   - The OAuth sign-in form shows the same sentence.
4. **Every failure does the same work.** An unknown username, or an assistant account (which
   never signs in, #260), checks the password against a fixed dummy PBKDF2 hash with the same
   algorithm and iterations as a real one.
5. **The source is the connection's address** (`getRemoteAddr()`). `X-Forwarded-For` is not
   trusted by default, because any client can set it.
   - `doc/guides/INSTALL.md` gets a "Behind a reverse proxy" note: set
     `server.forward-headers-strategy=native`, so Tomcat takes the client address from the
     proxy's header. Tomcat accepts that header only from private and loopback addresses by
     default.
   - `docker-compose.yml` carries the setting commented out, with that note. Turning it on there,
     with no proxy in the compose file, would let a client on the host fake its address (Docker
     connections from the host arrive from a private bridge address) and step around the
     per-address limit. Leaving it off changes nothing for someone running compose to try Requel.
6. **Monitoring.** Each refused attempt logs one WARN line (the username, the address and which
   limit, never the password) and increments the Micrometer counter `requel.login.throttled`,
   tagged `path` (`api`, `form`, `password-change`) and `limit`.
7. **Folded in: `Login` leaves `/api/commands`.** `POST /api/commands/Login` lets any signed-in
   user test another user's password, and its "wrong password" and "no such user" errors differ.
   The SPA doesn't use it and the gateway already refuses it. The registration is removed;
   `LoginCommand` itself stays, unused by the API.
8. **Plain wording** in code, tests, commit and PR: sign-in attempt limits.

## Changes

service-impl
- `LoginThrottle` (new, `service.auth`):
  - `check(username, source)` returns the seconds still to wait, or 0;
  - `recordFailure(username, source)` and `recordSuccess(username, source)`;
  - takes a `Clock` (tests use a fixed one). Entries live in `ConcurrentHashMap`s, swept on access
    and every few minutes so the maps stay bounded.
- `LoginThrottleProperties` (new): the counts, the window and the cap. The defaults are those in
  decision 2.
- `LoginThrottledException extends AuthenticationException` carries the seconds to wait.
- `PasswordCheck` (new, small): `matches(User user, String password)`, which runs the dummy-hash
  check when the user is null or an assistant. The three paths call it instead of
  `user.isPassword` directly.
- `AuthController.login`: `check`, refuse 429; otherwise look up the user, `PasswordCheck`, then
  record a failure or a success.
- `AuthController.changePassword` / `PasswordChangeService`: the same around the current-password
  check, keyed on the signed-in username and the source. A wrong current password counts as a
  failure; a refused request is 429.
- `RequelUserAuthenticationProvider`: the source comes from `WebAuthenticationDetails`. It
  throws `LoginThrottledException` when refused. `AuthorizationServerConfig`'s failure handler
  maps it to `?error=throttled`, and `OAuth2LoginPageController` shows the sentence.
- `UserCommandRegistrar`: drop the `Login` registration. `GatewayPolicyConfig` keeps its entry
  (harmless) or drops it; whichever keeps `CommandGatewayIT`'s catalog check honest.

user-jpa
- `UserImpl`: a static `dummyPasswordCheck(String password)`, which hashes with the preferred
  algorithm and iterations against a fixed salt, so `PasswordCheck` costs the same as a real
  check.

Config and docs
- `application.properties`: the `requel.login-throttle.*` defaults, commented.
- `docker-compose.yml`: the commented `SERVER_FORWARD_HEADERS_STRATEGY=native` line and its note.
- `doc/guides/INSTALL.md`: "Behind a reverse proxy" and the limits.

## Steps

1. Branch.
2. `LoginThrottle`, its properties and its unit test.
3. `PasswordCheck` and the dummy hash.
4. Wire the three paths. Map the form error.
5. Remove the `Login` registration.
6. Docs and config.
7. ITs.

## Test plan

`tmp/448-verify.sh`: `mvn clean verify -pl modules/coverage-report -am` and the coverage check.
No Angular change; the SPA already shows the server's message on a failed login (checked in
step 4; if it doesn't, the Angular change and its spec are added).

- `LoginThrottleTest` (fixed clock):
  - five failures for one username and source, then refused, with the wait doubling and capped at
    15 minutes;
  - recovery after the window;
  - a success clears the pair;
  - the per-source and per-username limits, from many usernames and from many sources;
  - one username's limit doesn't affect another, and one source's doesn't affect another.
- `LoginThrottleIT` (MockMvc, small limits set by `@TestPropertySource`):
  - `/api/auth/login`: failures, then 429 with `Retry-After`, and the right password is refused
    while throttled. After the window (a test clock bean moved forward), the right password signs
    in.
  - An unknown username is throttled the same way.
  - The OAuth form: failures, then the login page with `error=throttled`.
  - `/api/auth/password`: wrong current passwords, then 429.
  - The counter `requel.login.throttled` goes up.
- `PasswordCheckTest`: the unknown-user and assistant paths call the dummy hash, checked by a spy,
  not by timing.
- `POST /api/commands/Login` is refused as an unknown command type; `CommandGatewayIT` updated.

## Out of scope

- Password changes through `EditUser` and revoking tokens after a password change (#446).
- Client-secret guessing on `/oauth2/token` and API-token guessing (both are long random secrets).
- Limits shared between several Requel servers.
- A real MCP rate limiter (#69).

## Risks

- **A shared address.** Users behind one NAT share the per-address limit (50 failures in 15
  minutes). The username + address limit is what catches a single guesser. Raise
  `requel.login-throttle.per-source` if it bites.
- **Behind a proxy without the setting**, every sign-in shares the proxy's address. The guide
  note covers it.
- **Tests that sign in many times.** The test profile raises the limits so existing ITs and the
  Playwright run aren't throttled; `LoginThrottleIT` sets small ones.

## AC mapping

| AC | Covered by |
|---|---|
| The API login and the OAuth form limit attempts by account and by source, with backoff | Decision 2; `LoginThrottleTest`, `LoginThrottleIT` |
| Sign-in recovers after the window; no permanent lockout | Decision 2; the recovery tests |
| Responses stay generic; refused attempts are logged and counted, without credentials | Decisions 3 and 6; `LoginThrottleIT` |
| Tests cover repeated failures, several accounts and sources, recovery, both mechanisms | `LoginThrottleTest`, `LoginThrottleIT` |
| Unknown and known usernames take the same work | Decision 4; `PasswordCheckTest` |
| `/api/auth/password` is limited too | `LoginThrottleIT` |

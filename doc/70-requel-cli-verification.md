# Issue #70 (Phase A) — `requel-cli` end-to-end verification runbook

Manual verification for the CLI's parts that need a running server and, for the OAuth login, a real
browser — which can't be automated in CI. The unit suites already pass (gateway REST client, catalog,
PKCE, loopback callback, token source, login orchestration, seeded-client shape); this runbook covers
the live paths: interactive OAuth login, PAT login, command discovery, a real gateway write, token
precedence, and logout.

Run each part in order; record outcomes in the **Findings** table at the bottom.

## 0. Build the CLI and start the server

Build the fat jar (also gives the `requel` wrapper a jar to find):

```bash
mvn -pl modules/requel-cli -am package
alias requel='modules/requel-cli/src/main/scripts/requel'   # or: java -jar modules/requel-cli/target/requel-cli-2.0.0-dev.jar
requel --help    # lists run, commands, login, logout
```

Start the server with the CLI OAuth client seeded **and** gateway writes enabled (so `commands` and a
write are exercisable). Dev profile for the SPA CORS; AS signing key is ephemeral (fine here).

> **Rebuild `requel-app`, not just `requel-cli`.** The seeded `requel-cli` client lives in
> `service-impl`/`requel-app`, which is *not* a dependency of `requel-cli` — so
> `mvn -pl modules/requel-cli -am package` does **not** repackage the app jar. If the app jar predates
> the M5 server changes you'll get a 400 at `/oauth2/authorize` (unknown `client_id`). Rebuild it:
> `mvn -pl modules/requel-app -am package -DskipAngularBuild=true -DskipTests=true`. Confirm the
> startup log shows `seed-cli-client=true` and `Seeded OAuth client 'requel-cli' …`.

```bash
java -jar modules/requel-app/target/requel-app-2.0.0-dev.jar \
  --spring.profiles.active=dev --server.port=8080 \
  '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --spring.datasource.username=root --spring.datasource.password=password \
  --requel.oauth.seed-cli-client=true \
  --requel.gateway.write.enabled=true
```

Expected in the log: `Seeded OAuth client 'requel-cli' (loopback PKCE, scope=mcp, consent required) for
\`requel login\`.` and the OAuth config line showing `seed-cli-client=true`.

> Credentials are written to `~/.config/requel/credentials` (override with `REQUEL_CONFIG_DIR`). To
> start each part from a clean slate, `requel logout` or point `REQUEL_CONFIG_DIR` at a temp dir.

## A. Interactive OAuth login (the crux — proves the loopback + PKCE + seeded client)

```bash
requel --url http://localhost:8080 login --oauth
```

Expected: the CLI prints `Opening your browser to log in…` with an authorize URL and opens the browser.
Log in as a Requel user, approve the consent screen (naming "Requel CLI" and the `mcp` scope). The
browser lands on the CLI's loopback `/callback` showing "Login complete — you can close this tab", and
the terminal prints `Logged in. Tokens saved for http://localhost:8080 (…/credentials).`

**The assumption this confirms (verified working on Spring Authorization Server 1.x):** login
succeeds without an `invalid_redirect_uri` error — i.e. Spring AS matched the CLI's ephemeral
`http://127.0.0.1:<random>/callback` against the seeded port-less `http://127.0.0.1/callback` (RFC
8252 loopback port relaxation). If a future AS version ever fails here with `invalid_redirect_uri`,
the fallback is a fixed callback port and a redirect registered with that exact port.

Inspect the stored tokens (optional): the file has `oauth.http://localhost:8080.access`, `.refresh`,
`.expiresAt`, `.scope` keys.

**Pass:** browser login + consent complete; access + refresh tokens saved; no redirect-URI error.

## B. Use the OAuth session (proves the token authenticates the gateway as the user)

```bash
# Discover the exposed write commands (server has writes enabled, so this is non-empty)
requel --url http://localhost:8080 commands
#   expect: a table of write commands (EditGoal, EditStory, …). No --token needed — the stored OAuth
#   access token is used automatically.

requel --url http://localhost:8080 --output json commands | jq '.[0]'
#   expect: {commandType, inputType, title, write:true, …}

# Read subcommands over the QueryGateway (#100) — find a project to write to
requel --url http://localhost:8080 projects
#   expect: one line per project you can see, with counts. Use a name from here below.
requel --url http://localhost:8080 project "<name>" --tree     # content tree
requel --url http://localhost:8080 search "<name>" goal        # find entities by name

# Perform a real write as the logged-in user (use a project name from `requel projects`)
requel --url http://localhost:8080 run EditGoal --input '{"projectName":"<name>","name":"CLI smoke goal"}'
#   expect: OK EditGoal: … (id=…). The goal is created/edited as your user, through the gateway
#   (allow/deny policy + per-stakeholder authorization enforced server-side).
```

**Pass:** `commands` lists the catalog and `projects` lists your projects using the stored token; the
write succeeds and is attributed to the logged-in user.

## C. Writes-disabled and denylist behavior (optional, restart-gated)

Restart the server **without** `--requel.gateway.write.enabled=true`, then:

```bash
requel --url http://localhost:8080 commands
#   expect: "No commands exposed (server writes may be disabled: requel.gateway.write.enabled=false)."
requel --url http://localhost:8080 run EditUser --input '{}'
#   expect: Error [NOT_ALLOWED] … (a denied/unknown command → exit code 4), proving the denylist holds
#   regardless of the client.
echo $?   # 4 for NOT_ALLOWED, 5 for a request error
```

**Pass:** empty catalog when writes are off; a denied command returns a NOT_ALLOWED error + exit code.

## D. PAT login + token precedence (headless path still works)

```bash
# Mint a PAT in the Requel UI (or POST /api/auth/tokens), then:
requel --url http://localhost:8080 login --token reqpat_XXXXXXXX
#   expect: Saved credentials for http://localhost:8080 (…/credentials).
requel --url http://localhost:8080 commands   # authenticates with the PAT

# Precedence: an explicit --token / REQUEL_TOKEN overrides both stored OAuth and stored PAT
REQUEL_TOKEN=reqpat_OTHER requel --url http://localhost:8080 commands
```

**Pass:** PAT login stores and authenticates; an explicit flag/env token takes precedence.

## E. Logout (clears both OAuth and PAT for the URL)

`logout` clears only the *stored* credentials. A `--token` flag or `REQUEL_TOKEN`/`REQUEL_PAT` env var
still takes precedence (that's the designed order: flag/env > stored OAuth > stored PAT), so unset any
env token first or the request will still authenticate.

```bash
unset REQUEL_TOKEN REQUEL_PAT          # e.g. direnv may export these
requel --url http://localhost:8080 logout
#   expect: Cleared credentials for http://localhost:8080.
requel --url http://localhost:8080 commands
#   expect: with no stored credentials and no flag/env token, the server rejects the request
#   (401 → exit code 3 AUTH), confirming both the OAuth tokens and PAT were removed.
echo $?
```

**Pass:** logout removes stored credentials; a subsequent call is unauthenticated.

## Notes on auto-refresh (not directly scriptable)

Access tokens live 1h; `CliTokenSource` refreshes automatically via the 30-day rotating refresh token
when the access token is within 60s of expiry, persisting the rotation. Verifying this manually means
waiting out an access token (or temporarily shortening `ACCESS_TOKEN_TTL`); it is covered by
`CliTokenSourceTest` (refresh-on-expiry + rotation persistence + refresh-failure fallback), so it is
not part of the timed manual run.

## Findings

| Part | Check | Result | Notes |
|------|-------|--------|-------|
| A | `login --oauth` browser + consent, tokens saved | ☐ | confirm NO invalid_redirect_uri (loopback port relaxation) |
| B | `commands` + `run EditGoal` with stored OAuth token | ☐ | write attributed to the logged-in user |
| C | writes-off empty catalog; denied command → NOT_ALLOWED | ☐ | restart without write flag |
| D | PAT login + flag/env precedence | ☐ | |
| E | logout clears OAuth + PAT | ☐ | subsequent call → 401 / exit 3 |

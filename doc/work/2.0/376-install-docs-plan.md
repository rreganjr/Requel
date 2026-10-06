# #376 Install and run docs for 2.0 — implementation plan

Child of the Release 2.0.0 epic #375. Review: 2026-10-05 against `release/2.0` @ `ea686ab5`
(after #75).

## Summary

Rewrite the install and run docs for 2.0 and fix the small things in the tree that make them
wrong or unsafe:

- `doc/guides/DOCKER.md` becomes `doc/guides/INSTALL.md`: Compose, Docker, jar, upgrading, a
  configuration reference and a short "before you expose it" checklist.
- The README keeps a short quickstart, gets a current "What's new", and links to the guide.
- Startup refuses the built-in JWT secret unless the `dev` or `test` profile is active.
- The MCP server reports the real version; the AI provider profiles default to current models.
- The release steps bump the compose image tags along with the pom.

This is a code PR (one CI run), not a docs-only commit.

## Review against the tree

1. **Upgrade text is wrong.** 1.2.0 already ran Flyway (V1, V2), so a 1.2 database upgrades with
   no flags. Tested: a 1.2.0 database on MySQL 8.4 with the sample project imported migrated
   V3–V37 under 2.0; both logins and every entity carried over.
2. **1.0.x/1.1 databases are wiped** by 2.0's defaults (baseline 0, then V1's `DROP TABLE IF
   EXISTS`), and with `SPRING_FLYWAY_BASELINE_VERSION=1` they fail at V2 and V33. Fixing the wipe
   is #379; an in-place upgrade is backlog #380. For 2.0, a 1.0.x user exports each project to
   XML and imports it into a new 2.0 database (1.0.2 runs only on MySQL 5.x; 2.0 on 8.4).
3. **Unsafe defaults the docs don't mention:**
   - `REQUEL_JWT_SECRET` falls back to a public string (`application.properties`, and a second
     default in `JwtService`).
   - admin/admin, nothing forces a change.
   - The README's jar example and the AI guide's examples run with `--spring.profiles.active=dev`,
     which registers unauthenticated reset endpoints.
4. **AI guide contradicts itself** on Anthropic; `ai-anthropic` defaults to the retired
   `claude-3-5-sonnet-latest`. `gemini-2.0-flash` and `gpt-4o-mini` need checking.
5. **Version strings outside the pom:** both compose files pin `2.0.0-dev`;
   `spring.ai.mcp.server.version=2.0.0-dev`.
6. **README stale:** "What's new" stops at early 2.0; migrations table at V7 of 37; Spring Boot
   badge 3.3 (build is 3.5); "2.0 pending" badge; sample link to `v1.0.1-beta`.
7. **First start:** about 3.5 minutes of dictionary loading after "Started", during which logins
   fail. #379 makes the server open only when ready; the docs say how long first start takes.
8. **First source build** downloads the NLP data jar from GitHub Releases (`NLP_DATA.md`).

## Locked decisions (2026-10-05)

1. Fold the small non-markdown fixes into this ticket (one PR, one CI run).
2. JWT secret: **refuse to start** on the built-in secret unless `dev` or `test` is active.
3. `DOCKER.md` moves to `doc/guides/INSTALL.md` as the full guide; the README keeps a quickstart.
4. README refresh is in scope (user-guide links wait for #377).
5. Compose image tags stay `2.0.0-dev` on the branch; the release steps bump them.
6. 1.0.x upgrade path for 2.0 is XML export/import into a new database (in-place is #380).

## Contracts

### JWT secret guard (requel-app)

- A startup check (bean in requel-app, runs before the web server opens) reads
  `requel.jwt.secret`. If it equals either built-in default and neither `dev` nor `test` is an
  active profile, startup fails with:
  `REQUEL_JWT_SECRET is not set. Set it to a random value of at least 32 characters (for example
  openssl rand -base64 48). See doc/guides/INSTALL.md.`
- It also fails on a secret shorter than 32 bytes in any profile (HMAC needs it; today that is a
  cryptic `WeakKeyException`).
- The check lives in `JwtService` (service-impl): its Spring constructor takes the
  `Environment`; the two-argument constructor the unit tests use is unchanged. Its own `@Value`
  default is removed, so `application.properties` is the one default. A blank secret counts as
  unset. `InsecureJwtSecretFailureAnalyzer` (requel-app) reports it.
- Compose files pass `REQUEL_JWT_SECRET` through from the environment or `.env`
  (`REQUEL_JWT_SECRET=${REQUEL_JWT_SECRET:-}`). The e2e flows run with `dev` and are unaffected;
  ITs run with `test`.

### Version and model defaults

- `spring.ai.mcp.server.version=@project.version@` (Boot parent resource filtering).
- `application-ai-*.properties`: current default models, checked against each provider's model
  list when implemented. Docs show `REQUEL_AI_MODEL` as the override.

### Release steps (`doc/guides/RELEASE_PROCESS.md`)

`scripts/set-version.sh <version>` replaces the bare `mvn versions:set` in the rc, final,
hotfix and next-`-dev` steps. It also sets the compose files' image tags and
`requel-cli --version` (`RequelCli.java`), the two other places the version is written down.

### Image smoke test (`container-publish.yml`)

Found while implementing: the tag-time smoke test runs the compose stack with no profile, so
it needs a throwaway `REQUEL_JWT_SECRET`; and since #379 the port opens only after the
first-start dictionary load, so the health probe waits up to 10 minutes instead of 3.

## Docs

### `doc/guides/INSTALL.md` (from `DOCKER.md`, `git mv`)

1. **Requirements:** Docker, or Java 17 and MySQL 8.4; memory (compose gives the JVM 2 GB).
2. **Docker Compose** (recommended): download `docker-compose.yml` (pinned tag), create `.env`
   with `REQUEL_JWT_SECRET`, `docker compose up -d`, first start takes a few minutes, log in,
   change the admin password.
3. **Docker without Compose:** network, MySQL 8.4, Requel container with env vars.
4. **Jar:** download from the GitHub Release, run with env vars or `--` properties, no `dev`
   profile; macOS/Linux/Windows.
5. **Upgrading:**
   - from 1.2: back up, start 2.0 on the same database, Flyway migrates;
   - from 1.0.x/1.1: keep the old database as a backup (2.0 refuses to start on it, #379);
     export each project to XML in 1.x, start 2.0 on a new, empty MySQL 8.4 database, import the
     files, recreate the users;
   - from 2.0.x: replace the image or jar.
6. **Configuration reference:** datasource, `REQUEL_JWT_SECRET`, admin, AI provider (link to
   `AI_ASSISTANT_SETUP.md`), MCP/OAuth (link to the MCP guides), port, memory.
7. **Before you expose it:** JWT secret, change admin/admin, no `dev` profile, HTTPS in front,
   back up MySQL.
8. **Troubleshooting:** first start, the JWT message, the pre-Flyway message, port in use.

### README

Short Compose quickstart (with `.env`), links to INSTALL.md and the AI guide, current "What's
new" (AI review and definitions, MCP and `requel-cli`, tags, reports, stakeholder permissions,
project delete, provenance), building from source (Java 17, Maven, Node 22, GitHub reachable for
NLP data), migrations as a pointer to `db/migration` instead of a table, badges and links fixed.

### `AI_ASSISTANT_SETUP.md`

Intro and §2b agree that OpenAI, Anthropic, Gemini, Ollama/OpenAI-compatible are supported;
examples drop the `dev` profile; default models match the profiles; the CLI provider stays
marked development-only.

## Test plan

- Unit: the JWT guard (default + no profile fails; default + `dev` or `test` passes; short
  secret fails; a 32+ byte secret passes).
- Existing ITs (`test` profile) and e2e (`dev`) unchanged and green.
- `mvn clean verify`; `./scripts/check-doc-links.sh`.
- Manual, recorded in the PR: each install path from INSTALL.md on a machine without a checkout
  (Compose with the published `2.0.0-rc1` once it exists; until then the jar and a locally built
  image), and the MCP server reporting the pom version.

## Build order

1. Code: JWT guard and test, `JwtService` default removed, compose passthrough, MCP version,
   model defaults.
2. `git mv doc/guides/DOCKER.md doc/guides/INSTALL.md` (developer), then write INSTALL.md.
3. README, AI guide, RELEASE_PROCESS.md.
4. Doc links, verify script.

#379 should merge first: INSTALL.md describes its refusal message and ready-on-open startup.

## Out of scope

- The user guide (#377).
- In-place upgrade from 1.0.x (#380).
- Forcing an admin password change on first login (docs only; a ticket if wanted).

## Risks

- **Compose users with no `.env`** get a refusing container. The quickstart and the error
  message both say what to do.
- **Existing 2.0-dev deployments without the variable** stop starting after this merges. Same
  message.
- **Model IDs age.** The defaults are only defaults; `REQUEL_AI_MODEL` overrides them.

## Acceptance mapping

| AC (#376) | Where |
|---|---|
| Each path reaches login, sample project opens | INSTALL.md §2–4, manual check in the PR |
| No 1.x versions left except upgrade/history | INSTALL.md, README |
| `check-doc-links.sh` passes | verify script |
| AI setup current | `AI_ASSISTANT_SETUP.md`, profile defaults |
| Compose files reference a pullable tag | release steps bump them (decision 5) |

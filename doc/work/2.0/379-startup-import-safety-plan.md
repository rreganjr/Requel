# #379 Startup and import safety — implementation plan

Child of the Release 2.0.0 epic #375; lands before #376, whose install guide describes it.
Review: 2026-10-05 against `release/2.0` @ `ea686ab5`.

## Summary

Three fixes found while testing upgrades:

1. A database that has tables but no Flyway history stops startup with a clear message instead
   of being wiped by V1.
2. Import takes the project name from the file's `<name>`; a rename still wins.
3. The database initializers (users, dictionary) finish before the web server accepts requests,
   so the first login after a fresh start works.

## Review against the tree

1. `application.properties` has `spring.flyway.baseline-on-migrate=true` and
   `baseline-version=0`. On a 1.0.2 database loaded into MySQL 8.4, 2.0 baselined at 0, ran V1
   (`DROP TABLE IF EXISTS` / `CREATE TABLE` for every table) and V2–V37, and started with no users
   or projects. Nothing in the log says data was dropped. Fresh databases are empty, so V1 runs
   without baselining; 1.2+ databases have history at V2+, so baselining never applies to them.
   Nothing needs `baseline-on-migrate` today.
2. `ImportProjectStreamingCommandImpl.readProjectMetadata` reads `<organization name>` and
   `<description>` at depth 1 of `<project>` but not `<name>`; `resolveProjectName()` falls back
   to "Imported Project". Both Angular callers (`project-list.ts`, `sidebar-nav.ts`) call
   `importProject(file)` with no name. The round-trip ITs always pass a name; the e2e round trip
   only checks that some name came back.
3. `DatabaseInitializationRunner` runs `DatabaseInitializer.initialize()` on
   `ApplicationReadyEvent`, after Tomcat is listening. A fresh MySQL start logged "Started" at
   14:42:53 and "Database initialization complete." at 14:46:29; logins returned 401 in between.
   The other startup hooks are the three OAuth `ApplicationRunner`s in
   `AuthorizationServerConfig`, which don't depend on the initializers.
4. ITs run on H2 with Flyway disabled (`application-test.properties`); the `*MySqlIT` tests drive
   Flyway directly against Testcontainers MySQL 8.4 (`disabledWithoutDocker`).

## Locked decisions (2026-10-05)

1. Refuse, don't adopt: a non-empty schema with no history is never baselined automatically.
   In-place upgrade from 1.0.x is backlog #380.
2. The rename (the command's `name`) wins over the file; the file wins over "Imported Project";
   a taken name gets " (n)" as today.
3. Initialize before the web server starts; no 503 "initializing" mode.

## Contracts

### Pre-Flyway schema check (requel-app)

- `spring.flyway.baseline-on-migrate=false`; `baseline-version` removed.
- A `FlywayMigrationStrategy` bean, `RequelFlywayMigrationStrategy`, runs before `migrate()`:
  if the target schema has any table and no `flyway_schema_history`, it throws
  `IllegalStateException` with:

  > Database `<schema>` has `<n>` tables but no Flyway history, so it was not created by Requel
  > 1.2 or later. Requel will not modify it. To move a Requel 1.0/1.1 database, export each
  > project to XML in the old version and import it into Requel 2.0 on a new, empty database.
  > See doc/guides/INSTALL.md, "Upgrading".

  Otherwise it calls `flyway.migrate()` unchanged.
- The check is a static method over a `DataSource` + schema name so the IT can call it without
  a Spring context.

### Import name (project-jpa)

- `ProjectMetadata` gains `name`, read from `<name>` at depth 1 of `<project>` (trimmed; blank
  counts as absent).
- `resolveProjectName()` base name: command `name`, else metadata name, else
  "Imported Project"; collision suffix unchanged.
- Importing into an existing project (`project != null`) still keeps that project's name.

### Initialization before the web server (requel-app)

- `DatabaseInitializationRunner` becomes a `SmartLifecycle` whose `start()` runs
  `databaseInitializer.initialize()`, with a phase below the web server's
  (`WebServerStartStopLifecycle`), so it finishes before the port opens and before
  `ApplicationReadyEvent`. `isAutoStartup()` true; `stop()` no-op.
- Logging unchanged ("Running database initializers..." / "Database initialization
  complete.").

## Test plan

- `RequelFlywayMigrationStrategyMySqlIT` (Testcontainers, MySQL 8.4):
  - empty schema: migrates to the latest version;
  - schema at V2 (migrate to "2"): migrates the rest;
  - a table and a row created by hand, no history: throws with the message; the table and row
    are still there and no `flyway_schema_history` was created.
- Import:
  - IT (`ProjectXmlStreamingRoundTripIT` or a new import IT): export a project, import with no
    name → the original name plus " (1)" (the original still exists); import a file whose
    project name isn't taken → that name; with a name → the name; a file with no `<name>` →
    "Imported Project".
  - Unit test for `readProjectMetadata` on the 1.0.2 sample (`<name>` after `<actors>`).
  - e2e `projects.e2e.ts` round trip: the imported name is `<source name> (1)`.
- Initialization: an IT asserting the initializers have run by the time the context is
  refreshed (admin exists before `ApplicationReadyEvent`); existing ITs and e2e unchanged.
- `mvn clean verify`, Angular unaffected (no client change), e2e in CI.
- Manual, recorded in the PR: fresh MySQL start, first HTTP response only after
  "Database initialization complete.", admin logs in first try; 1.0.2 copy refuses with the
  message and is untouched.

## Build order

1. Flyway property + strategy + MySQL IT.
2. Import name + tests + e2e assertion.
3. Initializer lifecycle + IT.
4. Verify script; manual checks against the cloud MySQL 8.4 copies of the 1.0.2 and 1.2 data.

## Out of scope

- Install docs (#376).
- In-place 1.0.x upgrade (#380).
- A name field in the Angular import dialog (the file's name is now used; rename in the editor).

## Risks

- **A dev database created before Flyway was enabled** would now refuse. None should exist after
  1.2; the message says what to do.
- **Startup ordering:** something that assumed the server was already up during initialization
  would break. Nothing found (only the OAuth runners, which run later either way).
- **Port opens later on first start** (minutes). Compose has no web healthcheck; CI e2e disables
  the dictionary SQL initializer, so it stays fast.

## Acceptance mapping

| AC (#379) | Where |
|---|---|
| Pre-Flyway schema fails with the message, data untouched; fresh and 1.2-style migrate | strategy + MySQL IT |
| `<name>` used; rename wins; " (1)" on collision; IT and e2e | importer + IT + e2e |
| First response after initialization; admin logs in first try | lifecycle + IT + manual |

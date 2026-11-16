# #380 — In-place upgrade from Requel 1.0.x and 1.1 databases

https://github.com/rreganjr/Requel/issues/380

Branch `380-upgrade-from-1x` from `release/2.1`, PR to `release/2.1`. Size 5.

## Summary

Requel 1.0.x and 1.1 databases have no Flyway history, so 2.x refuses to start on them (#379)
and the guide sends their users through project XML export/import. This adds an opt-in upgrade
that runs before Flyway: it recognizes a 1.0.1, 1.0.2 or 1.1.0 schema, rewrites it into exactly
V1's shape, repairs the data those versions left incomplete, baselines at 1, and lets Flyway
apply V2 onward.

## What the old schemas look like (measured 2026-10-30)

Each schema was made by starting the published image (`rreganjr/requel:1.0.1`, `:1.0.2`,
`:1.1.0`) on an empty MySQL 5.7 database, loaded into MySQL 8.4 with `mysqldump`, and compared
with V1 column by column, index by index and foreign key by foreign key. All four have the same
81 tables (V1 adds `flyway_schema_history`) and the same foreign keys.

| From | Differences from V1 |
|---|---|
| 1.0.2 | 9 unique keys named `UK_x` (V1: `UKx`); 13 `datetime` columns (V1: `datetime(6)`); 7 `varchar(255)` columns that V1 declares as enums; `arguments.support_level` `int` (V1: `tinyint`); `vnframedef.semantics`/`syntax` `longtext` (V1: `mediumtext`); 7 dictionary ids without `AUTO_INCREMENT`. |
| 1.0.1 | As 1.0.2, and `users` has no `password_salt`, `password_encrypting_algorithm_name`, `password_encrypting_iterations`. |
| 1.1.0 | Columns, types, indexes and keys match V1 except 32 ids without `AUTO_INCREMENT`, plus 26 Hibernate `*_seq` tables. (V2 already drops those tables and adds `AUTO_INCREMENT`, which is why V1 looks like 1.1.) |

A 1.0.x image runs only on MySQL 5.x; 1.1.0 runs on 8.x. 1.2.0 already has Flyway history and
needs nothing.

## Locked decisions (2026-10-30)

1. **Opt-in.** The upgrade runs only with `requel.upgrade.from-1x=true`
   (`REQUEL_UPGRADE_FROM_1X=true`). Without it, #379's refusal stands, and its message now names
   the property and the backup step.
2. **Exact shape or refuse.** Before changing anything, the schema must match one of the three
   recorded shapes exactly (tables, columns with type/nullability/extra, indexes, foreign keys).
   Anything else is refused with a message naming the first difference; the database is left
   alone. MySQL DDL isn't transactional, so this check, not a rollback, is what keeps a failed
   upgrade from leaving a half-converted schema.
3. **One ordered SQL script per shape**, run on one connection with `FOREIGN_KEY_CHECKS=0` and
   `sql_mode` including `NO_AUTO_VALUE_ON_ZERO` (so WordNet's `categorydef` id 0 keeps its id when
   `AUTO_INCREMENT` is added). After it runs, the schema must equal V1's recorded shape, or startup
   stops before Flyway. Then baseline at 1 and migrate. No applied migration changes.
4. **Data fix-ups** go in the same scripts, after the schema changes. Known: 1.0.x never wrote
   `pods_scenarios`. The rest are found by the parity test below; a gap that can't be repaired is
   listed in the guide instead of blocking the ticket.
5. **Fixtures come from the published images**, not anyone's real data: schema from a
   `--no-data` dump, plus the rows of `doc/samples/Requel.xml` imported through each old UI, with
   the dictionary tables left empty (a real install's dictionary rows are covered by the
   `categorydef` id 0 case, which the fixture keeps).
6. **MySQL 5.7 → 8.4** stays a dump/load step in the guide; the upgrade runs on 8.4.

## Changes

### Upgrade step (requel-app)

- `LegacySchemaUpgrader` (new): reads the schema's shape from `information_schema`, matches it to
  a `LegacyShape` (`V1_0_1`, `V1_0_2`, `V1_1_0`), runs that shape's script, checks the result
  against V1, then `Flyway.baseline()` at version 1.
- Recorded shapes in `src/main/resources/db/legacy/`: `requel-1.0.1.shape`, `requel-1.0.2.shape`,
  `requel-1.1.0.shape`, `v1.shape` (one line per column/index/key, sorted; plain text so a
  mismatch message can quote a line). Scripts: `upgrade-1.0.sql` (1.0.1 runs a short prelude adding
  the three `users` columns first) and `upgrade-1.1.sql`.
- `RequelFlywayMigrationStrategy`: on a database with tables and no history, call the upgrader
  when the property is on, otherwise refuse as now. `UnversionedDatabaseException` message updated.
- `LegacyShapeMismatchException` (new) + `RequelStartupFailureAnalyzer` entry, so a refused
  upgrade prints a readable failure like #379's.
- `application.properties`: `requel.upgrade.from-1x=false`, documented.

### Fixture tooling

- `scripts/legacy-fixtures.sh` (new, maintainer-only, needs Docker): starts MySQL 5.7 and the old
  image, imports the sample project through the old UI (Playwright), dumps schema + project rows,
  and writes `modules/requel-app/src/test/resources/upgrade/requel-<version>.sql`. Run once per
  version; the dumps are committed so CI never pulls the old images.
- A `README.md` beside the fixtures saying where they came from and how to regenerate them.

### Docs

- `doc/guides/INSTALL.md`, "Upgrading": the in-place path (back up, dump/load 5.7 → 8.4 for 1.0.x,
  start 2.x once with `REQUEL_UPGRADE_FROM_1X=true`), with the XML route kept as the fallback.
  `DOCKER_HUB.md` likewise. Troubleshooting gets the shape-mismatch message.

## Test plan

`tmp/380-verify.sh`: `mvn clean verify -pl modules/coverage-report -am` + the coverage-module
check (the MySQL ITs need Docker).

- `LegacySchemaUpgradeMySqlIT` (new, Testcontainers MySQL 8.4, no Spring context), for each of the
  three fixtures: load it, upgrade, migrate; the result's shape equals a database migrated from
  empty. Also: without the property it is refused and untouched; a fixture with one column changed
  is refused before any change; the `categorydef` id 0 row keeps id 0.
- `LegacyUpgradeAppMySqlIT` (new, abstract, Spring context on the upgraded fixture) with a subclass
  per version:
  - `admin` and the sample's users sign in with their old passwords (1.0.1's unsalted MD5
    included), and `admin`/`admin` lands on the #381 forced change;
  - parity: the upgraded project's content read (#274) equals the same `Requel.xml` imported into
    the same context, apart from ids and dates. This is the test that finds the remaining data gaps.
- `RequelFlywayMigrationStrategyMySqlIT` keeps passing (refusal message updated).

## Out of scope

- MySQL 5.7 itself (1.0.x users dump/load into 8.4 first).
- Schemas that drifted from the three published versions (refused; XML export/import remains).
- Reloading or refreshing a 1.x install's dictionary data beyond what 2.x already does at startup.

## Risks

- **Real databases that differ from a fresh one** (long-lived installs, hand edits) are refused by
  the exact-shape check. That is deliberate; the message says what differed.
- **Old UIs under Playwright** (Echo2): if driving the import is impractical, the fixture's project
  rows are written as SQL by hand from the sample instead, and the README says so.
- **CI time:** three Spring contexts on Testcontainers MySQL. The fixtures are small (no
  dictionary data), so each should start in well under a minute.
- **2.x and an existing 1.x dictionary:** check that 2.x's dictionary initializer leaves loaded
  1.x dictionary tables alone or refreshes them cleanly; fold any fix in here.

## AC mapping

| Issue item | Covered by |
|---|---|
| A 1.0.x database upgrades in place | Decisions 1–4; `LegacySchemaUpgradeMySqlIT`, `LegacyUpgradeAppMySqlIT` |
| MySQL IT from a 1.0.2 fixture | Decision 5; both ITs |
| Compare an upgraded project with the same project imported from XML | Parity test |
| Check a 1.1.0 database the same way | The 1.1.0 fixture and subclass |

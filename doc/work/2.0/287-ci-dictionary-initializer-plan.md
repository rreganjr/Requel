# #287 CI `Build & test` doubled — implementation plan

Regression introduced by #248 (`5f6ea124`, issue #247). The `Build & test` job went from
~8-11 min to ~19-23 min; total CI wall clock from ~17 min to ~30 min, against a
`timeout-minutes: 30` ceiling. Companion ticket #288 covers the underlying design flaw this
exposed; this ticket only stops the bleeding.

## Confirmed mechanism (release/2.0 @ `ba2d307f`)

`DictionarySQLInitializer` (`modules/dictionary-jpa`) is registered in **every** Spring context:

```java
@ConditionalOnProperty(name = "requel.dictionary.sql-initializer.enabled",
        havingValue = "true", matchIfMissing = true)
```

`DatabaseInitializationRunner` fires it on `ApplicationReadyEvent`, which `@SpringBootTest`
publishes, so all 21 Spring-context test classes reach it. `initialize()` is a no-op only when
`dictionaryRepository.findCategories()` is non-empty — never true for a fresh test database.

The 17 `nlp/dictionary/*.sql.gz` files (36 MB gzipped) are mysqldump output:

```sql
SET NAMES utf8;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
LOCK TABLES `categorydef` WRITE;
```

`loadSQLFile` filters `lock tables` and executes everything else. `@@FOREIGN_KEY_CHECKS` is not
an H2 system variable even under `MODE=MYSQL`, so on H2 the very first file throws, the
`catch (Exception)` in `initialize()` rolls back and logs `could not load dictionary via SQL`, and
the whole import costs milliseconds. **That accident is why the H2 ITs were ever fast.**

`DeleteProjectMySqlIT` (#247) is the first test context in the build to point at a real MySQL —
Testcontainers `mysql:8.4`, Flyway on, `ddl-auto=none`. There the `SET` statements succeed, so all
17 files import for real. Measured ~2-4 min locally; on a 2-core runner over container overlayfs it
accounts for the observed +11 min.

The `e2e` job already knew: `.github/workflows/ci.yml` passes
`--requel.dictionary.sql-initializer.enabled=false` to the app jar. Nothing carried that knowledge
into the Maven test phase, and `E2E + frontend coverage` correspondingly did *not* slow down
(~9 min before and after), which is the control that isolates the cause.

### Timing evidence (Actions API, `Build & test`, successful runs)

| run | date | branch | duration |
|---|---|---|---|
| 271-275 | Sep 3-4 | release/2.0 + PRs | 8.6 - 11.7 min |
| 281 / 282 | Sep 5 | 241-ui-delete-project / release/2.0 | 7.8 / 10.8 min |
| **283** | **Sep 10** | **247-delete-project-cascade-hardening** | **22.4 min** |
| 287-304 | Sep 10-11 | release/2.0 + PRs | 18.8 - 22.9 min |

The entire delta sits in the `Maven build, unit + integration tests, Angular bundle` step
(~7-9 min -> ~19-21 min). Checkout, JDK/Node setup, Vitest (~1.1 min) and the Codecov uploads are
unchanged. Runner variance is ±5 min on top of the new baseline (runs 298 vs 299, same branch:
15.5 vs 22.2 min), so timings below are compared within a single machine, not across CI runs.

## The change

One line in `modules/requel-app/src/test/resources/application-test.properties`:

```properties
requel.dictionary.sql-initializer.enabled=false
```

Why the test profile rather than the IT class:

- `DeleteProjectMySqlIT` inherits `@ActiveProfiles("test")` from `DeleteProjectIT`, so the MySQL
  context is covered by the same line.
- It also stops the 20 H2 contexts from throwing and logging an ERROR on every boot.
- Nothing in the test tree depends on this import path. Classes that need dictionary data call
  `AbstractIntegrationTestCase.ensureDictionaryLoaded()`, which reads `dictionary.xml.gz` directly
  and is unaffected.

Rejected alternative: `registry.add("requel.dictionary.sql-initializer.enabled", () -> "false")` in
`DeleteProjectMySqlIT`'s `@DynamicPropertySource`. Narrower, but leaves the silent-failure noise in
place and lets the next MySQL-backed IT re-acquire the 11 minutes. #288 exists to fix the design;
this ticket should not half-fix it in two places.

Out of scope (tracked in #288): making the import dialect-explicit, making a failed import visible,
and documenting which environments load the dictionary via SQL vs `dictionary.xml.gz`.

## Verification

`tmp/time-dict-initializer.sh` (local scratch, not committed) runs `DeleteProjectMySqlIT` twice on
the same machine, forcing the property on and then off through `SPRING_APPLICATION_JSON`.

The env-var route is deliberate: environment variables are inherited by the surefire/failsafe forked
JVM regardless of pom `argLine`/`systemPropertyVariables` config, `SPRING_APPLICATION_JSON` carries
the literal property name (so it does not depend on which relaxed-binding mapper resolves
`REQUEL_DICTIONARY_SQL_INITIALIZER_ENABLED` vs `REQUEL_DICTIONARY_SQLINITIALIZER_ENABLED`), and it
outranks `application-test.properties` — so both arms stay valid after this change is committed.

### Measured result (PQM-61, 2026-09-12)

| arm | `requel.dictionary.sql-initializer.enabled` | elapsed | dictionary files loaded |
|---|---|---|---|
| before | true | 268 s | 17 |
| after | false | 29 s | 0 |

239 s saved on one `DeleteProjectMySqlIT` invocation. The class's own failsafe time is 19.58 s in
the `after` arm, against ~4.5 min in the `before` arm — the rest of each arm is Maven startup and
the module's non-test lifecycle.

The local delta (~4 min) is smaller than the CI delta (~11 min), as expected: this machine has
NVMe and more cores than a 2-core `ubuntu-latest` runner writing through container overlayfs. The
direction and the mechanism are what this measurement establishes; the CI magnitude is established
by the job timings above and will be confirmed by this branch's own run.

Both arms exited 0, so the import is genuinely optional — no test depends on it.

### Gate

- [x] `before` arm loads 17 dictionary files; `after` arm loads 0.
- [x] `after` arm materially faster (268 s -> 29 s); delta recorded on the PR.
- [x] `DeleteProjectMySqlIT` (8), `DeleteProjectIT` (8) and `DeleteCascadeIT` (3) pass with
      unchanged test counts.
- [ ] Full `mvn clean verify` green.
- [ ] Post-merge: `Build & test` on release/2.0 back to ~10 min.

# #288 `DictionarySQLInitializer` fails silently on H2 — implementation plan

Companion to #287, which stopped the bleeding (CI) and deliberately left the design flaw here:
`application-test.properties:32` says so in as many words. This ticket makes the dictionary import
*declared* rather than dialect-accidental, and makes a real failure impossible to miss.

## Review against the tree (`release/2.0` @ `994dd7b8`)

**What #287 already did.** `requel.dictionary.sql-initializer.enabled=false` in
`modules/requel-app/src/test/resources/application-test.properties`. Every `@SpringBootTest` that
carries `@ActiveProfiles("test")` — 20 of 23 — no longer registers the bean, so those contexts are
already quiet, and `DeleteProjectMySqlIT` (which inherits the profile from `DeleteProjectIT`) no
longer imports 36 MB.

**What is still live — AC 3 is not met.** Three context classes never see that file, because they
configure the datasource with `@TestPropertySource(locations = "classpath:db.properties")` instead
of activating the `test` profile:

| class | line |
|---|---|
| `ProjectXmlRoundTripIT` | `@SpringBootTest(classes = Application.class)` + `db.properties` |
| `ProjectXmlStreamingRoundTripIT` | same |
| `ProjectUserCreationIT` | same, plus inline `properties = {...}` |

`db.properties` sets only `db.*` keys — nothing named `requel.dictionary.sql-initializer.enabled` —
so `matchIfMissing = true` registers the bean, the import runs against H2, and each of these three
still logs `could not load dictionary via SQL` on every boot. Grep confirms the flag appears in
exactly one test resource.

**Production is governed by `matchIfMissing = true` alone.** No `application*.properties` under
`modules/requel-app/src/main/resources` and no `docker-compose.yml` key sets the flag; only
`.github/workflows/ci.yml:243` passes it to the e2e app jar. So whatever this ticket does must not
silently stop the real MySQL deployment from loading the dictionary.

**Why the dumps cannot execute on H2.** `categorydef.sql.gz` opens with `SET NAMES utf8`,
`SET SQL_MODE=''`, `SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0` and
`LOCK TABLES ... WRITE`, and closes with `UNLOCK TABLES` and `SET FOREIGN_KEY_CHECKS=@OLD_...`.
`loadSQLFile` filters only statements starting `lock tables`; every other line above reaches H2 and
the first `SET @@...` throws. These are 17 files, 36 MB gzipped, `synset_definition_word.sql.gz`
alone 13 MB.

**The import list does not come from Spring, and is not the list the Java constant names.**
`initialize()` builds a `ResourceBundleHelper(DictionarySQLInitializer.class.getName())` and reads
`DictionarySQLFiles` from
`modules/dictionary-jpa/src/main/resources/com/rreganjr/nlp/dictionary/impl/repository/init/DictionarySQLInitializer.properties`.
That bundle is authoritative; `PROP_DICTIONARY_SQL_FILES_DEFAULT` is only the fallback if it is
missing, and the two disagree. The bundle omits `synset_definition_word.sql.gz` (13 MB) and the
three `semcor_*` dumps, and adds `vnroletype.sql`, `vnselres.sql`, `vnroleref.sql.gz` and
`custom_vn.sql`. All 17 bundle-listed files exist on disk; the real import is **~14.4 MB gzipped**,
not the 36 MB #287's plan quoted from the Java constant. That does not change #287's conclusion,
only the number. It does mean the file list cannot be overridden by a Spring property today.

**Extra defects found in the same method** (not in the issue text):

- `conn` from `jdbcTemplate.getDataSource().getConnection()` is never closed and `autoCommit` is
  never restored — on a pooled datasource the connection is leaked with `autoCommit=false`.
- `getResourceAsStream(path)` can return `null` (a renamed or absent dump); the next line
  constructs `new GZIPInputStream(null)` and NPEs, which the same `catch (Exception)` swallows into
  the same indistinguishable log line.

## Locked decisions

1. **Runtime dialect guard; the property stays as-is.** `matchIfMissing = true` is kept, so the
   MySQL deployment needs no config change. The initializer inspects the connection and skips when
   the datasource is not MySQL. Rejected: making the dumps dialect-neutral — that would make the H2
   contexts import 36 MB for real and hand back the CI regression #287 just removed; and flipping
   `matchIfMissing` to `false`, which would require adding the property to prod/docker config that
   does not currently carry it, with a silent empty dictionary as the failure mode if missed.
2. **A failure that was asked for aborts the context.** Skipping on H2 is `INFO`. Failing while
   enabled *and* on MySQL *and* with an empty dictionary is a defect, and throws.
3. **The connection leak is fixed in this ticket** — same method, a few lines.
4. **The MySQL path gets one real import, of one small dump.** A Testcontainers IT imports
   `categorydef.sql.gz` (45 rows, 4 KB) only, to prove the mysqldump preamble executes on a real
   MySQL and that the guard lets it through. The full corpus is never imported by a test. This
   needs the file list to be configurable — see step 3, which removes the `ResourceBundle` in
   favour of Spring properties.

## Behaviour contract

| datasource | property | dictionary rows | behaviour |
|---|---|---|---|
| any | `false` | — | bean not registered (unchanged) |
| any | unset / `true` | non-empty | no-op (existing `findCategories()` guard) |
| not MySQL | unset / `true` | empty | **skip**, one `INFO` naming `dictionary.xml.gz` as the path tests use |
| MySQL | unset / `true` | empty | import; on failure **throw** and fail startup |

## Steps

1. **`platform-core`** — add `FatalInitializationException extends RuntimeException`.
   `DatabaseInitializer.initialize()` keeps its per-initializer `catch (RuntimeException)` (one bad
   initializer must not abort the chain) but rethrows this type. That preserves the behaviour the
   comment there defends while giving an initializer a way to say "this one is fatal".
2. **`DictionarySQLInitializer.initialize()`** —
   - try-with-resources on the `Connection` and the `Statement`; restore `autoCommit` in a
     `finally` before release.
   - read `conn.getMetaData().getDatabaseProductName()`; when it is not MySQL, log
     `INFO  dictionary SQL import skipped: datasource is {} , not MySQL. The dumps are mysqldump
     output; tests that need dictionary data call ensureDictionaryLoaded() (dictionary.xml.gz).`
     and return.
   - on MySQL, keep the existing load, but replace the swallowing `catch (Exception)` with a
     rollback followed by `throw new FatalInitializationException(...)` whose message states that
     **the dictionary is empty**, names the file that failed, and names
     `requel.dictionary.sql-initializer.enabled=false` as the way to opt out.
3. **Remove `ResourceBundleHelper` from `DictionarySQLInitializer`.** Replace the bundle lookup
   with Spring configuration:

   ```java
   public static final String PROP_DICTIONARY_SQL_FILES_DEFAULT =
           "categorydef.sql.gz, word.sql.gz, morphdef.sql.gz, morphref.sql.gz, synset.sql.gz, "
           + "sense.sql.gz, synset_subsumer_counts.sql.gz, linkdef.sql.gz, lexlinkref.sql.gz, "
           + "semlinkref.sql.gz, vnclass.sql.gz, vnframedef.sql.gz, vnframeref.sql.gz, "
           + "vnroletype.sql, vnselres.sql, vnroleref.sql.gz, custom_vn.sql";

   @Value("${requel.dictionary.sql-files-directory:" + PROP_DICTIONARY_SQL_FILES_DIRECTORY_DEFAULT + "}")
   private String dictionaryDirPath;
   @Value("${requel.dictionary.sql-files:" + PROP_DICTIONARY_SQL_FILES_DEFAULT + "}")
   private String dictionaryFiles;
   ```

   and delete
   `modules/dictionary-jpa/src/main/resources/com/rreganjr/nlp/dictionary/impl/repository/init/DictionarySQLInitializer.properties`.

   **The default above is transcribed from the bundle, not from the existing constant.** The two
   disagree and the bundle is what production runs (see the review section); adopting the old
   constant would silently swap ~14.4 MB of VerbNet-inclusive dumps for ~36 MB of semcor-inclusive
   ones, with no error, because all twenty tables exist in `V1__init.sql`. Correcting the constant
   is the point: after this change the class states the list it actually imports, in one place.

   This replaces the fallback seam an earlier draft of this plan proposed — one source of truth
   rather than two — and it is what gives the IT in step 6 its override. Out of scope: the other
   ten classes that construct a `ResourceBundleHelper` for a bundle that does not exist on disk.
4. **`loadSQLFile`** — fail explicitly when `getResourceAsStream` returns `null`, so a missing dump
   is distinguishable from a SQL error.
5. **`doc/DICTIONARY_LOADING.md`** (new, AC 4) — which environments load via SQL (MySQL deployment,
   docker-compose) and which use `dictionary.xml.gz` (`AbstractIntegrationTestCase
   .ensureDictionaryLoaded()`), what the property does, and why the dumps are MySQL-only. Link it
   from `CLAUDE.md`'s Key Documentation list.

## Test plan

- **`DictionarySQLInitializerDialectTest`** (unit, `dictionary-jpa`) — mocked `DataSource` /
  `Connection` / `DatabaseMetaData`. Product name `H2` → no statement executed, no throw. Product
  name `MySQL` with a failing `Statement` → `FatalInitializationException`, and the message says
  the dictionary is empty. Covers the branch logic in milliseconds, with no database.
- **`DatabaseInitializerTest`** (unit, `platform-core`) — an initializer throwing a plain
  `RuntimeException` still lets the chain continue; one throwing `FatalInitializationException`
  propagates.
- **`DictionarySQLMySqlImportIT`** (`requel-app`, `@Testcontainers(disabledWithoutDocker = true)`) —
  the `DeleteProjectMySqlIT` container config (MySQL 8.4, Flyway on, `ddl-auto=none`;
  `categorydef` is created by `V1__init.sql`), plus `@DynamicPropertySource` setting
  `requel.dictionary.sql-initializer.enabled=true` and `requel.dictionary.sql-files=categorydef.sql.gz`
  (the property introduced in step 3).
  Asserts `dictionaryRepository.findCategories()` returns 45 rows — i.e. the `SET NAMES`,
  `SET @OLD_FOREIGN_KEY_CHECKS`, `LOCK TABLES` and `UNLOCK TABLES` statements all execute against a
  real MySQL and the guard does not skip. One 4 KB dump, seconds not minutes. Testcontainers is a
  test-scope dependency of `requel-app` only, which is why this IT lives there and the mocked test
  lives in `dictionary-jpa`.
- **The three profile-less ITs** — assert the ERROR is gone. A log capture around context startup
  is awkward for an already-refreshed context, so the cheap version is a build-level check:
  `mvn clean verify` output must contain no `could not load dictionary via SQL`. Recorded in
  `tmp/288-verify.sh` rather than as a JUnit assertion.
- **Gate** — `mvn clean verify` green; no frontend change, so no vitest/tsc/e2e run is required.

```bash
# tmp/288-verify.sh
set -e
mvn clean verify | tee tmp/288-verify.log
! grep -q "could not load dictionary via SQL" tmp/288-verify.log
grep -c "dictionary SQL import skipped" tmp/288-verify.log   # expect >= 3
```

## Out of scope

- Making the dumps dialect-neutral, or regenerating them (see locked decision 1).
- A full-corpus MySQL import test. #287's own timing script measured the full import at 268s; the
  single-dump IT above buys the dialect coverage for a fraction of it. The corpus as a whole stays
  covered only by the real deployment.
- The `unlock tables` filter gap — `loadSQLFile` filters `lock tables` but not `UNLOCK TABLES`.
  Only reachable on MySQL, where the statement is valid, so it is latent, not a defect. Noted here
  so the next reader does not re-derive it.
- Moving the three profile-less ITs onto `@ActiveProfiles("test")`. It would also fix AC 3, but it
  changes their datasource and property surface and risks the id-collision behaviour CLAUDE.md
  warns about. Worth its own ticket.
- #268 (legacy lexical assistant precision), which is what actually consumes the dictionary.

## Risks

- **Fail-the-context can stop a production boot.** Narrow by construction: only when the property
  is on, the datasource is MySQL, and the dictionary is empty — i.e. exactly the case where the
  operator asked for an import and did not get one. The message names the opt-out property.
- **Product-name matching.** H2 reports `H2`; MySQL Connector/J reports `MySQL`. MariaDB's driver
  also reports `MySQL`, which is the behaviour we want (the dumps are MariaDB-compatible). Match
  case-insensitively on `mysql` as a substring.
- **The import list becomes a config surface.** `DictionarySQLInitializer.properties` ships inside
  the jar, so no deployment overrides it on disk today; if one ever did, that override stops working
  and has to become `requel.dictionary.sql-files`. Noted in `doc/DICTIONARY_LOADING.md`.
- **The new IT is skipped without Docker.** `disabledWithoutDocker = true` matches
  `DeleteProjectMySqlIT`, so a machine without Docker silently loses this coverage. Acceptable —
  CI has Docker — but it means a green local build is not proof the MySQL branch works.
- **`ApplicationReadyEvent` timing.** The runner fires after refresh, so a throw surfaces out of
  `SpringApplication.run()` — it fails a `@SpringBootTest` context load and stops the boot of the
  jar, which is the intent. Worth confirming in the `DatabaseInitializerTest` run rather than
  assuming.

## AC mapping

| AC | Covered by |
|---|---|
| Import is explicit, not accidental — pick one | Step 2, dialect guard (locked decision 1) |
| A failed import is visible | Steps 1-2: `FatalInitializationException`, message says the dictionary is empty |
| No Spring test context logs `could not load dictionary via SQL` | Step 2 + `tmp/288-verify.sh`; fixes the three profile-less ITs |
| `doc/` notes which environments load via SQL vs `dictionary.xml.gz` | Step 4 |

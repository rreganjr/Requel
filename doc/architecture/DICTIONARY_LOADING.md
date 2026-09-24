# Dictionary loading — which environments load what, and how

Requel ships a WordNet/VerbNet dictionary used by the legacy NLP assistants (spell check, glossary
suggestions, lexical analysis). There are **two unrelated paths** that put data in the dictionary
tables, and confusing them is what produced issues #287 and #288.

## The two paths

| | SQL dumps | XML snapshot |
|---|---|---|
| Loader | `DictionarySQLInitializer` (`modules/dictionary-jpa`) | `AbstractIntegrationTestCase.ensureDictionaryLoaded()` |
| Data | `nlp/dictionary/*.sql.gz`, 17 files, ~14.4 MB gzipped | `nlp/dictionary/dictionary.xml.gz`, ~5.9 MB |
| Runs on | **MySQL only** | any dialect, H2 included |
| Triggered by | `DatabaseInitializationRunner` on `ApplicationReadyEvent` | an explicit call from a test |
| Used by | the real deployment, docker-compose | integration tests that need dictionary data |

They are not alternatives to one another at runtime — nothing in the test suite depends on the SQL
path, and the deployment does not use the XML path.

Both sets of files ship in the `requel-nlp-data` jar, not in the source tree; the `nlp/dictionary/`
paths above are classpath paths inside it. How that jar is built, fetched and changed is in
[NLP_DATA.md](../guides/NLP_DATA.md).

## Why the dumps are MySQL-only

`nlp/dictionary/*.sql.gz` are mysqldump output. Each file opens with

```sql
SET NAMES utf8;
SET SQL_MODE='';
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
LOCK TABLES `categorydef` WRITE;
```

and closes with `UNLOCK TABLES` and `SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS`.
`loadSQLFile` filters statements beginning `lock tables` and executes everything else, so
`@@FOREIGN_KEY_CHECKS` reaches H2 and throws — H2 has no such system variable, even under
`MODE=MYSQL`. Regenerating the dumps dialect-neutrally was considered and rejected in #288: it
would make every H2 test context import ~14.4 MB for real, which is the CI cost #287 removed.

`DictionarySQLInitializer` therefore checks `DatabaseMetaData.getDatabaseProductName()` and skips
anything that is not MySQL, logging one INFO line that says the dictionary will be empty. MariaDB
reports `MySQL` and is not skipped — the dumps load there.

### If the dumps are ever regenerated

`readSQL` splits on `;`, strips `/* block comments */`, and has **no handling for `--` line
comments**. A `--` line is accumulated into the statement that follows it, which defeats the
`startsWith("lock tables")` filter in `loadSQLFile` — the `LOCK TABLES` statement is then sent to
the database with the comment glued to its front. On MySQL that is harmless (both the comment and
the statement are valid), but it means the dumps must keep the block-comment style they have. Plain
`mysqldump` emits a `-- MySQL dump 10.13 ...` header by default; these files do not have one, and a
regenerated set should not either (`--skip-comments`). Found while writing the #288 test fixture.

A regenerated set is new data: publish it as a new `nlp-data-<N>` release (see NLP_DATA.md) rather
than committing the dumps.

## Configuration

| property | default | meaning |
|---|---|---|
| `requel.dictionary.sql-initializer.enabled` | `true` | register the initializer at all |
| `requel.dictionary.sql-files-directory` | `nlp/dictionary/` | classpath directory holding the dumps |
| `requel.dictionary.sql-files` | the 17-file list in `DictionarySQLInitializer` | dumps to load, in order |

Where they are set today:

- `modules/requel-app/src/test/resources/application-test.properties` — `enabled=false` (#287), so
  no `test`-profile context imports anything.
- `.github/workflows/ci.yml` — `--requel.dictionary.sql-initializer.enabled=false` for the e2e app
  jar.
- `DictionarySQLMySqlImportIT` — `enabled=true` plus a one-file `sql-files` override, the only test
  that imports through this path.
- Nowhere else. The deployment relies on the default, which is on.

These are read with `Environment.getProperty`, deliberately not through a `@Value` placeholder: in
this application a `@Value` placeholder resolves against `application*.properties` but not against
properties supplied by `@TestPropertySource` or `@DynamicPropertySource`, so a test cannot configure
one. That is #293; see #288's plan for the measurement.

Before #288 the directory and file list came from
`com/rreganjr/nlp/dictionary/impl/repository/init/DictionarySQLInitializer.properties` through
`ResourceBundleHelper`, and that bundle disagreed with the Java constant beside it — the bundle
(which won) loaded the VerbNet `vnroletype.sql`, `vnselres.sql`, `vnroleref.sql.gz` and
`custom_vn.sql`, while the constant named `synset_definition_word.sql.gz` and three `semcor_*`
dumps it never actually loaded. The bundle is gone; its list is now the constant's value and the
`@Value` default. If a deployment ever overrode that `.properties` on the classpath, it must now
set `requel.dictionary.sql-files` instead.

## Failure behaviour

- **Not MySQL** — skipped, one INFO line, dictionary stays empty, boot continues. Normal for every
  H2 test context.
- **MySQL, import fails** — rolled back and `FatalInitializationException` is thrown, which
  `DatabaseInitializer` rethrows rather than swallowing, so the context or the application fails to
  start. The import was asked for and did not happen; an empty dictionary behind a green build is
  what #288 existed to stop.
- **Dictionary already populated** — no-op, no connection taken.

## No test context loads the dictionary via SQL

Every `@SpringBootTest` class activates the `test` profile, so
`requel.dictionary.sql-initializer.enabled=false` applies and `@ConditionalOnProperty` never
registers `DictionarySQLInitializer`. Three of them —`ProjectXmlRoundTripIT`,
`ProjectXmlStreamingRoundTripIT` and `ProjectUserCreationIT` — configure the datasource with
`@TestPropertySource(locations = "classpath:db.properties")` and write the profile annotation fully
qualified on the following line, which makes them easy to mistake for profile-less contexts:

```java
@TestPropertySource(locations = "classpath:db.properties")
@org.springframework.test.context.ActiveProfiles("test")
```

They are not. A test that needs dictionary data calls
`AbstractIntegrationTestCase.ensureDictionaryLoaded()`; nothing in the suite goes through the SQL
path except `DictionarySQLMySqlImportIT`, which turns the property back on for its own context.

## Layers (#319)

The loading paths above fill the WordNet corpus. Spell checking reads it together with three other
word sources, and which of them a check sees depends on whether it runs inside a project.

| Checker | User dictionary (jazzy's single slot) | Dictionaries list |
|---|---|---|
| Installation (no project) | `InstallSpellDictionary` | the six jazzy classpath lists, `DatabaseSpellDictionary` (WordNet) |
| Project | `ProjectSpellDictionary` | the six jazzy classpath lists, `DatabaseSpellDictionary` (WordNet), `InstallSpellDictionary` |

- **jazzy lists** — static `.dic` files on the classpath. Not editable.
- **WordNet** (`word`) — the corpus the loaders above fill. Read-only at runtime:
  `DatabaseSpellDictionary.addWord` throws, so a coding mistake cannot write user words into it.
- **Installation words** (`install_dictionary_words`) — added by an administrator on the
  Installation Dictionary page (`AddInstallDictionaryWord` / `DeleteInstallDictionaryWord`,
  administrators only, denied on the gateway). Known in every project at once.
- **Project words** (`project_dictionary_words`, #313) — added on the project's Dictionary page
  (`AddProjectDictionaryWord` / `DeleteProjectDictionaryWord`, gated on `Project[Edit]`) or by
  resolving an "Add to Dictionary" issue (`Annotation[Edit]`). Known in that project only, and
  exported with it.

`DatabaseSpellDictionary` and `InstallSpellDictionary` are single shared instances that query per
lookup, so an administrator's add or remove applies everywhere with no cache eviction. Each project
checker is cached and evicted when that project's words change.

Before #319 a project checker held only the jazzy lists and the project's words (#313 had dropped
WordNet from it by replacing jazzy's user dictionary), so a word known only to WordNet was flagged
inside every project. And before #313, every "Add to Dictionary" wrote a sense-less row into
`word`. `V20__install_dictionary_words.sql` moved those rows — any `word` row that no `sense`,
`lexlinkref`, `morphref` or `synset_definition_word` row references — into
`install_dictionary_words`, and added an index on `word.phonetic_code`, which every lookup uses.

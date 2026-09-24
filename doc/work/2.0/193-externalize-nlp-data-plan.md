# #193 Externalize the nlp-jpa NLP data out of the jar — implementation plan

https://github.com/rreganjr/Requel/issues/193

## Summary

Prune the ~126 MB of NLP data that nothing reads, publish the ~92 MB that remains as a versioned
data jar attached to a GitHub Release (`nlp-data-1`), and have the Maven build fetch it
(sha256-pinned), install it into the local repository, and consume it as an ordinary `runtime`
dependency of `nlp-jpa`. Every loader keeps its `getClassLoader().getResourceAsStream("nlp/...")`
call unchanged, the Spring Boot fat jar still carries the data (nested as
`BOOT-INF/lib/requel-nlp-data-1.jar`), so `java -jar` and the Dockerfile do not change. `nlp-jpa`'s
own jar drops to classes only, and the `MAVEN_OPTS: -Xmx6g` workaround comes out of all three
workflows.

## Review against the tree (`release/2.0` @ `1f878f59`)

**Still needed, and worse than when filed.** #189 set `MAVEN_OPTS: -Xmx4g`; `ci.yml`,
`release.yml` and `container-publish.yml` now all carry `-Xmx6g`, each with the comment that it is
a stopgap for this issue. The three ACs stand as written. The issue had no milestone; it goes on
v2.0.

**Everything is loaded from the classpath.** Every reader resolves a relative `nlp/...` path
through `getClassLoader().getResourceAsStream` — the OpenNLP tokenizer, sentencizer, parser and
tagger in `nlp-jpa`; `DictionaryInitializer`, `DictionarySQLInitializer`,
`WordNetDefinitionWordsInitializer` and `JpaDictionaryRepository` (jazzy lists) in
`dictionary-jpa`; and `VerbNetImporterTests` in `requel-app`. So moving the files into another jar
on the same classpath, at the same paths, needs no Java change. That is also why the data lives in
`nlp-jpa` while half its readers are in `dictionary-jpa`: it only has to be on the application
classpath, not in the reader's module.

**The OpenNLP path is probably broken today, independently of this ticket.** The four parser
models, `EnglishTok.bin.gz` and `EnglishSD.bin.gz` are gzip files (magic `1f8b`) in the pre-1.5
format, while `OpenNLPTokenizer` and `OpenNLPTagger` call the opennlp-tools 1.8.2 constructors
(`new TokenizerModel(stream)`, `new POSModel(stream)`), which read the 1.5+ zip model package. That
matches the `@Disabled` reasons on `NLPConstituentParseTests` and `NLPTextTests`. So AC 3 is read as
**no regression**: whatever NLP works on `release/2.0` works the same after this change. The
runtime smoke records the baseline first. Replacing the models is `tmp/issue-opennlp-bump.md`, not
this ticket; the files stay in the data jar until then because code still names them.

**The repo already consumes NLP data this way.** `nlp-jpa` depends on
`edu.stanford.nlp:stanford-corenlp:4.5.6:models` — a separate data artifact that never trips the
heap because `nlp-jpa` never repackages it. `requel-nlp-data` follows the same pattern.

**Why a GitHub Release asset, not the alternatives.**

- *Git LFS* changes how git stores the files, not where the build puts them: they would still be
  under `src/main/resources`, still deflated by `maven-jar-plugin`, and the heap would be unchanged.
  It fails AC 1 and AC 2.
- *Download from upstream* is not available: the dictionary SQL dumps and `dictionary.xml.gz` are
  Requel-generated, and the OpenNLP `.bin.gz` models are the pre-1.5 format with no reliable
  upstream host. Self-hosting is the only option, which is this plan.
- *GitHub Packages (Maven)* would let the jar be a plain dependency with no glue, but that registry
  requires a token to *read*, even for a public repo, so every contributor and every fresh machine
  would need a `settings.xml` credential to build. A release asset downloads anonymously.

### What is unused (removed in this ticket, 404 tracked files, 126.3 MB)

| Path under `modules/nlp-jpa/src/main/resources/nlp/` | Size | Why it is unused |
|---|---|---|
| `semcor/` (353 files) | 33 MB | Raw SemCor corpus. Its only reader, `ImportSemcorCommand`, is unreachable: `DictionaryCommandFactoryImpl.newImportSemcorCommand()` throws `"ImportSemcorCommand is no longer supported."` |
| `opennlp-tools/namefind/` | 25 MB | No reference anywhere in `modules/**` |
| `opennlp-tools/coref/` | 0.7 MB | No reference |
| `opennlp-tools/EnglishChunk.bin.gz` | 2.5 MB | No reference; the parser loads `parser/chunk.bin.gz` |
| `opennlp-tools/parser/dict.bin.gz`, `parser/tagdict` | 0.9 MB | No reference; the parser and tagger name their four model files and `head_rules` explicitly |
| `dictionary/categorydef.sql`, `dictionary/synset_subsumer_counts.sql` | 6.1 MB | Uncompressed duplicates; the import list names the `.sql.gz` |
| `dictionary/synset_definition_word.sql.gz`, `dictionary/semcor_{file,sentence,sentence_word}.sql.gz` | 22.2 MB | Not in `DictionarySQLInitializer.PROP_DICTIONARY_SQL_FILES_DEFAULT`, the list production has always imported (#288). `synset_definition_word` rows come from the `*.xml.gz` tagged glosses via `WordNetDefinitionWordsInitializer` |
| `jwnl/file_properties.xml`, `jwnl/wn30/{data,index}.*`, `jwnl/wn30/*.exc` | 35.3 MB | The JWNL WordNet database. Nothing calls `JWNL.initialize` or names `file_properties.xml`, and its one reader, `WordNetSenseKeyInitializer`, has its `@Component` commented out. **This is the one judgment call in the list** — see Locked decision 3 |

**Kept (92.4 MB, 282 files):** `jwnl/wn30/*.xml.gz` (`WordNetDefinitionWordsInitializer`),
`opennlp-tools/EnglishSD.bin.gz`, `EnglishTok.bin.gz`, `parser/{build,check,chunk,tag}.bin.gz`,
`parser/head_rules`, the 17 dumps on the import list plus `dictionary.xml.gz`, all of `jazzy/`
(2.1 MB; the British variants are the documented alternatives for the configurable list) and all of
`verbnet-2.1/` (2.8 MB).

## Locked decisions

1. **Data jar nested in the fat jar.** The data is an ordinary Maven dependency; Spring Boot stores
   nested jars uncompressed, so repackaging copies it rather than recompressing it. `java -jar`,
   the Dockerfile and `docker-compose` are unchanged. Rejected: an external directory on
   `loader.path` — it changes the launch command everywhere and needs the data mounted into every
   test and container.
2. **Delivered as a GitHub Release asset** (see the review above), fetched by
   `download-maven-plugin` and installed into the local repository by
   `maven-install-plugin:install-file`.
3. **Unused files are removed in this ticket, including the JWNL WordNet database.** They stay
   recoverable from git history. The dormant code that would read them (`WordNetSenseKeyInitializer`,
   `ClassPathFileManagerImpl`, the `jwnl` dependency) is left alone — see Out of scope.
4. **Git history is not rewritten.** This ticket is about build and run size; the clone keeps its
   280 MB pack.
5. **Milestone v2.0.**
6. **The data is versioned by an integer, and an asset is never replaced.** A change to the data is
   `nlp-data-2`, a new sha256 and a version bump in the root pom. The sha256 pin makes a swapped
   asset fail the build instead of silently changing the data.

## Contracts

- **Release:** tag `nlp-data-<N>` on `rreganjr/Requel`, one asset `requel-nlp-data-<N>.jar`, marked
  not-latest so it never displaces the app release. The tag does not match the `v*` filter on
  `release.yml` or `container-publish.yml`, so publishing it triggers no workflow.
- **Jar layout:** `nlp/...` at the root, byte-for-byte the kept files at their current paths. No
  classes, no manifest entries beyond the `jar` tool's default.
- **Coordinates:** `com.rreganjr.requel:requel-nlp-data:<N>:jar`, installed locally only. Root pom
  properties `requel.nlp-data.version` and `requel.nlp-data.sha256`.

## Steps

`git`/`gh` steps are the developer's; everything else is Claude's.

1. **Branch** — `git switch -c 193-externalize-nlp-data release/2.0`.
2. **Milestone** —
   `gh issue edit 193 --repo rreganjr/Requel --milestone v2.0`.
3. **Prune** (developer; Claude cannot delete over the bridge):
   ```bash
   P=modules/nlp-jpa/src/main/resources/nlp
   git rm -r -q -- "$P/semcor" "$P/opennlp-tools/namefind" "$P/opennlp-tools/coref" \
     "$P/opennlp-tools/EnglishChunk.bin.gz" "$P/opennlp-tools/parser/dict.bin.gz" \
     "$P/opennlp-tools/parser/tagdict" "$P/dictionary/categorydef.sql" \
     "$P/dictionary/synset_subsumer_counts.sql" "$P/dictionary/synset_definition_word.sql.gz" \
     "$P/dictionary/semcor_*.sql.gz" "$P/jwnl/file_properties.xml" \
     "$P/jwnl/wn30/data.*" "$P/jwnl/wn30/index.*" "$P/jwnl/wn30/*.exc"
   ```
   Expect 404 files. `.DS_Store` files there are untracked and ignored; the build script skips them.
4. **`scripts/build-nlp-data.sh <source-dir> <N>`** (new) — zips `<source-dir>/nlp` into
   `tmp/requel-nlp-data-<N>.jar` with a manifest and directory entries, entries sorted and
   timestamped at a fixed epoch, `.gz` stored and the rest deflated, `.DS_Store` skipped; prints the
   file count and sha256. Uses `python3`'s `zipfile` rather than the JDK `jar` tool, so it runs
   where only a JRE is installed and repeated builds are byte-identical. Refuses to run if
   `<source-dir>/nlp` is missing or the output already exists.
5. **Build and publish `nlp-data-1`** (developer). It must exist before the PR's CI runs:
   ```bash
   bash scripts/build-nlp-data.sh modules/nlp-jpa/src/main/resources 1
   gh release create nlp-data-1 tmp/requel-nlp-data-1.jar --repo rreganjr/Requel \
     --target release/2.0 --latest=false --title "NLP data 1" --notes-file tmp/nlp-data-1-notes.md
   ```
   Claude writes `tmp/nlp-data-1-notes.md` (data only, not an application release; what it holds;
   link to this plan). The printed sha256 goes into step 7.
6. **Remove the remaining data from the module** (developer) —
   `git rm -r -q -- modules/nlp-jpa/src/main/resources/nlp`.
7. **Root `pom.xml`** — properties `requel.nlp-data.version` = `1` and `requel.nlp-data.sha256`;
   `<module>modules/nlp-data</module>` ahead of `dictionary-jpa`; `download-maven-plugin` pinned
   in `pluginManagement` (`io.github.download-maven-plugin:download-maven-plugin:2.1.0`; the 1.x
   line was `com.googlecode.maven-download-plugin`). `maven-install-plugin` is already managed by
   `spring-boot-starter-parent`.
8. **`modules/nlp-data/pom.xml`** (new, packaging `pom`) — two executions in `initialize`, declared
   in this order so they run in it:
   - `download-maven-plugin:wget` — url
     `https://github.com/rreganjr/Requel/releases/download/nlp-data-${requel.nlp-data.version}/requel-nlp-data-${requel.nlp-data.version}.jar`,
     `outputDirectory` `${project.build.directory}`, `sha256` `${requel.nlp-data.sha256}`. The
     plugin's own cache (`~/.m2/repository/.cache/download-maven-plugin`) means one download per
     machine.
   - `maven-install-plugin:install-file` — that file as
     `com.rreganjr.requel:requel-nlp-data:${requel.nlp-data.version}:jar`, `generatePom=true`.
9. **`modules/nlp-jpa/pom.xml`** — add `com.rreganjr.requel:nlp-data:${project.version}` with
   `<type>pom</type>` (orders the reactor, and pulls the module into `-pl ... -am`), and
   `com.rreganjr.requel:requel-nlp-data:${requel.nlp-data.version}` at `runtime` scope. Update the
   module `<description>`.
10. **Workflows** — delete the `MAVEN_OPTS: -Xmx6g` entry and its comment from `ci.yml`,
    `release.yml` and `container-publish.yml` (and the `env:` block where it is the only entry).
    `setup-java`'s `cache: maven` already covers `~/.m2/repository`, so CI caches both the
    download and the installed jar.
11. **Guard test `NlpDataResourcesTest`** (`requel-app`, plain JUnit, no Spring context) — see the
    test plan.
12. **Docs** —
    - `doc/guides/NLP_DATA.md` (new): what the data jar holds, where it comes from, how the build
      fetches it, how to change the data (download `nlp-data-<N>`, unzip, edit, run the script,
      publish `nlp-data-<N+1>`, bump both properties — never replace an asset), and the
      first-build/IDE note below. The pruned-file table from this plan, so the next reader knows
      what was dropped and where history keeps it.
    - `doc/architecture/DICTIONARY_LOADING.md`: one line saying the `nlp/dictionary/` dumps now ship
      in `requel-nlp-data`.
    - `CLAUDE.md`: `nlp-data` under the Features modules; `doc/guides/NLP_DATA.md` under Key
      Documentation; a line under Build Commands that the first build downloads the data jar.
13. **Verify script** `tmp/193-verify.sh` — below.

## Test plan

- **`NlpDataResourcesTest`** (new, `requel-app`). For every default resource path in the code —
  `OpenNLPTokenizer`, `Sentencizer`, the four `OpenNLPParser` paths, `OpenNLPTagger`,
  `DictionaryInitializer`, each file of `WordNetDefinitionWordsInitializer`'s default list, each of
  the 17 files in `DictionarySQLInitializer.PROP_DICTIONARY_SQL_FILES_DEFAULT`, each file of
  `JpaDictionaryRepository.PROP_ENGLISH_DICTIONARY_FILES_DEFAULT`, and the VerbNet schema — assert
  the classloader resolves it **and that the URL points into `requel-nlp-data-`**. Reading the
  constants (not string literals) means a pruning mistake or a later rename fails here, with the
  path in the message, rather than as a swallowed `log.error` at boot. This is the AC 3 guard.
- **Existing coverage that now runs through the data jar** — `AbstractIntegrationTestCase
  .ensureDictionaryLoaded()` (`dictionary.xml.gz`), `DictionarySQLMySqlImportIT`
  (`categorydef.sql.gz` on Testcontainers MySQL) and `VerbNetImporterTests`. The model tests in
  `NLPConstituentParseTests` and `NLPTextTests` are `@Disabled` ("models not available", "legacy
  Stanford parser models; currently broken"), and the `assistant-legacy-nlp` tests load no data,
  so model loading is covered only by the guard test (presence) and the runtime smoke below.
  Re-enabling those tests is not this ticket.
- **Low-heap package** — `MAVEN_OPTS=-Xmx1g mvn -pl modules/requel-app -am package -DskipTests
  -DskipAngularBuild=true` succeeds. The local stand-in for AC 2.
- **Jar contents** — `nlp-jpa-2.0.0-dev.jar` has zero `nlp/` entries;
  `requel-app-2.0.0-dev.jar` contains `BOOT-INF/lib/requel-nlp-data-1.jar`.
- **Runtime smoke (AC 3, manual, before and after)** — boot the jar with the dev profile against a
  scratch MySQL schema so the dictionary SQL import really runs: once built from `release/2.0`
  (baseline) and once from this branch, each against a freshly dropped `requel_193`
  (`DROP DATABASE IF EXISTS requel_193;`). Compare: the dictionary
  import completes, the set of NLP `ERROR`/`failed to` log lines is identical (the OpenNLP ones are
  expected in both), and the spelling assistant flags a misspelled word in a new goal:
  ```bash
  java -jar modules/requel-app/target/requel-app-2.0.0-dev.jar \
    --spring.profiles.active=dev --server.port=8080 \
    '--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/requel_193?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
    --spring.datasource.username=root --spring.datasource.password=password
  ```
- **Gate** — `mvn clean verify` green with `MAVEN_OPTS` unset. No `requel-angular/**` change, so no
  vitest/tsc run; no routing change, but CI's e2e runs regardless. **AC 2 is only proven by the
  PR's CI run** with the heap setting gone.

```bash
# tmp/193-verify.sh
set -euo pipefail
unset MAVEN_OPTS
MAVEN_OPTS=-Xmx1g mvn -q -pl modules/requel-app -am package -DskipTests -DskipAngularBuild=true
test "$(unzip -l modules/nlp-jpa/target/nlp-jpa-2.0.0-dev.jar | grep -c ' nlp/' || true)" = 0
unzip -l modules/requel-app/target/requel-app-2.0.0-dev.jar | grep -q 'BOOT-INF/lib/requel-nlp-data-1.jar'
unset MAVEN_OPTS
mvn clean verify | tee tmp/193-verify.log
```

## Out of scope

- Rewriting git history to shrink the clone (Locked decision 4).
- The dormant JWNL code: `WordNetSenseKeyInitializer` (commented-out `@Component`),
  `ClassPathFileManagerImpl` and the `jwnl` dependency from `lib/maven-repo`. They compile without
  the data and nothing runs them. Removing them touches `dictionary-jpa` code and the project-local
  repository, which is a different change from this one.
- Deploying `requel-nlp-data` to GitHub Packages alongside the app (see Risks).
- Moving the data dependency to `dictionary-jpa`, which reads half of it. Where it is declared does
  not change the application classpath.
- Upgrading the OpenNLP models (`tmp/issue-opennlp-bump.md`).

## Risks

- **`install-file` timing.** The data jar is installed during the reactor's `initialize` of
  `nlp-data` and resolved when `nlp-jpa` builds, which works because Maven resolves each module's
  dependencies lazily and the `pom`-type dependency orders `nlp-data` first (including under
  `-T`). Two consequences go in the guide: an IDE that resolves the whole project on import shows
  `requel-nlp-data` as missing until one command-line build has run, and `mvn -pl <module>` without
  `-am` on a machine that has never built needs one full build first.
- **First build needs github.com.** The same as needing Maven Central today; after that it is
  cached in `~/.m2`.
- **Deployed poms reference an artifact that is not deployed.** `RELEASE.md`'s GitHub Packages
  deploy publishes `nlp-jpa`'s pom with a dependency on `requel-nlp-data`, which exists only in
  local repositories. Nothing consumes the deployed modules as libraries today; if something ever
  does, add a `deploy-file` of the data jar to that step.
- **Over-pruning.** A path that is read but was judged unused. The guard test covers every default
  path; a deployment that overrides a path property to one of the removed files would break. No
  property file or compose file in the repo does.
- **A second heap consumer.** If CI still runs out of memory without `MAVEN_OPTS`, the cause is not
  `nlp-jpa`'s jar (for example, the Boot repackage of the CoreNLP models jar). That is found in this
  ticket's CI run and fixed here, not by restoring the 6g.
- **Redistribution.** The same files are already distributed in the repo and in every built jar;
  the release asset adds a location, not a change in what is shipped. `semcor/LICENSE` goes with
  the pruned SemCor corpus.

## AC mapping

| AC | Covered by |
|---|---|
| Bulk NLP data removed from the jar | Steps 3 and 6 remove it from `nlp-jpa`; steps 7–9 bring it back as `requel-nlp-data`; verify script asserts zero `nlp/` entries in `nlp-jpa`'s jar |
| CI builds without elevated heap | Step 10 removes `MAVEN_OPTS` from all three workflows; `-Xmx1g` local package; proven by the PR's CI run |
| NLP functionality still works | `NlpDataResourcesTest` (every default path resolves from the data jar), the existing dictionary/VerbNet/legacy-assistant tests, and the MySQL runtime smoke |

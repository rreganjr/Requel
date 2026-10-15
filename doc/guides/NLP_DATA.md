# NLP data

The legacy NLP pipeline and the dictionary read about 90 MB of data from the classpath under
`nlp/...`: OpenNLP models, WordNet tagged glosses, the dictionary SQL dumps, `dictionary.xml.gz`,
the jazzy spelling lists and VerbNet. None of it is in the source tree. It ships as one versioned
jar, `requel-nlp-data-<N>.jar`, attached to the `nlp-data-<N>` GitHub Release, and the build
fetches it. Issue [#193](https://github.com/rreganjr/Requel/issues/193) moved it out of
`nlp-jpa`'s resources, where packaging it ran CI out of heap.

## How the build gets it

| Piece | Where | What it does |
|---|---|---|
| `requel.nlp-data.version`, `requel.nlp-data.sha256` | root `pom.xml` properties | which release to fetch, and the checksum it must match |
| `modules/nlp-data` | `pom` packaging module | in `initialize`: `download-maven-plugin:wget` fetches the asset and verifies the sha256, then `maven-install-plugin:install-file` installs it as `com.rreganjr.requel:requel-nlp-data:<N>` |
| `modules/nlp-jpa/pom.xml` | two dependencies | `nlp-data` (`type=pom`) orders the fetch ahead of `nlp-jpa`; `requel-nlp-data` (`runtime`) puts the data on the classpath |
| `requel-app` fat jar | `BOOT-INF/lib/requel-nlp-data-<N>.jar` | Spring Boot nests it like any dependency, so `java -jar` and the Dockerfile need nothing extra |

The download is cached in `~/.m2/repository/.cache/download-maven-plugin`, so each machine fetches
it once. CI's `setup-java` `cache: maven` covers the same directory. The release is public; no
token is needed.

The readers are unchanged: each loads a relative path with `getClassLoader().getResourceAsStream`.
The data only has to be on the application classpath, which is why `dictionary-jpa`'s initializers
can read files that `nlp-jpa` brings in. `NlpDataResourcesTest` (in `requel-app`) resolves every
default path the code names and fails if any is missing or is not served from the data jar.

### First build and IDEs

- The first build on a machine needs to reach github.com, just as it needs Maven Central.
- `requel-nlp-data` exists only in your local repository, put there by the `nlp-data` module. An
  IDE that resolves the whole project on import shows it as missing until one command-line build
  has run, for example `mvn -pl modules/requel-app -am package -DskipTests -DskipAngularBuild=true`.
- For the same reason, `mvn -pl <module>` without `-am` on a machine that has never built needs one
  full build first.
- A checksum failure means the downloaded file is not the one the pom pins. Do not change the
  pinned sha256 to match it; find out why the asset changed.

## Changing the data

A published asset is never replaced. Any change, even one file, is a new version.

```bash
# 1. unpack the current version (N=1 here) and edit it
mkdir -p tmp/nlp-data-2-src
unzip -q ~/.m2/repository/com/rreganjr/requel/requel-nlp-data/1/requel-nlp-data-1.jar -d tmp/nlp-data-2-src
#    ... add, replace or remove files under tmp/nlp-data-2-src/nlp/ ...

# 2. build the new jar; prints the file count and sha256
bash scripts/build-nlp-data.sh tmp/nlp-data-2-src 2

# 3. publish it as its own release, never marked latest
gh release create nlp-data-2 tmp/requel-nlp-data-2.jar --repo rreganjr/Requel \
  --target release/2.0 --latest=false --title "NLP data 2" --notes-file tmp/nlp-data-2-notes.md
```

Then set `requel.nlp-data.version` to `2` and `requel.nlp-data.sha256` to the printed value in the
root `pom.xml`, in the same PR that needs the new data. If a reader's default path changed, update
`NlpDataResourcesTest` with it. The `nlp-data-*` tags do not match the `v*` filter on
`release.yml` or `container-publish.yml`, so publishing one runs no workflow.

`scripts/build-nlp-data.sh` sorts entries and gives every one a fixed timestamp, stores `.gz` files
and deflates the rest, and skips `.DS_Store`. It needs only `python3`. Two builds of the same input
on the same machine are byte-identical; a different zlib can deflate differently, which is why the
pom pins the sha256 of the published asset rather than of a rebuild.

## What `nlp-data-1` holds

282 files, 88.8 MB:

| Path | Read by |
|---|---|
| `nlp/opennlp-tools/EnglishTok.bin.gz`, `EnglishSD.bin.gz` | `OpenNLPTokenizer`, `Sentencizer` |
| `nlp/opennlp-tools/parser/{build,check,chunk,tag}.bin.gz`, `parser/head_rules` | `OpenNLPParser`, `OpenNLPTagger` |
| `nlp/jwnl/wn30/{adj,adv,noun,verb}.xml.gz` | `WordNetDefinitionWordsInitializer` |
| `nlp/dictionary/*.sql*` (the 17-file import list), `dictionary.xml.gz` | `DictionarySQLInitializer`, `DictionaryInitializer` — see [DICTIONARY_LOADING.md](../architecture/DICTIONARY_LOADING.md) |
| `nlp/jazzy/` | `JpaDictionaryRepository` (spelling lists; the British variants are the alternatives for `PROP_ENGLISH_DICTIONARY_FILES`) |
| `nlp/verbnet-2.1/` | `VerbNetImporter` |

The OpenNLP models are the pre-1.5 gzip format, while the code calls the opennlp-tools 1.8.2
zip-model constructors, so that path most likely does not load today; the tests that exercise it
(`NLPConstituentParseTests`, `NLPTextTests`) are `@Disabled`. Replacing the models is a data change
as above.

### Dropped when the data left the source tree

404 files, 126 MB, that nothing read. They are in git history before #193 if they are ever wanted.

| Path | Why it was unused |
|---|---|
| `nlp/semcor/` | Raw SemCor corpus; `newImportSemcorCommand()` throws "no longer supported" |
| `nlp/opennlp-tools/namefind/`, `coref/`, `EnglishChunk.bin.gz`, `parser/dict.bin.gz`, `parser/tagdict` | No reference in the code |
| `nlp/dictionary/categorydef.sql`, `synset_subsumer_counts.sql` | Uncompressed duplicates of the `.sql.gz` on the import list |
| `nlp/dictionary/synset_definition_word.sql.gz`, `semcor_{file,sentence,sentence_word}.sql.gz` | Never on the import list production runs (#288); `synset_definition_word` is built from the tagged glosses |
| `nlp/jwnl/file_properties.xml`, `nlp/jwnl/wn30/{data,index}.*`, `*.exc` | The JWNL WordNet database. Nothing initialized JWNL. `WordNetSenseKeyInitializer`, the one class that would have read `index.sense`, had its `@Component` commented out; it, `ClassPathFileManagerImpl` and the `jwnl` dependency were removed in #314 |

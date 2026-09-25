# 314 — Take opennlp-tools 2.5.9 on release/2.0, and remove the dead JWNL code

Implementation plan for https://github.com/rreganjr/Requel/issues/314.
Base: `release/2.0` @ `1c828481`. Branch: `314-opennlp-2x`.

## Summary

`master` already declares `opennlp-tools` 2.5.9 (#41). This ticket makes `release/2.0` declare the
same version, so the eventual merge carries nothing for that line and nothing has to be committed to
`master`. It also keeps two Dependabot alerts closed. They read "fixed" today only because `master`
is the default branch, and they would reopen as soon as #300 makes `release/2.0` the default:

| Alert | Severity | Advisory | Affected |
|---|---|---|---|
| #27, GHSA-4v8g-86x5-3vrc | critical | XXE in `DictionaryEntryPersistor` | < 2.5.9 |
| #29, GHSA-659w-93r5-9j6m | high | OOM DoS in `AbstractModelReader` (the reader `readGISModel` uses) | < 2.5.9 |

The bump needs one compile fix (`SentenceDetector` takes `CharSequence` in 2.x) and one behavior
fix (`POSTaggerME` returns UD tags by default in 2.x). `opennlp-maxent:3.0.3` is removed. None of the
bundled OpenNLP models that go through the 1.5+ constructors load on 1.8.2, and they still won't on
2.5.9; making them work is out of scope. The runtime bar is **no worse than 1.8.2**.

Folded in: the dead JWNL code the issue already lists, and a span bug in `Sentencizer` that cuts
multi-sentence text into fragments on both versions (decision 8).

Backend only. Gate: `mvn clean verify`.

## Locked decisions

1. **Option 1, on `release/2.0`, in a normal squash-merged PR.** Options 2 and 3 are ruled out by
   the alerts: reverting `master` to 1.8.2, or leaving `release/2.0` on 1.8.2 past #300, reopens a
   critical and a high alert. Decided in review.
2. **All four OpenNLP classes stay** (`Sentencizer`, `OpenNLPTokenizer`, `OpenNLPTagger`,
   `OpenNLPParser`), even though only `Sentencizer` is wired. The whole NLP stack may be dropped
   later, so deleting them now buys little. They are kept compiling and correct under 2.x, not made
   to work. Decided in review.
3. **Working models are out of scope.** No `nlp-data-2` in this ticket. Decided in review; the
   legacy NLP isn't really in use.
4. **The runtime AC becomes "no worse than 1.8.2"**, replacing "all five OpenNLP tools load their
   bundled models" (there are four, and three of them fail to load on 1.8.2 already).
5. **`opennlp-maxent:3.0.3` is removed.** No source imports `opennlp.maxent.*` or
   `opennlp.model.*`; everything uses `opennlp.tools.*`, which 2.x covers.
6. **Penn tags via one helper in `AbstractOpenNLPTool`.** `newPosTagger(POSModel)` returns
   `new POSTaggerME(model, POSTagFormat.PENN)`, and both construction sites in `OpenNLPTagger` use
   it. It lives in the base class because `OpenNLPTagger`'s static initializer throws, so a test
   can't call a static on `OpenNLPTagger` itself.
7. **JWNL removal exactly as the issue lists it.**
8. **Fix `Sentencizer.sentencize` in this ticket.** It's in the file the compile fix already
   touches, and `Sentencizer` is the one piece of OpenNLP code that actually runs. Agreed in review.

## What the tree actually looks like

- **Master is one line ahead.** `master` is two commits past merge base `09ef3e8c` (`6a9b7662` plus
  the #41 merge), changing only `modules/nlp-jpa/pom.xml`.
- **OpenNLP lives in five files**, all in `modules/nlp-jpa/.../nlp/impl/`: `AbstractOpenNLPTool`,
  `Sentencizer`, `OpenNLPTokenizer`, `OpenNLPTagger`, `OpenNLPParser`.
- **What's wired:**
  - Only `Sentencizer` is a `@Component`.
  - `NLPProcessorFactoryImpl.getTokenizer()`/`getParser()` return `StanfordLexicalizedParser`.
  - `getPosTagger()` returns `OpenNLPTagger`; its only caller is the `@Disabled` `NLPTests`.
  - `OpenNLPTokenizer`/`OpenNLPParser` are referenced only for their path constants in
    `NlpDataResourcesTest`.
- **The models.** All six in `requel-nlp-data-1.jar` are gzip-wrapped pre-1.5 GIS files.
  `readGISModel` loads `parser/build` and `parser/check` on both versions.
  `SentenceModel`/`TokenizerModel`/`POSModel`/`ChunkerModel(InputStream)` fail on both with
  "manifest is null". `Sentencizer` catches that and uses its `SimpleSentenceDetector`; CI logs the
  fallback ("Falling back to simple sentence splitter ... manifest is null").
- **Compiling against 2.5.9.** The five files, compiled against each jar: 1.8.2 compiles clean.
  2.5.9 gives three errors, all in `Sentencizer.SimpleSentenceDetector`, because
  `sentDetect`/`sentPosDetect` take `CharSequence` in 2.x. Every other class and constructor used
  has the same signature in 2.5.9 (`HeadRules(String)` is gone, but we use `HeadRules(Reader)`).
- **Tag format.** Wrapping `parser/tag.bin.gz` in a `POSModel` and tagging
  `The user files a report .` gives `DT NN VBZ , NN .` on 1.8.2, `DET NOUN VERB PUNCT NOUN PUNCT` on
  2.5.9 by default, and `DT NN VBZ , NN .` on 2.5.9 with `POSTagFormat.PENN`.
  `OpenNLPTagger.process` passes tags to `ParseTag.tagOf`, which expects Penn tags.
- **The `Sentencizer` bug.** `sentencize` treats `Span.length()` as an end offset, probably left
  over from the pre-1.5 API, where `sentPosDetect` returned `int[]` end positions.
  `The user logs in. Then the user files a report.` comes out as `The user logs in.` /
  ` Then the us` / `er files a report.` on both versions. Text ending without a terminator
  (`Users log in. Then`) throws `StringIndexOutOfBoundsException` ("Range [13, 4) out of bounds"). `NLPProcessorFactoryImpl.processText` runs
  `Sentencizer` before the Stanford parser on every text the legacy assistants analyze, so they parse
  fragments today.
- **`slf4j-api`.** 2.5.9 adds it as a dependency. The version is managed by the Spring Boot parent,
  and `requel-app` already bridges log4j to it.

## Contracts

- `modules/nlp-jpa/pom.xml` declares `opennlp-tools` **2.5.9** and no `opennlp-maxent`.
- `modules/dictionary-jpa/pom.xml` has no `jwnl` dependency. The root `pom.xml` has no
  `<repositories>` block. `lib/maven-repo/` is gone.
- `AbstractOpenNLPTool.newPosTagger(POSModel)` (protected static) returns a Penn-format tagger.
- `Sentencizer.sentencize(text)` returns the trimmed text of each detected span, followed by any
  non-blank trailing text. A single sentence, or text with no spans, comes back as one element, as
  it does today.

## Step by step

1. **Branch:** `git switch -c 314-opennlp-2x release/2.0`.
2. **`modules/nlp-jpa/pom.xml`:** `opennlp-tools` 1.8.2 → 2.5.9; delete the `opennlp-maxent`
   dependency.
3. **`Sentencizer`:**
   - `SimpleSentenceDetector.sentDetect`/`sentPosDetect` take `CharSequence` and work on
     `toString()`.
   - (Decision 8) `sentencize` builds each sentence from `text.substring(span.getStart(),
     span.getEnd()).trim()`, then appends the remainder after the last span's `getEnd()` if it isn't
     blank.
   - Drop the now-unused `InvalidFormatException` import.
4. **`AbstractOpenNLPTool`:** add `newPosTagger(POSModel)`. **`OpenNLPTagger`:** replace both
   `new POSTaggerME(posModel)` with `newPosTagger(posModel)`, and drop the unused `Sequence` import.
5. **JWNL** (the deletes are `git rm` in the commit commands; they can't be done over the device
   bridge):
   - Delete `modules/dictionary-jpa/.../init/WordNetSenseKeyInitializer.java`.
   - Delete `modules/nlp-jpa/.../impl/wordnet/ClassPathFileManagerImpl.java`, the only file in that
     package.
   - Delete `lib/maven-repo/` (`jwnl-1.4-rc2.jar` and its stub `.pom`).
   - Remove the `jwnl` dependency and its comment from `modules/dictionary-jpa/pom.xml`.
   - Remove the `<repositories>` block and the comment above it from the root `pom.xml`.
   - `NlpDataResourcesTest` javadoc: replace the "Deliberately absent" paragraph with a note that
     `WordNetSenseKeyInitializer` was removed in #314.
   - `doc/guides/NLP_DATA.md`: change the JWNL row of the dropped-files table to say the initializer
     was removed in #314.
   - `scripts/build-nlp-data.sh` keeps its `nlp/jwnl/wn30/...` mention:
     `WordNetDefinitionWordsInitializer` still reads the `wn30/*.xml.gz` glosses.
6. **Classpath** (found by the first `mvn clean verify`, after this plan was agreed):
   - **Exclude `xercesImpl`.** CoreNLP → XOM → `xercesImpl` makes Xerces the JAXP
     `DocumentBuilderFactory`. Xerces rejects the `accessExternalDTD` attribute that OpenNLP 2.x's
     `XmlUtil` sets (part of the XXE fix), so every `POSTaggerME` construction threw
     `IllegalArgumentException`. The fix excludes `xercesImpl` from both CoreNLP declarations in
     `nlp-jpa` and both direct ones in `requel-app`, so JAXP resolves to the JDK's built-in parser.
     Nothing in Requel imports `org.apache.xerces`.
   - **Remove the legacy OpenNLP jars.** `requel-app` also declared `opennlp:tools:1.5.0` and
     `opennlp:maxent:3.0.0`, the pre-Apache coordinates. Nothing in `requel-app` imports them, and
     they put a second copy of the `opennlp.tools.*` packages on the classpath beside 2.5.9, so
     they're removed. Dependabot never saw them: its advisories are filed under
     `org.apache.opennlp`.
7. **Tests** (below).
8. **Verify:** `bash tmp/314-verify.sh`.

## Test plan

New `modules/requel-app/src/test/java/com/rreganjr/nlp/impl/OpenNLPUpgradeTest.java`: plain JUnit
5, no Spring context, run by surefire. It sits in package `com.rreganjr.nlp.impl` so it can reach the
protected statics.

| Test | Asserts | Fails today? |
|---|---|---|
| `sentencizerSplitsOnSpanBoundaries` | `The user logs in. Then the user files a report.` → exactly `The user logs in.`, `Then the user files a report.` | Yes (fragments) |
| `sentencizerKeepsASingleSentenceWhole` | one sentence in → level `SENTENCE`, no children | No |
| `sentencizerKeepsTrailingTextWithoutATerminator` | `Users log in. Then` → `Users log in.`, `Then` | Yes |
| `readGISModelLoadsParserBuildAndCheckModels` | both non-null; 50 and 2 outcomes. Exercises `AbstractModelReader` on 2.5.9 (the #29 code path) | No |
| `posTaggerUsesPennTags` | wrap `parser/tag.bin.gz` via `readGISModel` + `new POSModel("en", maxent, new HashMap<>(), new POSTaggerFactory())`; `newPosTagger(model)` tags `The user` as `DT`, `NN`, and no tag is one of `DET NOUN VERB ADP PUNCT` | Yes on 2.5.9 without the helper |

Existing suites:

- `NlpDataResourcesTest` still passes; no data paths change.
- If an assistant test was written around the fragment behavior, the `Sentencizer` fix changes its
  result. That's intended; update the test and say so in `commit.md`.
- `mvn clean verify` green.
- e2e: no route or UI change; CI runs it as usual.

`tmp/314-verify.sh` (gitignored):

```bash
#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
if git --no-optional-locks grep -n -I -e 'net\.didion' -e '<artifactId>jwnl' -e 'opennlp-maxent' -e 'project-local' -- ':!doc/work' ':!doc/archive'; then
  echo "leftover JWNL/maxent references above"; exit 1
fi
test ! -e lib/maven-repo || { echo "lib/maven-repo still present"; exit 1; }
grep -q '<version>2.5.9</version>' modules/nlp-jpa/pom.xml
mvn clean verify
```

## Out of scope

- Working OpenNLP models (`nlp-data-2`), and 2.x behavior of the tokenizer, tagger, chunker and
  parser beyond the tag format. They can't be exercised until models load; re-validate if models
  are ever replaced.
- Deleting the unwired OpenNLP classes (decision 2).
- `lib/`'s other Ant-era jars, including `lib/nlp/opennlp-tools-1.3.0.jar` and `maxent-2.4.0.jar`.
  Nothing references them, and Dependabot doesn't scan loose jars. A `lib/` cleanup is its own
  change.
- A `.github/dependabot.yml`: with `release/2.0` on the patched version there is nothing to suppress.
- Any commit to `master`.

## Risks

- **The `Sentencizer` fix changes what the legacy assistants see.** Multi-sentence text now reaches
  the Stanford parser as whole sentences instead of fragments, so annotations on multi-sentence
  goals and stories can change. That's the point of the fix, but it's a behavior change in a
  release candidate.
- **Hidden 2.x changes.** Only the code that runs is proven: the fallback splitter,
  `readGISModel`, and the tag format. The rest compiles but can't load models on either version.
- **The app-wide JAXP switch.** Excluding Xerces moves every XML parse in the app (JAXB import,
  digester, VerbNet) onto the JDK parser. `requel-app`'s pom already says it prefers the JDK's JAXP
  implementations. `mvn clean verify` and CI e2e cover it.
- **`slf4j-api` convergence.** Managed by the Spring Boot parent. If `mvn dependency:tree` shows two
  versions, pin it.

## AC mapping

| AC | Covered by |
|---|---|
| Decision recorded on the issue, with reasoning | Decision comment (`tmp/314-decision-comment.md`) |
| `master` and `release/2.0` declare the same `opennlp-tools` version | Step 2: both at 2.5.9 |
| Dependabot's re-raised PR handled deliberately | Revised: nothing to re-raise. `release/2.0` is on the patched version, so #27/#29 stay fixed after #300 |
| Option 1: verify green, runtime check, `opennlp-maxent` decided | Revised to "no worse than 1.8.2": `OpenNLPUpgradeTest`, `mvn clean verify`; `opennlp-maxent` removed (step 2) |
| No `net.didion.*`, `jwnl`, `lib/maven-repo/jwnl/` or `project-local` repository remains; `mvn clean verify` green | Step 5; checked by `tmp/314-verify.sh` |

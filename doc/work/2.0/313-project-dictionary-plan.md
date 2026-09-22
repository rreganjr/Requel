# 313 — Layered dictionary: install-level corpus plus project-scoped words

Implementation plan for https://github.com/rreganjr/Requel/issues/313.
Base: `release/2.0` @ `f3f14548`. Branch: `313-project-dictionary`.

## Summary

Add a third, project-scoped dictionary layer under the two existing installation-wide ones. A new
`project_dictionary_words` table holds the words users add through "Add to Dictionary"; the jazzy
classpath lists and the WordNet `word` table become read-only baselines. Spell-check lookups and
suggestions become project-aware by giving each project its own jazzy `SpellChecker` whose *user
dictionary* reads that project's rows. The words travel with the project through XML export/import
and are deleted with the project.

No authorization work here — #312 owns that and lands immediately after (see Locked decision 6).

## Locked decisions

1. **Per-project jazzy `SpellChecker`, not an existence check.** `isKnownWord` delegates to
   `SpellChecker.isCorrect`, and `findSpellingSuggestions` to `SpellChecker.getSuggestions`. Only a
   real per-project `SpellChecker` — same shared static dictionaries, a project-scoped user
   dictionary — makes a project word both *correct* and *suggestable*. An `isKnownWord(word) ||
   projectHasWord(...)` shortcut would satisfy AC1 and silently fail AC2.
2. **Cache entries are cheap and are built, not copied.** `staticDictionaries` is a
   `static final` collection of `SpellDictionaryHashMap` *instances*; a new `SpellChecker` re-adds
   the same objects by reference. A per-project checker costs one `SpellChecker` plus one
   `DatabaseSpellDictionary`, not another copy of the six `.dic` files.
3. **Scope key is a bare `Long projectId`, and the word entity has no association to `Project`.**
   `dictionary-jpa` depends on `platform-core` and nothing else of ours — not `project-domain`, not
   even `platform-identity` (#312 adds that one). Mapping a `@ManyToOne Project` would invert the
   module graph. The entity carries `projectId` as a plain column; the project's side of the link
   is a unidirectional `@OneToMany` + `@JoinColumn`, exactly as `Word.senses` already does.
4. **The project carries the words for export in a transient field, not a mapped collection.**
   `AbstractProjectOrDomain` gets a `@Transient @XmlElementWrapper(name = "dictionary")`
   `SortedSet<ProjectDictionaryWord>` that `ExportProjectCommandImpl` fills from the repository
   immediately before marshalling — the same carrier pattern, for the same reason, as
   `ProjectImpl.getExportTagAssignments()` right beside it.

   **Revised twice during commit 4, both times from a real failure.** It started as a mapped
   read-only `@OneToMany` + `@JoinColumn(insertable = false, updatable = false)` with
   `PERSIST, REFRESH` cascade mirroring `getGlossaryTerms()`. First the cascade had to go: with the
   join column read-only, cascading a persist inserts a word with a null `project_id`. Then the
   mapping itself had to go. Because the words are written through `DictionaryRepository` rather
   than through the collection, Hibernate's copy is only right when the export runs in a session
   that loads the project fresh; for a project created or imported in the same transaction the
   collection is still the empty set from the field initializer and the export silently carries no
   dictionary. `ProjectXmlStreamingRoundTripIT.projectDictionaryWordsRoundTrip` caught it as an
   empty `<dictionary/>`. A transient carrier cannot go stale, because nothing reads it but the
   marshaller.
5. **Project dictionary ≠ project glossary.** Two separate lists, by decision on the ticket.
   Glossary terms do not become known words in this ticket.
6. **The write stays ungated here.** `EditDictionaryWordCommandImpl` gains a `projectId` and
   nothing else: no `EditCommand`, no `AuthorizableCommand`, no `editedBy`, and the
   `catch (Exception e) { log.error(...) }` swallow stays. #312 adds all four on top, and is
   blocked on this ticket precisely because the gate cannot be scoped until the write is.
   Deliberately unchanged so the two diffs do not collide.
7. **Case-insensitive match, original case stored.** `(project_id, LOWER(lemma))` is the
   uniqueness rule, and lookups compare lower-cased. MySQL's default collation would give this for
   free; H2 under `create-drop` would not, and the ITs run on H2. Doing it explicitly in the query
   makes the two agree — the divergence class that bit #248.
8. **Migration for MySQL, entity mapping for the tests.** Flyway is disabled under the `test`
   profile and Hibernate runs `create-drop`, so the ITs get the table from the mapping; the
   deployment gets it from `V18__project_dictionary_words.sql`. Both must describe the same table.

## What the tree actually looks like

Verified at `f3f14548`:

- `JpaDictionaryRepository` is a `@Repository @Scope("singleton")`. `staticDictionaries` is a
  class-static `Collection<SpellDictionary>` loaded once from
  `nlp/jazzy/{eng_com,center,color,ize,labeled,yze}.dic`. `spellChecker` is a **single instance
  field**, built in the constructor: every static dictionary added, plus
  `setUserDictionary(new DatabaseSpellDictionary(this, (File) null))`. Passing `null` selects the
  `DoubleMeta` transformator for phonetic codes.
- `DatabaseSpellDictionary.getWords(phoneticCode)` is the only DB read in the spell path; it calls
  `dictionaryRepository.findWordsByPhoneticCode` against the WordNet `word` table.
  `addWord(text)` persists `new Word(text, getCode(text))` — a senseless row in that same table.
- Write path, unchanged by #305: `ResolveIssueWithAddWordToDictionaryPositionCommandImpl.execute()`
  → `EditDictionaryWordCommand.setLemma(getIssue().getWord())` → `addToDictionary(lemma)` →
  `SpellChecker.addToDictionary` → `DatabaseSpellDictionary.addWord`.
- **#305 put the project within reach for free.** `ResolveIssueCommandImpl` now implements
  `ProjectScopedCommand` with `getProject()` → `AnnotationCommandProjectResolver.of(getIssue())`.
  `ResolveIssueWithAddWordToDictionaryPositionCommandImpl` extends it, so `getProject().getId()` is
  already available in `execute()` with no new resolution logic.
- Read path call sites already hold a project:
  `LexicalAssistant.addSpellingIssue(..., ProjectOrDomain projectOrDomain, ...)` (project-jpa) and
  `LexicalSpellingAssistant.analyze(AssistantContext context, ...)` where
  `context.projectRef()` is an `EntityRef(String entityType, Long entityId)` (assistant-legacy-nlp).
- `SpellingChecker` and `SpellingSuggester` are `@Component` prototypes holding only a
  `DictionaryRepository`. `NLPProcessorFactoryImpl.newInstance` calls
  `getAutowireCapableBeanFactory().createBean(type)`, so **each `getSpellingChecker()` call returns
  a fresh instance** — a per-call `projectId` on the processor is safe, no shared mutable state.
- `NoOpNLPProcessorFactory` (`requel.nlp.enabled=false`) returns lambdas: `text -> Boolean.TRUE`
  and `text -> List.of()`. Any interface change must be mirrored there or the module stops
  compiling.
- `word` is `word (wordid, lemma UNIQUE(80), phonetic_code(80))` with FKs into it from `sense`,
  `lexlinkref`, `semcor_sentence_word` and `synset_definition_word`. Nothing in this ticket writes
  to it.
- The project table is **`pods`** (single-table inheritance for `AbstractProjectOrDomain`,
  discriminator `type`, PK `id`). There is no `projects` table. `ProjectImpl` is the only subclass,
  so only Project rows are ever referenced.
- Latest migration is `V17__delete_orphan_annotations.sql`; the reactor version in the root
  `pom.xml` is `2.0.0-dev`, so artifacts go in `doc/work/2.0/`.
- Export is reachability-driven JAXB over `ProjectImpl` plus the explicit
  `ExportProjectCommandImpl.CLASSES_FOR_JAXB` list. Import is the STAX set in
  `utils-jaxb/imports` (`GlossaryTerm{ImportXml,ImportXmlMapper,StaxImporter}`) plus an assembler
  in `project-jpa/imports`.
- `DeleteProjectCommandImpl` deletes children explicitly in reference-safe order and does not rely
  on DB cascades.

## Contracts

### Schema — `V18__project_dictionary_words.sql`

    CREATE TABLE `project_dictionary_words` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `project_id` bigint NOT NULL,
      `lemma` varchar(80) NOT NULL,
      `phonetic_code` varchar(80) DEFAULT NULL,
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_pdw_project_lemma` (`project_id`,`lemma`),
      KEY `idx_pdw_project_phonetic` (`project_id`,`phonetic_code`),
      CONSTRAINT `fk_pdw_project` FOREIGN KEY (`project_id`) REFERENCES `pods` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

`idx_pdw_project_phonetic` is the index the suggestion path uses —
`ProjectSpellDictionary.getWords(code)` queries by `(project_id, phonetic_code)` on every
suggestion lookup.

### Entity — `dictionary-jpa`, `com.rreganjr.nlp.dictionary.ProjectDictionaryWord`

Plain `@Entity @Table(name = "project_dictionary_words")`, `@GeneratedValue(strategy = IDENTITY)`
— **not** `AssignedOrGeneratedWordIdGenerator`, which exists only so dictionary imports can keep
dump-assigned ids. Fields: `id`, `projectId` (`@Column(name = "project_id", nullable = false)`),
`lemma`, `phoneticCode`. JAXB: `@XmlRootElement(name = "dictionaryWord")` with `lemma` and
`phoneticCode` as `@XmlAttribute`; `projectId` is `@XmlTransient` (the parent element supplies it
on import).

### Repository — `DictionaryRepository` additions

    Boolean isKnownWord(Long projectId, String word);
    List<String> findSpellingSuggestions(Long projectId, String word, int threshold);
    void addToDictionary(Long projectId, String word);
    List<ProjectDictionaryWord> findProjectWords(Long projectId);
    List<ProjectDictionaryWord> findProjectWordsByPhoneticCode(Long projectId, String code);
    int deleteProjectWords(Long projectId);

The existing project-less overloads stay and keep their current behaviour, for callers with no
project (and so `NoOpNLPProcessorFactory` and the non-project tests are unaffected). A `null`
`projectId` on the new overloads delegates to the old ones rather than throwing — the assistants
always have a project, but the processors are reachable from code paths that may not.

`addToDictionary(projectId, word)` writes a `ProjectDictionaryWord` with
`phoneticCode = generatePhoneticCode(word)` (the existing method, which is the same `DoubleMeta`
transformator jazzy uses) and evicts that project's cached checker. It is idempotent: an existing
`(project_id, lower(lemma))` row is left alone rather than raising a constraint violation, because
two users can resolve the same issue.

### The per-project checker cache — `JpaDictionaryRepository`

    private final Map<Long, SpellChecker> projectSpellCheckers = new ConcurrentHashMap<>();

    private SpellChecker spellCheckerFor(Long projectId) {
        if (projectId == null) return spellChecker;               // the installation-wide one
        return projectSpellCheckers.computeIfAbsent(projectId, id -> {
            SpellChecker checker = new SpellChecker();
            staticDictionaries.forEach(checker::addDictionary);   // same instances, by reference
            checker.setUserDictionary(new ProjectSpellDictionary(this, id));
            return checker;
        });
    }

`ProjectSpellDictionary` is `DatabaseSpellDictionary`'s sibling: same `SpellDictionaryASpell`
base, `getWords(code)` scoped to the project, `addWord(text)` writing a `ProjectDictionaryWord`.

It also **overrides `isCorrect` to compare case-insensitively**. The base class compares the query
against the candidate list exactly and then retries with the *query* lower-cased, never the stored
entry — right for the jazzy .dic files, whose entries are all lower-case, wrong for a layer that
stores a word in the case the user typed it. Without the override, adding "Requel" leaves "requel"
reading as a misspelling. Caught by `ProjectDictionaryIT` on the first run of commit 1. Comparing
in `isCorrect` rather than lower-casing on store keeps one entry per word, in its display case, so
suggestions are neither doubled nor flattened. Note this makes the project layer case-insensitive
where the installation-wide `DatabaseSpellDictionary` is not — a deliberate difference, not drift.

Eviction: `addToDictionary(projectId, ...)` and `deleteProjectWords(projectId)` both
`projectSpellCheckers.remove(projectId)`. The map is unbounded by project count — acceptable at
this scale (entries are two small objects), and noted under Risks.

### Factory and processors

    // NLPProcessorFactory
    NLPProcessor<Boolean> getSpellingChecker(Long projectId);
    NLPProcessor<Collection<NLPText>> getSimilarWordFinder(Long projectId);

The no-arg methods stay, delegating with `null`, so nothing that does not care has to change.
`SpellingChecker` and `SpellingSuggester` gain a `projectId` field with a setter;
`NLPProcessorFactoryImpl` sets it on the freshly created bean. `NoOpNLPProcessorFactory` gets the
two overloads returning the same constant lambdas.

### XML

`doc/samples/project.xsd`, in `abstractProjectOrDomain`, beside `glossary`:

    <xs:element name="dictionary" form="qualified" minOccurs="0">
      <xs:complexType>
        <xs:sequence>
          <xs:element ref="tns:dictionaryWord" minOccurs="0" maxOccurs="unbounded"/>
        </xs:sequence>
      </xs:complexType>
    </xs:element>

plus a top-level `<xs:element name="dictionaryWord" type="tns:dictionaryWord"/>` and its
`complexType` (attributes `lemma` required, `phoneticCode` optional). `minOccurs="0"` keeps every
existing project file valid.

Import mirrors the glossary set: `DictionaryWordImportXml`, `DictionaryWordImportXmlMapper`,
`DictionaryWordStaxImporter` in `utils-jaxb/imports` (the STAX importer looks for `dictionary` /
`dictionaryWord` the way `GlossaryTermStaxImporter` looks for `glossary` / `term`), a
`DictionaryWordImportDraft`, and a `DictionaryWordAssembler` in `project-jpa/imports` that writes
through `DictionaryRepository.addToDictionary(projectId, lemma)` so the phonetic code is recomputed
rather than trusted from the file.

## Step by step

**One branch, one PR, five commits on these boundaries** — `313-project-dictionary`, based on
`release/2.0`. The ticket was first written up as five stacked sub-PRs; that was reconsidered and
dropped. This repo squash-merges, so every branch above a merged one needs a
`git rebase --onto <old tip>` and a force-push, and four of those fall on the developer by hand.
More to the point, the first three pieces are not independently meaningful: commit 1 is a table and
an API nothing calls, and a reviewer cannot judge whether `isKnownWord(projectId, word)` is the
right shape until commit 3 writes through it. Stacking earns its keep with parallel reviewers or a
lower piece worth landing early, and neither applies here. Splitting the history rather than the
PR keeps the diff readable step by step, runs CI once, and leaves nothing half-landed on
`release/2.0` if the later steps slip.

### Commit 1 — schema, entity, repository, checker cache

1. `V18__project_dictionary_words.sql` as above.
2. `ProjectDictionaryWord` entity in `dictionary-jpa`.
3. `ProjectSpellDictionary` beside `DatabaseSpellDictionary`.
4. `DictionaryRepository` + `JpaDictionaryRepository`: the six new methods, the
   `projectSpellCheckers` cache, eviction.
5. Register the entity wherever `Word` is registered for the persistence unit; confirm H2
   `create-drop` builds the table (a context-loading IT is enough to prove it).

No caller changes; this commit is additive and the installation-wide behaviour is untouched.

### Commit 2 — thread the project through the read path

6. `NLPProcessorFactory` overloads; `NLPProcessorFactoryImpl.newInstance` + setter;
   `NoOpNLPProcessorFactory` overloads.
7. `SpellingChecker` / `SpellingSuggester`: `projectId` field, pass it to the repository.
8. `LexicalAssistant` — `getNLPProcessorFactory().getSpellingChecker(projectOrDomain.getId())` and
   the same for `getSimilarWordFinder`, at the two call sites around `checkSpelling` /
   `addSpellingIssue`.
9. `LexicalSpellingAssistant.analyzeProperty` — take the id from `context.projectRef()`, guarding
   for a null `projectRef`.

### Commit 3 — the write path

10. `EditDictionaryWordCommand` / `EditDictionaryWordCommandImpl`: `setProjectId(Long)`, and
    `execute()` calls `addToDictionary(projectId, lemma)`. Nothing else in that class changes
    (Locked decision 6).
11. `ResolveIssueWithAddWordToDictionaryPositionCommandImpl.execute()`: `command.setProjectId(
    getProject().getId())` using the `ProjectScopedCommand.getProject()` it already inherits from
    #305.
12. `CommandBackedAssistantResultApplicator` — verify the ADD_WORD_TO_DICTIONARY position it builds
    still resolves through the same command; no change expected, but it is the second producer of
    these positions and is easy to forget.

### Commit 4 — export / import

13. XSD: `dictionaryWord` element + type, `dictionary` wrapper on `abstractProjectOrDomain`.
14. `AbstractProjectOrDomain`: the read-only `@OneToMany` + `@XmlElementWrapper(name = "dictionary")`
    / `@XmlElementRef`.
15. `ExportProjectCommandImpl.CLASSES_FOR_JAXB` += `ProjectDictionaryWord.class`.
16. The four import classes + assembler, wired into the streaming import unit of work beside the
    glossary one.

### Commit 5 — delete cascade

17. `DeleteProjectCommandImpl`: a `dictionaryRepository.deleteProjectWords(project.getId())` step,
    placed with the other child deletes and before the project row itself. `project-jpa` already
    depends on `dictionary-jpa`; inject the repository the way the command takes its other
    collaborators.

## Test plan

Per-ticket verify script at `tmp/313-verify.sh`. No `requel-angular/**` changes, so no vitest, no
typecheck, no e2e: `mvn clean verify` is the whole gate.

New — `modules/requel-app/src/test/java/.../nlp/ProjectDictionaryIT.java`:

- **AC1** — two projects created in the test; add "requel" in A; `isKnownWord(aId, "requel")` true,
  `isKnownWord(bId, "requel")` false, `isKnownWord("requel")` (no project) false.
- **AC2** — with "requel" in project A, `findSpellingSuggestions(aId, "requle", 2)` contains
  "requel" and `findSpellingSuggestions(bId, "requle", 2)` does not.
- **AC7 (unit half)** — `deleteProjectWords(aId)` leaves B's rows.
- Case: adding "Requel" then "requel" in the same project yields one row and both spellings read as
  known.
- Cache: add a word, assert it is immediately known (proves eviction, not just the first read).

Per the repo's testing notes: create the projects the test needs rather than assuming ids or an
empty table — `@SpringBootTest` contexts are cached and earlier classes leave rows and
auto-increment counters behind, and ids from different tables collide routinely.

Extended:

- `ResolveIssueSpellingIT` (added by #305) — **AC3**: resolving an `AddWordToDictionaryPosition`
  adds a `project_dictionary_words` row for that project and leaves `word`'s row count unchanged.
- `ProjectXmlRoundTripIT` and `ProjectXmlStreamingRoundTripIT` — **AC6**: a project with two
  dictionary words exports and re-imports with both, on the DOM and the streaming path.
- `DeleteProjectIT` and `DeleteProjectMySqlIT` — **AC7**: a project with dictionary words deletes
  cleanly, a second project's words survive, and no FK or lock-wait failure on MySQL.
- `DictionarySQLMySqlImportIT` — **AC4**: project words present before the dump import are present
  and unchanged after it.
- **AC5** — a test that calls `AbstractIntegrationTestCase.ensureDictionaryLoaded()` with project
  words already present asserts the same.
- `NlpDisabledSmokeTest` — **AC9**: still green against the new overloads.

## Out of scope

- Authorization and exception propagation on the dictionary write (#312, immediately after).
- A UI to list or remove a project's dictionary words.
- Persisting the "Ignore word" resolution.
- Making glossary terms count as known words.
- Reclassifying the user-added rows already in `word`.

## Risks

- **Unbounded cache.** `projectSpellCheckers` has an entry per project touched since boot and no
  eviction policy beyond invalidation. Entries are small (Locked decision 2) and the deployment has
  hundreds of projects, not millions. If it ever matters, a size-bounded LRU is a drop-in.
- **Jazzy `SpellChecker` thread safety.** Today one instance serves every request, so the new
  per-project instances are no worse — but the cache makes sharing explicit rather than accidental.
  `ConcurrentHashMap.computeIfAbsent` must not do the DB read inside the mapping function; the
  dictionary reads lazily, per lookup, so it does not.
- **Double-mapped `project_id`.** The entity owns the column; the project's collection must declare
  `insertable = false, updatable = false` or Hibernate will refuse the duplicate mapping. Easy to
  get wrong and it fails at context startup, which is at least loud.
- **New XSD, old files.** Every existing project file still validates (`minOccurs="0"`), but a file
  exported after this lands will not validate against a pre-#313 `project.xsd`. Expected; called out
  because import/export compatibility is a stated guardrail.
- **`getIssue().getWord()` vs the project.** The resolve command takes the word from the issue and
  the project from the issue's grouping object — if an issue were ever regrouped, the pair could
  disagree. It cannot today (`AnnotationCommandProjectResolver` walks the same issue), and #312's
  gate will assert the project independently.

## AC mapping

| AC | Commit | Proven by |
|---|---|---|
| 1 — word in A unknown in B | Commit 1 + 2 | `ProjectDictionaryIT` |
| 2 — project word suggested only in that project | Commit 1 + 2 | `ProjectDictionaryIT` |
| 3 — resolve writes only the project table | Commit 3 | `ResolveIssueSpellingIT` |
| 4 — SQL dump reload leaves project words | Commit 1 | `DictionarySQLMySqlImportIT` |
| 5 — `dictionary.xml.gz` load leaves project words | Commit 1 | `ensureDictionaryLoaded` test |
| 6 — survives XML round trip | Commit 4 | `ProjectXml{,Streaming}RoundTripIT` |
| 7 — project delete removes only its own | Commit 5 | `DeleteProjectIT`, `DeleteProjectMySqlIT` |
| 8 — existing global `word` rows untouched | all | no migration touches `word`; asserted in AC3 |
| 9 — `requel.nlp.enabled=false` still boots | Commit 2 | `NlpDisabledSmokeTest` |

# 319 — Dictionary management: project word list and installation-wide words

Implementation plan for https://github.com/rreganjr/Requel/issues/319.
Base: `release/2.0` @ `717d627d`. Branch: `319-dictionary-management`.

## Summary

#313 gave each project its own dictionary, but only a spelling-issue resolution can write to it,
and nothing can read it back. This ticket adds:

- **a project dictionary page** (list, add, remove), with a **Dictionary** entry under each
  project in the left nav and on the project overview;
- **an installation-wide word list** in its own table, managed by a system admin from a new
  admin page, with both pages built on one shared list component;
- **the #313 regression fix:** per-project spell checks never consulted the WordNet `word` table.

One PR. Backend, one Flyway migration, and Angular. Gates: `mvn clean verify`, the Angular unit
suite, typecheck, dev build, and e2e in CI.

## Locked decisions

Decided in review:

1. **Project page add and remove need `Project[Edit]`.** The assistant stakeholder holds only
   `Annotation[Edit]`/`[Delete]`, so it can't change what counts as a misspelling in the text it
   checks. Adding by resolving a spelling issue keeps `Annotation[Edit]` (#312), through the
   existing `EditDictionaryWordCommandImpl`, unchanged. The page gets two **new** commands
   rather than a second gate on the old one.
2. **Installation-wide words get their own table, `install_dictionary_words`.** The WordNet
   `word` table becomes read-only again. The old project-less global add
   (`addToDictionary(String)`), which wrote a `word` row with no senses, writes to the new table.
   V20 moves the existing such rows across.
3. **The regression is fixed here.** jazzy's `SpellChecker` has one *user dictionary* slot
   (`setUserDictionary` replaces the field; `isCorrect` checks it, then `dictionaries`). #313 put
   `ProjectSpellDictionary` in that slot on project checkers, so `DatabaseSpellDictionary`
   (WordNet) dropped out. Confirmed in the `com.fifesoft:spellchecker:2.6.0` bytecode.
4. **Add as well as remove on the project page.**

Made in review without asking:

5. **Remove by row id.** The command refuses an id that isn't in the named project (the
   `projectId` is part of the delete's `WHERE`), and reports not-found when nothing matched.
6. **No re-analysis on removal.** A removed word is flagged again the next time an entity using
   it is analyzed. Existing issues aren't reopened.
7. **API exposure.** The two project commands join `GatewayPolicyConfig.ALLOWED`, so MCP gets
   tools from their input DTOs. The two installation commands go on `DENIED`, and the admin UI
   calls them through `/api/commands` (the `RepairProjectStakeholders` pattern).
8. **Word shape.** A word is trimmed, non-blank, has no whitespace inside it, and is at most
   80 characters (the column width in both tables). Otherwise it's refused with a field error on
   `lemma`. Adding a word already present, in any case, is a no-op that returns the existing row
   (the idempotence #313 chose).

## Checker layers after this ticket

| Checker | user dictionary (written by `addToDictionary`) | `dictionaries` (read-only) |
|---|---|---|
| installation (`projectId == null`) | `InstallSpellDictionary` (**new**; was WordNet) | six jazzy lists, WordNet `DatabaseSpellDictionary` |
| per project | `ProjectSpellDictionary` (unchanged) | six jazzy lists, WordNet (**fix**), `InstallSpellDictionary` (**new**) |

`DatabaseSpellDictionary` and `InstallSpellDictionary` are each one shared instance, added by
reference like the jazzy lists. Both read per lookup (a query by phonetic code), so an admin add
or remove applies everywhere at once with no cache eviction. `InstallSpellDictionary.isCorrect`
compares case-insensitively, like `ProjectSpellDictionary` (#313's reason: stored words keep the
case the user typed). `DatabaseSpellDictionary.addWord` is no longer reachable; it throws
`UnsupportedOperationException`, so a future caller fails loudly rather than writing to the corpus.

## Contracts

### Schema — `V20__install_dictionary_words.sql`

    CREATE TABLE install_dictionary_words (
      id bigint NOT NULL AUTO_INCREMENT,
      lemma varchar(80) NOT NULL,
      phonetic_code varchar(80) DEFAULT NULL,
      created_by_id bigint DEFAULT NULL,
      date_created datetime(6) DEFAULT NULL,
      PRIMARY KEY (id),
      UNIQUE KEY uk_idw_lemma (lemma),
      KEY idx_idw_phonetic (phonetic_code)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

- `created_by_id` is a soft reference with no FK, because `dictionary-jpa` can't name `users`.
- The default collation makes the lemma key case-insensitive on MySQL, and the repository checks
  case-insensitively too, so H2 agrees (#313 decision 7).
- The move: insert `lemma, phonetic_code` from every `word` row with no `sense`, `lexlinkref`
  (either side), `morphref` or `synset_definition_word` reference, using V17/V19's
  materialize-then-delete shape, then delete those rows from `word`. In the WordNet dumps every
  corpus word has a sense, so a row without one is a user addition. On a database with no
  corpus loaded, `word` is empty and the move does nothing.

### Entity and repository — `dictionary-jpa`

- `InstallDictionaryWord`: `id`, `lemma`, `phoneticCode`, `createdById` (plain `Long`),
  `dateCreated`, with `@GeneratedValue(IDENTITY)`.
- `DictionaryRepository` additions:

      List<InstallDictionaryWord> findInstallWords();                    // ordered by lemma
      List<InstallDictionaryWord> findInstallWordsByPhoneticCode(String code);
      InstallDictionaryWord addInstallWord(String lemma, Long createdById); // idempotent
      boolean deleteInstallWord(Long wordId);
      ProjectDictionaryWord findProjectWord(Long projectId, String lemma);   // case-insensitive, or null
      boolean deleteProjectWord(Long projectId, Long wordId);               // evicts the project's checker
      int countProjectWords(Long projectId);

- `addToDictionary(String)` becomes `addInstallWord(word, null)`.

### Commands — `project-jpa` (it can name `Project`, `User` and `SystemAdminUserRole`)

| Command | Input DTO (`service-api`) | Gate | Result |
|---|---|---|---|
| `AddProjectDictionaryWord` | `projectName`, `lemma` | `Project[Edit]` | `DictionaryWordDto(id, lemma)` |
| `DeleteProjectDictionaryWord` | `projectName`, `wordId` | `Project[Edit]` | none |
| `AddInstallDictionaryWord` | `lemma` | `RequiresSystemRole(SystemAdminUserRole)` | `DictionaryWordDto` |
| `DeleteInstallDictionaryWord` | `wordId` | `RequiresSystemRole(SystemAdminUserRole)` | none |

The project commands implement `ProjectScopedCommand` and hold the resolved `Project`, as
`EditDictionaryWordCommandImpl` does (its javadoc explains why re-loading the project refuses
everyone). A missing word is `NoSuchEntityException` (409 through `CommandController`).

### Queries — `service-impl`

- `GET /api/projects/{name}/dictionary` → `List<DictionaryWordDto>`, ordered by lemma. Lives in
  `ProjectQueryController`, so it reuses `requireProjectAccess`.
- `GET /api/admin/dictionary` → `List<DictionaryWordDto>`, from a new
  `DictionaryAdminQueryController`, with `ApiSecurityConfig` adding
  `.requestMatchers("/api/admin/**").hasRole("SystemAdminUserRole")` ahead of the `/api/**`
  catch-all.
- `ProjectDto` gains `int dictionaryWordCount` before `canDelete`. It's built in
  `ProjectQueryController.toDto` and `ProjectCommandRegistrar`, and by two test stubs
  (`StubProjectQueryGateway`, `QueryCommandsTest`).

### Angular

- `models/dictionary.ts`: `DictionaryWordDto { id: number; lemma: string }`.
  `core/dictionary.service.ts`: `listProjectWords`, `addProjectWord`, `removeProjectWord`,
  `listInstallWords`, `addInstallWord`, `removeInstallWord`.
- `shared/dictionary-word-list.ts`: a presentational component. Inputs: `words`, `loading`,
  `canAdd`, `canRemove`, `testid` prefix, `emptyMessage`. Outputs: `add(lemma)`,
  `remove(word)`. It owns the add form, its field error and the remove confirm. It's built on
  `app-data-table`, with the same add row and trash cell as `global-tags.ts`.
- `features/dictionary/project-dictionary.ts` at `projects/:name/dictionary`
  (`routeData({ section: 'project', breadcrumb: 'Dictionary' })`).
  `canAdd`/`canRemove` = `permissionService.canEdit('Project')`.
- `features/admin/install-dictionary.ts` at `/dictionary` behind `adminGuard`, titled
  "Installation Dictionary", linked from the admin accordion in `sidebar-nav.ts`.
- `sidebar-nav.ts` `entityGroups`: `{ label: 'Dictionary', type: 'Dictionary', count:
  project.dictionaryWordCount, icon: 'pi pi-language' }` after Glossary, plus its `navigate`
  branch. `project-workspace.ts` gets the same card. `models/project.ts` gains
  `dictionaryWordCount`, and the spec fixtures that build a `ProjectDto` get it too.

## Step by step

1. **Regression fix.** In `JpaDictionaryRepository`, keep the WordNet `DatabaseSpellDictionary`
   in a field and add it to every project checker's `dictionaries`. Add the `ProjectDictionaryIT`
   test below. This commit stands on its own.
2. **Installation layer.** `InstallDictionaryWord`, `InstallSpellDictionary`, the repository
   methods, the checker wiring in the table above, `DatabaseSpellDictionary.addWord` throwing,
   and `V20`.
3. **Project repository methods:** `findProjectWord`, `deleteProjectWord`, `countProjectWords`.
4. **Commands:** the four commands, factory methods, input DTOs, registrar entries, and the
   gateway allow/deny lists.
5. **Queries:** the two endpoints, the security matcher, and `ProjectDto.dictionaryWordCount`
   with its four construction sites.
6. **Angular:** model, service, shared component, both pages and routes, nav entries, the
   overview card, and the fixtures.
7. **Tests** (below), plus `doc/architecture/DICTIONARY_LOADING.md`: a "Layers" section with the
   table above. That's standing documentation, so it's edited in place.

Keep `tmp/319-verify.sh` running the full gate.

## Test plan

**Java**

| Class | Cases |
|---|---|
| `nlp/ProjectDictionaryIT` | a word only in `word` (persisted directly, as the corpus would hold it) is known and suggested inside a project (**fails on `717d627d`**); an installation word is known in every project and with no project; removing it makes it unknown everywhere at once; `deleteProjectWord` removes one word and leaves the same spelling in another project; a word id from another project is refused; `countProjectWords` |
| new `project/impl/command/ProjectDictionaryCommandTest` | add as a `Project[Edit]` holder; add refused without it; add refused for the assistant user; add is idempotent across case; blank, whitespace-inside and 81-character words refused on `lemma`; delete by id; delete of another project's id is not-found and changes nothing |
| new `project/impl/command/InstallDictionaryCommandTest` | admin add and delete; non-admin refused for both |
| `service/CommandGatewayIT` | the project commands through the gateway; the installation commands are denied there |
| `mcp/McpToolCatalogLockstepIT` | stays green, with the two new ALLOWED commands getting tools |
| MockMvc (the existing query-controller IT) | `GET /api/projects/{name}/dictionary` for a member, 403 for a non-member; `GET /api/admin/dictionary` 200 for an admin, 403 otherwise; `dictionaryWordCount` in the project list |
| `ProjectXmlStreamingRoundTripIT` | a removed word is absent from the export |
| `project/DeleteProjectMySqlIT` | V20 re-run (`runMigration`): a `word` row with no sense moves across, a row with a sense stays, and a second run is harmless |

**Angular unit:** the shared component (renders words; add emits the trimmed lemma; blank and
whitespace inputs are refused client-side; remove asks and then emits; add/remove are hidden
when not allowed); the project page (loads, adds, removes, read-only without `Project[Edit]`);
the admin page; the sidebar and overview entries with counts.

**e2e:** `dictionary.e2e.ts`. From the left nav, open a project's Dictionary; add a word; it
appears, and the nav count goes up; remove it and it's gone. As a user without `Project[Edit]`,
there's no add box or trash. As admin, add and remove an installation word from the admin page.

## Out of scope

Editing a word in place, bulk import, re-analysis on removal, editing the jazzy lists or the
WordNet corpus, and exporting installation words.

## Risks

- **An extra query per spell lookup in project context.** WordNet and the installation table
  are each an indexed phonetic-code lookup, which the installation checker already pays for
  WordNet. Assistant runs check a word at a time, so the cost grows with text length, not
  project count.
- **The regression fix changes assistant output.** Words known only to WordNet stop being
  flagged in projects, so existing spelling issues for them stay open until resolved or
  re-analyzed. That's the intended correction, and it goes in the PR body.
- **Legacy rows in `word` that other tables do reference.** V20's anti-join checks all four
  referencing tables, so none of those rows move.
- **`ProjectDto` shape change.** It's a record, so the compiler finds the four Java
  construction sites. The TypeScript fixtures are found by the `tsc` spec pass.

## AC mapping

| AC | Step | Proven by |
|---|---|---|
| Viewable from the left nav and overview, with counts | 5, 6 | MockMvc; sidebar/overview specs; e2e |
| `Project[Edit]` gates add/remove; read-only without it; assistant refused | 4, 6 | `ProjectDictionaryCommandTest`; page spec; e2e |
| Takes effect on the next check, no restart | 1–3 | `ProjectDictionaryIT` |
| Project-isolated | 3, 4 | `ProjectDictionaryIT`; `ProjectDictionaryCommandTest` |
| Removed word absent from export | 3 | `ProjectXmlStreamingRoundTripIT` |
| Admin manages installation words, applied everywhere at once; non-admin refused | 2, 4, 6 | `InstallDictionaryCommandTest`; `ProjectDictionaryIT`; e2e |
| Legacy `word` rows moved; corpus untouched | 2 | `DeleteProjectMySqlIT` V20 case |
| WordNet-only word not flagged in a project | 1 | `ProjectDictionaryIT` |
| Tests at each layer | 7 | this plan |

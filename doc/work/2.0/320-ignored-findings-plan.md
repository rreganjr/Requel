# 320 — Persist "Ignore" per entity + property, and fix the assistant-issue defects under it

Implementation plan for https://github.com/rreganjr/Requel/issues/320.
Base: `release/2.0` @ `3215f186` (#314 merged). Re-checked against `1c828481`: #314 and #332 change none of the files this plan touches, and `V21` is still the next migration.
Branches: `320-assistant-issue-defects`, then `320-ignored-findings` stacked on it.

## Summary

The review reproduced the ticket over MCP against a MySQL instance
(`tmp/320-repro-results.md`, `tmp/320-mcp/`). An ignored finding is **not** re-raised while its
resolved issue exists. What is broken sits underneath it:

- **Issues leak across entities.** The fallback lookups `findLexicalIssue` / `findIssue` ignore
  the annotatable, so a new finding attaches another entity's issue. Resolving, deleting, or
  accepting a fix on one entity then acts on all of them. Fix Spelling accepted from goal 10
  renamed goal 9.
- **Findings get stuck.** A finding that isn't `ACTIVE` is repointed at a new open issue but
  keeps its state, so the issue can never auto-resolve.
- **Import drops data.** Every issue resolution is lost, and so is every plain (non-lexical)
  issue: `AnnotationStaxImporter` reads only `<note>` and `<lexicalIssue>`.
- **An ignore lives only in its resolved issue.** Deleting the issue forgets it, and ignores
  can't be listed or undone.

Two stacked PRs:

| PR | Branch | Contents | Schema |
|---|---|---|---|
| A | `320-assistant-issue-defects` | leak fix, stuck-finding fix, import keeps resolutions and plain issues | none |
| B | `320-ignored-findings` | `IgnorePosition`, the ignored-findings store, V21, the applicator check, record on resolve, XML, delete paths, list/remove on the project dictionary page | `V21` |

PR A is pure defect work and stands on its own. If #320 is split, PR A becomes the new v2.0 bug
ticket as is, and #320 keeps PR B.

## Locked decisions

From the #320 review:

1. **Scope: per entity + property.** An ignore is keyed by the finding's idempotency key
   (`<assistant>:<type>:<id>:<finding-type>[:<property>]:<subject>`), which already encodes
   exactly that.
2. **Every finding type that offers an ignore**: `unknown-word`, `vague-word`, `complex-text`,
   `glossary-term`.
3. **Case-insensitive.** Keys are stored and compared lower-cased. `complex-text` hashes the
   sentence, so it matches the exact sentence.
4. **The leak fix is folded in.**
5. **Listed and removable** on the #319 project dictionary page.
6. **The stuck-finding fix is folded in.**
7. **Import keeps resolutions (folded in).** Import now also keeps plain issues: same importer,
   same fix. Found while planning, and shown live: the export had 1 `<issue>`, the import 0.
8. **Removing an ignore removes its resolved issue**, so the finding is raised again.

Made while planning:

9. **The store lives in `project-jpa`, behind an interface in `project-domain`.** Project
   delete, entity delete and XML export/import are all in `project-jpa`, which can't see
   `assistant-core` (`assistant_findings`). `assistant-core` already depends on
   `project-domain`, so the applicator reaches the store through the interface. This follows
   "interfaces in `-domain`, persistence in `-jpa`".
10. **`IgnorePosition` is a marker type.** Ignore positions are shared by text across every
    issue in a project (#284's reuse), so the position carries no subject. The resolve hook
    derives the ignore from the issue's findings.
11. **Record on resolve in `FindingResolutionTrackingCommandHandler`.** It already reacts to
    `ResolveIssueCommand` and looks findings up by `applied_annotation_id`. Recording there
    needs no new resolver command.
12. **One check in the applicator**, not in each assistant, so current and future assistants
    (the #258 AI ones) get it for free.
13. **Scoped lookups walk the annotatable's own annotations.** They iterate
    `annotatable.getAnnotations()` instead of adding a JPQL join through the `@ManyToAny`
    table. This is dialect-neutral, needs no new query, and entities carry tens of annotations,
    not thousands.

### Needs a thumbs-up

- **Re-analyze on remove.** Proposed: the remove command implements `AnalysisRequestSource`
  with the ignore's entity as its target, so `AnalysisInvokingCommandHandler` queues one run.
  Without that, the finding only returns when someone next edits the entity (results, H).
- **"Remove the resolved issue" means unlink it from this entity**
  (`RemoveAnnotationFromAnnotatable`, which deletes it once nothing else is attached). For a
  normal issue that is a delete. For an issue already shared through the old leak, the other
  entities keep their view.
- **V21 backfill includes leaked findings**, so what users see today doesn't change. Extras can
  be removed on the page.
- **`Project[Edit]` to remove**, as on the #319 page. Recording by resolving keeps
  `Annotation[Edit]`.

## Contracts

### PR A: scoped lookups (`annotation-jpa`)

`JpaAnnotationRepository`:

- `findLexicalIssue(grouping, annotatable, word[, property])`
- `findIssue(grouping, annotatable, message)`

Each walks `annotatable.getAnnotations()`. It keeps issues of the right type in the same
grouping object whose `word` (case-insensitive, `=`) and `annotatableEntityPropertyName` (null
matches null) match, or, for `findIssue`, whose `text` matches exactly. It returns the one with
the lowest id and throws `NoSuchAnnotationException` as today.

- **`annotatable == null`**: the only callers are the legacy `LexicalAssistant` and
  `AbstractAssistant`, which always pass one. Treat null as "no match" rather than
  project-wide.
- **Call sites** that change behaviour: `EditLexicalIssueCommandImpl` / `EditIssueCommandImpl`
  (used by the SPI applicator when a key is new), and legacy `LexicalAssistant` /
  `AbstractAssistant.addSimpleIssue` (unreachable, see Out of scope). They keep their
  signatures.
- **Deliberate change:** the legacy `addGlossaryIssue` shared one glossary issue per term
  across the project. It becomes per entity.

### PR A: stuck findings (`assistant-core`)

`CommandBackedAssistantResultApplicator.upsertFinding(context, result, action, annotation)`
now takes the applied annotation object (from `createdByActionKey`) instead of just its id.

When an existing finding isn't `ACTIVE` (`MANUALLY_RESOLVED`, `AUTO_RESOLVED`, `SUPERSEDED`)
and the applied annotation is an unresolved `Issue` or a `Note`, the finding is set to
`ACTIVE`, with `closedAt` and `supersededByRunId` cleared. This covers the delete-then-re-raise
case from the review and the analogous word removed → auto-resolved → word back case.

### PR A: import (`utils-jaxb`, `annotation-jpa`)

- `AnnotationStaxImporter` also reads `<issue>` as `AnnotationImportDraft.Type.ISSUE`. The
  assembler already handles `ISSUE`.
- `AnnotationImportXml` gains `resolvedByPosition`, `resolvedByUser` and `dateResolved`
  (`@XmlAttribute`), mapped onto three new `AnnotationImportDraft` fields.
- `AnnotationAssembler.assemble` resolves `resolvedByPosition` through
  `unitOfWork.resolve(PositionImpl.class, id)`, since positions are imported first. The user
  goes through the same path as `resolveCreatedBy`, falling back to the importing user as today.
  It then calls the issue's resolve with the parsed date. `IssueImpl` needs a package-level or
  assembler-visible setter for `dateResolved`, because `resolve()` stamps now.
- An unresolvable `resolvedByPosition` leaves the issue open and logs one WARN. It doesn't fail
  the import.

### PR B: schema (`V21__ignored_findings.sql`)

```sql
CREATE TABLE ignored_findings (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id    BIGINT       NOT NULL,
    target_type   VARCHAR(80)  NOT NULL,   -- as assistant_findings.target_type ("Goal")
    target_id     BIGINT       NOT NULL,
    assistant_id  VARCHAR(200) NOT NULL,
    finding_type  VARCHAR(120) NOT NULL,
    property_name VARCHAR(255) NULL,       -- null for glossary-term
    key_suffix    VARCHAR(255) NOT NULL,   -- key after "<assistant>:<type>:<id>:"
    key_lower     VARCHAR(255) NOT NULL,   -- LOWER(full idempotency key)
    subject       VARCHAR(500) NULL,       -- display: word, phrase, or sentence snippet
    annotation_id BIGINT       NULL,       -- the resolved issue, when known
    created_by_id BIGINT       NULL,
    date_created  DATETIME(6)  NULL,
    CONSTRAINT uq_ignored_findings_key UNIQUE (key_lower),
    INDEX idx_ignored_findings_project (project_id),
    INDEX idx_ignored_findings_target (target_type, target_id)
);
```

- **No foreign keys**, as with `assistant_findings`. Deletes are explicit (see Delete), which
  avoids the #247 FK and lock problems.
- **Retype** the existing ignore positions:
  ```sql
  UPDATE positions SET position_type = 'com.rreganjr.requel.annotation.impl.IgnorePosition'
  WHERE position_type = 'com.rreganjr.requel.annotation.impl.PositionImpl'
    AND text IN ('Ignore this word.', 'Ignore this phrase.');
  ```
- **Backfill** `INSERT … SELECT` from `assistant_findings f JOIN annotations a ON a.id =
  f.applied_annotation_id JOIN positions p ON p.id = a.resolved_by_position_id WHERE
  p.position_type = '…IgnorePosition' AND f.project_id IS NOT NULL`, grouped by
  `LOWER(f.idempotency_key)`. `key_suffix` is the key after `CONCAT(assistant_id, ':',
  target_type, ':', target_id, ':')`. `subject` comes from `a.word`, or else the evidence
  snippet in `f.evidence_json`, or else null.

### PR B: domain, store and position type

- **`project-domain`:** `IgnoredFinding` (read model) and `IgnoredFindingStore`:
  - `Set<String> ignoredKeys(Long projectId)` returns lower-cased keys, one query per apply.
  - `record(IgnoredFindingSpec, User)` is idempotent on `key_lower`.
  - `list(Long projectId)`, `find(Long projectId, Long id)` and `delete(Long projectId, Long id)`.
  - `deleteForTarget(String targetType, Long targetId)` and `deleteForProject(Long projectId)`.
- **`project-jpa`:** `IgnoredFindingImpl` (entity) and `JpaIgnoredFindingStore`.
- **`annotation-jpa`:** `IgnorePosition extends PositionImpl`
  (`@DiscriminatorValue("com.rreganjr.requel.annotation.impl.IgnorePosition")`, JAXB root
  `ignorePosition`), plus `EditIgnorePositionCommand(Impl)` with
  `AnnotationCommandFactory.newEditIgnorePositionCommand()`. It reuses an existing
  `IgnorePosition` with the same text in the grouping object, as `EditPositionCommand` does.
  `PositionTypes` pretty name: `IgnorePosition`. Resolving uses the base
  `ResolveIssueCommandImpl`; no new resolver.
- **`ResolveIssueCommand`** gains `getPosition()`. Today it only has the setter, and the hook
  needs the getter.

### PR B: assistants and applicator

- **The four SPI assistants** (`LexicalSpellingAssistant`, `LexicalVagueWordAssistant`,
  `LexicalComplexityAssistant`, `LexicalGlossaryTermAssistant`) emit their ignore position with
  `metadata.kind = "IGNORE"`. Nothing else changes, so keys are unchanged.
- **`applyPosition`** gets a `kind = IGNORE` branch that creates it with
  `newEditIgnorePositionCommand()`.
- **`apply()`** loads `ignoredKeys(projectId)` once. A `CREATE_OR_UPDATE_ISSUE` or
  `CREATE_OR_UPDATE_NOTE` whose `actionKey.toLowerCase()` is in the set is skipped: not
  applied, not added to `producedKeysByTarget`. Its children then skip on their own
  (`parentOfType` only consults `createdByActionKey`). Because the key isn't produced, an
  `ACTIVE`, untouched finding for a newly ignored key auto-resolves under
  `AUTO_RESOLVE_IF_UNTOUCHED`. A null project ref means no ignores.

### PR B: record on resolve (`requel-app`)

After a `ResolveIssueCommand` whose `getPosition()` is an `IgnorePosition`,
`FindingResolutionTrackingCommandHandler` records an ignore for every finding with
`applied_annotation_id` = the issue. The ignore takes the finding's project, target, assistant,
finding type, key, the issue's `annotatableEntityPropertyName`, its subject, and
`annotation_id`. It is best-effort like the existing bookkeeping: logged, never rolls back the
resolve.

### PR B: delete paths

- **Project:** `DeleteProjectCommandImpl` calls `ignoredFindingStore.deleteForProject`, next to
  `dictionaryRepository.deleteProjectWords` (step 10).
- **Entity:** `AbstractProjectCommand.removeAllAnnotationsBeforeDelete` also calls
  `deleteForTarget(entity's getProjectOrDomainEntityInterface().getSimpleName(), id)`, so every
  entity delete gets it through the one shared step.

### PR B: XML

- **Export.** `ProjectImpl` exports `<ignoredFindings><ignoredFinding assistant findingType
  property keySuffix subject target(IDREF) annotation(IDREF, optional) createdBy
  dateCreated/></ignoredFindings>`. `IgnorePosition` exports as `<ignorePosition>`. Both go in
  `doc/samples/project.xsd`.
- **Import.** An `IgnoredFindingStaxImporter`, run after annotations, resolves `target` and
  `annotation` through the unit of work. It rebuilds the key as
  `assistant:type:<new id>:keySuffix` and records the ignore. `PositionStaxImporter` reads
  `<ignorePosition>`. An unresolvable target skips the row with one WARN.

### PR B: commands and query (`project-jpa`, `service-api`, `service-impl`)

- **`GET /api/projects/{name}/ignored-findings`** returns `IgnoredFindingDto` (id, subject,
  findingType, entityType, entityId, entityName, propertyName, createdBy, dateCreated), ordered
  by entity then subject. `ProjectDto.ignoredFindingCount` powers the overview card, as #319
  did.
- **`DeleteIgnoredFinding`** takes `projectName, ignoredFindingId` and needs `Project[Edit]`. It
  deletes the row. If `annotation_id` is still attached to the target, it runs
  `RemoveAnnotationFromAnnotatable` (auth-exempt sub-step). It implements
  `AnalysisRequestSource` (target = the entity) so a run is queued. It goes on
  `GatewayPolicyConfig.ALLOWED`.
- **No "add ignore" command.** Resolving with Ignore is the only way in.

### PR B: Angular

- **`features/dictionary/project-dictionary.ts`** gets an "Ignored findings" section below the
  word list: subject, type, a link to the entity, property, who, when, and Remove, with a
  confirm. It is its own component, not a mode of `DictionaryWordList`.
- **`annotations-section.ts` `resolveLabel`** gets an explicit `case 'IgnorePosition': return
  'Ignore';`. The default stays "Ignore" for human positions.

## Step by step

### PR A (`320-assistant-issue-defects`)

1. **Regression test for today's behaviour** (`LexicalSpellingDispatchTest`): ignore, re-run
   with the text unchanged, no open issue. It passes before any change.
2. **Leak tests first** (they fail on the current code), then the scoped lookups.
3. **Stuck-finding test** (fails), then the `upsertFinding` change.
4. **Import tests** in both round-trip ITs (fail), then the importer, draft and assembler
   changes.
5. `tmp/320-verify.sh`: `mvn clean verify`.

### PR B (`320-ignored-findings`, based on A until A merges)

6. `IgnorePosition`, its command and factory method, `PositionTypes`, the JAXB root and XSD.
7. `V21`, `IgnoredFinding` / `IgnoredFindingStore`, `IgnoredFindingImpl` /
   `JpaIgnoredFindingStore`.
8. Assistants emit `kind = IGNORE`. The applicator gets its `IGNORE` branch and the ignored-key
   skip.
9. `ResolveIssueCommand.getPosition()`, and recording in
   `FindingResolutionTrackingCommandHandler`.
10. Delete paths (project, entity).
11. XML export and import of ignored findings and `<ignorePosition>`.
12. The query endpoint, `ProjectDto.ignoredFindingCount`, `DeleteIgnoredFinding` with its DTO,
    registrar entry, gateway allow-list and `AuthorizationIT` rows.
13. Angular: model, service, section component, `resolveLabel`, specs.
14. `tmp/320-verify.sh` runs the full gate: `mvn clean verify`, the Angular unit suite with
    `CI=1 … --watch=false`, typecheck and a dev build. e2e runs in CI, because the dictionary
    page and its page object change.

## Test plan

### PR A

`LexicalSpellingDispatchTest` (H2), each with fresh entities:

- **Ignore, re-run unchanged:** not re-raised (regression, green before and after).
- **Two goals, same unknown word in Name:** two distinct issues, each attached to one goal.
  Repeat for a story, a vague word and a glossary phrase.
- **Fix Spelling on goal B's issue** leaves goal A's name unchanged. Add as Actor on B links
  only B.
- **Stuck finding:** resolve with a plain position, delete the issue, re-run (a new open issue),
  then fix the word and re-run. The issue is auto-resolved and the finding ends
  `AUTO_RESOLVED`.
- **Auto-resolved, then back:** remove the word (auto-resolved), add it back (open issue, finding
  `ACTIVE`), remove it again (auto-resolved).

`ProjectXmlStreamingRoundTripIT` and `ProjectXmlRoundTripIT`:

- **A resolved lexical issue** round-trips with `resolvedByPosition` (the same position text and
  type), `resolvedByUser` and `dateResolved`.
- **A plain `Issue`** (open and resolved) survives import.
- **An issue whose `resolvedByPosition` is missing from the file** imports open, with no
  failure.

### PR B

`LexicalSpellingDispatchTest`:

- **Ignore survives deletion:** ignore, delete the resolved issue, re-run: no issue.
- **Per type:** the same for `vague-word`, `complex-text` and `glossary-term`.
- **Case:** ignore "Zorblat" in Name, rename to "zorblat checkout": no issue.
- **Scope:**
  - ignore on goal A: goal B still gets its own issue;
  - ignore on Name: the same word in Text is still flagged;
  - project A's ignore doesn't touch project B.
- **Newly ignored key, `ACTIVE` finding elsewhere on the same entity:** auto-resolved on the
  next run.

`IgnoredFinding` IT:

- Resolving with a plain position records nothing.
- Resolving with an `IgnorePosition` records one row per finding, idempotently.
- `DeleteIgnoredFinding` removes the row, unlinks the issue, and queues a run. The re-run raises
  a fresh issue.
- A shared legacy issue is unlinked from this entity only.

Deletion:

- `DeleteProjectIT` + `DeleteProjectMySqlIT`: ignores go with the project, another project's
  survive, no FK or lock-wait failures.
- A goal delete removes that goal's ignores only.

XML round-trip ITs:

- Ignored findings and `IgnorePosition` round-trip, and the keys are rebuilt with the new ids.
- After import, a re-run doesn't raise the ignored findings.

`V21` MySQL IT (the `DeleteProjectMySqlIT` re-run-a-migration pattern): seed findings,
annotations and positions, including two findings whose keys differ only in case and one leaked
finding. Run V21, then assert the retype, the backfill rows, and one row per lower-cased key.

Other:

- `AuthorizationIT`: `DeleteIgnoredFinding` needs `Project[Edit]`, and assistant-only
  permissions are refused.
- Angular:
  - `project-dictionary.spec.ts`: renders the section, and Remove calls the command and
    reloads.
  - `annotations-section` spec: `IgnorePosition` → "Ignore".

## Out of scope

- **The legacy `LexicalAssistant` / `AbstractAssistant` path.** It is unreachable:
  - every analyzing edit command implements `AnalysisRequestSource`;
  - `ImportProjectStreamingCommandImpl.analysisEnabled` defaults to `false`;
  - the other `invokeAnalysis()` bodies are no-ops.

  It gets the scoped-lookup change for free. It gets no ignore check and no fix for its
  complexity lookup (`findIssue(…, sentence)` never matches the stored message).
- **Keeping `createdBy` on import** (imported annotations are owned by the importer).
- **Splitting issues already shared by the old leak.** They stay shared, and a re-run no longer
  adds sharing.
- **#284's position reuse by text.** It stays; `IgnorePosition` is designed around it.
- **`assistant_findings` rows outliving their entity or project.** This is pre-existing and not
  made worse.
- **`ResolveIssue` over MCP, and `@NotBlank name` on update inputs.** Both are the new 2.x
  tickets from this review.

## Risks

- **Keys become a contract.** An assistant that changes its key format orphans its ignores.
  Document it in `RequelAssistant.analyze`'s javadoc and in `doc/architecture/` next to the
  finding state machine.
- **V21's text-match retype** also retypes a human-written position that says exactly "Ignore
  this word.". Resolving with it then records an ignore, which is what the text says it does.
- **#314's Sentencizer fix (`3215f186`)** re-cuts every sentence after the first: it used to
  cut at `Span.length()` instead of the span offsets. Findings on second and later sentences
  change once after upgrade:
  - `complex-text` hashes change;
  - words the old cut split mid-word stop being flagged;
  - untouched findings auto-resolve;
  - an ignore on one of the old keys stops matching.

  First sentences are unaffected. Tests use fixtures analyzed on the new base. Nothing is
  migrated.
- **Scoped lookups change the legacy glossary behaviour** (one issue per term per project → per
  entity). It is only reachable via the legacy path, which is unreachable.
- **An ignore check on every issue action:** one `ignoredKeys` query per apply, held in a
  `Set`. No per-action query.

## AC mapping

| #320 acceptance criterion | Covered by |
|---|---|
| Regression: ignore, re-run unchanged, no open issue | PR A step 1 |
| Not re-raised after the resolved issue is deleted, all four types | PR B dispatch tests |
| Case-insensitive | PR B case test |
| No cross-entity suppression; no foreign issue attached | PR A leak tests, PR B scope tests |
| Fixing the text auto-resolves a re-raised finding | PR A stuck-finding tests |
| Survives restart and XML round trip (with resolution) | PR A import tests, PR B XML tests |
| Project A's ignore doesn't touch project B | PR B scope test |
| Entity and project delete remove ignores | PR B deletion tests |
| Listed and removable on the dictionary page; removal behaves as decided | PR B IT + Angular specs |

# #272 Entity provenance: record which external source an entity came from — implementation plan

Issue: https://github.com/rreganjr/Requel/issues/272 (child 5 of epic #267)
Branch: `272-entity-provenance`, cut from `release/2.0` @ `f2255e12`
Notes: `doc/work/backlog/entity-provenance-notes.md` (the issue body still names the pre-#304 path
`doc/entity-provenance-notes.md`)

## Summary

Requel is authoritative. An external artifact (a Jira ticket, a guide, a review) is recorded once
per project as an **external source**, and each entity built from it carries a **DERIVED_FROM
link** to that source naming the fragment it came from (`AC-4`, `§3.2`). Both live in side tables
keyed by entity type + id, the way `ignored_findings` and `assistant_findings` do, so nothing in
the entity graph reaches them and no context pack can include them.

A new composite gateway command, `UpsertFromSource`, wraps any allowlisted `Edit*` command. In one
transaction it resolves the link, decides NEW / UPDATE / UNCHANGED / CONFLICT, runs the `Edit*` only
for NEW and UPDATE, raises an Issue for CONFLICT (the source changed *and* the entity was edited in
Requel since ingest), and records the link. `upsertGoalFromRequirement` becomes a thin wrapper
over it. #71's `requel-provenance` notes are converted by V26 and deleted, which also closes the AC4
leak those notes cause today.

The source record is the shape #273 needs ("build one mechanism"). #272 ships the `DERIVED_FROM`
relation only; #273 adds `CITES`, project-level attachment and supersedes.

## Review against the tree (`release/2.0` @ `f2255e12`)

1. **#71 already ships provenance, on a plain Note.** `RequirementGoalUpserter` renders a
   `ProvenanceDescriptor` into a fenced `requel-provenance` block (`ProvenanceNotes`) and attaches
   it as a NOTE. That is Option B on a plain Note, the option the notes reject. Neither the
   issue nor the notes mention it.
2. **AC4 is violated in shipped code.** Those notes have `source = null` and are authored by the
   client's user, so `MachineAnnotations.isMachineGenerated` is false and
   `EntityContextPackBuilder` sends the note, `sourceUrl` included, to the model.
3. **Identity and change detection are the same field in #71.** The match key is
   `(sourceSystem, sourceRef, criterionHash)`, so an edited acceptance criterion hashes differently
   and the re-ingest creates a second goal. This ticket makes identity `(source, fragment, entity)`
   and uses the hash only to detect change.
4. **The #71 lookup is O(goals).** It calls `getAnnotations` for every goal in the project and
   parses the notes. A side table makes it one indexed query.
5. **#273 depends on this design.** Its body says "structurally the same record as child 5's
   provenance … build one mechanism". **#275 depends on it too**: its AC is "the output names the
   project version and its sources (child 5)".
6. **The only ingest primitive creates goals only.** The roundtable build made actors, use cases,
   scenarios, glossary terms and a stakeholder with plain `Edit*` calls, so AC1 had no path for
   those types.
7. **One fragment can produce several entities.** The case study says "AC1 became goals 1 and 2",
   so "the same source and fragment updates *the* entity" is ambiguous as written.
8. **Nothing covers a source edited while the entity was also edited in Requel.** With Requel
   authoritative, overwriting silently is wrong.
9. **"Ingest run" has nothing behind it.** Requel has no ingest-run concept. The field is replaced
   by the source version each link last saw, which also gives "not in the latest source version"
   (a fragment removed upstream) at read time, with no deletion.
10. **Fragment ids like `AC#4` are positional,** so inserting an AC renumbers them. Requel can only
    match the key it is given; the gateway description says the key must be stable.
11. **AC4 needs scoping.** Every MCP caller is a model, and #275's emitted document names its sources.
12. **Tags lowercase their values** (`CON-3685` → `con-3685`, `TagNormalizer`), which is one of the
    measured failures. The source record keeps `externalId` and `fragment` exactly as given.

Patterns to copy: the `IgnoredFindingStore` stack (domain interface, JPA store, `deleteForTarget`
in `AbstractProjectCommand`'s delete helper, `deleteForProject` in `DeleteProjectCommandImpl`, the
transient export carrier on `ProjectImpl`, the STAX importer + `unitOfWork.resolve` on import), the
`*MigrationMySqlIT` Testcontainers pattern for Flyway, and `TargetFingerprint` (#270) to detect
"edited in Requel since ingest".

## Locked decisions (2026-09-29)

1. **Direction: Requel authoritative.** Provenance is a pointer; external systems are ingest
   sources and emit targets. Bidirectional sync stays out of scope.
2. **One shared record with #273.** A project-scoped `external_sources` row plus an
   `entity_source_links` row that carries a relation kind. #272 ships `DERIVED_FROM`.
3. **Conflict:** when the source fragment changed and the entity was edited in Requel since the
   last ingest, the entity is not overwritten. The result is `CONFLICT`, and an Issue is raised on the
   entity with the source's new wording as a position. An entity untouched in Requel just updates.
4. **API:** primitives `RecordSource` / `LinkSource` / `UnlinkSource` and reads `getSource`,
   `findEntitiesBySource`, `getEntitySources`. The server enforces the policy through the generic
   composite `UpsertFromSource` (decided after the enforcement collision: a find → edit → link
   client protocol cannot tell the ingest's own edit from a user's). `upsertGoalFromRequirement`
   stays as a goal-specific wrapper.
5. **Fan-out:** links are `(source, fragment, entity)`. `findEntitiesBySource` returns all of them.
   `UpsertFromSource` updates when exactly one entity **of the command's type** matches, and refuses
   with the candidate ids when several do. The caller then passes `entityId`.
6. **Migration:** V26 converts every #71 `requel-provenance` note to source + link rows and deletes
   the note. Roundtable's `source` tags are left alone.
7. **AC4 scope:** assistant context packs exclude provenance by construction. General reads
   (`getEntity`, `getProjectContext`, `getAnnotations`, #274's content read) never carry it. Only
   the three provenance reads return it. Emit (#275) reads it in-process.
8. **UI:** a read-only Sources section on the entity editors (system, external id, fragment,
   ingested-at, "not in latest source"). A `URL` locator opens in a new tab. No edit, no remove, no ingest UI.

Signed off 2026-09-29 (P6, P7 and P10 revised in discussion):

- **P1** Entity types: Goal, Story, Actor, UseCase, Scenario, Step, GlossaryTerm, Stakeholder.
  ReportGenerator is not ingested.
- **P2** An entity may carry links to several sources, as with the case study's four inputs.
- **P3** `system` is lower-cased (a vocabulary: `jira`, `github`, `doc`). `externalId` and
  `fragment` are stored and matched exactly.
- **P4** A missing `fragment` means "the whole source". For backward compatibility,
  `upsertGoalFromRequirement` without a `criterionRef` uses `hash:<first 12 of criterionHash>` as
  the fragment, which is #71's behaviour; the migration does the same for notes with no
  `criterionRef`.
- **P5** Fragment hash = `CriterionHash`, for continuity with #71's stored hashes (case-only edits
  are not detected, which is accepted).
- **P6** Migrated links have no entity fingerprint (#71 never stored the goal's text at ingest).
  On the first re-ingest that touches one, it is **inferred**: if `CriterionHash.of(goal.text)`
  equals the stored `criterionHash`, the text is still what #71 wrote and the link counts as
  unedited, so it updates. Otherwise it counts as edited and raises a CONFLICT. Either way the
  fingerprint is then filled in. The only wrong answer this can give is an unnecessary conflict,
  never an overwrite.
- **P7** A source's location is `locator` + `locator_type`. `URL` accepts `http`/`https` only, up to
  2048 characters. `PATH` is a relative path (no scheme, no `..`, e.g.
  `docs/Roundtable-Production-Guide.pdf`), stored as text and never opened by Requel. An
  `ATTACHMENT` type (an attachment id) is added when something stores files; #273 does not.
  A file source's `fragment` is a page or section (`p.12`, `§3.2`), and its `contentHash` is the
  caller's SHA-256 of the file bytes. No locator of any type is ever written into annotation
  text (the conflict Issue included) or reaches a model.
- **P8** Removing a link: `UnlinkSource` with the entity's `Edit` permission, and no UI.
  Deleting an entity removes its links; deleting a project removes its sources and links. A source
  with no links is kept, since #273 citations need that.
- **P9** Conflict Issue: authored by the caller, text
  `Source <system> <externalId> <fragment> changed since it was ingested; this <type> was edited in
  Requel since, so it was not updated.` It is reused by text (one open Issue per link). Each new source
  wording is added as a position `Source now reads: <text>`. Text is capped like
  `RequirementGoalUpserter.MAX_TEXT_LENGTH`.
- **P10** After V26, nothing writes or reads the `requel-provenance` note, so `ProvenanceNotes` and
  `ProvenanceDescriptor` are deleted. `CriterionHash` (now the fragment hash) and
  `GoalNameDerivation` stay. The `upsertGoalFromRequirement` input stays compatible, gaining
  optional `goalId` and `sourceVersion`. Its result drops `noteId` because there is no longer a
  note. The migration IT keeps a real #71 note as a static text fixture, captured before the
  renderer is deleted.

## Contracts

### Schema (`V26__entity_provenance.sql`)

```sql
CREATE TABLE IF NOT EXISTS `external_sources` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `system` varchar(40) NOT NULL,            -- lower-cased
  `external_id` varchar(255) NOT NULL,      -- exact
  `locator_type` varchar(20) DEFAULT NULL, -- URL | PATH (ATTACHMENT later)
  `locator` varchar(2048) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `kind` varchar(40) DEFAULT NULL,          -- #273
  `content_hash` varchar(128) DEFAULT NULL, -- caller-supplied source version
  `last_ingested_at` datetime(6) DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_external_sources` (`project_id`, `system`, `external_id`)
);
CREATE TABLE IF NOT EXISTS `entity_source_links` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `relation` varchar(20) NOT NULL,          -- DERIVED_FROM (#273 adds CITES)
  `target_type` varchar(80) NOT NULL,
  `target_id` bigint NOT NULL,
  `fragment` varchar(255) DEFAULT NULL,
  `fragment_key` varchar(255) NOT NULL DEFAULT '',  -- fragment or '' so the unique key works
  `fragment_hash` char(64) DEFAULT NULL,
  `source_hash_seen` varchar(128) DEFAULT NULL,
  `entity_fingerprint` char(64) DEFAULT NULL,
  `ingested_at` datetime(6) DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_entity_source_links` (`source_id`, `relation`, `target_type`, `target_id`, `fragment_key`),
  KEY `idx_esl_target` (`target_type`, `target_id`),
  KEY `idx_esl_fragment` (`source_id`, `fragment_key`),
  CONSTRAINT `fk_esl_source` FOREIGN KEY (`source_id`) REFERENCES `external_sources` (`id`) ON DELETE CASCADE
);
```

The target is a soft reference, as in `ignored_findings`, so delete paths remove the links
explicitly. The #71 conversion runs in the same migration:

- Find notes whose text contains ```` ```requel-provenance ```` through `goals_annotations`, and
  extract the JSON with `SUBSTRING_INDEX`. Only `JSON_VALID` blocks convert; any other note is
  untouched and logged in a temp table the way V17 did.
- Upsert `external_sources` on (goal's project, lower(`sourceSystem`), `sourceRef`), with
  `locator` = `sourceUrl` and `locator_type` = `URL` (both NULL when the note had no URL).
- Insert a `DERIVED_FROM` link with fragment = `criterionRef`, or `hash:<12>` (P4),
  `fragment_hash` = `criterionHash`, and `entity_fingerprint` NULL (P6).
- Delete the converted notes from `goals_annotations`, `annotation_annotatable` and `annotations`.

### Derived link status (computed at read time, never stored)

- `notInLatestSource`: `source_hash_seen` differs from the source's `content_hash`, and both are
  non-null.
- `editedSinceIngest`: `entity_fingerprint` is null, or it differs from `TargetFingerprint.of(entity)`.
- `sourceChanged` (only when the caller supplies fragment text): `fragment_hash` differs from
  `CriterionHash.of(text)`.
- Upsert decision: no link → NEW; link and `!sourceChanged` → UNCHANGED (the link's
  `source_hash_seen` and `ingested_at` are refreshed); `sourceChanged && !editedSinceIngest` →
  UPDATE; `sourceChanged && editedSinceIngest` → CONFLICT.

### Domain (`project-domain`)

- `ExternalSource`, `EntitySourceLink`, `SourceLinkRelation { DERIVED_FROM }`, and `ProvenanceStore`
  (`recordSource`, `findSource`, `linksForTarget`, `linksForSource(sourceId, fragment?)`, `link`,
  `unlink`, `deleteForTarget`, `deleteForProject`), mirroring `IgnoredFindingStore`.
- Move `TargetFingerprint` from `assistant-core` to `project-domain`. It only depends on
  `NamedEntity` and `TextEntity`, and assistant-core already depends on project-domain.
- `ProvenanceEntityTypes.BY_NAME` holds the P1 types. `IgnorableEntityTypes` stays as it is.
- Commands: `RecordSourceCommand` (`Project[Edit]`), `LinkSourceCommand` and `UnlinkSourceCommand`
  (the target's `<Type>[Edit]` via `ProjectScopedCommand`), and `UpsertFromSourceCommand` (holds
  the inner `Edit*` command as a sub-command; the inner command is authorized normally, not exempt).

### Persistence (`project-jpa`)

- `ExternalSourceImpl`, `EntitySourceLinkImpl`, `JpaProvenanceStore`.
- Setter-inject `ProvenanceStore` into `AbstractProjectCommand` beside `IgnoredFindingStore`. The
  delete helper calls `deleteForTarget`, and `DeleteProjectCommandImpl` calls `deleteForProject`.
- `UpsertFromSourceCommandImpl`: resolve the source (recording it if new), resolve the links for
  (source, fragment, type), then decide. For UPDATE, set the resolved id on the inner command;
  for NEW, leave it null. Run the inner command through `getCommandHandler()` in the same
  transaction, then `link(...)` with the fresh `TargetFingerprint` and hashes. For CONFLICT, run
  `EditIssue` and `EditPosition` sub-commands (P9) and do not advance the link. The result carries
  `status`, `entityId`, `entityType`, `candidates` (when ambiguous) and `issueId` (on conflict).

### API (`service-api`, `service-impl`)

- Input records: `RecordSourceInput(projectName, system, externalId, locatorType, locator, title,
  contentHash)`,
  `LinkSourceInput(projectName, entityType, entityId, system, externalId, fragment, fragmentText)`,
  `UnlinkSourceInput(...)`, and
  `UpsertFromSourceInput(projectName, command, input /* JSON object */, system, externalId, locatorType, locator,
  title, sourceVersion, fragment, fragmentText, entityId)`.
- `UpsertFromSource` binding: `command` must be in the allowlist
  (`EditGoal, EditStory, EditActor, EditUseCase, EditScenario, EditStep, EditGlossaryTerm,
  EditStakeholder`; exact names confirmed in `ProjectCommandRegistrar` during step 3). The registry
  converts `input` to the command's input class with the gateway's `ObjectMapper` and binds it as
  usual. A per-type `SourceUpsertTarget` supplies the id setter and result-id accessor.
- DTOs: `ExternalSourceDto` (with `locatorType`/`locator`), `EntitySourceLinkDto` (+ the derived flags), and
  `UpsertFromSourceResultDto`. No existing DTO gains a provenance field.
- REST read for the UI: `GET /api/projects/{name}/entities/{type}/{id}/sources`.
- `GatewayPolicyConfig` allowlists the four writes. #296's rules apply: a record input DTO per
  command, caller-facing descriptions, and `authorizationHint` (`UpsertFromSource`'s is input
  dependent: the inner command's Edit permission).

### MCP / gateway / CLI

- `QueryGateway`: `getSource`, `findEntitiesBySource(projectName, system, externalId, fragment?)`,
  `getEntitySources(projectName, entityType, entityId)`. `McpReadService` exposes all three.
- `McpWriteService`: the four writes. The `upsertFromSource` description states that fragment keys
  must be stable (not renumbered positions) and explains CONFLICT.
- `RequirementGoalUpserter` is rewritten over `UpsertFromSource` with `EditGoal`, keeping name
  derivation and collision disambiguation. A CONFLICT or ambiguous result maps to the existing
  `UpsertGoalResult` plus `status`, `issueId` and `candidates`.
- CLI: `UpsertGoalCommand` gains `--goal-id` and `--source-version`. The other commands are
  reachable through the generic `run`.

### Export / import

- `ProjectImpl.getExportSources()` is a transient carrier filled by `ExportProjectCommandImpl`
  from the store: `<sources><source system externalId locatorType locator title kind contentHash lastIngestedAt>`
  with nested `<link relation entityType entityRef fragment fragmentHash sourceHashSeen
  entityFingerprint ingestedAt/>`.
- `ExternalSourceStaxImporter` and `ExternalSourceImportXml` in `utils-jaxb/imports`. The
  importer resolves `entityRef` through `unitOfWork.resolve` like `importIgnoredFindings`, and a link
  whose entity is missing is skipped with a WARN. Fingerprints carry over unchanged, since an
  imported entity has identical text.

### Angular

- `shared/sources-section.ts` (read-only card): system · external id · fragment · ingested-at, a
  "Not in latest source" chip. A `URL` locator gets an external-link icon
  (`rel="noopener noreferrer"`); a `PATH` shows as plain text. The card is hidden when there are no
  links.
- Added to the goal, story, actor, use-case, scenario, term and stakeholder editors. Steps have no
  editor of their own, so step sources show in the scenario editor's step rows only if the
  change stays small; otherwise they wait for a follow-on.

## Step by step

1. Move `TargetFingerprint` to `project-domain` (`assistant-core` imports updated; no behaviour change).
2. Domain types, `ProvenanceStore`, JPA entities and the store; V26 DDL.
3. Commands: Record/Link/Unlink, then `UpsertFromSource` with the decision table; the delete-path
   and delete-project hooks.
4. The V26 #71 note conversion and `ProvenanceMigrationMySqlIT`.
5. `service-api` inputs/DTOs, registrar bindings, policy allowlist, descriptions and hints (#296).
6. Query gateway reads, MCP read/write tools, the REST read.
7. Rewrite `RequirementGoalUpserter`; delete `ProvenanceNotes`/`ProvenanceDescriptor` and their tests;
   update the CLI.
8. Export/import carrier and importer.
9. Angular `SourcesSectionComponent` and the editor wiring.
10. Docs: a standing section in `doc/guides/local_mcp_bridge.md` (the ingest protocol), and a line in
    `entity-provenance-notes.md` pointing to this plan (a backlog note, so adding the pointer is
    allowed).
11. `tmp/272-verify.sh`: `mvn clean verify`, the touched Angular specs, and both `tsc` checks.

## Test plan

- **Store / commands (H2):** record is idempotent on (project, system, externalId), and a
  lower-case system matches while the externalId and fragment case is preserved (the tag regression).
  Link is idempotent on the unique key. Unlink needs Edit. Entity delete and project delete leave no
  rows, for every P1 type.
- **Decision table:** NEW, UNCHANGED, UPDATE and CONFLICT each asserted on the entity text, the link
  row and the result. CONFLICT leaves the entity untouched, raises one Issue, and a second conflicting
  re-ingest adds a position to the same Issue rather than a second Issue. A fragment with two goals
  is refused with both ids; passing `entityId` updates that one. An ingest that the caller cannot
  Edit is refused.
- **AC1:** re-ingesting the roundtable-shaped fixture (8 goals from one source plus actors and a
  use case) twice leaves the entity counts unchanged, and an edited AC updates in place (the #71
  duplicate regression).
- **AC2:** `getEntitySources` for an entity and `findEntitiesBySource` for a source agree.
- **AC3:** `RecordSource` with a new `contentHash` reports `changed: true`, and with the same hash
  `false`. Links report `notInLatestSource` after a version bump for fragments not re-ingested.
- **AC4:** `EntityContextPackBuilder`, `ProjectContextPackBuilder` and `IssueContextPackBuilder`
  output for a linked entity contains neither the locator (URL or path) nor the external id. `getEntity`,
  `getProjectContext` and `getAnnotations` JSON contain no provenance fields (a guard test).
  The CONFLICT Issue text contains no locator.
- **AC5:** `ProjectXmlStreamingRoundTripIT.provenanceRoundTrip` exports, imports into a new project,
  and compares sources, links (by entity name), hashes and fingerprints.
- **P6 inference:** on a changed re-ingest, a migrated link on an unedited goal updates and one on
  an edited goal raises a CONFLICT. The fingerprint is filled in both cases.
- **P7 locators:** a `PATH` with a scheme or `..` is refused, and so is a `URL` with a
  non-http(s) scheme.
- **Migration:** `ProvenanceMigrationMySqlIT` (Testcontainers) covers a #71 note with and without
  `criterionRef`, a malformed block (left alone), and a human note (left alone). After V26 there are
  no provenance notes, the rows exist, and `upsertGoalFromRequirement` on the migrated goal resolves
  to it.
- **Upserter:** the existing `RequirementGoalUpserterTest` cases are ported, and a note is never
  written.
- **Angular:** `sources-section.spec.ts` (hidden when empty, chip, link attributes) plus an a11y
  spec. Editor specs stay green.
- e2e: no route changes; the CI run covers the editors.

## Out of scope

- Bidirectional sync, writing to Jira, and any ingest UI.
- `CITES`, project-level references and supersedes (#273). Rendering sources in emit (#275).
- Fuzzy matching of renumbered fragments (#71's ticket-4 `findBestGoalMatch`).
- Converting roundtable's `source` tags.
- Deleting entities whose fragment disappeared upstream (they are only flagged).

## Risks

- **The generic composite binds arbitrary `Edit*` input.** Mitigated by the allowlist, the
  unchanged inner authorization, the id being injected only from the resolved link, and the caller's
  own id field being rejected on UPDATE when it disagrees.
- **Delete paths that bypass the `AbstractProjectCommand` helper** (for example #325's orphan-step
  deletion) would leave dangling links. Reads drop links whose target no longer resolves, and the
  per-type delete test catches the rest.
- **V26 parsing in SQL.** The fenced block is pretty-printed JSON. The IT uses text rendered by the
  real `ProvenanceNotes` before it is deleted (the fixture is captured in step 4).
- **Size.** This is the largest #267 child. It stays one PR per the one-ticket rule; commits follow
  the step order so it reviews in pieces.

## AC mapping

| AC | Where |
| --- | --- |
| Re-ingest updates, no duplicates | `UpsertFromSourceCommandImpl` decision table; roundtable-fixture IT |
| Entity → source+fragment; source → entities | `getEntitySources`, `findEntitiesBySource` |
| Changed source detectable without reading entities | `external_sources.content_hash`, `RecordSource.changed`, `notInLatestSource` |
| Never in a context pack / locator never to a model | side tables (by construction) + V26 note removal + context-pack and read guard tests |
| Export/import preserves provenance | `<sources>` carrier + STAX importer; round-trip IT |

## Implementation notes (2026-09-29)

Where the build departed from the text above, and why:

1. **`external_sources.source_system`, not `system`.** `SYSTEM` is a MySQL 8 keyword; Hibernate
   writes column names unquoted, so the V26 migration IT failed on it. The Java property is still
   `system`.
2. **P1's stakeholder type is `NonUserStakeholder`.** A link records
   `getProjectOrDomainEntityInterface().getSimpleName()`, which is what the entity delete path
   passes to `deleteForTarget`; for a stakeholder that is `NonUserStakeholder`, matching
   `EditNonUserStakeholder`. User stakeholders are not ingested and have no entry.
3. **No `EditStep` in the UpsertFromSource allowlist.** Steps have no gateway edit command of
   their own (they are edited through `EditScenario`). A step can still carry a link through
   `LinkSource`, and its links still travel through export/import.
4. **P6, precisely.** A converted link's fingerprint is filled in only when the goal is found
   unedited (on UNCHANGED or UPDATED). An inferred-edited goal keeps a null fingerprint, so it is
   inferred again next time and keeps conflicting until someone resolves it; filling it in with
   the edited text would have made the next changed re-ingest overwrite the edit. The plan's
   "either way the fingerprint is then filled in" was wrong on that point.
5. **Shared rules moved to `project-domain`.** `CriterionHash` joined `TargetFingerprint` there,
   so the server-side ingest and the gateway clients share one normalization; `SourceLinks`
   holds the "edited since ingest" rule the command and the reads both use.
6. **Every link is exported by IDREF,** a non-user stakeholder's included. The import unit of
   work registers stakeholders as `Stakeholder`, so a `NonUserStakeholder` link resolves through
   that type. Ingest times travel too, so an imported link keeps when it was ingested.
7. **Validation runs in the commands, before the store,** so a bad system, external id or locator
   is `INVALID_INPUT` rather than a persistence failure. Import applies the same locator rules and
   drops an unusable locator with a WARN rather than the source.
8. **SSE.** An `UpsertFromSource` that created, updated or raised a conflict on an entity publishes
   a refresh for that entity, as the edit itself would have.
9. **Tests added beyond the plan:** `EntityProvenanceMySqlIT` runs every `EntityProvenanceIT` case
   on MySQL with the Flyway schema (binary-collated ids included).
10. **The ingest fingerprint covers what the wrapped edit would overwrite** (`SourceLinks.fingerprint`):
    name and text as `TargetFingerprint`, plus a scenario's steps, a use case's primary actor, and
    a story's primary actor and type. `EditScenario` replaces the whole step list, so without it a
    step edited in Requel would have been overwritten by a changed re-ingest. For a goal it is
    exactly `TargetFingerprint`, which is what P6 infers against.
11. **An upsert sets a source's locator and title only when it first records the source.**
    `RecordSource` needs `Project[Edit]`, `UpsertFromSource` only the entity type's `Edit`; letting
    the upsert repoint an existing source would have let a goal editor change the link every
    entity's Sources card shows. It still records the version it read.
12. **AMBIGUOUS writes nothing.** The decision is made from the source as recorded before the
    source is updated, so the retry with `entityId` is the call that records the new version.
13. **V26's working tables name their collation**, so the join against `external_sources` cannot
    hit an illegal mix of collations on a server whose default differs.

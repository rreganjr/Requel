# #273 Reference attachments and document authority — implementation plan

Issue: https://github.com/rreganjr/Requel/issues/273 (child 6 of epic #267)
Branch: `273-reference-attachments`, cut from `release/2.0` @ `3520b455`
Builds on: #272 (`doc/work/2.0/272-entity-provenance-plan.md`), which built the shared
`external_sources` / `entity_source_links` record this ticket extends

## Summary

A reference is an `external_sources` row. #272 already stores it per project with a system,
external id, locator, title and a reserved `kind` column. This ticket:

- adds a `note` column;
- makes `kind` and `note` writable;
- adds a `CITES` relation so any entity #272 can link to a source can also cite one;
- adds a directed authority relation between sources: "A defers to B", meaning B wins where
  the two disagree.

Every project source is a project-level reference. Authority is a DAG that resolves
transitively. References still never reach a model. The bundled "HTML Specification" generator
gains a Resources section rendered from the export's `<sources>`.

## Review against the tree (`release/2.0` @ `3520b455`)

1. **"Superseded" is the wrong word.** The guide's colophon reads *"if the two ever disagree, the
   repository is correct and this document needs updating"*. The guide stays current. What it
   states is precedence on conflict, not obsolescence (case study §9).
2. **AC4 contradicted #272's locked decision 7.** #272 says provenance is never in a context pack
   or a general read. #273 allowed references in, "not as instruction-bearing text". Aligned to
   #272: never.
3. **The round-1 review is not a citation.** It is a DERIVED_FROM source. The case study's
   citation row is ENTRA_SETUP.md, the Zoom permissions matrix and RUNBOOK.md.
4. **"Attached to a project" had no definition.** Every source is already project-scoped, so
   recording one is attaching it.
5. **The shape omitted identity.** A source needs `system` + `externalId`. `note` has no column.
   `kind` has a column that nothing writes: `ProvenanceStore.SourceSpec` has no kind, so it is
   always null.
6. **No removal path.** #272 has no source delete, because ingest-driven sources are kept (P8).
   References attached by hand need one.
7. **Import drops every relation but DERIVED_FROM.** `ImportProjectStreamingCommandImpl` (~l.474)
   hard-codes `SourceLinkRelation.DERIVED_FROM` and ignores the link's `relation` attribute. The
   bug is harmless today and becomes data loss the moment CITES exists. Folded in.
8. **Emit already has a path.** `GenerateReportCommandImpl` runs the project's generator XSLT over
   the export XML (`GET /api/projects/{name}/reports/{id}/run`), and the export already carries
   `<sources>`. That contradicts #275's "nothing renders it" and makes AC3 doable here.
9. **The bundled XSLT is copied into each project when the project is created**
   (`EditProjectCommandImpl.addBuiltinReportGenerator`). Existing projects keep their copy, so
   roundtable's "HTML Specification" (id 696) will not show Resources until its text is replaced.

## Locked decisions (2026-09-29)

1. **Authority means precedence.** "A defers to B" means both stay current, and B wins where
   they disagree. Nothing is hidden or flagged as obsolete. Emit lists both, with "defers to B".
2. **Emit AC:** add a Resources section to the bundled `project2html.xslt` now. Also add a
   resources AC to #275 for its ticket-format generator.
3. **A project-level reference is every project source.** `RecordSource` is the attach. A new
   `listSources` read returns them all, DERIVED_FROM sources included.
4. **References never reach a model.** No context pack and no general read carries a source,
   its note, kind, citations or authority. Only the dedicated provenance reads and emit do. AC4
   is reworded to match, and the #272 guard test is extended.

Defaults stated in the review, not objected to:

- **D1** `kind` is an open vocabulary, stripped and lower-cased like `system`, up to 40
  characters. Suggested values (in the description only, not enforced): `doc`, `runbook`,
  `guide`, `review`, `matrix`, `ticket`, `repo`.
- **D2** `note` is up to 1000 characters. Only `RecordSource` writes `kind` and `note`.
  `UpsertFromSource` never does (the same reasoning as #272 note 11). Per #316's contract, null
  leaves the value unchanged and `""` clears it. Title and locator keep their #272 behaviour.
- **D3** `LinkSource` / `UnlinkSource` gain an optional `relation`, defaulting to `DERIVED_FROM`
  so existing callers are unchanged. A `CITES` link:
  - may name a fragment (`§4`, `p.12`), or none for the whole document;
  - carries no hashes or fingerprint;
  - is refused if `fragmentText` is supplied;
  - needs the entity type's Edit permission, as #272 does.

  CITES links never take part in `UpsertFromSource` decisions, because `linksForFragment` already
  filters by relation.
- **D4** Authority is a DAG:
  - a source may defer to several others;
  - self-edges and cycles are refused, and so is an edge between sources of different projects;
  - resolution is transitive and returns `A`, `B` or `NONE`;
  - adding or removing an edge needs `Project[Edit]`;
  - an edge carries an optional note (e.g. "operational detail"). The note is recorded and
    rendered, never evaluated.
- **D5** `DeleteSource` needs `Project[Edit]`. It is refused while the source has DERIVED_FROM
  links, naming the count, so provenance never vanishes silently. It removes the source's CITES
  links and authority edges (FK cascade).
- **D6** The UI stays read-only:
  - the entity Sources card labels each row "Derived from" or "Cites" and shows `kind`;
  - the project workspace gets a References card.

  There is no authoring UI.
- **D7** Export/import carries `note`, CITES links and authority edges. A link with an unknown
  relation, or an edge whose source is missing from the file, is skipped with a WARN (the #257
  rule).

## Contracts

### Schema (`V27__reference_attachments.sql`)

```sql
ALTER TABLE `external_sources` ADD COLUMN `note` varchar(1000) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `source_authority` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL,
  `subordinate_id` bigint NOT NULL,   -- defers ...
  `superior_id` bigint NOT NULL,      -- ... to this one; it wins where they disagree
  `note` varchar(1000) DEFAULT NULL,
  `created_by_id` bigint DEFAULT NULL,
  `date_created` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_source_authority` (`subordinate_id`, `superior_id`),
  KEY `idx_sa_superior` (`superior_id`),
  KEY `idx_sa_project` (`project_id`),
  CONSTRAINT `fk_sa_subordinate` FOREIGN KEY (`subordinate_id`) REFERENCES `external_sources` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sa_superior`    FOREIGN KEY (`superior_id`)    REFERENCES `external_sources` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

`entity_source_links.relation` is already `varchar(20)`, so `CITES` needs no DDL.
`deleteForProject` deletes `source_authority` rows before the sources. The cascade covers
this too, but the delete is explicit, as the other side tables do.

### Domain (`project-domain`)

- `SourceLinkRelation` gains `CITES` (javadoc: the entity refers to the source; nothing was
  built from it).
- `ExternalSource` gains `getNote()`, and `getKind()`'s javadoc drops "reserved".
- New `SourceAuthorityEdge` (id, projectId, subordinate, superior, note, createdBy, dateCreated).
- New `SourceAuthority`, a pure helper over a project's edges:
  - `resolve(edges, a, b)` returns `Winner { A, B, NONE }` plus the chain;
  - `wouldCycle(edges, subordinate, superior)` is a BFS from the superior along defers-to edges;
  - `superiorsOf(edges, source)` returns the direct superiors.
- `ProvenanceStore` gains:
  - `kind` and `note` on `SourceSpec`;
  - `deleteSource(sourceId)`;
  - `authorityEdges(projectId)`;
  - `addAuthority(subordinate, superior, note, by)`, idempotent on the pair (updates the note);
  - `removeAuthority(subordinateId, superiorId)`, returning a boolean.
- `ProvenanceStore.normalizeKind` (strip, lower-case, blank = null) and length constants
  `MAX_KIND_LENGTH = 40` and `MAX_NOTE_LENGTH = 1000`, checked in `validateSourceSpec`.
- Commands:
  - `DeleteSourceCommand` (`Project[Edit]`);
  - `AddSourceAuthorityCommand` and `RemoveSourceAuthorityCommand` (`Project[Edit]`);
  - a `relation` property on `LinkSourceCommand` and `UnlinkSourceCommand`.

### Persistence (`project-jpa`)

- `ExternalSourceImpl.note`, and `SourceAuthorityEdgeImpl`.
- `JpaProvenanceStore`: the new methods. `addAuthority` checks
  `SourceAuthority.wouldCycle` under the project's edges before inserting.
- `RecordSourceCommandImpl` writes `kind` and `note` (D2). `UpsertFromSourceCommandImpl` passes
  null for both.
- `LinkSourceCommandImpl` / `UnlinkSourceCommandImpl` use the relation. CITES skips the hash
  and fingerprint work.
- `DeleteSourceCommandImpl` counts DERIVED_FROM links and refuses when there are any; otherwise
  it deletes.
- The import fix (review 7): parse `link.getRelation()` with `SourceLinkRelation.valueOf`
  (case-insensitive). Unknown or blank: a blank is treated as `DERIVED_FROM`, for files written
  before #273; anything else is skipped with a WARN.

### API (`service-api`, `service-impl`)

- `RecordSourceInput` gains `@Size(max=40) kind` and `@Size(max=1000) note`. Its description
  now covers attaching a reference ("record a document the project refers to, or one its
  entities were built from").
- `LinkSourceInput` / `UnlinkSourceInput` gain `@AllowedValues(SourceLinkRelation.class)
  relation`, optional.
- New record inputs (#296: every allowlisted command has one, with a description and
  `authorizationHint`):
  - `DeleteSourceInput(projectName, system, externalId)`;
  - `AddSourceAuthorityInput(projectName, system, externalId, defersToSystem,
    defersToExternalId, note)`;
  - `RemoveSourceAuthorityInput(projectName, system, externalId, defersToSystem,
    defersToExternalId)`.
- DTOs:
  - `ExternalSourceDto` gains `kind` and `note`;
  - new `SourceAuthorityDto(subordinate, superior, note)`, where each side is `{system,
    externalId, title}`;
  - new `ProjectSourcesDto(sources[], authority[], linkCounts)`, with per-source counts by
    relation;
  - new `SourceComparisonDto(winner: "A"|"B"|"NONE", chain[])`.

  No non-provenance DTO changes.
- REST read for the UI: `GET /api/projects/{name}/sources` → `ProjectSourcesDto`.
- `GatewayPolicyConfig` allowlists the three new writes.

### MCP / gateway / CLI

- New `QueryGateway` methods, exposed by `McpReadService`, `RestQueryGateway` and
  `InProcessQueryGateway`, with descriptions in `QueryDescriptions`:
  - `listSources(projectName)` → `ProjectSourcesDto`;
  - `compareSources(projectName, system, externalId, otherSystem, otherExternalId)` →
    `SourceComparisonDto`.
- `getEntitySources` and `findEntitiesBySource` already return `relation` on each link. Their
  descriptions now say a link may be CITES.
- `McpWriteService`: the three new writes, plus `relation` on link/unlink. The
  `addSourceAuthority` description states precedence semantics in plain words: "`defersTo` wins
  where the two disagree; both remain current".
- CLI: reachable through the generic `run`. No new subcommands.

### Export / import

- The `ExternalSourceXml` / `ExternalSourceImportXml` `note` attribute. `kind` is already
  exported; the importer now passes it through `SourceSpec`.
- Inside `<sources>`, after the `<source>` elements:
  `<sourceAuthority system externalId defersToSystem defersToExternalId note/>`. It is exported
  in (subordinate, superior) order so output is deterministic. On import, edges are added once
  every source is recorded, and cycle checks still apply. A refused edge is skipped with a WARN.

### Emit (`project2html.xslt`)

- A `rp:project/rp:sources` template renders `<h2 id="resources">Resources</h2>`, with one
  entry per source in export order (system, external id):
  - title, or the external id when there is no title;
  - kind;
  - `system externalId`;
  - the locator: a URL as `<a href rel="noopener noreferrer">`, a PATH as `<code>`;
  - the note;
  - "Defers to: …" from the authority edges;
  - "Cited by" and "Derived" entity names from its links.
- A TOC entry when there is at least one source.
- Output is escaped by `xsl:value-of`. A URL is only ever http(s) (the #272 P7 validator), so
  no `javascript:` href can reach the page.

### Angular

- `models/provenance.ts`: `kind` and `note` on `ExternalSourceDto`. New `ProjectSourcesDto`
  and `SourceAuthorityDto`.
- `shared/sources-section.ts`:
  - a relation label per row ("Derived from" / "Cites");
  - the kind chip;
  - CITES rows omit ingested-at and the "Not in latest source" chip.
- New `features/projects/project-references.ts`, a read-only card on the project workspace
  beside the assistants panel:
  - title, kind, system · external id, the locator (same link rules as the Sources card) and
    the note;
  - "Defers to …" and "Outranks …";
  - hidden when the project has no sources.
- `provenance.service.ts`: `getProjectSources(name)`.

## Step by step

1. V27 DDL. Domain: `CITES`, `note`, `SourceAuthorityEdge`, the `SourceAuthority` helper (unit
   tests first: resolve, cycle, chain).
2. JPA entity and store methods. `deleteForProject` covers edges.
3. Commands: RecordSource `kind`/`note`, link/unlink relation, DeleteSource, Add/Remove
   authority.
4. The import relation fix, plus note, kind and authority export/import.
5. `service-api` inputs and DTOs, registrar bindings, the policy allowlist, descriptions and
   hints (#296).
6. Gateway reads `listSources` / `compareSources`, MCP tools, and the REST read.
7. The `project2html.xslt` Resources section.
8. Angular: Sources card relation label, References card, service and models.
9. Docs: the ingest-protocol section in `doc/guides/local_mcp_bridge.md` gains "Citing
   references and recording authority".
10. `tmp/273-verify.sh`: `mvn clean verify`, the touched Angular specs, and both `tsc` checks.
11. Issue text (hand-over, after sign-off): the revised #273 body (below) and a comment on #275.

## Test plan

- **`SourceAuthority` (unit):**
  - a chain A→B→C resolves C over A, with chain `[A, B, C]`;
  - unrelated sources resolve to `NONE`;
  - a diamond has no duplicate chain entries;
  - `wouldCycle` is true for a self-edge, a 2-cycle and a 3-cycle.
- **Commands (H2, `EntityProvenanceIT`):**
  - RecordSource sets kind and note, `""` clears, null keeps; kind is lower-cased; over-length
    values are `INVALID_INPUT`;
  - UpsertFromSource leaves an existing kind and note alone;
  - LinkSource CITES with and without a fragment; `fragmentText` with CITES is refused;
  - a CITES link on a goal does not turn an UpsertFromSource of the same fragment into
    UNCHANGED;
  - UnlinkSource CITES leaves a DERIVED_FROM link on the same entity;
  - AddSourceAuthority is idempotent, updates the note, and refuses self, cycle and
    cross-project edges; RemoveSourceAuthority removes the edge;
  - DeleteSource is refused with DERIVED_FROM links; without them it removes the CITES links
    and the edges;
  - every new write needs its permission.
- **AC1:** `listSources` returns a source with only CITES links and one with no links.
  `getEntitySources` on a citing entity returns the CITES link.
- **AC2:** `compareSources` agrees with `SourceAuthority.resolve` for the roundtable shape:
  guide → RUNBOOK.md and review → repo.
- **AC3:** GenerateReport with the bundled generator on a new project with two references and
  one edge:
  - the output has the Resources section, the TOC entry, "Defers to" and the note;
  - two runs are byte-identical;
  - a project with no sources has no section.
- **AC4:** the #272 guard test gains a CITES link, a note, a kind and an edge note. Entity,
  project and issue context packs, `getEntity`, `getProjectContext` and `getAnnotations`
  contain none of them.
- **Export/import:** `ProjectXmlStreamingRoundTripIT.provenanceRoundTrip` gains note, kind, a
  CITES link (the regression for review 7) and an edge. A file with `relation="BOGUS"` skips
  that link with a WARN. An edge to a missing source is skipped.
- **MySQL:** `EntityProvenanceMySqlIT` runs the new cases on the Flyway schema (V27 applied).
- **Angular:**
  - `sources-section.spec.ts`: the relation label, and no staleness chip on CITES;
  - `project-references.spec.ts` plus an a11y spec: hidden when empty, locator link attributes,
    defers-to text.

  Workspace spec stays green.
- e2e: no route changes; CI covers the workspace.

## Out of scope

- Fetching, storing or indexing referenced documents, and the `ATTACHMENT` locator type.
- Authority scoped to a topic. The edge note can say it; nothing evaluates it.
- Assistants using authority to reconcile findings (#258).
- An authoring UI for references and authority.
- Refreshing existing projects' copies of the bundled XSLT. For roundtable: after merge, one
  `EditReportGenerator` on id 696 with the new `project2html.xslt` text.
- #275's ticket-format generator. It gets its own resources AC.
- `GenerateReportCommandImpl` swallowing transform errors into a log line. That is #275's
  "fails with a clear message" AC.

## Risks

- **Cycle checks under concurrency.** Two edges added at once could close a cycle. Adds are rare
  and project-scoped, so the store takes a pessimistic lock on the project's `external_sources`
  rows during `addAuthority`. Reads also guard: `resolve` tracks visited nodes and never loops.
- **Relation default on import.** A blank relation means DERIVED_FROM, so pre-#273 files import
  as before. Only an explicit unknown value is skipped.
- **Emitted HTML from user text.** Notes and titles are escaped by `value-of`, and hrefs are
  restricted to http(s) by the existing validator. The XSLT test asserts an escaped `<script>`
  in a note.

## AC mapping

| AC (revised) | Where |
| --- | --- |
| A reference can be recorded on a project and cited by an entity, and read back on both | `RecordSource` (+kind/note), `LinkSource relation=CITES`; `listSources`, `getEntitySources` |
| A source can defer to another and a reader can resolve which wins | `source_authority`, `AddSourceAuthority`, `SourceAuthority.resolve`, `compareSources` |
| References appear in an emitted document | `project2html.xslt` Resources section; GenerateReport test |
| References never reach a model | side tables (by construction); extended guard test |
| Export/import preserves references, citations and authority | `<sources>` carrier + importer relation fix; round-trip IT |

## Revised issue body (for `gh issue edit`)

Written to `tmp/issue-273-body.md`. The #275 comment is in `tmp/issue-275-comment.md`.

## Implementation notes (2026-09-29)

Where the build departed from the text above, and why:

1. **Edges are exported nested in the subordinate's source,** as
   `<externalSource …><sourceLink/>…<defersTo system externalId note/></externalSource>`,
   not as a separate list inside `<sources>`. The export element is `externalSources` (#272), and
   nesting keeps a single StAX importer. The importer adds edges only after every source is
   recorded, since an edge may name a source later in the file. `project.xsd` (doc/samples and
   website/integration/2.0) gains `defersTo`, a `sourceAuthority` type and the `note` attribute.
2. **Edge refusals are checked before the store.** An `IllegalArgumentException` thrown by
   `JpaProvenanceStore` reaches the caller as Spring's `InvalidDataAccessApiUsageException`: a
   gateway `EXECUTION_ERROR` rather than `INVALID_INPUT`, and a joined transaction marked
   rollback-only, which would have failed a whole import. `AddSourceAuthorityCommandImpl.checkEdge`
   (self-edge, cycle) runs in the command and in the importer. The store keeps its own guards,
   including the project-row lock, as a backstop.
3. **Fixed: the bundled report had no attributes at all.** Xalan 2.7.3 is on the app classpath,
   and `TransformerFactory.newInstance()` picked it up. With secure processing on it drops every
   attribute of a literal result element ("`href` attribute is not allowed on the `a` element"),
   so the HTML Specification had no links, anchors or classes. This predates #273 and would have
   left the Resources section with no links. `GenerateReportCommandImpl` now uses
   `TransformerFactory.newDefaultInstance()` (the JDK's XSLTC), which honours secure processing
   and keeps attributes.
4. **A citation never reads as edited.** `SourceLinks.isEditedSinceIngest` treats a missing
   fingerprint as edited (#272 P6). A CITES link has none, so the read reports
   `editedSinceIngest: false` for it.
5. **`relation` is validated by `@Pattern`** as well as `@AllowedValues`, because `@AllowedValues`
   only documents the schema. An unknown relation is `INVALID_INPUT`.
6. **DTO names.** The DTOs are `ProjectSourcesDto(sources, authority)` and
   `ProjectSourceDto(source, derivedCount, citedByCount, defersTo, outranks)`. Edge ends are
   `SourceRefDto(system, externalId, title)`. `SourceComparisonDto(winner, a, b, chain)` echoes
   both sources.
7. **`SourceAuthorityTest` lives in gateway-api's tests** beside `CriterionHashTest`, because
   `project-domain` has no test tree.
8. **UI placement.** The References card sits full-width under the workspace's three panels, since
   its list can be long.

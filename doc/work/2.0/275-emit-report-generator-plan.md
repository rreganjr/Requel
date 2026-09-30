# #275 Emit through ReportGenerator — implementation plan

Issue: https://github.com/rreganjr/Requel/issues/275 (child 8 of epic #267)
Branch: `275-emit-report-generator`, cut from `release/2.0` @ `8b518994`
Builds on: #271 (severity), #272 (sources), #273 (Resources section, JDK XSLTC), #274 (content read)

## Summary

A bundled **ticket-format generator** (XSLT 1.0, text output) renders a project as the Markdown
ticket of the roundtable case study: header with project name, version and sources; Goals; Actors;
Use Cases with their scenarios and ordered steps; Glossary; Open Issues by severity; Resources;
Traceability. Output is byte-identical for an unchanged project. The run path stops swallowing
errors, takes its content type from the generator, and names the missing reference when a template
cannot resolve one. Bundled generators render from the classpath instead of a creation-time copy,
so existing projects (roundtable included) get the current bundle.

## Review against the tree (`release/2.0` @ `8b518994`)

1. **The body predates #273/#274.** Three comments correct it: the generator already runs
   (`GenerateReportCommandImpl`, `GET /api/projects/{name}/reports/{id}/run`), the input is the
   XML export, not #274's read. The body is rewritten below.
2. **"Project version" does not exist.** `project2html.xslt` prints `@revision`, which is the
   project row's JPA `@Version`. Editing a goal does not change it.
3. **"An AI definition of kind EMIT" does not exist.** #258 defines `REVIEW | POLICY | CORPUS`.
4. **The export order is not stable.** `getAllScenariosAndSteps` and `ProjectImpl.getAnnotations`
   build `HashSet`s. Goals/actors/etc. are `TreeSet`s by name. Byte-identical output needs every
   list sorted explicitly in the template.
5. **Output is hard-wired to HTML.** `ProjectQueryController.runReport` sets `text/html` and
   `.html` before the transform runs; `report.service.ts` `downloadReport` appends `.html`.
6. **Failures return an empty 200.** Headers are written first, then `execute()` catches every
   exception and logs it.
7. **Leaks and charset.** The temp export file is never deleted; `getText().getBytes()` uses the
   platform charset.
8. **Stale bundle.** `EditProjectCommandImpl.addBuiltinReportGenerator` copies
   `xslt/project2html.xslt` into each new project. Projects created before #273 (roundtable, 621)
   never got the Resources section, and would never get a new generator.
9. **XSLT does not fail on a missing selection.** A template that selects an entity by name or
   follows a dangling IDREF silently emits nothing. Failing needs an explicit check
   (`xsl:message terminate="yes"`) and an error path that surfaces the message.
10. **"Reproduced" cannot mean byte-equal to v2.** v2 has hand-written asides and a curated goal
    order. It needs a checkable definition (AC mapping below).
11. **The export already carries what the open-issues section needs**: `severity`,
    `mustBeResolved`, `resolvedByPosition` on `IssueImpl`. It does not carry #270's `stale` flag,
    which is derived at read (`AnnotationFreshness`) and is per entity + annotation: an issue
    shared by two entities can be stale on one only. #270 decided to flag stale on every read, and
    the export is a read, so #275 adds it (decision 6).

## Locked decisions (2026-09-29, signed off 2026-09-30)

1. **Project version = content fingerprint.** SHA-256 over the canonical JSON of #274's
   `getProjectContent(annotations=open)` plus the project's sources (system, externalId,
   contentHash, defersTo), shown as the first 12 hex chars. Changes only when rendered content
   changes; an edit-and-revert gives the same version. `@revision` is no longer printed as the
   version.
2. **Bundled generators render from the classpath.** New nullable column `reports.builtin_key`.
   A generator with a key renders the current bundled resource; editing its text detaches it
   (clears the key) so user changes are never overwritten. Unedited existing copies are adopted
   (see Migration). Every project gets the ticket generator.
3. **Missing references fail, both kinds.** (a) a dangling IDREF in the export (step, actor, goal,
   use case, term); (b) a template that selects an entity by name that is not there. Both go
   through one named template `require` that terminates with the reference in the message. The
   REST call returns 422 `REPORT_FAILED` with that message.
4. **Open Issues section: yes.** Unresolved issues with `mustBeResolved=true`, sorted like #270's
   lists: severity (HIGH, MEDIUM, LOW), then fresh before stale, then id. Each names the entity it
   is on; a stale one is marked "(stale: the entity changed since this was raised)". Advisory
   (lexical) issues are left out; counts of advisory and stale issues go in Traceability.
5. **Defaults (unchallenged):**
   - REST + UI only. MCP/CLI `runReport` is not in #275.
   - Entity order: id (creation order) in every section; steps in scenario order.
   - The generator writes Markdown: `xsl:output method="text" media-type="text/markdown"`.
   - Narrative sections (problem statement, blocking check, intentional behaviour, open
     questions prose) are not generated and have no placeholders; the spec doc names them.
   - Format spec: `doc/guides/emit-ticket-format.md`. The ticket text says "a future EMIT kind in
     #258"; a comment on #258 records the need.
   - CI coverage is a synthetic fixture with golden files. Roundtable (621) is run by hand and the
     result is recorded under Implementation notes.

6. **Stale goes into the export.** Each annotation gets an export-only `staleOn` attribute: the
   IDREFS of the entities it is stale on, omitted when none. Import ignores it (staleness stays
   derived). `project2html.xslt` marks stale issues too.

## Contracts

### Report generator

- `ReportGenerator` gains `String getBuiltinKey()` (nullable). `ReportGeneratorImpl`:
  `@Column(name = "builtin_key", length = 64)`, exported as attribute `builtin`.
- `ReportGeneratorDto` gains `builtinKey`. The Angular editor shows "Bundled — editing detaches
  it" when set.
- `BuiltinReportGenerators` (project-jpa): registry of `key -> (name, resource path)`:
  - `project-html` -> "HTML Specification", `xslt/project2html.xslt`
  - `ticket-markdown` -> "Ticket (Markdown)", `xslt/project2ticket-md.xslt`
- `EditReportGeneratorCommandImpl`: a text change on a keyed generator clears the key.
- `EditProjectCommandImpl.addBuiltinReportGenerator` creates both, with key and the current text
  (text kept so export/import stay portable).
- Import: `builtin` attribute honoured when the key is known, otherwise dropped with a WARN.

### Export (stale)

- `ExportProjectCommandImpl` gets `AnnotationFreshness` (optional setter, `NONE` when the
  assistant module is absent, same pattern as `ProvenanceStore`), calls
  `staleAnnotations(project.getProjectEntities())` once, and sets a transient `staleOn` list on
  each exported annotation carrier.
- `AbstractAnnotation`: `@Transient @XmlAttribute(name = "staleOn") @XmlIDREF List<Object>`,
  export-only (no import mapping; the StAX importer skips the attribute).
- Sorted by id so the attribute is byte-stable.

### Generate command

- `GenerateReportCommand` gains `setParameter(String name, String value)` and
  `getMediaType()` / `getFileExtension()` (read from the transformer's output properties:
  `media-type`, else by `method` — `html`/`xml` -> `text/html` + `.html`, `text` -> `text/plain` +
  `.txt`; `text/markdown` -> `.md`).
- Text source: bundled resource when `builtinKey` is set, else `getText()`; decoded as UTF-8.
- Transform into a buffer. On failure throw `ReportGenerationException` (service-api or
  project-domain) carrying the `xsl:message` text or the transformer's message. No output is
  written on failure.
- Temp export file deleted in `finally`.
- Parameters passed to the XSLT: `projectVersion`.

### REST

- `runReport`: compute `projectVersion` (fingerprint), run the command, then set Content-Type and
  `Content-Disposition` filename from the command's media type/extension, then write the buffer.
- `ApiExceptionHandler`: `ReportGenerationException` -> 422 `REPORT_FAILED` with the message.
- `report.service.ts` `downloadReport`: filename from `Content-Disposition`; on non-OK, read the
  error body and throw its message so the UI shows it.

### Fingerprint

- `ProjectFingerprint` (service-impl): uses `ProjectContentQueryService` with the character cap
  bypassed (internal overload), serializes with the REST `ObjectMapper` to a tree, removes every
  `version` property (every content DTO and `ProjectDto` carry the JPA `@Version`, which moves on
  edit-and-revert) and `project.reportGeneratorCount` / `project.dictionaryWordCount` (adding the
  bundled generator or a dictionary word is not a content change), appends sorted source records,
  SHA-256 of the compact JSON, first 12 hex chars.

### Ticket generator (`xslt/project2ticket-md.xslt`)

Sections, in order, each omitted when empty:

1. `# <project name>` then `Project version: <projectVersion>` and `Sources: <titles or ids>`.
2. `## Goals` — numbered, `**name**` + text.
3. `## Actors` — `name — text`.
4. `## Use Cases` — name, primary actor, goals, then each scenario with numbered steps.
5. `## Glossary` — `**term**` — definition; canonical term noted.
6. `## Open Issues` — per decisions 4 and 6.
7. `## Resources` — each source: title/id, kind, locator, defers to, derived entities (same data
   as `project2html`).
8. `## Traceability` — counts per type (goals, actors, use cases, scenarios, steps, stories,
   glossary terms, open issues, advisory issues), source list.

Rules: every list `xsl:sort` by numeric id (`substring-after(@id, '_')`), text escaped for
Markdown where it would break structure (leading `#`, `|`), no dates, no `generate-id()`.
Every IDREF is resolved through `key()` and `require`.

## Migration — V28

- `V28__report_builtin_key.sql`: add `reports.builtin_key VARCHAR(64) NULL`.
- `BuiltinReportGeneratorUpgrader` (startup, after Flyway, idempotent):
  - rows named "HTML Specification" with no key whose text, line endings normalized to LF,
    SHA-256-matches a known bundled version get `builtin_key='project-html'`. Known versions are
    the blobs of `project2html.xslt` at `d13c1325`, `9567c893`, `d7eb0b73`, `ed779345`,
    `e5d06ad2`, listed as hashes in `xslt/builtin-history.txt`;
  - every project without a `ticket-markdown` generator gets one. A name clash with a user
    generator uses "Ticket (Markdown) (bundled)".
- Edited copies are left alone and reported in one INFO line with the count.

## Step by step

1. Commit this plan on the branch.
2. `GenerateReportCommandImpl`: buffer, UTF-8, temp-file cleanup, exception, media type, params.
3. `ReportGenerationException` + `ApiExceptionHandler` mapping; `runReport` rework.
4. `builtin_key`: V28, entity, DTO, edit-detach, export/import attribute.
5. `BuiltinReportGenerators` + upgrader + `builtin-history.txt`; project creation adds both.
6. `ProjectFingerprint` + cap-free overload in `ProjectContentQueryService`.
7. `project2ticket-md.xslt` with `require`; `project2html.xslt` swaps `@revision` for
   `$projectVersion` and sorts its lists by id.
8. Angular: filename from header, error message, bundled label.
9. `doc/guides/emit-ticket-format.md`.
10. Tests (below), then a manual run on roundtable (621) with the output diffed against v2.

## Test plan

- **Golden files** (`requel-app`, new `ReportGenerationIT`): synthetic fixture project imported from
  `src/test/resources/xml/emit-fixture.xml` (goals, actors, a use case with two scenarios sharing
  a step, glossary with a canonical term, three issues of different severity, one advisory issue,
  two sources with a defers-to). Ticket and HTML output compared byte-for-byte to
  `src/test/resources/emit/*.golden`.
- **Byte-identical rerun**: run the same project twice -> equal bytes; touch nothing but
  `dateCreated`-bearing fields (re-save a goal with the same text) -> equal bytes.
- **Version**: unchanged project -> same version; edit a goal -> new version; revert the edit ->
  original version; resolve an issue -> new version.
- **Missing reference**: a generator with `require` on a nonexistent goal name -> 422
  `REPORT_FAILED` naming it; a fixture with a dangling step ref -> 422 naming the ref.
- **Stale**: edit the text of an entity with an assistant finding -> the issue sorts after fresh
  issues of the same severity and is marked stale; an issue shared by two entities is marked only
  on the edited one; export round-trip (export, import, export) keeps no stale state on import and
  re-derives it; a human-written issue is never stale.
- **No partial output**: a failing generator writes no body bytes.
- **Content type**: ticket -> `text/markdown`, `.md`; HTML -> `text/html`, `.html`.
- **Bundled**: upgrader adopts an unedited historic copy (CRLF and LF), leaves an edited copy,
  adds the ticket generator once (idempotent on second run), handles the name clash; editing a
  keyed generator clears the key; export/import round-trips the key.
- **Temp file**: export temp file removed after success and failure.
- **Access**: 403 non-member, 404 unknown project / report (existing behaviour kept).
- Angular: `report.service.spec.ts` filename from header, error message surfaced.

## Out of scope

- Writing to Jira or any external system.
- Narrative sections (#258).
- A template authoring UI.
- MCP/CLI `runReport`.
- Per-goal source attribution beyond the Resources section's derived-entity list.
- Replacing XSLT. Browsers are dropping client-side XSLT (Chrome removes it on 2026-11-17), but
  Requel runs it server-side through the JDK's `javax.xml.transform`, which is unaffected. The
  spec doc keeps the section contract engine-neutral, so a later engine swap is a template change.

## Risks

- **Fingerprint cost** on large projects: the content read builds every DTO. Acceptable for an
  on-demand report; measured on roundtable.
- **Historic hashes**: a copy that differs only by whitespace from a bundled version is treated as
  edited and left alone. The INFO line makes that visible.
- **`getAllScenariosAndSteps` gaps** (#274 finding 7): a sub-scenario step missing from the export
  now fails the ticket generator loudly instead of emitting a gap. That is the intended behaviour,
  but roundtable may hit it; fix the export if so.
- **Markdown escaping**: entity text containing Markdown is passed through except where it breaks
  structure. Golden files pin the behaviour.

## AC mapping

| AC | Covered by |
| --- | --- |
| Roundtable produces a document without hand-editing | Manual run on 621 (Implementation notes); fixture IT |
| Unchanged project -> byte-identical output | Rerun test; id sort; no dates |
| Output names the project version and its sources | Header + Resources; version tests |
| Missing entity -> clear failure, not a gap | `require`; 422 `REPORT_FAILED` tests |
| Structural sections of the case-study ticket reproduced | Every goal, actor, use case, scenario (with ordered steps) and glossary term appears in its section, counts match the project summary; roundtable output diffed against v2 and differences listed in Implementation notes |
| Resources section with precedence ("defers to") | Resources section; golden files |
| Open issues ranked by severity (new) | Open Issues section; golden files |
| Stale issues marked, sorted after fresh (new) | `staleOn` in the export; stale tests |
| Existing projects get the current bundle (new) | `builtin_key` + upgrader tests |

## Revised issue body (for `gh issue edit`)

Written to `tmp/issue-275-body.md`. The #258 comment is in `tmp/issue-258-comment.md`.

## Note for #258

Comment: #275 names "a future EMIT kind" for the narrative half of an emitted ticket (problem
statement, blocking check, intentional behaviour, open questions). #258 defines REVIEW / POLICY /
CORPUS today; an EMIT kind would take the structural output of the ticket generator plus the open
issues and write those sections. No change to #258's scope now.

## Implementation notes

(filled in during the build)

# Emitted ticket format

How Requel renders a project as a ticket, and which parts of that ticket a template produces versus
which parts need judgment. Introduced by
[#275](https://github.com/rreganjr/Requel/issues/275) (epic
[#267](https://github.com/rreganjr/Requel/issues/267)). The format comes from the two
hand-written emissions in `doc/work/2.0/roundtable/`.

## Summary

A ticket has two halves:

- **Structural.** A deterministic projection of the model. The bundled **Ticket (Markdown)**
  generator (`xslt/project2ticket-md.xslt`) produces it, and an unchanged project renders
  byte-identically.
- **Narrative.** Prose that needs judgment: what the problem is, and which finding outranks the
  rest. No template produces it. It is the job of a future EMIT kind of assistant definition in
  the assistants epic ([#258](https://github.com/rreganjr/Requel/issues/258)). That epic defines
  `REVIEW`, `POLICY` and `CORPUS` today, so the kind does not exist yet.

Diffing one emission against the last is how a living document shows what changed. That only
works if the structural half is byte-stable, which is why the template sorts every list and
prints no dates.

## Sections

| Section | Half | Source in the model |
| --- | --- | --- |
| Title (`# <project name>`) | structural | project name |
| Project version | structural | content fingerprint (below) |
| Sources line | structural | the project's sources (#272/#273); `none recorded` when there are none |
| Problem statement | **narrative** | — |
| Blocking check | **narrative** | picked from the open issues by a person or assistant |
| Goals | structural | every goal: name and text, numbered in creation order |
| Actors | structural | every actor: name and text |
| Use Cases | structural | each use case: text, primary actor, actors, goals, its primary scenario and each additional scenario with ordered steps; a sub-scenario is nested under its step, and a scenario with no steps says so |
| Other Scenarios | structural | scenarios no use case or scenario uses, so every scenario appears somewhere |
| Glossary | structural | every term: definition, and the canonical term an alternate stands for |
| Intentional behaviour ("do not fix") | **narrative** | — |
| Open Issues | structural | unresolved issues with `mustBeResolved`: severity (HIGH, MEDIUM, LOW), then fresh before stale, then id; each names the entities it is on, and marks a stale one |
| Open questions (prose) | **narrative** | — |
| Resources | structural | each source: title, kind, locator, note, the sources it defers to, and the entities derived from or citing it |
| Traceability | structural | counts per type (goals, actors, use cases, scenarios, steps, stories, glossary terms, open issues and how many are stale, advisory issues) and the source list |

Advisory issues (`mustBeResolved = false`, which includes the legacy lexical findings after
[#268](https://github.com/rreganjr/Requel/issues/268)) are counted in Traceability and not
listed. Stories are counted but not rendered: neither hand-written emission rendered them.

## Project version

The version is a fingerprint of what a generator renders. It is the first 12 hex characters of the
SHA-256 of:

- the [#274](https://github.com/rreganjr/Requel/issues/274) content read with open annotations
  (stale flags included), and
- the project's sources: every field the Resources section shows.

The fingerprint leaves out every JPA `version`, the read's character count and cap, and the
summary's report-generator and dictionary-word counts, because those change when the content has
not. An edit that is later reverted gives the original version back. The project row's own
`@revision` is an optimistic-lock counter that does not move when an entity is edited, so no
generator shows it.

The run path passes the version to every generator as the `projectVersion` stylesheet
parameter.

## Writing a generator

A generator is an XSLT 1.0 stylesheet over the project XML export (`doc/samples/project.xsd`). It
runs on the JDK's XSLTC with secure processing on, so it cannot import other stylesheets or load
external documents.

- **Output type.** `xsl:output` decides the download. An explicit `media-type` wins
  (`text/markdown` gives `.md`). Without one, `method="text"` is `text/plain` (`.txt`), and
  `xml` or `html` is `text/html` (`.html`).
- **Stable output.** Sort every list, by `substring-after(@id, '_')` as a number for creation
  order. The export's own order is not stable, because some of its collections are hash sets.
  Print no dates and do not use `generate-id()`.
- **Fail rather than leave a gap.** A lookup that finds nothing emits nothing in XSLT, silently.
  Check every reference, and every entity you select by name, and stop with
  `<xsl:message terminate="yes">` when it is missing:

  ```xml
  <xsl:if test="count(rp:goals/rp:goal[rp:name = 'Blocking check']) = 0">
    <xsl:message terminate="yes">The report references goal named Blocking check, which is not in the project.</xsl:message>
  </xsl:if>
  ```

  The run returns 422 `REPORT_FAILED`, and its message is the message text. The bundled ticket
  generator does this through its `require` template.
- **XSLTC quirk.** A `for-each` with an `xsl:sort` over a node-set passed in by `xsl:with-param`
  produces nothing. Sort at the call site with `apply-templates` instead, as the ticket
  generator's `ref-list` mode does.
- **Stale.** An exported annotation carries `staleOn`: the ids of the entities it is stale on
  ([#270](https://github.com/rreganjr/Requel/issues/270)). It is export-only, and import ignores
  it.

## Bundled generators

Every project has one of each bundled generator, linked by its `builtin_key`:

| Key | Name | Template |
| --- | --- | --- |
| `project-html` | HTML Specification | `xslt/project2html.xslt` |
| `ticket-markdown` | Ticket (Markdown) | `xslt/project2ticket-md.xslt` |

- A keyed generator renders the current bundled template, not its stored copy, so an updated
  bundle reaches every project.
- Saving new template text detaches the generator (the key is cleared), and from then on its own
  text renders. Renaming it, or saving the same text, keeps the link.
- At startup, `BuiltinReportGeneratorUpgrader` links unedited copies to the bundle and adds any
  missing bundled generator. A copy counts as unedited when its text, with line endings
  normalized, matches a version listed in `xslt/builtin-history.txt` or the current bundle. The
  upgrader leaves edited copies alone.
- An export carries the key as the `builtin` attribute. An import keeps a key it knows and adds
  any bundled generator the project lacks.

When a bundled template changes, add the previous version's hash to `xslt/builtin-history.txt`.
Otherwise an unedited copy saved before the change will not be recognized by the upgrader.

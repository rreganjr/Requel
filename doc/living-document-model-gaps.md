# [Epic] Requel as a living document — annotation hygiene, provenance, and emit

Relates to #43. Where `doc/ai-per-type-assistants-epic.md` makes the *analysis* better, this epic
fixes the things that stop analysis from being useful once it exists: annotation noise that buries
findings, no way to record where a requirement came from, and no way to get the model back out.

Every item here was found by building a real project through the gateway and reading it back —
`doc/pq-roundtable-case-study.md` is the record. Nothing is speculative.

## The measurement that motivates this

`getProjectContext` on the roundtable project returns an `openIssues` list. Eight of its entries are
the substantive findings written during the case study — cross-ticket conflicts, a goal that may be
unmet in production, a vacuous quantifier. The rest, the large majority, are legacy lexical output
of this shape:

- `The word "MP4" in the Text is not recognized and may be spelled incorrectly.`
- `The word "webinar" ... may be spelled incorrectly.` (also RTMP, livestream, co-hosts)
- `The word "be" in the Text is vague and may lead to ambiguity.` (also "creates", "person",
  "action", "event", "covering")
- `The phrase "the room" is a potential glossary term, actor, or domain object/property.` (also
  "a person", "the gate", "Conduit", "a session", "an event" — nearly every noun phrase)

Every one carries `mustBeResolved: true`. **A reviewer opening this project cannot see the finding
that says the admin console may be open to the whole tenant, because it is one row among dozens of
reports that "be" is vague.** That is the problem this epic exists to fix, and it is not a
hypothetical — it is the state of the only real project in the system.

## Scope (child issues)

1. **Legacy lexical assistant precision** — the noise above.
2. **The phrase extractor truncates mid-word.**
3. **Annotations are never invalidated when their entity's text changes.**
4. **Issue severity is accepted, then silently dropped.**
5. **Entity provenance** — spec in `doc/entity-provenance-notes.md`.
6. **Reference attachments and document authority.**
7. **A project content read** — emit cannot get the model out.
8. **Emit through `ReportGenerator`.**

`doc/goal-relation-types-proposal.md` is deliberately *not* a child here — it is a small,
self-contained change that should be filed on its own and done first, because it unblocks structural
repair of findings the per-type assistants will start producing.

Suggested order: 4 and 2 are small and independent. 3 and 1 change what a reviewer sees and should
come next. 5, 6, 7, 8 are the emit chain and want doing together.

---

# Child 1 — Legacy lexical assistant precision

## What exists today

`assistant-legacy-nlp` provides `LexicalSpellingAssistant`, `LexicalVagueWordAssistant`,
`LexicalComplexityAssistant` and `LexicalGlossaryTermAssistant`, wired through the per-type
assistants in `project-jpa/.../impl/assistant/`. They serve the default post-edit task, so every
edit to any `TextEntity` produces annotations.

They are not wrong in principle — vague wording and undefined terms are real requirements problems,
which is why the taxonomy in the AI epic includes them. The failure is precision.

## Observed failure modes

- **Domain vocabulary flagged as misspelling.** MP4, webinar, livestream, RTMP, co-hosts. Any
  technical project will produce these by the dozen, and no dictionary will ever contain them all.
- **Function words flagged as vague.** "be", "creates", "person", "action", "event", "covering". A
  vague-word list that contains "be" will fire on essentially every sentence in the corpus.
- **Glossary candidates on almost every noun phrase.** "the room", "a person", "the gate", "a
  session", "Conduit", "an event". The signal — that "Room" genuinely needed defining — is real and
  is buried in its own false positives.
- **`mustBeResolved: true` on all of it**, so none of it can be dismissed as advisory.

## Work

- Suppress spelling findings for terms already in the project glossary, for any term appearing in
  another entity's name, and for tokens matching an acronym shape. A project glossary is the
  project's own dictionary and should be consulted before the system one.
- Cut the vague-word list back to words that are vague *as requirements language* (should, may,
  fast, easily, appropriate, sufficient, robust) and drop copulas and generic verbs.
- Rate-limit glossary candidates: propose a term once per project, not once per occurrence per
  entity, and only above a frequency or distinctiveness threshold.
- Default lexical findings to advisory (`mustBeResolved: false`); reserve must-resolve for findings
  an author has to answer.
- Make each lexical assistant individually switchable per project, so a project drowning in one can
  turn it off without losing the others.

## Acceptance criteria

- Re-running analysis over the roundtable project produces no spelling finding for a term in that
  project's glossary, and none for MP4, RTMP, webinar, livestream or co-hosts.
- No vague-word finding is raised for "be", "creates", "person", "action", "event" or "covering".
- A repeated noun phrase produces at most one glossary-candidate finding per project.
- Lexical findings default to advisory; a test pins the default.
- Each lexical assistant can be disabled per project, and disabling one leaves the others running.
- A before/after count on the roundtable project is recorded on the ticket.

## Not in scope

- Removing the legacy assistants, or porting them to the AI definition model.
- The AI review definitions (separate epic).

---

# Child 2 — The phrase extractor truncates mid-word

## What exists today

Glossary-candidate findings quote a phrase from the entity text. On the roundtable project the
quoted phrases include:

- `"Sees the Zoom livestream indicator and the recording notic"` — cut mid-word
- `"ermissions matrix"` — starts mid-word
- `"a Zoom webinar p"` — cut mid-word
- `"le, not a person — CON-3685 names Chris Peterson where it means this role, ..."` — starts
  mid-word, in a complexity finding

The cuts fall at arbitrary offsets rather than token boundaries, and at least one of them
(`"ermissions"`) then produces a *second* finding reporting that the truncated fragment is
misspelled — a false positive generated entirely by the extractor's own bug.

## Work

- Find where the extractor slices (OpenNLP span offsets applied to a different string than the one
  they were computed against is the likely cause — non-ASCII characters in the text, such as the em
  dashes and en dashes present in every one of the observed cases, are the first thing to check).
- Snap every quoted span to token boundaries before it is used in finding text.
- Suppress findings whose evidence fails a well-formedness check, rather than emitting a finding
  about a fragment.

## Acceptance criteria

- No finding on the roundtable project quotes a fragment that begins or ends mid-word.
- A regression test covers text containing em dashes, en dashes and non-ASCII punctuation.
- A fragment that fails the boundary check produces no finding rather than a malformed one.
- The `"ermissions"` cascade — a spelling finding derived from a truncation — cannot recur.

## Not in scope

- Changing which phrases are considered candidates (child 1).

---

# Child 3 — Annotations are never invalidated when their entity's text changes

## What exists today

A scenario in the roundtable project was created under the throwaway name "Zzz isolation probe no
steps", then renamed to "Manual verification pass on a test room" and given real text. The project
still carries an open, must-resolve issue reading:

> `The word "Zzz" in the Name is not recognized and may be spelled incorrectly.`

The word is gone. The finding is not. Nothing re-evaluates or retires an annotation when the text it
was derived from changes.

For a living document this is the most corrosive defect in the epic: **the longer a project is
maintained, the larger the fraction of its findings refer to text that no longer exists**, and a
reviewer has no way to tell a stale finding from a live one.

Note the interaction with the AI work: a stale finding is also fed back into the context pack, where
the prompt tells the model not to restate existing annotations. The model is therefore being told to
avoid duplicating findings about text that is gone.

## Work

- Record, on each machine-generated annotation, the entity version it was derived from.
- On edit, mark annotations from an earlier version as stale — and decide, explicitly and on the
  ticket, between retiring them automatically and surfacing them as "may no longer apply". Automatic
  retirement of a finding a human has replied to is not acceptable; retirement of an untouched
  lexical finding probably is.
- Exclude stale annotations from context packs.
- Never silently delete an annotation carrying human discussion (positions, arguments).

## Acceptance criteria

- Editing an entity's text marks its untouched machine-generated annotations stale within the same
  transaction.
- A stale annotation is visibly distinguished in the API response and in the UI.
- An annotation with positions or arguments is never auto-retired.
- Stale annotations do not appear in context packs.
- The "Zzz" finding on the roundtable project is gone after a re-analysis.

## Not in scope

- Re-running analysis automatically on every edit.

---

# Child 4 — Issue severity is accepted, then silently dropped

## What exists today

`McpReadService.draftAnnotation` reads a `severity` argument, declares it in the tool schema, and
passes it into `AnnotationAction`. The annotation SPI carries it end to end.

`Issue` (`annotation-domain/.../Issue.java`) has `positions`, `resolvedByPosition`,
`resolvedByUser`, `resolvedDate` — **and no severity field**. `EditIssue` on the gateway exposes
`text` and `mustBeResolved` and nothing else.

So a caller can supply a severity, the draft carries it, and persisting the annotation discards it
with no error. Everything downstream then has exactly one bit of priority information —
`mustBeResolved` — which on the roundtable project is `true` for a spelling false positive and
`true` for a possible production authorization hole, indistinguishable.

## Work

- Add severity to the `Issue` domain entity and its persistence, with a small closed vocabulary.
- Expose it on `EditIssue` and in the issue DTOs, and thread the value that `draftAnnotation`
  already accepts through to the persisted annotation.
- Order issues by severity wherever they are listed — `getProjectContext.openIssues`, the UI, and
  any future emit.
- Backfill existing issues to a default rather than null.

## Acceptance criteria

- A severity supplied to `draftAnnotation` survives to the persisted issue and comes back on read.
- `getProjectContext` returns open issues in severity order.
- An unrecognised severity is a field-level validation error, not a silent drop.
- Existing issues carry a defaulted severity after migration and no read path returns null for it.

## Not in scope

- Automatic severity assignment by the assistants.

---

# Child 5 — Entity provenance

Full analysis in `doc/entity-provenance-notes.md`: why tags were used for the roundtable ingest,
the four limits measured rather than guessed (case normalisation, identifier-not-locator, no
AC-level granularity, no temporal record, user-removable), why a plain `Note` is the wrong carrier
(it lands in every context pack and puts an external URL in front of a model), and the
recommendation.

## Work

- Decide the authoritative direction first: Requel as source of truth with external systems as emit
  targets, or reconciliation between two authorities. Everything else depends on it.
- A `SourceRef` on an entity: system, external id, fragment (the acceptance criterion, the page),
  URL, ingested-at, source content hash, ingest run.
- Exclude it from context packs by construction.
- Re-ingest resolves against it: same source and fragment updates the entity rather than creating a
  second one.
- XML export/import carries it.

## Acceptance criteria

- Re-ingesting the same source updates the entities it produced and creates no duplicates.
- An entity can report which source and which fragment it came from; a source can report which
  entities it produced.
- A changed source is distinguishable from an unchanged one without re-reading the entities.
- Provenance never appears in a context pack, and no provenance URL is ever passed to a model.
- Export and re-import preserves provenance.

## Not in scope

- Bidirectional sync.
- An ingest UI.

---

# Child 6 — Reference attachments and document authority

## What exists today

Nothing holds "this document exists and is relevant". The roundtable tickets cite ENTRA_SETUP.md, a
Zoom permissions matrix, the round-1 review and RUNBOOK.md; the model dropped all four, because none
is a goal, actor, use case or glossary term. The first emitted ticket lost every one of them.

Two of the three sources also state their own authority unprompted — the production guide says
RUNBOOK.md wins over it, and the review says the source wins over the review. **A living document
that cannot record which document supersedes which cannot reconcile them**, and reconciliation is
the whole proposition.

## Work

- A reference attachable to a project or any entity: title, URL or path, kind, note. Structurally
  the same record as child 5's provenance, differing in relation rather than shape — build one
  mechanism.
- An authority ordering between references, so "A is superseded by B" is a fact the model holds.
- Surface references in emit as a resources section, restoring what the first emission dropped.

## Acceptance criteria

- A reference can be attached to a project and to an entity, and read back on both.
- One reference can be recorded as superseded by another, and a reader can resolve which wins.
- References appear in an emitted document.
- References never enter context packs as instruction-bearing text.

## Not in scope

- Fetching, storing or indexing the referenced documents.
- Rendering document content.

---

# Child 7 — A project content read

## What exists today

`getProjectContext` returns the project summary, the tree, the glossary **in full**, and open issues
**in full** — but the tree carries only ids, names and types. Goal text, story text, actor
descriptions, use case text, scenarios and steps are not in it. `getProjectTree` is names only.

So a generator must either walk the tree and issue one `getEntity` per entity, or go around the
gateway to the XML export. Both emissions in the case study were written by hand from session
memory, which is precisely the thing that cannot be automated.

## Work

- A read that returns full project content in one call — every entity with its text, scenarios with
  their steps, relations, and annotations — with an explicit size cap and a documented behaviour
  when a project exceeds it.
- Decide whether this extends `getProjectContext` or is a new tool; `getProjectContext` is described
  as a *context bundle* for an AI caller, and a full content read is a different thing with
  different size characteristics.

## Acceptance criteria

- One call returns enough to reconstruct the project's normative content without further reads.
- Scenarios come back with their steps, and shared steps are identifiable as the same step.
- A project over the cap fails with a clear message naming the overflow rather than silently
  truncating.
- The read is covered by a test asserting entity counts match the project summary.

## Not in scope

- Streaming or pagination for very large projects.
- Changing `getProjectTree`.

---

# Child 8 — Emit through ReportGenerator

## What exists today

`ReportGenerator` is a `TextEntity` in `project-domain` with `EditReportGenerator` and
`DeleteReportGenerator` on the gateway, and every project auto-carries one — the roundtable project
has "HTML Specification" (id 696). Nothing renders it. The historical design was XSLT over the
project XML export.

Both emitted tickets in the case study were hand-written. The structural sections of them — actors,
use cases and scenarios, glossary, traceability — are mechanical projections that a template would
produce byte-identically every run, which matters because diffing one emission against the last is
how a living document shows what changed.

## Work

- Wire `ReportGenerator` to actually render a project through a template, using child 7's content
  read as input.
- Ship one working generator end to end rather than a framework — the ticket format from the case
  study is the obvious candidate.
- Define where the format specification lives, given that the structural half is deterministic and
  the narrative half is not. The narrative sections are an AI definition of kind EMIT in the
  assistants epic; the structural sections are the template. One document should say which is which.
- Emitted output records which project version and which sources it came from (child 5).

## Acceptance criteria

- Running a report generator over the roundtable project produces a document without hand-editing.
- Re-running against an unchanged project produces byte-identical output.
- The output names the project version and its sources.
- A generator referencing a missing entity fails with a clear message rather than emitting a gap.
- The structural sections of the case study's emitted ticket are reproduced by the generator.

## Not in scope

- Writing to Jira or any external system.
- The narrative sections (assistants epic).
- A template authoring UI.

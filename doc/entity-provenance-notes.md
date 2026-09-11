# Entity provenance — notes and options

Where an entity came from. Written while building the **PlatformQ Roundtable** project by ingesting
two Jira tickets through the MCP gateway (see `doc/pq-roundtable-case-study.md`), which is where the
empirical bits below come from.

## Why it matters

Requel becomes much more useful if a ticket, a spec or a transcript can be read in, analysed,
annotated, and emitted back out in corrected form — with the Requel project as the live document that
stays consistent while the external artifacts drift. Every part of that loop needs one thing Requel
does not have: **an entity knowing which external artifact it came from.**

Concretely, provenance is what makes these possible:

- **Re-ingest instead of duplicate.** Reading CON-3685 a second time must update the eight goals it
  produced, not create eight more.
- **Change detection.** Telling "the source is unchanged" from "the source was edited" requires a
  record of the source's state at ingest, not just its identity.
- **Traceability both ways.** "Which requirements came from this ticket" and "where did this
  requirement come from" are both routine questions.
- **Emit.** Regenerating a corrected ticket requires knowing which entities belong in it.

Before any of that: **pick an authoritative side.** Requel as source of truth with Jira as an emit
target is coherent. True bidirectional sync is where this class of project dies. That decision comes
first, because it determines whether provenance is a pointer (Requel authoritative) or a
reconciliation key (both authoritative).

## Option A — tags

Requel has `TagCategory` / `Tag` / `AssignTag`, and a `source` category with one tag per external
artifact is the cheapest thing that could work. It was used for the roundtable build.

**What works.** Zero code. Queryable today — "everything from CON-3685" is a tag filter.
Assign/unassign already exist, tags are project-scoped, and they carry a color so the UI can
distinguish them.

**What was observed to fail.**

- **Case normalisation.** `EditTagCategory` with name `Source` and values `CON-3685` / `CON-3686`
  came back as `source`, `con-3685`, `con-3686`. A tag cannot round-trip the canonical form of an
  external identifier, which matters the moment the identifier is used to construct a URL or compared
  against the source system.
- **Identifier, not locator.** A tag is category + value + color. There is no field for the Jira
  URL, so the link that makes provenance useful has nowhere to live.
- **No granularity.** The tag records that a goal came from CON-3685. It cannot record that it came
  from *acceptance criterion 4*. During the build this was the first thing missed — eight goals from
  one ticket, indistinguishable by tag.
- **No temporal record.** No ingest timestamp, no source version or content hash, so change
  detection is impossible.
- **User-removable.** If provenance is load-bearing for re-ingest idempotency, a label any user can
  detach is the wrong substrate; removing it silently turns the next re-ingest into a duplication.

**Verdict:** right for the experiment, wrong as the destination. Keep using it, and treat the four
failures above as the specification for whatever replaces it.

## Option B — an annotation

Annotations attach to any entity through the `@Any` / `@ManyToAny` discriminator pattern, carry text,
an author and a timestamp, and already render in the UI. A `Note` would work today; a dedicated
subtype presented differently is the natural evolution.

**Against using a plain `Note`, beyond it being generic:**

- **It pollutes the AI context.** `EntityContextPackBuilder` puts existing annotations into the
  context pack, and the review prompt explicitly tells the model *"the context may include an
  annotations list of EXISTING issues/notes… Do NOT restate, echo, paraphrase, or duplicate them."*
  A provenance note is noise in every pack for every entity, on every run, forever.
- **It is an injection surface.** A URL in annotation text reaches the model, and would reach
  anything that later follows links.
- **It is discussion furniture.** Notes, issues, positions and arguments are the IBIS layer — human
  deliberation. Machine-written bookkeeping does not belong in the same stream as a reviewer's
  argument.

A dedicated `SourceReference` annotation subtype fixes the presentation problem and can carry
structured fields (URL, external id, fragment, ingested-at, source hash). It costs a JPA subtype,
commands, DTOs and Angular rendering — the pattern exists, so this is work but not research. It would
still need to be excluded from context packs explicitly.

## Option C — a first-class field or entity

A `SourceRef` owned by the entity (or a side table keyed by entity type + id), holding: system
(`jira`), external id (`CON-3685`), fragment (`AC#4`), URL, ingested-at, source content hash, and
the ingest run that created it.

**For:** it is not user-removable, it is queryable and indexable, it can be excluded from context
packs by construction, and it is the only option that supports change detection. If re-ingest
idempotency is a real requirement, this is what it needs.

**Against:** it touches the domain model and every entity type, it needs XML export/import support,
and it is a lot of machinery to build before the loop has been proven.

## Recommendation

1. **Now:** keep tags. They cost nothing and they are already collecting evidence.
2. **Next:** decide the authoritative direction. Everything downstream depends on it.
3. **Then:** implement Option C for the entity types the ingest loop actually produces, and keep the
   `SourceReference` annotation subtype (Option B) only if there turns out to be a real need to
   *display* provenance as part of the discussion rather than as metadata.
4. **Whichever wins:** exclude it from context packs, and never let an external URL from provenance
   reach a model or a fetcher.

## What to watch for during the roundtable build

The two predictions worth checking against reality, since the project is live:

- AC-level granularity will be missed before ticket-level identity is.
- The first re-ingest will be the moment tags become untenable, because nothing can answer "has
  CON-3685 changed since I last read it?"

Record what actually happens in `doc/pq-roundtable-case-study.md`.

# #304 — Reorganize `doc/` by lifecycle

https://github.com/rreganjr/Requel/issues/304

## Summary

`doc/` holds 149 markdown files in one flat namespace plus the 2009 thesis-era originals.
Three lifecycles are interleaved with no way to tell them apart from a filename: standing
reference that stays true, ticket-scoped artifacts that are historical the moment their PR
merges, and frozen thesis material. The ~19 documents a newcomer actually needs are buried
under ~110 that they don't.

This is a **move-and-relink change only**. No document's contents are rewritten, nothing is
deleted except two regenerable files, and the commit is reviewable as a pure rename set.

## Locked decisions

1. **Split by lifecycle, not by topic.** A topic split (auth / ui / mcp / nlp) puts a dead
   2026-04 plan next to the architecture doc that superseded it. Lifecycle keeps "still true"
   separable from "this is how we got here".
2. **Release-scoped work folder, with the release derived not hardcoded.** Ticket artifacts go to
   `doc/work/<release>/`, where `<release>` is the reactor version's `major.minor` read from the
   root `pom.xml` (`2.0.0-dev` → `2.0`), skipping the `<parent>` block so the Spring Boot version
   is not mistaken for it. Nothing in the layout, the scripts or the CLAUDE.md rule names "2.0" as
   a literal. At rollover the folder simply changes: `doc/work/2.0/` freezes exactly as it is and
   `doc/work/2.1/` starts empty. Old release folders are **not** swept into `archive/` — a
   `work/2.0/` sitting beside `work/2.1/` already says what it is, and moving it would break every
   link into it for no gain. `archive/` is only for material that predates this scheme.
3. **No YAML front matter.** Rendered through plain CommonMark a front-matter block comes out
   as a horizontal rule plus an `<h2>` of the keys (verified against markdown-it, which is what
   most previewers are built on). GitHub special-cases it into a table; other renderers do not.
   More importantly it would duplicate state GitHub already owns — the issue number is already
   in the filename and open/closed lives on the issue. Duplicated status goes stale on the next
   merge; a generated index cannot.
4. **`doc/README.md` is hand-written prose** with one generated block between
   `<!-- BEGIN INDEX -->` / `<!-- END INDEX -->` markers. Only `doc/work/<release>/INDEX.md` is
   generated end to end, and it is a pure listing with no prose to lose.
5. **Reviews are kept.** They are not session transcripts; several are the only record of why a
   decision went the way it did — `port-tospring-boot-ai-codex-review.md` documents the Spring AI
   1.x / Boot 3.4+ incompatibility against this repo's 3.3.4 that set the current baseline, and
   `43-opus48-review.md` captures plan-vs-code drift at a named commit. They file alongside the
   plan they reviewed.
6. **`doc/images/` does not move.** It is referenced by both current architecture docs and the
   archived thesis material; moving it would churn image links in both for no gain.
7. **This plan moves itself.** `doc/work/2.0/304-doc-reorg-plan.md` is a ticket-scoped artifact and lands
   at `doc/work/2.0/304-doc-reorg-plan.md` in the same commit as everything else.

## Target layout

```
doc/
  README.md                     the map: what each folder is for, when a new file belongs there
  architecture/                 how the system is built (8 files)
  guides/                       how to run and operate it (8 files)
  ui/                           Angular design system and front-end posture (4 files)
  work/
    2.0/                        every plan, review, rollup and issue draft for release 2.0 (115)
      INDEX.md                  generated: issue #, title, state, artifacts
      roundtable/               pq-roundtable emitted experiment output
    backlog/                    proposals and epics not yet scheduled to a release (7)
  archive/                      superseded records of completed modernization work (18)
    2009-thesis/                .doc/.pdf/.vsd originals, docbook/, qsd/
  images/                       unchanged — shared by architecture docs and the archive
  samples/                      unchanged — project.xsd, Requel.xml, schema2.xsd
  claude/                       unchanged — tooling, not documentation
```

`ls doc/*.md` returns exactly one file when this is done.

## Classification

The full mapping is produced by `tmp/304-classify.sh` (gitignored), which emits
`<old-path>\t<new-path>` for every entry in `doc/`. It is the single source of truth for the
`git mv` list — the mapping is never hand-typed. Rules, in precedence order:

| Bucket | Rule | Count |
|---|---|---|
| `architecture/` | Explicit list: how the system is built, still true | 8 |
| `guides/` | Explicit list: how to run/operate/connect to it | 8 |
| `ui/` | Explicit list: Angular design system, bundle and zoneless posture | 4 |
| `work/backlog/` | Explicit list: proposals and `[Epic]` docs with no issue yet | 7 |
| `archive/` | Explicit list: completed pre-2.0 migration records | 18 |
| `archive/2009-thesis/` | Thesis-era binaries, `docbook/`, `qsd/` | (in the 18) |
| `work/2.0/` | **Default** — everything not named above | 115 |

Making `work/2.0/` the default rather than an explicit list is deliberate: a file nobody
classified is a ticket artifact until someone says otherwise, and the failure mode (a standing
doc sitting in `work/2.0/`) is visible and cheap to fix. The inverse — a dead plan sitting in
`architecture/` — is the problem this ticket exists to solve.

### Non-markdown and special cases

| File | Disposition |
|---|---|
| `.DS_Store` | delete; add to `.gitignore` |
| `docker_scout_report.txt` | delete — regenerable scan output, no provenance value |
| `rebuild_semcor_from_wordnet30_at_work.sql` | move to `scripts/` — it is executable input, not documentation |
| `pq-roundtable-emitted-CON-3685*.md` | `work/<release>/roundtable/` — generated experiment output, kept with its case study |
| `pq-roundtable-case-study.md` | `work/<release>/` — a Requel case study, not Conduit work; stays in the repo |
| `Requel System Structure.doc` | `archive/2009-thesis/requel-system-structure.doc` — the only filename with spaces |

## Step by step

### 1. Create the destination folders

```bash
mkdir -p doc/architecture doc/guides doc/ui doc/work/2.0/roundtable doc/work/backlog doc/archive/2009-thesis
```

### 2. Move with `git mv`, driven by the classifier

```bash
bash tmp/304-classify.sh | grep -v '^\S*\s(' | while IFS=$'\t' read -r old new; do
  git mv "doc/$old" "doc/$new"
done
```

`git mv` keeps the blob identical, so GitHub records these as renames and `git log --follow`
traverses them. This must happen **before** the link rewrite, so the rewrite can verify each
target exists at its new path.

### 3. Rewrite cross-references

There are roughly 450 `doc/…​.md` references in the tree. The rewrite is driven by the same
mapping, so a link and its target can never disagree:

- Build `old-basename → new-path-relative-to-doc` from the classifier output.
- For every `*.md` under `doc/`, plus `CLAUDE.md`, `README.md` and anything under `scripts/`,
  rewrite two forms:
  - repo-rooted: `doc/<old>` → `doc/<new>`
  - doc-relative (links between docs, e.g. `[…](../../architecture/AUTH_ARCH.md)`) → the correct `../` prefix for
    the *linking* file's new depth.
- Leave `doc/images/…` and `doc/samples/…` references alone — those paths are unchanged.
- Fail loudly on any `doc/*.md` reference whose basename is not in the mapping, rather than
  leaving it silently broken.

The four hub documents carry most of the inbound links and are the ones to eyeball by hand
afterwards: `UI_UX_REVIEW.md` (102 inbound), `124-lookandfeel-plan.md` (51),
`20-release-plan.md` (45), `UI_REFACTOR_PLAN.md` (38).

### 4. Write `doc/README.md`

Hand-written. Covers, in prose: what Requel's documentation is, what each folder means, and the
one decision a contributor has to make — *is this still going to be true in six months, or is it
a record of one ticket?* Ends with the generated index block.

### 5. Add `scripts/gen-doc-index.sh`

Takes the release as its first argument, defaulting to the same pom-derived value the classifier
uses, so nothing hardcodes "2.0". Generates `doc/work/<release>/INDEX.md` and the block inside
`doc/README.md` (which lists every release folder present, newest first). For each file in
`doc/work/<release>/`:

- parse the leading issue number from the filename where there is one;
- resolve title and state in **one** `gh issue list --repo rreganjr/Requel --state all --limit N
  --json number,title,state` call, not one call per issue (the retro scripts learned this the
  hard way — see the GraphQL budget note in CLAUDE.md);
- emit a table: issue, title, state, artifacts (plan / review / rollup), and flag any file whose
  issue number does not resolve.

Files with no issue number are listed in an "unattributed" section — that list should shrink
over time and is a useful smell.

### 6. Update CLAUDE.md

Three edits, all in the process sections:

- **Process Instructions**, replace *"All plans, reviews, notes and documentation go in the doc
  folder"* with the routing rule:

  > Ticket-scoped artifacts — plans, reviews, rollups, issue drafts — go in
  > `doc/work/<release>/`, where `<release>` is the reactor version's major.minor from the root
  > `pom.xml` (`2.0.0-dev` → `doc/work/2.0/`). Standing documentation goes in
  > `doc/architecture/`, `doc/guides/` or `doc/ui/` according to what it is. Proposals not yet
  > tied to an issue go in `doc/work/backlog/`. Nothing new belongs at `doc/` root except
  > `README.md`.

- **Development Workflow step 2**, change the plan path from `doc/<n>-<slug>-plan.md` to
  `doc/work/<release>/<n>-<slug>-plan.md`, and the same in the step-2 line of the command
  reference block at the end of the section.

- **Key Documentation** list: repoint the seven entries to their new paths.

### 7. Move this plan

`doc/work/2.0/304-doc-reorg-plan.md` → `doc/work/2.0/304-doc-reorg-plan.md`, in the same commit. It is
produced by the classifier's default rule, so no special case is needed.

## Verification

This ticket touches no Java or TypeScript, so the usual suites are not the gate — a link check
is. Per-ticket verify script at `tmp/304-verify.sh`:

1. **Root is clean** — `ls doc/*.md` returns only `README.md`.
2. **No link regressed** — resolve every relative markdown link under `doc/**`, `CLAUDE.md` and
   `README.md` to an absolute path, before and after. The two sorted sets must be identical;
   any link that resolved before and does not now is a failure. This is stronger than "no broken
   links now", because it also catches a link that was rewritten to the wrong existing file.
3. **No orphan references** — `grep -rn 'doc/[A-Za-z0-9_-]*\.md'` across the tree returns zero
   hits for paths that no longer exist.
4. **History survives** — `git log --follow` returns more than one commit for one sampled file
   from each bucket (e.g. `doc/architecture/AUTH_ARCH.md`,
   `doc/work/2.0/124-lookandfeel-plan.md`, `doc/archive/UPGRADE_PLAN.md`).
5. **Index is accurate** — `scripts/gen-doc-index.sh` runs clean and reports no unresolved issue
   numbers beyond the known unattributed set.
6. **Build untouched** — `mvn -pl modules/requel-app -am package -DskipAngularBuild=true
   -DskipTests=true` still succeeds, confirming nothing under `doc/` was on a build path.

Steps 1–5 are cheap and run locally; step 6 is the sanity check that this really was
documentation-only.

## Out of scope

- Rewriting, condensing or merging any document's contents. Several are stale in substance
  (`RELEASE_20_TEST_PLAN.md` at 1217 lines, `UI_REFACTOR_PLAN.md` at 1916) but editing them here
  would bury the renames in a diff nobody can review.
- Deciding which 2.0 plans are obsolete. Everything tied to 2.0 moves to `work/2.0/` regardless
  of whether its issue is closed; the generated index surfaces state without anyone triaging 115
  files by hand.
- Splitting `work/2.0/` into per-issue subfolders. Worth revisiting for the multi-file families
  (`240`/`241`/`242`/`247`, `112-*`, `43-*`) once the index exists and shows whether the flat
  release folder is actually hard to navigate.
- Moving documentation out of the repo into GitHub issues or a wiki.

## Risks

| Risk | Mitigation |
|---|---|
| Link rewrite silently repoints a link to the wrong file | Verification step 2 compares resolved target *sets*, not just "does it 404" |
| A reviewer cannot read a 160-file rename diff | Pure `git mv` + mechanical rewrite, no content edits, so `git show --stat -M` reads as a rename list; review the four hub docs by hand |
| New plans keep landing at `doc/` root | CLAUDE.md step 6 is part of this PR, not a follow-up |
| `git mv` of `Requel System Structure.doc` (spaces) | Classifier quotes it and renames to `requel-system-structure.doc` |
| Someone's local branch has an in-flight plan at the old path | Land this between tickets, not mid-stack; conflicts are rename/modify and resolve to the new path |

## Findings worth acting on

Noted while inventorying; none block this ticket.

- **`post-124-ui-rollup.md` was clobbered, and it has broken a script since 2026-08-31.**
  Not whittled down — commit `3e2609ac` ("docs: fill I8 issue number (#234) in post-#124 UI
  rollup") was meant to change one line and instead deleted all 39, leaving a 0-byte file. The
  last good content is at `ab53fa95` (1671 bytes). Because
  `scripts/reorder-post-124-ui-subissues.sh` reads `- [ ] #NNN` lines out of this file as its
  single source of truth for build order, it has been exiting 1 with *"No '- [ ] #NNN' checklist
  lines found"* ever since. Recovery is mechanical:

  ```bash
  git show ab53fa95:doc/work/2.0/post-124-ui-rollup.md > doc/work/2.0/post-124-ui-rollup.md
  # then apply the edit 3e2609ac intended: "#TBD" -> "#234" on the Phase 6 line
  EPIC=219 bash scripts/reorder-post-124-ui-subissues.sh --sync-checks
  ```

  This is a **content fix on a broken tool, not a move**, so it does not belong in this PR. It
  wants its own issue, landed first, with "the script runs clean" as its acceptance criterion —
  otherwise this reorg moves a known-broken file and muddies the blame.
- **`dictionary_dependencies.md` is 2 lines** — a heading and one line. Fold into
  `DICTIONARY_LOADING.md` or drop it.
- **The `pq-roundtable-*` files are Requel material, not Conduit work.** The issue body's original
  "relocate strays" line was wrong; the issue has been corrected rather than left to be
  contradicted by this plan. `pq-roundtable-case-study.md`
  is a case study *of Requel* and the two `-emitted-` files are Requel's own generated output
  from the ingest → analyse → annotate → emit loop. They stay.
- **`43-opus48-review.md` opens with a `TL;DR` heading**, which CLAUDE.md forbids. One-word fix,
  but it is a content edit and belongs in a separate commit.

## Acceptance criteria mapping

| AC | Where satisfied |
|---|---|
| 1. `ls doc/*.md` returns only `README.md` | Steps 1–2, 7; verification 1 |
| 2. No broken relative link | Step 3; verification 2, 3 |
| 3. `git log --follow` works | Step 2 (`git mv`); verification 4 |
| 4. README explains the folders | Step 4 |
| 5. CLAUDE.md names the new paths | Step 6 |
| 6. `work/2.0/INDEX.md` lists artifacts with state | Step 5; verification 5 |

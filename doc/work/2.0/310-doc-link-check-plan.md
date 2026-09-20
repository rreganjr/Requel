# #310 — A markdown link check for `doc/`, and the five unresolvable references

https://github.com/rreganjr/Requel/issues/310

## Summary

#304's verification pass surfaced five `doc/` paths that do not resolve. Only one of them is
wrong; the other four are prose that was never meant to resolve. A checker that cannot tell those
apart would report four false positives and invite four edits to dated work artifacts, which
`doc/README.md` and CLAUDE.md forbid.

Reviewing the ticket against the tree at `879f572c` turned up a sixth thing the issue did not know
about: **67 markdown links under `doc/` that a naive resolver calls broken**, all of them the
Codex/Cline citation form `[Name](./Requel/<repo-relative-path>:<line>)`, in
`work/2.0/TEST_IMPL_codex_review.md` (56), `archive/LOG4J_update.md` (9) and
`work/backlog/e2e-coverage-improvement-plan.md` (2). Every one of them resolves once the
`./Requel/` prefix and the `:<line>` suffix are stripped. Normalizing that form — rather than
allowlisting 67 lines or editing three historical documents — is what makes AC 2 true.

Two passes, one script:

- **Links** (`[text](target)`) must resolve. A broken one fails the run.
- **Prose** (a bare or backticked `doc/…​.md` inside a sentence) is reported informationally and
  never fails, with an allowlist so the list trends to empty without touching a historical file.

## Locked decisions

1. **A sibling script, not inline in `gen-doc-index.sh`.** `scripts/check-doc-links.sh` runs
   standalone with no `gh`, no token and no network. `gen-doc-index.sh` calls it **after** writing
   `INDEX.md` and the `doc/README.md` block, so the generated links are themselves checked, and
   propagates its exit code. The follow-on CI ticket can then run the checker alone.
2. **Citation-form normalization.** Before resolving, a link target loses any `#fragment`, a
   leading `./Requel/`, and a trailing `:<line>` or `:<line>:<col>`. Resolution is attempted
   relative to the citing file's directory and then to the repo root; a directory counts as
   resolved (`doc/README.md` links `work/backlog/`, and the generator writes that line itself).
   `http:`, `https:`, `mailto:` and pure `#anchor` targets are skipped — no network, and no anchor
   validation (zero links in the tree use a fragment today).
3. **Prose means `doc/…​.md`, nothing wider.** Restricted to tokens that literally start with
   `doc/`, the pass reports exactly the five references in the issue. Widened to any `*.md`-looking
   token it reports 139 distinct targets / 209 occurrences, nearly all of them bare-name citations
   of files that do exist (`AUTH_ARCH.md` ×13, `UI_REFACTOR_PLAN.md`, `RELEASE_20_TEST_PLAN.md`),
   template placeholders (`<issue>-<slug>-plan.md`) and shell fragments (`ls doc/*.md`). An
   informational list nobody reads is worse than no list.
4. **Fenced code blocks are skipped; inline code spans are skipped for links and kept for prose.**
   A path inside a fence is an example, not a reference — this is why
   `post-124-ui-epic-plan.md:98` (a `gh issue create --body-file …` example) is not reported while
   line 152, the same path in a sentence, is. A backticked path in running text is exactly the
   prose case the pass exists for.
5. **Scope is `doc/**/*.md` plus the three root documents** (`README.md`, `RELEASE.md`,
   `CLAUDE.md`). They are clean today and they are the files most likely to cite a path the next
   reorganization moves; including them costs one line.
6. **Opt-outs live in `scripts/doc-link-allow.tsv`**, following the `scripts/retro-overrides.tsv`
   precedent — nothing new belongs at the root of `doc/`. Entries are keyed on
   `citing-file<TAB>target<TAB>reason`, never on a line number, which shifts the first time anyone
   edits above it.
7. **One repoint, as a real link.** `work/2.0/ui-refactor-cursor-review.md:13` becomes
   `[initial completeness review](ui-refactor-claude-review.md)` rather than a swapped bare path,
   so the checker's link pass covers it from then on. This is the single edit to a dated artifact
   and it is what AC 5 carves out.
8. **#279's reference is temporary, not permanent.** The plan file on the
   `279-orphan-assistant-annotations` branch sits at the pre-reorg path
   `doc/279-orphan-assistant-annotations-plan.md`; after #304 it belongs at
   `doc/work/2.0/`. The allowlist entry says so and comes out when that branch rebases and the
   citation is corrected there — it is not a forever waiver.

## Contract

```
scripts/check-doc-links.sh [--quiet]

exit 0   no broken links
exit 1   at least one broken markdown link (listed as file:line -> target)
exit 2   usage / environment error
```

Output is two sections: `broken markdown links` (failing) and `unresolvable prose mentions`
(informational, allowlisted entries suppressed and counted). Implementation is bash wrapping a
`python3` heredoc, matching the house style already in `gen-doc-index.sh`.

`scripts/doc-link-allow.tsv`:

```
# citing file (repo-relative)                  target                                        reason
doc/work/2.0/43-review.md                      doc/43-comment.md                             The sentence says the references are broken; self-documenting.
doc/work/2.0/RELEASE_20_TEST_PLAN.md           doc/project-delete-support-plan.md            Conditional — "if/when it's prioritized, capture the design in …".
doc/work/2.0/post-124-ui-epic-plan.md          doc/post-124-ui-epic-body.md                  Instruction, not a link; epic #219 closed 8/8 and the body was never drafted.
doc/work/2.0/279-assistant-delete-race-plan.md doc/279-orphan-assistant-annotations-plan.md  TEMPORARY — remove when #279 rebases onto post-#304 release/2.0 and its citation moves to doc/work/2.0/.
```

## Step by step

### 1. `scripts/check-doc-links.sh`

File set: `doc/**/*.md` + `README.md`, `RELEASE.md`, `CLAUDE.md`. Per line, toggle on fence
markers (```` ``` ```` / `~~~`) and skip fenced lines. Link pass on the line with inline code spans
blanked; prose pass on the raw line, minus anything that is already a link target on that line.
Normalize, resolve, collect. Load the allowlist, suppress matching prose entries. Print, exit.

### 2. Wire it into `gen-doc-index.sh`

At the end, after the python heredoc writes `INDEX.md` and refreshes the README block:

```bash
"$root/scripts/check-doc-links.sh"
```

`set -euo pipefail` already propagates the non-zero exit. The index is written either way, which is
deliberate — a broken link somewhere in `doc/` is not a reason to leave a stale index behind.

### 3. The one repoint

`doc/work/2.0/ui-refactor-cursor-review.md:13`, `(doc/ui-refactor-review.md)` →
`[initial completeness review](ui-refactor-claude-review.md)`. That file is titled "UI Refactor
Plan — Completeness Review", which is what the citing sentence calls it.

### 4. `scripts/doc-link-allow.tsv`

The four entries above, with the header comment explaining that this file exists so historical
prose is never retro-edited.

### 5. `.gitignore` — `work/` → `/work/`

Found while writing this plan: `.gitignore:2` is `work/`, an unanchored pattern that matches **any**
directory called `work`, including `doc/work/`. The 125 files #304 moved there stayed tracked
because `git mv` keeps a tracked path tracked, but every *new* document under `doc/work/<release>/`
since then is silently ignored — this plan included. Anchoring it to `/work/` restores the intent
(a scratch directory at the repo root; nothing is tracked under one today and none exists) and
makes the release folder writable again without `git add -f`.

The same bug has a second victim: `doc/work/2.0/INDEX.md`, generated after #304 and never
committable, so `doc/README.md`'s index link is a 404 on GitHub today. It is added in this
commit. To catch that class in future, the checker reports a link that resolves **only in the
working tree** — untracked or ignored — as an informational line rather than a failure, since a
generated file can legitimately be ahead of the index mid-change.

### 6. `doc/README.md`

Two sentences under "The index": the generator also checks links, the checker runs standalone, and
intended dangling prose is recorded in `scripts/doc-link-allow.tsv`.

## Verification

No `modules/**` and no `requel-angular/**` change, so `mvn clean verify`, the vitest suite, the
typecheck and e2e are all not applicable to this ticket. The gate is `tmp/310-verify.sh`:

1. `./scripts/check-doc-links.sh` exits 0 and reports **0 broken** out of **195 links**.
2. The prose list is empty after allowlisting (5 found, 1 fixed by the repoint, 4 suppressed).
3. Negative test: a fixture file with a deliberately broken link makes the script exit 1 and name
   it; a fixture with a broken path inside a fence does not.
4. Negative test: `[x](./Requel/pom.xml:45)` resolves; `[x](./Requel/nope.xml:45)` does not.
5. The changed set is exactly `scripts/check-doc-links.sh`, `scripts/doc-link-allow.tsv`,
   `scripts/gen-doc-index.sh`, `doc/README.md`, `doc/work/2.0/ui-refactor-cursor-review.md`,
   `.gitignore`, `doc/work/2.0/INDEX.md` and this plan.
6. The "resolves only in the working tree" list is empty once `INDEX.md` is staged.
7. `./scripts/gen-doc-index.sh` (needs `gh`; developer runs it) still writes the index and now
   exits 0.

## Out of scope

- Editing dated work artifacts to make historical prose resolve — the four allowlist entries exist
  precisely so this never happens.
- Creating `post-124-ui-epic-body.md` or `project-delete-support-plan.md` retroactively.
- Wiring the checker into CI — a separate ticket once it has run clean for a while.
- Anchor/heading validation and reachability checks on the 93 external URLs.
- Fixing #279's citation, which belongs to that branch's rebase.

## Risks

- **The citation-form normalization hides real rot in those 67 links.** It resolves the file, not
  the line, so a moved method still reports clean. Accepted: the alternative is 67 permanent
  waivers, and a file-level check is strictly better than today's nothing.
- **A future tool emits a citation form we do not normalize.** The failure mode is a loud false
  positive on the next run, not silence — acceptable, and the fix is one more normalization rule.
- **The `.gitignore` anchor is a repo-wide change** riding along in a docs ticket. It is one
  character and it is what makes this ticket's own plan committable; the alternative is
  `git add -f` on every future plan, which will be forgotten.
- **The prose pass is narrow by design** and will not catch a bare-name citation
  (`AUTH_ARCH.md`) that rots. Revisit with a basename-resolution fallback if that ever bites.

## Acceptance criteria mapping

| AC | Where |
|---|---|
| 1. Resolves every markdown link under `doc/**/*.md`, exits non-zero on a broken one | Steps 1–2; contract above |
| 2. Zero broken markdown links on `release/2.0` after #304 | Verification 1 — measured: 195 links, 0 broken with normalization |
| 3. Unresolvable prose mentions informational, never fatal | Step 1 prose pass; Verification 2 |
| 4. `ui-refactor-cursor-review.md` points at a file that exists | Step 3 |
| 5. No dated work artifact edited except that repoint | Verification 5 |

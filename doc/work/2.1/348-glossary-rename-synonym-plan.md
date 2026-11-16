# #348 — Glossary rename keeps the old name as a synonym; offer to rewrite referers' text

https://github.com/rreganjr/Requel/issues/348

Branch `348-glossary-rename-synonym` from `release/2.1`, PR to `release/2.1`. Size 5.

## Summary

Renaming a glossary term changes `terms.name` and nothing else. Text written against the old name
stops matching any term, so #268's glossary check raises it again as a candidate: the rename
re-creates the finding it was meant to clear. This keeps the old name as a synonym of the renamed
term, keeps synonyms out of the vague-word check, and lets the user replace the old name in the
term's referers, from the term editor or over the gateway.

## Locked decisions (2026-11-02)

1. **The synonym.** On a rename, `EditGlossaryTermCommandImpl` adds a term with the old name whose
   canonical term is the renamed one.
   - It is skipped when the old name is already a term or synonym in the project, or the rename
     changes only case.
   - It gets no definition and no referers; those stay with the renamed term.
   - This covers both rename paths: the term editor, and accepting #268's vague-word fix, which
     goes through the same command (the text-editor registry).
2. **Synonyms skip the vague-word check.** `LexicalVagueWordAssistant` leaves a glossary term with
   a canonical term alone. Otherwise the new synonym "Dry-run event" gets the same "event is vague"
   issue, and accepting it would try to rename the synonym into a duplicate. The other checks
   (spelling and so on) still run.
3. **The offer to rewrite is stateless.** The term editor shows "Still uses an old name": each
   referer of the term whose name or text contains one of its synonyms, with Replace per row and
   Replace all. It is computed from current data, so a rename made by accepting the vague-word fix
   (in the issue panel, not the editor) gets it too, and it survives a reload. It only lists;
   flagging a synonym's use as a finding stays #258.
4. **`ReplaceTermText`, a new command.**
   - Input: project, term (canonical), synonym, and the referers to change (type + id).
   - Per referer, it builds the edit through the text-editor registry the spelling fix uses, so
     each entity's own `Edit*` command and permission apply. Name and text.
   - Matching: whole words (or the whole phrase), case-insensitive; a leading capital carries over
     to the replacement.
   - A referer the caller can't edit is refused and the rest are applied; the result lists
     `applied` and `refused`. A referer not in the term's referers, or one that no longer
     contains the synonym, is reported as `unchanged`.
   - On the gateway allowlist, so MCP and the CLI can do the same.
5. **The term detail gains `synonymUses`**: per synonym, the referers still using it. After a
   rename the `EditGlossaryTerm` result (the term detail) shows the new synonym in
   `alternateTerms` and the referers to rewrite in `synonymUses`; the editor panel reads the same
   field.
6. **Out of scope** as in the issue: text that uses a term without being a referer; flagging
   synonym use (#258).

## Changes

project-domain
- `TermTextMatcher` (new, pure): `contains(text, phrase)` and `replace(text, phrase, replacement)`
  on word boundaries, case-insensitive, carrying a leading capital per match. A boundary is a
  non-letter, non-digit character or the text's edge, so "dry-run event" matches in "the
  dry-run event's slides" and not in "dry-run events".
- `ReplaceTermTextCommand` (interface) and `ProjectCommandFactory.newReplaceTermTextCommand()`.

project-jpa
- `EditGlossaryTermCommandImpl.execute`: before renaming, remember the old name; after the merge,
  when the name changed (not case-only) and no term or synonym has the old name, persist a
  `GlossaryTermImpl(old name)` with `canonicalTerm` = the renamed term and add it to the project's
  terms and the term's alternates. `getCreatedSynonym()` reports it.
- `ReplaceTermTextCommandImpl` (new): resolves the term, the synonym (must be one of the term's
  alternates) and each referer (must be one of the term's referers); for each, Name then Text,
  replaces through `TermTextMatcher` and builds the edit with `AnnotatableTextEditRegistry`;
  pre-checks the edit's stakeholder permission (as `ResolveIssueWithChangeSpellingPositionCommandImpl`
  does) and records a refusal instead of throwing; executes the rest through the command handler.
  It is a `ProjectScopedCommand`. Its own authorization requirement is null: every change is an
  `Edit*` command checked on its own, so a caller who may edit nothing changes nothing.

assistant-legacy-nlp
- `LexicalVagueWordAssistant`: skip a `GlossaryTerm` whose canonical term is set. A stale
  vague-word finding on such a term is retired by the next analysis (#270), as for any finding no
  longer reported.

service-api / service-impl
- `GlossaryTermDto`: `synonymUses` (`List<SynonymUseDto>`: synonym id, name, referers), filled by
  `toGlossaryTermDetailDto` with `TermTextMatcher`; null in summaries.
- `ReplaceTermTextInput` (projectName, termId, synonymId, referers: `[{entityType, entityId}]`)
  and `ReplaceTermTextResult` (applied, refused, unchanged: `EntityReferenceDto` lists).
- `ProjectCommandRegistrar`: register `ReplaceTermText`. `GatewayPolicyConfig.ALLOWED`: add it;
  the MCP catalog lockstep test follows.

requel-angular
- `models/term.ts`: `synonymUses`. `TermService.replaceTermText(...)`.
- `features/terms/term-editor.ts`: the "Still uses an old name" panel, shown when `synonymUses` is
  non-empty: one row per referer (type, linked name, `Replace "<old>" with "<new>"`), and Replace
  all per synonym. After the call, an announcement ("Replaced in 3; 1 refused: Story 'X' needs
  Edit permission") and a reload.
- Specs; e2e: rename a term, see the panel, Replace all, the referers' text reads the new name.

## Steps

1. Branch.
2. `TermTextMatcher` and its unit test.
3. The synonym on rename.
4. The vague-word skip.
5. `ReplaceTermText`, its input/result, registration and gateway entry.
6. `synonymUses` on the detail DTO.
7. Angular panel, service, specs, e2e.
8. Tests below.

## Test plan

`tmp/348-verify.sh`: backend `mvn clean verify -pl modules/coverage-report -am`, the coverage
check, the Angular typechecks and unit tests. e2e runs in CI (and locally where it can).

- `TermTextMatcherTest`: whole word and whole phrase; case-insensitive; a leading capital carries
  over; no match inside a longer word; several matches; a hyphenated phrase; null and empty text.
- `GlossaryRenameIT`:
  - renaming "Dry-run event" to "Dry-run webinar" leaves a "Dry-run event" term whose canonical
    term is "Dry-run webinar";
  - a rename whose old name already exists as a term or synonym creates no duplicate;
  - a case-only rename creates no synonym;
  - accepting a vague-word fix on the term's name (the ChangeSpelling path) creates the synonym
    too;
  - re-analysis raises no glossary candidate for "dry-run event" in a story that says it, and no
    vague-word issue on the synonym.
- `ReplaceTermTextIT`:
  - Replace all edits exactly the referers that contain the old name, in Name and Text, each
    through its `Edit*` command (the version goes up);
  - a user with Story[Edit] but not Goal[Edit] gets the goal refused and the stories applied;
  - a referer that isn't one, or no longer contains the old name, is `unchanged`;
  - over the gateway: allowed, same result.
- `ProjectQueryController` detail: `synonymUses` lists the referers using each synonym; a term
  with no synonyms has an empty list.
- Angular: `term-editor.spec.ts` (panel hidden without uses; rows and Replace all call the
  service; the result is announced); `term.service.spec.ts`.
- MCP catalog lockstep and `CommandGatewayIT` follow the allowlist.

## Out of scope

- Text using a term without being its referer; flagging synonym use (#258).
- Renaming a synonym's canonical term chain (a synonym of a synonym).

## Risks

- **Matching phrases in prose.** Word boundaries keep "event" from matching "events" or
  "eventual"; possessives ("event's") match, which is wanted. Plurals of the old name aren't
  rewritten; the panel only lists what matches, so nothing is changed silently.
- **Many referers.** Replace all runs one `Edit*` per referer in one request; each triggers
  analysis as any edit does. Roundtable-sized projects have a handful per term.

## AC mapping

| AC | Covered by |
|---|---|
| Rename leaves an old-name synonym; re-analysis raises no candidate for it | Decisions 1–2; `GlossaryRenameIT` |
| An existing old-name term or synonym means no duplicate | Decision 1; `GlossaryRenameIT` |
| The rename result lists the referers that use the old name; replace edits exactly those, each through its Edit* command; one without Edit is refused, the rest applied | Decisions 4–5; `ReplaceTermTextIT` |
| Case-only renames create no synonym and offer no rewrite | Decision 1; `GlossaryRenameIT` |
| MCP/CLI: the result reports the synonym and the referers | Decision 5; the gateway test |

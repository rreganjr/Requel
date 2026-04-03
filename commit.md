Add open issues view; fix spelling issue resolution — infinite loop and wrong replacement word

## Open Issues view

Added a project-wide view of all unresolved annotation issues, accessible from
the sidebar under each project and at `/projects/:name/open-issues`.

**Backend**
- `OpenIssueDto.java` — new record: `issueId`, `issueText`, `mustBeResolved`,
  `entityType`, `entityId`, `entityName`
- `ProjectQueryController.java` — new `GET /api/projects/{name}/open-issues`
  endpoint: walks all project entities, collects their unresolved issues, and
  returns them sorted by entity type → entity name → issue text
- `app.routes.ts` — added route `projects/:name/open-issues`

**Angular**
- `open-issues.ts` — `OpenIssuesComponent`: paginated, sortable, globally
  filterable table of issues; badge showing must-resolve count; clicking an
  entity name navigates to its editor; maps entity type names to route segments
  via a `ENTITY_ROUTES` lookup table
- `sidebar-nav.ts` — added "Open Issues" as a fixed child node (no count) under
  each project in the tree; `onNodeSelect` handles `OpenIssues` type to
  navigate to the new route

---

Two bugs in the "Fix Spelling" position resolution path, both triggered when
resolving a LexicalIssue via a ChangeSpellingPosition through the REST command API.

## Bug 1: Request hangs indefinitely (infinite loop)

`ResolveIssueWithChangeSpellingPositionCommandImpl.fixSpelling()` used a
`while (indexOf(fromWord) >= 0)` loop to replace occurrences. When `toWord`
contains `fromWord` (e.g. "act" → "action", "is" → "this"), the search from
position 0 finds the replacement and loops forever. The Java thread pegs CPU
at 100%; the browser/proxy eventually drops the connection and Spring logs a
socket exception — manifesting as a hang until timeout.

**Fix:** Replaced the while-loop with `String.replace(fromWord, toWord)`.
Java's `String.replace(CharSequence, CharSequence)` uses `Matcher.replaceAll()`
which advances past each matched position and never re-scans replaced text,
making it immune to this class of infinite loop. Also added a null guard on the
value returned from the reflective property getter.

**File:** `modules/annotation-jpa/.../command/ResolveIssueWithChangeSpellingPositionCommandImpl.java`

## Bug 2: Wrong replacement text (full description stored as proposed word)

After the hang was fixed, applying a spelling correction replaced the misspelled
word with the full human-readable position description (e.g. "Change the word
''s' to 's'") instead of just the proposed replacement word ("s").

Root cause: `ChangeSpellingPosition` has two distinct fields — `text` (the
human-readable description) and `proposedWord` (the actual replacement word,
stored in `proposed_word`). During XML project import, the import pipeline
dropped `proposedWord` before it could reach the assembler:

- `PositionImportXml` — had `proposedWord` (read from XML attribute), OK
- `PositionImportXmlMapper` — read `xml.getProposedWord()` but never stored it
- `PositionImportDraft` — had no `proposedWord` field, so it was silently lost
- `ProjectPositionAssembler` — had no way to access it; fell back to `draft.getText()`

This caused every `ChangeSpellingPosition` created via import to have
`proposed_word = text` in the database (372 corrupt records in the live DB).

**Fix (code):**
- `PositionImportDraft.java` — added `proposedWord` field, getter, and builder method
- `PositionImportXmlMapper.java` — wired `.proposedWord(xml.getProposedWord())` into the draft builder; removed the dead `text` fallback that was masking the missing field
- `ProjectPositionAssembler.java` — `changeSpellingPosition` case now passes `draft.getProposedWord()` instead of `draft.getText()`

**Fix (data):**
- `V6__fix_change_spelling_proposed_word.sql` — Flyway migration that corrects all existing corrupt records by extracting the proposed word from the description text using `SUBSTRING_INDEX(SUBSTRING_INDEX(text, ' to "', -1), '"', 1)`

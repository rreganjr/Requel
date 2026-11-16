# #486 — rc5 acceptance fixes

https://github.com/rreganjr/Requel/issues/486

Branch `486-rc5-fixes` from `release/2.1` (at `2.1.0-rc5`), PR to `release/2.1`. Size 5. Found
in the rc5 acceptance test (#420), run in a test project; the fixes ship in rc6. Seven fixes, one
ticket.

## Locked decisions (2026-11-11)

All seven go into 2.1 (Ron, 2026-11-11). The issue's own wording is the scope; decisions made
during implementation are marked.

### 1. "Analyse with AI" with no AI

- `GET /api/ai/corpus/availability?projectId=` returns `AiReviewAvailabilityDto(available,
  reason)` after the same access check as a run. It is unavailable when AI is off on the server,
  the project is off `requel.ai.projectAllowlist`, or no corpus definition is in effect after the
  project's switches (`SimpleAssistantRegistry.anyCorpusReview`, any set kind).
- The corpus panel asks once per project and shows **Analyse with AI** only when it is
  available. Find overlaps (no AI) is unchanged.
- `CorpusAnalysisService.request` refuses an `ANALYSIS` it can't run with
  `AiReviewUnavailableException`; the controller answers 409 with the reason.
- `AssistantRunWorker` names the entity type in "No assistant analyses a Project here."
  (`entityTypeName`: the domain interface's simple name, never a proxy class).

### 2. A refused delete shows only "Validation failed"

- `refusalMessage(result, fallback)` in `command.service.ts`: a refusal whose violations carry
  the reason shows their messages, without the "validation failed: " prefix; otherwise the
  error. Every editor's delete uses it (actor, use case, goal, story, stakeholder, scenario, term,
  report, and the delete-project dialog).
- `DeleteScenarioCommandImpl`'s primary-scenario message reads for a person.

### 3. Requel can't read its own rewrite

- `StepReading.withKnownSubjects(reading, name, actorNames)`: when the parser finds no verb or
  no agent, a name that starts with one of the project's actors, or "the system", with or without
  "the", followed by more words, reads as that subject. The longest name that fits wins.
- `StepStructureAssistant` and `StepSequenceAssistant` both read through it, with the actors of
  the use cases the step is in (`StepStructureAssistant.actorNames`).

### 4. Editors don't refresh after a resolve

- `annotations-section` emits `resolved` after a successful resolve and reload.
- The actor, use case, goal, story, scenario, term and step editors reload their entity quietly
  on it, as for a change from another session; a dirty form keeps its edits and is told.

### 5. The breadcrumb and tab title keep an old name

- `ArtifactNameService` holds the open entity's name against the url, cleared on navigation;
  `set(name)` also sets the title to "<name> · Requel".
- The breadcrumb's last crumb takes that name. All nine editors set it from their name signal
  once the entity exists, so it follows a save.

### 6. The sequence issue's "Do nothing." is another assistant's

- `StepSequenceAssistant` offers "Leave the steps as they are." (`DO_NOTHING_MSG`), so its
  position is its own.
- **Decided during implementation.** The #459 prune skipped a position on more than one issue,
  so it would never have removed the shared "Do nothing." from an rc5 sequence issue.
  `DeletePositionCommand.setIssue(issue)`: when the position is also on other issues it is only
  taken off that issue (and unresolves it, if it was the resolution); without an issue the
  command deletes the position as before. The prune passes the issue, so a shared,
  assistant-only position no longer offered leaves this issue and stays on the others.

### 7. A switched-off definition says "In effect for Goal"

- `AssistantDefinitionDtos.toDto` gives a definition that is off an empty `inEffectFor`.
- The definitions list reads "Off: not in effect" for it.

## Test plan

`tmp/486-verify.sh` runs the backend verify (the MySQL ITs need Docker), the coverage-modules
check, the Angular typechecks, a dev build and the full Angular unit suite.

1. `CorpusAnalysisServiceTest`: AI off, off the allowlist, and no corpus definition in effect each
   refuse an `ANALYSIS` with the reason; Find overlaps still runs.
   `CorpusAnalysisControllerTest`: 409 and the availability read. `DefinitionRegistryTest`:
   `anyCorpusReview` after the switches, a definition that is off, and no executor.
   `NlpDisabledDispatchTest`: the entity-type message. `project-corpus-panel.spec`: the button
   follows availability.
2. `command.service.spec`: `refusalMessage`. `scenario-editor.spec`: a refused delete shows the
   server's reason.
3. `StepReadingTest`; `StepStructureAssistantIT`: a step starting with a known actor reads as
   theirs, and every rewrite the check offers reads back.
4. `annotations-section.spec` (emits only on success), `step-editor.spec` and
   `scenario-editor.spec` (reload after a resolve).
5. `breadcrumb.spec`: the name follows a rename and clears on navigation; the 25 editor specs.
6. `StepSequenceAssistantTest` and `StepSequenceAssistantIT` with the new wording;
   `StepSequenceAssistantIT.aReRunTakesASharedPositionItNoLongerOffersOffItsIssueOnly` (the
   shared position manufactured with `JdbcTemplate`).
7. `ProjectDefinitionsApiIT.aDefinitionThatIsOffIsInEffectForNothing`;
   `project-definitions.spec`.
8. Local e2e on a local stack: the corpus, scenario, step-page and term specs.
9. Local build on the rc5 database before the PR.

## Out of scope

- The other 2.1 acceptance sections (account security, CLI, export schema, screenshots). They
  run on rc6.

## Risks

- **`DeletePositionCommand` gains a mode.** Only the prune sets the issue; the API's
  `DeletePosition` and every other caller leave it unset and delete as before.
- **Every editor now writes the tab title.** It is cleared on navigation, so a list page never
  shows an entity's name.

## AC mapping

| AC | Covered by |
|---|---|
| With AI off: no Analyse with AI, a 409 naming the reason, no Java class name | 1 |
| A refused delete shows the server's reason; the primary-scenario wording | 2 |
| A rewrite reads back; "Member downloads the invoice" is Member's | 3 |
| After a Rewrite, Merge or Fix Spelling the editor shows the change | 4 |
| After a rename the breadcrumb and title show the new name | 5 |
| The sequence issue offers "Leave the steps as they are.", credited to the sequence check | 6 |
| A switched-off definition reads "Off: not in effect" | 7 |
| Unit tests, the touched e2e specs, a local build on the rc5 database | the above, 8, 9 |

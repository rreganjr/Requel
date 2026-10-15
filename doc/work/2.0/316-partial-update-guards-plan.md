# 316 — Edit commands overwrite unsupplied properties on partial update

Implementation plan for https://github.com/rreganjr/Requel/issues/316.
Base: `release/2.0` @ `6b998fe6`. Branch: `316-partial-update-guards`.

## Summary

On update, ten project `Edit*CommandImpl` classes overwrite properties the caller didn't send.
This ticket gives them one contract: **null or absent leaves the persisted value alone, and `""`
clears it**. The contract covers name, text (`description`/`definition`), story/scenario/step type
and project organization. Angular currently clears a field by sending null, so it switches to
sending `''`. That switch also fixes clearing on use cases, actors and glossary terms, which is
already broken because those commands guard text today. Last, the #305 text-edit SPI stops sending
both name and text for a one-property correction, since it no longer has to.

Primary actor and scenario steps need a clear signal of their own, so they move to
https://github.com/rreganjr/Requel/issues/325.

Backend and Angular both change. The gates are `mvn clean verify`, the Angular unit tests for the
touched specs, typecheck, and e2e in CI.

## Locked decisions

1. **`""` clears, null means "leave it alone".** Decided in review. The input DTOs are records,
   so the server can't tell an absent field from an explicit null, and presence tracking
   (`JsonNullable`/`Optional`) would touch every Input record for no extra expressiveness.
2. **Properties in scope: name, text, type, organization.** Primary actor and steps go to #325.
   Out of scope: the annotation commands (Note, Issue, Position, Argument, the Position
   subclasses), Tag, TagCategory, User and the dictionary commands.
3. **`""` is stored as sent.** A cleared text persists as `""`, not null. Goal, story and
   non-user stakeholder already store `""` this way, and Angular loads with `?? ''`, so reads
   don't care which it is. No normalisation pass and no data migration.
4. **`organizationName: ""` means no organization, on create as well as update.** Today, if
   anything sent `""`, `resolveOrganization()` would try to create an organization with an empty
   name, which fails bean validation ("organization name is required"), so the save fails.
   Angular never sends `""` on create, so nothing observable changes, but it's technically a
   create-side change. Agreed in review.
5. **DTO validation is unchanged.** `@NotBlank name` stays on the Goal, Scenario, Story,
   NonUserStakeholder, ReportGenerator, UseCase, Actor and GlossaryTerm inputs, so over the API a
   null name is still rejected for those types. The name guard matters for Project and Step, whose
   inputs don't require a name, and for in-process callers (the #305 SPI, and `EditUseCase`'s
   nested `EditScenario`). Relaxing `@NotBlank` for updates would be a separate API change.
6. **The #305 SPI sends only the edited property.** Decided in review.
   `ProjectAnnotatableTextEditorConfiguration.configure` stops reading the other property back
   off the entity. The existing `ResolveIssueSpellingIT.correctingTheNameLeavesTheTextIntact`
   then proves the contract end to end.

## What the tree actually looks like

The update-branch table in the issue is current as of `6b998fe6`. Two things it implies that the
steps below rely on:

- **`EditUseCaseCommandImpl`'s update branch** builds a nested `EditScenario` with name, type and
  step commands but **no text**. Once `EditScenarioCommandImpl` guards text, that nested call
  leaves the scenario's text alone, so no change to `EditUseCase` is needed for that bug.
- **Where Angular sends null to clear:**

  | File | Field |
  |---|---|
  | `features/projects/project-editor.ts` | `description: description \|\| null`; organization clears to `organizationId`/`organizationName` both null |
  | `features/scenarios/scenario-editor.ts` | `text: text \|\| null` (details save, and `applyStepEdit`); step `text: step.text ?? null` |
  | `features/use-cases/use-case-editor.ts` | `text: text \|\| null` (two save paths) |
  | `features/actors/actor-editor.ts` | `description: text \|\| null` |
  | `features/terms/term-editor.ts` | `text \|\| null` |
  | `features/reports/report-editor.ts` → `core/report.service.ts` | `text \|\| null`; `saveReport(text: string \| null)` |

  `goal-editor.ts`, `story-editor.ts` and `stakeholder-editor.ts` already send `''`.

## Contracts

| Property (update) | null / absent | `""` | value |
|---|---|---|---|
| `name` | unchanged | as today | set |
| `text` / `description` / `definition` | unchanged | cleared (stored `""`) | set |
| `storyTypeName` / `scenarioTypeName` | unchanged | — (not a valid enum; unchanged behaviour) | set |
| `organizationId` + `organizationName` (Project) | both null → unchanged | `organizationName: ""` → no organization | resolve / create as today |

Create semantics are unchanged, apart from locked decision 4.

Document the contract once in the javadoc of each affected `Edit*Input` record's `@param` lines
("null leaves it unchanged on update; empty string clears"). The MCP write tools and the CLI
derive their schemas from those records, so external callers see the same wording.

## Step by step

**Commit 1 — guard the scalar setters.** In the update branch only:

- `EditGoalCommandImpl`, `EditStoryCommandImpl`, `EditScenarioCommandImpl`: wrap `setText` in
  `if (getText() != null)`. Story and Scenario also wrap `setStoryType` / `setType` in a
  null check on the resolved type.
- `EditScenarioStepCommandImpl`: guard name, text and type.
- `EditNonUserStakeholderCommandImpl`, `EditReportGeneratorCommandImpl`: guard name and text.
  `EditNonUserStakeholder` sets text outside the if/else today, so move it into the update branch
  (the create path still sets it from `getText()`).
- `EditProjectCommandImpl`: guard name and text (`setText` also sits outside the if/else today).
  Organization is covered in commit 2.

Match the existing `EditGoalCommandImpl` comment style: one line citing #316 at each new guard.

**Commit 2 — Project organization and null-name uniqueness.**

- `resolveOrganization()` returns a small result that says whether the caller supplied an
  organization at all, not just the `Organization` (or null). Sketch: `organizationId != null` →
  find by id; `organizationName` non-empty → find or create; `organizationName` is `""` →
  supplied, no organization; both null → not supplied. On update, call `setOrganization` only
  when an organization was supplied. On create, `""` and both-null both mean no organization.
- `EditProjectCommandImpl`, `EditNonUserStakeholderCommandImpl` and
  `EditReportGeneratorCommandImpl`: skip the name-uniqueness lookup when `getName() == null`, as
  `EditGoalCommandImpl` does. This is a real fix, not tidying: `findProjectByName`,
  `findStakeholderByProjectOrDomainAndName` and `findReportGeneratorByProjectOrDomainAndName`
  all call `name.trim()`, so today a null name NPEs before the command reaches its setters.
  The ReportGenerator lookup has the same NPE, so it's included even though the issue names only
  Project and NUS.

**Commit 3 — #305 SPI.** `ProjectAnnotatableTextEditorConfiguration.configure`: set only the edited
property, name or text, and rewrite the javadoc paragraph that cites #316 to state the contract
it now relies on.

**Commit 4 — Angular sends `''` to clear.** For each file in the table above, replace
`|| null` / `?? null` with `''` for text fields. In `project-editor.ts`, when no organization is
selected, send `organizationName: ''` (and `organizationId: null`). `report.service.ts`'s
`saveReport` takes `text: string`. Update the specs that assert the old `null` payloads.

**Commit 5 — javadoc on the Input records**, per Contracts.

**Commit 6 — tests.** Below.

Keep `tmp/316-verify.sh` running `mvn clean verify`, the touched Angular specs (`CI=1 npx ng test
--watch=false --include=...`), both `tsc --noEmit` passes, and `ng build --configuration
development`.

## Test plan

Gates: `mvn clean verify` green; the Angular unit tests for the touched specs; typecheck; e2e in CI,
since user-visible save flows change.

**Java — one partial-update test group per command**, in the existing classes under
`modules/requel-app/src/test/java/com/rreganjr/requel/project/impl/command/`. Each creates the
entity with a name, a text and (where it applies) a type, then runs update commands directly:

| Class | Cases |
|---|---|
| `GoalCommandTest` | null text keeps text; `""` clears it |
| `StoryCommandTest` | null text keeps text; null `storyTypeName` keeps type; `""` clears text |
| `ScenarioCommandTest` | null text keeps text; null `scenarioTypeName` keeps type; `""` clears text |
| `ScenarioStepCommandTest` | null name, null text, null type each keep their value; `""` clears text |
| `StakeholderCommandTest` (non-user) | null name keeps name; null text keeps text; `""` clears text |
| `ReportGeneratorCommandTest` | null name keeps name; null text keeps text; `""` clears text |
| `EditProjectCommandImplTest` | null name keeps name; null description keeps it; `""` clears it; no org fields keeps the organization; `organizationName: ""` clears it and creates no organization (row count unchanged); create with `""` has no organization |
| `UseCaseCommandTest` | a use-case update leaves its scenario's text intact (fails on `6b998fe6`); existing guards still hold |
| `ActorCommandTest`, `GlossaryTermCommandTest` | `""` clears text/definition (already guarded; pins the clear half of the contract) |

Every "keeps" case also asserts that the property that *was* sent changed, so a command that
ignores everything doesn't pass. Look up ids after insert and never assume them (CLAUDE.md).

**Java — SPI end to end.**

- `ResolveIssueSpellingIT.correctingTheNameLeavesTheTextIntact` stays green with the read-back
  removed. That's the proof that the guard, not the workaround, now protects text.
- Add `correctingTheTextLeavesTheNameIntact` beside it.
- `ResolveIssueDictionaryIT` stays green unchanged.

**Angular unit tests.** Each editor in the table gets a spec case: empty the text control, save, and
assert that the command input carries `text: ''` (or `description: ''`, `organizationName: ''`).
Existing specs that assert `null` for an empty field get updated. That's intended, not a regression.

**e2e.** One "clear the description and save; reload; it stays empty" case each in
`use-cases.e2e.ts`, `actors.e2e.ts`, `terms.e2e.ts`, `projects.e2e.ts` (plus clearing the
organization), `scenarios.e2e.ts` and `reports.e2e.ts`. Use-case, actor and term fail on
`6b998fe6`. The others guard against a regression from commit 1.

**Unchanged, expected green:** `ProjectXml{,Streaming}RoundTripIT`, `CommandGatewayIT`,
`AuthorizationIT`, `DeleteProjectIT`, `DeleteCascadeIT`.

## Out of scope

- Primary actor and scenario steps → #325.
- The annotation commands, Tag, TagCategory, User, dictionary commands.
- Relaxing `@NotBlank name` on update inputs (locked decision 5).
- Normalising existing `""`/null text in the database.

## Risks

- **External callers that clear with null.** An MCP agent or CLI user who sends `text: null` to
  clear now keeps the old text. That's the intended contract change. The Input javadoc (commit 5)
  is where they'll find it, so call it out in the PR body.
- **An Angular save path the grep missed.** If a path still sends null to clear, clearing silently
  stops working there. The table came from grepping `|| null` / `?? null` across
  `features/**` and `core/**`. Re-run that grep before the commit, and the e2e cases cover each
  editor.
- **`""` versus null in exports and DTOs.** Cleared text now round-trips as `""` rather than null.
  Goals and stories already do this, and the XML round-trip ITs cover the export.
- **Scenario saves resend every step.** Step text sent as null now keeps the step's text instead
  of clearing it. Commit 4 makes Angular send `''` for an emptied step, so a step that the user
  never gave text stays `''` or null as before.

## AC mapping

| AC | Commit | Proven by |
|---|---|---|
| 1 — null `name` leaves the name unchanged | 1 | Project, Step, NonUserStakeholder, ReportGenerator tests; SPI IT |
| 2 — null text leaves it unchanged; `""` clears it | 1, 4 | per-command tests; Angular specs; e2e |
| 3 — null type leaves it unchanged | 1 | Story, Scenario, Step tests |
| 4 — organization unchanged when omitted; `""` clears without creating one | 2 | `EditProjectCommandImplTest` |
| 5 — use-case update no longer nulls its scenario's text | 1 | `UseCaseCommandTest` |
| 6 — name-less Project/NUS update skips the uniqueness lookup | 2 | Project, NUS and ReportGenerator tests (the update succeeds instead of NPEing) |
| 7 — create semantics unchanged | 1, 2 | existing create tests green; create-with-`""` org test |
| 8 — one partial-update test per affected command | 6 | table above |
| 9 — Angular sends `''`; clearing works in the UI | 4 | Angular specs; e2e cases |
| 10 — the SPI sends only the edited property; `ResolveIssue*IT` green | 3 | `ResolveIssueSpellingIT` (both cases), `ResolveIssueDictionaryIT` |

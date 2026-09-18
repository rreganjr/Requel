# #302 — Assistant permissions on annotations

https://github.com/rreganjr/Requel/issues/302

## Summary

The `assistant` user is granted **zero** stakeholder permissions on a project created through
the UI and **every** permission on an imported one, so its annotation writes are refused on
the former. The fix is one definition of what the assistant holds — `Annotation[Edit]` and
`Annotation[Delete]` — owned by the repository and applied identically by creation, import
and `RepairProjectStakeholders`.

Two things surfaced during review that belong in this ticket because the fix is wrong without
them:

- The background AI path (`CommandBackedAssistantResultApplicator`) authors **every** write as
  `context.triggeringUser()` — the human whose edit triggered the run — while
  `context.assistantUser()` is carried from `AnalysisRequestDispatcher` and never used. That
  is why #302 has not bitten from the AI side: those writes ride the triggering user's
  permissions. Assistant-authored annotations should be authored by the assistant.
- `LexicalAssistant` performs one write that is not an annotation: adding the analyzed entity
  as a *referer* on an existing glossary term, through `EditGlossaryTermCommand`, which
  requires `GlossaryTerm[Edit]`. A back-reference is annotation bookkeeping, not a
  requirements edit, so it gets a narrow command instead of widening the assistant's set.

**These land together in one PR.** Switching the applicator to the assistant identity before
the grants exist would take the AI path from working (on the human's permissions) to failing.

## Locked decisions

1. **The set is `Annotation[Edit]` + `Annotation[Delete]`, and nothing else.** Derived from
   the command inventory, not from taste:
   - `Annotation[Edit]` — `EditNoteCommandImpl`, `EditIssueCommandImpl`,
     `EditPositionCommandImpl`, `EditArgumentCommandImpl`, and the lexical subclasses
     (`EditLexicalIssue`, `EditChangeSpellingPosition`, `EditAddWordToDictionaryPosition`,
     `EditAddWordToGlossaryPosition`, `EditAddActorToProjectPosition`) which inherit it.
   - `Annotation[Delete]` — `DeleteIssueCommandImpl`, `DeleteNoteCommandImpl`,
     `DeletePositionCommandImpl`, `DeleteArgumentCommandImpl`. Not optional: the applicator's
     `applyCleanupAction` / `reconcileStaleFindings` retract the assistant's own prior findings
     on every re-run under `AUTO_RESOLVE_IF_UNTOUCHED` / `MARK_SUPERSEDED`.
   - `Annotation[Grant]` exists as a row but nothing requires it. Excluded.
2. **The definition lives in `ProjectRepository`**, beside `findAvailableStakeholderPermissions()`.
   Three callers, one source.
3. **Assistant-authored writes are authored by the assistant.** The applicator uses
   `context.assistantUser()` for annotation and cleanup writes. MCP/agent writes are
   unaffected — those go through `InProcessCommandGateway`, which stamps `editedBy` from the
   `SecurityContext` (the logged-in user), and that is intentional.
4. **The glossary referer is annotation bookkeeping.** A new narrow command requiring
   `Annotation[Edit]`; `EditGlossaryTermCommand` keeps `GlossaryTerm[Edit]` unconditionally so
   the user-facing "Add to Glossary" resolver is unchanged.
5. **Import narrows for the assistant only.** No other stakeholder's assignment changes.
6. **A failing IT is the proof**, not a manual repro.
7. **`AuthorizationExemptable` is not the mechanism.** It landed in `c49a9c1f` (#69, PR #79)
   and every one of its ~25 call sites is a parent delete marking its own cascade children
   exempt so a Delete-only stakeholder is not re-checked for Edit on each container; the TODOs
   point at #75 for removal. Using it here would exempt every assistant write wholesale. Note
   the ticket body is wrong that the commands are not marked `AuthorizationExemptable` — both
   inherit the flag from `AbstractEditCommand`, defaulting to false.

8. **Revoke down, not just up.** Wherever the assistant already holds more than the set —
   any project imported before this change — the extras are removed, by import and by
   `RepairProjectStakeholders` alike. The assistant is a service account: nobody assigned it
   the full matrix by hand, the import loop did. Scoped strictly to the `assistant`
   stakeholder; creator and human stakeholders stay grant-only in both paths.

## Contracts

```java
// project-jpa: com.rreganjr.requel.project.ProjectRepository
/**
 * The permissions the background assistant holds on every project, however the project was
 * created. Exactly Annotation[Edit] and Annotation[Delete] — see #302.
 */
Set<StakeholderPermission> findAssistantStakeholderPermissions();
```

`JpaProjectRepository` filters `findAvailableStakeholderPermissions()` by permission key
(`com.rreganjr.requel.annotation.Annotation[Edit]`, `...[Delete]`) and **throws** if it
resolves fewer than two rows rather than returning a short set — a silent under-grant would
reproduce #302 in a new shape.

```java
// project-domain: new command, internal (not registered in the API command registry)
public interface AddGlossaryTermRefererCommand extends EditCommand {
    void setGlossaryTerm(GlossaryTerm glossaryTerm);
    void setReferer(ProjectOrDomainEntity referer);
}
```
Impl is `ProjectScopedCommand, AuthorizableCommand` returning
`RequiresStakeholderPermission(Annotation.class, "Edit")`; it adds to the referer set only and
touches no other glossary-term field.

## Step by step

1. **`findAssistantStakeholderPermissions()`** — on the `ProjectRepository` interface (which
   lives in `project-jpa`), impl in `JpaProjectRepository`, with the fail-loud check.
2. **`EditProjectCommandImpl.createProject()`** — four changes, all on the assistant lines:
   grant the assistant set; wrap `findUserByUsername("assistant")` in the same
   `NoSuchUserException` catch import uses (today an assistant-less DB fails project creation
   outright); add the persisted row to `projectImpl.getStakeholders()`; call
   `ensureProjectMembership()`. (Folded-in items from the review.)
3. **`ImportProjectStreamingCommandImpl`** — split `addUserAsStakeholder` so the permission set
   is a parameter: creator keeps `findAvailableStakeholderPermissions()` and stays grant-only;
   the assistant is *set* to `findAssistantStakeholderPermissions()` — granting what is missing
   and revoking anything else, since an import into an existing project can find an
   over-granted row already there.
4. **`AddGlossaryTermRefererCommand`** — new command + factory registration; repoint
   `LexicalAssistant.addProjectOrDomainEntityAsRefererToGlossaryTerm` and
   `CommandBackedAssistantResultApplicator.applyGlossaryTermReferer` at it.
5. **`RepairProjectStakeholdersCommandImpl`** — ensure the assistant row exists and holds
   exactly the set, revoking extras left by older imports. Add a `permissionsRevoked` counter
   beside the existing ones so a run reports what it took away, and update the interface
   javadoc, which currently states the assistant is deliberately untouched pending this ticket.
6. **`CommandBackedAssistantResultApplicator`** — resolve `context.assistantUser()` for
   annotation and cleanup writes, with a fallback to the triggering user if the assistant ref
   is absent so a run never dies on identity resolution.
7. **`doc/AUTH_ARCH.md`** — add an "assistant identity and permissions" section: which identity
   writes what (background assistant vs MCP), and the one place the set is defined.

## Verification

Backend only — no `requel-angular/**` change, so no vitest, typecheck or Playwright run is
required for this ticket.

```bash
mvn clean verify
```

New/changed tests:

- **`AssistantPermissionsIT`** (new) — create a project through `EditProjectCommand`, assert
  the assistant stakeholder holds exactly the set; execute an `EditIssueCommand` as the
  assistant and assert it succeeds. Written first, confirmed red against the current tree.
- **`ImportProjectStreamingCommandTest`** — assert the imported assistant holds the same set,
  and that the creator still holds the full matrix.
- **`RepairProjectStakeholdersIT`** — a project with an over-granted assistant and one with no
  assistant row; assert both end at the set.
- **`LexicalSpellingDispatchTest`, `GoalAssistantTest`, `ProjectAssistantTest`,
  `AiReviewDispatchIT`** — authorship changes from the triggering user to the assistant;
  expect assertions here to need updating, and treat any that don't as suspicious.

Per `CLAUDE.md`'s H2 note: these ITs share a Spring context and its auto-increment counters
with every other IT in the run, so assert on permission keys and usernames, never on ids.

## Out of scope

- **Resolver permission bypass** — `Fix Spelling` mutates the annotatable's text by reflection
  with no command and no authorization; `Add to Dictionary` runs a non-authorizable
  `EditDictionaryWordCommand`; `Ignore` resolves any issue with no permission at all. Tracked as
  [#305](https://github.com/rreganjr/Requel/issues/305) — a resolve permission that does not
  imply `Actor[Edit]` / `GlossaryTerm[Edit]`.
  **Dependency:** the applicator's cleanup includes `RESOLVE_ISSUE` (`autoResolveIfUntouched`),
  so when #305 lands the assistant's set here grows by the new resolve permission. #305 must
  update `findAssistantStakeholderPermissions()`, not invent a second definition.
- **Per-assistant identities** — one user per assistant, so Lexical and the AI reviewer can
  differ. Belongs to epic #258.
- **Imported non-creator stakeholders get no permissions** — `StakeholderAssembler` never
  restores `projectPermissions` from the XML, so every imported stakeholder but the creator and
  the assistant starts empty. Pre-existing, unrelated to the assistant.
- **#75 permission coherence / removing `AuthorizationExemptable`.**

## Risks

- **Ordering.** The applicator identity switch (step 6) must not ship ahead of the grants
  (steps 1–3): today the AI path succeeds on the triggering user's permissions, and reversing
  the order breaks it. One PR.
- **Authorship visibly changes.** AI-written annotations stop showing the human who triggered
  the run. Existing annotations keep their recorded author; no backfill.
- **Revoking is the one destructive step.** It is scoped to the `assistant` stakeholder and to
  permissions outside the set, but a bug in the filter would strip a human's permissions.
  `RepairProjectStakeholdersIT` asserts the creator's matrix survives a run.
- **Old imports differ from new ones** until `RepairProjectStakeholders` runs. The repair is
  idempotent and safe to run repeatedly.
- **A missing `assistant` user** is currently fatal to project creation; step 2 makes it a
  warning, which is the import path's existing behaviour.

## Acceptance criteria mapping

| AC | Where it is satisfied |
|---|---|
| Assistant can file annotations on a UI-created project | Steps 1–2; `AssistantPermissionsIT` |
| Same permission set however the project was created | Steps 2–3; import test |
| The set is the minimum, not the full matrix | Locked decision 1 + step 4 (removes the only non-annotation write) |
| A repaired project matches a freshly created one | Step 5 (grant + revoke); `RepairProjectStakeholdersIT` |

## Ticket body corrections

Worth editing into #302 so the record matches the tree:

1. Scope item 3 — `RepairProjectStakeholders` does not restore the assistant to
   `createProject()`'s behaviour; it never touches the assistant at all, by design, pending
   this ticket.
2. "Neither command is marked `AuthorizationExemptable`" — both inherit the flag from
   `AbstractEditCommand` (default false). See locked decision 7 for why it is still not the fix.
3. "should be able to read everything ... which is neither current behaviour" — reads already
   work. `ProjectQueryController.requireProjectAccess` needs only a stakeholder row or
   `SystemAdminUserRole`, and in-process assistants read through repositories with no check.
4. The minimum set is `Annotation[Edit]` **and** `Annotation[Delete]` — Delete is required by
   the AI cleanup path, not conditional on wanting retraction.

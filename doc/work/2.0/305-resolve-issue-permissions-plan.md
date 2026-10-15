# 305 — Resolve-issue actions bypass stakeholder permissions

Implementation plan for https://github.com/rreganjr/Requel/issues/305.
Base: `release/2.0` @ `b35de1b7`. Branch: `305-resolve-issue-permissions`.

## Summary

Resolving an issue is unauthorized over `/api/commands`. Close it by making
`ResolveIssueCommandImpl` an `AuthorizableCommand` requiring the existing `Annotation[Edit]`
permission, and by routing the Fix Spelling write through each annotatable's own
`Edit*Command` instead of reflective setters.

No new `StakeholderPermissionType`, no Flyway migration, no change to the assistant permission
set — those all fall away with the decision to reuse `Annotation[Edit]`.

## Locked decisions

1. **Permission is the existing `Annotation[Edit]`.** Resolve is an annotation write.
   `Annotation.class` is already the permission entity type every annotation command uses, so
   there is no new enum constant and therefore no `ALTER TABLE` against
   `stakeholder_permissions.permission_type`, which is a MySQL `enum('Delete','Edit','Grant')`
   that H2 `create-drop` would not have caught.
2. **The assistant is already covered.** `findAssistantStakeholderPermissions()` returns
   `Annotation[Edit]` + `Annotation[Delete]`, so `CommandBackedAssistantResultApplicator`
   keeps working and `AssistantPermissionsIT`'s size assertion is untouched. #302 is fully
   discharged as a dependency.
3. **A resolve is all-or-nothing.** A Fix Spelling that spans several annotatables edits every
   one or none. A refusal on any leaves the issue unresolved.
4. **Spelling-fix edits run with `setAnalysisEnabled(false)`.** The replacement word came from
   the dictionary's own suggestion, so re-analysing it is wasted work and risks the lexical
   assistant minting a fresh issue on text it just told the user to fix.
5. **The registry covers the types the assistants actually analyze.** `AssistantTaskRunner`
   analyses exactly Project, Goal, Story, Actor, UseCase (fanning out to Scenario and Actor)
   and Step, so a lexical issue can only ever attach to one of those seven. ProjectTeam,
   GoalRelation, GlossaryTerm, NonUserStakeholder and UserStakeholder are never analysed and
   get no registration; `EditProjectTeamCommand` is therefore not built, because the case it
   would serve cannot occur. An unregistered annotatable refuses the resolve with a clear
   error rather than an NPE, so the safety net stays if an assistant is added later.
6. **No version in the request.** "Optimistic-lock validation" here means the write goes
   through the entity's command, which reloads and can fail on a concurrent change. The
   `setExpectedVersion` machinery from #108 is passed `null` deliberately, and that is
   documented at the call site.

## What the tree actually looks like

Facts the plan depends on, verified at `b35de1b7`:

- `ResolveIssueCommandImpl` extends `AbstractEditCommand` (`EditCommand`,
  `AuthorizationExemptable`). It does not implement `AuthorizableCommand`, so
  `AuthorizingCommandHandler` passes it through. Its four subclasses inherit that.
- `AnnotationCommandProjectResolver` already resolves a `Project` from an `Annotation`, a
  `Position` or an `Annotatable`. It is package-private in
  `com.rreganjr.requel.annotation.impl.command`, the same package as `ResolveIssueCommandImpl`,
  and `annotation-jpa` already depends on `project-domain`. `EditNoteCommandImpl` and
  `EditIssueCommandImpl` are the precedent.
- `ResolveIssueInput` is `(projectName, issueId, positionId)`. `AnnotationCommandRegistrar`
  never calls `setAnnotatable`, so every UI resolve takes the `getAnnotatable() == null` branch
  and rewrites every annotatable on the issue. `projectName` is currently unused by the builder.
- A lexical issue's target property is only ever `"Name"` or `"Text"`
  (`ProjectOrDomainEntityAssistant.PROP_NAME` / `PROP_TEXT`), and `checkSpelling` only ever runs
  on a `ProjectOrDomainEntity`. There is no third property and no non-project annotatable.
- `setName` is declared on `EditProjectOrDomainEntityCommand`; `setText` on
  `EditTextEntityCommand`. `setAnalysisEnabled` and `setExpectedVersion` are on
  `AbstractEditProjectOrDomainEntityCommand`.
- **Hazard, and it is a pattern.** Step 0's audit found six of the eleven candidate commands
  overwrite a property unguarded on update: `EditProjectCommandImpl`, `EditGoalCommandImpl`,
  `EditScenarioCommandImpl` and `EditStoryCommandImpl` set text unconditionally while guarding
  name; `EditScenarioStepCommandImpl` and `EditNonUserStakeholderCommandImpl` guard neither.
  `EditUseCaseCommandImpl`, `EditActorCommandImpl` and `EditGlossaryTermCommandImpl` guard
  both. `EditGoalRelationCommandImpl` and `EditUserStakeholderCommandImpl` have no name/text
  setters at all. So a name-only edit through most of these nulls the entity's text, and the
  registry must always supply both properties (see Step 2). The unguarded setters are a latent
  bug in their own right and are filed separately rather than fixed here.

## Contracts

### New SPI, in `annotation-domain`, package `com.rreganjr.requel.annotation.spi`

Sits beside `AnnotatableTypeRegistry` and follows the same shape: an interface plus a default
implementation, populated from `project-jpa` via Spring configuration. The annotation module
must not import project implementation classes.

    public interface AnnotatableTextEditRegistry {
        void registerTextEditor(Class<? extends Annotatable> entityType,
                                AnnotatableTextEditor editor);
        Optional<AnnotatableTextEditor> resolveTextEditor(Class<?> entityType);
        Map<Class<? extends Annotatable>, AnnotatableTextEditor> getRegisteredTextEditors();
    }

    @FunctionalInterface
    public interface AnnotatableTextEditor {
        /**
         * Build a configured but unexecuted command that sets propertyName to newValue on
         * annotatable, leaving that entity's other text properties as they are.
         */
        Command newEditCommand(Annotatable annotatable, String propertyName, String newValue,
                               User editedBy);
    }

`Command` is `com.rreganjr.command.Command` from `platform-core`; `User` is
`com.rreganjr.platform.identity.User`. Both are visible to `annotation-domain`.

The returned command is run through `getCommandHandler().execute(...)`, which re-enters
`AuthorizingCommandHandler` — that is the gate.

### New shared authorization check, in `project-domain`

`AuthorizingCommandHandler` lives in `requel-app`, which `annotation-jpa` cannot depend on. The
stakeholder check moves down to `project-domain` as
`StakeholderAuthorizationChecker.isSatisfied(Project, User, AuthorizationRequirement)`;
`AuthorizingCommandHandler.checkStakeholderPermission` delegates to it rather than keeping a
second copy. That comment in the handler about one copy on purpose stays true.

The resolver uses it for the pre-flight pass, so a refusal is deterministic and names the
entity, rather than depending on transaction rollback semantics. Rollback remains the
belt-and-braces.

## Step by step

**Step 0 — audit the Edit commands. DONE.** Result recorded above and in the PR body: six of
eleven overwrite unguarded. A separate issue is filed for those; this ticket does not change
their null semantics.

**Step 1 — authorize `ResolveIssueCommandImpl`.**
`implements AuthorizableCommand, ProjectScopedCommand`.
`getAuthorizationRequirement()` returns
`RequiresStakeholderPermission(Annotation.class, "Edit")`.
`getProject()` returns `AnnotationCommandProjectResolver.of(getIssue())`, falling back to
`of(getPosition())` and `ofAnnotatable(getAnnotatable())`. A `null` project means the
annotation is domain-scoped and the handler denies it — intended, and pinned by a test.
Add a class comment that the nested `Edit*Command`s stay non-exempt on purpose: the handler
short-circuits on `AuthorizationExemptable` before the authorizable check, so marking them
exempt would silently reopen this.

**Step 2 — the registry and its registrations.**
Interface and default implementation in `annotation-domain`. Registrations in `project-jpa`,
alongside `ProjectAnnotatableRegistryConfiguration`, one entry per analysed type: Project,
Goal, Story, Actor, UseCase, Scenario, Step. Seven entries, not twelve — see decision 5.

Each editor lambda:
1. resolves the command from `ProjectCommandFactory`;
2. sets the project or domain from `entity.getProjectOrDomain()`;
3. sets the entity itself via that command's own setter (`setGoal`, `setStory`, … — there is no
   common interface for this, which is the only genuinely per-type part);
4. **reads the entity's current name and text and sets both**, substituting the new value for
   whichever property is being fixed. This is what makes `EditGoalCommandImpl`'s unguarded
   `setText` harmless;
5. `setEditedBy(editedBy)`;
6. `setAnalysisEnabled(false)`;
7. leaves `expectedVersion` null, with a comment pointing at decision 6.

**Step 3 — rewrite `ResolveIssueWithChangeSpellingPositionCommandImpl`.**
Collect the target annotatables (the single one, or all of the issue's). For each, resolve an
editor and build the command. Pre-flight every built command's requirement through
`StakeholderAuthorizationChecker`; an unmapped type or a refused check throws
`AuthorizationException` naming the entity, before any write. Then execute them in order
through the command handler and call `super.execute()`.
Keep the existing case-preservation and `String.replace` semantics of `fixSpelling`. Delete
`getTextToChange`, `setChangedText`, `getPropertyGetter` and `getPropertySetter`.

**Step 4 — Angular.** `annotations-section` currently takes `[canEdit]` from the parent, which
binds it to the host entity's Edit (`permissionService.canEdit('Goal')` and friends) — the
wrong permission for every annotation action, not just resolve. Add a
`canEditAnnotations = computed(() => this.permissionService.hasPermission('Annotation', 'Edit'))`
and bind the section to that. `hasPermission` already exists and the `/my-permissions` map is
keyed by `getEntityType().getSimpleName()`, so `Annotation` is already present.

## Test plan

- `AuthorizationIT` — resolve scenarios per row of the ticket's table: permitted and refused for
  Ignore, Fix Spelling, Add as Actor, Add to Glossary; a user with `Annotation[Edit]` but
  without `Goal[Edit]` refused on a goal spelling fix; the cross-entity atomic case (holds
  `Story[Edit]`, not `Goal[Edit]`, issue spans both — nothing edited, issue unresolved); a
  domain-scoped annotation denied.
- New unit test: every entity type `AssistantTaskRunner` can analyse has a registration in
  `AnnotatableTextEditRegistry`. This is what catches a new assistant added without a text
  editor. A second case asserts an unregistered annotatable refuses cleanly rather than NPEing.
- New unit test: a name-only spelling fix leaves the entity's text unchanged (the
  `EditGoalCommandImpl` hazard, pinned).
- `PositionResolverSelectionTest` / `ResolveIssueCommandSelectionIT` — selection unchanged;
  assert each selected resolver now reports an authorization requirement.
- `requel-angular` — `annotations-section` specs for `Annotation[Edit]` gating.
- `e2e/annotations.e2e.ts` clicks `annotation-resolve-issue`; confirm the fixture user holds
  `Annotation[Edit]`.

Gate: `mvn clean verify`, plus `CI=1 npm test -- --watch=false` and both `tsc --noEmit` passes
in `requel-angular`. e2e in CI. Per-ticket script at `tmp/305-verify.sh`.

## Out of scope

- The global dictionary gate — #312.
- A version in `ResolveIssueInput`.
- The unguarded `setName`/`setText` overwrites found in Step 0 — filed as #316.
- `EditProjectTeamCommand` and text setters on `EditUserStakeholderCommand`: no assistant
  analyses those types, so no lexical issue can reach them.
- Adding `annotatableId` to `ResolveIssueInput` so a resolve targets one entity. The
  all-or-nothing rule makes this unnecessary for correctness; revisit if the atomic refusal
  proves annoying in practice.

## Risks

- **Uniqueness conflicts.** A spelling fix that changes a Goal's or Actor's *name* runs that
  command's uniqueness check. If the corrected name already exists in the project the resolve
  fails with a uniqueness conflict rather than a permission error. Correct behaviour, but it
  needs to surface as a usable message, and it is a new failure mode for a button that
  previously always succeeded.
- **Registry drift.** Adding an assistant for a new entity type without also registering a text
  editor makes that type's lexical issues unresolvable. The parity test against
  `AssistantTaskRunner` is the guard, and it is the main thing that can rot here.
- `EditActorCommandImpl.execute` contains duplicated dead blocks and runs its uniqueness check
  with a possibly-null name. Pre-existing; not touched here, but it is in the blast radius of
  any change to how that command is called.

## AC mapping

| Acceptance criterion | Covered by |
|---|---|
| No resolve without `Annotation[Edit]`, including direct API | Step 1; `AuthorizationIT` |
| No goal text change via Fix Spelling without `Goal[Edit]` | Steps 2, 4; `AuthorizationIT` |
| No add without `Actor[Edit]` / `GlossaryTerm[Edit]` | already true; pinned by `AuthorizationIT` |
| Multi-annotatable Fix Spelling is atomic | Step 3 pre-flight; cross-entity IT |
| Concurrent modification raises an optimistic-lock failure | Step 2 (routing through the command) |
| No reflective property access remains | Step 3 |
| UI does not offer actions the backend refuses | Step 4 |

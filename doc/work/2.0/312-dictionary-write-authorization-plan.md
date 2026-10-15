# 312 — Authorize the project dictionary write, and stop swallowing its failures

Implementation plan for https://github.com/rreganjr/Requel/issues/312.
Base: `release/2.0` @ `423ae4d2`. Branch: `312-dictionary-write-auth`.

## Summary

`EditDictionaryWordCommandImpl` performs a persistent write with no authorization and reports its
own failure as success. #313 made the write project-scoped, which is what makes a correct gate
possible: the command moves from `dictionary-jpa` to `project-jpa`, implements
`AuthorizableCommand` and `ProjectScopedCommand`, requires `Annotation[Edit]` on the project it
writes to, refuses a null `projectId` instead of falling back to the installation-wide dictionary,
and lets exceptions propagate so a failed write leaves the issue unresolved.

Backend only. No Angular changes, so no JS unit, typecheck or e2e gate.

## Locked decisions

1. **Move the command up a module rather than teach authorization about project ids.**
   `RequiresStakeholderPermission` is only evaluated for a command implementing
   `ProjectScopedCommand`, which returns a `Project`. `dictionary-jpa` cannot see `Project`:
   `project-domain` depends on `dictionary-jpa` (`modules/project-domain/pom.xml`), so the reverse
   edge is a cycle — #313 locked decision 3 fixed the scope key as a bare `Long` for that reason.
   The alternative considered was a `ProjectIdScopedCommand` marker plus a
   `RequiresStakeholderPermissionOnProjectId` variant resolved through `ProjectRepository` inside
   `AuthorizingCommandHandler`. Rejected: it extends a core sealed authorization type and the
   handler's constructor for one command, when `project-jpa` already hosts
   `ResolveIssueWithAddActorPositionCommandImpl` and
   `ResolveIssueWithAddGlossaryTermPositionCommandImpl` for exactly this reason.
2. **`RequiresStakeholderPermission(Annotation.class, "Edit")`.** The same permission #305 put on
   the enclosing resolve. No new `StakeholderPermissionType`, so no
   `enum('Delete','Edit','Grant')` column change, and no behaviour change for a stakeholder who can
   already resolve. `Project[Edit]` would break Add to Dictionary for annotation-only stakeholders.
3. **Null `projectId` is refused.** `EditDictionaryWordCommand.setProjectId`'s "a null projectId
   keeps the old installation-wide behaviour" contract is withdrawn. `DictionaryRepository`'s
   single-argument `addToDictionary(String)` and the null branch of `addToDictionary(Long, String)`
   stay as they are — they are the repository's business and `ImportProjectStreamingCommandImpl`
   calls the two-argument form with a real id — but after this ticket nothing reaches the null
   branch. Removing it is a follow-up, not this diff.
4. **Keep `extends AbstractDictionaryCommand`.** Not `AbstractProjectCommand`, not
   `AbstractEditCommand`: both implement `AuthorizationExemptable`, and
   `AuthorizingCommandHandler.execute` checks that flag and returns *before* the
   `AuthorizableCommand` branch. Extending either would silently skip the gate this ticket adds —
   the same trap `ResolveIssueCommandImpl`'s javadoc warns about for #305's sub-commands.
5. **The resolver sets `editedBy` explicitly.** `CurrentUserCommandHandler` injects it from the
   `SecurityContext` for any `EditCommand` with a null `editedBy`, which covers HTTP but not
   in-process callers — tests and initializers have no `SecurityContext`, `editedBy` stays null,
   and `AuthorizingCommandHandler.checkAuthorization` returns early on a null `editedBy`. Relying
   on the injection would make the refusal test pass for the wrong reason.
6. **Ordering, not rollback, keeps the issue unresolved.** The nested command runs before
   `super.execute()` in the resolver, so a propagated exception means the resolve never happens.
   No transaction reasoning required.

## What the tree actually looks like

Verified at `423ae4d2`:

- `EditDictionaryWordCommandImpl` (`modules/dictionary-jpa/.../impl/command/`) extends
  `AbstractDictionaryCommand extends AbstractCommand`. Fields: `lemma`, `projectId`. No
  `editedBy`, no interfaces beyond `EditDictionaryWordCommand extends Command`.
- Its only structural reference is `DictionaryCommandFactoryImpl:76`. Its only caller is
  `ResolveIssueWithAddWordToDictionaryPositionCommandImpl`, which since #313 sets
  `command.setProjectId(getProject() == null ? null : getProject().getId())`.
- `ResolveIssueCommandImpl` (annotation-jpa) implements `AuthorizableCommand, ProjectScopedCommand`
  since #305, with `getProject()` delegating to `AnnotationCommandProjectResolver`.
- `ProjectCommandFactory` lives in `project-domain`, whose pom already lists `dictionary-jpa`, so it
  can declare a method returning `EditDictionaryWordCommand`.
- `ProjectRepository` (in `project-jpa`) has `findById(Class<T>, Long)`.
- `AuthorizationRequirement` is a sealed interface of four records; `AuthorizingCommandHandler`
  handles each and resolves the stakeholder through `ProjectScopedCommand.getProject()`.

## Contracts

| Before | After |
|---|---|
| `interface EditDictionaryWordCommand extends Command` | `extends EditCommand` |
| `dictionary-jpa` → `platform-core` only | `+ platform-identity` |
| `com.rreganjr.nlp.dictionary.impl.command.EditDictionaryWordCommandImpl` | `com.rreganjr.requel.project.impl.command.EditDictionaryWordCommandImpl` |
| `DictionaryCommandFactory.newEditDictionaryWordCommand()` | `ProjectCommandFactory.newEditDictionaryWordCommand()` |
| resolver injects `DictionaryCommandFactory` | injects `ProjectCommandFactory` |
| `execute()` swallows every exception | propagates |
| null `projectId` → installation-wide write | null `projectId` → `EntityValidationException` |

The command keeps its bean name `editDictionaryWordCommand` and `@Scope("prototype")`;
`com.rreganjr.requel.project.impl.command` is already component-scanned.

## Step by step

**Commit 1 — interface and module edge.**
`EditDictionaryWordCommand extends EditCommand`. Add `platform-identity` to
`modules/dictionary-jpa/pom.xml`. Rewrite the `setProjectId` javadoc: the project whose dictionary
receives the word, required, no installation-wide fallback. Note in the type javadoc that the
command is not registered for `/api/commands` and belongs on `GatewayPolicyConfig.DENIED` if it
ever is.

**Commit 2 — move the implementation.**
Delete `modules/dictionary-jpa/src/main/java/com/rreganjr/nlp/dictionary/impl/command/EditDictionaryWordCommandImpl.java`.
Add `modules/project-jpa/src/main/java/com/rreganjr/requel/project/impl/command/EditDictionaryWordCommandImpl.java`:

- `extends AbstractDictionaryCommand implements EditDictionaryWordCommand, AuthorizableCommand, ProjectScopedCommand`
- constructor `(DictionaryRepository, ProjectRepository)`
- fields `lemma`, `projectId`, `editedBy`
- `getProject()` → `projectRepository.findById(ProjectImpl.class, projectId)`; null id returns null,
  and a `NoSuchEntityException` is caught and returned as null. Both make
  `AuthorizingCommandHandler` deny with "not a stakeholder on the target project" rather than
  throwing out of the check.
- `getAuthorizationRequirement()` → `new RequiresStakeholderPermission(Annotation.class, "Edit")`
- `execute()` validates that `projectId` is set (`EntityValidationException.emptyRequiredProperty`),
  then `getDictionaryRepository().addToDictionary(getProjectId(), getLemma())` with no try/catch.

**Commit 3 — move the factory method.**
Remove `newEditDictionaryWordCommand()` from `DictionaryCommandFactory` and
`DictionaryCommandFactoryImpl`; add it to `ProjectCommandFactory` and `ProjectCommandFactoryImpl`,
returning `getCreationStrategy().newInstance(EditDictionaryWordCommandImpl.class)`.

**Commit 4 — resolver.**
`ResolveIssueWithAddWordToDictionaryPositionCommandImpl` swaps `DictionaryCommandFactory` for
`ProjectCommandFactory` in its constructor and sets `setEditedBy(getEditedBy())` alongside the
existing lemma and project id. Ordering unchanged: nested command, then `super.execute()`.

**Commit 5 — tests.** Below.

## Test plan

Gate: `mvn clean verify` green. Backend-only change, so no `requel-angular` suites.

- **`AnnotationCommandTest`** — extend
  `resolveIssueWithAddWordToDictionaryResolvesIssue` with siblings:
  - permitted: the existing admin path, plus an assertion that the word is in the *project's*
    dictionary (`findProjectWords`) and not in `word`;
  - refused: a user without `Annotation[Edit]`, `editedBy` set explicitly — expect
    `AuthorizationException`, the issue still unresolved, and no `project_dictionary_words` row;
  - null `projectId`: `EditDictionaryWordCommand` executed directly with a lemma and no project —
    expect the validation exception and no write anywhere.
- **Independent gating** — execute `EditDictionaryWordCommand` directly (not through the resolver)
  as a user without `Annotation[Edit]` on the target project, to prove the command refuses on its
  own rather than inheriting #305's gate.
- **`AuthorizationIT`** — unchanged. #305 already covers the HTTP refusals at
  `POST /api/commands/ResolveIssue` (`deleterWithoutAnnotationEditCannotResolveIssue` and
  friends); repeating them here would test #305, not this ticket.
- **`ProjectDictionaryIT`, `ResolveIssueSpellingIT`, `DeleteProjectIT`,
  `ProjectXml{,Streaming}RoundTripIT`** — expected green untouched; they exercise the repository
  and the resolve path, neither of whose behaviour changes for an authorized user.

## Out of scope

- Removing `DictionaryRepository.addToDictionary(String)` and the null branch of the two-argument
  overload once nothing calls them (follow-up).
- A UI to list or remove a project's dictionary words (#313 out-of-scope, still true).
- Registering `EditDictionaryWord` for `/api/commands` or the gateway.
- Any change to the Angular resolve buttons: #305 bound them to the annotation permission already.

## Risks

- **The exempt-flag trap.** Extending the wrong base class, or a future refactor marking this
  command exempt, disables the gate silently rather than loudly. Locked decision 4 plus the
  independent-gating test are the guard.
- **`getProject()` is called before `execute()`.** A stale or bogus `projectId` surfaces as an
  authorization denial rather than a not-found. That is the intended reading — you cannot be a
  stakeholder on a project that is not there — but it does mean a data bug presents as a 403.
- **Fixture permissions.** `AnnotationCommandTest` runs as `admin`; the test passes today under
  #305's resolve gate, so `admin` holds `Annotation[Edit]` on those fixture projects and the nested
  gate will pass too. If a future fixture change strips creator grants the way `AuthorizationIT`
  does, these tests fail for a reason unrelated to the code.
- **Two `EditDictionaryWordCommandImpl` names in git history.** The move is a delete plus an add in
  a different module; `git log --follow` on the old path stops here.

## AC mapping

| AC | Commit | Proven by |
|---|---|---|
| 1 — refused without `Annotation[Edit]`, independently of the resolver | 2, 3, 4 | `AnnotationCommandTest` refused case + direct-execution case |
| 2 — refused or failed write leaves the issue unresolved | 2, 4 | `AnnotationCommandTest` refused and null-project cases |
| 3 — permitted user resolves end to end, word in the project dictionary | 2, 3, 4 | `AnnotationCommandTest` permitted case |
| 4 — no `projectId` writes nothing, in particular nothing installation-wide | 1, 2 | `AnnotationCommandTest` null-project case |
| 5 — stays out of the registry and the gateway allowlist | 1 | unchanged `GatewayPolicyConfig`; javadoc note |

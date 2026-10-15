# #398: rc6 acceptance fixes, plan

Issue: https://github.com/rreganjr/Requel/issues/398 · branch `398-rc6-fixes` · epic #375 · found by #378 on rc6 (Firefox and Safari).

## 1. Server errors show only a red outline

`applyCommandErrors` attaches a `{ server: message }` validator and revalidates with `emitEvent: false`, then marks the control touched. A control the user already edited was touched before, so nothing emits. `app-field` is OnPush and re-renders only when its control emits, so the message never appears. Its `aria-invalid` is refreshed in `ngAfterContentChecked` on every pass, which is the red outline.

Fix: `app-field` implements `ngDoCheck` and calls `markForCheck()` when what it would show (error shown, message, required marker) differs from what it last rendered. It covers every editor and every way a control's errors change without an event.

Test: `app-field.spec.ts`, a server error applied to an already-touched control shows its message. It fails without the fix.

## 2. Organization and email not marked required; a missing organization is "a username conflict"

`users.organization_id` is NOT NULL and `UserImpl.emailAddress` is `@NotEmpty`, but the forms don't mark either. With no organization, `EditUserCommandImpl` passes `null`, the insert fails, and `JpaUserRepository` maps every Hibernate constraint failure on a user to the username uniqueness message.

Fix:

- `EditUserInput.organizationName` is `@NotBlank(message = "organization is required.")` and `@FromEntityProperty("organization")`; `UserImpl.getOrganization()` is `@NotNull` with the same message. The violation lands on the `organizationName` field.
- Create User and Edit Account: `emailAddress` is `required` + `email`, `organizationName` is `notBlank()`, so both show the required marker and refuse to submit empty.

Tests: `AuthorizationIT` (a new user with a blank organization is a 422 on `organizationName`); the user-editor and edit-account specs require both fields.

## 3. Import drops user stakeholders' permissions

The export writes `<projectPermissions>` under each `<user-stakeholder>`; `StakeholderImportXml` didn't map it, so `StakeholderAssembler` created every user stakeholder with none. Only the importer and the assistant got permissions, from `addUserAsStakeholder`.

Fix: `StakeholderImportXml` maps the permissions; `StakeholderImportXmlMapper` turns them into permission keys (`entityType[permissionType]`) on `StakeholderImportDraft`; `StakeholderAssembler` grants the matching ones from `findAvailableStakeholderPermissions()` and logs and skips an unknown key. The importer and the assistant are handled as before.

Test: `ImportProjectStreamingCommandTest.importedUserStakeholdersKeepTheirPermissions`, admin keeps exactly the 29 permissions the sample gives it.

## 4. Names with a trailing space; LIKE in lookups by name

`EditGoal` saved "Trailing " as given. `findGoalByProjectOrDomainAndName` trims the name and matches with `LIKE`, which doesn't pad, so the goal could never be found to relate it. `LIKE` also made `_` and `%` wildcards.

Fix:

- Names are trimmed when set: `AbstractEditProjectOrDomainEntityCommand` (goals, actors, stories, use cases, scenarios and steps, glossary terms, stakeholders, reports), `EditProjectCommandImpl`, the `Copy*` commands' new names, and the import's project name.
- `JpaProjectRepository`'s lookups by name use `=`, through `singleResultByName`: the name as given, then trimmed. A name saved with spaces before this fix is still found, without a migration.
- `JpaUserRepository` matches usernames and organization names with `=`.

Tests: `GoalRelationCommandTest`, a goal named "Trailing " is saved trimmed and can be related either way; a goal saved as "Legacy " (set in the database) is still found; "a_b" and "axb" can be related.

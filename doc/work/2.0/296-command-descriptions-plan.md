# #296 Populate `@CommandDescription` across the gateway command catalog — implementation plan

Blocked on #271 (see locked decision 11).

## Summary

45 of the 52 allowlisted gateway write commands still reach an MCP client or CLI user as a
humanized name plus a field list. This ticket writes a caller-facing `@CommandDescription` for each
of them, and in the same PR fills `CommandDescriptor.authorizationHint`, which exists but is always
`null`. The hint is derived from each command's own `getAuthorizationRequirement()`, so it follows
the code, and the MCP tool description and CLI help both show it. A Spring IT against the real
catalog makes a missing description, a missing hint or an input-less allowlisted command fail the
build.

The read surface is in scope too: the 11 MCP read tools and the 7 CLI read commands get the same
caller-facing descriptions from one shared source, and every read-tool schema property gets a
description. #296 waits for #271, which changes what `draftAnnotation` and `EditIssue` do with
`severity`.

## Review against the tree (`release/2.0` @ `8fa39c91`)

**Count.** `GatewayPolicyConfig.ALLOWED` holds 52 commands. Seven are already described:
`EditTag`, `EditTagCategory` (#255), `EditScenario`, `ConvertStepToScenario` (#252),
`AddProjectDictionaryWord`, `DeleteProjectDictionaryWord` (#319) and `DeleteIgnoredFinding`
(#320). The issue body's "#255 populates it for `EditTagCategory` and `EditTag` only" is out of
date; 45 remain.

**Input types.** Every allowlisted command is registered with a record DTO. None is
`Void.class`, no two share a DTO, and all but one are named `<Command>Input`
(`SetPrimaryScenarioOnUseCase` uses `SetPrimaryScenarioInput`). The AC's `Void.class` exemption
covers nothing today.

**Factory methods.** Every allowlisted registration has a `factoryMethod`. The only
`registerWithBuilder` registration is `ResolveIssue`, which is on `DENIED`.

**Authorization requirements.** `AuthorizationRequirement` is a sealed interface with four
records. For most allowlisted commands `getAuthorizationRequirement()` returns the same value
whatever the input. Seven do not:

| Command(s) | Blank instance reports | Actual rule |
|---|---|---|
| `EditTag`, `DeleteTag`, `AssignTag`, `UnassignTag` (`AbstractTagCommand`), `EditTagCategory`, `DeleteTagCategory` (`AbstractTagCategoryCommand`) | system administrator (no project) | `Annotation[Edit]` / `Annotation[Delete]` for a project tag; system administrator for a global tag |
| `EditProject` | `null` | creating: the `createProjects` role permission, checked in `execute()`; editing: `Project[Edit]` |

**Consumers of the hint.** `GatewayCommandController` already sends it in its JSON and
`gateway-rest-client`'s `CommandInfo` already reads it. Neither `McpWriteService.describe()` nor
the CLI's `TypedCommands.describe()` shows it.

**Existing tests.** `GatewayCommandCatalogImplTest` mocks `ApiCommandFactory`, so it never sees a
real DTO and cannot catch a missing description. The catalog skips an allowlisted command that is
not registered, without saying so. `McpToolCatalogLockstepIT` already runs against the real wired
catalog and `McpWriteService`, and is where the new assertions go.

**Double period.** `McpWriteService.describe()` returns `base + "."`. Every existing description
ends in a period, so MCP clients see `…what was stored.. Input fields: …`.
`McpWriteServiceSchemaTest` only asserts `startsWith`/`contains`, so it misses this.

**Stale comments.** The javadoc on `CommandDescription` ("Populating the rest of the catalog is
tracked separately"), on `GatewayCommandCatalogImpl.describe()` ("Null is the normal case for most
of the catalog today") and on `GatewayCommandCatalogImplTest.anUndescribedCommandLeavesTheDescriptionNull`
("most of the catalog still relies on that") are wrong once this lands.

**The read surface.** It has no catalog and no annotation. Its descriptions are inline strings:

- `McpReadService.listTools()` builds 11 read tools with one-line descriptions: `listProjects`,
  `getProject`, `getProjectTree`, `getGlossary`, `getOpenIssues`, `getAnnotations`, `getEntity`,
  `getEntityNeighbors`, `searchProjectEntities`, `getProjectContext`, `draftAnnotation`.
- `requel-cli` has 7 read commands, each described separately in its picocli `@Command`:
  `projects`, `project`, `glossary`, `open-issues`, `entity`, `search`, `context`. They say much
  the same as the MCP tools in different words.
- `upsertGoal` (MCP) and `upsert-goal` (CLI) are hand-written and already detailed.

Gaps a caller meets:

- No property in `projectNameSchema()`, `entityRefSchema()`, `searchSchema()` or
  `draftAnnotationSchema()` has a `description`.
- `entityType` is a free string. `InProcessQueryGateway.getEntity` accepts exactly Goal, Story,
  Actor, UseCase, Scenario and GlossaryTerm and throws on anything else. Only `getEntity`'s
  description lists them. `getAnnotations` passes the type to `AnnotationQueryController`, so its
  accepted set may differ.
- `draftAnnotation` says "the caller submits it for application" without naming how.

No open ticket covers read descriptions. Two open v2.0 tickets add read tools: #274 (project
content read) and #72 (find-best-match read tool).

**#271 (open, v2.0).** `draftAnnotation` accepts and schema-declares `severity`, and persisting
drops it because `Issue` has no severity. #271 adds severity to `Issue`, exposes it on `EditIssue`
and orders open issues by it. The `draftAnnotation`, `EditIssue`, `getOpenIssues` and
`getProjectContext` descriptions depend on that outcome.

## Locked decisions

1. **Milestone** stays v2.0. MCP support is part of 2.0, and agents working over MCP are the
   callers most likely to get the partial-update rule and the refusals wrong.
2. **Stricter AC in place of the `Void.class` exemption:** every allowlisted command has a record
   input DTO. #252 removed the input-less step commands from the allowlist because an empty
   schema made them uncallable.
3. **Shared wording lives in constants.** The #316 partial-update rule is one sentence in
   `CommandDescriptions` (new, `service-api`), concatenated into each annotation that needs it.
   Annotation values accept compile-time constant concatenation. Each MCP tool description is read
   on its own, so the sentence is repeated in each one rather than stated once in the server
   instructions.
4. **Fill `authorizationHint`, derived from the code.** The catalog creates each allowlisted
   command once, with no input, and renders its requirement. A new optional
   `@CommandDescription.authorization` element overrides the derived text for the seven
   input-dependent commands.
5. **Permissions leave the prose.** Descriptions stop saying "Requires Project Edit." and similar
   (`AddProjectDictionaryWord`, `DeleteProjectDictionaryWord`, `DeleteIgnoredFinding` today); the
   hint says it.
6. **The test is a Spring IT in `McpToolCatalogLockstepIT`.** It reuses that class's cached
   context, so CI gets no extra context.
7. **Fold in** the double-period fix and the three stale comments.
8. **The read surface is in scope.** It covers the 11 MCP read tools, the 7 CLI read commands and
   every read-tool schema property. It excludes the Angular-facing REST query endpoints, which no
   external caller reads descriptions from.
9. **Read text has one source.** A `QueryDescriptions` constants class goes in `gateway-api`,
   which both `mcp-server` and `requel-cli` depend on. The MCP tools and the CLI help use the same
   sentences.
10. **No authorization hint for reads.** Reads don't go through `AuthorizableCommand`, and
    `McpToolDescriptor` has no hint field. Where visibility matters, a sentence says so, e.g.
    only projects you are a stakeholder on.
11. **#271 lands first.** The plan is committed on the branch now; implementation starts after
    #271 merges, and the severity-related text is written against that behavior.

## Contracts

**`@CommandDescription`**

```java
public @interface CommandDescription {
    /** The description, written for a caller. */
    String value();
    /**
     * Overrides the authorization hint the catalog derives from the command, for a command whose
     * requirement depends on its input. Empty means derive it.
     */
    String authorization() default "";
}
```

**`CommandDescriptions`** (final class, `com.rreganjr.requel.service.api`). Draft wording; the
exact text is checked against the #316 implementation before use:

```java
public static final String PARTIAL_UPDATE = " When editing, a field you leave null or omit keeps"
        + " its current value and an empty string clears it.";
```

**Hint text** (`AuthorizationHints.render(AuthorizationRequirement)` in `service-impl`, an
exhaustive `switch` over the sealed interface, so a fifth record fails the compile). The format
follows the `CommandDescriptor` javadoc's `Goal[Edit]` example:

| Requirement | Hint |
|---|---|
| `RequiresStakeholderPermission(Goal, "Edit")` | `Goal[Edit]` |
| `RequiresStakeholderPermissionOrSystemAdminRole(Project, "Delete", SystemAdminUserRole)` | `Project[Delete] or system administrator` |
| `RequiresSystemRole(SystemAdminUserRole)` | `system administrator` |
| `RequiresRolePermission("createProjects")` | `createProjects role permission` |
| `null` | `null` |

`SystemAdminUserRole` lives in `user-jpa`, so the role is named from `roleType.getSimpleName()`:
`SystemAdminUserRole` becomes `system administrator`, and any other role falls back to its simple
name.

**Overrides** (draft):

- Tag commands: `Annotation[Edit] for a project tag; system administrator for a global tag` (use
  `Delete` for the two delete commands).
- `EditProject`: `createProjects role permission to create; Project[Edit] to edit`.

**Rendering to callers**

- MCP: `<description><.> Requires: <hint>. Input fields: …` A period is added after the
  description only when it doesn't already end in `.`, `!` or `?`. The `Requires:` sentence is
  left out when the hint is null.
- CLI: `TypedCommands.describe()` adds a `Requires: <hint>` line after the description.

**`QueryDescriptions`** (final class, `com.rreganjr.requel.gateway`): one constant per read
tool (`LIST_PROJECTS`, `GET_PROJECT`, …) plus shared property descriptions (`PROJECT_NAME`,
`ENTITY_TYPE`, `ENTITY_ID`, `SEARCH_QUERY`). `McpReadService` uses them for tool and property
descriptions. The CLI commands use them in `@Command(description = …)` and
`@Parameters(description = …)`; picocli annotation attributes accept constants.

**Read-tool schemas.** Each property gets a `description`. `entityType` gets an `enum` wherever
the accepted set is fixed. The set comes from the code, not from this plan.

## Steps

1. **`service-api`:** add `authorization()` to `CommandDescription` and fix its javadoc. Add
   `CommandDescriptions` with `PARTIAL_UPDATE`.
2. **`service-impl`:** add `AuthorizationHints.render(...)`.
3. **`GatewayCommandCatalogImpl`:**
   - Build the descriptor list lazily and once, on the first `descriptors()`/`find()` call,
     instead of in the constructor. Creating a command calls
     `ApplicationContextCommandFactoryStrategy.createBean`, and doing that inside a singleton's
     constructor, while the context is still starting, invites ordering problems and circular
     references.
   - For each command, the hint is the input type's `authorization()` when it is non-empty.
     Otherwise it is `render(((AuthorizableCommand) registry.lookup(type).factoryMethod().get()).getAuthorizationRequirement())`.
   - A failure to create the command leaves the hint `null` and logs a warning; the IT turns that
     into a failure.
   - Fix the stale javadoc on `describe()`.
4. **`McpWriteService.describe()`:** add the punctuation check and the `Requires:` sentence.
5. **`requel-cli` `TypedCommands.describe()`:** add the `Requires:` line.
6. **Write the 45 descriptions** (list below). Each is written from the command implementation
   and its input applicator in the registrar, not from this plan. Each one says:
   - what the command does to state;
   - what the id and name fields select, and what a null id does;
   - anything transformed on write, and anything refused rather than resolved;
   - `+ CommandDescriptions.PARTIAL_UPDATE` where the #316 contract applies.
   Keep each to a few sentences and don't restate the field list.
7. **Existing seven:** remove the permission sentences (decision 5). Add `PARTIAL_UPDATE` to
   `EditScenario` if its implementation follows #316. Add the `authorization` override to
   `EditTag` and `EditTagCategory`.
8. **`gateway-api`:** add `QueryDescriptions`.
9. **`McpReadService`:** replace the 11 inline descriptions with the constants, add property
   descriptions to the four schema helpers, and add the `entityType` enum where it is fixed. Say
   in `draftAnnotation` how a draft is applied, and describe `severity` as #271 leaves it.
10. **`requel-cli`:** point the 7 read commands' `@Command`/`@Parameters` descriptions at the same
    constants. Review `upsert-goal` and `upsertGoal` for accuracy only.
11. **Tests** (below), and fix the stale comment on
    `anUndescribedCommandLeavesTheDescriptionNull`.
12. **`tmp/296-verify.sh`:** the gate.

### The 45 commands, with what is already known to need saying

Verify each note against the code before writing it.

- **Project:** `EditProject` (null id creates, which needs `createProjects`), `DeleteProject`
  (removes the project and everything in it; also allowed for a system administrator who is not a
  stakeholder).
- **Stakeholders:** `EditNonUserStakeholder` (non-user stakeholders only; user stakeholders are
  managed in the UI), `DeleteStakeholder` (the gateway refuses a user stakeholder,
  `NonUserStakeholderDeletePolicy`).
- **Goals:** `EditGoal`, `EditGoalRelation`, `DeleteGoalRelation`, `CopyGoal` (what the copy is
  named when `newGoalName` is null), `DeleteGoal`, `AddGoalToGoalContainer`,
  `RemoveGoalFromGoalContainer` (what counts as a container).
- **Stories:** `EditStory`, `CopyStory`, `DeleteStory`, `AddStoryToStoryContainer`,
  `RemoveStoryFromStoryContainer`.
- **Actors:** `EditActor`, `CopyActor`, `DeleteActor`, `AddActorToActorContainer`,
  `RemoveActorFromActorContainer`.
- **Use cases:**
  - `EditUseCase` (#325: the primary actor is required and can't be cleared; a rename renames the
    primary scenario only if its name matched the old use-case name).
  - `CopyUseCase`, `DeleteUseCase`, `AddScenarioToUseCase`, `RemoveScenarioFromUseCase`.
  - `SetPrimaryScenarioOnUseCase` (whether the scenario must already belong to the use case).
- **Scenarios:** `CopyScenario`, `DeleteScenario`.
- **Glossary:** `EditGlossaryTerm`, `DeleteGlossaryTerm`.
- **Reports:** `EditReportGenerator` (definition only; generating a report isn't exposed),
  `DeleteReportGenerator`.
- **Annotations / IBIS:**
  - `EditNote`, `DeleteNote`, `EditIssue`, `DeleteIssue`, `DeletePosition`, `EditArgument`,
    `DeleteArgument`.
  - `EditPosition` (#284/#281): creating a position whose text already exists in the grouping
    object links the existing one; renaming onto another position's text is refused and the
    refusal names its id; existing duplicates are merged when met, lowest id kept.
- **Tagging:** `DeleteTag`, `AssignTag`, `UnassignTag`, `DeleteTagCategory` (global versus project
  tags, via the override).

## Test plan

**`McpToolCatalogLockstepIT`** (existing class and context):

- `everyAllowedCommandIsRegisteredAndCatalogued`: each `ALLOWED` entry is registered and
  `catalog.find(type)` is present. This closes the gap left by the catalog skipping unregistered
  commands.
- `everyAllowedCommandHasARecordInput` (decision 2): the input type is a record, not
  `Void.class`. The message names the command and points at #252.
- `everyAllowedCommandIsDescribed`: description non-blank, named in the failure message.
- `everyAllowedCommandCarriesAnAuthorizationHint`: hint non-null. Pins `EditGoal` →
  `Goal[Edit]`, `DeleteProject` → `Project[Delete] or system administrator`, and `EditTag` →
  contains `system administrator` (the override wins).
- `mcpToolDescriptionsAreWellFormed`: no `..` in any typed tool description, and each one contains
  `Requires:`.

**Unit**

- `AuthorizationHintsTest`: one case per record type, plus `null`, plus a role other than
  `SystemAdminUserRole` falling back to its simple name.
- `GatewayCommandCatalogImplTest`:
  - Update the mocks for lazy building and `registry.lookup(...)`.
  - New `theOverrideWinsOverTheDerivedHint` and `theHintIsDerivedFromTheCommand` (stub
    registration whose factory returns an `AuthorizableCommand`).
  - New `aCommandThatCannotBeCreatedLeavesTheHintNull`.
- `McpWriteServiceSchemaTest`:
  - `aDescriptionEndingInAPeriodGetsNoSecondPeriod`.
  - `theHintIsRenderedAfterTheDescription`.
  - `aNullHintIsLeftOut`.
  - Update the `Ping.` case if needed.
- `TypedCommandsTest`: the usage message has a `Requires:` line when the hint is set, and none when
  it is null.
- `McpReadServiceTest` (new cases), which catches a new read tool from #274 or #72 shipped
  without text:
  - `everyReadToolIsDescribed`: every tool from `listTools()` has a non-blank description.
  - `everyReadToolPropertyIsDescribed`: every property in every read tool's input schema has a
    non-blank `description`.
  - `entityTypeIsAnEnumOfTheSupportedTypes`: pins the enum to the types `InProcessQueryGateway`
    accepts.
- CLI: one `QueryCommandsTest` case asserting a read command's usage text comes from
  `QueryDescriptions`.

**Gate:** `mvn clean verify`. No `requel-angular/**` changes. No routing or UI changes, so e2e is
not required beyond CI's normal run.

## Out of scope

- The Angular-facing REST query endpoints.
- An authorization hint for reads (decision 10).
- Per-field descriptions in the JSON schema.
- Enforcement: the hint is informational, and authorization stays in `AuthorizingCommandHandler`.
- `ResolveIssue` over the gateway (a separate 2.x ticket).
- Making `Edit*Input.name` required only on create (a separate 2.x ticket).

## Risks

- **Descriptions go stale as behavior changes.** They sit on the DTO beside the fields. The IT
  pins presence, not content, so keep claims to behavior a caller can see.
- **Creating commands for the hint.** Prototype command beans are autowired but not executed. Lazy
  building keeps this out of context startup. A command whose constructor does work shows up as a
  null hint and a failing IT, not a broken context.
- **Override drift.** Seven hand-written hints can drift from `AbstractTagCommand` and
  `EditProjectCommandImpl`. Each override sits next to prose that already describes the same rule.
- **Description length.** MCP clients show these in tool pickers, so a few sentences each.
- **#271 ordering.** If #271 changes more than severity (e.g. how `getOpenIssues` orders
  results), the read descriptions follow it; that is why this ticket waits.

## AC mapping

| AC | Covered by |
|---|---|
| Every allowlisted command carries a `@CommandDescription` | step 6–7; `everyAllowedCommandIsDescribed` |
| A test fails the build for an undescribed allowlisted command | `everyAllowedCommandIsDescribed` (real catalog, real DTOs) |
| ~~`Void.class` inputs exempt~~ → every allowlisted command has a record input | `everyAllowedCommandHasARecordInput` |
| (added) Every allowlisted command carries an authorization hint, shown by MCP and CLI | steps 2–5; `everyAllowedCommandCarriesAnAuthorizationHint`, schema and CLI tests |
| (added) Every MCP read tool and each of its input properties is described; CLI read commands use the same text | steps 8–10; `everyReadToolIsDescribed`, `everyReadToolPropertyIsDescribed`, CLI test |

# #264 Project-authored definitions — implementation plan

Child 6 of epic #258, and the last one built. Review: 2026-10-04 against `release/2.0` @
`e80c57de` (after #266).

## Summary

Project members holding a new permission can author the assistant definitions their project
runs. They can:

- fork a bundled definition and edit the copy;
- revert the copy back to the bundled one;
- create a brand-new definition, such as a project's own policy;
- delete a definition the project created.

Each write is a command on the existing authorizing and auditing chain, with optimistic locking.
The same validation that guards bundled definitions runs on every write, plus rules that only
apply to a project author. Reads are gated by the same permission. The UI is a definitions page
linked from the overview's Assistants panel. Enable and disable stay on #268's switch.

## Review against the tree

See the issue's "What exists today". The points that shape the plan:

- **Storage.**
  - `AssistantDefinitionStore.save` validates against neighbours, upserts by (key, project) and
    evicts the cache.
  - A project row with a bundled key overrides the bundled definition for that project.
  - There's no single-row delete and no lock column.
- **Validation.**
  - `AssistantDefinitionValidator` covers fields, kind pairing, scope, providers, budgets,
    vocabulary, schema, instruction length and collisions.
  - It reports plain strings (`InvalidAssistantDefinitionException.problems()`).
  - `executorBean` is unrestricted.
- **Commands.**
  - Command impls live in project-jpa and reach assistant code through project-domain SPIs.
    `ProjectAssistantDefinitions` exists (only `deleteForProject`), and #268's
    `ProjectAssistantSettingsStore` is the model.
  - Edits carry `version` and compare it in the command (#108 `expectedVersion` →
    `EntityLockException` → 409).
  - `EntityValidationException` maps to 422 `FieldViolation`s.
- **Catalog.**
  - `SwitchableAssistantCatalog` and `AnnotationSources` are install-wide: bean assistants plus
    bundled definitions.
  - `EditProjectAssistantSetting` refuses an id the catalog doesn't list. As things stand, a
    project-only definition couldn't be switched.
- **Permissions.**
  - `stakeholder_permissions.entity_type` is a varchar and seeded by
    `StakeholderPermissionsInitializer`.
  - #240 backfilled `Project[Delete]` to `Project[Edit]` holders in the same initializer.
  - The stakeholder editor's permission grid lists whatever entity types the server returns.

## Locked decisions (2026-10-04)

1. **Permission: `AssistantDefinition[Edit]`.**
   - There's no `StakeholderPermissionType` change, so no MySQL enum migration.
   - Project creators get it.
   - Existing `Project[Edit]` holders are backfilled.
   - It gates reads and writes.
2. **A denial is a 403**, like every other unauthorized command. No audit row (none is written
   today).
3. **Enable and disable use #268's switch.** There are no definition enable/disable commands. The
   definition's own `enabled` stays install-wide; projects never write it.
4. **Authorship covers forks and brand-new definitions.**
5. **One ticket, one PR.**

## Contracts

### Permission

- **Marker interface `com.rreganjr.requel.project.AssistantDefinitionPermission`** in
  project-domain. Its simple name is `AssistantDefinition`, the entity-type string. (A marker
  interface, because `RequiresStakeholderPermission` takes a `Class`.)
- **`StakeholderPermissionsInitializer`:**
  - adds `AssistantDefinition[Edit]`;
  - `backfillAssistantDefinitionPermission()` grants it to every `UserStakeholder` with
    `Project[Edit]`, idempotently, as #240 did.
- **Project creation** needs no change. `EditProjectCommandImpl` grants a creator every
  `findAvailableStakeholderPermissions()`, so the new row is included once seeded. Import and
  `RepairProjectStakeholders` likewise.
- **No `SystemAdmin` bypass.** The commands use `RequiresStakeholderPermission`, like the
  `Project[Edit]` commands. An administrator needs the permission on the project.
- **Angular.** The stakeholder permission grid shows the row as "AI definitions", Edit only. A
  hint under the grid says the risk (see Risks).

### SPI: `ProjectAssistantDefinitions` (project-domain), widened

```java
int deleteForProject(Long projectId);                        // existing
List<View> effective(Long projectId);                        // bundled + project, per key
Optional<View> find(Long projectId, String key);
Optional<View> bundled(String key);
View create(Long projectId, Draft draft, String by);
View edit(Long projectId, String key, int lockVersion, Draft draft, String by);
View fork(Long projectId, String key, String by);
void revert(Long projectId, String key, int lockVersion);  // delete the override
void delete(Long projectId, String key, int lockVersion);  // project-only key
```

- `ProjectAssistantDefinitions.Draft` and `View` are plain records nested in the SPI. They carry
  the editable fields, plus kind, task type, source, `forkedFromVersion`, `bundledVersion`,
  `lockVersion` and `inEffectFor`.
- `AssistantDefinitionStore` implements the SPI. The view mapping lives there, so project-domain
  never sees `AssistantDefinition`.
- A rejected write throws `InvalidDefinitionException(List<Problem(field, message)>)`, a
  project-domain exception that `InvalidAssistantDefinitionException` now extends. The command
  rethrows it as `BeanValidationException` with property names and messages, so the existing 422
  mapping routes errors to fields.
- The commands extend `AbstractCommand` (the API's command factory requires
  `CommandMetadataAware`) and are denied on the command gateway: prompt text is authored in the
  app, not by an external client.

### Storage

- **V37 `assistant_definitions.lock_version INT NOT NULL DEFAULT 0`,** with `@Version` on the
  entity. `definition_version` stays the content version that bundled seeding compares.
  - A project edit increments both. Each saved edit is a new content version, recorded on the run.
  - Fork sets `definition_version = 1` and `forked_from_version = bundled.version`.
- **`AssistantDefinitionRepository.deleteByDefinitionKeyAndProjectId`** for revert and delete.
  Both evict the project's cache.
- **Edit and fork never change the key** of a bundled-key row. Revert deletes the row, so the
  bundled row is back exactly with nothing copied.

### Validation (on every SPI write)

- **Field attribution.** The validator's problems carry a field: `key`, `displayName`, `scope`,
  `contextProviders`, `contextBudgets`, `vocabulary`, `instructions`, `outputSchema`,
  `executorBean`, `kind`, `taskType`.
  - `InvalidAssistantDefinitionException` keeps `problems()` as strings for the existing callers
    (seeding, the dev override) and adds `fieldProblems()`.
- **Project-only rules,** checked in the store's project write path:
  - `executorBean` must be null. A draft that sets it is rejected (`executorBean`).
  - `kind`, `taskType` and output schema come from the kind and can't be edited.
    - A draft names a kind only on create: REVIEW, POLICY or CORPUS.
    - The task type and schema follow from the kind: REVIEW → `REQUIREMENTS_REVIEW` /
      `RequirementsReviewOutput` v2; POLICY and CORPUS as paired by #265 and #266.
    - Any attempt to change the task type or schema is rejected (`outputSchema` / `taskType`).
  - A new key must match `^[a-z][a-z0-9-]{2,79}$`. It must not be a bean assistant's id or a
    bundled key (forking is how a bundled key is customised). Problem field: `key`.
  - The project's definitions must stay within a ceiling (`requel.ai.definitions.max-per-project`,
    default 50), so a project can't make every run arbitrarily expensive.
- **Unchanged:** collisions, instruction length against `maxInputTokens`, vocabulary,
  providers and budgets.

### Commands (project-jpa, on the command endpoint)

| Command | Input | Requirement |
|---|---|---|
| `CreateAssistantDefinition` | `projectName, kind, key, displayName, scope, contextProviders, contextBudgets, instructions, vocabulary, localOnly` | `AssistantDefinition[Edit]` |
| `EditAssistantDefinition` | `projectName, key, version,` the editable fields | same |
| `ForkAssistantDefinition` | `projectName, key` | same |
| `RevertAssistantDefinition` | `projectName, key, version` | same |
| `DeleteAssistantDefinition` | `projectName, key, version` | same |

- Each command extends `AbstractProjectCommand` and implements `AuthorizableCommand`. Each is
  registered in `ProjectCommandRegistrar` with an input DTO in service-api, and the result is the
  definition view.
- **A stale `version`** → `EntityLockException` → 409, the standard optimistic-lock error.
- **SSE.** `Project:<id>` change event, so an open definitions page reloads.

### Catalog and labels (project-aware)

- **`SwitchableAssistantCatalog`** gains `switchableAssistants(Long projectId)` and
  `describe(String id, Long projectId)`. The defaults fall back to the install-wide methods.
  - The registry adds the project's definitions, with a fork's display name.
  - `GET /api/projects/{name}/assistants` and `EditProjectAssistantSetting` use the project-aware
    forms, so a project-only definition gets a switch.
- **`AnnotationSources.kind/name(source, projectId)`.** `toIssueDto` takes the project from the
  first annotatable entity, so a project definition's issues show its name and kind.
- **Revert or delete leaves stale switch rows.** `project_assistant_settings` rows for a deleted
  key are harmless (no catalog entry) and are deleted with it.

### Reads (service-impl `ProjectQueryController`)

- **`GET /api/projects/{name}/definitions`** → `ProjectDefinitionDto[]`, grouped by task type:
  - key, display name, kind, task type, scope;
  - source (BUNDLED or PROJECT), version, `forkedFromVersion`, `bundledVersion` (when the key is
    bundled; `bundledVersion > forkedFromVersion` means "bundled has a newer version");
  - enabled (the #268 switch);
  - `inEffectFor` (the entity types or set kinds it covers after specific-beats-fallback);
  - `lockVersion`.
- **`GET /api/projects/{name}/definitions/{key}`** → the full definition: instructions,
  vocabulary, providers, budgets, local-only, and its bundled baseline when the key is bundled.
- **Both need project access plus `AssistantDefinition[Edit]`.** A denial is a 403, as the other
  project reads do.
- **`RunView` / `AiReviewDto` gain `definitionSources`** (from `assistant_runs.template_source`).

### Angular

- **Routes:** `projects/:name/definitions` (list) and `projects/:name/definitions/:key` (editor),
  following the dictionary page (#319). The Assistants panel links "Manage definitions" when
  `canEdit('AssistantDefinition')`.
- **List page.** Grouped by kind (AI review, Policies, Corpus analysis). Each row shows:
  - display name, source badge, version, and "bundled v<n> is newer";
  - what it's in effect for;
  - actions: Customize (fork) for a bundled one; Edit, Revert or Delete for a project one;
    "New definition".
- **Editor.**
  - **Fields:** display name; scope (multi-select of entity types, or set kinds for CORPUS);
    context providers (per kind); budgets; instructions (textarea with a character count against
    the cap); vocabulary (type, description and category rows); local-only.
  - For a fork, the bundled baseline is read-only beside it.
  - 422 field errors land on their controls. A 409 says "changed by someone else, reload".
- **The risk note** goes on the editor and under the permission grid.
- **Without the permission,** the link is hidden and a direct visit shows "You don't have
  permission to manage AI definitions" (403).

## Test plan

- **assistant-core unit:**
  - validator field attribution;
  - project-only rules (`executorBean`, key format, reserved keys, kind fixed, the per-project
    ceiling);
  - store create, edit, fork, revert and delete, with cache eviction;
  - the lock version on a stale edit;
  - the project-aware catalog and describe.
- **project-jpa / requel-app unit:**
  - each command's requirement, input validation and lock check;
  - initializer backfill (idempotent).
- **requel-app ITs** (`ProjectDefinitionsIT`):
  - a project definition changes only that project's run;
  - fork records `forkedFromVersion`, and revert restores the bundled row exactly;
  - each AC rejection returns 422 with its field;
  - a stale version → 409;
  - without the permission, reads and writes → 403;
  - an upgraded install gives `Project[Edit]` holders the permission;
  - a project-only definition has a switch and its issue DTO shows its name and kind;
  - a definition that induces malformed output fails the run with zero annotations;
  - the run read shows key, version and source.
- **service-impl unit:** read DTO mapping and the source label with a project.
- **Angular component tests:** list, editor (view, edit, fork, revert, create, delete), field
  errors, 409, permission-denied, and the panel link.
- **Coverage:** every new file at 75% patch coverage or above from unit tests. CI's patch report
  doesn't count ITs reliably (seen on #266).

## Build order

1. Permission marker, and the initializer's seed and backfill. Unit tests.
2. V37 lock column, repository delete, and validator field attribution with the project-only rules.
3. Widened SPI and store implementation. Unit tests.
4. Commands, registrar, input DTOs and error mapping. Unit tests.
5. Project-aware catalog, switch command, and `AnnotationSources` with a project.
6. Read endpoints, DTOs, and `definitionSources` on the run read.
7. ITs.
8. Angular: service, models, list page, editor, panel link, permission-grid label and hint. Specs.
9. Docs: `AI_ASSISTANT_SETUP.md` (authoring, the permission, the risk).

## Out of scope

- Organization-level definitions.
- Carrying definitions in project XML export/import.
- Version history or diffs beyond project vs bundled.
- User-level preferences.
- Auditing denied commands.
- Merging a newer bundled version into a fork. The UI says it's newer, and the author reverts and
  re-forks.

## Risks

- **Prompt text a project member controls.** A definition can tell the model anything. What it
  can't do:
  - change the output envelope (fixed schema, validated by Requel);
  - write outside the run's target or participants (the applicator);
  - name a bean (`executorBean` rejected);
  - send more than the input cap;
  - reach a remote provider when the project's data-handling settings forbid it (#262).

  The worst case is misleading issues in that project. The note says so where the permission is
  granted.
- **Cost.** More definitions mean more provider calls per review. The per-project ceiling and the
  #268 switches bound it.
- **Fork drift.** A fork doesn't follow bundled upgrades. "Newer" is shown; merging isn't offered.
- **Identity per key.** A project-only key gets an `assistant-<key>` identity. Two projects that
  pick the same new key share that user, which is a stakeholder of each. That's acceptable, but
  documented.

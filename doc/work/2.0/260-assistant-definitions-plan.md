# #260 Assistant definitions: model, storage, per-project registry — implementation plan

Issue: https://github.com/rreganjr/Requel/issues/260 (child 2 of epic #258, fourth in its build order)
Branch: `260-assistant-definitions`, cut from `release/2.0` @ `c338936d`
Builds on: #259 (`cli` provider), #262 (redaction, pseudonyms), #268 (per-project switch store),
#270 (machine-authored rules), #302 (assistant stakeholder), #355 (evaluation baseline)

## Summary

The AI review stops being a hard-coded bean. Its prompt, scope, vocabulary and schema become an
`AssistantDefinition` row, seeded from a bundled JSON file, and one generic executor runs whichever
definitions match the entity. The registry merges them with the existing bean assistants (lexical,
step structure), so nothing else changes. With only the seeded default, the prompt is byte-for-byte
today's, and the #355 baseline is reproduced. Each assistant also gets its own user identity,
marked by a new `AssistantUserRole`, so lexical and AI annotations are told apart.

## Review against the tree (`release/2.0` @ `c338936d`)

1. **Run columns exist.** `assistant_runs.template_id` / `template_version` / `template_source` (V8)
   are never written; they take the definition key, version and source. No new run column for that.
2. **The key must stay `ai-requirements-review`.** `RequirementsReviewAssistant.actionKey` embeds the
   assistant id in every finding's idempotency key.
3. **The collision rule as written breaks #263's fallback** (the seeded default has empty scope).
4. **Dropping unverified evidence would change the baseline.** Kept as a warning here.
5. **No provider registry yet.** The review builds one pack with `EntityContextPackBuilder`.
6. **#268's store and catalog already exist.** `SimpleAssistantRegistry` is the
   `SwitchableAssistantCatalog`; `projectSwitchable()` puts an assistant on the overview toggles.
7. **One assistant user today.** `User.isAssistant` compares the username with `assistant`.
   `AssistantUserInitializer` creates it with the password `assistant` and `ProjectUserRole`, so it
   can log in (see "Fold-in to confirm").
8. **Roles support a marker interface.** `UserImpl.getRoleForType` uses `isAssignableFrom`, so
   `User.isAssistant` in platform-identity can test a marker `AssistantRole` interface that
   `AssistantUserRole` (user-jpa) implements.
9. **`RequirementsReviewAssistant`** is a 486-line `@Component` bean; only its test, `AiProperties`
   (a comment) and `AiReviewService` (`TASK_TYPE`) reference it.

## Locked decisions (2026-10-01)

- **Collision:** per task type, at most one *specific* definition per entity type and at most one
  empty-scope definition. Empty scope is the fallback, used only where no specific one matches.
- **Evidence:** verify, keep the finding, count the miss on the run. #263 switches to dropping.
- **On/off:** `enabled` is install-wide; per project through #268's store keyed by definition key,
  so bundled AI definitions show up as overview toggles.
- **Context providers:** fixed set `{entity}` until #261.
- **Kind:** `REVIEW | POLICY` stored; `POLICY` rejected by validation until #265.
- **Storage:** one `assistant_definitions` table (V31) with JSON text columns for scope, context
  providers and vocabulary.
- **Identity marker:** `AssistantUserRole`; the existing `assistant` user gets it.
- **Pseudonyms:** every assistant identity is `assistant` in context packs.
- **Extra AC:** re-run `score.py` and match the re-scored baseline within noise.

## Contracts

### `assistant_definitions` (V31)

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT auto | |
| `definition_key` | VARCHAR(120) | unique with `project_id` (null = bundled) |
| `display_name` | VARCHAR(200) | |
| `kind` | VARCHAR(20) | `REVIEW` / `POLICY` |
| `task_type` | VARCHAR(80) | e.g. `REQUIREMENTS_REVIEW` |
| `scope_json` | TEXT | `["Goal","Story"]`; `[]` = fallback |
| `context_providers_json` | TEXT | `["entity"]` |
| `instructions` | TEXT | the prompt |
| `vocabulary_json` | TEXT | `[{"type":"AMBIGUOUS","description":"…"}]` |
| `output_schema_name` / `_version` | VARCHAR | allowed set: `RequirementsReviewOutput` v1 |
| `enabled` | BOOLEAN | install-wide switch |
| `definition_version` | INT | bumped on change |
| `source` | VARCHAR(20) | `BUNDLED` / `PROJECT` |
| `project_id` | BIGINT NULL | owner of a project definition |
| `forked_from_version` | INT NULL | bundled version a project copy came from |
| `executor_bean` | VARCHAR(200) NULL | escape hatch |
| `created_at` / `updated_at` / `created_by` / `updated_by` | | audit |

V31 also adds `assistant_runs.evidence_unverified INT NOT NULL DEFAULT 0`.

### Bundled seed — `assistant-ai/src/main/resources/ai/definitions/requirements-review.json`

Key `ai-requirements-review`, version 1, `REVIEW`, task `REQUIREMENTS_REVIEW`, scope `[]`, providers
`["entity"]`, schema `RequirementsReviewOutput` v1, the nine finding types with one-line
descriptions, and `instructions` = today's `TASK_INSTRUCTIONS` exactly. `DefinitionSeeder` (a
system initializer) validates every bundled file and fails startup on any error, then inserts a
missing row or updates a `BUNDLED` row whose stored version is lower. It never touches `PROJECT`
rows.

### Store and cache — `AssistantDefinitionStore` (assistant-core)

`definitionsFor(Long projectId, String taskType)` = enabled bundled rows + that project's rows (a
project row with the same key overrides the bundled one). Cached per project (bundled set cached
once); `save(...)` validates then evicts. Repository-level only; no REST.

### Validation — `AssistantDefinitionValidator`

Known entity types (the reviewable set: Goal, Story, Actor, UseCase, Scenario, Step), provider ids in
`{entity}`, kind `REVIEW`, non-empty vocabulary with unique types, schema in the allowed set,
instructions within `maxInputTokens * 4` characters, non-blank key and task type, and the collision
rule across the definitions it would sit beside.

### Executor — `DefinitionExecutorAssistant` (assistant-ai)

Today's `RequirementsReviewAssistant` body, parameterised by a resolved definition: `assistantId()`
= key; `targetType()` = `TextEntity`; `handlesTask` = the definition's task type; instructions,
schema and vocabulary from the definition. The output schema is loaded once by name/version.
`RequirementsReviewAssistant` is deleted; its tests move to `DefinitionExecutorAssistantTest`.
`AiReviewService` keeps its own `TASK_TYPE`.

Evidence check: each `evidenceReferences` string, whitespace-normalised and case-sensitive, must
occur in the target's name or text (also normalised). A miss leaves the finding as it is and adds a
`definition.evidenceUnverified` count to the result metadata. The worker writes the run's total to
`evidence_unverified`; the #355 read returns it, and `score.py` reports it.

Traceability: the request `attributes` carry `definitionKey` / `definitionVersion` /
`definitionSource`; each issue and note action's metadata carries the same two keys; the worker
writes the definitions that ran to `template_id` (keys, comma-joined), `template_version` and
`template_source`.

### Registry — `DefinitionAwareAssistantRegistry` (assistant-core, replaces the `@Component` on
`SimpleAssistantRegistry`)

```
findAssistantsFor(target, context):
  beans = SimpleAssistantRegistry.findAssistantsFor(target, context)   // unchanged path
  if no executor factory (AI off) or target not a TextEntity: return beans
  defs  = store.definitionsFor(project, context.taskType())
          minus project-switched-off keys
  pick  = specific definitions whose scope contains the target's type,
          else the fallback
  each  -> executorBean set ? that bean : factory.executorFor(def)
  return beans + executors
```

`DefinitionExecutorFactory` is an assistant-core SPI implemented in assistant-ai under
`requel.ai.enabled=true`, so with AI off nothing changes. `switchableAssistants()` lists the beans
plus every enabled bundled definition (display name from the definition).

### Identities

- `AssistantRole` marker interface (platform-identity); `AssistantUserRole` (user-jpa,
  discriminator `AssistantUserRole`) implements it. `User.isAssistant(user)` = `user != null &&
  user.hasRole(AssistantRole.class)`.
- `AssistantUserInitializer` grants the role to the existing `assistant` user (idempotent) and
  creates one user per bean assistant and bundled definition: username `assistant-<assistantId>`,
  name from the display name, a random password, `editable=false`.
- `AssistantIdentities` SPI (assistant-core, implemented in requel-app): `User identityFor(String
  assistantId, Project project)` finds or creates the identity and makes sure it is a stakeholder
  of the project with `findAssistantStakeholderPermissions()`. The applicator calls it per result
  instead of `resolveAssistantUser(context)`. That one call covers import, repair, project creation
  and project-authored definitions later, so those three commands keep adding only the legacy
  `assistant` stakeholder.
- `AuthorPseudonyms` maps any `User.isAssistant` author to `assistant`.

## Fold-in to confirm

**The assistant account can log in (confirmed: `POST /api/auth/login` only checks the password).** `AssistantUserInitializer` gives it the password
`assistant` and `ProjectUserRole`, so anyone can log in as `assistant` / `assistant` on any
install that hasn't changed it. Proposed: every login path (REST, the OAuth2 form, API tokens) refuses a user with `AssistantUserRole`, and every identity, including
the existing one, gets a random password. Small, and the new identities need the same rule.

## Step by step

1. platform-identity `AssistantRole`; user-jpa `AssistantUserRole`; `User.isAssistant`; login refusal.
2. V31: `assistant_definitions`, `assistant_runs.evidence_unverified`.
3. assistant-core: entity, repository, store with cache, validator, `DefinitionExecutorFactory` SPI,
   `DefinitionAwareAssistantRegistry`, `AssistantIdentities` SPI, applicator per-result identity,
   worker writing `template_*` and `evidence_unverified`.
4. assistant-ai: `DefinitionExecutorAssistant` (moved body), factory, `DefinitionSeeder`, bundled JSON;
   delete `RequirementsReviewAssistant`.
5. requel-app: `AssistantIdentities` implementation, `AssistantUserInitializer` changes, the #355 read
   returns `evidenceUnverified`; `score.py` reports it.
6. Tests (below), then the #355 re-run on your Mac.

## Test plan

- **Golden:** the seeded definition's instructions equal the former `TASK_INSTRUCTIONS` (kept as a
  test constant), and a fake-CLI review sends the same prompt bytes as before (captured stdin).
- **Two projects, two prompts:** a project definition with a different key for project B; reviews
  of A and B in one JVM send different instructions.
- **Enable/disable:** the install-wide flag and the project switch each take effect on the next run.
- **Legacy path unchanged:** post-edit lexical runs match today (existing ITs).
- **Validation:** unknown type, unknown provider, `POLICY`, empty vocabulary, bad schema, two
  specific definitions on one type, two fallbacks — each rejected on save; a bad bundled file fails
  the seeder.
- **Fallback:** a specific Goal definition replaces the fallback on goals only.
- **O(1) reads:** a 20-entity bulk review issues one definition query (Hibernate statistics).
- **Evidence:** a reply citing text not in the entity keeps its finding and records
  `evidence_unverified = 1`.
- **executorBean:** a definition naming a test bean dispatches to it.
- **Identities:** lexical and AI findings on one entity have different authors, both
  `User.isAssistant`; #270 staleness and auto-resolve still treat them as machine; context packs
  show `assistant`; the legacy `assistant` user's annotations stay machine-authored; an assistant
  identity cannot log in.
- **Run record:** `template_id` / `template_version` / `template_source` written.
- **Baseline:** re-run `score.py` (cli/claude, 3 runs); re-score the committed baseline with the same
  patterns; per-type hit rates within ±1 hit, spurious within ±0.5 per run, zero schema failures.

## Out of scope

- REST, MCP or UI for authoring definitions (#264). The overview toggles appear through the existing
  catalog with no Angular change.
- Context providers beyond `entity` (#261); per-type definitions and tuning (#263).
- POLICY runtime (#265); corpus analyses (#266).
- Re-attributing existing annotations to the new identities.

## Risks

- **Prompt drift.** Any change to how instructions reach the client changes the prompt; the golden
  stdin test pins it.
- **Stakeholder creation in the apply transaction.** It runs under the #279 project lock; the IT
  covers a first run on a project with no identity stakeholder yet.
- **The applicator's legacy fallback** to the triggering user, when no identity resolves, stays.

## Implementation notes (2026-10-01)

Where the build differs from the plan above:

- **Registry.** No `DefinitionAwareAssistantRegistry`: `SimpleAssistantRegistry` (already the
  `SwitchableAssistantCatalog`) takes the store and the optional `DefinitionExecutorFactory` by
  setter and does the selection itself. One bean, one place that applies the project switches.
- **Seeder** is `AssistantDefinitionSeeder` in assistant-core (`SmartInitializingSingleton`), reading
  `classpath*:ai/definitions/*.json`; the bundled file still lives in assistant-ai.
- **`AssistantIdentities`** is declared in project-domain and implemented in project-jpa
  (`JpaAssistantIdentities`): it needs the user and project repositories, which assistant-core
  can't see. `identityFor(assistantId, projectId)`.
- **Proxies.** Repositories wrap what they return in an `EntityProxy` unless the caller is a
  command (`DomainObjectWrappingAdvice`). `JpaAssistantIdentities` isn't one, so it unwraps the
  project, user and permissions before building the stakeholder; a wrapped user cascaded into
  Hibernate as "detached entity passed to persist" and marked the apply transaction rollback-only.
- **Circular reference.** `JpaAssistantIdentities` takes `ObjectProvider<CommandHandler>`: the
  command handler chain reaches the run worker, which reaches the applicator, which reaches it.
- **`User.isAssistant`** is the role **or** the legacy username `assistant`, so an install whose
  `assistant` user hasn't been granted the role yet (before `AssistantUserInitializer` runs) stays
  machine-authored. `UserImpl.isPassword` returns false for any assistant identity; that is the
  login refusal for every path, since they all check the password through it.
- **Golden test** is the unit test (`DefinitionExecutorAssistantTest`): the seeded instructions equal
  the former `TASK_INSTRUCTIONS` and reach the client request unchanged. There is no captured
  pre-#260 stdin to diff against, so the IT checks the prompt only for the fork marker.
- **O(1) reads** are pinned at the store (`AssistantDefinitionStoreTest`: one repository read per
  project across a bulk review) instead of with Hibernate statistics.
- **`AssistantUserRoleTest`** lives in requel-app: user-jpa has no test dependencies.
- **Export.** The identities are stakeholders, so a project export now carries
  `assistantUserRole`. It is in `ExportProjectCommandImpl.CLASSES_FOR_JAXB` and, as an optional
  extra choice under `userRoles`, in `doc/samples/project.xsd` and
  `website/integration/2.0/project.xsd` (kept identical). Without it every export and report
  failed with "unable to marshal type AssistantUserRole". Import does not map the role: imported
  annotations by an `assistant-<id>` user land on this install's identity of the same name, which
  `AssistantUserInitializer` has already created with the role.
- **Project delete.** Project definitions have no foreign key, so `DeleteProjectCommandImpl` deletes
  them through `ProjectAssistantDefinitions` (project-domain SPI, implemented by
  `AssistantDefinitionStore`), as it does the #268 settings.

New ITs: `AssistantDefinitionsIT` (seed, fork per project, install-wide and project switches,
the review's own author and its stakeholder row, run record and unverified evidence),
`ProjectAssistantSettingsIT.aLexicalFindingIsWrittenByItsAssistantsOwnIdentity` (the lexical
author, kept where the dictionary is already loaded: each test context with the dictionary holds
its own copy in H2) and `AuthorizationIT.theAssistantUserCannotLogIn`.

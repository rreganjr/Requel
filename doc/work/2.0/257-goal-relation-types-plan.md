# #257 Expand GoalRelationType and serve the vocabulary from one source: implementation plan

Issue: https://github.com/rreganjr/Requel/issues/257
Branch: `257-goal-relation-types`, cut from `release/2.0` @ `f8190e7f` (after #296 / PR #346)
Background: `doc/work/backlog/goal-relation-types-proposal.md`, `doc/work/2.0/pq-roundtable-case-study.md`

## Summary

`GoalRelationType` grows from `Supports | Conflicts` to seven values: `Refines`, `Duplicates`,
`DependsOn`, `Obstructs` and `Measures` join the existing two. The enum carries its own metadata
(label, description, symmetric or directed, inverse label). Every consumer reads that metadata:
a new read endpoint for the Angular picker and list rendering, the gateway/MCP input schema for
`EditGoalRelation`, and the validation messages. After this ticket, adding a value means editing
the enum and nothing else.

The review found three problems the issue text didn't cover, and this ticket fixes all three:

1. The MySQL column is `enum('Conflicts','Supports')`, so the new values need a migration.
2. The streaming XML import keeps only `Supports` relations, so `Conflicts` is already lost on
   every import.
3. A repeated relation hits the DB unique key instead of getting a validation error.

## Review against the tree (`release/2.0` @ `f8190e7f`)

- **Schema.** `V1__init.sql`: `relation_type enum('Conflicts','Supports') DEFAULT NULL`, plus
  `UNIQUE KEY (to_goal_internal_id, from_goal_internal_id)`, which the entity's
  `@UniqueConstraint` mirrors. `@Enumerated(EnumType.STRING)` only controls how Hibernate writes
  the value. MySQL rejects `'Refines'`, and H2 `create-drop` never shows the problem. The highest
  migration is V22, so this one is **V23**.
- **Import.** `ImportProjectStreamingCommandImpl` is the only import path
  (`ProjectCommandFactoryImpl.newImportProjectCommand`). `GoalImportXmlMapper.toDraft` adds a
  target only when `"Supports".equalsIgnoreCase(rel.getRelationType())`. `GoalImportDraft` holds a
  `Set<String> relationTargets` with no type, and `GoalAssembler.linkSupport` hard-codes
  `GoalRelationType.Supports`. `ProjectXmlStreamingRoundTripIT` has no relation assertions, which
  is why nobody noticed. `GoalRelationImpl.GoalRelationTypeAdapter` still marshals the export by
  name; its `unmarshal` is only reached by legacy JAXB unmarshalling, not the live import.
- **Command.** `EditGoalRelationCommandImpl.execute` already turns a failed `valueOf` into
  `EntityValidationException.validationFailed(GoalRelation.class, "relationType", ...)`, but the
  message doesn't list the permitted values. A second relation between the same ordered pair has
  no check (`// TODO: check for uniqueness?`) and fails at flush on the unique key. Nothing stops
  A→B `Conflicts` alongside B→A `Conflicts`.
- **Gateway / MCP.** `EditGoalRelationInput.relationType` is `@NotBlank String`. Its
  `@CommandDescription` (written in #296) says "relationType is exactly Supports or Conflicts".
  `CommandInputSchema.jsonType` maps every String to `{"type":"string"}` and has no way to list
  enum values (#271 put that out of scope). `GoalRelationDto` javadoc also says
  `"Supports" or "Conflicts"`.
- **Read.** `ProjectQueryController.toGoalDetailDto` fills both relation lists with
  `r.getRelationType().name()`. The `/api/projects/stakeholder-permissions` endpoint in the same
  controller is the precedent for serving a vocabulary.
- **Angular.** `goal-editor.ts` hard-codes `relationTypeOptions` (two entries) and
  `newRelationType = 'Supports'`. Both lists render `{{ r.relationType }}`, so "Related To This
  Goal" shows `Supports` where it means "supported by". `excludeGoalIds()` hides goals this goal
  already relates *to*, not goals that relate to it. `models/goal.ts` types
  `relationType: 'Supports' | 'Conflicts'`. The e2e `GoalEditorPage.addRelation` takes the same
  union and picks the option by its visible name.
- **Nothing branches on the value** in Java or TS, apart from the import filter above. No
  assistant reads relations: the legacy `GoalAssistant.analyzeGoalRelations` is unimplemented.

## Locked decisions

From the review (2026-09-26):

1. **Column type:** V23 converts `relation_type` to `VARCHAR(32)`, not a longer ENUM, so the Java
   enum is the only list.
2. **One relation per ordered pair of goals.** The unique key stays. A repeat is refused with a
   field-level validation error before flush.
3. **Symmetry:** stored in the direction entered, with no normalisation. A symmetric relation
   (`Conflicts`, `Duplicates`) is refused when the reverse pair already holds **the same type**.
   Symmetric types render the same word in both lists.
4. **Unknown value on import:** that relation is skipped with one WARN naming the value and both
   goals, and the rest of the file imports.
5. **Folded in:** the streaming import carries the relation type, so all seven values (and
   `Conflicts`, which is lost today) round-trip.

Proposed in this plan (flag any you want changed):

6. **Parsing is case-insensitive and trimmed** (`GoalRelationType.parse(String)` →
   `Optional`), matching `IssueSeverity.parse`. `"supports"` is accepted and stored as
   `Supports`. Today `valueOf` is exact-case, so this only loosens input.
7. **Labels:** Supports, Conflicts, Refines, Duplicates, "Depends on", Obstructs, Measures.
   **Inverse labels:** Supported by, Conflicts, Refined by, Duplicates, Required by, Obstructed
   by, Measured by. Both relation tables read as "this goal *type* goal": the from list uses the
   label, the to list uses the inverse label.
8. **The schema carries meanings, not just names.** An AI client choosing between `Refines` and
   `Duplicates` needs the definitions. `CommandInputSchema` therefore emits `enum` plus a
   `description` listing each value's meaning. This goes through a new `@AllowedValues`
   annotation, so it works for any String input, not only this one.
9. **`EditIssueInput.severity` also gets `@AllowedValues(IssueSeverity.class)`.** It's one
   annotation, and it closes the #271 out-of-scope item now that the mechanism exists. The command
   still parses case-insensitively; the schema just advertises the canonical names.

## Contracts

### Domain (`project-domain`, `platform-core`)

```java
public enum GoalRelationType implements DescribedValue {
    Supports("Supports", "Supported by", false,
        "The from goal has a positive influence on the success of the to goal."),
    Conflicts("Conflicts", "Conflicts", true,
        "The two goals cannot both be satisfied."),
    Refines("Refines", "Refined by", false,
        "The from goal is a more concrete statement of the to goal."),
    Duplicates("Duplicates", "Duplicates", true,
        "The two goals state the same requirement."),
    DependsOn("Depends on", "Required by", false,
        "The from goal cannot be achieved until the to goal is."),
    Obstructs("Obstructs", "Obstructed by", false,
        "The from goal makes the to goal harder without making it unsatisfiable."),
    Measures("Measures", "Measured by", false,
        "The from goal is the measurable proxy for the outcome stated by the to goal.");

    public String getLabel(); public String getInverseLabel();
    public boolean isSymmetric(); public String getDescription();
    public static Optional<GoalRelationType> parse(String value);   // trim, case-insensitive
    public static String permittedValues();                          // "Supports, Conflicts, ..."
}
```

`DescribedValue` (`platform-core`, package `com.rreganjr.requel`): `String getDescription()`.
It lives this low so `gateway-api` can read descriptions without a new dependency. `IssueSeverity`
does not implement it (out of scope), so its schema carries names only.

### Schema (`V23__goal_relation_type_varchar.sql`)

```sql
-- Issue #257: goal relation types are defined by GoalRelationType alone. V1 declared the column
-- as enum('Conflicts','Supports'), which rejects every new value; varchar keeps the Java enum the
-- only list. Existing values are unchanged.
ALTER TABLE `goal_relations` MODIFY `relation_type` varchar(32) DEFAULT NULL;
```

`GoalRelationImpl.getRelationType()` adds `@Column(name = "relation_type", length = 32)` and
`@JdbcTypeCode(SqlTypes.VARCHAR)`, so H2 `create-drop` also builds a varchar and the two schemas
match. (Hibernate 6 otherwise emits a native `enum` column on dialects that support one.)

### Command (`project-jpa`)

`EditGoalRelationCommandImpl.execute`, in order, after the goals resolve and the self-relation
check:

- `parse` fails → `validationFailed(GoalRelation.class, "relationType", "The goal relation type
  cannot be <x>; permitted values are Supports, Conflicts, Refines, Duplicates, DependsOn,
  Obstructs, Measures.")`.
- Another relation from `fromGoal` to `toGoal` exists (ignoring the one being edited) →
  `validationFailed(GoalRelation.class, "toGoal", "\"<from>\" already has a <type> relation to
  \"<to>\"; change that relation's type instead.")`.
- The type is symmetric and `toGoal` already has the same type pointing at `fromGoal` →
  `validationFailed(GoalRelation.class, "toGoal", "\"<to>\" already <label> \"<from>\"; a
  <label> relation holds in both directions.")`.

Both relation checks run on create and on update (an update can change the goals or the type).
The DB key stays as the backstop for a concurrent create.

### Import (`platform-core`, `utils-jaxb`, `project-jpa`)

- `GoalImportDraft`: `Set<String> relationTargets` becomes `Map<String, String> relations`
  (target external id → type name, insertion-ordered). It stays a String because `platform-core`
  sits below `project-domain`. The builder method is `relation(targetId, type)`.
- `GoalImportXmlMapper.toDraft`: keeps every relation that has a `toGoal`, whatever its type.
  If the XML repeats a target, the first entry wins; the unique key allows only one anyway.
- `GoalAssembler.attachSupports` → `attachRelations`. It parses each type, and an unknown value
  logs `WARN Skipping goal relation "<from>" -> "<to>": unknown relationType "<x>"` and continues.
  `linkSupport(source, target)` → `link(source, target, type)`.
- `doc/samples/project.xsd`: no change (`relationType` is already `xs:string`).
- `GoalRelationTypeAdapter.unmarshal`: switches to `parse(...).orElse(null)` with a comment that
  it isn't on the live import path. Legacy unmarshalling then yields a null type instead of
  throwing.

### API (`service-api`, `service-impl`)

- `GoalRelationTypeDto(String value, String label, String description, boolean symmetric,
  String inverseLabel)`.
- `GET /api/projects/goal-relation-types` → `List<GoalRelationTypeDto>` in enum order, declared
  next to `/stakeholder-permissions` and before `/{name}`. It requires authentication like the
  rest of the controller and needs no project access, because the vocabulary is the same for
  every project.
- `EditGoalRelationInput.relationType`: `@NotBlank @AllowedValues(GoalRelationType.class)`.
  The `@CommandDescription` changes "relationType is exactly Supports or Conflicts" to
  "relationType is one of the values the schema lists; symmetric types (Conflicts, Duplicates)
  hold in both directions, so the reverse of an existing one is refused; a pair of goals holds
  at most one relation in each direction". The javadoc for `EditGoalRelationInput` and
  `GoalRelationDto` points at `GoalRelationType`.
- `@AllowedValues(Class<? extends Enum<?>> value)`: a new annotation in `service-api` next to
  `@CommandDescription`, with `RECORD_COMPONENT` and `METHOD` targets and runtime retention.
- `EditIssueInput.severity`: `@AllowedValues(IssueSeverity.class)` (decision 9).

### Gateway / MCP (`gateway-api`)

`CommandInputSchema.of`: when a component carries `@AllowedValues`, its node becomes
`{"type":"string","enum":[names]}`. If the enum implements `DescribedValue`, the node also gets
a `"description"` of the form `"Supports: <description>; Conflicts: ...".` Both the MCP write
tools (`McpWriteService`) and `GET /api/gateway/commands` (`GatewayCommandController`) use
`CommandInputSchema.of`, so both pick it up with no change on their side.

### Angular

- `models/goal.ts`: `GoalRelationDto.relationType: string`; new `GoalRelationTypeDto`.
- `core/goal.service.ts`: `getRelationTypes()`, cached for the service's lifetime (the
  vocabulary only changes with a deploy).
- `goal-editor.ts`: loads the types on init. `relationTypeOptions` becomes a computed value over
  them (`label` → `value`), and the dialog shows the selected type's description under the
  select. `newRelationType` defaults to the first served value. The from table renders the
  label, and "Related To This Goal" renders the inverse label. Its description changes from
  "Other goals that support or conflict with this goal." to "Other goals with a relation to this
  goal." An unknown value falls back to the raw string.
- `e2e/pages/GoalEditorPage.addRelation(toGoalName, relationLabel = 'Supports')`: it takes the
  visible label (string), not the union.

## Step by step

1. `DescribedValue`; `GoalRelationType` metadata, `parse`, `permittedValues`.
2. `GoalRelationImpl` column mapping and adapter; `V23__goal_relation_type_varchar.sql`.
3. `EditGoalRelationCommandImpl` parse, message, and the pair and symmetric checks.
4. Import: draft, mapper, assembler.
5. `@AllowedValues`, `CommandInputSchema`, `EditGoalRelationInput` / `EditIssueInput` / DTO
   javadoc and description.
6. `GoalRelationTypeDto` and the endpoint.
7. Angular model, service, editor; e2e page object and a new e2e case.
8. Tests below. `tmp/257-verify.sh` runs `mvn clean verify`, the Angular unit suite
   (`CI=1 npm test -- --watch=false`), both `tsc` checks and a development build.

## Test plan

**Unit / H2 IT**

- `GoalRelationTypeTest`: `parse` trims, is case-insensitive, and returns empty for null, blank
  and `"Blocks"`; every value has a non-blank label, inverse label and description; symmetric
  values have `inverseLabel == label`; `permittedValues` lists all seven in order.
- `GoalRelationCommandTest` (extend):
  - each of the seven values creates and re-reads;
  - `"Blocks"` fails on `relationType`, and the message contains `Measures`;
  - a second A→B (any type) fails on `toGoal`, and no second row exists;
  - B→A `Conflicts` after A→B `Conflicts` fails; B→A `Supports` after A→B `Conflicts` succeeds;
    B→A `Duplicates` after A→B `Conflicts` succeeds;
  - updating A→B from `Supports` to `Conflicts` while B→A `Conflicts` exists fails;
  - updating a relation's type to its own current value succeeds (it doesn't collide with
    itself);
  - `"supports"` is stored as `Supports`.
- `CommandInputSchemaTest`: an `@AllowedValues` component emits `enum` in declaration order,
  plus a description when the enum is a `DescribedValue`; a plain String is unchanged.
- `McpWriteServiceTest` / `GatewayCommandController` test: the `EditGoalRelation` schema lists
  the seven values.
- `ProjectXmlStreamingRoundTripIT` (extend): a project with one relation of each type, all seven,
  exports and imports with the type and direction intact. An export with its `relationType`
  hand-edited to `"Blocks"` imports without that relation and logs one WARN; every other goal and
  relation imports.
- `GoalImportXmlMapperTest` (new or extended): non-`Supports` types reach the draft, and a
  repeated target keeps the first entry.
- `ProjectQueryController` / MockMvc: `GET /api/projects/goal-relation-types` returns seven
  entries with labels, and is 401 when not authenticated.

**MySQL (Testcontainers)**

- `GoalRelationTypeMigrationMySqlIT`, following the plain-Flyway pattern of
  `IssueSeverityMigrationMySqlIT`: migrate to V22, insert a `Supports` and a `Conflicts` row
  (plus the goals and user they need), migrate to latest, and assert both are unchanged and
  every one of the seven values inserts. Skipped (not failed) without Docker, like its siblings.

**Angular**

- `goal-editor.spec.ts`: the picker options come from the service (seven, with labels); the
  from table shows the label and the to table the inverse label (`Supported by`); the selected
  type's description renders; the server's validation message reaches `showError`.
- `goal.service.spec.ts`: `getRelationTypes` makes one request across two calls.

**e2e**

- `goals.e2e.ts`: add a relation with `Refines` and assert "Refines" in this goal's table and
  "Refined by" on the target goal's "Related To This Goal". The existing `Supports` cases keep
  passing with the label-based page object.

## Out of scope

- Any analysis that reasons over relation types (the per-type Goal assistant, #258).
- Making relation types user-editable or per-project (issue: "Not data-driven").
- More than one relation per ordered pair (decision 2).
- Changing or rejecting existing reverse-symmetric duplicates already in a database; the check
  applies to writes from now on.
- `DescribedValue` on `IssueSeverity` or other enums.
- Moving `goal-relation-types-proposal.md` out of `doc/work/backlog/`: the issue body links it
  where it is.
- Older Requel builds importing a newer export: they still fail at `valueOf`, and that can't be
  fixed from here.

## Risks

- **V23 on a large `goal_relations` table:** `MODIFY` rebuilds the table. Goal relations are few
  per project, so this is low risk.
- **Schema drift between H2 and MySQL:** if `@JdbcTypeCode(SqlTypes.VARCHAR)` isn't honoured
  with `@Enumerated`, H2 builds an enum column, which still works because it is generated from all
  seven constants. The MySQL IT is the check that matters.
- **Case-insensitive parsing** (decision 6) accepts input that used to fail. No known caller sends
  wrong case; all current callers send exact names.
- **The e2e page-object signature changes** (union → label). Every caller is in `e2e/`, and the
  TS compiler finds them all.
- **Existing reverse-symmetric pairs** in a database stay and display twice. That is accepted; it
  was possible before and is refused from now on.

## AC mapping

| #257 acceptance criterion | Covered by |
|---|---|
| All seven values accepted by `EditGoalRelation` on H2 and MySQL | `GoalRelationCommandTest`; `GoalRelationTypeMigrationMySqlIT` |
| All seven round-trip through XML, including `Conflicts` | `ProjectXmlStreamingRoundTripIT` |
| Unknown `relationType` on import skipped with a WARN, rest imports | `ProjectXmlStreamingRoundTripIT` `"Blocks"` case |
| Picker populated from server; no hard-coded list or union in the frontend | endpoint test; `goal-editor.spec.ts`; `models/goal.ts` |
| Directed relations show the inverse label from the target; symmetric the same word | `goal-editor.spec.ts`; e2e `Refines` case |
| Unrecognised `relationType` is a field-level error naming the permitted values | `GoalRelationCommandTest` `"Blocks"` case |
| Repeat pair / reverse symmetric refused with a field-level error, by test | `GoalRelationCommandTest` pair and symmetry cases |
| MCP `EditGoalRelation` schema lists the values, generated from the enum | `CommandInputSchemaTest`; `McpWriteServiceTest` |
| Existing `Supports` / `Conflicts` unchanged | `GoalRelationTypeMigrationMySqlIT`; existing tests |

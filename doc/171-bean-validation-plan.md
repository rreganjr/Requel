# Implementation Plan — #171 Bean validation constraints on artifact name/text and user email

Blocker for #132 (merged as PR #175 without the artifact limits) and for #173. Referenced by
`doc/132-reactive-forms-plan.md` §2.3, which is the closest thing #171 currently has to a spec.

Grounded in `release/2.0` at **`c6e10c4`** — i.e. *after* #132 merged. That matters: #132 shipped
`requel-angular/src/app/shared/validation-limits.ts` with the artifact entries **deliberately
absent**, and two editors carry literal `#171` TODO comments where a `maxLength` goes. #171 is now
the ticket that closes a loop rather than one that opens it.

This plan resolves the four things #171's body left undecided, and corrects three factual claims in
it. §8 has the proposed ticket-body rewrite.

## Decisions (locked)

1. **Constraints are enforced by wiring validation into the command handler chain.** #171's first
   two bullets are inert as written — see §1. `ApiCommandFactory.newCommand` is the single shared
   entry point for all three write paths, so it is where the input becomes reachable for every
   caller at once. **Revised during slice 1:** validating *in* `newCommand` puts validation before
   the chain and therefore before `AuthorizingCommandHandler`, so an unauthorized caller sending a
   malformed payload got a 422 listing field names instead of a 403 — three `AuthorizationIT` cases
   caught it. Validation now lives in a `ValidatingCommandHandler` wrapped inside
   `AuthorizingCommandHandler`, recovering the input DTO from the `CommandMetadata` that
   `newCommand` stamps. §2 and §2.2.
2. **`name` gets `@Size(max = 255)`; `text` stays unbounded.** 255 matches every existing
   `varchar(255)` column, so **no Flyway migration is required** for the artifact fields. `text`
   is `@Lob`/`longtext` and gets no `@Size` — the client then mirrors a real limit on `name` and
   honestly has none for `text`. §3.
3. **`ArgumentImpl` / `PositionImpl` text is in scope.** Both are `varchar(255)` with no
   validation and no `@Lob`, so long text fails at the database today rather than as a field
   message. This is the only live *defect* in the ticket's neighbourhood. §4.
4. **#171 carries the Angular follow-through.** The backend constants and their client mirror land
   in one PR, which is what `validation-limits.ts`'s "every entry names its backend source" rule
   exists to make reviewable. §5.

## 1. The blocker: nothing in the codebase validates a DTO

```
$ grep -rn "@Valid\|@Validated\|Validator" modules --include=*.java \
    | grep -v /target/ | grep -v /test/ | grep -v BeanValidation
(no results)
```

Validation runs **only** at Hibernate flush time. The path is
`ConstraintViolationException` → `ExceptionMapper:79` → `BeanValidationExceptionAdapter` →
`BeanValidationException` → `CommandController:137` → `CommandResult.FieldViolation`.
`CommandController:105` does `objectMapper.convertValue(rawInput, inputType)` and hands the DTO
straight to `apiCommandFactory.newCommand` — no validator is ever invoked on it.

Consequences for #171 as written:

- **Bullet 1 is inert.** `@Size(max = …)` on `Edit*Input` records would be documentation.
  `EditGoalInput`'s existing `@NotBlank` already is: a blank goal name is rejected by
  `GoalImpl:96`'s `@NotEmpty`, not by the DTO.
- **Bullet 2 is a no-op.** `UserImpl:330` already carries `@Email`. Adding `@Email` to
  `EditUserInput.emailAddress` changes no runtime behavior; the client-side `email` validator
  #132 shipped already mirrors a constraint the server enforces.

**There are 40 DTOs already carrying dormant constraints** (`@NotBlank` ×64, `@NotNull` ×52,
`@NotEmpty` ×13 across `service-api`). Enabling validation activates all of them at once. That is
the real risk in this ticket and §2.3 does not mention it.

## 2. Where validation gets wired

Three write paths reach the command chain, and they share exactly one choke point:

| Path | Entry | Reaches |
|---|---|---|
| Angular SPA | `CommandController:111` | `apiCommandFactory.newCommand(commandType, input, file)` |
| MCP tools | `McpWriteService:169` → `InProcessCommandGateway:100` | `apiCommandFactory.newCommand(commandType, boundInput)` |
| requel-cli / remote connector | `GatewayCommandController:92` → `InProcessCommandGateway:100` | same |

So **validate inside `ApiCommandFactory.newCommand`**, not with `@Valid` on the controller
signature. `@Valid` on `CommandController` would cover the SPA only and silently leave MCP and the
CLI unvalidated — and `@RequestBody Map<String, Object>` isn't the DTO anyway, so `@Valid` there
has nothing to bind to.

Sketch:

```java
// ApiCommandFactory
private final jakarta.validation.Validator validator;   // spring-boot-starter-validation, present

public Command newCommand(String commandType, Object input, MultipartFile file) {
    if (input != null) {
        Set<ConstraintViolation<Object>> violations = validator.validate(input);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);   // adapted below
        }
    }
    ...
}
```

`ConstraintViolationException` thrown here escapes `ExceptionMapper` (which only adapts
repository-layer throwables), so `CommandController`'s `catch (EntityValidationException e)`
misses it and it falls through to the 500 handler. Two options, in preference order:

1. **Wrap at the throw site** in a `BeanValidationException` built the same way
   `BeanValidationExceptionAdapter` builds one, so the existing `CommandController:137` branch and
   the existing `GatewayCommandController` error shape both keep working unchanged. Preferred —
   zero client-visible contract change beyond *earlier, better-targeted* violations.
2. Add a `catch (ConstraintViolationException e)` branch alongside the existing one. More code in
   two controllers, same result.

**This obsoletes #176.** `ConstraintViolation.getPropertyPath()` on a DTO yields *input-DTO field
names* — `emailAddress`, `userRoleNames`, `name` — which is precisely what #176 was filed to make
`CommandController` emit. Once DTO-level violations dominate, the eleven per-editor
`{ entityProperty: controlName }` maps #132 added become mostly dead weight. Recommend
re-pointing #176 at "delete the per-editor maps now that violations carry DTO field names" and
closing it after #171, rather than implementing it separately.

### 2.2 Ordering: validation must run inside authorization

Validating in `newCommand` runs *before* the handler chain, so `AuthorizingCommandHandler` never gets
a look. An unauthorized caller sending an invalid payload then receives a 422 with field names and
messages rather than a 403 — the DTO shape leaks to someone with no access.

The fix uses the chain the codebase already has for cross-cutting concerns.
`ValidatingCommandHandler` is wrapped *inside* `AuthorizingCommandHandler` in
`modules/requel-app/src/main/resources/spring/commandHandlerConfig.xml`:

```
Auditing → CurrentUser → RetryOnLockFailures → ExceptionMapping
         → Authorizing → Validating → FindingResolutionTracking → AnalysisInvoking → Default
```

Two things make this work:

- **Recovering the input.** The chain carries a `Command`, not the DTO. `newCommand` already stamps
  `CommandMetadata(commandType, input)` for auditing, so the handler reads the input back from
  there. Commands with no metadata or a null input are skipped — correct for the sub-commands a
  command runs internally, which never had an input DTO. `newCommand` fails fast if an API command
  with an input is not `CommandMetadataAware`, so the skip can never silently swallow one that
  should have been validated.
- **The exception survives the outer handlers.** `RetryOnLockFailuresCommandHandler` catches
  `EntityValidationException` first and rethrows without retrying, and
  `ExceptionMapper.convertException` returns any `EntityException` unchanged rather than re-wrapping
  it. So the per-field messages reach `CommandController` intact.

Pin it with a test that sends an *invalid* payload as an *unauthorized* user and asserts 403. Every
other case in `AuthorizationIT` sends valid payloads, so nothing else would notice the regression.

### 2.1 The 40-DTO activation risk

Turning the validator on is all-or-nothing per DTO. Before merging, sweep every registered command
with a valid payload and assert it still succeeds. Two failure modes to expect:

- **Over-strict dormant constraints.** Each one is either a real bug the DTO documented correctly,
  or a wrong annotation to delete — decide per case, don't blanket-relax.

  **Found in slice 1: `EditStoryInput.primaryActorName` carried `@NotBlank`.** It made every story
  edit 422, including stories that never had a primary actor. Wrong four ways over: the record's own
  javadoc for that parameter says "null to clear"; `ProjectCommandRegistrar:364` applies it
  unconditionally, so null *is* the clear; `StoryPrimaryActorMappingTest` asserts "not setting
  primaryActorName → clears it"; and `EditUseCaseInput.primaryActorName` — same field, same
  semantics — has no constraint. Even `AuthorizationIT`'s own `editStoryJson` helper omits the field.
  Removed, with a comment recording why it stays off. Wrong since #104 and invisible until validation
  went live, which is the whole argument for this slice.
- **Message regression.** Entity messages are hand-written and user-facing ("a unique name is
  required.", "one or more roles must be selected."). Bean-validation defaults on the DTO are not
  ("must not be blank"). Where a DTO constraint now fires *before* the entity one, the user sees
  the worse message. Give every DTO constraint that can front-run an entity constraint an explicit
  `message = …` matching the entity's wording.

`modules/requel-app/src/test/java/.../CommandControllerTest` and the MCP lockstep tests
(`McpWriteCatalogLockstepTest`) are the natural homes for the sweep.

## 3. The limits

`ddl-auto=none` in production (Flyway), `create-drop` in tests
(`application-test.properties:9`). Current state from `V1__init.sql`:

| table | `name` | `text` |
|---|---|---|
| goals, stories, actors, usecases, scenarios, terms, reports, stakeholders, pods | `varchar(255)` | `longtext` |
| tag_category (`V13__tagging.sql:42`) | `varchar(255)` | — |
| **arguments, positions** | — | **`varchar(255)`** |
| user_role_permissions | `varchar(50)` | — |
| categorydef | `varchar(32)` | — |

**`@Size(max = 255)` on `name` therefore needs no migration.** #171's bullet "create Flyway
migrations where columns require expansion" is conditional and, at 255, empty. Strike it or make
it conditional in the body.

**`text` gets no `@Size`.** `AbstractTextEntity.getText()` is `@Lob` → `longtext`. A cap would be
invented rather than mirrored, and `validation-limits.ts`'s stated rule forbids client caps with
no server counterpart.

### 3.1 Correction to the ticket's "Key Note"

The body infers unbounded artifact text from `VerbNetFrame`'s `length = 16277215`. That's
`dictionary-jpa` (`vnframedef.semantics` / `.syntax`, the VerbNet import) — unrelated to artifacts.
Artifact `text` is unbounded because `AbstractTextEntity.getText()` is `@Lob`. The conclusion
("most of the real work is on `name`") is right; the evidence is not. Worth fixing since that note
is what scopes the ticket.

### 3.2 Where the annotations go

`name` is **not** inherited — 18 classes override `getName()` with their own `@Column` /
`@NotEmpty`:

```
project-jpa   AbstractProjectOrDomain, ActorImpl, GlossaryTermImpl, GoalImpl,
              NonUserStakeholderImpl, ProjectTeamImpl, ReportGeneratorImpl,
              StepImpl, StoryImpl, UseCaseImpl
tagging-jpa   TagCategoryImpl
user-jpa      OrganizationImpl, JpaUserRolePermission
dictionary-jpa Category, Linkdef, VerbNetClass, VerbNetFrame, VerbNetSelectionRestrictionType
```

Two placements:

- **Per-subclass**, next to each existing `@NotEmpty`. Explicit, greppable, 13 edits (skip
  `dictionary-jpa` — those are import-only, fixed-vocabulary, and two are already narrower than
  255).
- **Hoisted** to `AbstractProjectOrDomainEntity.getName()`. One edit, but the subclass overrides
  shadow the annotation unless each override is removed, and JPA property-access inheritance with
  overridden getters is exactly where this codebase already has a JAXB hack (`GoalImpl:102-106`).

Recommend **per-subclass**. The hoist is a refactor with a bean-validation-inheritance footgun, and
this ticket should not be the one to find it.

Also note `stakeholders`, `goals`, `terms`, `actors`, `usecases`, `scenarios`, `stories` carry
`UNIQUE KEY (projectordomain_id, name)`. At `varchar(255)` utf8mb4 that's 1020 bytes, inside
InnoDB's 3072-byte limit — so 255 is safe, but any widening beyond ~767 would break those indexes.
Another reason to hold at 255.

### 3.3 Entity/Flyway drift

Any `@Column(length = …)` added here is applied to the H2 test schema by `create-drop` but **not**
to production, which is Flyway-only. A `@Column(length)` that disagrees with `V1__init.sql` passes
CI and truncates in production. Since decision 2 needs no width changes, the safest move is to add
`@Size` **only** and touch no `@Column(length)` on the artifact fields at all.

## 4. `ArgumentImpl` / `PositionImpl` — the actual defect

`ArgumentImpl:126` and `PositionImpl:166` declare `getText()` with **no `@Lob`** and no
`@Column(length)` → `varchar(255)` in `V1__init.sql`. `AbstractAnnotation:188` (notes, issues) *is*
`longtext`. So today a long argument or position body produces a truncation / `DataException` from
the driver, surfaced as the generic `INTERNAL_ERROR` from `CommandController`'s catch-all — not a
field message under the field.

Fix, matching the notes/issues precedent:

- Add `@Lob` to both getters.
- Flyway `V14__widen_annotation_text.sql`: `ALTER TABLE arguments MODIFY text longtext;`
  and the same for `positions`. This is the one migration #171 actually needs.
- `EditArgumentInput` / `EditPositionInput` already carry `@NotBlank`-family constraints (3 and 2
  respectively) that go live with §2 — include them in the sweep.

Widening rather than capping at 255 is the call because these are free-text discussion fields on
the annotations surface, and 255 characters is not a defensible product limit for one.

## 5. The Angular follow-through

#132 left the seams open and named them:

- `shared/validation-limits.ts` — module doc says the artifact entries land "when #171 merges".
  Add `ARTIFACT_NAME_MAX_LENGTH = 255`, sourced comment naming the per-entity `@Size(max = 255)`.
  Add no `text` entry (decision 2) and say why, so the absence reads as deliberate.
- `Validators.maxLength(ARTIFACT_NAME_MAX_LENGTH)` on the `name` control in
  `goals/goal-editor.ts:335`, `stories/story-editor.ts:340`, `terms/term-editor.ts:252`,
  `reports/report-editor.ts:182`. `users/user-editor.ts:258` and `users/edit-account.ts:217` are
  `users.name` — same 255 column, same constant.
- Delete the two `#171` TODO comments: `terms/term-editor.ts:247`,
  `reports/report-editor.ts:180`.
- `maxlength` **attribute** on the inputs as well as the validator, so the browser prevents
  overtyping instead of only reporting it after the fact. `app-field` renders the control via
  content projection, so this is per-call-site markup, not a primitive change.
- The five wizard editors (#173: project, actor, stakeholder, scenario, use-case) are still
  `[(ngModel)]` and out of scope here — but once #171 defines the constant, #173 has no excuse to
  invent its own. Add a line to `doc/132-reactive-forms-plan.md` §2.2's table pointing `n` at the
  new constant instead of at "#171".

`maxlength` is already in `DEFAULT_FORM_ERRORS` and `ERROR_PRECEDENCE` (`shared/form-errors.ts:60`),
so no message work is needed — as §2.3 predicted.

## 6. XML import/export and the XSD

`doc/samples/project.xsd` has **zero** `maxLength` restrictions — every field is bare `xs:string`.
So #171's "verify constraints propagate correctly through XML import/export" has no assertion to
make as stated.

Import goes through JAXB unmarshalling into the same entities
(`utils-jaxb/.../imports/*StaxImporter`, `ImportProjectStreamingCommandImpl`) and persists through
the repository, so **entity** `@Size` does apply on import — an over-long name in an imported file
will fail validation. That is the behavior worth asserting, and it is a behavior *change*: files
that imported before may now be rejected.

Two sub-decisions, both cheap:

- **Add `xs:maxLength` restrictions to the XSD** mirroring the 255, so an invalid file is
  diagnosable before it reaches the database. Export already advertises the schema location
  (`ExportProjectCommandImpl:160`).
- **Or leave the XSD permissive** and rely on entity validation, accepting a worse error for
  malformed imports.

Recommend adding the restrictions — it is a named `xs:simpleType` plus a type swap on the `name`
elements, and it makes the round-trip claim testable.

Either way: add an import test with a 256-character name asserting a clean validation failure
rather than a 500, and confirm `ProjectXmlController` surfaces it usefully.

## 7. Tests

- **`ApiCommandFactoryValidationTest`** — a violating DTO produces a `BeanValidationException`
  whose property names are DTO field names; a valid one passes through untouched.
- **Command sweep** (§2.1) — every registered command, valid payload, still succeeds. This is the
  test that protects the 40 dormant DTOs.
- **Per-entity `@Size`** — 255 accepted, 256 rejected, on one representative from each of
  `project-jpa` / `tagging-jpa` / `user-jpa`. Not all 13; the annotation is the same shape.
- **`arguments` / `positions`** — text over 255 now round-trips (post-migration) instead of
  erroring.
- **MCP lockstep** — `McpWriteCatalogLockstepTest` and `McpWriteServiceSchemaTest` still pass;
  confirm `CommandInputSchema` either reflects the new `@Size` in the generated tool schema or
  deliberately doesn't (worth a one-line note either way, since MCP clients read those schemas).
- **XML import** — over-long name yields a validation failure, not a 500 (§6).
- **Angular** — `maxLength` fires at 256 in each of the six editors; extend the existing
  `*.spec.ts`. No new a11y cases needed — the error rendering path is unchanged.

## 8. Proposed ticket-body rewrite

> **Add bean validation constraints to artifact name/text and user email inputs**
>
> Blocks #173. Unblocked #132, which merged without artifact max lengths and left
> `validation-limits.ts` and two editor TODOs waiting on this ticket. Plan:
> `doc/171-bean-validation-plan.md`.
>
> **Current state.** Nothing in `modules/` validates an input DTO — there is no `@Valid`,
> `@Validated`, or `Validator` call in main source. Validation runs only at Hibernate flush,
> adapted by `BeanValidationExceptionAdapter`. Consequently the 40 `service-api` DTOs that already
> carry `@NotBlank` / `@NotNull` / `@NotEmpty` are dormant, and there is exactly one `@Size` in
> main source (`UserImpl:385`, roles). Artifact `name` columns are `varchar(255)` with no `@Size`;
> artifact `text` is `@Lob`/`longtext`. `ArgumentImpl.getText()` and `PositionImpl.getText()` lack
> `@Lob` and so are `varchar(255)` with no validation — long text fails at the driver as a 500.
>
> **Scope.**
> - Invoke the validator in `ApiCommandFactory.newCommand` — the single choke point shared by
>   `CommandController` (SPA), `InProcessCommandGateway` (MCP), and `GatewayCommandController`
>   (CLI) — wrapping violations in `BeanValidationException` so the existing error contract holds.
> - `@Size(max = 255)` on `name` in the 13 `project-jpa` / `tagging-jpa` / `user-jpa` entities that
>   override `getName()`. No `@Size` on `text` (`@Lob`, unbounded by design). No
>   `@Column(length)` changes — 255 already matches the DDL, so **no migration for these fields**.
> - `@Size(max = 255)` + explicit messages on the corresponding `Edit*Input` record fields;
>   `@Email` on `EditUserInput.emailAddress` (mirrors `UserImpl:330`).
> - Add `@Lob` to `ArgumentImpl.getText()` / `PositionImpl.getText()` and a Flyway migration
>   widening `arguments.text` / `positions.text` to `longtext`, matching `annotations.text`.
> - Sweep every registered command to confirm the 40 now-live DTO constraints don't reject valid
>   payloads, and give any DTO constraint that front-runs an entity constraint the entity's
>   user-facing message.
> - Add `xs:maxLength` restrictions to `doc/samples/project.xsd` and an import test asserting an
>   over-long name fails as validation, not as a 500.
> - Angular: add `ARTIFACT_NAME_MAX_LENGTH` to `shared/validation-limits.ts`, apply
>   `Validators.maxLength` + the `maxlength` attribute in the six migrated editors, and delete the
>   `#171` TODOs at `term-editor.ts:247` and `report-editor.ts:180`.
>
> **Note.** This likely obsoletes #176 — DTO-level `ConstraintViolation.getPropertyPath()` already
> yields input-DTO field names, which is what #176 asked `CommandController` to emit. Re-point
> #176 at deleting #132's per-editor name maps.

## 9. Sequencing and size

```
#171 ─┬─→ #173 wizards (inherits ARTIFACT_NAME_MAX_LENGTH)
      └─→ #176 (re-scoped: delete the per-editor maps)
```

Suggested slices, each independently reviewable:

1. `ApiCommandFactory` validation + `BeanValidationException` wrapping + the command sweep. The
   risky slice; land it alone.
2. Entity `@Size` (13 sites) + DTO `@Size`/`@Email` + messages.
3. `Argument` / `Position` `@Lob` + `V14` migration.
4. XSD restrictions + import test.
5. Angular constants, validators, `maxlength` attributes, TODO deletions.

Point estimate: **5**, up from whatever the body implies. Slice 1 is most of it, and it is a
behavior change across every write path rather than an annotation sweep.

# 176 — Report field violations using input-DTO field names, not JPA entity property names

## Summary

`CommandController` builds `CommandResult.FieldViolation.field` from
`BeanValidationException.getEntityPropertyNames()` (JPA entity property names). Angular forms are
built from input DTOs, so the two vocabularies diverge exactly where it matters, and nine per-editor
`{ entityProperty: controlName }` maps exist to re-route violations. This emits input-DTO field
names from the server so those maps can go away.

## Review outcome / decisions

Reviewed against current code (post #171/#177, #132/#175). **Not obsolete**, but rescoped:

1. **Input-DTO validation already emits DTO names.** `CommandInputValidator` (via
   `ValidatingCommandHandler`) bean-validates the bound input DTO and throws `BeanValidationException`
   with DTO property paths. So `name`/`text`/`email` (DTO `@Size`/`@Email`) already route correctly.
   The remaining leak is only the **entity-flush** path (`BeanValidationExceptionAdapter` →
   JPA property names) for constraints that fire at flush.
2. **Mechanism: `@FromEntityProperty` annotation** on the divergent input-DTO field, naming the JPA
   entity property it corresponds to. A resolver reflects these per input-DTO class and builds the
   entity-property → DTO-field map. Colocated with the DTO, no central table, no global-collision
   risk (`type`/`description` recur across entities).
3. **Class B control renames** (form control name ≠ DTO field): user-editor `roleNames` →
   `userRoleNames`; scenario-editor `scenarioType` → `scenarioTypeName`.
4. **Project org composite:** the single `organization` control is fed by two DTO fields
   (`organizationId`, `organizationName`). Handle with a small **shared** alias inside
   `applyCommandErrors` (`organizationId`/`organizationName` → `organization`), not a per-editor map.
5. **Gateway out of scope:** it surfaces these as an `INVALID_INPUT` message string, not structured
   `FieldViolation`s for a form. HTTP controller only.

## Verified starting state (release/2.0)

- `CommandController` lines ~128-147 build `FieldViolation` from `getEntityPropertyNames()`.
- `CommandInputValidator` + `ValidatingCommandHandler` present; `Edit*Input` carry `@Size` on
  name/text already.
- Nine `*_FIELD_MAP`s. Classified:
  - **Class A** (entity ≠ DTO, DTO == control) — fixed by server map, client entry dies:
    `canonicalTerm→canonicalTermId`, `primaryActor→primaryActorName`, `encryptedPassword→password`,
    `description→text`, `type→scenarioTypeName`, `roles→userRoleNames`.
  - **Class B** (DTO ≠ control) — needs control rename: user `roleNames`, scenario `scenarioType`.
  - **Composite** — project `organization` (two DTO fields → one control): shared alias.
  - **No-ops** — stakeholder `teamName→teamName`, `text→text`; empty report map: delete outright.

## Work items

### 4.1 `@FromEntityProperty` annotation (service-api)
New `@Retention(RUNTIME) @Target(FIELD)` annotation `@FromEntityProperty(String value)`. Annotate the
divergent input-DTO record components (verify each entity property name against the JPA entity during
implementation; annotate only fields whose constraint actually fires at entity flush):
- `EditGlossaryTermInput.canonicalTermId` → `@FromEntityProperty("canonicalTerm")`
- `EditUseCaseInput.primaryActorName` → `@FromEntityProperty("primaryActor")`
- `EditUserInput.password` → `@FromEntityProperty("encryptedPassword")`
- `EditUserInput.userRoleNames` → `@FromEntityProperty("roles")`
- `EditActorInput.text` → `@FromEntityProperty("description")`
- `EditScenarioInput.scenarioTypeName` → `@FromEntityProperty("type")`

### 4.2 Resolver + CommandController (service-impl)
- A small resolver: given the input-DTO `Class<?>`, reflect `@FromEntityProperty` components and
  return `Map<entityProperty, dtoField>` (cached per class).
- In the `EntityValidationException`/`BeanValidationException` arm, translate each entity property
  name via the resolver for the command's input DTO class (`apiCommandFactory.getInputType(commandType)`);
  pass through unchanged when unmapped; keep null `field` for a property with no DTO counterpart.
  Preserve the existing nested/indexed handling.

### 4.3 Client — rename Class B controls
- user-editor: `roleNames` → `userRoleNames` (FormControl, checkbox template binding, getters, and
  the `userRoleNames: value.roleNames` submit payload).
- scenario-editor: `scenarioType` → `scenarioTypeName` (FormControl + template; leave the unrelated
  `step.scenarioType` steps-table display field).

### 4.4 Client — shared alias + delete maps
- `form-errors.ts`: drop the `map` parameter from `applyCommandErrors`; add a small shared alias
  constant (`organizationId`/`organizationName` → `organization`) applied internally, documented.
  Keep the index-stripping fallback for `roles[0].name`-style paths.
- Delete all nine `*_FIELD_MAP`s and update every `applyCommandErrors(...)` call site.

### 4.5 Tests
- `CommandControllerTest`: update violation assertions to DTO names; add cases for the divergent
  fields (e.g. an `encryptedPassword` entity violation surfaces as `password`).
- Unit test for the resolver.
- Editor specs: update for the two renamed controls; remove map-based expectations.
- `form-errors` spec: alias behavior; `applyCommandErrors` without a `map` argument.

## Acceptance criteria

1. On the entity-flush path, `FieldViolation.field` carries the input-DTO field name, resolved from
   `@FromEntityProperty` on the command's input DTO class; unmapped names pass through; no-counterpart
   properties yield a null (command-level) field.
2. Class B controls renamed to match their DTO fields.
3. Project org routing handled by a shared alias, not a per-editor map.
4. All nine `*_FIELD_MAP`s and the `map` parameter of `applyCommandErrors` removed.
5. Nested/indexed path fallback preserved.
6. `mvn clean verify` and `npm test` green; assertions updated to the new names.

## Not in scope

- Gateway field-name translation (message string only).
- When a violation is produced or the 422 response shape; inline-vs-toast rendering (#133).

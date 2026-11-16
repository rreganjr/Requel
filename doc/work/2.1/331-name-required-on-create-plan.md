# #331 — Edit inputs: name required only on create

https://github.com/rreganjr/Requel/issues/331

Branch `331-name-required-on-create` from `release/2.1`, PR to `release/2.1`. Size 2.

## Summary

#316 made the Edit commands read a null name as "keep it", but left `@NotBlank` on the input
DTOs, so over the API and MCP an update must still send the current name. Make `name` required
only when creating, on the eight inputs in the issue's table.

Already done: `EditStoryInput.primaryActorName` has had no constraint since #171 turned DTO
validation on, and a null keeps the actor since #325; only a test is missing. `EditStepInput.name`
is not required either.

## Locked decisions (2026-10-29)

1. **One class-level constraint, `@RequiredOnCreate(id = "<idField>", fields = "name")`**, in
   `service-api`, with `RequiredOnCreateValidator`. Create (id null): each field must not be null or
   blank. Update (id set): null is allowed, `""` (or blank) is refused. The violation is reported on
   `name`, not the record, so field errors still route to the form control and gateway callers see
   the field. Chosen over validation groups, which would need `ValidatingCommandHandler` to pick a
   group per command.
2. `name` loses `@NotBlank` (keeps `@Size`) on `EditGoalInput`, `EditActorInput`,
   `EditUseCaseInput`, `EditScenarioInput`, `EditGlossaryTermInput`, `EditNonUserStakeholderInput`,
   `EditReportGeneratorInput` and `EditStoryInput`. The MCP and CLI schemas mark a field required
   from `@NotNull`/`@NotBlank`, so they stop listing `name` as always required with no further
   change.
3. The seven descriptions that said "name is required on every call, so send the current name to
   keep it" now say "name is required when creating; leave it out when editing to keep the current
   name".
4. `EditNullNameIT.editScenarioByIdWithoutANameIsRejectedAsInvalidInput` pinned #316's behavior and
   flips on purpose to "keeps the current name". The blank-name test stays.

## Test plan

`tmp/331-verify.sh`: `mvn clean verify -pl modules/coverage-report -am` and the coverage check
(backend only).

- `RequiredOnCreateTest` (new, `service-impl`), parameterized over the eight inputs: create with no
  name or a blank one is refused on `name`; create with a name passes; update without a name passes;
  update with `""` is refused on `name`.
- `EditNullNameIT`: EditScenario and EditGoal over the gateway with no name keep it; EditGoal create
  without a name is `INVALID_INPUT` naming `name`; EditStory update without `primaryActorName` keeps
  the actor; blank name on update still refused.

## Out of scope

- Annotation inputs (`text`), which #316 did not cover.
- Making a use case's primary actor optional (#325 kept it required).

## AC mapping

| Issue AC | Covered by |
|---|---|
| Text-only update keeps the name, per input | `RequiredOnCreateTest` (all eight), `EditNullNameIT` (gateway) |
| Create without a name refused on `name` | `RequiredOnCreateTest`, `EditNullNameIT` |
| Update with `name: ""` refused on `name` | `RequiredOnCreateTest` |
| EditStory update without `primaryActorName` keeps it | `EditNullNameIT` |
| Validation tests for create and update per input | `RequiredOnCreateTest` |

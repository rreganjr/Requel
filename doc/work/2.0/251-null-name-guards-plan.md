# 251 — `EditScenario` throws an NPE on a null name when editing by id

Milestone **v2.0**, labels `bug` / `gateway`. Backend-only, no Angular change.

## Summary

`EditScenarioInput.name` is the only `Edit*Input` in the API without `@NotBlank`. A null name
therefore passes validation, reaches `EditScenarioCommandImpl`, and dies inside
`JpaProjectRepository.findScenarioByProjectOrDomainAndName:193`, which calls `name.trim()`. The
caller sees `Cannot invoke "String.trim()" because "name" is null` as a command failure.

## Verified against `release/2.0`

- **The four siblings all require a name.** `EditGoalInput`, `EditStoryInput`, `EditUseCaseInput`
  and `EditActorInput` each carry `@NotBlank` on `name`; `EditScenarioInput` carries only `@Size`.
  That omission is the whole reason only Scenario reaches the NPE.
- **Two commands already guard anyway.** `EditUseCaseCommandImpl:131` skips the uniqueness lookup
  with `if (getName() != null && !getName().trim().isEmpty())` and `:181` guards the setter; and
  `EditActorCommandImpl:179`/`:187` do the same. Their inputs are `@NotBlank` too, so these guards
  exist for callers that never see a DTO.
- **Those callers are real.** `EditUseCaseCommandImpl:167` builds an `EditScenarioCommand` itself,
  and the XML importer and NLP analysis construct commands directly. Validation cannot help them.
- **`EditGoalCommandImpl` (`:125`, `:144`) and `EditStoryCommandImpl` (`:155`, `:187`) have the same
  unguarded shape** as Scenario. Unreachable through the gateway because their inputs are
  `@NotBlank`, but reachable the same way UseCase's guards anticipate.

## Locked decisions

- **Both halves of the AC, not a choice between them.** The ticket offers "preserve the current
  name" *or* "make it `@NotBlank` and return a field-level error". The evidence says do both, at
  different layers: `@NotBlank` for the gateway, guards for the direct callers. Either alone leaves
  a hole — validation cannot protect a command built in Java, and a guard alone would make Scenario
  the only entity accepting a nameless update.
- **No name-optional partial updates.** Confirmed with the developer: no entity supports them today
  and Scenario will not be the exception.
- **Goal and Story get the guards too.** Same latent defect, same two-line treatment, one CI run.

## Changes

1. `modules/service-api/.../dto/EditScenarioInput.java` — `@NotBlank` on `name`, matching the
   other four records exactly (annotation above the existing `@Size`).
2. `modules/project-jpa/.../command/EditScenarioCommandImpl.java` — guard the uniqueness lookup and
   the setter, copying `EditUseCaseCommandImpl`.
3. `modules/project-jpa/.../command/EditGoalCommandImpl.java` — the same guards.
4. `modules/project-jpa/.../command/EditStoryCommandImpl.java` — the same guards.
5. `modules/requel-app/src/test/.../service/EditNullNameIT.java` (new) — both layers:
   - `EditScenario` by id with no name, and with a blank name, are `INVALID_INPUT` at the gateway;
   - the Scenario, Goal and Story commands each keep their current name when edited with a null
     name and changed text;
   - supplying a name still renames, so the guard cannot swallow a real rename.

## Test plan (gate: `mvn clean verify`, script `tmp/251-verify.sh`)

No `requel-angular/**` change, so no vitest, no tsc, no e2e.

```bash
mvn -pl modules/requel-app -am test -Dtest='EditNullNameIT,ScenarioCommandTest,CommandGatewayIT'
mvn clean verify
```

`ScenarioCommandTest` and `CommandGatewayIT` are in the focused run because they exercise the three
commands and the gateway's validation mapping respectively — the guards must not change either.

## AC mapping

| AC | Covered by |
| --- | --- |
| Editing by id without a name preserves the name, or returns a field-level validation error | Both: change 1 gives the gateway a field-level `INVALID_INPUT`; changes 2-4 preserve the name for direct callers |
| No `NullPointerException` reaches the caller | `EditNullNameIT` — the gateway cases assert the exception kind, the command cases assert the surviving name |
| Covered by a test | `EditNullNameIT`, five cases |

## Out of scope

- The `like` in `findScenarioByProjectOrDomainAndName` (`:192`), which matches `%` as a wildcard —
  the same defect #284 records for `findPosition`. Worth folding into #284 rather than fixed here.
- Whether `Edit*Input` should support partial updates at all. Decided against above; revisiting is a
  design change, not a bug fix.

## Risks

- **`@NotBlank` is an API contract change.** An MCP client calling `EditScenario` by id without a
  name previously got a failed command; it now gets a validation error. Both are failures, so
  nothing that worked stops working — but the error shape changes, and that belongs in the PR body.
- Guards on Goal and Story change no reachable behaviour today; they are there so the next direct
  caller does not rediscover this.

## Process (CLAUDE.md)

Branch `251-null-name-guards` off `release/2.0`; `tmp/251-verify.sh` → `mvn clean verify`;
`commit.md` with `Closes #251`; the developer runs every `git`/`gh` command.

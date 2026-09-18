# 252 — empty input schemas on the scenario-step gateway tools (plan)

Issue: https://github.com/rreganjr/Requel/issues/252

## What is actually happening

`CommandRegistry` has a two-argument `register(commandType, factoryMethod)` overload whose javadoc
reads "Register with no input mapping (placeholder for future DTO wiring)". It delegates with
`Void.class` as the input type. `GatewayCommandCatalogImpl` passes that straight into the
descriptor, and the MCP layer renders it as `{"properties": {}, "type": "object"}` — a tool that
advertises no way to call it.

Fifteen commands use that overload. Eleven are not on `GatewayPolicyConfig.ALLOWED`, so they are
invisible. **Four are, and all four are uncallable MCP tools today**, not the one the ticket names:

    EditScenarioStep   CopyScenarioStep   ConvertStepToScenario   DeleteScenarioStep

## Decision: remove three, wire one

`EditScenario`'s applicator builds a complete step list and calls `c.setStepCommands(stepCmds)`, so
the list is replaced wholesale on every save. That decides three of the four:

- **`EditScenarioStep`** — redundant. Editing a step is the applicator's "existing plain step"
  branch, reached by sending the step with its `stepId`.
- **`DeleteScenarioStep`** — redundant. Omitting a step from the array deletes it, because the list
  is replaced rather than merged.
- **`CopyScenarioStep`** — approximable by sending a new step (`stepId: null`) carrying the same
  name and text. Not byte-identical to what the copy command does, but not worth a DTO on
  speculation; if a real need appears it can be wired then.

**`ConvertStepToScenario` is the exception and gets a real schema.** It is genuinely unreachable
through `EditScenario`: sending `isScenario: true` with an existing `stepId` routes to
`findScenarioById`, which walks `project.getScenarios()` and throws
`IllegalArgumentException: Scenario not found` for a plain step's id. There is no other path from a
plain step to a sub-scenario, so dropping it would remove capability rather than a duplicate.

## Why removing from ALLOWED is safe

`ALLOWED` gates the gateway surface only — `/api/gateway/commands` and the MCP tools generated from
the catalog. The Angular UI posts to `/api/commands` (`CommandController`), a different controller
that does not consult the policy, and it does not reference any of the four command types at all.
The three stay registered and callable there; only their gateway exposure goes away.

## The durable part: a catalog invariant

AC 2 asks the lockstep test to cover the distinction. The version that actually holds is an
invariant rather than four assertions: **no allowlisted, registered command may resolve to
`Void.class`.** That would have caught all four, and it catches the next one — any of the eleven
unexposed placeholder registrations reproduces this silently the day someone allowlists it.

It belongs in `McpToolCatalogLockstepIT`, not `GatewayCommandCatalogImplTest`: the unit test mocks
`CommandRegistry` and hands every command the same stub input type, so it cannot see real wiring.
The failure message must name the offending command types, since the whole problem is that an empty
schema looks like a working tool.

## Changes

**`modules/service-impl/.../gateway/GatewayPolicyConfig.java`** — drop `EditScenarioStep`,
`CopyScenarioStep` and `DeleteScenarioStep` from `ALLOWED`; keep `ConvertStepToScenario`. Comment
says what replaces each on the `EditScenario` steps array, so the next reader does not re-add them.

**`modules/service-api/.../dto/ConvertStepToScenarioInput.java`** (new) — `projectName`, `stepId`.
Carries a `@CommandDescription` (the mechanism added in #255) saying it promotes an existing plain
step to a sub-scenario in place and updates the scenarios referencing it.

**`modules/service-impl/.../command/ProjectCommandRegistrar.java`** — replace the placeholder
registration with a real one: resolve the project by name, find the step with
`findStepByIdAcrossScenarios`, call `setProjectOrDomain` and
`ConvertStepToScenarioCommand.setOriginalScenarioStep`, and extract the result as a scenario detail
DTO the way `EditScenario` does.

## Tests

- `McpToolCatalogLockstepIT` — the invariant, failure message naming offenders, plus a test for the
  ticket's own distinction: the three are absent from `ALLOWED`, the catalog and the tool list,
  while `ConvertStepToScenario` is present with `ConvertStepToScenarioInput` as its schema.
- `CommandGatewayIT` — the invariant also goes on the registry seam, inside the existing
  `everyAllowlistedCommandIsRegisteredAndAuthorizable` loop, which already walks `ALLOWED` and had
  the right shape. Two seams, because an empty schema can arrive either by registering without a
  DTO or by allowlisting something that was.
- `CommandGatewayIT` — a `ConvertStepToScenario` execution test. Not the domain round trip planned
  here: `ScenarioStepCommandTest.convertStepToScenario` already covers the conversion semantics at
  the command level and duplicating it adds nothing. What is new and unproven is the applicator, so
  the test builds a step through `EditScenario`'s steps array, converts it, and asserts the promoted
  scenario keeps the step's name. That last assertion matters: the command is an
  `EditScenarioCommand` underneath and builds its scenario from name, text and type, so the
  applicator has to carry them over from the step or the conversion yields an unnamed scenario —
  found by reading the existing command test, which sets all three by hand.

## Out of scope

- The eleven unexposed placeholder registrations. The invariant only binds allowlisted commands;
  giving the rest real DTOs is separate work.
- Standalone step editing through the gateway — that is what this removes, deliberately.

## Gate

`mvn clean verify`. No frontend change here, so `npm test` is not part of this ticket's gate.

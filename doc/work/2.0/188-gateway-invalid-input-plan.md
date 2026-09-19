# 188 — Gateway maps id-lookup misses to INVALID_INPUT, not EXECUTION_ERROR

## Summary

`InProcessCommandGateway.execute` has no `catch` for `IllegalArgumentException`, so an id-lookup
miss thrown from a registration's input applicator (during `apiCommandFactory.newCommand`) falls
through to the generic `catch (Exception)` and is reported as `GatewayException.Kind.EXECUTION_ERROR`.
That is a caller error — the wrong id — not a server failure. The HTTP `CommandController` already
classifies it correctly (`catch (IllegalArgumentException)` → 400 BAD_REQUEST). This aligns the
gateway with that behaviour: `IllegalArgumentException` → `INVALID_INPUT`.

Downstream this matters because MCP maps `INVALID_INPUT` → JSON-RPC `INVALID_PARAMS` but
`EXECUTION_ERROR` → `INTERNAL_ERROR`, so today an LLM agent that passes a stale id sees a server
crash instead of "fix your arguments." The gateway HTTP wrapper also returns 400 vs 422 depending
on the classification.

Unblocks #187's gateway acceptance criteria. Backend-only.

## Verified starting state (release/2.0, post-#178/#180/#189)

- `InProcessCommandGateway.execute` catch order today: `AuthorizationException` → UNAUTHORIZED,
  re-throw `GatewayException`, `EntityValidationException` → INVALID_INPUT, `Exception` →
  EXECUTION_ERROR (warn-logged). No `IllegalArgumentException` arm.
- `bindInput` already maps Jackson `IllegalArgumentException` (malformed input) to INVALID_INPUT,
  but it runs *before* the try block, so it does not cover applicator-thrown lookups.
- `ProjectCommandRegistrar` container lookups (`findGoalContainerById` / `findStoryContainerById` /
  `findActorContainerById`) were changed by #189 to throw `EntityValidationException`, so they
  already surface as INVALID_INPUT (covered by existing IT tests). The gap is the **child / entity
  lookups** that still throw `IllegalArgumentException`: `findGoalById`, `findStoryById`,
  `findActorById`, `findUseCaseById`, `findStakeholderById`, `findScenarioById`, `findStepById`,
  `findGlossaryTermById`, `findReportGeneratorById`, `findGoalRelationById`, and the name-based
  finds — 19 sites in total, all currently mapped to EXECUTION_ERROR through the gateway.

## Work item

In `InProcessCommandGateway.execute`, add a `catch (IllegalArgumentException e)` arm **between** the
`EntityValidationException` and the generic `Exception` arms:

```java
} catch (IllegalArgumentException e) {
    // An id-lookup miss (e.g. "Goal not found") thrown from a registration input applicator while
    // the command is built. That is the caller naming something that does not exist, so INVALID_INPUT
    // rather than EXECUTION_ERROR — MCP then maps it to INVALID_PARAMS, not INTERNAL_ERROR, and the
    // gateway HTTP wrapper returns 400 not 422, matching CommandController. Logged at debug because a
    // caller error is not a server problem. Mirrors the HTTP controller's IllegalArgumentException arm.
    log.debug("Gateway command '{}' rejected invalid input: {}", commandType, e.getMessage());
    throw new GatewayException(GatewayException.Kind.INVALID_INPUT,
            "Input for command '" + commandType + "' is invalid: " + e.getMessage(), e);
}
```

Ordering is safe: `IllegalArgumentException` is unrelated to `AuthorizationException`,
`GatewayException`, and `EntityValidationException`, and is more specific than `Exception`.

## Test plan

- `CommandGatewayIT`: add a test that an association command with a **valid container but an unknown
  child id** returns `INVALID_INPUT` (was EXECUTION_ERROR) — e.g. `AddGoalToGoalContainer` with a
  real use-case container + `containerType=UseCase` but a non-existent `goalId`, driving
  `findGoalById` → `IllegalArgumentException`. Assert `ex.getKind() == INVALID_INPUT`. This fails on
  the current code and passes after the fix.
- The existing container-type INVALID_INPUT tests (#189) continue to pass unchanged.
- Gate: `mvn clean verify` (`npm test` untouched — no client change).

## Acceptance criteria

- An id-lookup miss issued through the gateway yields `GatewayException.Kind.INVALID_INPUT`.
- Behaviour matches the HTTP controller (400 BAD_REQUEST) and lets MCP map to INVALID_PARAMS.
- No change to authorization, not-allowed, not-found, or genuine execution-error classification.

## Not in scope / accepted tradeoff

- A genuine internal bug that happens to throw `IllegalArgumentException` will now be reported as
  INVALID_INPUT rather than EXECUTION_ERROR. This is the same tradeoff the HTTP controller already
  makes, and the registrar uses `IllegalArgumentException` specifically for caller-facing id misses,
  so the classification is correct for every current thrower. Debug logging keeps the detail without
  implying a server fault.
- No change to `CommandController` (already correct) or to MCP/CLI mapping (they already interpret
  the two kinds correctly).

# #482 — rc4: a refused save shows "changed elsewhere" instead of the server's reason

https://github.com/rreganjr/Requel/issues/482

Branch `482-stale-version-message` from `release/2.1` (at `2.1.0-rc4`), PR to `release/2.1`.
Size 2. Found in the rc4 acceptance test (#420); the fix ships in rc5.

## Summary

Saving a scenario with a new step named like an existing step was refused, rightly, with "The
name conflicts with an existing Step: step 5 ... already uses the name 'Download the receipt'".
The form said "This scenario was changed elsewhere. Your copy has been refreshed".

`/api/commands` answers 409 for every business-rule refusal (`EntityException`), uniqueness
conflicts included, and for a stale version. The editors treat any 409 as a stale version: they
reload and show "changed elsewhere". 2.0 did the same; #345 noted "Angular already reads 409 as a
conflict", but it reads it as a stale version.

## Locked decisions (2026-11-11)

1. **The server gives a stale version its own code.** `CommandController` answers
   `StaleVersionException` (every stale-version path throws it since #345: the version checks, the
   flush-time optimistic-lock adapter, the assistant definitions' lock check) and Hibernate's
   `OptimisticLockException` with 409 and error code `STALE_VERSION`. Every other 409 keeps
   `CONFLICT`. The status stays 409, so other clients see no change in status.
2. **Angular decides by the code.** `CommandResult` gains `code` (the error body's `error`), and
   `isStaleVersion(result)` (409 and `STALE_VERSION`) replaces the bare `status === 409` checks in
   the goal, story, actor, stakeholder, use case and scenario editors and the project's definitions
   list and editor. Any other 409 shows the server's message and leaves the form as it is.
3. **The gateway is unchanged.** It already maps both to `GatewayException.Kind.CONFLICT` (#345);
   MCP and the CLI show the message.

## Test plan

`tmp/482-verify.sh` runs the backend verify, the coverage-modules check, the Angular typechecks, a
dev build and the full Angular unit suite.

- `CommandControllerTest`: a `StaleVersionException` and an `OptimisticLockException` are 409
  `STALE_VERSION`; a uniqueness conflict and another `EntityException` are 409 `CONFLICT`.
- `command.service.spec`: the code is carried through, and `isStaleVersion` tells the two apart.
- `scenario-editor.spec`, `goal-editor.spec`: a 409 `CONFLICT` shows the server's message, with no
  reload, and the edit is kept; the stale-version cases now send `STALE_VERSION`.
- `project-definition-editor.spec`: a 409 `CONFLICT` shows the server's message.
- `story-editor.spec`, `project-definitions.spec`: the stale-version cases send `STALE_VERSION`.
- Local build on the rc4 database: a step renamed to an existing step's name shows the name
  conflict.

## Out of scope

- How the scenario editor offers to reuse the existing step (the server message says how).

## AC mapping

| AC | Covered by |
|---|---|
| A name-conflict refusal shows the server's message and keeps the form | scenario-editor.spec, goal-editor.spec, local build |
| A stale version still reloads with "changed elsewhere" | the existing stale-version specs, now coded |
| Specs per editor; a controller test for the two codes | the above |

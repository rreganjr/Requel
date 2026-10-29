# #345 — Caller mistakes reach gateway callers as EXECUTION_ERROR

https://github.com/rreganjr/Requel/issues/345

Branch `345-gateway-error-kinds` from `release/2.1`, PR to `release/2.1`. Size raised from 2 to 3
(decision 3 widened the scope).

## Summary

A gateway caller (MCP, `requel-cli`, the REST gateway) cannot tell "fix your arguments" from "the
server broke": uniqueness conflicts, stale versions and unknown project names all come back as
`EXECUTION_ERROR` (MCP `INTERNAL_ERROR`), a stale version is retried three times first, and
`EditNonUserStakeholder` with a user stakeholder's id fails with a `ClassCastException`. Give each
mistake its own kind, stop retrying what cannot succeed, and drop the unread
`EditProjectInput.id` (moved here from #340).

## Locked decisions (2026-10-28)

1. **A new `GatewayException.Kind.CONFLICT`** for uniqueness conflicts and stale versions. The
   gateway HTTP wrapper answers 409; `RestCommandGateway` maps 409 back to `CONFLICT`; MCP reports
   it as `INVALID_PARAMS` with the message (JSON-RPC has no conflict code). `/api/commands` already
   answers 409 for both and Angular already reads 409 as a conflict, so the UI does not change.
2. **Stale versions are not retried.** `EntityLockException.staleEntity(...)` returns a new
   `StaleVersionException extends EntityLockException`; `RetryOnLockFailuresCommandHandler` rethrows
   it without retrying. Lock-acquisition failures and Hibernate's own stale-state failures (a
   concurrent write inside the transaction) still retry: a fresh attempt can succeed.
3. **Not found is `NOT_FOUND` everywhere.** `NoSuchEntityException` (unknown `projectName` and the
   repository lookups) and the binders' unknown-id refusals both report not found: 404 on
   `/api/commands` and the gateway HTTP wrapper, `NOT_FOUND` at the gateway, `INVALID_PARAMS` on MCP
   (as today for `NOT_FOUND`). The binders' `IllegalArgumentException("<X> not found: <id>")` (about
   25 sites in the Project, Annotation, Tag and Provenance registrars, #342/#343 included) become a
   new `NotFoundInputException extends IllegalArgumentException`, so anything catching
   `IllegalArgumentException` keeps working while the mappers can tell the two apart. `Kind.NOT_FOUND`'s
   javadoc widens from "unknown command type" to "unknown command type, or a named entity that does
   not exist".
4. **`EditProjectInput.id` is removed** from the record, its `@CommandDescription` and the Angular
   project editor. The server binds with Spring's `ObjectMapper`, which ignores unknown properties,
   so a 2.0 client still sending `id` keeps working over REST and the gateway. MCP tool schemas are
   generated with `additionalProperties:false` from the record, so the tool simply stops offering it.
5. **`EditNonUserStakeholder` with a user stakeholder's id** is refused in the binder with
   `IllegalArgumentException` (`INVALID_INPUT`): "Stakeholder <id> is a user stakeholder; use
   EditUserStakeholder."
6. **Folded in:** revert the temporary `runs-on: ubuntu-26.04` that #425 merged into `ci.yml`
   (both jobs back to `ubuntu-latest`). #425's run on 26.04 was green, which met #300's AC.

## Compatibility (minor release)

Every change is to how a failure is reported, plus a field the server never read. No request
that succeeds today fails or behaves differently. Status changes a 2.0 client could observe:
unknown `projectName` 409 → 404 on `/api/commands`; uniqueness and stale version
`EXECUTION_ERROR`/422 → `CONFLICT`/409 on the gateway; unknown ids 400 → 404.

## Contracts

| Failure | Exception | `/api/commands` | Gateway kind | Gateway HTTP | MCP |
|---|---|---|---|---|---|
| Name already taken | `UniquenessConflictException` (new, from `EntityException.uniquenessConflict`) | 409 | `CONFLICT` | 409 | `INVALID_PARAMS` |
| Caller's version is stale | `StaleVersionException` (new) | 409 | `CONFLICT` | 409 | `INVALID_PARAMS` |
| Unknown `projectName` / repository miss | `NoSuchEntityException` | 404 | `NOT_FOUND` | 404 | `INVALID_PARAMS` |
| Unknown id in a binder | `NotFoundInputException` (new) | 404 | `NOT_FOUND` | 404 | `INVALID_PARAMS` |
| User stakeholder id on `EditNonUserStakeholder` | `IllegalArgumentException` | 400 | `INVALID_INPUT` | 400 | `INVALID_PARAMS` |
| Lock wait / deadlock after 3 retries | `EntityLockException` | 409 (unchanged) | `EXECUTION_ERROR` (unchanged) | 422 | `INTERNAL_ERROR` |

`UniquenessConflictException` and `StaleVersionException` stay `EntityException` subclasses, so
code catching `EntityException` is unaffected.

## Step by step

1. `platform-core`: `UniquenessConflictException` (returned by both `uniquenessConflict`
   factories), `StaleVersionException` (returned by `staleEntity`); `RetryOnLockFailuresCommandHandler`
   rethrows `StaleVersionException` before its `EntityLockException` branch.
2. `service-api` (or wherever the registrars can reach): `NotFoundInputException extends
   IllegalArgumentException`; replace the binders' "not found" `IllegalArgumentException`s.
3. `gateway-api`: `Kind.CONFLICT`; widen `NOT_FOUND`'s javadoc.
4. `InProcessCommandGateway`: catch `NotFoundInputException` and `NoSuchEntityException` →
   `NOT_FOUND`, `UniquenessConflictException`/`StaleVersionException` → `CONFLICT`, ahead of the
   existing `IllegalArgumentException` and generic branches.
5. `GatewayCommandController.statusFor`: `CONFLICT` → 409. `RestCommandGateway`: 409 → `CONFLICT`.
   `McpWriteService.mapGatewayException`: `CONFLICT` → `INVALID_PARAMS`. `requel-cli` prints the
   kind it gets; check it has no exhaustive switch to extend.
6. `CommandController`: `NoSuchEntityException` and `NotFoundInputException` → 404 `NOT_FOUND`,
   ahead of the `EntityException` (409) and `IllegalArgumentException` (400) branches.
7. `ProjectCommandRegistrar` `EditNonUserStakeholder` binder: refuse a non-`NonUserStakeholder`.
8. `EditProjectInput`: drop `id` and "The id field is ignored." Angular `project-editor.ts`: stop
   sending `id`; its spec updated.
9. `ci.yml`: `runs-on: ubuntu-latest` on both jobs.

## Test plan

`tmp/345-verify.sh`: `mvn clean verify -pl modules/coverage-report -am`, then the Angular unit
specs touched, typecheck and a dev build (`requel-angular/**` changed).

- **Gateway (`CommandGatewayIT`)**, asserting the kind: a duplicate goal name → `CONFLICT`; an
  `EditGoal` with a stale `version` → `CONFLICT`, and the log shows no retry; `EditGoal` on an
  unknown `projectName` → `NOT_FOUND`; `EditGoal` with an unknown `goalId` → `NOT_FOUND`;
  `EditNonUserStakeholder` with a user stakeholder's id → `INVALID_INPUT`.
- **MCP (`McpWriteService` unit test):** `CONFLICT` and `NOT_FOUND` map to `INVALID_PARAMS`,
  `EXECUTION_ERROR` to `INTERNAL_ERROR`.
- **REST client (`RestCommandGateway` test):** 409 → `CONFLICT`, 404 → `NOT_FOUND`.
- **`/api/commands` (`AuthorizationIT` or a `CommandControllerTest` case):** unknown `projectName`
  → 404; unknown id → 404; duplicate name → 409.
- **`RetryOnLockFailuresCommandHandlerTest`:** `StaleVersionException` is thrown after one
  attempt; a plain `EntityLockException` is still retried.
- **`EditProjectInput`:** a request carrying `id` still binds and succeeds; the MCP lockstep test
  passes without it.
- Existing tests asserting the old kinds or statuses (`#340`'s 409, #342/#343's `INVALID_INPUT` for
  unknown ids) are updated: those behaviors changed on purpose.

## Out of scope

- Other `EntityException`s (business-rule refusals) stay 409 / `EXECUTION_ERROR`.
- Query-side errors (`InProcessQueryGateway` already maps a miss to 404).

## Risks

- **Tests pinned to the old codes.** Several ITs assert 409 or `INVALID_INPUT` for not-found;
  they are updated deliberately and listed in the PR.
- **A 2.0 script keyed on the old status codes.** Only failure statuses change; listed above.

## AC mapping

| Issue AC / item | Covered by |
|---|---|
| Uniqueness conflict kind | decision 1; `CommandGatewayIT` |
| Stale version not retried, conflict kind | decision 2; `RetryOnLockFailuresCommandHandlerTest`, `CommandGatewayIT` |
| `EditNonUserStakeholder` user id → validation error | decision 5; `CommandGatewayIT` |
| MCP error code per kind | `McpWriteService` test |
| (from #340) unknown names → NOT_FOUND | decision 3; `CommandGatewayIT`, `/api/commands` test |
| (from #340) remove `EditProjectInput.id` | decision 4 |

# UI Refactor Plan Completeness Review (Codex)

Scope reviewed:
- `doc/UI_REFACTOR_PLAN.md`
- `doc/IDENTITY_MIGRATION.md` (dependency alignment check)
- `doc/RELEASE_GUIDE.md` (delivery/release alignment check)

This review focuses on implementation completeness: contract consistency, delivery gates, operational safety, and cross-plan dependencies.

## Findings (Ordered by Severity)

### Critical

1. Optimistic locking is declared but not wired into write contracts.
- Evidence:
  - `doc/UI_REFACTOR_PLAN.md:943` says entities include `version` for optimistic locking.
  - Edit/delete command inputs listed in phases do not include `version` (examples: `:714-719`, `:742-747`, `:773-775`, `:827-832`).
- Risk:
  - Lost updates in concurrent editing and unclear conflict handling behavior.
- Recommendation:
  - Require `version` on every mutating command input (`Edit*`, `Delete*`, relation edits).
  - Define conflict response contract (`409`, structured error payload, user-facing retry behavior).

### High

1. Query contract is internally inconsistent (paginated envelope vs raw arrays).
- Evidence:
  - `doc/UI_REFACTOR_PLAN.md:941` defines list responses as `{ items, total, page, pageSize }`.
  - Phase endpoint definitions still describe raw arrays, e.g. `UserDto[]` (`:643`), `StakeholderDto[]` (`:696`), `GoalDto[]` (`:722`).
- Risk:
  - Frontend/backend drift and avoidable rework once implementation starts.
- Recommendation:
  - Pick one standard and apply it uniformly in all endpoint definitions.
  - If pagination is optional for some endpoints, document explicit exceptions.

2. No phase exit criteria or test gates are defined.
- Evidence:
  - Phases list work items but no explicit acceptance criteria (`doc/UI_REFACTOR_PLAN.md:575-881`).
  - Only one generic verify statement appears in Phase 0 (`:620`).
- Risk:
  - “Done” becomes subjective; regressions likely during multi-phase coexistence.
- Recommendation:
  - Add a Definition of Done per phase:
    - required backend tests,
    - required frontend tests,
    - smoke/e2e scenarios,
    - demo-able acceptance checklist.

3. Cutover/rollback strategy is missing for coexistence and Echo2 removal.
- Evidence:
  - Coexistence is acknowledged (`doc/UI_REFACTOR_PLAN.md:577`) and Echo2 removal is planned (`:871-881`), but no rollback path is documented.
- Risk:
  - If Angular release blocks production workflows, rollback steps are undefined.
- Recommendation:
  - Add a cutover playbook:
    - runtime toggle/routing strategy during migration,
    - release order,
    - rollback procedure (including DB/schema assumptions),
    - “go/no-go” checklist for Phase 10.

4. Identity migration dependency is not integrated into UI refactor sequencing.
- Evidence:
  - UI plan relies on current user/password/role model (`doc/UI_REFACTOR_PLAN.md:553-559`, `:599`).
  - Identity plan introduces `platform-identity` and role/authority remapping (`doc/IDENTITY_MIGRATION.md:14-15`, `:33-40`, `:71-84`).
- Risk:
  - Duplicate auth work or contract churn mid-migration.
- Recommendation:
  - Add an explicit dependency note:
    - either “UI refactor pins to current identity model until Phase X,”
    - or “Identity migration pre-req milestones required before API auth implementation.”

### Medium

1. SSE query-token risk is accepted but lacks mitigations.
- Evidence:
  - `doc/UI_REFACTOR_PLAN.md:447-448` acknowledges token exposure in URL/logs and accepts risk.
- Recommendation:
  - Document minimum mitigations: access-log query redaction, short-lived stream token, and explicit TLS-only requirement.

2. Operational observability is not specified.
- Evidence:
  - Event bridge and SSE architecture are defined (`doc/UI_REFACTOR_PLAN.md:535-544`) but no metrics/alerts/logging plan.
- Recommendation:
  - Add baseline telemetry:
    - command latency/error rates by type,
    - query latency/error rates,
    - active SSE connections/reconnect rates,
    - auth failure rates.

3. Effort estimates have no owners, dates, or critical path dependencies.
- Evidence:
  - Relative-size estimates exist (`doc/UI_REFACTOR_PLAN.md:1031-1047`) without schedule/ownership.
- Recommendation:
  - Add a lightweight execution table: owner, target window, blockers/dependencies per phase.

## Completeness Summary

- Architecture completeness: strong.
- API/auth direction: strong.
- Delivery completeness (quality gates, rollout safety, dependency management): incomplete.

Current readiness assessment: good technical blueprint, not yet fully execution-ready for low-risk delivery.

## Suggested Minimum Additions Before Implementation Starts

1. Resolve optimistic locking contract (`version` in write DTOs + 409 conflict behavior).
2. Normalize list endpoint response format across the full doc.
3. Add phase-by-phase Definition of Done with required tests.
4. Add cutover + rollback plan for coexistence and final Echo2 removal.
5. Add identity migration dependency stance (pin vs prerequisite).

## Follow Up Review

### What Improved Since The First Review

- Optimistic locking is now specified with `version` fields and a `409 Conflict` contract (`doc/UI_REFACTOR_PLAN.md:1246-1262`), and edit/delete command inputs now include `version` across phases (examples: `:937`, `:962`, `:1012-1017`, `:1041-1045`, `:1072-1073`, `:1097`, `:1126-1129`).
- Query list response format is now standardized as a paginated envelope (`doc/UI_REFACTOR_PLAN.md:1242`).
- Cutover/rollback and phase Definition of Done were added (`doc/UI_REFACTOR_PLAN.md:1353-1369`).
- Identity migration sequencing/dependency notes were added (`doc/UI_REFACTOR_PLAN.md:1347-1351`).
- Authorization design is now explicitly documented and linked via `doc/AUTH_ARCH.md` (`doc/UI_REFACTOR_PLAN.md:859-861`).

### Remaining Findings (Ordered by Severity)

### High

1. Authorization contract drift between `UI_REFACTOR_PLAN.md` and `AUTH_ARCH.md`.
- Evidence:
  - UI plan JWT claims are documented as `sub`, `roles`, `exp` (`doc/UI_REFACTOR_PLAN.md:287`, `:882`).
  - `AUTH_ARCH.md` includes `permissions` in JWT and Angular depends on it (`doc/AUTH_ARCH.md:427-429`, `:458-460`).
  - `AUTH_ARCH.md` requires `GET /api/projects/{projectId}/my-permissions` (`doc/AUTH_ARCH.md:440`, `:548`), but this endpoint is not listed in UI plan API conventions (`doc/UI_REFACTOR_PLAN.md:1192-1212`).
  - UI plan says all project features are visible to any authenticated user (`doc/UI_REFACTOR_PLAN.md:855`), while `AUTH_ARCH.md` defines fine-grained project permission gating in Angular (`doc/AUTH_ARCH.md:435-505`).
- Recommendation:
  - Choose one final permission exposure model and align both docs:
    - JWT includes `permissions` vs JWT roles-only,
    - whether `/my-permissions` is required,
    - whether Angular project UI is coarse role-gated or permission-gated.

2. Admin endpoint strategy in `AUTH_ARCH.md` conflicts with command-dispatch API shape.
- Evidence:
  - `AUTH_ARCH.md` examples use URL-based admin rules for `/api/admin/**` (`doc/AUTH_ARCH.md:407`, `:556`).
  - UI plan uses command dispatch at `/api/commands/{commandType}` and user endpoints at `/api/users` (`doc/UI_REFACTOR_PLAN.md:1188`, `:1195`), not `/api/admin/**`.
- Risk:
  - URL-only role checks may not protect admin-only command types under shared `/api/commands/{commandType}`.
- Recommendation:
  - Document the real enforcement point for admin-only commands (e.g., `AuthorizingCommandHandler` + per-command requirements, with optional method-level `@PreAuthorize` where applicable) and remove/adjust `/api/admin/**` examples unless those routes will actually exist.

### Medium

1. `AUTH_ARCH.md` interface examples are internally inconsistent for current-user access.
- Evidence:
  - `AuthorizableCommand` extends `EditCommand` and defines only `getAuthorizationRequirement()` (`doc/AUTH_ARCH.md:115-122`).
  - `AuthorizingCommandHandler` uses `command.getEditedBy()` (`doc/AUTH_ARCH.md:173`), but the `EditCommand` snippet shows only `setEditedBy(User)` (`doc/AUTH_ARCH.md:74-76`).
- Recommendation:
  - Clarify the contract by either:
    - adding `getEditedBy()` to the relevant interface, or
    - passing the resolved current user to the handler separately and avoiding command-side getters.

2. Cutover statement and optimistic-locking prerequisite currently conflict.
- Evidence:
  - UI plan says no schema changes/migrations are needed for Angular frontend (`doc/UI_REFACTOR_PLAN.md:1354`).
  - Open question states `@Version` may require Flyway migrations (`doc/UI_REFACTOR_PLAN.md:1402`).
- Recommendation:
  - Make rollback/cutover conditional:
    - Path A: existing `@Version` fields already present (no schema migration),
    - Path B: version columns added via Flyway (document migration/rollback implications).

3. Query-side authorization exception mapping is not explicit.
- Evidence:
  - Command-side mapping is described via `ExceptionMappingCommandHandler` (`doc/AUTH_ARCH.md:522`).
  - Query authorization throws `AuthorizationException` in `ProjectAccessChecker` (`doc/AUTH_ARCH.md:359-360`), but query paths do not traverse command handlers.
- Recommendation:
  - Add explicit REST-layer mapping (e.g., `@ControllerAdvice`) for query-side `AuthorizationException` to ensure consistent HTTP `403` payloads.

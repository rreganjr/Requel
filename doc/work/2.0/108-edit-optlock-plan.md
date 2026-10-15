Scope expansion for #108: wire the caller-supplied optimistic-lock `version` through **every** `Edit*` command whose input DTO already declares a `version` field, not just `EditGoal`.

`EditGoal` is done and its tests pass. The pattern it establishes: on an update, reload the persisted entity, compare the caller's `version` to the persisted version, and throw `EntityLockException.staleEntity(...)` on a mismatch (a stale update fails cleanly instead of silently overwriting). Conflict surfacing needs no new code — `CommandController` already maps `EntityException` / `OptimisticLockException` to **HTTP 409**. `create` (no id / no existing entity) is never version-checked.

## Commands to update (10 total)

**Shared hierarchy** — interface `EditProjectOrDomainEntityCommand`, impl base `AbstractEditProjectOrDomainEntityCommand`. Add `setExpectedVersion(Integer)` to the interface once and a `checkExpectedVersion(entity)` helper to the base once; each command calls the helper in `execute()` and gets wired in the registrar.

1. ✅ `EditGoal` — `EditGoalInput.goalId` (done; will be refactored onto the shared helper)
2. `EditActor` — `EditActorInput.actorId`
3. `EditStory` — `EditStoryInput.storyId`
4. `EditUseCase` — `EditUseCaseInput.useCaseId`
5. `EditScenario` — `EditScenarioInput.scenarioId` (lock check on the top-level scenario; nested step DTOs are out of scope)
6. `EditNonUserStakeholder` — `EditNonUserStakeholderInput.stakeholderId`
7. `EditUserStakeholder` — `EditUserStakeholderInput` (resolved by project + username; no id)

**Separate hierarchies** — no shared base; add the setter to each interface and the field + check to each impl individually.

8. `EditGoalRelation` — `EditGoalRelationInput` (from/to goal names; impl extends `AbstractProjectCommand`)
9. `EditProject` — `EditProjectInput.projectName` (aggregate root; `EditProjectCommand` in project-domain / project-jpa)
10. `EditUser` — `EditUserInput.username` (user-domain / user-jpa)

## Approach

- Interface: `setExpectedVersion(Integer)`; non-null on an update triggers the check, null (or create) skips it.
- Impl: reload the entity, throw `EntityLockException.staleEntity(entityType, entity, Updating)` when `expectedVersion != persistedVersion`.
- Registrar wiring: call `c.setExpectedVersion(i.version())` on the **edit** branch only, in `ProjectCommandRegistrar` and `UserCommandRegistrar`.
- `EditUserStakeholder` and `EditGoalRelation` have no id field in their input; the registrar already uses `version != null` as the "this is an edit" signal to resolve the existing entity by natural key. Keep that gate (to preserve create-vs-edit semantics) and simply add the lock check inside it. (Considered fully decoupling `version` from the edit signal, but that would turn a create against an existing natural key from a uniqueness conflict into a silent upsert — out of scope for this ticket.)
- Tests (mirror `GoalCommandTest`): per command — stale-version update rejected, matching-version update succeeds, create ignores `version`.

## Known limitation

The check compares the caller's `version` against the freshly-loaded persisted version at command time (a small lock window), matching the `EditGoal` v1 approach — not full end-to-end JPA `@Version` propagation into the persistence context. Acceptable for v1; noted for a future hardening ticket if needed.

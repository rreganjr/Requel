# 240 — Backend `DeleteProject` command (cascade, auth, audit)

Epic #239 · child #240. Foundation for #241 (UI) and #242 (MCP/CLI gateway).

## Summary

Requel can delete every child entity of a project but not a project itself: there is no
`DeleteProjectCommand` interface, impl, factory method, API DTO, or registration, and no
`Project[Delete]` permission. This ticket adds a first-class, audited project delete on the CQRS
command path that removes the whole aggregate, authorized as the acting user's `Project[Delete]`.

## Current state (verified in tree)

- **No JPA cascade to lean on.** `AbstractProjectOrDomain` maps `actors`, `goals`, `stories`,
  `useCases`, `scenarios`, `stakeholders`, `teams`, `reportGenerators`, `glossaryTerms` as
  `@OneToMany(cascade = {PERSIST, REFRESH})` — no `REMOVE`, no `orphanRemoval`. `annotations` are
  `@ManyToMany` via join tables. So `ProjectRepository.delete(project)` will not remove children;
  deletion must be explicit.
- **Delete-command pattern** (e.g. `DeleteGoalCommandImpl`, `DeleteStakeholderCommandImpl`):
  extends `AbstractEditProjectCommand`, implements the command + `AuthorizableCommand` +
  `ProjectScopedCommand`. `execute()` detaches annotations (`RemoveAnnotationFromAnnotatable`),
  drops glossary-term referers, detaches from containers / relations via existing sub-commands
  **marked auth-exempt** (`AuthorizationExemptable.setAuthorizationExempt(true)`, #75), then
  `getRepository().delete(entity)`. Auth: `RequiresStakeholderPermission(X.class, "Delete")`.
- **`DeleteStakeholder` deletes only the association** — `stakeholder.removeFromProject()` then
  `delete(stakeholder)`; it never deletes the `User`. Cascading user-stakeholders on a project
  delete is therefore safe.
- **No `Project[Delete]` permission.** `StakeholderPermissionsInitializer.getPermissionTypes()`
  seeds `Project[Edit]` and `Project[Grant]` only (child types have Edit/Grant/Delete).
- **Creator gets all permissions.** `EditProjectCommandImpl` (create branch) grants the creator's
  `UserStakeholder` every `findAvailableStakeholderPermissions()`. So once `Project[Delete]` is
  seeded, new project creators receive it automatically. `SystemAdmin` bypasses all auth checks.
- **API wiring** (per `DeleteGoal`): a `Delete<Entity>Input` record in `service-api/.../dto`
  (`projectName`, id, `version`), registered in `ProjectCommandRegistrar` (mapping input→command),
  with a `ProjectCommandFactory.newDelete<Entity>Command()` method. These delete commands are what
  #242 later exposes on the gateway.

## Locked decisions

- **Cascade by orchestration, not mapping change.** `DeleteProjectCommandImpl` deletes each child
  by invoking the existing `Delete*Command`s (auth-exempt), reusing their tested reference-cleanup
  and per-entity audit, then deletes the project's own annotations/tag-assignments/teams and the
  project row. We do **not** add `CascadeType.REMOVE`/`orphanRemoval` to the aggregate mappings
  (would change persistence semantics repo-wide and still not cover the `@ManyToMany`/`@Any` join
  tables) and do **not** hand-roll native bulk deletes (bypasses cleanup and the annotation
  registry). This mirrors how `DeleteGoal` already composes `RemoveGoalFromGoalContainer` /
  `DeleteGoalRelation`.
- **Delete order (reference-safe):** scenarios → use-cases → stories → actors → goals → glossary
  terms → report generators → stakeholders (non-user and user) → teams → project annotations +
  tag assignments → `delete(project)`. Containers before goals so goal referer sets are already
  emptied; glossary terms after the entities that refer to them.
- **All stakeholders removed, incl. user-stakeholders.** Deleting the project severs every
  stakeholder association (safe — no `User` is deleted). The gateway's non-user-only guard (#242)
  governs deleting an *individual* stakeholder, not whole-project cascade, so it does not apply here.
- **Auth:** `getAuthorizationRequirement()` → `RequiresStakeholderPermission(Project.class,
  "Delete")`. Seed `Project[Delete]` in `StakeholderPermissionsInitializer`.
- **Optimistic lock + audit + SSE** as the other commands: honor the project `@Version`; the delete
  flows through `AuthorizingCommandHandler` (audit row for the acting user) and fires the `Project:0`
  sidebar broadcast so open clients drop the project.

## Files to add / change

Add:
- `project-domain/.../command/DeleteProjectCommand.java` — `extends EditCommand`, `setProject(Project)`.
- `project-jpa/.../impl/command/DeleteProjectCommandImpl.java` — the orchestration above.
- `service-api/.../dto/DeleteProjectInput.java` — `record DeleteProjectInput(@NotBlank String
  projectName, Integer version)` (mirror `DeleteGoalInput`).

Change:
- `project-domain/.../command/ProjectCommandFactory.java` — add `newDeleteProjectCommand()`.
- `project-jpa/.../impl/command/ProjectCommandFactoryImpl.java` — implement it.
- `service-impl/.../command/ProjectCommandRegistrar.java` — register `DeleteProject` (input→command).
- `project-jpa/.../impl/repository/init/StakeholderPermissionsInitializer.java` — seed
  `Project[Delete]`.

## Open decisions (need a call before/at coding)

1. **Backfill `Project[Delete]` to existing project owners?** New creators and `SystemAdmin` are
   covered automatically; stakeholders on projects created *before* this change won't hold the new
   permission. Options: (a) one-time grant in an initializer/Flyway migration to every
   `UserStakeholder` that already holds `Project[Edit]`; (b) defer — admin-bypass covers the E2E
   cleanup, owners get it on new projects. **Recommend (a)** so existing owners can delete their own
   projects; it's a few lines and keeps behavior intuitive.
2. **Input key — `projectName` vs `projectId`?** `DeleteGoalInput` carries `projectName` + `goalId`.
   A project has only itself, and names are unique, so `projectName` + `version` is enough and
   consistent. **Recommend `projectName` + `version`.** (Flag: the UI deletes by the project the
   user is on, so name is natural; MCP #242 can pass either.)

## Test plan (gate: `mvn clean verify`)

- **Command IT (H2)** — seed a project with ≥1 of every child (goal + relation, story, actor,
  use-case, scenario + step, stakeholder [user & non-user], glossary term referenced by a goal,
  report generator, annotations on several entities, tag assignments). Run `DeleteProject`; assert:
  project gone from `listProjects`/repo; every child table empty for that project id; annotation
  join tables and tag assignments cleared; **no** `User` rows deleted.
- **Auth IT** (extend `AuthorizationIT`) — a stakeholder without `Project[Delete]` is rejected and
  nothing is deleted; a holder (and `SystemAdmin`) succeeds.
- **Optimistic lock** — stale `version` rejected with the standard lock error; nothing deleted.
- **Not-found** — unknown/already-deleted project → clean not-found error, not 500.
- **Audit** — a command-audit row attributed to the acting user is written for the delete.
- **Permission seed** — `Project[Delete]` present in `findAvailableStakeholderPermissions()`; a
  newly-created project's creator holds it. (If open decision 1 = backfill, assert an existing
  owner gains it after init/migration.)

## Out of scope

- UI delete + export-first prompt (#241); gateway/CLI exposure (#242).
- Soft-delete / trash-restore (XML export is the backup path).
- Bulk / multi-project delete, and any delete-performance fast-path (note as a follow-up if the
  359-project cleanup proves slow via per-entity orchestration).

## Risks

- **Performance** on large projects / bulk cleanup: per-entity sub-commands fire annotation removal,
  analysis dispatch and SSE each. Acceptable for correctness-first v1; a native fast-path can be a
  follow-up if needed.
- **Ordering / cross-references** (goal↔container, scenario↔use-case): mitigated by container-before-goal
  ordering and the existing detach sub-commands; the command IT with a fully-populated project is the
  guard.
- **Analysis/SSE on a vanishing project**: ensure per-child events don't error once the project is
  going away; the `Project:0` broadcast (not `Project:<id>`) is what clients listen to.

## AC mapping (from `doc/delete-project-epic.md`, Child 1)

- Deletes project + all children in one tx, no orphans → command IT.
- Rejects without `Project[Delete]` → auth IT.
- Stale version rejected → optimistic-lock test.
- Command-audit row for acting user → audit assertion.
- Non-existent id → clean not-found → not-found test.
- IT covers cascade, auth denial, stale-version → test plan above.

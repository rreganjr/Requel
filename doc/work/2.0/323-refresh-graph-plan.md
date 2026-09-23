# 323 — Stop refresh from loading every user's active projects

Implementation plan for https://github.com/rreganjr/Requel/issues/323.
Base: `release/2.0` @ `594de9bb`. Branch: `323-refresh-graph`.

## Summary

`em.refresh(entity)` on a story, actor or goal re-reads the entity along with its eager to-one
graph. That graph reaches users several ways (the entity's `createdBy`, its project's
`createdBy`, and so on), and each user drags in `userRoles` (EAGER) →
`ProjectUserRole.activeProjects` (EAGER). Hibernate left-joins all of those into one select, so
the chains multiply: about N^6 rows for a user on N projects. A single-admin install, where the
admin's `activeProjects` gains every project it creates, stops being able to add a story to a
container after a handful of projects.

The fix goes at the source rather than at each command:

1. `ProjectUserRole.activeProjects` becomes `FetchType.LAZY`. The one reader that needs the whole
   set, `GET /api/projects`, fetches it explicitly through a repository query.
2. `CascadeType.REFRESH` comes off every `@ManyToOne` that points up at `UserImpl`. The
   CLAUDE.md guardrail already forbids them, and #247 missed these.

Every refresh site benefits without being edited. The tests pin the behaviour for each one.

Backend only. No Angular changes, so there is no JS unit, typecheck or e2e gate.

## Locked decisions

1. **LAZY plus explicit fetch, not `@Fetch(SELECT)`.** Decided in review. With `@Fetch(SELECT)`,
   every user load would still pay for the whole project set, just in extra selects instead of
   joins. With LAZY, a user stops carrying projects around, and the readers that need them say
   so.
2. **The explicit fetch is a query, not an entity graph.** `ProjectRepository.findActiveProjects(User)`
   runs `select p from ProjectUserRole r join r.activeProjects p where r.user = :user`. It returns
   projects without initializing the collection, and it doesn't depend on open-in-view. An
   entity graph would have to be threaded through `UserRepository.findUserByUsername`, which every
   authenticated request goes through, and that would put the cost back on everyone.
3. **Scope is every refresh site, not five.** The review counted five in the issue comment. The
   tree has seven in `project-jpa`: `AddStoryToStoryContainer:111`, `AddActorToActorContainer:111`,
   `AddGoalToGoalContainer:117`, `RemoveStoryFromStoryContainer:107`,
   `RemoveActorFromActorContainer:107`, `RemoveGoalFromGoalContainer:110`, `DeleteStory:153`. There
   are also three in `annotation-jpa` that refresh an annotation, whose own `createdBy` reaches a
   user: `DeleteAnnotationGroup:98`, `RemoveAllAnnotationsFromAnnotatable:105`,
   `RemoveAnnotationFromAnnotatable:117`. None of the ten call sites changes, because the fix is
   in the mappings. Tests cover all seven project sites and one annotation site.
4. **Strip REFRESH from all eleven upward user edges, not only `AbstractProjectOrDomainEntity.createdBy`.**
   *(Widened from the review answer. Needs a thumbs-up.)* The same one-token violation appears on:

   | Mapping | Line |
   |---|---|
   | `AbstractProjectOrDomainEntity.getCreatedBy` | 164 |
   | `AbstractProjectOrDomain` (creator, already LAZY) | 165 |
   | `AbstractStakeholder` (user) | 94 |
   | `GoalRelationImpl` (createdBy) | 214 |
   | `ProjectUserRole.getUser` (role → owning user) | 93 |
   | `AbstractAnnotation.createdBy` | 252 |
   | `IssueImpl` (user) | 179 |
   | `PositionImpl` (createdBy) | 199 |
   | `ArgumentImpl` (createdBy) | 147 |
   | `TagImpl` (createdBy) | 158 |
   | `TagCategoryImpl` (createdBy) | 149 |

   Removing REFRESH changes nothing a caller can observe. Nothing refreshes an entity in order to
   pick up a change to its creator, and PERSIST and MERGE stay where they are.
5. **Mutators work on a managed user.** `EditProjectCommandImpl:259` adds to
   `role.getActiveProjects()` on `editedBy`. `CurrentUserCommandHandler` resolves that user
   *outside* `DefaultCommandHandler`'s `@Transactional`, and tests set it from their own load. With
   open-in-view (on by default, never set in this repo) an HTTP request shares one EntityManager,
   so today it works. An in-process caller hands over a detached user, and touching a LAZY
   collection on it throws `LazyInitializationException`. The command re-reads the user
   (`getUserRepository().get(user)`) before touching the role. `EditUserStakeholderCommandImpl:194`
   gets the same treatment. `UserStakeholderImpl.ensureProjectMembership` and
   `removeFromProject` go through `getUser()` on a managed stakeholder, so they are already safe.
6. **No schema change.** A fetch type and a cascade are mapping-only. `user_roles_active_projects`
   stays as it is, and there's no Flyway migration.
7. **No behaviour change to any of the ten commands.** This is the issue's AC 4. The same rows are
   written and the same collections are visible after `execute()`.

## What the tree actually looks like

Verified at `594de9bb`:

- `ProjectUserRole.getActiveProjects()`: `@ManyToMany(targetEntity = ProjectImpl.class,
  cascade = PERSIST, fetch = EAGER)` with `@SortNatural`, backed by a `TreeSet`, join table
  `user_roles_active_projects(project_user_role_id, active_projects_id)`.
- `UserImpl.getUserRoles()`: `@OneToMany(..., fetch = EAGER)`. It stays EAGER. There are few
  roles, and `UserDtoMapper` / `AuthorizingCommandHandler` read them on every request.
- Readers of `activeProjects` outside the domain: only `ProjectQueryController.listProjects:136`
  (plus its mock in `ProjectQueryControllerTest:146`). Neither `UserDtoMapper` nor
  `AuthorizingCommandHandler` touches it.
- Mutators: `EditProjectCommandImpl:259`, `EditUserStakeholderCommandImpl:194`,
  `UserStakeholderImpl:218,230`.
- XML: `projectUserRole` marshals only `userPermissions` (`doc/samples/Requel.xml:1117`), so
  export and import never touch `activeProjects`.
- Nothing in the test tree reads Hibernate `Statistics` or installs a `StatementInspector`.

## Contracts

| Before | After |
|---|---|
| `activeProjects` `fetch = EAGER` | `fetch = LAZY` |
| 11 `@ManyToOne(UserImpl)` with `CascadeType.REFRESH` | REFRESH removed; other cascades unchanged |
| `listProjects` reads `role.getActiveProjects()` | `projectRepository.findActiveProjects(user)` |
| — | `ProjectRepository.findActiveProjects(User): Set<Project>` (sorted, same order as `@SortNatural`) |
| `EditProject` / `EditUserStakeholder` touch `editedBy`'s role directly | re-read the managed user first |

## Step by step

**Commit 1 — REFRESH off the user edges.** The eleven mappings in locked decision 4. Put a
one-line `// #323` comment on each, matching the #247 comment style, pointing at the CLAUDE.md
guardrail.

**Commit 2 — LAZY `activeProjects`.** Change the fetch type and extend the existing #247 comment
on the getter to say why it is LAZY and to name `findActiveProjects` as the way to read it.

**Commit 3 — the explicit fetch.** Add `findActiveProjects(User)` to `ProjectRepository` and
`JpaProjectRepository`, returning a `TreeSet` so callers keep the natural order. Switch
`ProjectQueryController.listProjects` to it, and update `ProjectQueryControllerTest` to mock the
repository call instead of the role.

**Commit 4 — managed user in the mutators.** `EditProjectCommandImpl` and
`EditUserStakeholderCommandImpl` re-read the user through `getUserRepository().get(...)` before
`getRoleForType(ProjectUserRole.class)`. `EditProject` already merges when it grants the role,
so reuse that managed instance.

**Commit 5 — tests.** Below.

Keep `tmp/323-verify.sh` running `mvn clean verify`.

## Test plan

Gate: `mvn clean verify` green. Backend-only change, so no `requel-angular` suites.

**New `modules/requel-app/src/test/java/com/rreganjr/requel/project/RefreshGraphIT.java`.**

- *Instrumentation:* unwrap `SessionFactory` from the `EntityManagerFactory`, call
  `getStatistics().setStatisticsEnabled(true)` and `clear()` around the command under test, and
  turn statistics off again in `@AfterEach`. No property change, so other ITs are unaffected.
- *Fixture:* a fresh user with a `ProjectUserRole`, one real project holding a story, an actor, a
  goal and a use case. A "wide" variant then adds 20 more rows to `user_roles_active_projects`
  for that role using `JdbcTemplate`, pointing at 20 bare projects. That manufactures the large-N
  state without running 20 `EditProject`s (CLAUDE.md "manufacture outcomes with JdbcTemplate").
  Look up ids after insert; never assume them.
- *Assertions, per command:* run each of the seven project commands, plus
  `RemoveAnnotationFromAnnotatable`, as that user, and check:
  - the collection load count for `ProjectUserRole.activeProjects` is **0**, which proves the
    graph no longer reaches it (issue AC 1);
  - the `ProjectImpl` entity load count is at most 1, meaning only the entity's own project;
  - the prepared-statement count is **the same** for the narrow and wide fixtures (issue AC 2).
- *Behaviour:* after each Add or Remove, the entity's `getReferers()` / container collection shows
  the change, so the refresh still does its job (issue AC 4).

**Lazy-load regressions.**

- `ProjectQueryControllerTest`: listing still returns the user's active projects, sorted.
- MockMvc `GET /api/projects` as a non-admin user with two active projects returns both. Add it
  to whichever existing IT covers the projects endpoint.
- An in-process `EditProject` with an `editedBy` loaded in a separate transaction (detached)
  succeeds, and the project shows up in `findActiveProjects`. This is the regression locked
  decision 5 guards against.
- Login/JWT: `AuthorizationIT` and the auth ITs stay green unchanged. They read roles and
  permissions, never `activeProjects`.

**Unchanged, expected green:** `CommandGatewayIT` (including
`storyContainerResolvesUseCaseByExplicitType`), `ProjectXml{,Streaming}RoundTripIT`,
`DeleteProjectIT` / `DeleteProjectMySqlIT`, `DeleteCascadeIT`.

## Out of scope

- Replacing the native-insert-plus-refresh pattern with targeted collection reloads. Once the
  graph is fixed, the refresh is cheap, and the Hibernate 6.5 `@ManyToAny` workaround it serves
  is its own problem.
- Making `userRoles` LAZY.
- Other EAGER to-many chains that aren't implicated here. If one turns up during the work, file it.
- A Testcontainers MySQL variant of `RefreshGraphIT`. Row multiplication doesn't depend on the
  dialect, and nothing here involves locking or FK ordering.

## Risks

- **A LAZY reader nobody listed.** Anything that touches `activeProjects` on a detached user now
  throws where it used to work. The grep above is the whole list today, and the detached-user
  test covers the realistic path. Open-in-view masks the problem for HTTP, so an MCP or other
  non-servlet path is where it would show up first.
- **A `@SortNatural` LAZY set initializes on `add`.** Adding to it loads the whole set, as one
  separate select. That's bounded by N rather than N^6, and it happens only in the mutators.
- **Widening the REFRESH removal (decision 4).** If some code quietly relied on a refresh
  reloading a user, it now sees the user as already loaded in the session. No such code was found.
  This is why the change needs a thumbs-up.
- **Statistics are global to the SessionFactory.** Keep `RefreshGraphIT` in one class, reset the
  counters around each command, and never read absolute totals.

## AC mapping

| AC | Commit | Proven by |
|---|---|---|
| 1 — adding a story (and the other refresh sites) does not load users' active projects | 1, 2 | `RefreshGraphIT` collection-load count = 0 |
| 2 — row/statement counts do not scale with installation size | 1, 2 | `RefreshGraphIT` narrow vs wide statement counts equal |
| 3 — `CommandGatewayIT` passes with many more projects | 1, 2 | `CommandGatewayIT` green; `RefreshGraphIT` wide fixture |
| 4 — no behaviour change to the commands | — | `RefreshGraphIT` behaviour assertions; existing command ITs green |
| test plan — login/JWT and project list still work with LAZY | 3, 4 | `ProjectQueryControllerTest`, `GET /api/projects` IT, detached `EditProject` test, `AuthorizationIT` |

# #447 — Tag reads check project read access

https://github.com/rreganjr/Requel/issues/447

Branch `447-tag-query-project-access` from `release/2.1`, PR to `release/2.1`. Size 2.

## Summary

`TagQueryController` reads tags straight from the repositories. It never asks who is calling,
so a signed-in user who isn't on a project can read its tags, categories and tagged entities.
The tag commands already check project access (#343). This gives the five read routes the same
read check the annotation reads use (#296), through `ProjectReadAccess`.

## Locked decisions (2026-11-01)

1. **Tags in a named project.** `GET /api/tags`, `/api/tags/categories` and
   `/api/tags/categories/typed` with `projectName`:
   - a project that doesn't exist: 404;
   - a project the caller can't read: 403 (a system admin reads every project; anyone else reads
     the projects they are a user stakeholder on).

   With no `projectName`, they return global tags and categories only, open to any signed-in
   user, as today.
2. **`GET /api/tags/on-entity`**: 404 both when the entity doesn't exist and when it is in a
   project the caller can't read. The route takes no project name, so a 403 would confirm the
   entity exists. An unknown entity type stays 400.
3. **`GET /api/tags/{tagId}/entities`**:
   - a tag that doesn't exist, or a project tag in a project the caller can't read: 404;
   - a global tag lists only the entities in projects the caller can read;
   - an entity whose project can't be found is listed for a system admin only.
4. **Folded in:**
   - The release plan update (`doc/work/2.1/21-release-plan.md`): Phase 4b, ordered #447,
     #448, #446, and #459 added to Phase 6.
   - The refresh-token tests that failed during the fall-back hour (`PublicClientRefreshTokenIT`,
     "expiresAt must be after issuedAt"). Reproduce first (step 5).
5. **Plain wording.** Code, tests, commit and PR describe the change as access checks on tag
   reads, nothing more.

## Changes

service-impl
- `TagQueryController`:
  - Takes a `CurrentUserResolver`, as `AnnotationQueryController` does.
  - `resolveProjectId` becomes a lookup that returns 404 for a missing project
    (`NoSuchProjectException`) and 403 when `ProjectReadAccess.canRead` is false. The three
    project-scoped routes use it. A blank `projectName` keeps the global-only path.
  - `getTagsOnEntity`:
    - resolve the entity class through `TaggableTypeRegistry` (unknown type: 400);
    - load the entity with the `EntityManager`;
    - return 404 unless the entity is found and `canRead(projectOf(entity), caller)`.
  - `getEntitiesWithTag`:
    - a project tag (`getProjectId() != null`) whose project the caller can't read: 404;
    - otherwise each taggable is kept only if its project is readable (unknown project: admin
      only).
    - The caller's readable-project decision is cached per project id within the request, so a
      global tag on many entities costs one check per project.
- `ProjectReadAccess`: `projectOf` takes the tagged entity. Goals and stories are both
  `Annotatable`, so it gets a `Taggable` overload that delegates when the entity is `Annotatable`
  and returns null otherwise. A `canReadProjectId(Long, User)` helper backs the tag checks.

No Angular change: the UI asks for tags in the open project only, so a member never sees the new
responses. No gateway, MCP or CLI change: none of them reads tags.

## Steps

1. Branch.
2. `ProjectReadAccess` helpers.
3. `TagQueryController` checks.
4. Tests (below). `TagApiIT` calls the controller directly, so its reads run with an admin in the
   security context.
5. Refresh-token failure: run `PublicClientRefreshTokenIT` with the JVM in `America/New_York` and
   the clock set inside the 2026-11-01 01:00–02:00 hour (`libfaketime`, or a fixed `Clock` if the
   authorization server takes one).
   - If it reproduces, find where the time is computed or stored and fix the cause. If the cause
     is only the test database storing times without a zone, pin the test JVM to UTC
     (`-Duser.timezone=UTC` in the surefire and failsafe `argLine`).
   - If it doesn't reproduce, leave the code alone and note it in the PR.
6. Release plan update.

## Test plan

`tmp/447-verify.sh`: `mvn clean verify -pl modules/coverage-report -am` and the coverage check.

- `TagQueryAccessIT` (new; MockMvc against the running security chain). Fixture:
  - a project A with a goal and a story, each tagged with a project tag and a global tag;
  - a project B the caller isn't on, set up the same way;
  - three callers: a member of A, a non-member, and a system admin.
- For each route, the non-member is refused, as the decisions above say:
  - `/api/tags?projectName=B`, `/categories?projectName=B` and `/categories/typed?projectName=B`
    → 403;
  - `/on-entity` for B's goal → 404, the same as for an id that doesn't exist;
  - `/{B's project tag}/entities` → 404.
- The member of A reads A's tags, categories, `on-entity` and entities as before.
- The global tag's `/entities`: the member of A sees A's entities only; the admin sees both
  projects.
- No `projectName`: global tags and categories for every caller.
- A missing project → 404. An unknown entity type → 400.
- `TagApiIT` and the existing tag ITs pass unchanged apart from the security context.

## Out of scope

- The tag commands (#343, done).
- Login and password changes (#448, #446).
- A `projectName` parameter on `/on-entity` (decision 2 makes it unnecessary).

## Risks

- **A non-member's UI.** Nothing in Angular asks for another project's tags, so no screen
  changes. If one did, it would now get 403 or 404.
- **Cost of the global-tag filter.** Each tagged entity's project is loaded. The per-request cache
  keeps that to one check per project.

## AC mapping

| AC | Covered by |
|---|---|
| Every project-scoped tag and category read uses the shared project read check | Decision 1; `TagQueryAccessIT` |
| Entity tag lookups check the entity's project first | Decision 2; `TagQueryAccessIT` |
| Tag-to-entity lists leave out entities in projects the caller can't read, global tags included | Decision 3; `TagQueryAccessIT` |
| A non-member is refused on every route; member, admin and global reads still work | `TagQueryAccessIT` |
| Regression tests; the verification gates pass | `tmp/447-verify.sh` |

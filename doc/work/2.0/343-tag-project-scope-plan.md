# #343 — Tag commands act across projects

https://github.com/rreganjr/Requel/issues/343

Patch release: `v2.0.2`. Branch `343-tag-project-scope` from `origin/master`, PR to `master`.

## Summary

Three tag commands let a caller with `Annotation[Edit]` on project B write to something that
belongs to another project, or to a global tag or category that only a system administrator may
change. The permission check takes its project from the wrong place in all three:

| Command | Scope taken from | Hole |
|---|---|---|
| `AssignTag` | the tagged entity | attaches project A's tag (category, value, color) to a B entity, exposing A's tag text to B's readers |
| `EditTag` with `tagId` | the caller's `projectName` | `projectName=B` plus A's or a global `tagId` renames/recolors that tag |
| `EditTagCategory` with `categoryId` | the caller's `projectName` | `projectName=B` plus A's or a global `categoryId` rewrites its values, allowed types and exclusivity |

`DeleteTag` and `DeleteTagCategory` already take the scope from the tag/category and are correct.
The Angular UI never sends `tagId` or `categoryId` to an edit (it only creates), so the holes are
reachable over REST, MCP and the CLI only, and the fix changes nothing the UI does.

## Locked decisions

1. **One ticket.** The `EditTag`/`EditTagCategory` holes are the same defect in the same
   registrar, so they ride on #343 rather than a new ticket.
2. **A mismatched `projectName` on an edit-by-id is refused**, not ignored. Ignoring it would let a
   caller believe it moved a tag between projects when nothing moved.
3. **No data cleanup.** No migration removes cross-project links already in a database.
   `UnassignTag` stays unguarded so any such link can still be removed.
4. **Patch-safe.** No change to any input record's fields, `project.xsd`, configuration or
   Flyway. Only behavior on inputs that were wrong to accept, and the description text.

## Contracts

- **`AssignTag`:** a tag whose `projectId` is non-null must belong to the tagged entity's project
  (the project itself, or the project of a `ProjectOrDomainEntity`). Otherwise
  `EntityValidationException` on `tagId`: *"Tag <id> belongs to another project and cannot be
  assigned here."* A global tag (`projectId` null) is assignable anywhere, as today.
- **`EditTag` with `tagId`:**
  - Unknown `tagId` → `IllegalArgumentException("Tag not found: <id>")`. Today a miss falls
    through to create a new tag, which is the same unknown-id-creates defect #342 covers for
    annotations.
  - The project scope is the tag's own project (null for a global tag), resolved as `DeleteTag`
    does. Authorization therefore checks the tag's project, or requires system administrator for
    a global tag.
  - `projectName` may be null/blank (meaning "the tag's own scope") or must name the tag's
    project. Anything else → `IllegalArgumentException`: *"Tag <id> belongs to project '<A>'"* or
    *"Tag <id> is global"*, *"; omit projectName or pass the tag's own project."*
  - With `tagId` null (create), nothing changes: `projectName` null/blank still means global.
- **`EditTagCategory` with `categoryId`:** the same rule with the category's project. Unknown
  `categoryId` is already refused in `execute`; move that refusal into the binder so it happens
  before scope resolution.

Both refusal types map to `INVALID_INPUT` at the gateway (MCP `INVALID_PARAMS`); over REST,
`IllegalArgumentException` is 400 and `EntityValidationException` 422, as for every other command.

## Step by step

1. **`AssignTagCommandImpl.execute`** (`tagging-jpa`): before `applyCategoryRules`, compare
   `tagImpl.getProjectId()` with the managed taggable's project id and refuse a mismatch. Put the
   check in the command, not the binder, so any non-gateway caller is covered too.
2. **`TagCommandRegistrar` `EditTag` binder** (`service-impl`): when `tagId` is non-null, load the
   tag (refuse if missing), set the scope with `resolveProject(tag.getProjectId())`, and refuse a
   non-blank `projectName` that does not name that project. When `tagId` is null, keep
   `resolveProjectByName`.
3. **`TagCommandRegistrar` `EditTagCategory` binder:** the same for `categoryId`, using
   `findCategoryById`.
4. **Descriptions:** update the `@CommandDescription` text on `AssignTagInput` (a project tag only
   on its own project's entities), `EditTagInput` and `EditTagCategoryInput` (with an id, the scope
   is the existing tag's or category's; `projectName` is optional and must match). The
   `authorization` strings stay accurate as written.

## Test plan

`tmp/343-verify.sh` runs `mvn clean verify` (backend only; no `requel-angular/**` change).

- **`TagCommandIT`** (command level):
  - `AssignTag` of A's tag to a B goal → `EntityValidationException`, no link written.
  - `AssignTag` of a global tag to a B goal → succeeds.
  - `AssignTag` of A's tag to an A goal → succeeds.
  - `AssignTag` of A's tag to project B itself → refused.
- **`TagApiIT`** (through `CommandController`, exercising the binders):
  - `EditTag` with A's `tagId` and `projectName=B` → 4xx, tag unchanged.
  - `EditTag` with a global `tagId` and `projectName=B` → 4xx, tag unchanged.
  - `EditTag` with A's `tagId` and `projectName` null → succeeds, edits A's tag in A's scope.
  - `EditTag` with an unknown `tagId` → 4xx, no tag created.
  - The same four cases for `EditTagCategory` with `categoryId`.
- **`AuthorizationIT`:** the editor stakeholder of the fixture project, with `projectName` set to
  its own project and a tag/category id from a second project → refused, nothing changed. This is
  the case the issue is about: a real non-admin user, through the security chain.
- `McpToolCatalogLockstepIT` and `typedWriteToolDescriptionsAreWellFormed` still pass after the
  description edits.

## Out of scope

- Removing cross-project tag links already in a database (decision 3).
- Whether annotation inputs' `projectName` should scope their lookups: #342.
- Error-kind changes (`CONFLICT` and the like): #345, v2.1.
- The 2.0.2 version bump. It rides on whichever 2.0.2 PR merges last
  (`doc/guides/RELEASE_PROCESS.md`, "Hotfixes", step 3).

## Risks

- **A script that edits by id while passing a different `projectName`.** It worked before and is
  refused now. That combination never did what such a caller intended (the tag stayed in its
  own project), so the refusal surfaces an existing mistake.
- **`ProjectOrDomain` that is a domain, not a project.** A project-scoped tag on a domain entity
  is refused; a global tag still works. Domain entities are not taggable from the UI today.

## AC mapping

| Issue AC | Covered by |
|---|---|
| Assigning A's tag to a B entity is refused over REST, MCP and CLI | `TagCommandIT`, `TagApiIT`, `AuthorizationIT`; MCP and CLI share the gateway path |
| A global tag, or A's tag on an A entity, still works | `TagCommandIT` |
| `UnassignTag` guard checked | decision 3: deliberately left unguarded |
| `findCategory` still resolves as intended | unchanged; it keys on the tag's own project |
| (widened) `EditTag`/`EditTagCategory` by id cannot cross projects or reach global ones | `TagApiIT`, `AuthorizationIT` |

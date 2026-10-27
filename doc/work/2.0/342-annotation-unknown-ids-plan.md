# #342 — Annotation edits create on an unknown id; deletes fail with NPE; note reuse uses LIKE

https://github.com/rreganjr/Requel/issues/342

Patch release: `v2.0.2`. Branch `342-annotation-unknown-ids` from `origin/master`, PR to
`master`. This is the last 2.0.2 fix, so the PR also carries the 2.0.2 version bump and the
`vite` security bump.

## Summary

The annotation binders in `AnnotationCommandRegistrar` look child ids up with
`entityManager.find` and pass the result on without checking it:

| Command | Unknown id today |
|---|---|
| `EditNote` `noteId`, `EditIssue` `issueId`, `EditPosition` `positionId`, `EditArgument` `argumentId` | null, so the command creates a new annotation and reports success |
| `EditPosition` `issueId` | null issue; the create path persists a position attached to no issue |
| `EditArgument` `positionId` | null position |
| `DeleteNote`, `DeleteIssue`, `DeletePosition`, `DeleteArgument` | null, then `NullPointerException` (`EXECUTION_ERROR`) |

`JpaAnnotationRepository.findNote` (the `EditNote` reuse lookup) is `text like :message` with
`getSingleResult()`: `%` and `_` are wildcards, and two notes with the same text make the
command fail.

Found in review: the edit commands take their permission scope from the target entity
(`EditNote`, `EditIssue`) or the parent (`EditPosition` from the issue, `EditArgument` from the
position), never from the child named by id. A stakeholder with `Annotation[Edit]` on project B
can pass project A's `noteId` or `issueId` (rewriting A's text and attaching it to a B entity),
A's `positionId` under a B issue, or A's `argumentId` under a B position. This is the #343
defect in the annotation binders. The deletes take their scope from the deleted child and are
correct.

`ResolveIssue` already refuses unknown ids in its builder; the fix follows that pattern.

## Locked decisions

1. **Unknown ids are refused in the binder** with `IllegalArgumentException("<Type> not
   found: <id>")`, the `ResolveIssue` pattern: REST 400, gateway `INVALID_INPUT` (MCP
   `INVALID_PARAMS`).
2. **Same-project check folded in.** An edit-by-id is refused unless the child's project is the
   target's or parent's project; a child that reaches no project (an orphan position, a
   domain-scoped annotation) is refused too. Same exception type, message *"<Type> <id> belongs
   to another project."* `projectName` is still neither read nor removed; that decision stays in
   v2.1.
3. **Duplicate notes resolve to the lowest id**, without merging. `findNote` matches with `=`,
   ordered by id. Notes have no children to reparent, so nothing is lost by leaving the
   duplicates.
4. **Patch-safe.** No change to any input record's fields, `project.xsd`, configuration or
   Flyway. Only behavior on inputs that were wrong to accept, and description text.
5. **Two non-code changes ride along** (`doc/guides/RELEASE_PROCESS.md`, "Hotfixes", step 3):
   - `requel-angular`: `@angular/build` 21.2.27 → 21.2.28, which pins `vite` 7.3.7 and closes
     Dependabot alerts #191–#195 (dev server and test runner only; nothing in the jar or image).
   - The 2.0.2 version bump: `./scripts/set-version.sh 2.0.2` plus the user-facing docs.

## Contracts

| Command | Lookup | Same-project rule |
|---|---|---|
| `EditNote` with `noteId` | `NoteImpl` | note's project = the annotatable's project |
| `EditIssue` with `issueId` | `IssueImpl` | issue's project = the annotatable's project |
| `EditPosition` | `issueId` → `IssueImpl` (always); `positionId` → `PositionImpl` | position's project (through its issues) = the issue's project |
| `EditArgument` | `positionId` → `PositionImpl` (always); `argumentId` → `ArgumentImpl` | argument's project (through its position) = the position's project |
| `DeleteNote` / `DeleteIssue` / `DeletePosition` / `DeleteArgument` | the id | none: the scope already comes from the deleted child |

A project is resolved with `AnnotationCommandProjectResolver`, made reachable from
`service-impl` (it is package-private in `annotation-jpa` today), or with an equivalent helper in
the registrar. With a null id on an edit, nothing changes: the command creates, and `EditNote`/
`EditPosition` still reuse an annotation with the same text.

`findNote(groupingObject, annotatable, message)`: `note.text = :message and note.groupingObject =
:groupingObject order by note.id`, first row; empty → `NoSuchAnnotationException` as today.

## Step by step

1. **`AnnotationCommandRegistrar`** (`service-impl`): a `require(Class, id, label)` helper that
   does the `find` and throws on null, and a `requireSameProject(child, expected, label, id)`
   helper. Use them in the four edit binders and the four delete binders.
2. **`JpaAnnotationRepository.findNote`** (`annotation-jpa`): `=` and lowest id, javadoc citing
   #342 the way `findPosition` cites #284.
3. **Descriptions** (`service-api`): drop "an unknown noteId/issueId/positionId/argumentId creates
   a new ... rather than failing" from `EditNoteInput`, `EditIssueInput`, `EditPositionInput` and
   `EditArgumentInput`; say an unknown or another project's id is refused. The delete
   descriptions gain "an unknown id is refused".
4. **`requel-angular/package.json` and lockfile:** already done on the branch (npm 11 was needed
   for the install; npm 10's peer resolver crashes on `@angular/build`'s optional peers).
5. **Version 2.0.2:** the developer runs `./scripts/set-version.sh 2.0.2` (it deletes `.bak`
   files, which Claude cannot do over the bridge); Claude edits the user-facing docs listed in
   `RELEASE_PROCESS.md` "User-facing versions" (`README.md`, `doc/guides/INSTALL.md`,
   `doc/guides/DOCKER_HUB.md` keeping a `2.0.1` row in the tag table, `website/index.html`,
   `website/guide/integrations.html`), then the grep in that section shows only the tag-table row.

## Test plan

`tmp/342-verify.sh` runs `mvn clean verify` and the Angular unit suite (`requel-angular/**`
changed: the build tool bump), plus a dev build.

- **`AnnotationUnknownIdIT`** (new, through the in-process gateway like `EditPositionReuseIT`):
  - Each of `EditNote`, `EditIssue`, `EditPosition`, `EditArgument` with an unknown child id →
    `INVALID_INPUT`, and no annotation created (row counts unchanged).
  - `EditPosition` with an unknown `issueId`, `EditArgument` with an unknown `positionId` →
    `INVALID_INPUT`, nothing created.
  - Each of the four deletes with an unknown id → `INVALID_INPUT` (was an NPE).
  - `EditNote` reuse: a note with `_` in its text does not match another note (`a_b` vs `axb`);
    with two notes of the same text (inserted with `JdbcTemplate`), `EditNote` without `noteId`
    attaches the lower id and succeeds.
- **`AuthorizationIT`** (next to the #343 cases; the editor holds `Annotation[Edit]` on the
  fixture project and none on a second project):
  - `EditNote` on a fixture entity with the second project's `noteId` → 400, the note unchanged.
  - The same for `EditIssue` with `issueId`, `EditPosition` with `positionId` under a fixture
    issue, and `EditArgument` with `argumentId` under a fixture position.
  - Control: the same edits with the fixture project's own ids succeed.
- `McpToolCatalogLockstepIT` still passes after the description edits.

## Out of scope

- Whether `projectName` on annotation inputs should scope the lookup or be removed: v2.1.
- Error kinds such as NOT_FOUND or CONFLICT: #345, v2.1.
- Merging duplicate notes (decision 3).
- Existing orphan positions created by the old `EditPosition` path: no cleanup migration.

## Risks

- **A script that edited by id across projects, or relied on create-on-unknown-id.** Both are
  refused now. Neither did what such a caller meant: the first rewrote another project's
  annotation, the second created a duplicate.
- **An orphan position can no longer be edited by id.** It answers no issue, so it reaches no
  project and the same-project check refuses it. It was never reachable from the UI.

## AC mapping

| Issue AC / work item | Covered by |
|---|---|
| Unknown child id on edit and delete is refused | `AnnotationUnknownIdIT` |
| `EditPosition` / `EditArgument` refuse an unknown parent id | `AnnotationUnknownIdIT` |
| `findNote` uses `=` and picks a deterministic row | `AnnotationUnknownIdIT` (wildcard, duplicates) |
| Decide on `projectName` | deferred to v2.1 (decision 2) |
| Descriptions updated | step 3; `McpToolCatalogLockstepIT` |
| (added) An id from another project is refused | `AuthorizationIT` |

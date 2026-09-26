Found while writing the annotation command descriptions for #296.

### What happens

- **An unknown child id creates instead of failing.** `EditNote`, `EditIssue`, `EditPosition` and `EditArgument` look the id up with `entityManager.find`; a miss returns null, and the command creates a new annotation (`AnnotationCommandRegistrar`). A caller editing note 999 that does not exist gets a new note and success.
- **`EditPosition` with an unknown `issueId`** leaves the issue null, and the create path persists a position attached to no issue.
- **The annotation deletes fail badly on an unknown id**: the command gets null and fails with a `NullPointerException`, reported as `EXECUTION_ERROR` rather than not found.
- **`EditNote`'s reuse lookup** (`JpaAnnotationRepository.findNote`) is `text like :message` with `getSingleResult`: `%` and `_` in the text are wildcards, and two matching notes make the command fail.
- **`projectName` is carried and ignored** by every annotation input; the target comes only from the ids.

### Work

- An unknown `noteId`, `issueId`, `positionId` or `argumentId` on an edit is a not-found error (`IllegalArgumentException` in the binder, so `INVALID_INPUT`), and on a delete the same.
- `EditPosition` and `EditArgument` refuse an unknown parent id.
- `findNote` matches with `=` (or escapes the pattern) and picks a deterministic row when several match (lowest id, as #284 did for positions).
- Decide whether `projectName` should scope the lookup (refuse an id from another project) or be removed from these inputs.
- Update the affected `@CommandDescription`s, which currently warn that an unknown id creates.

### AC

- Tests for each unknown-id case on edit and delete, and for the note reuse with `%`/`_` and with duplicates.

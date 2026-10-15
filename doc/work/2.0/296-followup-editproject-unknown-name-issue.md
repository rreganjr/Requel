Found while writing the `EditProject` description for #296.

### What happens

`EditProjectInput.projectName` selects the project to edit, but the registrar treats a name that matches no project the same as a null one: it creates a project (`ProjectCommandRegistrar` `EditProject` binder, `findProjectByName` miss → create). A caller who misspells the project they meant to edit gets a second project with the new `name`, and success.

The DTO's `id` field is never read, although its javadoc said it selected the project until #296 corrected the comment.

### Work

- An `EditProject` whose `projectName` is non-null and matches no project is refused as not found, the way every other project-scoped command treats an unknown `projectName`. Creating stays `projectName` null.
- Decide on `id`: remove it from `EditProjectInput` (and the Angular callers, if any send it), or make it select the project. Removing is the smaller change.
- Update the `EditProject` `@CommandDescription`, which currently warns about this.

### AC

- `EditProject` with an unknown `projectName` returns a not-found error and creates nothing, over REST, MCP and CLI.
- `EditProject` with `projectName` null still creates a project.
- `id` is either gone or selects the project, with a test either way.

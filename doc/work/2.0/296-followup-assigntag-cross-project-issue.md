Found while writing the tagging descriptions for #296.

### What happens

`AssignTag` takes its project scope from the tagged entity (`TagCommandRegistrar`, `projectScopeOf(taggable)`), so the authorization check is `Annotation[Edit]` on the entity's project. Nothing checks that a project-scoped tag belongs to that project. A stakeholder of project B who knows a tag id from project A can attach A's tag, with its category and value, to an entity in B, which also exposes the tag's text to B's readers.

Global tags (no project) are meant to be usable everywhere and are not affected.

### Work

- `AssignTag` refuses a project-scoped tag whose project is not the entity's project, with a validation error.
- Check whether `UnassignTag` needs the same guard (it can only remove a link that exists, so probably not).
- Check the category lookup (`findCategory`, project first then global) still resolves as intended after the change.

### AC

- Assigning project A's tag to a project B entity is refused over REST, MCP and CLI; assigning a global tag, or A's tag to an A entity, still works.

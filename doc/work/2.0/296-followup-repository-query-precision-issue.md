Found while researching the command descriptions for #296.

### What happens

1. **Name lookups treat `%` and `_` as wildcards.** `JpaProjectRepository`'s name finders for projects, goals, stories, actors, use cases, scenarios and non-user stakeholders use `name like :name` on the trimmed name without escaping. A name containing `_` or `%` can match a different entity, both when resolving a name and in the uniqueness checks. #254 fixed this for steps (`=`).
2. **The `Remove*FromContainer` deletes ignore the container type.** The native deletes behind `RemoveGoalFromGoalContainer`, `RemoveStoryFromStoryContainer` and `RemoveActorFromActorContainer` match on the member id and the container id only (`JpaProjectRepository` `remove*ContainerFrom*JoinTable`). Container ids are per-table auto-increment, so a use case and a story (or the project) with the same numeric id both lose their link. #189 fixed the resolution side of this; the delete side was missed.

### Work

- Switch the name finders to `=` (the collation already makes it case- and accent-insensitive on MySQL), or escape the pattern, and add a test with `_` in a name that must not match another.
- Add the container type to each native delete's `where`, and a test with two containers of different types sharing an id (`CommandGatewayIT` already builds colliding ids for #189).

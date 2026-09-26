Found while researching the command descriptions for #296.

### What happens

Several caller mistakes reach a gateway caller as `EXECUTION_ERROR` (MCP `INTERNAL_ERROR`, HTTP 5xx-ish) instead of an input or conflict error, so an MCP or CLI caller cannot tell "fix your arguments" from "the server broke":

- **Uniqueness conflicts.** `EntityException.uniquenessConflict` returns a plain `EntityException`, which `InProcessCommandGateway` maps to `EXECUTION_ERROR`. Every "name already taken" refusal (goals, stories, actors, use cases, glossary terms, positions renamed onto existing text, …) lands there.
- **Stale versions.** `EntityLockException` is retried by `RetryOnLockFailuresCommandHandler`, fails the same way each time, then maps to `EXECUTION_ERROR`. #296 wired `version` into seven deletes, so this now covers deletes too.
- **`EditNonUserStakeholder` with a user stakeholder's id** hard-casts in the binder and throws `ClassCastException`.

### Work

- Map uniqueness conflicts to `INVALID_INPUT` (or a new `CONFLICT` kind, if MCP/HTTP should say 409), in the gateway and in `CommandController`.
- Don't retry a stale-version `EntityLockException` (it cannot succeed), and map it to the same conflict kind.
- Refuse a user stakeholder id in the `EditNonUserStakeholder` binder with a validation error.

### AC

- Gateway tests for each case asserting the kind, and the MCP error code it maps to.

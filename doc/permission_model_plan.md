# Stakeholder Permission Coherence — Plan

> Planning doc for a future issue, surfaced during issue #69 Slice 2 (command authorization
> hardening). Captures a problem with how stakeholder permissions are modeled and granted, and
> proposes a path. Tracked as https://github.com/rreganjr/Requel/issues/75.

## The current model (confirmed)

- `StakeholderPermissionType` has three values: `Edit` ("create and edit a project entity"),
  `Delete` ("delete a project entity"), `Grant` ("grant permissions to others"). There is **no
  `Read`/`View`** type — this matches the original thesis design.
- Read access is **project membership**: query controllers gate reads on
  `findUserStakeholder(project, user)`, not on a per-entity permission.
- Permissions are **per entity type × verb**, granted independently to a stakeholder
  (`StakeholderPermissionsInitializer` seeds the catalog: Edit/Grant/Delete for Project (Edit/Grant
  only), Annotation, Goal, Actor, Story, UseCase, Scenario, GlossaryTerm, Stakeholder,
  ReportGenerator).
- `Edit` and `Delete` are **independent** — `Edit` does not imply `Delete`, and neither implies the
  other. `AuthorizationIT` encodes this (separate "editor" and "deleter" users).
- A project's **creator is granted every available permission** on creation
  (`EditProjectCommandImpl.createProject` → `findAvailableStakeholderPermissions()` →
  `grantStakeholderPermission`), so owners never hit the problem below; only partially-permissioned
  stakeholders do.

## The problem

Many operations are not single-entity actions — they cascade across related entities, and each
cascade step is its own command with its own permission. Granting the verb permission for the
"headline" entity is not enough to actually perform the operation.

Concrete case (the Slice 2 trigger): deleting a goal. `DeleteGoalCommandImpl.execute()` cascades
by invoking, through the command handler, as the same user:

- `RemoveGoalFromGoalContainerCommand` for every container the goal belongs to,
- `DeleteGoalRelationCommand` for every relation,
- `RemoveAnnotationFromAnnotatableCommand` for its annotations.

A goal can live in **five** container types (everything that implements `GoalContainer`):
`ProjectOrDomain`, `Story`, `UseCase`, `Actor`, `Stakeholder`. And because the API always adds a
new goal to the project container, **every** goal has at least one container. So once the structural
removal commands require `Goal[Edit]` (Slice 2), a `Goal[Delete]`-only stakeholder can never delete
a goal — `Delete` becomes meaningless without also granting `Edit`.

The general shape: to grant someone a coherent capability ("can delete goals"), you must hand-grant
a bundle (`Goal[Delete]` + `Goal[Edit]` on every container interaction + `Annotation[Delete]` + …).
Doing that one permission at a time is unrealistic, easy to get wrong, and produces stakeholders who
look authorized but are silently blocked mid-operation.

### Container / cascade graph (for reference)

- `GoalContainer`: `ProjectOrDomain`, `Story`, `UseCase`, `Actor`, `Stakeholder`
- `ActorContainer`: `ProjectOrDomain`, `Story`, `UseCase`
- `StoryContainer`: `ProjectOrDomain`, `UseCase`
- `Scenario` ∈ `UseCase`
- Deletes also cascade into annotations (`Annotation[Delete]`) and relations.

## Principles for a fix

- A granted capability should be **coherent**: if a stakeholder is authorized for an operation,
  every intrinsic sub-step of that operation should also be authorized, without separately
  hand-granting each.
- **Detaching is part of deleting.** Removing an entity from its containers / relations /
  annotations as part of deleting it should be authorized by that entity's `Delete` — not by
  `Edit` on each container type.
- Keep the **model auditable and least-privilege**: avoid collapsing everything into one
  super-permission; preserve the Edit / Delete / Grant distinction.
- Don't regress the existing `AuthorizationIT` intent (editor ≠ deleter ≠ granter), except where it
  encoded the now-known gap.

## Options

### A. Internal cascade exemption (tactical; recommended now, in #69)
Sub-commands invoked **within an already-authorized parent command** are not independently
re-authorized. The parent (`DeleteGoal`, authorized for `Goal[Delete]`) is responsible; its
structural removals run exempt. Direct API/gateway calls to those same commands remain fully
authorized (e.g. `RemoveGoalFromGoalContainer` standalone still needs `Goal[Edit]`).

Mechanism: an explicit `AuthorizationExemptable` flag the parent sets on each cascade sub-command,
honored by `AuthorizingCommandHandler`. Preferred over reusing the existing `editedBy == null`
bypass, because the sub-commands use `editedBy` during `execute()` (audit + entity resolution).

- Pros: small, correct semantics ("delete includes detach"), unblocks Slice 2, fixes the latent
  `DeleteStory`/`DeleteActor` versions of the same bug.
- Cons: doesn't address the broader "realistic granting" concern on its own.

### B. Permission implication graph (check-time expansion)
Declare that holding a permission implies others (e.g. `Goal[Delete]` implies the structural
container-detach rights needed to delete a goal). At check time, expand the stakeholder's granted
set with implied permissions before evaluating.

- Pros: coherent capabilities without changing what's stored; flexible.
- Cons: indirection — effective permissions differ from stored ones; the implication map must be
  maintained alongside the cascade graph.

### C. Grant-time closure (auto-grant prerequisites)
When granting a permission, automatically add its prerequisites (granting `Goal[Delete]` also grants
the container `Edit` rights it requires). `EditUserStakeholder` enforces the closure.

- Pros: stored permissions are self-consistent; what you see is what you have.
- Cons: grants balloon; revocation is awkward (which implied grants to remove?).

### D. Permission bundles / presets (roles within a project)
Offer coherent presets ("Contributor", "Editor", "Reviewer", "Owner") that map to vetted permission
sets, with per-permission editing as an advanced option. The default `ProjectUserRole` seeds a
coherent bundle.

- Pros: matches how people actually assign access; reduces one-off incoherence; good UX.
- Cons: doesn't by itself fix intrinsic cascades (still pair with A); needs UI work.

## Recommendation

Two-track:

1. **Now, inside #69:** implement Option A (cascade exemption). It is the semantically correct fix
   for the immediate regression, is small, and keeps `Goal[Delete]` meaningful. Slice 2 proceeds
   with the `deleterCanDeleteGoal` test still expecting 200.
2. **This ticket (#75):** adopt Option A as the structural baseline, then add coherence for
   *granting* — recommended **A + D** (bundles/presets as the primary grant UX) with the
   implication graph (B) only if needed for edge cases. Closure (C) is the least attractive due to
   revocation complexity. Decide during that ticket.

## Scope of the future ticket

- A declared map of operation → required permissions and intrinsic cascade steps (derived from the
  container/cascade graph above), as the single source of truth.
- Whichever coherence approach is chosen (bundles and/or implication), plus migration of the
  default `ProjectUserRole` grant and the project-creator grant onto it.
- Update `AuthorizationIT` to reflect coherent capabilities; add tests for cascade-via-Delete and
  for bundle grants.
- Angular stakeholder-permission editor updates if bundles are adopted.

## Open questions

- Bundles vs implication graph as the primary mechanism (or both)?
- Should `Edit` imply anything (e.g. the structural edits on that entity's own relationships), or
  stay strictly create+edit-of-the-entity?
- Is a `Read`/`View` permission ever wanted, or does membership-as-read remain the model? (The
  thesis never had Read; default is to keep membership-as-read.)
- Where does the operation→permissions map live — `project-domain`, or a new authorization module?

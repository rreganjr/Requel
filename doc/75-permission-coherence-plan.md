# Issue #75 — Stakeholder permission coherence: implementation plan

Implements the future-ticket scope from `doc/permission_model_plan.md` (the #75 issue body). Replaces
the temporary `AuthorizationExemptable` stop-gap from #69 with a principled model, and makes granting
coherent via bundles/presets.

## Decisions (locked)

- **Approach: A + D.** Formalize the cascade baseline (A) as the structural foundation, then add
  grant coherence via **bundles/presets** (D). Option C (grant-time closure) is rejected (revocation
  complexity); Option B (implication graph) is held in reserve for edge cases only.
- **`Edit` stays strict.** `Edit` authorizes create/edit of the entity itself. Detaching an entity
  from its containers / relations / annotations is authorized by that entity's **`Delete`**
  ("detaching is part of deleting"), not by `Edit` on each container type.
- **Membership-as-read retained.** No new `Read`/`View` verb; reads stay gated on project
  membership (being a stakeholder), as in the original thesis design.
- **Policy location: `project-domain`.** The operation→permissions/cascade map is project-specific
  and references project entity types, so it lives in `project-domain` (a new
  `com.rreganjr.requel.project.authorization` package), consistent with `ProjectScopedCommand`.

## Current state (confirmed in-tree)

- `StakeholderPermissionType`: `Edit`, `Delete`, `Grant` (no `Read`). Catalog seeded by
  `StakeholderPermissionsInitializer`: `Project` (Edit, Grant — **no Delete**); `Annotation`, `Goal`,
  `Actor`, `Stakeholder`, `GlossaryTerm`, `Story`, `UseCase`, `Scenario`, `ReportGenerator` (Edit,
  Grant, Delete each).
- `AuthorizingCommandHandler` (requel-app) checks, in order: an `AuthorizationExemptable` bypass flag,
  then the command's `AuthorizationRequirement` (`RequiresSystemRole` / `RequiresRolePermission` /
  `RequiresStakeholderPermission`). Stakeholder check builds the key `entityType.getName() + "[" +
  type + "]"` and looks it up on the user's `UserStakeholder`.
- The **stop-gap**: `AuthorizationExemptable` (platform-identity) + `TODO(#75)` in
  `AuthorizingCommandHandler`, and `setAuthorizationExempt(true)` calls in `DeleteGoalCommandImpl` and
  the `Delete{Story,Actor,UseCase,GlossaryTerm}` + annotation delete commands.
- **Known gap:** in `DeleteGoalCommandImpl`, the `RemoveGoalFromGoalContainer` and `DeleteGoalRelation`
  sub-commands are marked exempt, but the `RemoveAnnotationFromAnnotatable` sub-command is **not** — so
  a `Goal[Delete]`-only stakeholder deleting a goal that has annotations can still be blocked. The
  formalized policy must cover annotation detach as an intrinsic delete step.
- Project creator gets every stakeholder permission via `EditProjectCommandImpl.createProject →
  findAvailableStakeholderPermissions() → grantStakeholderPermission`.

### Container / cascade graph (from the issue)

- `GoalContainer`: ProjectOrDomain, Story, UseCase, Actor, Stakeholder
- `ActorContainer`: ProjectOrDomain, Story, UseCase
- `StoryContainer`: ProjectOrDomain, UseCase
- `Scenario` ∈ UseCase
- Deletes also cascade into annotations (`Annotation[Delete]`) and relations.

## Part A — cascade baseline (formalized)

Replace the ad-hoc boolean with a declared, centralized policy so "an authorized operation covers its
intrinsic sub-steps" is data-driven, not a flag each command remembers to set.

- **`OperationAuthorizationPolicy` (project-domain).** A single source of truth mapping each
  delete/mutation operation to (a) its top-level `AuthorizationRequirement` and (b) the set of
  intrinsic cascade sub-command types it is responsible for (detach-from-container, delete-relation,
  remove-annotation). Derived from the container/cascade graph above.
- **Handler support.** Replace the raw `AuthorizationExemptable.isAuthorizationExempt()` bypass with
  a typed "intrinsic sub-step of an authorized ancestor" concept: when a parent command whose
  `AuthorizationRequirement` passed dispatches an intrinsic cascade sub-command (per the policy), that
  sub-command is not independently re-checked. **Direct** gateway/API calls to the same sub-command
  type remain fully authorized (unchanged).
- **Remove the stop-gap.** Delete the `TODO(#75)` markers and the `setAuthorizationExempt(true)` calls
  across `DeleteGoal/Story/Actor/UseCase/GlossaryTerm` and annotation delete commands; keep the
  `AuthorizationExemptable` interface only if the new mechanism reuses it (otherwise remove it too).
- **Close the annotation gap** so annotation detach during an entity delete is covered by the entity's
  `Delete`.

## Part D — permission bundles / presets

Coherent, named grant presets so nobody hand-assembles 25+ individual permissions.

- **Bundles (project-domain constants, vetted sets):**
  - **Owner** — every available permission (what the project creator gets today).
  - **Editor** — Edit + Delete on all content entities (Goal/Actor/Story/UseCase/Scenario/
    GlossaryTerm/Annotation/ReportGenerator) + Project[Edit]; no Grant.
  - **Contributor** — Edit on content entities + Annotation[Edit/Delete]; no Delete of content, no
    Grant.
  - **Reviewer** — Annotation[Edit/Delete] only (comment/discuss); membership gives read.
  Each bundle expands to a coherent, cascade-consistent permission set.
- **Apply-bundle path.** Extend the stakeholder-permission grant flow (`EditUserStakeholder…`) with an
  "apply bundle" operation, keeping per-permission editing as the advanced option.
- **Migrate seeded grants.** Point `EditProjectCommandImpl.createProject` at the **Owner** bundle
  instead of an ad-hoc "grant everything", and define the default bundle a newly-invited stakeholder
  receives (proposed: **Contributor**) — without silently changing existing stakeholders.
- **Angular.** Add a preset picker to the stakeholder-permission editor (advanced per-permission
  editing remains); may need a small bundles endpoint + the existing `my-permissions` shape.

## Slices (each independently testable; commit only when told)

1. **A — cascade baseline (backend).** `OperationAuthorizationPolicy` + handler support; remove the
   stop-gap and `TODO(#75)` markers; close the annotation-detach gap. Update `AuthorizationIT`
   (deleter can delete without Edit; editor≠deleter≠granter preserved) and
   `AuthorizingCommandHandlerTest`; add cascade-via-Delete tests for Story/Actor/UseCase/GlossaryTerm.
2. **D — bundles (backend).** Bundle definitions + coherent expansion; apply-bundle command/API;
   migrate creator + default-invite grants onto bundles; tests for each preset's capability.
3. **D — bundles (frontend).** Angular stakeholder-permission editor preset UI + any supporting
   endpoint; `ng test`.

Verify per slice: `mvn clean verify` (backend); `cd requel-angular && ng test --watch=false` (slice 3).

## Testing strategy

- `AuthorizationIT`: preserve editor/deleter/granter separation except the now-known cascade gap;
  `deleterCanDeleteGoal` (and Story/Actor/UseCase/GlossaryTerm equivalents) pass with only `[Delete]`.
- Direct call to a cascade sub-command (e.g. `RemoveGoalFromGoalContainer`) still requires
  `Goal[Edit]` — proving the exemption is scoped to intrinsic cascades, not a blanket bypass.
- Bundle tests: applying Editor/Contributor/Reviewer/Owner yields the expected coherent capability and
  nothing more.

## Open items to confirm during implementation

- Default bundle for a newly-invited (non-creator) stakeholder — proposed **Contributor**.
- Whether `AuthorizationExemptable` is removed outright or repurposed as the typed intrinsic-step
  marker.
- Exact Editor vs Contributor line (does Contributor get any Delete? proposed: no).

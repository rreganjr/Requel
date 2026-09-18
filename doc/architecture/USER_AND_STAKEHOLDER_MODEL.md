# User & Stakeholder Model

This note explains why the platform maintains both `com.rreganjr.requel.user.User` and the project-facing stakeholder layer, how data flows between them, and where system versus project responsibilities live. It targets developers who touch identity, project membership, or permissions.

## Mental Model

| Concept | Scope | Owns | Backing Types |
| ------- | ----- | ---- | ------------- |
| **User** | System-wide | Authentication, canonical profile, system roles | `User`, `UserImpl`, `UserRole` (and subclasses) |
| **Stakeholder** | Project or domain template | Participation metadata, project permissions, team membership | `Stakeholder`, `AbstractStakeholder`, `UserStakeholderImpl`, `NonUserStakeholderImpl` |
| **UserStakeholder** | Project-level adapter | Bridges a user into a project, exposing project permissions while deferring to the user for identity data | `UserStakeholder`, `UserStakeholderImpl` |

Think of a `User` as "who can sign in" and a `Stakeholder` as "how this person or organization participates in a specific project."

## User Responsibilities

- **Canonical person record** – The user stores name, username, email, phone number, organization, password hash, and the system roles that enable platform-level behavior. Because a person can participate in many projects, this record is the single source of truth for personal information.
- **Authentication & authorisation** – `UserImpl` manages password hashing (`PasswordHasher`) and exposes system-level roles such as `SystemAdminUserRole` and `ProjectUserRole`. These roles follow `UserRole` and hang off the user independently of any project.
- **System roles** –  
  - `SystemAdminUserRole` grants global administration permissions.  
  - `ProjectUserRole` exists once per user but carries per-project membership (see below).  
  - `DomainAdminUserRole` is reserved for future template workflows.
- **Cross-project invariants** – Because these fields are shared across every project, edits made through one project’s UI reflect everywhere.

## Project & Stakeholder Responsibilities

- **Stakeholder definition** – Inside a project, a stakeholder is any person, organization, or entity with a vested interest in the outcome. Stakeholders capture the voices that influence requirements: project sponsors, regulatory bodies, customer proxies, vendors, etc. Many stakeholders will never sign into the system (for example, a government agency that sets policy or a corporate steering committee), but they are still represented so their goals, decisions, and permissions can be tracked.
- **Participation context** – Stakeholders live under `ProjectOrDomain` and represent “this person or organization inside this project”. They own attributes that only make sense in that context: team assignments, goal ownership, project-specific permissions, and any per-project engagement notes.
- **Project permissions** – `StakeholderPermission` and `StakeholderPermissionType` capture fine-grained rights (e.g., edit goals, grant permissions). These are scoped to a single project and are granted/revoked on the stakeholder.
- **Goal relationships** – `AbstractStakeholder` implements `GoalContainer`, so stakeholders keep the set of goals they own or influence.
- **Project membership** – The `ProjectUserRole` referenced above tracks which projects the user can open (`activeProjects`). Stakeholders represent the richer in-project representation once a user is inside.

## UserStakeholder: the Adapter

`UserStakeholderImpl` is the glue between the system identity and a project:

- Maintains a many-to-one link to `UserImpl`, pulling the canonical profile into the project context.
- Should front all personal details through stakeholder-facing accessors (e.g., `getDisplayName()`, `getDisplayEmailAddress()`, `getDisplayPhoneNumber()`). Those methods delegate to the user record today, and later can respect per-project overrides. UI code and APIs should rely on these stakeholder methods rather than reaching directly for `getUser()` when displaying names or contact information.
- Owns the project permission set (`stakeholderPermissions`) so team members can grant or revoke access without mutating the global user.
- Models end-user or domain-expert participation: when someone both influences the project and needs application access, their stakeholder record becomes the adapter that ties real-world responsibilities to system identity.
- Ensures JAXB import/export keeps referential integrity by resolving users through `UserRepository` during `afterUnmarshal`.

### Accessing the Underlying User

- Normal project workflows (navigation trees, grids, export views, API payloads) should call stakeholder-level helpers to render identity details. This keeps presentation logic agnostic to whether the person is a user or a non-user stakeholder and allows future overrides without refactoring every caller. Example: instead of `((UserStakeholder) stakeholder).getUser().getName()`, call `stakeholder.getDisplayName()` which internally delegates appropriately.
- Some subsystems legitimately need the underlying `User`:
  - Mapping an authenticated application session to the corresponding stakeholder (`Project.getUserStakeholder(user)`).
  - Bridging to platform identity services (password reset, role management).
  - Persisting or importing data that must reference the canonical `User` entity.
- In those cases, the stakeholder interface can expose an explicit resolver method (e.g., `asUser()` or `resolveUser()`), making the intent clear and constraining direct access to well understood call sites.

This adapter pattern is the reason we can move identity code into `platform-identity` without rewriting the entire UI: once the platform returns a lean identity interface, the adapter can resolve or wrap it into the richer stakeholder-aware type the project layer expects.

## Handling Contact Information

- **Stakeholders own contact data by default.** For non-user stakeholders (e.g., agencies, vendors, domain experts without logins), email, phone, and other contact details live directly on the stakeholder record; that is the only place the system knows how to reach them.
- **Canonical user data applies automatically.** When a stakeholder wraps a user, the stakeholder delegates to the user’s profile for contact info unless an override is provided. This guarantees that a user who participates in many projects edits their details once and sees it reflected everywhere.
- **Project-specific overrides are optional.** If a project needs alternate contact info—for example, a domain expert wants a project-specific phone number—store the override on the stakeholder (e.g., `StakeholderContactOverrides`). The adapter’s “effective” accessors return the override when present and fall back to the user otherwise.
- **Display helpers use effective data.** UI code should always call the stakeholder’s effective name/email/phone methods. That keeps rendering logic consistent for both user and non-user stakeholders while allowing overrides to work transparently.

## Roles: System vs Project

| Layer | Type | Examples | Who manages it | Stored on |
| ----- | ---- | -------- | -------------- | --------- |
| System | `UserRole` | `SystemAdminUserRole`, `ProjectUserRole`, `DomainAdminUserRole` (future) | Platform administrators | User |
| Project | `StakeholderPermission` | `goal:edit`, `scenario:grant`, `project:invite` | Project stakeholders with grant rights | Stakeholder |

- **System roles** unlock application bootstrap flows: who can create new projects, who can invite users, who can administer the platform. They are coarse-grained and scoped to the entire deployment.
- **Project permissions** determine what actions someone can take inside a specific project. They are assigned by peers and travel with the stakeholder record during export/import.
- `ProjectUserRole` bridges the two layers by tracking which projects a user can access while still being governed as a system role (admin decides who gets added). Once inside a project, their stakeholder record and permissions take over.

## Key Implementation Touchpoints

- `com.rreganjr.requel.user.User` extends `com.rreganjr.platform.identity.User`, making it the canonical identity contract for downstream modules during the migration.
- `JpaUserRepository` supplies users and roles at the system level; project code consumes it primarily through `UserStakeholderImpl.afterUnmarshal` and `LoginCommandImpl`.
- `AbstractStakeholder` persists generic project participation details and ensures all stakeholder variants share sorting and goal containment behavior.
- YAML/JAXB import relies on adapters (`User2UserImplAdapter`, `JAXBCreatedEntityPatcher`, `JAXBUserRolePatcher`) to stitch together identities and stakeholders when loading projects.

## Roadmap Notes

- The new `IdentityUserAdapter` in `modules/requel-app/src/main/java/com/rreganjr/requel/user/bridge` centralises conversions between the lean identity API and the richer user object, paving the way for the platform module to own authentication.
- When `platform-identity` starts returning its own implementations, the adapter will fetch or construct the matching `UserStakeholder`/`User` combination so existing UI code keeps working.
- Future work to align with Spring Security authorities should update this document once authority mapping replaces `StakeholderPermission` or adds a compatibility layer.

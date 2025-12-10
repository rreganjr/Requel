# Identity Migration Plan

## 1. Motivation
The current `com.rreganjr.requel.user` package mixes two distinct concerns:
- **Platform identity** – authentication, password management, roles/permissions, and other cross-application needs.
- **Requirements domain** – project stakeholders, annotations, and business-facing user details.

Splitting these responsibilities allows us to build a reusable “platform” layer while keeping the requirements domain isolated from authentication details.

## 2. Target Module Layout
| Module | Responsibility | Notes |
|--------|----------------|-------|
| `platform-core` | Shared primitives (`Describable`, `NamedEntity`, exceptions, bootstrapping, future `CreatedEntity`) | Already hosts exception hierarchy and bootstrap SPI. |
| `platform-identity` *(new)* | Authentication & authorization APIs/implementation | Owns the public identity interfaces plus Spring Security integration and password logic. |
| Domain modules (`requel-*`) | Requirement-management concepts | Reference users through the lightweight identity API; stakeholder objects carry domain-specific profile data. |

## 3. Identity API Shape
- Expose a minimal contract in `com.rreganjr.platform.identity`:
  ```java
  public interface User {
      Long getId();
      String getUsername();
  }

  public interface Role {
      String getName();
  }
  ```
- Keep the interface name `User` so downstream code does not rename everything to `UserReference`.
- Internally, `platform-identity` provides implementations that satisfy both our interface and Spring Security’s `UserDetails` / `GrantedAuthority`.

## 4. Migration Steps
1. **Create `platform-identity` module**  
   - New Maven module inheriting from the parent.  
   - Dependencies: `spring-security-core`, password encoder libraries, etc.
2. **Move identity internals**  
   - Relocate `com.rreganjr.requel.user` classes (password handling, validators, role management) into the new module.  
   - Adapt `UserImpl` to implement `UserDetails` and expose authorities through Spring Security.
   - Centralise hashing/salting logic (e.g., `PasswordHasher`) and associated exceptions inside `platform-identity`.
   - Shift shared role contracts and exceptions (`UserRole`, `UserRolePermission`, `NoSuch*` identity errors) into `platform-identity` so the requirement domain consumes them without owning their definitions.
3. **Update platform contracts**  
   - Move `CreatedEntity` and related helpers to `platform-core`, replacing the dependency on the full `User` API with the new lightweight interface.  
   - Provide optional helpers (e.g., `getDisplayName`) only when truly needed.
4. **Domain refactor**  
   - Shift personal profile data (name, email, phone) from `User` into stakeholder entities.  
     - Every stakeholder (human or organization) owns its descriptive details, regardless of whether it has login credentials.  
     - A project stakeholder that also logs in simply references the corresponding platform `User`.  
   - Update UI and services to read stakeholder information from the stakeholder model, not the identity object.  
   - Introduce factories/adapters so legacy code that expects `user.getName()` is routed through its stakeholder association until refactors are complete.
5. **Permissions mapping**  
   - Map existing role enums to Spring’s `GrantedAuthority`.  
   - Where domain code checks roles (`hasRole`, `grantRole`, etc.), provide a compatibility service in identity until we rework those flows.
6. **Spring configuration**  
   - Replace direct references to former user classes with beans from `platform-identity`.  
   - Ensure bootstrapping (`DatabaseInitializer`) requests services from the new module when creating default accounts.
7. **Build & IDE**  
   - Add the module to the parent POM and update IntelliJ run configurations (`-pl modules/platform-identity modules/requel-app -am …`).  
   - Reload Maven projects so the IDE notices the new structure.
8. **Validation**  
   - Run `mvn -pl modules/requel-app -am test` (works once local repo writes are permitted).  
   - Add focused unit tests around the identity module (password hashing, authority mapping).

## 5. Roles vs. Permissions

The current system exposes two parallel constructs:
- `UserRole` – system-level responsibilities assigned directly to a user (e.g., administrator).  
- `StakeholderPermission` – project-level capabilities tied to stakeholder instances (`UserStakeholder`, `NonUserStakeholder`).

This overlap complicates reasoning about authorization. The migration should:

1. **Unify authorization around the identity layer**  
   - Model roles/permissions as identity concerns, aligning with Spring Security authorities.  
   - Provide a single source of truth (`User.getAuthorities()`) that the domain can inspect when needed.

2. **Keep stakeholder purely about the participant**  
   - Stakeholder entities capture business context (contact details, organization ties, participation).  
   - When a stakeholder needs permissions, it references the associated user’s authorities rather than maintaining a separate permission graph.

3. **Transition strategy**  
   - *Snapshot existing definitions*: capture every `StakeholderPermission` (entity type + `StakeholderPermissionType`) and assess how it maps to current user roles.  
   - *Introduce a bridge service*: create a `StakeholderAuthorityMapper` that converts stakeholder permission sets into Spring `GrantedAuthority` names (e.g., `project:goal:edit`).  
   - *Unify storage*: persist authorities in a platform-owned table (e.g., `identity_user_authority`) while keeping project metadata attached to the stakeholder for XML import/export.  
   - *Maintain XML compatibility*: update the JAXB import/export process to translate between the persisted authorities and the XML schema fields, so round-tripping does not change.  
   - *Deprecate direct permission checks*: refactor `stakeholder.hasPermission(...)` to delegate to the mapper, allowing a gradual migration without breaking existing logic.

Capturing this alignment in the plan prevents duplicate authorization paths and keeps stakeholders focused on “who/what is involved in the project” while users describe “who can log in and what rights they have”.

### 5.1 User Roles Today

Current role classes:

- `SystemAdminUserRole` – global administrator able to manage users, projects, and grants.  
- `ProjectUserRole` – links a user to project memberships (`ProjectUserRole.getProject()`), effectively the “what projects can this user see?” list.  
- `DomainAdminUserRole` – legacy placeholder for domain template workflows; largely unused today.

Implications for the migration:

1. **Separate global vs. project-scoped authorities**
   - Treat `SystemAdminUserRole` as a global authority (e.g., `ROLE_SYSTEM_ADMIN`) managed exclusively by platform admins.
   - Model project membership as an identity concern (`UserProjectMembership`) expressed as authorities like `project:<projectId>:member`.  The project domain can still maintain stakeholder records, but the identity layer owns who is allowed to open the project.

2. **Stakeholder grants vs. admin grants**
   - Keep the ability for stakeholders to grant permissions within a project via the authority mapper described above. These grants affect only in-project capabilities.
   - Global/project membership authorities continue to be managed via admin tooling, replacing the existing `ProjectUserRole` setters.

3. **Domain admin role**
   - Leave the `DomainAdminUserRole` migration for a later phase when the domain template feature is revived; document it as out-of-scope for the initial identity split.

4. **Plan updates**
   - When implementing the new identity module, create a dedicated persistence model for project access (`identity_project_membership`) that the admin UI can manage.  
   - During the migration, provide a compatibility layer that translates existing `ProjectUserRole` records into the new authority structure so legacy data remains valid.  
   - Update admin workflows to assign authorities through the identity service rather than manipulating role objects directly.

This approach preserves the original distinction—admins control who can access projects, project stakeholders control in-project capabilities—while consolidating everything under a single authority system.

## 5. Risks & Mitigations
- **Large refactor surface** – Move in small steps. Introduce the new interface and module first, then migrate classes while keeping builds green.  
- **Legacy dependencies** – Some domain code may still expect concrete user implementations; introduce adapters or services to bridge the gap temporarily.  
- **Spring Security alignment** – Turning `UserImpl` into `UserDetails` is straightforward, but verify the login flows and context wiring during the migration.  
- **Role semantics** – Validate every `hasRole`/`grantRole` call to ensure behaviour matches the new authority mapping.

## 6. Next Actions
1. Scaffold `platform-identity` with the public interfaces and Spring Security dependencies.  
2. Move password, role, and user implementation classes into the new module, keeping package names stable during the initial cut.  
3. Refactor domain modules to depend on the new identity API, focusing first on `CreatedEntity` and stakeholder profile ownership.  
4. Iterate on role/permission mapping and update UI/business logic accordingly.

### 6.1 Stakeholder Contact Access Refactor

1. **Add stakeholder-facing accessors**  
   - Extend `Stakeholder` (and implementations in `project/impl`) with display helpers (`getDisplayName`, `getDisplayEmailAddress`, `getDisplayPhoneNumber`) that delegate to the underlying user when present and fall back to stakeholder-local data otherwise.  
   - Ensure `UserStakeholderImpl` pulls values from `User` while non-user stakeholders use their own fields.
2. **Audit UI & service layers**  
   - Replace direct calls to `((UserStakeholder) stakeholder).getUser().get…` with the new stakeholder accessors in high-traffic panels (for example `modules/requel-app/src/main/java/com/rreganjr/requel/ui/project/StakeholderNavigatorPanel.java`).  
   - Document any remaining call sites that still need raw user access so future migrations can revisit them.
3. **Command & workflow checks**  
   - Review project commands (`…/impl/command`) and services for similar patterns, e.g., `ImportProjectCommandImpl` when it inspects `ProjectUserRole`. Determine whether stakeholder accessors suffice or if raw user access is required.
4. **XML import/export caveat**  
   - Preserve explicit user references in JAXB adapters and import/export flows (e.g., `UserStakeholderImpl.afterUnmarshal`, `JAXBUserRolePatcher`) so data round-trips remain deterministic. Clearly comment where raw `getUser()` must stay for persistence wiring.
5. **Testing & regression pass**  
   - After rewiring callers, run UI smoke tests (where possible) and targeted unit tests to confirm stakeholder display remains correct and no null-pointer regressions appear when non-user stakeholders are present.

#### 6.1.1 Intentional direct user access (current)

- **Stakeholder editing:** `modules/requel-app/src/main/java/com/rreganjr/requel/ui/project/UserStakeholderEditorPanel.java` still surfaces `getUser()` to populate selectors and audit permission changes—this panel needs the underlying identity object.
- **Project lookup helpers:** `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/ProjectImpl.java` resolves `Project.getUserStakeholder(User)` by comparing the canonical user instance.
- **Import/export workflows:** `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/UserStakeholderImpl.java` (`afterUnmarshal`) and `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/command/ImportProjectCommandImpl.java` manipulate `User` references while stitching persisted data to runtime stakeholders.
- **Role management commands:** `modules/requel-app/src/main/java/com/rreganjr/requel/project/impl/command/DeleteStakeholderCommandImpl.java` and the `ProjectUserRole` hierarchy operate on users to maintain project membership lists.
- These call sites are documented so future migration steps can introduce adapters or services without regressing persistence semantics.
- See `doc/USER_ACCESS_AUDIT.md` for the full inventory and recommendations.

## 7. Current Coupling Snapshot
- `com.rreganjr.requel.user.User` is referenced from 172 source files; 83 in `project`, 34 in `ui`, 21 in `annotation`, 14 in `user`, and the remainder spread across utility and shared helpers.
- The majority of usages expect rich profile data (`getName`, `getEmailAddress`, `getPhoneNumber`) and stakeholder-specific associations, highlighting why simply swapping in the slim `platform.identity.User` causes compile/runtime failures.
- JAXB adapters, repository projections, and UI command flows all directly depend on `UserImpl`, so moving those classes without a compatibility layer immediately breaks imports.
- `UserSet`/`UserSetImpl` surfaced type-erasure issues when generic signatures changed to the new interface, reinforcing the need for an adapter instead of direct replacements.

## 8. Adapter Bridge Strategy
- Introduced `com.rreganjr.requel.user.bridge.IdentityUserAdapter` with a default passthrough implementation that keeps builds green while establishing a single translation point between modules.
- The adapter currently performs a safe cast when the identity instance already implements the richer interface; future phases will resolve or wrap foreign implementations (e.g., fetching a `UserImpl` by id or stakeholder link).
- All forthcoming migrations of repositories/services into `platform-identity` should return the lean interface and rely on the adapter when the UI/domain layer requires `com.rreganjr.requel.user.User`.
- Once stakeholder contact data is separated, we can extend the adapter to compose a view object rather than exposing legacy fields, decoupling identity from profile attributes.

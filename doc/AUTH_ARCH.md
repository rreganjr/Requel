# Authorization Architecture

This document describes how authorization works in the current Requel system and how it will be adapted for the new CQRS API architecture (see `doc/UI_REFACTOR_PLAN.md`).

## 1. Current Authorization Model

### 1.1 Two-Layer Structure

Authorization has two layers:

**Layer 1 — System Roles (coarse-grained)**

| Role Class | Purpose | Permissions |
|---|---|---|
| `SystemAdminUserRole` | Global admin | Implicit — bypasses all checks |
| `ProjectUserRole` | Project membership | `createProjects`, `inviteUsers` (via `UserRolePermission`) |
| `DomainAdminUserRole` | Legacy/disabled | Ignore |

Roles use JPA single-table inheritance (`AbstractUserRole`). A user has a set of roles. Permissions are per-role via `JpaUserRolePermission` entities in the `user_role_permissions` table.

Key methods:
- `user.hasRole(SystemAdminUserRole.class)` — check if user has a system role
- `user.hasUserRolePermission("createProjects")` — check role-level permission

**Layer 2 — Stakeholder Permissions (fine-grained, per-project)**

Each project has stakeholders (user or non-user). User stakeholders have granular permissions:

- **10 entity types:** Goal, Actor, UseCase, Story, Scenario, GlossaryTerm, Stakeholder, Annotation, Project, ReportGenerator
- **3 permission types:** Edit, Delete, Grant
- **= 30 permission combinations per stakeholder**

Permission key format: `com.rreganjr.requel.project.Goal[Edit]`

Permissions are initialized at startup by `StakeholderPermissionsInitializer` which creates all combinations. Stakeholders are granted/revoked specific permissions.

Key methods:
- `stakeholder.hasPermission(entityType, StakeholderPermissionType.Edit)`
- `stakeholder.grantStakeholderPermission(permission)`
- `stakeholder.revokeStakeholderPermission(permission)`

### 1.2 Current Enforcement — The Gap

Most authorization is enforced by **Echo2 UI panels**, not in commands:

- `AbstractRequelProjectEditorPanel.isReadOnlyMode()` checks `stakeholder.hasPermission(entityType, Edit)`
- `AbstractRequelProjectEditorPanel.isShowDelete()` checks `stakeholder.hasPermission(entityType, Delete)`
- `EditUserCommandImpl.execute()` checks `getEditedBy().hasRole(SystemAdminUserRole.class)` for username/role changes — this is **the only command** with an explicit auth check

When the UI becomes a separate Angular app calling API endpoints, the server-side UI panel checks disappear. Every command and query must enforce its own authorization.

### 1.3 Current Command Handler Chain

Commands execute through a decorator chain of `CommandHandler` implementations, configured in `commandHandlerConfig.xml`:

```
RetryOnLockFailuresCommandHandler
  → ExceptionMappingCommandHandler
    → AnalysisInvokingCommandHandler
      → DefaultCommandHandler (@Transactional → command.execute())
```

Each handler implements `CommandHandler.execute(T command)` and wraps the next handler. The `DefaultCommandHandler` at the bottom provides the `@Transactional` boundary and calls `command.execute()`.

Key interfaces in the chain:

```java
// Base command — unit of work
public interface Command {
    void execute() throws Exception;
}

// Knows who is making the change
public interface EditCommand extends Command {
    void setEditedBy(User editedBy);
}

// Triggers NLP analysis after successful edit
public interface AnalyzableEditCommand extends EditCommand {
    void setAnalysisEnabled(boolean analysisEnabled);
    void invokeAnalysis();
}
```

**Composite commands** (commands that execute sub-commands) call `getCommandHandler().execute(subCommand)` internally, which means sub-commands traverse the full handler chain including transactions and analysis.

## 2. New Authorization Architecture

### 2.1 Design Principle: Authorization in the Handler Chain

Rather than scattering auth checks inside each Command's `execute()` method, we add an **`AuthorizingCommandHandler`** to the existing handler chain. This follows the same decorator pattern as `AnalysisInvokingCommandHandler` and `ExceptionMappingCommandHandler`.

The new chain:

```
RetryOnLockFailuresCommandHandler
  → ExceptionMappingCommandHandler
    → AuthorizingCommandHandler              ← NEW
      → AnalysisInvokingCommandHandler
        → DefaultCommandHandler (@Transactional)
```

The `AuthorizingCommandHandler` inspects the command before delegating to the next handler. If the user is not authorized, it throws an `AuthorizationException` before the command enters the transactional boundary.

### 2.2 AuthorizableCommand Interface

Following the same pattern as `EditCommand` (which adds `setEditedBy`) and `AnalyzableEditCommand` (which adds `invokeAnalysis`), we introduce an `AuthorizableCommand` interface that exposes what the handler needs to check authorization:

```java
/**
 * A command that declares its authorization requirements.
 * The AuthorizingCommandHandler inspects this interface to determine
 * if the current user is permitted to execute the command.
 */
public interface AuthorizableCommand extends EditCommand {

    /**
     * The authorization requirement for this command.
     * Returns null if no authorization check is needed (open commands).
     */
    AuthorizationRequirement getAuthorizationRequirement();
}
```

`AuthorizationRequirement` is a sealed interface with variants for each authorization pattern:

```java
public sealed interface AuthorizationRequirement {

    /** User must have the specified system role (e.g., SystemAdminUserRole). */
    record RequiresSystemRole(Class<? extends UserRole> roleType)
        implements AuthorizationRequirement {}

    /** User must have the specified role-level permission (e.g., "createProjects"). */
    record RequiresRolePermission(String permissionName)
        implements AuthorizationRequirement {}

    /** User must be a stakeholder on the target project with the specified permission. */
    record RequiresStakeholderPermission(
        Class<?> entityType,
        StakeholderPermissionType permissionType
    ) implements AuthorizationRequirement {}
}
```

### 2.3 AuthorizingCommandHandler Implementation

```java
public class AuthorizingCommandHandler implements CommandHandler {

    private final CommandHandler delegate;
    private final ProjectRepository projectRepository;

    public AuthorizingCommandHandler(CommandHandler delegate,
                                     ProjectRepository projectRepository) {
        this.delegate = delegate;
        this.projectRepository = projectRepository;
    }

    @Override
    public <T extends Command> T execute(T command) throws Exception {
        if (command instanceof AuthorizableCommand authCmd) {
            checkAuthorization(authCmd);
        }
        return delegate.execute(command);
    }

    private void checkAuthorization(AuthorizableCommand command) {
        AuthorizationRequirement req = command.getAuthorizationRequirement();
        if (req == null) return; // no restriction

        // The user is already set via EditCommand.setEditedBy()
        User user = command.getEditedBy();

        switch (req) {
            case RequiresSystemRole r -> {
                if (!user.hasRole(r.roleType())) {
                    throw new AuthorizationException(
                        "Requires role: " + r.roleType().getSimpleName());
                }
            }
            case RequiresRolePermission r -> {
                if (!user.hasUserRolePermission(r.permissionName())) {
                    throw new AuthorizationException(
                        "Requires permission: " + r.permissionName());
                }
            }
            case RequiresStakeholderPermission r -> {
                // Command must provide the project context
                if (command instanceof ProjectScopedCommand psc) {
                    Stakeholder stakeholder = projectRepository
                        .getStakeholderForUser(psc.getProject(), user);
                    if (!stakeholder.hasPermission(
                            r.entityType(), r.permissionType())) {
                        throw new AuthorizationException(
                            "Requires stakeholder permission: "
                            + r.entityType().getSimpleName()
                            + "[" + r.permissionType() + "]");
                    }
                } else {
                    throw new AuthorizationException(
                        "Stakeholder permission required but command "
                        + "does not provide project context");
                }
            }
        }
    }
}
```

### 2.4 ProjectScopedCommand Interface

For stakeholder permission checks, the handler needs to know which project the command operates on. Commands that work within a project context implement:

```java
/**
 * A command that operates within a specific project.
 * Used by AuthorizingCommandHandler to resolve the stakeholder
 * and check project-level permissions.
 */
public interface ProjectScopedCommand {
    Project getProject();
}
```

Most project commands already have access to the project — they receive it as a setter or resolve it during setup. This interface just exposes it for the handler to read.

### 2.5 Nested (Composite) Command Authorization

Composite commands call `getCommandHandler().execute(subCommand)` internally, which means sub-commands traverse the full handler chain — including the `AuthorizingCommandHandler`.

This is the right behavior with one important consideration: **who is the user for the sub-command?**

There are two cases:

**Case 1: User-initiated sub-commands** — The parent command sets the same `editedBy` user on the sub-command. The sub-command's `getAuthorizationRequirement()` is checked against that user. This is correct — the user must be authorized for all operations their top-level action triggers.

**Case 2: System-initiated sub-commands** — Some commands are triggered by the NLP assistant or other system processes. These run as the "assistant" user. The assistant's authorization must be set up so it has the necessary permissions. The `AuthorizingCommandHandler` checks the assistant user the same way it checks any user.

This means authorization is checked at every level in a composite command. Each sub-command declares its own requirement, and the handler verifies it. There is no "trusted internal" bypass — the handler chain is the single enforcement point.

If a future case requires a parent command to execute a sub-command with elevated privileges, we can add an `AuthorizationRequirement` variant like `Unrestricted` that the handler skips. But this should be rare and explicit, not the default.

### 2.6 Where Each Interface Lives

| Interface | Module | Rationale |
|---|---|---|
| `AuthorizableCommand` | `platform-core` | Alongside `Command`, `EditCommand` |
| `AuthorizationRequirement` | `platform-core` | Used by handler and commands |
| `AuthorizingCommandHandler` | `requel-app` | Needs `ProjectRepository` — wired in handler chain config |
| `ProjectScopedCommand` | `project-domain` | Project-specific concept |
| `AuthorizationException` | `platform-core` | Alongside other exceptions |

### 2.7 Example: EditGoalCommandImpl

```java
public class EditGoalCommandImpl extends AbstractEditProjectOrDomainEntityCommand
    implements EditGoalCommand, ApiCommand<EditGoalInput>,
               AuthorizableCommand, ProjectScopedCommand {

    // ... existing fields and constructor ...

    @Override
    public AuthorizationRequirement getAuthorizationRequirement() {
        return new RequiresStakeholderPermission(Goal.class, Edit);
    }

    @Override
    public Project getProject() {
        return this.project; // already available from setup
    }

    @Override
    public void applyInput(EditGoalInput input) {
        // ... map DTO fields to command ...
    }

    @Override
    public void execute() throws Exception {
        // ... existing business logic, no auth check needed here ...
    }
}
```

### 2.8 Example: EditUserCommandImpl

```java
public class EditUserCommandImpl extends AbstractUserCommand
    implements EditUserCommand, ApiCommand<EditUserInput>,
               AuthorizableCommand {

    @Override
    public AuthorizationRequirement getAuthorizationRequirement() {
        // Only system admins can edit other users' roles/username
        // Self-edit of password/name is handled separately
        if (isEditingOtherUser() || isChangingRoles()) {
            return new RequiresSystemRole(SystemAdminUserRole.class);
        }
        return null; // self-edit, no special role needed
    }

    // ... no need for the manual hasRole check in execute() anymore ...
}
```

### 2.9 Handler Chain Configuration

Updated `commandHandlerConfig.xml`:

```xml
<bean id="commandHandler"
      class="com.rreganjr.command.RetryOnLockFailuresCommandHandler">
  <constructor-arg index="0">
    <bean class="com.rreganjr.command.ExceptionMappingCommandHandler">
      <constructor-arg index="0">
        <bean class="com.rreganjr.repository.jpa.ExceptionMapper"/>
      </constructor-arg>
      <constructor-arg index="1">
        <bean class="com.rreganjr.requel.command.AuthorizingCommandHandler">
          <constructor-arg index="0">
            <bean class="com.rreganjr.requel.command.AnalysisInvokingCommandHandler">
              <constructor-arg index="0">
                <bean class="com.rreganjr.command.DefaultCommandHandler"/>
              </constructor-arg>
            </bean>
          </constructor-arg>
          <constructor-arg index="1">
            <ref bean="projectRepository"/>
          </constructor-arg>
        </bean>
      </constructor-arg>
    </bean>
  </constructor-arg>
  <constructor-arg index="1">
    <ref bean="projectRepository"/>
  </constructor-arg>
</bean>
```

Note: `AuthorizingCommandHandler` is placed **inside** `ExceptionMappingCommandHandler` so that `AuthorizationException` gets mapped to the appropriate HTTP response (403 Forbidden) by the exception mapping layer.

## 3. Query Authorization

Read endpoints don't go through the command handler chain. Authorization for queries is enforced in the REST controller or a service layer:

### 3.1 Project Access Check

```java
@Service
public class ProjectAccessChecker {

    private final ProjectRepository projectRepository;

    /** Verifies the user is a stakeholder on this project. */
    public Stakeholder requireStakeholder(Project project, User user) {
        try {
            return projectRepository.getStakeholderForUser(project, user);
        } catch (NoSuchStakeholderException e) {
            throw new AuthorizationException(
                "User is not a stakeholder on project: " + project.getName());
        }
    }

    /** SystemAdmin can see everything; others only their projects. */
    public boolean canAccessProject(Project project, User user) {
        if (user.hasRole(SystemAdminUserRole.class)) return true;
        try {
            projectRepository.getStakeholderForUser(project, user);
            return true;
        } catch (NoSuchStakeholderException e) {
            return false;
        }
    }
}
```

### 3.2 Query Controller Usage

```java
@RestController
@RequestMapping("/api/projects/{projectId}")
public class ProjectGoalQueryController {

    @GetMapping("/goals")
    public Page<GoalDto> listGoals(
            @PathVariable Long projectId,
            @AuthenticationPrincipal JwtUser principal,
            Pageable pageable) {
        Project project = projectRepository.get(projectId);
        User user = currentUserResolver.resolve(principal);
        projectAccessChecker.requireStakeholder(project, user);
        return goalQueryService.listGoals(project, pageable);
    }
}
```

### 3.3 Admin-Only Endpoints

System admin endpoints use Spring Security's `@PreAuthorize` or URL-based security config:

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/admin/**").hasRole("SYSTEM_ADMIN")
            .requestMatchers("/api/auth/login").permitAll()
            .requestMatchers("/api/**").authenticated()
        );
        // ...
    }
}
```

## 4. Angular Permission Exposure

The Angular app needs permission information to configure the UI (show/hide edit buttons, delete buttons, etc.).

### 4.1 System Roles — In the JWT

The JWT token includes the user's system roles:

```json
{
  "sub": "ron",
  "roles": ["PROJECT_USER"],
  "permissions": ["createProjects"],
  "exp": 1234567890
}
```

Angular reads these from the token for system-level UI decisions (show admin menu, show "New Project" button).

### 4.2 Project Permissions — Via API Endpoint

Project-level stakeholder permissions are too granular and project-specific for the JWT. They are fetched per-project:

```
GET /api/projects/{projectId}/my-permissions
→ {
    "stakeholderId": 42,
    "permissions": ["Goal[Edit]", "Goal[Delete]", "Actor[Edit]", ...]
  }
```

### 4.3 Angular PermissionService

```typescript
@Injectable({ providedIn: 'root' })
export class PermissionService {
  private projectPermissions = signal<Map<number, string[]>>(new Map());

  // System-level (from JWT)
  readonly isSystemAdmin = computed(() =>
    this.authService.roles().includes('SYSTEM_ADMIN'));

  readonly canCreateProjects = computed(() =>
    this.authService.permissions().includes('createProjects'));

  // Project-level (fetched from API, cached per project)
  loadProjectPermissions(projectId: number): void {
    this.http.get<MyPermissionsDto>(
      `/api/projects/${projectId}/my-permissions`
    ).subscribe(result => {
      this.projectPermissions.update(map => {
        const next = new Map(map);
        next.set(projectId, result.permissions);
        return next;
      });
    });
  }

  hasPermission(projectId: number, entityType: string,
                permissionType: string): Signal<boolean> {
    return computed(() => {
      const perms = this.projectPermissions().get(projectId) ?? [];
      return perms.includes(`${entityType}[${permissionType}]`);
    });
  }
}
```

### 4.4 Usage in Components

```typescript
@Component({
  template: `
    @if (canEditGoal()) {
      <button (click)="edit()">Edit</button>
    }
    @if (canDeleteGoal()) {
      <button (click)="delete()">Delete</button>
    }
  `
})
export class GoalEditorComponent {
  private permissions = inject(PermissionService);
  projectId = input.required<number>();

  canEditGoal = computed(() =>
    this.permissions.hasPermission(this.projectId(), 'Goal', 'Edit')());
  canDeleteGoal = computed(() =>
    this.permissions.hasPermission(this.projectId(), 'Goal', 'Delete')());
}
```

## 5. Authorization Error Handling

### 5.1 HTTP Response

`AuthorizationException` maps to HTTP 403 Forbidden with a structured error body:

```json
{
  "error": "FORBIDDEN",
  "message": "Requires stakeholder permission: Goal[Edit]",
  "timestamp": "2026-03-06T12:00:00Z"
}
```

The `ExceptionMappingCommandHandler` wraps the `AuthorizingCommandHandler`, so authorization failures are converted to the appropriate application exception type before reaching the REST layer.

### 5.2 Angular Error Handling

The Angular HTTP interceptor handles 403 responses:

- Display a notification ("You don't have permission to perform this action")
- Do not redirect — the user stays on the current page
- Optionally refresh permissions in case they changed

## 6. Migration Approach

### Phase 0 — Foundation (UI_REFACTOR_PLAN Phase 0)
- Add `AuthorizableCommand`, `AuthorizationRequirement`, `ProjectScopedCommand` interfaces
- Add `AuthorizingCommandHandler` to the handler chain
- Add `AuthorizationException`
- Add `ProjectAccessChecker` service

### Phase 2 — Project CRUD (UI_REFACTOR_PLAN Phase 2)
- Add `GET /api/projects/{projectId}/my-permissions` endpoint (needed once project-scoped UI appears)
- Implement Angular `PermissionService` — system roles from JWT, project permissions from the my-permissions endpoint
- Wire permission checks into project editor and stakeholder components

### Per-Phase (as commands are API-enabled)
- Each command that implements `ApiCommand<T>` also implements `AuthorizableCommand`
- Add `getAuthorizationRequirement()` returning the appropriate requirement
- Project-scoped commands implement `ProjectScopedCommand`
- Query controllers use `ProjectAccessChecker`

## 7. Summary

| Concern | Where | How |
|---|---|---|
| Authentication | JWT filter (Spring Security) | Already planned in UI_REFACTOR_PLAN.md Section 3.4 |
| System role check | Spring Security config + JWT roles | URL rules for `/api/admin/**` |
| Command authorization | `AuthorizingCommandHandler` in handler chain | Inspects `AuthorizableCommand.getAuthorizationRequirement()` |
| Nested command auth | Same handler chain | Sub-commands go through full chain; each declares its own requirement |
| Query authorization | `ProjectAccessChecker` in controllers | Verifies stakeholder membership before returning data |
| Angular UI gating | `PermissionService` + signals | System roles from JWT, project permissions from API endpoint |

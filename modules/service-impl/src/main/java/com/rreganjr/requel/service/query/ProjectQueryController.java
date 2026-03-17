package com.rreganjr.requel.service.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.StakeholderPermission;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.ExportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.service.api.dto.NonUserStakeholderDetails;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.ProjectPermissionsDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;
import com.rreganjr.requel.service.api.dto.StakeholderDto;
import com.rreganjr.requel.service.api.dto.UserStakeholderDetails;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Read endpoints for project users.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectQueryController {

    private static final Logger log = LoggerFactory.getLogger(ProjectQueryController.class);

    private final ProjectRepository projectRepository;
    private final ProjectCommandFactory projectCommandFactory;
    private final CommandHandler commandHandler;
    private final CurrentUserResolver currentUserResolver;
    private final EntityManager entityManager;

    public ProjectQueryController(ProjectRepository projectRepository,
                                  ProjectCommandFactory projectCommandFactory,
                                  CommandHandler commandHandler,
                                  CurrentUserResolver currentUserResolver,
                                  EntityManager entityManager) {
        this.projectRepository = projectRepository;
        this.projectCommandFactory = projectCommandFactory;
        this.commandHandler = commandHandler;
        this.currentUserResolver = currentUserResolver;
        this.entityManager = entityManager;
    }

    /**
     * GET /api/projects — list projects visible to the current user.
     * Admins see all projects; regular users see their active projects.
     */
    @GetMapping
    public List<ProjectDto> listProjects() {
        User user = currentUserResolver.resolve();
        Collection<? extends Project> projects;
        if (user.hasRole(SystemAdminUserRole.class)) {
            projects = findAllProjects();
        } else if (user.hasRole(ProjectUserRole.class)) {
            ProjectUserRole role = user.getRoleForType(ProjectUserRole.class);
            projects = role.getActiveProjects();
        } else {
            projects = Collections.emptySet();
        }
        return projects.stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(ProjectDto::name))
                .toList();
    }

    /**
     * GET /api/projects/{name} — single project by name.
     */
    @GetMapping("/{name}")
    public ResponseEntity<ProjectDto> getProject(@PathVariable String name) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            return ResponseEntity.ok(toDto(project));
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * GET /api/projects/{name}/my-permissions — stakeholder permissions for the current user.
     */
    @GetMapping("/{name}/my-permissions")
    public ResponseEntity<ProjectPermissionsDto> getMyPermissions(@PathVariable String name) {
        try {
            Project project = projectRepository.findProjectByName(name);
            User user = requireProjectAccess(project);

            boolean canCreate = user.hasRole(ProjectUserRole.class)
                    && user.getRoleForType(ProjectUserRole.class).canCreateProjects();

            // Find the current user's stakeholder in this project
            UserStakeholder stakeholder = findUserStakeholder(project, user);
            if (stakeholder == null) {
                return ResponseEntity.ok(new ProjectPermissionsDto(false, canCreate, Collections.emptyMap()));
            }

            // Build permission map: entitySimpleName -> set of permission type names
            Map<String, Set<String>> permMap = new HashMap<>();
            for (StakeholderPermission perm : stakeholder.getStakeholderPermissions()) {
                String entityName = perm.getEntityType().getSimpleName();
                permMap.computeIfAbsent(entityName, k -> new HashSet<>())
                        .add(perm.getPermissionType().name());
            }

            return ResponseEntity.ok(new ProjectPermissionsDto(true, canCreate, permMap));
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * GET /api/projects/{name}/tree — project content tree for sidebar navigation.
     */
    @GetMapping("/{name}/tree")
    public ResponseEntity<List<ProjectTreeNodeDto>> getProjectTree(@PathVariable String name) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            List<ProjectTreeNodeDto> tree = new ArrayList<>();

            tree.add(treeGroup("Stakeholders", project.getStakeholders(), s -> s.getDisplayName()));
            tree.add(treeGroup("Goals", project.getGoals(), Goal::getName));
            tree.add(treeGroup("Stories", project.getStories(), Story::getName));
            tree.add(treeGroup("Actors", project.getActors(), Actor::getName));
            tree.add(treeGroup("Use Cases", project.getUseCases(), UseCase::getName));
            tree.add(treeGroup("Glossary", project.getGlossaryTerms(), GlossaryTerm::getName));

            return ResponseEntity.ok(tree);
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * GET /api/projects/{name}/export — download project as XML.
     */
    @GetMapping("/{name}/export")
    public void exportProject(@PathVariable String name, HttpServletResponse response) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            response.setContentType("application/xml");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + project.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".xml\"");

            ExportProjectCommand cmd = projectCommandFactory.newExportProjectCommand();
            cmd.setProject(project);
            cmd.setOutputStream(response.getOutputStream());
            commandHandler.execute(cmd);
        } catch (NoSuchProjectException e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (AuthorizationException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            log.error("Could not export project '{}': {}", name, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * GET /api/projects/{name}/stakeholders — list all stakeholders for a project.
     */
    @GetMapping("/{name}/stakeholders")
    public ResponseEntity<?> listStakeholders(@PathVariable String name) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            List<StakeholderDto> dtos = project.getStakeholders().stream()
                    .map(ProjectQueryController::toStakeholderDto)
                    .sorted(Comparator.comparing(StakeholderDto::name))
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * GET /api/projects/{name}/stakeholders/{id} — single stakeholder detail.
     */
    @GetMapping("/{name}/stakeholders/{stakeholderId}")
    public ResponseEntity<?> getStakeholder(@PathVariable String name,
                                            @PathVariable Long stakeholderId) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            for (Stakeholder s : project.getStakeholders()) {
                if (s.getId().equals(stakeholderId)) {
                    return ResponseEntity.ok(toStakeholderDto(s));
                }
            }
            return ResponseEntity.notFound().build();
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Verify the current user is an admin or a stakeholder on the project.
     * Throws AuthorizationException if access is denied.
     */
    private User requireProjectAccess(Project project) {
        User user = currentUserResolver.resolve();
        if (user.hasRole(SystemAdminUserRole.class)) {
            return user;
        }
        if (findUserStakeholder(project, user) != null) {
            return user;
        }
        throw new AuthorizationException("You do not have access to this project.");
    }

    private UserStakeholder findUserStakeholder(Project project, User user) {
        for (Stakeholder s : project.getStakeholders()) {
            if (s.matchesUser(user) && s instanceof UserStakeholder us) {
                return us;
            }
        }
        return null;
    }

    private <T extends ProjectOrDomainEntity> ProjectTreeNodeDto treeGroup(String groupName,
                                             Collection<? extends T> items,
                                             java.util.function.Function<T, String> nameExtractor) {
        List<ProjectTreeNodeDto> children = items.stream()
                .map(item -> new ProjectTreeNodeDto(item.getId(), groupName, nameExtractor.apply(item)))
                .sorted(Comparator.comparing(ProjectTreeNodeDto::name))
                .toList();
        return new ProjectTreeNodeDto(groupName, groupName, children);
    }

    @SuppressWarnings("unchecked")
    private List<Project> findAllProjects() {
        return entityManager
                .createQuery("select p from ProjectImpl p order by p.name")
                .getResultList();
    }

    public static StakeholderDto toStakeholderDto(Stakeholder stakeholder) {
        UserStakeholderDetails userDetails = null;
        NonUserStakeholderDetails nonUserDetails = null;

        if (stakeholder instanceof UserStakeholder us) {
            List<String> permissionKeys = us.getStakeholderPermissions().stream()
                    .map(StakeholderPermission::getPermissionKey)
                    .sorted()
                    .toList();
            userDetails = new UserStakeholderDetails(
                    us.getDisplayUsername(),
                    us.getDisplayEmailAddress(),
                    us.getDisplayPhoneNumber(),
                    us.getTeam() != null ? us.getTeam().getName() : null,
                    permissionKeys
            );
        } else if (stakeholder instanceof NonUserStakeholder nus) {
            nonUserDetails = new NonUserStakeholderDetails(nus.getText());
        }

        return new StakeholderDto(
                stakeholder.getId(),
                stakeholder.getVersion(),
                stakeholder.getDisplayName(),
                stakeholder.isUserStakeholder() ? "user" : "non-user",
                stakeholder.getCreatedBy() != null ? stakeholder.getCreatedBy().getDisplayName() : null,
                userDetails,
                nonUserDetails
        );
    }

    private ProjectDto toDto(Project project) {
        return new ProjectDto(
                project.getId(),
                project.getVersion(),
                project.getName(),
                project.getText(),
                project.getOrganization() != null ? project.getOrganization().getName() : null,
                project.getCreatedBy() != null ? project.getCreatedBy().getDisplayName() : null,
                project.getStatus(),
                project.getStakeholders().size(),
                project.getGoals().size(),
                project.getStories().size(),
                project.getActors().size(),
                project.getUseCases().size(),
                project.getScenarios().size(),
                project.getGlossaryTerms().size()
        );
    }
}

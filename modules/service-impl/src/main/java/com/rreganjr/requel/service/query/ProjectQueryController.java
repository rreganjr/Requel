package com.rreganjr.requel.service.query;

import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectUserRole;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Read endpoints for project administration.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectQueryController {

    private final ProjectRepository projectRepository;
    private final CurrentUserResolver currentUserResolver;
    private final EntityManager entityManager;

    public ProjectQueryController(ProjectRepository projectRepository,
                                  CurrentUserResolver currentUserResolver,
                                  EntityManager entityManager) {
        this.projectRepository = projectRepository;
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
            return ResponseEntity.ok(toDto(project));
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Project> findAllProjects() {
        return entityManager
                .createQuery("select p from ProjectImpl p order by p.name")
                .getResultList();
    }

    private ProjectDto toDto(Project project) {
        return new ProjectDto(
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

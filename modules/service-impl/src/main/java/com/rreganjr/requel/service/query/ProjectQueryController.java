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
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomain;
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
import com.rreganjr.requel.service.api.dto.ActorDto;
import com.rreganjr.requel.service.api.dto.ScenarioDto;
import com.rreganjr.requel.service.api.dto.StepDto;
import com.rreganjr.requel.service.api.dto.EntityReferenceDto;
import com.rreganjr.requel.service.api.dto.GoalDto;
import com.rreganjr.requel.service.api.dto.GoalRelationDto;
import com.rreganjr.requel.service.api.dto.NonUserStakeholderDetails;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.UseCaseDto;
import com.rreganjr.requel.service.api.dto.ProjectPermissionsDto;
import com.rreganjr.requel.service.api.dto.ProjectTreeNodeDto;
import com.rreganjr.requel.service.api.dto.StakeholderDto;
import com.rreganjr.requel.service.api.dto.StakeholderPermissionDto;
import com.rreganjr.requel.service.api.dto.StoryDto;
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
     * GET /api/projects/stakeholder-permissions — catalog of all available stakeholder permissions.
     * Returns the full permission matrix (entity type × permission type) so the
     * stakeholder editor can render checkboxes.
     * NOTE: This must be declared before /{name} to avoid the path variable matching "stakeholder-permissions".
     */
    @GetMapping("/stakeholder-permissions")
    public List<StakeholderPermissionDto> listAvailablePermissions() {
        return projectRepository.findAvailableStakeholderPermissions().stream()
                .map(p -> new StakeholderPermissionDto(
                        p.getPermissionKey(),
                        p.getEntityType().getSimpleName(),
                        p.getPermissionType().name()))
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
                    return ResponseEntity.ok(toStakeholderDetailDto(s));
                }
            }
            return ResponseEntity.notFound().build();
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // ── Goals ──────────────────────────────────────────────────────────

    /**
     * GET /api/projects/{name}/goals — list all goals (summary, no relations/referers).
     */
    @GetMapping("/{name}/goals")
    public ResponseEntity<?> listGoals(@PathVariable String name) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            List<GoalDto> dtos = project.getGoals().stream()
                    .map(ProjectQueryController::toGoalSummaryDto)
                    .sorted(Comparator.comparing(GoalDto::name))
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * GET /api/projects/{name}/goals/{goalId} — single goal with relations and referencedBy.
     */
    @GetMapping("/{name}/goals/{goalId}")
    public ResponseEntity<?> getGoal(@PathVariable String name, @PathVariable Long goalId) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            for (Goal g : project.getGoals()) {
                if (g.getId().equals(goalId)) {
                    return ResponseEntity.ok(toGoalDetailDto(g));
                }
            }
            return ResponseEntity.notFound().build();
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // ── Stories ────────────────────────────────────────────────────────

    /**
     * GET /api/projects/{name}/stories — list all stories (summary).
     */
    @GetMapping("/{name}/stories")
    public ResponseEntity<?> listStories(@PathVariable String name) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            List<StoryDto> dtos = project.getStories().stream()
                    .map(ProjectQueryController::toStorySummaryDto)
                    .sorted(Comparator.comparing(StoryDto::name))
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * GET /api/projects/{name}/stories/{storyId} — single story with goals and actors.
     */
    @GetMapping("/{name}/stories/{storyId}")
    public ResponseEntity<?> getStory(@PathVariable String name, @PathVariable Long storyId) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            for (Story s : project.getStories()) {
                if (s.getId().equals(storyId)) {
                    return ResponseEntity.ok(toStoryDetailDto(s));
                }
            }
            return ResponseEntity.notFound().build();
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // ── Actors ─────────────────────────────────────────────────────────

    /**
     * GET /api/projects/{name}/actors — list all actors (summary).
     */
    @GetMapping("/{name}/actors")
    public ResponseEntity<?> listActors(@PathVariable String name) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            List<ActorDto> dtos = project.getActors().stream()
                    .map(ProjectQueryController::toActorSummaryDto)
                    .sorted(Comparator.comparing(ActorDto::name))
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * GET /api/projects/{name}/actors/{actorId} — single actor with goals.
     */
    @GetMapping("/{name}/actors/{actorId}")
    public ResponseEntity<?> getActor(@PathVariable String name, @PathVariable Long actorId) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            for (Actor a : project.getActors()) {
                if (a.getId().equals(actorId)) {
                    return ResponseEntity.ok(toActorDetailDto(a));
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
     * GET /api/projects/{name}/scenarios — list of scenarios (no steps).
     */
    @GetMapping("/{name}/scenarios")
    public ResponseEntity<?> listScenarios(@PathVariable String name) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            List<ScenarioDto> dtos = project.getScenarios().stream()
                    .map(ProjectQueryController::toScenarioSummaryDto)
                    .sorted(Comparator.comparing(ScenarioDto::name))
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * GET /api/projects/{name}/scenarios/{scenarioId} — single scenario with step list.
     */
    @GetMapping("/{name}/scenarios/{scenarioId}")
    public ResponseEntity<?> getScenario(@PathVariable String name, @PathVariable Long scenarioId) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            for (Scenario s : project.getScenarios()) {
                if (s.getId().equals(scenarioId)) {
                    return ResponseEntity.ok(toScenarioDetailDto(s));
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
     * GET /api/projects/{name}/use-cases — list of use cases (summary, no sub-tables).
     */
    @GetMapping("/{name}/use-cases")
    public ResponseEntity<?> listUseCases(@PathVariable String name) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            List<UseCaseDto> dtos = project.getUseCases().stream()
                    .map(ProjectQueryController::toUseCaseSummaryDto)
                    .sorted(Comparator.comparing(UseCaseDto::name))
                    .toList();
            return ResponseEntity.ok(dtos);
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * GET /api/projects/{name}/use-cases/{useCaseId} — single use case with sub-tables.
     */
    @GetMapping("/{name}/use-cases/{useCaseId}")
    public ResponseEntity<?> getUseCase(@PathVariable String name, @PathVariable Long useCaseId) {
        try {
            Project project = projectRepository.findProjectByName(name);
            requireProjectAccess(project);
            for (UseCase uc : project.getUseCases()) {
                if (uc.getId().equals(useCaseId)) {
                    return ResponseEntity.ok(toUseCaseDetailDto(uc));
                }
            }
            return ResponseEntity.notFound().build();
        } catch (NoSuchProjectException e) {
            return ResponseEntity.notFound().build();
        } catch (AuthorizationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────

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
                nonUserDetails,
                null
        );
    }

    public static StakeholderDto toStakeholderDetailDto(Stakeholder stakeholder) {
        StakeholderDto summary = toStakeholderDto(stakeholder);
        List<EntityReferenceDto> goals = stakeholder.getGoals().stream()
                .map(g -> new EntityReferenceDto("Goal", g.getId(), g.getName()))
                .sorted(Comparator.comparing(EntityReferenceDto::name))
                .toList();
        return new StakeholderDto(
                summary.id(), summary.version(), summary.name(), summary.type(),
                summary.createdBy(), summary.userDetails(), summary.nonUserDetails(),
                goals
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

    // ── Goal/Story DTO mappers ────────────────────────────────────────

    static GoalDto toGoalSummaryDto(Goal goal) {
        return new GoalDto(
                goal.getId(), goal.getVersion(), goal.getName(), goal.getText(),
                goal.getCreatedBy() != null ? goal.getCreatedBy().getDisplayName() : null,
                null, null, null);
    }

    public static GoalDto toGoalDetailDto(Goal goal) {
        List<GoalRelationDto> fromRelations = goal.getRelationsFromThisGoal().stream()
                .map(r -> new GoalRelationDto(r.getId(), r.getVersion(),
                        r.getToGoal().getId(), r.getToGoal().getName(),
                        r.getRelationType().name()))
                .sorted(Comparator.comparing(GoalRelationDto::goalName))
                .toList();

        List<GoalRelationDto> toRelations = goal.getRelationsToThisGoal().stream()
                .map(r -> new GoalRelationDto(r.getId(), r.getVersion(),
                        r.getFromGoal().getId(), r.getFromGoal().getName(),
                        r.getRelationType().name()))
                .sorted(Comparator.comparing(GoalRelationDto::goalName))
                .toList();

        List<EntityReferenceDto> referers = goal.getReferers().stream()
                .map(ProjectQueryController::toEntityReference)
                .sorted(Comparator.comparing(EntityReferenceDto::entityType)
                        .thenComparing(EntityReferenceDto::name))
                .toList();

        return new GoalDto(
                goal.getId(), goal.getVersion(), goal.getName(), goal.getText(),
                goal.getCreatedBy() != null ? goal.getCreatedBy().getDisplayName() : null,
                fromRelations, toRelations, referers);
    }

    static StoryDto toStorySummaryDto(Story story) {
        return new StoryDto(
                story.getId(), story.getVersion(), story.getName(), story.getText(),
                story.getStoryType().name(),
                story.getCreatedBy() != null ? story.getCreatedBy().getDisplayName() : null,
                null, null);
    }

    public static StoryDto toStoryDetailDto(Story story) {
        List<EntityReferenceDto> goals = story.getGoals().stream()
                .map(g -> new EntityReferenceDto("Goal", g.getId(), g.getName()))
                .sorted(Comparator.comparing(EntityReferenceDto::name))
                .toList();

        List<EntityReferenceDto> actors = story.getActors().stream()
                .map(a -> new EntityReferenceDto("Actor", a.getId(), a.getName()))
                .sorted(Comparator.comparing(EntityReferenceDto::name))
                .toList();

        return new StoryDto(
                story.getId(), story.getVersion(), story.getName(), story.getText(),
                story.getStoryType().name(),
                story.getCreatedBy() != null ? story.getCreatedBy().getDisplayName() : null,
                goals, actors);
    }

    // ── Actor DTO mappers ─────────────────────────────────────────────

    static ActorDto toActorSummaryDto(Actor actor) {
        return new ActorDto(
                actor.getId(), actor.getVersion(), actor.getName(), actor.getText(),
                actor.getCreatedBy() != null ? actor.getCreatedBy().getDisplayName() : null,
                null, null, null);
    }

    public static ActorDto toActorDetailDto(Actor actor) {
        List<EntityReferenceDto> goals = actor.getGoals().stream()
                .map(g -> new EntityReferenceDto("Goal", g.getId(), g.getName()))
                .sorted(Comparator.comparing(EntityReferenceDto::name))
                .toList();
        List<EntityReferenceDto> referencedByUseCases = actor.getReferers().stream()
                .filter(r -> r instanceof UseCase)
                .map(r -> new EntityReferenceDto("UseCase", ((UseCase) r).getId(), ((UseCase) r).getName()))
                .sorted(Comparator.comparing(EntityReferenceDto::name))
                .toList();
        List<EntityReferenceDto> referencedByStories = actor.getReferers().stream()
                .filter(r -> r instanceof Story)
                .map(r -> new EntityReferenceDto("Story", ((Story) r).getId(), ((Story) r).getName()))
                .sorted(Comparator.comparing(EntityReferenceDto::name))
                .toList();
        return new ActorDto(
                actor.getId(), actor.getVersion(), actor.getName(), actor.getText(),
                actor.getCreatedBy() != null ? actor.getCreatedBy().getDisplayName() : null,
                goals, referencedByUseCases, referencedByStories);
    }

    static ScenarioDto toScenarioSummaryDto(Scenario scenario) {
        return new ScenarioDto(
                scenario.getId(), scenario.getVersion(), scenario.getName(), scenario.getText(),
                scenario.getType() != null ? scenario.getType().name() : null,
                scenario.getCreatedBy() != null ? scenario.getCreatedBy().getDisplayName() : null,
                null);
    }

    public static ScenarioDto toScenarioDetailDto(Scenario scenario) {
        List<StepDto> steps = new ArrayList<>();
        for (Step step : scenario.getSteps()) {
            boolean isScenario = step instanceof Scenario;
            steps.add(new StepDto(
                    step.getId(),
                    step.getVersion(),
                    step.getName(),
                    step.getText(),
                    step.getType() != null ? step.getType().name() : null,
                    isScenario,
                    isScenario ? step.getId() : null));
        }
        return new ScenarioDto(
                scenario.getId(), scenario.getVersion(), scenario.getName(), scenario.getText(),
                scenario.getType() != null ? scenario.getType().name() : null,
                scenario.getCreatedBy() != null ? scenario.getCreatedBy().getDisplayName() : null,
                steps);
    }

    public static UseCaseDto toUseCaseSummaryDto(UseCase uc) {
        return new UseCaseDto(
                uc.getId(), uc.getVersion(), uc.getName(), uc.getText(),
                uc.getPrimaryActor() != null ? uc.getPrimaryActor().getName() : null,
                uc.getCreatedBy() != null ? uc.getCreatedBy().getDisplayName() : null,
                uc.getScenario() != null ? uc.getScenario().getId() : null,
                uc.getScenario() != null ? uc.getScenario().getName() : null,
                uc.getScenario() != null ? uc.getScenario().getSteps().size() : null,
                null, null, null);
    }

    public static UseCaseDto toUseCaseDetailDto(UseCase uc) {
        List<GoalDto> goals = uc.getGoals().stream()
                .map(g -> new GoalDto(g.getId(), g.getVersion(), g.getName(), g.getText(),
                        g.getCreatedBy() != null ? g.getCreatedBy().getDisplayName() : null,
                        null, null, null))
                .sorted(Comparator.comparing(GoalDto::name, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<ActorDto> actors = uc.getActors().stream()
                .map(a -> new ActorDto(a.getId(), a.getVersion(), a.getName(), a.getText(),
                        a.getCreatedBy() != null ? a.getCreatedBy().getDisplayName() : null,
                        null, null, null))
                .sorted(Comparator.comparing(ActorDto::name))
                .toList();
        List<StoryDto> stories = uc.getStories().stream()
                .map(s -> new StoryDto(s.getId(), s.getVersion(), s.getName(), s.getText(),
                        s.getStoryType() != null ? s.getStoryType().name() : null,
                        s.getCreatedBy() != null ? s.getCreatedBy().getDisplayName() : null,
                        null, null))
                .sorted(Comparator.comparing(StoryDto::name, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return new UseCaseDto(
                uc.getId(), uc.getVersion(), uc.getName(), uc.getText(),
                uc.getPrimaryActor() != null ? uc.getPrimaryActor().getName() : null,
                uc.getCreatedBy() != null ? uc.getCreatedBy().getDisplayName() : null,
                uc.getScenario() != null ? uc.getScenario().getId() : null,
                uc.getScenario() != null ? uc.getScenario().getName() : null,
                uc.getScenario() != null ? uc.getScenario().getSteps().size() : null,
                goals, actors, stories);
    }

    /**
     * Convert a GoalContainer referer to an EntityReferenceDto.
     * GoalContainer doesn't expose getId() — we check concrete types.
     */
    private static EntityReferenceDto toEntityReference(GoalContainer container) {
        if (container instanceof ProjectOrDomainEntity entity) {
            String typeName = entity.getProjectOrDomainEntityInterface().getSimpleName();
            return new EntityReferenceDto(typeName, entity.getId(), entity.getName());
        }
        if (container instanceof ProjectOrDomain pod) {
            return new EntityReferenceDto("Project", pod.getId(), pod.getName());
        }
        // Fallback for unknown container types
        return new EntityReferenceDto("Unknown", null, container.getDescription());
    }
}

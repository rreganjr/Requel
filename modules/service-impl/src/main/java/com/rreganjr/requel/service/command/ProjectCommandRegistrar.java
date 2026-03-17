package com.rreganjr.requel.service.command;

import java.io.IOException;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.AddGoalToGoalContainerCommand;
import com.rreganjr.requel.project.command.CopyGoalCommand;
import com.rreganjr.requel.project.command.CopyStoryCommand;
import com.rreganjr.requel.project.command.DeleteGoalCommand;
import com.rreganjr.requel.project.command.DeleteGoalRelationCommand;
import com.rreganjr.requel.project.command.DeleteStakeholderCommand;
import com.rreganjr.requel.project.command.DeleteStoryCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditGoalRelationCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveGoalFromGoalContainerCommand;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.api.dto.AddGoalToGoalContainerInput;
import com.rreganjr.requel.service.api.dto.CopyGoalInput;
import com.rreganjr.requel.service.api.dto.CopyStoryInput;
import com.rreganjr.requel.service.api.dto.DeleteGoalInput;
import com.rreganjr.requel.service.api.dto.DeleteGoalRelationInput;
import com.rreganjr.requel.service.api.dto.DeleteStakeholderInput;
import com.rreganjr.requel.service.api.dto.DeleteStoryInput;
import com.rreganjr.requel.service.api.dto.EditGoalInput;
import com.rreganjr.requel.service.api.dto.EditGoalRelationInput;
import com.rreganjr.requel.service.api.dto.EditNonUserStakeholderInput;
import com.rreganjr.requel.service.api.dto.EditProjectInput;
import com.rreganjr.requel.service.api.dto.EditStoryInput;
import com.rreganjr.requel.service.api.dto.EditUserStakeholderInput;
import com.rreganjr.requel.service.api.dto.ImportProjectInput;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.RemoveGoalFromGoalContainerInput;
import com.rreganjr.requel.service.query.ProjectQueryController;

import jakarta.annotation.PostConstruct;

/**
 * Registers all project domain command types with the CQRS command registry at startup.
 * Input applicators (DTO → command setter mapping) are added in Phase 1/2 as Angular
 * pages are built. Until then, commands are registered with no input mapping.
 */
@Component
public class ProjectCommandRegistrar {

    private static final Logger log = LoggerFactory.getLogger(ProjectCommandRegistrar.class);

    private final ProjectCommandFactory factory;
    private final ProjectRepository projectRepository;
    private final CommandRegistry registry;

    public ProjectCommandRegistrar(ProjectCommandFactory factory,
                                   ProjectRepository projectRepository,
                                   CommandRegistry registry) {
        this.factory = factory;
        this.projectRepository = projectRepository;
        this.registry = registry;
    }

    @PostConstruct
    void registerCommands() {
        // Project
        registry.register("EditProject", EditProjectInput.class,
                factory::newEditProjectCommand,
                (cmd, input) -> {
                    EditProjectCommand c = (EditProjectCommand) cmd;
                    EditProjectInput i = (EditProjectInput) input;

                    // If projectName matches an existing project, set it for update; otherwise leave null for create
                    if (i.projectName() != null) {
                        try {
                            c.setProject(projectRepository.findProjectByName(i.projectName()));
                        } catch (NoSuchProjectException e) {
                            // New project — leave project null, command will create
                        }
                    }

                    if (i.name() != null) c.setName(i.name());
                    if (i.description() != null) c.setText(i.description());
                    if (i.organizationId() != null) c.setOrganizationId(i.organizationId());
                    if (i.organizationName() != null) c.setOrganizationName(i.organizationName());
                },
                null, // no file
                cmd -> toDto(((EditProjectCommand) cmd).getProject()));
        registry.register("ExportProject", factory::newExportProjectCommand);
        registry.register("ImportProject", ImportProjectInput.class,
                factory::newImportProjectCommand,
                (cmd, input) -> {
                    ImportProjectCommand c = (ImportProjectCommand) cmd;
                    ImportProjectInput i = (ImportProjectInput) input;
                    if (i.name() != null && !i.name().isBlank()) c.setName(i.name());
                    if (Boolean.TRUE.equals(i.enableAnalysis())) c.setAnalysisEnabled(true);
                },
                (cmd, file) -> {
                    ImportProjectCommand c = (ImportProjectCommand) cmd;
                    MultipartFile mf = (MultipartFile) file;
                    try {
                        c.setInputStream(mf.getInputStream());
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to read uploaded file", e);
                    }
                },
                cmd -> toDto(((ImportProjectCommand) cmd).getProject()));

        // Stakeholders
        registry.register("EditUserStakeholder", EditUserStakeholderInput.class,
                factory::newEditUserStakeholderCommand,
                (cmd, input) -> {
                    EditUserStakeholderCommand c = (EditUserStakeholderCommand) cmd;
                    EditUserStakeholderInput i = (EditUserStakeholderInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setProjectOrDomain(project);
                    c.setUsername(i.username());
                    if (i.teamName() != null) c.setTeamName(i.teamName());
                    if (i.permissionKeys() != null) c.setStakeholderPermissions(new HashSet<>(i.permissionKeys()));
                    // For edit: find existing stakeholder by project + username
                    if (i.version() != null) {
                        UserStakeholder existing = findUserStakeholderByUsername(project, i.username());
                        if (existing != null) c.setStakeholder(existing);
                    }
                },
                null, // no file
                cmd -> ProjectQueryController.toStakeholderDto(
                        ((EditUserStakeholderCommand) cmd).getStakeholder()));

        registry.register("EditNonUserStakeholder", EditNonUserStakeholderInput.class,
                factory::newEditNonUserStakeholderCommand,
                (cmd, input) -> {
                    EditNonUserStakeholderCommand c = (EditNonUserStakeholderCommand) cmd;
                    EditNonUserStakeholderInput i = (EditNonUserStakeholderInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setProjectOrDomain(project);
                    c.setName(i.name());
                    if (i.text() != null) c.setText(i.text());
                    // For edit: find existing non-user stakeholder by project + name
                    if (i.version() != null) {
                        NonUserStakeholder existing = findNonUserStakeholderByName(project, i.name());
                        if (existing != null) c.setStakeholder(existing);
                    }
                },
                null, // no file
                cmd -> ProjectQueryController.toStakeholderDto(
                        ((EditNonUserStakeholderCommand) cmd).getStakeholder()));

        registry.register("DeleteStakeholder", DeleteStakeholderInput.class,
                factory::newDeleteStakeholderCommand,
                (cmd, input) -> {
                    DeleteStakeholderCommand c = (DeleteStakeholderCommand) cmd;
                    DeleteStakeholderInput i = (DeleteStakeholderInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    Stakeholder stakeholder = findStakeholderById(project, i.stakeholderId());
                    c.setStakeholder(stakeholder);
                });

        // Goals
        registry.register("EditGoal", EditGoalInput.class,
                factory::newEditGoalCommand,
                (cmd, input) -> {
                    EditGoalCommand c = (EditGoalCommand) cmd;
                    EditGoalInput i = (EditGoalInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    if (i.version() != null) {
                        c.setGoal(findGoalByName(project, i.name()));
                    } else {
                        c.setGoalContainer(project);
                    }
                    c.setName(i.name());
                    if (i.text() != null) c.setText(i.text());
                },
                null,
                cmd -> ProjectQueryController.toGoalDetailDto(((EditGoalCommand) cmd).getGoal()));

        registry.register("EditGoalRelation", EditGoalRelationInput.class,
                factory::newEditGoalRelationCommand,
                (cmd, input) -> {
                    EditGoalRelationCommand c = (EditGoalRelationCommand) cmd;
                    EditGoalRelationInput i = (EditGoalRelationInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setProjectOrDomain(project);
                    c.setFromGoal(i.fromGoalName());
                    c.setToGoal(i.toGoalName());
                    c.setRelationType(i.relationType());
                    if (i.version() != null) {
                        // Find existing relation for edit
                        GoalRelation existing = findGoalRelationById(project, i.fromGoalName(), i.toGoalName());
                        if (existing != null) c.setGoalRelation(existing);
                    }
                });

        registry.register("DeleteGoalRelation", DeleteGoalRelationInput.class,
                factory::newDeleteGoalRelationCommand,
                (cmd, input) -> {
                    DeleteGoalRelationCommand c = (DeleteGoalRelationCommand) cmd;
                    DeleteGoalRelationInput i = (DeleteGoalRelationInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setGoalRelation(findGoalRelationByIdFromProject(project, i.goalRelationId()));
                });

        registry.register("CopyGoal", CopyGoalInput.class,
                factory::newCopyGoalCommand,
                (cmd, input) -> {
                    CopyGoalCommand c = (CopyGoalCommand) cmd;
                    CopyGoalInput i = (CopyGoalInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setOriginalGoal(findGoalById(project, i.goalId()));
                    if (i.newGoalName() != null) c.setNewGoalName(i.newGoalName());
                },
                null,
                cmd -> ProjectQueryController.toGoalDetailDto(((CopyGoalCommand) cmd).getNewGoal()));

        registry.register("DeleteGoal", DeleteGoalInput.class,
                factory::newDeleteGoalCommand,
                (cmd, input) -> {
                    DeleteGoalCommand c = (DeleteGoalCommand) cmd;
                    DeleteGoalInput i = (DeleteGoalInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setGoal(findGoalById(project, i.goalId()));
                });

        registry.register("AddGoalToGoalContainer", AddGoalToGoalContainerInput.class,
                factory::newAddGoalToGoalContainerCommand,
                (cmd, input) -> {
                    AddGoalToGoalContainerCommand c = (AddGoalToGoalContainerCommand) cmd;
                    AddGoalToGoalContainerInput i = (AddGoalToGoalContainerInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    GoalContainer container = findGoalContainerById(project, i.goalContainerId());
                    c.setGoalContainer(container);
                    c.setGoal(findGoalById(project, i.goalId()));
                });

        registry.register("RemoveGoalFromGoalContainer", RemoveGoalFromGoalContainerInput.class,
                factory::newRemoveGoalFromGoalContainerCommand,
                (cmd, input) -> {
                    RemoveGoalFromGoalContainerCommand c = (RemoveGoalFromGoalContainerCommand) cmd;
                    RemoveGoalFromGoalContainerInput i = (RemoveGoalFromGoalContainerInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    GoalContainer container = findGoalContainerById(project, i.goalContainerId());
                    c.setGoalContainer(container);
                    c.setGoal(findGoalById(project, i.goalId()));
                });

        // Stories
        registry.register("EditStory", EditStoryInput.class,
                factory::newEditStoryCommand,
                (cmd, input) -> {
                    EditStoryCommand c = (EditStoryCommand) cmd;
                    EditStoryInput i = (EditStoryInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    if (i.version() != null) {
                        c.setStory(findStoryByName(project, i.name()));
                    } else {
                        c.setStoryContainer(project);
                    }
                    c.setName(i.name());
                    if (i.text() != null) c.setText(i.text());
                    if (i.storyTypeName() != null) c.setStoryTypeName(i.storyTypeName());
                },
                null,
                cmd -> ProjectQueryController.toStoryDetailDto(((EditStoryCommand) cmd).getStory()));

        registry.register("CopyStory", CopyStoryInput.class,
                factory::newCopyStoryCommand,
                (cmd, input) -> {
                    CopyStoryCommand c = (CopyStoryCommand) cmd;
                    CopyStoryInput i = (CopyStoryInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setOriginalStory(findStoryById(project, i.storyId()));
                    if (i.newStoryName() != null) c.setNewStoryName(i.newStoryName());
                },
                null,
                cmd -> ProjectQueryController.toStoryDetailDto(((CopyStoryCommand) cmd).getNewStory()));

        registry.register("DeleteStory", DeleteStoryInput.class,
                factory::newDeleteStoryCommand,
                (cmd, input) -> {
                    DeleteStoryCommand c = (DeleteStoryCommand) cmd;
                    DeleteStoryInput i = (DeleteStoryInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setStory(findStoryById(project, i.storyId()));
                });

        registry.register("AddStoryToStoryContainer", factory::newAddStoryToStoryContainerCommand);
        registry.register("RemoveStoryFromStoryContainer", factory::newRemoveStoryFromStoryContainerCommand);

        // Actors
        registry.register("EditActor", factory::newEditActorCommand);
        registry.register("AddActorToActorContainer", factory::newAddActorToActorContainerCommand);
        registry.register("RemoveActorFromActorContainer", factory::newRemoveActorFromActorContainerCommand);
        registry.register("CopyActor", factory::newCopyActorCommand);
        registry.register("DeleteActor", factory::newDeleteActorCommand);

        // Use Cases & Scenarios
        registry.register("EditUseCase", factory::newEditUseCaseCommand);
        registry.register("EditScenario", factory::newEditScenarioCommand);
        registry.register("EditScenarioStep", factory::newEditScenarioStepCommand);
        registry.register("CopyUseCase", factory::newCopyUseCaseCommand);
        registry.register("CopyScenario", factory::newCopyScenarioCommand);
        registry.register("CopyScenarioStep", factory::newCopyScenarioStepCommand);
        registry.register("ConvertStepToScenario", factory::newConvertStepToScenarioCommand);
        registry.register("DeleteUseCase", factory::newDeleteUseCaseCommand);
        registry.register("DeleteScenario", factory::newDeleteScenarioCommand);
        registry.register("DeleteScenarioStep", factory::newDeleteScenarioStepCommand);

        // Glossary
        registry.register("EditGlossaryTerm", factory::newEditGlossaryTermCommand);
        registry.register("EditAddWordToGlossaryPosition", factory::newEditAddWordToGlossaryPositionCommand);
        registry.register("EditAddActorToProjectPosition", factory::newEditAddActorToProjectPositionCommand);
        registry.register("ReplaceGlossaryTerm", factory::newReplaceGlossaryTermCommand);
        registry.register("DeleteGlossaryTerm", factory::newDeleteGlossaryTermCommand);

        // Reports
        registry.register("EditReportGenerator", factory::newEditReportGeneratorCommand);
        registry.register("GenerateReport", factory::newGenerateReportCommand);
        registry.register("DeleteReportGenerator", factory::newDeleteReportGeneratorCommand);

        // NLP cleanup
        registry.register("RemoveUnneedLexicalIssues", factory::newRemoveUnneedLexicalIssuesCommand);

        log.info("Registered {} project command types", 37);
    }

    private static UserStakeholder findUserStakeholderByUsername(Project project, String username) {
        for (Stakeholder s : project.getStakeholders()) {
            if (s instanceof UserStakeholder us && us.getUser().getUsername().equals(username)) {
                return us;
            }
        }
        return null;
    }

    private static NonUserStakeholder findNonUserStakeholderByName(Project project, String name) {
        for (Stakeholder s : project.getStakeholders()) {
            if (s instanceof NonUserStakeholder nus && nus.getName().equals(name)) {
                return nus;
            }
        }
        return null;
    }

    private static Stakeholder findStakeholderById(Project project, Long stakeholderId) {
        for (Stakeholder s : project.getStakeholders()) {
            if (s.getId().equals(stakeholderId)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Stakeholder not found: " + stakeholderId);
    }

    private static Goal findGoalById(Project project, Long goalId) {
        for (Goal g : project.getGoals()) {
            if (g.getId().equals(goalId)) return g;
        }
        throw new IllegalArgumentException("Goal not found: " + goalId);
    }

    private static GoalContainer findGoalContainerById(Project project, Long containerId) {
        for (Stakeholder s : project.getStakeholders()) {
            if (s.getId().equals(containerId)) return (GoalContainer) s;
        }
        for (Story s : project.getStories()) {
            if (s.getId().equals(containerId)) return (GoalContainer) s;
        }
        // Actors and UseCases (Phase 5+) not yet handled
        throw new IllegalArgumentException("GoalContainer not found: " + containerId);
    }

    private static Goal findGoalByName(Project project, String name) {
        for (Goal g : project.getGoals()) {
            if (g.getName().equals(name)) return g;
        }
        throw new IllegalArgumentException("Goal not found: " + name);
    }

    private static GoalRelation findGoalRelationById(Project project, String fromGoalName, String toGoalName) {
        Goal fromGoal = findGoalByName(project, fromGoalName);
        for (GoalRelation r : fromGoal.getRelationsFromThisGoal()) {
            if (r.getToGoal().getName().equals(toGoalName)) return r;
        }
        return null;
    }

    private static GoalRelation findGoalRelationByIdFromProject(Project project, Long relationId) {
        for (Goal g : project.getGoals()) {
            for (GoalRelation r : g.getRelationsFromThisGoal()) {
                if (r.getId().equals(relationId)) return r;
            }
        }
        throw new IllegalArgumentException("GoalRelation not found: " + relationId);
    }

    private static Story findStoryById(Project project, Long storyId) {
        for (Story s : project.getStories()) {
            if (s.getId().equals(storyId)) return s;
        }
        throw new IllegalArgumentException("Story not found: " + storyId);
    }

    private static Story findStoryByName(Project project, String name) {
        for (Story s : project.getStories()) {
            if (s.getName().equals(name)) return s;
        }
        throw new IllegalArgumentException("Story not found: " + name);
    }

    private static ProjectDto toDto(Project project) {
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

package com.rreganjr.requel.service.command;

import java.io.IOException;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.DeleteStakeholderCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.api.dto.DeleteStakeholderInput;
import com.rreganjr.requel.service.api.dto.EditNonUserStakeholderInput;
import com.rreganjr.requel.service.api.dto.EditProjectInput;
import com.rreganjr.requel.service.api.dto.EditUserStakeholderInput;
import com.rreganjr.requel.service.api.dto.ImportProjectInput;
import com.rreganjr.requel.service.api.dto.ProjectDto;
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
        registry.register("EditGoal", factory::newEditGoalCommand);
        registry.register("EditGoalRelation", factory::newEditGoalRelationCommand);
        registry.register("AddGoalToGoalContainer", factory::newAddGoalToGoalContainerCommand);
        registry.register("RemoveGoalFromGoalContainer", factory::newRemoveGoalFromGoalContainerCommand);
        registry.register("CopyGoal", factory::newCopyGoalCommand);
        registry.register("DeleteGoal", factory::newDeleteGoalCommand);
        registry.register("DeleteGoalRelation", factory::newDeleteGoalRelationCommand);

        // Stories
        registry.register("EditStory", factory::newEditStoryCommand);
        registry.register("AddStoryToStoryContainer", factory::newAddStoryToStoryContainerCommand);
        registry.register("RemoveStoryFromStoryContainer", factory::newRemoveStoryFromStoryContainerCommand);
        registry.register("CopyStory", factory::newCopyStoryCommand);
        registry.register("DeleteStory", factory::newDeleteStoryCommand);

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

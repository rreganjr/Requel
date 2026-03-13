package com.rreganjr.requel.service.command;

import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.api.dto.EditProjectInput;
import com.rreganjr.requel.service.api.dto.ImportProjectInput;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
        registry.register("EditUserStakeholder", factory::newEditUserStakeholderCommand);
        registry.register("EditNonUserStakeholder", factory::newEditNonUserStakeholderCommand);
        registry.register("DeleteStakeholder", factory::newDeleteStakeholderCommand);

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

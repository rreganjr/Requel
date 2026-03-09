package com.rreganjr.requel.service.command;

import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.service.api.CommandRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registers all project domain command types with the CQRS command registry at startup.
 * Input applicators (DTO → command setter mapping) are added in Phase 1/2 as Angular
 * pages are built. Until then, commands are registered with no input mapping.
 */
@Component
public class ProjectCommandRegistrar {

    private static final Logger log = LoggerFactory.getLogger(ProjectCommandRegistrar.class);

    private final ProjectCommandFactory factory;
    private final CommandRegistry registry;

    public ProjectCommandRegistrar(ProjectCommandFactory factory, CommandRegistry registry) {
        this.factory = factory;
        this.registry = registry;
    }

    @PostConstruct
    void registerCommands() {
        // Project
        registry.register("EditProject", factory::newEditProjectCommand);
        registry.register("ExportProject", factory::newExportProjectCommand);
        registry.register("ImportProject", factory::newImportProjectCommand);

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
}

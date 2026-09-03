/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
 *
 * Requel is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Requel is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Requel. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.rreganjr.requel.service.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ActorContainer;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.GoalContainer;
import com.rreganjr.requel.project.GoalRelation;
import com.rreganjr.requel.project.NonUserStakeholder;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.StoryContainer;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.project.UserStakeholder;
import com.rreganjr.requel.project.command.AddActorToActorContainerCommand;
import com.rreganjr.requel.project.command.AddGoalToGoalContainerCommand;
import com.rreganjr.requel.project.command.AddScenarioToUseCaseCommand;
import com.rreganjr.requel.project.command.AddStoryToStoryContainerCommand;
import com.rreganjr.requel.project.command.CopyActorCommand;
import com.rreganjr.requel.project.command.CopyGoalCommand;
import com.rreganjr.requel.project.command.CopyScenarioCommand;
import com.rreganjr.requel.project.command.CopyStoryCommand;
import com.rreganjr.requel.project.command.CopyUseCaseCommand;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.command.DeleteActorCommand;
import com.rreganjr.requel.project.command.DeleteReportGeneratorCommand;
import com.rreganjr.requel.project.command.EditReportGeneratorCommand;
import com.rreganjr.requel.project.command.DeleteGlossaryTermCommand;
import com.rreganjr.requel.project.command.EditGlossaryTermCommand;
import com.rreganjr.requel.project.command.DeleteProjectCommand;
import com.rreganjr.requel.project.command.DeleteGoalCommand;
import com.rreganjr.requel.project.command.DeleteGoalRelationCommand;
import com.rreganjr.requel.project.command.DeleteScenarioCommand;
import com.rreganjr.requel.project.command.DeleteStakeholderCommand;
import com.rreganjr.requel.project.command.DeleteStoryCommand;
import com.rreganjr.requel.project.command.DeleteUseCaseCommand;
import com.rreganjr.requel.project.command.EditActorCommand;
import com.rreganjr.requel.project.command.EditGoalCommand;
import com.rreganjr.requel.project.command.EditGoalRelationCommand;
import com.rreganjr.requel.project.command.EditNonUserStakeholderCommand;
import com.rreganjr.requel.project.command.EditProjectCommand;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.EditStoryCommand;
import com.rreganjr.requel.project.command.EditUseCaseCommand;
import com.rreganjr.requel.project.command.EditUserStakeholderCommand;
import com.rreganjr.requel.project.command.ImportProjectCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveActorFromActorContainerCommand;
import com.rreganjr.requel.project.command.RemoveGoalFromGoalContainerCommand;
import com.rreganjr.requel.project.command.RemoveScenarioFromUseCaseCommand;
import com.rreganjr.requel.project.command.SetPrimaryScenarioOnUseCaseCommand;
import com.rreganjr.requel.project.command.RemoveStoryFromStoryContainerCommand;
import com.rreganjr.requel.project.exception.NoSuchProjectException;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.api.dto.ActorDto;
import com.rreganjr.requel.service.api.dto.AddActorToActorContainerInput;
import com.rreganjr.requel.service.api.dto.AddGoalToGoalContainerInput;
import com.rreganjr.requel.service.api.dto.AddScenarioToUseCaseInput;
import com.rreganjr.requel.service.api.dto.AddStoryToStoryContainerInput;
import com.rreganjr.requel.service.api.dto.CopyActorInput;
import com.rreganjr.requel.service.api.dto.CopyGoalInput;
import com.rreganjr.requel.service.api.dto.CopyScenarioInput;
import com.rreganjr.requel.service.api.dto.CopyStoryInput;
import com.rreganjr.requel.service.api.dto.CopyUseCaseInput;
import com.rreganjr.requel.service.api.dto.DeleteActorInput;
import com.rreganjr.requel.service.api.dto.DeleteScenarioInput;
import com.rreganjr.requel.service.api.dto.DeleteProjectInput;
import com.rreganjr.requel.service.api.dto.DeleteGoalInput;
import com.rreganjr.requel.service.api.dto.DeleteGoalRelationInput;
import com.rreganjr.requel.service.api.dto.DeleteStakeholderInput;
import com.rreganjr.requel.service.api.dto.DeleteStoryInput;
import com.rreganjr.requel.service.api.dto.DeleteUseCaseInput;
import com.rreganjr.requel.service.api.dto.EditActorInput;
import com.rreganjr.requel.service.api.dto.EditScenarioInput;
import com.rreganjr.requel.service.api.dto.EditStepInput;
import com.rreganjr.requel.service.api.dto.EditGoalInput;
import com.rreganjr.requel.service.api.dto.EditGoalRelationInput;
import com.rreganjr.requel.service.api.dto.EditNonUserStakeholderInput;
import com.rreganjr.requel.service.api.dto.EditProjectInput;
import com.rreganjr.requel.service.api.dto.EditStoryInput;
import com.rreganjr.requel.service.api.dto.EditUseCaseInput;
import com.rreganjr.requel.service.api.dto.EditUserStakeholderInput;
import com.rreganjr.requel.service.api.dto.ImportProjectInput;
import com.rreganjr.requel.service.api.dto.ProjectDto;
import com.rreganjr.requel.service.api.dto.RemoveActorFromActorContainerInput;
import com.rreganjr.requel.service.api.dto.RemoveGoalFromGoalContainerInput;
import com.rreganjr.requel.service.api.dto.RemoveScenarioFromUseCaseInput;
import com.rreganjr.requel.service.api.dto.SetPrimaryScenarioInput;
import com.rreganjr.requel.service.api.dto.RemoveStoryFromStoryContainerInput;
import com.rreganjr.requel.service.api.dto.DeleteGlossaryTermInput;
import com.rreganjr.requel.service.api.dto.DeleteReportGeneratorInput;
import com.rreganjr.requel.service.api.dto.EditGlossaryTermInput;
import com.rreganjr.requel.service.api.dto.EditReportGeneratorInput;
import com.rreganjr.requel.service.api.dto.GlossaryTermDto;
import com.rreganjr.requel.service.api.dto.ReportGeneratorDto;
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
                    // Caller's version drives the optimistic-lock check on update (issue #108).
                    c.setExpectedVersion(i.version());

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

        registry.register("DeleteProject", DeleteProjectInput.class,
                factory::newDeleteProjectCommand,
                (cmd, input) -> {
                    DeleteProjectCommand c = (DeleteProjectCommand) cmd;
                    DeleteProjectInput i = (DeleteProjectInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setProject(project);
                    // Caller's version drives the optimistic-lock check (issue #108).
                    c.setExpectedVersion(i.version());
                });

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
                    // For edit (caller supplies a version): resolve the existing stakeholder by its
                    // natural key (project + username) and apply the optimistic-lock check (issue
                    // #108). Without a version this is treated as a create (unchanged behavior).
                    if (i.version() != null) {
                        UserStakeholder existing = findUserStakeholderByUsername(project, i.username());
                        if (existing != null) c.setStakeholder(existing);
                        c.setExpectedVersion(i.version());
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
                    // For edit: find existing non-user stakeholder by ID
                    if (i.stakeholderId() != null) {
                        NonUserStakeholder existing = (NonUserStakeholder) findStakeholderById(project, i.stakeholderId());
                        c.setStakeholder(existing);
                        c.setExpectedVersion(i.version());
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
                    if (i.goalId() != null) {
                        c.setGoal(findGoalById(project, i.goalId()));
                        // Wire the caller-supplied optimistic-lock version (issue #108).
                        c.setExpectedVersion(i.version());
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
                    // For edit (caller supplies a version): resolve the existing relation by its
                    // natural key (from/to goal) and apply the optimistic-lock check (issue #108).
                    // Without a version this is treated as a create (unchanged behavior).
                    if (i.version() != null) {
                        GoalRelation existing = findGoalRelationById(project, i.fromGoalName(), i.toGoalName());
                        if (existing != null) c.setGoalRelation(existing);
                        c.setExpectedVersion(i.version());
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
                    GoalContainer container = findGoalContainerById(project, i.goalContainerId(), i.containerType());
                    c.setGoalContainer(container);
                    c.setGoal(findGoalById(project, i.goalId()));
                },
                null,
                cmd -> ProjectQueryController.toContainerDetailDto(
                        ((AddGoalToGoalContainerCommand) cmd).getGoalContainer()),
                cmd -> ProjectQueryController.toGoalDetailDto(
                        ((AddGoalToGoalContainerCommand) cmd).getGoal()),
                null);

        registry.register("RemoveGoalFromGoalContainer", RemoveGoalFromGoalContainerInput.class,
                factory::newRemoveGoalFromGoalContainerCommand,
                (cmd, input) -> {
                    RemoveGoalFromGoalContainerCommand c = (RemoveGoalFromGoalContainerCommand) cmd;
                    RemoveGoalFromGoalContainerInput i = (RemoveGoalFromGoalContainerInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    GoalContainer container = findGoalContainerById(project, i.goalContainerId(), i.containerType());
                    c.setGoalContainer(container);
                    c.setGoal(findGoalById(project, i.goalId()));
                },
                null,
                cmd -> ProjectQueryController.toContainerDetailDto(
                        ((RemoveGoalFromGoalContainerCommand) cmd).getGoalContainer()),
                cmd -> ProjectQueryController.toGoalDetailDto(
                        ((RemoveGoalFromGoalContainerCommand) cmd).getGoal()),
                null);

        // Stories
        registry.register("EditStory", EditStoryInput.class,
                factory::newEditStoryCommand,
                (cmd, input) -> {
                    EditStoryCommand c = (EditStoryCommand) cmd;
                    EditStoryInput i = (EditStoryInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    if (i.storyId() != null) {
                        c.setStory(findStoryById(project, i.storyId()));
                        c.setExpectedVersion(i.version());
                    } else {
                        c.setStoryContainer(project);
                    }
                    c.setName(i.name());
                    if (i.text() != null) c.setText(i.text());
                    if (i.storyTypeName() != null) c.setStoryTypeName(i.storyTypeName());
                    c.setPrimaryActorName(i.primaryActorName());
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

        registry.register("AddStoryToStoryContainer", AddStoryToStoryContainerInput.class,
                factory::newAddStoryToStoryContainerCommand,
                (cmd, input) -> {
                    AddStoryToStoryContainerCommand c = (AddStoryToStoryContainerCommand) cmd;
                    AddStoryToStoryContainerInput i = (AddStoryToStoryContainerInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setStoryContainer(findStoryContainerById(project, i.storyContainerId(), i.containerType()));
                    c.setStory(findStoryById(project, i.storyId()));
                },
                null,
                cmd -> ProjectQueryController.toContainerDetailDto(
                        ((AddStoryToStoryContainerCommand) cmd).getStoryContainer()),
                cmd -> ProjectQueryController.toStoryDetailDto(
                        ((AddStoryToStoryContainerCommand) cmd).getStory()),
                null);

        registry.register("RemoveStoryFromStoryContainer", RemoveStoryFromStoryContainerInput.class,
                factory::newRemoveStoryFromStoryContainerCommand,
                (cmd, input) -> {
                    RemoveStoryFromStoryContainerCommand c = (RemoveStoryFromStoryContainerCommand) cmd;
                    RemoveStoryFromStoryContainerInput i = (RemoveStoryFromStoryContainerInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setStoryContainer(findStoryContainerById(project, i.storyContainerId(), i.containerType()));
                    c.setStory(findStoryById(project, i.storyId()));
                },
                null,
                cmd -> ProjectQueryController.toContainerDetailDto(
                        ((RemoveStoryFromStoryContainerCommand) cmd).getStoryContainer()),
                cmd -> ProjectQueryController.toStoryDetailDto(
                        ((RemoveStoryFromStoryContainerCommand) cmd).getStory()),
                null);

        // Actors
        registry.register("EditActor", EditActorInput.class,
                factory::newEditActorCommand,
                (cmd, input) -> {
                    EditActorCommand c = (EditActorCommand) cmd;
                    EditActorInput i = (EditActorInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    if (i.actorId() != null) {
                        c.setActor(findActorById(project, i.actorId()));
                        c.setExpectedVersion(i.version());
                    } else {
                        c.setActorContainer(project);
                    }
                    c.setName(i.name());
                    if (i.description() != null) c.setText(i.description());
                    c.setProjectOrDomain(project);
                },
                null,
                cmd -> ProjectQueryController.toActorDetailDto(((EditActorCommand) cmd).getActor()));

        registry.register("AddActorToActorContainer", AddActorToActorContainerInput.class,
                factory::newAddActorToActorContainerCommand,
                (cmd, input) -> {
                    AddActorToActorContainerCommand c = (AddActorToActorContainerCommand) cmd;
                    AddActorToActorContainerInput i = (AddActorToActorContainerInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setActorContainer(findActorContainerById(project, i.actorContainerId(), i.containerType()));
                    c.setActor(findActorById(project, i.actorId()));
                },
                null,
                cmd -> ProjectQueryController.toContainerDetailDto(
                        ((AddActorToActorContainerCommand) cmd).getActorContainer()),
                cmd -> ProjectQueryController.toActorDetailDto(
                        ((AddActorToActorContainerCommand) cmd).getActor()),
                null);

        registry.register("RemoveActorFromActorContainer", RemoveActorFromActorContainerInput.class,
                factory::newRemoveActorFromActorContainerCommand,
                (cmd, input) -> {
                    RemoveActorFromActorContainerCommand c = (RemoveActorFromActorContainerCommand) cmd;
                    RemoveActorFromActorContainerInput i = (RemoveActorFromActorContainerInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setActorContainer(findActorContainerById(project, i.actorContainerId(), i.containerType()));
                    c.setActor(findActorById(project, i.actorId()));
                },
                null,
                cmd -> ProjectQueryController.toContainerDetailDto(
                        ((RemoveActorFromActorContainerCommand) cmd).getActorContainer()),
                cmd -> ProjectQueryController.toActorDetailDto(
                        ((RemoveActorFromActorContainerCommand) cmd).getActor()),
                null);
        registry.register("CopyActor", CopyActorInput.class,
                factory::newCopyActorCommand,
                (cmd, input) -> {
                    CopyActorCommand c = (CopyActorCommand) cmd;
                    CopyActorInput i = (CopyActorInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setOriginalActor(findActorById(project, i.actorId()));
                    if (i.newActorName() != null) c.setNewActorName(i.newActorName());
                },
                null,
                cmd -> ProjectQueryController.toActorDetailDto(((CopyActorCommand) cmd).getNewActor()));

        registry.register("DeleteActor", DeleteActorInput.class,
                factory::newDeleteActorCommand,
                (cmd, input) -> {
                    DeleteActorCommand c = (DeleteActorCommand) cmd;
                    DeleteActorInput i = (DeleteActorInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setActor(findActorById(project, i.actorId()));
                });

        // Use Cases & Scenarios
        registry.register("EditUseCase", EditUseCaseInput.class,
                factory::newEditUseCaseCommand,
                (cmd, input) -> {
                    EditUseCaseCommand c = (EditUseCaseCommand) cmd;
                    EditUseCaseInput i = (EditUseCaseInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setProjectOrDomain(project);
                    if (i.useCaseId() != null) {
                        c.setUseCase(findUseCaseById(project, i.useCaseId()));
                        c.setExpectedVersion(i.version());
                    }
                    c.setName(i.name());
                    if (i.text() != null) c.setText(i.text());
                    if (i.primaryActorName() != null) c.setPrimaryActorName(i.primaryActorName());
                    c.setStepCommands(new ArrayList<>());
                },
                null,
                cmd -> ProjectQueryController.toUseCaseDetailDto(((EditUseCaseCommand) cmd).getUseCase()));

        registry.register("AddScenarioToUseCase", AddScenarioToUseCaseInput.class,
                factory::newAddScenarioToUseCaseCommand,
                (cmd, input) -> {
                    AddScenarioToUseCaseCommand c = (AddScenarioToUseCaseCommand) cmd;
                    AddScenarioToUseCaseInput i = (AddScenarioToUseCaseInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setUseCase(findUseCaseById(project, i.useCaseId()));
                    c.setScenario(findScenarioById(project, i.scenarioId()));
                },
                null,
                cmd -> ProjectQueryController.toUseCaseDetailDto(((AddScenarioToUseCaseCommand) cmd).getUseCase()));

        registry.register("RemoveScenarioFromUseCase", RemoveScenarioFromUseCaseInput.class,
                factory::newRemoveScenarioFromUseCaseCommand,
                (cmd, input) -> {
                    RemoveScenarioFromUseCaseCommand c = (RemoveScenarioFromUseCaseCommand) cmd;
                    RemoveScenarioFromUseCaseInput i = (RemoveScenarioFromUseCaseInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setUseCase(findUseCaseById(project, i.useCaseId()));
                    c.setScenario(findScenarioById(project, i.scenarioId()));
                },
                null,
                cmd -> ProjectQueryController.toUseCaseDetailDto(((RemoveScenarioFromUseCaseCommand) cmd).getUseCase()));

        registry.register("SetPrimaryScenarioOnUseCase", SetPrimaryScenarioInput.class,
                factory::newSetPrimaryScenarioOnUseCaseCommand,
                (cmd, input) -> {
                    SetPrimaryScenarioOnUseCaseCommand c = (SetPrimaryScenarioOnUseCaseCommand) cmd;
                    SetPrimaryScenarioInput i = (SetPrimaryScenarioInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setUseCase(findUseCaseById(project, i.useCaseId()));
                    c.setScenario(findScenarioById(project, i.scenarioId()));
                },
                null,
                cmd -> ProjectQueryController.toUseCaseDetailDto(((SetPrimaryScenarioOnUseCaseCommand) cmd).getUseCase()));

        registry.register("EditScenario", EditScenarioInput.class,
                factory::newEditScenarioCommand,
                (cmd, input) -> {
                    EditScenarioCommand c = (EditScenarioCommand) cmd;
                    EditScenarioInput i = (EditScenarioInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setProjectOrDomain(project);
                    if (i.scenarioId() != null) {
                        c.setScenario(findScenarioById(project, i.scenarioId()));
                        c.setExpectedVersion(i.version());
                    }
                    if (i.name() != null) c.setName(i.name());
                    if (i.text() != null) c.setText(i.text());
                    if (i.scenarioTypeName() != null) c.setScenarioTypeName(i.scenarioTypeName());

                    List<EditScenarioStepCommand> stepCmds = new ArrayList<>();
                    if (i.steps() != null) {
                        for (EditStepInput si : i.steps()) {
                            if (si.isScenario() && si.stepId() == null) {
                                // New sub-scenario — create inline
                                EditScenarioCommand subCmd = (EditScenarioCommand) factory.newEditScenarioCommand();
                                subCmd.setProjectOrDomain(project);
                                subCmd.setName(si.name());
                                if (si.text() != null) subCmd.setText(si.text());
                                subCmd.setScenarioTypeName(si.scenarioTypeName() != null ? si.scenarioTypeName() : "Primary");
                                subCmd.setStepCommands(new ArrayList<>());
                                stepCmds.add(subCmd);
                            } else if (si.isScenario()) {
                                // Existing sub-scenario reference — pass through as a step update
                                // using EditScenarioStepCommand so we don't clear its own step list
                                EditScenarioStepCommand subCmd = factory.newEditScenarioStepCommand();
                                Scenario existingSub = findScenarioById(project, si.stepId());
                                subCmd.setProjectOrDomain(project);
                                subCmd.setStep(existingSub);
                                subCmd.setName(si.name() != null ? si.name() : existingSub.getName());
                                subCmd.setText(si.text() != null ? si.text() : existingSub.getText());
                                subCmd.setScenarioTypeName(si.scenarioTypeName() != null
                                        ? si.scenarioTypeName() : existingSub.getType().name());
                                stepCmds.add(subCmd);
                            } else if (si.stepId() == null) {
                                // New plain step
                                EditScenarioStepCommand stepCmd = factory.newEditScenarioStepCommand();
                                stepCmd.setProjectOrDomain(project);
                                stepCmd.setName(si.name());
                                if (si.text() != null) stepCmd.setText(si.text());
                                stepCmd.setScenarioTypeName(si.scenarioTypeName() != null ? si.scenarioTypeName() : "Primary");
                                stepCmds.add(stepCmd);
                            } else {
                                // Existing plain step
                                EditScenarioStepCommand stepCmd = factory.newEditScenarioStepCommand();
                                Step existingStep = findStepByIdAcrossScenarios(project, si.stepId());
                                stepCmd.setProjectOrDomain(project);
                                stepCmd.setStep(existingStep);
                                stepCmd.setName(si.name() != null ? si.name() : existingStep.getName());
                                stepCmd.setText(si.text() != null ? si.text() : existingStep.getText());
                                stepCmd.setScenarioTypeName(si.scenarioTypeName() != null
                                        ? si.scenarioTypeName() : existingStep.getType().name());
                                stepCmds.add(stepCmd);
                            }
                        }
                    }
                    c.setStepCommands(stepCmds);
                },
                null,
                cmd -> ProjectQueryController.toScenarioDetailDto(((EditScenarioCommand) cmd).getScenario()));

        registry.register("CopyScenario", CopyScenarioInput.class,
                factory::newCopyScenarioCommand,
                (cmd, input) -> {
                    CopyScenarioCommand c = (CopyScenarioCommand) cmd;
                    CopyScenarioInput i = (CopyScenarioInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setOriginalScenario(findScenarioById(project, i.scenarioId()));
                },
                null,
                cmd -> ProjectQueryController.toScenarioDetailDto(((CopyScenarioCommand) cmd).getNewScenario()));

        registry.register("DeleteScenario", DeleteScenarioInput.class,
                factory::newDeleteScenarioCommand,
                (cmd, input) -> {
                    DeleteScenarioCommand c = (DeleteScenarioCommand) cmd;
                    DeleteScenarioInput i = (DeleteScenarioInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setScenario(findScenarioById(project, i.scenarioId()));
                });

        registry.register("EditScenarioStep", factory::newEditScenarioStepCommand);
        registry.register("CopyUseCase", CopyUseCaseInput.class,
                factory::newCopyUseCaseCommand,
                (cmd, input) -> {
                    CopyUseCaseCommand c = (CopyUseCaseCommand) cmd;
                    CopyUseCaseInput i = (CopyUseCaseInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setOriginalUseCase(findUseCaseById(project, i.useCaseId()));
                },
                null,
                cmd -> ProjectQueryController.toUseCaseDetailDto(((CopyUseCaseCommand) cmd).getNewUseCase()));
        registry.register("CopyScenarioStep", factory::newCopyScenarioStepCommand);
        registry.register("ConvertStepToScenario", factory::newConvertStepToScenarioCommand);
        registry.register("DeleteUseCase", DeleteUseCaseInput.class,
                factory::newDeleteUseCaseCommand,
                (cmd, input) -> {
                    DeleteUseCaseCommand c = (DeleteUseCaseCommand) cmd;
                    DeleteUseCaseInput i = (DeleteUseCaseInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setUseCase(findUseCaseById(project, i.useCaseId()));
                });
        registry.register("DeleteScenarioStep", factory::newDeleteScenarioStepCommand);

        // Glossary
        registry.register("EditGlossaryTerm", EditGlossaryTermInput.class,
                factory::newEditGlossaryTermCommand,
                (cmd, input) -> {
                    EditGlossaryTermCommand c = (EditGlossaryTermCommand) cmd;
                    EditGlossaryTermInput i = (EditGlossaryTermInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setProjectOrDomain(project);
                    if (i.termId() != null) {
                        c.setGlossaryTerm(findTermById(project, i.termId()));
                    }
                    c.setName(i.name());
                    if (i.text() != null) c.setText(i.text());
                    if (i.canonicalTermId() != null) {
                        c.setCanonicalTerm(findTermById(project, i.canonicalTermId()));
                    }
                },
                null,
                cmd -> ProjectQueryController.toGlossaryTermDetailDto(((EditGlossaryTermCommand) cmd).getGlossaryTerm()));

        registry.register("DeleteGlossaryTerm", DeleteGlossaryTermInput.class,
                factory::newDeleteGlossaryTermCommand,
                (cmd, input) -> {
                    DeleteGlossaryTermCommand c = (DeleteGlossaryTermCommand) cmd;
                    DeleteGlossaryTermInput i = (DeleteGlossaryTermInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setGlossaryTerm(findTermById(project, i.termId()));
                });

        registry.register("EditAddWordToGlossaryPosition", factory::newEditAddWordToGlossaryPositionCommand);
        registry.register("EditAddActorToProjectPosition", factory::newEditAddActorToProjectPositionCommand);
        registry.register("ReplaceGlossaryTerm", factory::newReplaceGlossaryTermCommand);

        // Reports
        registry.register("EditReportGenerator", EditReportGeneratorInput.class,
                factory::newEditReportGeneratorCommand,
                (cmd, input) -> {
                    EditReportGeneratorCommand c = (EditReportGeneratorCommand) cmd;
                    EditReportGeneratorInput i = (EditReportGeneratorInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setProjectOrDomain(project);
                    if (i.reportId() != null) {
                        c.setReportGenerator(findReportGeneratorById(project, i.reportId()));
                    }
                    c.setName(i.name());
                    if (i.text() != null) c.setText(i.text());
                },
                null,
                cmd -> ProjectQueryController.toReportGeneratorDetailDto(((EditReportGeneratorCommand) cmd).getReportGenerator()));
        registry.register("GenerateReport", factory::newGenerateReportCommand);
        registry.register("DeleteReportGenerator", DeleteReportGeneratorInput.class,
                factory::newDeleteReportGeneratorCommand,
                (cmd, input) -> {
                    DeleteReportGeneratorCommand c = (DeleteReportGeneratorCommand) cmd;
                    DeleteReportGeneratorInput i = (DeleteReportGeneratorInput) input;
                    Project project = projectRepository.findProjectByName(i.projectName());
                    c.setReportGenerator(findReportGeneratorById(project, i.reportId()));
                });

        // NLP cleanup
        registry.register("RemoveUnneedLexicalIssues", factory::newRemoveUnneedLexicalIssuesCommand);

        log.info("Registered {} project command types", 40);
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

    private static GoalContainer findGoalContainerById(Project project, Long containerId, String containerType) {
        // Resolve strictly within the named type to avoid cross-type ID collisions
        // (all tables use per-table auto-increment). issue #189
        if ("Project".equalsIgnoreCase(containerType)) {
            if (project.getId().equals(containerId)) return project;
            throw EntityValidationException.validationFailed(GoalContainer.class, "goalContainerId",
                    "id " + containerId + " does not identify the project");
        }
        if ("UseCase".equalsIgnoreCase(containerType)) {
            for (UseCase uc : project.getUseCases()) {
                if (uc.getId().equals(containerId)) return (GoalContainer) uc;
            }
            throw EntityValidationException.validationFailed(GoalContainer.class, "goalContainerId",
                    "id " + containerId + " does not identify a use case goal container");
        }
        if ("Story".equalsIgnoreCase(containerType)) {
            for (Story s : project.getStories()) {
                if (s.getId().equals(containerId)) return (GoalContainer) s;
            }
            throw EntityValidationException.validationFailed(GoalContainer.class, "goalContainerId",
                    "id " + containerId + " does not identify a story goal container");
        }
        if ("Actor".equalsIgnoreCase(containerType)) {
            for (Actor a : project.getActors()) {
                if (a.getId().equals(containerId)) return (GoalContainer) a;
            }
            throw EntityValidationException.validationFailed(GoalContainer.class, "goalContainerId",
                    "id " + containerId + " does not identify an actor goal container");
        }
        if ("Stakeholder".equalsIgnoreCase(containerType) || "UserStakeholder".equalsIgnoreCase(containerType)
                || "NonUserStakeholder".equalsIgnoreCase(containerType)) {
            for (Stakeholder s : project.getStakeholders()) {
                if (s.getId().equals(containerId)) return (GoalContainer) s;
            }
            throw EntityValidationException.validationFailed(GoalContainer.class, "goalContainerId",
                    "id " + containerId + " does not identify a stakeholder goal container");
        }
        throw EntityValidationException.validationFailed(GoalContainer.class, "containerType",
                "'" + containerType + "' is not a valid goal container type "
                        + "(expected Project, UseCase, Story, Actor, or Stakeholder)");
    }

    private static StoryContainer findStoryContainerById(Project project, Long containerId, String containerType) {
        // Resolve strictly within the named type. Only Project and UseCase are StoryContainers. issue #189
        if ("Project".equalsIgnoreCase(containerType)) {
            if (project.getId().equals(containerId)) return project;
            throw EntityValidationException.validationFailed(StoryContainer.class, "storyContainerId",
                    "id " + containerId + " does not identify the project");
        }
        if ("UseCase".equalsIgnoreCase(containerType)) {
            for (UseCase uc : project.getUseCases()) {
                if (uc.getId().equals(containerId)) return uc;
            }
            throw EntityValidationException.validationFailed(StoryContainer.class, "storyContainerId",
                    "id " + containerId + " does not identify a use case story container");
        }
        throw EntityValidationException.validationFailed(StoryContainer.class, "containerType",
                "'" + containerType + "' is not a valid story container type (expected Project or UseCase)");
    }

    private static ActorContainer findActorContainerById(Project project, Long containerId, String containerType) {
        // Resolve strictly within the named type. Project, UseCase, and Story are ActorContainers. issue #189
        if ("Project".equalsIgnoreCase(containerType)) {
            if (project.getId().equals(containerId)) return project;
            throw EntityValidationException.validationFailed(ActorContainer.class, "actorContainerId",
                    "id " + containerId + " does not identify the project");
        }
        if ("UseCase".equalsIgnoreCase(containerType)) {
            for (UseCase uc : project.getUseCases()) {
                if (uc.getId().equals(containerId)) return uc;
            }
            throw EntityValidationException.validationFailed(ActorContainer.class, "actorContainerId",
                    "id " + containerId + " does not identify a use case actor container");
        }
        if ("Story".equalsIgnoreCase(containerType)) {
            for (Story s : project.getStories()) {
                if (s.getId().equals(containerId)) return (ActorContainer) s;
            }
            throw EntityValidationException.validationFailed(ActorContainer.class, "actorContainerId",
                    "id " + containerId + " does not identify a story actor container");
        }
        throw EntityValidationException.validationFailed(ActorContainer.class, "containerType",
                "'" + containerType + "' is not a valid actor container type (expected Project, UseCase, or Story)");
    }

    private static UseCase findUseCaseById(Project project, Long useCaseId) {
        for (UseCase uc : project.getUseCases()) {
            if (uc.getId().equals(useCaseId)) return uc;
        }
        throw new IllegalArgumentException("UseCase not found: " + useCaseId);
    }

    private static Actor findActorById(Project project, Long actorId) {
        for (Actor a : project.getActors()) {
            if (a.getId().equals(actorId)) return a;
        }
        throw new IllegalArgumentException("Actor not found: " + actorId);
    }

    private static GlossaryTerm findTermById(Project project, Long termId) {
        for (GlossaryTerm t : project.getGlossaryTerms()) {
            if (t.getId().equals(termId)) return t;
        }
        throw new IllegalArgumentException("GlossaryTerm not found: " + termId);
    }

    private static ReportGenerator findReportGeneratorById(Project project, Long reportId) {
        for (ReportGenerator r : project.getReportGenerators()) {
            if (r.getId().equals(reportId)) return r;
        }
        throw new IllegalArgumentException("ReportGenerator not found: " + reportId);
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

    private static Scenario findScenarioById(Project project, Long scenarioId) {
        for (Scenario s : project.getScenarios()) {
            if (s.getId().equals(scenarioId)) return s;
        }
        throw new IllegalArgumentException("Scenario not found: " + scenarioId);
    }

    private static Step findStepByIdAcrossScenarios(Project project, Long stepId) {
        for (Scenario s : project.getScenarios()) {
            for (Step step : s.getSteps()) {
                if (step.getId().equals(stepId)) return step;
            }
        }
        throw new IllegalArgumentException("Step not found: " + stepId);
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
                project.getGlossaryTerms().size(),
                project.getReportGenerators().size()
        );
    }
}

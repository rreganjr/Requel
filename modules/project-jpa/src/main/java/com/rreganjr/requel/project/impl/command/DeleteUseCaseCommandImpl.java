/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2008, 2009, 2025 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.DeleteUseCaseCommand;
import com.rreganjr.requel.project.command.DeleteScenarioCommand;
import com.rreganjr.requel.project.command.DeleteScenarioStepCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.command.RemoveActorFromActorContainerCommand;
import com.rreganjr.requel.project.command.RemoveGoalFromGoalContainerCommand;
import com.rreganjr.requel.project.command.RemoveStoryFromStoryContainerCommand;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Delete a usecase from a project, cleaning up references from other project
 * entities, usecase relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteUseCaseCommand")
@Scope("prototype")
public class DeleteUseCaseCommandImpl extends AbstractEditProjectCommand implements
		DeleteUseCaseCommand, ProjectScopedCommand, AuthorizableCommand {

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(com.rreganjr.requel.project.UseCase.class, "Delete");
	}

	private UseCase usecase;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteUseCaseCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setUseCase(UseCase usecase) {
		this.usecase = usecase;
	}

	protected UseCase getUseCase() {
		return usecase;
	}

	@Override
	public void execute() throws Exception {
		UseCase usecase = getRepository().get(getUseCase());
		User editedBy = getRepository().get(getEditedBy());
		Set<Annotation> annotations = new HashSet<Annotation>(usecase.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			removeAnnotationFromAnnotatableCommand.setEditedBy(editedBy);
			removeAnnotationFromAnnotatableCommand.setAnnotatable(usecase);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		// remove this entity as a referer to any terms
		for (GlossaryTerm term : usecase.getProjectOrDomain().getGlossaryTerms()) {
			if (term.getReferers().contains(usecase)) {
				term.getReferers().remove(usecase);
			}
		}
		Set<Actor> actors = new HashSet<Actor>(usecase.getActors());
		actors.add(usecase.getPrimaryActor());
		for (Actor actor : actors) {
			RemoveActorFromActorContainerCommand removeActorFromActorContainerCommand = getProjectCommandFactory()
					.newRemoveActorFromActorContainerCommand();
			removeActorFromActorContainerCommand.setActor(actor);
			removeActorFromActorContainerCommand.setActorContainer(usecase);
			removeActorFromActorContainerCommand.setEditedBy(getEditedBy());
			// TODO(#75): part of an authorized delete; exempt the detach sub-command from
			// re-auth (see https://github.com/rreganjr/Requel/issues/75)
			((com.rreganjr.platform.command.AuthorizationExemptable) removeActorFromActorContainerCommand).setAuthorizationExempt(true);
			getCommandHandler().execute(removeActorFromActorContainerCommand);
		}
		Set<Goal> goals = new HashSet<Goal>(usecase.getGoals());
		for (Goal goal : goals) {
			RemoveGoalFromGoalContainerCommand removeGoalFromGoalContainerCommand = getProjectCommandFactory()
					.newRemoveGoalFromGoalContainerCommand();
			removeGoalFromGoalContainerCommand.setGoal(goal);
			removeGoalFromGoalContainerCommand.setGoalContainer(usecase);
			removeGoalFromGoalContainerCommand.setEditedBy(getEditedBy());
			// TODO(#75): part of an authorized delete; exempt the detach sub-command from
			// re-auth (see https://github.com/rreganjr/Requel/issues/75)
			((com.rreganjr.platform.command.AuthorizationExemptable) removeGoalFromGoalContainerCommand).setAuthorizationExempt(true);
			getCommandHandler().execute(removeGoalFromGoalContainerCommand);
		}
		Set<Story> stories = new HashSet<Story>(usecase.getStories());
		for (Story story : stories) {
			RemoveStoryFromStoryContainerCommand removeStoryFromStoryContainerCommand = getProjectCommandFactory()
					.newRemoveStoryFromStoryContainerCommand();
			removeStoryFromStoryContainerCommand.setStory(story);
			removeStoryFromStoryContainerCommand.setStoryContainer(usecase);
			removeStoryFromStoryContainerCommand.setEditedBy(getEditedBy());
			// TODO(#75): part of an authorized delete; exempt the detach sub-command from
			// re-auth (see https://github.com/rreganjr/Requel/issues/75)
			((com.rreganjr.platform.command.AuthorizationExemptable) removeStoryFromStoryContainerCommand).setAuthorizationExempt(true);
			getCommandHandler().execute(removeStoryFromStoryContainerCommand);
		}
		// #247: capture the use-case's own scenarios (primary + additional) before deleting it.
		// They are NOT in project.getScenarios(), so the DeleteProject cascade never removes them;
		// left behind they (and their steps) orphan rows in the `scenarios` table that still
		// reference the project via projectordomain_id -> pods and block delete(project).
		Set<Scenario> ownedScenarios = new HashSet<Scenario>();
		if (usecase.getScenario() != null) {
			ownedScenarios.add(usecase.getScenario());
		}
		ownedScenarios.addAll(usecase.getAdditionalScenarios());

		for (Scenario scenario : getProjectRepository().findScenariosUsedByUseCase(usecase)) {
			// TODO: add command RemoveUsecaseFromScenario
			scenario.getUsingUseCases().remove(usecase);
		}
		// delete the use-case first: removing the use-case row also removes its own
		// use_cases.scenario_id FK value and its usecase_scenarios join rows, so the orphaned
		// scenarios below can then be deleted without FK conflicts.
		usecase.getProjectOrDomain().getUseCases().remove(usecase);
		// #247: clear any annotation link committed since this entity was loaded.
		removeAllAnnotationsBeforeDelete(usecase, editedBy);
		getRepository().delete(usecase);
		// #247: flush so the use-case row (and its usecase_scenarios / usecase_* join rows)
		// are gone before the owned scenarios' lazy getUsingUseCases() collections load
		// below - a collection initialization does not auto-flush, so without this the
		// just-removed use case would still be "using" its scenario and every owned scenario
		// would be skipped as shared, leaving exactly the orphans this ticket is about.
		getRepository().flush();

		// #247: now delete the orphaned scenarios that belonged only to this use-case, together
		// with their steps. Scenario- and step-rows share the `scenarios` table (single-table
		// inheritance) and the same pods FK, so both must go. Mirror the DeleteProject walk:
		// gather each owned scenario plus any nested scenario-steps, delete plain steps first,
		// then the scenarios. Skip a scenario still referenced as another use-case's primary
		// (shared).
		Set<Step> ownedScenariosAndSteps = new HashSet<Step>();
		java.util.Deque<Scenario> toExamine = new java.util.ArrayDeque<Scenario>();
		for (Scenario ownedScenario : ownedScenarios) {
			Scenario managedScenario = getRepository().get(ownedScenario);
			boolean sharedAsPrimary = false;
			for (UseCase using : managedScenario.getUsingUseCases()) {
				if (!using.equals(usecase)) {
					sharedAsPrimary = true;
					break;
				}
			}
			if (!sharedAsPrimary && ownedScenariosAndSteps.add(managedScenario)) {
				toExamine.add(managedScenario);
			}
		}
		while (!toExamine.isEmpty()) {
			Scenario current = toExamine.pop();
			for (Step step : current.getSteps()) {
				if (ownedScenariosAndSteps.add(step) && step instanceof Scenario nested) {
					toExamine.add(nested);
				}
			}
		}
		for (Step step : ownedScenariosAndSteps) {
			if (!(step instanceof Scenario)) {
				DeleteScenarioStepCommand deleteStepCommand = getProjectCommandFactory()
						.newDeleteScenarioStepCommand();
				deleteStepCommand.setScenarioStep(step);
				deleteStepCommand.setEditedBy(getEditedBy());
				((com.rreganjr.platform.command.AuthorizationExemptable) deleteStepCommand).setAuthorizationExempt(true);
				getCommandHandler().execute(deleteStepCommand);
			}
		}
		for (Step step : ownedScenariosAndSteps) {
			if (step instanceof Scenario scenario) {
				DeleteScenarioCommand deleteScenarioCommand = getProjectCommandFactory()
						.newDeleteScenarioCommand();
				deleteScenarioCommand.setScenario(scenario);
				deleteScenarioCommand.setEditedBy(getEditedBy());
				((com.rreganjr.platform.command.AuthorizationExemptable) deleteScenarioCommand).setAuthorizationExempt(true);
				getCommandHandler().execute(deleteScenarioCommand);
			}
		}
	}

	@Override
	public Project getProject() {
		if (usecase != null && usecase.getProjectOrDomain() instanceof Project project) return project;
		return null;
	}

}

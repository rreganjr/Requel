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
package com.rreganjr.requel.project.impl.command;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.Command;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationExemptable;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.exception.EntityLockException;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.GlossaryTerm;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.project.ProjectTeam;
import com.rreganjr.requel.project.ReportGenerator;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Stakeholder;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.Story;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.DeleteActorCommand;
import com.rreganjr.requel.project.command.DeleteGlossaryTermCommand;
import com.rreganjr.requel.project.command.DeleteGoalCommand;
import com.rreganjr.requel.project.command.DeleteProjectCommand;
import com.rreganjr.requel.project.command.DeleteReportGeneratorCommand;
import com.rreganjr.requel.project.command.DeleteScenarioCommand;
import com.rreganjr.requel.project.command.DeleteScenarioStepCommand;
import com.rreganjr.requel.project.command.DeleteStakeholderCommand;
import com.rreganjr.requel.project.command.DeleteStoryCommand;
import com.rreganjr.requel.project.command.DeleteUseCaseCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;

/**
 * Delete a whole project and every entity it contains (issue #240, epic #239).
 *
 * <p>
 * Requel's aggregate mappings cascade only {@code PERSIST}/{@code REFRESH}, so a
 * bare {@code delete(project)} would orphan every child. Rather than change the
 * persistence semantics repo-wide (or hand-roll native bulk deletes that bypass
 * reference cleanup and the annotation/tag registries), this command cascades by
 * orchestration: it invokes the existing per-entity {@code Delete*Command}s -
 * marked auth-exempt (issue #75) so a {@code Project[Delete]} holder is not
 * re-checked for each child's {@code Edit}/{@code Delete} - reusing their tested
 * reference cleanup and per-entity audit, then clears the project's own
 * annotations and teams and deletes the project row.
 *
 * <p>
 * Delete order is reference-safe: steps and scenarios (which detach from their
 * using use-cases) before use-cases; containers before the goals/actors/stories
 * they hold; glossary terms after the entities that refer to them.
 *
 * @author ron
 */
@Controller("deleteProjectCommand")
@Scope("prototype")
public class DeleteProjectCommandImpl extends AbstractEditProjectCommand implements
		DeleteProjectCommand, AuthorizableCommand, ProjectScopedCommand {

	private Project project;

	private Integer expectedVersion;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteProjectCommandImpl(AssistantFacade assistantManager, UserRepository userRepository,
			ProjectRepository projectRepository, ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setProject(Project project) {
		this.project = project;
	}

	@Override
	public void setExpectedVersion(Integer expectedVersion) {
		this.expectedVersion = expectedVersion;
	}

	protected Integer getExpectedVersion() {
		return expectedVersion;
	}

	@Override
	public void execute() throws Exception {
		Project project = getRepository().get(getProject());
		User editedBy = getRepository().get(getEditedBy());

		// Enforce the caller-supplied optimistic-lock version (issue #108).
		if (getExpectedVersion() != null
				&& getExpectedVersion().intValue() != project.getVersion()) {
			throw EntityLockException.staleEntity(Project.class, project,
					EntityExceptionActionType.Deleting);
		}

		// Note: project-scoped tag rows/assignments (tagging module) are intentionally
		// not cleaned up here. The tagging schema uses soft FKs (no DB constraint) for
		// project_id and taggable_id (see V13__tagging.sql), so deleting the project and
		// its taggable children never violates a constraint; the orphaned rows are the
		// same benign leftover the existing per-entity Delete*Commands produce. Cleaning
		// them belongs to the tagging module (it owns TagRepository, which project-jpa
		// does not depend on) - a follow-up, not this command.

		// 1) Scenarios and steps. Delete plain (non-scenario) steps first, then
		// every scenario-typed entity (top-level plus any nested as a step).
		// Walk scenarios and their steps via the domain interfaces (the impl's
		// getAllScenariosAndSteps() is not exposed on Project).
		Set<Step> allScenariosAndSteps = new HashSet<Step>(project.getScenarios());
		java.util.Deque<Scenario> toExamine = new java.util.ArrayDeque<Scenario>(
				project.getScenarios());
		while (!toExamine.isEmpty()) {
			Scenario current = toExamine.pop();
			for (Step step : current.getSteps()) {
				if (allScenariosAndSteps.add(step) && step instanceof Scenario nested) {
					toExamine.add(nested);
				}
			}
		}
		for (Step step : allScenariosAndSteps) {
			if (!(step instanceof Scenario)) {
				DeleteScenarioStepCommand command = getProjectCommandFactory()
						.newDeleteScenarioStepCommand();
				command.setScenarioStep(step);
				executeExempt(command, editedBy);
			}
		}
		Set<Scenario> scenarios = new HashSet<Scenario>(project.getScenarios());
		for (Step step : allScenariosAndSteps) {
			if (step instanceof Scenario scenario) {
				scenarios.add(scenario);
			}
		}
		for (Scenario scenario : scenarios) {
			DeleteScenarioCommand command = getProjectCommandFactory().newDeleteScenarioCommand();
			command.setScenario(scenario);
			executeExempt(command, editedBy);
		}

		// 2) Use-cases (their contained scenarios are already gone).
		for (UseCase useCase : new HashSet<UseCase>(project.getUseCases())) {
			DeleteUseCaseCommand command = getProjectCommandFactory().newDeleteUseCaseCommand();
			command.setUseCase(useCase);
			executeExempt(command, editedBy);
		}

		// 3) Stories.
		for (Story story : new HashSet<Story>(project.getStories())) {
			DeleteStoryCommand command = getProjectCommandFactory().newDeleteStoryCommand();
			command.setStory(story);
			executeExempt(command, editedBy);
		}

		// 4) Actors.
		for (Actor actor : new HashSet<Actor>(project.getActors())) {
			DeleteActorCommand command = getProjectCommandFactory().newDeleteActorCommand();
			command.setActor(actor);
			executeExempt(command, editedBy);
		}

		// 5) Goals (containers above have emptied their referer sets).
		for (Goal goal : new HashSet<Goal>(project.getGoals())) {
			DeleteGoalCommand command = getProjectCommandFactory().newDeleteGoalCommand();
			command.setGoal(goal);
			executeExempt(command, editedBy);
		}

		// 6) Glossary terms (after the entities that refer to them).
		for (GlossaryTerm term : new HashSet<GlossaryTerm>(project.getGlossaryTerms())) {
			DeleteGlossaryTermCommand command = getProjectCommandFactory()
					.newDeleteGlossaryTermCommand();
			command.setGlossaryTerm(term);
			executeExempt(command, editedBy);
		}

		// 7) Report generators.
		for (ReportGenerator reportGenerator : new HashSet<ReportGenerator>(
				project.getReportGenerators())) {
			DeleteReportGeneratorCommand command = getProjectCommandFactory()
					.newDeleteReportGeneratorCommand();
			command.setReportGenerator(reportGenerator);
			executeExempt(command, editedBy);
		}

		// 8) Stakeholders - user and non-user alike. This severs the project
		// association only; no User row is ever deleted (see DeleteStakeholder).
		for (Stakeholder stakeholder : new HashSet<Stakeholder>(project.getStakeholders())) {
			DeleteStakeholderCommand command = getProjectCommandFactory()
					.newDeleteStakeholderCommand();
			command.setStakeholder(stakeholder);
			executeExempt(command, editedBy);
		}

		// 9) Teams (no per-entity command). Detach members (a @ManyToMany join)
		// and delete the team row.
		for (ProjectTeam team : new HashSet<ProjectTeam>(project.getTeams())) {
			ProjectTeam managedTeam = getRepository().get(team);
			managedTeam.getMembers().clear();
			project.getTeams().remove(team);
			getRepository().delete(managedTeam);
		}

		// 10) The project's own annotations.
		for (Annotation annotation : new HashSet<Annotation>(project.getAnnotations())) {
			RemoveAnnotationFromAnnotatableCommand command = getAnnotationCommandFactory()
					.newRemoveAnnotationFromAnnotatableCommand();
			command.setEditedBy(editedBy);
			command.setAnnotatable(project);
			command.setAnnotation(annotation);
			getCommandHandler().execute(command);
		}

		// 11) Finally, the project row itself.
		getRepository().delete(project);
	}

	/**
	 * Run an internally-orchestrated cascade sub-command as part of this
	 * authorized project delete: attribute it to the acting user and exempt it
	 * from re-authorization (issue #75) so a {@code Project[Delete]} holder is
	 * not re-checked for each child's own permission.
	 */
	private void executeExempt(EditCommand command, User editedBy) throws Exception {
		command.setEditedBy(editedBy);
		((AuthorizationExemptable) command).setAuthorizationExempt(true);
		getCommandHandler().execute((Command) command);
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Project.class, "Delete");
	}
}

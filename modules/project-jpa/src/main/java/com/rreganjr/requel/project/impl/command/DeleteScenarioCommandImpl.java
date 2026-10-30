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

import com.rreganjr.platform.command.CascadeAuthorizable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.RemoveAnnotationFromAnnotatableCommand;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.DeleteScenarioStepCommand;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.project.command.DeleteScenarioCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.user.UserRepository;

/**
 * Delete a scenario from a project, cleaning up references from other project
 * entities, scenario relations and annotations.
 * 
 * @author ron
 */
@Controller("deleteScenarioCommand")
@Scope("prototype")
public class DeleteScenarioCommandImpl extends AbstractDeleteProjectEntityCommand implements
		DeleteScenarioCommand, ProjectScopedCommand, AuthorizableCommand {

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(com.rreganjr.requel.project.Scenario.class, "Delete");
	}

	private Scenario scenario;

	/**
	 * @param assistantManager
	 * @param userRepository
	 * @param projectRepository
	 * @param projectCommandFactory
	 * @param annotationCommandFactory
	 * @param commandHandler
	 */
	@Autowired
	public DeleteScenarioCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
	}

	@Override
	public void setScenario(Scenario scenario) {
		this.scenario = scenario;
	}

	protected Scenario getScenario() {
		return scenario;
	}

	@Override
	public void execute() throws Exception {
		Scenario scenario = getRepository().get(getScenario());
		// #296: refuse before touching anything if the caller read an older version.
		refuseStaleDelete(Scenario.class, scenario, scenario.getVersion());
		// #341: a use case's primary scenario cannot go while the use case still points at it
		// (usecases.scenario_id): it used to fail on that foreign key. Refuse it up front, naming
		// the use case. A use case deleted earlier in this transaction (DeleteUseCase,
		// DeleteProject) is not found: the query flushes first.
		List<UseCase> primaryOf = getProjectRepository().findUseCasesWithPrimaryScenario(scenario);
		if (!primaryOf.isEmpty()) {
			throw EntityValidationException.validationFailed(Scenario.class, "scenarioId",
					"\"" + scenario.getName() + "\" is the primary scenario of use case \""
							+ primaryOf.get(0).getName() + "\"; make another scenario its primary"
							+ " (SetPrimaryScenarioOnUseCase) or delete the use case first.");
		}
		User editedBy = getRepository().get(getEditedBy());
		Set<Annotation> annotations = new HashSet<Annotation>(scenario.getAnnotations());
		for (Annotation annotation : annotations) {
			RemoveAnnotationFromAnnotatableCommand removeAnnotationFromAnnotatableCommand = CascadeAuthorizable.cascade(this,
					getAnnotationCommandFactory().newRemoveAnnotationFromAnnotatableCommand());
			removeAnnotationFromAnnotatableCommand.setEditedBy(editedBy);
			removeAnnotationFromAnnotatableCommand.setAnnotatable(scenario);
			removeAnnotationFromAnnotatableCommand.setAnnotation(annotation);
			getCommandHandler().execute(removeAnnotationFromAnnotatableCommand);
		}
		// remove this entity as a referer to any terms
		// #386: by native delete; removing from the @ManyToAny referers set fails in Hibernate.
		getProjectRepository().removeGlossaryTermReferer(null, scenario);
		Set<Scenario> scenarioReferers = new HashSet<Scenario>(scenario.getUsingScenarios());
		for (Scenario scenarioReferer : scenarioReferers) {
			scenarioReferer.getSteps().remove(scenario);
		}
		// #341: the scenario's own plain steps went nowhere: nothing deleted them and there is no
		// cascade, so each stayed in `scenarios` holding its name. Detach the step list, then
		// delete each plain step no other scenario uses, as EditScenario does for a dropped step
		// (#325). A nested scenario is only unlinked; it is a scenario in its own right.
		List<Step> ownSteps = new ArrayList<Step>(scenario.getSteps());
		scenario.getSteps().clear();
		for (Step step : ownSteps) {
			if (!(step instanceof Scenario)
					&& !getProjectRepository().isStepUsedByAnotherScenario(step, scenario)) {
				DeleteScenarioStepCommand deleteStep = CascadeAuthorizable.cascade(this,
						getProjectCommandFactory().newDeleteScenarioStepCommand());
				deleteStep.setScenarioStep(step);
				deleteStep.setEditedBy(editedBy);
				getCommandHandler().execute(deleteStep);
			}
		}
		// #247: a scenario may also be an ADDITIONAL (alternative/exception) scenario of any
		// use case in the project (usecase_scenarios, owned by the use case, no inverse
		// mapping). Those join rows carry an FK to the scenario row, so detach it from every
		// such use case before the delete, or MySQL refuses the delete.
		for (UseCase usecase : scenario.getProjectOrDomain().getUseCases()) {
			usecase.getAdditionalScenarios().remove(scenario);
		}
		scenario.getProjectOrDomain().getScenarios().remove(scenario);
		// #247: clear any annotation link committed since this entity was loaded.
		removeAllAnnotationsBeforeDelete(scenario, editedBy);
		getRepository().delete(scenario);
	}

	@Override
	public Project getProject() {
		if (scenario != null && scenario.getProjectOrDomain() instanceof Project project) return project;
		return null;
	}

}

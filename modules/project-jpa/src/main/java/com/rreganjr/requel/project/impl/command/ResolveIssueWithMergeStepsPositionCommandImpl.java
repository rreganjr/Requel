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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.impl.command.ResolveIssueCommandImpl;
import com.rreganjr.requel.project.Scenario;
import com.rreganjr.requel.project.Step;
import com.rreganjr.requel.project.command.EditScenarioCommand;
import com.rreganjr.requel.project.command.EditScenarioStepCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.MergeStepsPosition;
import com.rreganjr.validator.EntityValidationException;

/**
 * Issue #472: resolve the step-sequence issue on a scenario by merging two of its steps. The first
 * step takes the merged name and the second leaves the scenario, through one {@code EditScenario}
 * (which authorizes itself as Scenario[Edit], refuses a name another step has, and deletes the
 * dropped step when no other scenario uses it); then the issue is resolved.
 *
 * <p>
 * Refused, on {@code positionId}, when the scenario no longer has a step with the first name
 * immediately followed by one with the second (renamed or reordered since), or when the first
 * step is now used by another scenario too, since renaming it would rename it there.
 */
@Controller("resolveIssueWithMergeStepsPositionCommand")
@Scope("prototype")
public class ResolveIssueWithMergeStepsPositionCommandImpl extends ResolveIssueCommandImpl {

	private final ProjectCommandFactory projectCommandFactory;

	@Autowired
	public ResolveIssueWithMergeStepsPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			ProjectCommandFactory projectCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
		this.projectCommandFactory = projectCommandFactory;
	}

	@Override
	protected MergeStepsPosition getPosition() {
		return (MergeStepsPosition) org.hibernate.Hibernate.unproxy(super.getPosition());
	}

	@Override
	public void execute() throws Exception {
		validate();
		Issue issue = getRepository().get(getIssue());
		MergeStepsPosition position = getPosition();
		User editedBy = getRepository().get(getEditedBy());
		Scenario scenario = scenarioOf(issue);
		List<Step> steps = new ArrayList<Step>(scenario.getSteps());

		int first = -1;
		for (int index = 0; index + 1 < steps.size(); index++) {
			if (Objects.equals(steps.get(index).getName(), position.getExpectedName())
					&& Objects.equals(steps.get(index + 1).getName(),
							position.getExpectedSecondName())) {
				first = index;
				break;
			}
		}
		if (first < 0) {
			throw EntityValidationException.validationFailed(Issue.class, "positionId",
					"\"" + scenario.getName() + "\" no longer has \"" + position.getExpectedName()
							+ "\" followed by \"" + position.getExpectedSecondName()
							+ "\", so this merge no longer applies. Re-run the analysis for a"
							+ " current one.");
		}
		Step firstStep = steps.get(first);
		if (firstStep.getUsingScenarios().stream().anyMatch(other -> !other.equals(scenario))) {
			throw EntityValidationException.validationFailed(Issue.class, "positionId",
					"\"" + firstStep.getName() + "\" is also used by another scenario, and"
							+ " merging would rename it there too.");
		}

		EditScenarioCommand edit = projectCommandFactory.newEditScenarioCommand();
		edit.setEditedBy(editedBy);
		edit.setProjectOrDomain(scenario.getProjectOrDomain());
		edit.setScenario(scenario);
		List<EditScenarioStepCommand> stepCommands = new ArrayList<EditScenarioStepCommand>();
		for (int index = 0; index < steps.size(); index++) {
			if (index == first + 1) {
				continue;
			}
			Step step = steps.get(index);
			EditScenarioStepCommand stepCommand = projectCommandFactory
					.newEditScenarioStepCommand();
			stepCommand.setEditedBy(editedBy);
			stepCommand.setProjectOrDomain(scenario.getProjectOrDomain());
			stepCommand.setStep(step);
			stepCommand.setName(index == first ? position.getProposedName() : step.getName());
			stepCommand.setText(step.getText());
			if (step.getType() != null) {
				stepCommand.setScenarioTypeName(step.getType().name());
			}
			stepCommands.add(stepCommand);
		}
		edit.setStepCommands(stepCommands);
		getCommandHandler().execute(edit);
		super.execute();
	}

	/** The scenario the issue is on, or the annotatable given, which must be a scenario. */
	private Scenario scenarioOf(Issue issue) {
		if (getAnnotatable() != null) {
			Annotatable annotatable = getRepository().get(getAnnotatable());
			if (org.hibernate.Hibernate.unproxy(annotatable) instanceof Scenario scenario) {
				return scenario;
			}
			throw EntityValidationException.validationFailed(Issue.class, "positionId",
					"A merge resolves an issue on a scenario.");
		}
		for (Annotatable annotatable : issue.getAnnotatables()) {
			if (org.hibernate.Hibernate.unproxy(annotatable) instanceof Scenario scenario) {
				return scenario;
			}
		}
		throw EntityValidationException.validationFailed(Issue.class, "positionId",
				"A merge resolves an issue on a scenario, and issue " + issue.getId()
						+ " isn't on one.");
	}

	@Override
	protected void validate() {
		super.validate();
		MergeStepsPosition position = getPosition();
		if (position.getProposedName() == null || position.getProposedName().isBlank()) {
			throw EntityValidationException.emptyRequiredProperty(MergeStepsPosition.class,
					position, "proposedName", EntityExceptionActionType.Updating);
		}
	}
}

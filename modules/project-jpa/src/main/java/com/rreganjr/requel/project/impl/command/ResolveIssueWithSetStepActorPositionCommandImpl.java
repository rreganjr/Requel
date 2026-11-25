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
import com.rreganjr.requel.project.impl.SetStepActorPosition;
import com.rreganjr.validator.EntityValidationException;

/**
 * #468: resolve an issue on a step (or scenario) by giving it an actor role: the name becomes the
 * action alone and the role says who does it, through {@code EditScenarioStep} (or
 * {@code EditScenario} for a scenario, its steps left as they are), which authorizes itself as
 * Scenario[Edit] and refuses a name another step or scenario already has, naming it. Then the issue
 * is resolved.
 * <p>
 * Refused, on {@code positionId}, once the name is no longer the one the proposal was built from.
 */
@Controller("resolveIssueWithSetStepActorPositionCommand")
@Scope("prototype")
public class ResolveIssueWithSetStepActorPositionCommandImpl extends ResolveIssueCommandImpl {

	private final ProjectCommandFactory projectCommandFactory;

	@Autowired
	public ResolveIssueWithSetStepActorPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			ProjectCommandFactory projectCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
		this.projectCommandFactory = projectCommandFactory;
	}

	@Override
	protected SetStepActorPosition getPosition() {
		return (SetStepActorPosition) org.hibernate.Hibernate.unproxy(super.getPosition());
	}

	@Override
	public void execute() throws Exception {
		validate();
		Issue issue = getRepository().get(getIssue());
		SetStepActorPosition position = getPosition();
		User editedBy = getRepository().get(getEditedBy());
		Step step = stepOf(issue);
		if (!Objects.equals(step.getName(), position.getExpectedName())) {
			throw EntityValidationException.validationFailed(Issue.class, "positionId",
					"The name is now \"" + step.getName() + "\", not \""
							+ position.getExpectedName() + "\", so this no longer applies. Re-run"
							+ " the analysis for a current one.");
		}
		EditScenarioStepCommand edit;
		if (step instanceof Scenario scenario) {
			EditScenarioCommand editScenario = projectCommandFactory.newEditScenarioCommand();
			editScenario.setScenario(scenario);
			edit = editScenario;
		} else {
			edit = projectCommandFactory.newEditScenarioStepCommand();
			edit.setStep(step);
		}
		edit.setEditedBy(editedBy);
		edit.setProjectOrDomain(step.getProjectOrDomain());
		edit.setName(position.getProposedName());
		edit.setActorRoleName(position.getActorRoleName());
		edit.setActorName(position.getActorName());
		getCommandHandler().execute(edit);
		super.execute();
	}

	/** The step or scenario the issue is on, or the annotatable given, which must be one. */
	private Step stepOf(Issue issue) {
		if (getAnnotatable() != null) {
			Object annotatable = org.hibernate.Hibernate.unproxy(getRepository().get(getAnnotatable()));
			if (annotatable instanceof Step step) {
				return step;
			}
			throw EntityValidationException.validationFailed(Issue.class, "positionId",
					"Setting the actor resolves an issue on a step or scenario.");
		}
		for (Annotatable annotatable : issue.getAnnotatables()) {
			if (org.hibernate.Hibernate.unproxy(annotatable) instanceof Step step) {
				return step;
			}
		}
		throw EntityValidationException.validationFailed(Issue.class, "positionId",
				"Setting the actor resolves an issue on a step or scenario, and issue "
						+ issue.getId() + " isn't on one.");
	}

	@Override
	protected void validate() {
		super.validate();
		SetStepActorPosition position = getPosition();
		if (position.getProposedName() == null || position.getProposedName().isBlank()) {
			throw EntityValidationException.emptyRequiredProperty(SetStepActorPosition.class,
					position, "proposedName", EntityExceptionActionType.Updating);
		}
		if (position.getActorRoleName() == null || position.getActorRoleName().isBlank()) {
			throw EntityValidationException.emptyRequiredProperty(SetStepActorPosition.class,
					position, "actorRole", EntityExceptionActionType.Updating);
		}
	}
}

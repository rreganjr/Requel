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
import com.rreganjr.requel.project.Actor;
import com.rreganjr.requel.project.ProjectOrDomain;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.UseCase;
import com.rreganjr.requel.project.command.AddActorToActorContainerCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.AddActorToUseCasePosition;
import com.rreganjr.validator.EntityValidationException;

/**
 * #468: resolve an "actor not in the use case" issue by adding the actor to the use case, both
 * found by name in the project of the entity the issue is on, through
 * {@code AddActorToActorContainer} (Actor[Edit]). An actor already in the use case is left alone.
 * Then the issue is resolved. Refused, on {@code positionId}, when either is gone.
 */
@Controller("resolveIssueWithAddActorToUseCasePositionCommand")
@Scope("prototype")
public class ResolveIssueWithAddActorToUseCasePositionCommandImpl extends ResolveIssueCommandImpl {

	private final ProjectCommandFactory projectCommandFactory;

	@Autowired
	public ResolveIssueWithAddActorToUseCasePositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory,
			ProjectCommandFactory projectCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
		this.projectCommandFactory = projectCommandFactory;
	}

	@Override
	protected AddActorToUseCasePosition getPosition() {
		return (AddActorToUseCasePosition) org.hibernate.Hibernate.unproxy(super.getPosition());
	}

	@Override
	public void execute() throws Exception {
		validate();
		Issue issue = getRepository().get(getIssue());
		AddActorToUseCasePosition position = getPosition();
		User editedBy = getRepository().get(getEditedBy());
		ProjectOrDomain project = projectOf(issue);
		UseCase useCase = project.getUseCases().stream()
				.filter(candidate -> sameName(candidate.getName(), position.getUseCaseName()))
				.findFirst()
				.orElseThrow(() -> EntityValidationException.validationFailed(Issue.class,
						"positionId", "There is no use case named \"" + position.getUseCaseName()
								+ "\" any more, so this no longer applies."));
		Actor actor = project.getActors().stream()
				.filter(candidate -> sameName(candidate.getName(), position.getActorName()))
				.findFirst()
				.orElseThrow(() -> EntityValidationException.validationFailed(Issue.class,
						"positionId", "There is no actor named \"" + position.getActorName()
								+ "\" any more, so this no longer applies."));
		if (!useCase.getActors().contains(actor) && !actor.equals(useCase.getPrimaryActor())) {
			AddActorToActorContainerCommand add = projectCommandFactory
					.newAddActorToActorContainerCommand();
			add.setEditedBy(editedBy);
			add.setActor(actor);
			add.setActorContainer(useCase);
			getCommandHandler().execute(add);
		}
		super.execute();
	}

	private static boolean sameName(String a, String b) {
		return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
	}

	private ProjectOrDomain projectOf(Issue issue) {
		for (Annotatable annotatable : issue.getAnnotatables()) {
			if (org.hibernate.Hibernate.unproxy(annotatable) instanceof ProjectOrDomainEntity entity
					&& entity.getProjectOrDomain() != null) {
				return entity.getProjectOrDomain();
			}
		}
		if (issue.getGroupingObject() instanceof ProjectOrDomain project) {
			return project;
		}
		throw EntityValidationException.validationFailed(Issue.class, "positionId",
				"Issue " + issue.getId() + " isn't on anything in a project.");
	}

	@Override
	protected void validate() {
		super.validate();
		AddActorToUseCasePosition position = getPosition();
		if (position.getActorName() == null || position.getActorName().isBlank()) {
			throw EntityValidationException.emptyRequiredProperty(AddActorToUseCasePosition.class,
					position, "actorName", EntityExceptionActionType.Updating);
		}
		if (position.getUseCaseName() == null || position.getUseCaseName().isBlank()) {
			throw EntityValidationException.emptyRequiredProperty(AddActorToUseCasePosition.class,
					position, "useCaseName", EntityExceptionActionType.Updating);
		}
	}
}

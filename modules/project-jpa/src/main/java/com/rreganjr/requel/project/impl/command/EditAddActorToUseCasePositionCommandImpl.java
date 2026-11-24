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
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.impl.command.EditPositionCommandImpl;
import com.rreganjr.requel.project.command.EditAddActorToUseCasePositionCommand;
import com.rreganjr.requel.project.impl.AddActorToUseCasePosition;

/**
 * #468: create or edit an {@link AddActorToUseCasePosition}. With no position given, one on the
 * issue for the same actor and use case is updated, so a re-run updates it in place.
 */
@Controller("editAddActorToUseCasePositionCommand")
@Scope("prototype")
public class EditAddActorToUseCasePositionCommandImpl extends EditPositionCommandImpl implements
		EditAddActorToUseCasePositionCommand {

	private String actorName;
	private String useCaseName;

	@Autowired
	public EditAddActorToUseCasePositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	@Override
	public void setActorName(String actorName) {
		this.actorName = actorName;
	}

	@Override
	public void setUseCaseName(String useCaseName) {
		this.useCaseName = useCaseName;
	}

	@Override
	public void execute() throws Exception {
		User editedBy = getRepository().get(getEditedBy());
		Issue issue = getRepository().get(getIssue());
		AddActorToUseCasePosition position = (org.hibernate.Hibernate.unproxy(super.getPosition())
				instanceof AddActorToUseCasePosition existing) ? existing : null;
		if (position == null && issue != null) {
			position = existing(issue);
		}
		if (position == null) {
			position = getRepository().persist(new AddActorToUseCasePosition(getText(), editedBy,
					actorName, useCaseName));
		} else {
			position.setText(getText());
			position.setActorName(actorName);
			position.setUseCaseName(useCaseName);
			position = getRepository().merge(position);
		}
		if (issue != null) {
			position.getIssues().add(issue);
		}
		setPosition(position);
		if (issue != null) {
			issue.getPositions().add(position);
			setIssue(issue);
		}
	}

	private AddActorToUseCasePosition existing(Issue issue) {
		for (Position candidate : issue.getPositions()) {
			if (org.hibernate.Hibernate.unproxy(candidate) instanceof AddActorToUseCasePosition add
					&& Objects.equals(add.getActorName(), actorName)
					&& Objects.equals(add.getUseCaseName(), useCaseName)) {
				return add;
			}
		}
		return null;
	}
}

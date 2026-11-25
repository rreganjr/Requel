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
import com.rreganjr.requel.project.command.EditSetStepActorPositionCommand;
import com.rreganjr.requel.project.impl.SetStepActorPosition;

/**
 * #468: create or edit a {@link SetStepActorPosition}. With no position given, one on the issue
 * with the same role, actor and proposed name is updated, so a re-run of the step-structure check
 * updates its positions in place; a renamed step gets new ones and the applicator prunes the old
 * ones nobody argued on (#459).
 */
@Controller("editSetStepActorPositionCommand")
@Scope("prototype")
public class EditSetStepActorPositionCommandImpl extends EditPositionCommandImpl implements
		EditSetStepActorPositionCommand {

	private String proposedName;
	private String expectedName;
	private String actorRoleName;
	private String actorName;

	@Autowired
	public EditSetStepActorPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	@Override
	public void setProposedName(String proposedName) {
		this.proposedName = proposedName;
	}

	@Override
	public void setExpectedName(String expectedName) {
		this.expectedName = expectedName;
	}

	@Override
	public void setActorRoleName(String actorRoleName) {
		this.actorRoleName = actorRoleName;
	}

	@Override
	public void setActorName(String actorName) {
		this.actorName = actorName;
	}

	@Override
	public void execute() throws Exception {
		User editedBy = getRepository().get(getEditedBy());
		Issue issue = getRepository().get(getIssue());
		SetStepActorPosition position = (org.hibernate.Hibernate.unproxy(super.getPosition())
				instanceof SetStepActorPosition existing) ? existing : null;
		if (position == null && issue != null) {
			position = existing(issue);
		}
		if (position == null) {
			position = getRepository().persist(new SetStepActorPosition(getText(), editedBy,
					proposedName, expectedName, actorRoleName, actorName));
		} else {
			position.setText(getText());
			position.setProposedName(proposedName);
			position.setExpectedName(expectedName);
			position.setActorRoleName(actorRoleName);
			position.setActorName(actorName);
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

	private SetStepActorPosition existing(Issue issue) {
		for (Position candidate : issue.getPositions()) {
			if (org.hibernate.Hibernate.unproxy(candidate) instanceof SetStepActorPosition set
					&& Objects.equals(set.getActorRoleName(), actorRoleName)
					&& Objects.equals(set.getActorName(), actorName)
					&& Objects.equals(set.getProposedName(), proposedName)) {
				return set;
			}
		}
		return null;
	}
}

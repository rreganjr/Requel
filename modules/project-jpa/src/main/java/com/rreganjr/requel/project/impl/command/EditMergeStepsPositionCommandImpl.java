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
import com.rreganjr.requel.project.command.EditMergeStepsPositionCommand;
import com.rreganjr.requel.project.impl.MergeStepsPosition;

/**
 * Issue #472: create or edit a {@link MergeStepsPosition}. With no position given, an existing
 * merge on the issue with the same proposed name is updated, so a re-run of the step-sequence
 * check updates its merges in place.
 */
@Controller("editMergeStepsPositionCommand")
@Scope("prototype")
public class EditMergeStepsPositionCommandImpl extends EditPositionCommandImpl implements
		EditMergeStepsPositionCommand {

	private String proposedName;
	private String expectedName;
	private String expectedSecondName;

	@Autowired
	public EditMergeStepsPositionCommandImpl(CommandHandler commandHandler,
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
	public void setExpectedSecondName(String expectedSecondName) {
		this.expectedSecondName = expectedSecondName;
	}

	@Override
	public void execute() throws Exception {
		User editedBy = getRepository().get(getEditedBy());
		Issue issue = getRepository().get(getIssue());
		MergeStepsPosition position = (org.hibernate.Hibernate.unproxy(super.getPosition())
				instanceof MergeStepsPosition existing) ? existing : null;
		if (position == null && issue != null) {
			position = existingMerge(issue);
		}
		if (position == null) {
			position = getRepository().persist(new MergeStepsPosition(getText(), editedBy,
					proposedName, expectedName, expectedSecondName));
		} else {
			position.setText(getText());
			position.setProposedName(proposedName);
			position.setExpectedName(expectedName);
			position.setExpectedSecondName(expectedSecondName);
			position = getRepository().merge(position);
		}
		if (issue != null) {
			position.getIssues().add(issue);
		}
		setPosition(position);
		// add the position to the issue after it has been merged so that if it is a proxy it
		// will be unwrapped by the framework.
		if (issue != null) {
			issue.getPositions().add(position);
			setIssue(issue);
		}
	}

	private MergeStepsPosition existingMerge(Issue issue) {
		for (Position candidate : issue.getPositions()) {
			if (org.hibernate.Hibernate.unproxy(candidate) instanceof MergeStepsPosition merge
					&& Objects.equals(merge.getProposedName(), proposedName)) {
				return merge;
			}
		}
		return null;
	}
}

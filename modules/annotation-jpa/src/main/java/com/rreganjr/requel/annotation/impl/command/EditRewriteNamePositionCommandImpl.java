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
package com.rreganjr.requel.annotation.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditRewriteNamePositionCommand;
import com.rreganjr.requel.annotation.impl.RewriteNamePosition;

/**
 * Issue #459: create or edit a {@link RewriteNamePosition}. There is no lookup by content: the
 * assistant applicator finds an existing one by its action key and passes it in.
 */
@Controller("editRewriteNamePositionCommand")
@Scope("prototype")
public class EditRewriteNamePositionCommandImpl extends EditPositionCommandImpl implements
		EditRewriteNamePositionCommand {

	private String proposedName;
	private String expectedName;

	@Autowired
	public EditRewriteNamePositionCommandImpl(CommandHandler commandHandler,
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
	public void execute() throws Exception {
		User editedBy = getRepository().get(getEditedBy());
		Issue issue = getRepository().get(getIssue());
		RewriteNamePosition position = (org.hibernate.Hibernate.unproxy(super.getPosition())
				instanceof RewriteNamePosition existing)
				? existing : null;
		if (position == null) {
			position = getRepository().persist(
					new RewriteNamePosition(getText(), editedBy, proposedName, expectedName));
		} else {
			position.setText(getText());
			position.setProposedName(proposedName);
			position.setExpectedName(expectedName);
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
}

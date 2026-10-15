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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.exception.EntityException;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditPositionCommand;
import com.rreganjr.requel.annotation.impl.PositionImpl;
import com.rreganjr.platform.identity.User;

/**
 * @author ron
 */
@Controller("editPositionCommand")
@Scope("prototype")
public class EditPositionCommandImpl extends AbstractEditCommand implements EditPositionCommand, com.rreganjr.requel.project.ProjectScopedCommand,
		com.rreganjr.platform.command.AuthorizableCommand {

	private Position position;
	private Issue issue;
	private String text;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public EditPositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	/**
	 * @see com.rreganjr.requel.annotation.command.EditPositionCommand#setIssue(com.rreganjr.requel.annotation.Issue)
	 */
	@Override
	public void setIssue(Issue issue) {
		this.issue = issue;
	}

	protected Issue getIssue() {
		return issue;
	}

	/**
	 * @see com.rreganjr.requel.annotation.command.EditPositionCommand#getPosition()
	 */
	@Override
	public Position getPosition() {
		return position;
	}

	/**
	 * @see com.rreganjr.requel.annotation.command.EditPositionCommand#setPosition(com.rreganjr.requel.annotation.Position)
	 */
	@Override
	public void setPosition(Position position) {
		this.position = position;
	}

	/**
	 * @see com.rreganjr.requel.annotation.command.EditPositionCommand#setText(java.lang.String)
	 */
	@Override
	public void setText(String text) {
		this.text = text;
	}

	protected String getText() {
		return text;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		validate();
		User editedBy = getRepository().get(getEditedBy());
		Issue issue = getRepository().get(getIssue());
		PositionImpl position = (PositionImpl) getPosition();
		if (position == null) {
			// Look for an existing position that matches the text and reference it with the
			// issue: PositionImpl.issues is a @ManyToMany, so one position answering several
			// issues is the design, not a collision. Keeping the lookup's result is the whole
			// point — dropping it left `position` null on the found path, and the add below
			// then threw a NullPointerException (issue #281).
			if (issue != null) {
				List<Position> matches = getAnnotationRepository()
						.findPositions(issue.getGroupingObject(), getText());
				if (matches.isEmpty()) {
					position = getRepository().persist(new PositionImpl(getText(), editedBy));
				} else if (matches.size() == 1) {
					position = (PositionImpl) matches.get(0);
				} else {
					// #284: duplicates already exist. This command is the only path that hits
					// them and is already a write in a transaction, so repair them here rather
					// than failing: lowest id survives, and its issue links, arguments and
					// resolutions absorb the rest.
					position = (PositionImpl) getAnnotationRepository()
							.mergeDuplicatePositions(matches);
				}
			} else {
				position = getRepository().persist(new PositionImpl(getText(), editedBy));
			}
		} else {
			refuseCollidingTextEdit(position, issue);
			position.setText(getText());
			position = getRepository().merge(position);
		}
		if (issue != null) {
			position.getIssues().add(issue);
		}
		setPosition(position);

		// add the position to the issue after it has been merged so that if it
		// is a proxy it will be unwrapped by the framework.
		if (issue != null) {
			issue.getPositions().add(position);
			setIssue(issue);
		}
	}

	/**
	 * #284: renaming a position onto another's text is one of the two ways duplicates are created
	 * — this branch had no uniqueness check at all. Refuse it, and name the position that already
	 * holds the text so the caller can act on it.
	 */
	private void refuseCollidingTextEdit(PositionImpl position, Issue issue) {
		if (getText().equals(position.getText())) {
			return;
		}
		Object groupingObject = null;
		if (issue != null) {
			groupingObject = issue.getGroupingObject();
		} else if (!position.getIssues().isEmpty()) {
			groupingObject = position.getIssues().iterator().next().getGroupingObject();
		}
		if (groupingObject == null) {
			// Nothing to be unique within.
			return;
		}
		for (Position existing : getAnnotationRepository().findPositions(groupingObject,
				getText())) {
			if (!existing.getId().equals(position.getId())) {
				throw EntityException.uniquenessConflict(null,
						"The text conflicts with an existing Position: position "
								+ existing.getId() + " in this grouping object already has that"
								+ " text. Edit that position instead, or choose different text.");
			}
		}
	}

	protected void validate() {
		if ((getText() == null) || "".equals(getText().trim())) {
			throw EntityValidationException.emptyRequiredProperty(Position.class, getPosition(),
					"text", EntityExceptionActionType.Updating);
		}
	}

	@Override
	public com.rreganjr.requel.project.Project getProject() {
		com.rreganjr.requel.project.Project p = AnnotationCommandProjectResolver.of(getIssue());
		if (p != null) {
			return p;
		}
		return AnnotationCommandProjectResolver.of(position);
	}

	@Override
	public com.rreganjr.platform.command.AuthorizationRequirement getAuthorizationRequirement() {
		return new com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission(com.rreganjr.requel.annotation.Annotation.class, "Edit");
	}
}

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

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditIgnorePositionCommand;
import com.rreganjr.requel.annotation.impl.IgnorePosition;

/**
 * Issue #320: attach an {@link IgnorePosition} to an issue. Like a plain position, one ignore
 * position with a given text serves every issue in the grouping object; the lowest-id match is
 * reused and a new one is created only when there is none.
 */
@Controller("editIgnorePositionCommand")
@Scope("prototype")
public class EditIgnorePositionCommandImpl extends EditPositionCommandImpl implements
		EditIgnorePositionCommand {

	@Autowired
	public EditIgnorePositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	@Override
	public void execute() throws Exception {
		User editedBy = getRepository().get(getEditedBy());
		Issue issue = getRepository().get(getIssue());
		IgnorePosition position = (IgnorePosition) getPosition();
		if (position == null) {
			position = existingIgnorePosition(issue);
			if (position == null) {
				position = getRepository().persist(new IgnorePosition(getText(), editedBy));
			}
		} else {
			position.setText(getText());
			position = getRepository().merge(position);
		}
		if (issue != null) {
			position.getIssues().add(issue);
		}
		setPosition(position);
		// add the position to the issue after it has been merged so that if it is a proxy it will
		// be unwrapped by the framework.
		if (issue != null) {
			issue.getPositions().add(position);
			setIssue(issue);
		}
	}

	private IgnorePosition existingIgnorePosition(Issue issue) {
		if (issue == null) {
			return null;
		}
		List<Position> matches = getAnnotationRepository().findPositions(issue.getGroupingObject(),
				getText());
		IgnorePosition best = null;
		for (Position match : matches) {
			Object unproxied = Hibernate.unproxy(match);
			if (unproxied instanceof IgnorePosition candidate
					&& (best == null || candidate.getId() < best.getId())) {
				best = candidate;
			}
		}
		return best;
	}
}

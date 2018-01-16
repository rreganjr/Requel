/*
 * $Id$
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.DeleteIssueCommand;
import com.rreganjr.requel.annotation.command.DeletePositionCommand;

/**
 * @author ron
 */
@Controller("deleteIssueCommand")
@Scope("prototype")
public class DeleteIssueCommandImpl extends AbstractEditCommand implements DeleteIssueCommand {

	private Issue issue;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public DeleteIssueCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	/**
	 * @see com.rreganjr.requel.annotation.command.DeleteIssueCommand#setIssue(com.rreganjr.requel.annotation.Issue)
	 */
	@Override
	public void setIssue(Issue issue) {
		this.issue = issue;
	}

	protected Issue getIssue() {
		return issue;
	}

	/**
	 * @see com.rreganjr.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		Issue issue = getRepository().get(getIssue());
		for (Annotatable annotatable : issue.getAnnotatables()) {
			annotatable.getAnnotations().remove(issue);
		}
		Set<Position> positions = new HashSet<Position>(issue.getPositions());
		issue.getPositions().clear();
		for (Position position : positions) {
			position.getIssues().remove(issue);
			if (position.getIssues().isEmpty()) {
				DeletePositionCommand deletePositionCommand = getAnnotationCommandFactory()
						.newDeletePositionCommand();
				deletePositionCommand.setPosition(position);
				deletePositionCommand.setEditedBy(getEditedBy());
				getCommandHandler().execute(deletePositionCommand);
			}
		}
		getRepository().delete(issue);
	}

}

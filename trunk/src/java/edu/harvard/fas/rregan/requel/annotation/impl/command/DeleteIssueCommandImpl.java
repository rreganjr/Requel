/*
 * $Id: DeleteIssueCommandImpl.java,v 1.2 2009/02/15 09:31:35 rregan Exp $
 * Copyright 2008, 2009 Ron Regan Jr. All Rights Reserved.
 * This file is part of Requel - the Collaborative Requirments
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
package edu.harvard.fas.rregan.requel.annotation.impl.command;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.DeleteIssueCommand;
import edu.harvard.fas.rregan.requel.annotation.command.DeletePositionCommand;

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
	 * @see edu.harvard.fas.rregan.requel.annotation.command.DeleteIssueCommand#setIssue(edu.harvard.fas.rregan.requel.annotation.Issue)
	 */
	@Override
	public void setIssue(Issue issue) {
		this.issue = issue;
	}

	protected Issue getIssue() {
		return issue;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
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

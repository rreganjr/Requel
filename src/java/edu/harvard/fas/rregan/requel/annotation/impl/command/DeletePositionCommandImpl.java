/*
 * $Id: DeletePositionCommandImpl.java,v 1.3 2009/02/23 09:39:55 rregan Exp $
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
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.Argument;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.DeleteArgumentCommand;
import edu.harvard.fas.rregan.requel.annotation.command.DeletePositionCommand;

/**
 * @author ron
 */
@Controller("deletePositionCommand")
@Scope("prototype")
public class DeletePositionCommandImpl extends AbstractEditCommand implements DeletePositionCommand {

	private Position position;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public DeletePositionCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	/**
	 * @see edu.harvard.fas.rregan.requel.annotation.command.DeletePositionCommand#setPosition(edu.harvard.fas.rregan.requel.annotation.Position)
	 */
	@Override
	public void setPosition(Position position) {
		this.position = position;
	}

	protected Position getPosition() {
		return position;
	}

	/**
	 * @see edu.harvard.fas.rregan.command.Command#execute()
	 */
	@Override
	public void execute() throws Exception {
		Position position = getRepository().get(getPosition());
		for (Issue issue : position.getIssues()) {
			issue.getPositions().remove(position);
			if (position.equals(issue.getResolvedByPosition())) {
				issue.unresolve();
			}
		}
		Set<Argument> arguments = new HashSet<Argument>(position.getArguments());
		position.getArguments().clear();
		for (Argument argument : arguments) {
			DeleteArgumentCommand deleteArgumentCommand = getAnnotationCommandFactory()
					.newDeleteArgumentCommand();
			deleteArgumentCommand.setArgument(argument);
			deleteArgumentCommand.setEditedBy(getEditedBy());
			getCommandHandler().execute(deleteArgumentCommand);
		}
		getRepository().delete(position);
	}

}

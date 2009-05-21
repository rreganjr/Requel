/*
 * $Id$
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import edu.harvard.fas.rregan.command.CommandHandler;
import edu.harvard.fas.rregan.repository.EntityExceptionActionType;
import edu.harvard.fas.rregan.requel.EntityValidationException;
import edu.harvard.fas.rregan.requel.annotation.Annotatable;
import edu.harvard.fas.rregan.requel.annotation.AnnotationRepository;
import edu.harvard.fas.rregan.requel.annotation.Issue;
import edu.harvard.fas.rregan.requel.annotation.Position;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.ResolveIssueCommand;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * @author ron
 */
@Controller("resolveIssueCommand")
@Scope("prototype")
public class ResolveIssueCommandImpl extends AbstractEditCommand implements ResolveIssueCommand {

	private Annotatable annotatable;
	private Issue issue;
	private Position position;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public ResolveIssueCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	protected Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public void setIssue(Issue issue) {
		this.issue = issue;
	}

	public Issue getIssue() {
		return issue;
	}

	@Override
	public Annotatable getAnnotatable() {
		return annotatable;
	}

	@Override
	public void setAnnotatable(Annotatable annotatable) {
		this.annotatable = annotatable;
	}

	@Override
	public void execute() throws Exception {
		validate();
		User resolvedByUser = getRepository().get(getEditedBy());
		Position position = getRepository().get(getPosition());
		Issue issue = getRepository().get(getIssue());
		position.resolveIssue(issue, resolvedByUser);
		setPosition(getRepository().merge(position));
		setIssue(getRepository().merge(issue));
	}

	protected void validate() {
		if (getIssue() == null) {
			throw EntityValidationException.emptyRequiredProperty(Issue.class, getIssue(), "issue",
					EntityExceptionActionType.Updating);
		}
		if (getPosition() == null) {
			throw EntityValidationException.emptyRequiredProperty(Issue.class, getPosition(),
					"position", EntityExceptionActionType.Updating);
		}
	}
}

/*
 * $Id: EditIssueCommandImpl.java,v 1.17 2009/02/17 11:50:47 rregan Exp $
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
import edu.harvard.fas.rregan.requel.annotation.NoSuchAnnotationException;
import edu.harvard.fas.rregan.requel.annotation.command.AnnotationCommandFactory;
import edu.harvard.fas.rregan.requel.annotation.command.EditIssueCommand;
import edu.harvard.fas.rregan.requel.annotation.impl.IssueImpl;
import edu.harvard.fas.rregan.requel.user.User;

/**
 * Create or edit an issue annotation on an annotatable entity.
 * 
 * @author ron
 */
@Controller("editIssueCommand")
@Scope("prototype")
public class EditIssueCommandImpl extends AbstractAnnotationCommand implements EditIssueCommand {

	private Issue issue;
	private boolean mustBeResolved;

	/**
	 * @param commandHandler
	 * @param annotationCommandFactory
	 * @param repository
	 */
	@Autowired
	public EditIssueCommandImpl(CommandHandler commandHandler,
			AnnotationCommandFactory annotationCommandFactory, AnnotationRepository repository) {
		super(commandHandler, annotationCommandFactory, repository);
	}

	public Issue getIssue() {
		return issue;
	}

	public void setIssue(Issue issue) {
		this.issue = issue;
	}

	protected boolean getMustBeResolved() {
		return mustBeResolved;
	}

	public void setMustBeResolved(boolean mustBeResolved) {
		this.mustBeResolved = mustBeResolved;
	}

	@Override
	public void execute() {
		validate();
		User editedBy = getRepository().get(getEditedBy());
		Object groupingObject = getRepository().get(getGroupingObject());
		Annotatable annotatable = getRepository().get(getAnnotatable());
		IssueImpl issueImpl = (IssueImpl) getIssue();
		if (issueImpl == null) {
			// TODO: look for an issue with the existing text
			try {
				issueImpl = (IssueImpl) getAnnotationRepository().findIssue(groupingObject,
						annotatable, getText());
			} catch (NoSuchAnnotationException e) {
				issueImpl = getRepository().persist(
						new IssueImpl(groupingObject, getText(), getMustBeResolved(), editedBy));
			}
		} else {
			issueImpl.setText(getText());
			issueImpl.setMustBeResolved(getMustBeResolved());
			issueImpl = getRepository().merge(issueImpl);
		}
		if (annotatable != null) {
			issueImpl.getAnnotatables().add(annotatable);
		}
		setIssue(issueImpl);
		// add the issue to the annotatable after it has been merged so that if
		// it is a proxy it will be unwrapped by the framework.
		if (annotatable != null) {
			annotatable.getAnnotations().add(issueImpl);
			setAnnotatable(annotatable);
		}
	}

	protected void validate() {
		if ((getText() == null) || "".equals(getText().trim())) {
			throw EntityValidationException.emptyRequiredProperty(Issue.class, getIssue(), "text",
					EntityExceptionActionType.Updating);
		}
	}

}

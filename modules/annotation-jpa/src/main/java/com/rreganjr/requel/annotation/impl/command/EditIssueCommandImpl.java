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
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.validator.EntityValidationException;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.IssueSeverity;
import com.rreganjr.requel.annotation.NoSuchAnnotationException;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.EditIssueCommand;
import com.rreganjr.requel.annotation.impl.IssueImpl;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectOrDomainEntity;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.platform.identity.User;

/**
 * Create or edit an issue annotation on an annotatable entity.
 * 
 * @author ron
 */
@Controller("editIssueCommand")
@Scope("prototype")
public class EditIssueCommandImpl extends AbstractAnnotationCommand
		implements EditIssueCommand, AuthorizableCommand, ProjectScopedCommand {

	private Issue issue;
	private Boolean mustBeResolved;
	private IssueSeverity severity;

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

	/**
	 * @return the value for a new issue: an unset {@code mustBeResolved} creates the issue with
	 *         {@code false}.
	 */
	protected boolean getMustBeResolved() {
		return Boolean.TRUE.equals(mustBeResolved);
	}

	/**
	 * @return the supplied value, or null when the caller left it out (unchanged on update).
	 */
	protected Boolean getMustBeResolvedOrNull() {
		return mustBeResolved;
	}

	public void setMustBeResolved(boolean mustBeResolved) {
		this.mustBeResolved = mustBeResolved;
	}

	public void setMustBeResolved(Boolean mustBeResolved) {
		this.mustBeResolved = mustBeResolved;
	}

	/**
	 * @return the supplied severity, or null for "the kind's default" on create and "unchanged" on
	 *         update.
	 */
	protected IssueSeverity getSeverity() {
		return severity;
	}

	public void setSeverity(IssueSeverity severity) {
		this.severity = severity;
	}

	/**
	 * Apply a supplied severity; a null one leaves the issue's current (or default) severity.
	 */
	protected void applySeverity(IssueImpl issue) {
		if (severity != null) {
			issue.setSeverity(severity);
		}
	}

	@Override
	public void execute() {
		validate();
		User editedBy = getRepository().get(getEditedBy());
		Object groupingObject = getRepository().get(getGroupingObject());
		Annotatable annotatable = getRepository().get(getAnnotatable());
		IssueImpl issueImpl = (IssueImpl) getIssue();
		if (issueImpl == null) {
			try {
				// Reuse an existing issue with the same text. Only a supplied severity is applied
				// to it (#271); its mustBeResolved is left as it is.
				issueImpl = (IssueImpl) getAnnotationRepository().findIssue(groupingObject,
						annotatable, getText());
				applySeverity(issueImpl);
			} catch (NoSuchAnnotationException e) {
				IssueImpl created = new IssueImpl(groupingObject, getText(), getMustBeResolved(),
						editedBy);
				applySeverity(created);
				issueImpl = getRepository().persist(created);
			}
		} else {
			issueImpl.setText(getText());
			// #271: a property the caller left out is unchanged (#316's partial-update contract).
			if (getMustBeResolvedOrNull() != null) {
				issueImpl.setMustBeResolved(getMustBeResolvedOrNull());
			}
			applySeverity(issueImpl);
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

	@Override
	public Project getProject() {
		Object groupingObject = getGroupingObject();
		if (groupingObject instanceof Project project) {
			return project;
		}

		Annotatable annotatable = getAnnotatable();
		if (annotatable instanceof ProjectOrDomainEntity entity
				&& entity.getProjectOrDomain() instanceof Project project) {
			return project;
		}

		if (issue != null) {
			if (issue.getGroupingObject() instanceof Project project) {
				return project;
			}
			for (Annotatable issueAnnotatable : issue.getAnnotatables()) {
				if (issueAnnotatable instanceof ProjectOrDomainEntity entity
						&& entity.getProjectOrDomain() instanceof Project project) {
					return project;
				}
			}
		}

		return null;
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Annotation.class, "Edit");
	}

}

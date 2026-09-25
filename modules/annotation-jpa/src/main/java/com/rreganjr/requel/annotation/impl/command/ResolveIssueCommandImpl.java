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
import com.rreganjr.requel.annotation.Annotatable;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.annotation.AnnotationRepository;
import com.rreganjr.requel.annotation.Issue;
import com.rreganjr.requel.annotation.Position;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.annotation.command.ResolveIssueCommand;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.platform.identity.User;

/**
 * Resolves an issue with one of its positions.
 * <p>
 * Requires {@code Annotation[Edit]} on the issue's project (issue #305). Resolving is an
 * annotation write, so it reuses the permission every other annotation command already
 * requires rather than introducing a permission type of its own. The four resolver
 * subclasses inherit this requirement; those that edit a project entity additionally run
 * that entity's own {@code Edit*Command}, which is authorized in its own right.
 * <p>
 * Those nested commands are deliberately <em>not</em> marked authorization-exempt.
 * {@link com.rreganjr.requel.annotation.impl.command.AbstractEditCommand} implements
 * {@code AuthorizationExemptable} and {@code AuthorizingCommandHandler} short-circuits on
 * that flag <em>before</em> the {@code AuthorizableCommand} check, so exempting a resolve's
 * sub-commands would silently reopen the hole this class closes.
 *
 * @author ron
 */
@Controller("resolveIssueCommand")
@Scope("prototype")
public class ResolveIssueCommandImpl extends AbstractEditCommand
		implements ResolveIssueCommand, AuthorizableCommand, ProjectScopedCommand {

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

	@Override
	public Position getResolvingPosition() {
		return position;
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

	/**
	 * The project the resolved issue belongs to, for the stakeholder check. An annotation has
	 * no direct project reference, so this walks the same routes the other annotation commands
	 * use: the issue's grouping object or annotatables, then the position's issues, then the
	 * explicitly-set annotatable. Returns {@code null} for a domain-scoped annotation, which
	 * {@code AuthorizingCommandHandler} treats as "not a stakeholder on the target project"
	 * and denies.
	 */
	@Override
	public Project getProject() {
		Project project = AnnotationCommandProjectResolver.of(getIssue());
		if (project != null) {
			return project;
		}
		project = AnnotationCommandProjectResolver.of(getPosition());
		if (project != null) {
			return project;
		}
		return AnnotationCommandProjectResolver.ofAnnotatable(getAnnotatable());
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Annotation.class, "Edit");
	}
}

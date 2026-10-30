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
 * Those nested commands are deliberately <em>not</em> cascade steps
 * ({@link com.rreganjr.platform.command.CascadeAuthorizable}, #75): a step is authorized by its
 * parent's permission, so making a resolve's entity edits steps would let Annotation[Edit] edit
 * any entity - the hole this class closes.
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

	/**
	 * Subclasses that override this call it first: issue and position are required, and #330 the
	 * position must be one of the issue's and fit its kind of issue.
	 */
	protected void validate() {
		// The field, not getIssue(): the lexical resolvers narrow getIssue() with a cast, which
		// would fail on the wrong kind of issue before requirePositionOfIssue could say so.
		if (issue == null) {
			throw EntityValidationException.emptyRequiredProperty(Issue.class, issue, "issue",
					EntityExceptionActionType.Updating);
		}
		if (getPosition() == null) {
			throw EntityValidationException.emptyRequiredProperty(Issue.class, getPosition(),
					"position", EntityExceptionActionType.Updating);
		}
		requirePositionOfIssue();
	}

	/**
	 * Issue #330: accepting a position resolves its issue, so the position has to be one the
	 * issue offers. Nothing checked it, so any position id resolved any issue, another project's
	 * included, and a resolver that needs a lexical issue (Add as Actor, Fix Spelling, ...) failed
	 * on a cast when handed another kind. Both are refused here, as a field error on
	 * {@code positionId}, before a resolver changes anything.
	 */
	protected void requirePositionOfIssue() {
		Issue issue = getRepository().get(this.issue);
		Position position = getRepository().get(getPosition());
		Long positionId = position.getId();
		boolean offered = issue.getPositions().stream()
				.anyMatch(p -> p.getId() != null && p.getId().equals(positionId));
		if (!offered) {
			throw EntityValidationException.validationFailed(Issue.class, "positionId", "Position "
					+ positionId + " is not one of issue " + issue.getId() + "'s positions.");
		}
		if (!requiredIssueType().isInstance(org.hibernate.Hibernate.unproxy(issue))) {
			throw EntityValidationException.validationFailed(Issue.class, "positionId", "Position "
					+ positionId + " resolves only " + requiredIssueType().getSimpleName()
					+ "s, and issue " + issue.getId() + " is not one.");
		}
	}

	/** The kind of issue this resolver works on; the lexical resolvers narrow it. */
	protected Class<? extends Issue> requiredIssueType() {
		return Issue.class;
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
		Project project = AnnotationCommandProjectResolver.of(issue);
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

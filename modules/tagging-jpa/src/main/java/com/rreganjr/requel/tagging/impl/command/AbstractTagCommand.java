/*
 * This file is part of Requel - the Collaborative Requirements
 * Elicitation System.
 *
 * Copyright 2026 Ron Regan Jr. All Rights Reserved.
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
package com.rreganjr.requel.tagging.impl.command;

import com.rreganjr.command.AbstractCommand;
import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationExemptable;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresSystemRole;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.tagging.Tag;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.command.TagCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Base for tag mutation commands.
 *
 * <p>Authorization mirrors the annotation seam (tags are a cross-cutting attachment like
 * annotations): a project-scoped tag reuses the stakeholder {@code Annotation[Edit]} /
 * {@code Annotation[Delete]} permission on the owning project, while a global (system) tag
 * requires the {@link SystemAdminUserRole}. A dedicated {@code Tag} permission can be
 * introduced later (see doc/project-entity-categorization.md §4) without changing callers.</p>
 *
 * @author ron
 */
public abstract class AbstractTagCommand extends AbstractCommand
		implements TagCommand, AuthorizationExemptable, ProjectScopedCommand, AuthorizableCommand {

	private final CommandHandler commandHandler;
	private final TagCommandFactory tagCommandFactory;
	private User editedBy;
	private Tag tag;
	private Object projectScope;
	private boolean authorizationExempt = false;

	protected AbstractTagCommand(CommandHandler commandHandler, TagCommandFactory tagCommandFactory,
			TagRepository repository) {
		super(repository);
		this.commandHandler = commandHandler;
		this.tagCommandFactory = tagCommandFactory;
	}

	protected CommandHandler getCommandHandler() {
		return commandHandler;
	}

	protected TagCommandFactory getTagCommandFactory() {
		return tagCommandFactory;
	}

	protected TagRepository getTagRepository() {
		return (TagRepository) getRepository();
	}

	@Override
	public void setEditedBy(User editedBy) {
		this.editedBy = editedBy;
	}

	@Override
	public User getEditedBy() {
		return editedBy;
	}

	@Override
	public void setTag(Tag tag) {
		this.tag = tag;
	}

	@Override
	public Tag getTag() {
		return tag;
	}

	@Override
	public void setProjectScope(Object project) {
		this.projectScope = project;
	}

	protected Object getProjectScope() {
		return projectScope;
	}

	@Override
	public boolean isAuthorizationExempt() {
		return authorizationExempt;
	}

	@Override
	public void setAuthorizationExempt(boolean authorizationExempt) {
		this.authorizationExempt = authorizationExempt;
	}

	@Override
	public Project getProject() {
		return (projectScope instanceof Project p) ? p : null;
	}

	/**
	 * @return the stakeholder permission type ("Edit" or "Delete") to require for a
	 *         project-scoped tag.
	 */
	protected abstract String stakeholderPermissionType();

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		if (getProject() == null) {
			// Global/system tag — admin only.
			return new RequiresSystemRole(SystemAdminUserRole.class);
		}
		return new RequiresStakeholderPermission(Annotation.class, stakeholderPermissionType());
	}
}

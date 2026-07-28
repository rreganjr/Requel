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
import com.rreganjr.requel.tagging.TagCategory;
import com.rreganjr.requel.tagging.TagRepository;
import com.rreganjr.requel.tagging.command.TagCategoryCommand;
import com.rreganjr.requel.tagging.command.TagCommandFactory;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;

/**
 * Base for tag-category mutation commands. Authorization mirrors the tag commands: a project-scoped
 * category reuses the stakeholder {@code Annotation[Edit]}/{@code [Delete]} permission; a global
 * category requires the {@link SystemAdminUserRole}.
 *
 * @author ron
 */
public abstract class AbstractTagCategoryCommand extends AbstractCommand
		implements TagCategoryCommand, AuthorizationExemptable, ProjectScopedCommand, AuthorizableCommand {

	private final CommandHandler commandHandler;
	private final TagCommandFactory tagCommandFactory;
	private User editedBy;
	private Object projectScope;
	private TagCategory tagCategory;
	private boolean authorizationExempt = false;

	protected AbstractTagCategoryCommand(CommandHandler commandHandler,
			TagCommandFactory tagCommandFactory, TagRepository repository) {
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
	public void setProjectScope(Object project) {
		this.projectScope = project;
	}

	protected Object getProjectScope() {
		return projectScope;
	}

	protected void setTagCategory(TagCategory tagCategory) {
		this.tagCategory = tagCategory;
	}

	@Override
	public TagCategory getTagCategory() {
		return tagCategory;
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

	/** "Edit" or "Delete" — the stakeholder permission required for a project-scoped category. */
	protected abstract String stakeholderPermissionType();

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		if (getProject() == null) {
			return new RequiresSystemRole(SystemAdminUserRole.class);
		}
		return new RequiresStakeholderPermission(Annotation.class, stakeholderPermissionType());
	}
}

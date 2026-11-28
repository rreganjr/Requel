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
package com.rreganjr.requel.service.command;

import com.rreganjr.command.CommandMetadata;
import com.rreganjr.command.CommandMetadataAware;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.requel.annotation.Annotation;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;
import com.rreganjr.requel.service.api.dto.BulkResolveIssuesInput;
import com.rreganjr.requel.service.api.dto.BulkResolveIssuesResultDto;
import com.rreganjr.platform.identity.User;

/**
 * #507: one action on many issues. Needs Annotation[Edit] in the project to start; each issue
 * then goes through its own command (ResolveIssue or EditIssue) and that command's permission
 * checks, in its own transaction, via {@link BulkIssueResolver}.
 */
public class BulkResolveIssuesCommand implements AuthorizableCommand, ProjectScopedCommand, CommandMetadataAware {

	private final BulkIssueResolver resolver;
	private final BulkResolveIssuesInput input;
	private final Project project;
	private User editedBy;
	private CommandMetadata commandMetadata;
	private BulkResolveIssuesResultDto result;

	BulkResolveIssuesCommand(BulkIssueResolver resolver, BulkResolveIssuesInput input, Project project) {
		this.resolver = resolver;
		this.input = input;
		this.project = project;
	}

	@Override
	public void execute() {
		result = resolver.resolve(project, input);
	}

	public BulkResolveIssuesResultDto getResult() {
		return result;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Annotation.class, "Edit");
	}

	@Override
	public User getEditedBy() {
		return editedBy;
	}

	@Override
	public void setEditedBy(User editedBy) {
		this.editedBy = editedBy;
	}

	@Override
	public CommandMetadata getCommandMetadata() {
		return commandMetadata;
	}

	@Override
	public void setCommandMetadata(CommandMetadata commandMetadata) {
		this.commandMetadata = commandMetadata;
	}
}

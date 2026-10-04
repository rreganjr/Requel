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
package com.rreganjr.requel.project.impl.command;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.rreganjr.command.AbstractCommand;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.identity.User;
import com.rreganjr.repository.jpa.BeanValidationException;
import com.rreganjr.requel.project.AssistantDefinition;
import com.rreganjr.requel.project.InvalidDefinitionException;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantDefinitions;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.command.AssistantDefinitionCommand;
import com.rreganjr.validator.EntityValidationException;

/**
 * Issue #264: the commands that author a project's assistant definitions, through
 * {@link ProjectAssistantDefinitions}. Each needs {@code AssistantDefinition[Edit]} on the
 * project. A broken rule comes back as a validation failure naming the field, so the API reports
 * it against the form control; a stale lock version as the standard lock error.
 *
 * <p>Created through the application context ({@code ProjectCommandFactoryImpl}), which injects
 * the setters; without the definitions (no AI module) a command fails rather than does nothing.
 */
public abstract class AssistantDefinitionCommandImpl extends AbstractCommand
		implements AssistantDefinitionCommand, AuthorizableCommand {

	/** No repository of its own: it writes through {@link ProjectAssistantDefinitions}. */
	protected AssistantDefinitionCommandImpl() {
		super(null);
	}

	private Project project;
	private String key;
	private Integer lockVersion;
	private ProjectAssistantDefinitions.Draft draft;
	private ProjectAssistantDefinitions.View definition;
	private User editedBy;
	private ProjectAssistantDefinitions definitions;
	private ProjectAssistantSettingsStore settingsStore;

	@Autowired(required = false)
	public void setDefinitions(ProjectAssistantDefinitions definitions) {
		this.definitions = definitions;
	}

	@Autowired(required = false)
	public void setSettingsStore(ProjectAssistantSettingsStore settingsStore) {
		this.settingsStore = settingsStore;
	}

	@Override
	public void setProject(Project project) {
		this.project = project;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public void setLockVersion(Integer lockVersion) {
		this.lockVersion = lockVersion;
	}

	@Override
	public void setDraft(ProjectAssistantDefinitions.Draft draft) {
		this.draft = draft;
	}

	@Override
	public ProjectAssistantDefinitions.View getDefinition() {
		return definition;
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
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(AssistantDefinition.class, "Edit");
	}

	@Override
	public void execute() throws Exception {
		if (project == null) {
			throw EntityValidationException.emptyRequiredProperty(Project.class, null, "project",
					actionType());
		}
		if (definitions == null) {
			throw new IllegalStateException("assistant definitions are not available");
		}
		try {
			definition = apply(definitions, project.getId(), editedBy == null ? null
					: editedBy.getUsername());
		} catch (InvalidDefinitionException e) {
			throw fieldErrors(e, actionType());
		}
	}

	/** The change itself; returns the definition after it, or null. */
	protected abstract ProjectAssistantDefinitions.View apply(
			ProjectAssistantDefinitions definitions, Long projectId, String by);

	protected abstract EntityExceptionActionType actionType();

	protected String key() {
		return key;
	}

	protected ProjectAssistantDefinitions.Draft draft() {
		if (draft == null) {
			throw new InvalidDefinitionException(key, List.of(
					new InvalidDefinitionException.Problem("definition", "the definition is missing")));
		}
		return draft;
	}

	protected int lockVersion() {
		if (lockVersion == null) {
			throw new InvalidDefinitionException(key, List.of(
					new InvalidDefinitionException.Problem("version", "version is required")));
		}
		return lockVersion;
	}

	protected ProjectAssistantSettingsStore settingsStore() {
		return settingsStore;
	}

	/** Every problem as a field error, for the API's 422 (#176). */
	static BeanValidationException fieldErrors(InvalidDefinitionException e,
			EntityExceptionActionType actionType) {
		List<InvalidDefinitionException.Problem> problems = e.fieldProblems();
		String[] fields = new String[problems.size()];
		String[] messages = new String[problems.size()];
		for (int i = 0; i < problems.size(); i++) {
			fields[i] = problems.get(i).field() == null ? "definition" : problems.get(i).field();
			messages[i] = problems.get(i).message();
		}
		return new BeanValidationException(e, AssistantDefinition.class, null, fields, messages,
				actionType, e.getMessage());
	}

	/** A new project-only definition. */
	public static class CreateImpl extends AssistantDefinitionCommandImpl
			implements AssistantDefinitionCommand.Create {
		@Override
		protected ProjectAssistantDefinitions.View apply(ProjectAssistantDefinitions definitions,
				Long projectId, String by) {
			return definitions.create(projectId, draft(), by);
		}

		@Override
		protected EntityExceptionActionType actionType() {
			return EntityExceptionActionType.Creating;
		}
	}

	/** An edit of one of the project's definitions. */
	public static class EditImpl extends AssistantDefinitionCommandImpl
			implements AssistantDefinitionCommand.Edit {
		@Override
		protected ProjectAssistantDefinitions.View apply(ProjectAssistantDefinitions definitions,
				Long projectId, String by) {
			return definitions.edit(projectId, key(), lockVersion(), draft(), by);
		}

		@Override
		protected EntityExceptionActionType actionType() {
			return EntityExceptionActionType.Updating;
		}
	}

	/** A copy of a bundled definition, to edit. */
	public static class ForkImpl extends AssistantDefinitionCommandImpl
			implements AssistantDefinitionCommand.Fork {
		@Override
		protected ProjectAssistantDefinitions.View apply(ProjectAssistantDefinitions definitions,
				Long projectId, String by) {
			return definitions.fork(projectId, key(), by);
		}

		@Override
		protected EntityExceptionActionType actionType() {
			return EntityExceptionActionType.Creating;
		}
	}

	/** Back to the bundled definition: the project's copy goes. */
	public static class RevertImpl extends AssistantDefinitionCommandImpl
			implements AssistantDefinitionCommand.Revert {
		@Override
		protected ProjectAssistantDefinitions.View apply(ProjectAssistantDefinitions definitions,
				Long projectId, String by) {
			definitions.revert(projectId, key(), lockVersion());
			return null;
		}

		@Override
		protected EntityExceptionActionType actionType() {
			return EntityExceptionActionType.Deleting;
		}
	}

	/**
	 * A definition the project created goes. Its switch is set back to on, so a later definition
	 * with the same key doesn't inherit an old "off" (#268: an on switch is the same as none).
	 */
	public static class DeleteImpl extends AssistantDefinitionCommandImpl
			implements AssistantDefinitionCommand.Delete {
		@Override
		protected ProjectAssistantDefinitions.View apply(ProjectAssistantDefinitions definitions,
				Long projectId, String by) {
			definitions.delete(projectId, key(), lockVersion());
			if (settingsStore() != null) {
				settingsStore().setEnabled(projectId, key(), true, getEditedBy());
			}
			return null;
		}

		@Override
		protected EntityExceptionActionType actionType() {
			return EntityExceptionActionType.Deleting;
		}
	}
}

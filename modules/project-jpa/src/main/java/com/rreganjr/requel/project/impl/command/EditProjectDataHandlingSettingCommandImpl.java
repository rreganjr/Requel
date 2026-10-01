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
package com.rreganjr.requel.project.impl.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.rreganjr.command.CommandHandler;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.platform.exception.EntityExceptionActionType;
import com.rreganjr.platform.identity.User;
import com.rreganjr.requel.annotation.command.AnnotationCommandFactory;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.DataHandlingSettings;
import com.rreganjr.requel.project.ProjectAssistantSettingsStore;
import com.rreganjr.requel.project.ProjectRepository;
import com.rreganjr.requel.project.command.EditProjectDataHandlingSettingCommand;
import com.rreganjr.requel.project.command.ProjectCommandFactory;
import com.rreganjr.requel.project.impl.ProjectAssistantSettingImpl;
import com.rreganjr.requel.project.impl.assistant.AssistantFacade;
import com.rreganjr.requel.user.UserRepository;
import com.rreganjr.validator.EntityValidationException;

/**
 * Issue #262: see {@link EditProjectDataHandlingSettingCommand}. The key must be one of
 * {@link DataHandlingSettings#KEYS}; anything else is refused rather than stored. The setting is
 * stored in #268's {@link ProjectAssistantSettingsStore}, where no row means on.
 */
@Controller("editProjectDataHandlingSettingCommand")
@Scope("prototype")
public class EditProjectDataHandlingSettingCommandImpl extends AbstractProjectCommand
		implements EditProjectDataHandlingSettingCommand, AuthorizableCommand {

	private Project project;
	private String key;
	private boolean enabled = true;
	private User editedBy;
	private ProjectAssistantSettingsStore settingsStore;

	@Autowired
	public EditProjectDataHandlingSettingCommandImpl(AssistantFacade assistantManager,
			UserRepository userRepository, ProjectRepository projectRepository,
			ProjectCommandFactory projectCommandFactory,
			AnnotationCommandFactory annotationCommandFactory, CommandHandler commandHandler) {
		super(assistantManager, userRepository, projectRepository, projectCommandFactory,
				annotationCommandFactory, commandHandler);
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
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public User getEditedBy() {
		return editedBy;
	}

	@Override
	public void setEditedBy(User editedBy) {
		this.editedBy = editedBy;
	}

	/** What a project may send to an AI provider is a project setting, like #268's switches. */
	@Override
	public AuthorizationRequirement getAuthorizationRequirement() {
		return new RequiresStakeholderPermission(Project.class, "Edit");
	}

	@Override
	public void execute() throws Exception {
		if (project == null) {
			throw EntityValidationException.emptyRequiredProperty(Project.class, null, "project",
					EntityExceptionActionType.Updating);
		}
		if (key == null || key.isBlank()) {
			throw EntityValidationException.emptyRequiredProperty(
					ProjectAssistantSettingImpl.class, null, "key",
					EntityExceptionActionType.Updating);
		}
		if (!DataHandlingSettings.isKey(key)) {
			throw EntityValidationException.validationFailed(ProjectAssistantSettingImpl.class,
					"key", "\"" + key + "\" is not a data-handling setting; expected one of "
							+ DataHandlingSettings.KEYS);
		}
		if (settingsStore == null) {
			throw new IllegalStateException("no ProjectAssistantSettingsStore is configured");
		}
		settingsStore.setEnabled(project.getId(), key, enabled, editedBy);
	}
}

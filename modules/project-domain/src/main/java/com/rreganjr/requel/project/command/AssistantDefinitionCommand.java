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
package com.rreganjr.requel.project.command;

import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectAssistantDefinitions;
import com.rreganjr.requel.project.ProjectScopedCommand;

/**
 * Issue #264: a change to one of a project's assistant definitions, made by a project member who
 * holds {@code AssistantDefinition[Edit]}. The kinds of change are the sub-interfaces; each reads
 * only the setters it needs.
 */
public interface AssistantDefinitionCommand extends EditCommand, ProjectScopedCommand {

	void setProject(Project project);

	/** The definition's key (not read by create, which takes it from the draft). */
	void setKey(String key);

	/** The optimistic-lock version the caller read (edit, revert, delete). */
	void setLockVersion(Integer lockVersion);

	/** The definition as written (create, edit). */
	void setDraft(ProjectAssistantDefinitions.Draft draft);

	/** The definition after the change, or null for revert and delete. */
	ProjectAssistantDefinitions.View getDefinition();

	/** A new project-only definition. */
	interface Create extends AssistantDefinitionCommand {
	}

	/** An edit of the project's own definition or its copy of a bundled one. */
	interface Edit extends AssistantDefinitionCommand {
	}

	/** Copy a bundled definition into the project, to edit. */
	interface Fork extends AssistantDefinitionCommand {
	}

	/** Drop the project's copy of a bundled definition. */
	interface Revert extends AssistantDefinitionCommand {
	}

	/** Delete a definition the project created. */
	interface Delete extends AssistantDefinitionCommand {
	}
}

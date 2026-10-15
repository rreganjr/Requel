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
package com.rreganjr.requel.project.command;

import com.rreganjr.platform.command.EditCommand;
import com.rreganjr.requel.project.Project;
import com.rreganjr.requel.project.ProjectScopedCommand;

/**
 * Issue #268: switch one of a project's assistants on or off. Only an assistant the registry
 * lists as switchable can be set. Switching one off leaves its existing findings as they are; it
 * just stops running. Requires {@code Project[Edit]}.
 */
public interface EditProjectAssistantSettingCommand extends EditCommand, ProjectScopedCommand {

	void setProject(Project project);

	void setAssistantId(String assistantId);

	void setEnabled(boolean enabled);
}

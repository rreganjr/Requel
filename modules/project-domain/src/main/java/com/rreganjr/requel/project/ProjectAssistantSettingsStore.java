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
package com.rreganjr.requel.project;

import java.util.Set;

import com.rreganjr.platform.identity.User;

/**
 * Issue #268: which assistants are switched off in a project. One row per project and assistant
 * that has been set; no row means the assistant runs. Keyed by assistant id so later per-project
 * assistant settings (#258) extend it rather than replace it.
 */
public interface ProjectAssistantSettingsStore {

	/** @return the ids of the assistants switched off in the project; empty for a null id. */
	Set<String> disabledAssistants(Long projectId);

	/** Switch an assistant on or off in a project, recording who did it. */
	void setEnabled(Long projectId, String assistantId, boolean enabled, User by);

	/** Remove the project's settings; the project delete path calls this. */
	int deleteForProject(Long projectId);
}

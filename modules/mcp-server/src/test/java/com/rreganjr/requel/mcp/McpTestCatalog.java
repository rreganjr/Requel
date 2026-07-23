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
package com.rreganjr.requel.mcp;

import com.rreganjr.requel.gateway.CommandDescriptor;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.service.api.dto.AddGoalToGoalContainerInput;
import com.rreganjr.requel.service.api.dto.EditGoalInput;
import com.rreganjr.requel.service.api.dto.EditIssueInput;
import com.rreganjr.requel.service.api.dto.EditNoteInput;
import com.rreganjr.requel.service.api.dto.EditProjectInput;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A small, representative {@link GatewayCommandCatalog} for MCP unit tests: a handful of the real
 * allowlisted command types wired to their real input DTOs, so tool-name and schema generation is
 * exercised against genuine records without booting a Spring context.
 */
final class McpTestCatalog {

	private McpTestCatalog() {
	}

	/** A representative catalog covering create/edit, association, and annotation commands. */
	static GatewayCommandCatalog sample() {
		Map<String, CommandDescriptor> byType = new LinkedHashMap<>();
		add(byType, "EditProject", EditProjectInput.class, "Edit Project");
		add(byType, "EditGoal", EditGoalInput.class, "Edit Goal");
		add(byType, "AddGoalToGoalContainer", AddGoalToGoalContainerInput.class,
				"Add Goal To Goal Container");
		add(byType, "EditNote", EditNoteInput.class, "Edit Note");
		add(byType, "EditIssue", EditIssueInput.class, "Edit Issue");
		return of(byType);
	}

	private static void add(Map<String, CommandDescriptor> byType, String commandType,
			Class<?> inputType, String title) {
		byType.put(commandType,
				new CommandDescriptor(commandType, inputType, title, null, true, null));
	}

	private static GatewayCommandCatalog of(Map<String, CommandDescriptor> byType) {
		List<CommandDescriptor> descriptors = List.copyOf(byType.values());
		return new GatewayCommandCatalog() {
			@Override
			public List<CommandDescriptor> descriptors() {
				return descriptors;
			}

			@Override
			public Optional<CommandDescriptor> find(String commandType) {
				return Optional.ofNullable(byType.get(commandType));
			}
		};
	}
}

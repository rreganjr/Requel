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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.gateway.CommandDescriptor;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.gateway.GatewayResult;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Lockstep guard (issue #104): the MCP typed write tools are generated <em>from</em> the shared
 * {@link GatewayCommandCatalog}, so the set of typed write tools must be exactly the catalog's
 * command types (plus the generic {@code runCommand} escape hatch). This is the {@code MCP tools ⊆
 * catalog} regression — if the catalog and the advertised tools ever diverge, this fails. The
 * full-context, real-allowlist version lives in {@code McpToolCatalogLockstepIT} (requel-app).
 */
class McpWriteCatalogLockstepTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final GatewayCommandCatalog catalog = McpTestCatalog.sample();
	private final McpWriteService writes = new McpWriteService(
			request -> new GatewayResult(request.commandType(), null), catalog, objectMapper, true);

	@Test
	void everyTypedWriteToolMapsToACatalogCommand() {
		List<String> catalogTypes = catalog.descriptors().stream()
				.map(CommandDescriptor::commandType).toList();

		List<String> typedToolNames = writes.toolDescriptors().stream()
				.map(McpToolDescriptor::name)
				.filter(name -> !name.equals(McpWriteService.RUN_COMMAND))
				.toList();

		// Subset (⊆) AND coverage: the typed write surface is exactly the catalog.
		assertThat(typedToolNames).allSatisfy(name ->
				assertThat(catalog.find(name)).as("tool '%s' must be in the catalog", name)
						.isPresent());
		assertThat(typedToolNames).containsExactlyInAnyOrderElementsOf(catalogTypes);
	}

	@Test
	void handlesEveryCatalogCommandAndTheGenericTool() {
		assertThat(writes.handles(McpWriteService.RUN_COMMAND)).isTrue();
		catalog.descriptors().forEach(d ->
				assertThat(writes.handles(d.commandType()))
						.as("handles('%s')", d.commandType()).isTrue());
		assertThat(writes.handles("NotACommand")).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	void toolSchemaIsDerivedFromTheCommandInputDto() {
		McpToolDescriptor editGoal = writes.toolDescriptors().stream()
				.filter(t -> t.name().equals("EditGoal")).findFirst().orElseThrow();

		var properties = (java.util.Map<String, Object>) editGoal.inputSchema().get("properties");
		// EditGoalInput(projectName, goalId, name, text, version) → typed JSON schema properties.
		assertThat(properties).containsKeys("projectName", "goalId", "name", "text", "version");
		assertThat(editGoal.inputSchema()).containsEntry("additionalProperties", false);

		// Required is derived from the @NotBlank annotations the command applicator implies:
		// projectName + name are unconditional; goalId/text/version are create-or-update/optional.
		var required = (java.util.List<String>) editGoal.inputSchema().get("required");
		assertThat(required).containsExactlyInAnyOrder("projectName", "name");
		assertThat(required).doesNotContain("goalId", "text", "version");
	}
}

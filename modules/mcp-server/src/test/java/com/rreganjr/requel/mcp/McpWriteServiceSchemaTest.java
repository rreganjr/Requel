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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rreganjr.requel.gateway.CommandDescriptor;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.gateway.GatewayResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Exercises the input-DTO → JSON-schema generation in {@link McpWriteService#toolDescriptors()}:
 * every Java-to-JSON type mapping, required-field derivation from {@code @NotNull}/{@code @NotBlank},
 * the description synthesis, and the empty/{@link Void} input case.
 */
class McpWriteServiceSchemaTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	enum Color { RED, GREEN }

	/** A record covering every jsonType branch, with a required String and a required Long. */
	record RichInput(
			@NotBlank String name,
			@NotNull Long id,
			int count,
			boolean flag,
			Double ratio,
			Color color,
			List<String> tags,
			Object blob) {
	}

	private McpWriteService serviceFor(CommandDescriptor... descriptors) {
		GatewayCommandCatalog catalog = new GatewayCommandCatalog() {
			@Override
			public List<CommandDescriptor> descriptors() {
				return List.of(descriptors);
			}

			@Override
			public Optional<CommandDescriptor> find(String commandType) {
				return List.of(descriptors).stream()
						.filter(d -> d.commandType().equals(commandType)).findFirst();
			}
		};
		return new McpWriteService(request -> new GatewayResult(request.commandType(), null),
				catalog, objectMapper, true);
	}

	private McpToolDescriptor tool(McpWriteService svc, String name) {
		return svc.toolDescriptors().stream().filter(t -> t.name().equals(name))
				.findFirst().orElseThrow();
	}

	@Test
	@SuppressWarnings("unchecked")
	void mapsEveryJavaTypeToItsJsonType() {
		McpWriteService svc = serviceFor(
				new CommandDescriptor("Rich", RichInput.class, "Rich", "Does a thing", true, null));
		Map<String, Object> schema = tool(svc, "Rich").inputSchema();
		var props = (Map<String, Object>) schema.get("properties");

		assertThat(props.get("name")).isEqualTo(Map.of("type", "string"));
		assertThat(props.get("id")).isEqualTo(Map.of("type", "integer"));
		assertThat(props.get("count")).isEqualTo(Map.of("type", "integer"));   // primitive int
		assertThat(props.get("flag")).isEqualTo(Map.of("type", "boolean"));    // primitive boolean
		assertThat(props.get("ratio")).isEqualTo(Map.of("type", "number"));    // Double
		assertThat(props.get("color")).isEqualTo(Map.of("type", "string"));    // enum → string
		assertThat(props.get("tags")).isEqualTo(Map.of("type", "array"));      // List → array
		assertThat(props.get("blob")).isEqualTo(Map.of("type", "object"));     // fallback object
		assertThat(schema).containsEntry("additionalProperties", false);
	}

	@Test
	@SuppressWarnings("unchecked")
	void derivesRequiredOnlyFromValidationAnnotations() {
		McpWriteService svc = serviceFor(
				new CommandDescriptor("Rich", RichInput.class, "Rich", "Does a thing", true, null));
		var required = (List<String>) tool(svc, "Rich").inputSchema().get("required");
		// Only the @NotBlank/@NotNull components; unannotated ones (incl. primitives) are optional.
		assertThat(required).containsExactlyInAnyOrder("name", "id");
		assertThat(required).doesNotContain("count", "flag", "ratio", "color", "tags", "blob");
	}

	@Test
	void descriptionUsesCatalogDescriptionWhenPresentAndListsFields() {
		McpWriteService svc = serviceFor(
				new CommandDescriptor("Rich", RichInput.class, "Rich Title", "Does a thing", true,
						null));
		String description = tool(svc, "Rich").description();
		assertThat(description).startsWith("Does a thing.")   // non-blank description wins over title
				.contains("Input fields: name, id, count, flag, ratio, color, tags, blob.");
	}

	@Test
	@SuppressWarnings("unchecked")
	void voidInputYieldsEmptyObjectSchemaAndTitleOnlyDescription() {
		// Null description → falls back to the title; Void input → no properties, no field hint.
		McpWriteService svc = serviceFor(
				new CommandDescriptor("Ping", Void.class, "Ping", null, true, null));
		McpToolDescriptor ping = tool(svc, "Ping");

		Map<String, Object> schema = ping.inputSchema();
		assertThat((Map<String, Object>) schema.get("properties")).isEmpty();
		assertThat((List<String>) schema.get("required")).isEmpty();
		assertThat(schema).containsEntry("additionalProperties", false);
		assertThat(ping.description()).isEqualTo("Ping.");
	}

	@Test
	void callingAToolNotInTheCatalogIsRejected() {
		McpWriteService svc = serviceFor(
				new CommandDescriptor("Rich", RichInput.class, "Rich", "Does a thing", true, null));
		assertThatThrownBy(() -> svc.call("NotACommand", null))
				.isInstanceOf(McpInvalidParamsException.class)
				.hasMessageContaining("Unknown MCP write tool");
	}
}

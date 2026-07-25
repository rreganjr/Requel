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
package com.rreganjr.requel.service.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.rreganjr.requel.gateway.CommandDescriptor;
import com.rreganjr.requel.gateway.CommandGateway;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.gateway.GatewayResult;
import com.rreganjr.requel.service.gateway.GatewayCommandController.DescriptorView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the descriptor endpoint's {@link DescriptorView} projection (issue #103): it now
 * carries a JSON {@code schema} for each command's input DTO (via {@link
 * com.rreganjr.requel.gateway.CommandInputSchema}), which the CLI turns into per-field typed
 * subcommands. Also pins the write-flag gating (empty list when writes are disabled). Constructs the
 * controller directly with a stub catalog — no Spring context needed.
 */
class GatewayCommandControllerTest {

	record EditThingInput(@NotBlank String name, @NotNull Long id, String note) {
	}

	private static final CommandGateway NOOP_GATEWAY =
			request -> new GatewayResult(request.commandType(), null);

	private static GatewayCommandCatalog catalogOf(CommandDescriptor... descriptors) {
		return new GatewayCommandCatalog() {
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
	}

	@Test
	@SuppressWarnings("unchecked")
	void descriptorViewCarriesInputSchemaWithRequiredFields() {
		GatewayCommandCatalog catalog = catalogOf(
				new CommandDescriptor("EditThing", EditThingInput.class, "Edit Thing",
						"Edits a thing", true, "Thing[Edit]"));
		GatewayCommandController controller =
				new GatewayCommandController(NOOP_GATEWAY, catalog, true);

		List<DescriptorView> views = controller.descriptors();

		assertThat(views).singleElement().satisfies(v -> {
			assertThat(v.commandType()).isEqualTo("EditThing");
			assertThat(v.inputType()).isEqualTo("EditThingInput"); // simple name, not the Class
			assertThat(v.write()).isTrue();
			assertThat(v.schema()).containsEntry("type", "object")
					.containsEntry("additionalProperties", false);
			var props = (Map<String, Object>) v.schema().get("properties");
			assertThat(props).containsOnlyKeys("name", "id", "note");
			assertThat(props.get("name")).isEqualTo(Map.of("type", "string"));
			assertThat(props.get("id")).isEqualTo(Map.of("type", "integer"));
			// Only @NotBlank/@NotNull components are required; the unannotated one is optional.
			assertThat((List<String>) v.schema().get("required"))
					.containsExactlyInAnyOrder("name", "id");
		});
	}

	@Test
	void descriptorsAreEmptyWhenWritesDisabled() {
		GatewayCommandCatalog catalog = catalogOf(
				new CommandDescriptor("EditThing", EditThingInput.class, "Edit Thing", null, true,
						null));
		GatewayCommandController controller =
				new GatewayCommandController(NOOP_GATEWAY, catalog, false);

		assertThat(controller.descriptors()).isEmpty();
	}
}

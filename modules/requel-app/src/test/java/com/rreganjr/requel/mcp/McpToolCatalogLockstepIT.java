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

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.gateway.CommandDescriptor;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.service.gateway.GatewayPolicyConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Full-context lockstep regression (issue #104): with writes enabled, the MCP server's typed write
 * tools are generated from the shared {@link GatewayCommandCatalog}, which is itself built from the
 * same {@code GatewayPolicyConfig.ALLOWED} set the gateway's allow/deny policy enforces. This
 * asserts {@code MCP tools ⊆ catalog} against the real wired catalog and real allowlist, so the MCP
 * surface, the CLI's {@code /descriptors} surface, and the enforced policy cannot drift apart.
 *
 * <p>Enables {@code requel.gateway.write.enabled} for this context so the write tools are advertised
 * (the flag is read once when the {@link McpWriteService} bean is constructed).
 */
@TestPropertySource(properties = "requel.gateway.write.enabled=true")
public class McpToolCatalogLockstepIT extends AbstractIntegrationTestCase {

	private McpWriteService writeService;
	private GatewayCommandCatalog catalog;

	@Autowired
	protected void setWriteService(McpWriteService writeService) {
		this.writeService = writeService;
	}

	@Autowired
	protected void setCatalog(GatewayCommandCatalog catalog) {
		this.catalog = catalog;
	}

	@Test
	public void typedWriteToolsAreExactlyTheCatalogAndAllSubsetOfTheAllowlist() {
		assertThat(writeService.isWriteEnabled())
				.as("write flag must be on for this IT").isTrue();

		List<String> catalogTypes = catalog.descriptors().stream()
				.map(CommandDescriptor::commandType).toList();
		assertThat(catalogTypes).as("catalog should be non-empty when the app is booted").isNotEmpty();

		List<String> typedToolNames = writeService.toolDescriptors().stream()
				.map(McpToolDescriptor::name)
				.filter(name -> !name.equals(McpWriteService.RUN_COMMAND))
				.toList();

		// MCP tools ⊆ catalog: every typed write tool is a catalog command...
		assertThat(typedToolNames).allSatisfy(name ->
				assertThat(catalog.find(name)).as("MCP tool '%s' must be in the catalog", name)
						.isPresent());
		// ...and the catalog is itself ⊆ the enforced allowlist, so nothing outside the policy leaks.
		assertThat(typedToolNames).allSatisfy(name ->
				assertThat(GatewayPolicyConfig.ALLOWED)
						.as("MCP tool '%s' must be on the gateway allowlist", name)
						.contains(name));
		// Full coverage: the typed write surface equals the catalog (no hidden extras, none dropped).
		assertThat(typedToolNames).containsExactlyInAnyOrderElementsOf(catalogTypes);

		// The generic escape hatch is always present alongside the typed tools.
		assertThat(writeService.toolDescriptors()).extracting(McpToolDescriptor::name)
				.contains(McpWriteService.RUN_COMMAND);
	}
}

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
import static org.assertj.core.api.Assertions.catchThrowable;

import com.rreganjr.AbstractIntegrationTestCase;
import com.rreganjr.requel.gateway.CommandDescriptor;
import com.rreganjr.requel.annotation.spi.AnnotatableTypeRegistry;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.gateway.QueryDescriptions;
import com.rreganjr.requel.gateway.QueryGateway;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.api.dto.ConvertStepToScenarioInput;
import com.rreganjr.requel.service.api.dto.DeleteProjectInput;
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

	private CommandRegistry registry;
	private QueryGateway queryGateway;
	private AnnotatableTypeRegistry annotatableTypes;

	@Autowired
	protected void setRegistry(CommandRegistry registry) {
		this.registry = registry;
	}

	@Autowired
	protected void setQueryGateway(QueryGateway queryGateway) {
		this.queryGateway = queryGateway;
	}

	@Autowired
	protected void setAnnotatableTypes(AnnotatableTypeRegistry annotatableTypes) {
		this.annotatableTypes = annotatableTypes;
	}

	// ---- issue #296: the catalog a caller reads is complete ------------------------------------

	/**
	 * The catalog skips an allowlisted command that is not registered, without saying so, so the
	 * checks below would pass over it. Every allowlisted command is registered in this deployment.
	 */
	@Test
	public void everyAllowedCommandIsRegisteredAndCatalogued() {
		assertThat(GatewayPolicyConfig.ALLOWED).allSatisfy(type -> {
			assertThat(registry.isRegistered(type)).as("%s is allowlisted but not registered", type)
					.isTrue();
			assertThat(catalog.find(type)).as("%s is missing from the catalog", type).isPresent();
		});
	}

	/**
	 * Stricter than {@link #noAdvertisedCommandHasAnEmptyInputSchema}: every allowlisted command's
	 * input is a record, which is what {@code CommandInputSchema} derives a schema from. A command
	 * with no input DTO advertises an empty schema and cannot be called (#252).
	 */
	@Test
	public void everyAllowedCommandHasARecordInput() {
		assertThat(catalog.descriptors()).allSatisfy(d -> assertThat(d.inputType())
				.as("%s needs a record input DTO (see #252)", d.commandType())
				.isNotNull().matches(Class::isRecord, "is a record"));
	}

	/** A newly allowlisted command without a {@code @CommandDescription} fails here. */
	@Test
	public void everyAllowedCommandIsDescribed() {
		assertThat(catalog.descriptors()).allSatisfy(d -> assertThat(d.description())
				.as("%s: add a @CommandDescription to %s", d.commandType(),
						d.inputType() == null ? "its input DTO" : d.inputType().getSimpleName())
				.isNotBlank());
	}

	/**
	 * Every allowlisted command carries a hint. Derived hints come from creating each command
	 * through its factory, so this also shows that works in the running context; the pins check
	 * one derived hint of each shape and one override.
	 */
	@Test
	public void everyAllowedCommandCarriesAnAuthorizationHint() {
		assertThat(catalog.descriptors()).allSatisfy(d -> assertThat(d.authorizationHint())
				.as("%s has no authorization hint", d.commandType()).isNotBlank());

		assertThat(hint("EditGoal")).isEqualTo("Goal[Edit]");
		assertThat(hint("DeleteProject")).isEqualTo("Project[Delete] or system administrator");
		assertThat(hint("DeleteIssue")).isEqualTo("Annotation[Delete]");
		assertThat(hint("EditTag")).contains("Annotation[Edit]").contains("system administrator");
		assertThat(hint("EditProject")).contains("createProjects").contains("Project[Edit]");
	}

	private String hint(String commandType) {
		return catalog.find(commandType).map(CommandDescriptor::authorizationHint).orElse(null);
	}

	/** What an MCP client reads: one full stop per sentence, then the permission, then the fields. */
	@Test
	public void typedWriteToolDescriptionsAreWellFormed() {
		assertThat(writeService.toolDescriptors())
				.filteredOn(t -> catalog.find(t.name()).isPresent())
				.isNotEmpty()
				.allSatisfy(t -> assertThat(t.description()).as(t.name())
						.doesNotContain("..").contains(" Requires: ").contains(" Input fields: "));
	}

	/**
	 * The read tools advertise {@code entityType} as an enum, so the lists in
	 * {@link QueryDescriptions} must match what the application accepts: the annotatable types are
	 * the registry's keys, and the readable types are exactly those {@code getEntity} does not
	 * reject as unsupported.
	 */
	@Test
	public void theAdvertisedEntityTypesAreTheOnesTheApplicationAccepts() {
		assertThat(annotatableTypes.getRegisteredAnnotatableTypes().keySet())
				.containsExactlyInAnyOrderElementsOf(QueryDescriptions.ANNOTATABLE_ENTITY_TYPES);

		for (String type : QueryDescriptions.READABLE_ENTITY_TYPES) {
			assertThat(unsupportedByGetEntity(type)).as("getEntity should accept %s", type).isFalse();
		}
		for (String type : QueryDescriptions.ANNOTATABLE_ENTITY_TYPES) {
			if (!QueryDescriptions.READABLE_ENTITY_TYPES.contains(type)) {
				assertThat(unsupportedByGetEntity(type))
						.as("%s is readable by getEntity; add it to READABLE_ENTITY_TYPES", type)
						.isTrue();
			}
		}
	}

	/** Whether {@code getEntity} rejects the type itself, before looking for any project. */
	private boolean unsupportedByGetEntity(String entityType) {
		Throwable thrown = catchThrowable(
				() -> queryGateway.getEntity("#296 no such project", entityType, -1L));
		return thrown instanceof IllegalArgumentException
				&& String.valueOf(thrown.getMessage()).contains("Unsupported entity type");
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
				.filter(name -> !McpWriteService.COMPOSITE_TOOLS.contains(name))
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

	/**
	 * Issue #252: an allowlisted command registered without an input DTO records {@code Void.class},
	 * and the catalog turns that into {@code {"properties": {}, "type": "object"}} — a typed tool
	 * that advertises no way to call it. Four commands were in that state at once and nothing
	 * failed, because an empty schema looks like a working tool from every angle except using it.
	 *
	 * <p>Set-general on purpose: the eleven other commands registered through the same placeholder
	 * overload are invisible only because they are not allowlisted, so this fails the day one of
	 * them is added without a DTO.
	 */
	@Test
	public void noAdvertisedCommandHasAnEmptyInputSchema() {
		List<String> schemaless = catalog.descriptors().stream()
				.filter(d -> d.inputType() == null || d.inputType() == Void.class)
				.map(CommandDescriptor::commandType)
				.toList();

		assertThat(schemaless)
				.as("these commands are advertised with an empty input schema and cannot be "
						+ "called; give each a real input DTO or take it off GatewayPolicyConfig"
						+ ".ALLOWED")
				.isEmpty();
	}

	/**
	 * Issue #252's own AC: the distinction between the step command that is offered and the three
	 * that are not.
	 *
	 * <p>{@code EditScenario} replaces the whole steps array on save, so creating, editing and
	 * deleting steps all go through it — a step with no {@code stepId} is created, one with its
	 * {@code stepId} is edited, and one left out is deleted. {@code ConvertStepToScenario} is the
	 * exception: sending {@code isScenario: true} with a plain step's id routes to a scenario
	 * lookup that throws for a step, so it is the only one that would lose capability.
	 */
	@Test
	public void onlyConvertStepToScenarioIsOfferedOfTheStepCommands() {
		assertThat(GatewayPolicyConfig.ALLOWED)
				.as("steps are managed through EditScenario's steps array")
				.doesNotContain("EditScenarioStep", "CopyScenarioStep", "DeleteScenarioStep");
		assertThat(catalog.find("EditScenarioStep")).isEmpty();
		assertThat(catalog.find("CopyScenarioStep")).isEmpty();
		assertThat(catalog.find("DeleteScenarioStep")).isEmpty();
		assertThat(writeService.toolDescriptors()).extracting(McpToolDescriptor::name)
				.doesNotContain("EditScenarioStep", "CopyScenarioStep", "DeleteScenarioStep");

		assertThat(GatewayPolicyConfig.ALLOWED).contains("ConvertStepToScenario");
		CommandDescriptor descriptor = catalog.find("ConvertStepToScenario").orElseThrow();
		assertThat(descriptor.inputType())
				.as("the typed tool's JSON schema is derived from this DTO")
				.isEqualTo(ConvertStepToScenarioInput.class);
		assertThat(descriptor.description())
				.as("a caller has to be told steps are otherwise managed through EditScenario")
				.contains("EditScenario");
		assertThat(writeService.toolDescriptors()).extracting(McpToolDescriptor::name)
				.contains("ConvertStepToScenario");
	}

	/**
	 * Issue #242: {@code DeleteProject} is exposed on the write gateway. The assertions above are
	 * set-general, so this pins the ticket's own AC against the real allowlist and the real wired
	 * catalog: on the allowlist, in the catalog (which is what the REST {@code /descriptors}
	 * endpoint and therefore the {@code requel-cli} command list read), advertised as a typed tool,
	 * and dispatchable through the generic {@code runCommand} — with its schema derived from
	 * {@link DeleteProjectInput}.
	 */
	@Test
	public void deleteProjectIsExposedAsATypedToolAndOnTheAllowlist() {
		assertThat(GatewayPolicyConfig.ALLOWED)
				.as("DeleteProject must be on the gateway allowlist").contains("DeleteProject");
		assertThat(GatewayPolicyConfig.DENIED)
				.as("DeleteProject is project-scoped, not identity/file-transfer")
				.doesNotContain("DeleteProject");

		assertThat(catalog.find("DeleteProject"))
				.as("the catalog is what /descriptors and requel-cli enumerate").isPresent();
		CommandDescriptor descriptor = catalog.find("DeleteProject").orElseThrow();
		assertThat(descriptor.inputType())
				.as("the typed tool's JSON schema is derived from this DTO")
				.isEqualTo(DeleteProjectInput.class);

		assertThat(writeService.toolDescriptors()).extracting(McpToolDescriptor::name)
				.contains("DeleteProject");
		assertThat(writeService.handles("DeleteProject"))
				.as("runCommand and the typed tool both dispatch it").isTrue();
	}
}

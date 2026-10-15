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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rreganjr.command.Command;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.platform.command.AuthorizationRequirement.RequiresStakeholderPermission;
import com.rreganjr.requel.gateway.CommandDescriptor;
import com.rreganjr.requel.project.Goal;
import com.rreganjr.requel.service.api.CommandDescription;
import com.rreganjr.requel.service.api.CommandRegistration;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.api.dto.EditTagCategoryInput;
import com.rreganjr.requel.service.api.dto.EditTagInput;
import com.rreganjr.requel.service.command.ApiCommandFactory;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class GatewayCommandCatalogImplTest {

    @Test
    void catalogCoversEveryRegisteredAllowedCommandAsAWrite() {
        CommandRegistry registry = mock(CommandRegistry.class);
        when(registry.isRegistered(anyString())).thenReturn(true);
        ApiCommandFactory factory = mock(ApiCommandFactory.class);
        // doReturn(...) avoids the Class<?> generic-capture mismatch that when(...).thenReturn hits.
        doReturn(Object.class).when(factory).getInputType(anyString());

        GatewayCommandCatalogImpl catalog = new GatewayCommandCatalogImpl(registry, factory);

        assertThat(catalog.descriptors())
                .extracting(CommandDescriptor::commandType)
                .containsExactlyInAnyOrderElementsOf(GatewayPolicyConfig.ALLOWED);
        assertThat(catalog.descriptors()).allMatch(CommandDescriptor::write);
        // Allowed commands are present; denied ones (e.g. user management) are not.
        assertThat(catalog.find("EditGoal")).isPresent();
        assertThat(catalog.find("EditUser")).isEmpty();
    }

    @Test
    void skipsCommandsNotRegisteredInThisDeployment() {
        CommandRegistry registry = mock(CommandRegistry.class);
        when(registry.isRegistered(anyString())).thenReturn(false);
        ApiCommandFactory factory = mock(ApiCommandFactory.class);

        GatewayCommandCatalogImpl catalog = new GatewayCommandCatalogImpl(registry, factory);

        assertThat(catalog.descriptors()).isEmpty();
    }

    @Test
    void humanizeSplitsPascalCase() {
        assertThat(GatewayCommandCatalogImpl.humanize("EditGoal")).isEqualTo("Edit Goal");
        assertThat(GatewayCommandCatalogImpl.humanize("AddScenarioToUseCase"))
                .isEqualTo("Add Scenario To Use Case");
    }

    @Test
    void descriptionComesFromTheInputTypesAnnotation() {
        assertThat(GatewayCommandCatalogImpl.describe(DescribedInput.class))
                .isEqualTo("Does the thing, and slugs what you give it.");
        assertThat(GatewayCommandCatalogImpl.describe(UndescribedInput.class)).isNull();
        assertThat(GatewayCommandCatalogImpl.describe(Void.class)).isNull();
        assertThat(GatewayCommandCatalogImpl.describe(null)).isNull();
    }

    /**
     * A command with no annotation must leave the description null rather than inventing one:
     * {@code McpWriteService} falls back to the title plus the input's field names. Every
     * allowlisted command is described (issue #296, pinned by {@code McpToolCatalogLockstepIT}),
     * so only a newly added one would reach this.
     */
    @Test
    void anUndescribedCommandLeavesTheDescriptionNull() {
        CommandRegistry registry = mock(CommandRegistry.class);
        when(registry.isRegistered(anyString())).thenReturn(true);
        ApiCommandFactory factory = mock(ApiCommandFactory.class);
        doReturn(UndescribedInput.class).when(factory).getInputType(anyString());

        GatewayCommandCatalogImpl catalog = new GatewayCommandCatalogImpl(registry, factory);

        assertThat(catalog.descriptors()).allMatch(d -> d.description() == null);
    }

    /**
     * The two tagging commands are why the mechanism exists (issue #255): a caller has to be told
     * that what they send is not what gets stored. Asserting against the real DTOs rather than a
     * stub means the annotation cannot be dropped from them without this failing.
     */
    @Test
    void theTaggingCommandsDescribeTheirNormalization() {
        assertThat(GatewayCommandCatalogImpl.describe(EditTagCategoryInput.class))
                .contains("slug")
                .contains("con-3685");
        assertThat(GatewayCommandCatalogImpl.describe(EditTagInput.class))
                .contains("slug")
                .contains("con-3685");
    }

    /** Issue #296: an input-dependent command states its hint, and that wins over derivation. */
    @Test
    void theAuthorizationOverrideWinsOverTheDerivedHint() {
        CommandRegistry registry = mock(CommandRegistry.class);
        GatewayCommandCatalogImpl catalog =
                new GatewayCommandCatalogImpl(registry, mock(ApiCommandFactory.class));

        assertThat(catalog.authorizationHint("EditTag", OverriddenInput.class))
                .isEqualTo("Annotation[Edit] for a project tag");
        verify(registry, never()).lookup(anyString());
    }

    /** Issue #296: otherwise the hint is read off a command created with no input. */
    @Test
    void theHintIsDerivedFromTheCommand() {
        AuthorizableCommand command = mock(AuthorizableCommand.class);
        when(command.getAuthorizationRequirement())
                .thenReturn(new RequiresStakeholderPermission(Goal.class, "Edit"));
        CommandRegistry registry = mock(CommandRegistry.class);
        doReturn(registration(() -> command)).when(registry).lookup("EditGoal");
        GatewayCommandCatalogImpl catalog =
                new GatewayCommandCatalogImpl(registry, mock(ApiCommandFactory.class));

        assertThat(catalog.authorizationHint("EditGoal", DescribedInput.class))
                .isEqualTo("Goal[Edit]");
    }

    /** A command that cannot be created gets no hint; the lockstep IT turns that into a failure. */
    @Test
    void aCommandThatCannotBeCreatedLeavesTheHintNull() {
        CommandRegistry registry = mock(CommandRegistry.class);
        doReturn(registration(() -> {
            throw new IllegalStateException("no bean");
        })).when(registry).lookup("EditGoal");
        GatewayCommandCatalogImpl catalog =
                new GatewayCommandCatalogImpl(registry, mock(ApiCommandFactory.class));

        assertThat(catalog.authorizationHint("EditGoal", DescribedInput.class)).isNull();
    }

    /**
     * Issue #296: deriving hints creates commands through the application context, so the catalog
     * waits for first use instead of doing it while the context is still creating it.
     */
    @Test
    void theCatalogIsBuiltOnFirstUseAndOnlyOnce() {
        CommandRegistry registry = mock(CommandRegistry.class);
        when(registry.isRegistered(anyString())).thenReturn(true);
        ApiCommandFactory factory = mock(ApiCommandFactory.class);
        doReturn(Object.class).when(factory).getInputType(anyString());

        GatewayCommandCatalogImpl catalog = new GatewayCommandCatalogImpl(registry, factory);
        verifyNoInteractions(registry, factory);

        catalog.descriptors();
        catalog.find("EditGoal");
        catalog.descriptors();
        verify(registry, times(GatewayPolicyConfig.ALLOWED.size())).isRegistered(anyString());
    }

    private static CommandRegistration<Object> registration(Supplier<Command> factoryMethod) {
        return new CommandRegistration<>("EditGoal", Object.class, factoryMethod, null, null, null,
                null, null);
    }

    @CommandDescription(value = "Does the thing.",
            authorization = "Annotation[Edit] for a project tag")
    private record OverriddenInput(String name) {
    }

    @CommandDescription("Does the thing, and slugs what you give it.")
    private record DescribedInput(String name) {
    }

    private record UndescribedInput(String name) {
    }
}

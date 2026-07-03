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
import static org.mockito.Mockito.when;

import com.rreganjr.requel.gateway.CommandDescriptor;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.command.ApiCommandFactory;
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
}

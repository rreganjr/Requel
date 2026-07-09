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

import com.rreganjr.requel.gateway.CommandDescriptor;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.command.ApiCommandFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * The single source of truth for the gateway's exposed write commands (issue #70). Builds a
 * {@link CommandDescriptor} for every command on the gateway allowlist
 * ({@link GatewayPolicyConfig#ALLOWED}) that is actually registered, deriving the input DTO type
 * from the {@link CommandRegistry}. Because it is built from the same {@code ALLOWED} set the
 * {@link com.rreganjr.requel.gateway.CommandPolicy} bean uses, the catalog cannot drift from what
 * the gateway actually permits.
 *
 * <p>Consumed by the REST descriptors endpoint (so {@code requel-cli} generates its surface from it)
 * and available to any front-end that wants to enumerate the write surface. Read/query operations
 * are the {@link com.rreganjr.requel.gateway.QueryGateway} surface, not part of this command catalog.
 */
@Component
public class GatewayCommandCatalogImpl implements GatewayCommandCatalog {

    private final List<CommandDescriptor> descriptors;
    private final LinkedHashMap<String, CommandDescriptor> byType = new LinkedHashMap<>();

    public GatewayCommandCatalogImpl(CommandRegistry registry, ApiCommandFactory apiCommandFactory) {
        List<CommandDescriptor> built = new ArrayList<>();
        for (String commandType : new TreeSet<>(GatewayPolicyConfig.ALLOWED)) {
            if (!registry.isRegistered(commandType)) {
                continue; // allowlisted but not registered in this deployment — skip.
            }
            Class<?> inputType;
            try {
                inputType = apiCommandFactory.getInputType(commandType);
            } catch (RuntimeException e) {
                inputType = Void.class;
            }
            CommandDescriptor descriptor = new CommandDescriptor(
                    commandType, inputType, humanize(commandType), null, true, null);
            built.add(descriptor);
            byType.put(commandType, descriptor);
        }
        this.descriptors = List.copyOf(built);
    }

    @Override
    public List<CommandDescriptor> descriptors() {
        return descriptors;
    }

    @Override
    public Optional<CommandDescriptor> find(String commandType) {
        return Optional.ofNullable(byType.get(commandType));
    }

    /** Turn a PascalCase command type into a spaced title, e.g. {@code EditGoal} → {@code Edit Goal}. */
    static String humanize(String commandType) {
        return commandType == null ? "" : commandType.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
    }
}

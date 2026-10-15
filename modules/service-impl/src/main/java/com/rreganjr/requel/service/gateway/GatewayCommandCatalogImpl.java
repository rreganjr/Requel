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

import com.rreganjr.command.Command;
import com.rreganjr.platform.command.AuthorizableCommand;
import com.rreganjr.requel.gateway.CommandDescriptor;
import com.rreganjr.requel.gateway.GatewayCommandCatalog;
import com.rreganjr.requel.service.api.CommandDescription;
import com.rreganjr.requel.service.api.CommandRegistration;
import com.rreganjr.requel.service.api.CommandRegistry;
import com.rreganjr.requel.service.command.ApiCommandFactory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * <p>The descriptors are built on first use rather than in the constructor (issue #296). Deriving
 * the authorization hint creates each command through its registration's factory, which asks the
 * application context for a prototype bean; doing that while this singleton is itself being
 * created would make the catalog's construction depend on the order the context builds beans.
 */
@Component
public class GatewayCommandCatalogImpl implements GatewayCommandCatalog {

    private static final Logger log = LoggerFactory.getLogger(GatewayCommandCatalogImpl.class);

    private final CommandRegistry registry;
    private final ApiCommandFactory apiCommandFactory;

    /** Built once, on first use; see the class comment. */
    private volatile Map<String, CommandDescriptor> byType;

    public GatewayCommandCatalogImpl(CommandRegistry registry, ApiCommandFactory apiCommandFactory) {
        this.registry = registry;
        this.apiCommandFactory = apiCommandFactory;
    }

    @Override
    public List<CommandDescriptor> descriptors() {
        return List.copyOf(built().values());
    }

    @Override
    public Optional<CommandDescriptor> find(String commandType) {
        return Optional.ofNullable(built().get(commandType));
    }

    private Map<String, CommandDescriptor> built() {
        Map<String, CommandDescriptor> result = byType;
        if (result == null) {
            synchronized (this) {
                result = byType;
                if (result == null) {
                    result = build();
                    byType = result;
                }
            }
        }
        return result;
    }

    private Map<String, CommandDescriptor> build() {
        LinkedHashMap<String, CommandDescriptor> built = new LinkedHashMap<>();
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
            built.put(commandType, new CommandDescriptor(commandType, inputType,
                    humanize(commandType), describe(inputType), true,
                    authorizationHint(commandType, inputType)));
        }
        return Collections.unmodifiableMap(built);
    }

    /**
     * The caller-facing description the input DTO declares with {@link CommandDescription}, or
     * {@code null} when it declares none.
     *
     * <p>Every allowlisted command declares one (issue #296, asserted by
     * {@code McpToolCatalogLockstepIT}); the null case remains for a command added without one,
     * where the MCP layer falls back to the title plus the input's field names. Reading the text
     * from the DTO rather than hardcoding it here keeps the description beside the fields it
     * describes, where whoever changes those fields will see it.
     */
    static String describe(Class<?> inputType) {
        if (inputType == null) {
            return null;
        }
        CommandDescription description = inputType.getAnnotation(CommandDescription.class);
        return description == null ? null : description.value();
    }

    /**
     * The permission a caller needs, as text (issue #296). The input type's
     * {@link CommandDescription#authorization()} wins when it is set; otherwise the hint is derived
     * from a command created with no input. That is right for a command whose requirement is fixed
     * and wrong for one whose requirement depends on its input, which is what the override is for.
     *
     * <p>A command that cannot be created, or that is not an {@link AuthorizableCommand}, gets a
     * null hint and a warning; {@code McpToolCatalogLockstepIT} fails on a null hint.
     */
    String authorizationHint(String commandType, Class<?> inputType) {
        String override = authorizationOverride(inputType);
        if (override != null) {
            return override;
        }
        try {
            CommandRegistration<?> registration = registry.lookup(commandType);
            Supplier<Command> factory = registration == null ? null : registration.factoryMethod();
            if (factory == null) {
                log.warn("No authorization hint for {}: it has no factory method", commandType);
                return null;
            }
            Command command = factory.get();
            if (!(command instanceof AuthorizableCommand authorizable)) {
                log.warn("No authorization hint for {}: {} is not an AuthorizableCommand",
                        commandType, command == null ? "null" : command.getClass().getName());
                return null;
            }
            return AuthorizationHints.render(authorizable.getAuthorizationRequirement());
        } catch (RuntimeException e) {
            log.warn("No authorization hint for {}: {}", commandType, e.toString());
            return null;
        }
    }

    /** The {@link CommandDescription#authorization()} override, or {@code null} when unset. */
    static String authorizationOverride(Class<?> inputType) {
        if (inputType == null) {
            return null;
        }
        CommandDescription description = inputType.getAnnotation(CommandDescription.class);
        return description == null || description.authorization().isBlank()
                ? null : description.authorization();
    }

    /** Turn a PascalCase command type into a spaced title, e.g. {@code EditGoal} → {@code Edit Goal}. */
    static String humanize(String commandType) {
        return commandType == null ? "" : commandType.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
    }
}

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
package com.rreganjr.requel.service.command;

import com.rreganjr.command.Command;
import com.rreganjr.requel.service.api.CommandRegistration;
import com.rreganjr.requel.service.api.CommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * In-memory command registry. Per-domain registrars register their command types
 * at startup via {@code @PostConstruct}. The composite ApiCommandFactory queries
 * this registry to look up input types and create commands.
 */
@Component
public class CommandRegistryImpl implements CommandRegistry {

    private static final Logger log = LoggerFactory.getLogger(CommandRegistryImpl.class);

    private final ConcurrentHashMap<String, CommandRegistration<?>> registrations = new ConcurrentHashMap<>();

    @Override
    public <T> void register(String commandType, Class<T> inputClass,
                             Supplier<Command> factoryMethod,
                             BiConsumer<Command, T> inputApplicator,
                             BiConsumer<Command, Object> fileApplicator,
                             Function<Command, Object> resultExtractor,
                             Function<Object, Command> commandBuilder) {
        var registration = new CommandRegistration<>(commandType, inputClass, factoryMethod,
                inputApplicator, fileApplicator, resultExtractor, commandBuilder);
        var existing = registrations.putIfAbsent(commandType, registration);
        if (existing != null) {
            throw new IllegalStateException(
                    "Duplicate command type registration: " + commandType);
        }
        log.debug("Registered command type: {} (input: {}, file: {}, result: {}, builder: {})", commandType,
                inputClass == Void.class ? "none" : inputClass.getSimpleName(),
                fileApplicator != null ? "yes" : "no",
                resultExtractor != null ? "yes" : "no",
                commandBuilder != null ? "yes" : "no");
    }

    @Override
    public CommandRegistration<?> lookup(String commandType) {
        var registration = registrations.get(commandType);
        if (registration == null) {
            throw new IllegalArgumentException("Unknown command type: " + commandType);
        }
        return registration;
    }

    @Override
    public boolean isRegistered(String commandType) {
        return registrations.containsKey(commandType);
    }
}

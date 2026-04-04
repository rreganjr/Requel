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
package com.rreganjr.requel.service.api;

import com.rreganjr.command.Command;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registry for API command types. Per-domain registrars register their command
 * types at startup via {@link #register}. The composite CommandFactory facade
 * queries this registry to look up input types and create commands.
 */
public interface CommandRegistry {

    /**
     * Full registration: all fields including optional commandBuilder.
     * When commandBuilder is non-null it is used in place of factoryMethod + inputApplicator —
     * it receives the raw deserialized input and returns a fully-configured command.
     * Use this for polymorphic commands where the correct subtype depends on the input.
     */
    <T> void register(String commandType, Class<T> inputClass,
                      Supplier<Command> factoryMethod,
                      BiConsumer<Command, T> inputApplicator,
                      BiConsumer<Command, Object> fileApplicator,
                      Function<Command, Object> resultExtractor,
                      Function<Object, Command> commandBuilder);

    /**
     * Standard registration: input class, factory, input applicator, file applicator, result extractor.
     */
    default <T> void register(String commandType, Class<T> inputClass,
                              Supplier<Command> factoryMethod,
                              BiConsumer<Command, T> inputApplicator,
                              BiConsumer<Command, Object> fileApplicator,
                              Function<Command, Object> resultExtractor) {
        register(commandType, inputClass, factoryMethod, inputApplicator, fileApplicator, resultExtractor, null);
    }

    /**
     * Register with input + file applicators, no result extractor.
     */
    default <T> void register(String commandType, Class<T> inputClass,
                              Supplier<Command> factoryMethod,
                              BiConsumer<Command, T> inputApplicator,
                              BiConsumer<Command, Object> fileApplicator) {
        register(commandType, inputClass, factoryMethod, inputApplicator, fileApplicator, null, null);
    }

    /**
     * Register with input applicator only (no file, no result extractor).
     */
    default <T> void register(String commandType, Class<T> inputClass,
                              Supplier<Command> factoryMethod,
                              BiConsumer<Command, T> inputApplicator) {
        register(commandType, inputClass, factoryMethod, inputApplicator, null, null, null);
    }

    /**
     * Register with no input mapping (placeholder for future DTO wiring).
     */
    default void register(String commandType, Supplier<Command> factoryMethod) {
        register(commandType, Void.class, factoryMethod, null, null, null, null);
    }

    /**
     * Register a command whose type depends on the input (polymorphic factory).
     * The commandBuilder function receives the deserialized input and must return
     * a fully-configured command ready for the handler chain.
     */
    default <T> void registerWithBuilder(String commandType, Class<T> inputClass,
                                         Function<T, Command> commandBuilder,
                                         Function<Command, Object> resultExtractor) {
        @SuppressWarnings("unchecked")
        Function<Object, Command> erased = input -> commandBuilder.apply((T) input);
        register(commandType, inputClass, null, null, null, resultExtractor, erased);
    }

    /**
     * Look up a registration by command type string.
     *
     * @throws IllegalArgumentException if the command type is not registered
     */
    CommandRegistration<?> lookup(String commandType);

    /**
     * @return true if a command type is registered
     */
    boolean isRegistered(String commandType);
}

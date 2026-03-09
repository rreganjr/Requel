package com.rreganjr.requel.service.api;

import com.rreganjr.command.Command;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Registry for API command types. Per-domain registrars register their command
 * types at startup via {@link #register}. The composite CommandFactory facade
 * queries this registry to look up input types and create commands.
 */
public interface CommandRegistry {

    /**
     * Register a command type with its input class, factory method, and input applicator.
     * The applicator maps a deserialized input DTO onto the command's setters.
     */
    <T> void register(String commandType, Class<T> inputClass,
                      Supplier<Command> factoryMethod,
                      BiConsumer<Command, T> inputApplicator);

    /**
     * Register a command type with no input mapping (placeholder for Phase 1+ DTO wiring).
     * Commands registered this way can be created but will receive no input.
     */
    default void register(String commandType, Supplier<Command> factoryMethod) {
        register(commandType, Void.class, factoryMethod, null);
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

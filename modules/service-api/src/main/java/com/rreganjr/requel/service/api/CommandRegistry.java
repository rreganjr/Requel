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
     * Full registration: input class, factory, input applicator, file applicator, result extractor.
     */
    <T> void register(String commandType, Class<T> inputClass,
                      Supplier<Command> factoryMethod,
                      BiConsumer<Command, T> inputApplicator,
                      BiConsumer<Command, Object> fileApplicator,
                      Function<Command, Object> resultExtractor);

    /**
     * Register with input + file applicators, no result extractor.
     */
    default <T> void register(String commandType, Class<T> inputClass,
                              Supplier<Command> factoryMethod,
                              BiConsumer<Command, T> inputApplicator,
                              BiConsumer<Command, Object> fileApplicator) {
        register(commandType, inputClass, factoryMethod, inputApplicator, fileApplicator, null);
    }

    /**
     * Register with input applicator only (no file, no result extractor).
     */
    default <T> void register(String commandType, Class<T> inputClass,
                              Supplier<Command> factoryMethod,
                              BiConsumer<Command, T> inputApplicator) {
        register(commandType, inputClass, factoryMethod, inputApplicator, null, null);
    }

    /**
     * Register with no input mapping (placeholder for future DTO wiring).
     */
    default void register(String commandType, Supplier<Command> factoryMethod) {
        register(commandType, Void.class, factoryMethod, null, null, null);
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

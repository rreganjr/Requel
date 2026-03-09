package com.rreganjr.requel.service.command;

import com.rreganjr.command.Command;
import com.rreganjr.requel.service.api.CommandRegistration;
import com.rreganjr.requel.service.api.CommandRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
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
                             BiConsumer<Command, T> inputApplicator) {
        var registration = new CommandRegistration<>(commandType, inputClass, factoryMethod, inputApplicator);
        var existing = registrations.putIfAbsent(commandType, registration);
        if (existing != null) {
            throw new IllegalStateException(
                    "Duplicate command type registration: " + commandType);
        }
        log.debug("Registered command type: {} (input: {})", commandType,
                inputClass == Void.class ? "none" : inputClass.getSimpleName());
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

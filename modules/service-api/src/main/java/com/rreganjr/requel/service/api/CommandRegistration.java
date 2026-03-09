package com.rreganjr.requel.service.api;

import com.rreganjr.command.Command;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Binds a command type string to its input DTO class, factory method, and
 * input applicator. Per-domain registrars register these at startup.
 * <p>
 * The input applicator is a function that maps a deserialized input DTO
 * onto the command's setter-based API. This keeps input mapping external
 * to the domain command, so existing commands don't need modification.
 *
 * @param commandType     the command type string (e.g. "EditGoal", "NewUser")
 * @param inputClass      the input DTO class for JSON deserialization (Void.class if no input)
 * @param factoryMethod   supplies a new, uninitialized command instance from the domain factory
 * @param inputApplicator maps the deserialized input DTO onto the command; null if no input mapping
 */
public record CommandRegistration<T>(
        String commandType,
        Class<T> inputClass,
        Supplier<Command> factoryMethod,
        BiConsumer<Command, T> inputApplicator
) {
}
